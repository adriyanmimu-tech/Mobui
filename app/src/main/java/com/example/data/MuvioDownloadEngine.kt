package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.R
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.PosterArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class MuvioDownloadEngine(private val context: Context) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private fun downloadsDir(): File {
        val dir = File(context.filesDir, "muvio_offline_videos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun manifestFile(): File = File(downloadsDir(), "downloads_manifest.json")

    fun targetFileFor(downloadId: String): File {
        val safeName = downloadId.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(88)
        return File(downloadsDir(), "$safeName.mp4")
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun deleteFile(downloadId: String) {
        runCatching {
            val f = targetFileFor(downloadId)
            if (f.exists()) f.delete()
        }
    }

    suspend fun calculateDownloadsBytes(): Long = withContext(Dispatchers.IO) {
        runCatching {
            downloadsDir().listFiles()
                ?.filter { it.isFile && it.name.endsWith(".mp4") }
                ?.sumOf { it.length() }
                ?: 0L
        }.getOrDefault(0L)
    }

    suspend fun calculateCacheBytes(): Long = withContext(Dispatchers.IO) {
        runCatching {
            val imgDir = File(context.cacheDir, "muvio_img_cache")
            val subDir = File(context.cacheDir, "muvio_bn_subs")
            directorySize(imgDir) + directorySize(subDir)
        }.getOrDefault(0L)
    }

    suspend fun clearAppCaches(): Long = withContext(Dispatchers.IO) {
        runCatching {
            val before = calculateCacheBytes()
            File(context.cacheDir, "muvio_img_cache").deleteRecursively()
            File(context.cacheDir, "muvio_bn_subs").deleteRecursively()
            before
        }.getOrDefault(0L)
    }

    private fun directorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun saveDownloadsManifest(downloads: List<DownloadItem>) = withContext(Dispatchers.IO) {
        runCatching {
            val arr = JSONArray()
            downloads
                .filter { it.status == DownloadStatus.COMPLETED && !it.localFilePath.isNullOrBlank() }
                .forEach { dl ->
                    val file = File(dl.localFilePath!!)
                    if (file.exists() && file.length() > 0L) {
                        arr.put(
                            JSONObject().apply {
                                put("downloadId", dl.downloadId)
                                put("mediaId", dl.mediaId)
                                put("title", dl.title)
                                put("quality", dl.quality)
                                put("sizeText", dl.sizeText)
                                put("localFilePath", dl.localFilePath)
                                put("streamUrl", dl.streamUrl)
                                put("posterUrl", dl.poster.remoteUrl.orEmpty())
                                put("season", dl.season)
                                put("episode", dl.episode)
                                put("episodeLabel", dl.episodeLabel)
                            }
                        )
                    }
                }
            manifestFile().writeText(arr.toString())
        }
    }

    suspend fun loadPersistedDownloads(): List<DownloadItem> = withContext(Dispatchers.IO) {
        runCatching {
            val mf = manifestFile()
            if (!mf.exists()) return@runCatching emptyList()
            val arr = JSONArray(mf.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val localPath = obj.optString("localFilePath", "")
                    val file = File(localPath)
                    if (localPath.isBlank() || !file.exists() || file.length() <= 0L) continue
                    val mediaId = obj.optString("mediaId", "")
                    val downloadId = obj.optString("downloadId", "").ifBlank { mediaId }
                    val title = obj.optString("title", "Offline Video")
                    if (mediaId.isBlank()) continue
                    val posterUrl = obj.optString("posterUrl", "").takeIf { it.isNotBlank() }
                    add(
                        DownloadItem(
                            mediaId = mediaId,
                            title = title,
                            quality = obj.optString("quality", "HD"),
                            sizeText = obj.optString("sizeText", "${formatMb(file.length())} • Ready offline"),
                            progress = 1f,
                            status = DownloadStatus.COMPLETED,
                            poster = PosterArtwork(
                                remoteUrl = posterUrl,
                                fallbackRes = R.drawable.img_poster_action,
                                tintHex = 0xFF141923,
                                accentHex = 0xFFE5C185,
                                badgeTitle = title
                            ),
                            localFilePath = localPath,
                            streamUrl = obj.optString("streamUrl", ""),
                            downloadId = downloadId,
                            season = obj.optInt("season", 1),
                            episode = obj.optInt("episode", 1),
                            episodeLabel = obj.optString("episodeLabel", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Multi-chunk Range-continuable MP4 downloader.
     * Automatically resumes and stitches capped HTTP 206 chunks (e.g., 16-50 MB proxy chunks)
     * until the entire video file is downloaded without premature completion.
     */
    suspend fun downloadVideo(
        downloadId: String,
        streamUrl: String,
        headers: Map<String, String>,
        onProgress: (progress: Float, sizeLabel: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (streamUrl.contains(".m3u8", ignoreCase = true) || streamUrl.contains("/api/hls/", ignoreCase = true)) {
                throw IOException("HLS streams cannot be saved offline yet. Select an MP4 stream.")
            }
            val outFile = targetFileFor(downloadId)
            var downloaded = if (outFile.exists()) outFile.length() else 0L
            var knownTotalBytes = -1L
            var consecutiveRetries = 0
            var chunkRounds = 0

            while (chunkRounds < 400) {
                coroutineContext.ensureActive()
                chunkRounds++

                val reqBuilder = Request.Builder()
                    .url(streamUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    )
                headers.forEach { (k, v) ->
                    if (k.isNotBlank() && v.isNotBlank()) reqBuilder.header(k, v)
                }
                if (downloaded > 0L) {
                    reqBuilder.header("Range", "bytes=$downloaded-")
                }

                val chunkOutcome = try {
                    client.newCall(reqBuilder.build()).execute().use { response ->
                        if (response.code == 416 && downloaded > 0L) {
                            return@use ChunkOutcome.Completed
                        }
                        if (!response.isSuccessful) {
                            throw IOException("Download failed (HTTP ${response.code})")
                        }

                        val isPartial = response.code == 206
                        val parsedTotal = parseContentRangeTotal(response.header("Content-Range"))
                        if (parsedTotal > 0L) {
                            knownTotalBytes = parsedTotal
                        }

                        val body = response.body ?: throw IOException("Empty video response")
                        val append = isPartial && downloaded > 0L
                        if (!append) {
                            downloaded = 0L
                        }
                        val contentLength = body.contentLength()
                        if (knownTotalBytes <= 0L && contentLength > 0L && !isPartial) {
                            knownTotalBytes = contentLength
                        }

                        var bytesThisRound = 0L
                        body.byteStream().use { input ->
                            FileOutputStream(outFile, append).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var lastReportMs = 0L
                                while (true) {
                                    coroutineContext.ensureActive()
                                    val read = input.read(buffer)
                                    if (read == -1) break
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    bytesThisRound += read

                                    val now = System.currentTimeMillis()
                                    if (now - lastReportMs >= 420L) {
                                        lastReportMs = now
                                        val frac = if (knownTotalBytes > 0L) {
                                            (downloaded.toFloat() / knownTotalBytes.toFloat()).coerceIn(0.01f, 0.99f)
                                        } else {
                                            0.25f
                                        }
                                        val label = if (knownTotalBytes > 0L) {
                                            "${formatMb(downloaded)} / ${formatMb(knownTotalBytes)}"
                                        } else {
                                            "${formatMb(downloaded)} downloaded"
                                        }
                                        onProgress(frac, label)
                                    }
                                }
                                output.flush()
                            }
                        }

                        when {
                            knownTotalBytes > 0L && downloaded >= knownTotalBytes -> ChunkOutcome.Completed
                            isPartial && bytesThisRound > 0L && (knownTotalBytes <= 0L || downloaded < knownTotalBytes) -> ChunkOutcome.ContinueNextChunk
                            !isPartial -> ChunkOutcome.Completed
                            else -> ChunkOutcome.Completed
                        }
                    }
                } catch (ioe: IOException) {
                    if (downloaded > 0L && consecutiveRetries < 3) {
                        consecutiveRetries++
                        delay(450L * consecutiveRetries)
                        ChunkOutcome.ContinueNextChunk
                    } else {
                        throw ioe
                    }
                }

                when (chunkOutcome) {
                    ChunkOutcome.Completed -> break
                    ChunkOutcome.ContinueNextChunk -> {
                        consecutiveRetries = 0
                    }
                }
            }

            if (downloaded <= 0L) {
                throw IOException("Downloaded file is empty")
            }
            onProgress(1f, "${formatMb(downloaded)} • Ready offline")
            outFile
        }
    }

    private enum class ChunkOutcome {
        ContinueNextChunk,
        Completed
    }

    private fun parseContentRangeTotal(header: String?): Long {
        if (header.isNullOrBlank()) return -1L
        val slashIdx = header.lastIndexOf('/')
        if (slashIdx < 0 || slashIdx + 1 >= header.length) return -1L
        val totalPart = header.substring(slashIdx + 1).trim()
        if (totalPart == "*") return -1L
        return totalPart.toLongOrNull() ?: -1L
    }

    fun formatMb(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return when {
            bytes <= 0L -> "0 MB"
            mb >= 1024.0 -> String.format("%.2f GB", mb / 1024.0)
            mb < 0.1 -> "${(bytes / 1024L).coerceAtLeast(1L)} KB"
            else -> String.format("%.1f MB", mb)
        }
    }
}
