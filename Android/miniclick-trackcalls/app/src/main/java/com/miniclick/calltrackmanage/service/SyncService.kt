package com.miniclick.calltrackmanage.service

import android.annotation.SuppressLint
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        if (checkHasPermissions()) {
            startPhoneStateMonitoring()
        } else {
            Log.d(TAG, "Permissions missing, skipping monitor start")
        }
        
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun checkHasPermissions(): Boolean {
        val phoneState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
        
        val callLog = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        return phoneState && callLog
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
                handleCallStateChange(state, phoneNumber)
            }
        }
        
        try {
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Log.d(TAG, "PhoneStateListener registered")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for PhoneStateListener", e)
        }
    }

    private fun handleCallStateChange(state: Int, phoneNumber: String? = null) {
        Log.d(TAG, "Call state changed: $lastState -> $state (Number: $phoneNumber)")
        
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                // Incoming call
                if (lastState == TelephonyManager.CALL_STATE_IDLE) {
                    Log.d(TAG, "Incoming call started - showing caller ID")
                    showCallerIdOverlay(phoneNumber)
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Outgoing call started or incoming call answered
                if (lastState == TelephonyManager.CALL_STATE_IDLE) {
                    Log.d(TAG, "Outgoing call started - showing caller ID")
                    showCallerIdOverlay(phoneNumber)
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended
                if (lastState != TelephonyManager.CALL_STATE_IDLE) {
                    Log.d(TAG, "Call ended - hiding caller ID and triggering sync")
                    
                    // Hide caller ID overlay
                    CallerIdManager.hide(this)
                    
                    // Delay slightly to allow call log to be updated
                    serviceScope.launch {
                        delay(2000) // Wait 2 seconds for call log to update
                        triggerSync()
                    }
                }
            }
        }
        
        lastState = state
    }
    
    private fun showCallerIdOverlay(initialPhoneNumber: String? = null) {
        // If we already have the number, show it immediately
        if (!initialPhoneNumber.isNullOrBlank()) {
            Log.d(TAG, "Already have number, showing caller ID: $initialPhoneNumber")
            CallerIdManager.show(this, initialPhoneNumber)
            return
        }

        // Query call log to get the most recent/active call's phone number
        // We retry a few times because the call log might not update instantly
        serviceScope.launch {
            var phoneNumber: String? = null
            repeat(3) { attempt ->
                try {
                    phoneNumber = getActiveCallNumber()
                    if (!phoneNumber.isNullOrBlank()) {
                        Log.d(TAG, "Found active call number from log: $phoneNumber (attempt ${attempt + 1})")
                        CallerIdManager.show(this@SyncService, phoneNumber!!)
                        return@launch
                    }
                    if (attempt < 2) delay(1000) // Wait longer between retries
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting active call number", e)
                }
            }
            Log.d(TAG, "Could not determine active call number after retries")
        }
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun getActiveCallNumber(): String? = withContext(Dispatchers.IO) {
        try {
            // Query call log for the most recent call (within last 10 seconds)
            val tenSecondsAgo = System.currentTimeMillis() - 10_000
            
            val cursor = contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(android.provider.CallLog.Calls.NUMBER),
                "${android.provider.CallLog.Calls.DATE} > ?",
                arrayOf(tenSecondsAgo.toString()),
                "${android.provider.CallLog.Calls.DATE} DESC"
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                    if (numberIndex >= 0) {
                        return@withContext it.getString(numberIndex)
                    }
                }
            }
            
            // Fallback: get the most recent call regardless of time
            val fallbackCursor = contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(android.provider.CallLog.Calls.NUMBER),
                null,
                null,
                "${android.provider.CallLog.Calls.DATE} DESC LIMIT 1"
            )
            
            fallbackCursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                    if (numberIndex >= 0) {
                        return@withContext it.getString(numberIndex)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to read call log", e)
        }
        null
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
        if (!checkHasPermissions()) {
            Log.d(TAG, "Sync skipped - missing permissions")
            return
        }
        
        val settingsRepository = com.miniclick.calltrackmanage.data.SettingsRepository.getInstance(this)
        if (!settingsRepository.isSetupGuideCompleted()) {
            Log.d(TAG, "Sync skipped - setup guide not completed")
            return
        }

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
