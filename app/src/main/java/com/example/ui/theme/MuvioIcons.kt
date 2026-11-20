package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lightweight Material Design vector icons used across Muvio.
 * Replaces the ~16 MB `material-icons-extended` artifact with ~20 KB of exact 24dp vector paths,
 * preserving identical visual design while drastically reducing APK size.
 */
private inline fun materialIcon(
    name: String,
    autoMirror: Boolean = false,
    block: PathBuilder.() -> Unit
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = autoMirror
).apply {
    path(fill = SolidColor(Color.Black), pathBuilder = block)
}.build()

private var _playlistAdd: ImageVector? = null
val Icons.AutoMirrored.Rounded.PlaylistAdd: ImageVector
    get() = _playlistAdd ?: materialIcon("PlaylistAdd", autoMirror = true) {
        moveTo(14f, 10f); horizontalLineTo(3f); verticalLineTo(12f); horizontalLineTo(14f); close()
        moveTo(14f, 6f); horizontalLineTo(3f); verticalLineTo(8f); horizontalLineTo(14f); close()
        moveTo(18f, 14f); verticalLineTo(10f); horizontalLineTo(16f); verticalLineTo(14f); horizontalLineTo(12f); verticalLineTo(16f); horizontalLineTo(16f); verticalLineTo(20f); horizontalLineTo(18f); verticalLineTo(16f); horizontalLineTo(22f); verticalLineTo(14f); close()
        moveTo(3f, 16f); horizontalLineTo(10f); verticalLineTo(14f); horizontalLineTo(3f); close()
    }.also { _playlistAdd = it }

private var _volumeUp: ImageVector? = null
val Icons.AutoMirrored.Rounded.VolumeUp: ImageVector
    get() = _volumeUp ?: materialIcon("VolumeUp", autoMirror = true) {
        moveTo(3f, 9f); verticalLineTo(15f); horizontalLineTo(7f); lineTo(12f, 20f); verticalLineTo(4f); lineTo(7f, 9f); close()
        moveTo(16.5f, 12f); curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 7.97f); verticalLineTo(16.02f); curveTo(15.48f, 15.29f, 16.5f, 13.77f, 16.5f, 12f); close()
        moveTo(14f, 3.23f); verticalLineTo(5.29f); curveTo(16.89f, 6.15f, 19f, 8.83f, 19f, 12f); curveTo(19f, 15.17f, 16.89f, 17.85f, 14f, 18.71f); verticalLineTo(20.77f); curveTo(18.01f, 19.86f, 21f, 16.28f, 21f, 12f); curveTo(21f, 7.72f, 18.01f, 4.14f, 14f, 3.23f); close()
    }.also { _volumeUp = it }

private var _volumeOff: ImageVector? = null
val Icons.AutoMirrored.Rounded.VolumeOff: ImageVector
    get() = _volumeOff ?: materialIcon("VolumeOff", autoMirror = true) {
        moveTo(16.5f, 12f); curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 7.97f); verticalLineTo(10.18f); lineTo(16.45f, 12.63f); curveTo(16.48f, 12.43f, 16.5f, 12.22f, 16.5f, 12f); close()
        moveTo(19f, 12f); curveTo(19f, 12.94f, 18.8f, 13.82f, 18.46f, 14.64f); lineTo(19.97f, 16.15f); curveTo(20.63f, 14.91f, 21f, 13.5f, 21f, 12f); curveTo(21f, 7.72f, 18.01f, 4.14f, 14f, 3.23f); verticalLineTo(5.29f); curveTo(16.89f, 6.15f, 19f, 8.83f, 19f, 12f); close()
        moveTo(4.27f, 3f); lineTo(3f, 4.27f); lineTo(7.73f, 9f); horizontalLineTo(3f); verticalLineTo(15f); horizontalLineTo(7f); lineTo(12f, 20f); verticalLineTo(13.27f); lineTo(16.25f, 17.52f); curveTo(15.58f, 18.04f, 14.83f, 18.45f, 14f, 18.7f); verticalLineTo(20.76f); curveTo(15.38f, 20.45f, 16.63f, 19.81f, 17.69f, 18.95f); lineTo(19.73f, 21f); lineTo(21f, 19.73f); lineTo(12f, 10.73f); close()
        moveTo(12f, 4f); lineTo(9.91f, 6.09f); lineTo(12f, 8.18f); close()
    }.also { _volumeOff = it }

private var _pause: ImageVector? = null
val Icons.Rounded.Pause: ImageVector
    get() = _pause ?: materialIcon("Pause") {
        moveTo(8f, 19f); curveTo(9.1f, 19f, 10f, 18.1f, 10f, 17f); verticalLineTo(7f); curveTo(10f, 5.9f, 9.1f, 5f, 8f, 5f); curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f); verticalLineTo(17f); curveTo(6f, 18.1f, 6.9f, 19f, 8f, 19f); close()
        moveTo(14f, 7f); verticalLineTo(17f); curveTo(14f, 18.1f, 14.9f, 19f, 16f, 19f); curveTo(17.1f, 19f, 18f, 18.1f, 18f, 17f); verticalLineTo(7f); curveTo(18f, 5.9f, 17.1f, 5f, 16f, 5f); curveTo(14.9f, 5f, 14f, 5.9f, 14f, 7f); close()
    }.also { _pause = it }

private var _download: ImageVector? = null
val Icons.Rounded.Download: ImageVector
    get() = _download ?: materialIcon("Download") {
        moveTo(16.59f, 9f); horizontalLineTo(15f); verticalLineTo(4f); curveTo(15f, 3.45f, 14.55f, 3f, 14f, 3f); horizontalLineTo(10f); curveTo(9.45f, 3f, 9f, 3.45f, 9f, 4f); verticalLineTo(9f); horizontalLineTo(7.41f); curveTo(6.52f, 9f, 6.07f, 10.08f, 6.7f, 10.71f); lineTo(11.29f, 15.3f); curveTo(11.68f, 15.69f, 12.31f, 15.69f, 12.7f, 15.3f); lineTo(17.29f, 10.71f); curveTo(17.92f, 10.08f, 17.48f, 9f, 16.59f, 9f); close()
        moveTo(5f, 19f); curveTo(5f, 19.55f, 5.45f, 20f, 6f, 20f); horizontalLineTo(18f); curveTo(18.55f, 20f, 19f, 19.55f, 19f, 19f); curveTo(19f, 18.45f, 18.55f, 18f, 18f, 18f); horizontalLineTo(6f); curveTo(5.45f, 18f, 5f, 18.45f, 5f, 19f); close()
    }.also { _download = it }

