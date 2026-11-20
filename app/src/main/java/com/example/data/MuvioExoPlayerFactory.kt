package com.example.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.extractor.text.SubtitleParser

/**
 * Production-tuned Media3 ExoPlayer factory for ultra-smooth VOD (Source 1 & Source 2),
 * Shorts, and Live playback:
 * - Hardware-first decoders with automatic software decoder fallback for exotic codecs
 * - RangeContinuableHttpDataSource (OkHttp HTTP/2 pooled + lookahead chunk prefetching)
 * - Fast-start startup + deep anti-stutter forward/back buffering for variable mobile networks
 * - Resilient 4-retry load error handling + adaptive HLS bitrate switching
 * - Instant keyframe-snapped seeking (SeekParameters.CLOSEST_SYNC)
 */
@OptIn(UnstableApi::class)
object MuvioExoPlayerFactory {

    private val mediaAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    private val resilientLoadErrorPolicy = DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 4)

    private fun createRenderersFactory(context: Context): DefaultRenderersFactory =
        DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true)

    fun createVodPlayer(
        context: Context,
        headers: Map<String, String> = emptyMap(),
        subtitleParserFactory: SubtitleParser.Factory? = null
    ): ExoPlayer {
        val httpFactory = buildHttpDataSourceFactory(headers, connectMs = 18_000, readMs = 30_000)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)
            .apply {
                if (subtitleParserFactory != null) {
                    setSubtitleParserFactory(subtitleParserFactory)
                }
            }
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 25_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 1_200,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .setBackBuffer(/* backBufferDurationMs = */ 20_000, /* retainBackBufferFromKeyframe = */ true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .build()
        }

        return ExoPlayer.Builder(context, createRenderersFactory(context))
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
            .apply {
                setAudioAttributes(mediaAudioAttributes, /* handleAudioFocus = */ true)
                setHandleAudioBecomingNoisy(true)
                setSeekParameters(SeekParameters.CLOSEST_SYNC)
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
    }

    fun createShortsPlayer(
        context: Context,
        headers: Map<String, String> = emptyMap()
    ): ExoPlayer {
        val httpFactory = buildHttpDataSourceFactory(headers, connectMs = 15_000, readMs = 25_000)
        val mediaSourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory))
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 8_000,
                /* maxBufferMs = */ 24_000,
                /* bufferForPlaybackMs = */ 600,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .build()
        }

        return ExoPlayer.Builder(context, createRenderersFactory(context))
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                setAudioAttributes(mediaAudioAttributes, /* handleAudioFocus = */ true)
                setHandleAudioBecomingNoisy(true)
                setSeekParameters(SeekParameters.CLOSEST_SYNC)
            }
    }

    fun createLivePlayer(
        context: Context,
        headers: Map<String, String> = emptyMap()
    ): ExoPlayer {
        val httpFactory = buildHttpDataSourceFactory(headers, connectMs = 20_000, readMs = 30_000)
        val mediaSourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory))
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 1_200,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setAllowVideoMixedMimeTypeAdaptiveness(true)
                .setAllowAudioMixedMimeTypeAdaptiveness(true)
                .setAllowVideoNonSeamlessAdaptiveness(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .build()
        }

        return ExoPlayer.Builder(context, createRenderersFactory(context))
            .setMediaSourceFactory(mediaSourceFactory)
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
