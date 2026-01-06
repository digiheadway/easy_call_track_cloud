# ProGuard rules for DeviceAdmin

# Retrofit & Gson
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class com.example.deviceadmin.StatusResponse { *; }
-keep interface com.example.deviceadmin.ApiService { *; }

# WorkManager
-keep class androidx.work.** { *; }

# App Components (ensure Manifest classes aren't renamed)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Application

# Aliases (referenced in Manifest)
-keep class com.example.deviceadmin.MainActivityAlias
-keep class com.example.deviceadmin.DownloadsAlias

# Coroutines (Debug metadata)
-keepnames class kotlinx.coroutines.**