private var _downloadForOffline: ImageVector? = null
val Icons.Rounded.DownloadForOffline: ImageVector
    get() = _downloadForOffline ?: materialIcon("DownloadForOffline") {
        moveTo(12f, 2f); curveTo(6.49f, 2f, 2f, 6.49f, 2f, 12f); reflectiveCurveTo(6.49f, 22f, 12f, 22f); reflectiveCurveTo(22f, 17.51f, 22f, 12f); reflectiveCurveTo(17.51f, 2f, 12f, 2f); close()
        moveTo(11f, 10f); verticalLineTo(6f); horizontalLineTo(13f); verticalLineTo(10f); horizontalLineTo(16f); lineTo(12f, 14f); lineTo(8f, 10f); close()
        moveTo(17f, 17f); horizontalLineTo(7f); verticalLineTo(15f); horizontalLineTo(17f); close()
    }.also { _downloadForOffline = it }

private var _downloadForOfflineOutlined: ImageVector? = null
val Icons.Outlined.DownloadForOffline: ImageVector
    get() = _downloadForOfflineOutlined ?: materialIcon("DownloadForOfflineOutlined") {
        moveTo(12f, 2f); curveTo(6.49f, 2f, 2f, 6.49f, 2f, 12f); reflectiveCurveTo(6.49f, 22f, 12f, 22f); reflectiveCurveTo(22f, 17.51f, 22f, 12f); reflectiveCurveTo(17.51f, 2f, 12f, 2f); close()
        moveTo(12f, 20f); curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f); reflectiveCurveTo(7.59f, 4f, 12f, 4f); reflectiveCurveTo(20f, 7.59f, 20f, 12f); reflectiveCurveTo(16.41f, 20f, 12f, 20f); close()
        moveTo(13f, 10f); verticalLineTo(6f); horizontalLineTo(11f); verticalLineTo(10f); horizontalLineTo(8f); lineTo(12f, 14f); lineTo(16f, 10f); close()
        moveTo(17f, 15f); horizontalLineTo(7f); verticalLineTo(17f); horizontalLineTo(17f); close()
    }.also { _downloadForOfflineOutlined = it }

private var _bookmark: ImageVector? = null
val Icons.Rounded.Bookmark: ImageVector
    get() = _bookmark ?: materialIcon("Bookmark") {
        moveTo(17f, 3f); horizontalLineTo(7f); curveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f); verticalLineTo(21f); lineTo(12f, 18f); lineTo(19f, 21f); verticalLineTo(5f); curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f); close()
    }.also { _bookmark = it }

private var _bookmarkBorder: ImageVector? = null
val Icons.Rounded.BookmarkBorder: ImageVector
    get() = _bookmarkBorder ?: materialIcon("BookmarkBorder") {
        moveTo(17f, 3f); horizontalLineTo(7f); curveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f); verticalLineTo(21f); lineTo(12f, 18f); lineTo(19f, 21f); verticalLineTo(5f); curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f); close()
        moveTo(17f, 18f); lineTo(12f, 15.82f); lineTo(7f, 18f); verticalLineTo(5f); horizontalLineTo(17f); close()
    }.also { _bookmarkBorder = it }

val Icons.Outlined.BookmarkBorder: ImageVector
    get() = Icons.Rounded.BookmarkBorder

private var _chatBubbleOutline: ImageVector? = null
val Icons.Outlined.ChatBubbleOutline: ImageVector
    get() = _chatBubbleOutline ?: materialIcon("ChatBubbleOutline") {
        moveTo(20f, 2f); horizontalLineTo(4f); curveTo(2.9f, 2f, 2f, 2.9f, 2f, 4f); verticalLineTo(22f); lineTo(6f, 18f); horizontalLineTo(20f); curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f); verticalLineTo(4f); curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f); close()
        moveTo(20f, 16f); horizontalLineTo(6f); lineTo(4f, 18f); verticalLineTo(4f); horizontalLineTo(20f); close()
    }.also { _chatBubbleOutline = it }

private var _deleteOutline: ImageVector? = null
val Icons.Outlined.DeleteOutline: ImageVector
    get() = _deleteOutline ?: materialIcon("DeleteOutline") {
        moveTo(6f, 19f); curveTo(6f, 20.1f, 6.9f, 21f, 8f, 21f); horizontalLineTo(16f); curveTo(17.1f, 21f, 18f, 20.1f, 18f, 19f); verticalLineTo(7f); horizontalLineTo(6f); close()
        moveTo(8f, 9f); horizontalLineTo(16f); verticalLineTo(19f); horizontalLineTo(8f); close()
        moveTo(15.5f, 4f); lineTo(14.5f, 3f); horizontalLineTo(9.5f); lineTo(8.5f, 4f); horizontalLineTo(5f); verticalLineTo(6f); horizontalLineTo(19f); verticalLineTo(4f); close()
    }.also { _deleteOutline = it }

private var _helpOutline: ImageVector? = null
val Icons.Outlined.HelpOutline: ImageVector
    get() = _helpOutline ?: materialIcon("HelpOutline") {
        moveTo(11f, 18f); horizontalLineTo(13f); verticalLineTo(16f); horizontalLineTo(11f); close()
        moveTo(12f, 2f); curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f); reflectiveCurveTo(6.48f, 22f, 12f, 22f); reflectiveCurveTo(22f, 17.52f, 22f, 12f); reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
        moveTo(12f, 20f); curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f); reflectiveCurveTo(7.59f, 4f, 12f, 4f); reflectiveCurveTo(20f, 7.59f, 20f, 12f); reflectiveCurveTo(16.41f, 20f, 12f, 20f); close()
        moveTo(12f, 6f); curveTo(9.79f, 6f, 8f, 7.79f, 8f, 10f); horizontalLineTo(10f); curveTo(10f, 8.9f, 10.9f, 8f, 12f, 8f); reflectiveCurveTo(14f, 8.9f, 14f, 10f); curveTo(14f, 12f, 11f, 11.75f, 11f, 15f); horizontalLineTo(13f); curveTo(13f, 12.75f, 16f, 12.5f, 16f, 10f); curveTo(16f, 7.79f, 14.21f, 6f, 12f, 6f); close()
    }.also { _helpOutline = it }

