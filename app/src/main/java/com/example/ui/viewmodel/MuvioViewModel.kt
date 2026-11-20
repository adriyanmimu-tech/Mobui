package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ApiKeyMissingException
import com.example.data.MovieBoxApiClient
import com.example.data.MovieBoxParsers
import com.example.data.MuvioCatalogRepository
import com.example.data.MuvioDownloadEngine
import com.example.data.MuvioPreferencesStore
import com.example.data.MuvioRemoteConfigClient
import com.example.data.MuvioSeedRepository
import com.example.data.SecondaryCatalogApiClient
import com.example.model.ApiCategory
import com.example.model.ApiQuotaInfo
import com.example.model.CategoryBannerItem
import com.example.model.CommentItem
import com.example.model.ContentSource
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.DubVersion
import com.example.model.EpisodeItem
import com.example.model.FilterOptions
import com.example.model.LiveMatchItem
import com.example.model.MainTab
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.NovelItem
import com.example.model.PlatformBrand
import com.example.model.RemoteAppConfig
import com.example.model.ShortReelItem
import com.example.model.StaffMember
import com.example.model.StreamResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BrowseSource {
    data class LocalSection(val sectionTitle: String) : BrowseSource
    data class CatalogPath(val path: String) : BrowseSource
    data class Platform(val name: String) : BrowseSource
    data object Upcoming : BrowseSource
}

data class MuvioUiState(
    val selectedTab: MainTab = MainTab.HOME,
    val topTabs: List<String> = emptyList(),
    val selectedTopTab: String = "Trending",
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isSearchingRemote: Boolean = false,
    val remoteSearchResults: List<MediaItem> = emptyList(),
    val searchSourceFilter: ContentSource? = null,
    val hotSearches: List<String> = listOf("Squid Game", "Hindi", "Bengali", "Godzilla", "Anime", "Thriller"),
    val recentSearches: List<String> = emptyList(),
    val searchSuggestions: List<String> = emptyList(),
    val selectedSeriesFilter: String = "Top Series",
    val selectedMovieFilter: String = "Cinema",
    val heroItems: List<MediaItem> = emptyList(),
    val allMedia: List<MediaItem> = emptyList(),
    val liveSections: Map<String, List<MediaItem>> = emptyMap(),
    val liveMatches: List<LiveMatchItem> = emptyList(),
    val categoryBanners: List<CategoryBannerItem> = emptyList(),
    val apiCategories: List<ApiCategory> = emptyList(),
    val platforms: List<PlatformBrand> = listOf(
        PlatformBrand("Netflix", "Netflix", 0xFFE50914),
        PlatformBrand("Hoichoi", "Hoichoi", 0xFFFF2E55),
        PlatformBrand("Zee5", "ZEE5", 0xFF8B5CF6),
        PlatformBrand("PrimeVideo", "Prime Video", 0xFF00A8E1),
        PlatformBrand("Disney", "Disney+", 0xFF1D4ED8)
    ),
    val upcomingMedia: List<MediaItem> = emptyList(),
    val adultMedia: List<MediaItem> = emptyList(),
    val isLoadingAdult: Boolean = false,
    val isAdultVerified: Boolean = false,
    val showAdultGateDialog: Boolean = false,
    val pendingAdultCategory: ApiCategory? = null,
    val filterOptions: FilterOptions = FilterOptions(),
    val quotaInfo: ApiQuotaInfo? = null,
    val shortReels: List<ShortReelItem> = emptyList(),
    val isLoadingShorts: Boolean = false,
    val shortStreamsByReelId: Map<String, List<StreamResource>> = emptyMap(),
    val loadingShortReelIds: Set<String> = emptySet(),
    val shortStreamErrorByReelId: Map<String, String> = emptyMap(),
    val novels: List<NovelItem> = emptyList(),
    val watchlistIds: Set<String> = emptySet(),
    val watchHistoryIds: List<String> = emptyList(),
    val watchProgressByMedia: Map<String, Float> = emptyMap(),
    val watchPositionMsByEpisode: Map<String, Long> = emptyMap(),
    val selectedMood: String = "All",
    val surprisePick: MediaItem? = null,
    val sleepTimerMinutes: Int = 0,
    val meSectionIndex: Int = 0, // 0 = Watchlist, 1 = History, 2 = Settings
    val downloads: List<DownloadItem> = emptyList(),
    val downloadsStorageText: String = "0 MB",
    val cacheStorageText: String = "0 MB",
    val commentsByMedia: Map<String, List<CommentItem>> = emptyMap(),
    val selectedLanguageByMedia: Map<String, String> = emptyMap(),
    val selectedSeasonByMedia: Map<String, Int> = emptyMap(),
    val selectedEpisodeByMedia: Map<String, Int> = emptyMap(),
    val seasonsCountByMedia: Map<String, Int> = emptyMap(),
    val seasonEpisodesByMedia: Map<String, Map<Int, List<EpisodeItem>>> = emptyMap(),
    val streamsByMedia: Map<String, List<StreamResource>> = emptyMap(),
    val streamErrorByMedia: Map<String, String> = emptyMap(),
    val selectedStreamIndexByMedia: Map<String, Int> = emptyMap(),
    val detailRecommendationsByMedia: Map<String, List<MediaItem>> = emptyMap(),
    val categoryBrowseTitle: String = "",
    val categoryBrowseItems: List<MediaItem> = emptyList(),
    val categoryBrowsePage: Int = 1,
    val isLoadingCategoryPage: Boolean = false,
    val canLoadMoreCategory: Boolean = false,
    val activeBrowseSource: BrowseSource = BrowseSource.LocalSection("All"),
    val activeStaffMember: StaffMember? = null,
    val staffMovies: List<MediaItem> = emptyList(),
    val isLoadingStaff: Boolean = false,
    val activeLiveMatch: LiveMatchItem? = null,
    val isLoadingHome: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val isLoadingStream: Boolean = false,
    val isApiConnected: Boolean = false,
    val isApiKeyConfigured: Boolean = false,
    val isSecondaryApiConfigured: Boolean = false,
    val apiStatusMessage: String = "Connecting...",
    val secondarySourceError: String? = null,
    val autoPlayNext: Boolean = true,
    val wifiOnlyDownloads: Boolean = false,
    val preferredQuality: String = "1080p HD",
    val subtitleSize: String = "Medium",
    val remoteConfig: RemoteAppConfig = RemoteAppConfig(),
    val isRemoteConfigSynced: Boolean = false,
    val isCheckingUpdates: Boolean = false,
    val showUpdateDialog: Boolean = false,
    val isAnnouncementDismissed: Boolean = false,
    val snackMessage: String? = null
)

