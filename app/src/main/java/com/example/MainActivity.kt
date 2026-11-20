package com.example

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.player.LocalMuvioPipController
import com.example.ui.player.MuvioPipController
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.content.Intent
import com.example.data.MuvioDownloadEngine
import com.example.data.MuvioPreferencesStore
import com.example.model.DownloadStatus
import com.example.model.MainTab
import com.example.ui.components.MuvioBottomBar
import com.example.ui.components.MuvioInAppUpdateDialog
import com.example.ui.screens.DetailPlayerScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.ExploreHubScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveSportsPlayerOverlay
import com.example.ui.screens.MeScreen
import com.example.ui.screens.SeeAllCategoryScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.StaffFilmographySheet
import com.example.ui.theme.MuvioCrimsonGlow
import com.example.ui.theme.MuvioSurfaceElevated
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTheme
import com.example.ui.viewmodel.MuvioViewModel

const val ROUTE_MAIN = "main"
const val ROUTE_DETAIL = "detail/{mediaId}"
const val ROUTE_CATEGORY = "category/{sectionTitle}"

class MainActivity : ComponentActivity() {
    private lateinit var pipController: MuvioPipController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pipController = MuvioPipController(this)
        pipController.registerReceiver()
        configureFastImageLoader()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            CompositionLocalProvider(LocalMuvioPipController provides pipController) {
                MuvioTheme {
                    MuvioApp()
                }
            }
        }
    }

    override fun onDestroy() {
        if (::pipController.isInitialized) {
            pipController.unregisterReceiver()
        }
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (::pipController.isInitialized) {
            pipController.onUserLeaveHint()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (::pipController.isInitialized) {
            pipController.onPipModeChanged(isInPictureInPictureMode)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        @Suppress("DEPRECATION")
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && ::pipController.isInitialized) {
            pipController.onPipModeChanged(isInPictureInPictureMode)
        }
    }

    private fun configureFastImageLoader() {
        if (imageLoaderConfigured) return
        imageLoaderConfigured = true
        runCatching {
            val appContext = applicationContext
            Coil.setImageLoader {
                val dispatcher = Dispatcher().apply {
                    maxRequests = 64
                    maxRequestsPerHost = 24
                }
                val sharedHttpClient = OkHttpClient.Builder()
                    .dispatcher(dispatcher)
                    .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(18, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val req = chain.request().newBuilder()
                            .header(
                                "User-Agent",
                                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                            )
                            .build()
                        chain.proceed(req)
                    }
                    .build()
                ImageLoader.Builder(appContext)
                    .okHttpClient(sharedHttpClient)
                    .memoryCache {
                        MemoryCache.Builder(appContext)
                            .maxSizePercent(0.28)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(appContext.cacheDir.resolve("muvio_img_cache"))
                            .maxSizeBytes(100L * 1024L * 1024L)
                            .build()
                    }
                    .allowHardware(true)
                    .allowRgb565(true)
                    .respectCacheHeaders(false)
                    .crossfade(100)
                    .build()
            }
        }
    }

    companion object {
        @Volatile
        private var imageLoaderConfigured = false
    }
}

