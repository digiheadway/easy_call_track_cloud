package com.deviceadmin.app.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.deviceadmin.app.receiver.DeviceAdminReceiver

/**
 * Helper class for Device Policy Manager operations.
 * Centralizes all device admin and device owner functionality.
 */
class DevicePolicyHelper(private val context: Context) {

    companion object {
        private const val TAG = "DevicePolicyHelper"
    }

    private val dpm: DevicePolicyManager by lazy {
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    val adminComponent: ComponentName by lazy {
        ComponentName(context, DeviceAdminReceiver::class.java)
    }

    // ===== Status Checks =====

    /**
     * Checks if this app is a device administrator.
     */
    fun isDeviceAdmin(): Boolean {
        return dpm.isAdminActive(adminComponent)
    }

    /**
     * Checks if this app is the device owner.
     */
    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    // ===== Protection Controls =====

    /**
     * Enables all device protection restrictions.
     * Requires device owner privileges for full functionality.
     */
    fun enableProtection(): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Cannot enable protection - not device owner")
            return false
        }

        return try {
            // Block uninstall
            dpm.setUninstallBlocked(adminComponent, context.packageName, true)
            
            // Add user restrictions
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_APPS_CONTROL)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
            }
            
            Log.d(TAG, "Protection enabled successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable protection", e)
            false
        }
    }

    /**
     * Disables all device protection restrictions.
     */
    fun disableProtection(): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Cannot disable protection - not device owner")
            return false
        }

        return try {
            // Allow uninstall
            dpm.setUninstallBlocked(adminComponent, context.packageName, false)
            
            // Clear user restrictions
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_APPS_CONTROL)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_ADJUST_VOLUME)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
            }
            
            Log.d(TAG, "Protection disabled successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable protection", e)
            false
        }
    }

    // ===== Lock Task Mode =====

    /**
     * Enables lock task mode for the specified activity.
     * This pins the activity and prevents user from leaving.
     */
    fun enableLockTaskMode(): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "Cannot enable lock task - not device owner")
            return false
        }

        return try {
            // Allow this package to enter lock task mode
            val currentPackages = dpm.getLockTaskPackages(adminComponent)
            if (!currentPackages.contains(context.packageName)) {
                dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            }

            // Configure lock task features (strict mode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setLockTaskFeatures(adminComponent, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }

            // Add hardware lockdown restrictions
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
            dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_ADJUST_VOLUME)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
            }

            Log.d(TAG, "Lock task mode prepared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare lock task mode", e)
            false
        }
    }

    /**
     * Disables status bar while device is locked.
     */
    fun setStatusBarDisabled(disabled: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        
        return try {
            dpm.setStatusBarDisabled(adminComponent, disabled)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set status bar state", e)
            false
        }
    }
}
