package com.deviceadmin.app.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.deviceadmin.app.service.ProtectionAccessibilityService

/**
 * Helper for checking system permissions.
 */
object PermissionHelper {

    /**
     * Checks if the accessibility service is enabled.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, ProtectionAccessibilityService::class.java)
        
        // Method 1: Check Settings.Secure
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        
        val colonSplitter = enabledServices.split(':')
        for (componentString in colonSplitter) {
            val enabledComponent = ComponentName.unflattenFromString(componentString)
            if (enabledComponent == expectedComponent) {
                return true
            }
        }
        
        // Method 2: Check AccessibilityManager
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServicesList = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        for (serviceInfo in enabledServicesList) {
            if (serviceInfo.resolveInfo.serviceInfo.packageName == context.packageName) {
                return true
            }
        }
        
        return false
    }

    /**
     * Checks if overlay (draw over other apps) permission is granted.
     */
    fun isOverlayPermissionGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
}
