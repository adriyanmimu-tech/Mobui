package com.example.ui.screens

import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import com.example.ui.theme.Api
import com.example.ui.theme.AutoStories
import com.example.ui.theme.Bookmark
import com.example.ui.theme.BookmarkBorder
import com.example.ui.theme.ChatBubbleOutline
import com.example.ui.theme.DeleteOutline
import com.example.ui.theme.Pause
import com.example.ui.theme.VolumeOff
import com.example.ui.theme.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.MuvioExoPlayerFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.model.DownloadItem
import com.example.model.DownloadStatus
import com.example.model.FilterOptions
import com.example.model.MediaItem
import com.example.model.MediaType
import com.example.model.NovelItem
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.MuvioBrandMark
import com.example.ui.components.MuvioPosterArtwork
import com.example.ui.theme.MuvioBg
import com.example.ui.theme.MuvioChampagne
import com.example.ui.theme.MuvioChampagneLight
import com.example.ui.theme.MuvioCyan
import com.example.ui.theme.MuvioEmerald
import com.example.ui.theme.MuvioEmeraldContainer
import com.example.ui.theme.MuvioGlass
import com.example.ui.theme.MuvioGlassBorder
import com.example.ui.theme.MuvioGold
import com.example.ui.theme.MuvioHotRed
import com.example.ui.theme.MuvioOutline
import com.example.ui.theme.MuvioSurface
import com.example.ui.theme.MuvioSurfaceCard
import com.example.ui.theme.MuvioSurfaceElevated
import com.example.ui.theme.MuvioTextMuted
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTextSecondary
import com.example.ui.theme.PlayfairFontFamily
import com.example.ui.viewmodel.MuvioUiState