private var _autoStories: ImageVector? = null
val Icons.Rounded.AutoStories: ImageVector
    get() = _autoStories ?: materialIcon("AutoStories") {
        moveTo(21f, 5f); curveTo(19.89f, 4.65f, 18.67f, 4.5f, 17.5f, 4.5f); curveTo(15.55f, 4.5f, 13.45f, 4.9f, 12f, 6f); curveTo(10.55f, 4.9f, 8.45f, 4.5f, 6.5f, 4.5f); curveTo(4.55f, 4.5f, 2.45f, 4.9f, 1f, 6f); verticalLineTo(20.65f); curveTo(1f, 20.9f, 1.25f, 21.15f, 1.5f, 21.15f); curveTo(1.6f, 21.15f, 1.65f, 21.1f, 1.75f, 21.1f); curveTo(3.1f, 20.45f, 5.05f, 20f, 6.5f, 20f); curveTo(8.45f, 20f, 10.55f, 20.4f, 12f, 21.5f); curveTo(13.35f, 20.65f, 15.8f, 20f, 17.5f, 20f); curveTo(19.15f, 20f, 20.85f, 20.3f, 22.25f, 21.05f); curveTo(22.35f, 21.1f, 22.4f, 21.1f, 22.5f, 21.1f); curveTo(22.75f, 21.1f, 23f, 20.85f, 23f, 20.6f); verticalLineTo(6f); curveTo(22.4f, 5.55f, 21.75f, 5.25f, 21f, 5f); close()
        moveTo(21f, 18.5f); curveTo(19.9f, 18.15f, 18.7f, 18f, 17.5f, 18f); curveTo(15.8f, 18f, 13.35f, 18.65f, 12f, 19.5f); verticalLineTo(8f); curveTo(13.35f, 7.15f, 15.8f, 6.5f, 17.5f, 6.5f); curveTo(18.7f, 6.5f, 19.9f, 6.65f, 21f, 7f); close()
    }.also { _autoStories = it }

val Icons.Outlined.AutoStories: ImageVector
    get() = Icons.Rounded.AutoStories

private var _slowMotionVideo: ImageVector? = null
val Icons.Rounded.SlowMotionVideo: ImageVector
    get() = _slowMotionVideo ?: materialIcon("SlowMotionVideo") {
        moveTo(13.05f, 9.79f); lineTo(10f, 7.5f); verticalLineTo(16.5f); lineTo(13.05f, 14.21f); lineTo(16f, 12f); close()
        moveTo(12f, 2f); curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f); reflectiveCurveTo(6.48f, 22f, 12f, 22f); reflectiveCurveTo(22f, 17.52f, 22f, 12f); reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
        moveTo(12f, 20f); curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f); reflectiveCurveTo(7.59f, 4f, 12f, 4f); reflectiveCurveTo(20f, 7.59f, 20f, 12f); reflectiveCurveTo(16.41f, 20f, 12f, 20f); close()
    }.also { _slowMotionVideo = it }

val Icons.Outlined.SlowMotionVideo: ImageVector
    get() = Icons.Rounded.SlowMotionVideo

private var _api: ImageVector? = null
val Icons.Rounded.Api: ImageVector
    get() = _api ?: materialIcon("Api") {
        moveTo(14f, 12f); lineTo(12f, 14f); lineTo(10f, 12f); lineTo(12f, 10f); close()
        moveTo(12f, 6f); lineTo(14.12f, 8.12f); lineTo(16.62f, 5.62f); lineTo(12f, 1f); lineTo(7.38f, 5.62f); lineTo(9.88f, 8.12f); close()
        moveTo(6f, 12f); lineTo(8.12f, 9.88f); lineTo(5.62f, 7.38f); lineTo(1f, 12f); lineTo(5.62f, 16.62f); lineTo(8.12f, 14.12f); close()
        moveTo(18f, 12f); lineTo(15.88f, 14.12f); lineTo(18.38f, 16.62f); lineTo(23f, 12f); lineTo(18.38f, 7.38f); lineTo(15.88f, 9.88f); close()
        moveTo(12f, 18f); lineTo(9.88f, 15.88f); lineTo(7.38f, 18.38f); lineTo(12f, 23f); lineTo(16.62f, 18.38f); lineTo(14.12f, 15.88f); close()
    }.also { _api = it }

private var _autoAwesome: ImageVector? = null
val Icons.Rounded.AutoAwesome: ImageVector
    get() = _autoAwesome ?: materialIcon("AutoAwesome") {
        moveTo(19f, 9f); lineTo(20.25f, 6.25f); lineTo(23f, 5f); lineTo(20.25f, 3.75f); lineTo(19f, 1f); lineTo(17.75f, 3.75f); lineTo(15f, 5f); lineTo(17.75f, 6.25f); close()
        moveTo(11.5f, 9.5f); lineTo(9f, 4f); lineTo(6.5f, 9.5f); lineTo(1f, 12f); lineTo(6.5f, 14.5f); lineTo(9f, 20f); lineTo(11.5f, 14.5f); lineTo(17f, 12f); close()
        moveTo(19f, 15f); lineTo(17.75f, 17.75f); lineTo(15f, 19f); lineTo(17.75f, 20.25f); lineTo(19f, 23f); lineTo(20.25f, 20.25f); lineTo(23f, 19f); lineTo(20.25f, 17.75f); close()
    }.also { _autoAwesome = it }

private var _darkMode: ImageVector? = null
val Icons.Rounded.DarkMode: ImageVector
    get() = _darkMode ?: materialIcon("DarkMode") {
        moveTo(12f, 3f); curveTo(7.05f, 3f, 3f, 7.05f, 3f, 12f); curveTo(3f, 16.95f, 7.05f, 21f, 12f, 21f); curveTo(15.71f, 21f, 18.88f, 18.73f, 20.25f, 15.5f); curveTo(19.35f, 15.82f, 18.38f, 16f, 17.37f, 16f); curveTo(12.75f, 16f, 9f, 12.25f, 9f, 7.63f); curveTo(9f, 5.91f, 9.52f, 4.31f, 10.42f, 3f); close()
    }.also { _darkMode = it }

val Icons.Rounded.Bedtime: ImageVector
    get() = Icons.Rounded.DarkMode

