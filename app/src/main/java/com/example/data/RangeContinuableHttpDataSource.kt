package com.example.data

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * OkHttp-backed Media3 [DataSource] that seamlessly handles BOTH standard HTTP 200/206 streams
 * (HLS playlists, segments, subtitles, direct MP4s) AND proxy servers that cap individual
 * HTTP 206 Partial Content responses into fixed virtual chunks (e.g. 512 KB / 3 MB per request
 * with `Content-Range: bytes start-end/total`).
 *
 * Includes background lookahead prefetching of the next 206 chunk once 55% of the current chunk
 * has been read, eliminating chunk-boundary round-trip stalls during high-bitrate playback.
 */
@OptIn(UnstableApi::class)
class RangeContinuableHttpDataSource(
    private val okHttpClient: OkHttpClient,
    private val defaultHeaders: Map<String, String> = emptyMap(),
    private val userAgent: String = DEFAULT_USER_AGENT
) : BaseDataSource(/* isNetwork = */ true) {

    class Factory(
        private val headers: Map<String, String> = emptyMap(),
        private val connectTimeoutMs: Int = 15_000,
        private val readTimeoutMs: Int = 25_000,
        private val userAgent: String = DEFAULT_USER_AGENT
    ) : DataSource.Factory {

        private val client: OkHttpClient by lazy {
            sharedBaseClient.newBuilder()
                .connectTimeout(connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .writeTimeout(15_000L, TimeUnit.MILLISECONDS)
                .build()
        }

        override fun createDataSource(): DataSource = RangeContinuableHttpDataSource(
            okHttpClient = client,
            defaultHeaders = headers,
            userAgent = userAgent
        )
    }

    private class PrefetchedChunk(
        val expectedStartOffset: Long,
        val call: Call
    ) {
        val latch = CountDownLatch(1)

        @Volatile
        var response: Response? = null

        @Volatile
        var error: IOException? = null

        @Volatile
        var cancelled: Boolean = false

        fun cancelAndClose() {
            cancelled = true
            runCatching { call.cancel() }
            runCatching { response?.close() }
            response = null
            latch.countDown()
        }
    }

    private var currentDataSpec: DataSpec? = null
    private var currentUri: Uri? = null
    private var activeResponse: Response? = null
    private var activeStream: InputStream? = null
    private var opened = false

    private var currentPosition: Long = 0L
    private var currentChunkStart: Long = 0L
    private var currentChunkEndExclusive: Long = C.LENGTH_UNSET.toLong()
    private var requestedEndExclusive: Long = C.LENGTH_UNSET.toLong()
    private var bytesRemainingToDeliver: Long = C.LENGTH_UNSET.toLong()
    private var totalContentLength: Long = C.LENGTH_UNSET.toLong()
    private var supportsChunkContinuation: Boolean = false
    private var responseHeaders: Map<String, List<String>> = emptyMap()
    private var prefetchedNextChunk: PrefetchedChunk? = null

    override fun open(dataSpec: DataSpec): Long {
        cancelPrefetch()
        this.currentDataSpec = dataSpec
        this.currentUri = dataSpec.uri
        this.currentPosition = dataSpec.position
        this.currentChunkStart = dataSpec.position
        this.currentChunkEndExclusive = C.LENGTH_UNSET.toLong()
        this.requestedEndExclusive = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.position + dataSpec.length
        } else {
            C.LENGTH_UNSET.toLong()
        }
        this.totalContentLength = C.LENGTH_UNSET.toLong()
        this.supportsChunkContinuation = false

        transferInitializing(dataSpec)

        val response = executeChunkRequestWithRetry(
            dataSpec = dataSpec,
            startOffset = dataSpec.position,
            endInclusive = if (requestedEndExclusive != C.LENGTH_UNSET.toLong()) {
                requestedEndExclusive - 1L
            } else {
                C.LENGTH_UNSET.toLong()
            },
            forceRangeHeader = dataSpec.position > 0L || dataSpec.length != C.LENGTH_UNSET.toLong()
        )

        val code = response.code
        this.responseHeaders = response.headers.toMultimap()

        // HTTP 416 Range Not Satisfiable at or past EOF -> treat cleanly as 0-byte empty stream
        if (code == 416 && dataSpec.position > 0L) {
            response.close()
            this.bytesRemainingToDeliver = 0L
            this.opened = true
            transferStarted(dataSpec)
            return 0L
        }

        if (code < 200 || code > 299) {
            val message = response.message
            val headersMap = response.headers.toMultimap()
            response.close()
            throw HttpDataSource.InvalidResponseCodeException(
                code,
                message,
                null,
                headersMap,
                dataSpec,
                ByteArray(0)
            )
        }

        val body = response.body ?: run {
            response.close()
            throw HttpDataSource.HttpDataSourceException(
                IOException("Empty HTTP response body"),
                dataSpec,
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }

        val inputStream = body.byteStream()
        this.activeResponse = response
        this.activeStream = inputStream

        val headerContentLength = response.header("Content-Length")?.trim()?.toLongOrNull()
            ?: body.contentLength().takeIf { it >= 0L }
            ?: C.LENGTH_UNSET.toLong()

        val contentRange = parseContentRange(response.header("Content-Range"))

        // If server ignored our Range header and returned 200 OK while position > 0, skip forward safely
        if (code == 200 && dataSpec.position > 0L) {
            try {
                skipFully(inputStream, dataSpec.position)
            } catch (e: IOException) {
                closeActiveChunk()
                throw HttpDataSource.HttpDataSourceException.createForIOException(
                    e,
                    dataSpec,
                    HttpDataSource.HttpDataSourceException.TYPE_OPEN
                )
            }
        }

        if (code == 206 && contentRange != null && contentRange.totalLength > 0L) {
            this.totalContentLength = contentRange.totalLength
            this.currentChunkStart = dataSpec.position
            this.currentChunkEndExclusive = contentRange.end + 1L
            if (contentRange.start < dataSpec.position) {
                try {
                    skipFully(inputStream, dataSpec.position - contentRange.start)
                } catch (e: IOException) {
                    closeActiveChunk()
                    throw HttpDataSource.HttpDataSourceException.createForIOException(
                        e,
                        dataSpec,
                        HttpDataSource.HttpDataSourceException.TYPE_OPEN
                    )
                }
            }
            val remainingInFile = (contentRange.totalLength - dataSpec.position).coerceAtLeast(0L)
            this.bytesRemainingToDeliver = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                minOf(dataSpec.length, remainingInFile)
            } else {
                remainingInFile
            }
            val chunkEndsBeforeRequested = if (requestedEndExclusive != C.LENGTH_UNSET.toLong()) {
                (contentRange.end + 1L) < minOf(requestedEndExclusive, contentRange.totalLength)
            } else {
                (contentRange.end + 1L) < contentRange.totalLength
            }
            this.supportsChunkContinuation = chunkEndsBeforeRequested
        } else {
            val adjustedRemaining = if (code == 200 && headerContentLength >= 0L) {
                (headerContentLength - dataSpec.position).coerceAtLeast(0L)
            } else {
                headerContentLength
            }
            this.totalContentLength = if (code == 200 && headerContentLength >= 0L) {
                headerContentLength
            } else {
                C.LENGTH_UNSET.toLong()
            }
            this.bytesRemainingToDeliver = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                if (adjustedRemaining >= 0L) minOf(dataSpec.length, adjustedRemaining) else dataSpec.length
            } else {
                adjustedRemaining
            }
            this.supportsChunkContinuation = false
        }

        this.opened = true
        transferStarted(dataSpec)
        return this.bytesRemainingToDeliver
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemainingToDeliver == 0L) return C.RESULT_END_OF_INPUT

        val spec = currentDataSpec ?: return C.RESULT_END_OF_INPUT

        while (true) {
            val stream = activeStream
            if (stream == null) {
                if (!openNextChunkIfAvailable(spec)) {
                    return C.RESULT_END_OF_INPUT
                }
                continue
            }

            val maxToRead = if (bytesRemainingToDeliver != C.LENGTH_UNSET.toLong()) {
                minOf(length.toLong(), bytesRemainingToDeliver).toInt()
            } else {
                length
            }
            if (maxToRead <= 0) return C.RESULT_END_OF_INPUT

            val bytesRead = try {
                stream.read(buffer, offset, maxToRead)
            } catch (e: IOException) {
                if (supportsChunkContinuation && openNextChunkIfAvailable(spec)) {
                    continue
                }
                throw HttpDataSource.HttpDataSourceException.createForIOException(
                    e,
                    spec,
                    HttpDataSource.HttpDataSourceException.TYPE_READ
                )
            }

            if (bytesRead == -1) {
                closeActiveChunk()
                if (openNextChunkIfAvailable(spec)) {
                    continue
                }
                return C.RESULT_END_OF_INPUT
            }

            if (bytesRead > 0) {
                currentPosition += bytesRead
                if (bytesRemainingToDeliver != C.LENGTH_UNSET.toLong()) {
                    bytesRemainingToDeliver = (bytesRemainingToDeliver - bytesRead).coerceAtLeast(0L)
                }
                maybeStartNextChunkPrefetch(spec)
                bytesTransferred(bytesRead)
                return bytesRead
            }
        }
    }

    private fun maybeStartNextChunkPrefetch(dataSpec: DataSpec) {
        if (!supportsChunkContinuation || prefetchedNextChunk != null) return
        val chunkEnd = currentChunkEndExclusive
        if (chunkEnd == C.LENGTH_UNSET.toLong() || chunkEnd <= currentChunkStart) return
        if (totalContentLength > 0L && chunkEnd >= totalContentLength) return
        if (requestedEndExclusive != C.LENGTH_UNSET.toLong() && chunkEnd >= requestedEndExclusive) return

        val chunkSize = chunkEnd - currentChunkStart
        val readInChunk = currentPosition - currentChunkStart
        // Start warming up the next 206 chunk early (at 25%) so slow proxy TTFB never stalls playback
        if (chunkSize <= 0L || readInChunk * 100L < chunkSize * 25L) return

        val endInclusive = computeEndInclusive()
        if (endInclusive != C.LENGTH_UNSET.toLong() && endInclusive < chunkEnd) return

        val request = runCatching {
            buildChunkRequest(
                dataSpec = dataSpec,
                startOffset = chunkEnd,
                endInclusive = endInclusive,
                forceRangeHeader = true
            )
        }.getOrNull() ?: return

        val call = okHttpClient.newCall(request)
        val pending = PrefetchedChunk(expectedStartOffset = chunkEnd, call = call)
        prefetchedNextChunk = pending
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                pending.error = e
                pending.latch.countDown()
            }

            override fun onResponse(call: Call, response: Response) {
                if (pending.cancelled) {
                    runCatching { response.close() }
                } else {
                    pending.response = response
                }
                pending.latch.countDown()
            }
        })
    }

    private fun takePrefetchedResponseIfMatching(startOffset: Long): Response? {
        val pending = prefetchedNextChunk ?: return null
        prefetchedNextChunk = null
        if (pending.expectedStartOffset != startOffset) {
            pending.cancelAndClose()
            return null
        }
        return try {
            val completed = pending.latch.await(15_000L, TimeUnit.MILLISECONDS)
            if (!completed) {
                pending.cancelAndClose()
                null
            } else {
                val resp = pending.response
                pending.response = null
                resp
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            pending.cancelAndClose()
            null
        }
    }

    private fun cancelPrefetch() {
        prefetchedNextChunk?.cancelAndClose()
        prefetchedNextChunk = null
    }

    private fun computeEndInclusive(): Long = when {
        requestedEndExclusive != C.LENGTH_UNSET.toLong() && totalContentLength > 0L ->
            minOf(requestedEndExclusive, totalContentLength) - 1L
        requestedEndExclusive != C.LENGTH_UNSET.toLong() ->
            requestedEndExclusive - 1L
        else -> C.LENGTH_UNSET.toLong()
    }

    private fun openNextChunkIfAvailable(dataSpec: DataSpec): Boolean {
        if (!supportsChunkContinuation) return false
        if (totalContentLength > 0L && currentPosition >= totalContentLength) return false
        if (bytesRemainingToDeliver == 0L) return false

        closeActiveChunk()

        val endInclusive = computeEndInclusive()
        if (endInclusive != C.LENGTH_UNSET.toLong() && endInclusive < currentPosition) {
            return false
        }

        var lastError: IOException? = null
        val prefetched = takePrefetchedResponseIfMatching(currentPosition)
        for (attempt in 0..2) {
            try {
                val response = if (attempt == 0 && prefetched != null) {
                    prefetched
                } else {
                    if (attempt > 0) {
                        runCatching { Thread.sleep(if (attempt == 1) 200L else 500L) }
                    }
                    executeChunkRequest(
                        dataSpec = dataSpec,
                        startOffset = currentPosition,
                        endInclusive = endInclusive,
                        forceRangeHeader = true
                    )
                }
                val code = response.code
                if (code == 416) {
                    response.close()
                    bytesRemainingToDeliver = 0L
                    return false
                }
                if (code !in 200..299) {
                    response.close()
                    throw IOException("Unexpected HTTP $code while fetching continuation chunk at byte $currentPosition")
                }
                val body = response.body ?: run {
                    response.close()
                    throw IOException("Empty body while fetching continuation chunk at byte $currentPosition")
                }
                val nextStream = body.byteStream()
                val nextRange = parseContentRange(response.header("Content-Range"))
                if (code == 206 && nextRange != null && nextRange.totalLength > 0L) {
                    if (nextRange.start < currentPosition) {
                        skipFully(nextStream, currentPosition - nextRange.start)
                    } else if (nextRange.start > currentPosition) {
                        response.close()
                        throw IOException("Range gap: expected $currentPosition, got ${nextRange.start}")
                    }
                    totalContentLength = nextRange.totalLength
                    currentChunkStart = currentPosition
                    currentChunkEndExclusive = nextRange.end + 1L
                    supportsChunkContinuation = (nextRange.end + 1L) < nextRange.totalLength
                } else if (code == 200) {
                    skipFully(nextStream, currentPosition)
                    currentChunkStart = currentPosition
                    currentChunkEndExclusive = C.LENGTH_UNSET.toLong()
                    supportsChunkContinuation = false
                } else {
                    currentChunkStart = currentPosition
                    currentChunkEndExclusive = C.LENGTH_UNSET.toLong()
                    supportsChunkContinuation = false
                }
                activeResponse = response
                activeStream = nextStream
                return true
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw HttpDataSource.HttpDataSourceException.createForIOException(
            lastError ?: IOException("Failed to continue partial stream"),
            dataSpec,
            HttpDataSource.HttpDataSourceException.TYPE_READ
        )
    }

    private fun executeChunkRequestWithRetry(
        dataSpec: DataSpec,
        startOffset: Long,
        endInclusive: Long,
        forceRangeHeader: Boolean
    ): Response {
        return try {
            executeChunkRequest(dataSpec, startOffset, endInclusive, forceRangeHeader)
        } catch (firstErr: HttpDataSource.HttpDataSourceException) {
            if (firstErr.cause is SocketTimeoutException) {
                runCatching { Thread.sleep(250L) }
                executeChunkRequest(dataSpec, startOffset, endInclusive, forceRangeHeader)
            } else {
                throw firstErr
            }
        }
    }

    private fun buildChunkRequest(
        dataSpec: DataSpec,
        startOffset: Long,
        endInclusive: Long,
        forceRangeHeader: Boolean
    ): Request {
        val urlString = dataSpec.uri.toString().trim()
        val requestBuilder = try {
            Request.Builder().url(urlString)
        } catch (e: IllegalArgumentException) {
            throw HttpDataSource.HttpDataSourceException(
                IOException("Invalid stream URL: $urlString", e),
                dataSpec,
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }

        if (dataSpec.httpMethod == DataSpec.HTTP_METHOD_HEAD) {
            requestBuilder.head()
        }

        val mergedHeaders = linkedMapOf<String, String>()
        defaultHeaders.forEach { (k, v) -> if (k.isNotBlank() && v.isNotBlank()) mergedHeaders[k.trim()] = v.trim() }
        dataSpec.httpRequestHeaders.forEach { (k, v) -> if (k.isNotBlank() && v.isNotBlank()) mergedHeaders[k.trim()] = v.trim() }

        val hasCustomUa = mergedHeaders.keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasCustomUa) {
            requestBuilder.header("User-Agent", userAgent)
        }
        mergedHeaders.forEach { (k, v) ->
            if (!k.equals("Range", ignoreCase = true)) {
                runCatching { requestBuilder.header(k, v) }
            }
        }

        if (forceRangeHeader || startOffset > 0L || endInclusive != C.LENGTH_UNSET.toLong()) {
            val rangeValue = if (endInclusive != C.LENGTH_UNSET.toLong() && endInclusive >= startOffset) {
                "bytes=$startOffset-$endInclusive"
            } else {
                "bytes=$startOffset-"
            }
            requestBuilder.header("Range", rangeValue)
        }

        if (!mergedHeaders.keys.any { it.equals("Accept-Encoding", ignoreCase = true) }) {
            requestBuilder.header("Accept-Encoding", "identity")
        }

        return requestBuilder.build()
    }

    private fun executeChunkRequest(
        dataSpec: DataSpec,
        startOffset: Long,
        endInclusive: Long,
        forceRangeHeader: Boolean
    ): Response {
        val request = buildChunkRequest(dataSpec, startOffset, endInclusive, forceRangeHeader)
        return try {
            okHttpClient.newCall(request).execute()
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }
    }

    private fun skipFully(stream: InputStream, bytesToSkip: Long) {
        if (bytesToSkip > 2L * 1024L * 1024L) {
            throw IOException("Server ignored Range request (refusing to skip ${bytesToSkip / (1024 * 1024)} MB)")
        }
        var remaining = bytesToSkip
        val skipBuf = ByteArray(16_384)
        while (remaining > 0L) {
            val toRead = minOf(remaining, skipBuf.size.toLong()).toInt()
            val n = stream.read(skipBuf, 0, toRead)
            if (n == -1) throw EOFException("Stream ended while skipping $bytesToSkip bytes")
            remaining -= n
        }
    }

    private fun closeActiveChunk() {
        try {
            activeStream?.close()
        } catch (_: Exception) {
            // Ignore stream close errors
        }
        activeStream = null
        try {
            activeResponse?.close()
        } catch (_: Exception) {
            // Ignore response close errors
        }
        activeResponse = null
    }

    override fun getUri(): Uri? = activeResponse?.request?.url?.toString()?.let { Uri.parse(it) } ?: currentUri

    override fun getResponseHeaders(): Map<String, List<String>> = responseHeaders

    override fun close() {
        cancelPrefetch()
        closeActiveChunk()
        currentDataSpec = null
        currentUri = null
        currentChunkEndExclusive = C.LENGTH_UNSET.toLong()
        supportsChunkContinuation = false
        if (opened) {
            opened = false
            transferEnded()
        }
    }

    private data class ParsedContentRange(
        val start: Long,
        val end: Long,
        val totalLength: Long
    )

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        private val CONTENT_RANGE_REGEX = Regex("""bytes\s+(\d+)-(\d+)/(\d+|\*)""", RegexOption.IGNORE_CASE)

        private val sharedBaseClient: OkHttpClient by lazy {
            val dispatcher = Dispatcher().apply {
                maxRequests = 48
                maxRequestsPerHost = 16
            }
            OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }

        private fun parseContentRange(header: String?): ParsedContentRange? {
            if (header.isNullOrBlank()) return null
            val match = CONTENT_RANGE_REGEX.find(header.trim()) ?: return null
            val start = match.groupValues[1].toLongOrNull() ?: return null
            val end = match.groupValues[2].toLongOrNull() ?: return null
            val totalStr = match.groupValues[3]
            val total = if (totalStr == "*") C.LENGTH_UNSET.toLong() else (totalStr.toLongOrNull() ?: return null)
            return ParsedContentRange(start = start, end = end, totalLength = total)
        }
    }
}
