package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import com.example.ui.theme.Download
import com.example.ui.theme.GridView
import com.example.ui.theme.HighQuality
import com.example.ui.theme.Translate
import com.example.ui.theme.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DubVersion
import com.example.model.EpisodeItem
import com.example.model.MediaItem
import com.example.model.StreamResource
import com.example.model.SubtitleTrack
import com.example.ui.theme.MuvioChampagne
import com.example.ui.theme.MuvioChampagneLight
import com.example.ui.theme.MuvioEmeraldContainer
import com.example.ui.theme.MuvioGlass
import com.example.ui.theme.MuvioGlassBorder
import com.example.ui.theme.MuvioOutline
import com.example.ui.theme.MuvioSurface
import com.example.ui.theme.MuvioSurfaceCard
import com.example.ui.theme.MuvioSurfaceElevated
import com.example.ui.theme.MuvioTextMuted
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTextSecondary
import com.example.ui.theme.PlayfairFontFamily

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackQualityAudioSheet(
    resources: List<StreamResource>,
    selectedStreamIndex: Int,
    dubs: List<DubVersion>,
    selectedLanguage: String,
    subtitles: List<SubtitleTrack>,
    selectedSubIndex: Int,
    subtitleSize: String = "Medium",
    playbackSpeed: Float,
    isLoadingStream: Boolean,
    hasBengaliTrack: Boolean = false,
    isTranslatingBengali: Boolean = false,
    bengaliTranslateProgress: Float = 0f,
    onSelectStreamIndex: (Int) -> Unit,
    onSelectDub: (DubVersion) -> Unit,
    onSelectSubtitle: (Int) -> Unit,
    onSelectSubtitleSize: (String) -> Unit = {},
    onRequestBengaliSubtitle: () -> Unit = {},
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MuvioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Playback Settings",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlayfairFontFamily),
                color = MuvioTextPrimary
            )
            Text(
                text = "Video quality, audio tracks, subtitles & speed",
                style = MaterialTheme.typography.bodySmall,
                color = MuvioTextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Video Quality Section
            Text(
                text = "VIDEO QUALITY",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                color = MuvioChampagne
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingStream) {
                Text(
                    text = "Fetching available HD streams...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuvioTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    resources.forEachIndexed { idx, res ->
                        val isActive = idx == selectedStreamIndex || (selectedStreamIndex >= resources.size && idx == 0)
                        val subtitleDesc = when (res.quality.lowercase()) {
                            "4k", "2160p" -> "Ultra HD • Cinema Reference"
                            "1080p" -> "Full HD • Crisp Studio Clarity"
                            "720p", "hd" -> "HD • Balanced Stream"
                            "480p" -> "Standard • Faster Loading"
                            "360p" -> "Data Saver • Low Bandwidth"
                            else -> "Direct Stream • ${res.language}"
                        }
                        Surface(
                            color = if (isActive) MuvioEmeraldContainer else MuvioSurfaceCard,
                            border = BorderStroke(
                                1.dp,
                                if (isActive) MuvioChampagne else MuvioOutline.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectStreamIndex(idx)
                                    onDismiss()
                                }
                                .testTag("quality_option_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = res.quality,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isActive) MuvioChampagneLight else MuvioTextPrimary
                                    )
                                    Text(
                                        text = subtitleDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MuvioTextSecondary
                                    )
                                }
                                if (isActive) {
                                    Surface(
                                        color = MuvioChampagne,
                                        shape = CircleShape,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = Color(0xFF140F05),
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Audio / Dub Tracks (only when real dubs exist)
            if (dubs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "AUDIO & DUB TRACKS",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = MuvioChampagne
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(dubs, key = { idx, dub -> "dub_${dub.language}_${dub.title}_$idx" }) { _, dub ->
                        val active = dub.language.equals(selectedLanguage, ignoreCase = true)
                        Surface(
                            color = if (active) MuvioChampagne else MuvioSurfaceElevated,
                            border = BorderStroke(0.9.dp, if (active) MuvioChampagne else MuvioOutline),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    onSelectDub(dub)
                                    onDismiss()
                                }
                                .testTag("dub_chip_${dub.language}")
                        ) {
                            Text(
                                text = dub.language,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (active) Color(0xFF140F05) else MuvioTextPrimary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 3. Subtitles (always visible so user can enable Bengali subtitles on any movie)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "SUBTITLES (CC)",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                color = MuvioChampagne
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val isOff = selectedSubIndex < 0 && !isTranslatingBengali
                    Surface(
                        color = if (isOff) MuvioChampagne else MuvioSurfaceElevated,
                        border = BorderStroke(0.9.dp, if (isOff) MuvioChampagne else MuvioOutline),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectSubtitle(-1) }
                    ) {
                        Text(
                            text = "Off",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isOff) Color(0xFF140F05) else MuvioTextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
                if (!hasBengaliTrack) {
                    item {
                        Surface(
                            color = if (isTranslatingBengali) MuvioEmeraldContainer else MuvioSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isTranslatingBengali) MuvioChampagne else MuvioChampagne.copy(alpha = 0.65f)
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(enabled = !isTranslatingBengali) {
                                    onRequestBengaliSubtitle()
                                }
                                .testTag("subtitle_bengali_auto_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Translate,
                                    contentDescription = "বাংলা সাবটাইটেল",
                                    tint = MuvioChampagne,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isTranslatingBengali) {
                                        "বাংলা তৈরি হচ্ছে ${(bengaliTranslateProgress * 100).toInt()}%"
                                    } else {
                                        "বাংলা (Auto)"
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MuvioChampagneLight
                                )
                            }
                        }
                    }
                }
                items(subtitles.size) { idx ->
                    val sub = subtitles[idx]
                    val active = idx == selectedSubIndex
                    Surface(
                        color = if (active) MuvioChampagne else MuvioSurfaceElevated,
                        border = BorderStroke(0.9.dp, if (active) MuvioChampagne else MuvioOutline),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectSubtitle(idx) }
                    ) {
                        Text(
                            text = sub.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) Color(0xFF140F05) else MuvioTextPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // 3b. Subtitle Font Size
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "SUBTITLE SIZE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                color = MuvioChampagne
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Small", "Medium", "Large").forEach { sizeOption ->
                    val active = sizeOption.equals(subtitleSize, ignoreCase = true)
                    Surface(
                        color = if (active) MuvioChampagne else MuvioSurfaceElevated,
                        border = BorderStroke(0.8.dp, if (active) MuvioChampagne else MuvioOutline),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectSubtitleSize(sizeOption) }
                            .testTag("subtitle_size_${sizeOption.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = sizeOption,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (active) Color(0xFF140F05) else MuvioTextPrimary
                            )
                        }
                    }
                }
            }

            // 4. Playback Speed
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "PLAYBACK SPEED",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                color = MuvioChampagne
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                    val active = spd == playbackSpeed
                    Surface(
                        color = if (active) MuvioChampagne else MuvioSurfaceElevated,
                        border = BorderStroke(0.8.dp, if (active) MuvioChampagne else MuvioOutline),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectSpeed(spd) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        ) {
                            Text(
                                text = "${spd}x",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (active) Color(0xFF140F05) else MuvioTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQualitySheet(
    title: String,
    availableQualities: List<String>,
    onSelectQuality: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = remember(availableQualities) {
        val base = availableQualities.distinct().ifEmpty { listOf("1080p", "720p", "480p") }
        base.map { q ->
            val sizeHint = when (q.lowercase()) {
                "4k", "2160p" -> "~2.8 GB • Ultra HD"
                "1080p" -> "~1.2 GB • Full HD"
                "720p", "hd" -> "~680 MB • Recommended"
                "480p" -> "~340 MB • Fast Download"
                "360p" -> "~210 MB • Data Saver"
                else -> "Offline Ready"
            }
            q to sizeHint
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MuvioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Download Quality",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlayfairFontFamily),
                color = MuvioTextPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MuvioChampagne,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { (quality, hint) ->
                    Surface(
                        color = MuvioSurfaceCard,
                        border = BorderStroke(0.9.dp, MuvioGlassBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectQuality(quality)
                                onDismiss()
                            }
                            .testTag("download_quality_$quality")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.HighQuality,
                                    contentDescription = null,
                                    tint = MuvioChampagne,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = quality,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MuvioTextPrimary
                                    )
                                    Text(
                                        text = hint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MuvioTextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Download $quality",
                                tint = MuvioChampagne,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
fun NoirEpisodesSection(
    media: MediaItem,
    seasonsCount: Int,
    selectedSeason: Int,
    selectedEpisode: Int,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (Int) -> Unit,
    onDownloadEpisode: (EpisodeItem) -> Unit,
    onLoadMoreEpisodes: () -> Unit = {}
) {
    val episodes = media.episodes
    if (episodes.isEmpty()) return

    var showSeasonMenu by remember { mutableStateOf(false) }
    var isGridMode by remember { mutableStateOf(episodes.size > 24) }
    var selectedRangeIndex by remember { mutableIntStateOf(0) }
    val pageSize = 20
    val ranges = remember(episodes.size) {
        episodes.chunked(pageSize)
    }
    val visibleEpisodes = ranges.getOrElse(selectedRangeIndex) { episodes }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Header Row: Season Selector Pill + Episode Count + List/Grid View Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        color = MuvioSurfaceElevated,
                        border = BorderStroke(0.9.dp, MuvioGlassBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { if (seasonsCount > 1) showSeasonMenu = true }
                            .testTag("season_dropdown")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Season ${selectedSeason.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MuvioChampagneLight
                            )
                            if (seasonsCount > 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Select Season",
                                    tint = MuvioChampagne,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = showSeasonMenu,
                        onDismissRequest = { showSeasonMenu = false },
                        containerColor = MuvioSurfaceElevated
                    ) {
                        (1..seasonsCount.coerceAtLeast(1)).forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Season ${s.toString().padStart(2, '0')}",
                                        color = if (s == selectedSeason) MuvioChampagne else MuvioTextPrimary
                                    )
                                },
                                onClick = {
                                    onSelectSeason(s)
                                    selectedRangeIndex = 0
                                    showSeasonMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${episodes.size} Episodes",
                    style = MaterialTheme.typography.labelLarge,
                    color = MuvioTextSecondary
                )
            }

            // Compact Grid vs Rich Cinema List Toggle
            Surface(
                color = MuvioSurfaceCard,
                border = BorderStroke(0.8.dp, MuvioOutline),
                shape = RoundedCornerShape(12.dp)
            ) {
                IconButton(
                    onClick = { isGridMode = !isGridMode },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_episode_layout")
                ) {
                    Icon(
                        imageVector = if (isGridMode) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                        contentDescription = if (isGridMode) "Switch to Episode List" else "Switch to Compact Grid",
                        tint = MuvioChampagne,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Range Tabs (1-20, 21-40...) when > 20 episodes
        if (ranges.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ranges.size) { rIdx ->
                    val startNum = ranges[rIdx].first().number
                    val endNum = ranges[rIdx].last().number
                    val active = rIdx == selectedRangeIndex
                    Surface(
                        color = if (active) MuvioChampagne else MuvioSurfaceCard,
                        border = BorderStroke(0.8.dp, if (active) MuvioChampagne else MuvioOutline),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedRangeIndex = rIdx }
                    ) {
                        Text(
                            text = "$startNum–$endNum",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (active) Color(0xFF140F05) else MuvioTextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isGridMode) {
            // Compact 5-column Grid for fast Anime / Long Series navigation
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleEpisodes.chunked(5).forEach { rowEps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowEps.forEach { ep ->
                            val isCurrent = ep.number == selectedEpisode
                            Surface(
                                color = if (isCurrent) MuvioEmeraldContainer else MuvioSurfaceCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) MuvioChampagne else MuvioOutline.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectEpisode(ep.number) }
                                    .testTag("episode_chip_${ep.number}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = ep.number.toString().padStart(2, '0'),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrent) MuvioChampagneLight else MuvioTextPrimary
                                    )
                                }
                            }
                        }
                        repeat(5 - rowEps.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // Premium Noir Cinema Episode Cards with 16:9 Mini Preview Thumbnail + Quick Download
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                visibleEpisodes.forEach { ep ->
                    val isCurrent = ep.number == selectedEpisode
                    Surface(
                        color = if (isCurrent) MuvioEmeraldContainer.copy(alpha = 0.85f) else MuvioSurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) MuvioChampagne else MuvioOutline.copy(alpha = 0.65f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectEpisode(ep.number) }
                            .testTag("episode_chip_${ep.number}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 16:9 Mini Preview Thumbnail
                            Box(
                                modifier = Modifier
                                    .width(106.dp)
                                    .height(62.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MuvioSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                val thumbUrl = media.backdropUrl ?: media.poster.remoteUrl
                                if (!thumbUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = thumbUrl,
                                        contentDescription = ep.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = media.backdropRes),
                                        contentDescription = ep.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.6f))
                                            )
                                        )
                                )
                                Surface(
                                    color = if (isCurrent) MuvioChampagne else MuvioGlass,
                                    shape = CircleShape,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isCurrent) Color(0xFF140F05) else Color.White,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "EP ${ep.number.toString().padStart(2, '0')}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp
                                        ),
                                        color = MuvioChampagne
                                    )
                                    if (isCurrent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = MuvioChampagne.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "NOW PLAYING",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                                color = MuvioChampagneLight,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = ep.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MuvioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = ep.duration,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MuvioTextMuted
                                )
                            }

                            IconButton(
                                onClick = { onDownloadEpisode(ep) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "Download Episode ${ep.number}",
                                    tint = MuvioTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (episodes.size < 500) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MuvioSurfaceElevated,
                    border = BorderStroke(0.9.dp, MuvioChampagne.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onLoadMoreEpisodes)
                        .testTag("load_more_episodes_sheet_btn")
                ) {
                    Text(
                        text = "+ Load More Episodes",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MuvioChampagneLight,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NoirCompactAudioEpisodesSection(
    media: MediaItem,
    availableResources: List<StreamResource>,
    activeQualityLabel: String,
    selectedLanguage: String,
    seasonsCount: Int,
    selectedSeason: Int,
    selectedEpisode: Int,
    onSelectLanguage: (String) -> Unit,
    onSelectDub: (DubVersion) -> Unit,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (Int) -> Unit,
    onOpenAllEpisodes: () -> Unit,
    onOpenQualitySheet: () -> Unit,
    onLoadMoreEpisodes: () -> Unit = {}
) {
    val episodes = media.episodes
    val hasEpisodes = episodes.isNotEmpty()
    val distinctLanguages = remember(availableResources, media.dubs) {
        val fromResources = availableResources.mapNotNull { it.language.takeIf { lang -> lang.isNotBlank() } }
        val fromDubs = media.dubs.map { it.language }
        (fromDubs + fromResources).distinctBy { it.lowercase() }
    }
    val hasAudioOptions = media.dubs.isNotEmpty() || distinctLanguages.size > 1
    val activeAudioLabel = remember(selectedLanguage, media.dubs, distinctLanguages) {
        val raw = selectedLanguage.takeIf { it.isNotBlank() }
            ?: media.dubs.firstOrNull()?.language
            ?: distinctLanguages.firstOrNull()
            ?: "Original"
        raw.lowercase().replaceFirstChar { it.uppercase() }
    }

    var showAudioMenu by remember { mutableStateOf(false) }
    var showSeasonMenu by remember { mutableStateOf(false) }
    val sourceLabel = availableResources.firstOrNull()?.uploader?.takeIf { it.isNotBlank() } ?: "Muvio Direct"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 10.dp)
    ) {
        // 1. Clean Single-Line Section Header: Resources + subtle uploader/source
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Resources",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MuvioTextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Uploaded by $sourceLabel",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MuvioTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Uniform Selector Pills Row: [ Audio/Dub ▾ ] [ Season 01 ▾ ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio / Dub Dropdown Pill
            Box {
                Surface(
                    color = MuvioSurfaceCard,
                    border = BorderStroke(1.dp, MuvioOutline.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (hasAudioOptions) {
                                showAudioMenu = true
                            } else {
                                onOpenQualitySheet()
                            }
                        }
                        .testTag("compact_audio_dropdown")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeAudioLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MuvioTextPrimary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Select Audio Track",
                            tint = MuvioChampagne,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showAudioMenu,
                    onDismissRequest = { showAudioMenu = false },
                    containerColor = MuvioSurfaceElevated
                ) {
                    if (media.dubs.isNotEmpty()) {
                        media.dubs.forEach { dub ->
                            val isSelected = dub.language.equals(activeAudioLabel, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = dub.language,
                                        color = if (isSelected) MuvioChampagne else MuvioTextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectDub(dub)
                                    onSelectLanguage(dub.language)
                                    showAudioMenu = false
                                }
                            )
                        }
                    } else {
                        distinctLanguages.forEach { lang ->
                            val isSelected = lang.equals(activeAudioLabel, ignoreCase = true)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = lang,
                                        color = if (isSelected) MuvioChampagne else MuvioTextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectLanguage(lang)
                                    showAudioMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Season Selector Pill (identical height & clean Noir styling)
            if (hasEpisodes || seasonsCount > 1) {
                Box {
                    Surface(
                        color = MuvioSurfaceCard,
                        border = BorderStroke(1.dp, MuvioOutline.copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { if (seasonsCount > 1) showSeasonMenu = true }
                            .testTag("compact_season_dropdown")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Season ${selectedSeason.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MuvioTextPrimary
                            )
                            if (seasonsCount > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Select Season",
                                    tint = MuvioChampagne,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showSeasonMenu,
                        onDismissRequest = { showSeasonMenu = false },
                        containerColor = MuvioSurfaceElevated
                    ) {
                        (1..seasonsCount.coerceAtLeast(1)).forEach { s ->
                            val epCount = media.seasonEpisodes[s]?.size ?: 0
                            val countSuffix = when {
                                epCount > 0 -> "  •  $epCount eps"
                                media.seasonEpisodes.containsKey(s) -> "  •  Upcoming"
                                else -> ""
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Season ${s.toString().padStart(2, '0')}$countSuffix",
                                        color = if (s == selectedSeason) MuvioChampagne else MuvioTextPrimary,
                                        fontWeight = if (s == selectedSeason) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectSeason(s)
                                    showSeasonMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Pinned [ All ] + Horizontal Scrollable Square Episode Tiles: [All] | [01] [02] [03] ...
        if (hasEpisodes) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pinned "All" tile on the left (stays anchored while scrolling episodes)
                Surface(
                    color = MuvioSurfaceElevated,
                    border = BorderStroke(0.9.dp, MuvioOutline.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenAllEpisodes)
                        .testTag("episode_tile_all")
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MuvioTextPrimary
                        )
                    }
                }

                // Scrollable numbered episode tiles
                LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 8.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(episodes, key = { idx, ep -> "compact_ep_${ep.number}_$idx" }) { _, ep ->
                        val isCurrent = ep.number == selectedEpisode
                        val tileShape = RoundedCornerShape(10.dp)
                        Surface(
                            color = Color.Transparent,
                            border = BorderStroke(
                                width = if (isCurrent) 1.1.dp else 0.8.dp,
                                color = if (isCurrent) MuvioChampagne else MuvioOutline.copy(alpha = 0.55f)
                            ),
                            shape = tileShape,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(tileShape)
                                .background(
                                    brush = if (isCurrent) {
                                        Brush.linearGradient(
                                            listOf(
                                                MuvioChampagne.copy(alpha = 0.24f),
                                                MuvioSurfaceCard
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(MuvioSurfaceCard, MuvioSurfaceCard)
                                        )
                                    }
                                )
                                .clickable { onSelectEpisode(ep.number) }
                                .testTag("episode_chip_${ep.number}")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ep.number.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isCurrent) MuvioChampagneLight else MuvioTextSecondary
                                )
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 4.dp)
                                            .width(14.dp)
                                            .height(2.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MuvioChampagne)
                                    )
                                }
                            }
                        }
                    }
                    if (episodes.size < 500) {
                        item(key = "compact_ep_load_more") {
                            Surface(
                                color = MuvioSurfaceElevated,
                                border = BorderStroke(0.9.dp, MuvioChampagne.copy(alpha = 0.65f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onLoadMoreEpisodes)
                                    .testTag("episode_tile_load_more")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+ More",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MuvioChampagneLight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (seasonsCount > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Season ${selectedSeason.toString().padStart(2, '0')} episodes coming soon",
                style = MaterialTheme.typography.bodySmall,
                color = MuvioTextMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllEpisodesBottomSheet(
    media: MediaItem,
    seasonsCount: Int,
    selectedSeason: Int,
    selectedEpisode: Int,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (Int) -> Unit,
    onDownloadEpisode: (EpisodeItem) -> Unit,
    onLoadMoreEpisodes: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MuvioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "All Episodes",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlayfairFontFamily),
                    color = MuvioTextPrimary
                )
                Text(
                    text = "${media.title} • Season $selectedSeason",
                    style = MaterialTheme.typography.bodySmall,
                    color = MuvioChampagne,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MuvioOutline.copy(alpha = 0.5f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                NoirEpisodesSection(
                    media = media,
                    seasonsCount = seasonsCount,
                    selectedSeason = selectedSeason,
                    selectedEpisode = selectedEpisode,
                    onSelectSeason = onSelectSeason,
                    onSelectEpisode = { epNum ->
                        onSelectEpisode(epNum)
                        onDismiss()
                    },
                    onDownloadEpisode = onDownloadEpisode,
                    onLoadMoreEpisodes = onLoadMoreEpisodes
                )
            }
        }
    }
}

