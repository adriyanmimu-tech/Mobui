package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.MuvioChampagne
import com.example.ui.theme.MuvioCrimson
import com.example.ui.theme.MuvioCrimsonBorder
import com.example.ui.theme.MuvioCrimsonContainer
import com.example.ui.theme.MuvioCrimsonDeep
import com.example.ui.theme.MuvioCrimsonGlow
import com.example.ui.theme.MuvioGlassBorder
import com.example.ui.theme.MuvioSurfaceCard
import com.example.ui.theme.MuvioSurfaceElevated
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTextSecondary

@Composable
fun HomeLiveSyncingCard(
    statusMessage: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MuvioSurfaceCard,
        border = BorderStroke(1.dp, MuvioCrimsonBorder.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("home_live_syncing_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MuvioCrimsonContainer.copy(alpha = 0.55f),
                            MuvioSurfaceCard,
                            MuvioSurfaceElevated
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MuvioCrimsonContainer)
                ) {
                    CircularProgressIndicator(
                        color = MuvioCrimsonGlow,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_syncing_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MuvioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.home_syncing_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MuvioTextSecondary
                    )
                    if (statusMessage.isNotBlank() && statusMessage != "Connecting...") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MuvioChampagne
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSecretsRequiredCard(
    offlineDownloadCount: Int = 0,
    onRetrySync: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = MuvioSurfaceCard,
        border = BorderStroke(1.dp, MuvioCrimsonBorder),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("home_secrets_required_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MuvioCrimsonContainer.copy(alpha = 0.65f),
                            MuvioSurfaceCard,
                            MuvioSurfaceCard
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(MuvioCrimson, MuvioCrimsonDeep))
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = stringResource(R.string.cd_connect_api_icon),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_secrets_setup_eyebrow),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.3.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MuvioChampagne
                    )
                    Text(
                        text = stringResource(R.string.home_secrets_setup_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MuvioTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.home_secrets_setup_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MuvioTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetrySync,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MuvioCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("home_retry_sync_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.home_retry_sync_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (offlineDownloadCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenDownloads,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MuvioSurfaceElevated,
                        contentColor = MuvioChampagne
                    ),
                    border = BorderStroke(1.dp, MuvioChampagne.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("home_open_downloads_shortcut_btn")
                ) {
                    Text(
                        text = "${stringResource(R.string.home_open_downloads_cta)} ($offlineDownloadCount)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeConnectionErrorCard(
    statusMessage: String,
    offlineDownloadCount: Int = 0,
    onRetry: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = MuvioSurfaceCard,
        border = BorderStroke(1.dp, MuvioGlassBorder),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("home_connection_error_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MuvioCrimsonContainer)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = stringResource(R.string.cd_connection_warning_icon),
                        tint = MuvioCrimsonGlow,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_reconnect_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MuvioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MuvioTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MuvioCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("home_retry_sync_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.home_retry_sync_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpenDownloads,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MuvioSurfaceElevated,
                    contentColor = MuvioChampagne
                ),
                border = BorderStroke(1.dp, MuvioChampagne.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("home_open_downloads_shortcut_btn")
            ) {
                val countSuffix = if (offlineDownloadCount > 0) " ($offlineDownloadCount)" else ""
                Text(
                    text = "${stringResource(R.string.home_open_downloads_cta)}$countSuffix",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
