# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== Samsung Health SDK =====
-keep class com.samsung.android.sdk.health.** { *; }
-keep interface com.samsung.android.sdk.health.** { *; }

# ===== Hilt / Dagger =====
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <fields>;
}
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <fields>;
}

# ===== Room Database =====
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface *

# ===== Retrofit / OkHttp =====
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Retrofit service interfaces with generic signatures (fixes ParameterizedType errors)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep all Retrofit API interfaces in this project
-keep interface com.samsung.android.health.sdk.sample.healthdiary.update.data.api.** { *; }
-keep interface com.samsung.android.health.sdk.sample.healthdiary.api.** { *; }

-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ===== Gson =====
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep TypeToken anonymous classes for Gson reflection (fixes ParameterizedType errors)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ===== Kotlinx Serialization =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclasseswithmembers class **$$serializer {
    *** childSerializers();
}

-keep,includedescriptorclasses class com.samsung.android.health.sdk.sample.healthdiary.**$$serializer { *; }
-keepclassmembers class com.samsung.android.health.sdk.sample.healthdiary.** {
    *** Companion;
}

# ===== API Models =====
-keep class com.samsung.android.health.sdk.sample.healthdiary.api.models.** { *; }
-keep class com.samsung.android.health.sdk.sample.healthdiary.wearable.model.** { *; }
-keep class com.samsung.android.health.sdk.sample.healthdiary.config.Yaml** { *; }
-keep class com.samsung.android.health.sdk.sample.healthdiary.update.data.model.** { *; }

# ===== SnakeYAML =====
-keep class org.yaml.snakeyaml.** { *; }
-dontwarn org.yaml.snakeyaml.**

# ===== Google Play Services Wearable =====
-keep class com.google.android.gms.wearable.** { *; }
-keep interface com.google.android.gms.wearable.** { *; }

# ===== Data Binding =====
-keep class androidx.databinding.** { *; }

# ===== WorkManager =====
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker

# ===== Compose =====
-keep class androidx.compose.** { *; }

# ===== Keep entity classes for Room =====
-keep class com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.** { *; }
-keep class com.samsung.android.health.sdk.sample.healthdiary.data.room.SensorDataEntity { *; }
-keep class com.samsung.android.health.sdk.sample.healthdiary.data.ingest.** { *; }
-keep class com.samsung.android.health.sdk.sample.healthdiary.workout.data.** { *; }