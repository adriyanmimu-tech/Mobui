# Muvio Release R8 / ProGuard rules — full Media3 / ExoPlayer & OkHttp release protection

# Keep @Keep annotated models & BuildConfig fields safe under R8 shrinking
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.BuildConfig { *; }

# Strip verbose/debug/info Log calls in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# OkHttp / Okio optional platform classes
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**

# Media3 / ExoPlayer full reflection-safe keep rules (prevents Release APK black-screen / extractor stripping)
-keep class androidx.media3.exoplayer.source.DefaultMediaSourceFactory { *; }
-keep class androidx.media3.exoplayer.source.ProgressiveMediaSource$Factory { *; }
-keep class androidx.media3.exoplayer.hls.HlsMediaSource$Factory { *; }
-keep class androidx.media3.exoplayer.dash.DashMediaSource$Factory { *; }
-keep class androidx.media3.datasource.okhttp.** { *; }
-keep class androidx.media3.extractor.DefaultExtractorsFactory { *; }
-keep class androidx.media3.extractor.mp4.** { *; }
-keep class androidx.media3.extractor.mkv.** { *; }
-keep class androidx.media3.extractor.ts.** { *; }
-keep class androidx.media3.extractor.flv.** { *; }
-keep class androidx.media3.extractor.ogg.** { *; }
-keep class androidx.media3.extractor.text.** { *; }
-keep class androidx.media3.exoplayer.video.** { *; }
-keep class androidx.media3.exoplayer.audio.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**
