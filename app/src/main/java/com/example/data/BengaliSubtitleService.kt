package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Universal Bengali subtitle engine for Muvio.
 *
 * Resolution pipeline:
 * 1. Native Bengali subtitle track already provided by MovieBox (Source 1) or Source 2.
 * 2. Instant disk-cached Bengali `.vtt` file from previous translation.
 * 3. Cross-source MovieBox subtitle lookup when playing a Source 2 title with no caption tracks.
 * 4. Fast parallel SRT/VTT cue parser & Bengali translator that builds a clean UTF-8 WebVTT file
 *    so ANY movie/episode with English/Hindi/other captions plays with crisp Bengali subtitles.
 */
object BengaliSubtitleService {

    private const val CHROME_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val movieBoxFallbackClient: MovieBoxApiClient by lazy {
        MovieBoxApiClient()
    }

    data class SubtitleCue(
        val startTime: String,
        val endTime: String,
        val text: String
    )

    fun isBengaliTrack(track: SubtitleTrack): Boolean {
        val lang = track.language.trim().lowercase()
        val label = track.label.trim().lowercase()
        return lang == "bn" ||
            lang == "ben" ||
            lang.startsWith("bn-") ||
            lang.startsWith("bn_") ||
            label.contains("bangla") ||
            label.contains("bengali") ||
            label.contains("বাংলা")
    }

    /**
     * Keeps native Bengali tracks at the top so they are easy to spot and auto-selected first.
     */
    fun prioritizeBengaliFirst(tracks: List<SubtitleTrack>): List<SubtitleTrack> {
        if (tracks.size <= 1) return tracks
        val (bengali, others) = tracks.partition { isBengaliTrack(it) }
        return bengali + others
    }

    /**
     * Resolves a native Bengali track if available, or builds & caches a translated Bengali WebVTT
     * subtitle file from the movie's existing subtitles (or MovieBox cross-lookup subtitles).
     */
    suspend fun resolveOrTranslateBengaliSubtitle(
        context: Context,
        title: String,
        season: Int,
        episode: Int,
        existingSubtitles: List<SubtitleTrack>,
        streamHeaders: Map<String, String> = emptyMap(),
        onProgress: (Float) -> Unit = {}
    ): Result<SubtitleTrack> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Return native Bengali immediately if already present
            existingSubtitles.firstOrNull { isBengaliTrack(it) }?.let { nativeBn ->
                onProgress(1f)
                return@runCatching nativeBn
            }

            // 2. Gather candidate source subtitles (existing first, else cross-lookup MovieBox by title)
            var candidates = existingSubtitles
            if (candidates.isEmpty() && title.isNotBlank()) {
                onProgress(0.08f)
                candidates = lookupMovieBoxSubtitles(title, season, episode)
                candidates.firstOrNull { isBengaliTrack(it) }?.let { crossNativeBn ->
                    onProgress(1f)
                    return@runCatching crossNativeBn.copy(
                        label = "বাংলা (MovieBox)"
                    )
                }
            }

            val sourceTrack = pickBestSourceTrackForTranslation(candidates)
                ?: error("এই মুভির কোনো সোর্স সাবটাইটেল পাওয়া যায়নি")

            // 3. Check disk cache before translating
            val cacheDir = File(context.cacheDir, "muvio_bn_subs").apply { mkdirs() }
            val cacheKey = sha256("${sourceTrack.url}|bn_v1").take(24)
            val cacheFile = File(cacheDir, "$cacheKey.vtt")
            if (cacheFile.exists() && cacheFile.length() > 64L) {
                onProgress(1f)
                return@runCatching SubtitleTrack(
                    id = "bn_auto_$cacheKey",
                    language = "bn",
                    label = "বাংলা (Auto)",
                    url = Uri.fromFile(cacheFile).toString()
                )
            }

            // 4. Download source subtitle payload
            onProgress(0.15f)
            val rawPayload = downloadSubtitleText(sourceTrack.url, streamHeaders)
            val cues = parseCues(rawPayload)
            if (cues.isEmpty()) {
                error("সাবটাইটেল ফাইল পড়া যায়নি")
            }

            // 5. Translate cues in parallel batches (fast & preserves exact timestamps)
            val translatedCues = translateCuesToBengali(
                cues = cues,
                onProgress = { frac -> onProgress(0.18f + frac * 0.78f) }
            )

            // 6. Write clean UTF-8 WebVTT to cache
            val vttContent = buildWebVtt(translatedCues)
            cacheFile.writeText(vttContent, Charsets.UTF_8)
            pruneOldCacheFiles(cacheDir, maxFiles = 30)

