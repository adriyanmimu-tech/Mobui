package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.model.RemoteAppConfig
import com.example.ui.theme.MuvioChampagne
import com.example.ui.theme.MuvioChampagneLight
import com.example.ui.theme.MuvioCrimson
import com.example.ui.theme.MuvioCrimsonBorder
import com.example.ui.theme.MuvioCrimsonContainer
import com.example.ui.theme.MuvioCrimsonDeep
import com.example.ui.theme.MuvioCrimsonGlow
import com.example.ui.theme.MuvioSurfaceCard
import com.example.ui.theme.MuvioSurfaceElevated
import com.example.ui.theme.MuvioTextPrimary
import com.example.ui.theme.MuvioTextSecondary

@Composable
fun LiveAnnouncementBanner(
    tag: String,
    message: String,
    hasActionLink: Boolean,
    onBannerClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MuvioSurfaceCard,
        border = BorderStroke(1.dp, MuvioChampagne.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(if (hasActionLink) Modifier.clickable(onClick = onBannerClick) else Modifier)
            .testTag("home_live_announcement_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MuvioCrimsonContainer.copy(alpha = 0.72f),
                            MuvioSurfaceElevated
                        )
                    )
                )
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MuvioChampagne.copy(alpha = 0.18f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = stringResource(R.string.cd_announcement_icon),
                    tint = MuvioChampagneLight,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tag.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MuvioChampagne
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MuvioTextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("dismiss_announcement_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.cd_dismiss_announcement),
                    tint = MuvioTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MuvioInAppUpdateDialog(
    config: RemoteAppConfig,
    onUpdateNow: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val resolvedTitle = config.updateTitle.ifBlank {
        stringResource(R.string.update_dialog_default_title)
    }
    val resolvedNotes = config.updateNotes.ifBlank {
        "নতুন ভার্সনে দ্রুত স্ট্রিমিং, উন্নত পারফরম্যান্স এবং নতুন ফিচার যোগ করা হয়েছে।"
    }

    AlertDialog(
        onDismissRequest = {
            if (!config.forceUpdate) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !config.forceUpdate,
            dismissOnClickOutside = !config.forceUpdate
        ),
        containerColor = MuvioSurfaceElevated,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(MuvioCrimson, MuvioCrimsonDeep)))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.cd_update_icon),
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = MuvioCrimsonContainer,
                    border = BorderStroke(0.8.dp, MuvioCrimsonBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}  →  v${config.latestVersionName}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MuvioChampagneLight,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MuvioTextPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = resolvedNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuvioTextSecondary
                )
                if (config.forceUpdate) {
                    Text(
                        text = "অ্যাপ ঠিকভাবে চালানোর জন্য এই আপডেটটি প্রয়োজন।",
                        style = MaterialTheme.typography.labelMedium,
                        color = MuvioCrimsonGlow
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdateNow(config.updateUrl) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MuvioCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("in_app_update_now_btn")
            ) {
                Text(
                    text = stringResource(R.string.update_dialog_cta),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        dismissButton = if (!config.forceUpdate) {
            {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("in_app_update_later_btn")
                ) {
                    Text(
                        text = stringResource(R.string.update_dialog_later),
                        color = MuvioTextSecondary
                    )
                }
            }
        } else {
            null
        }
    )
}
