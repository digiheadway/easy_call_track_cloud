package com.example.salescrm.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.salescrm.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.salescrm.data.Priority
import kotlinx.coroutines.flow.first
import com.example.salescrm.data.UserPreferencesRepository

/**
 * BroadcastReceiver for handling reminder alarms
 * 
 * Handles:
 * - Advance reminders (5 min before) - silent notification with snooze option
 * - Main reminders - sound based on priority
 * - Snooze actions - reschedules task due time by 30 minutes
 * - Dismiss actions
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        const val TAG = "ReminderReceiver"
        const val ACTION_SHOW_REMINDER = "com.example.salescrm.ACTION_SHOW_REMINDER"
        const val ACTION_ADVANCE_REMINDER = "com.example.salescrm.ACTION_ADVANCE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.salescrm.ACTION_SNOOZE_REMINDER"
        const val ACTION_SNOOZE_30_REMINDER = "com.example.salescrm.ACTION_SNOOZE_30_REMINDER"
        const val ACTION_DISMISS_REMINDER = "com.example.salescrm.ACTION_DISMISS_REMINDER"
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received intent: ${intent.action}")
        
        when (intent.action) {
            ACTION_ADVANCE_REMINDER -> handleAdvanceReminder(context, intent)
            ACTION_SHOW_REMINDER -> handleShowReminder(context, intent)
            ACTION_SNOOZE_REMINDER -> handleSnoozeReminder(context, intent)
            ACTION_SNOOZE_30_REMINDER -> handleSnooze30Reminder(context, intent)
            ACTION_DISMISS_REMINDER -> handleDismissReminder(context, intent)
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
        }
    }
    
    /**
     * Handle advance reminder - show silent notification 5 min before task
     */
    private fun handleAdvanceReminder(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
        val taskDescription = intent.getStringExtra(ReminderManager.EXTRA_TASK_DESCRIPTION) ?: "Task Reminder"
        val priorityName = intent.getStringExtra(ReminderManager.EXTRA_PRIORITY) ?: Priority.NORMAL.name
        val linkedPersonId = intent.getIntExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, -1)
        val linkedPersonName = intent.getStringExtra(ReminderManager.EXTRA_LINKED_PERSON_NAME)
        val linkedPersonPhone = intent.getStringExtra(ReminderManager.EXTRA_LINKED_PERSON_PHONE)
        val taskType = intent.getStringExtra(ReminderManager.EXTRA_TASK_TYPE)
        val dueTime = intent.getStringExtra(ReminderManager.EXTRA_DUE_TIME)
        val defaultCountry = intent.getStringExtra(ReminderManager.EXTRA_DEFAULT_COUNTRY) ?: "US"
        
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID in advance reminder intent")
            return
        }
        
        Log.d(TAG, "Showing advance reminder for task $taskId (5 min before)")
        
        // Start the service for advance notification (silent)
        val serviceIntent = Intent(context, ReminderService::class.java).apply {
            action = ReminderService.ACTION_START_ADVANCE_REMINDER
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
            putExtra(ReminderManager.EXTRA_TASK_DESCRIPTION, taskDescription)
            putExtra(ReminderManager.EXTRA_PRIORITY, priorityName)
            putExtra(ReminderManager.EXTRA_TASK_TYPE, taskType)
            putExtra(ReminderManager.EXTRA_DUE_TIME, dueTime)
            putExtra(ReminderManager.EXTRA_DEFAULT_COUNTRY, defaultCountry)
            if (linkedPersonId != -1) {
                putExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, linkedPersonId)
            }
            linkedPersonName?.let { putExtra(ReminderManager.EXTRA_LINKED_PERSON_NAME, it) }
            linkedPersonPhone?.let { putExtra(ReminderManager.EXTRA_LINKED_PERSON_PHONE, it) }
        }
        
        try {
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ReminderService for advance reminder: ${e.message}")
        }
    }
    
    /**
     * Handle main reminder - show notification with sound based on priority
     */
    private fun handleShowReminder(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
        val taskDescription = intent.getStringExtra(ReminderManager.EXTRA_TASK_DESCRIPTION) ?: "Task Reminder"
        val priorityName = intent.getStringExtra(ReminderManager.EXTRA_PRIORITY) ?: Priority.NORMAL.name
        val linkedPersonId = intent.getIntExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, -1)
        val linkedPersonName = intent.getStringExtra(ReminderManager.EXTRA_LINKED_PERSON_NAME)
        val linkedPersonPhone = intent.getStringExtra(ReminderManager.EXTRA_LINKED_PERSON_PHONE)
        val taskType = intent.getStringExtra(ReminderManager.EXTRA_TASK_TYPE)
        val dueTime = intent.getStringExtra(ReminderManager.EXTRA_DUE_TIME)
        val defaultCountry = intent.getStringExtra(ReminderManager.EXTRA_DEFAULT_COUNTRY) ?: "US"
        
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID in reminder intent")
            return
        }
        
        Log.d(TAG, "Showing main reminder for task $taskId with priority $priorityName")
        
        // Start the foreground service to show notification and play sound
        val serviceIntent = Intent(context, ReminderService::class.java).apply {
            action = ReminderService.ACTION_START_REMINDER
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
            putExtra(ReminderManager.EXTRA_TASK_DESCRIPTION, taskDescription)
            putExtra(ReminderManager.EXTRA_PRIORITY, priorityName)
            putExtra(ReminderManager.EXTRA_TASK_TYPE, taskType)
            putExtra(ReminderManager.EXTRA_DUE_TIME, dueTime)
            putExtra(ReminderManager.EXTRA_DEFAULT_COUNTRY, defaultCountry)
            if (linkedPersonId != -1) {
                putExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, linkedPersonId)
            }
            linkedPersonName?.let { putExtra(ReminderManager.EXTRA_LINKED_PERSON_NAME, it) }
            linkedPersonPhone?.let { putExtra(ReminderManager.EXTRA_LINKED_PERSON_PHONE, it) }
        }
        
        try {
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ReminderService: ${e.message}")
        }
    }
    
    /**
     * Handle snooze reminder (5 min snooze from advance notification)
     */
    private fun handleSnoozeReminder(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
        
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID in snooze intent")
            return
        }
        
        Log.d(TAG, "Snoozing reminder for task $taskId")
        
        // Stop the service
        val serviceIntent = Intent(context, ReminderService::class.java).apply {
            action = ReminderService.ACTION_SNOOZE_REMINDER
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
        }
        context.startService(serviceIntent)
    }
    
    /**
     * Handle 30 min snooze - reschedules task due time by 30 minutes
     */
    private fun handleSnooze30Reminder(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
        
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID in snooze 30 intent")
            return
        }
        
        Log.d(TAG, "Snoozing reminder for task $taskId by 30 minutes (rescheduling task)")
        
        // Stop the service and reschedule the task
        val serviceIntent = Intent(context, ReminderService::class.java).apply {
            action = ReminderService.ACTION_SNOOZE_30_REMINDER
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
        }
        context.startService(serviceIntent)
    }
    
    private fun handleDismissReminder(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
        
        if (taskId == -1) {
            Log.e(TAG, "Invalid task ID in dismiss intent")
            return
        }
        
        Log.d(TAG, "Dismissing reminder for task $taskId")
        
        // Stop the service
        val serviceIntent = Intent(context, ReminderService::class.java).apply {
            action = ReminderService.ACTION_STOP_REMINDER
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
        }
        context.startService(serviceIntent)
    }
    
    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "Boot completed - rescheduling reminders and starting call monitor")
        
        // Start the call monitor service for persistent caller ID
        CallMonitorService.startService(context)
        
        // Reschedule all pending reminders after boot
        val database = AppDatabase.getDatabase(context)
        val repository = com.example.salescrm.data.CrmRepository(database.salesDao())
        val reminderManager = ReminderManager(context)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allTasks = repository.getAllTasksSync()
                val people = repository.getAllPeopleSync()
                val peopleMap = people.associate { it.id to Pair(it.name, it.phone) }
                val userPrefs = UserPreferencesRepository(context)
                val defaultCountry = userPrefs.defaultCountry.first()
                
                reminderManager.scheduleAllReminders(allTasks, defaultCountry) { personId ->
                    peopleMap[personId]
                }
                Log.d(TAG, "Rescheduled ${allTasks.size} tasks after boot")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule reminders on boot: ${e.message}")
            }
        }
    }
}
