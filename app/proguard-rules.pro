# SquadPing ProGuard & R8 Optimization Rules

# 1. Keep domain models
-keep class com.example.model.** { *; }

# 2. Keep Audio & Foreground Service components
-keep class com.example.audio.VoiceForegroundService { *; }
-keep class com.example.audio.** { *; }

# 3. Strip debug logging for production performance
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 4. Room Database optimization
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# 5. Moshi & OkHttp optimizations
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-dontwarn okio.**
-dontwarn javax.annotation.**

# 6. Coroutines & Flow optimizations
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# 7. Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
