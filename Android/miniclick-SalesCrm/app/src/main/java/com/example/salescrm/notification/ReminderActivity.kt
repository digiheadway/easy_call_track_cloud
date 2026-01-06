package com.example.salescrm.notification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salescrm.MainActivity
import com.example.salescrm.data.Priority
import com.example.salescrm.data.SampleData
import com.example.salescrm.data.Task
import com.example.salescrm.data.Person
import com.example.salescrm.ui.theme.PrimaryBlue
import com.example.salescrm.ui.theme.SalesCrmTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.example.salescrm.data.CallLogRepository
import com.example.salescrm.data.UserPreferencesRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Full screen activity shown when a reminder notification is clicked.
 * Provides:
 * - Task details (description, type, due time, priority)
 * - Person details if linked (name, phone)
 * - Snooze options (30 min snooze reschedules task)
 * - Direct actions: Call, WhatsApp, Open Task, Open Person
 */
class ReminderActivity : ComponentActivity() {
    
    private var taskId: Int = -1
    private var taskDescription: String = "Task Reminder"
    private var priorityName: String = Priority.NORMAL.name
    private var linkedPersonId: Int = -1
    private var linkedPersonName: String? = null
    private var linkedPersonPhone: String? = null
    private var taskType: String? = null
    private var dueTime: String? = null
    private var defaultCountry: String = "US"
    private var defaultWhatsAppPackage: String = "always_ask"
    private lateinit var userPrefs: UserPreferencesRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Extract data from intent
        taskId = intent.getIntExtra(ReminderManager.EXTRA_TASK_ID, -1)
        taskDescription = intent.getStringExtra(ReminderManager.EXTRA_TASK_DESCRIPTION) ?: "Task Reminder"
        priorityName = intent.getStringExtra(ReminderManager.EXTRA_PRIORITY) ?: Priority.NORMAL.name
        linkedPersonId = intent.getIntExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, -1)
        linkedPersonName = intent.getStringExtra(ReminderManager.EXTRA_LINKED_PERSON_NAME)
        linkedPersonPhone = intent.getStringExtra(ReminderManager.EXTRA_LINKED_PERSON_PHONE)
        taskType = intent.getStringExtra(ReminderManager.EXTRA_TASK_TYPE)
        dueTime = intent.getStringExtra(ReminderManager.EXTRA_DUE_TIME)
        defaultCountry = intent.getStringExtra(ReminderManager.EXTRA_DEFAULT_COUNTRY) ?: "US"
        
        userPrefs = UserPreferencesRepository(this)
        lifecycleScope.launch {
            defaultWhatsAppPackage = userPrefs.defaultWhatsAppPackage.first()
        }
        
        // Find the actual task and person
        val task = SampleData.tasks.find { it.id == taskId }
        val priority = try { Priority.valueOf(priorityName) } catch (e: Exception) { Priority.NORMAL }
        
        // Get person from SampleData if not provided via intent
        val person = if (linkedPersonId != -1) {
            SampleData.people.find { it.id == linkedPersonId }
        } else {
            null
        }
        
        // Update person info from actual data if available
        if (person != null) {
            linkedPersonName = person.name
            linkedPersonPhone = person.phone
        }
        
        setContent {
            SalesCrmTheme {
                ReminderScreen(
                    task = task,
                    person = person,
                    taskDescription = taskDescription,
                    priority = priority,
                    taskType = taskType,
                    dueTime = dueTime,
                    onSnooze30 = { snooze30Reminder() },
                    onDismiss = { dismissReminder() },
                    onOpenTask = { openTask() },
                    onOpenPerson = { openPerson() },
                    onCall = { callPerson() },
                    onWhatsApp = { whatsappPerson() }
                )
            }
        }
    }
    
    private fun snooze30Reminder() {
        // Stop the current reminder sound
        val stopIntent = Intent(this, ReminderService::class.java).apply {
            action = ReminderService.ACTION_SNOOZE_30_REMINDER
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
        }
        startService(stopIntent)
        finish()
    }
    
    private fun dismissReminder() {
        // Stop the reminder service
        val stopIntent = Intent(this, ReminderService::class.java).apply {
            action = ReminderService.ACTION_STOP_REMINDER
        }
        startService(stopIntent)
        
        // Cancel the scheduled reminder
        val reminderManager = ReminderManager(this)
        reminderManager.cancelReminder(taskId)
        
        finish()
    }
    
    private fun openTask() {
        // Stop the reminder
        val stopIntent = Intent(this, ReminderService::class.java).apply {
            action = ReminderService.ACTION_STOP_REMINDER
        }
        startService(stopIntent)
        
        // Open main activity with task ID
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
            putExtra("open_task", true)
        }
        startActivity(mainIntent)
        finish()
    }
    
    private fun openPerson() {
        // Stop the reminder
        val stopIntent = Intent(this, ReminderService::class.java).apply {
            action = ReminderService.ACTION_STOP_REMINDER
        }
        startService(stopIntent)
        
        // Open main activity with person ID
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderManager.EXTRA_LINKED_PERSON_ID, linkedPersonId)
            putExtra("open_person", true)
        }
        startActivity(mainIntent)
        finish()
    }
    
    private fun callPerson() {
        linkedPersonPhone?.let { phone ->
            val dialPhone = CallLogRepository.formatPhoneForDialer(phone, defaultCountry)
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$dialPhone")
            }
            startActivity(intent)
        }
    }
    
    private fun whatsappPerson() {
        linkedPersonPhone?.let { phone ->
            val intent = CallLogRepository.createWhatsAppChooserIntent(this, phone, defaultWhatsAppPackage)
            startActivity(intent)
        }
    }
}

