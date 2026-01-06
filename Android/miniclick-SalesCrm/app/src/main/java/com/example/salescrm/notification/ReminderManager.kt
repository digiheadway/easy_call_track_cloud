package com.example.salescrm.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.salescrm.data.Priority
import com.example.salescrm.data.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * ReminderManager - Manages scheduling and canceling task reminders
 * 
 * Notification Types:
 * 1. Advance Notification (5 min before) - Silent, with snooze option (30 min reschedule)
 * 2. Main Reminder:
 *    - NORMAL: Ring for 5 seconds
 *    - HIGH: Ring for 15 seconds at 70% volume, full screen
 *    - SUPER_HIGH: Ring for 1 minute full volume, sticky notification
 */
class ReminderManager(private val context: Context) {
    
    companion object {
        const val TAG = "ReminderManager"
        
        // Notification Channel IDs
        const val CHANNEL_SILENT = "reminder_silent"           // For 5-min advance notification
        const val CHANNEL_LOW_PRIORITY = "reminder_low"        // Normal priority
        const val CHANNEL_MEDIUM_PRIORITY = "reminder_medium"  // High priority
        const val CHANNEL_HIGH_PRIORITY = "reminder_high"      // Super high priority
        
        // Intent extras keys
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_DESCRIPTION = "task_description"
        const val EXTRA_PRIORITY = "priority"
        const val EXTRA_LINKED_PERSON_ID = "linked_person_id"
        const val EXTRA_LINKED_PERSON_NAME = "linked_person_name"
        const val EXTRA_LINKED_PERSON_PHONE = "linked_person_phone"
        const val EXTRA_IS_ADVANCE_REMINDER = "is_advance_reminder"
        const val EXTRA_TASK_TYPE = "task_type"
        const val EXTRA_DUE_TIME = "due_time"
        const val EXTRA_DEFAULT_COUNTRY = "default_country"
        
        // Advance reminder time (5 minutes before main reminder)
        const val ADVANCE_REMINDER_MINUTES = 5
        
        // Snooze duration (30 minutes - reschedules task)
        const val SNOOZE_DURATION_MINUTES = 30
        
        // Request code base for pending intents
        private const val REQUEST_CODE_BASE = 10000
        private const val REQUEST_CODE_ADVANCE_BASE = 30000
    }
    
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager: NotificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for different priority levels and advance notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Silent Channel - For 5-min advance notification
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                "Advance Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent notification 5 minutes before task"
                enableVibration(false)
                setSound(null, null)
            }
            
            // Low Priority Channel (Normal) - Sound/Vib managed by Service
            val lowChannel = NotificationChannel(
                CHANNEL_LOW_PRIORITY,
                "Normal Priority Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notification managed by app service"
                enableVibration(false)
                setSound(null, null)
            }
            
            // Medium Priority Channel (High) - Sound/Vib managed by Service
            val mediumChannel = NotificationChannel(
                CHANNEL_MEDIUM_PRIORITY,
                "High Priority Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification managed by app service"
                enableVibration(false)
                setSound(null, null)
            }
            
            // High Priority Channel (Super High) - Sound/Vib managed by Service
            val highChannel = NotificationChannel(
                CHANNEL_HIGH_PRIORITY,
                "Super High Priority Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification managed by app service"
                enableVibration(false)
                setSound(null, null)
                setBypassDnd(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(silentChannel, lowChannel, mediumChannel, highChannel)
            )
            
            Log.d(TAG, "Notification channels created")
        }
    }
    
    /**
     * Schedule reminders for a task:
     * - Advance silent notification 5 minutes before (if task has reminder and due time)
     * - Main reminder at the scheduled time
     */
    fun scheduleReminder(task: Task, personName: String? = null, personPhone: String? = null, defaultCountry: String = "US") {
        if (!task.showReminder || task.dueTime == null) {
            Log.d(TAG, "Skipping reminder for task ${task.id}: showReminder=${task.showReminder}, dueTime=${task.dueTime}")
            return
        }
        
        // Calculate main reminder time
        val dueDateTime = task.dueDate.atTime(task.dueTime)
        val mainReminderDateTime = dueDateTime.minusMinutes(task.reminderMinutesBefore.toLong())
        val mainReminderTimeMillis = mainReminderDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        
        // Check if main reminder time is in the past
        if (mainReminderTimeMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Reminder time for task ${task.id} is in the past, skipping")
            return
        }
        
        // Calculate advance reminder time (5 minutes before main reminder)
        val advanceReminderDateTime = mainReminderDateTime.minusMinutes(ADVANCE_REMINDER_MINUTES.toLong())
        val advanceReminderTimeMillis = advanceReminderDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        
        val priority = Priority.fromId(task.priorityId)
        
        // Schedule advance reminder if it's in the future
        if (advanceReminderTimeMillis > System.currentTimeMillis()) {
            scheduleAdvanceReminder(task, advanceReminderTimeMillis, personName, personPhone, defaultCountry)
        }
        
        // Schedule main reminder
        scheduleMainReminder(task, mainReminderTimeMillis, personName, personPhone, defaultCountry)
    }
    
    private fun scheduleAdvanceReminder(task: Task, reminderTimeMillis: Long, personName: String?, personPhone: String?, defaultCountry: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_ADVANCE_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_DESCRIPTION, task.description)
            putExtra(EXTRA_PRIORITY, Priority.fromId(task.priorityId).name)
            putExtra(EXTRA_TASK_TYPE, task.type.label)
            putExtra(EXTRA_DUE_TIME, task.dueTime?.toString())
            putExtra(EXTRA_DEFAULT_COUNTRY, defaultCountry)
            putExtra(EXTRA_IS_ADVANCE_REMINDER, true)
            task.linkedPersonId?.let { putExtra(EXTRA_LINKED_PERSON_ID, it) }
            personName?.let { putExtra(EXTRA_LINKED_PERSON_NAME, it) }
            personPhone?.let { putExtra(EXTRA_LINKED_PERSON_PHONE, it) }
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ADVANCE_BASE + task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        scheduleAlarm(pendingIntent, reminderTimeMillis, "advance reminder", task.id)
    }
    
    private fun scheduleMainReminder(task: Task, reminderTimeMillis: Long, personName: String?, personPhone: String?, defaultCountry: String) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_DESCRIPTION, task.description)
            putExtra(EXTRA_PRIORITY, Priority.fromId(task.priorityId).name)
            putExtra(EXTRA_TASK_TYPE, task.type.label)
            putExtra(EXTRA_DUE_TIME, task.dueTime?.toString())
            putExtra(EXTRA_DEFAULT_COUNTRY, defaultCountry)
            putExtra(EXTRA_IS_ADVANCE_REMINDER, false)
            task.linkedPersonId?.let { putExtra(EXTRA_LINKED_PERSON_ID, it) }
            personName?.let { putExtra(EXTRA_LINKED_PERSON_NAME, it) }
            personPhone?.let { putExtra(EXTRA_LINKED_PERSON_PHONE, it) }
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        scheduleAlarm(pendingIntent, reminderTimeMillis, "main reminder", task.id)
    }
    
    private fun scheduleAlarm(pendingIntent: PendingIntent, timeMillis: Long, type: String, taskId: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact $type for task $taskId")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        timeMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact $type for task $taskId (no exact alarm permission)")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact $type for task $taskId")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule $type for task $taskId: ${e.message}")
        }
    }
    
    /**
     * Cancel all reminders for a task (both advance and main)
     */
    fun cancelReminder(taskId: Int) {
        // Cancel main reminder
        val mainIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SHOW_REMINDER
        }
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + taskId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(mainPendingIntent)
        
        // Cancel advance reminder
        val advanceIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_ADVANCE_REMINDER
        }
        val advancePendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ADVANCE_BASE + taskId,
            advanceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(advancePendingIntent)
        
        Log.d(TAG, "Cancelled all reminders for task $taskId")
    }
    
    /**
     * Schedule reminders for all pending tasks
     */
    fun scheduleAllReminders(tasks: List<Task>, defaultCountry: String = "US", getPerson: (Int) -> Pair<String?, String?>? = { null }) {
        val today = LocalDate.now()
        val now = LocalTime.now()
        
        tasks.filter { task ->
            task.showReminder &&
            task.dueTime != null &&
            task.status == com.example.salescrm.data.TaskStatus.PENDING &&
            (task.dueDate.isAfter(today) || 
             (task.dueDate == today && task.dueTime!!.isAfter(now)))
        }.forEach { task ->
            val personInfo = task.linkedPersonId?.let { getPerson(it) }
            scheduleReminder(task, personInfo?.first, personInfo?.second, defaultCountry)
        }
        
        Log.d(TAG, "Scheduled reminders for ${tasks.size} tasks")
    }
    
    /**
     * Cancel all reminders
     */
    fun cancelAllReminders(tasks: List<Task>) {
        tasks.forEach { task ->
            cancelReminder(task.id)
        }
        Log.d(TAG, "Cancelled all reminders")
    }
    
    /**
     * Get the notification channel ID for a priority level
     */
    fun getChannelIdForPriority(priority: Priority): String {
        return when (priority) {
            Priority.SUPER_HIGH -> CHANNEL_HIGH_PRIORITY
            Priority.HIGH -> CHANNEL_MEDIUM_PRIORITY
            Priority.NORMAL -> CHANNEL_LOW_PRIORITY
        }
    }
    
    /**
     * Check if we have notification permission
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    /**
     * Check if we can schedule exact alarms
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
