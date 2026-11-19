package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import com.example.ui.theme.AutoAwesome
import com.example.ui.theme.Bookmark
import com.example.ui.theme.BookmarkBorder
import com.example.ui.theme.NotificationsActive
import com.example.ui.theme.NotificationsNone
import com.example.ui.theme.Shuffle
import com.example.ui.theme.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ContentSource
import com.example.model.LiveMatchItem
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.PlatformBrand
import kotlinx.coroutines.delay
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.MuvioPosterArtwork
import com.example.ui.components.MuvioWordmark
import com.example.ui.theme.MuvioBg
import com.example.ui.theme.MuvioBronze
import com.example.ui.theme.MuvioChampagne
import com.example.ui.theme.MuvioChampagneLight
import com.example.ui.theme.MuvioGlass
import com.example.ui.theme.MuvioGlassBorder
import com.example.ui.theme.MuvioLiveRed
import com.example.ui.theme.MuvioOutline
import com.example.ui.theme.MuvioSurfaceCard
import com.example.ui.theme.MuvioSurfaceElevated
import com.example.ui.theme.MuvioTextMuted
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTextSecondary
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun NoirFullBleedHeroPager(
    heroItems: List<MediaItem>,
    watchlistIds: Set<String>,
    isApiConnected: Boolean,
    onOpenSearch: () -> Unit,
    onOpenWatchlist: () -> Unit = {},
    onMediaClick: (MediaItem) -> Unit,
    onToggleWatchlist: (String) -> Unit
) {
    if (heroItems.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { heroItems.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    // Smooth auto-rotate across trending Movies, Series, and Anime (pauses while user swipes)
    LaunchedEffect(heroItems.size, isDragged) {
        if (heroItems.size <= 1 || isDragged) return@LaunchedEffect
        while (true) {
            delay(4200L)
            val nextPage = (pagerState.currentPage + 1) % heroItems.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(455.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = heroItems[page]
            val inList = item.id in watchlistIds

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onMediaClick(item) }
                    .testTag("hero_banner_${item.id}")
            ) {
                val heroUrl = item.backdropUrl ?: item.poster.remoteUrl
                if (!heroUrl.isNullOrBlank()) {
                    val heroContext = androidx.compose.ui.platform.LocalContext.current
                    val heroRequest = remember(heroUrl, item.backdropRes) {
                        coil.request.ImageRequest.Builder(heroContext)
                            .data(heroUrl)
                            .size(900, 1200)
                            .precision(coil.size.Precision.INEXACT)
                            .memoryCacheKey(heroUrl)
                            .diskCacheKey(heroUrl)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .allowRgb565(true)
                            .crossfade(120)
                            .error(item.backdropRes)
                            .build()
                    }
                    AsyncImage(
                        model = heroRequest,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = item.backdropRes),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Multi-stop Noir velvet cinema scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to MuvioBg.copy(alpha = 0.72f),
                                    0.28f to Color.Transparent,
                                    0.58f to MuvioBg.copy(alpha = 0.55f),
                                    0.85f to MuvioBg.copy(alpha = 0.94f),
                                    1.0f to MuvioBg
                                )
                            )
                        )
                )

                // Editorial Bottom Showcase Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    val typeBadge = when {
                        item.mediaType == MediaType.ANIME || "anime" in item.sectionTags -> "TRENDING ANIME"
                        item.mediaType == MediaType.SERIES || item.mediaType == MediaType.DRAMA -> "TRENDING SERIES"
                        else -> "TRENDING MOVIE"
                    }
                    Surface(
                        color = MuvioChampagne.copy(alpha = 0.16f),
                        border = BorderStroke(0.8.dp, MuvioChampagne.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "$typeBadge  •  ${item.languageBadge?.uppercase() ?: "4K HDR"}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 1.4.sp
                            ),
                            color = MuvioChampagneLight,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = PlayfairFontFamily,
                            fontSize = 30.sp,
                            lineHeight = 35.sp
                        ),
                        color = MuvioTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MuvioChampagne,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.rating.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MuvioChampagne
                        )
                        Text(
                            text = "   •   ${item.year}   •   ${item.genre}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuvioTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Luxury Action CTAs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable { onMediaClick(item) }
                                .testTag("hero_play_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(MuvioChampagneLight, MuvioChampagne, MuvioBronze)
                                        )
                                    )
                                    .padding(horizontal = 24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Watch Now",
                                    tint = Color(0xFF140F05),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Watch Now",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFF140F05)
                                )
                            }
                        }

                        Surface(
                            color = MuvioGlass,
                            border = BorderStroke(1.dp, if (inList) MuvioChampagne else MuvioOutline),
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .clickable { onToggleWatchlist(item.id) }
                                .testTag("hero_watchlist_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (inList) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = "My List",
                                    tint = if (inList) MuvioChampagne else MuvioTextPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (inList) "In List" else "My List",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (inList) MuvioChampagne else MuvioTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Minimal Top Header (Overlaid transparently on Hero)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MuvioWordmark()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isApiConnected) {
                    Surface(
                        color = MuvioGlass,
                        border = BorderStroke(0.8.dp, MuvioGlassBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MuvioChampagne)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MuvioChampagne
                            )
                        }
                    }
                }

                // Translucent Floating Watchlist Shortcut Orb
                Box {
                    Surface(
                        color = MuvioGlass,
                        border = BorderStroke(1.dp, MuvioGlassBorder),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenWatchlist)
                            .testTag("home_watchlist_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (watchlistIds.isNotEmpty()) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Open Watchlist",
                                tint = if (watchlistIds.isNotEmpty()) MuvioChampagne else MuvioTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (watchlistIds.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(3.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MuvioChampagne)
                        )
                    }
                }

                // Translucent Floating Search Orb
                Surface(
                    color = MuvioGlass,
                    border = BorderStroke(1.dp, MuvioGlassBorder),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenSearch)
                        .testTag("search_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search movies and series",
                            tint = MuvioTextPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        // Slim Architectural Pager Indicator Bars
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(heroItems.size) { index ->
                val active = pagerState.currentPage == index
                val indicatorWidth by animateDpAsState(
                    targetValue = if (active) 24.dp else 7.dp,
                    animationSpec = tween(durationMillis = 300),
                    label = "heroIndicatorWidth"
                )
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(indicatorWidth)
                        .clip(CircleShape)
                        .background(if (active) MuvioChampagne else MuvioOutline)
                )
            }
        }
    }
}

