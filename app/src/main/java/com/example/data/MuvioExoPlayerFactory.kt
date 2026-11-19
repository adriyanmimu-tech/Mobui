package com.example.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.text.SubtitleParser

/**
 * Production-tuned Media3 ExoPlayer factory for ultra-smooth VOD, Shorts, and Live playback.
 * Uses RangeContinuableHttpDataSource (OkHttp HTTP/2 pooled + seamless server-capped 206 chunk
 * continuation), fast-start adaptive buffering, generous back-buffer for instant seek-back,
 * adaptive HLS bitrate switching, and audio focus management.
 */
@OptIn(UnstableApi::class)
object MuvioExoPlayerFactory {

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
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory)))
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
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory)))
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
    ): DataSource.Factory {
        return RangeContinuableHttpDataSource.Factory(
            headers = headers,
            connectTimeoutMs = connectMs,
            readTimeoutMs = readMs
        )
    }
}
