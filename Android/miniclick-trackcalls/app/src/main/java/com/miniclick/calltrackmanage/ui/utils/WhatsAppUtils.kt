package com.miniclick.calltrackmanage.ui.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.os.UserManager
import android.content.pm.LauncherApps
import com.miniclick.calltrackmanage.ui.settings.AppInfo

object WhatsAppUtils {

    fun fetchAvailableWhatsappApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val apps = mutableListOf<AppInfo>()

        // Known WhatsApp package names (including common clones)
        val knownWhatsappPackages = listOf(
            "com.whatsapp",                    // Regular WhatsApp
            "com.whatsapp.w4b",                // WhatsApp Business
            "com.whatsapp.w4b.clone",          // Cloned Business
            "com.whatsapp.clone",              // Cloned WhatsApp
            "com.gbwhatsapp",                  // GB WhatsApp
            "com.whatsapp1",                   // Dual WhatsApp
            "com.whatsapp2",                   // Dual WhatsApp 2
            "com.dual.whatsapp",               // Dual Space WhatsApp
            "com.parallel.space.pro",          // Parallel Space
            "com.lbe.parallel.intl",           // Parallel Space International
            "com.ludashi.dualspace",           // Dual Space
            // OnePlus Parallel Apps patterns
            "com.oneplus.clone.whatsapp",
            "com.oneplus.clone.com.whatsapp",
            "com.oneplus.clone.com.whatsapp.w4b",
            // Xiaomi Second Space / Dual Apps
            "com.miui.securitycore.whatsapp",
            "com.miui.clone.whatsapp",
            // Samsung Dual Messenger
            "com.samsung.android.game.cloudgame.whatsapp",
            // Huawei App Twin
            "com.huawei.clone.whatsapp"
        )

        // Method 1: Query by intent to see what handles wa.me
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/")
        }

        try {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                .forEach { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    // Filter out browsers (like Chrome/Samsung Browser) that catch generic wa.me links
                    if (packageName.contains("whatsapp", ignoreCase = true) || 
                        packageName.contains("com.whatsapp", ignoreCase = true)) {
                        val label = resolveInfo.loadLabel(packageManager).toString()
                        apps.add(AppInfo(label, packageName))
                    }
                }
        } catch (e: Exception) { e.printStackTrace() }

        // Method 2: Check known package names directly
        knownWhatsappPackages.forEach { packageName ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val label = packageManager.getApplicationLabel(appInfo).toString()
                if (apps.none { it.packageName == packageName }) {
                    apps.add(AppInfo(label, packageName))
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // Not installed
            }
        }

        // Method 3: Search ALL installed apps for WhatsApp (catches OnePlus/Xiaomi clones)
        try {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(
                PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES
            ).filter { 
                it.packageName.contains("whatsapp", ignoreCase = true) ||
                (it.packageName.contains("clone", ignoreCase = true) && 
                 packageManager.getApplicationLabel(it).toString().contains("whatsapp", ignoreCase = true))
            }.forEach { appInfo ->
                if (apps.none { it.packageName == appInfo.packageName }) {
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    apps.add(AppInfo(label, appInfo.packageName))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Method 4: Check for apps in work profile / managed profiles
        try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            
            userManager?.userProfiles?.forEach { userHandle ->
                listOf("com.whatsapp", "com.whatsapp.w4b").forEach { pkg ->
                    try {
                        launcherApps?.getActivityList(pkg, userHandle)?.forEach { launcherActivity ->
                            val label = launcherActivity.label.toString()
                            val suffix = if (userHandle != Process.myUserHandle()) " (Clone)" else ""
                            val identifier = if (userHandle == Process.myUserHandle()) pkg else "$pkg#${userHandle.hashCode()}"
                            if (apps.none { it.packageName == identifier }) {
                                apps.add(AppInfo("$label$suffix", identifier))
                            }
                        }
                    } catch (e: Exception) { /* Skip */ }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        return apps.distinctBy { it.packageName }.sortedBy { it.label }
    }

    fun openWhatsApp(context: Context, phoneNumber: String, packageName: String?) {
        try {
            val cleaned = phoneNumber.replace("[^\\d]".toRegex(), "")
            // Use 91 as prefix if not present for Indian numbers, or assume user provides full number
            // Standardize: if starts with 10 digits and no prefix, add 91
            val formatted = if (cleaned.length == 10) "91$cleaned" else cleaned
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formatted"))
            
            if (packageName != null && packageName != "Always Ask") {
                if (packageName.contains("#")) {
                    // It's a profile-specific app (e.g. Work Profile)
                    // Unfortunately, we can't easily setPackage to a specific profile's app via Uri intent
                    // unless we use LauncherApps to start it. But wa.me URLs are usually handled by browsers too.
                    // For now, strip the # and try to set package.
                    intent.setPackage(packageName.split("#")[0])
                } else {
                    intent.setPackage(packageName)
                }
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: try without package
            try {
                val cleaned = phoneNumber.replace("[^\\d]".toRegex(), "")
                val formatted = if (cleaned.length == 10) "91$cleaned" else cleaned
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$formatted"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }
}