@Composable
fun NoirMoodConciergeSection(
    selectedMood: String,
    surprisePick: MediaItem?,
    onSelectMood: (String) -> Unit,
    onSurpriseMe: () -> Unit,
    onDismissSurprise: () -> Unit,
    onMediaClick: (MediaItem) -> Unit
) {
    val moods = remember {
        listOf("All", "Late Night Thriller", "Comfort Drama", "Anime Marathon", "Bengali Gems", "Action Rush")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TONIGHT'S MOOD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 1.6.sp
                ),
                color = MuvioTextSecondary
            )

            // Surprise Me Concierge CTA
            Surface(
                color = MuvioChampagne.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MuvioChampagne.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onSurpriseMe)
                    .testTag("surprise_me_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MuvioChampagne,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Pick For Me",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MuvioChampagneLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(moods) { mood ->
                val active = mood == selectedMood
                Surface(
                    color = if (active) MuvioChampagne else MuvioSurfaceCard,
                    border = BorderStroke(
                        0.8.dp,
                        if (active) MuvioChampagne else MuvioOutline
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectMood(mood) }
                        .testTag("mood_chip_$mood")
                ) {
                    Text(
                        text = mood,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (active) Color(0xFF140F05) else MuvioTextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Animated Concierge Spotlight Card
        AnimatedVisibility(
            visible = surprisePick != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            surprisePick?.let { pick ->
                Surface(
                    color = MuvioSurfaceCard,
                    border = BorderStroke(1.dp, MuvioGlassBorder),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("surprise_pick_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MuvioChampagne.copy(alpha = 0.08f),
                                        MuvioSurfaceCard
                                    )
                                )
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(78.dp)
                                .height(112.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onMediaClick(pick) }
                        ) {
                            MuvioPosterArtwork(
                                poster = pick.poster,
                                contentDescription = pick.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CONCIERGE PICK",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                                    color = MuvioChampagne
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss",
                                    tint = MuvioTextMuted,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable(onClick = onDismissSurprise)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pick.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = PlayfairFontFamily),
                                color = MuvioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "★ ${pick.rating} • ${pick.year} • ${pick.genre}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MuvioTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = MuvioChampagne,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onMediaClick(pick) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFF140F05),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Watch",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF140F05)
                                        )
                                    }
                                }
                                Surface(
                                    color = MuvioSurfaceElevated,
                                    border = BorderStroke(0.8.dp, MuvioOutline),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(onClick = onSurpriseMe)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Shuffle",
                                            tint = MuvioTextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Another",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MuvioTextPrimary
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
}

@Composable
fun NoirContinueWatchingRail(
    items: List<Pair<MediaItem, Float>>,
    onMediaClick: (MediaItem) -> Unit
) {
    if (items.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { "cw_${it.first.id}" }) { (media, progress) ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MuvioSurfaceCard,
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.08f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .width(238.dp)
                    .height(134.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onMediaClick(media) }
                    .testTag("continue_watching_${media.id}")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val bgUrl = media.backdropUrl ?: media.poster.remoteUrl
                    if (!bgUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = bgUrl,
                            contentDescription = media.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = media.backdropRes),
                            contentDescription = media.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.20f),
                                        Color.Black.copy(alpha = 0.84f)
                                    )
                                )
                            )
                    )

                    // Center Frosted Play Emblem
                    Surface(
                        color = MuvioGlass,
                        border = BorderStroke(1.dp, MuvioChampagne.copy(alpha = 0.75f)),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Resume ${media.title}",
                            tint = MuvioChampagneLight,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = media.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MuvioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MuvioChampagne
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        // Slim Champagne Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(MuvioChampagne)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoirPlatformTile(
    brand: PlatformBrand,
    onClick: () -> Unit
) {
    Surface(
        color = MuvioSurfaceCard,
        border = BorderStroke(0.9.dp, Color(brand.accentHex).copy(alpha = 0.42f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("platform_chip_${brand.apiName}")
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(brand.accentHex).copy(alpha = 0.16f),
                            MuvioSurfaceCard
                        )
                    )
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(Color(brand.accentHex))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = brand.displayName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                ),
                color = MuvioTextPrimary
            )
        }
    }
}

