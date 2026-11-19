package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightnessHigh
import com.example.ui.theme.MuvioGlassBorder
import com.example.ui.theme.VolumeOff
import com.example.ui.theme.VolumeUp
import kotlin.math.roundToInt

enum class PlayerGestureType {
    BRIGHTNESS,
    VOLUME
}

data class PlayerGestureHudState(
    val type: PlayerGestureType,
    val level: Float // 0f..1f
)

/**
 * Reads the current window brightness (or system brightness fallback) as 0.05f..1.0f.
 */
fun readCurrentBrightness(activity: Activity?): Float {
    val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
    if (windowBrightness in 0f..1f) return windowBrightness.coerceIn(0.02f, 1f)
    return runCatching {
        val resolver = activity?.contentResolver ?: return@runCatching 0.5f
        val sysVal = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        (sysVal / 255f).coerceIn(0.05f, 1f)
    }.getOrDefault(0.5f)
}

/**
 * Applies screen brightness smoothly to the current Activity window without needing WRITE_SETTINGS.
 */
fun applyWindowBrightness(activity: Activity?, level: Float) {
    val window = activity?.window ?: return
    val params = window.attributes
    params.screenBrightness = level.coerceIn(0.02f, 1f)
    window.attributes = params
}

/**
 * Restores system-managed brightness when leaving the video player screen.
 */
fun resetWindowBrightness(activity: Activity?) {
    val window = activity?.window ?: return
    val params = window.attributes
    if (params.screenBrightness != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = params
    }
}

/**
 * Reads current STREAM_MUSIC volume ratio in 0f..1f.
 */
fun readMediaVolumeFraction(context: Context): Float {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return 0.5f
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, max)
    return cur.toFloat() / max.toFloat()
}

/**
 * Sets STREAM_MUSIC volume smoothly from a 0f..1f fraction without showing the bulky system volume popup.
 */
fun applyMediaVolumeFraction(context: Context, fraction: Float) {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val target = (fraction.coerceIn(0f, 1f) * max).roundToInt().coerceIn(0, max)
    if (audio.getStreamVolume(AudioManager.STREAM_MUSIC) != target) {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }
}

/**
 * Floating dark translucent pill HUD for vertical brightness & volume swipe gestures,
 * matching the crisp horizontal bar aesthetic seen in modern cinema players.
 */
@Composable
fun PlayerGesturePillHud(
    hudState: PlayerGestureHudState?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = hudState != null,
        enter = fadeIn(tween(110)),
        exit = fadeOut(tween(220)),
        modifier = modifier
    ) {
        val state = hudState ?: return@AnimatedVisibility
        val clamped = state.level.coerceIn(0f, 1f)
        val percent = (clamped * 100f).roundToInt()
        val icon = when (state.type) {
            PlayerGestureType.BRIGHTNESS -> Icons.Rounded.BrightnessHigh
            PlayerGestureType.VOLUME -> if (clamped <= 0.01f) {
                Icons.AutoMirrored.Rounded.VolumeOff
            } else {
                Icons.AutoMirrored.Rounded.VolumeUp
            }
        }
        val iconDesc = when (state.type) {
            PlayerGestureType.BRIGHTNESS -> "Brightness"
            PlayerGestureType.VOLUME -> "Volume"
        }

        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(0.7.dp, MuvioGlassBorder.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = iconDesc,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )

                // Smooth white horizontal progress bar on soft translucent track
                Box(
                    modifier = Modifier
                        .width(112.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.28f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(clamped)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }

                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }
    }
}
