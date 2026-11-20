package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.R
import com.example.model.CommentItem
import com.example.model.ContentSource
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PosterArtwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.muvioSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "muvio_user_settings"
)

data class LocalHistoryRecord(
    val media: MediaItem,
    val progress: Float,
    val lastSeason: Int,
    val lastEpisode: Int,
    val positionMs: Long = 0L
)

data class MuvioSettingsSnapshot(
    val autoPlayNext: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val preferredQuality: String = "1080p HD",
    val subtitleSize: String = "Medium",
    val isAdultVerified: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val watchlist: List<MediaItem> = emptyList(),
    val history: List<LocalHistoryRecord> = emptyList(),
    val commentsByMedia: Map<String, List<CommentItem>> = emptyMap(),
    val cachedMovieBoxBaseUrl: String = "",
    val cachedSecondaryBaseUrl: String = ""
)

class MuvioPreferencesStore(private val context: Context) {

    private object Keys {
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
        val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        val SUBTITLE_SIZE = stringPreferencesKey("subtitle_size")
        val ADULT_VERIFIED = booleanPreferencesKey("adult_verified")
        val RECENT_SEARCHES_JSON = stringPreferencesKey("recent_searches_json")
        val WATCHLIST_JSON = stringPreferencesKey("local_watchlist_json")
        val HISTORY_JSON = stringPreferencesKey("local_history_json")
        val COMMENTS_JSON = stringPreferencesKey("local_comments_json")
        val REMOTE_MOVIEBOX_URL = stringPreferencesKey("remote_moviebox_base_url")
        val REMOTE_SECONDARY_URL = stringPreferencesKey("remote_secondary_base_url")
    }

    val settingsFlow: Flow<MuvioSettingsSnapshot> = context.muvioSettingsDataStore.data
        .catch { err ->
            if (err is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw err
        }
        .map { prefs ->
            MuvioSettingsSnapshot(
                autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
                wifiOnlyDownloads = prefs[Keys.WIFI_ONLY_DOWNLOADS] ?: false,
                preferredQuality = prefs[Keys.PREFERRED_QUALITY] ?: "1080p HD",
                subtitleSize = prefs[Keys.SUBTITLE_SIZE] ?: "Medium",
                isAdultVerified = prefs[Keys.ADULT_VERIFIED] ?: false,
                recentSearches = decodeStringList(prefs[Keys.RECENT_SEARCHES_JSON]),
                watchlist = decodeWatchlist(prefs[Keys.WATCHLIST_JSON]),
                history = decodeHistory(prefs[Keys.HISTORY_JSON]),
                commentsByMedia = decodeComments(prefs[Keys.COMMENTS_JSON]),
                cachedMovieBoxBaseUrl = prefs[Keys.REMOTE_MOVIEBOX_URL].orEmpty(),
                cachedSecondaryBaseUrl = prefs[Keys.REMOTE_SECONDARY_URL].orEmpty()
            )
        }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.muvioSettingsDataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        context.muvioSettingsDataStore.edit { it[Keys.WIFI_ONLY_DOWNLOADS] = enabled }
    }

    suspend fun setPreferredQuality(quality: String) {
        context.muvioSettingsDataStore.edit { it[Keys.PREFERRED_QUALITY] = quality }
    }

    suspend fun setSubtitleSize(size: String) {
        context.muvioSettingsDataStore.edit { it[Keys.SUBTITLE_SIZE] = size }
    }

    suspend fun setAdultVerified(verified: Boolean) {
        context.muvioSettingsDataStore.edit { it[Keys.ADULT_VERIFIED] = verified }
    }

    suspend fun saveRemoteBaseUrls(movieBoxUrl: String, secondaryUrl: String) {
        context.muvioSettingsDataStore.edit { prefs ->
            if (movieBoxUrl.startsWith("https://", ignoreCase = true)) {
                prefs[Keys.REMOTE_MOVIEBOX_URL] = movieBoxUrl.trim().trimEnd('/')
            }
            if (secondaryUrl.startsWith("https://", ignoreCase = true)) {
                prefs[Keys.REMOTE_SECONDARY_URL] = secondaryUrl.trim().trimEnd('/')
            }
        }
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        context.muvioSettingsDataStore.edit { prefs ->
            val current = decodeStringList(prefs[Keys.RECENT_SEARCHES_JSON])
            val updated = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(10)
            prefs[Keys.RECENT_SEARCHES_JSON] = encodeStringList(updated)
        }
    }