private var _cast: ImageVector? = null
val Icons.Rounded.Cast: ImageVector
    get() = _cast ?: materialIcon("Cast") {
        moveTo(21f, 3f); horizontalLineTo(3f); curveTo(1.9f, 3f, 1f, 3.9f, 1f, 5f); verticalLineTo(8f); horizontalLineTo(3f); verticalLineTo(5f); horizontalLineTo(21f); verticalLineTo(19f); horizontalLineTo(14f); verticalLineTo(21f); horizontalLineTo(21f); curveTo(22.1f, 21f, 23f, 20.1f, 23f, 19f); verticalLineTo(5f); curveTo(23f, 3.9f, 22.1f, 3f, 21f, 3f); close()
        moveTo(1f, 18f); verticalLineTo(21f); horizontalLineTo(4f); curveTo(4f, 19.34f, 2.66f, 18f, 1f, 18f); close()
        moveTo(1f, 14f); verticalLineTo(16f); curveTo(3.76f, 16f, 6f, 18.24f, 6f, 21f); horizontalLineTo(8f); curveTo(8f, 17.13f, 4.87f, 14f, 1f, 14f); close()
        moveTo(1f, 10f); verticalLineTo(12f); curveTo(5.97f, 12f, 10f, 16.03f, 10f, 21f); horizontalLineTo(12f); curveTo(12f, 14.92f, 7.07f, 10f, 1f, 10f); close()
    }.also { _cast = it }

private var _closedCaption: ImageVector? = null
val Icons.Rounded.ClosedCaption: ImageVector
    get() = _closedCaption ?: materialIcon("ClosedCaption") {
        moveTo(19f, 4f); horizontalLineTo(5f); curveTo(3.89f, 4f, 3f, 4.9f, 3f, 6f); verticalLineTo(18f); curveTo(3f, 19.1f, 3.89f, 20f, 5f, 20f); horizontalLineTo(19f); curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f); verticalLineTo(6f); curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f); close()
        moveTo(11f, 11f); horizontalLineTo(9.5f); verticalLineTo(10.5f); horizontalLineTo(7.5f); verticalLineTo(13.5f); horizontalLineTo(9.5f); verticalLineTo(13f); horizontalLineTo(11f); verticalLineTo(14f); curveTo(11f, 14.55f, 10.55f, 15f, 10f, 15f); horizontalLineTo(7f); curveTo(6.45f, 15f, 6f, 14.55f, 6f, 14f); verticalLineTo(10f); curveTo(6f, 9.45f, 6.45f, 9f, 7f, 9f); horizontalLineTo(10f); curveTo(10.55f, 9f, 11f, 9.45f, 11f, 10f); close()
        moveTo(18f, 11f); horizontalLineTo(16.5f); verticalLineTo(10.5f); horizontalLineTo(14.5f); verticalLineTo(13.5f); horizontalLineTo(16.5f); verticalLineTo(13f); horizontalLineTo(18f); verticalLineTo(14f); curveTo(18f, 14.55f, 17.55f, 15f, 17f, 15f); horizontalLineTo(14f); curveTo(13.45f, 15f, 13f, 14.55f, 13f, 14f); verticalLineTo(10f); curveTo(13f, 9.45f, 13.45f, 9f, 14f, 9f); horizontalLineTo(17f); curveTo(17.55f, 9f, 18f, 9.45f, 18f, 10f); close()
    }.also { _closedCaption = it }

private var _forward10: ImageVector? = null
val Icons.Rounded.Forward10: ImageVector
    get() = _forward10 ?: materialIcon("Forward10") {
        moveTo(18f, 13f); curveTo(18f, 16.31f, 15.31f, 19f, 12f, 19f); reflectiveCurveTo(6f, 16.31f, 6f, 13f); reflectiveCurveTo(8.69f, 7f, 12f, 7f); verticalLineTo(11f); lineTo(17f, 6f); lineTo(12f, 1f); verticalLineTo(5f); curveTo(7.58f, 5f, 4f, 8.58f, 4f, 13f); reflectiveCurveTo(7.58f, 21f, 12f, 21f); reflectiveCurveTo(20f, 17.42f, 20f, 13f); close()
        moveTo(10.89f, 16f); horizontalLineTo(9.89f); verticalLineTo(12.26f); lineTo(8.8f, 12.6f); verticalLineTo(11.77f); lineTo(10.79f, 11.06f); horizontalLineTo(10.89f); close()
        moveTo(15.17f, 14.07f); curveTo(15.17f, 15.27f, 14.54f, 16.07f, 13.46f, 16.07f); reflectiveCurveTo(11.75f, 15.27f, 11.75f, 14.07f); verticalLineTo(13.06f); curveTo(11.75f, 11.86f, 12.38f, 11.06f, 13.46f, 11.06f); reflectiveCurveTo(15.17f, 11.86f, 15.17f, 13.06f); close()
        moveTo(14.17f, 12.93f); curveTo(14.17f, 12.22f, 13.88f, 11.86f, 13.46f, 11.86f); reflectiveCurveTo(12.75f, 12.22f, 12.75f, 12.93f); verticalLineTo(14.2f); curveTo(12.75f, 14.91f, 13.04f, 15.27f, 13.46f, 15.27f); reflectiveCurveTo(14.17f, 14.91f, 14.17f, 14.2f); close()
    }.also { _forward10 = it }

private var _replay10: ImageVector? = null
val Icons.Rounded.Replay10: ImageVector
    get() = _replay10 ?: materialIcon("Replay10") {
        moveTo(12f, 5f); verticalLineTo(1f); lineTo(7f, 6f); lineTo(12f, 11f); verticalLineTo(7f); curveTo(15.31f, 7f, 18f, 9.69f, 18f, 13f); reflectiveCurveTo(15.31f, 19f, 12f, 19f); reflectiveCurveTo(6f, 16.31f, 6f, 13f); horizontalLineTo(4f); curveTo(4f, 17.42f, 7.58f, 21f, 12f, 21f); reflectiveCurveTo(20f, 17.42f, 20f, 13f); reflectiveCurveTo(16.42f, 5f, 12f, 5f); close()
        moveTo(10.89f, 16f); horizontalLineTo(9.89f); verticalLineTo(12.26f); lineTo(8.8f, 12.6f); verticalLineTo(11.77f); lineTo(10.79f, 11.06f); horizontalLineTo(10.89f); close()
        moveTo(15.17f, 14.07f); curveTo(15.17f, 15.27f, 14.54f, 16.07f, 13.46f, 16.07f); reflectiveCurveTo(11.75f, 15.27f, 11.75f, 14.07f); verticalLineTo(13.06f); curveTo(11.75f, 11.86f, 12.38f, 11.06f, 13.46f, 11.06f); reflectiveCurveTo(15.17f, 11.86f, 15.17f, 13.06f); close()
        moveTo(14.17f, 12.93f); curveTo(14.17f, 12.22f, 13.88f, 11.86f, 13.46f, 11.86f); reflectiveCurveTo(12.75f, 12.22f, 12.75f, 12.93f); verticalLineTo(14.2f); curveTo(12.75f, 14.91f, 13.04f, 15.27f, 13.46f, 15.27f); reflectiveCurveTo(14.17f, 14.91f, 14.17f, 14.2f); close()
    }.also { _replay10 = it }

