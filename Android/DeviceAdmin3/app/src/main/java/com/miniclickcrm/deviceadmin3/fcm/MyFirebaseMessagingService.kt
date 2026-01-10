package com.miniclickcrm.deviceadmin3.fcm

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.miniclickcrm.deviceadmin3.manager.DeviceManager

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            val command = remoteMessage.data["command"]
            Log.d("FCM", "Message data payload command: $command")
            
            val prefs = getSharedPreferences("device_admin_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Sync all settings if provided in the push
            remoteMessage.data["amount"]?.let { editor.putInt("amount", it.toIntOrNull() ?: 0) }
            remoteMessage.data["message"]?.let { editor.putString("message", it) }
            remoteMessage.data["call_to"]?.let { editor.putString("call_to", it) }
            remoteMessage.data["is_protected"]?.let { editor.putBoolean("is_protected", it == "1" || it == "true") }
            remoteMessage.data["is_freezed"]?.let { editor.putBoolean("is_freezed", it == "1" || it == "true") }
            editor.apply()
            
            // Handle offline unlock codes (comma separated)
            remoteMessage.data["unlock_codes"]?.let { codesStr ->
                val codes = codesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (codes.isNotEmpty()) {
                    val securityManager = com.miniclickcrm.deviceadmin3.manager.SecurityManager(this)
                    securityManager.storeUnlockCodes(codes)
                }
            }

            val deviceManager = DeviceManager(this)
            
            // Apply states
            remoteMessage.data["is_protected"]?.let { 
                deviceManager.setProtected(it == "1" || it == "true")
            }

            when (command) {
                "LOCK_DEVICE" -> {
                    val message = remoteMessage.data["message"] ?: prefs.getString("message", "Device locked") ?: "Device locked"
                    deviceManager.lockDevice(message)
                }
                "UNLOCK_DEVICE" -> deviceManager.unlockDevice()
                "TEMPORAL_UNLOCK" -> {
                    val duration = remoteMessage.data["duration"]?.toIntOrNull() ?: 60
                    deviceManager.temporalUnlock(duration)
                }
                "REMOVE_PROTECTION" -> deviceManager.removeProtection()
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        // Token is also handled/saved by FcmTokenManager when needed
        com.miniclickcrm.deviceadmin3.utils.FcmTokenManager.saveToken(this, token)
    }
}