@Composable
fun NoirLiveMatchCard(
    match: LiveMatchItem,
    onToggleNotify: () -> Unit,
    onWatchLive: () -> Unit
) {
    val hasLiveStream = match.streamUrl.isNotBlank()
    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (hasLiveStream) MuvioGlassBorder else MuvioOutline
        ),
        color = MuvioSurfaceCard,
        shadowElevation = 6.dp,
        modifier = Modifier
            .width(288.dp)
            .clickable { if (hasLiveStream) onWatchLive() else onToggleNotify() }
            .testTag("live_match_${match.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A1612),
                            MuvioSurfaceCard,
                            Color(0xFF12121A)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SportsSoccer,
                        contentDescription = null,
                        tint = MuvioChampagne,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = match.league.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            letterSpacing = 1.sp
                        ),
                        color = MuvioTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (match.isLive || hasLiveStream) {
                    Surface(
                        color = MuvioLiveRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "● LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MuvioLiveRed,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NoirTeamColumn(
                    shortCode = match.homeShort,
                    name = match.homeTeam,
                    logoUrl = match.homeLogoUrl,
                    badgeColor = Color(match.homeBadgeColor),
                    modifier = Modifier.weight(1f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    if (!match.isLive && match.subTime.isNotBlank()) {
                        Text(
                            text = match.subTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MuvioTextMuted
                        )
                    }
                    Text(
                        text = match.scoreOrTime,
                        fontFamily = PlayfairFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (match.isLive) MuvioChampagne else MuvioTextPrimary,
                        maxLines = 1
                    )
                }

                NoirTeamColumn(
                    shortCode = match.awayShort,
                    name = match.awayTeam,
                    logoUrl = match.awayLogoUrl,
                    badgeColor = Color(match.awayBadgeColor),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = if (hasLiveStream) MuvioChampagne else MuvioSurfaceElevated,
                border = if (!hasLiveStream) BorderStroke(0.8.dp, MuvioOutline) else null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clickable { if (hasLiveStream) onWatchLive() else onToggleNotify() }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (hasLiveStream) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF140F05),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    } else if (!match.isLive) {
                        Icon(
                            imageVector = if (match.isNotified) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                            contentDescription = null,
                            tint = MuvioChampagne,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = when {
                            hasLiveStream -> "Watch Live HD"
                            match.isLive -> "Match Info"
                            match.isNotified -> "Reminder Active"
                            else -> "Remind Me"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (hasLiveStream) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (hasLiveStream) Color(0xFF140F05) else MuvioChampagne
                    )
                }
            }
        }
    }
}

