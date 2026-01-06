package com.deviceadmin.app.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import android.widget.Toast

/**
 * Device Admin Receiver for handling device admin lifecycle events.
 * Required for Device Admin and Device Owner functionality.
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DeviceAdminReceiver"
        
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, DeviceAdminReceiver::class.java)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin enabled")
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show()
        
        initializeDeviceOwnerPolicies(context)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin disabled")
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling device admin will remove all protection features."
    }

    /**
     * Initialize policies when app becomes device owner.
     */
    private fun initializeDeviceOwnerPolicies(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = getComponentName(context)
        
        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.d(TAG, "Not device owner - skipping initial policies")
            return
        }

        try {
            // Enable lock task mode for this package
            dpm.setLockTaskPackages(componentName, arrayOf(context.packageName))
            
            // Set initial restrictions
            dpm.setUninstallBlocked(componentName, context.packageName, true)
            dpm.addUserRestriction(componentName, UserManager.DISALLOW_UNINSTALL_APPS)
            dpm.addUserRestriction(componentName, UserManager.DISALLOW_APPS_CONTROL)
            
            Log.d(TAG, "Device owner policies initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize policies", e)
        }
    }
}
