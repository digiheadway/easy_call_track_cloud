package com.example.salescrm.notification

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
import com.example.salescrm.MainActivity
import com.example.salescrm.R
import com.example.salescrm.data.CallLogRepository
import com.example.salescrm.data.formatBudget
import com.example.salescrm.data.UserPreferencesRepository
import com.example.salescrm.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * A persistent foreground service that monitors phone call state.
 * This is more reliable than a BroadcastReceiver for background execution
 * on modern Android versions with strict battery optimization.
 */
class CallMonitorService : Service() {

    companion object {
        private const val TAG = "CallMonitorService"
        private const val CHANNEL_ID = "call_monitor_channel"
        private const val NOTIFICATION_ID = 60002
        
        fun startService(context: Context) {
            val intent = Intent(context, CallMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, CallMonitorService::class.java)
            context.stopService(intent)
        }
        
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (CallMonitorService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var lastNumber: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        registerPhoneStateListener()
        Log.d(TAG, "CallMonitorService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart if killed
    }

    override fun onDestroy() {
        unregisterPhoneStateListener()
        serviceScope.cancel()
        Log.d(TAG, "CallMonitorService stopped")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Monitoring",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Monitors incoming and outgoing calls for caller ID"
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Caller ID Active")
            .setContentText("Monitoring calls for lead identification")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun registerPhoneStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            registerTelephonyCallback()
        } else {
            // Older versions
            registerLegacyPhoneStateListener()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallback() {
        telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallStateChange(state, null)
            }
        }
        
        try {
            telephonyManager?.registerTelephonyCallback(
                mainExecutor,
                telephonyCallback!!
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission for TelephonyCallback", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyPhoneStateListener() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleCallStateChange(state, phoneNumber)
            }
        }
        
        try {
            telephonyManager?.listen(
                phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission for PhoneStateListener", e)
        }
    }

    private fun unregisterPhoneStateListener() {
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

    private fun handleCallStateChange(state: Int, phoneNumber: String?) {
        Log.d(TAG, "Call state changed: $state, number: $phoneNumber")
        
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                // Incoming call
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    val number = phoneNumber ?: getIncomingNumberFromCallLog()
                    if (!number.isNullOrBlank()) {
                        lastNumber = number
                        handleLiveCall(number, "Incoming Call")
                    }
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call in progress (could be outgoing)
                if (lastState == TelephonyManager.CALL_STATE_IDLE) {
                    // This is an outgoing call
                    serviceScope.launch {
                        delay(500) // Wait for call log to update
                        val number = getOutgoingNumberFromCallLog()
                        if (!number.isNullOrBlank()) {
                            lastNumber = number
                            handleLiveCall(number, "Outgoing Call")
                        }
                    }
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended
                if (lastState != TelephonyManager.CALL_STATE_IDLE) {
                    handleCallEnd()
                }
            }
        }
        
        lastState = state
    }

    private fun getIncomingNumberFromCallLog(): String? {
        // On Android 10+, we need to read from call log since EXTRA_INCOMING_NUMBER is restricted
        return try {
            val cursor = contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(android.provider.CallLog.Calls.NUMBER),
                "${android.provider.CallLog.Calls.TYPE} = ?",
                arrayOf(android.provider.CallLog.Calls.INCOMING_TYPE.toString()),
                "${android.provider.CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(0)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading call log", e)
            null
        }
    }

    private fun getOutgoingNumberFromCallLog(): String? {
        return try {
            val cursor = contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(android.provider.CallLog.Calls.NUMBER),
                "${android.provider.CallLog.Calls.TYPE} = ?",
                arrayOf(android.provider.CallLog.Calls.OUTGOING_TYPE.toString()),
                "${android.provider.CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(0)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading call log", e)
            null
        }
    }

