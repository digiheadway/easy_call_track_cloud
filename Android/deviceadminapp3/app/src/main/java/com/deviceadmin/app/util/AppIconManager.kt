package com.deviceadmin.app.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Manages app icon visibility in the launcher.
 * Used for stealth mode functionality.
 */
object AppIconManager {

    private const val TAG = "AppIconManager"
    private const val LAUNCHER_ALIAS = "com.deviceadmin.app.MainLauncherAlias"

    /**
     * Hides the app icon from the launcher.
     * Only works on Android < 10.
     */
    fun hideIcon(context: Context): Boolean {
        return setComponentState(context, LAUNCHER_ALIAS, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }

    /**
     * Shows the app icon in the launcher.
     */
    fun showIcon(context: Context): Boolean {
        return setComponentState(context, LAUNCHER_ALIAS, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }

    /**
     * Checks if the app icon is currently hidden.
     */
    fun isIconHidden(context: Context): Boolean {
        val pm = context.packageManager
        val componentName = ComponentName(context, LAUNCHER_ALIAS)
        
        return try {
            val state = pm.getComponentEnabledSetting(componentName)
            state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check icon state", e)
            false
        }
    }

    private fun setComponentState(context: Context, componentClass: String, state: Int): Boolean {
        val pm = context.packageManager
        val componentName = ComponentName(context, componentClass)
        
        return try {
            pm.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Component state set: $componentClass -> $state")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set component state", e)
            false
        }
    }
}
