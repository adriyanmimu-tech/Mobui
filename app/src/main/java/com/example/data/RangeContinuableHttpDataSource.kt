package com.example.data

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * OkHttp-backed Media3 [DataSource] that seamlessly handles BOTH standard HTTP 200/206 streams
 * (HLS playlists, segments, subtitles, direct MP4s) AND proxy servers that cap individual
 * HTTP 206 Partial Content responses into fixed virtual chunks (e.g. 512 KB / 3 MB per request
 * with `Content-Range: bytes start-end/total`).
 *
 * Media3's built-in DefaultHttpDataSource treats a single capped 206 chunk's Content-Length as
 * EOF, causing Mp4Extractor to hit STATE_ENDED before parsing large `moov` atoms. This class
 * reports the true remaining file length from `Content-Range` and transparently issues follow-up
 * Range requests over a pooled HTTP/2 OkHttp connection whenever a server-capped chunk ends.
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

    private var currentDataSpec: DataSpec? = null
    private var currentUri: Uri? = null
    private var activeResponse: Response? = null
    private var activeStream: InputStream? = null
    private var opened = false

    private var currentPosition: Long = 0L
    private var requestedEndExclusive: Long = C.LENGTH_UNSET.toLong()
    private var bytesRemainingToDeliver: Long = C.LENGTH_UNSET.toLong()
    private var totalContentLength: Long = C.LENGTH_UNSET.toLong()
    private var supportsChunkContinuation: Boolean = false
    private var responseHeaders: Map<String, List<String>> = emptyMap()

    override fun open(dataSpec: DataSpec): Long {
        this.currentDataSpec = dataSpec
        this.currentUri = dataSpec.uri
        this.currentPosition = dataSpec.position
        this.requestedEndExclusive = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.position + dataSpec.length
        } else {
            C.LENGTH_UNSET.toLong()
        }
        this.totalContentLength = C.LENGTH_UNSET.toLong()
        this.supportsChunkContinuation = false

        transferInitializing(dataSpec)

        val response = executeChunkRequest(
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
                androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
        }

        this.activeResponse = response
        this.activeStream = body.byteStream()

        val headerContentLength = response.header("Content-Length")?.trim()?.toLongOrNull()
            ?: body.contentLength().takeIf { it >= 0L }
            ?: C.LENGTH_UNSET.toLong()

        val contentRange = parseContentRange(response.header("Content-Range"))

        if (code == 206 && contentRange != null && contentRange.totalLength > 0L) {
            this.totalContentLength = contentRange.totalLength
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
            this.totalContentLength = if (code == 200 && headerContentLength >= 0L) {
                headerContentLength
            } else {
                C.LENGTH_UNSET.toLong()
            }
            this.bytesRemainingToDeliver = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
                if (headerContentLength >= 0L) minOf(dataSpec.length, headerContentLength) else dataSpec.length
            } else {
                headerContentLength
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
                // Retry once from the exact byte offset if a chunked proxy connection dropped mid-read
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
                bytesTransferred(bytesRead)
                return bytesRead
            }
        }
    }

    private fun openNextChunkIfAvailable(dataSpec: DataSpec): Boolean {
        if (!supportsChunkContinuation) return false
        if (totalContentLength > 0L && currentPosition >= totalContentLength) return false
        if (bytesRemainingToDeliver == 0L) return false

        closeActiveChunk()

        val endInclusive = when {
            requestedEndExclusive != C.LENGTH_UNSET.toLong() && totalContentLength > 0L ->
                minOf(requestedEndExclusive, totalContentLength) - 1L
            requestedEndExclusive != C.LENGTH_UNSET.toLong() ->
                requestedEndExclusive - 1L
            else -> C.LENGTH_UNSET.toLong()
        }

        if (endInclusive != C.LENGTH_UNSET.toLong() && endInclusive < currentPosition) {
            return false
        }

        var lastError: IOException? = null
        for (attempt in 0..1) {
            try {
                val response = executeChunkRequest(
                    dataSpec = dataSpec,
                    startOffset = currentPosition,
                    endInclusive = endInclusive,
                    forceRangeHeader = true
                )
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
                val nextRange = parseContentRange(response.header("Content-Range"))
                if (code == 206 && nextRange != null && nextRange.totalLength > 0L) {
                    totalContentLength = nextRange.totalLength
                    supportsChunkContinuation = (nextRange.end + 1L) < nextRange.totalLength
                } else {
                    supportsChunkContinuation = false
                }
                activeResponse = response
                activeStream = body.byteStream()
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

    private fun executeChunkRequest(
        dataSpec: DataSpec,
        startOffset: Long,
        endInclusive: Long,
        forceRangeHeader: Boolean
    ): Response {
        val requestBuilder = Request.Builder().url(dataSpec.uri.toString())

        val mergedHeaders = linkedMapOf<String, String>()
        defaultHeaders.forEach { (k, v) -> if (k.isNotBlank() && v.isNotBlank()) mergedHeaders[k] = v }
        dataSpec.httpRequestHeaders.forEach { (k, v) -> if (k.isNotBlank() && v.isNotBlank()) mergedHeaders[k] = v }

        val hasCustomUa = mergedHeaders.keys.any { it.equals("User-Agent", ignoreCase = true) }
        if (!hasCustomUa) {
            requestBuilder.header("User-Agent", userAgent)
        }
        mergedHeaders.forEach { (k, v) ->
            if (!k.equals("Range", ignoreCase = true)) {
                requestBuilder.header(k, v)
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

        // Disable transparent gzip on media bytes so byte offsets match Content-Range exactly
        if (!mergedHeaders.keys.any { it.equals("Accept-Encoding", ignoreCase = true) }) {
            requestBuilder.header("Accept-Encoding", "identity")
        }

        return try {
            okHttpClient.newCall(requestBuilder.build()).execute()
        } catch (e: IOException) {
            throw HttpDataSource.HttpDataSourceException.createForIOException(
                e,
                dataSpec,
                HttpDataSource.HttpDataSourceException.TYPE_OPEN
            )
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
        closeActiveChunk()
        currentDataSpec = null
        currentUri = null
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
            OkHttpClient.Builder()
                .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
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