@Composable
fun MuvioApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val factory = remember(appContext) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MuvioViewModel(
                    preferencesStore = MuvioPreferencesStore(appContext),
                    downloadEngine = MuvioDownloadEngine(appContext)
                ) as T
            }
        }
    }
    val viewModel: MuvioViewModel = viewModel(factory = factory)
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackMessage) {
        val msg = uiState.snackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_MAIN,
            enterTransition = {
                fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it / 12 }
            },
            exitTransition = {
                fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { it / 12 }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            composable(ROUTE_MAIN) {
                BackHandler(enabled = uiState.selectedTab != MainTab.HOME || uiState.isSearchActive) {
                    if (uiState.isSearchActive) {
                        viewModel.setSearchActive(false)
                    } else {
                        viewModel.selectTab(MainTab.HOME)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = uiState.selectedTab,
                        animationSpec = tween(150, easing = FastOutSlowInEasing),
                        label = "main_tab_crossfade"
                    ) { activeTab ->
                    when (activeTab) {
                        MainTab.HOME -> {
                            val southAsiaList = remember(
                                uiState.allMedia,
                                uiState.remoteConfig
                            ) {
                                viewModel.southAsiaTrendingItems()
                            }
                            val seriesList = remember(
                                uiState.allMedia,
                                uiState.selectedSeriesFilter,
                                uiState.selectedTopTab,
                                uiState.selectedMood
                            ) {
                                viewModel.filteredTrendingSeries()
                            }
                            val moviesList = remember(
                                uiState.allMedia,
                                uiState.selectedMovieFilter,
                                uiState.selectedTopTab,
                                uiState.selectedMood
                            ) {
                                viewModel.filteredTrendingMovies()
                            }
                            val continueWatching = remember(
                                uiState.allMedia,
                                uiState.watchHistoryIds,
                                uiState.watchProgressByMedia
                            ) {
                                viewModel.continueWatchingItems()
                            }
                            HomeScreen(
                                uiState = uiState,
                                southAsiaList = southAsiaList,
                                seriesList = seriesList,
                                moviesList = moviesList,
                                continueWatchingItems = continueWatching,
                                onTopTabClick = { tab ->
                                    if (tab == "18+") {
                                        viewModel.requestAdultAccess()
                                    } else {
                                        viewModel.selectTopTab(tab)
                                        viewModel.loadCatalogForCategory(tab)
                                    }
                                },
                                onSearchActiveChange = viewModel::setSearchActive,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSubmitSearch = viewModel::saveRecentSearch,
                                onClearRecentSearches = viewModel::clearRecentSearches,
                                onSearchSourceFilterClick = viewModel::selectSearchSourceFilter,
                                onRetrySecondarySource = viewModel::retrySecondarySource,
                                onSeriesFilterClick = viewModel::selectSeriesFilter,
                                onMovieFilterClick = viewModel::selectMovieFilter,
                                onCategoryBannerClick = { key ->
                                    viewModel.applyCategoryBanner(key)
                                    viewModel.loadCatalogForCategory(key)
                                },
                                onMediaClick = { item ->
                                    viewModel.openMediaDetail(item)
                                    navController.navigate("detail/${Uri.encode(item.id)}")
                                },
                                onToggleWatchlist = viewModel::toggleWatchlist,
                                onOpenWatchlist = viewModel::openWatchlistShortcut,
                                onSelectMood = viewModel::selectMood,
                                onSurpriseMe = viewModel::pickSurpriseMedia,
                                onDismissSurprise = viewModel::dismissSurprisePick,
                                onSeeAllClick = { title ->
                                    viewModel.openLocalSectionBrowse(title)
                                    navController.navigate("category/${Uri.encode(title)}")
                                },
                                onToggleMatchNotify = viewModel::toggleLiveMatchNotify,
                                onLiveMatchClick = viewModel::openLiveMatch,
                                onPlatformClick = { brand ->
                                    viewModel.openPlatform(brand)
                                    navController.navigate("category/${Uri.encode(brand.displayName)}")
                                },
                                onApiCategoryClick = { cat ->
                                    val isAdultCat = cat.path.contains("adult", ignoreCase = true) ||
                                        cat.name.contains("18+", ignoreCase = true)
                                    if (isAdultCat) {
                                        if (viewModel.requestAdultAccess(cat)) {
                                            navController.navigate("category/${Uri.encode(cat.name)}")
                                        }
                                    } else {
                                        viewModel.openCatalogPath(cat.name, cat.path)
                                        navController.navigate("category/${Uri.encode(cat.name)}")
                                    }
                                },
                                onUpcomingSeeAllClick = {
                                    viewModel.openUpcomingBrowse()
                                    navController.navigate("category/${Uri.encode("Coming Soon")}")
                                },
                                onOpenDownloads = {
                                    viewModel.selectTab(MainTab.DOWNLOADS)
                                },
                                onOpenAnnouncementUrl = { url ->
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                onDismissAnnouncement = viewModel::dismissAnnouncement,
                                onRefreshApi = {
                                    viewModel.refreshHomeFeed()
                                    viewModel.fetchRemoteConfig(userInitiated = false)
                                }
                            )
                        }

                        MainTab.NOVEL_HUB -> {
                            ExploreHubScreen(
                                uiState = uiState,
                                onMediaClick = { item ->
                                    viewModel.openMediaDetail(item)
                                    navController.navigate("detail/${Uri.encode(item.id)}")
                                },
                                onPlatformClick = { brand ->
                                    viewModel.openPlatform(brand)
                                    navController.navigate("category/${Uri.encode(brand.displayName)}")
                                },
                                onApiCategoryClick = { cat ->
                                    val isAdultCat = cat.path.contains("adult", ignoreCase = true) ||
                                        cat.name.contains("18+", ignoreCase = true)
                                    if (isAdultCat) {
                                        if (viewModel.requestAdultAccess(cat)) {
                                            navController.navigate("category/${Uri.encode(cat.name)}")
                                        }
                                    } else {
                                        viewModel.openCatalogPath(cat.name, cat.path)
                                        navController.navigate("category/${Uri.encode(cat.name)}")
                                    }
                                },
                                onOpenSearch = {
                                    viewModel.selectTab(MainTab.HOME)
                                    viewModel.setSearchActive(true)
                                },
                                onRefreshApi = viewModel::refreshHomeFeed
                            )
                        }

                        MainTab.SHORTS -> {
                            ShortsScreen(
                                uiState = uiState,
                                onToggleLike = viewModel::toggleShortLike,
                                onToggleSave = viewModel::toggleShortSave,
                                onWatchFullSeries = { reel ->
                                    val resolvedId = viewModel.openShortReelFullDrama(reel)
                                    navController.navigate("detail/${Uri.encode(resolvedId)}")
                                },
                                onResolveReelStream = viewModel::resolveShortReelStream,
                                onRetryShorts = viewModel::retryShorts
                            )
                        }

                        MainTab.DOWNLOADS -> {
                            DownloadsScreen(
                                downloads = uiState.downloads,
                                onTogglePause = viewModel::toggleDownloadPause,
                                onRemoveDownload = viewModel::removeDownload,
                                onOpenMedia = { downloadId ->
                                    viewModel.openDownloadedMedia(downloadId)?.let { resolvedMediaId ->
                                        navController.navigate("detail/${Uri.encode(resolvedMediaId)}")
                                    }
                                }
                            )
                        }

                        MainTab.ME -> {
                            LaunchedEffect(Unit) {
                                viewModel.refreshStorageStats()
                            }
                            MeScreen(
                                uiState = uiState,
                                onMediaClick = { item ->
                                    viewModel.openMediaDetail(item)
                                    navController.navigate("detail/${Uri.encode(item.id)}")
                                },
                                onToggleAutoPlay = viewModel::toggleAutoPlayNext,
                                onToggleWifiOnly = viewModel::toggleWifiOnlyDownloads,
                                onCycleQuality = viewModel::cyclePreferredQuality,
                                onCycleSubtitleSize = {
                                    val nextSize = when (uiState.subtitleSize) {
                                        "Small" -> "Medium"
                                        "Medium" -> "Large"
                                        else -> "Small"
                                    }
                                    viewModel.setSubtitleSize(nextSize)
                                },
                                onClearHistory = viewModel::clearWatchHistory,
                                onClearCache = viewModel::clearAppCache,
                                onCheckForUpdates = { viewModel.fetchRemoteConfig(userInitiated = true) },
                                onRefreshApi = {
                                    viewModel.refreshHomeFeed()
                                    viewModel.fetchRemoteConfig(userInitiated = false)
                                },
                                onSelectMeSection = viewModel::selectMeSection,
                                onToggleWatchlist = viewModel::toggleWatchlist
                            )
                        }
                    }
                    }

                    // Bottom Navigation Bar with Elevated Center Shorts Orb
                    MuvioBottomBar(
                        selectedTab = uiState.selectedTab,
                        activeDownloadCount = uiState.downloads.count { it.status == DownloadStatus.DOWNLOADING },
                        onSelectTab = viewModel::selectTab,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            composable(
                route = ROUTE_DETAIL,
                arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId").orEmpty()
                val media = viewModel.getMediaById(mediaId) ?: uiState.allMedia.firstOrNull()
                if (media != null) {
                    val recommendations = remember(media.id, uiState.allMedia, uiState.detailRecommendationsByMedia) {
                        uiState.detailRecommendationsByMedia[media.id]?.takeIf { it.isNotEmpty() }
                            ?: uiState.allMedia.filter { it.id != media.id }.take(6)
                    }
                    val defaultLang = media.resources.firstOrNull()?.language ?: "Hindi dub"
                    val selectedLang = uiState.selectedLanguageByMedia[media.id] ?: defaultLang
                    val selectedEp = uiState.selectedEpisodeByMedia[media.id] ?: 1
                    val selectedSeason = uiState.selectedSeasonByMedia[media.id] ?: 1
                    val seasonsCount = uiState.seasonsCountByMedia[media.id] ?: maxOf(1, media.seasonEpisodes.size)
                    val activeSeasonEps = uiState.seasonEpisodesByMedia[media.id]?.get(selectedSeason)
                        ?: media.seasonEpisodes[selectedSeason]
                        ?: media.episodes
                    val mediaForDisplay = remember(media, activeSeasonEps) {
                        if (activeSeasonEps != media.episodes) media.copy(episodes = activeSeasonEps) else media
                    }
                    val liveStreams = uiState.streamsByMedia[media.id].orEmpty()
                    val selectedStreamIdx = uiState.selectedStreamIndexByMedia[media.id] ?: 0

                    val episodeKey = "${media.id}_s${selectedSeason}_e${selectedEp}"
                    DetailPlayerScreen(
                        media = mediaForDisplay,
                        recommendations = recommendations,
                        comments = uiState.commentsByMedia[media.id].orEmpty(),
                        liveStreams = liveStreams,
                        selectedStreamIndex = selectedStreamIdx,
                        isLoadingStream = uiState.isLoadingStream,
                        streamError = uiState.streamErrorByMedia[media.id],
                        seasonsCount = seasonsCount,
                        selectedSeason = selectedSeason,
                        isInWatchlist = media.id in uiState.watchlistIds,
                        selectedLanguage = selectedLang,
                        selectedEpisode = selectedEp,
                        initialResumePositionMs = uiState.watchPositionMsByEpisode[episodeKey] ?: 0L,
                        autoPlayNext = uiState.autoPlayNext,
                        subtitleSize = uiState.subtitleSize,
                        onSelectSubtitleSize = viewModel::setSubtitleSize,
                        sleepTimerMinutes = uiState.sleepTimerMinutes,
                        onCycleSleepTimer = viewModel::cycleSleepTimer,
                        onSleepTimerElapsed = { viewModel.resetSleepTimer(showNotice = true) },
                        onProgressChange = { frac, posMs, durMs ->
                            viewModel.updateWatchProgress(media.id, frac, posMs, durMs)
                        },
                        onRetryStream = { viewModel.fetchStreamsForMedia(mediaForDisplay, selectedSeason, selectedEp) },
                        onFallbackToAlternateStream = { viewModel.fallbackToAlternateStream(mediaForDisplay, selectedSeason, selectedEp) },
                        onBack = { navController.popBackStack() },
                        onToggleWatchlist = { viewModel.toggleWatchlist(media.id) },
                        onDownloadClick = { viewModel.startDownload(media) },
                        onDownloadWithQuality = { quality -> viewModel.startDownload(media, quality) },
                        onSelectLanguage = { lang -> viewModel.selectLanguage(media.id, lang) },
                        onSelectDub = { dub -> viewModel.selectDubVersion(media, dub) },
                        onStaffClick = { staff -> viewModel.openStaffProfile(staff) },
                        onSelectStreamIndex = { idx -> viewModel.selectStreamIndex(media.id, idx) },
                        onSelectSeason = { season -> viewModel.selectSeason(media, season) },
                        onSelectEpisode = { ep -> viewModel.selectEpisode(media, ep) },
                        onLoadMoreEpisodes = { viewModel.loadMoreEpisodesForCurrentSeason(media) },
                        onToggleCommentLike = { commentId -> viewModel.toggleCommentLike(media.id, commentId) },
                        onPostComment = { text -> viewModel.postComment(media.id, text) },
                        onRecommendationClick = { rec ->
                            viewModel.openMediaDetail(rec)
                            navController.navigate("detail/${Uri.encode(rec.id)}")
                        }
                    )
                }
            }

            composable(
                route = ROUTE_CATEGORY,
                arguments = listOf(navArgument("sectionTitle") { type = NavType.StringType })
            ) { backStackEntry ->
                val routeTitle = backStackEntry.arguments?.getString("sectionTitle") ?: "All"
                val title = uiState.categoryBrowseTitle.ifBlank { routeTitle }
                val list = uiState.categoryBrowseItems.ifEmpty { uiState.allMedia }
                SeeAllCategoryScreen(
                    sectionTitle = title,
                    itemsList = list,
                    filterOptions = uiState.filterOptions,
                    isLoadingMore = uiState.isLoadingCategoryPage,
                    canLoadMore = uiState.canLoadMoreCategory,
                    onLoadMore = viewModel::loadNextBrowsePage,
                    onBack = { navController.popBackStack() },
                    onMediaClick = { item ->
                        viewModel.openMediaDetail(item)
                        navController.navigate("detail/${Uri.encode(item.id)}")
                    }
                )
            }
        }

        // 18+ Mature Content Age Verification Gate
        if (uiState.showAdultGateDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissAdultGate,
                containerColor = MuvioSurfaceElevated,
                title = {
                    Text(
                        text = "18+ Age Confirmation",
                        color = MuvioTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "This section contains mature 18+ rated films and series. Please confirm that you are 18 years or older to continue.",
                        color = MuvioTextPrimary.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val pendingCat = viewModel.confirmAdultAge()
                            if (pendingCat != null) {
                                navController.navigate("category/${Uri.encode(pendingCat.name)}")
                            }
                        }
                    ) {
                        Text("I am 18+", color = MuvioCrimsonGlow)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissAdultGate) {
                        Text("Cancel", color = MuvioTextPrimary.copy(alpha = 0.7f))
                    }
                }
            )
        }

        // In-App Online Update Checker Dialog (Play Policy safe: opens verified HTTPS release/store link)
        if (uiState.showUpdateDialog && uiState.remoteConfig.updateUrl.startsWith("https://")) {
            MuvioInAppUpdateDialog(
                config = uiState.remoteConfig,
                onUpdateNow = { url ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                    if (!uiState.remoteConfig.forceUpdate) {
                        viewModel.dismissUpdateDialog()
                    }
                },
                onDismiss = viewModel::dismissUpdateDialog
            )
        }

        // Cast & Crew Filmography Bottom Sheet (/staff?staffId=...)
        uiState.activeStaffMember?.let { staff ->
            StaffFilmographySheet(
                staff = staff,
                movies = uiState.staffMovies,
                isLoading = uiState.isLoadingStaff,
                onDismiss = viewModel::closeStaffProfile,
                onMediaClick = { item ->
                    viewModel.openMediaDetail(item)
                    navController.navigate("detail/${Uri.encode(item.id)}")
                }
            )
        }

        // Native ExoPlayer Live Sports Overlay
        uiState.activeLiveMatch?.let { match ->
            LiveSportsPlayerOverlay(
                match = match,
                onClose = viewModel::closeLiveMatch
            )
        }

        val pipController = LocalMuvioPipController.current
        val isInPipMode by pipController?.isInPipMode?.collectAsStateWithLifecycle()
            ?: remember { androidx.compose.runtime.mutableStateOf(false) }

        if (!isInPipMode) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 86.dp, start = 16.dp, end = 16.dp)
            ) { data ->
                Snackbar(
                    containerColor = MuvioSurfaceElevated,
                    contentColor = MuvioTextPrimary,
                    actionColor = MuvioCrimsonGlow,
                    snackbarData = data
                )
            }
        }
    }
}
