package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.model.ContentSource
import com.example.model.EpisodeItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PosterArtwork
import com.example.model.StaffMember
import com.example.model.StreamResource
import com.example.model.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

data class SecondaryDetailResult(
    val updatedItem: MediaItem,
    val seasonsCount: Int,
    val seasonEpisodes: Map<Int, List<EpisodeItem>>,
    val initialStreams: List<StreamResource>
)

data class SecondarySuggestResult(
    val suggestions: List<String>,
    val items: List<MediaItem>
)

class SecondaryCatalogApiClient(
    private val baseUrl: String = BuildConfig.SECONDARY_CATALOG_BASE_URL.trimEnd('/'),
    private val apiKeyProvider: () -> String = { BuildConfig.SECONDARY_CATALOG_API_KEY.trim() }
) {
    private val originUrl: String = run {
        val parsed = baseUrl.toHttpUrlOrNull()
        if (parsed != null) {
            val isDefaultPort = (parsed.scheme == "https" && parsed.port == 443) ||
                (parsed.scheme == "http" && parsed.port == 80)
            if (isDefaultPort) {
                "${parsed.scheme}://${parsed.host}"
            } else {
                "${parsed.scheme}://${parsed.host}:${parsed.port}"
            }
        } else {
            baseUrl.substringBefore("/api").trimEnd('/')
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val apiKey = apiKeyProvider().trim()
        val newUrl = if (apiKey.isNotEmpty() && original.url.queryParameter("apiKey").isNullOrBlank()) {
            original.url.newBuilder()
                .addQueryParameter("apiKey", apiKey)
                .build()
        } else {
            original.url
        }
        val builder = original.newBuilder()
            .url(newUrl)
            .header("Accept", "application/json, text/plain, */*")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
        if (apiKey.isNotEmpty()) {
            builder.header("X-API-Key", apiKey)
        }
        chain.proceed(builder.build())
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(35, TimeUnit.SECONDS)
        .readTimeout(55, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(authInterceptor)
        .build()

    fun isApiKeyConfigured(): Boolean {
        val key = apiKeyProvider().trim()
        return key.isNotEmpty() && key != "YOUR_CINEPULSE_API_KEY"
    }

    fun isConfigured(): Boolean = baseUrl.startsWith("http") && isApiKeyConfigured()

    // 1. GET /home (Trending, New Uploads & Banners)
    suspend fun fetchHome(): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            requireApiKey()
            val url = buildUrl("/home", emptyMap())
            val root = executeGetJsonWithRetry(url)
            parseHomeJson(root)
        }
    }

    // 2. GET /catalog?path=/bollywood&page=1 (or /anime, /drama, /southIndian, /language/bangla, /ongoing, /adult18plus)
    suspend fun fetchCatalog(path: String = "/", page: Int = 1): Result<List<MediaItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireApiKey()
                val url = buildUrl("/catalog", mapOf("path" to path, "page" to page.toString()))
                val root = executeGetJsonWithRetry(url)
                parseCatalogJson(root, pathHint = path)
            }
        }

    // 3. GET /search?q=Pushpa&page=1
    suspend fun searchMedia(query: String, page: Int = 1): Result<List<MediaItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireApiKey()
                val trimmed = query.trim()
                if (trimmed.isBlank()) return@runCatching emptyList()
                val url = buildUrl("/search", mapOf("q" to trimmed, "page" to page.toString()))
                val root = executeGetJsonWithRetry(url)
                parseCatalogJson(root, pathHint = "")
            }
        }

    // 4. GET /suggest?q=Pushpa
    suspend fun fetchSuggestions(query: String): Result<SecondarySuggestResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireApiKey()
                val trimmed = query.trim()
                if (trimmed.isBlank()) return@runCatching SecondarySuggestResult(emptyList(), emptyList())
                val url = buildUrl("/suggest", mapOf("q" to trimmed))
                val root = executeGetJsonWithRetry(url)
                parseSuggestJson(root)
            }
        }

    // 5. GET /hot-search
    suspend fun fetchHotSearch(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            requireApiKey()
            val url = buildUrl("/hot-search", emptyMap())
            val root = executeGetJsonWithRetry(url)
            parseHotSearchJson(root)
        }
    }

    // 6. GET /upcoming?page=1
    suspend fun fetchUpcoming(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            requireApiKey()
            val url = buildUrl("/upcoming", mapOf("page" to page.toString()))
            val root = executeGetJsonWithRetry(url)
            parseCatalogJson(root, pathHint = "/upcoming")
        }
    }

    // 7. GET /details?path=/movie/... (or ?title=Pushpa)
    suspend fun fetchDetails(detailPath: String, fallbackItem: MediaItem): Result<SecondaryDetailResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                requireApiKey()
                val cleanPath = detailPath.ifBlank { fallbackItem.sourceDetailPath }.trim()
                val primaryParams = if (cleanPath.startsWith("/")) {
                    mapOf("path" to cleanPath)
                } else {
                    mapOf("title" to fallbackItem.title)
                }
                val firstResult = runCatching {
                    val root = executeGetJsonWithRetry(buildUrl("/details", primaryParams))
                    parseDetailsJson(root, fallbackItem)
                }
                if (firstResult.isSuccess) {
                    val detail = firstResult.getOrThrow()
                    val hasRichData = detail.initialStreams.isNotEmpty() ||
                        detail.seasonEpisodes.values.any { it.isNotEmpty() } ||
                        detail.updatedItem.synopsis.isNotBlank()
                    if (hasRichData || !primaryParams.containsKey("path")) {
                        return@runCatching detail
                    }
                }
                // Fallback to ?title=... if ?path=... returned incomplete/empty data
                val titleRoot = executeGetJsonWithRetry(buildUrl("/details", mapOf("title" to fallbackItem.title)))
                parseDetailsJson(titleRoot, fallbackItem)
            }
        }

    // 8. GET /stream?path=/series/...&se=1&ep=1 (or ?title=...&ep=1 — Direct MP4 + Adaptive 1080p/720p HLS)
    suspend fun fetchStreams(
        detailPath: String,
        title: String,
        season: Int = 1,
        episode: Int = 1,
        isSeries: Boolean = false,
        fallbackLanguage: String = "Original"
    ): Result<List<StreamResource>> = withContext(Dispatchers.IO) {
        runCatching {
            requireApiKey()
            val cleanPath = detailPath.trim()
            val params = linkedMapOf<String, String>()
            if (cleanPath.startsWith("/")) {
                params["path"] = cleanPath
            } else {
                params["title"] = title
            }
            if (isSeries || season > 1 || episode > 1) {
                params["se"] = season.coerceAtLeast(1).toString()
                params["ep"] = episode.coerceAtLeast(1).toString()
            } else {
                params["ep"] = "1"
            }

            val firstStreams = runCatching {
                val root = executeGetJsonWithRetry(buildUrl("/stream", params))
                parseStreamsJson(root, title = title, fallbackLanguage = fallbackLanguage)
            }.getOrDefault(emptyList())

            if (firstStreams.isNotEmpty() || !params.containsKey("path")) {
                return@runCatching firstStreams
            }

            // Fallback to ?title=...&se=...&ep=... if path lookup returned no streams
            val fallbackParams = linkedMapOf("title" to title)
            if (isSeries || season > 1 || episode > 1) {
                fallbackParams["se"] = season.coerceAtLeast(1).toString()
                fallbackParams["ep"] = episode.coerceAtLeast(1).toString()
            } else {
                fallbackParams["ep"] = "1"
            }
            val fallbackRoot = executeGetJsonWithRetry(buildUrl("/stream", fallbackParams))
            parseStreamsJson(fallbackRoot, title = title, fallbackLanguage = fallbackLanguage)
        }
    }

    // 9. GET /related?title=Pushpa
    suspend fun fetchRelated(title: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        runCatching {
            requireApiKey()
            val trimmed = title.trim()
            if (trimmed.isBlank()) return@runCatching emptyList()
            val url = buildUrl("/related", mapOf("title" to trimmed))
            val root = executeGetJsonWithRetry(url)
            parseCatalogJson(root, pathHint = "")
        }
    }

    private fun requireApiKey() {
        if (!isApiKeyConfigured()) {
            throw IOException("Add SECONDARY_CATALOG_API_KEY in AI Studio Secrets panel to enable Source 2.")
        }
    }

    internal fun parseHomeJson(root: JSONObject): List<MediaItem> {
        val containers = listOfNotNull(root.optJSONObject("data"), root)
        val collected = mutableListOf<MediaItem>()

        val arrayKeys = listOf(
            "banners", "banner", "hero", "featured",
            "trending", "newUploads", "new_uploads", "latest",
            "items", "results", "movies", "series", "anime", "drama", "list", "catalog"
        )

        for (container in containers) {
            for (key in arrayKeys) {
                val arr = container.optJSONArray(key) ?: continue
                val hint = when {
                    key.contains("anime", true) -> "/anime"
                    key.contains("drama", true) || key.contains("series", true) -> "/drama"
                    else -> ""
                }
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    parseCatalogItem(obj, pathHint = hint)?.let { collected += it }
                }
            }

            // Also inspect sections array or map if provided
            val sectionsArr = container.optJSONArray("sections")
            if (sectionsArr != null) {
                for (i in 0 until sectionsArr.length()) {
                    val secObj = sectionsArr.optJSONObject(i) ?: continue
                    val secTitle = secObj.cleanString("title").ifBlank { secObj.cleanString("name") }
                    val secPath = secObj.cleanString("path").ifBlank { secTitle }
                    val list = secObj.optJSONArray("items")
                        ?: secObj.optJSONArray("movies")
                        ?: secObj.optJSONArray("results")
                        ?: secObj.optJSONArray("list")
                        ?: continue
                    for (j in 0 until list.length()) {
                        val obj = list.optJSONObject(j) ?: continue
                        parseCatalogItem(obj, pathHint = secPath)?.let { collected += it }
                    }
                }
            }

            val sectionsObj = container.optJSONObject("sections")
            if (sectionsObj != null) {
                val keys = sectionsObj.keys()
                while (keys.hasNext()) {
                    val secKey = keys.next()
                    val list = sectionsObj.optJSONArray(secKey) ?: continue
                    for (j in 0 until list.length()) {
                        val obj = list.optJSONObject(j) ?: continue
                        parseCatalogItem(obj, pathHint = secKey)?.let { collected += it }
                    }
                }
            }
        }

        return collected.distinctBy { it.id }
    }

    internal fun parseHotSearchJson(root: JSONObject): List<String> {
        val dataObj = root.optJSONObject("data")
        val arr = root.optJSONArray("hotSearches")
            ?: root.optJSONArray("hot")
            ?: root.optJSONArray("trending")
            ?: root.optJSONArray("queries")
            ?: root.optJSONArray("items")
            ?: root.optJSONArray("results")
            ?: dataObj?.optJSONArray("hotSearches")
            ?: dataObj?.optJSONArray("hot")
            ?: dataObj?.optJSONArray("trending")
            ?: dataObj?.optJSONArray("queries")
            ?: dataObj?.optJSONArray("items")
            ?: return emptyList()

        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            when (val entry = arr.opt(i)) {
                is String -> entry.trim().takeIf { it.isNotBlank() }?.let { out += it }
                is JSONObject -> {
                    val label = entry.cleanString("title")
                        .ifBlank { entry.cleanString("query") }
                        .ifBlank { entry.cleanString("keyword") }
                        .ifBlank { entry.cleanString("name") }
                    if (label.isNotBlank()) out += label
                }
                else -> Unit
            }
        }
        return out.distinct().take(12)
    }

    internal fun parseSuggestJson(root: JSONObject): SecondarySuggestResult {
        val dataObj = root.optJSONObject("data")
        val arr = root.optJSONArray("suggestions")
            ?: root.optJSONArray("items")
            ?: root.optJSONArray("results")
            ?: dataObj?.optJSONArray("suggestions")
            ?: dataObj?.optJSONArray("items")
            ?: dataObj?.optJSONArray("results")
            ?: return SecondarySuggestResult(emptyList(), emptyList())

        val labels = mutableListOf<String>()
        val items = mutableListOf<MediaItem>()
        for (i in 0 until arr.length()) {
            when (val entry = arr.opt(i)) {
                is String -> {
                    val s = entry.trim()
                    if (s.isNotBlank()) labels += s
                }
                is JSONObject -> {
                    val title = entry.cleanString("title").ifBlank { entry.cleanString("name") }
                    if (title.isNotBlank()) labels += title
                    parseCatalogItem(entry, pathHint = "")?.let { items += it }
                }
                else -> Unit
            }
        }
        return SecondarySuggestResult(
            suggestions = labels.distinct().take(8),
            items = items.distinctBy { it.id }
        )
    }

    internal fun parseCatalogJson(root: JSONObject, pathHint: String = ""): List<MediaItem> {
        val dataObj = root.optJSONObject("data")
        val itemsArr = root.optJSONArray("items")
            ?: dataObj?.optJSONArray("items")
            ?: root.optJSONArray("results")
            ?: dataObj?.optJSONArray("results")
            ?: root.optJSONArray("movies")
            ?: dataObj?.optJSONArray("movies")
            ?: root.optJSONArray("related")
            ?: dataObj?.optJSONArray("related")
            ?: root.optJSONArray("upcoming")
            ?: dataObj?.optJSONArray("upcoming")
            ?: root.optJSONArray("list")
            ?: dataObj?.optJSONArray("list")
            ?: root.optJSONArray("catalog")
            ?: return emptyList()

        val result = mutableListOf<MediaItem>()
        for (i in 0 until itemsArr.length()) {
            val obj = itemsArr.optJSONObject(i) ?: continue
            parseCatalogItem(obj, pathHint = pathHint)?.let { result += it }
        }
        return result.distinctBy { it.id }
    }

    internal fun parseStreamsJson(
        root: JSONObject,
        title: String,
        fallbackLanguage: String = "Original"
    ): List<StreamResource> {
        val containers = listOfNotNull(
            root.optJSONObject("data"),
            root.optJSONObject("stream"),
            root.optJSONObject("result"),
            root.optJSONObject("detail"),
            root
        )

        val subs = containers.flatMap { parseSubtitles(it) }.distinctBy { it.url }
        val collected = mutableListOf<StreamResource>()

        for (container in containers) {
            val arrayKeys = listOf("sources", "streams", "processedSources", "links", "qualities", "urls")
            for (key in arrayKeys) {
                val arr = container.optJSONArray(key) ?: continue
                collected += parseSourcesArray(
                    arr = arr,
                    title = title,
                    fallbackLanguage = fallbackLanguage,
                    subtitles = subs,
                    fallbackDirectUrl = ""
                )
            }

            // Direct named MP4 / HLS fields on /stream response
            val directFields = listOf(
                "mp4" to "mp4",
                "mp4Url" to "mp4",
                "directMp4" to "mp4",
                "directUrl" to "mp4",
                "hls" to "hls",
                "hlsUrl" to "hls",
                "m3u8" to "hls",
                "m3u8Url" to "hls",
                "masterUrl" to "hls",
                "streamUrl" to "",
                "url" to ""
            )
            directFields.forEachIndexed { idx, (fieldKey, typeHint) ->
                val raw = container.cleanString(fieldKey)
                val resolved = resolveUrl(raw)
                if (resolved.startsWith("http")) {
                    val quality = normalizeQuality("", typeHint, resolved, idx)
                    collected += StreamResource(
                        id = "s2_stream_${fieldKey}_$idx",
                        label = "$title [$quality]",
                        language = fallbackLanguage,
                        quality = quality,
                        uploader = "CinePulse HD",
                        streamUrl = resolved,
                        subtitles = subs,
                        headers = buildStreamHeaders(resolved, emptyMap())
                    )
                }
            }
        }

        return collected
            .distinctBy { it.streamUrl }
            .sortedByDescending { qualityRank(it.quality) }
    }

    internal fun parseDetailsJson(root: JSONObject, fallbackItem: MediaItem): SecondaryDetailResult {
        val detailObj = root.optJSONObject("detail")
            ?: root.optJSONObject("data")?.optJSONObject("detail")
            ?: root.optJSONObject("data")
            ?: root

        val title = detailObj.cleanString("title").ifBlank { fallbackItem.title }
        val synopsis = detailObj.cleanString("storyline")
            .ifBlank { detailObj.cleanString("synopsis") }
            .ifBlank { detailObj.cleanString("description") }
            .ifBlank { detailObj.cleanString("overview") }
            .ifBlank { detailObj.cleanString("plot") }
            .ifBlank { fallbackItem.synopsis }

        val year = extractYear(
            detailObj.cleanString("year").ifBlank { detailObj.cleanString("releaseDate") }.ifBlank { title },
            fallbackItem.year
        )
        val rating = detailObj.optDouble("rating", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
            ?: detailObj.cleanString("rating").toDoubleOrNull()?.takeIf { it > 0.0 }
            ?: fallbackItem.rating

        val genre = parseStringOrArray(detailObj, listOf("genre", "genres", "category", "categories"))
            .ifBlank { fallbackItem.genre }
        val country = detailObj.cleanString("country")
            .ifBlank { detailObj.cleanString("region") }
            .ifBlank { fallbackItem.country }

        val posterRaw = detailObj.cleanString("poster").ifBlank { detailObj.cleanString("image") }
        val posterUrl = resolveUrl(posterRaw).ifBlank { fallbackItem.poster.remoteUrl.orEmpty() }.takeIf { it.isNotBlank() }

        val screenshotsArr = detailObj.optJSONArray("screenshots")
        val firstScreenshot = screenshotsArr?.optString(0)?.let { resolveUrl(it) }.orEmpty()
        val backdropRaw = detailObj.cleanString("backdrop")
            .ifBlank { detailObj.cleanString("banner") }
            .ifBlank { firstScreenshot }
        val backdropUrl = resolveUrl(backdropRaw).ifBlank { posterUrl ?: fallbackItem.backdropUrl.orEmpty() }.takeIf { it.isNotBlank() }

        val castList = parseCast(detailObj)

        val rawEpisodesArr = detailObj.optJSONArray("episodes")
            ?: detailObj.optJSONArray("episodeList")
            ?: root.optJSONArray("episodes")

        val groupedBySeason = linkedMapOf<Int, MutableList<EpisodeItem>>()
        val topLevelSubtitles = parseSubtitles(detailObj)
        var firstEpisodeStreams: List<StreamResource> = emptyList()

        if (rawEpisodesArr != null && rawEpisodesArr.length() > 0) {
            for (i in 0 until rawEpisodesArr.length()) {
                val epObj = rawEpisodesArr.optJSONObject(i) ?: continue
                val seasonNum = readPositiveInt(epObj, listOf("season", "se", "seasonNumber", "season_number")).coerceAtLeast(1)
                val currentSeasonList = groupedBySeason.getOrPut(seasonNum) { mutableListOf() }
                val epNum = readPositiveInt(epObj, listOf("number", "ep", "episode", "episodeNumber", "index"))
                    .let { if (it > 0) it else currentSeasonList.size + 1 }
                val epTitle = epObj.cleanString("title")
                    .ifBlank { epObj.cleanString("name") }
                    .ifBlank { "Episode $epNum" }
                val epDuration = epObj.cleanString("duration")
                    .ifBlank { epObj.cleanString("runtime") }
                    .ifBlank { "HD" }

                val epSubs = (parseSubtitles(epObj) + topLevelSubtitles).distinctBy { it.url }
                val epSources = parseSourcesArray(
                    epObj.optJSONArray("sources") ?: epObj.optJSONArray("streams") ?: epObj.optJSONArray("links"),
                    title = title,
                    fallbackLanguage = fallbackItem.languageBadge ?: "Original",
                    subtitles = epSubs,
                    fallbackDirectUrl = epObj.cleanString("url").ifBlank { epObj.cleanString("streamUrl") }
                )
                if (firstEpisodeStreams.isEmpty() && epSources.isNotEmpty()) {
                    firstEpisodeStreams = epSources
                }
                currentSeasonList += EpisodeItem(
                    number = epNum,
                    title = epTitle,
                    duration = epDuration,
                    streamUrl = epSources.firstOrNull()?.streamUrl.orEmpty(),
                    sources = epSources
                )
            }
        }

        if (firstEpisodeStreams.isEmpty()) {
            firstEpisodeStreams = parseStreamsJson(root, title = title, fallbackLanguage = fallbackItem.languageBadge ?: "Original")
        }

        val resolvedPath = detailObj.cleanString("path").ifBlank { fallbackItem.sourceDetailPath }
        val totalEpisodesCount = groupedBySeason.values.sumOf { it.size }
        val inferredIsSeries = groupedBySeason.size > 1 ||
            totalEpisodesCount > 1 ||
            resolvedPath.contains("/series", ignoreCase = true) ||
            fallbackItem.mediaType == MediaType.SERIES ||
            fallbackItem.mediaType == MediaType.ANIME ||
            fallbackItem.mediaType == MediaType.DRAMA ||
            title.contains("season", ignoreCase = true) ||
            Regex("(?i)\\bS0?\\d{1,2}\\b").containsMatchIn(title)

        val resolvedMediaType = when {
            fallbackItem.mediaType == MediaType.ANIME ||
                genre.contains("anime", ignoreCase = true) ||
                genre.contains("animation", ignoreCase = true) ||
                title.contains("anime", ignoreCase = true) -> MediaType.ANIME
            inferredIsSeries -> MediaType.SERIES
            else -> MediaType.MOVIE
        }

        val finalSeasonMap: Map<Int, List<EpisodeItem>> = if (inferredIsSeries) {
            groupedBySeason.toSortedMap().mapValues { it.value.toList() }
        } else {
            emptyMap()
        }
        val firstSeasonEpisodes = if (inferredIsSeries) {
            finalSeasonMap[1] ?: finalSeasonMap.values.firstOrNull().orEmpty()
        } else {
            emptyList()
        }
        val seasonsCount = if (inferredIsSeries) {
            maxOf(1, finalSeasonMap.size, finalSeasonMap.keys.maxOrNull() ?: 1)
        } else {
            1
        }

        val updatedItem = fallbackItem.copy(
            title = title,
            year = year,
            country = country,
            genre = genre,
            rating = (Math.round(rating * 10.0) / 10.0),
            mediaType = resolvedMediaType,
            synopsis = synopsis,
            duration = detailObj.cleanString("duration").ifBlank {
                if (inferredIsSeries) "${firstSeasonEpisodes.size} Episodes" else fallbackItem.duration
            },
            poster = fallbackItem.poster.copy(remoteUrl = posterUrl ?: fallbackItem.poster.remoteUrl),
            backdropUrl = backdropUrl ?: fallbackItem.backdropUrl,
            episodes = firstSeasonEpisodes,
            seasonEpisodes = finalSeasonMap,
            resources = firstEpisodeStreams.ifEmpty { fallbackItem.resources },
            cast = castList.ifEmpty { fallbackItem.cast },
            contentSource = ContentSource.SECONDARY,
            sourceDetailPath = resolvedPath
        )

        return SecondaryDetailResult(
            updatedItem = updatedItem,
            seasonsCount = seasonsCount,
            seasonEpisodes = finalSeasonMap,
            initialStreams = firstEpisodeStreams
        )
    }

    private fun parseCast(detailObj: JSONObject): List<StaffMember> {
        val castArr = detailObj.optJSONArray("cast")
            ?: detailObj.optJSONArray("stars")
            ?: detailObj.optJSONArray("actors")
        val out = mutableListOf<StaffMember>()
        if (castArr != null) {
            for (i in 0 until castArr.length()) {
                when (val entry = castArr.opt(i)) {
                    is String -> {
                        val name = entry.trim()
                        if (name.isNotBlank() && !name.equals("null", true)) {
                            out += StaffMember(id = "s2_cast_$i", name = name, role = "Cast")
                        }
                    }
                    is JSONObject -> {
                        val name = entry.cleanString("name").ifBlank { entry.cleanString("title") }
                        if (name.isNotBlank()) {
                            val role = entry.cleanString("role").ifBlank { entry.cleanString("character") }.ifBlank { "Cast" }
                            val avatar = resolveUrl(entry.cleanString("avatar").ifBlank { entry.cleanString("image") }).takeIf { it.isNotBlank() }
                            out += StaffMember(id = "s2_cast_$i", name = name, role = role, avatarUrl = avatar)
                        }
                    }
                    else -> Unit
                }
            }
        } else {
            val rawCastStr = detailObj.cleanString("cast")
            if (rawCastStr.isNotBlank()) {
                rawCastStr.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEachIndexed { idx, name ->
                    out += StaffMember(id = "s2_cast_$idx", name = name, role = "Cast")
                }
            }
        }
        return out.distinctBy { it.name }.take(14)
    }

    private fun parseCatalogItem(obj: JSONObject, pathHint: String): MediaItem? {
        val rawPath = obj.cleanString("path")
            .ifBlank { obj.cleanString("href") }
            .ifBlank { obj.cleanString("link") }
            .ifBlank { obj.cleanString("url") }
            .ifBlank { obj.cleanString("slug") }
            .ifBlank { obj.cleanString("id") }
        val title = obj.cleanString("title")
            .ifBlank { obj.cleanString("name") }
        if (title.isBlank()) return null
        val effectivePath = rawPath.ifBlank { title }

        val posterRaw = obj.cleanString("poster")
            .ifBlank { obj.cleanString("image") }
            .ifBlank { obj.cleanString("img") }
            .ifBlank { obj.cleanString("cover") }
            .ifBlank { obj.cleanString("thumb") }
            .ifBlank { obj.cleanString("thumbnail") }
        val posterUrl = resolveUrl(posterRaw).takeIf { it.isNotBlank() }

        val backdropRaw = obj.cleanString("backdrop")
            .ifBlank { obj.cleanString("banner") }
            .ifBlank { posterRaw }
        val backdropUrl = resolveUrl(backdropRaw).takeIf { it.isNotBlank() } ?: posterUrl

        val rawType = obj.cleanString("type").ifBlank { obj.cleanString("category") }.lowercase()
        val lowerTitle = title.lowercase()
        val lowerPath = effectivePath.lowercase()
        val lowerHint = pathHint.lowercase()

        val mediaType = when {
            lowerHint.contains("anime") || rawType.contains("anime") || lowerPath.contains("/anime") || lowerTitle.contains("anime") -> MediaType.ANIME
            lowerHint.contains("drama") || lowerHint.contains("ongoing") ||
                rawType.contains("series") || rawType.contains("tv") || rawType.contains("drama") ||
                lowerPath.contains("/tv") || lowerPath.contains("/series") || lowerPath.contains("/season") ||
                Regex("(?i)\\b(season\\s*\\d+|s0?\\d{1,2})\\b").containsMatchIn(title) -> MediaType.SERIES
            else -> MediaType.MOVIE
        }

        val langBadge = extractLanguageBadge(obj, title) ?: when {
            lowerHint.contains("bangla") -> "Bengali"
            lowerHint.contains("bollywood") || lowerHint.contains("southindian") -> "Hindi"
            else -> null
        }
        val year = extractYear(obj.cleanString("year").ifBlank { obj.cleanString("releaseDate") }.ifBlank { title }, 2025)
        val rating = obj.optDouble("rating", Double.NaN).takeIf { !it.isNaN() && it > 0.0 }
            ?: obj.cleanString("rating").toDoubleOrNull()?.takeIf { it > 0.0 }
            ?: 8.1
        val genre = parseStringOrArray(obj, listOf("genre", "genres", "category"))
            .ifBlank {
                when (mediaType) {
                    MediaType.ANIME -> "Anime"
                    MediaType.SERIES -> "Drama"
                    else -> "Action, Drama"
                }
            }
        val country = obj.cleanString("country").ifBlank {
            when {
                lowerHint.contains("bangla") || langBadge.equals("Bengali", ignoreCase = true) -> "Bangladesh"
                lowerHint.contains("bollywood") || lowerHint.contains("southindian") || langBadge.equals("Hindi", ignoreCase = true) -> "India"
                langBadge.equals("Korean", ignoreCase = true) -> "South Korea"
                mediaType == MediaType.ANIME -> "Japan"
                else -> "International"
            }
        }

        val fallbackRes = when (mediaType) {
            MediaType.ANIME -> R.drawable.img_poster_anime
            MediaType.SERIES, MediaType.DRAMA, MediaType.SHORT_TV -> R.drawable.img_poster_drama
            MediaType.MOVIE -> R.drawable.img_poster_action
        }

        val tags = mutableListOf("secondary_catalog", "most_trending")
        if (mediaType == MediaType.ANIME) tags += "anime"
        if (lowerHint.contains("bangla") || langBadge.equals("Bengali", ignoreCase = true)) tags += "bengali"
        if (lowerHint.contains("bollywood")) tags += "bollywood"
        if (lowerHint.contains("southindian")) tags += "south_indian"
        if (mediaType == MediaType.SERIES || mediaType == MediaType.DRAMA) tags += "trending_series"
        if (mediaType == MediaType.MOVIE) tags += "trending_movies"

        val namespacedId = buildSecondaryId(effectivePath)

        return MediaItem(
            id = namespacedId,
            title = title,
            year = year,
            country = country,
            genre = genre,
            rating = (Math.round(rating * 10.0) / 10.0),
            languageBadge = langBadge,
            mediaType = mediaType,
            subCategory = when {
                mediaType == MediaType.ANIME -> "Anime"
                langBadge.equals("Bengali", ignoreCase = true) -> "New Bengali"
                mediaType == MediaType.SERIES -> "Top Series"
                else -> "Cinema"
            },
            sectionTags = tags.distinct(),
            synopsis = obj.cleanString("storyline").ifBlank { obj.cleanString("description") }.ifBlank { obj.cleanString("synopsis") },
            duration = obj.cleanString("quality").ifBlank { if (mediaType == MediaType.SERIES) "Series" else "HD Stream" },
            poster = PosterArtwork(
                remoteUrl = posterUrl,
                fallbackRes = fallbackRes,
                tintHex = 0xFF101920,
                accentHex = 0xFFD4AF37,
                badgeTitle = title
            ),
            backdropRes = R.drawable.img_hero_cinema,
            backdropUrl = backdropUrl,
            resources = listOf(
                StreamResource(
                    id = "${namespacedId}_default",
                    label = "$title [CinePulse HD]",
                    language = langBadge ?: "Original",
                    quality = "HD",
                    uploader = "CinePulse HD"
                )
            ),
            contentSource = ContentSource.SECONDARY,
            sourceDetailPath = if (rawPath.startsWith("/")) rawPath else ""
        )
    }

    private fun parseSourcesArray(
        arr: JSONArray?,
        title: String,
        fallbackLanguage: String,
        subtitles: List<SubtitleTrack>,
        fallbackDirectUrl: String
    ): List<StreamResource> {
        val out = mutableListOf<StreamResource>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val entry = arr.opt(i)
                when (entry) {
                    is JSONObject -> {
                        val rawUrl = entry.cleanString("url")
                            .ifBlank { entry.cleanString("streamUrl") }
                            .ifBlank { entry.cleanString("file") }
                            .ifBlank { entry.cleanString("src") }
                        val resolved = resolveUrl(rawUrl)
                        if (!resolved.startsWith("http")) continue
                        val streamType = entry.cleanString("type").lowercase()
                        val qualityRaw = entry.cleanString("quality")
                            .ifBlank { entry.cleanString("resolution") }
                            .ifBlank { entry.cleanString("label") }
                            .ifBlank { entry.cleanString("name") }
                        val quality = normalizeQuality(qualityRaw, streamType, resolved, i)
                        val language = entry.cleanString("language")
                            .ifBlank { entry.cleanString("lang") }
                            .ifBlank { fallbackLanguage }
                        val headers = buildStreamHeaders(resolved, extractHeaders(entry))
                        val sourceSubs = (parseSubtitles(entry) + subtitles).distinctBy { it.url }
                        out += StreamResource(
                            id = "s2_src_$i",
                            label = "$title [$quality]",
                            language = language,
                            quality = quality,
                            uploader = "CinePulse HD",
                            streamUrl = resolved,
                            subtitles = sourceSubs,
                            headers = headers
                        )
                    }
                    is String -> {
                        val resolved = resolveUrl(entry.trim())
                        if (resolved.startsWith("http")) {
                            val quality = normalizeQuality("", "", resolved, i)
                            out += StreamResource(
                                id = "s2_src_$i",
                                label = "$title [$quality]",
                                language = fallbackLanguage,
                                quality = quality,
                                uploader = "CinePulse HD",
                                streamUrl = resolved,
                                subtitles = subtitles,
                                headers = buildStreamHeaders(resolved, emptyMap())
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
        if (out.isEmpty() && fallbackDirectUrl.isNotBlank()) {
            val resolved = resolveUrl(fallbackDirectUrl)
            if (resolved.startsWith("http")) {
                val quality = normalizeQuality("", "", resolved, 0)
                out += StreamResource(
                    id = "s2_src_direct",
                    label = "$title [$quality]",
                    language = fallbackLanguage,
                    quality = quality,
                    uploader = "CinePulse HD",
                    streamUrl = resolved,
                    subtitles = subtitles,
                    headers = buildStreamHeaders(resolved, emptyMap())
                )
            }
        }
        return out
            .distinctBy { it.streamUrl }
            .sortedByDescending { qualityRank(it.quality) }
    }

    private fun buildStreamHeaders(resolvedUrl: String, existing: Map<String, String>): Map<String, String> {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isEmpty() || !resolvedUrl.startsWith(originUrl, ignoreCase = true)) {
            return existing
        }
        if (existing.keys.any { it.equals("X-API-Key", ignoreCase = true) }) {
            return existing
        }
        return existing + ("X-API-Key" to apiKey)
    }

    private fun parseSubtitles(obj: JSONObject): List<SubtitleTrack> {
        val keys = listOf("subtitles", "captions", "tracks")
        val subs = mutableListOf<SubtitleTrack>()
        for (key in keys) {
            val arr = obj.optJSONArray(key) ?: continue
            for (i in 0 until arr.length()) {
                val subObj = arr.optJSONObject(i) ?: continue
                val url = resolveUrl(subObj.cleanString("url").ifBlank { subObj.cleanString("file") })
                if (!url.startsWith("http")) continue
                val lang = subObj.cleanString("lang").ifBlank { subObj.cleanString("lan") }.ifBlank { "en" }
                val label = subObj.cleanString("label").ifBlank { subObj.cleanString("lanName") }.ifBlank { lang.uppercase() }
                subs += SubtitleTrack(id = "s2_sub_$i", language = lang, label = label, url = url)
            }
        }
        return subs.distinctBy { it.url }
    }

    private fun extractHeaders(obj: JSONObject): Map<String, String> {
        val hObj = obj.optJSONObject("headers") ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        val keys = hObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = hObj.cleanString(k)
            if (v.isNotBlank()) map[k] = v
        }
        return map
    }

    internal fun resolveUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return ""
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> "$originUrl$trimmed"
            else -> "$originUrl/$trimmed"
        }
    }

    private fun JSONObject.cleanString(key: String): String {
        if (!has(key) || isNull(key)) return ""
        val raw = optString(key, "").trim()
        return if (raw.equals("null", ignoreCase = true)) "" else raw
    }

    private fun buildSecondaryId(path: String): String {
        val cleaned = path.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(48)
        val suffix = if (cleaned.isNotBlank()) cleaned else path.hashCode().toUInt().toString(16)
        return "s2_$suffix"
    }

    private fun normalizeQuality(raw: String, streamType: String, url: String, index: Int): String {
        val combined = "$raw $url".lowercase()
        val isHls = streamType == "hls" || combined.contains(".m3u8") || combined.contains("/api/hls/")
        val isMp4 = streamType == "mp4" || combined.contains(".mp4")
        return when {
            combined.contains("2160") || combined.contains("4k") -> "4K"
            combined.contains("1080") -> if (isHls) "1080p HLS" else "1080p"
            combined.contains("720") -> if (isHls) "720p HLS" else "720p"
            combined.contains("480") -> "480p"
            combined.contains("360") -> "360p"
            raw.isNotBlank() -> raw.take(18)
            isMp4 -> "Direct MP4"
            isHls -> "Adaptive HLS"
            else -> if (index == 0) "HD" else "Server ${index + 1}"
        }
    }

    private fun qualityRank(quality: String): Int = when {
        quality.contains("4K", true) || quality.contains("2160") -> 2160
        quality.contains("1080") -> 1080
        quality.contains("MP4", true) -> 950
        quality.contains("HLS", true) -> 900
        quality.contains("720") -> 720
        quality.equals("HD", true) -> 700
        quality.contains("480") -> 480
        quality.contains("360") -> 360
        else -> 500
    }

    private fun extractLanguageBadge(obj: JSONObject, title: String): String? {
        val raw = obj.cleanString("language").ifBlank { obj.cleanString("audio") }.ifBlank { obj.cleanString("badge") }
        val combined = "$raw $title".lowercase()
        return when {
            combined.contains("bangla") || combined.contains("bengali") -> "Bengali"
            combined.contains("dual audio") -> "Dual Audio"
            combined.contains("hindi") -> "Hindi"
            combined.contains("korean") -> "Korean"
            combined.contains("japanese") -> "Japanese"
            combined.contains("english") -> "English"
            raw.isNotBlank() -> raw.take(14)
            else -> null
        }
    }

    private fun extractYear(raw: String, fallback: Int): Int {
        val match = Regex("\\b(19\\d{2}|20\\d{2})\\b").find(raw)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: fallback
    }

    private fun parseStringOrArray(obj: JSONObject, keys: List<String>): String {
        for (key in keys) {
            val arr = obj.optJSONArray(key)
            if (arr != null && arr.length() > 0) {
                val joined = (0 until arr.length())
                    .mapNotNull { arr.optString(it).trim().takeIf { s -> s.isNotBlank() && !s.equals("null", true) } }
                    .take(3)
                    .joinToString(", ")
                if (joined.isNotBlank()) return joined
            }
            val str = obj.cleanString(key)
            if (str.isNotBlank()) return str
        }
        return ""
    }

    private fun readPositiveInt(obj: JSONObject, keys: List<String>): Int {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)
            val parsed = when (value) {
                is Number -> value.toInt()
                is String -> value.trim().toIntOrNull() ?: 0
                else -> 0
            }
            if (parsed > 0) return parsed
        }
        return 0
    }

    private fun buildUrl(endpointPath: String, params: Map<String, String>): String {
        val normalizedPath = if (endpointPath.startsWith("/")) endpointPath else "/$endpointPath"
        val parsed = "$baseUrl$normalizedPath".toHttpUrlOrNull()
            ?: throw IOException("Invalid Secondary Catalog URL: $baseUrl$normalizedPath")
        val builder = parsed.newBuilder()
        params.forEach { (k, v) ->
            if (k == "path" && v == "/") {
                builder.addEncodedQueryParameter("path", "/")
            } else {
                builder.addQueryParameter(k, v)
            }
        }
        return builder.build().toString()
    }

    private fun executeGetJsonWithRetry(url: String): JSONObject {
        return try {
            executeGetJson(url)
        } catch (_: SocketTimeoutException) {
            executeGetJson(url)
        }
    }

    private fun executeGetJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val trimmed = body.trim()

            if (response.code == 401 || response.code == 403) {
                val msg = "Source 2 API key invalid or missing (HTTP ${response.code}). Set SECONDARY_CATALOG_API_KEY in Secrets."
                Log.w("MuvioSource2", "$msg -> $url")
                throw IOException(msg)
            }
            if (response.code == 404) {
                val msg = "Source 2 endpoint not found (HTTP 404). Please verify your CinePulse API server is awake."
                Log.w("MuvioSource2", "$msg -> $url")
                throw IOException(msg)
            }
            if (!response.isSuccessful) {
                val detail = if (trimmed.startsWith("<")) "Server waking up or busy" else trimmed.take(90)
                val msg = "Source 2 HTTP ${response.code}: $detail"
                Log.w("MuvioSource2", "$msg -> $url")
                throw IOException(msg)
            }
            if (trimmed.isEmpty()) return JSONObject()
            if (trimmed.startsWith("<")) {
                val msg = "Source 2 returned HTML instead of JSON (server may be restarting)."
                Log.w("MuvioSource2", "$msg -> $url")
                throw IOException(msg)
            }
            return if (trimmed.startsWith("[")) {
                JSONObject().put("items", JSONArray(trimmed))
            } else {
                JSONObject(trimmed)
            }
        }
    }
}
