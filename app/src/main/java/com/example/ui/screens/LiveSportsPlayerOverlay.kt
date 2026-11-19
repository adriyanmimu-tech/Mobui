package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import com.example.ui.theme.Pause
import com.example.ui.theme.SportsSoccer
import com.example.ui.theme.VolumeOff
import com.example.ui.theme.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.data.MuvioExoPlayerFactory
import coil.compose.AsyncImage
import com.example.model.LiveMatchItem
import com.example.model.MediaItem
import com.example.model.StaffMember
import com.example.ui.components.MediaPosterCard
import com.example.ui.theme.MuvioBg
import com.example.ui.theme.MuvioEmerald
import com.example.ui.theme.MuvioLiveRed
import com.example.ui.theme.MuvioOutline
import com.example.ui.theme.MuvioSurface
import com.example.ui.theme.MuvioSurfaceElevated
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTextSecondary

@OptIn(UnstableApi::class)
@Composable
fun LiveSportsPlayerOverlay(
    match: LiveMatchItem,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryNonce by remember { mutableIntStateOf(0) }

    val exoPlayer = remember(match.id, match.streamUrl, retryNonce) {
        MuvioExoPlayerFactory.createLivePlayer(context, match.headers).apply {
            setMediaItem(ExoMediaItem.fromUri(match.streamUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = "Live stream interrupted (${error.errorCodeName}). Tap Retry."
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        exoPlayer.playWhenReady = isPlaying
    }

    LaunchedEffect(isMuted, exoPlayer) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("live_sports_player_overlay")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar with league & close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MuvioLiveRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "● LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MuvioLiveRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${match.homeTeam} vs ${match.awayTeam}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MuvioTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_live_sports_player")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close live sports player",
                        tint = MuvioTextPrimary
                    )
                }
            }

            // 16:9 Live HLS Player Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    update = { it.player = exoPlayer },
                    modifier = Modifier.fillMaxSize()
                )

                if (isBuffering && errorMessage == null) {
                    CircularProgressIndicator(color = MuvioEmerald)
                }

                errorMessage?.let { msg ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MuvioTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MuvioEmerald,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable {
                                errorMessage = null
                                isBuffering = true
                                retryNonce++
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Retry",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Retry Stream",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // Player Quick Controls & Scoreboard Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.SportsSoccer,
                        contentDescription = null,
                        tint = MuvioEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = match.league,
                            style = MaterialTheme.typography.titleSmall,
                            color = MuvioTextPrimary
                        )
                        Text(
                            text = "${match.sportType} • ${match.scoreOrTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuvioEmerald
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MuvioSurfaceElevated,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MuvioOutline)
                    ) {
                        IconButton(onClick = { isPlaying = !isPlaying }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MuvioTextPrimary
                            )
                        }
                    }
                    Surface(
                        color = MuvioSurfaceElevated,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MuvioOutline)
                    ) {
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = MuvioTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffFilmographySheet(
    staff: StaffMember,
    movies: List<MediaItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onMediaClick: (MediaItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MuvioSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Surface(
                    color = MuvioSurfaceElevated,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MuvioEmerald),
                    modifier = Modifier.size(50.dp)
                ) {
                    if (!staff.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = staff.avatarUrl,
                            contentDescription = staff.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = staff.name.firstOrNull()?.uppercase() ?: "S",
                                style = MaterialTheme.typography.titleLarge,
                                color = MuvioEmerald
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = staff.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MuvioTextPrimary
                    )
                    Text(
                        text = staff.role,
                        style = MaterialTheme.typography.bodySmall,
                        color = MuvioEmerald
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MuvioEmerald)
                }
            } else if (movies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No additional titles found for ${staff.name}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MuvioTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(movies.chunked(3)) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                MediaPosterCard(
                                    item = item,
                                    onClick = {
                                        onDismiss()
                                        onMediaClick(item)
                                    },
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
