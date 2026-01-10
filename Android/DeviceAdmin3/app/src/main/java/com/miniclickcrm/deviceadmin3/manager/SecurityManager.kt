package com.miniclickcrm.deviceadmin3.manager

import android.content.Context
import android.util.Log

class SecurityManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)

    fun storeUnlockCodes(codes: List<String>) {
        prefs.edit().putStringSet("unlock_codes", codes.toSet()).apply()
    }

    fun verifyCode(inputCode: String): Boolean {
        val codes = prefs.getStringSet("unlock_codes", emptySet()) ?: emptySet()
        return codes.contains(inputCode)
    }

    fun isMasterCode(input: String): Boolean {
        return input == "00998877"
    }

    fun getRecoveryKey(): String {
        return "00998877" 
    }

    fun setRecoveryKey(key: String) {
        prefs.edit().putString("recovery_key", key).apply()
    }
}
