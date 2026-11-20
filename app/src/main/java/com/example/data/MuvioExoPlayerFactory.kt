package com.example.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.text.SubtitleParser

/**
 * Production-tuned Media3 ExoPlayer factory for ultra-smooth VOD (Source 1 & Source 2),
 * Shorts, and Live playback:
 * - Hardware-first decoders with automatic software decoder fallback for exotic codecs
 * - RangeContinuableHttpDataSource (OkHttp HTTP/2 pooled + early 25% lookahead chunk prefetching)
 * - Fast-start HLS chunkless preparation + 1.5 Mbps initial bitrate estimate for instant startup
 * - Deep anti-stutter buffering (35s min / 90s max / 1s start / 5s rebuffer cushion) for proxied streams
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

    private fun createSmartMediaSourceFactory(
        dataSourceFactory: DataSource.Factory,
        subtitleParserFactory: SubtitleParser.Factory? = null
    ): MediaSource.Factory {
        val defaultFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)
            .apply {
                if (subtitleParserFactory != null) {
                    setSubtitleParserFactory(subtitleParserFactory)
                }
            }
        val fastHlsFactory = HlsMediaSource.Factory(dataSourceFactory)
            .setAllowChunklessPreparation(true)
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)

        return object : MediaSource.Factory {
            override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
                defaultFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                fastHlsFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                return this
            }

            override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
                defaultFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                fastHlsFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                return this
            }

            override fun getSupportedTypes(): IntArray = defaultFactory.supportedTypes

            override fun createMediaSource(mediaItem: MediaItem): MediaSource {
                val localConfig = mediaItem.localConfiguration
                val uriStr = localConfig?.uri?.toString().orEmpty()
                val isHls = localConfig?.mimeType == MimeTypes.APPLICATION_M3U8 ||
                    uriStr.contains(".m3u8", ignoreCase = true) ||
                    uriStr.contains("/api/hls/", ignoreCase = true)
                val hasExternalSubs = localConfig?.subtitleConfigurations?.isNotEmpty() == true
                // Use DefaultMediaSourceFactory when external subtitles are attached so Media3 merges
                // subtitle tracks automatically; otherwise use fast chunkless HlsMediaSource directly.
                return if (isHls && !hasExternalSubs) {
                    fastHlsFactory.createMediaSource(mediaItem)
                } else {
                    defaultFactory.createMediaSource(mediaItem)
                }
            }
        }
    }

    fun createVodPlayer(
        context: Context,
        headers: Map<String, String> = emptyMap(),
        subtitleParserFactory: SubtitleParser.Factory? = null
    ): ExoPlayer {
        val httpFactory = buildHttpDataSourceFactory(headers, connectMs = 18_000, readMs = 30_000)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSourceFactory = createSmartMediaSourceFactory(dataSourceFactory, subtitleParserFactory)

        // Instant 0.6s startup + 2s quick rebuffer recovery + balanced 20s..50s forward buffer
        // so videos begin playing immediately without over-choking mobile data or Render proxies.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 20_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 600,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setBackBuffer(/* backBufferDurationMs = */ 15_000, /* retainBackBufferFromKeyframe = */ true)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        // Start adaptive HLS estimation at 800 Kbps so the very first segment downloads in <1s
        // for instant playback, then smoothly scales up to 720p/1080p as throughput is measured.
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(800_000L)
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
            .setBandwidthMeter(bandwidthMeter)
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
        val mediaSourceFactory = createSmartMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory))
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 8_000,
                /* maxBufferMs = */ 24_000,
                /* bufferForPlaybackMs = */ 600,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(1_200_000L)
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
            .setBandwidthMeter(bandwidthMeter)
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
        val mediaSourceFactory = createSmartMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory))
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 12_000,
                /* maxBufferMs = */ 35_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(1_500_000L)
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
            .setBandwidthMeter(bandwidthMeter)
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
