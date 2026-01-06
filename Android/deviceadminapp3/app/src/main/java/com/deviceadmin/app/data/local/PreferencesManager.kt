package com.deviceadmin.app.data.local

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.deviceadmin.app.data.model.DeviceState
import com.deviceadmin.app.data.model.PhoneState
import com.deviceadmin.app.data.model.ProtectionState

/**
 * Manages persistent storage for device state and app preferences.
 * Uses SharedPreferences as the backing store.
 */
class PreferencesManager(private val context: Context) {

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREFS_NAME = "device_admin_prefs"
        
        // Preference Keys
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_PHONE_STATE = "phone_state"
        private const val KEY_PROTECTION_STATE = "protection_state"
        private const val KEY_UNINSTALL_ALLOWED = "uninstall_allowed"
        private const val KEY_EMI_AMOUNT = "emi_amount"
        private const val KEY_MESSAGE = "message"
        private const val KEY_CALL_TO = "call_to"
        private const val KEY_BREAK_END_TIME = "break_end_time"
        
        // Global settings key (for Device Owner backup)
        private const val GLOBAL_DEVICE_ID_KEY = "com.deviceadmin.app.DEVICE_ID"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retrieves the complete device state from preferences.
     */
    fun getDeviceState(): DeviceState {
        return DeviceState(
            deviceId = getDeviceId(),
            isSetupComplete = isSetupComplete(),
            phoneState = getPhoneState(),
            protectionState = getProtectionState(),
            isUninstallAllowed = isUninstallAllowed(),
            emiAmount = getEmiAmount(),
            message = getMessage(),
            callToNumber = getCallToNumber(),
            breakEndTime = getBreakEndTime()
        )
    }

    // ===== Device ID =====
    
    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        
        // Fallback: try to retrieve from Global Settings (Device Owner backup)
        if (deviceId.isEmpty()) {
            try {
                deviceId = Settings.Global.getString(context.contentResolver, GLOBAL_DEVICE_ID_KEY) ?: ""
                if (deviceId.isNotEmpty()) {
                    setDeviceId(deviceId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read device ID from Global Settings", e)
            }
        }
        
        return deviceId
    }

    fun setDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun backupDeviceIdToGlobal(deviceId: String): Boolean {
        return try {
            Settings.Global.putString(context.contentResolver, GLOBAL_DEVICE_ID_KEY, deviceId)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup device ID to Global Settings", e)
            false
        }
    }

    // ===== Setup State =====
    
    fun isSetupComplete(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETE, false)
    }

    fun setSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }

    // ===== Phone State =====
    
    fun getPhoneState(): PhoneState {
        val value = prefs.getString(KEY_PHONE_STATE, null)
        return PhoneState.fromString(value)
    }

    fun setPhoneState(state: PhoneState) {
        val value = when (state) {
            PhoneState.ACTIVE -> "Active"
            PhoneState.FROZEN -> "Freeze"
        }
        prefs.edit().putString(KEY_PHONE_STATE, value).apply()
    }

    // ===== Protection State =====
    
    fun getProtectionState(): ProtectionState {
        val value = prefs.getString(KEY_PROTECTION_STATE, null)
        return ProtectionState.fromString(value)
    }

    fun setProtectionState(state: ProtectionState) {
        val value = when (state) {
            ProtectionState.ENABLED -> "enabled"
            ProtectionState.DISABLED -> "disabled"
        }
        prefs.edit().putString(KEY_PROTECTION_STATE, value).apply()
    }

    // ===== Uninstall Allowed =====
    
    fun isUninstallAllowed(): Boolean {
        return prefs.getBoolean(KEY_UNINSTALL_ALLOWED, true)
    }

    fun setUninstallAllowed(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_UNINSTALL_ALLOWED, allowed).apply()
    }

    // ===== EMI Data =====
    
    fun getEmiAmount(): Int {
        return prefs.getInt(KEY_EMI_AMOUNT, 0)
    }

    fun setEmiAmount(amount: Int) {
        prefs.edit().putInt(KEY_EMI_AMOUNT, amount).apply()
    }

    fun getMessage(): String? {
        return prefs.getString(KEY_MESSAGE, null)
    }

    fun setMessage(message: String?) {
        prefs.edit().putString(KEY_MESSAGE, message).apply()
    }

    fun getCallToNumber(): String? {
        return prefs.getString(KEY_CALL_TO, null)
    }

    fun setCallToNumber(number: String?) {
        prefs.edit().putString(KEY_CALL_TO, number).apply()
    }

    // ===== Break Time =====
    
    fun getBreakEndTime(): Long {
        return prefs.getLong(KEY_BREAK_END_TIME, 0L)
    }

    fun setBreakEndTime(time: Long) {
        prefs.edit().putLong(KEY_BREAK_END_TIME, time).apply()
    }

    fun isOnBreak(): Boolean {
        return System.currentTimeMillis() < getBreakEndTime()
    }

    // ===== Clear All =====
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
