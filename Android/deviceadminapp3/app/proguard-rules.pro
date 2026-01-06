# ProGuard rules for DeviceAdmin App

# Keep Retrofit classes
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# Keep API models
-keep class com.deviceadmin.app.data.model.** { *; }
-keep interface com.deviceadmin.app.data.remote.** { *; }

# Keep Gson
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker

# App Components (Manifest)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.accessibilityservice.AccessibilityService

# Keep DeviceAdminReceiver
-keep public class * extends android.app.admin.DeviceAdminReceiver

# Coroutines Debug Metadata
-keepnames class kotlinx.coroutines.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