@Composable
private fun NoirTeamColumn(
    shortCode: String,
    name: String,
    logoUrl: String?,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = badgeColor.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.8f)),
            shape = CircleShape,
            modifier = Modifier.size(36.dp)
        ) {
            if (!logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = shortCode,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MuvioTextPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MuvioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NoirSearchOverlay(
    query: String,
    isSearchingRemote: Boolean,
    remoteResults: List<MediaItem>,
    selectedSourceFilter: ContentSource? = null,
    secondarySourceError: String? = null,
    hotSearches: List<String>,
    suggestions: List<String>,
    allMedia: List<MediaItem>,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectSourceFilter: (ContentSource?) -> Unit = {},
    onRetrySecondarySource: () -> Unit = {},
    onMediaClick: (MediaItem) -> Unit
) {
    val trimmed = query.trim()
    val localFiltered = remember(trimmed, allMedia) {
        if (trimmed.isEmpty()) {
            allMedia
        } else {
            allMedia.filter {
                it.title.contains(trimmed, ignoreCase = true) ||
                    it.genre.contains(trimmed, ignoreCase = true) ||
                    it.country.contains(trimmed, ignoreCase = true) ||
                    (it.languageBadge?.contains(trimmed, ignoreCase = true) == true)
            }
        }
    }
    val combinedResults = remember(trimmed, remoteResults, localFiltered) {
        val raw = if (trimmed.isNotEmpty() && remoteResults.isNotEmpty()) {
            (remoteResults + localFiltered).distinctBy { it.id }
        } else {
            localFiltered
        }
        val p = raw.filter { it.contentSource == ContentSource.PRIMARY }
        val s = raw.filter { it.contentSource == ContentSource.SECONDARY }
        if (p.isEmpty() || s.isEmpty()) {
            raw
        } else {
            val out = mutableListOf<MediaItem>()
            val maxLen = maxOf(p.size, s.size)
            for (i in 0 until maxLen) {
                if (i < p.size) out += p[i]
                if (i < s.size) out += s[i]
            }
            out
        }
    }
    val primaryCount = remember(combinedResults) { combinedResults.count { it.contentSource == ContentSource.PRIMARY } }
    val secondaryCount = remember(combinedResults) { combinedResults.count { it.contentSource == ContentSource.SECONDARY } }
    val results = remember(combinedResults, selectedSourceFilter) {
        if (selectedSourceFilter == null) {
            combinedResults
        } else {
            combinedResults.filter { it.contentSource == selectedSourceFilter }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MuvioBg)
            .statusBarsPadding()
    ) {
        // Luxury Frosted Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Close search",
                    tint = MuvioTextPrimary
                )
            }
            Surface(
                color = MuvioSurfaceElevated,
                border = BorderStroke(1.dp, MuvioGlassBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MuvioChampagne,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MuvioTextPrimary),
                            cursorBrush = SolidColor(MuvioChampagne),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input_field"),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search films, dramas, anime...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MuvioTextMuted
                                    )
                                }
                                inner()
                            }
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MuvioTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isSearchingRemote) {
            LinearProgressIndicator(
                color = MuvioChampagne,
                trackColor = MuvioOutline.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (trimmed.isNotEmpty() && suggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "INSTANT SUGGESTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                        color = MuvioTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(suggestions) { sug ->
                            Surface(
                                color = MuvioSurfaceCard,
                                border = BorderStroke(1.dp, MuvioChampagne.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onQueryChange(sug) }
                            ) {
                                Text(
                                    text = sug,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MuvioTextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "TRENDING SEARCHES",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = MuvioChampagne
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hotSearches) { tag ->
                        Surface(
                            color = MuvioSurfaceElevated,
                            border = BorderStroke(0.8.dp, MuvioOutline),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onQueryChange(tag) }
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelLarge,
                                color = MuvioChampagneLight,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Dual-Source Search Filter Row: All (50/50) | Source 1 | Source 2
            item {
                val filterOptions = listOf(
                    null to "All (50/50)",
                    ContentSource.PRIMARY to "Source 1 ($primaryCount)",
                    ContentSource.SECONDARY to "Source 2 ($secondaryCount)"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterOptions) { (source, label) ->
                        val active = selectedSourceFilter == source
                        Surface(
                            color = if (active) MuvioChampagne else MuvioSurfaceCard,
                            border = BorderStroke(
                                1.dp,
                                if (active) MuvioChampagne else MuvioOutline.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onSelectSourceFilter(source) }
                                .testTag("search_source_chip_${source?.name ?: "ALL"}")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) Color(0xFF140F05) else MuvioTextPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            if (!secondarySourceError.isNullOrBlank() && (selectedSourceFilter == ContentSource.SECONDARY || secondaryCount == 0)) {
                item {
                    Surface(
                        color = MuvioSurfaceCard,
                        border = BorderStroke(0.9.dp, MuvioChampagne.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRetrySecondarySource)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = secondarySourceError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MuvioTextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Retry",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MuvioChampagne
                            )
                        }
                    }
                }
            }

            if (results.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 54.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No titles matched \"$trimmed\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MuvioTextSecondary
                        )
                    }
                }
            } else {
                items(results.chunked(3)) { rowItems ->
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

            item { Spacer(modifier = Modifier.height(104.dp)) }
        }
    }
}