@Composable
fun ReminderScreen(
    task: Task?,
    person: Person?,
    taskDescription: String,
    priority: Priority,
    taskType: String?,
    dueTime: String?,
    onSnooze30: () -> Unit,
    onDismiss: () -> Unit,
    onOpenTask: () -> Unit,
    onOpenPerson: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    val priorityColor = Color(priority.color)
    val backgroundGradient = when (priority) {
        Priority.SUPER_HIGH -> Brush.verticalGradient(
            colors = listOf(Color(0xFF1A1A2E), Color(0xFF3D1A1A))
        )
        Priority.HIGH -> Brush.verticalGradient(
            colors = listOf(Color(0xFF16213E), Color(0xFF1A2E3D))
        )
        Priority.NORMAL -> Brush.verticalGradient(
            colors = listOf(Color(0xFF1A1A2E), Color(0xFF1A2E2E))
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(priorityColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (priority) {
                        Priority.SUPER_HIGH -> Icons.Filled.Warning
                        Priority.HIGH -> Icons.Filled.Notifications
                        Priority.NORMAL -> Icons.Filled.NotificationsNone
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = priorityColor
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Priority label
            Text(
                text = when (priority) {
                    Priority.SUPER_HIGH -> "⚠️ URGENT REMINDER"
                    Priority.HIGH -> "📋 Task Reminder"
                    Priority.NORMAL -> "📝 Reminder"
                },
                fontSize = if (priority == Priority.SUPER_HIGH) 24.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = priorityColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Task details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Task description
                    Text(
                        text = task?.description ?: taskDescription,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Task info chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Task type
                        if (taskType != null || task?.type != null) {
                            InfoChip(
                                icon = Icons.Filled.Category,
                                text = taskType ?: task?.type?.label ?: ""
                            )
                        }
                        
                        // Due time
                        val displayTime = dueTime ?: task?.dueTime?.toString()
                        val formattedTime = remember(displayTime) {
                            displayTime?.let {
                                try {
                                    val time = LocalTime.parse(it)
                                    time.format(DateTimeFormatter.ofPattern("hh:mm a"))
                                } catch (e: Exception) { null }
                            }
                        }
                        if (formattedTime != null) {
                            InfoChip(
                                icon = Icons.Filled.Schedule,
                                text = formattedTime
                            )
                        }
                        
                        // Priority
                        InfoChip(
                            icon = Icons.Filled.Flag,
                            text = priority.label,
                            color = priorityColor
                        )
                    }
                }
            }
            
            // Person details card (if linked)
            if (person != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "👤 Linked Person",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Person name
                        Text(
                            text = person.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        // Phone
                        if (person.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = person.phone,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Contact action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Call button
                            Button(
                                onClick = onCall,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22C55E)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call")
                            }
                            
                            // WhatsApp button
                            Button(
                                onClick = onWhatsApp,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("WhatsApp")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Open person profile button
                        OutlinedButton(
                            onClick = onOpenPerson,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Profile")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Snooze section
            Text(
                text = "⏰ Snooze for 30 minutes",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            
            Text(
                text = "(Reschedules task due time)",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onSnooze30,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Snooze, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Snooze 30 min", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main action buttons
            Button(
                onClick = onOpenTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Task", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.3f))
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dismiss Reminder", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    color: Color = Color.White.copy(alpha = 0.8f)
) {
    AssistChip(
        onClick = { },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        },
        label = { Text(text, color = color) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    )
}
