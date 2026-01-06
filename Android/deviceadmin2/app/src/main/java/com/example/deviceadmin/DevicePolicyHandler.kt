package com.example.deviceadmin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserManager

object DevicePolicyHandler {

    fun applyEnterprisePolicies(context: Context, dpm: DevicePolicyManager, admin: ComponentName, enabled: Boolean) {
        if (enabled) {
            // Disables USB debugging
            dpm.setGlobalSetting(admin, android.provider.Settings.Global.ADB_ENABLED, "0")
            
            // Disables Factory Reset
            dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
            
            // Disables Safe Boot
            dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
            
            // Disables status bar expansion (requires Device Owner or specific API)
            // dpm.setStatusBarDisabled(admin, true) 
        } else {
            dpm.setGlobalSetting(admin, android.provider.Settings.Global.ADB_ENABLED, "1")
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
            dpm.clearUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
            // dpm.setStatusBarDisabled(admin, false)
        }
    }
}
