# The app uses reflection only for Gson DTOs; keep their fields and the no-arg constructor
# path (data classes with default values) intact when R8 is enabled.
-keepclassmembers,allowobfuscation class com.radiomaster.app.data.remote.dto.** {
    <fields>;
    *** get*();
    void set*(***);
    <init>(...);
}
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*,RuntimeVisibleAnnotations

# Gson internals (TypeToken subclasses are created by Retrofit's converter factory).
-dontwarn sun.misc.**
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.** { *; }

# Room generates code against the entities; keep accessors reachable from generated impls.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Media3 uses reflection to create optional MediaSource factories (e.g. the HLS module).
-keep class androidx.media3.exoplayer.hls.HlsMediaSource$Factory { *; }
-keep class androidx.media3.datasource.okhttp.OkHttpDataSource$Factory { *; }

# OkHttp / Retrofit ship their own consumer rules; these silence optional dependencies.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
