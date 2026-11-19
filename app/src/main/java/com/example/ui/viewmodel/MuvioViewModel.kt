package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiKeyMissingException
import com.example.data.MovieBoxApiClient
import com.example.data.MovieBoxParsers
import com.example.data.MuvioCatalogRepository
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
    val novels: List<NovelItem> = emptyList(),
    val watchlistIds: Set<String> = emptySet(),
    val watchHistoryIds: List<String> = emptyList(),
    val watchProgressByMedia: Map<String, Float> = emptyMap(),
    val selectedMood: String = "All",
    val surprisePick: MediaItem? = null,
    val sleepTimerMinutes: Int = 0,
    val meSectionIndex: Int = 0, // 0 = Watchlist, 1 = History, 2 = Settings
    val downloads: List<DownloadItem> = emptyList(),
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
    val apiStatusMessage: String = "Connecting...",
    val secondarySourceError: String? = null,
    val autoPlayNext: Boolean = true,
    val wifiOnlyDownloads: Boolean = true,
    val preferredQuality: String = "1080p HD",
    val snackMessage: String? = null
)

class MuvioViewModel(
    private val repository: MuvioCatalogRepository = MuvioSeedRepository(),
    private val apiClient: MovieBoxApiClient = MovieBoxApiClient(),
    private val secondaryApiClient: SecondaryCatalogApiClient = SecondaryCatalogApiClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MuvioUiState())
    val uiState: StateFlow<MuvioUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var suggestJob: Job? = null
    private var detailJob: Job? = null
    private var streamJob: Job? = null
    private var browseJob: Job? = null
    private var staffJob: Job? = null

    init {
        loadInitialData()
        startDownloadProgressTicker()
        refreshHomeFeed()
    }

    private fun loadInitialData() {
        _uiState.update {
            it.copy(
                topTabs = repository.getTopTabs(),
                novels = repository.getNovels(),
                isApiKeyConfigured = apiClient.isApiKeyConfigured()
            )
        }
    }

    fun refreshHomeFeed() {
        val primaryConfigured = apiClient.isApiKeyConfigured()
        val secondaryConfigured = secondaryApiClient.isConfigured()

        if (!primaryConfigured && !secondaryConfigured) {
            _uiState.update {
                it.copy(
                    isLoadingHome = false,
                    isApiConnected = false,
                    isApiKeyConfigured = false,
                    apiStatusMessage = "Add MOVIEBOX_API_KEY in AI Studio Secrets panel for live catalog & streams."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoadingHome = true,
                isApiKeyConfigured = primaryConfigured,
                apiStatusMessage = "Syncing Multi-Source Live Feed..."
            )
        }

        // Source 2: CinePulse HD API v1 (runs in parallel with Source 1 for 50/50 Home blending)
        if (secondaryConfigured) {
            fetchSecondaryHomePages(primaryConfigured)
        } else {
            _uiState.update {
                it.copy(
                    secondarySourceError = "Add SECONDARY_CATALOG_API_KEY in AI Studio Secrets panel to enable Source 2."
                )
            }
        }

        if (!primaryConfigured) return

        // 1. Primary /home feed (100% live items — zero demo items)
        viewModelScope.launch {
            apiClient.fetchHomeFeed()
                .onSuccess { feed ->
                    if (feed.allItems.isNotEmpty()) {
                        _uiState.update { state ->
                            val rawMerged = (feed.allItems + state.allMedia.filter { it.poster.remoteUrl != null }).distinctBy { it.id }
                            val mergedMedia = interleaveSources5050(rawMerged)
                            state.copy(
                                isLoadingHome = false,
                                isApiConnected = true,
                                heroItems = buildTrendingHeroMix(feed.heroItems, mergedMedia),
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
                    val message = when (err) {
                        is ApiKeyMissingException -> "Add MOVIEBOX_API_KEY in AI Studio Secrets panel."
                        else -> "Live sync unavailable (${err.message?.take(45) ?: "Network"}). Tap Retry."
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

        // 2. Live Movies, TV Series & Anime shelves so Trending & Auto-Hero mix all three 50/50
        viewModelScope.launch {
            apiClient.fetchCatalog("/type/movies", page = 1).onSuccess { movies ->
                if (movies.isNotEmpty()) {
                    _uiState.update { state ->
                        val merged = interleaveSources5050((state.allMedia + movies).distinctBy { it.id })
                        state.copy(
                            allMedia = merged,
                            heroItems = buildTrendingHeroMix(state.heroItems, merged)
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            apiClient.fetchCatalog("/type/tv", page = 1).onSuccess { series ->
                if (series.isNotEmpty()) {
                    _uiState.update { state ->
                        val merged = interleaveSources5050((state.allMedia + series).distinctBy { it.id })
                        state.copy(
                            allMedia = merged,
                            heroItems = buildTrendingHeroMix(state.heroItems, merged)
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
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
                            heroItems = buildTrendingHeroMix(state.heroItems, merged)
                        )
                    }
                }
            }
        }

        // 3. Hot Short TV - GET /shorts?page=1
        viewModelScope.launch {
            apiClient.fetchShorts(page = 1).onSuccess { reels ->
                if (reels.isNotEmpty()) {
                    _uiState.update { it.copy(shortReels = reels) }
                }
            }
        }

        // 4. Trending Hot Searches - GET /hot-search
        viewModelScope.launch {
            apiClient.fetchHotSearch().onSuccess { hot ->
                if (hot.isNotEmpty()) {
                    _uiState.update { it.copy(hotSearches = hot.take(12)) }
                }
            }
        }

        // 5. Coming Soon / Upcoming - GET /upcoming?page=1
        viewModelScope.launch {
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
            apiClient.fetchCategories().onSuccess { categories ->
                if (categories.isNotEmpty()) {
                    _uiState.update { it.copy(apiCategories = categories) }
                }
            }
        }

        // 7. Filter Options - GET /filter-options
        viewModelScope.launch {
            apiClient.fetchFilterOptions().onSuccess { options ->
                _uiState.update { it.copy(filterOptions = options) }
            }
        }

        // 8. API Key Quota & Expiry - GET /me
        viewModelScope.launch {
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

    private fun fetchSecondaryHomePages(primaryConfigured: Boolean) {
        if (!secondaryApiClient.isConfigured()) {
            _uiState.update {
                it.copy(
                    secondarySourceError = "Add SECONDARY_CATALOG_API_KEY in AI Studio Secrets panel to enable Source 2."
                )
            }
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
        // Enrich 50/50 shelves across Anime, Drama, Bengali, Bollywood & South Indian
        listOf("/anime", "/drama", "/language/bangla", "/bollywood").forEach { catPath ->
            viewModelScope.launch {
                secondaryApiClient.fetchCatalog(path = catPath, page = 1).onSuccess { catItems ->
                    if (catItems.isNotEmpty()) {
                        _uiState.update { state ->
                            val merged = interleaveSources5050((state.allMedia + catItems).distinctBy { it.id })
                            state.copy(
                                secondarySourceError = null,
                                allMedia = merged,
                                heroItems = buildTrendingHeroMix(state.heroItems, merged)
                            )
                        }
                    }
                }
            }
        }
        // Blend Source 2 /hot-search into Trending Searches
        viewModelScope.launch {
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

    internal fun buildTrendingHeroMix(
        baseHero: List<MediaItem>,
        allMedia: List<MediaItem>
    ): List<MediaItem> {
        val pool = interleaveSources5050((baseHero + allMedia).distinctBy { it.id })
            .filter { !it.backdropUrl.isNullOrBlank() || !it.poster.remoteUrl.isNullOrBlank() }
        if (pool.isEmpty()) return baseHero

        val trendingMovies = interleaveSources5050(pool.filter { it.mediaType == MediaType.MOVIE })
        val trendingSeries = interleaveSources5050(pool.filter { it.mediaType == MediaType.SERIES || it.mediaType == MediaType.DRAMA })
        val trendingAnime = interleaveSources5050(pool.filter { it.mediaType == MediaType.ANIME || "anime" in it.sectionTags })

        val interleaved = mutableListOf<MediaItem>()
        val seenIds = mutableSetOf<String>()
        val maxSlots = 12
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
        return interleaveSources5050(interleaved)
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
        val activeSeason = _uiState.value.selectedSeasonByMedia[media.id] ?: 1
        val activeEpNum = _uiState.value.selectedEpisodeByMedia[media.id] ?: 1

        detailJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, isLoadingStream = true) }
            secondaryApiClient.fetchDetails(media.sourceDetailPath, media)
                .onSuccess { detail ->
                    val seasonEps = detail.seasonEpisodes[activeSeason]
                        ?: detail.seasonEpisodes.values.firstOrNull().orEmpty()
                    val selectedEpSources = seasonEps.find { it.number == activeEpNum }?.sources
                        ?.takeIf { it.isNotEmpty() }
                        ?: detail.initialStreams

                    val updatedItem = detail.updatedItem.copy(
                        episodes = seasonEps,
                        seasonEpisodes = detail.seasonEpisodes
                    )

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

                        state.copy(
                            isLoadingDetail = false,
                            isLoadingStream = selectedEpSources.isEmpty(),
                            allMedia = nextAll,
                            seasonsCountByMedia = state.seasonsCountByMedia + (media.id to detail.seasonsCount),
                            seasonEpisodesByMedia = if (detail.seasonEpisodes.isNotEmpty()) {
                                state.seasonEpisodesByMedia + (media.id to detail.seasonEpisodes)
                            } else {
                                state.seasonEpisodesByMedia
                            },
                            streamsByMedia = if (selectedEpSources.isNotEmpty()) {
                                state.streamsByMedia + (media.id to selectedEpSources)
                            } else {
                                state.streamsByMedia
                            },
                            selectedStreamIndexByMedia = state.selectedStreamIndexByMedia + (media.id to 0),
                            detailRecommendationsByMedia = if (recs.isNotEmpty()) {
                                state.detailRecommendationsByMedia + (media.id to recs)
                            } else {
                                state.detailRecommendationsByMedia
                            }
                        )
                    }

                    // If /details didn't include direct streams for this episode/movie, fetch from Source 2 /stream endpoint
                    if (selectedEpSources.isEmpty()) {
                        fetchStreamsForMedia(updatedItem, activeSeason, activeEpNum)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingDetail = false) }
                    // Still attempt direct Source 2 /stream fetch by title/path
                    fetchStreamsForMedia(media, activeSeason, activeEpNum)
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
                            quality = if (url.contains(".m3u8", true)) "Adaptive HLS" else "Direct MP4",
                            uploader = "CinePulse HD",
                            streamUrl = url
                        )
                    )
                }
            if (!embeddedStreams.isNullOrEmpty()) {
                _uiState.update { current ->
                    current.copy(
                        isLoadingStream = false,
                        streamsByMedia = current.streamsByMedia + (media.id to embeddedStreams),
                        selectedStreamIndexByMedia = current.selectedStreamIndexByMedia + (media.id to 0)
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
                        current.copy(
                            isLoadingStream = false,
                            streamsByMedia = current.streamsByMedia + (media.id to secondaryStreams),
                            streamErrorByMedia = current.streamErrorByMedia - media.id,
                            selectedStreamIndexByMedia = current.selectedStreamIndexByMedia + (media.id to 0)
                        )
                    }
                    return@launch
                }

                // If Source 2 returned 0 streams, try primary MovieBox /stream as a backup so playback still works
                val fallbackPrimary = if (apiClient.isApiKeyConfigured()) {
                    apiClient.fetchStreams(
                        title = media.title,
                        season = season,
                        episode = episode,
                        isSeries = isSeries
                    ).getOrNull().orEmpty()
                } else {
                    emptyList()
                }

                _uiState.update { current ->
                    if (fallbackPrimary.isNotEmpty()) {
                        current.copy(
                            isLoadingStream = false,
                            streamsByMedia = current.streamsByMedia + (media.id to fallbackPrimary),
                            streamErrorByMedia = current.streamErrorByMedia - media.id,
                            selectedStreamIndexByMedia = current.selectedStreamIndexByMedia + (media.id to 0)
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
            apiClient.fetchStreams(
                title = media.title,
                season = season,
                episode = episode,
                isSeries = isSeries
            ).onSuccess { streams ->
                _uiState.update { current ->
                    if (streams.isNotEmpty()) {
                        current.copy(
                            isLoadingStream = false,
                            streamsByMedia = current.streamsByMedia + (media.id to streams),
                            streamErrorByMedia = current.streamErrorByMedia - media.id,
                            selectedStreamIndexByMedia = current.selectedStreamIndexByMedia + (media.id to 0)
                        )
                    } else {
                        current.copy(
                            isLoadingStream = false,
                            streamErrorByMedia = current.streamErrorByMedia + (media.id to "এই ভিডিওর স্ট্রীম পাওয়া যায়নি")
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update { current ->
                    current.copy(
                        isLoadingStream = false,
                        streamErrorByMedia = current.streamErrorByMedia + (
                            media.id to (err.message?.takeIf { it.isNotBlank() } ?: "স্ট্রীম লোড করতে সমস্যা হয়েছে")
                        )
                    )
                }
            }
        }
    }

    private fun startDownloadProgressTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1500)
                _uiState.update { state ->
                    val hasActive = state.downloads.any { it.status == DownloadStatus.DOWNLOADING }
                    if (!hasActive) {
                        state
                    } else {
                        state.copy(
                            downloads = state.downloads.map { dl ->
                                if (dl.status == DownloadStatus.DOWNLOADING) {
                                    val next = (dl.progress + 0.04f).coerceAtMost(1f)
                                    dl.copy(
                                        progress = next,
                                        status = if (next >= 1f) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING,
                                        sizeText = if (next >= 1f) "1.1 GB • Ready offline" else "${(next * 1100).toInt()} MB / 1.1 GB"
                                    )
                                } else {
                                    dl
                                }
                            }
                        )
                    }
                }
            }
        }
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
            val updated = (listOf(mediaId) + state.watchHistoryIds.filterNot { it == mediaId }).take(15)
            state.copy(watchHistoryIds = updated)
        }
    }

    fun toggleWatchlist(mediaId: String) {
        _uiState.update { state ->
            val next = if (mediaId in state.watchlistIds) {
                state.watchlistIds - mediaId
            } else {
                state.watchlistIds + mediaId
            }
            val msg = if (mediaId in next) "Added to My List" else "Removed from My List"
            state.copy(watchlistIds = next, snackMessage = msg)
        }
    }

    fun selectLanguage(mediaId: String, language: String) {
        _uiState.update { state ->
            state.copy(selectedLanguageByMedia = state.selectedLanguageByMedia + (mediaId to language))
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
                selectedEpisodeByMedia = state.selectedEpisodeByMedia + (media.id to 1)
            )
        }
        fetchStreamsForMedia(media, season, 1)
    }

    fun selectEpisode(media: MediaItem, episodeNumber: Int) {
        val season = _uiState.value.selectedSeasonByMedia[media.id] ?: 1
        _uiState.update { state ->
            state.copy(selectedEpisodeByMedia = state.selectedEpisodeByMedia + (media.id to episodeNumber))
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
        _uiState.update { state ->
            val current = state.commentsByMedia[mediaId].orEmpty()
            val updated = current.map { comment ->
                if (comment.id == commentId) {
                    val liked = !comment.isLiked
                    comment.copy(
                        isLiked = liked,
                        likes = if (liked) comment.likes + 1 else (comment.likes - 1).coerceAtLeast(0)
                    )
                } else {
                    comment
                }
            }
            state.copy(commentsByMedia = state.commentsByMedia + (mediaId to updated))
        }
    }

    fun postComment(mediaId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            val current = state.commentsByMedia[mediaId].orEmpty()
            val newComment = CommentItem(
                id = "c_${System.currentTimeMillis()}",
                author = "You",
                avatarInitials = "M",
                avatarColor = 0xFF00B362,
                flagEmoji = "\uD83C\uDDE7\uD83C\uDDE9",
                timestamp = "Just now",
                content = trimmed,
                likes = 0,
                repliesCount = 0
            )
            state.copy(
                commentsByMedia = state.commentsByMedia + (mediaId to (listOf(newComment) + current)),
                snackMessage = "Comment posted"
            )
        }
    }

    fun startDownload(media: MediaItem, qualityOverride: String? = null) {
        _uiState.update { state ->
            val chosenQuality = qualityOverride ?: state.preferredQuality
            if (state.downloads.any { it.mediaId == media.id }) {
                state.copy(snackMessage = "Already in Downloads")
            } else {
                val item = DownloadItem(
                    mediaId = media.id,
                    title = media.title,
                    quality = chosenQuality,
                    sizeText = "Starting...",
                    progress = 0.08f,
                    status = DownloadStatus.DOWNLOADING,
                    poster = media.poster
                )
                state.copy(
                    downloads = listOf(item) + state.downloads,
                    snackMessage = "Downloading ${media.title} ($chosenQuality)"
                )
            }
        }
    }

    fun toggleDownloadPause(mediaId: String) {
        _uiState.update { state ->
            state.copy(
                downloads = state.downloads.map { dl ->
                    if (dl.mediaId == mediaId && dl.status != DownloadStatus.COMPLETED) {
                        val nextStatus = if (dl.status == DownloadStatus.DOWNLOADING) {
                            DownloadStatus.PAUSED
                        } else {
                            DownloadStatus.DOWNLOADING
                        }
                        dl.copy(status = nextStatus)
                    } else {
                        dl
                    }
                }
            )
        }
    }

    fun removeDownload(mediaId: String) {
        _uiState.update { state ->
            state.copy(downloads = state.downloads.filterNot { it.mediaId == mediaId })
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
        _uiState.update { it.copy(autoPlayNext = !it.autoPlayNext) }
    }

    fun toggleWifiOnlyDownloads() {
        _uiState.update { it.copy(wifiOnlyDownloads = !it.wifiOnlyDownloads) }
    }

    fun cyclePreferredQuality() {
        _uiState.update { state ->
            val next = when (state.preferredQuality) {
                "1080p HD" -> "4K HDR"
                "4K HDR" -> "720p HD"
                else -> "1080p HD"
            }
            state.copy(preferredQuality = next)
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

    fun updateWatchProgress(mediaId: String, progress: Float) {
        val clamped = progress.coerceIn(0.05f, 0.98f)
        _uiState.update { state ->
            state.copy(watchProgressByMedia = state.watchProgressByMedia + (mediaId to clamped))
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

    fun moodFilteredMedia(source: List<MediaItem>, mood: String): List<MediaItem> {
        return when (mood) {
            "Late Night Thriller" -> source.filter {
                it.genre.contains("Thriller", true) || it.genre.contains("Crime", true) || it.genre.contains("Mystery", true) || it.genre.contains("Horror", true)
            }.ifEmpty { source }
            "Comfort Drama" -> source.filter {
                it.genre.contains("Romance", true) || it.genre.contains("Drama", true) || it.genre.contains("Comedy", true)
            }.ifEmpty { source }
            "Anime Marathon" -> source.filter {
                it.mediaType == MediaType.ANIME || "anime" in it.sectionTags
            }.ifEmpty { source }
            "Bengali Gems" -> source.filter {
                it.languageBadge.equals("Bengali", true) || "bengali" in it.sectionTags
            }.ifEmpty { source }
            "Action Rush" -> source.filter {
                it.genre.contains("Action", true) || it.genre.contains("Sci-Fi", true) || it.genre.contains("Adventure", true)
            }.ifEmpty { source }
            else -> source
        }
    }

    fun clearSnackMessage() {
        _uiState.update { it.copy(snackMessage = null) }
    }

    fun filteredTrendingSeries(): List<MediaItem> {
        val state = _uiState.value
        val base = interleaveSources5050(
            state.allMedia.filter {
                it.mediaType == MediaType.SERIES || it.mediaType == MediaType.DRAMA || it.mediaType == MediaType.ANIME
            }.ifEmpty { state.allMedia }
        )
        return when (state.selectedSeriesFilter) {
            "Anime" -> interleaveSources5050(base.filter { it.mediaType == MediaType.ANIME || "anime" in it.sectionTags })
                .ifEmpty { base }
            "Asian Drama" -> interleaveSources5050(base.filter { it.country in listOf("South Korea", "China", "Japan") && it.mediaType != MediaType.ANIME })
                .ifEmpty { base }
            "Indian Drama" -> interleaveSources5050(base.filter { it.country in listOf("India", "Bangladesh") })
                .ifEmpty { base }
            else -> base
        }
    }

    fun filteredTrendingMovies(): List<MediaItem> {
        val state = _uiState.value
        val base = interleaveSources5050(
            state.allMedia.filter {
                it.mediaType == MediaType.MOVIE || it.subCategory == "New Bengali"
            }.ifEmpty { state.allMedia }
        )
        return when (state.selectedMovieFilter) {
            "TOP Movies" -> interleaveSources5050(base.sortedByDescending { it.rating })
            "New Bengali" -> interleaveSources5050(
                state.allMedia.filter { it.languageBadge == "Bengali" || "bengali" in it.sectionTags }
            ).ifEmpty { base }
            "Bollywood" -> interleaveSources5050(base.filter { it.country == "India" }).ifEmpty { base }
            else -> base
        }
    }
}
