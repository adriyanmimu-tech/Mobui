package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.model.ContentSource
import com.example.model.MainTab
import com.example.model.MediaItem
import com.example.model.PosterArtwork
import com.example.ui.theme.DownloadForOffline
import com.example.ui.theme.Explore
import com.example.ui.theme.MuvioBadgeOverlay
import com.example.ui.theme.MuvioChampagne
import com.example.ui.theme.MuvioChampagneLight
import com.example.ui.theme.MuvioCrimson
import com.example.ui.theme.MuvioCrimsonBorder
import com.example.ui.theme.MuvioCrimsonDeep
import com.example.ui.theme.MuvioCrimsonGlow
import com.example.ui.theme.MuvioGlass
import com.example.ui.theme.MuvioGlassBorder
import com.example.ui.theme.MuvioHotRed
import com.example.ui.theme.MuvioSurfaceCard
import com.example.ui.theme.MuvioTextMuted
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTextSecondary
import com.example.ui.theme.PlayfairFontFamily
import com.example.ui.theme.SlowMotionVideo

@Composable
fun MuvioBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black,
        border = BorderStroke(1.dp, MuvioCrimsonBorder),
        shadowElevation = 10.dp,
        modifier = modifier.size(size)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_muvio_ribbon_logo),
            contentDescription = "Muvio Brand Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun MuvioWordmark(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MuvioBrandMark(size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "MUVIO",
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = 3.0.sp,
                color = MuvioTextPrimary
            )
            Text(
                text = "VELVET CINEMA",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    letterSpacing = 2.0.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MuvioCrimsonGlow
            )
        }
    }
}

private val PosterVignetteBrush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color.Black.copy(alpha = 0.14f),
        0.45f to Color.Transparent,
        0.82f to Color.Black.copy(alpha = 0.56f),
        1.0f to Color.Black.copy(alpha = 0.84f)
    )
)

@Composable
fun MuvioPosterArtwork(
    poster: PosterArtwork,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(modifier = modifier.background(MuvioSurfaceCard)) {
        val remoteUrl = poster.remoteUrl
        if (!remoteUrl.isNullOrBlank()) {
            val request = remember(remoteUrl, poster.fallbackRes) {
                ImageRequest.Builder(context)
                    .data(remoteUrl)
                    .size(340, 490)
                    .precision(coil.size.Precision.INEXACT)
                    .memoryCacheKey(remoteUrl)
                    .diskCacheKey(remoteUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .allowRgb565(true)
                    .crossfade(110)
                    .error(poster.fallbackRes)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = poster.fallbackRes),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PosterVignetteBrush)
        )
    }
}

