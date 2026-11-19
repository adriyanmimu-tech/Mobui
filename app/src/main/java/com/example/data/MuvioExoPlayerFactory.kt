package com.example.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.text.SubtitleParser

/**
 * Production-tuned Media3 ExoPlayer factory for ultra-smooth VOD, Shorts, and Live playback.
 * Uses fast-start adaptive buffering, generous back-buffer for instant seek-back,
 * adaptive HLS bitrate switching, audio focus management, and network wake lock.
 */
@OptIn(UnstableApi::class)
object MuvioExoPlayerFactory {

    private const val CHROME_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    private val mediaAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    fun createVodPlayer(
        context: Context,
        headers: Map<String, String> = emptyMap(),
        subtitleParserFactory: SubtitleParser.Factory? = null
    ): ExoPlayer {
        val httpFactory = buildHttpDataSourceFactory(headers, connectMs = 15_000, readMs = 25_000)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory).apply {
            if (subtitleParserFactory != null) {
                setSubtitleParserFactory(subtitleParserFactory)
            }
        }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setBackBuffer(/* backBufferDurationMs = */ 15_000, /* retainBackBufferFromKeyframe = */ true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .build()
        }

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
            .apply {
                setAudioAttributes(mediaAudioAttributes, /* handleAudioFocus = */ true)
                setHandleAudioBecomingNoisy(true)
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    fun createShortsPlayer(context: Context): ExoPlayer {
        val httpFactory = buildHttpDataSourceFactory(emptyMap(), connectMs = 12_000, readMs = 20_000)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 6_000,
                /* maxBufferMs = */ 20_000,
                /* bufferForPlaybackMs = */ 600,
                /* bufferForPlaybackAfterRebufferMs = */ 1_200
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                setAudioAttributes(mediaAudioAttributes, /* handleAudioFocus = */ true)
                setHandleAudioBecomingNoisy(true)
            }
    }

    fun createLivePlayer(
        context: Context,
        headers: Map<String, String> = emptyMap()
    ): ExoPlayer {
        val httpFactory = buildHttpDataSourceFactory(headers, connectMs = 18_000, readMs = 25_000)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 8_000,
                /* maxBufferMs = */ 25_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .build()
        }

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                setAudioAttributes(mediaAudioAttributes, /* handleAudioFocus = */ true)
                setHandleAudioBecomingNoisy(true)
            }
    }

    private fun buildHttpDataSourceFactory(
        headers: Map<String, String>,
        connectMs: Int,
        readMs: Int
    ): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(connectMs)
            .setReadTimeoutMs(readMs)
            .setUserAgent(CHROME_UA)
            .apply {
                if (headers.isNotEmpty()) {
                    setDefaultRequestProperties(headers)
                }
            }
    }
}
