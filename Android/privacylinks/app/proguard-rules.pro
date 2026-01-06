# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

# General
-keep class com.clicktoearn.linkbox.** { *; }


# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Facebook Audience Network
-dontwarn com.facebook.infer.annotation.**
-dontwarn com.facebook.ads.internal.**
-keep class com.facebook.ads.** { *; }
-keep class com.facebook.infer.annotation.** { *; }