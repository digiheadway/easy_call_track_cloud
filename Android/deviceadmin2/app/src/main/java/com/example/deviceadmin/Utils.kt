package com.example.deviceadmin

import android.content.Context
import android.content.SharedPreferences

object Utils {
    private const val PREFS_NAME = "enterprise_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_IS_FREEZED = "is_freezed"
    private const val KEY_IS_PROTECTED = "is_protected"
    private const val KEY_STEALTH_MODE = "stealth_mode"
    private const val KEY_EMI_AMOUNT = "emi_amount"
    private const val KEY_SERVER_MESSAGE = "server_message"
    
    const val MASTER_PIN = "1133"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setDeviceId(context: Context, id: String) {
        getPrefs(context).edit().putString(KEY_DEVICE_ID, id).apply()
    }

    fun getDeviceId(context: Context): String {
        return getPrefs(context).getString(KEY_DEVICE_ID, "") ?: ""
    }

    fun setFreezed(context: Context, freezed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_FREEZED, freezed).apply()
    }

    fun isFreezed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_FREEZED, false)
    }

    fun setProtected(context: Context, protected: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_IS_PROTECTED, protected).apply()
    }

    fun isProtected(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_PROTECTED, false)
    }

    fun setStealthMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_STEALTH_MODE, enabled).apply()
    }

    fun isStealthMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STEALTH_MODE, false)
    }

    fun setEmiAmount(context: Context, amount: String) {
        getPrefs(context).edit().putString(KEY_EMI_AMOUNT, amount).apply()
    }

    fun getEmiAmount(context: Context): String {
        return getPrefs(context).getString(KEY_EMI_AMOUNT, "$0.00") ?: "$0.00"
    }

    fun setServerMessage(context: Context, message: String) {
        getPrefs(context).edit().putString(KEY_SERVER_MESSAGE, message).apply()
    }

    fun getServerMessage(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_MESSAGE, "This device is locked.") ?: "This device is locked."
    }
}