            onProgress(1f)
            SubtitleTrack(
                id = "bn_auto_$cacheKey",
                language = "bn",
                label = "বাংলা (Auto)",
                url = Uri.fromFile(cacheFile).toString()
            )
        }
    }

    private suspend fun lookupMovieBoxSubtitles(
        title: String,
        season: Int,
        episode: Int
    ): List<SubtitleTrack> {
        val cleanedTitle = title
            .replace(Regex("\\[.*?]|\\(.*?\\)"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleanedTitle.length < 2) return emptyList()

        val isSeriesHint = season > 1 || episode > 1
        val directStreams = movieBoxFallbackClient.fetchStreams(
            title = cleanedTitle,
            season = season.coerceAtLeast(1),
            episode = episode.coerceAtLeast(1),
            isSeries = isSeriesHint
        ).getOrNull().orEmpty()
        val directSubs = directStreams.flatMap { it.subtitles }.distinctBy { it.url }
        if (directSubs.isNotEmpty()) return directSubs

        val searchResults = movieBoxFallbackClient.searchMedia(cleanedTitle).getOrNull().orEmpty()
        if (searchResults.isEmpty()) return emptyList()

        val bestMatch = searchResults.firstOrNull { item ->
            item.title.equals(cleanedTitle, ignoreCase = true) ||
                item.title.contains(cleanedTitle, ignoreCase = true) ||
                cleanedTitle.contains(item.title, ignoreCase = true)
        } ?: searchResults.first()

        val streams = movieBoxFallbackClient.fetchStreams(
            title = bestMatch.title,
            season = season.coerceAtLeast(1),
            episode = episode.coerceAtLeast(1),
            isSeries = isSeriesHint || bestMatch.episodes.isNotEmpty()
        ).getOrNull().orEmpty()

        return streams.flatMap { it.subtitles }.distinctBy { it.url }
    }

    private fun pickBestSourceTrackForTranslation(tracks: List<SubtitleTrack>): SubtitleTrack? {
        if (tracks.isEmpty()) return null
        return tracks.firstOrNull {
            val l = it.language.lowercase()
            val n = it.label.lowercase()
            l == "en" || l == "eng" || l.startsWith("en-") || n.contains("english")
        } ?: tracks.firstOrNull {
            val l = it.language.lowercase()
            val n = it.label.lowercase()
            l == "hi" || l == "hin" || n.contains("hindi")
        } ?: tracks.first()
    }

    private fun downloadSubtitleText(url: String, headers: Map<String, String>): String {
        val reqBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", CHROME_UA)
            .header("Accept", "*/*")
        headers.forEach { (k, v) ->
            if (!k.equals("Accept-Encoding", ignoreCase = true)) {
                reqBuilder.header(k, v)
            }
        }
        httpClient.newCall(reqBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("Subtitle HTTP ${resp.code}")
            }
            return resp.body?.string().orEmpty()
        }
    }

    /**
     * Parses both SubRip (.srt) and WebVTT (.vtt) timestamped captions cleanly.
     */
    fun parseCues(rawText: String): List<SubtitleCue> {
        val normalized = rawText
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        val blocks = normalized.split(Regex("\n\\s*\n+"))
        val cues = ArrayList<SubtitleCue>(blocks.size)
        val timeRegex = Regex(
            """(\d{1,2}:\d{2}:\d{2}[.,]\d{1,3}|\d{2}:\d{2}[.,]\d{1,3})\s*-->\s*(\d{1,2}:\d{2}:\d{2}[.,]\d{1,3}|\d{2}:\d{2}[.,]\d{1,3})"""
        )

        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            val firstLine = lines.first()
            if (firstLine.startsWith("WEBVTT", ignoreCase = true) ||
                firstLine.startsWith("NOTE", ignoreCase = true) ||
                firstLine.startsWith("STYLE", ignoreCase = true)
            ) {
                continue
            }

            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex < 0) continue

            val match = timeRegex.find(lines[timeLineIndex]) ?: continue
            val start = normalizeVttTimestamp(match.groupValues[1])
            val end = normalizeVttTimestamp(match.groupValues[2])

            val captionText = lines
                .drop(timeLineIndex + 1)
                .joinToString("\n")
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("\\{\\\\[^}]+\\}"), "")
                .trim()

            if (captionText.isNotEmpty()) {
                cues += SubtitleCue(startTime = start, endTime = end, text = captionText)
            }
        }
        return cues
    }

    private fun normalizeVttTimestamp(raw: String): String {
        val dot = raw.replace(',', '.')
        val parts = dot.split(':')
        val withHours = if (parts.size == 2) "00:$dot" else dot
        val mainAndMs = withHours.split('.')
        val hhmmss = mainAndMs[0].split(':').joinToString(":") { it.padStart(2, '0') }
        val ms = mainAndMs.getOrElse(1) { "000" }.padEnd(3, '0').take(3)
        return "$hhmmss.$ms"
    }

    private suspend fun translateCuesToBengali(
        cues: List<SubtitleCue>,
        onProgress: (Float) -> Unit
    ): List<SubtitleCue> = coroutineScope {
        // Pack cues into chunks of ~1400 chars or max 24 cues so each request is super fast and reliable
        val chunks = mutableListOf<List<Pair<Int, SubtitleCue>>>()
        var current = mutableListOf<Pair<Int, SubtitleCue>>()
        var charCount = 0

        cues.forEachIndexed { idx, cue ->
            val len = cue.text.length + 14
            if (current.isNotEmpty() && (charCount + len > 1400 || current.size >= 24)) {
                chunks += current
                current = mutableListOf()
                charCount = 0
            }
            current += idx to cue
            charCount += len
        }
        if (current.isNotEmpty()) chunks += current

        val results = arrayOfNulls<SubtitleCue>(cues.size)
        val semaphore = Semaphore(2)
        var completedChunks = 0
        val totalChunks = chunks.size.coerceAtLeast(1)

        chunks.map { chunk ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    coroutineContext.ensureActive()
                    val translatedBatch = translateBatch(chunk.map { it.second.text })
                    chunk.forEachIndexed { localIdx, (globalIdx, origCue) ->
                        val bnText = translatedBatch.getOrNull(localIdx)?.takeIf { it.isNotBlank() }
                            ?: origCue.text
                        results[globalIdx] = origCue.copy(text = bnText)
                    }
                    synchronized(this@BengaliSubtitleService) {
                        completedChunks++
                        onProgress(completedChunks.toFloat() / totalChunks.toFloat())
                    }
                }
            }
        }.awaitAll()

        cues.indices.map { idx -> results[idx] ?: cues[idx] }
    }

    private fun translateBatch(lines: List<String>): List<String> {
        if (lines.isEmpty()) return emptyList()
        val markerPrefix = "<<<MUVIO_"
        val markerSuffix = ">>>"
        val joined = lines.mapIndexed { idx, text ->
            val singleLine = text.replace("\n", " | ")
            "$markerPrefix$idx$markerSuffix $singleLine"
        }.joinToString("\n")

        val translatedRaw = requestBengaliTranslation(joined) ?: return lines

        val extracted = MutableList(lines.size) { idx -> lines[idx] }
        val markerRegex = Regex("""<<<\s*MUVIO[_\s]*(\d+)\s*>>>""")
        val matches = markerRegex.findAll(translatedRaw).toList()

        if (matches.isEmpty()) {
            // Fallback: split line-by-line if translator stripped custom markers
            val splitLines = translatedRaw.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (splitLines.size == lines.size) {
                return splitLines.map { it.replace(" | ", "\n") }
            }
            return lines
        }

        for (i in matches.indices) {
            val match = matches[i]
            val cueIndex = match.groupValues[1].toIntOrNull() ?: continue
            if (cueIndex !in extracted.indices) continue
            val start = match.range.last + 1
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else translatedRaw.length
            val piece = translatedRaw.substring(start, end)
                .trim()
                .replace(" | ", "\n")
                .replace("| ", "\n")
                .trim()
            if (piece.isNotEmpty()) {
                extracted[cueIndex] = piece
            }
        }
        return extracted
    }

    private fun requestBengaliTranslation(text: String): String? {
        repeat(3) { attempt ->
            val result = runCatching {
                val formBody = FormBody.Builder()
                    .add("client", "gtx")
                    .add("sl", "auto")
                    .add("tl", "bn")
                    .add("dt", "t")
                    .add("q", text)
                    .build()

                val request = Request.Builder()
                    .url("https://translate.googleapis.com/translate_a/single")
                    .header("User-Agent", CHROME_UA)
                    .post(formBody)
                    .build()

                httpClient.newCall(request).execute().use { resp ->
                    if (resp.code == 429 || resp.code in 500..599) {
                        return@runCatching "__RETRY__"
                    }
                    if (!resp.isSuccessful) return@runCatching null
                    val body = resp.body?.string().orEmpty()
                    if (body.isBlank()) return@runCatching null
                    val root = JSONArray(body)
                    val sentences = root.optJSONArray(0) ?: return@runCatching null
                    buildString {
                        for (i in 0 until sentences.length()) {
                            val seg = sentences.optJSONArray(i) ?: continue
                            val part = seg.optString(0)
                            if (part.isNotEmpty() && part != "null") {
                                append(part)
                            }
                        }
                    }
                }
            }.getOrNull()

            if (result == "__RETRY__") {
                if (attempt < 2) {
                    runCatching { Thread.sleep(600L * (attempt + 1)) }
                }
            } else if (result != null) {
                return result
            } else if (attempt < 2) {
                runCatching { Thread.sleep(400L * (attempt + 1)) }
            }
        }
        return null
    }

    fun buildWebVtt(cues: List<SubtitleCue>): String {
        return buildString(cues.size * 64) {
            append("WEBVTT\n\n")
            cues.forEachIndexed { idx, cue ->
                append(idx + 1)
                append('\n')
                append(cue.startTime)
                append(" --> ")
                append(cue.endTime)
                append('\n')
                append(cue.text)
                append("\n\n")
            }
        }
    }

    private fun pruneOldCacheFiles(dir: File, maxFiles: Int) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (files.size > maxFiles) {
            files.drop(maxFiles).forEach { runCatching { it.delete() } }
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
