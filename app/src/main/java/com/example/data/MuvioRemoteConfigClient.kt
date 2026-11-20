package com.example.data

import com.example.BuildConfig
import com.example.model.RemoteAppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MuvioRemoteConfigClient(
    private val configUrlProvider: () -> String = { BuildConfig.REMOTE_CONFIG_URL.trim() }
) {
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun isConfigured(): Boolean {
        val url = configUrlProvider().trim()
        return url.startsWith("https://", ignoreCase = true) &&
            !url.contains("YOUR_REMOTE_CONFIG_JSON_URL", ignoreCase = true)
    }

    suspend fun fetchRemoteConfig(): Result<RemoteAppConfig> = withContext(Dispatchers.IO) {
        val url = configUrlProvider().trim()
        if (!isConfigured()) {
            return@withContext Result.failure(IllegalStateException("Remote config URL is not configured"))
        }
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Remote config HTTP ${response.code}")
                }
                val raw = response.body?.string().orEmpty().trim()
                if (!raw.startsWith("{")) {
                    throw IOException("Invalid remote config JSON")
                }
                parseConfigJson(JSONObject(raw))
            }
        }
    }

    internal fun parseConfigJson(obj: JSONObject): RemoteAppConfig {
        val defaultRegions = RemoteAppConfig().priorityRegions
        val parsedRegions = parseStringArray(obj.optJSONArray("priorityRegions"))
        return RemoteAppConfig(
            latestVersionCode = obj.optInt("latestVersionCode", BuildConfig.VERSION_CODE).coerceAtLeast(1),
            latestVersionName = obj.optString("latestVersionName", BuildConfig.VERSION_NAME).trim()
                .ifBlank { BuildConfig.VERSION_NAME },
            forceUpdate = obj.optBoolean("forceUpdate", false),
            updateTitle = obj.optString("updateTitle", "").trim(),
            updateNotes = obj.optString("updateNotes", "").trim(),
            updateUrl = sanitizeHttpsUrl(obj.optString("updateUrl", "")),
            announcementTag = obj.optString("announcementTag", "LIVE UPDATE").trim().ifBlank { "LIVE UPDATE" },
            announcementText = obj.optString("announcementText", "").trim(),
            announcementActionUrl = sanitizeHttpsUrl(obj.optString("announcementActionUrl", "")),
            movieBoxBaseUrl = sanitizeHttpsUrl(obj.optString("movieBoxBaseUrl", "")),
            secondaryCatalogBaseUrl = sanitizeHttpsUrl(obj.optString("secondaryCatalogBaseUrl", "")),
            featuredBannerTitles = parseStringArray(obj.optJSONArray("featuredBannerTitles")),
            priorityRegions = parsedRegions.ifEmpty { defaultRegions }
        )
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val value = arr.optString(i).trim()
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun sanitizeHttpsUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("https://", ignoreCase = true)) trimmed else ""
    }
}