private var _fullscreen: ImageVector? = null
val Icons.Rounded.Fullscreen: ImageVector
    get() = _fullscreen ?: materialIcon("Fullscreen") {
        moveTo(7f, 14f); horizontalLineTo(5f); verticalLineTo(19f); horizontalLineTo(10f); verticalLineTo(17f); horizontalLineTo(7f); close()
        moveTo(5f, 10f); horizontalLineTo(7f); verticalLineTo(7f); horizontalLineTo(10f); verticalLineTo(5f); horizontalLineTo(5f); close()
        moveTo(17f, 17f); horizontalLineTo(14f); verticalLineTo(19f); horizontalLineTo(19f); verticalLineTo(14f); horizontalLineTo(17f); close()
        moveTo(14f, 5f); verticalLineTo(7f); horizontalLineTo(17f); verticalLineTo(10f); horizontalLineTo(19f); verticalLineTo(5f); close()
    }.also { _fullscreen = it }

private var _fullscreenExit: ImageVector? = null
val Icons.Rounded.FullscreenExit: ImageVector
    get() = _fullscreenExit ?: materialIcon("FullscreenExit") {
        moveTo(5f, 16f); horizontalLineTo(8f); verticalLineTo(19f); horizontalLineTo(10f); verticalLineTo(14f); horizontalLineTo(5f); close()
        moveTo(8f, 8f); horizontalLineTo(5f); verticalLineTo(10f); horizontalLineTo(10f); verticalLineTo(5f); horizontalLineTo(8f); close()
        moveTo(14f, 19f); horizontalLineTo(16f); verticalLineTo(16f); horizontalLineTo(19f); verticalLineTo(14f); horizontalLineTo(14f); close()
        moveTo(16f, 8f); verticalLineTo(5f); horizontalLineTo(14f); verticalLineTo(10f); horizontalLineTo(19f); verticalLineTo(8f); close()
    }.also { _fullscreenExit = it }

private var _gridView: ImageVector? = null
val Icons.Rounded.GridView: ImageVector
    get() = _gridView ?: materialIcon("GridView") {
        moveTo(5f, 3f); horizontalLineTo(9f); curveTo(10.1f, 3f, 11f, 3.9f, 11f, 5f); verticalLineTo(9f); curveTo(11f, 10.1f, 10.1f, 11f, 9f, 11f); horizontalLineTo(5f); curveTo(3.9f, 11f, 3f, 10.1f, 3f, 9f); verticalLineTo(5f); curveTo(3f, 3.9f, 3.9f, 3f, 5f, 3f); close()
        moveTo(5f, 13f); horizontalLineTo(9f); curveTo(10.1f, 13f, 11f, 13.9f, 11f, 15f); verticalLineTo(19f); curveTo(11f, 20.1f, 10.1f, 21f, 9f, 21f); horizontalLineTo(5f); curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f); verticalLineTo(15f); curveTo(3f, 13.9f, 3.9f, 13f, 5f, 13f); close()
        moveTo(15f, 3f); horizontalLineTo(19f); curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f); verticalLineTo(9f); curveTo(21f, 10.1f, 20.1f, 11f, 19f, 11f); horizontalLineTo(15f); curveTo(13.9f, 11f, 13f, 10.1f, 13f, 9f); verticalLineTo(5f); curveTo(13f, 3.9f, 13.9f, 3f, 15f, 3f); close()
        moveTo(15f, 13f); horizontalLineTo(19f); curveTo(20.1f, 13f, 21f, 13.9f, 21f, 15f); verticalLineTo(19f); curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f); horizontalLineTo(15f); curveTo(13.9f, 21f, 13f, 20.1f, 13f, 19f); verticalLineTo(15f); curveTo(13f, 13.9f, 13.9f, 13f, 15f, 13f); close()
    }.also { _gridView = it }

private var _highQuality: ImageVector? = null
val Icons.Rounded.HighQuality: ImageVector
    get() = _highQuality ?: materialIcon("HighQuality") {
        moveTo(19f, 4f); horizontalLineTo(5f); curveTo(3.89f, 4f, 3f, 4.9f, 3f, 6f); verticalLineTo(18f); curveTo(3f, 19.1f, 3.89f, 20f, 5f, 20f); horizontalLineTo(19f); curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f); verticalLineTo(6f); curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f); close()
        moveTo(11f, 15f); horizontalLineTo(9.5f); verticalLineTo(13f); horizontalLineTo(7.5f); verticalLineTo(15f); horizontalLineTo(6f); verticalLineTo(9f); horizontalLineTo(7.5f); verticalLineTo(11.5f); horizontalLineTo(9.5f); verticalLineTo(9f); horizontalLineTo(11f); close()
        moveTo(18f, 14f); curveTo(18f, 14.55f, 17.55f, 15f, 17f, 15f); horizontalLineTo(16.25f); verticalLineTo(16.5f); horizontalLineTo(14.75f); verticalLineTo(15f); horizontalLineTo(14f); curveTo(13.45f, 15f, 13f, 14.55f, 13f, 14f); verticalLineTo(10f); curveTo(13f, 9.45f, 13.45f, 9f, 14f, 9f); horizontalLineTo(17f); curveTo(17.55f, 9f, 18f, 9.45f, 18f, 10f); close()
        moveTo(14.5f, 13.5f); horizontalLineTo(16.5f); verticalLineTo(10.5f); horizontalLineTo(14.5f); close()
    }.also { _highQuality = it }

