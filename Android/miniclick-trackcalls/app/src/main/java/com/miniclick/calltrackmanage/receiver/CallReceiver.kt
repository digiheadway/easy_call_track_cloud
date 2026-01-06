package com.miniclick.calltrackmanage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.miniclick.calltrackmanage.service.CallerIdManager
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker
import com.miniclick.calltrackmanage.data.SettingsRepository
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import com.miniclick.calltrackmanage.MainActivity
import com.miniclick.calltrackmanage.R
import android.os.Build

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Incoming call - show caller ID overlay
                    if (!phoneNumber.isNullOrBlank()) {
                        Log.d(TAG, "Incoming call from $phoneNumber - showing caller ID")
                        CallerIdManager.show(context, phoneNumber)
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call answered or outgoing call started
                    Log.d(TAG, "Call in progress (OFFHOOK state)")
                    wasOffHook = true
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d(TAG, "Call ended (IDLE state). Triggering immediate sync.")
                    
                    // Hide caller ID overlay
                    CallerIdManager.hide(context)
                    
                    // Trigger immediate sync for metadata
                    CallSyncWorker.runNow(context)
                    
                    // Also trigger recording upload check
                    RecordingUploadWorker.runNow(context)

                    // Show guidance only if the call was actually answered/made
                    if (wasOffHook) {
                        val settings = SettingsRepository.getInstance(context)
                        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                        val defaultDialer = telecomManager?.defaultDialerPackage
                        
                        // 1. Google Dialer Recording Reminder
                        if (settings.isShowRecordingReminder() && defaultDialer == "com.google.android.dialer") {
                            showGuidanceNotification(context)
                        }

                        // 2. Unknown Number Note Prompt
                        if (settings.isShowUnknownNoteReminder() && !phoneNumber.isNullOrBlank()) {
                            checkAndShowNotePrompt(context, phoneNumber)
                        }
                    }
                    
                    // Reset for next call
                    wasOffHook = false
                }
            }
        }
    }

    private fun checkAndShowNotePrompt(context: Context, phoneNumber: String) {
        // Query system contacts to see if this is an unknown number
        val contactName = fetchContactName(context, phoneNumber)
        if (contactName == null) {
            // Unknown number! Show prompt to add note
            showNotePromptNotification(context, phoneNumber)
        }
    }

    private fun fetchContactName(context: Context, phoneNumber: String): String? {
        val uri = android.net.Uri.withAppendedPath(
            android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(phoneNumber)
        )
        val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun showNotePromptNotification(context: Context, phoneNumber: String) {
        val channelId = "call_note_prompt_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Add Call Note",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_PERSON_DETAILS", phoneNumber)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 3002, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Unknown Number: $phoneNumber")
            .setContentText("Tap to add a note or label for this person.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3002, notification)
    }

    private fun showGuidanceNotification(context: Context) {
        val settings = SettingsRepository.getInstance(context)
        if (!settings.isCallRecordEnabled()) return

        val channelId = "call_guidance_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Call Recording Guidance",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val body = "If you recorded this call, share it to \"${context.getString(R.string.public_app_name)}\" to sync it."

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Recorded with Google Dialer?")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3001, notification)
    }

    companion object {
        private const val TAG = "CallReceiver"
        private var wasOffHook = false
    }
}
