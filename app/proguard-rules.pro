# Muvio Release R8 / ProGuard optimization rules

# Keep app data models & BuildConfig fields safe under R8 shrinking
-keep class com.example.model.** { *; }
-keep class com.example.BuildConfig { *; }

# Strip debug Log calls in release builds for smaller & faster bytecode
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# OkHttp / Okio optional platform classes
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Media3 / ExoPlayer HLS & UI PlayerView reflection safety
-keep class androidx.media3.exoplayer.hls.** { *; }
-keep class androidx.media3.ui.PlayerView { *; }
-dontwarn androidx.media3.**