private var _localMovies: ImageVector? = null
val Icons.Rounded.LocalMovies: ImageVector
    get() = _localMovies ?: materialIcon("LocalMovies") {
        moveTo(18f, 3f); verticalLineTo(5f); horizontalLineTo(16f); verticalLineTo(3f); horizontalLineTo(8f); verticalLineTo(5f); horizontalLineTo(6f); verticalLineTo(3f); horizontalLineTo(4f); verticalLineTo(21f); horizontalLineTo(6f); verticalLineTo(19f); horizontalLineTo(8f); verticalLineTo(21f); horizontalLineTo(16f); verticalLineTo(19f); horizontalLineTo(18f); verticalLineTo(21f); horizontalLineTo(20f); verticalLineTo(3f); close()
        moveTo(8f, 17f); horizontalLineTo(6f); verticalLineTo(15f); horizontalLineTo(8f); close()
        moveTo(8f, 13f); horizontalLineTo(6f); verticalLineTo(11f); horizontalLineTo(8f); close()
        moveTo(8f, 9f); horizontalLineTo(6f); verticalLineTo(7f); horizontalLineTo(8f); close()
        moveTo(18f, 17f); horizontalLineTo(16f); verticalLineTo(15f); horizontalLineTo(18f); close()
        moveTo(18f, 13f); horizontalLineTo(16f); verticalLineTo(11f); horizontalLineTo(18f); close()
        moveTo(18f, 9f); horizontalLineTo(16f); verticalLineTo(7f); horizontalLineTo(18f); close()
    }.also { _localMovies = it }

private var _notificationsActive: ImageVector? = null
val Icons.Rounded.NotificationsActive: ImageVector
    get() = _notificationsActive ?: materialIcon("NotificationsActive") {
        moveTo(7.58f, 4.08f); lineTo(6.15f, 2.65f); curveTo(3.75f, 4.48f, 2.17f, 7.3f, 2.03f, 10.5f); horizontalLineTo(4.03f); curveTo(4.18f, 7.85f, 5.54f, 5.53f, 7.58f, 4.08f); close()
        moveTo(19.97f, 10.5f); horizontalLineTo(21.97f); curveTo(21.82f, 7.3f, 20.24f, 4.48f, 17.85f, 2.65f); lineTo(16.43f, 4.08f); curveTo(18.45f, 5.53f, 19.82f, 7.85f, 19.97f, 10.5f); close()
        moveTo(18f, 11f); curveTo(18f, 7.93f, 16.36f, 5.36f, 13.5f, 4.68f); verticalLineTo(4f); curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f); reflectiveCurveTo(10.5f, 3.17f, 10.5f, 4f); verticalLineTo(4.68f); curveTo(7.63f, 5.36f, 6f, 7.92f, 6f, 11f); verticalLineTo(16f); lineTo(4f, 18f); verticalLineTo(19f); horizontalLineTo(20f); verticalLineTo(18f); lineTo(18f, 16f); close()
        moveTo(12f, 22f); curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f); horizontalLineTo(10f); curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f); close()
    }.also { _notificationsActive = it }

private var _notificationsNone: ImageVector? = null
val Icons.Rounded.NotificationsNone: ImageVector
    get() = _notificationsNone ?: materialIcon("NotificationsNone") {
        moveTo(12f, 22f); curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f); horizontalLineTo(10f); curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f); close()
        moveTo(18f, 16f); verticalLineTo(11f); curveTo(18f, 7.93f, 16.37f, 5.36f, 13.5f, 4.68f); verticalLineTo(4f); curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f); reflectiveCurveTo(10.5f, 3.17f, 10.5f, 4f); verticalLineTo(4.68f); curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f); verticalLineTo(16f); lineTo(4f, 18f); verticalLineTo(19f); horizontalLineTo(20f); verticalLineTo(18f); close()
        moveTo(16f, 17f); horizontalLineTo(8f); verticalLineTo(11f); curveTo(8f, 8.52f, 9.51f, 6.5f, 12f, 6.5f); reflectiveCurveTo(16f, 8.52f, 16f, 11f); close()
    }.also { _notificationsNone = it }

private var _pictureInPictureAlt: ImageVector? = null
val Icons.Rounded.PictureInPictureAlt: ImageVector
    get() = _pictureInPictureAlt ?: materialIcon("PictureInPictureAlt") {
        moveTo(19f, 11f); horizontalLineTo(11f); verticalLineTo(17f); horizontalLineTo(19f); close()
        moveTo(23f, 19f); verticalLineTo(4.98f); curveTo(23f, 3.88f, 22.1f, 3f, 21f, 3f); horizontalLineTo(3f); curveTo(1.9f, 3f, 1f, 3.88f, 1f, 4.98f); verticalLineTo(19f); curveTo(1f, 20.1f, 1.9f, 21f, 3f, 21f); horizontalLineTo(21f); curveTo(22.1f, 21f, 23f, 20.1f, 23f, 19f); close()
        moveTo(21f, 19.02f); horizontalLineTo(3f); verticalLineTo(4.97f); horizontalLineTo(21f); close()
    }.also { _pictureInPictureAlt = it }

private var _shuffle: ImageVector? = null
val Icons.Rounded.Shuffle: ImageVector
    get() = _shuffle ?: materialIcon("Shuffle") {
        moveTo(10.59f, 9.17f); lineTo(5.41f, 4f); lineTo(4f, 5.41f); lineTo(9.17f, 10.58f); close()
        moveTo(14.5f, 4f); lineTo(16.54f, 6.04f); lineTo(4f, 18.59f); lineTo(5.41f, 20f); lineTo(17.96f, 7.46f); lineTo(20f, 9.5f); verticalLineTo(4f); close()
        moveTo(14.83f, 13.41f); lineTo(13.42f, 14.82f); lineTo(16.55f, 17.95f); lineTo(14.5f, 20f); horizontalLineTo(20f); verticalLineTo(14.5f); lineTo(17.96f, 16.54f); close()
    }.also { _shuffle = it }