    suspend fun clearRecentSearches() {
        context.muvioSettingsDataStore.edit { it.remove(Keys.RECENT_SEARCHES_JSON) }
    }

    suspend fun upsertWatchlist(media: MediaItem) {
        context.muvioSettingsDataStore.edit { prefs ->
            val current = decodeWatchlist(prefs[Keys.WATCHLIST_JSON])
            val updated = (listOf(media) + current.filterNot { it.id == media.id }).take(100)
            prefs[Keys.WATCHLIST_JSON] = encodeWatchlist(updated)
        }
    }

    suspend fun removeWatchlist(mediaId: String) {
        context.muvioSettingsDataStore.edit { prefs ->
            val current = decodeWatchlist(prefs[Keys.WATCHLIST_JSON])
            val updated = current.filterNot { it.id == mediaId }
            prefs[Keys.WATCHLIST_JSON] = encodeWatchlist(updated)
        }
    }

    suspend fun upsertHistory(
        media: MediaItem,
        progress: Float,
        season: Int,
        episode: Int,
        positionMs: Long = 0L
    ) {
        context.muvioSettingsDataStore.edit { prefs ->
            val current = decodeHistory(prefs[Keys.HISTORY_JSON])
            val record = LocalHistoryRecord(
                media = media,
                progress = progress.coerceIn(0f, 1f),
                lastSeason = season.coerceAtLeast(1),
                lastEpisode = episode.coerceAtLeast(1),
                positionMs = positionMs.coerceAtLeast(0L)
            )
            val updated = (listOf(record) + current.filterNot { it.media.id == media.id }).take(30)
            prefs[Keys.HISTORY_JSON] = encodeHistory(updated)
        }
    }

    suspend fun clearHistory() {
        context.muvioSettingsDataStore.edit { it.remove(Keys.HISTORY_JSON) }
    }

    suspend fun addComment(mediaId: String, comment: CommentItem) {
        context.muvioSettingsDataStore.edit { prefs ->
            val current = decodeComments(prefs[Keys.COMMENTS_JSON]).toMutableMap()
            val list = current[mediaId].orEmpty()
            current[mediaId] = (listOf(comment) + list.filterNot { it.id == comment.id }).take(40)
            prefs[Keys.COMMENTS_JSON] = encodeComments(current)
        }
    }

    suspend fun updateCommentLike(mediaId: String, commentId: String, likes: Int, isLiked: Boolean) {
        context.muvioSettingsDataStore.edit { prefs ->
            val current = decodeComments(prefs[Keys.COMMENTS_JSON]).toMutableMap()
            val list = current[mediaId] ?: return@edit
            val updated = list.map { c ->
                if (c.id == commentId) c.copy(likes = likes, isLiked = isLiked) else c
            }
            current[mediaId] = updated
            prefs[Keys.COMMENTS_JSON] = encodeComments(current)
        }
    }