@Composable
fun MediaPosterCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showRank: Boolean = false
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("poster_card_${item.id}")
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MuvioSurfaceCard,
            border = BorderStroke(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.04f),
                        MuvioCrimson.copy(alpha = 0.18f)
                    )
                )
            ),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MuvioPosterArtwork(
                    poster = item.poster,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-left frosted obsidian rating pill
                Surface(
                    color = MuvioBadgeOverlay,
                    border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MuvioChampagne,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = item.rating.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp
                            ),
                            color = MuvioTextPrimary
                        )
                    }
                }

                // Top-right Crimson Silk language / quality badge
                item.languageBadge?.let { badge ->
                    Surface(
                        color = MuvioCrimson.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(7.dp)
                    ) {
                        Text(
                            text = badge.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.5.dp, vertical = 2.5.dp)
                        )
                    }
                }

                // Bottom-right subtle dual-source micro-tag
                if (item.contentSource == ContentSource.SECONDARY) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        border = BorderStroke(0.6.dp, MuvioChampagne.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "HD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MuvioChampagneLight,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Dramatic Editorial Playfair Rank Numeral
                if (showRank && item.rankNumber != null) {
                    Text(
                        text = item.rankNumber.toString().padStart(2, '0'),
                        fontFamily = PlayfairFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 44.sp,
                        color = MuvioChampagneLight.copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 0.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            ),
            color = MuvioTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = "${item.year} • ${item.genre.substringBefore(",")}",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
            color = MuvioTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    leadingEmoji: String? = null,
    eyebrow: String? = null,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Crimson-to-Champagne vertical silk ribbon bar
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MuvioCrimsonGlow, MuvioCrimson, MuvioChampagne)
                        )
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        ),
                        color = MuvioCrimsonGlow
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MuvioTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onSeeAllClick != null) {
            Surface(
                color = MuvioGlass,
                border = BorderStroke(0.8.dp, MuvioGlassBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onSeeAllClick)
                    .testTag("see_all_${title.lowercase().replace(" ", "_")}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = stringResource(R.string.see_all),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MuvioChampagneLight
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.see_all),
                        tint = MuvioCrimsonGlow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MuvioBottomBar(
    selectedTab: MainTab,
    activeDownloadCount: Int,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // Velvet Cinema Floating Smoked-Glass Capsule Dock
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xEB0B0D14),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.14f),
                        MuvioCrimson.copy(alpha = 0.48f),
                        Color.White.copy(alpha = 0.14f)
                    )
                )
            ),
            shape = RoundedCornerShape(34.dp),
            shadowElevation = 22.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CapsuleNavItem(
                    label = stringResource(R.string.nav_home),
                    selected = selectedTab == MainTab.HOME,
                    selectedIcon = Icons.Rounded.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onSelectTab(MainTab.HOME) },
                    testTag = "nav_home",
                    modifier = Modifier.weight(1f)
                )

                CapsuleNavItem(
                    label = stringResource(R.string.nav_novel_hub),
                    selected = selectedTab == MainTab.NOVEL_HUB,
                    selectedIcon = Icons.Rounded.Explore,
                    unselectedIcon = Icons.Outlined.Explore,
                    onClick = { onSelectTab(MainTab.NOVEL_HUB) },
                    testTag = "nav_novel_hub",
                    modifier = Modifier.weight(1f)
                )

                CapsuleNavItem(
                    label = stringResource(R.string.nav_shorts),
                    selected = selectedTab == MainTab.SHORTS,
                    selectedIcon = Icons.Rounded.SlowMotionVideo,
                    unselectedIcon = Icons.Outlined.SlowMotionVideo,
                    isFeaturedCenter = true,
                    onClick = { onSelectTab(MainTab.SHORTS) },
                    testTag = "nav_shorts",
                    modifier = Modifier.weight(1f)
                )

                CapsuleNavItem(
                    label = stringResource(R.string.nav_downloads),
                    selected = selectedTab == MainTab.DOWNLOADS,
                    selectedIcon = Icons.Rounded.DownloadForOffline,
                    unselectedIcon = Icons.Outlined.DownloadForOffline,
                    countBadge = activeDownloadCount,
                    onClick = { onSelectTab(MainTab.DOWNLOADS) },
                    testTag = "nav_downloads",
                    modifier = Modifier.weight(1f)
                )

                CapsuleNavItem(
                    label = stringResource(R.string.nav_me),
                    selected = selectedTab == MainTab.ME,
                    selectedIcon = Icons.Rounded.Person,
                    unselectedIcon = Icons.Outlined.Person,
                    onClick = { onSelectTab(MainTab.ME) },
                    testTag = "nav_me",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CapsuleNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    isFeaturedCenter: Boolean = false,
    countBadge: Int = 0
) {
    val iconTint by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            isFeaturedCenter -> MuvioCrimsonGlow
            else -> MuvioTextSecondary
        },
        label = "capsuleTint"
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 54.dp else 40.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "capsuleWidth"
    )

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .height(33.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        if (selected) {
                            Brush.horizontalGradient(
                                listOf(MuvioCrimsonGlow, MuvioCrimson, MuvioCrimsonDeep)
                            )
                        } else if (isFeaturedCenter) {
                            Brush.horizontalGradient(
                                listOf(
                                    MuvioCrimson.copy(alpha = 0.22f),
                                    MuvioCrimsonDeep.copy(alpha = 0.28f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(21.dp)
                )
            }

            if (countBadge > 0) {
                Surface(
                    color = MuvioHotRed,
                    shape = CircleShape,
                    modifier = Modifier.offset(x = 2.dp, y = (-2).dp)
                ) {
                    Text(
                        text = countBadge.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.2.sp
            ),
            color = if (selected) MuvioTextPrimary else MuvioTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