private var _sportsSoccer: ImageVector? = null
val Icons.Rounded.SportsSoccer: ImageVector
    get() = _sportsSoccer ?: materialIcon("SportsSoccer") {
        moveTo(12f, 2f); curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f); reflectiveCurveTo(6.48f, 22f, 12f, 22f); reflectiveCurveTo(22f, 17.52f, 22f, 12f); reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
        moveTo(13f, 5.3f); lineTo(14.35f, 4.35f); curveTo(16.17f, 4.91f, 17.71f, 6.12f, 18.69f, 7.72f); lineTo(18.08f, 9.13f); lineTo(16.59f, 9.63f); lineTo(13f, 7.03f); close()
        moveTo(9.65f, 4.35f); lineTo(11f, 5.3f); verticalLineTo(7.03f); lineTo(7.41f, 9.63f); lineTo(5.92f, 9.13f); lineTo(5.31f, 7.72f); curveTo(6.29f, 6.12f, 7.83f, 4.91f, 9.65f, 4.35f); close()
        moveTo(7.08f, 17.11f); lineTo(5.94f, 17.21f); curveTo(4.73f, 15.81f, 4f, 13.99f, 4f, 12f); curveTo(4f, 11.88f, 4.01f, 11.77f, 4.02f, 11.65f); lineTo(5.04f, 10.92f); lineTo(6.65f, 11.46f); lineTo(8.02f, 15.65f); close()
        moveTo(14.5f, 19.59f); curveTo(13.71f, 19.85f, 12.87f, 20f, 12f, 20f); reflectiveCurveTo(10.29f, 19.85f, 9.5f, 19.59f); lineTo(8.88f, 18.1f); lineTo(9.54f, 17f); horizontalLineTo(14.46f); lineTo(15.12f, 18.1f); close()
        moveTo(14.27f, 15f); horizontalLineTo(9.73f); lineTo(8.38f, 10.85f); lineTo(12f, 8.23f); lineTo(15.62f, 10.85f); close()
        moveTo(18.06f, 17.21f); lineTo(16.92f, 17.11f); lineTo(15.98f, 15.65f); lineTo(17.35f, 11.46f); lineTo(18.96f, 10.92f); lineTo(19.98f, 11.65f); curveTo(19.99f, 11.77f, 20f, 11.88f, 20f, 12f); curveTo(20f, 13.99f, 19.27f, 15.81f, 18.06f, 17.21f); close()
    }.also { _sportsSoccer = it }

private var _tune: ImageVector? = null
val Icons.Rounded.Tune: ImageVector
    get() = _tune ?: materialIcon("Tune") {
        moveTo(3f, 17f); verticalLineTo(19f); horizontalLineTo(9f); verticalLineTo(17f); close()
        moveTo(3f, 5f); verticalLineTo(7f); horizontalLineTo(13f); verticalLineTo(5f); close()
        moveTo(13f, 21f); verticalLineTo(19f); horizontalLineTo(21f); verticalLineTo(17f); horizontalLineTo(13f); verticalLineTo(15f); horizontalLineTo(11f); verticalLineTo(21f); close()
        moveTo(7f, 9f); verticalLineTo(11f); horizontalLineTo(3f); verticalLineTo(13f); horizontalLineTo(7f); verticalLineTo(15f); horizontalLineTo(9f); verticalLineTo(9f); close()
        moveTo(21f, 13f); verticalLineTo(11f); horizontalLineTo(11f); verticalLineTo(13f); close()
        moveTo(15f, 9f); horizontalLineTo(17f); verticalLineTo(7f); horizontalLineTo(21f); verticalLineTo(5f); horizontalLineTo(17f); verticalLineTo(3f); horizontalLineTo(15f); close()
    }.also { _tune = it }

private var _tv: ImageVector? = null
val Icons.Rounded.Tv: ImageVector
    get() = _tv ?: materialIcon("Tv") {
        moveTo(21f, 3f); horizontalLineTo(3f); curveTo(1.9f, 3f, 1f, 3.9f, 1f, 5f); verticalLineTo(17f); curveTo(1f, 18.1f, 1.9f, 19f, 3f, 19f); horizontalLineTo(8f); verticalLineTo(21f); horizontalLineTo(16f); verticalLineTo(19f); horizontalLineTo(21f); curveTo(22.1f, 19f, 23f, 18.1f, 23f, 17f); verticalLineTo(5f); curveTo(23f, 3.9f, 22.1f, 3f, 21f, 3f); close()
        moveTo(21f, 17f); horizontalLineTo(3f); verticalLineTo(5f); horizontalLineTo(21f); close()
    }.also { _tv = it }

private var _viewAgenda: ImageVector? = null
val Icons.Rounded.ViewAgenda: ImageVector
    get() = _viewAgenda ?: materialIcon("ViewAgenda") {
        moveTo(20f, 13f); horizontalLineTo(4f); curveTo(3.45f, 13f, 3f, 13.45f, 3f, 14f); verticalLineTo(20f); curveTo(3f, 20.55f, 3.45f, 21f, 4f, 21f); horizontalLineTo(20f); curveTo(20.55f, 21f, 21f, 20.55f, 21f, 20f); verticalLineTo(14f); curveTo(21f, 13.45f, 20.55f, 13f, 20f, 13f); close()
        moveTo(20f, 3f); horizontalLineTo(4f); curveTo(3.45f, 3f, 3f, 3.45f, 3f, 4f); verticalLineTo(10f); curveTo(3f, 10.55f, 3.45f, 11f, 4f, 11f); horizontalLineTo(20f); curveTo(20.55f, 11f, 21f, 10.55f, 21f, 10f); verticalLineTo(4f); curveTo(21f, 3.45f, 20.55f, 3f, 20f, 3f); close()
    }.also { _viewAgenda = it }

private var _brightnessHigh: ImageVector? = null
val Icons.Rounded.BrightnessHigh: ImageVector
    get() = _brightnessHigh ?: materialIcon("BrightnessHigh") {
        moveTo(20f, 8.69f); verticalLineTo(4f); horizontalLineTo(15.31f); lineTo(12f, 0.69f); lineTo(8.69f, 4f); horizontalLineTo(4f); verticalLineTo(8.69f); lineTo(0.69f, 12f); lineTo(4f, 15.31f); verticalLineTo(20f); horizontalLineTo(8.69f); lineTo(12f, 23.31f); lineTo(15.31f, 20f); horizontalLineTo(20f); verticalLineTo(15.31f); lineTo(23.31f, 12f); lineTo(20f, 8.69f); close()
        moveTo(12f, 18f); curveTo(8.69f, 18f, 6f, 15.31f, 6f, 12f); reflectiveCurveTo(8.69f, 6f, 12f, 6f); reflectiveCurveTo(18f, 8.69f, 18f, 12f); reflectiveCurveTo(15.31f, 18f, 12f, 18f); close()
        moveTo(12f, 8f); curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f); reflectiveCurveTo(9.79f, 16f, 12f, 16f); reflectiveCurveTo(16f, 14.21f, 16f, 12f); reflectiveCurveTo(14.21f, 8f, 12f, 8f); close()
    }.also { _brightnessHigh = it }