    private fun encodeStringList(items: List<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i).trim()
                    if (v.isNotEmpty()) add(v)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun mediaToJson(m: MediaItem): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("title", m.title)
        put("year", m.year)
        put("genre", m.genre)
        put("rating", m.rating)
        put("mediaType", m.mediaType.name)
        put("contentSource", m.contentSource.name)
        put("posterUrl", m.poster.remoteUrl.orEmpty())
        put("backdropUrl", m.backdropUrl.orEmpty())
        put("sourceDetailPath", m.sourceDetailPath)
    }

    private fun jsonToMedia(obj: JSONObject): MediaItem? {
        val id = obj.optString("id").trim()
        val title = obj.optString("title").trim()
        if (id.isBlank() || title.isBlank()) return null
        val typeName = obj.optString("mediaType")
        val resolvedType = enumValues<MediaType>().firstOrNull { it.name == typeName } ?: MediaType.MOVIE
        val sourceName = obj.optString("contentSource")
        val resolvedSource = enumValues<ContentSource>().firstOrNull { it.name == sourceName } ?: ContentSource.PRIMARY
        val posterUrl = obj.optString("posterUrl").ifBlank { null }
        val backdropUrl = obj.optString("backdropUrl").ifBlank { posterUrl }
        val genre = obj.optString("genre").ifBlank { "Drama" }
        return MediaItem(
            id = id,
            title = title,
            year = obj.optInt("year", 2025),
            country = "Global",
            genre = genre,
            rating = obj.optDouble("rating", 8.0),
            languageBadge = null,
            mediaType = resolvedType,
            subCategory = genre,
            sectionTags = emptyList(),
            synopsis = "",
            duration = if (resolvedType == MediaType.MOVIE) "2h 00m" else "Series",
            poster = PosterArtwork(
                remoteUrl = posterUrl,
                fallbackRes = R.drawable.img_poster_drama,
                tintHex = 0xFF1A0E12,
                accentHex = 0xFFE51A38,
                badgeTitle = title
            ),
            backdropRes = R.drawable.img_hero_cinema,
            backdropUrl = backdropUrl,
            contentSource = resolvedSource,
            sourceDetailPath = obj.optString("sourceDetailPath")
        )
    }

    private fun encodeWatchlist(items: List<MediaItem>): String {
        val arr = JSONArray()
        items.forEach { arr.put(mediaToJson(it)) }
        return arr.toString()
    }

    private fun decodeWatchlist(raw: String?): List<MediaItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { jsonToMedia(it) }?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeHistory(items: List<LocalHistoryRecord>): String {
        val arr = JSONArray()
        items.forEach { rec ->
            arr.put(
                mediaToJson(rec.media).apply {
                    put("progress", rec.progress.toDouble())
                    put("lastSeason", rec.lastSeason)
                    put("lastEpisode", rec.lastEpisode)
                    put("positionMs", rec.positionMs)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeHistory(raw: String?): List<LocalHistoryRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val media = jsonToMedia(obj) ?: continue
                    // Legacy entries saved before positionMs tracking lack "positionMs" and had a
                    // fake 0.05f/0.06f floor — default positionMs to 0L so they start cleanly at 00:00.
                    val savedPosMs = obj.optLong("positionMs", 0L).coerceAtLeast(0L)
                    val rawProgress = if (obj.has("positionMs")) {
                        obj.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    add(
                        LocalHistoryRecord(
                            media = media,
                            progress = rawProgress,
                            lastSeason = obj.optInt("lastSeason", 1).coerceAtLeast(1),
                            lastEpisode = obj.optInt("lastEpisode", 1).coerceAtLeast(1),
                            positionMs = savedPosMs
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeComments(map: Map<String, List<CommentItem>>): String {
        val root = JSONObject()
        map.forEach { (mediaId, list) ->
            val arr = JSONArray()
            list.forEach { c ->
                arr.put(
                    JSONObject().apply {
                        put("id", c.id)
                        put("author", c.author)
                        put("timestamp", c.timestamp)
                        put("content", c.content)
                        put("likes", c.likes)
                        put("isLiked", c.isLiked)
                    }
                )
            }
            root.put(mediaId, arr)
        }
        return root.toString()
    }

    private fun decodeComments(raw: String?): Map<String, List<CommentItem>> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val result = linkedMapOf<String, List<CommentItem>>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val mediaId = keys.next()
                val arr = root.optJSONArray(mediaId) ?: continue
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val author = obj.optString("author").ifBlank { "You" }
                        add(
                            CommentItem(
                                id = obj.optString("id"),
                                author = author,
                                avatarInitials = author.take(1).uppercase(),
                                avatarColor = 0xFFE51A38,
                                flagEmoji = "\uD83C\uDDE7\uD83C\uDDE9",
                                timestamp = obj.optString("timestamp").ifBlank { "Saved" },
                                content = obj.optString("content"),
                                likes = obj.optInt("likes", 0),
                                repliesCount = 0,
                                isLiked = obj.optBoolean("isLiked", false)
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) result[mediaId] = list
            }
            result
        }.getOrDefault(emptyMap())
    }
}
