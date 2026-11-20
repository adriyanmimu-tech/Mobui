package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ApiCategory
import com.example.model.ContentSource
import com.example.model.LiveMatchItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PlatformBrand
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.MuvioBg
import com.example.ui.theme.MuvioChampagne
import com.example.ui.theme.MuvioChampagneLight
import com.example.ui.theme.MuvioCrimson
import com.example.ui.theme.MuvioCrimsonBorder
import com.example.ui.theme.MuvioCrimsonContainer
import com.example.ui.theme.MuvioCrimsonGlow
import com.example.ui.theme.MuvioGlassBorder
import com.example.ui.theme.MuvioOutline
import com.example.ui.theme.MuvioSurfaceCard
import com.example.ui.theme.MuvioSurfaceElevated
import com.example.ui.theme.MuvioTextMuted
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTextSecondary
import com.example.ui.viewmodel.MuvioUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MuvioUiState,
    seriesList: List<MediaItem>,
    moviesList: List<MediaItem>,
    continueWatchingItems: List<Pair<MediaItem, Float>> = emptyList(),
    onTopTabClick: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    onSearchSourceFilterClick: (ContentSource?) -> Unit = {},
    onRetrySecondarySource: () -> Unit = {},
    onSeriesFilterClick: (String) -> Unit,
    onMovieFilterClick: (String) -> Unit,
    onCategoryBannerClick: (String) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onToggleWatchlist: (String) -> Unit = {},
    onOpenWatchlist: () -> Unit = {},
    onSelectMood: (String) -> Unit = {},
    onSurpriseMe: () -> Unit = {},
    onDismissSurprise: () -> Unit = {},
    onSeeAllClick: (String) -> Unit,
    onToggleMatchNotify: (String) -> Unit,
    onLiveMatchClick: (LiveMatchItem) -> Unit = {},
    onPlatformClick: (PlatformBrand) -> Unit = {},
    onApiCategoryClick: (ApiCategory) -> Unit = {},
    onUpcomingSeeAllClick: () -> Unit = {},
    onRefreshApi: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (uiState.isSearchActive) {
        NoirSearchOverlay(
            query = uiState.searchQuery,
            isSearchingRemote = uiState.isSearchingRemote,
            remoteResults = uiState.remoteSearchResults,
            selectedSourceFilter = uiState.searchSourceFilter,
            secondarySourceError = uiState.secondarySourceError,
            hotSearches = uiState.hotSearches,
            recentSearches = uiState.recentSearches,
            suggestions = uiState.searchSuggestions,
            allMedia = uiState.allMedia,
            onBack = { onSearchActiveChange(false) },
            onQueryChange = onSearchQueryChange,
            onSubmitSearch = onSubmitSearch,
            onClearRecentSearches = onClearRecentSearches,
            onSelectSourceFilter = onSearchSourceFilterClick,
            onRetrySecondarySource = onRetrySecondarySource,
            onMediaClick = onMediaClick
        )
        return
    }

    val activeTopTab = uiState.selectedTopTab
    val filteredAll = remember(uiState.allMedia, uiState.adultMedia, activeTopTab, uiState.selectedMood) {
        val byTab = when (activeTopTab) {
            "18+" -> uiState.adultMedia
            "Movies" -> uiState.allMedia.filter { it.mediaType == MediaType.MOVIE }
            "Series" -> uiState.allMedia.filter { it.mediaType == MediaType.SERIES || it.mediaType == MediaType.DRAMA }
            "Anime" -> uiState.allMedia.filter { it.mediaType == MediaType.ANIME || "anime" in it.sectionTags }
            "Bengali" -> uiState.allMedia.filter { it.languageBadge.equals("Bengali", true) || "bengali" in it.sectionTags }
            else -> uiState.allMedia
        }

        when (uiState.selectedMood) {
            "Late Night Thriller" -> byTab.filter {
                it.genre.contains("Thriller", true) || it.genre.contains("Crime", true) || it.genre.contains("Mystery", true) || it.genre.contains("Horror", true)
            }
            "Comfort Drama" -> byTab.filter {
                it.genre.contains("Romance", true) || it.genre.contains("Drama", true) || it.genre.contains("Comedy", true)
            }
            "Anime Marathon" -> byTab.filter { it.mediaType == MediaType.ANIME || "anime" in it.sectionTags }
            "Bengali Gems" -> byTab.filter { it.languageBadge.equals("Bengali", true) || "bengali" in it.sectionTags }
            "Action Rush" -> byTab.filter { it.genre.contains("Action", true) || it.genre.contains("Sci-Fi", true) || it.genre.contains("Adventure", true) }
            else -> byTab
        }
    }

    val gemsList = remember(filteredAll) {
        val tagged = filteredAll.filter { "gems" in it.sectionTags || "secondary_catalog" in it.sectionTags }
        val pool = tagged.ifEmpty { filteredAll }
        val p = pool.filter { it.contentSource == ContentSource.PRIMARY }
        val s = pool.filter { it.contentSource == ContentSource.SECONDARY }
        if (p.isEmpty() || s.isEmpty()) pool.take(10) else buildList {
            for (i in 0 until maxOf(p.size, s.size)) {
                if (i < p.size) add(p[i])
                if (i < s.size) add(s[i])
            }
        }.take(10)
    }
    val hotShortList = remember(filteredAll) {
        filteredAll.filter { "hot_short" in it.sectionTags || it.mediaType == MediaType.SHORT_TV }.ifEmpty { filteredAll.take(6) }
    }
    val mostTrendingGrid = remember(filteredAll) {
        val pool = filteredAll.filter { "most_trending" in it.sectionTags }.ifEmpty { filteredAll }
        val p = pool.filter { it.contentSource == ContentSource.PRIMARY }
        val s = pool.filter { it.contentSource == ContentSource.SECONDARY }
        if (p.isEmpty() || s.isEmpty()) pool.take(9) else buildList {
            for (i in 0 until maxOf(p.size, s.size)) {
                if (i < p.size) add(p[i])
                if (i < s.size) add(s[i])
            }
        }.take(9)
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoadingHome,
        onRefresh = onRefreshApi,
        modifier = modifier
            .fillMaxSize()
            .background(MuvioBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 118.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Edge-to-Edge Full-Bleed Cinema Hero (or Noir Header while loading live API)
            item(key = "home_hero", contentType = "hero") {
                if (uiState.heroItems.isNotEmpty()) {
                    NoirFullBleedHeroPager(
                        heroItems = uiState.heroItems,
                        watchlistIds = uiState.watchlistIds,
                        isApiConnected = uiState.isApiConnected,
                        onOpenSearch = { onSearchActiveChange(true) },
                        onOpenWatchlist = onOpenWatchlist,
                        onMediaClick = onMediaClick,
                        onToggleWatchlist = onToggleWatchlist
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.ui.components.MuvioWordmark()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (uiState.isLoadingHome) "Loading Live Cinema..." else "Pull to Refresh",
                                style = MaterialTheme.typography.labelMedium,
                                color = MuvioChampagne
                            )
                            Surface(
                                color = MuvioSurfaceElevated,
                                border = BorderStroke(1.dp, MuvioGlassBorder),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onSearchActiveChange(true) }
                                    .testTag("fallback_search_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search movies and series",
                                        tint = MuvioTextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loading / Quick-Connect / Sync Status Area
            if (uiState.allMedia.isEmpty()) {
                val needsKeys = !uiState.isApiKeyConfigured && !uiState.isSecondaryApiConfigured
                item(key = "home_empty_catalog_state") {
                    when {
                        uiState.isLoadingHome -> {
                            HomeLiveSyncingCard(statusMessage = uiState.apiStatusMessage)
                        }
                        needsKeys -> {
                            HomeSecretsRequiredCard(
                                onRetrySync = onRefreshApi
                            )
                        }
                        else -> {
                            HomeConnectionErrorCard(
                                statusMessage = uiState.apiStatusMessage,
                                onRetry = onRefreshApi
                            )
                        }
                    }
                }
            } else if (uiState.isLoadingHome) {
                item {
                    LinearProgressIndicator(
                        color = MuvioChampagne,
                        trackColor = MuvioOutline.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                    )
                }
            } else if (!uiState.isApiConnected) {
                item {
                    Surface(
                        color = MuvioSurfaceCard,
                        border = BorderStroke(0.8.dp, MuvioGlassBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable(onClick = onRefreshApi)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.apiStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MuvioTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelLarge,
                                color = MuvioChampagne
                            )
                        }
                    }
                }
            }

            // 2. Refined Editorial Category Tab Strip
            item(key = "home_top_tabs", contentType = "tabs") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(uiState.topTabs, key = { idx, tab -> "tab_${tab}_$idx" }, contentType = { _, _ -> "tab" }) { _, tabName ->
                        val isSelected = tabName == activeTopTab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTopTabClick(tabName) }
                                .padding(vertical = 4.dp)
                                .testTag("top_tab_$tabName")
                        ) {
                            Text(
                                text = tabName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = if (isSelected) 16.sp else 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MuvioTextPrimary else MuvioTextMuted
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .height(3.dp)
                                    .width(if (isSelected) 26.dp else 0.dp)
                                    .clip(CircleShape)
                                    .background(MuvioCrimsonGlow)
                            )
                        }
                    }
                }
            }

            // 2b. Signature Asymmetric Velvet Bento Quick-Launch Hub
            if (activeTopTab != "18+") {
                item(key = "home_velvet_bento", contentType = "bento") {
                    VelvetSpotlightBento(
                        totalCatalogCount = uiState.allMedia.size,
                        onSelectCategory = onTopTabClick,
                        onOpenSearch = { onSearchActiveChange(true) }
                    )
                }
            }

            // 3. Tonight's Mood + Surprise Me Cinema Concierge
            item(key = "home_mood_concierge", contentType = "mood") {
                NoirMoodConciergeSection(
                    selectedMood = uiState.selectedMood,
                    surprisePick = uiState.surprisePick,
                    onSelectMood = onSelectMood,
                    onSurpriseMe = onSurpriseMe,
                    onDismissSurprise = onDismissSurprise,
                    onMediaClick = onMediaClick
                )
            }

            // 3b. Continue Watching Rail (appears automatically once the user has watch history)
            if (continueWatchingItems.isNotEmpty() && activeTopTab != "18+") {
                item(key = "home_continue_watching", contentType = "continue_watching") {
                    SectionHeader(
                        title = "Continue Watching",
                        eyebrow = "RESUME"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NoirContinueWatchingRail(
                        items = continueWatchingItems,
                        onMediaClick = onMediaClick
                    )
                }
            }

            // Honest empty-filter state when a tab + mood combination matches 0 loaded titles
            if (activeTopTab != "18+" &&
                uiState.allMedia.isNotEmpty() &&
                filteredAll.isEmpty() &&
                moviesList.isEmpty() &&
                seriesList.isEmpty()
            ) {
                item(key = "home_empty_filter") {
                    Surface(
                        color = MuvioSurfaceCard,
                        border = BorderStroke(0.9.dp, MuvioGlassBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No titles matched \"${uiState.selectedMood}\" in $activeTopTab",
                                style = MaterialTheme.typography.titleSmall,
                                color = MuvioTextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MuvioChampagne,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        onSelectMood("All")
                                        onTopTabClick("Trending")
                                    }
                                    .testTag("reset_home_filters_btn")
                            ) {
                                Text(
                                    text = "Reset Filters",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF140F05),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Dedicated 18+ Mature Catalog Grid when "18+" top tab is selected
            if (activeTopTab == "18+") {
                item {
                    SectionHeader(
                        title = "18+ Mature Cinema",
                        eyebrow = "ADULTS ONLY",
                        onSeeAllClick = {
                            onApiCategoryClick(ApiCategory("adult18plus", "18+ Mature", "/adult18plus"))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.isLoadingAdult && uiState.adultMedia.isEmpty()) {
                        LinearProgressIndicator(
                            color = MuvioChampagne,
                            trackColor = MuvioOutline.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                                .height(2.dp)
                                .clip(CircleShape)
                        )
                    } else if (uiState.adultMedia.isEmpty()) {
                        Text(
                            text = "No 18+ titles loaded yet. Tap All > to browse.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MuvioTextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            uiState.adultMedia.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { item ->
                                        MediaPosterCard(
                                            item = item,
                                            onClick = { onMediaClick(item) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Top 10 Trending Movies with Editorial Filter Chips (Live Only)
            if (moviesList.isNotEmpty() && activeTopTab != "18+") {
                item(key = "home_trending_movies", contentType = "shelf") {
                    SectionHeader(
                        title = stringResource(R.string.section_trending_movies),
                        eyebrow = "BOX OFFICE",
                        onSeeAllClick = { onSeeAllClick("Trending Movies") }
                    )
                    NoirFilterChipRow(
                        options = listOf("Cinema", "TOP Movies", "New Bengali", "Bollywood"),
                        selected = uiState.selectedMovieFilter,
                        onSelect = onMovieFilterClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(moviesList, key = { idx, item -> "movie_${item.id}_$idx" }, contentType = { _, _ -> "poster" }) { _, item ->
                            MediaPosterCard(
                                item = item,
                                showRank = true,
                                onClick = { onMediaClick(item) },
                                modifier = Modifier.width(132.dp)
                            )
                        }
                    }
                }
            }

            // 7. Top 10 Trending TV Series & Anime (Live Only)
            if (seriesList.isNotEmpty() && activeTopTab != "18+") {
                item(key = "home_trending_series", contentType = "shelf") {
                    SectionHeader(
                        title = stringResource(R.string.section_trending_series),
                        eyebrow = "BINGE WORTHY",
                        onSeeAllClick = { onSeeAllClick("Trending TV Series") }
                    )
                    NoirFilterChipRow(
                        options = listOf("Top Series", "Anime", "Asian Drama", "Indian Drama"),
                        selected = uiState.selectedSeriesFilter,
                        onSelect = onSeriesFilterClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(seriesList, key = { idx, item -> "series_${item.id}_$idx" }, contentType = { _, _ -> "poster" }) { _, item ->
                            MediaPosterCard(
                                item = item,
                                showRank = true,
                                onClick = { onMediaClick(item) },
                                modifier = Modifier.width(132.dp)
                            )
                        }
                    }
                }
            }

            // 7b. Streaming Platforms Rail (Netflix, Hoichoi, ZEE5, Prime Video, Disney+)
            if (uiState.platforms.isNotEmpty() && activeTopTab != "18+") {
                item(key = "home_platforms", contentType = "platforms") {
                    SectionHeader(
                        title = "Streaming Platforms",
                        eyebrow = "HUBS"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.platforms, key = { "platform_${it.apiName}" }) { brand ->
                            NoirPlatformTile(
                                brand = brand,
                                onClick = { onPlatformClick(brand) }
                            )
                        }
                    }
                }
            }

            // 8. Coming Soon / Upcoming Releases
            if (uiState.upcomingMedia.isNotEmpty() && activeTopTab != "18+") {
                item(key = "home_upcoming", contentType = "shelf") {
                    SectionHeader(
                        title = "Coming Soon",
                        eyebrow = "PREMIERES",
                        onSeeAllClick = onUpcomingSeeAllClick
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(uiState.upcomingMedia.take(12), key = { idx, item -> "up_${item.id}_$idx" }, contentType = { _, _ -> "poster" }) { _, item ->
                            Column(modifier = Modifier.width(126.dp)) {
                                MediaPosterCard(
                                    item = item,
                                    onClick = { onMediaClick(item) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (item.releaseDateText.isNotBlank()) {
                                    Text(
                                        text = item.releaseDateText.take(10),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MuvioChampagne,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 3.dp, start = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 9. Live API Categories (includes 18+, excludes sports/cricket/football/originals)
            if (uiState.apiCategories.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.section_categories),
                        eyebrow = "COLLECTIONS"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val displayCategories = remember(uiState.apiCategories) {
                        val hasAdult = uiState.apiCategories.any {
                            it.path.contains("adult", ignoreCase = true) || it.name.contains("18+", ignoreCase = true)
                        }
                        if (hasAdult) {
                            uiState.apiCategories.take(28)
                        } else {
                            listOf(ApiCategory("adult18plus", "18+ Mature", "/adult18plus")) + uiState.apiCategories.take(27)
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(displayCategories, key = { idx, cat -> "cat_${cat.path}_$idx" }) { _, apiCat ->
                            val isAdultCat = apiCat.path.contains("adult", ignoreCase = true) || apiCat.name.contains("18+", ignoreCase = true)
                            Surface(
                                color = if (isAdultCat) MuvioChampagne.copy(alpha = 0.16f) else MuvioSurfaceCard,
                                border = BorderStroke(
                                    0.9.dp,
                                    if (isAdultCat) MuvioChampagne.copy(alpha = 0.7f) else MuvioOutline
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onApiCategoryClick(apiCat) }
                                    .testTag("api_category_${apiCat.id}")
                            ) {
                                Text(
                                    text = apiCat.name,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isAdultCat) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isAdultCat) MuvioChampagneLight else MuvioTextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 10. Gems & Hot Short TV (Live Only)
            if (gemsList.isNotEmpty() && activeTopTab != "18+") {
                item(key = "home_gems", contentType = "shelf") {
                    SectionHeader(
                        title = stringResource(R.string.section_gems),
                        eyebrow = "CURATED",
                        onSeeAllClick = { onSeeAllClick("Gems for you") }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(gemsList, key = { idx, item -> "gems_${item.id}_$idx" }, contentType = { _, _ -> "poster" }) { _, item ->
                            MediaPosterCard(
                                item = item,
                                onClick = { onMediaClick(item) },
                                modifier = Modifier.width(126.dp)
                            )
                        }
                    }
                }
            }

            if (hotShortList.isNotEmpty() && activeTopTab != "18+") {
                item(key = "home_hot_short", contentType = "shelf") {
                    SectionHeader(
                        title = stringResource(R.string.section_hot_short_tv),
                        eyebrow = "MICRO DRAMAS",
                        onSeeAllClick = { onSeeAllClick("Hot Short TV") }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(hotShortList, key = { idx, item -> "short_${item.id}_$idx" }, contentType = { _, _ -> "poster" }) { _, item ->
                            MediaPosterCard(
                                item = item,
                                onClick = { onMediaClick(item) },
                                modifier = Modifier.width(126.dp)
                            )
                        }
                    }
                }
            }

            // 11. Most Trending 3-Column Editorial Showcase (Live Only)
            if (mostTrendingGrid.isNotEmpty() && activeTopTab != "18+") {
                item(key = "home_most_trending_grid", contentType = "grid") {
                    SectionHeader(
                        title = stringResource(R.string.section_most_trending),
                        eyebrow = "SPOTLIGHT",
                        onSeeAllClick = { onSeeAllClick("Most Trending") }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        mostTrendingGrid.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { item ->
                                    MediaPosterCard(
                                        item = item,
                                        onClick = { onMediaClick(item) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 12. Additional Live API Sections from /home
            if (uiState.isApiConnected && uiState.liveSections.isNotEmpty() && activeTopTab != "18+") {
                uiState.liveSections.entries.forEach { (sectionTitle, sectionItems) ->
                    if (sectionItems.isNotEmpty()) {
                        item(key = "live_sec_$sectionTitle") {
                            SectionHeader(
                                title = sectionTitle,
                                eyebrow = "LIVE SHELF",
                                onSeeAllClick = { onSeeAllClick(sectionTitle) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(sectionItems, key = { idx, item -> "ls_${sectionTitle}_${item.id}_$idx" }) { _, item ->
                                    MediaPosterCard(
                                        item = item,
                                        onClick = { onMediaClick(item) },
                                        modifier = Modifier.width(126.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoirFilterChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            val isSelected = option == selected
            Surface(
                color = if (isSelected) MuvioCrimsonContainer else Color.Transparent,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(
                    0.9.dp,
                    if (isSelected) MuvioCrimsonBorder else MuvioOutline.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelect(option) }
                    .testTag("filter_chip_$option")
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) MuvioCrimsonGlow else MuvioTextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}
