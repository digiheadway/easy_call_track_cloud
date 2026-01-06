package com.example.deviceadmin

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MyDeviceAdminReceiver : DeviceAdminReceiver() {
    
    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, MyDeviceAdminReceiver::class.java)
        }
    }
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Device Admin Enabled", Toast.LENGTH_SHORT).show()
        
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = getComponentName(context)
        
        if (dpm.isDeviceOwnerApp(context.packageName)) {
            // Allow LockActivity to use lock task mode
            dpm.setLockTaskPackages(componentName, arrayOf(context.packageName))
            
            // Set initial restrictions
            try {
                dpm.setUninstallBlocked(componentName, context.packageName, true)
                dpm.addUserRestriction(componentName, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
                dpm.addUserRestriction(componentName, android.os.UserManager.DISALLOW_APPS_CONTROL)
            } catch (e: Exception) {
                // Log failure
            }
        }
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Device Admin Disabled", Toast.LENGTH_SHORT).show()
    }
}
