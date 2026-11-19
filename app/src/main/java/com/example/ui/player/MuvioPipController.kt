package com.example.ui.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Smart Picture-in-Picture controller for Muvio.
 *
 * Features:
 * - Manual PiP entry from the player control bar.
 * - Smart Auto-PiP on Android 12+ (`setAutoEnterEnabled`) and Android 8–11 (`onUserLeaveHint`)
 *   whenever a video is actively playing.
 * - Seamless morph transition using the player view's screen `sourceRectHint`.
 * - Interactive PiP window remote controls (Rewind 10s, Play/Pause, Forward 10s).
 */
class MuvioPipController(
    private val activity: Activity
) {
    private val _isInPipMode = MutableStateFlow(false)
    val isInPipMode: StateFlow<Boolean> = _isInPipMode.asStateFlow()

    private var isPlayerScreenActive: Boolean = false
    private var isPlaying: Boolean = false
    private var sourceRectHint: Rect? = null
    private var onTogglePlayPause: (() -> Unit)? = null
    private var onRewind10: (() -> Unit)? = null
    private var onForward10: (() -> Unit)? = null
    private var receiverRegistered: Boolean = false

    val isPipSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> onTogglePlayPause?.invoke()
                ACTION_PIP_REWIND -> onRewind10?.invoke()
                ACTION_PIP_FORWARD -> onForward10?.invoke()
            }
        }
    }

    fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_REWIND)
            addAction(ACTION_PIP_FORWARD)
        }
        runCatching {
            ContextCompat.registerReceiver(
                activity,
                pipActionReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    fun unregisterReceiver() {
        if (!receiverRegistered) return
        runCatching { activity.unregisterReceiver(pipActionReceiver) }
        receiverRegistered = false
    }

    fun onPipModeChanged(inPip: Boolean) {
        _isInPipMode.value = inPip
    }

    fun bindPlayer(
        isPlaying: Boolean,
        sourceRect: Rect?,
        onTogglePlayPause: () -> Unit,
        onRewind10: () -> Unit,
        onForward10: () -> Unit
    ) {
        this.isPlayerScreenActive = true
        this.isPlaying = isPlaying
        if (sourceRect != null && sourceRect.width() > 0 && sourceRect.height() > 0) {
            this.sourceRectHint = sourceRect
        }
        this.onTogglePlayPause = onTogglePlayPause
        this.onRewind10 = onRewind10
        this.onForward10 = onForward10
        syncPipParams()
    }

    fun updateSourceRect(rect: Rect) {
        if (rect.width() <= 0 || rect.height() <= 0) return
        if (sourceRectHint != rect) {
            sourceRectHint = rect
            if (isPlayerScreenActive) {
                syncPipParams()
            }
        }
    }

    fun unbindPlayer() {
        isPlayerScreenActive = false
        isPlaying = false
        sourceRectHint = null
        onTogglePlayPause = null
        onRewind10 = null
        onForward10 = null
        syncPipParams()
    }

    fun enterPip(): Boolean {
        if (!isPipSupported || activity.isFinishing || activity.isDestroyed) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                activity.enterPictureInPictureMode(buildPipParams())
            }.getOrDefault(false)
        } else {
            false
        }
    }

    /**
     * Fallback Smart Auto-PiP for Android 8–11 (API 26–30) when the user presses Home.
     * On Android 12+ (API 31+), `setAutoEnterEnabled(true)` handles this natively with smooth morphing.
     */
    fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O until Build.VERSION_CODES.S) {
            if (isPlayerScreenActive && isPlaying && !_isInPipMode.value) {
                enterPip()
            }
        }
    }

    private fun syncPipParams() {
        if (!isPipSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (activity.isFinishing || activity.isDestroyed) return
        runCatching {
            activity.setPictureInPictureParams(buildPipParams())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(buildRemoteActions())

        sourceRectHint?.takeIf { it.width() > 0 && it.height() > 0 }?.let { rect ->
            builder.setSourceRectHint(rect)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isPlayerScreenActive && isPlaying)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildRemoteActions(): List<RemoteAction> {
        val rewindIntent = PendingIntent.getBroadcast(
            activity,
            REQ_REWIND,
            Intent(ACTION_PIP_REWIND).setPackage(activity.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getBroadcast(
            activity,
            REQ_PLAY_PAUSE,
            Intent(ACTION_PIP_PLAY_PAUSE).setPackage(activity.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val forwardIntent = PendingIntent.getBroadcast(
            activity,
            REQ_FORWARD,
            Intent(ACTION_PIP_FORWARD).setPackage(activity.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        return listOf(
            RemoteAction(
                Icon.createWithResource(activity, android.R.drawable.ic_media_rew),
                "Rewind 10s",
                "Rewind 10 seconds",
                rewindIntent
            ),
            RemoteAction(
                Icon.createWithResource(activity, playPauseIcon),
                playPauseTitle,
                playPauseTitle,
                playPauseIntent
            ),
            RemoteAction(
                Icon.createWithResource(activity, android.R.drawable.ic_media_ff),
                "Forward 10s",
                "Forward 10 seconds",
                forwardIntent
            )
        )
    }

    companion object {
        private const val ACTION_PIP_PLAY_PAUSE = "com.example.muvio.PIP_PLAY_PAUSE"
        private const val ACTION_PIP_REWIND = "com.example.muvio.PIP_REWIND"
        private const val ACTION_PIP_FORWARD = "com.example.muvio.PIP_FORWARD"

        private const val REQ_REWIND = 4001
        private const val REQ_PLAY_PAUSE = 4002
        private const val REQ_FORWARD = 4003
    }
}

val LocalMuvioPipController = staticCompositionLocalOf<MuvioPipController?> { null }