class MuvioViewModel(
    private val repository: MuvioCatalogRepository = MuvioSeedRepository(),
    private val apiClient: MovieBoxApiClient = MovieBoxApiClient(),
    private val secondaryApiClient: SecondaryCatalogApiClient = SecondaryCatalogApiClient(),
    private val remoteConfigClient: MuvioRemoteConfigClient = MuvioRemoteConfigClient(),
    private val preferencesStore: MuvioPreferencesStore? = null,
    private val downloadEngine: MuvioDownloadEngine? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MuvioUiState())
    val uiState: StateFlow<MuvioUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var suggestJob: Job? = null
    private var detailJob: Job? = null
    private var streamJob: Job? = null
    private var browseJob: Job? = null
    private var staffJob: Job? = null
    private var hasAutoRetriedColdStart: Boolean = false
    private val downloadJobs = mutableMapOf<String, Job>()
    private val lastSyncedProgressByMedia = mutableMapOf<String, Float>()

    init {
        loadInitialData()
        observePersistedSettings()
        restorePersistedDownloads()
        refreshStorageStats()
        fetchRemoteConfig(userInitiated = false)
        refreshHomeFeed()
    }

    private fun restorePersistedDownloads() {
        val engine = downloadEngine ?: return
        viewModelScope.launch {
            val saved = engine.loadPersistedDownloads()
            if (saved.isNotEmpty()) {
                _uiState.update { state ->
                    val merged = (state.downloads + saved).distinctBy { it.downloadId }
                    state.copy(downloads = merged)
                }
            }
            refreshStorageStats()
        }
    }

    fun refreshStorageStats() {
        val engine = downloadEngine ?: return
        viewModelScope.launch {
            val dlBytes = engine.calculateDownloadsBytes()
            val cacheBytes = engine.calculateCacheBytes()
            _uiState.update {
                it.copy(
                    downloadsStorageText = engine.formatMb(dlBytes),
                    cacheStorageText = engine.formatMb(cacheBytes)
                )
            }
        }
    }

    fun clearAppCache() {
        val engine = downloadEngine ?: return
        viewModelScope.launch {
            val freed = engine.clearAppCaches()
            refreshStorageStats()
            _uiState.update {
                it.copy(snackMessage = "Cleared ${engine.formatMb(freed)} cache")
            }
        }
    }

    fun clearWatchHistory() {
        lastSyncedProgressByMedia.clear()
        _uiState.update {
            it.copy(
                watchHistoryIds = emptyList(),
                watchProgressByMedia = emptyMap(),
                watchPositionMsByEpisode = emptyMap(),
                snackMessage = "Watch history cleared"
            )
        }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.clearHistory() }
        }
    }

    private fun observePersistedSettings() {
        val store = preferencesStore ?: return
        viewModelScope.launch {
            store.settingsFlow.collect { saved ->
                if (saved.cachedMovieBoxBaseUrl.startsWith("https://") && apiClient.runtimeBaseUrlOverride.isBlank()) {
                    apiClient.runtimeBaseUrlOverride = saved.cachedMovieBoxBaseUrl
                }
                if (saved.cachedSecondaryBaseUrl.startsWith("https://") && secondaryApiClient.runtimeBaseUrlOverride.isBlank()) {
                    secondaryApiClient.runtimeBaseUrlOverride = saved.cachedSecondaryBaseUrl
                }
                _uiState.update { state ->
                    val existingIds = state.allMedia.mapTo(mutableSetOf()) { it.id }
                    val missingWatchlist = saved.watchlist.filter { it.id !in existingIds }
                    val missingHistory = saved.history
                        .map { it.media }
                        .filter { it.id !in existingIds && missingWatchlist.none { w -> w.id == it.id } }
                    val missingMedia = missingWatchlist + missingHistory

                    val mergedProgress = state.watchProgressByMedia.toMutableMap()
                    val mergedPositions = state.watchPositionMsByEpisode.toMutableMap()
                    val mergedSeasons = state.selectedSeasonByMedia.toMutableMap()
                    val mergedEpisodes = state.selectedEpisodeByMedia.toMutableMap()
                    saved.history.forEach { entry ->
                        if (entry.progress > 0f) {
                            mergedProgress.putIfAbsent(entry.media.id, entry.progress)
                            lastSyncedProgressByMedia.putIfAbsent(entry.media.id, entry.progress)
                        }
                        val epKey = "${entry.media.id}_s${entry.lastSeason.coerceAtLeast(1)}_e${entry.lastEpisode.coerceAtLeast(1)}"
                        if (entry.positionMs >= 45_000L) {
                            mergedPositions.putIfAbsent(epKey, entry.positionMs)
                        }
                        if (entry.lastSeason >= 1) mergedSeasons.putIfAbsent(entry.media.id, entry.lastSeason)
                        if (entry.lastEpisode >= 1) mergedEpisodes.putIfAbsent(entry.media.id, entry.lastEpisode)
                    }

                    val nextComments = state.commentsByMedia.toMutableMap()
                    saved.commentsByMedia.forEach { (mediaId, localList) ->
                        val existing = nextComments[mediaId].orEmpty()
                        val localIds = localList.mapTo(mutableSetOf()) { it.id }
                        nextComments[mediaId] = localList + existing.filterNot { it.id in localIds }
                    }

                    state.copy(
                        autoPlayNext = saved.autoPlayNext,
                        wifiOnlyDownloads = saved.wifiOnlyDownloads,
                        preferredQuality = saved.preferredQuality,
                        subtitleSize = saved.subtitleSize,
                        recentSearches = saved.recentSearches,
                        isAdultVerified = state.isAdultVerified || saved.isAdultVerified,
                        watchlistIds = saved.watchlist.mapTo(mutableSetOf()) { it.id },
                        watchHistoryIds = saved.history.map { it.media.id }.take(30),
                        watchProgressByMedia = mergedProgress,
                        watchPositionMsByEpisode = mergedPositions,
                        selectedSeasonByMedia = mergedSeasons,
                        selectedEpisodeByMedia = mergedEpisodes,
                        commentsByMedia = nextComments,
                        allMedia = if (missingMedia.isNotEmpty()) state.allMedia + missingMedia else state.allMedia
                    )
                }
            }
        }
    }

    fun fetchRemoteConfig(userInitiated: Boolean = false) {
        if (!remoteConfigClient.isConfigured()) {
            if (userInitiated) {
                _uiState.update {
                    it.copy(
                        isCheckingUpdates = false,
                        snackMessage = "Muvio v${BuildConfig.VERSION_NAME} • Up to date"
                    )
                }
            }
            return
        }
        viewModelScope.launch {
            if (userInitiated) {
                _uiState.update { it.copy(isCheckingUpdates = true) }
            }
            remoteConfigClient.fetchRemoteConfig()
                .onSuccess { config ->
                    val prevPrimaryUrl = apiClient.runtimeBaseUrlOverride
                    val prevSecondaryUrl = secondaryApiClient.runtimeBaseUrlOverride
                    var urlsChanged = false
                    if (config.movieBoxBaseUrl.startsWith("https://") &&
                        !config.movieBoxBaseUrl.equals(prevPrimaryUrl, ignoreCase = true)
                    ) {
                        apiClient.runtimeBaseUrlOverride = config.movieBoxBaseUrl
                        urlsChanged = true
                    }
                    if (config.secondaryCatalogBaseUrl.startsWith("https://") &&
                        !config.secondaryCatalogBaseUrl.equals(prevSecondaryUrl, ignoreCase = true)
                    ) {
                        secondaryApiClient.runtimeBaseUrlOverride = config.secondaryCatalogBaseUrl
                        urlsChanged = true
                    }
                    if (config.movieBoxBaseUrl.isNotBlank() || config.secondaryCatalogBaseUrl.isNotBlank()) {
                        preferencesStore?.saveRemoteBaseUrls(
                            config.movieBoxBaseUrl,
                            config.secondaryCatalogBaseUrl
                        )
                    }
                    val hasUpdate = config.latestVersionCode > BuildConfig.VERSION_CODE &&
                        config.updateUrl.startsWith("https://")
                    _uiState.update { state ->
                        val updatedHero = if (state.allMedia.isNotEmpty()) {
                            buildTrendingHeroMix(state.heroItems, state.allMedia, config)
                        } else {
                            state.heroItems
                        }
                        state.copy(
                            remoteConfig = config,
                            isRemoteConfigSynced = true,
                            isCheckingUpdates = false,
                            showUpdateDialog = hasUpdate,
                            isAnnouncementDismissed = if (config.announcementText != state.remoteConfig.announcementText) false else state.isAnnouncementDismissed,
                            heroItems = updatedHero,
                            snackMessage = when {
                                userInitiated && hasUpdate -> "নতুন আপডেট পাওয়া গেছে (v${config.latestVersionName})"
                                userInitiated -> "আপনার অ্যাপ সর্বশেষ ভার্সনে (v${BuildConfig.VERSION_NAME}) আছে!"
                                else -> state.snackMessage
                            }
                        )
                    }
                    if (urlsChanged && _uiState.value.allMedia.isEmpty()) {
                        refreshHomeFeed()
                    }
                }
                .onFailure {
                    if (userInitiated) {
                        _uiState.update {
                            it.copy(
                                isCheckingUpdates = false,
                                snackMessage = "অনলাইন আপডেট চেক করা যায়নি, ইন্টারনেট চেক করুন।"
                            )
                        }
                    }
                }
        }
    }

    fun dismissUpdateDialog() {
        if (_uiState.value.remoteConfig.forceUpdate) return
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun dismissAnnouncement() {
        _uiState.update { it.copy(isAnnouncementDismissed = true) }
    }

    private fun loadInitialData() {
        _uiState.update {
            it.copy(
                topTabs = repository.getTopTabs(),
                novels = repository.getNovels(),
                isApiKeyConfigured = apiClient.isApiKeyConfigured(),
                isSecondaryApiConfigured = secondaryApiClient.isConfigured()
            )
        }
    }

    fun refreshHomeFeed(isAutoColdStartRetry: Boolean = false) {
        if (!isAutoColdStartRetry) {
            hasAutoRetriedColdStart = false
        }
        val primaryConfigured = apiClient.isApiKeyConfigured()
        val secondaryConfigured = secondaryApiClient.isConfigured()

        if (!primaryConfigured && !secondaryConfigured) {
            _uiState.update {
                it.copy(
                    isLoadingHome = false,
                    isApiConnected = false,
                    isApiKeyConfigured = false,
                    isSecondaryApiConfigured = false,
                    apiStatusMessage = "সার্ভার সাময়িকভাবে বন্ধ আছে। কিছুক্ষণ পর আবার চেষ্টা করুন।"
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoadingHome = true,
                isApiKeyConfigured = primaryConfigured,
                isSecondaryApiConfigured = secondaryConfigured,
                apiStatusMessage = if (isAutoColdStartRetry) {
                    "সার্ভার চালু হচ্ছে... স্বয়ংক্রিয়ভাবে আবার চেষ্টা করা হচ্ছে"
                } else {
                    "Syncing Live BD, India & Global Cinema..."
                }
            )
        }

        // Source 2: MovieLink BD API v1 (runs in parallel with Source 1 for 50/50 Home blending)
        if (secondaryConfigured) {
            fetchSecondaryHomePages(primaryConfigured)
        } else {
            _uiState.update {
                it.copy(secondarySourceError = null)
            }
        }

        if (!primaryConfigured) return

        // 1. Primary /home feed (100% live items — zero demo items)
        viewModelScope.launch {
            apiClient.fetchHomeFeed()
                .onSuccess { feed ->
                    if (feed.allItems.isNotEmpty()) {
                        _uiState.update { state ->
                            val rawMerged = (feed.allItems + state.allMedia).distinctBy { it.id }
                            val mergedMedia = interleaveSources5050(rawMerged)
                            state.copy(
                                isLoadingHome = false,
                                isApiConnected = true,
                                heroItems = buildTrendingHeroMix(feed.heroItems, mergedMedia, state.remoteConfig),
                                allMedia = mergedMedia,
                                liveSections = feed.sections,
                                apiStatusMessage = "Connected • ${mergedMedia.size} live titles synced"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoadingHome = false,
                                isApiConnected = true,
                                apiStatusMessage = "Connected • Loading live shelves..."
                            )
                        }
                    }
                }
                .onFailure { err ->
                    val errText = err.message.orEmpty()
                    val isColdStartError = err is java.net.SocketTimeoutException ||
                        err is java.net.ConnectException ||
                        errText.contains("502") || errText.contains("503") || errText.contains("504") ||
                        errText.contains("timeout", ignoreCase = true)

                    if (isColdStartError && !hasAutoRetriedColdStart && _uiState.value.allMedia.isEmpty()) {
                        hasAutoRetriedColdStart = true
                        _uiState.update {
                            it.copy(
                                isLoadingHome = true,
                                apiStatusMessage = "সার্ভার চালু হচ্ছে... স্বয়ংক্রিয়ভাবে আবার চেষ্টা করা হচ্ছে"
                            )
                        }
                        delay(2_500L)
                        refreshHomeFeed(isAutoColdStartRetry = true)
                        return@launch
                    }

                    val message = when (err) {
                        is ApiKeyMissingException -> "সার্ভার সাময়িকভাবে পাওয়া যাচ্ছে না। পরে আবার চেষ্টা করুন।"
                        else -> "সার্ভার কানেক্ট হতে সময় নিচ্ছে। নিচের Retry বাটনে ট্যাপ করুন।"
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoadingHome = false,
                            isApiConnected = state.allMedia.isNotEmpty(),
                            apiStatusMessage = if (state.allMedia.isNotEmpty()) "Connected • Source 2 active" else message
                        )
                    }
                }
        }

        // 2. Defer non-critical catalog & South Asia regional enrichment so Home posters load smoothly
        viewModelScope.launch {
            delay(800L)
            listOf("/bengali" to "bengali", "/bollywood" to "bollywood").forEach { (path, regionTag) ->
                launch {
                    apiClient.fetchCatalog(path, page = 1).onSuccess { regional ->
                        if (regional.isNotEmpty()) {
                            val tagged = regional.map { item ->
                                item.copy(
                                    country = item.country.ifBlank { if (regionTag == "bengali") "Bangladesh" else "India" },
                                    languageBadge = item.languageBadge ?: if (regionTag == "bengali") "Bengali" else "Hindi",
                                    sectionTags = (item.sectionTags + regionTag + "south_asia").distinct()
                                )
                            }
                            _uiState.update { state ->
                                val merged = interleaveSources5050((state.allMedia + tagged).distinctBy { it.id })
                                state.copy(
                                    allMedia = merged,
                                    heroItems = buildTrendingHeroMix(state.heroItems, merged, state.remoteConfig)
                                )
                            }
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            delay(1_100L)
            apiClient.fetchCatalog("/type/movies", page = 1).onSuccess { movies ->
                if (movies.isNotEmpty()) {
                    _uiState.update { state ->
                        val merged = interleaveSources5050((state.allMedia + movies).distinctBy { it.id })
                        state.copy(
                            allMedia = merged,
                            heroItems = buildTrendingHeroMix(state.heroItems, merged, state.remoteConfig)
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            delay(1_150L)
            apiClient.fetchCatalog("/type/tv", page = 1).onSuccess { series ->
                if (series.isNotEmpty()) {
                    _uiState.update { state ->
                        val merged = interleaveSources5050((state.allMedia + series).distinctBy { it.id })
                        state.copy(
                            allMedia = merged,
                            heroItems = buildTrendingHeroMix(state.heroItems, merged, state.remoteConfig)
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            delay(1_200L)
            apiClient.fetchCatalog("/anime", page = 1).onSuccess { animeList ->
                if (animeList.isNotEmpty()) {
                    val taggedAnime = animeList.map { item ->
                        item.copy(
                            mediaType = MediaType.ANIME,
                            sectionTags = (item.sectionTags + "anime").distinct()
                        )
                    }
                    _uiState.update { state ->
                        val merged = interleaveSources5050((state.allMedia + taggedAnime).distinctBy { it.id })
                        state.copy(
                            allMedia = merged,
                            heroItems = buildTrendingHeroMix(state.heroItems, merged, state.remoteConfig)
                        )
                    }
                }
            }
        }

        // 3. Hot Short TV - GET /shorts?page=1
        viewModelScope.launch {
            delay(1_300L)
            _uiState.update { it.copy(isLoadingShorts = true) }
            apiClient.fetchShorts(page = 1)
                .onSuccess { reels ->
                    val resolvedReels = if (reels.isNotEmpty()) reels else buildFallbackShortReels(_uiState.value.allMedia)
                    _uiState.update {
                        it.copy(
                            isLoadingShorts = false,
                            shortReels = if (resolvedReels.isNotEmpty()) resolvedReels else it.shortReels
                        )
                    }
                    resolvedReels.firstOrNull()?.let { resolveShortReelStream(it) }
                }
                .onFailure {
                    val fallbackReels = buildFallbackShortReels(_uiState.value.allMedia)
                    _uiState.update {
                        it.copy(
                            isLoadingShorts = false,
                            shortReels = if (it.shortReels.isEmpty() && fallbackReels.isNotEmpty()) fallbackReels else it.shortReels
                        )
                    }
                }
        }

        // 4. Trending Hot Searches - GET /hot-search
        viewModelScope.launch {
            delay(1_350L)
            apiClient.fetchHotSearch().onSuccess { hot ->
                if (hot.isNotEmpty()) {
                    _uiState.update { it.copy(hotSearches = hot.take(12)) }
                }
            }
        }

        // 5. Coming Soon / Upcoming - GET /upcoming?page=1
        viewModelScope.launch {
            delay(1_400L)
            apiClient.fetchUpcoming(page = 1).onSuccess { upcoming ->
                if (upcoming.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(
                            upcomingMedia = upcoming,
                            allMedia = (state.allMedia + upcoming).distinctBy { it.id }
                        )
                    }
                }
            }
        }

        // 6. All Categories (sports excluded, 18+ included) - GET /categories
        viewModelScope.launch {
            delay(1_450L)
            apiClient.fetchCategories().onSuccess { categories ->
                if (categories.isNotEmpty()) {
                    _uiState.update { it.copy(apiCategories = categories) }
                }
            }
        }

        // 7. Filter Options - GET /filter-options
        viewModelScope.launch {
            delay(1_500L)
            apiClient.fetchFilterOptions().onSuccess { options ->
                _uiState.update { it.copy(filterOptions = options) }
            }
        }

        // 8. API Key Quota & Expiry - GET /me
        viewModelScope.launch {
            delay(1_550L)
            apiClient.fetchAccountQuota().onSuccess { quota ->
                _uiState.update { it.copy(quotaInfo = quota) }
            }
        }
    }

    fun retrySecondarySource() {
        val currentQuery = _uiState.value.searchQuery.trim()
        fetchSecondaryHomePages(apiClient.isApiKeyConfigured())
        if (_uiState.value.isSearchActive && currentQuery.length >= 2) {
            updateSearchQuery(currentQuery)
        }
    }

    fun retryShorts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingShorts = true) }
            if (apiClient.isApiKeyConfigured()) {
                apiClient.fetchShorts(page = 1)
                    .onSuccess { reels ->
                        val resolvedReels = if (reels.isNotEmpty()) reels else buildFallbackShortReels(_uiState.value.allMedia)
                        _uiState.update {
                            it.copy(
                                isLoadingShorts = false,
                                shortReels = if (resolvedReels.isNotEmpty()) resolvedReels else it.shortReels
                            )
                        }
                        resolvedReels.firstOrNull()?.let { resolveShortReelStream(it, forceRefresh = true) }
                        return@launch
                    }
            }
            val fallbackReels = buildFallbackShortReels(_uiState.value.allMedia)
            _uiState.update {
                it.copy(
                    isLoadingShorts = false,
                    shortReels = if (fallbackReels.isNotEmpty()) fallbackReels else it.shortReels,
                    snackMessage = if (fallbackReels.isEmpty()) "Could not load shorts. Tap Retry." else null
                )
            }
            fallbackReels.firstOrNull()?.let { resolveShortReelStream(it, forceRefresh = true) }
        }
    }

    private fun buildFallbackShortReels(catalog: List<MediaItem>): List<ShortReelItem> {
        val candidates = catalog.filter {
            it.mediaType == MediaType.SHORT_TV || "hot_short" in it.sectionTags
        }.ifEmpty {
            catalog.filter { it.mediaType == MediaType.DRAMA || it.mediaType == MediaType.SERIES }.take(10)
        }
        return candidates.take(12).mapIndexed { idx, item ->
            ShortReelItem(
                id = "reel_${item.id}",
                linkedMediaId = item.id,
                title = item.title,
                episodeTag = item.duration.ifBlank { "Short Drama • HD" },
                description = item.synopsis.ifBlank { "Watch full micro-drama on Muvio." },
                likesCount = 1400 + (idx * 280),
                commentsCount = 48 + (idx * 11),
                poster = item.poster,
                streamUrl = item.resources.firstOrNull()?.streamUrl.orEmpty()
            )
        }
    }

    fun resolveShortReelStream(reel: ShortReelItem, forceRefresh: Boolean = false) {
        val state = _uiState.value
        if (!forceRefresh) {
            if (state.shortStreamsByReelId[reel.id]?.isNotEmpty() == true) return
            if (reel.id in state.loadingShortReelIds) return
            if (reel.streamUrl.isNotBlank()) {
                val direct = StreamResource(
                    id = "short_direct_${reel.id}",
                    label = reel.title,
                    language = "Original",
                    quality = "HD",
                    uploader = "Muvio Shorts",
                    streamUrl = reel.streamUrl
                )
                _uiState.update {
                    it.copy(
                        shortStreamsByReelId = it.shortStreamsByReelId + (reel.id to listOf(direct)),
                        shortStreamErrorByReelId = it.shortStreamErrorByReelId - reel.id
                    )
                }
                return
            }
        }

        _uiState.update {
            it.copy(
                loadingShortReelIds = it.loadingShortReelIds + reel.id,
                shortStreamErrorByReelId = it.shortStreamErrorByReelId - reel.id
            )
        }

        viewModelScope.launch {
            var resolved: List<StreamResource> = emptyList()
            if (apiClient.isApiKeyConfigured()) {
                resolved = apiClient.fetchStreams(
                    title = reel.title,
                    season = 1,
                    episode = 1,
                    isSeries = true
                ).getOrNull().orEmpty()
                if (resolved.isEmpty()) {
                    resolved = apiClient.fetchStreams(
                        title = reel.title,
                        season = 1,
                        episode = 1,
                        isSeries = false
                    ).getOrNull().orEmpty()
                }
            }
            if (resolved.isEmpty() && secondaryApiClient.isConfigured()) {
                val linked = getMediaById(reel.linkedMediaId)
                resolved = secondaryApiClient.fetchStreams(
                    detailPath = linked?.sourceDetailPath.orEmpty(),
                    title = reel.title,
                    season = 1,
                    episode = 1,
                    isSeries = true
                ).getOrNull().orEmpty()
            }

            // Order 720p/1080p/480p first so vertical short reels start playing fast
            val ordered = resolved.sortedBy { res ->
                val q = res.quality.lowercase()
                when {
                    "720" in q -> 0
                    "480" in q -> 1
                    "1080" in q -> 2
                    "hd" in q -> 3
                    else -> 4
                }
            }

            _uiState.update { current ->
                if (ordered.isNotEmpty()) {
                    current.copy(
                        loadingShortReelIds = current.loadingShortReelIds - reel.id,
                        shortStreamsByReelId = current.shortStreamsByReelId + (reel.id to ordered),
                        shortStreamErrorByReelId = current.shortStreamErrorByReelId - reel.id
                    )
                } else {
                    current.copy(
                        loadingShortReelIds = current.loadingShortReelIds - reel.id,
                        shortStreamErrorByReelId = current.shortStreamErrorByReelId + (
                            reel.id to "ভিডিও স্ট্রীম পাওয়া যায়নি • Tap Retry"
                        )
                    )
                }
            }
        }
    }

    fun saveRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        _uiState.update { state ->
            val updated = (listOf(clean) + state.recentSearches.filterNot { it.equals(clean, ignoreCase = true) }).take(10)
            state.copy(recentSearches = updated)
        }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.addRecentSearch(clean) }
        }
    }

    fun clearRecentSearches() {
        _uiState.update { it.copy(recentSearches = emptyList()) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.clearRecentSearches() }
        }
    }

    fun setSubtitleSize(size: String) {
        _uiState.update { it.copy(subtitleSize = size) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setSubtitleSize(size) }
        }
    }

    private fun fetchSecondaryHomePages(primaryConfigured: Boolean) {
        if (!secondaryApiClient.isConfigured()) {
            _uiState.update { it.copy(secondarySourceError = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(secondarySourceError = null) }
            secondaryApiClient.fetchHome()
                .onSuccess { secondaryHome ->
                    if (secondaryHome.isNotEmpty()) {
                        _uiState.update { state ->
                            val merged = interleaveSources5050((state.allMedia + secondaryHome).distinctBy { it.id })
                            state.copy(
                                isLoadingHome = false,
                                isApiConnected = true,
                                secondarySourceError = null,
                                allMedia = merged,
                                heroItems = buildTrendingHeroMix(state.heroItems, merged),
                                apiStatusMessage = "Connected • Multi-Source 50/50 (${merged.size} titles)"
                            )
                        }
                    } else {
                        // Fallback to /catalog?path=/&page=1 if /home returned empty
                        secondaryApiClient.fetchCatalog(path = "/", page = 1).onSuccess { fallbackItems ->
                            if (fallbackItems.isNotEmpty()) {
                                _uiState.update { state ->
                                    val merged = interleaveSources5050((state.allMedia + fallbackItems).distinctBy { it.id })
                                    state.copy(
                                        isLoadingHome = false,
                                        isApiConnected = true,
                                        secondarySourceError = null,
                                        allMedia = merged,
                                        heroItems = buildTrendingHeroMix(state.heroItems, merged),
                                        apiStatusMessage = "Connected • Multi-Source 50/50 (${merged.size} titles)"
                                    )
                                }
                            } else {
                                _uiState.update { state ->
                                    state.copy(
                                        isLoadingHome = if (!primaryConfigured) false else state.isLoadingHome,
                                        secondarySourceError = "Source 2 returned 0 items for home feed"
                                    )
                                }
                            }
                        }
                    }
                }
                .onFailure { err ->
                    val errText = err.message?.take(140) ?: "Source 2 connection failed"
                    _uiState.update { state ->
                        state.copy(
                            isLoadingHome = if (!primaryConfigured) false else state.isLoadingHome,
                            isApiConnected = if (!primaryConfigured) false else state.isApiConnected,
                            secondarySourceError = errText,
                            apiStatusMessage = if (!primaryConfigured) errText else state.apiStatusMessage
                        )
                    }
                }
        }
        // Enrich 50/50 shelves with BD, India, South Indian, Drama & Anime catalogs
        listOf("/language/bangla", "/bollywood", "/southIndian", "/drama", "/anime").forEachIndexed { idx, catPath ->
            viewModelScope.launch {
                delay(900L + idx * 140L)
                secondaryApiClient.fetchCatalog(path = catPath, page = 1).onSuccess { catItems ->
                    if (catItems.isNotEmpty()) {
                        val isRegional = catPath in listOf("/language/bangla", "/bollywood", "/southIndian")
                        val enriched = if (isRegional) {
                            catItems.map { it.copy(sectionTags = (it.sectionTags + "south_asia").distinct()) }
                        } else {
                            catItems
                        }
                        _uiState.update { state ->
                            val merged = interleaveSources5050((state.allMedia + enriched).distinctBy { it.id })
                            state.copy(
                                secondarySourceError = null,
                                allMedia = merged,
                                heroItems = buildTrendingHeroMix(state.heroItems, merged, state.remoteConfig)
                            )
                        }
                    }
                }
            }
        }
        // Blend Source 2 /hot-search into Trending Searches
        viewModelScope.launch {
            delay(1_400L)
            secondaryApiClient.fetchHotSearch().onSuccess { hot ->
                if (hot.isNotEmpty()) {
                    _uiState.update { state ->
                        val combined = mutableListOf<String>()
                        val maxLen = maxOf(state.hotSearches.size, hot.size)
                        for (i in 0 until maxLen) {
                            if (i < state.hotSearches.size) combined += state.hotSearches[i]
                            if (i < hot.size) combined += hot[i]
                        }
                        state.copy(hotSearches = combined.distinct().take(12))
                    }
                }
            }
        }
        // Blend Source 2 /upcoming 50/50 into Coming Soon
        viewModelScope.launch {
            delay(1_500L)
            secondaryApiClient.fetchUpcoming(page = 1).onSuccess { upcoming ->
                if (upcoming.isNotEmpty()) {
                    _uiState.update { state ->
                        val blendedUpcoming = interleaveSources5050((state.upcomingMedia + upcoming).distinctBy { it.id })
                        val mergedAll = interleaveSources5050((state.allMedia + upcoming).distinctBy { it.id })
                        state.copy(
                            upcomingMedia = blendedUpcoming,
                            allMedia = mergedAll
                        )
                    }
                }
            }
        }
    }

    internal fun interleaveSources5050(items: List<MediaItem>): List<MediaItem> {
        val primary = items.filter { it.contentSource == ContentSource.PRIMARY }
        val secondary = items.filter { it.contentSource == ContentSource.SECONDARY }
        return interleaveTwoLists(primary, secondary)
    }

    internal fun interleaveTwoLists(
        first: List<MediaItem>,
        second: List<MediaItem>
    ): List<MediaItem> {
        if (first.isEmpty()) return second.distinctBy { it.id }
        if (second.isEmpty()) return first.distinctBy { it.id }
        val out = mutableListOf<MediaItem>()
        val seen = mutableSetOf<String>()
        val maxLen = maxOf(first.size, second.size)
        for (i in 0 until maxLen) {
            if (i < first.size && seen.add(first[i].id)) out += first[i]
            if (i < second.size && seen.add(second[i].id)) out += second[i]
        }
        return out
    }

    internal fun isSouthAsiaRegional(
        item: MediaItem,
        config: RemoteAppConfig = _uiState.value.remoteConfig
    ): Boolean {
        if (item.sectionTags.any { it in listOf("bengali", "bollywood", "south_indian", "south_asia") }) {
            return true
        }
        val haystack = buildString {
            append(item.country).append(' ')
            append(item.languageBadge.orEmpty()).append(' ')
            append(item.subCategory).append(' ')
            append(item.genre).append(' ')
            append(item.title)
        }.lowercase()
        val keywords = (config.priorityRegions + listOf(
            "bangladesh", "bengali", "bangla", "kolkata", "dhaka",
            "india", "indian", "hindi", "bollywood", "tollywood",
            "south indian", "tamil", "telugu", "malayalam", "kannada",
            "punjabi", "marathi", "pakistan", "pakistani", "nepali",
            "hoichoi", "chorki", "bongo", "zee5"
        )).map { it.lowercase() }.distinct()
        return keywords.any { kw -> kw.length >= 3 && haystack.contains(kw) }
    }

    fun southAsiaTrendingItems(): List<MediaItem> {
        val state = _uiState.value
        val regional = state.allMedia.filter { isSouthAsiaRegional(it, state.remoteConfig) }
        val sortedRegional = regional.sortedByDescending { item ->
            val bdBoost = if (item.languageBadge.equals("Bengali", true) ||
                item.country.contains("Bangladesh", true) ||
                "bengali" in item.sectionTags
            ) 2.0 else 0.0
            val yearBoost = if (item.year >= 2024) 1.5 else 0.0
            bdBoost + yearBoost + (item.rating / 10.0)
        }
        return interleaveSources5050(sortedRegional).take(16)
    }

    internal fun buildTrendingHeroMix(
        baseHero: List<MediaItem>,
        allMedia: List<MediaItem>,
        config: RemoteAppConfig = _uiState.value.remoteConfig
    ): List<MediaItem> {
        val pool = interleaveSources5050((baseHero + allMedia).distinctBy { it.id })
            .filter { !it.backdropUrl.isNullOrBlank() || !it.poster.remoteUrl.isNullOrBlank() }
        if (pool.isEmpty()) return baseHero

        val interleaved = mutableListOf<MediaItem>()
        val seenIds = mutableSetOf<String>()
        val maxSlots = 12

        // 1. Remote-curated Featured Banner Titles (instant online override via Remote Config JSON)
        if (config.featuredBannerTitles.isNotEmpty()) {
            config.featuredBannerTitles.forEach { featuredTitle ->
                val match = pool.firstOrNull {
                    it.title.contains(featuredTitle, ignoreCase = true)
                }
                if (match != null && interleaved.size < maxSlots && seenIds.add(match.id)) {
                    interleaved += match
                }
            }
        }

        // 2. Auto-prioritize Trending BD, India & South Asia hits (50/50 MovieBox + MovieLink BD)
        val regionalHits = interleaveSources5050(
            pool.filter { isSouthAsiaRegional(it, config) }
        )
        regionalHits.take(6).forEach { regional ->
            if (interleaved.size < maxSlots && seenIds.add(regional.id)) {
                interleaved += regional
            }
        }

        // 3. Fill remaining Hero slots with top global Movies, Series & Anime interleaved 50/50
        val trendingMovies = interleaveSources5050(pool.filter { it.mediaType == MediaType.MOVIE })
        val trendingSeries = interleaveSources5050(pool.filter { it.mediaType == MediaType.SERIES || it.mediaType == MediaType.DRAMA })
        val trendingAnime = interleaveSources5050(pool.filter { it.mediaType == MediaType.ANIME || "anime" in it.sectionTags })

        var idx = 0
        while (interleaved.size < maxSlots && idx < 6) {
            listOf(trendingMovies, trendingSeries, trendingAnime).forEach { bucket ->
                if (interleaved.size < maxSlots && idx < bucket.size) {
                    val candidate = bucket[idx]
                    if (seenIds.add(candidate.id)) {
                        interleaved += candidate
                    }
                }
            }
            idx++
        }
        // Fill remaining slots up to 10 from pool if any bucket was small
        for (item in pool + baseHero) {
            if (interleaved.size >= 10) break
            if (seenIds.add(item.id)) interleaved += item
        }
        return interleaved
    }

    fun requestAdultAccess(category: ApiCategory? = null): Boolean {
        val state = _uiState.value
        if (state.isAdultVerified) {
            if (category != null) {
                openCatalogPath(category.name, category.path)
            } else {
                selectTopTab("18+")
                loadAdultCatalog()
            }
            return true
        }
        _uiState.update {
            it.copy(
                showAdultGateDialog = true,
                pendingAdultCategory = category
            )
        }
        return false
    }

    fun confirmAdultAge(): ApiCategory? {
        val pending = _uiState.value.pendingAdultCategory
        _uiState.update {
            it.copy(
                isAdultVerified = true,
                showAdultGateDialog = false,
                pendingAdultCategory = null
            )
        }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setAdultVerified(true) }
        }
        if (pending != null) {
            openCatalogPath(pending.name, pending.path)
        } else {
            selectTopTab("18+")
            loadAdultCatalog()
        }
        return pending
    }

    fun dismissAdultGate() {
        _uiState.update {
            it.copy(showAdultGateDialog = false, pendingAdultCategory = null)
        }
    }

    fun loadAdultCatalog() {
        val primaryConfigured = apiClient.isApiKeyConfigured()
        val secondaryConfigured = secondaryApiClient.isConfigured()
        if (!primaryConfigured && !secondaryConfigured) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAdult = true) }
            var pItems: List<MediaItem> = emptyList()
            var sItems: List<MediaItem> = emptyList()
            coroutineScope {
                if (primaryConfigured) {
                    launch {
                        apiClient.fetchCatalog("/adult18plus", page = 1).onSuccess { pItems = it }
                    }
                }
                if (secondaryConfigured) {
                    launch {
                        secondaryApiClient.fetchCatalog("/adult18plus", page = 1).onSuccess { sItems = it }
                    }
                }
            }
            val blended = interleaveTwoLists(pItems, sItems)
            _uiState.update { state ->
                state.copy(
                    isLoadingAdult = false,
                    adultMedia = blended.ifEmpty { state.adultMedia },
                    allMedia = interleaveSources5050((state.allMedia + blended).distinctBy { it.id })
                )
            }
        }
    }

    fun loadCatalogForCategory(categoryName: String) {
        if (categoryName == "18+") {
            loadAdultCatalog()
            return
        }
        val primaryPath = when (categoryName) {
            "Trending Movies", "Cinema", "Movies" -> "/type/movies"
            "Trending TV Series", "Series" -> "/type/tv"
            "Bollywood" -> "/bollywood"
            "Anime" -> "/anime"
            "Bengali" -> "/bengali"
            "Hot Short TV" -> "/shorts"
            else -> "/trending"
        }
        val secondaryPath = when (categoryName) {
            "Trending Movies", "Cinema", "Movies" -> "/new-uploads"
            "Trending TV Series", "Series" -> "/drama"
            "Bollywood" -> "/bollywood"
            "Anime" -> "/anime"
            "Bengali" -> "/language/bangla"
            else -> "/"
        }
        if (apiClient.isApiKeyConfigured()) {
            viewModelScope.launch {
                apiClient.fetchCatalog(primaryPath, page = 1).onSuccess { items ->
                    if (items.isNotEmpty()) {
                        _uiState.update { state ->
                            val merged = interleaveSources5050((state.allMedia + items).distinctBy { it.id })
                            state.copy(allMedia = merged)
                        }
                    }
                }
            }
        }
        if (secondaryApiClient.isConfigured()) {
            viewModelScope.launch {
                secondaryApiClient.fetchCatalog(secondaryPath, page = 1).onSuccess { items ->
                    if (items.isNotEmpty()) {
                        _uiState.update { state ->
                            val merged = interleaveSources5050((state.allMedia + items).distinctBy { it.id })
                            state.copy(allMedia = merged, secondarySourceError = null)
                        }
                    }
                }
            }
        }
    }

    private fun mapSecondaryBrowsePath(path: String, title: String): String {
        val p = path.lowercase()
        val t = title.lowercase()
        return when {
            p.contains("adult") || t.contains("18+") -> "/adult18plus"
            p.contains("anime") || t.contains("anime") -> "/anime"
            p.contains("bangla") || p.contains("bengali") || t.contains("bengali") -> "/language/bangla"
            p.contains("bollywood") || t.contains("bollywood") -> "/bollywood"
            p.contains("south") || t.contains("south") -> "/southIndian"
            p.contains("ongoing") || t.contains("ongoing") -> "/ongoing"
            p.contains("/tv") || p.contains("series") || p.contains("drama") || t.contains("series") -> "/drama"
            p.contains("movie") || t.contains("movie") -> "/bollywood"
            else -> "/"
        }
    }

    fun openCatalogPath(title: String, path: String) {
        browseJob?.cancel()
        _uiState.update {
            it.copy(
                categoryBrowseTitle = title,
                categoryBrowseItems = emptyList(),
                categoryBrowsePage = 1,
                isLoadingCategoryPage = true,
                canLoadMoreCategory = true,
                activeBrowseSource = BrowseSource.CatalogPath(path)
            )
        }
        val primaryConfigured = apiClient.isApiKeyConfigured()
        val secondaryConfigured = secondaryApiClient.isConfigured()
        if (!primaryConfigured && !secondaryConfigured) {
            _uiState.update {
                it.copy(
                    categoryBrowseItems = it.allMedia,
                    isLoadingCategoryPage = false,
                    canLoadMoreCategory = false
                )
            }
            return
        }
        val secPath = mapSecondaryBrowsePath(path, title)
        browseJob = viewModelScope.launch {
            var pItems: List<MediaItem> = emptyList()
            var sItems: List<MediaItem> = emptyList()
            coroutineScope {
                if (primaryConfigured) {
                    launch {
                        apiClient.fetchCatalog(path, page = 1).onSuccess { pItems = it }
                    }
                }
                if (secondaryConfigured) {
                    launch {
                        secondaryApiClient.fetchCatalog(secPath, page = 1).onSuccess { sItems = it }
                    }
                }
            }
            val blended = interleaveTwoLists(pItems, sItems)
            _uiState.update { state ->
                state.copy(
                    categoryBrowseItems = blended.ifEmpty { state.allMedia },
                    allMedia = interleaveSources5050((state.allMedia + blended).distinctBy { it.id }),
                    isLoadingCategoryPage = false,
                    canLoadMoreCategory = blended.isNotEmpty()
                )
            }
        }
    }

    fun openPlatform(brand: PlatformBrand) {
        browseJob?.cancel()
        _uiState.update {
            it.copy(
                categoryBrowseTitle = "${brand.displayName} Originals",
                categoryBrowseItems = emptyList(),
                categoryBrowsePage = 1,
                isLoadingCategoryPage = true,
                canLoadMoreCategory = true,
                activeBrowseSource = BrowseSource.Platform(brand.apiName)
            )
        }
        if (!apiClient.isApiKeyConfigured()) {
            _uiState.update {
                it.copy(
                    categoryBrowseItems = it.allMedia,
                    isLoadingCategoryPage = false,
                    canLoadMoreCategory = false
                )
            }
            return
        }
        browseJob = viewModelScope.launch {
            apiClient.fetchPlatform(brand.apiName, page = 1)
                .onSuccess { items ->
                    _uiState.update { state ->
                        state.copy(
                            categoryBrowseItems = items.ifEmpty { state.allMedia },
                            allMedia = (state.allMedia + items).distinctBy { it.id },
                            isLoadingCategoryPage = false,
                            canLoadMoreCategory = items.isNotEmpty()
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            categoryBrowseItems = state.allMedia,
                            isLoadingCategoryPage = false,
                            canLoadMoreCategory = false
                        )
                    }
                }
        }
    }

    fun openUpcomingBrowse() {
        browseJob?.cancel()
        val cached = _uiState.value.upcomingMedia
        _uiState.update {
            it.copy(
                categoryBrowseTitle = "Coming Soon",
                categoryBrowseItems = cached,
                categoryBrowsePage = 1,
                isLoadingCategoryPage = cached.isEmpty() && apiClient.isApiKeyConfigured(),
                canLoadMoreCategory = apiClient.isApiKeyConfigured(),
                activeBrowseSource = BrowseSource.Upcoming
            )
        }
        if (cached.isEmpty() && apiClient.isApiKeyConfigured()) {
            browseJob = viewModelScope.launch {
                apiClient.fetchUpcoming(page = 1)
                    .onSuccess { items ->
                        _uiState.update { state ->
                            state.copy(
                                upcomingMedia = items,
                                categoryBrowseItems = items,
                                allMedia = (state.allMedia + items).distinctBy { it.id },
                                isLoadingCategoryPage = false,
                                canLoadMoreCategory = items.isNotEmpty()
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update { it.copy(isLoadingCategoryPage = false, canLoadMoreCategory = false) }
                    }
            }
        }
    }

    fun openLocalSectionBrowse(sectionTitle: String) {
        val state = _uiState.value
        val catalogPath = when (sectionTitle) {
            "Trending Movies", "Cinema" -> "/type/movies"
            "Trending TV Series" -> "/type/tv"
            "Bollywood" -> "/bollywood"
            "Anime" -> "/anime"
            "Hot Short TV" -> "/shorts"
            else -> null
        }
        if (catalogPath != null && apiClient.isApiKeyConfigured()) {
            openCatalogPath(sectionTitle, catalogPath)
            return
        }
        val list = state.liveSections[sectionTitle] ?: when (sectionTitle) {
            "Trending in BD & South Asia" -> southAsiaTrendingItems()
            "Trending Movies" -> state.allMedia.filter { it.mediaType == MediaType.MOVIE }
            "Trending TV Series" -> state.allMedia.filter {
                it.mediaType == MediaType.SERIES || it.mediaType == MediaType.DRAMA || it.mediaType == MediaType.ANIME
            }
            "Hot Short TV" -> state.allMedia.filter {
                "hot_short" in it.sectionTags || it.mediaType == MediaType.SHORT_TV
            }
            else -> state.allMedia
        }
        _uiState.update {
            it.copy(
                categoryBrowseTitle = sectionTitle,
                categoryBrowseItems = list.ifEmpty { state.allMedia },
                categoryBrowsePage = 1,
                isLoadingCategoryPage = false,
                canLoadMoreCategory = false,
                activeBrowseSource = BrowseSource.LocalSection(sectionTitle)
            )
        }
    }

    fun loadNextBrowsePage() {
        val state = _uiState.value
        val primaryConfigured = apiClient.isApiKeyConfigured()
        val secondaryConfigured = secondaryApiClient.isConfigured()
        if (state.isLoadingCategoryPage || !state.canLoadMoreCategory || (!primaryConfigured && !secondaryConfigured)) return
        val nextPage = state.categoryBrowsePage + 1
        val source = state.activeBrowseSource
        val title = state.categoryBrowseTitle

        browseJob?.cancel()
        browseJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategoryPage = true) }
            var pItems: List<MediaItem> = emptyList()
            var sItems: List<MediaItem> = emptyList()
            coroutineScope {
                when (source) {
                    is BrowseSource.CatalogPath -> {
                        if (primaryConfigured) {
                            launch { apiClient.fetchCatalog(source.path, page = nextPage).onSuccess { pItems = it } }
                        }
                        if (secondaryConfigured) {
                            val secPath = mapSecondaryBrowsePath(source.path, title)
                            launch { secondaryApiClient.fetchCatalog(secPath, page = nextPage).onSuccess { sItems = it } }
                        }
                    }
                    is BrowseSource.Platform -> {
                        if (primaryConfigured) {
                            launch { apiClient.fetchPlatform(source.name, page = nextPage).onSuccess { pItems = it } }
                        }
                    }
                    is BrowseSource.Upcoming -> {
                        if (primaryConfigured) {
                            launch { apiClient.fetchUpcoming(page = nextPage).onSuccess { pItems = it } }
                        }
                        if (secondaryConfigured) {
                            launch { secondaryApiClient.fetchUpcoming(page = nextPage).onSuccess { sItems = it } }
                        }
                    }
                    is BrowseSource.LocalSection -> Unit
                }
            }
            val newItems = interleaveTwoLists(pItems, sItems)
            _uiState.update { current ->
                val mergedBrowse = (current.categoryBrowseItems + newItems).distinctBy { it.id }
                val addedNew = mergedBrowse.size > current.categoryBrowseItems.size
                current.copy(
                    categoryBrowseItems = mergedBrowse,
                    categoryBrowsePage = if (addedNew) nextPage else current.categoryBrowsePage,
                    allMedia = interleaveSources5050((current.allMedia + newItems).distinctBy { it.id }),
                    isLoadingCategoryPage = false,
                    canLoadMoreCategory = addedNew
                )
            }
        }
    }

    fun openMediaDetail(media: MediaItem) {
        // Ensure the opened item is immediately registered in allMedia so DetailPlayerScreen
        // never misses getMediaById() and never falls back to another movie.
        _uiState.update { state ->
            if (state.allMedia.any { it.id == media.id }) {
                state
            } else {
                state.copy(allMedia = listOf(media) + state.allMedia)
            }
        }
        recordWatchHistory(media.id)

        // Strict Source Isolation: Source 2 items ONLY fetch details, episodes & streams from SecondaryCatalogApiClient
        if (media.contentSource == ContentSource.SECONDARY) {
            openSecondaryMediaDetail(media)
            return
        }

        // Immediately seed seasons & episodes if media is a series/anime (e.g., "S1-S4" in title or SERIES/ANIME type)
        val (inferredSeasonsCount, inferredSeasonMap) = MovieBoxParsers.inferSeriesSeasonsAndEpisodes(media)
        val initialSeason = _uiState.value.selectedSeasonByMedia[media.id] ?: 1
        if (inferredSeasonMap.isNotEmpty()) {
            _uiState.update { state ->
                val existingMap = state.seasonEpisodesByMedia[media.id].orEmpty()
                val effectiveMap = if (existingMap.isNotEmpty()) existingMap else inferredSeasonMap
                val effectiveCount = maxOf(
                    state.seasonsCountByMedia[media.id] ?: 1,
                    inferredSeasonsCount,
                    effectiveMap.keys.maxOrNull() ?: 1
                )
                val activeEps = effectiveMap[initialSeason] ?: effectiveMap.values.firstOrNull().orEmpty()
                val enrichedMedia = media.copy(
                    episodes = media.episodes.ifEmpty { activeEps },
                    seasonEpisodes = media.seasonEpisodes.ifEmpty { effectiveMap }
                )
                val updatedAll = if (state.allMedia.any { it.id == media.id }) {
                    state.allMedia.map { if (it.id == media.id) enrichedMedia else it }
                } else {
                    listOf(enrichedMedia) + state.allMedia
                }
                state.copy(
                    allMedia = updatedAll,
                    seasonsCountByMedia = state.seasonsCountByMedia + (media.id to effectiveCount),
                    seasonEpisodesByMedia = state.seasonEpisodesByMedia + (media.id to effectiveMap)
                )
            }
        }

        if (!apiClient.isApiKeyConfigured()) return

        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true) }
            apiClient.fetchDetails(media.title, media.id)
                .onSuccess { detail ->
                    _uiState.update { state ->
                        val activeSeason = state.selectedSeasonByMedia[media.id] ?: 1
                        val existingMap = state.seasonEpisodesByMedia[media.id].orEmpty()
                        val mergedSeasonMap = when {
                            detail.seasonEpisodes.isNotEmpty() -> detail.seasonEpisodes
                            existingMap.isNotEmpty() -> existingMap
                            else -> emptyMap()
                        }
                        val resolvedSeasonsCount = maxOf(
                            detail.seasonsCount,
                            mergedSeasonMap.size,
                            mergedSeasonMap.keys.maxOrNull() ?: 1,
                            state.seasonsCountByMedia[media.id] ?: 1
                        )
                        val activeSeasonEpisodes = mergedSeasonMap[activeSeason]
                            ?: detail.updatedItem?.episodes?.takeIf { it.isNotEmpty() }
                            ?: mergedSeasonMap.values.firstOrNull().orEmpty()

                        val currentInState = state.allMedia.find { it.id == media.id } ?: media
                        val updatedItem = detail.updatedItem?.let { fetched ->
                            currentInState.copy(
                                synopsis = fetched.synopsis.ifBlank { currentInState.synopsis },
                                year = fetched.year,
                                country = fetched.country.ifBlank { currentInState.country },
                                genre = fetched.genre.ifBlank { currentInState.genre },
                                rating = fetched.rating,
                                duration = fetched.duration.ifBlank { currentInState.duration },
                                mediaType = if (fetched.mediaType != MediaType.MOVIE || currentInState.mediaType == MediaType.MOVIE) {
                                    fetched.mediaType
                                } else {
                                    currentInState.mediaType
                                },
                                episodes = activeSeasonEpisodes.ifEmpty { currentInState.episodes },
                                seasonEpisodes = mergedSeasonMap.ifEmpty { currentInState.seasonEpisodes },
                                dubs = fetched.dubs.ifEmpty { currentInState.dubs },
                                cast = fetched.cast.ifEmpty { currentInState.cast },
                                backdropUrl = fetched.backdropUrl ?: currentInState.backdropUrl
                            )
                        } ?: currentInState.copy(
                            episodes = activeSeasonEpisodes.ifEmpty { currentInState.episodes },
                            seasonEpisodes = mergedSeasonMap.ifEmpty { currentInState.seasonEpisodes }
                        )

                        val nextAll = if (state.allMedia.any { it.id == media.id }) {
                            state.allMedia.map { if (it.id == media.id) updatedItem else it }
                        } else {
                            listOf(updatedItem) + state.allMedia
                        }
                        val mergedRecs = (nextAll + detail.recommendations).distinctBy { it.id }
                        state.copy(
                            isLoadingDetail = false,
                            allMedia = mergedRecs,
                            seasonsCountByMedia = state.seasonsCountByMedia + (media.id to resolvedSeasonsCount),
                            seasonEpisodesByMedia = if (mergedSeasonMap.isNotEmpty()) {
                                state.seasonEpisodesByMedia + (media.id to mergedSeasonMap)
                            } else {
                                state.seasonEpisodesByMedia
                            },
                            detailRecommendationsByMedia = if (detail.recommendations.isNotEmpty()) {
                                state.detailRecommendationsByMedia + (media.id to detail.recommendations)
                            } else {
                                state.detailRecommendationsByMedia
                            }
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingDetail = false) }
                }
        }

        // Parallel GET /related?title=...
        viewModelScope.launch {
            apiClient.fetchRelated(media.title).onSuccess { related ->
                if (related.isNotEmpty()) {
                    _uiState.update { state ->
                        val existing = state.detailRecommendationsByMedia[media.id].orEmpty()
                        val combined = (existing + related).distinctBy { it.id }
                        state.copy(
                            allMedia = (state.allMedia + related).distinctBy { it.id },
                            detailRecommendationsByMedia = state.detailRecommendationsByMedia + (media.id to combined)
                        )
                    }
                }
            }
        }

        // Parallel GET /comments?subjectId=...
        if (!media.id.startsWith("seed_")) {
            viewModelScope.launch {
                apiClient.fetchComments(media.id, page = 1).onSuccess { remoteComments ->
                    if (remoteComments.isNotEmpty()) {
                        _uiState.update { state ->
                            val userPosted = state.commentsByMedia[media.id].orEmpty().filter { it.author == "You" }
                            state.copy(
                                commentsByMedia = state.commentsByMedia + (media.id to (userPosted + remoteComments).distinctBy { it.id })
                            )
                        }
                    }
                }
            }
        }

        val season = _uiState.value.selectedSeasonByMedia[media.id] ?: 1
        val episode = _uiState.value.selectedEpisodeByMedia[media.id] ?: 1
        fetchStreamsForMedia(media, season, episode)
    }

    private fun openSecondaryMediaDetail(media: MediaItem) {
        detailJob?.cancel()
        streamJob?.cancel()
        val hasSavedEp = _uiState.value.selectedEpisodeByMedia.containsKey(media.id)
        val activeSeason = _uiState.value.selectedSeasonByMedia[media.id] ?: 1
        val activeEpNum = _uiState.value.selectedEpisodeByMedia[media.id] ?: 1

        // Start fetching the stream IN PARALLEL with /details for movies & items with a detail path
        // so ExoPlayer starts buffering right away instead of waiting for /details to finish first!
        val canPreFetchStreamInParallel = secondaryApiClient.isConfigured() &&
            (media.mediaType == MediaType.MOVIE || hasSavedEp)
        if (canPreFetchStreamInParallel) {
            fetchStreamsForMedia(media, activeSeason, activeEpNum)
        }

        detailJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, isLoadingStream = it.streamsByMedia[media.id].isNullOrEmpty()) }
            secondaryApiClient.fetchDetails(media.sourceDetailPath, media)
                .onSuccess { detail ->
                    val seasonEps = detail.seasonEpisodes[activeSeason]
                        ?: detail.seasonEpisodes.values.firstOrNull().orEmpty()
                    // For long-running Bangla shows (e.g., Gopal Bhar starting at Ep 1221),
                    // snap to the actual first episode number if ep 1 doesn't exist in the season list.
                    val resolvedEpNum = if (seasonEps.isNotEmpty() && seasonEps.none { it.number == activeEpNum }) {
                        seasonEps.first().number
                    } else {
                        activeEpNum
                    }
                    val selectedEpSources = seasonEps.find { it.number == resolvedEpNum }?.sources
                        ?.takeIf { it.isNotEmpty() }
                        ?: detail.initialStreams

                    val updatedItem = detail.updatedItem.copy(
                        episodes = seasonEps,
                        seasonEpisodes = detail.seasonEpisodes
                    )

                    var alreadyHasStreams = false
                    _uiState.update { state ->
                        val nextAll = if (state.allMedia.any { it.id == media.id }) {
                            state.allMedia.map { if (it.id == media.id) updatedItem else it }
                        } else {
                            listOf(updatedItem) + state.allMedia
                        }
                        val existingRelated = state.detailRecommendationsByMedia[media.id].orEmpty()
                        val fallbackSecondaryRecs = state.allMedia
                            .filter { it.contentSource == ContentSource.SECONDARY && it.id != media.id }
                            .take(12)
                        val recs = existingRelated.ifEmpty { fallbackSecondaryRecs }

                        val existingStreams = state.streamsByMedia[media.id].orEmpty()
                        alreadyHasStreams = existingStreams.isNotEmpty() && resolvedEpNum == activeEpNum
                        val finalStreams = if (alreadyHasStreams) existingStreams else selectedEpSources
                        val preferredIdx = choosePreferredStreamIndex(finalStreams, state.preferredQuality)

                        state.copy(
                            isLoadingDetail = false,
                            isLoadingStream = finalStreams.isEmpty(),
                            allMedia = nextAll,
                            selectedEpisodeByMedia = state.selectedEpisodeByMedia + (media.id to resolvedEpNum),
                            seasonsCountByMedia = state.seasonsCountByMedia + (media.id to detail.seasonsCount),
                            seasonEpisodesByMedia = if (detail.seasonEpisodes.isNotEmpty()) {
                                state.seasonEpisodesByMedia + (media.id to detail.seasonEpisodes)
                            } else {
                                state.seasonEpisodesByMedia
                            },
                            streamsByMedia = if (finalStreams.isNotEmpty()) {
                                state.streamsByMedia + (media.id to finalStreams)
                            } else {
                                state.streamsByMedia
                            },
                            selectedStreamIndexByMedia = if (alreadyHasStreams) {
                                state.selectedStreamIndexByMedia
                            } else {
                                state.selectedStreamIndexByMedia + (media.id to preferredIdx)
                            },
                            detailRecommendationsByMedia = if (recs.isNotEmpty()) {
                                state.detailRecommendationsByMedia + (media.id to recs)
                            } else {
                                state.detailRecommendationsByMedia
                            }
                        )
                    }

                    // If /details didn't include direct streams and parallel fetch didn't already load them, fetch /stream now
                    if (selectedEpSources.isEmpty() && !alreadyHasStreams) {
                        fetchStreamsForMedia(updatedItem, activeSeason, resolvedEpNum)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingDetail = false) }
                    if (_uiState.value.streamsByMedia[media.id].isNullOrEmpty()) {
                        fetchStreamsForMedia(media, activeSeason, activeEpNum)
                    }
                }
        }

        // Parallel GET /related?title=... strictly from Source 2 (zero mixing with Source 1)
        if (secondaryApiClient.isConfigured()) {
            viewModelScope.launch {
                secondaryApiClient.fetchRelated(media.title).onSuccess { related ->
                    if (related.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                allMedia = interleaveSources5050((state.allMedia + related).distinctBy { it.id }),
                                detailRecommendationsByMedia = state.detailRecommendationsByMedia + (media.id to related)
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectDubVersion(media: MediaItem, dub: DubVersion) {
        _uiState.update { state ->
            state.copy(
                selectedLanguageByMedia = state.selectedLanguageByMedia + (media.id to dub.language),
                snackMessage = "Loading ${dub.language} stream..."
            )
        }
        val season = _uiState.value.selectedSeasonByMedia[media.id] ?: 1
        val episode = _uiState.value.selectedEpisodeByMedia[media.id] ?: 1
        fetchStreamsForMedia(media.copy(title = dub.title), season, episode)
    }

    fun openStaffProfile(staff: StaffMember) {
        staffJob?.cancel()
        _uiState.update {
            it.copy(
                activeStaffMember = staff,
                staffMovies = emptyList(),
                isLoadingStaff = apiClient.isApiKeyConfigured()
            )
        }
        if (!apiClient.isApiKeyConfigured()) return
        staffJob = viewModelScope.launch {
            apiClient.fetchStaffMovies(staff.id, page = 1)
                .onSuccess { movies ->
                    _uiState.update { state ->
                        state.copy(
                            staffMovies = movies,
                            allMedia = (state.allMedia + movies).distinctBy { it.id },
                            isLoadingStaff = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingStaff = false) }
                }
        }
    }

    fun closeStaffProfile() {
        staffJob?.cancel()
        _uiState.update { it.copy(activeStaffMember = null, staffMovies = emptyList(), isLoadingStaff = false) }
    }

    fun openLiveMatch(match: LiveMatchItem) {
        if (match.streamUrl.isNotBlank()) {
            _uiState.update { it.copy(activeLiveMatch = match) }
        } else {
            toggleLiveMatchNotify(match.id)
            _uiState.update { it.copy(snackMessage = "Match reminder updated for ${match.homeTeam} vs ${match.awayTeam}") }
        }
    }

    fun closeLiveMatch() {
        _uiState.update { it.copy(activeLiveMatch = null) }
    }

    fun fetchStreamsForMedia(media: MediaItem, season: Int = 1, episode: Int = 1) {
        // Strict Source Isolation: Source 2 items use ONLY Source 2 embedded sources or Source 2 /stream endpoint
        if (media.contentSource == ContentSource.SECONDARY) {
            val state = _uiState.value
            val seasonMap = state.seasonEpisodesByMedia[media.id] ?: media.seasonEpisodes
            val seasonEps = seasonMap[season] ?: media.episodes
            val matchedEp = seasonEps.find { it.number == episode }
            val embeddedStreams = matchedEp?.sources?.takeIf { it.isNotEmpty() }
                ?: matchedEp?.streamUrl?.takeIf { it.startsWith("http") }?.let { url ->
                    listOf(
                        StreamResource(
                            id = "${media.id}_s${season}_e${episode}",
                            label = "${media.title} [HD]",
                            language = media.languageBadge ?: "Original",
                            quality = if (
                                url.contains(".m3u8", true) ||
                                url.contains("m3u8", true) ||
                                url.contains("/api/hls/", true)
                            ) "Adaptive HLS" else "Direct MP4",
                            uploader = "CinePulse HD",
                            streamUrl = url
                        )
                    )
                }
            if (!embeddedStreams.isNullOrEmpty()) {
                _uiState.update { current ->
                    val preferredIdx = choosePreferredStreamIndex(embeddedStreams, current.preferredQuality)
                    current.copy(
                        isLoadingStream = false,
                        streamsByMedia = current.streamsByMedia + (media.id to embeddedStreams),
                        selectedStreamIndexByMedia = current.selectedStreamIndexByMedia + (media.id to preferredIdx)
                    )
                }
                return
            }

            if (!secondaryApiClient.isConfigured()) {
                _uiState.update { it.copy(isLoadingStream = false) }
                return
            }

            val isSeries = media.mediaType != MediaType.MOVIE || media.episodes.isNotEmpty() || seasonMap.isNotEmpty() || season > 1 || episode > 1
            streamJob?.cancel()
            streamJob = viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isLoadingStream = true,
                        streamErrorByMedia = it.streamErrorByMedia - media.id
                    )
                }
                val secondaryResult = secondaryApiClient.fetchStreams(
                    detailPath = media.sourceDetailPath,
                    title = media.title,
                    season = season,
                    episode = episode,
                    isSeries = isSeries,
                    fallbackLanguage = media.languageBadge ?: "Original"
                )
                val secondaryStreams = secondaryResult.getOrNull().orEmpty()
                if (secondaryStreams.isNotEmpty()) {
                    _uiState.update { current ->
                        val preferredIdx = choosePreferredStreamIndex(secondaryStreams, current.preferredQuality)
                        current.copy(
                            isLoadingStream = false,
                            streamsByMedia = current.streamsByMedia + (media.id to secondaryStreams),
                            streamErrorByMedia = current.streamErrorByMedia - media.id,
                            selectedStreamIndexByMedia = current.selectedStreamIndexByMedia + (media.id to preferredIdx)
                        )
                    }
                    return@launch
                }

                // If Source 2 returned 0 streams, try primary MovieBox ONLY when title strictly matches
                val fallbackPrimary = if (apiClient.isApiKeyConfigured()) {
                    val matchedPrimary = apiClient.searchMedia(media.title).getOrNull().orEmpty()
                        .firstOrNull { isStrictTitleMatch(media.title, it.title) }
                    if (matchedPrimary != null) {
                        apiClient.fetchStreams(
                            title = matchedPrimary.title,
                            season = season,
                            episode = episode,
                            isSeries = isSeries
                        ).getOrNull().orEmpty()
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                _uiState.update { current ->
                    if (fallbackPrimary.isNotEmpty()) {
                        val preferredIdx = choosePreferredStreamIndex(fallbackPrimary, current.preferredQuality)
                        current.copy(
                            isLoadingStream = false,
                            streamsByMedia = current.streamsByMedia + (media.id to fallbackPrimary),
                            streamErrorByMedia = current.streamErrorByMedia - media.id,
                            selectedStreamIndexByMedia = current.selectedStreamIndexByMedia + (media.id to preferredIdx)
                        )
                    } else {
                        val reason = secondaryResult.exceptionOrNull()?.message
                            ?.takeIf { it.isNotBlank() }
                            ?: "এই ভিডিওর স্ট্রীম লিংক পাওয়া যায়নি"
                        current.copy(
                            isLoadingStream = false,
                            streamErrorByMedia = current.streamErrorByMedia + (media.id to reason)
                        )
                    }
                }
            }
            return
        }

        if (!apiClient.isApiKeyConfigured()) {
            _uiState.update {
                it.copy(
                    isLoadingStream = false,
                    streamErrorByMedia = it.streamErrorByMedia + (media.id to "MOVIEBOX_API_KEY সেট করা নেই")
                )
            }
            return
        }
        val state = _uiState.value
        val hasSeasons = (state.seasonsCountByMedia[media.id] ?: 1) > 1 ||
            state.seasonEpisodesByMedia[media.id]?.isNotEmpty() == true
        val isSeries = media.mediaType != MediaType.MOVIE || media.episodes.isNotEmpty() || hasSeasons || season > 1 || episode > 1
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingStream = true,
                    streamErrorByMedia = it.streamErrorByMedia - media.id
                )
            }
            val primaryResult = apiClient.fetchStreams(
                title = media.title,
                season = season,
                episode = episode,
                isSeries = isSeries
            )
            val primaryStreams = primaryResult.getOrNull().orEmpty()
            val resolvedStreams = if (primaryStreams.isNotEmpty()) {
                primaryStreams
            } else if (secondaryApiClient.isConfigured()) {
                val matchedSecondary = secondaryApiClient.searchMedia(media.title).getOrNull().orEmpty()
                    .firstOrNull { isStrictTitleMatch(media.title, it.title) }
                if (matchedSecondary != null) {
                    secondaryApiClient.fetchStreams(
                        detailPath = matchedSecondary.sourceDetailPath,
                        title = matchedSecondary.title,
                        season = season,
                        episode = episode,
                        isSeries = isSeries,
                        fallbackLanguage = media.languageBadge ?: "Original"
                    ).getOrNull().orEmpty()
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
            _uiState.update { current ->
                if (resolvedStreams.isNotEmpty()) {
                    val preferredIdx = choosePreferredStreamIndex(resolvedStreams, current.preferredQuality)
                    current.copy(
                        isLoadingStream = false,
                        streamsByMedia = current.streamsByMedia + (media.id to resolvedStreams),
                        streamErrorByMedia = current.streamErrorByMedia - media.id,
                        selectedStreamIndexByMedia = current.selectedStreamIndexByMedia + (media.id to preferredIdx)
                    )
                } else {
                    val reason = primaryResult.exceptionOrNull()?.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "এই ভিডিওর স্ট্রীম পাওয়া যায়নি"
                    current.copy(
                        isLoadingStream = false,
                        streamErrorByMedia = current.streamErrorByMedia + (media.id to reason)
                    )
                }
            }
        }
    }

    /**
     * Cross-source automatic playback failover:
     * If every stream from the active source fails inside ExoPlayer (e.g., broken CDN link or
     * upstream proxy outage), automatically fetch playable streams for the same title from the
     * alternate configured source (Source 2 -> Source 1 or Source 1 -> Source 2).
     */
    fun fallbackToAlternateStream(media: MediaItem, season: Int = 1, episode: Int = 1) {
        val state = _uiState.value
        val hasSeasons = (state.seasonsCountByMedia[media.id] ?: 1) > 1 ||
            state.seasonEpisodesByMedia[media.id]?.isNotEmpty() == true
        val isSeries = media.mediaType != MediaType.MOVIE || media.episodes.isNotEmpty() || hasSeasons || season > 1 || episode > 1
        val tryPrimaryBackup = media.contentSource == ContentSource.SECONDARY

        if ((tryPrimaryBackup && !apiClient.isApiKeyConfigured()) ||
            (!tryPrimaryBackup && !secondaryApiClient.isConfigured())
        ) {
            _uiState.update { current ->
                current.copy(
                    isLoadingStream = false,
                    streamErrorByMedia = current.streamErrorByMedia + (
                        media.id to "ভিডিও স্ট্রীম চালু করা যায়নি, আবার চেষ্টা করুন"
                    )
                )
            }
            return
        }

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingStream = true,
                    streamErrorByMedia = it.streamErrorByMedia - media.id
                )
            }
            // Verify the alternate server actually has a closely matching title before fetching
            // streams, preventing fuzzy title searches from swapping to a completely different movie.
            val backupStreams = if (tryPrimaryBackup) {
                val matchedPrimary = apiClient.searchMedia(media.title).getOrNull().orEmpty()
                    .firstOrNull { isStrictTitleMatch(media.title, it.title) }
                if (matchedPrimary != null) {
                    apiClient.fetchStreams(
                        title = matchedPrimary.title,
                        season = season,
                        episode = episode,
                        isSeries = isSeries
                    ).getOrNull().orEmpty()
                } else {
                    emptyList()
                }
            } else {
                val matchedSecondary = secondaryApiClient.searchMedia(media.title).getOrNull().orEmpty()
                    .firstOrNull { isStrictTitleMatch(media.title, it.title) }
                if (matchedSecondary != null) {
                    secondaryApiClient.fetchStreams(
                        detailPath = matchedSecondary.sourceDetailPath,
                        title = matchedSecondary.title,
                        season = season,
                        episode = episode,
                        isSeries = isSeries,
                        fallbackLanguage = media.languageBadge ?: "Original"
                    ).getOrNull().orEmpty()
                } else {
                    emptyList()
                }
            }

            _uiState.update { current ->
                if (backupStreams.isNotEmpty()) {
                    val existingUrls = current.streamsByMedia[media.id].orEmpty().map { it.streamUrl }.toSet()
                    val freshBackup = backupStreams.filterNot { it.streamUrl in existingUrls }.ifEmpty { backupStreams }
                    val preferredIdx = choosePreferredStreamIndex(freshBackup, current.preferredQuality)
                    current.copy(
                        isLoadingStream = false,
                        streamsByMedia = current.streamsByMedia + (media.id to freshBackup),
                        streamErrorByMedia = current.streamErrorByMedia - media.id,
                        selectedStreamIndexByMedia = current.selectedStreamIndexByMedia + (media.id to preferredIdx),
                        snackMessage = if (tryPrimaryBackup) {
                            "ব্যাকআপ সার্ভার (Source 1) থেকে চালু হচ্ছে..."
                        } else {
                            "ব্যাকআপ সার্ভার (Source 2) থেকে চালু হচ্ছে..."
                        }
                    )
                } else {
                    current.copy(
                        isLoadingStream = false,
                        streamErrorByMedia = current.streamErrorByMedia + (
                            media.id to "ভিডিও স্ট্রীম পাওয়া যায়নি, আবার চেষ্টা করুন"
                        )
                    )
                }
            }
        }
    }

    private fun isStrictTitleMatch(requestedTitle: String, candidateTitle: String): Boolean {
        fun normalize(raw: String): String = raw
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("\\[[^]]*]"), " ")
            .replace(Regex("(?i)\\b(season|s0?\\d{1,2}|ep\\s*\\d+|hindi|bengali|bangla|dubbed|dual audio|4k|1080p|720p|hd)\\b"), " ")
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val a = normalize(requestedTitle)
        val b = normalize(candidateTitle)
        if (a.isBlank() || b.isBlank()) return false
        if (a == b) return true
        val tokensA = a.split(' ').filter { it.length >= 2 }.toSet()
        val tokensB = b.split(' ').filter { it.length >= 2 }.toSet()
        if (tokensA.isEmpty() || tokensB.isEmpty()) return false
        val overlap = tokensA.intersect(tokensB).size
        val minSize = minOf(tokensA.size, tokensB.size)
        val maxSize = maxOf(tokensA.size, tokensB.size)
        return overlap == minSize && (maxSize - minSize) <= 1
    }

    internal fun choosePreferredStreamIndex(streams: List<StreamResource>, preferredQuality: String): Int {
        if (streams.isEmpty()) return 0
        // If any stream is a direct CDN URL (not proxied through Render), prefer those first
        // for dramatically faster startup and zero proxy throttling.
        val directIndices = streams.indices.filter { !streams[it].streamUrl.contains(".onrender.com", ignoreCase = true) }
        val candidateIndices = if (directIndices.isNotEmpty()) directIndices else streams.indices.toList()
        val allProxiedOnRender = directIndices.isEmpty()

        val desiredTokens = when {
            preferredQuality.contains("4K", ignoreCase = true) -> listOf("4K", "2160", "1080", "720")
            preferredQuality.contains("720", ignoreCase = true) -> listOf("720", "480", "1080")
            // When every stream is proxied through Render, start on smooth 720p/MP4 instead of
            // heavy 1080p HLS proxy segments (1080p stays available in the Quality picker).
            allProxiedOnRender -> listOf("720", "MP4", "480", "HD", "1080")
            else -> listOf("1080", "720", "4K", "2160")
        }
        for (token in desiredTokens) {
            val match = candidateIndices.firstOrNull { idx ->
                streams[idx].quality.contains(token, ignoreCase = true)
            }
            if (match != null) return match
        }
        return candidateIndices.first()
    }

    fun openShortReelFullDrama(reel: ShortReelItem): String {
        val existing = getMediaById(reel.linkedMediaId)
            ?: _uiState.value.allMedia.find { it.title.equals(reel.title, ignoreCase = true) }
        val preResolvedStreams = _uiState.value.shortStreamsByReelId[reel.id].orEmpty()
        if (existing != null) {
            if (preResolvedStreams.isNotEmpty() && _uiState.value.streamsByMedia[existing.id].isNullOrEmpty()) {
                _uiState.update {
                    it.copy(streamsByMedia = it.streamsByMedia + (existing.id to preResolvedStreams))
                }
            }
            openMediaDetail(existing)
            return existing.id
        }
        val synthesized = MediaItem(
            id = reel.linkedMediaId.ifBlank { "short_${reel.id}" },
            title = reel.title,
            year = 2025,
            country = "Global",
            genre = "Short Drama",
            rating = 8.2,
            duration = reel.episodeTag.ifBlank { "Short Series" },
            synopsis = reel.description.ifBlank { reel.title },
            mediaType = MediaType.SHORT_TV,
            languageBadge = null,
            subCategory = "Short Drama",
            sectionTags = listOf("hot_short"),
            poster = reel.poster,
            backdropRes = reel.poster.fallbackRes,
            backdropUrl = reel.poster.remoteUrl,
            resources = when {
                preResolvedStreams.isNotEmpty() -> preResolvedStreams
                reel.streamUrl.isNotBlank() -> listOf(
                    StreamResource(
                        id = "short_src_${reel.id}",
                        label = reel.title,
                        language = "Original",
                        quality = "HD",
                        uploader = "Muvio Shorts",
                        streamUrl = reel.streamUrl
                    )
                )
                else -> emptyList()
            }
        )
        _uiState.update { state ->
            state.copy(
                allMedia = (listOf(synthesized) + state.allMedia).distinctBy { it.id },
                streamsByMedia = if (preResolvedStreams.isNotEmpty()) {
                    state.streamsByMedia + (synthesized.id to preResolvedStreams)
                } else {
                    state.streamsByMedia
                }
            )
        }
        openMediaDetail(synthesized)
        return synthesized.id
    }

    fun openDownloadedMedia(downloadIdOrMediaId: String): String? {
        val dl = _uiState.value.downloads.find { it.downloadId == downloadIdOrMediaId || it.mediaId == downloadIdOrMediaId }
        val resolvedMediaId = dl?.mediaId ?: downloadIdOrMediaId
        val existing = getMediaById(resolvedMediaId)
        val targetMedia = existing ?: dl?.let {
            MediaItem(
                id = it.mediaId,
                title = it.title,
                year = 2025,
                country = "Offline",
                genre = "Downloaded",
                rating = 8.0,
                duration = it.quality,
                synopsis = "Offline saved title.",
                mediaType = if (it.episodeLabel.isNotBlank()) MediaType.SERIES else MediaType.MOVIE,
                languageBadge = null,
                subCategory = "Downloads",
                sectionTags = emptyList(),
                poster = it.poster,
                backdropRes = it.poster.fallbackRes,
                backdropUrl = it.poster.remoteUrl
            )
        } ?: return null

        if (existing == null) {
            _uiState.update { state ->
                state.copy(allMedia = (listOf(targetMedia) + state.allMedia).distinctBy { it.id })
            }
        }

        // If a completed offline file exists on disk, play directly from local storage
        val localPath = dl?.localFilePath
        if (dl?.status == DownloadStatus.COMPLETED && !localPath.isNullOrBlank() && java.io.File(localPath).exists()) {
            val offlineStream = StreamResource(
                id = "offline_${dl.downloadId}",
                label = "${targetMedia.title} ${dl.episodeLabel} (Offline)".trim(),
                language = "Offline",
                quality = dl.quality,
                uploader = "Local Storage",
                streamUrl = android.net.Uri.fromFile(java.io.File(localPath)).toString()
            )
            recordWatchHistory(targetMedia.id)
            _uiState.update { state ->
                state.copy(
                    isLoadingStream = false,
                    selectedSeasonByMedia = state.selectedSeasonByMedia + (targetMedia.id to dl.season),
                    selectedEpisodeByMedia = state.selectedEpisodeByMedia + (targetMedia.id to dl.episode),
                    streamsByMedia = state.streamsByMedia + (targetMedia.id to listOf(offlineStream)),
                    streamErrorByMedia = state.streamErrorByMedia - targetMedia.id,
                    selectedStreamIndexByMedia = state.selectedStreamIndexByMedia + (targetMedia.id to 0)
                )
            }
            return targetMedia.id
        }

        openMediaDetail(targetMedia)
        return targetMedia.id
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab, isSearchActive = false) }
    }

    fun selectMeSection(index: Int) {
        _uiState.update { it.copy(meSectionIndex = index.coerceIn(0, 2)) }
    }

    fun openWatchlistShortcut() {
        _uiState.update {
            it.copy(
                selectedTab = MainTab.ME,
                meSectionIndex = 0,
                isSearchActive = false
            )
        }
    }

    fun selectTopTab(tabName: String) {
        _uiState.update { it.copy(selectedTopTab = tabName) }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery,
                searchSourceFilter = if (!active) null else it.searchSourceFilter,
                remoteSearchResults = if (!active) emptyList() else it.remoteSearchResults,
                searchSuggestions = if (!active) emptyList() else it.searchSuggestions
            )
        }
    }

    fun selectSearchSourceFilter(source: ContentSource?) {
        _uiState.update { it.copy(searchSourceFilter = source) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        suggestJob?.cancel()
        val trimmed = query.trim()
        val primaryConfigured = apiClient.isApiKeyConfigured()
        val secondaryConfigured = secondaryApiClient.isConfigured()
        if (trimmed.length < 2 || (!primaryConfigured && !secondaryConfigured)) {
            _uiState.update {
                it.copy(
                    isSearchingRemote = false,
                    remoteSearchResults = emptyList(),
                    searchSuggestions = emptyList()
                )
            }
            return
        }

        // Fast autocomplete from BOTH Source 1 (/suggest) and Source 2 (/api/suggest)
        suggestJob = viewModelScope.launch {
            delay(200)
            coroutineScope {
                if (primaryConfigured) {
                    launch {
                        apiClient.fetchSuggestions(trimmed).onSuccess { suggestions ->
                            _uiState.update { state ->
                                state.copy(searchSuggestions = (suggestions + state.searchSuggestions).distinct().take(8))
                            }
                        }
                    }
                }
                if (secondaryConfigured) {
                    launch {
                        secondaryApiClient.fetchSuggestions(trimmed).onSuccess { res ->
                            _uiState.update { state ->
                                val nextSuggestions = (state.searchSuggestions + res.suggestions).distinct().take(8)
                                val nextRemote = if (res.items.isNotEmpty()) {
                                    interleaveSources5050((state.remoteSearchResults + res.items).distinctBy { it.id })
                                } else {
                                    state.remoteSearchResults
                                }
                                val nextAll = if (res.items.isNotEmpty()) {
                                    interleaveSources5050((state.allMedia + res.items).distinctBy { it.id })
                                } else {
                                    state.allMedia
                                }
                                state.copy(
                                    searchSuggestions = nextSuggestions,
                                    remoteSearchResults = nextRemote,
                                    allMedia = nextAll,
                                    secondarySourceError = null
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dual-source parallel search: emit results incrementally & blend 50/50 without blocking
        searchJob = viewModelScope.launch {
            delay(320)
            _uiState.update { it.copy(isSearchingRemote = true) }
            var primaryResults: List<MediaItem> = emptyList()
            var secondaryResults: List<MediaItem> = emptyList()
            var primaryDone = !primaryConfigured
            var secondaryDone = !secondaryConfigured

            coroutineScope {
                if (primaryConfigured) {
                    launch {
                        apiClient.searchMedia(trimmed)
                            .onSuccess { list -> primaryResults = list }
                        primaryDone = true
                        val blended = interleaveTwoLists(primaryResults, secondaryResults)
                        _uiState.update { state ->
                            val mergedAll = interleaveSources5050((state.allMedia + blended).distinctBy { it.id })
                            state.copy(
                                isSearchingRemote = !(primaryDone && secondaryDone),
                                remoteSearchResults = blended,
                                allMedia = mergedAll
                            )
                        }
                    }
                }
                if (secondaryConfigured) {
                    launch {
                        secondaryApiClient.searchMedia(trimmed)
                            .onSuccess { list ->
                                secondaryResults = list
                                _uiState.update { it.copy(secondarySourceError = null) }
                            }
                            .onFailure { err ->
                                _uiState.update {
                                    it.copy(secondarySourceError = err.message?.take(120) ?: "Source 2 search failed")
                                }
                            }
                        secondaryDone = true
                        val blended = interleaveTwoLists(primaryResults, secondaryResults)
                        _uiState.update { state ->
                            val mergedAll = interleaveSources5050((state.allMedia + blended).distinctBy { it.id })
                            state.copy(
                                isSearchingRemote = !(primaryDone && secondaryDone),
                                remoteSearchResults = blended,
                                allMedia = mergedAll
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectSeriesFilter(filter: String) {
        _uiState.update { it.copy(selectedSeriesFilter = filter) }
    }

    fun selectMovieFilter(filter: String) {
        _uiState.update { it.copy(selectedMovieFilter = filter) }
    }

    fun applyCategoryBanner(filterKey: String) {
        val targetTopTab = when (filterKey) {
            "Anime" -> "Anime"
            "Bengali" -> "Bengali"
            "Movies" -> "Cinema"
            else -> "Trending"
        }
        _uiState.update { it.copy(selectedTopTab = targetTopTab) }
    }

    fun getMediaById(mediaId: String): MediaItem? =
        _uiState.value.allMedia.find { it.id == mediaId }

    fun recordWatchHistory(mediaId: String) {
        _uiState.update { state ->
            val updated = (listOf(mediaId) + state.watchHistoryIds.filterNot { it == mediaId }).take(30)
            state.copy(watchHistoryIds = updated)
        }
        val store = preferencesStore ?: return
        val media = getMediaById(mediaId) ?: return
        val state = _uiState.value
        val progress = state.watchProgressByMedia[mediaId] ?: 0f
        val season = state.selectedSeasonByMedia[mediaId] ?: 1
        val episode = state.selectedEpisodeByMedia[mediaId] ?: 1
        val epKey = "${mediaId}_s${season}_e${episode}"
        val positionMs = state.watchPositionMsByEpisode[epKey] ?: 0L
        viewModelScope.launch {
            store.upsertHistory(media, progress, season, episode, positionMs)
        }
    }

    fun toggleWatchlist(mediaId: String) {
        var added = false
        _uiState.update { state ->
            val next = if (mediaId in state.watchlistIds) {
                added = false
                state.watchlistIds - mediaId
            } else {
                added = true
                state.watchlistIds + mediaId
            }
            val msg = if (added) "Added to My List" else "Removed from My List"
            state.copy(watchlistIds = next, snackMessage = msg)
        }
        val store = preferencesStore ?: return
        viewModelScope.launch {
            if (added) {
                val media = getMediaById(mediaId)
                if (media != null) store.upsertWatchlist(media)
            } else {
                store.removeWatchlist(mediaId)
            }
        }
    }

    fun selectLanguage(mediaId: String, language: String) {
        _uiState.update { state ->
            val streams = state.streamsByMedia[mediaId]
                ?: state.allMedia.find { it.id == mediaId }?.resources.orEmpty()
            val matchingIdx = streams.indexOfFirst { it.language.equals(language, ignoreCase = true) }
            val nextStreamMap = if (matchingIdx >= 0) {
                state.selectedStreamIndexByMedia + (mediaId to matchingIdx)
            } else {
                state.selectedStreamIndexByMedia
            }
            state.copy(
                selectedLanguageByMedia = state.selectedLanguageByMedia + (mediaId to language),
                selectedStreamIndexByMedia = nextStreamMap
            )
        }
    }

    fun selectStreamIndex(mediaId: String, index: Int) {
        _uiState.update { state ->
            state.copy(selectedStreamIndexByMedia = state.selectedStreamIndexByMedia + (mediaId to index))
        }
    }

    fun selectSeason(media: MediaItem, season: Int) {
        _uiState.update { state ->
            val seasonMap = state.seasonEpisodesByMedia[media.id]
                ?: media.seasonEpisodes
            val seasonEps = if (seasonMap.containsKey(season)) {
                seasonMap[season].orEmpty()
            } else {
                MovieBoxParsers.generateNumberedEpisodes(12)
            }
            val updatedMap = if (seasonMap.containsKey(season)) seasonMap else (seasonMap + (season to seasonEps))
            val updatedAll = state.allMedia.map { item ->
                if (item.id == media.id) {
                    item.copy(episodes = seasonEps, seasonEpisodes = updatedMap)
                } else {
                    item
                }
            }
            state.copy(
                allMedia = updatedAll,
                seasonEpisodesByMedia = state.seasonEpisodesByMedia + (media.id to updatedMap),
                selectedSeasonByMedia = state.selectedSeasonByMedia + (media.id to season),
                selectedEpisodeByMedia = state.selectedEpisodeByMedia + (media.id to 1),
                streamsByMedia = state.streamsByMedia - media.id,
                streamErrorByMedia = state.streamErrorByMedia - media.id,
                selectedStreamIndexByMedia = state.selectedStreamIndexByMedia + (media.id to 0)
            )
        }
        fetchStreamsForMedia(media, season, 1)
    }

    fun selectEpisode(media: MediaItem, episodeNumber: Int) {
        val season = _uiState.value.selectedSeasonByMedia[media.id] ?: 1
        _uiState.update { state ->
            state.copy(
                selectedEpisodeByMedia = state.selectedEpisodeByMedia + (media.id to episodeNumber),
                streamsByMedia = state.streamsByMedia - media.id,
                streamErrorByMedia = state.streamErrorByMedia - media.id,
                selectedStreamIndexByMedia = state.selectedStreamIndexByMedia + (media.id to 0)
            )
        }
        fetchStreamsForMedia(media, season, episodeNumber)
    }

    fun loadMoreEpisodesForCurrentSeason(media: MediaItem) {
        val season = _uiState.value.selectedSeasonByMedia[media.id] ?: 1
        _uiState.update { state ->
            val seasonMap = state.seasonEpisodesByMedia[media.id] ?: media.seasonEpisodes
            val currentList = seasonMap[season] ?: media.episodes
            val currentCount = currentList.size
            if (currentCount >= 500) return@update state
            val nextCount = (currentCount + 24).coerceAtMost(500)
            val expanded = currentList + ((currentCount + 1)..nextCount).map { num ->
                EpisodeItem(num, "Episode $num", "HD")
            }
            val updatedMap = seasonMap + (season to expanded)
            val updatedAll = state.allMedia.map { item ->
                if (item.id == media.id) {
                    item.copy(episodes = expanded, seasonEpisodes = updatedMap)
                } else {
                    item
                }
            }
            state.copy(
                allMedia = updatedAll,
                seasonEpisodesByMedia = state.seasonEpisodesByMedia + (media.id to updatedMap)
            )
        }
    }

    fun toggleCommentLike(mediaId: String, commentId: String) {
        var nextLikes = 0
        var nextLiked = false
        _uiState.update { state ->
            val current = state.commentsByMedia[mediaId].orEmpty()
            val updated = current.map { comment ->
                if (comment.id == commentId) {
                    val liked = !comment.isLiked
                    val likesCount = if (liked) comment.likes + 1 else (comment.likes - 1).coerceAtLeast(0)
                    nextLikes = likesCount
                    nextLiked = liked
                    comment.copy(
                        isLiked = liked,
                        likes = likesCount
                    )
                } else {
                    comment
                }
            }
            state.copy(commentsByMedia = state.commentsByMedia + (mediaId to updated))
        }
        val store = preferencesStore ?: return
        viewModelScope.launch {
            store.updateCommentLike(mediaId, commentId, nextLikes, nextLiked)
        }
    }

    fun postComment(mediaId: String, text: String) {
        val trimmed = text.trim().take(500)
        if (trimmed.isEmpty()) return
        val commentId = "c_${System.currentTimeMillis()}"
        val displayAuthor = "You"
        val newComment = CommentItem(
            id = commentId,
            author = displayAuthor,
            avatarInitials = "Y",
            avatarColor = 0xFFE51A38,
            flagEmoji = "\uD83C\uDDE7\uD83C\uDDE9",
            timestamp = "Just now",
            content = trimmed,
            likes = 0,
            repliesCount = 0
        )
        _uiState.update { state ->
            val current = state.commentsByMedia[mediaId].orEmpty()
            state.copy(
                commentsByMedia = state.commentsByMedia + (mediaId to (listOf(newComment) + current)),
                snackMessage = "Comment posted"
            )
        }
        val store = preferencesStore ?: return
        viewModelScope.launch {
            store.addComment(mediaId, newComment)
        }
    }

    fun startDownload(media: MediaItem, qualityOverride: String? = null) {
        val currentState = _uiState.value
        val chosenQuality = qualityOverride ?: currentState.preferredQuality
        val isSeries = media.mediaType != MediaType.MOVIE || media.episodes.isNotEmpty() ||
            (currentState.seasonsCountByMedia[media.id] ?: 1) > 1
        val season = currentState.selectedSeasonByMedia[media.id] ?: 1
        val episode = currentState.selectedEpisodeByMedia[media.id] ?: 1
        val downloadId = if (isSeries) "${media.id}_s${season}_e${episode}" else media.id
        val epLabel = if (isSeries) "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}" else ""
        val displayTitle = if (epLabel.isNotBlank()) "${media.title} • $epLabel" else media.title

        if (currentState.downloads.any { it.downloadId == downloadId && it.status != DownloadStatus.FAILED }) {
            _uiState.update { it.copy(snackMessage = "Already in Downloads") }
            return
        }

        val engine = downloadEngine
        if (engine != null && currentState.wifiOnlyDownloads && !engine.isWifiConnected()) {
            _uiState.update { it.copy(snackMessage = "Wi-Fi Only Downloads enabled. Connect to Wi-Fi or turn it off in Settings.") }
            return
        }

        val availableStreams = currentState.streamsByMedia[media.id].orEmpty().ifEmpty { media.resources }
        val chosenStream = selectDirectDownloadStream(availableStreams, chosenQuality)

        val item = DownloadItem(
            mediaId = media.id,
            title = displayTitle,
            quality = chosenQuality,
            sizeText = if (engine == null) "Queued" else "Finding MP4...",
            progress = 0.02f,
            status = DownloadStatus.DOWNLOADING,
            poster = media.poster,
            streamUrl = chosenStream?.streamUrl.orEmpty(),
            downloadId = downloadId,
            season = season,
            episode = episode,
            episodeLabel = epLabel
        )
        _uiState.update { state ->
            state.copy(
                downloads = listOf(item) + state.downloads.filterNot { it.downloadId == downloadId },
                snackMessage = "Downloading $displayTitle ($chosenQuality)"
            )
        }

        if (engine != null) {
            launchRealDownload(media, downloadId, season, episode, chosenQuality, chosenStream)
        }
    }

    private fun isHlsStream(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") || lower.contains("/api/hls/")
    }

    private fun selectDirectDownloadStream(
        streams: List<StreamResource>,
        chosenQuality: String
    ): StreamResource? {
        val directMp4s = streams.filter { it.streamUrl.isNotBlank() && !isHlsStream(it.streamUrl) }
        if (directMp4s.isEmpty()) return null
        return directMp4s.firstOrNull {
            it.quality.contains(chosenQuality.substringBefore(" "), ignoreCase = true)
        } ?: directMp4s.firstOrNull()
    }

    private fun launchRealDownload(
        media: MediaItem,
        downloadId: String,
        season: Int,
        episode: Int,
        chosenQuality: String,
        initialStream: StreamResource?
    ) {
        val engine = downloadEngine ?: return
        downloadJobs[downloadId]?.cancel()
        downloadJobs[downloadId] = viewModelScope.launch {
            var targetStream = initialStream?.takeIf { it.streamUrl.isNotBlank() && !isHlsStream(it.streamUrl) }
            val isSeries = media.mediaType != MediaType.MOVIE

            // If no direct MP4 yet, query primary & backup source so HLS-only items still find an MP4 if available
            if (targetStream == null) {
                val primaryFirst = media.contentSource != ContentSource.SECONDARY
                if (primaryFirst && apiClient.isApiKeyConfigured()) {
                    val s1 = apiClient.fetchStreams(media.title, season, episode, isSeries).getOrNull().orEmpty()
                    targetStream = selectDirectDownloadStream(s1, chosenQuality)
                } else if (!primaryFirst && secondaryApiClient.isConfigured()) {
                    val s2 = secondaryApiClient.fetchStreams(
                        detailPath = media.sourceDetailPath,
                        title = media.title,
                        season = season,
                        episode = episode,
                        isSeries = isSeries,
                        fallbackLanguage = media.languageBadge ?: "Original"
                    ).getOrNull().orEmpty()
                    targetStream = selectDirectDownloadStream(s2, chosenQuality)
                }

                // Cross-source fallback for MP4 download
                if (targetStream == null) {
                    if (primaryFirst && secondaryApiClient.isConfigured()) {
                        val s2Backup = secondaryApiClient.fetchStreams(
                            detailPath = "",
                            title = media.title,
                            season = season,
                            episode = episode,
                            isSeries = isSeries,
                            fallbackLanguage = media.languageBadge ?: "Original"
                        ).getOrNull().orEmpty()
                        targetStream = selectDirectDownloadStream(s2Backup, chosenQuality)
                    } else if (!primaryFirst && apiClient.isApiKeyConfigured()) {
                        val s1Backup = apiClient.fetchStreams(media.title, season, episode, isSeries).getOrNull().orEmpty()
                        targetStream = selectDirectDownloadStream(s1Backup, chosenQuality)
                    }
                }
            }

            if (targetStream == null || targetStream.streamUrl.isBlank()) {
                updateDownloadState(downloadId) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        sizeText = "শুধুমাত্র অনলাইন স্ট্রিমিং (HLS) পাওয়া গেছে, অফলাইন MP4 নেই",
                        progress = 0f
                    )
                }
                _uiState.update {
                    it.copy(snackMessage = "এই ভিডিওটি শুধুমাত্র অনলাইন স্ট্রিমিং (HLS) সাপোর্ট করে, অফলাইন MP4 পাওয়া যায়নি।")
                }
                return@launch
            }

            updateDownloadState(downloadId) { it.copy(streamUrl = targetStream.streamUrl, sizeText = "Connecting...") }

            val result = engine.downloadVideo(
                downloadId = downloadId,
                streamUrl = targetStream.streamUrl,
                headers = targetStream.headers
            ) { frac, sizeLabel ->
                updateDownloadState(downloadId) {
                    if (it.status == DownloadStatus.DOWNLOADING) {
                        it.copy(progress = frac, sizeText = sizeLabel)
                    } else {
                        it
                    }
                }
            }

            result.onSuccess { file ->
                updateDownloadState(downloadId) {
                    it.copy(
                        progress = 1f,
                        status = DownloadStatus.COMPLETED,
                        localFilePath = file.absolutePath
                    )
                }
                engine.saveDownloadsManifest(_uiState.value.downloads)
                refreshStorageStats()
                _uiState.update { it.copy(snackMessage = "${media.title} ready offline") }
            }.onFailure { err ->
                if (err is kotlinx.coroutines.CancellationException) throw err
                updateDownloadState(downloadId) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        sizeText = err.message?.take(48) ?: "Download failed"
                    )
                }
            }
        }
    }

    private fun updateDownloadState(downloadId: String, transform: (DownloadItem) -> DownloadItem) {
        _uiState.update { state ->
            state.copy(
                downloads = state.downloads.map { dl ->
                    if (dl.downloadId == downloadId || dl.mediaId == downloadId) transform(dl) else dl
                }
            )
        }
    }

    fun toggleDownloadPause(downloadId: String) {
        val existing = _uiState.value.downloads.find { it.downloadId == downloadId || it.mediaId == downloadId } ?: return
        if (existing.status == DownloadStatus.COMPLETED) return
        val key = existing.downloadId

        if (existing.status == DownloadStatus.DOWNLOADING) {
            downloadJobs[key]?.cancel()
            updateDownloadState(key) { it.copy(status = DownloadStatus.PAUSED, sizeText = "Paused") }
        } else {
            val engine = downloadEngine
            if (engine != null && _uiState.value.wifiOnlyDownloads && !engine.isWifiConnected()) {
                _uiState.update { it.copy(snackMessage = "Connect to Wi-Fi to resume download") }
                return
            }
            updateDownloadState(key) { it.copy(status = DownloadStatus.DOWNLOADING, sizeText = "Resuming...") }
            val media = getMediaById(existing.mediaId)
            if (media != null && engine != null) {
                val stream = _uiState.value.streamsByMedia[existing.mediaId].orEmpty()
                    .firstOrNull { it.streamUrl == existing.streamUrl }
                    ?: existing.streamUrl.takeIf { it.isNotBlank() }?.let { url ->
                        StreamResource("dl_$key", existing.title, "Original", existing.quality, "Muvio", url)
                    }
                launchRealDownload(media, key, existing.season, existing.episode, existing.quality, stream)
            }
        }
    }

    fun removeDownload(downloadId: String) {
        val existing = _uiState.value.downloads.find { it.downloadId == downloadId || it.mediaId == downloadId }
        val key = existing?.downloadId ?: downloadId
        downloadJobs[key]?.cancel()
        downloadJobs.remove(key)
        downloadEngine?.deleteFile(key)
        _uiState.update { state ->
            state.copy(downloads = state.downloads.filterNot { it.downloadId == key || it.mediaId == downloadId })
        }
        downloadEngine?.let { engine ->
            viewModelScope.launch {
                engine.saveDownloadsManifest(_uiState.value.downloads)
                refreshStorageStats()
            }
        }
    }

    fun toggleLiveMatchNotify(matchId: String) {
        _uiState.update { state ->
            state.copy(
                liveMatches = state.liveMatches.map { match ->
                    if (match.id == matchId) match.copy(isNotified = !match.isNotified) else match
                }
            )
        }
    }

    fun toggleShortLike(reelId: String) {
        _uiState.update { state ->
            state.copy(
                shortReels = state.shortReels.map { reel ->
                    if (reel.id == reelId) {
                        val liked = !reel.isLiked
                        reel.copy(
                            isLiked = liked,
                            likesCount = if (liked) reel.likesCount + 1 else reel.likesCount - 1
                        )
                    } else {
                        reel
                    }
                }
            )
        }
    }

    fun toggleShortSave(reelId: String) {
        _uiState.update { state ->
            state.copy(
                shortReels = state.shortReels.map { reel ->
                    if (reel.id == reelId) reel.copy(isSaved = !reel.isSaved) else reel
                }
            )
        }
    }

    fun toggleAutoPlayNext() {
        val next = !_uiState.value.autoPlayNext
        _uiState.update { it.copy(autoPlayNext = next) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setAutoPlayNext(next) }
        }
    }

    fun toggleWifiOnlyDownloads() {
        val next = !_uiState.value.wifiOnlyDownloads
        _uiState.update { it.copy(wifiOnlyDownloads = next) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setWifiOnlyDownloads(next) }
        }
    }

    fun cyclePreferredQuality() {
        val next = when (_uiState.value.preferredQuality) {
            "1080p HD" -> "4K HDR"
            "4K HDR" -> "720p HD"
            else -> "1080p HD"
        }
        _uiState.update { it.copy(preferredQuality = next) }
        preferencesStore?.let { store ->
            viewModelScope.launch { store.setPreferredQuality(next) }
        }
    }

    fun selectMood(mood: String) {
        _uiState.update { it.copy(selectedMood = mood) }
    }

    fun pickSurpriseMedia() {
        val state = _uiState.value
        val pool = moodFilteredMedia(state.allMedia, state.selectedMood).ifEmpty { state.allMedia }
        if (pool.isEmpty()) return
        val currentId = state.surprisePick?.id
        val candidates = pool.filter { it.id != currentId }.ifEmpty { pool }
        val chosen = candidates.random()
        _uiState.update { it.copy(surprisePick = chosen) }
    }

    fun dismissSurprisePick() {
        _uiState.update { it.copy(surprisePick = null) }
    }

    fun updateWatchProgress(
        mediaId: String,
        progress: Float,
        positionMs: Long = 0L,
        durationMs: Long = 0L
    ) {
        val clamped = progress.coerceIn(0f, 1f)
        val season = _uiState.value.selectedSeasonByMedia[mediaId] ?: 1
        val episode = _uiState.value.selectedEpisodeByMedia[mediaId] ?: 1
        val epKey = "${mediaId}_s${season}_e${episode}"
        val isNearEnd = durationMs > 0L && (positionMs >= durationMs - 30_000L || clamped >= 0.94f)
        val resumablePosMs = if (!isNearEnd && positionMs >= 45_000L) positionMs else 0L

        _uiState.update { state ->
            val updatedPositions = if (resumablePosMs > 0L) {
                state.watchPositionMsByEpisode + (epKey to resumablePosMs)
            } else {
                state.watchPositionMsByEpisode - epKey
            }
            state.copy(
                watchProgressByMedia = state.watchProgressByMedia + (mediaId to clamped),
                watchPositionMsByEpisode = updatedPositions
            )
        }
        val store = preferencesStore ?: return
        val lastSynced = lastSyncedProgressByMedia[mediaId]
        val shouldSync = lastSynced == null ||
            kotlin.math.abs(clamped - lastSynced) >= 0.03f ||
            isNearEnd ||
            (resumablePosMs >= 45_000L && lastSynced == 0f)
        if (shouldSync) {
            lastSyncedProgressByMedia[mediaId] = clamped
            val media = getMediaById(mediaId) ?: return
            viewModelScope.launch {
                store.upsertHistory(media, clamped, season, episode, resumablePosMs)
            }
        }
    }

    fun cycleSleepTimer() {
        _uiState.update { state ->
            val next = when (state.sleepTimerMinutes) {
                0 -> 15
                15 -> 30
                30 -> 60
                else -> 0
            }
            val msg = if (next == 0) "Sleep Timer Off" else "Sleep Timer set for $next min"
            state.copy(sleepTimerMinutes = next, snackMessage = msg)
        }
    }

    fun resetSleepTimer(showNotice: Boolean = true) {
        _uiState.update { state ->
            state.copy(
                sleepTimerMinutes = 0,
                snackMessage = if (showNotice) "Sleep timer paused playback" else state.snackMessage
            )
        }
    }

    fun moodFilteredMedia(source: List<MediaItem>, mood: String): List<MediaItem> {
        return when (mood) {
            "Late Night Thriller" -> source.filter {
                it.genre.contains("Thriller", true) || it.genre.contains("Crime", true) || it.genre.contains("Mystery", true) || it.genre.contains("Horror", true)
            }
            "Comfort Drama" -> source.filter {
                it.genre.contains("Romance", true) || it.genre.contains("Drama", true) || it.genre.contains("Comedy", true)
            }
            "Anime Marathon" -> source.filter {
                it.mediaType == MediaType.ANIME || "anime" in it.sectionTags
            }
            "Bengali Gems" -> source.filter {
                it.languageBadge.equals("Bengali", true) || "bengali" in it.sectionTags
            }
            "Action Rush" -> source.filter {
                it.genre.contains("Action", true) || it.genre.contains("Sci-Fi", true) || it.genre.contains("Adventure", true)
            }
            else -> source
        }
    }

    fun continueWatchingItems(): List<Pair<MediaItem, Float>> {
        val state = _uiState.value
        val byId = state.allMedia.associateBy { it.id }
        return state.watchHistoryIds.mapNotNull { id ->
            val media = byId[id] ?: return@mapNotNull null
            val progress = state.watchProgressByMedia[id] ?: 0f
            media to progress
        }.take(10)
    }

    fun clearSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun filteredTrendingSeries(): List<MediaItem> {
        val state = _uiState.value
        if (state.selectedTopTab == "Movies") return emptyList()
        val tabBase = when (state.selectedTopTab) {
            "Anime" -> state.allMedia.filter { it.mediaType == MediaType.ANIME || "anime" in it.sectionTags }
            "Bengali" -> state.allMedia.filter {
                (it.mediaType == MediaType.SERIES || it.mediaType == MediaType.DRAMA || it.mediaType == MediaType.ANIME) &&
                    (it.languageBadge.equals("Bengali", true) || "bengali" in it.sectionTags)
            }
            else -> state.allMedia.filter {
                it.mediaType == MediaType.SERIES || it.mediaType == MediaType.DRAMA || it.mediaType == MediaType.ANIME
            }
        }
        val moodBase = if (state.selectedMood == "All") tabBase else moodFilteredMedia(tabBase, state.selectedMood)
        val base = interleaveSources5050(moodBase)
        return when (state.selectedSeriesFilter) {
            "Anime" -> interleaveSources5050(base.filter { it.mediaType == MediaType.ANIME || "anime" in it.sectionTags })
            "Asian Drama" -> interleaveSources5050(base.filter { it.country in listOf("South Korea", "China", "Japan") && it.mediaType != MediaType.ANIME })
            "Indian Drama" -> interleaveSources5050(base.filter { it.country in listOf("India", "Bangladesh") })
            else -> base
        }
    }

    fun filteredTrendingMovies(): List<MediaItem> {
        val state = _uiState.value
        if (state.selectedTopTab == "Series" || state.selectedTopTab == "Anime") return emptyList()
        val tabBase = when (state.selectedTopTab) {
            "Bengali" -> state.allMedia.filter {
                (it.mediaType == MediaType.MOVIE || it.subCategory == "New Bengali") &&
                    (it.languageBadge.equals("Bengali", true) || "bengali" in it.sectionTags)
            }
            else -> state.allMedia.filter {
                it.mediaType == MediaType.MOVIE || it.subCategory == "New Bengali"
            }
        }
        val moodBase = if (state.selectedMood == "All") tabBase else moodFilteredMedia(tabBase, state.selectedMood)
        val base = interleaveSources5050(moodBase)
        return when (state.selectedMovieFilter) {
            "TOP Movies" -> interleaveSources5050(base.sortedByDescending { it.rating })
            "New Bengali" -> interleaveSources5050(
                base.filter { it.languageBadge.equals("Bengali", true) || "bengali" in it.sectionTags }
            )
            "Bollywood" -> interleaveSources5050(base.filter { it.country == "India" })
            else -> base
        }
    }
}
