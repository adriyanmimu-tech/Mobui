package com.example.data

import com.example.BuildConfig
import com.example.model.ApiCategory
import com.example.model.ApiQuotaInfo
import com.example.model.CommentItem
import com.example.model.EpisodeItem
import com.example.model.FilterOptions
import com.example.model.LiveMatchItem
import com.example.model.MediaItem
import com.example.model.ShortReelItem
import com.example.model.StreamResource
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class ApiKeyMissingException : IllegalStateException(
    "MOVIEBOX_API_KEY is not configured in AI Studio Secrets."
)

data class HomeFeedResult(
    val heroItems: List<MediaItem>,
    val allItems: List<MediaItem>,
    val sections: Map<String, List<MediaItem>>,
    val sports: List<LiveMatchItem> = emptyList()
)

data class MediaDetailResult(
    val updatedItem: MediaItem?,
    val recommendations: List<MediaItem>,
    val seasonsCount: Int,
    val seasonEpisodes: Map<Int, List<EpisodeItem>> = emptyMap()
)

class MovieBoxApiClient(
    private val apiKeyProvider: () -> String = { BuildConfig.MOVIEBOX_API_KEY.trim() }
) {

    private val defaultBaseUrl: String = BuildConfig.MOVIEBOX_BASE_URL.trimEnd('/')

    @Volatile
    var runtimeBaseUrlOverride: String = ""

    @Volatile
    var runtimeApiKeyOverride: String = ""

    private val baseUrl: String
        get() = runtimeBaseUrlOverride.trim().trimEnd('/').ifBlank { defaultBaseUrl }

    private fun effectiveApiKey(): String =
        runtimeApiKeyOverride.trim().ifBlank { apiKeyProvider().trim() }

    fun isApiKeyConfigured(): Boolean {
        val key = effectiveApiKey()
        return key.isNotEmpty() && key != "YOUR_MOVIEBOX_API_KEY"
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val apiKey = effectiveApiKey()
        val newUrl = original.url.newBuilder()
            .addQueryParameter("apiKey", apiKey)
            .build()
        val newRequest = original.newBuilder()
            .url(newUrl)
            .header("X-API-Key", apiKey)
            .header("Accept", "application/json")
            .build()
        chain.proceed(newRequest)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(45, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .build()
    }

    // 1. GET /home
    suspend fun fetchHomeFeed(): Result<HomeFeedResult> = withContext(Dispatchers.IO) {
        executeGetJson("$baseUrl/home").mapCatching { root -> parseHomeJson(root) }
    }

    // 2. GET /categories
    suspend fun fetchCategories(): Result<List<ApiCategory>> = withContext(Dispatchers.IO) {
        executeGetJson("$baseUrl/categories").mapCatching { root ->
            MovieBoxParsers.parseCategories(MovieBoxParsers.unwrapPayload(root))
        }
    }

    // 3. GET /search?q=...
    suspend fun searchMedia(query: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        executeGetJson("$baseUrl/search?q=$encoded").mapCatching { root -> parseSearchJson(root) }
    }

    // 4. GET /suggest?q=...
    suspend fun fetchSuggestions(query: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        executeGetJson("$baseUrl/suggest?q=$encoded").mapCatching { root ->
            MovieBoxParsers.parseStringList(
                MovieBoxParsers.unwrapPayload(root),
                listOf("suggestions", "items", "list", "keywords", "results", "data")
            )
        }
    }

    // 5. GET /hot-search
    suspend fun fetchHotSearch(): Result<List<String>> = withContext(Dispatchers.IO) {
        executeGetJson("$baseUrl/hot-search").mapCatching { root ->
            MovieBoxParsers.parseStringList(
                MovieBoxParsers.unwrapPayload(root),
                listOf("hotSearch", "hot", "keywords", "items", "list", "trending", "data")
            )
        }
    }

    // 6. GET /catalog?path=...&page=...
    suspend fun fetchCatalog(path: String, page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        executeGetJson("$baseUrl/catalog?path=$encodedPath&page=$page").mapCatching { root ->
            MovieBoxParsers.extractItemList(MovieBoxParsers.unwrapPayload(root), path)
        }
    }

    // 7. GET /shorts?page=...
    suspend fun fetchShorts(page: Int = 1): Result<List<ShortReelItem>> = withContext(Dispatchers.IO) {
        executeGetJson("$baseUrl/shorts?page=$page").mapCatching { root ->
            MovieBoxParsers.parseShortsPayload(MovieBoxParsers.unwrapPayload(root))
        }
    }

    // 8. GET /sports
    suspend fun fetchSports(): Result<List<LiveMatchItem>> = withContext(Dispatchers.IO) {
        executeGetJson("$baseUrl/sports").mapCatching { root ->
            parseSportsJson(root)
        }
    }

    // 9. GET /platform?name=...&page=...
    suspend fun fetchPlatform(name: String, page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        val encodedName = URLEncoder.encode(name, "UTF-8")
        executeGetJson("$baseUrl/platform?name=$encodedName&page=$page").mapCatching { root ->
            MovieBoxParsers.extractItemList(MovieBoxParsers.unwrapPayload(root), "platform_$name")
        }
    }

    // 10. GET /upcoming?page=...
    suspend fun fetchUpcoming(page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        executeGetJson("$baseUrl/upcoming?page=$page").mapCatching { root ->
            MovieBoxParsers.extractItemList(MovieBoxParsers.unwrapPayload(root), "upcoming")
        }
    }

    // 11. GET /filter-options
    suspend fun fetchFilterOptions(): Result<FilterOptions> = withContext(Dispatchers.IO) {
        executeGetJson("$baseUrl/filter-options").mapCatching { root ->
            MovieBoxParsers.parseFilterOptions(MovieBoxParsers.unwrapPayload(root))
        }
    }

    // 12. GET /details?title=...
    suspend fun fetchDetails(title: String, mediaId: String? = null): Result<MediaDetailResult> = withContext(Dispatchers.IO) {
        val cleanTitle = cleanQueryTitle(title)
        val baseTitle = extractBaseTitle(title)
        val encodedClean = URLEncoder.encode(cleanTitle, "UTF-8")
        val idParam = if (!mediaId.isNullOrBlank() && !mediaId.startsWith("seed_")) {
            "&subjectId=${URLEncoder.encode(mediaId, "UTF-8")}"
        } else {
            ""
        }

        val firstResult = executeGetJson("$baseUrl/details?title=$encodedClean$idParam")
            .mapCatching { root -> parseDetailsJson(root, title) }

        val firstDetail = firstResult.getOrNull()
        val needsRetryWithBase = baseTitle.isNotBlank() &&
            !baseTitle.equals(cleanTitle, ignoreCase = true) &&
            (firstDetail == null || (firstDetail.seasonEpisodes.isEmpty() && firstDetail.updatedItem?.episodes.isNullOrEmpty()))

        if (needsRetryWithBase) {
            val encodedBase = URLEncoder.encode(baseTitle, "UTF-8")
            val retryResult = executeGetJson("$baseUrl/details?title=$encodedBase")
                .mapCatching { root -> parseDetailsJson(root, title) }
            val retryDetail = retryResult.getOrNull()
            if (retryDetail != null) {
                if (firstDetail == null || retryDetail.seasonEpisodes.size >= firstDetail.seasonEpisodes.size) {
                    return@withContext Result.success(retryDetail)
                }
            }
        }
        firstResult
    }

    // 13. GET /related?title=...
    suspend fun fetchRelated(title: String): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        val cleanTitle = cleanQueryTitle(title)
        val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
        executeGetJson("$baseUrl/related?title=$encodedTitle").mapCatching { root ->
            MovieBoxParsers.extractItemList(MovieBoxParsers.unwrapPayload(root), "related")
        }
    }

    // 14. GET /comments?subjectId=...&page=...
    suspend fun fetchComments(subjectId: String, page: Int = 1): Result<List<CommentItem>> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(subjectId, "UTF-8")
        executeGetJson("$baseUrl/comments?subjectId=$encodedId&page=$page").mapCatching { root ->
            MovieBoxParsers.parseCommentsPayload(MovieBoxParsers.unwrapPayload(root))
        }
    }

    // 15. GET /staff?staffId=...&page=...
    suspend fun fetchStaffMovies(staffId: String, page: Int = 1): Result<List<MediaItem>> = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(staffId, "UTF-8")
        executeGetJson("$baseUrl/staff?staffId=$encodedId&page=$page").mapCatching { root ->
            MovieBoxParsers.extractItemList(MovieBoxParsers.unwrapPayload(root), "staff_$staffId")
        }
    }

    // 16. GET /stream?title=...&se=...&ep=...
    suspend fun fetchStreams(
        title: String,
        season: Int = 1,
        episode: Int = 1,
        isSeries: Boolean = false
    ): Result<List<StreamResource>> = withContext(Dispatchers.IO) {
        val cleanTitle = cleanQueryTitle(title)
        val baseTitle = extractBaseTitle(title)
        val se = if (isSeries) season.coerceAtLeast(1) else 1
        val ep = if (isSeries) episode.coerceAtLeast(1) else 1
        val encodedClean = URLEncoder.encode(cleanTitle, "UTF-8")
        val primaryUrl = "$baseUrl/stream?title=$encodedClean&se=$se&ep=$ep"
        val primaryResult = executeGetJson(primaryUrl).mapCatching { root -> parseStreamsJson(root, cleanTitle) }
        val primaryList = primaryResult.getOrNull().orEmpty()
        if (primaryList.isNotEmpty() || baseTitle.equals(cleanTitle, ignoreCase = true) || baseTitle.isBlank()) {
            return@withContext primaryResult
        }
        val encodedBase = URLEncoder.encode(baseTitle, "UTF-8")
        val fallbackUrl = "$baseUrl/stream?title=$encodedBase&se=$se&ep=$ep"
        executeGetJson(fallbackUrl).mapCatching { root -> parseStreamsJson(root, baseTitle) }
    }

    // 17. GET /me
    suspend fun fetchAccountQuota(): Result<ApiQuotaInfo> = withContext(Dispatchers.IO) {
        executeGetJson("$baseUrl/me").mapCatching { root ->
            MovieBoxParsers.parseQuotaInfo(MovieBoxParsers.unwrapPayload(root))
        }
    }

    // Internal test hooks
    internal fun parseHomeJson(root: JSONObject): HomeFeedResult =
        MovieBoxParsers.parseHomePayload(MovieBoxParsers.unwrapPayload(root))

    internal fun parseSearchJson(root: JSONObject): List<MediaItem> =
        MovieBoxParsers.extractItemList(MovieBoxParsers.unwrapPayload(root), "search")

    internal fun parseDetailsJson(root: JSONObject, fallbackTitle: String): MediaDetailResult =
        MovieBoxParsers.parseDetailsPayload(root, MovieBoxParsers.unwrapPayload(root), fallbackTitle)

    internal fun parseStreamsJson(root: JSONObject, title: String): List<StreamResource> {
        val parsed = MovieBoxParsers.parseStreamsPayload(MovieBoxParsers.unwrapPayload(root), title)
        val key = effectiveApiKey()
        val origin = baseUrl.substringBefore("/api").trimEnd('/')
        if (key.isEmpty() || origin.isBlank()) return parsed
        return parsed.map { res ->
            if (res.streamUrl.startsWith(origin, ignoreCase = true) &&
                res.headers.keys.none { it.equals("X-API-Key", ignoreCase = true) }
            ) {
                res.copy(headers = res.headers + ("X-API-Key" to key))
            } else {
                res
            }
        }
    }

    internal fun parseSportsJson(root: JSONObject): List<LiveMatchItem> =
        MovieBoxParsers.parseSportsPayload(MovieBoxParsers.unwrapPayload(root))

    private fun cleanQueryTitle(raw: String): String =
        raw.replace(Regex("\\s*\\[[^]]*]"), "").trim().ifBlank { raw.trim() }

    internal fun extractBaseTitle(raw: String): String =
        raw.replace(Regex("\\s*\\[[^]]*]"), "")
            .replace(Regex("(?i)\\s*\\bS0?\\d{1,2}\\s*[-–to]+\\s*S?0?\\d{1,2}\\b"), "")
            .replace(Regex("(?i)\\s*\\bSeason\\s*\\d{1,2}(\\s*[-–to]+\\s*\\d{1,2})?\\b"), "")
            .replace(Regex("(?i)\\s*\\bS0?\\d{1,2}\\b"), "")
            .replace(Regex("\\s*\\((19|20)\\d{2}\\)"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
            .ifBlank { cleanQueryTitle(raw) }

    private fun executeGetJson(url: String): Result<JSONObject> {
        if (!isApiKeyConfigured()) {
            return Result.failure(ApiKeyMissingException())
        }
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                val request = Request.Builder().url(url).get().build()
                httpClient.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val isRetryable = response.code in listOf(502, 503, 504)
                        lastError = IOException("HTTP ${response.code}: ${bodyStr.take(180)}")
                        if (isRetryable && attempt == 0) {
                            Thread.sleep(700L)
                            return@repeat
                        }
                        return Result.failure(lastError!!)
                    }
                    val trimmed = bodyStr.trim()
                    val json = when {
                        trimmed.startsWith("{") -> JSONObject(trimmed)
                        trimmed.startsWith("[") -> JSONObject().put("items", JSONArray(trimmed))
                        else -> return Result.failure(IOException("Unexpected response format"))
                    }
                    return Result.success(json)
                }
            } catch (e: java.net.SocketTimeoutException) {
                lastError = e
                if (attempt == 0) {
                    Thread.sleep(500L)
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.failure(lastError ?: IOException("Request timed out"))
    }
}
