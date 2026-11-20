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
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.text.SubtitleParser
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

/**
 * Next-Gen Media3 1.5.1 Cinema Player Engine for Muvio (Source 1, Source 2, Shorts & Live):
 * - Official Media3 OkHttpDataSource (HTTP/2 pooled) for HLS (.m3u8) & DASH (.mpd) streams
 * - RangeContinuableHttpDataSource + ProgressiveMediaSource (CBR seeking enabled) for MP4/MKV/proxied VOD
 * - Chunkless HLS preparation + 800 Kbps fast-start adaptive bitrate estimation for instant first frame
 * - Hardware-first MediaCodec rendering with automatic software decoder fallback
 * - Fast 0.6s startup + 2s quick rebuffer recovery + 15s..50s smooth anti-stutter buffering
 * - Resilient 4-retry error handling policy + instant keyframe-snapped seeking
 */
@OptIn(UnstableApi::class)
object MuvioExoPlayerFactory {

    private val mediaAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    private val resilientLoadErrorPolicy = DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 4)

    private val sharedStreamingOkHttp: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 20
        }
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
            .connectTimeout(18, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun createRenderersFactory(context: Context): DefaultRenderersFactory =
        DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true)

    private fun buildOfficialOkHttpDataSourceFactory(headers: Map<String, String>): DataSource.Factory {
        val cleanHeaders = linkedMapOf<String, String>()
        headers.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) cleanHeaders[k.trim()] = v.trim()
        }
        val customUa = cleanHeaders.entries
            .firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }
            ?.value
            ?: RangeContinuableHttpDataSource.DEFAULT_USER_AGENT
        val filteredHeaders = cleanHeaders.filterKeys { !it.equals("User-Agent", ignoreCase = true) }

        return OkHttpDataSource.Factory(sharedStreamingOkHttp)
            .setUserAgent(customUa)
            .setDefaultRequestProperties(filteredHeaders)
    }

    private fun createHybridMediaSourceFactory(
        context: Context,
        headers: Map<String, String>,
        connectMs: Int,
        readMs: Int,
        subtitleParserFactory: SubtitleParser.Factory? = null
    ): MediaSource.Factory {
        // 1. Official Media3 OkHttpDataSource for HLS (.m3u8), DASH (.mpd), and merged subtitle tracks
        val officialHttpFactory = buildOfficialOkHttpDataSourceFactory(headers)
        val officialDataSourceFactory = DefaultDataSource.Factory(context, officialHttpFactory)

        // 2. Range-continuable DataSource for progressive MP4/MKV/proxy streams that cap 206 chunk sizes
        val rangeHttpFactory = RangeContinuableHttpDataSource.Factory(
            headers = headers,
            connectTimeoutMs = connectMs,
            readTimeoutMs = readMs
        )
        val progressiveDataSourceFactory = DefaultDataSource.Factory(context, rangeHttpFactory)

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        val defaultFactory = DefaultMediaSourceFactory(progressiveDataSourceFactory, extractorsFactory)
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)
            .apply {
                if (subtitleParserFactory != null) {
                    setSubtitleParserFactory(subtitleParserFactory)
                }
            }

        val hlsWithSubsFactory = DefaultMediaSourceFactory(officialDataSourceFactory, extractorsFactory)
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)
            .apply {
                if (subtitleParserFactory != null) {
                    setSubtitleParserFactory(subtitleParserFactory)
                }
            }

        val fastHlsFactory = HlsMediaSource.Factory(officialDataSourceFactory)
            .setAllowChunklessPreparation(true)
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)

        val dashFactory = DashMediaSource.Factory(officialDataSourceFactory)
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)

        val progressiveFactory = ProgressiveMediaSource.Factory(progressiveDataSourceFactory, extractorsFactory)
            .setLoadErrorHandlingPolicy(resilientLoadErrorPolicy)

        return object : MediaSource.Factory {
            override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
                defaultFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                hlsWithSubsFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                fastHlsFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                dashFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                progressiveFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                return this
            }

            override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
                defaultFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                hlsWithSubsFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                fastHlsFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                dashFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                progressiveFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                return this
            }

            override fun getSupportedTypes(): IntArray = defaultFactory.supportedTypes

            override fun createMediaSource(mediaItem: MediaItem): MediaSource {
                val localConfig = mediaItem.localConfiguration
                val uriStr = localConfig?.uri?.toString().orEmpty()
                val mime = localConfig?.mimeType
                val isHls = mime == MimeTypes.APPLICATION_M3U8 ||
                    uriStr.contains(".m3u8", ignoreCase = true) ||
                    uriStr.contains("/api/hls/", ignoreCase = true)
                val isDash = mime == MimeTypes.APPLICATION_MPD ||
                    uriStr.contains(".mpd", ignoreCase = true)
                val hasExternalSubs = localConfig?.subtitleConfigurations?.isNotEmpty() == true

                return when {
                    isHls && !hasExternalSubs -> fastHlsFactory.createMediaSource(mediaItem)
                    isHls && hasExternalSubs -> hlsWithSubsFactory.createMediaSource(mediaItem)
                    isDash && !hasExternalSubs -> dashFactory.createMediaSource(mediaItem)
                    isDash && hasExternalSubs -> hlsWithSubsFactory.createMediaSource(mediaItem)
                    !hasExternalSubs -> progressiveFactory.createMediaSource(mediaItem)
                    else -> defaultFactory.createMediaSource(mediaItem)
                }
            }
        }
    }

    fun createVodPlayer(
        context: Context,
        headers: Map<String, String> = emptyMap(),
        subtitleParserFactory: SubtitleParser.Factory? = null
    ): ExoPlayer {
        val mediaSourceFactory = createHybridMediaSourceFactory(
            context = context,
            headers = headers,
            connectMs = 18_000,
            readMs = 30_000,
            subtitleParserFactory = subtitleParserFactory
        )

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 18_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 600,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setBackBuffer(/* backBufferDurationMs = */ 15_000, /* retainBackBufferFromKeyframe = */ true)
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

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
        val mediaSourceFactory = createHybridMediaSourceFactory(
            context = context,
            headers = headers,
            connectMs = 15_000,
            readMs = 25_000
        )
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 8_000,
                /* maxBufferMs = */ 24_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_500
            )
            .setPrioritizeTimeOverSizeThresholds(false)
            .build()

        val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .setInitialBitrateEstimate(1_000_000L)
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
        val mediaSourceFactory = createHybridMediaSourceFactory(
            context = context,
            headers = headers,
            connectMs = 20_000,
            readMs = 30_000
        )
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 12_000,
                /* maxBufferMs = */ 35_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_500
            )
            .setPrioritizeTimeOverSizeThresholds(false)
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
            }
    }
}
