# Muvio Release R8 / ProGuard optimization rules (maximum safe APK size reduction)

# Repackage obfuscated classes into flat root package & allow aggressive R8 optimizations
-repackageclasses ''
-allowaccessmodification

# Keep @Keep annotated Firestore DTOs & BuildConfig fields safe under R8 shrinking
-keep @androidx.annotation.Keep class * { *; }
-keep class com.example.BuildConfig { *; }

# Strip debug/info/warn Log calls in release builds for smaller & faster bytecode
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

# Media3 / ExoPlayer reflection-instantiated HLS factory & PlayerView only (shrink unused internals)
-keep class androidx.media3.exoplayer.hls.HlsMediaSource$Factory {
    public <init>(androidx.media3.datasource.DataSource$Factory);
}
-keep class androidx.media3.ui.PlayerView { *; }
-dontwarn androidx.media3.**
