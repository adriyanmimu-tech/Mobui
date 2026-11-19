package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import com.example.ui.theme.Bedtime
import com.example.ui.theme.Cast
import com.example.ui.theme.ChatBubbleOutline
import com.example.ui.theme.ClosedCaption
import com.example.ui.theme.DarkMode
import com.example.ui.theme.Download
import com.example.ui.theme.Forward10
import com.example.ui.theme.Fullscreen
import com.example.ui.theme.FullscreenExit
import com.example.ui.theme.HelpOutline
import com.example.ui.theme.LocalMovies
import com.example.ui.theme.Pause
import com.example.ui.theme.PictureInPictureAlt
import com.example.ui.theme.PlaylistAdd
import com.example.ui.theme.Replay10
import com.example.ui.theme.Tune
import com.example.ui.theme.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.CircularProgressIndicator
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.BengaliSubtitleService
import com.example.data.MuvioExoPlayerFactory
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.text.subrip.SubripParser
import androidx.media3.extractor.text.webvtt.WebvttParser
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CommentItem
import com.example.model.DubVersion
import com.example.model.MediaItem
import com.example.model.StaffMember
import com.example.model.StreamResource
import com.example.model.SubtitleTrack
import com.example.ui.components.MediaPosterCard
import com.example.ui.player.LocalMuvioPipController
import com.example.ui.player.PlayerGestureHudState
import com.example.ui.player.PlayerGesturePillHud
import com.example.ui.player.PlayerGestureType
import com.example.ui.player.applyMediaVolumeFraction
import com.example.ui.player.applyWindowBrightness
import com.example.ui.player.readCurrentBrightness
import com.example.ui.player.readMediaVolumeFraction
import com.example.ui.player.resetWindowBrightness
import com.example.ui.theme.MuvioBg
import com.example.ui.theme.MuvioChampagne
import com.example.ui.theme.MuvioChampagneLight
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
import com.example.ui.theme.Translate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPlayerScreen(
    media: MediaItem,
    recommendations: List<MediaItem>,
    comments: List<CommentItem>,
    liveStreams: List<StreamResource>,
    selectedStreamIndex: Int,
    isLoadingStream: Boolean,
    streamError: String? = null,
    seasonsCount: Int,
    selectedSeason: Int,
    isInWatchlist: Boolean,
    selectedLanguage: String,
    selectedEpisode: Int,
    sleepTimerMinutes: Int = 0,
    onCycleSleepTimer: () -> Unit = {},
    onProgressChange: (Float) -> Unit = {},
    onRetryStream: () -> Unit = {},
    onBack: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onDownloadClick: () -> Unit,
    onDownloadWithQuality: (String) -> Unit = { onDownloadClick() },
    onSelectLanguage: (String) -> Unit,
    onSelectDub: (DubVersion) -> Unit = {},
    onStaffClick: (StaffMember) -> Unit = {},
    onSelectStreamIndex: (Int) -> Unit,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (Int) -> Unit,
    onLoadMoreEpisodes: () -> Unit = {},
    onToggleCommentLike: (String) -> Unit,
    onPostComment: (String) -> Unit,
    onRecommendationClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val pipController = LocalMuvioPipController.current
    val isInPipMode by pipController?.isInPipMode?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val subtitleScope = rememberCoroutineScope()

    var isFullscreen by remember { mutableStateOf(false) }
    var isTheaterDim by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var progressFraction by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0 = For you (default), 1 = Comments
    var showInfoSheet by remember { mutableStateOf(false) }
    var showCommentSheet by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var showAllEpisodesSheet by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedSubIndex by remember { mutableIntStateOf(-1) }
    var commentDraft by remember { mutableStateOf("") }

    // Universal Bengali subtitle translation state (per episode)
    val episodeKey = "${media.id}_s${selectedSeason}_e${selectedEpisode}"
    var generatedBengaliTrack by remember(episodeKey) { mutableStateOf<SubtitleTrack?>(null) }
    var isTranslatingBengali by remember(episodeKey) { mutableStateOf(false) }
    var bengaliTranslateProgress by remember(episodeKey) { mutableFloatStateOf(0f) }
    var subtitleStatusBanner by remember(episodeKey) { mutableStateOf<String?>(null) }
    var bengaliJob by remember(episodeKey) { mutableStateOf<Job?>(null) }

    val availableResources = liveStreams.ifEmpty { media.resources }
    val activeResource = availableResources.getOrNull(selectedStreamIndex)
        ?: availableResources.firstOrNull { it.language == selectedLanguage }
        ?: availableResources.firstOrNull()
    val activeEpisode = media.episodes.firstOrNull { it.number == selectedEpisode }
    val streamUrl = activeResource?.streamUrl?.takeIf { it.isNotBlank() }
        ?: activeEpisode?.streamUrl?.takeIf { it.isNotBlank() }
        .orEmpty()
    val rawSubtitles = activeResource?.subtitles.orEmpty()
    val activeSubtitles = remember(rawSubtitles, generatedBengaliTrack) {
        val merged = buildList {
            generatedBengaliTrack?.let { gen ->
                if (rawSubtitles.none { it.url == gen.url }) {
                    add(gen)
                }
            }
            addAll(BengaliSubtitleService.prioritizeBengaliFirst(rawSubtitles))
        }
        merged.distinctBy { it.url }
    }
    val hasBengaliTrack = remember(activeSubtitles) {
        activeSubtitles.any { BengaliSubtitleService.isBengaliTrack(it) }
    }
    val activeQualityLabel = activeResource?.quality?.takeIf { it.isNotBlank() } ?: "HD"

    val requestBengaliSubtitle: () -> Unit = {
        val existingBnIdx = activeSubtitles.indexOfFirst { BengaliSubtitleService.isBengaliTrack(it) }
        if (existingBnIdx >= 0) {
            selectedSubIndex = existingBnIdx
            subtitleStatusBanner = "বাংলা সাবটাইটেল চালু হয়েছে"
        } else if (!isTranslatingBengali) {
            bengaliJob?.cancel()
            isTranslatingBengali = true
            bengaliTranslateProgress = 0.05f
            subtitleStatusBanner = "বাংলা সাবটাইটেল তৈরি হচ্ছে..."
            bengaliJob = subtitleScope.launch {
                val res = BengaliSubtitleService.resolveOrTranslateBengaliSubtitle(
                    context = context,
                    title = media.title,
                    season = selectedSeason,
                    episode = selectedEpisode,
                    existingSubtitles = rawSubtitles,
                    streamHeaders = activeResource?.headers.orEmpty(),
                    onProgress = { p -> bengaliTranslateProgress = p }
                )
                isTranslatingBengali = false
                res.onSuccess { bnTrack ->
                    generatedBengaliTrack = bnTrack
                    selectedSubIndex = 0
                    subtitleStatusBanner = "${bnTrack.label} চালু হয়েছে"
                }.onFailure { err ->
                    subtitleStatusBanner = err.message ?: "বাংলা সাবটাইটেল পাওয়া যায়নি"
                }
            }
        }
    }

    LaunchedEffect(subtitleStatusBanner, isTranslatingBengali) {
        if (subtitleStatusBanner != null && !isTranslatingBengali) {
            delay(2_400L)
            subtitleStatusBanner = null
        }
    }

    // Automatically select native Bengali subtitle if MovieBox/Source 2 already provides one,
    // otherwise default to the first available subtitle track.
    LaunchedEffect(episodeKey, activeSubtitles.size) {
        if (activeSubtitles.isEmpty()) {
            selectedSubIndex = -1
        } else {
            val bnIdx = activeSubtitles.indexOfFirst { BengaliSubtitleService.isBengaliTrack(it) }
            if (bnIdx >= 0 && (selectedSubIndex == -1 || selectedSubIndex >= activeSubtitles.size)) {
                selectedSubIndex = bnIdx
            } else if (selectedSubIndex == -1 || selectedSubIndex >= activeSubtitles.size) {
                selectedSubIndex = 0
            }
        }
    }

    // Handle hardware back cleanly when in fullscreen
    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            onBack()
        }
    }

    // Restore orientation, window brightness, and PiP registration when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            bengaliJob?.cancel()
            resetWindowBrightness(activity)
            pipController?.unbindPlayer()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Real Sleep Timer countdown that automatically pauses playback
    LaunchedEffect(sleepTimerMinutes, isPlaying) {
        if (sleepTimerMinutes > 0 && isPlaying) {
            delay(sleepTimerMinutes * 60_000L)
            isPlaying = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isTheaterDim || isInPipMode) Color.Black else MuvioBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top 16:9 (or Fullscreen / PiP) Video Player Stage
            PlayerStage(
                media = media,
                episodeKey = episodeKey,
                streamUrl = streamUrl,
                streamHeaders = activeResource?.headers.orEmpty(),
                isLoadingStream = isLoadingStream,
                streamError = streamError,
                subtitles = activeSubtitles,
                hasBengaliTrack = hasBengaliTrack,
                isTranslatingBengali = isTranslatingBengali,
                bengaliTranslateProgress = bengaliTranslateProgress,
                subtitleStatusBanner = subtitleStatusBanner,
                activeQualityLabel = activeQualityLabel,
                playbackSpeed = playbackSpeed,
                selectedSubIndex = selectedSubIndex,
                isPlaying = isPlaying,
                isFullscreen = isFullscreen,
                isInPipMode = isInPipMode,
                showControls = showControls && !isInPipMode,
                simulatedProgressFraction = progressFraction,
                onRetryStream = onRetryStream,
                onSelectSpeed = { playbackSpeed = it },
                onSelectSubtitle = { selectedSubIndex = it },
                onRequestBengaliSubtitle = requestBengaliSubtitle,
                onOpenQualitySheet = { showQualitySheet = true },
                onToggleControls = { showControls = !showControls },
                onAutoHideControls = { showControls = false },
                onTogglePlay = { isPlaying = !isPlaying },
                onEnterPip = {
                    val entered = pipController?.enterPip() == true
                    if (!entered) {
                        subtitleStatusBanner = "এই ডিভাইসে PiP সাপোর্ট পাওয়া যায়নি"
                    }
                },
                onProgressReport = onProgressChange,
                onSimulatedSeekFraction = { progressFraction = it },
                onSimulatedRewind10 = { progressFraction = (progressFraction - 0.04f).coerceAtLeast(0f) },
                onSimulatedForward10 = { progressFraction = (progressFraction + 0.04f).coerceAtMost(1f) },
                onBackClick = {
                    if (isFullscreen) {
                        isFullscreen = false
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    } else {
                        onBack()
                    }
                },
                onToggleFullscreen = {
                    isFullscreen = !isFullscreen
                    activity?.requestedOrientation = if (isFullscreen) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                },
                onHelpClick = { showHelpDialog = true },
                onCastClick = { showHelpDialog = true }
            )

            // 2. Scrollable Detail, Episodes, Cast & Comments Body (hidden in Fullscreen & PiP)
            if (!isFullscreen && !isInPipMode) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 36.dp)
                ) {
                    // Title & Metadata
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = media.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MuvioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showInfoSheet = true }
                                        .padding(horizontal = 4.dp, vertical = 3.dp)
                                        .testTag("detail_info_button")
                                ) {
                                    Text(
                                        text = "Info",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = MuvioTextSecondary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "Info",
                                        tint = MuvioTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val isSeries = media.episodes.isNotEmpty() || seasonsCount > 1
                            val primaryGenre = media.genre.substringBefore(",").trim().ifBlank { "Drama" }
                            val seasonSuffix = if (isSeries) {
                                val count = seasonsCount.coerceAtLeast(1)
                                "  |  $count ${if (count == 1) "season" else "seasons"}"
                            } else {
                                ""
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (isSeries) Icons.Rounded.Tv else Icons.Rounded.LocalMovies,
                                    contentDescription = null,
                                    tint = MuvioTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "  |  ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuvioOutline
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = MuvioChampagne,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${media.rating}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MuvioChampagne
                                )
                                Text(
                                    text = "  |  ${media.year}  |  ${media.country}  |  $primaryGenre$seasonSuffix",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = MuvioTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Uniform Clean Action Pills Row (Quality lives neatly in the player top bar)
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                ActionPillButton(
                                    icon = if (isInWatchlist) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    label = if (isInWatchlist) stringResource(R.string.action_added_to_list) else stringResource(R.string.action_add_to_list),
                                    highlighted = isInWatchlist,
                                    onClick = onToggleWatchlist,
                                    testTag = "btn_add_to_list"
                                )
                            }
                            item {
                                ActionPillButton(
                                    icon = Icons.Rounded.Share,
                                    label = stringResource(R.string.action_share),
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Watch ${media.title} (${media.year}) on Muvio!")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                                    },
                                    testTag = "btn_share"
                                )
                            }
                            item {
                                ActionPillButton(
                                    icon = Icons.Rounded.Download,
                                    label = stringResource(R.string.action_download),
                                    onClick = { showDownloadSheet = true },
                                    testTag = "btn_download_pill"
                                )
                            }
                            item {
                                ActionPillButton(
                                    icon = Icons.Rounded.Bedtime,
                                    label = if (sleepTimerMinutes > 0) "Sleep ${sleepTimerMinutes}m" else "Sleep",
                                    highlighted = sleepTimerMinutes > 0,
                                    onClick = onCycleSleepTimer,
                                    testTag = "btn_sleep_timer"
                                )
                            }
                            item {
                                ActionPillButton(
                                    icon = Icons.Rounded.DarkMode,
                                    label = if (isTheaterDim) "Lights On" else "Theater",
                                    highlighted = isTheaterDim,
                                    onClick = { isTheaterDim = !isTheaterDim },
                                    testTag = "btn_theater_dim"
                                )
                            }
                        }
                    }

                    // Compact Muvio Noir Audio/Dub & Episode Strip ([All] [01] [02] [03]...)
                    item {
                        NoirCompactAudioEpisodesSection(
                            media = media,
                            availableResources = availableResources,
                            activeQualityLabel = activeQualityLabel,
                            selectedLanguage = selectedLanguage,
                            seasonsCount = seasonsCount,
                            selectedSeason = selectedSeason,
                            selectedEpisode = selectedEpisode,
                            onSelectLanguage = onSelectLanguage,
                            onSelectDub = onSelectDub,
                            onSelectSeason = onSelectSeason,
                            onSelectEpisode = { epNum ->
                                onSelectEpisode(epNum)
                                isPlaying = true
                                progressFraction = 0f
                            },
                            onOpenAllEpisodes = { showAllEpisodesSheet = true },
                            onOpenQualitySheet = { showQualitySheet = true },
                            onLoadMoreEpisodes = onLoadMoreEpisodes
                        )
                    }

                    // Cast & Crew Strip (Tapping loads /staff?staffId=...)
                    if (media.cast.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "Cast & Crew",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MuvioTextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(media.cast, key = { idx, p -> "cast_${p.id}_${p.name}_$idx" }) { _, person ->
                                        Surface(
                                            color = MuvioSurfaceCard,
                                            border = BorderStroke(1.dp, MuvioOutline),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onStaffClick(person) }
                                                .testTag("staff_chip_${person.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = MuvioSurfaceElevated,
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    if (!person.avatarUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = person.avatarUrl,
                                                            contentDescription = person.name,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = person.name.firstOrNull()?.uppercase() ?: "C",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MuvioEmerald
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = person.name,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = MuvioTextPrimary,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = person.role,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MuvioTextSecondary,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tabs: For you | Comments(N) • | + Post
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    DetailTabItem(
                                        label = stringResource(R.string.tab_for_you),
                                        selected = selectedBottomTab == 0,
                                        onClick = { selectedBottomTab = 0 },
                                        testTag = "tab_for_you"
                                    )
                                    DetailTabItem(
                                        label = "${stringResource(R.string.tab_comments)} (${comments.size})",
                                        selected = selectedBottomTab == 1,
                                        showDot = true,
                                        onClick = { selectedBottomTab = 1 },
                                        testTag = "tab_comments"
                                    )
                                }

                                if (selectedBottomTab == 1) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { showCommentSheet = true }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                            .testTag("btn_post_comment")
                                    ) {
                                        Surface(
                                            color = MuvioChampagne,
                                            shape = CircleShape,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = null,
                                                tint = Color(0xFF140F05),
                                                modifier = Modifier.padding(2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.action_post),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MuvioChampagne
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MuvioOutline.copy(alpha = 0.6f))
                        }
                    }

                    // Tab Body
                    if (selectedBottomTab == 1) {
                        itemsIndexed(comments, key = { idx, c -> "cmt_${c.id}_$idx" }) { _, comment ->
                            CommentRow(
                                comment = comment,
                                onToggleLike = { onToggleCommentLike(comment.id) },
                                onDownloadClick = { showDownloadSheet = true }
                            )
                            HorizontalDivider(
                                color = MuvioOutline.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    } else {
                        items(recommendations.chunked(3)) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { rec ->
                                    MediaPosterCard(
                                        item = rec,
                                        onClick = { onRecommendationClick(rec) },
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
    }

    // Playback Quality, Audio/Dub, Subtitles & Speed Bottom Sheet
    if (showQualitySheet && !isInPipMode) {
        PlaybackQualityAudioSheet(
            resources = availableResources,
            selectedStreamIndex = selectedStreamIndex,
            dubs = media.dubs,
            selectedLanguage = selectedLanguage,
            subtitles = activeSubtitles,
            selectedSubIndex = selectedSubIndex,
            playbackSpeed = playbackSpeed,
            isLoadingStream = isLoadingStream,
            hasBengaliTrack = hasBengaliTrack,
            isTranslatingBengali = isTranslatingBengali,
            bengaliTranslateProgress = bengaliTranslateProgress,
            onSelectStreamIndex = { idx ->
                onSelectStreamIndex(idx)
                isPlaying = true
                showControls = true
            },
            onSelectDub = { dub ->
                onSelectDub(dub)
                onSelectLanguage(dub.language)
            },
            onSelectSubtitle = { selectedSubIndex = it },
            onRequestBengaliSubtitle = {
                requestBengaliSubtitle()
                showQualitySheet = false
            },
            onSelectSpeed = { playbackSpeed = it },
            onDismiss = { showQualitySheet = false }
        )
    }

    // Download Quality Bottom Sheet
    if (showDownloadSheet) {
        DownloadQualitySheet(
            title = media.title,
            availableQualities = availableResources.map { it.quality }.filter { it.isNotBlank() },
            onSelectQuality = { quality -> onDownloadWithQuality(quality) },
            onDismiss = { showDownloadSheet = false }
        )
    }

    // All Episodes Bottom Sheet (triggered by "All" tile on compact episode strip)
    if (showAllEpisodesSheet && media.episodes.isNotEmpty()) {
        AllEpisodesBottomSheet(
            media = media,
            seasonsCount = seasonsCount,
            selectedSeason = selectedSeason,
            selectedEpisode = selectedEpisode,
            onSelectSeason = onSelectSeason,
            onSelectEpisode = { epNum ->
                onSelectEpisode(epNum)
                isPlaying = true
                progressFraction = 0f
            },
            onDownloadEpisode = {
                showAllEpisodesSheet = false
                showDownloadSheet = true
            },
            onDismiss = { showAllEpisodesSheet = false }
        )
    }

    // Info Bottom Sheet
    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            containerColor = MuvioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = media.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MuvioTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${media.year} • ${media.country} • ${media.genre} • ${media.duration}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuvioEmerald
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = media.synopsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MuvioTextSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Post Comment Bottom Sheet
    if (showCommentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentSheet = false },
            containerColor = MuvioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Add a comment",
                    style = MaterialTheme.typography.titleMedium,
                    color = MuvioTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    placeholder = { Text("Share your review or request...", color = MuvioTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MuvioEmerald,
                        unfocusedBorderColor = MuvioOutline,
                        focusedTextColor = MuvioTextPrimary,
                        unfocusedTextColor = MuvioTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        color = MuvioEmerald,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable {
                            if (commentDraft.isNotBlank()) {
                                onPostComment(commentDraft)
                                commentDraft = ""
                                showCommentSheet = false
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Post",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Post Comment",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = MuvioSurfaceElevated,
            title = {
                Text("Muvio Stream Ready", color = MuvioTextPrimary)
            },
            text = {
                Text(
                    "Player UI & Jetpack Media3 ExoPlayer are wired and ready. Connect your API stream URLs next to stream live HLS/MP4 videos and cast to TV.",
                    color = MuvioTextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it", color = MuvioEmerald)
                }
            }
        )
    }
}

private val PlayerControlsScrimBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Black.copy(alpha = 0.65f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.78f)
    )
)

@OptIn(UnstableApi::class)
@Composable
private fun PlayerStage(
    media: MediaItem,
    episodeKey: String,
    streamUrl: String,
    streamHeaders: Map<String, String>,
    isLoadingStream: Boolean,
    streamError: String?,
    subtitles: List<SubtitleTrack>,
    hasBengaliTrack: Boolean,
    isTranslatingBengali: Boolean,
    bengaliTranslateProgress: Float,
    subtitleStatusBanner: String?,
    activeQualityLabel: String,
    playbackSpeed: Float,
    selectedSubIndex: Int,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    isInPipMode: Boolean,
    showControls: Boolean,
    simulatedProgressFraction: Float,
    onRetryStream: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectSubtitle: (Int) -> Unit,
    onRequestBengaliSubtitle: () -> Unit,
    onOpenQualitySheet: () -> Unit,
    onToggleControls: () -> Unit,
    onAutoHideControls: () -> Unit,
    onTogglePlay: () -> Unit,
    onEnterPip: () -> Unit,
    onProgressReport: (Float) -> Unit,
    onSimulatedSeekFraction: (Float) -> Unit,
    onSimulatedRewind10: () -> Unit,
    onSimulatedForward10: () -> Unit,
    onBackClick: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onHelpClick: () -> Unit,
    onCastClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val pipController = LocalMuvioPipController.current
    var showSubMenu by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryCounter by remember { mutableIntStateOf(0) }
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }
    var lastLoadedEpisodeKey by remember { mutableStateOf(episodeKey) }
    var playerBoundsRect by remember { mutableStateOf<Rect?>(null) }

    // Smooth vertical swipe gesture state for left=Brightness, right=Volume
    var gestureHudState by remember { mutableStateOf<PlayerGestureHudState?>(null) }
    var isVerticalDragging by remember { mutableStateOf(false) }

    val activeSub = subtitles.getOrNull(selectedSubIndex)
    val hasStream = streamUrl.isNotBlank()

    // Reuse one hardware-accelerated ExoPlayer across quality/subtitle/episode switches;
    // recreate only when HTTP header map changes or stream toggles from empty to non-empty.
    val exoPlayer = remember(hasStream, streamHeaders) {
        if (hasStream) {
            MuvioExoPlayerFactory.createVodPlayer(
                context = context,
                headers = streamHeaders,
                subtitleParserFactory = AutoDetectSubtitleParserFactory()
            )
        } else {
            null
        }
    }

    // Bind active player & playback controls to Smart PiP Controller (auto-enter + remote buttons)
    LaunchedEffect(pipController, exoPlayer, isPlaying, playerBoundsRect) {
        pipController?.bindPlayer(
            isPlaying = isPlaying,
            sourceRect = playerBoundsRect,
            onTogglePlayPause = onTogglePlay,
            onRewind10 = {
                if (exoPlayer != null) exoPlayer.seekBack() else onSimulatedRewind10()
            },
            onForward10 = {
                if (exoPlayer != null) exoPlayer.seekForward() else onSimulatedForward10()
            }
        )
    }

    // Load or hot-swap media item cleanly while preserving playback position on quality/subtitle switches
    LaunchedEffect(exoPlayer, streamUrl, activeSub?.url, retryCounter) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (streamUrl.isBlank()) return@LaunchedEffect

        playbackError = null
        isBuffering = true
        val sameEpisode = lastLoadedEpisodeKey == episodeKey
        val resumeMs = if (sameEpisode && player.currentPosition > 2_000L) player.currentPosition else 0L
        lastLoadedEpisodeKey = episodeKey

        val builder = ExoMediaItem.Builder().setUri(streamUrl)
        if (streamUrl.contains(".m3u8", ignoreCase = true) || streamUrl.contains("m3u8", ignoreCase = true)) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }
        if (activeSub != null) {
            val mime = inferSubtitleMimeType(activeSub.url)
            val subConfig = ExoMediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(activeSub.url))
                .setMimeType(mime)
                .setLanguage(activeSub.language)
                .setLabel(activeSub.label)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED)
                .build()
            builder.setSubtitleConfigurations(listOf(subConfig))
        }
        player.setMediaItem(builder.build(), resumeMs)
        player.prepare()
        player.playWhenReady = isPlaying
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                } else if (playbackState == Player.STATE_ENDED &&
                    (exoPlayer?.duration ?: C.TIME_UNSET) <= 0L &&
                    playbackError == null
                ) {
                    playbackError = "ভিডিও স্ট্রীম চালু হয়নি, আবার চেষ্টা করুন"
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = error.localizedMessage ?: "Stream playback error (${error.errorCodeName})"
            }
        }
        exoPlayer?.addListener(listener)
        onDispose {
            exoPlayer?.removeListener(listener)
            exoPlayer?.release()
        }
    }

    // PiP-aware lifecycle handling: keep playing smoothly during Picture-in-Picture (ON_PAUSE),
    // and pause cleanly when Activity truly stops (PiP dismissed or backgrounded without PiP).
    DisposableEffect(lifecycleOwner, exoPlayer, activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    val inPip = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N &&
                        (activity?.isInPictureInPictureMode == true)
                    if (!inPip) {
                        exoPlayer?.pause()
                    }
                }
                Lifecycle.Event.ON_STOP -> exoPlayer?.pause()
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) exoPlayer?.play()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isPlaying, playbackSpeed, exoPlayer) {
        exoPlayer?.let { player ->
            player.playWhenReady = isPlaying
            player.setPlaybackSpeed(playbackSpeed)
        }
    }

    // Auto-hide controls after 4 seconds of active playback so zero UI work runs during viewing
    LaunchedEffect(showControls, isPlaying, showSubMenu) {
        if (showControls && isPlaying && !showSubMenu && exoPlayer != null) {
            delay(4_000L)
            onAutoHideControls()
        }
    }

    // Clear double-tap seek pill quickly
    LaunchedEffect(doubleTapFeedback) {
        if (doubleTapFeedback != null) {
            delay(650L)
            doubleTapFeedback = null
        }
    }

    // Fade out brightness/volume pill HUD smoothly shortly after finger lifts
    LaunchedEffect(gestureHudState, isVerticalDragging) {
        if (gestureHudState != null && !isVerticalDragging) {
            delay(750L)
            gestureHudState = null
        }
    }

    // Gentle low-overhead watch progress sync every 5 seconds without triggering UI recomposition
    LaunchedEffect(exoPlayer, isPlaying) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (isPlaying) {
            delay(5_000L)
            val dur = player.duration
            if (dur > 0L) {
                val frac = (player.currentPosition.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                onProgressReport(frac)
            }
        }
    }

    val stageModifier = if (isFullscreen || isInPipMode) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .aspectRatio(16f / 9f)
    }

    Box(
        modifier = stageModifier
            .background(Color.Black)
            .onGloballyPositioned { coords ->
                runCatching {
                    val b = coords.boundsInWindow()
                    if (b.left.isFinite() && b.top.isFinite() && b.right.isFinite() && b.bottom.isFinite()) {
                        val rect = Rect(
                            b.left.roundToInt(),
                            b.top.roundToInt(),
                            b.right.roundToInt(),
                            b.bottom.roundToInt()
                        )
                        if (rect.width() > 0 && rect.height() > 0) {
                            playerBoundsRect = rect
                            pipController?.updateSourceRect(rect)
                        }
                    }
                }
            }
            .pointerInput(exoPlayer, isInPipMode) {
                if (isInPipMode) return@pointerInput
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        val isLeft = offset.x < size.width / 2f
                        if (exoPlayer != null) {
                            if (isLeft) {
                                exoPlayer.seekBack()
                                doubleTapFeedback = "-10s"
                            } else {
                                exoPlayer.seekForward()
                                doubleTapFeedback = "+10s"
                            }
                        } else {
                            if (isLeft) {
                                onSimulatedRewind10()
                                doubleTapFeedback = "-10s"
                            } else {
                                onSimulatedForward10()
                                doubleTapFeedback = "+10s"
                            }
                        }
                    }
                )
            }
            .pointerInput(activity, isInPipMode) {
                if (isInPipMode) return@pointerInput
                var activeType = PlayerGestureType.VOLUME
                var activeLevel = 0.5f
                detectVerticalDragGestures(
                    onDragStart = { startOffset ->
                        isVerticalDragging = true
                        activeType = if (startOffset.x < size.width / 2f) {
                            PlayerGestureType.BRIGHTNESS
                        } else {
                            PlayerGestureType.VOLUME
                        }
                        activeLevel = when (activeType) {
                            PlayerGestureType.BRIGHTNESS -> readCurrentBrightness(activity)
                            PlayerGestureType.VOLUME -> readMediaVolumeFraction(context)
                        }
                        gestureHudState = PlayerGestureHudState(activeType, activeLevel)
                    },
                    onDragEnd = { isVerticalDragging = false },
                    onDragCancel = { isVerticalDragging = false },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val fullTravelPx = (size.height * 0.65f).coerceAtLeast(180f)
                        val delta = -dragAmount / fullTravelPx
                        activeLevel = (activeLevel + delta).coerceIn(0f, 1f)
                        when (activeType) {
                            PlayerGestureType.BRIGHTNESS -> applyWindowBrightness(activity, activeLevel)
                            PlayerGestureType.VOLUME -> applyMediaVolumeFraction(context, activeLevel)
                        }
                        gestureHudState = PlayerGestureHudState(activeType, activeLevel)
                    }
                )
            }
            .testTag("video_player_stage")
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = isPlaying
                        subtitleView?.apply {
                            setApplyEmbeddedStyles(false)
                            setApplyEmbeddedFontSizes(false)
                            setFractionalTextSize(0.056f)
                            setBottomPaddingFraction(0.06f)
                            setStyle(
                                CaptionStyleCompat(
                                    android.graphics.Color.WHITE,
                                    android.graphics.Color.TRANSPARENT,
                                    android.graphics.Color.TRANSPARENT,
                                    CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                    android.graphics.Color.BLACK,
                                    Typeface.SANS_SERIF
                                )
                            )
                        }
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                    view.keepScreenOn = isPlaying
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (!media.backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = media.backdropUrl,
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        } else {
            Image(
                painter = painterResource(id = media.backdropRes),
                contentDescription = media.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
            )
        }

        if (isBuffering || (isLoadingStream && exoPlayer == null)) {
            CircularProgressIndicator(
                color = MuvioEmerald,
                strokeWidth = 2.8.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(38.dp)
            )
        }

        // Sleek floating Brightness / Volume Swipe HUD (top-center, matching modern cinema players)
        if (!isInPipMode) {
            PlayerGesturePillHud(
                hudState = gestureHudState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
            )
        }

        // Non-blocking Bengali Subtitle progress / status pill
        AnimatedVisibility(
            visible = !isInPipMode && subtitleStatusBanner != null && gestureHudState == null,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(0.8.dp, MuvioChampagne.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isTranslatingBengali) {
                        CircularProgressIndicator(
                            color = MuvioChampagne,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = null,
                            tint = MuvioChampagne,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (isTranslatingBengali) {
                            "বাংলা সাবটাইটেল তৈরি হচ্ছে... ${(bengaliTranslateProgress * 100).toInt()}%"
                        } else {
                            subtitleStatusBanner.orEmpty()
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MuvioChampagneLight
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isInPipMode && doubleTapFeedback != null,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(0.8.dp, MuvioGlassBorder)
            ) {
                Text(
                    text = doubleTapFeedback.orEmpty(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MuvioChampagneLight,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (!isInPipMode) {
            val activeError = playbackError ?: if (!isLoadingStream && exoPlayer == null) streamError else null
            activeError?.let { errMsg ->
                Surface(
                    color = Color.Black.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MuvioOutline),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MuvioTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MuvioEmerald,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable {
                                if (exoPlayer != null) {
                                    playbackError = null
                                    retryCounter++
                                } else {
                                    onRetryStream()
                                }
                            }
                        ) {
                            Text(
                                text = "Retry Stream",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showControls && !isInPipMode,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(240)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PlayerControlsScrimBrush)
            ) {
                // Top Overlay Bar (< Back, Quality/Speed Pill, CC Bengali/Subtitles, TV Cast, Help)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("player_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = MuvioGlass,
                            border = BorderStroke(0.9.dp, MuvioGlassBorder),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenQualitySheet)
                                .testTag("player_quality_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "Quality, Audio & Speed settings",
                                    tint = MuvioChampagne,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (playbackSpeed != 1.0f) "$activeQualityLabel • ${playbackSpeed}x" else activeQualityLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MuvioChampagneLight
                                )
                            }
                        }

                        // Always-visible CC Subtitles button with 1-tap Bengali (Auto) support
                        Box {
                            IconButton(
                                onClick = { showSubMenu = true },
                                modifier = Modifier.testTag("player_cc_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ClosedCaption,
                                    contentDescription = "Subtitles",
                                    tint = if (selectedSubIndex >= 0 || isTranslatingBengali) MuvioEmerald else Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showSubMenu,
                                onDismissRequest = { showSubMenu = false },
                                containerColor = MuvioSurfaceElevated
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Off",
                                            color = if (selectedSubIndex < 0) MuvioEmerald else MuvioTextPrimary
                                        )
                                    },
                                    onClick = {
                                        onSelectSubtitle(-1)
                                        showSubMenu = false
                                    }
                                )
                                if (!hasBengaliTrack) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Translate,
                                                contentDescription = null,
                                                tint = MuvioChampagne,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = if (isTranslatingBengali) {
                                                    "বাংলা তৈরি হচ্ছে ${(bengaliTranslateProgress * 100).toInt()}%"
                                                } else {
                                                    "বাংলা (Auto Translate)"
                                                },
                                                fontWeight = FontWeight.SemiBold,
                                                color = MuvioChampagneLight
                                            )
                                        },
                                        onClick = {
                                            onRequestBengaliSubtitle()
                                            showSubMenu = false
                                        }
                                    )
                                }
                                subtitles.forEachIndexed { idx, sub ->
                                    val isBn = BengaliSubtitleService.isBengaliTrack(sub)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isBn && !sub.label.contains("বাংলা")) "বাংলা (${sub.label})" else sub.label,
                                                fontWeight = if (isBn || idx == selectedSubIndex) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (idx == selectedSubIndex) MuvioEmerald else MuvioTextPrimary
                                            )
                                        },
                                        onClick = {
                                            onSelectSubtitle(idx)
                                            showSubMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = onCastClick) {
                            Icon(
                                imageVector = Icons.Rounded.Cast,
                                contentDescription = "TV Cast",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onHelpClick) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = "Help",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Playback Controls (Rewind 10, Play/Pause, Forward 10)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (exoPlayer != null) exoPlayer.seekBack() else onSimulatedRewind10()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, MuvioEmerald.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .size(52.dp)
                            .clickable(onClick = onTogglePlay)
                            .testTag("player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer != null) exoPlayer.seekForward() else onSimulatedForward10()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Isolated Bottom Scrub Bar — only this small composable recomposes while controls are visible
                PlayerBottomScrubBar(
                    exoPlayer = exoPlayer,
                    isPlaying = isPlaying,
                    isFullscreen = isFullscreen,
                    simulatedProgressFraction = simulatedProgressFraction,
                    onTogglePlay = onTogglePlay,
                    onSimulatedSeekFraction = onSimulatedSeekFraction,
                    onEnterPip = onEnterPip,
                    onToggleFullscreen = onToggleFullscreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PlayerBottomScrubBar(
    exoPlayer: ExoPlayer?,
    isPlaying: Boolean,
    isFullscreen: Boolean,
    simulatedProgressFraction: Float,
    onTogglePlay: () -> Unit,
    onSimulatedSeekFraction: (Float) -> Unit,
    onEnterPip: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var positionMs by remember(exoPlayer) { mutableStateOf(exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L) }
    var durationMs by remember(exoPlayer) { mutableStateOf(exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    // Polls ONLY while this scrub bar is actually mounted (i.e., controls are visible)
    LaunchedEffect(exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            if (!isScrubbing) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                val dur = player.duration
                if (dur > 0L) durationMs = dur
            }
            delay(500L)
        }
    }

    val sliderValue = when {
        isScrubbing -> scrubFraction
        exoPlayer != null && durationMs > 0L -> (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        else -> simulatedProgressFraction
    }

    val displayPosMs = if (isScrubbing && durationMs > 0L) {
        (scrubFraction * durationMs).toLong()
    } else {
        positionMs
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Slider(
            value = sliderValue,
            onValueChange = { fraction ->
                if (exoPlayer != null && durationMs > 0L) {
                    isScrubbing = true
                    scrubFraction = fraction
                } else {
                    onSimulatedSeekFraction(fraction)
                }
            },
            onValueChangeFinished = {
                if (exoPlayer != null && durationMs > 0L && isScrubbing) {
                    val seekMs = (scrubFraction * durationMs).toLong()
                    positionMs = seekMs
                    exoPlayer.seekTo(seekMs)
                }
                isScrubbing = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MuvioEmerald,
                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
        )

        val timeText = if (exoPlayer != null && durationMs > 0L) {
            "${formatMillis(displayPosMs)}/${formatMillis(durationMs)}"
        } else {
            "00:00/--:--"
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        IconButton(
            onClick = onEnterPip,
            modifier = Modifier
                .size(32.dp)
                .testTag("player_pip_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.PictureInPictureAlt,
                contentDescription = "Picture in Picture",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onToggleFullscreen,
            modifier = Modifier
                .size(32.dp)
                .testTag("player_fullscreen")
        ) {
            Icon(
                imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                contentDescription = "Fullscreen",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun inferSubtitleMimeType(url: String): String {
    val cleanPath = url.substringBefore('?').lowercase()
    return when {
        cleanPath.endsWith(".vtt") || url.contains("webvtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
        cleanPath.endsWith(".ass") || cleanPath.endsWith(".ssa") -> MimeTypes.TEXT_SSA
        cleanPath.endsWith(".ttml") || cleanPath.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

@OptIn(UnstableApi::class)
private class AutoDetectSubtitleParserFactory : SubtitleParser.Factory {
    private val delegate = DefaultSubtitleParserFactory()

    override fun supportsFormat(format: Format): Boolean = delegate.supportsFormat(format)

    override fun getCueReplacementBehavior(format: Format): Int = delegate.getCueReplacementBehavior(format)

    override fun create(format: Format): SubtitleParser {
        val primary = delegate.create(format)
        return object : SubtitleParser {
            override fun parse(
                data: ByteArray,
                offset: Int,
                length: Int,
                outputOptions: SubtitleParser.OutputOptions,
                output: Consumer<CuesWithTiming>
            ) {
                val head = String(data, offset, minOf(length, 96), Charsets.UTF_8)
                    .trimStart('\uFEFF', ' ', '\t', '\r', '\n')
                val parserToUse = when {
                    head.startsWith("WEBVTT", ignoreCase = true) -> WebvttParser()
                    head.startsWith("[Script Info]", ignoreCase = true) ->
                        delegate.create(format.buildUpon().setSampleMimeType(MimeTypes.TEXT_SSA).build())
                    head.startsWith("<?xml", ignoreCase = true) || head.startsWith("<tt", ignoreCase = true) ->
                        delegate.create(format.buildUpon().setSampleMimeType(MimeTypes.APPLICATION_TTML).build())
                    head.firstOrNull()?.isDigit() == true -> SubripParser()
                    else -> primary
                }
                try {
                    parserToUse.parse(data, offset, length, outputOptions, output)
                } catch (_: Exception) {
                    val fallback = if (parserToUse is WebvttParser) SubripParser() else WebvttParser()
                    try {
                        fallback.parse(data, offset, length, outputOptions, output)
                    } catch (_: Exception) {
                        // Ignore malformed caption payload cleanly without breaking playback
                    }
                }
            }

            override fun getCueReplacementBehavior(): Int = primary.cueReplacementBehavior
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0L) {
        "%d:%02d:%02d".format(hrs, mins, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}

@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    highlighted: Boolean = false
) {
    Surface(
        color = if (highlighted) MuvioChampagne.copy(alpha = 0.16f) else MuvioSurfaceElevated,
        border = BorderStroke(
            width = 0.9.dp,
            color = if (highlighted) MuvioChampagne.copy(alpha = 0.7f) else MuvioOutline.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) MuvioChampagneLight else MuvioTextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (highlighted) MuvioChampagneLight else MuvioTextPrimary
            )
        }
    }
}

@Composable
private fun DetailTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    showDot: Boolean = false
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) MuvioTextPrimary else MuvioTextSecondary
            )
            if (showDot) {
                Box(
                    modifier = Modifier
                        .padding(start = 3.dp, top = 2.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MuvioHotRed)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 56.dp else 0.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) MuvioChampagne else Color.Transparent)
        )
    }
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    onToggleLike: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Surface(
            color = Color(comment.avatarColor),
            shape = CircleShape,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = comment.avatarInitials,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.author,
                    style = MaterialTheme.typography.titleSmall,
                    color = MuvioTextSecondary
                )
                if (comment.flagEmoji.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = comment.flagEmoji, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MuvioTextMuted
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                color = MuvioTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleLike)
                        .padding(end = 12.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like comment",
                        tint = if (comment.isLiked) MuvioHotRed else MuvioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = comment.likes.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MuvioTextSecondary
                    )
                }

                if (comment.hasDownloadPromo) {
                    Surface(
                        color = MuvioEmerald,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable(onClick = onDownloadClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "HD Download",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Replies",
                            tint = MuvioTextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = comment.repliesCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MuvioTextSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share comment",
                    tint = MuvioTextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
