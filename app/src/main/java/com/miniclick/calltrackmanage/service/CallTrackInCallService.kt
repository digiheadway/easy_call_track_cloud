package com.miniclick.calltrackmanage.service

import android.telecom.InCallService

/**
 * A dummy implementation of InCallService required by Android to 
 * register the app as a default dialer.
 */
class CallTrackInCallService : InCallService() {
    
    companion object {
        var currentCall: android.telecom.Call? = null
        const val CHANNEL_ID = "call_channel"
        const val NOTIFICATION_ID = 12345
        
        private var instance: java.lang.ref.WeakReference<CallTrackInCallService>? = null

        fun setAudioRoute(route: Int) {
            instance?.get()?.setAudioRoute(route)
        }

        fun toggleMute(shouldMute: Boolean) {
            instance?.get()?.setMuted(shouldMute)
        }
        
        fun getCurrentAudioState(): android.telecom.CallAudioState? {
            return instance?.get()?.callAudioState
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = java.lang.ref.WeakReference(this)
        createNotificationChannel()
    }

    override fun onCallAdded(call: android.telecom.Call) {
        super.onCallAdded(call)
        currentCall = call
        
        // Launch UI immediately
        launchInCallActivity()
        
        // Show notification immediately
        updateNotification(call)
        
        call.registerCallback(object : android.telecom.Call.Callback() {
            override fun onStateChanged(call: android.telecom.Call, state: Int) {
                super.onStateChanged(call, state)
                updateNotification(call)
                if (state == android.telecom.Call.STATE_DISCONNECTED) {
                    currentCall = null
                    stopForeground(true)
                }
            }
        })
    }

    override fun onCallRemoved(call: android.telecom.Call) {
        super.onCallRemoved(call)
        if (currentCall == call) {
            currentCall = null
            stopForeground(true)
        }
    }
    
    private fun launchInCallActivity() {
        try {
            val intent = android.content.Intent(this, com.miniclick.calltrackmanage.ui.call.InCallActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            // Recommended flags for launching from service
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP) 
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("CallTrackInCallService", "Failed to launch activity", e)
        }
    }

    private fun updateNotification(call: android.telecom.Call) {
        val contactName = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val stateText = when (call.state) {
            android.telecom.Call.STATE_RINGING -> "Incoming Call"
            android.telecom.Call.STATE_DIALING -> "Dialing..."
            android.telecom.Call.STATE_ACTIVE -> "In Call"
            android.telecom.Call.STATE_DISCONNECTED -> "Ended"
            else -> "Call"
        }
        
        val intent = android.content.Intent(this, com.miniclick.calltrackmanage.ui.call.InCallActivity::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 123, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // build the notification
        val builder = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(contactName)
            .setContentText(stateText)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_CALL)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX) // Heads up
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(false)

        try {
            startForeground(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("CallTrackInCallService", "Error showing notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Calls",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Active call status"
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
