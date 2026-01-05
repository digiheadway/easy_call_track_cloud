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
                    // Keep overlay visible during call if it was showing
                    Log.d(TAG, "Call in progress (OFFHOOK state)")
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d(TAG, "Call ended (IDLE state). Triggering immediate sync.")
                    
                    // Hide caller ID overlay
                    CallerIdManager.hide(context)
                    
                    // Trigger immediate sync for metadata
                    CallSyncWorker.runNow(context)
                    
                    // Also trigger recording upload check
                    RecordingUploadWorker.runNow(context)

                    // Show guidance for Google Dialer users
                    showGuidanceNotification(context)
                }
            }
        }
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

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Call Finished")
            .setContentText("Recorded using Google Dialer? Share it to Sync.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("If you recorded this call using the Google Phone app, remember to Share the recording to \"${context.getString(R.string.public_app_name)}\" to sync it to the dashboard."))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3001, notification)
    }

    companion object {
        private const val TAG = "CallReceiver"
    }
}