private var _translate: ImageVector? = null
val Icons.Rounded.Translate: ImageVector
    get() = _translate ?: materialIcon("Translate") {
        moveTo(12.87f, 15.07f); lineTo(10.33f, 12.56f); lineTo(10.36f, 12.53f); curveTo(12.1f, 10.59f, 13.34f, 8.36f, 14.07f, 6f); horizontalLineTo(17f); verticalLineTo(4f); horizontalLineTo(10f); verticalLineTo(2f); horizontalLineTo(8f); verticalLineTo(4f); horizontalLineTo(1f); verticalLineTo(5.99f); horizontalLineTo(12.17f); curveTo(11.5f, 7.92f, 10.44f, 9.75f, 9f, 11.35f); curveTo(8.07f, 10.32f, 7.3f, 9.19f, 6.69f, 8f); horizontalLineTo(4.69f); curveTo(5.42f, 9.63f, 6.42f, 11.17f, 7.67f, 12.56f); lineTo(2.58f, 17.58f); lineTo(4f, 19f); lineTo(9f, 14f); lineTo(12.11f, 17.11f); lineTo(12.87f, 15.07f); close()
        moveTo(18.5f, 10f); horizontalLineTo(16.5f); lineTo(12f, 22f); horizontalLineTo(14f); lineTo(15.12f, 19f); horizontalLineTo(19.87f); lineTo(21f, 22f); horizontalLineTo(23f); lineTo(18.5f, 10f); close()
        moveTo(15.88f, 17f); lineTo(17.5f, 12.67f); lineTo(19.12f, 17f); horizontalLineTo(15.88f); close()
    }.also { _translate = it }

private var _lockOpen: ImageVector? = null
val Icons.Rounded.LockOpen: ImageVector
    get() = _lockOpen ?: materialIcon("LockOpen") {
        moveTo(18f, 8f); horizontalLineTo(17f); verticalLineTo(6f); curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f); curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f); horizontalLineTo(9f); curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f); reflectiveCurveTo(15f, 4.34f, 15f, 6f); verticalLineTo(8f); horizontalLineTo(6f); curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f); verticalLineTo(20f); curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f); horizontalLineTo(18f); curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f); verticalLineTo(10f); curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f); close()
        moveTo(18f, 20f); horizontalLineTo(6f); verticalLineTo(10f); horizontalLineTo(18f); verticalLineTo(20f); close()
        moveTo(12f, 17f); curveTo(13.1f, 17f, 14f, 16.1f, 14f, 15f); reflectiveCurveTo(13.1f, 13f, 12f, 13f); reflectiveCurveTo(10f, 13.9f, 10f, 15f); reflectiveCurveTo(10.9f, 17f, 12f, 17f); close()
    }.also { _lockOpen = it }

private var _aspectRatio: ImageVector? = null
val Icons.Rounded.AspectRatio: ImageVector
    get() = _aspectRatio ?: materialIcon("AspectRatio") {
        moveTo(19f, 12f); horizontalLineTo(17f); verticalLineTo(15f); horizontalLineTo(14f); verticalLineTo(17f); horizontalLineTo(19f); verticalLineTo(12f); close()
        moveTo(7f, 9f); horizontalLineTo(10f); verticalLineTo(7f); horizontalLineTo(5f); verticalLineTo(12f); horizontalLineTo(7f); verticalLineTo(9f); close()
        moveTo(21f, 3f); horizontalLineTo(3f); curveTo(1.9f, 3f, 1f, 3.9f, 1f, 5f); verticalLineTo(19f); curveTo(1f, 20.1f, 1.9f, 21f, 3f, 21f); horizontalLineTo(21f); curveTo(22.1f, 21f, 23f, 20.1f, 23f, 19f); verticalLineTo(5f); curveTo(23f, 3.9f, 22.1f, 3f, 21f, 3f); close()
        moveTo(21f, 19.01f); horizontalLineTo(3f); verticalLineTo(4.99f); horizontalLineTo(21f); verticalLineTo(19.01f); close()
    }.also { _aspectRatio = it }

private var _skipNext: ImageVector? = null
val Icons.Rounded.SkipNext: ImageVector
    get() = _skipNext ?: materialIcon("SkipNext") {
        moveTo(6f, 18f); lineTo(14.5f, 12f); lineTo(6f, 6f); verticalLineTo(18f); close()
        moveTo(16f, 6f); verticalLineTo(18f); horizontalLineTo(18f); verticalLineTo(6f); horizontalLineTo(16f); close()
    }.also { _skipNext = it }

private var _explore: ImageVector? = null
val Icons.Rounded.Explore: ImageVector
    get() = _explore ?: materialIcon("Explore") {
        moveTo(12f, 10.9f); curveTo(11.39f, 10.9f, 10.9f, 11.39f, 10.9f, 12f); curveTo(10.9f, 12.61f, 11.39f, 13.1f, 12f, 13.1f); curveTo(12.61f, 13.1f, 13.1f, 12.61f, 13.1f, 12f); curveTo(13.1f, 11.39f, 12.61f, 10.9f, 12f, 10.9f); close()
        moveTo(12f, 2f); curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f); reflectiveCurveTo(6.48f, 22f, 12f, 22f); reflectiveCurveTo(22f, 17.52f, 22f, 12f); reflectiveCurveTo(17.52f, 2f, 12f, 2f); close()
        moveTo(14.19f, 14.19f); lineTo(6f, 18f); lineTo(9.81f, 9.81f); lineTo(18f, 6f); lineTo(14.19f, 14.19f); close()
    }.also { _explore = it }

val Icons.Outlined.Explore: ImageVector
    get() = Icons.Rounded.Explore

private var _history: ImageVector? = null
val Icons.Rounded.History: ImageVector
    get() = _history ?: materialIcon("History") {
        moveTo(13f, 3f); curveTo(8.03f, 3f, 4f, 7.03f, 4f, 12f); horizontalLineTo(1f); lineTo(4.89f, 15.89f); lineTo(4.96f, 16.03f); lineTo(9f, 12f); horizontalLineTo(6f); curveTo(6f, 8.13f, 9.13f, 5f, 13f, 5f); reflectiveCurveTo(20f, 8.13f, 20f, 12f); reflectiveCurveTo(16.87f, 19f, 13f, 19f); curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f); lineTo(6.64f, 18.36f); curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f); curveTo(17.97f, 21f, 22f, 16.97f, 22f, 12f); reflectiveCurveTo(17.97f, 3f, 13f, 3f); close()
        moveTo(12f, 8f); verticalLineTo(13f); lineTo(16.28f, 15.54f); lineTo(17f, 14.33f); lineTo(13.5f, 12.25f); verticalLineTo(8f); horizontalLineTo(12f); close()
    }.also { _history = it }


