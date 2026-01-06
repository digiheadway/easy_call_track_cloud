package com.example.salescrm.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.salescrm.MainActivity
import com.example.salescrm.R
import com.example.salescrm.data.Priority
import com.example.salescrm.data.SampleData
import com.example.salescrm.data.CallLogRepository
import com.example.salescrm.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * ReminderService - Foreground service for task reminders
 * 
 * Handles different notification types:
 * 1. Advance Notification (5 min before) - Silent with snooze option (30 min reschedule)
 * 2. Main Reminder based on priority:
 *    - NORMAL: Ring for 5 seconds, normal notification
 *    - HIGH: Ring for 15 seconds at 70% device volume, full-screen
 *    - SUPER_HIGH: Ring for 1 minute at full device volume, sticky notification
 * 
 * Volume handling: Uses percentage of device volume, not absolute volume changes
 */
class ReminderService : Service() {
    
    companion object {
        const val TAG = "ReminderService"
        const val ACTION_START_REMINDER = "com.example.salescrm.ACTION_START_REMINDER"
        const val ACTION_START_ADVANCE_REMINDER = "com.example.salescrm.ACTION_START_ADVANCE_REMINDER"
        const val ACTION_STOP_REMINDER = "com.example.salescrm.ACTION_STOP_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.salescrm.ACTION_SNOOZE_REMINDER"
        const val ACTION_SNOOZE_30_REMINDER = "com.example.salescrm.ACTION_SNOOZE_30_REMINDER"
        
        private const val NOTIFICATION_ID_BASE = 20000
        private const val NOTIFICATION_ID_ADVANCE_BASE = 40000
        
        // Ring durations based on priority
        private const val NORMAL_PRIORITY_DURATION_MS = 5000L    // 5 seconds
        private const val HIGH_PRIORITY_DURATION_MS = 15000L     // 15 seconds
        private const val SUPER_HIGH_PRIORITY_DURATION_MS = 60000L // 1 minute
        
        // Volume levels (as percentage of device max volume)
        private const val NORMAL_VOLUME_PERCENT = 0.5f    // 50%
        private const val HIGH_VOLUME_PERCENT = 0.7f       // 70%
        private const val SUPER_HIGH_VOLUME_PERCENT = 1.0f // 100%
        
        private const val SNOOZE_DURATION_MS = 5 * 60 * 1000L // 5 minutes (short snooze)
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentTaskId: Int = -1
    private var currentPriority: Priority = Priority.NORMAL
    private var isPlaying = false
    private var currentPersonId: Int = -1
    private var currentPersonName: String? = null
    private var currentPersonPhone: String? = null
    private var currentTaskDescription: String = ""
    private var currentTaskType: String? = null
    private var currentDueTime: String? = null
    private var defaultCountry: String = "US"
    private var isAdvanceReminder = false
    
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    private lateinit var repository: com.example.salescrm.data.CrmRepository
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ReminderService created")
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        // Initialize Repository
        val database = com.example.salescrm.data.local.AppDatabase.getDatabase(applicationContext)
        repository = com.example.salescrm.data.CrmRepository(database.salesDao())
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_ADVANCE_REMINDER -> {
                extractIntentData(intent)
                isAdvanceReminder = true
                startAdvanceReminder()
            }
            ACTION_START_REMINDER -> {
                extractIntentData(intent)
                isAdvanceReminder = false
                startMainReminder()
            }
            ACTION_STOP_REMINDER -> {
                stopReminder()
            }
            ACTION_SNOOZE_REMINDER -> {
                val taskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
                snoozeReminder(taskId)
            }
            ACTION_SNOOZE_30_REMINDER -> {
                val taskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
                snooze30AndRescheduleTask(taskId)
            }
        }
        
