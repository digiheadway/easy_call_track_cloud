package com.miniclickcrm.deviceadmin3.manager

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.miniclickcrm.deviceadmin3.LockScreenActivity
import com.miniclickcrm.deviceadmin3.receiver.MyAdminReceiver

class DeviceManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(context, MyAdminReceiver::class.java)
    private val prefs = context.getSharedPreferences("device_admin_prefs", Context.MODE_PRIVATE)

    fun isAdminActive(): Boolean {
        return dpm.isAdminActive(adminComponent)
    }

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun systemLock() {
        if (isAdminActive()) {
            dpm.lockNow()
        }
    }
    
    /**
     * Freeze device and show lock screen with custom message
     */
    fun freezeDevice(message: String) {
        prefs.edit().putBoolean("is_freezed", true).apply()
        
        if (isAdminActive()) {
            dpm.lockNow()
        }
        
        // Show lock screen activity with message
        val lockIntent = Intent(context, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("lock_message", message)
        }
        context.startActivity(lockIntent)
        
        // Enable launcher mode to intercept Home button
        toggleLauncher(true)
    }
    
    /**
     * Disable/Disable the Launcher capability of LockScreenActivity
     */
    fun toggleLauncher(enable: Boolean) {
        val pm = context.packageManager
        val component = ComponentName(context, "com.miniclickcrm.deviceadmin3.HomeLauncherAlias")
        val state = if (enable) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        
        pm.setComponentEnabledSetting(
            component,
            state,
            PackageManager.DONT_KILL_APP
        )
    }
    
    /**
     * Unfreeze device - dismiss lock screen
     */
    fun unfreezeDevice() {
        prefs.edit().putBoolean("is_freezed", false).apply()
        
        // Send broadcast to close lock screen
        val intent = Intent("com.miniclickcrm.deviceadmin3.UNLOCK_DEVICE")
        context.sendBroadcast(intent)

        // Disable launcher mode
        toggleLauncher(false)
    }
    
    /**
     * Check if device is currently frozen (locked) by our app
     */
    fun isFreezed(): Boolean {
        return prefs.getBoolean("is_freezed", false)
    }

    /**
     * Check if protection (anti-uninstall) is active
     */
    fun isProtected(): Boolean {
        return prefs.getBoolean("is_protected", false)
    }

    /**
     * Legacy helper, now refers to isFreezed
     */
    fun isLocked(): Boolean = isFreezed()
    
    /**
     * Legacy alias for freezeDevice
     */
    fun lockDevice(message: String) = freezeDevice(message)

    /**
     * Legacy alias for unfreezeDevice
     */
    fun unlockDevice() = unfreezeDevice()

    fun setProtected(protected: Boolean) {
        prefs.edit().putBoolean("is_protected", protected).apply()
        setUninstallBlocked(protected)
        // Disallow factory reset if is device owner and protected
        if (isDeviceOwner()) {
            setUserRestriction(android.os.UserManager.DISALLOW_FACTORY_RESET, protected)
        }
    }

    fun setUninstallBlocked(blocked: Boolean) {
        if (isDeviceOwner()) {
            dpm.setUninstallBlocked(adminComponent, context.packageName, blocked)
        }
    }

    fun setUserRestriction(restriction: String, restricted: Boolean) {
        if (isDeviceOwner()) {
            if (restricted) {
                dpm.addUserRestriction(adminComponent, restriction)
            } else {
                dpm.clearUserRestriction(adminComponent, restriction)
            }
        }
    }

    fun temporalUnlock(durationMinutes: Int) {
        // Unfreeze device temporarily
        unfreezeDevice()
        
        // Set timer to re-freeze after duration
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val message = prefs.getString("message", "Device frozen") ?: "Device frozen"
            freezeDevice(message)
        }, durationMinutes * 60 * 1000L)
    }

    fun removeProtection() {
        unfreezeDevice()
        setProtected(false)
        
        if (isAdminActive()) {
            if (isDeviceOwner()) {
                dpm.clearDeviceOwnerApp(context.packageName)
            }
            dpm.removeActiveAdmin(adminComponent)
        }
    }
}
