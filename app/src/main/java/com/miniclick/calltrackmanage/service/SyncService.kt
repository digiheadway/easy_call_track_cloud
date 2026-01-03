package com.miniclick.calltrackmanage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.miniclick.calltrackmanage.MainActivity
import com.miniclick.calltrackmanage.R
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker
import kotlinx.coroutines.*

/**
 * Foreground service that monitors for call events and triggers sync.
 * This runs persistently to ensure calls are synced even when app is closed.
 */
class SyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null
    
    private var lastState = TelephonyManager.CALL_STATE_IDLE

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SyncService created")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        startPhoneStateMonitoring()
        
        // Run initial sync
        triggerSync()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SyncService started")
        return START_STICKY // Restart if killed
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SyncService destroyed")
        stopPhoneStateMonitoring()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps call tracking active in background"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Cloud")
            .setContentText("Syncing calls in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startPhoneStateMonitoring() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            startTelephonyCallback()
        } else {
            // Older versions
            startPhoneStateListener()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startTelephonyCallback() {
        telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallStateChange(state)
            }
        }
        
        try {
            telephonyManager?.registerTelephonyCallback(
                mainExecutor,
                telephonyCallback!!
            )
            Log.d(TAG, "TelephonyCallback registered")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for TelephonyCallback", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun startPhoneStateListener() {
        phoneStateListener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleCallStateChange(state)
            }
        }
        
        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Log.d(TAG, "PhoneStateListener registered")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for PhoneStateListener", e)
        }
    }

    private fun handleCallStateChange(state: Int) {
        Log.d(TAG, "Call state changed: $lastState -> $state")
        
        // When call ends (transition to IDLE from RINGING or OFFHOOK)
        if (state == TelephonyManager.CALL_STATE_IDLE && lastState != TelephonyManager.CALL_STATE_IDLE) {
            Log.d(TAG, "Call ended - triggering sync")
            
            // Delay slightly to allow call log to be updated
            serviceScope.launch {
                delay(2000) // Wait 2 seconds for call log to update
                triggerSync()
            }
        }
        
        lastState = state
    }

    private fun stopPhoneStateMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager?.unregisterTelephonyCallback(it)
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let {
                telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }

    private fun triggerSync() {
        Log.d(TAG, "Triggering sync workers")
        CallSyncWorker.runNow(this)
        RecordingUploadWorker.runNow(this)
    }

    companion object {
        private const val TAG = "SyncService"
        private const val CHANNEL_ID = "sync_service_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, SyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SyncService::class.java))
        }
    }
}