@OptIn(UnstableApi::class)
@Composable
fun ShortsScreen(
    uiState: MuvioUiState,
    onToggleLike: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onWatchFullSeries: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reels = uiState.shortReels
    if (reels.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MuvioBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MuvioChampagne)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading live short dramas...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuvioTextSecondary
                )
            }
        }
        return
    }
    val pagerState = rememberPagerState(pageCount = { reels.size })
    var isMuted by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val reel = reels[page]
            val isCurrentPage = pagerState.currentPage == page
            var paused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { paused = !paused }
                    .testTag("shorts_reel_${reel.id}")
            ) {
                MuvioPosterArtwork(
                    poster = reel.poster,
                    contentDescription = reel.title,
                    modifier = Modifier.fillMaxSize()
                )

                if (isCurrentPage && reel.streamUrl.isNotBlank()) {
                    val context = LocalContext.current
                    val shortPlayer = remember(reel.streamUrl) {
                        MuvioExoPlayerFactory.createShortsPlayer(context).apply {
                            setMediaItem(ExoMediaItem.fromUri(reel.streamUrl))
                            repeatMode = Player.REPEAT_MODE_ONE
                            prepare()
                            playWhenReady = true
                        }
                    }
                    DisposableEffect(shortPlayer) {
                        onDispose { shortPlayer.release() }
                    }
                    LaunchedEffect(paused, isMuted, shortPlayer) {
                        shortPlayer.playWhenReady = !paused
                        shortPlayer.volume = if (isMuted) 0f else 1f
                    }
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = shortPlayer
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                setShutterBackgroundColor(android.graphics.Color.BLACK)
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                keepScreenOn = !paused
                            }
                        },
                        update = { view ->
                            if (view.player !== shortPlayer) view.player = shortPlayer
                            view.keepScreenOn = !paused
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                if (paused) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Paused",
                            tint = Color.White,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // Right-side social stack
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 112.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { onToggleLike(reel.id) }) {
                            Icon(
                                imageVector = if (reel.isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like reel",
                                tint = if (reel.isLiked) MuvioHotRed else Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = "${reel.likesCount / 1000}.${(reel.likesCount % 1000) / 100}K",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { onWatchFullSeries(reel.linkedMediaId) }) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = reel.commentsCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = { onToggleSave(reel.id) }) {
                        Icon(
                            imageVector = if (reel.isSaved) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save reel",
                            tint = if (reel.isSaved) MuvioEmerald else Color.White,
                            modifier = Modifier.size(29.dp)
                        )
                    }
                }

                // Bottom-left Title & Watch Full CTA
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.78f)
                        .padding(start = 16.dp, bottom = 110.dp)
                ) {
                    Surface(
                        color = MuvioEmeraldContainer,
                        border = BorderStroke(1.dp, MuvioEmerald),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = reel.episodeTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MuvioEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = reel.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reel.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MuvioEmerald,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { onWatchFullSeries(reel.linkedMediaId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Watch Full Drama",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Top Shorts Title Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Muvio Shorts",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { isMuted = !isMuted }) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = if (isMuted) "Unmute shorts" else "Mute shorts",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Swipe up",
                    style = MaterialTheme.typography.labelMedium,
                    color = MuvioEmerald
                )
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelHubScreen(
    uiState: MuvioUiState,
    modifier: Modifier = Modifier
) {
    var activeNovel by remember { mutableStateOf<NovelItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MuvioBg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NovelHub",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MuvioTextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = MuvioHotRed, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "HOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Original stories behind Muvio hit dramas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MuvioTextSecondary
                )
            }
            Icon(
                imageVector = Icons.Rounded.AutoStories,
                contentDescription = null,
                tint = MuvioEmerald,
                modifier = Modifier.size(28.dp)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.novels, key = { it.id }) { novel ->
                Surface(
                    color = MuvioSurfaceCard,
                    border = BorderStroke(1.dp, MuvioOutline),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeNovel = novel }
                        .testTag("novel_card_${novel.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(78.dp)
                                .height(106.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            MuvioPosterArtwork(
                                poster = novel.poster,
                                contentDescription = novel.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = novel.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MuvioTextPrimary
                            )
                            Text(
                                text = "By ${novel.author} • ${novel.chaptersCount} Chapters",
                                style = MaterialTheme.typography.bodySmall,
                                color = MuvioEmerald
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = novel.synopsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MuvioTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = MuvioGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${novel.rating} • ${novel.genre}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MuvioTextSecondary
                                    )
                                }
                                Text(
                                    text = "Read Ch.1 >",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MuvioEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    activeNovel?.let { novel ->
        ModalBottomSheet(
            onDismissRequest = { activeNovel = null },
            containerColor = MuvioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MuvioTextPrimary
                )
                Text(
                    text = "Chapter 1 • ${novel.author}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MuvioEmerald
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = novel.sampleChapter,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                    color = MuvioTextPrimary
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DownloadsScreen(
    downloads: List<DownloadItem>,
    onTogglePause: (String) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onOpenMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MuvioBg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.headlineSmall,
                color = MuvioTextPrimary
            )
            Text(
                text = "${downloads.size} items",
                style = MaterialTheme.typography.labelLarge,
                color = MuvioEmerald
            )
        }

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No offline videos yet. Tap Download on any movie or drama to save it here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MuvioTextSecondary
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(downloads, key = { it.mediaId }) { item ->
                    Surface(
                        color = MuvioSurfaceCard,
                        border = BorderStroke(1.dp, MuvioOutline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenMedia(item.mediaId) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(86.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                MuvioPosterArtwork(
                                    poster = item.poster,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MuvioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${item.quality} • ${item.sizeText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuvioTextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                if (item.status == DownloadStatus.COMPLETED) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MuvioEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Saved for offline playback",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MuvioEmerald
                                        )
                                    }
                                } else {
                                    LinearProgressIndicator(
                                        progress = { item.progress },
                                        color = MuvioEmerald,
                                        trackColor = MuvioOutline,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            }

                            if (item.status != DownloadStatus.COMPLETED) {
                                IconButton(onClick = { onTogglePause(item.mediaId) }) {
                                    Icon(
                                        imageVector = if (item.status == DownloadStatus.DOWNLOADING) {
                                            Icons.Rounded.Pause
                                        } else {
                                            Icons.Rounded.PlayArrow
                                        },
                                        contentDescription = "Pause or resume",
                                        tint = MuvioEmerald
                                    )
                                }
                            }
                            IconButton(onClick = { onRemoveDownload(item.mediaId) }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete download",
                                    tint = MuvioTextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MeScreen(
    uiState: MuvioUiState,
    onMediaClick: (MediaItem) -> Unit,
    onToggleAutoPlay: () -> Unit,
    onToggleWifiOnly: () -> Unit,
    onCycleQuality: () -> Unit,
    onRefreshApi: () -> Unit = {},
    onSelectMeSection: (Int) -> Unit = {},
    onToggleWatchlist: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val watchlistMedia = remember(uiState.allMedia, uiState.watchlistIds) {
        uiState.allMedia.filter { it.id in uiState.watchlistIds }
    }
    val historyMedia = remember(uiState.allMedia, uiState.watchHistoryIds) {
        uiState.watchHistoryIds.mapNotNull { id -> uiState.allMedia.find { it.id == id } }
    }
    var watchlistFilter by remember { mutableStateOf("All") }
    val filteredWatchlist = remember(watchlistMedia, watchlistFilter) {
        when (watchlistFilter) {
            "Movies" -> watchlistMedia.filter { it.mediaType == MediaType.MOVIE }
            "Series" -> watchlistMedia.filter { it.mediaType != MediaType.MOVIE }
            else -> watchlistMedia
        }
    }
    val sections = remember { listOf("Watchlist", "History", "Settings") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MuvioBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 122.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header
        item {
            Surface(
                color = MuvioSurfaceCard,
                border = BorderStroke(1.dp, MuvioChampagne.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MuvioBrandMark(size = 50.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Muvio Atelier",
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlayfairFontFamily),
                            color = MuvioTextPrimary
                        )
                        Text(
                            text = "Watchlist: ${watchlistMedia.size}  •  History: ${historyMedia.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuvioTextSecondary
                        )
                    }
                    Surface(
                        color = MuvioEmeraldContainer,
                        border = BorderStroke(0.8.dp, MuvioChampagne.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (uiState.isApiConnected) "NOIR LIVE" else "NOIR PRO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MuvioChampagne,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // 3-Segment Noir Switcher: Watchlist | History | Settings
        item {
            Surface(
                color = MuvioSurfaceCard,
                border = BorderStroke(0.9.dp, MuvioGlassBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sections.forEachIndexed { idx, label ->
                        val active = uiState.meSectionIndex == idx
                        val badgeCount = when (idx) {
                            0 -> watchlistMedia.size
                            1 -> historyMedia.size
                            else -> 0
                        }
                        Surface(
                            color = if (active) MuvioChampagne else Color.Transparent,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectMeSection(idx) }
                                .testTag("me_tab_${label.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (badgeCount > 0) "$label ($badgeCount)" else label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (active) Color(0xFF140F05) else MuvioTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        when (uiState.meSectionIndex) {
            // TAB 0: WATCHLIST (Filter Chips + 3-Column Poster Grid with 1-Tap Bookmark Remove)
            0 -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All", "Movies", "Series").forEach { filter ->
                            val selected = watchlistFilter == filter
                            Surface(
                                color = if (selected) MuvioEmeraldContainer else MuvioSurfaceCard,
                                border = BorderStroke(
                                    0.9.dp,
                                    if (selected) MuvioChampagne else MuvioOutline.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { watchlistFilter = filter }
                                    .testTag("watchlist_filter_${filter.lowercase()}")
                            ) {
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (selected) MuvioChampagneLight else MuvioTextSecondary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                if (filteredWatchlist.isEmpty()) {
                    item {
                        Surface(
                            color = MuvioSurfaceCard,
                            border = BorderStroke(0.9.dp, MuvioOutline.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = MuvioChampagne,
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (watchlistMedia.isEmpty()) "Your Watchlist is Empty" else "No $watchlistFilter in Watchlist",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MuvioTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap '+ My List' on any movie, series, or anime to save it here for quick access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuvioTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(filteredWatchlist.chunked(3)) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                Box(modifier = Modifier.weight(1f)) {
                                    MediaPosterCard(
                                        item = item,
                                        onClick = { onMediaClick(item) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    // 1-Tap Bookmark Remove Pill on top-right
                                    Surface(
                                        color = MuvioGlass,
                                        border = BorderStroke(0.8.dp, MuvioGlassBorder),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .clickable { onToggleWatchlist(item.id) }
                                            .testTag("watchlist_remove_${item.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Bookmark,
                                                contentDescription = "Remove ${item.title} from Watchlist",
                                                tint = MuvioChampagne,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // TAB 1: WATCH HISTORY (16:9 Progress Cards)
            1 -> {
                if (historyMedia.isEmpty()) {
                    item {
                        Surface(
                            color = MuvioSurfaceCard,
                            border = BorderStroke(0.9.dp, MuvioOutline.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MuvioChampagne,
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No Watch History Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MuvioTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Movies and episodes you watch will appear here so you can resume anytime.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuvioTextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(historyMedia, key = { "hist_row_${it.id}" }) { item ->
                        val progress = (uiState.watchProgressByMedia[item.id] ?: 0.25f).coerceIn(0.05f, 1f)
                        Surface(
                            color = MuvioSurfaceCard,
                            border = BorderStroke(0.9.dp, MuvioOutline.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onMediaClick(item) }
                                .testTag("history_card_${item.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(114.dp)
                                        .height(66.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MuvioSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val thumbUrl = item.backdropUrl ?: item.poster.remoteUrl
                                    if (!thumbUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = thumbUrl,
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                    Surface(
                                        color = MuvioGlass,
                                        border = BorderStroke(0.8.dp, MuvioChampagne),
                                        shape = CircleShape,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Resume",
                                            tint = MuvioChampagneLight,
                                            modifier = Modifier.padding(5.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MuvioTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.year} • ${item.genre}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MuvioTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(MuvioOutline.copy(alpha = 0.5f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(progress)
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .background(MuvioChampagne)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MuvioChampagne
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: SETTINGS & LIVE API STATUS
            else -> {
                item {
                    Surface(
                        color = MuvioEmeraldContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MuvioChampagne.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onRefreshApi)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Api,
                                contentDescription = null,
                                tint = MuvioChampagne,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.isApiConnected) "MovieBox Live API Connected" else "MovieBox API Sync",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MuvioChampagne
                                )
                                Text(
                                    text = uiState.apiStatusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuvioTextSecondary
                                )
                                uiState.quotaInfo?.let { q ->
                                    val quotaLine = buildString {
                                        if (q.clientName.isNotBlank()) append("${q.clientName} • ")
                                        append(q.plan.ifBlank { q.quota })
                                        if (q.expiresAt.isNotBlank()) append(" • Exp ${q.expiresAt}")
                                    }
                                    if (quotaLine.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = quotaLine,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MuvioCyan
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (uiState.isLoadingHome) "..." else "Sync",
                                style = MaterialTheme.typography.labelLarge,
                                color = MuvioChampagne
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Playback & App Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MuvioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MuvioSurfaceCard,
                        border = BorderStroke(1.dp, MuvioOutline),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Auto-play next episode", style = MaterialTheme.typography.bodyLarge, color = MuvioTextPrimary)
                                    Text("Seamless drama & anime bingeing", style = MaterialTheme.typography.bodySmall, color = MuvioTextSecondary)
                                }
                                Switch(
                                    checked = uiState.autoPlayNext,
                                    onCheckedChange = { onToggleAutoPlay() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = MuvioChampagne)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Download on Wi-Fi only", style = MaterialTheme.typography.bodyLarge, color = MuvioTextPrimary)
                                    Text("Protect mobile data", style = MaterialTheme.typography.bodySmall, color = MuvioTextSecondary)
                                }
                                Switch(
                                    checked = uiState.wifiOnlyDownloads,
                                    onCheckedChange = { onToggleWifiOnly() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = MuvioChampagne)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onCycleQuality)
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Default Streaming Quality", style = MaterialTheme.typography.bodyLarge, color = MuvioTextPrimary)
                                Text(uiState.preferredQuality, style = MaterialTheme.typography.labelLarge, color = MuvioChampagne)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeeAllCategoryScreen(
    sectionTitle: String,
    itemsList: List<MediaItem>,
    filterOptions: FilterOptions = FilterOptions(),
    isLoadingMore: Boolean = false,
    canLoadMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onBack: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGenre by remember { mutableStateOf("All") }
    val genreChips = remember(filterOptions.genres) {
        if (filterOptions.genres.isNotEmpty()) {
            listOf("All") + filterOptions.genres.take(10)
        } else {
            listOf("All", "Action", "Drama", "Thriller", "Comedy", "Sci-Fi", "Anime")
        }
    }
    val displayedItems = remember(itemsList, selectedGenre) {
        if (selectedGenre == "All") {
            itemsList
        } else {
            itemsList.filter {
                it.genre.contains(selectedGenre, ignoreCase = true) ||
                    it.country.contains(selectedGenre, ignoreCase = true) ||
                    it.subCategory.contains(selectedGenre, ignoreCase = true)
            }.ifEmpty { itemsList }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MuvioBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MuvioTextPrimary
                )
            }
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MuvioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${displayedItems.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MuvioEmerald,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        // Filter Chips from /filter-options
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genreChips) { chip ->
                val isSelected = chip == selectedGenre
                Surface(
                    color = if (isSelected) MuvioEmeraldContainer else MuvioSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) MuvioEmerald else MuvioOutline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedGenre = chip }
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MuvioEmerald else MuvioTextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (isLoadingMore && displayedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MuvioEmerald)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedItems.chunked(3)) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                if (canLoadMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    color = MuvioEmerald,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Surface(
                                    color = MuvioEmeraldContainer,
                                    border = BorderStroke(1.dp, MuvioEmerald),
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(22.dp))
                                        .clickable(onClick = onLoadMore)
                                        .testTag("load_more_category_btn")
                                ) {
                                    Text(
                                        text = "Load More",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MuvioEmerald,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
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
