package com.example.salescrm.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.salescrm.MainActivity
import com.example.salescrm.R
import com.example.salescrm.data.CallLogRepository
import com.example.salescrm.data.CrmRepository
import com.example.salescrm.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import com.example.salescrm.data.UserPreferencesRepository

class CallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallReceiver"
        private const val CHANNEL_ID = "call_tracking_channel"
        private const val NOTIFICATION_ID_CALL_ID = 50000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        // 1. Outgoing Call
        if (action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            if (!phoneNumber.isNullOrBlank()) {
                handleLiveCall(context, phoneNumber, "Outgoing Call")
            }
            return
        }

        // 2. Incoming Call / Call Ended
        if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d(TAG, "Phone state changed: $state")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                // Incoming Call
                val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                if (!phoneNumber.isNullOrBlank()) {
                    handleLiveCall(context, phoneNumber, "Incoming Call")
                }
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended - wait a bit for system call log to update
                // Using goAsync for potentially slightly longer operation
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        delay(2000)
                        handleCallEnd(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun handleLiveCall(context: Context, phoneNumber: String, typeLabel: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val salesDao = database.salesDao()
                val normalizedNumber = CallLogRepository.normalizePhoneNumber(phoneNumber)
                
                // Optimized single query check
                val people = salesDao.getAllPeopleSync()
                val matchedPerson = people.find { 
                    CallLogRepository.normalizePhoneNumber(it.phone) == normalizedNumber ||
                    CallLogRepository.normalizePhoneNumber(it.alternativePhone) == normalizedNumber
                }
                
                if (matchedPerson != null) {
                    val customItems = salesDao.getAllCustomItemsOnce()
                    val details = buildPersonDetails(matchedPerson, customItems)
                    
                    // Extract structured details
                    val stageLabel = customItems.find { it.id == matchedPerson.stageId && it.type == "STAGE" }?.label ?: matchedPerson.stageId
                    val segmentLabel = customItems.find { it.id == matchedPerson.segmentId && it.type == "SEGMENT" }?.label ?: matchedPerson.segmentId
                    val priorityLabel = com.example.salescrm.data.Priority.fromId(matchedPerson.priorityId).label
                    val formattedBudget = if (matchedPerson.budget.isNotBlank()) com.example.salescrm.data.formatBudget(matchedPerson.budget) else ""
                    val note = matchedPerson.note
                    
                    val userPrefs = UserPreferencesRepository(context)
                    val defaultCountry = userPrefs.defaultCountry.first()
                    
                    // Show floating overlay banner if permission is granted
                    if (CallerIdOverlayService.canShowOverlay(context)) {
                        showCallerIdOverlay(
                            context, matchedPerson.id, matchedPerson.name, phoneNumber, typeLabel, details,
                            formattedBudget, stageLabel, priorityLabel, segmentLabel, note, defaultCountry
                        )
                    } else {
                        // Fallback to notification if overlay permission is not granted
                        showCallerIdNotification(context, matchedPerson.id, matchedPerson.name, phoneNumber, typeLabel, details, isOngoing = true, 0L, defaultCountry)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling live call", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
    
    private fun showCallerIdOverlay(
        context: Context,
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
        val intent = Intent(context, CallerIdOverlayService::class.java).apply {
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
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private suspend fun handleCallEnd(context: Context) {
        // Hide the overlay banner if it's showing
        hideCallerIdOverlay(context)
        
        val database = AppDatabase.getDatabase(context)
        val salesDao = database.salesDao()
        val repository = CallLogRepository(context)
        
        // Get the latest call from system call log
        val latestCalls = repository.fetchCallLog(
            fromDate = java.time.LocalDate.now(),
            toDate = java.time.LocalDate.now()
        )
        
        if (latestCalls.isEmpty()) return
        
        // The most recent call is usually the first one from fetchCallLog if sorted by timestamp
        val lastCall = latestCalls.maxByOrNull { it.timestamp } ?: return
        
        // Ensure the call was very recent (within last 30 seconds)
        val now = LocalDateTime.now()
        val secondsDiff = java.time.Duration.between(lastCall.timestamp, now).seconds
        if (Math.abs(secondsDiff) > 60) {
            Log.d(TAG, "Last call was too long ago: $secondsDiff seconds ago")
            return
        }

        val people = salesDao.getAllPeopleSync()
        val normalizedNumber = CallLogRepository.normalizePhoneNumber(lastCall.phoneNumber)
        val matchedPerson = people.find { 
            CallLogRepository.normalizePhoneNumber(it.phone) == normalizedNumber ||
            CallLogRepository.normalizePhoneNumber(it.alternativePhone) == normalizedNumber
        }

        if (matchedPerson != null) {
            val customItems = salesDao.getAllCustomItemsOnce()
            val details = buildPersonDetails(matchedPerson, customItems)
            val userPrefs = UserPreferencesRepository(context)
            val defaultCountry = userPrefs.defaultCountry.first()
            showCallerIdNotification(context, matchedPerson.id, matchedPerson.name, lastCall.phoneNumber, lastCall.callType.label, details, isOngoing = false, lastCall.duration, defaultCountry)
        } else {
            showAddLeadNotification(context, lastCall.phoneNumber, lastCall.contactName, lastCall.callType.label, lastCall.duration)
        }
    }
    
    private fun hideCallerIdOverlay(context: Context) {
        val intent = Intent(context, CallerIdOverlayService::class.java).apply {
            action = CallerIdOverlayService.ACTION_HIDE
        }
        context.startService(intent)
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

    private fun showCallerIdNotification(
        context: Context, 
        personId: Int, 
        name: String, 
        phone: String, 
        type: String,
        details: String,
        isOngoing: Boolean,
        duration: Long = 0,
        defaultCountry: String = "US"
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        // Intent for full screen activity (kept for pending intent but not used as fullScreenIntent)
        val fullScreenIntent = Intent(context, PostCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
            putExtra(PostCallActivity.EXTRA_PERSON_ID, personId)
            putExtra(PostCallActivity.EXTRA_NAME, name)
            putExtra(PostCallActivity.EXTRA_PHONE, phone)
            putExtra(PostCallActivity.EXTRA_TYPE, type)
            putExtra(PostCallActivity.EXTRA_DURATION, duration)
            putExtra(PostCallActivity.EXTRA_DEFAULT_COUNTRY, defaultCountry)
        }
        
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            personId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content intent (if user clicks the notification normally)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_person_id", personId)
            putExtra("action", "open_profile")
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context, 
            personId, 
            contentIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isOngoing) "$type • Tap to open profile" else "$type Ended • Tap to open profile"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("👤 $name")
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setPriority(NotificationCompat.PRIORITY_HIGH) 
            .setCategory(NotificationCompat.CATEGORY_CALL)
            // .setFullScreenIntent(fullScreenPendingIntent, true) // Removed to prevent banner/heads-up takeover if requested
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(30000) // Auto dismiss after 30s

        notificationManager.notify(NOTIFICATION_ID_CALL_ID, builder.build())
    }

    private fun showAddLeadNotification(
        context: Context, 
        phone: String, 
        name: String?, 
        type: String,
        duration: Long
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("prefill_phone", phone)
            putExtra("prefill_name", name)
            putExtra("action", "add_lead")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            phone.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (name != null) "Add $name in CRM?" else "Add $phone in CRM?"
        
        // Match the request format: "Just now - [Type] call {icon} [Duration]"
        val durationStr = CallLogRepository.formatDuration(duration)
        val content = "Just now - $type call 📞 $durationStr • Tap to add as lead"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID_CALL_ID + 1, builder.build())
    }

    private fun createChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Tracking",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows caller identification and add lead alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