        return START_NOT_STICKY
    }
    
    private fun extractIntentData(intent: Intent) {
        currentTaskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
        currentTaskDescription = intent.getStringExtra(ReminderManager.EXTRA_TASK_DESCRIPTION) ?: "Task Reminder"
        val priorityName = intent.getStringExtra(ReminderManager.EXTRA_PRIORITY) ?: Priority.NORMAL.name
        currentPriority = try { Priority.valueOf(priorityName) } catch (e: Exception) { Priority.NORMAL }
        currentPersonId = intent.getIntExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, -1)
        currentPersonName = intent.getStringExtra(ReminderManager.EXTRA_LINKED_PERSON_NAME)
        currentPersonPhone = intent.getStringExtra(ReminderManager.EXTRA_LINKED_PERSON_PHONE)
        currentTaskType = intent.getStringExtra(ReminderManager.EXTRA_TASK_TYPE)
        currentDueTime = intent.getStringExtra(ReminderManager.EXTRA_DUE_TIME)
        defaultCountry = intent.getStringExtra(ReminderManager.EXTRA_DEFAULT_COUNTRY) ?: "US"
        
        // Try to get person info from SampleData if not provided
        if (currentPersonId != -1 && currentPersonName == null) {
            // SampleData Removed. Rely on Intent extras being correct.
            // If data is critical, we would need to fetch it async but this method returns Unit synchronously.
        }
    }
    
    /**
     * Start advance reminder - silent notification with snooze option
     */
    private fun startAdvanceReminder() {
        Log.d(TAG, "Starting advance reminder for task $currentTaskId")
        
        val notification = createAdvanceNotification()
        startForeground(NOTIFICATION_ID_ADVANCE_BASE + currentTaskId, notification)
        
        // No sound for advance reminder - it's silent
        // Just show the notification and keep service alive briefly
        handler.postDelayed({
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }, 1000)
    }
    
    /**
     * Start main reminder with sound based on priority
     */
    private fun startMainReminder() {
        Log.d(TAG, "Starting main reminder for task $currentTaskId with priority ${currentPriority.name}")
        
        // Cancel any advance notification for this task
        notificationManager.cancel(NOTIFICATION_ID_ADVANCE_BASE + currentTaskId)
        
        // Create and show notification first (required for foreground service)
        val notification = createMainNotification()
        startForeground(NOTIFICATION_ID_BASE + currentTaskId, notification)
        
        // Start playing sound based on priority
        when (currentPriority) {
            Priority.NORMAL -> playNormalPrioritySound()
            Priority.HIGH -> playHighPrioritySound()
            Priority.SUPER_HIGH -> playSuperHighPrioritySound()
        }
    }
    
    /**
     * Create silent advance notification (5 min before task)
     */
    private fun createAdvanceNotification(): Notification {
        // For advance notifications, we want clicking the notification to open the task directly in CrmApp 
        // bypassing the full-screen ReminderActivity.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderManager.EXTRA_TASK_ID, currentTaskId)
            putExtra("open_task", true)
        }
        val directContentIntent = PendingIntent.getActivity(
            this,
            currentTaskId + 7000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val snoozePendingIntent = createSnooze30PendingIntent()
        val dismissPendingIntent = createDismissPendingIntent()
        
        val notificationText = buildNotificationText(isAdvance = true)
        
        val builder = NotificationCompat.Builder(this, ReminderManager.CHANNEL_SILENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📋 Upcoming: ${currentTaskType ?: "Task"}")
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(directContentIntent)
            .setAutoCancel(false)
            .setOngoing(currentPriority == Priority.SUPER_HIGH) // Sticky for super high priority
            .setSilent(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Snooze 30 min",
                snoozePendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Dismiss",
                dismissPendingIntent
            )
        
        // Add person contact actions if available
        addPersonActions(builder)
        
        return builder.build()
    }
    
    /**
     * Create main notification with full details
     */
    private fun createMainNotification(): Notification {
        val reminderManager = ReminderManager(this)
        val channelId = reminderManager.getChannelIdForPriority(currentPriority)
        
        val contentIntent = createContentIntent()
        val snoozePendingIntent = createSnooze30PendingIntent()
        val dismissPendingIntent = createDismissPendingIntent()
        
        val notificationText = buildNotificationText(isAdvance = false)
        
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getPriorityTitle())
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(getPriorityForNotification())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOngoing(currentPriority == Priority.SUPER_HIGH) // Sticky for super high priority
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Snooze 30 min",
                snoozePendingIntent
            )
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Dismiss",
                dismissPendingIntent
            )
        
        // Add person contact actions if available
        addPersonActions(builder)
        
        // For high/super high priority, make it a heads-up notification with full screen intent
        if (currentPriority == Priority.HIGH || currentPriority == Priority.SUPER_HIGH) {
            builder.setFullScreenIntent(contentIntent, true)
        }
        
        return builder.build()
    }
    
    private fun buildNotificationText(isAdvance: Boolean): String {
        val parts = mutableListOf<String>()
        
        parts.add(currentTaskDescription)
        
        if (isAdvance) {
            parts.add("\n⏰ Due in 5 minutes")
        }
        
        currentDueTime?.let {
            try {
                val time = LocalTime.parse(it)
                parts.add("🕐 ${time.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))}")
            } catch (e: Exception) { }
        }
        
        currentPersonName?.let {
            parts.add("👤 $it")
        }
        
        currentPersonPhone?.let {
            parts.add("📞 $it")
        }
        
        return parts.joinToString("\n")
    }
    
    private fun addPersonActions(builder: NotificationCompat.Builder) {
        if (currentPersonPhone != null && currentPersonId != -1) {
            // Add Call action
            val dialPhone = CallLogRepository.formatPhoneForDialer(currentPersonPhone, defaultCountry)
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${dialPhone}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val callPendingIntent = PendingIntent.getActivity(
                this,
                currentTaskId + 5000,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                "📞 Call",
                callPendingIntent
            )
            
            // Add WhatsApp action
            // Note: For notification actions, we can't use Intent.createChooser() because 
            // PendingIntent requires a direct intent. However, if the user has multiple WhatsApp 
            // apps, Android will show its own app disambiguation dialog when the wa.me URL is opened.
            val cleanPhone = currentPersonPhone?.replace(Regex("[^0-9+]"), "")?.let { num ->
                if (num.startsWith("+")) num.removePrefix("+") else num
            } ?: ""
            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$cleanPhone")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val whatsappPendingIntent = PendingIntent.getActivity(
                this,
                currentTaskId + 6000,
                whatsappIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                "💬 WhatsApp",
                whatsappPendingIntent
            )
        }
    }
    
    private fun createContentIntent(): PendingIntent {
        val intent = Intent(this, ReminderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderManager.EXTRA_TASK_ID, currentTaskId)
            putExtra(ReminderManager.EXTRA_TASK_DESCRIPTION, currentTaskDescription)
            putExtra(ReminderManager.EXTRA_PRIORITY, currentPriority.name)
            putExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, currentPersonId)
            putExtra(ReminderManager.EXTRA_LINKED_PERSON_NAME, currentPersonName)
            putExtra(ReminderManager.EXTRA_LINKED_PERSON_PHONE, currentPersonPhone)
            putExtra(ReminderManager.EXTRA_TASK_TYPE, currentTaskType)
            putExtra(ReminderManager.EXTRA_DUE_TIME, currentDueTime)
        }
        return PendingIntent.getActivity(
            this,
            currentTaskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createSnooze30PendingIntent(): PendingIntent {
        val intent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SNOOZE_30_REMINDER
            putExtra(ReminderManager.EXTRA_TASK_ID, currentTaskId)
        }
        return PendingIntent.getBroadcast(
            this,
            currentTaskId + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun createDismissPendingIntent(): PendingIntent {
        val intent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_DISMISS_REMINDER
            putExtra(ReminderManager.EXTRA_TASK_ID, currentTaskId)
        }
        return PendingIntent.getBroadcast(
            this,
            currentTaskId + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun getPriorityTitle(): String {
        return when (currentPriority) {
            Priority.SUPER_HIGH -> "⚠️ URGENT: ${currentTaskType ?: "Task"}"
            Priority.HIGH -> "📋 ${currentTaskType ?: "Task"} Reminder"
            Priority.NORMAL -> "📝 ${currentTaskType ?: "Task"} Reminder"
        }
    }
    
    private fun getPriorityForNotification(): Int {
        return when (currentPriority) {
            Priority.SUPER_HIGH -> NotificationCompat.PRIORITY_MAX
            Priority.HIGH -> NotificationCompat.PRIORITY_HIGH
            Priority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
        }
    }
    
    /**
     * Normal priority: Ring for 5 seconds at 50% of device volume
     */
    private fun playNormalPrioritySound() {
        Log.d(TAG, "Playing normal priority sound (5 seconds)")
        isPlaying = true
        
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val targetVolume = calculateVolume(NORMAL_VOLUME_PERCENT)
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@ReminderService, alarmUri)
                setVolume(NORMAL_VOLUME_PERCENT, NORMAL_VOLUME_PERCENT)
                prepare()
                start()
            }
            
            // Vibrate once
            vibrateOnce()
            
            // Stop after 5 seconds
            handler.postDelayed({
                if (isPlaying && currentPriority == Priority.NORMAL) {
                    stopReminder()
                }
            }, NORMAL_PRIORITY_DURATION_MS)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing normal priority sound: ${e.message}")
            stopReminder()
        }
    }
    
    /**
     * High priority: Ring for 15 seconds at 70% of device volume, full-screen notification
     */
    private fun playHighPrioritySound() {
        Log.d(TAG, "Playing high priority sound (15 seconds at 70% volume)")
        isPlaying = true
        
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@ReminderService, alarmUri)
                setVolume(HIGH_VOLUME_PERCENT, HIGH_VOLUME_PERCENT)
                isLooping = true
                prepare()
                start()
            }
            
            // Vibrate pattern
            startVibrationPattern()
            
            // Stop after 15 seconds
            handler.postDelayed({
                if (isPlaying && currentPriority == Priority.HIGH) {
                    stopReminder()
                }
            }, HIGH_PRIORITY_DURATION_MS)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing high priority sound: ${e.message}")
            stopReminder()
        }
    }
    
    /**
     * Super high priority: Ring for 1 minute at 100% of device volume, sticky notification
     */
    private fun playSuperHighPrioritySound() {
        Log.d(TAG, "Playing super high priority sound (1 minute at full volume)")
        isPlaying = true
        
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@ReminderService, alarmUri)
                setVolume(SUPER_HIGH_VOLUME_PERCENT, SUPER_HIGH_VOLUME_PERCENT)
                isLooping = true
                prepare()
                start()
            }
            
            // Continuous vibration
            startContinuousVibration()
            
            // Stop after 1 minute
            handler.postDelayed({
                if (isPlaying && currentPriority == Priority.SUPER_HIGH) {
                    // Don't fully stop - keep notification but stop sound
                    stopPlayback()
                    // Update notification to show it needs attention
                    val notification = NotificationCompat.Builder(this, ReminderManager.CHANNEL_HIGH_PRIORITY)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("⚠️ URGENT: Needs Attention")
                        .setContentText(currentTaskDescription)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setOngoing(true) // Still sticky
                        .setContentIntent(createContentIntent())
                        .addAction(
                            R.drawable.ic_launcher_foreground,
                            "Snooze 30 min",
                            createSnooze30PendingIntent()
                        )
                        .addAction(
                            R.drawable.ic_launcher_foreground,
                            "Dismiss",
                            createDismissPendingIntent()
                        )
                        .build()
                    notificationManager.notify(NOTIFICATION_ID_BASE + currentTaskId, notification)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }, SUPER_HIGH_PRIORITY_DURATION_MS)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing super high priority sound: ${e.message}")
        }
    }
    
    private fun calculateVolume(percent: Float): Int {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        return (maxVolume * percent).toInt()
    }
    
    private fun vibrateOnce() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(500)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating: ${e.message}")
        }
    }
    
    private fun startVibrationPattern() {
        try {
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration pattern: ${e.message}")
        }
    }
    
    private fun startContinuousVibration() {
        try {
            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting continuous vibration: ${e.message}")
        }
    }
    
    /**
     * Snooze reminder for 5 minutes (short snooze)
     */
    private fun snoozeReminder(taskId: Int) {
        Log.d(TAG, "Snoozing reminder for task $taskId (5 min)")
        
        stopPlayback()
        
        // Schedule a new reminder for 5 minutes later
        handler.postDelayed({
            val intent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
                putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
                putExtra(ReminderManager.EXTRA_TASK_DESCRIPTION, currentTaskDescription)
                putExtra(ReminderManager.EXTRA_PRIORITY, currentPriority.name)
                putExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, currentPersonId)
                putExtra(ReminderManager.EXTRA_LINKED_PERSON_NAME, currentPersonName)
                putExtra(ReminderManager.EXTRA_LINKED_PERSON_PHONE, currentPersonPhone)
            }
            sendBroadcast(intent)
        }, SNOOZE_DURATION_MS)
        
        // Update notification to show snoozed state
        val notification = NotificationCompat.Builder(this, ReminderManager.CHANNEL_SILENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ Reminder Snoozed")
            .setContentText("Will remind again in 5 minutes")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_BASE + taskId, notification)
        
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }
    
    /**
     * Snooze for 30 minutes and reschedule task due time
     */
    private fun snooze30AndRescheduleTask(taskId: Int) {
        Log.d(TAG, "Snoozing and rescheduling task $taskId by 30 minutes")
        
        stopPlayback()
        
        // Find and update the task's due time by 30 minutes
        // Find and update the task's due time by 30 minutes
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Get fresh task from DB
                val allTasks = repository.getAllTasksSync()
                val task = allTasks.find { it.id == taskId }
                
                if (task != null && task.dueTime != null) {
                    val newDueTime = task.dueTime.plusMinutes(30)
                    
                    // Update task with new due time
                    val updatedTask = task.copy(dueTime = newDueTime)
                    repository.saveTask(updatedTask)
                    
                    // Reschedule reminder for the updated task
                    val reminderManager = ReminderManager(this@ReminderService)
                    reminderManager.scheduleReminder(updatedTask, currentPersonName, currentPersonPhone)
                    
                    Log.d(TAG, "Task $taskId rescheduled to ${newDueTime}")
                    
                    // Update notification UI on main thread
                    handler.post {
                        updateSnoozeNotification(taskId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule task: ${e.message}")
            }
        }
        
    }
    
    private fun updateSnoozeNotification(taskId: Int) {
        // Update notification to show snoozed state
        val notification = NotificationCompat.Builder(this, ReminderManager.CHANNEL_SILENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⏰ Task Rescheduled")
            .setContentText("Task postponed by 30 minutes")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setSilent(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_BASE + taskId, notification)
        
        // Cancel advance notification if any
        notificationManager.cancel(NOTIFICATION_ID_ADVANCE_BASE + taskId)
        
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }
    
    private fun stopReminder() {
        Log.d(TAG, "Stopping reminder")
        
        stopPlayback()
        
        // Cancel notifications
        if (currentTaskId != -1) {
            notificationManager.cancel(NOTIFICATION_ID_BASE + currentTaskId)
            notificationManager.cancel(NOTIFICATION_ID_ADVANCE_BASE + currentTaskId)
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun stopPlayback() {
        isPlaying = false
        
        // Stop media player
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player: ${e.message}")
        }
        
        // Stop vibration
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration: ${e.message}")
        }
        
        // Remove any pending handlers
        handler.removeCallbacksAndMessages(null)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ReminderService destroyed")
        stopPlayback()
    }
}
