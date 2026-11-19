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
import com.example.ui.theme.AutoStories
import com.example.ui.theme.DownloadForOffline
import com.example.ui.theme.SlowMotionVideo
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
import com.example.model.MainTab
import com.example.model.MediaItem
import com.example.model.PosterArtwork
import com.example.ui.theme.MuvioBadgeOverlay
import com.example.ui.theme.MuvioBg
import com.example.ui.theme.MuvioBronze
import com.example.ui.theme.MuvioChampagne
import com.example.ui.theme.MuvioChampagneLight
import com.example.ui.theme.MuvioGlass
import com.example.ui.theme.MuvioGlassBorder
import com.example.ui.theme.MuvioHotRed
import com.example.ui.theme.MuvioOutline
import com.example.ui.theme.MuvioSurfaceCard
import com.example.ui.theme.MuvioTextMuted
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTextSecondary
import com.example.ui.theme.PlayfairFontFamily

@Composable
fun MuvioBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MuvioBg,
        border = BorderStroke(1.dp, MuvioGlassBorder),
        shadowElevation = 6.dp,
        modifier = modifier.size(size)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_muvio_noir_logo_1795061163701),
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
        MuvioBrandMark(size = 34.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "MUVIO",
                fontFamily = PlayfairFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 2.8.sp,
                color = MuvioTextPrimary
            )
            Text(
                text = "NOIR CINEMA",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    letterSpacing = 1.8.sp
                ),
                color = MuvioChampagne
            )
        }
    }
}

private val PosterVignetteBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Black.copy(alpha = 0.08f),
        Color.Transparent,
        Color.Black.copy(alpha = 0.58f)
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
                    .crossfade(100)
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

        // Refined Noir cinematic vignette for crisp overlay legibility
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
            shape = RoundedCornerShape(16.dp),
            color = MuvioSurfaceCard,
            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.09f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.69f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MuvioPosterArtwork(
                    poster = item.poster,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-left subtle rating pill
                Surface(
                    color = MuvioBadgeOverlay,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                                letterSpacing = 0.sp
                            ),
                            color = MuvioTextPrimary
                        )
                    }
                }

                // Top-right Language Badge
                item.languageBadge?.let { badge ->
                    Surface(
                        color = MuvioChampagne.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(7.dp),
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
                            color = Color(0xFF140F05),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Asymmetric Editorial Playfair Rank Number
                if (showRank && item.rankNumber != null) {
                    Text(
                        text = item.rankNumber.toString().padStart(2, '0'),
                        fontFamily = PlayfairFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        color = MuvioChampagneLight.copy(alpha = 0.78f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
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
            // Subtle Champagne Gold vertical editorial hairline
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MuvioChampagneLight, MuvioBronze)
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
                            letterSpacing = 1.4.sp
                        ),
                        color = MuvioChampagne
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                    color = MuvioTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onSeeAllClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("see_all_${title.lowercase().replace(" ", "_")}")
            ) {
                Text(
                    text = stringResource(R.string.see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MuvioChampagne
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.see_all),
                    tint = MuvioChampagne,
                    modifier = Modifier.size(18.dp)
                )
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
    // Noir Atelier Floating Frosted Glass Capsule Dock
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MuvioGlass,
            border = BorderStroke(1.dp, MuvioGlassBorder),
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 18.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
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
                    selectedIcon = Icons.Rounded.AutoStories,
                    unselectedIcon = Icons.Outlined.AutoStories,
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
            selected -> Color(0xFF140F05)
            isFeaturedCenter -> MuvioChampagne
            else -> MuvioTextSecondary
        },
        label = "capsuleTint"
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 52.dp else 40.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "capsuleWidth"
    )

    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(22.dp))
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
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected) {
                            Brush.horizontalGradient(listOf(MuvioChampagneLight, MuvioChampagne))
                        } else if (isFeaturedCenter) {
                            Brush.horizontalGradient(
                                listOf(
                                    MuvioChampagne.copy(alpha = 0.14f),
                                    MuvioBronze.copy(alpha = 0.14f)
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
            color = if (selected) MuvioChampagne else MuvioTextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
