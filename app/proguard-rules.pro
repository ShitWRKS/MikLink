# ProGuard rules for MikLink
# ===========================

# Keep DTOs used by Moshi (backup - @JsonClass should handle this)
-keep class com.app.miklink.data.remote.mikrotik.dto.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory { *; }

# iText's connector optionally loads the non-Android Bouncy Castle adapter.
# MikLink does not use cryptographic PDF operations; Android PDF generation is device-tested.
-dontwarn com.itextpdf.bouncycastle.BouncyCastleFactory

# Keep BuildConfig for runtime checks
-keep class com.app.miklink.BuildConfig { *; }