    private fun handleLiveCall(phoneNumber: String, typeLabel: String) {
        serviceScope.launch {
            try {
                // First check if Caller ID feature is enabled
                val userPrefs = UserPreferencesRepository(this@CallMonitorService)
                val callerIdEnabled = userPrefs.callerIdEnabled.first()
                
                if (!callerIdEnabled) {
                    Log.d(TAG, "Caller ID is disabled, skipping overlay")
                    return@launch
                }
                
                val database = AppDatabase.getDatabase(this@CallMonitorService)
                val salesDao = database.salesDao()
                val normalizedNumber = CallLogRepository.normalizePhoneNumber(phoneNumber)
                
                val people = salesDao.getAllPeopleSync()
                val matchedPerson = people.find { 
                    CallLogRepository.normalizePhoneNumber(it.phone) == normalizedNumber ||
                    CallLogRepository.normalizePhoneNumber(it.alternativePhone) == normalizedNumber
                }
                
                if (matchedPerson != null) {
                    val customItems = salesDao.getAllCustomItemsOnce()
                    val details = buildPersonDetails(matchedPerson, customItems)
                    val defaultCountry = userPrefs.defaultCountry.first()
                    
                    if (CallerIdOverlayService.canShowOverlay(this@CallMonitorService)) {
                        val stageLabel = customItems.find { it.id == matchedPerson.stageId && it.type == "STAGE" }?.label ?: matchedPerson.stageId
                        val segmentLabel = customItems.find { it.id == matchedPerson.segmentId && it.type == "SEGMENT" }?.label ?: matchedPerson.segmentId
                        val priorityLabel = com.example.salescrm.data.Priority.fromId(matchedPerson.priorityId).label
                        val formattedBudget = if (matchedPerson.budget.isNotBlank()) com.example.salescrm.data.formatBudget(matchedPerson.budget) else ""
                        val note = matchedPerson.note

                        showCallerIdOverlay(
                            matchedPerson.id, matchedPerson.name, phoneNumber, typeLabel, details,
                            formattedBudget, stageLabel, priorityLabel, segmentLabel, note, defaultCountry
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling live call", e)
            }
        }
    }

    private fun handleCallEnd() {
        // Hide overlay
        val hideIntent = Intent(this, CallerIdOverlayService::class.java).apply {
            action = CallerIdOverlayService.ACTION_HIDE
        }
        startService(hideIntent)
        
        // Show post-call notification
        serviceScope.launch {
            delay(2000) // Wait for call log to update
            showPostCallNotification()
        }
    }

    private suspend fun showPostCallNotification() {
        try {
            val database = AppDatabase.getDatabase(this)
            val salesDao = database.salesDao()
            val repository = CallLogRepository(this)
            
            val latestCalls = repository.fetchCallLog(
                fromDate = java.time.LocalDate.now(),
                toDate = java.time.LocalDate.now()
            )
            
            if (latestCalls.isEmpty()) return
            
            val lastCall = latestCalls.maxByOrNull { it.timestamp } ?: return
            
            val now = LocalDateTime.now()
            val secondsDiff = java.time.Duration.between(lastCall.timestamp, now).seconds
            if (Math.abs(secondsDiff) > 60) return

            val people = salesDao.getAllPeopleSync()
            val normalizedNumber = CallLogRepository.normalizePhoneNumber(lastCall.phoneNumber)
            val matchedPerson = people.find { 
                CallLogRepository.normalizePhoneNumber(it.phone) == normalizedNumber ||
                CallLogRepository.normalizePhoneNumber(it.alternativePhone) == normalizedNumber
            }

            if (matchedPerson != null) {
                val customItems = salesDao.getAllCustomItemsOnce()
                val details = buildPersonDetails(matchedPerson, customItems)
                val userPrefs = UserPreferencesRepository(this)
                val defaultCountry = userPrefs.defaultCountry.first()
                showCallerIdNotification(matchedPerson.id, matchedPerson.name, lastCall.phoneNumber, lastCall.callType.label, details, lastCall.duration, defaultCountry)
            } else {
                showAddLeadNotification(lastCall.phoneNumber, lastCall.contactName, lastCall.callType.label, lastCall.duration)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing post-call notification", e)
        }
    }

    private fun buildPersonDetails(person: com.example.salescrm.data.local.PersonEntity, customItems: List<com.example.salescrm.data.local.CustomItemEntity>): String {
        val status = if (person.isInPipeline) "In Pipeline" else "Contact"
        val stageLabel = customItems.find { it.id == person.stageId && it.type == "STAGE" }?.label ?: person.stageId
        val segmentLabel = customItems.find { it.id == person.segmentId && it.type == "SEGMENT" }?.label ?: person.segmentId
        val priorityLabel = com.example.salescrm.data.Priority.fromId(person.priorityId).label
        val formattedBudget = if (person.budget.isNotBlank()) com.example.salescrm.data.formatBudget(person.budget) else "N/A"
        val note = if (person.note.isNotBlank()) person.note else "No main note"

        return """
            $status | $stageLabel
            Priority: $priorityLabel | Segment: $segmentLabel
            Budget: $formattedBudget
            Note: $note
        """.trimIndent()
    }

    private fun showCallerIdOverlay(
        personId: Int,
        name: String,
        phone: String,
        type: String,
        details: String,
        budget: String,
        stage: String,
        priorityLabel: String,
        segment: String,
        note: String,
        defaultCountry: String
    ) {
        val intent = Intent(this, CallerIdOverlayService::class.java).apply {
            action = CallerIdOverlayService.ACTION_SHOW
            putExtra(CallerIdOverlayService.EXTRA_PERSON_ID, personId)
            putExtra(CallerIdOverlayService.EXTRA_NAME, name)
            putExtra(CallerIdOverlayService.EXTRA_PHONE, phone)
            putExtra(CallerIdOverlayService.EXTRA_TYPE, type)
            putExtra(CallerIdOverlayService.EXTRA_DETAILS, details)
            putExtra(CallerIdOverlayService.EXTRA_BUDGET, budget)
            putExtra(CallerIdOverlayService.EXTRA_STAGE, stage)
            putExtra(CallerIdOverlayService.EXTRA_PRIORITY_LABEL, priorityLabel)
            putExtra(CallerIdOverlayService.EXTRA_SEGMENT, segment)
            putExtra(CallerIdOverlayService.EXTRA_NOTE, note)
            putExtra(CallerIdOverlayService.EXTRA_DEFAULT_COUNTRY, defaultCountry)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun showCallerIdNotification(
        personId: Int,
        name: String,
        phone: String,
        type: String,
        details: String,
        duration: Long,
        defaultCountry: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_person_id", personId)
            putExtra("action", "open_profile")
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this, 
            personId, 
            contentIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "call_tracking_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("👤 $name")
            .setContentText("$type Ended • Tap to open profile")
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(30000)

        notificationManager.notify(50000, builder.build())
    }

    private fun showAddLeadNotification(phone: String, name: String?, type: String, duration: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("prefill_phone", phone)
            putExtra("prefill_name", name)
            putExtra("action", "add_lead")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 
            phone.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (name != null) "Add $name in CRM?" else "Add $phone in CRM?"
        val durationStr = CallLogRepository.formatDuration(duration)
        val content = "Just now - $type call 📞 $durationStr • Tap to add as lead"

        val builder = NotificationCompat.Builder(this, "call_tracking_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(50001, builder.build())
    }
}
