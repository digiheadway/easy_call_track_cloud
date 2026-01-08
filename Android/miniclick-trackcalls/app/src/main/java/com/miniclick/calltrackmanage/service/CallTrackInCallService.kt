package com.miniclick.calltrackmanage.service

import android.telecom.Call
import android.telecom.InCallService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CallStateInfo(
    val phoneNumber: String = "",
    val state: Int = Call.STATE_DISCONNECTED,
    val duration: Long = 0
)

/**
 * A dummy implementation of InCallService required by Android to 
 * register the app as a default dialer.
 */
class CallTrackInCallService : InCallService() {
    
    companion object {
        private val _callStatus = MutableStateFlow<CallStateInfo?>(null)
        val callStatus: StateFlow<CallStateInfo?> = _callStatus.asStateFlow()

        var currentCall: Call? = null
            set(value) {
                field = value
                if (value == null) {
                    _callStatus.value = null
                } else {
                    updateStatus(value)
                }
            }
        
        fun updateStatus(call: Call) {
            _callStatus.value = CallStateInfo(
                phoneNumber = call.details?.handle?.schemeSpecificPart ?: "Unknown",
                state = call.state
            )
        }

        const val CHANNEL_ID = "call_channel"
        const val INCOMING_CHANNEL_ID = "incoming_call_channel"
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
                updateStatus(call)
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
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            
            // On Android 10+, check if we can launch from background
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Starting from Android Q, we rely more on the full-screen intent from notification
                // But we still try to launch directly as well
                val keyguardManager = getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                if (keyguardManager?.isKeyguardLocked == true) {
                    // Screen is locked - full-screen intent will handle it
                    android.util.Log.d("CallTrackInCallService", "Device locked, relying on full-screen notification intent")
                } else {
                    startActivity(intent)
                }
            } else {
                startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("CallTrackInCallService", "Failed to launch activity", e)
        }
    }

    private fun updateNotification(call: android.telecom.Call) {
        val contactName = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val isRinging = call.state == android.telecom.Call.STATE_RINGING
        val isActive = call.state == android.telecom.Call.STATE_ACTIVE
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
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 123, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Create Decline action PendingIntent
        val declineIntent = android.content.Intent(this, CallActionReceiver::class.java).apply {
            action = "DECLINE_CALL"
        }
        val declinePending = android.app.PendingIntent.getBroadcast(
            this, 1, declineIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Create Answer action PendingIntent
        val answerIntent = android.content.Intent(this, CallActionReceiver::class.java).apply {
            action = "ANSWER_CALL"
        }
        val answerPending = android.app.PendingIntent.getBroadcast(
            this, 2, answerIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Create Hang Up action PendingIntent (for active calls)
        val hangUpIntent = android.content.Intent(this, CallActionReceiver::class.java).apply {
            action = "DECLINE_CALL"
        }
        val hangUpPending = android.app.PendingIntent.getBroadcast(
            this, 3, hangUpIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Use incoming call channel for ringing state (higher priority)
        val channelId = if (isRinging) INCOMING_CHANNEL_ID else CHANNEL_ID

        // Create Person for CallStyle
        val person = androidx.core.app.Person.Builder()
            .setName(contactName)
            .setImportant(true)
            .build()

        // Build notification with CallStyle for proper system-level call notification
        val builder = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_CALL)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)  // Critical: Makes notification non-dismissable
            .setAutoCancel(false)

        // Use CallStyle for Android 12+ (API 31+) for proper call notification styling
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val callStyle = if (isRinging) {
                // Incoming call style - shows Answer and Decline buttons
                androidx.core.app.NotificationCompat.CallStyle.forIncomingCall(
                    person,
                    declinePending,
                    answerPending
                )
            } else {
                // Ongoing call style - shows Hang Up button
                androidx.core.app.NotificationCompat.CallStyle.forOngoingCall(
                    person,
                    hangUpPending
                )
            }
            builder.setStyle(callStyle)
        } else {
            // Fallback for older Android versions
            builder.setContentTitle(contactName)
            builder.setContentText(stateText)
            
            if (isRinging) {
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePending)
                builder.addAction(android.R.drawable.ic_menu_call, "Answer", answerPending)
            } else if (isActive) {
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hang Up", hangUpPending)
            }
        }

        try {
            startForeground(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("CallTrackInCallService", "Error showing notification", e)
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val manager = getSystemService(android.app.NotificationManager::class.java)
            
            // Regular call channel
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Calls",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Active call status"
            manager.createNotificationChannel(channel)

            // Incoming call channel (critical importance for full-screen intent)
            val incomingChannel = android.app.NotificationChannel(
                INCOMING_CHANNEL_ID,
                "Incoming Calls",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            incomingChannel.description = "Incoming call alerts"
            incomingChannel.setBypassDnd(true)
            incomingChannel.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            incomingChannel.setShowBadge(true)
            manager.createNotificationChannel(incomingChannel)
        }
    }
}

/**
 * BroadcastReceiver to handle call actions (Answer/Decline) from notification
 */
class CallActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        when (intent.action) {
            "ANSWER_CALL" -> {
                CallTrackInCallService.currentCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
            }
            "DECLINE_CALL" -> {
                CallTrackInCallService.currentCall?.disconnect()
            }
        }
    }
}
