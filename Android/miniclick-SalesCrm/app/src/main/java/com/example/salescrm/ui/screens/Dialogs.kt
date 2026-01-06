package com.example.salescrm.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.example.salescrm.data.*
import com.example.salescrm.ui.components.*
import com.example.salescrm.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

// ==================== ADD/EDIT TASK DIALOG ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskSheet(
    task: Task?,
    linkedPersonId: Int?,  // Can be null for standalone tasks
    people: List<Person> = emptyList(),  // List of all people for selection
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = LocalContext.current
    
    // Just-in-time notification permission handling
    var pendingTask by remember { mutableStateOf<Task?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Save the task regardless of permission result - user made their choice
        pendingTask?.let { onSave(it) }
        pendingTask = null
    }

    // Find the linked person first to get their priority
    val initialPerson = (task?.linkedPersonId ?: linkedPersonId)?.let { id -> people.find { it.id == id } }
    
    // State
    var taskType by remember { mutableStateOf(task?.type ?: TaskType.FOLLOW_UP) }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var dueDate by remember { mutableStateOf(task?.dueDate ?: LocalDate.now()) }
    var dueTime by remember { mutableStateOf(task?.dueTime) }
    // Priority: use task's priority if editing, else inherit from person's priority, else default to "normal"
    var priorityId by remember { 
        mutableStateOf(
            task?.priorityId 
                ?: initialPerson?.let { if (it.isInPipeline) it.pipelinePriorityId else it.priorityId }
                ?: "normal"
        ) 
    }
    var selectedPersonId by remember { mutableStateOf(task?.linkedPersonId ?: linkedPersonId) }
    
    // Reminder state
    var showReminder by remember { mutableStateOf(task?.showReminder ?: true) }
    var reminderMinutes by remember { mutableStateOf(task?.reminderMinutesBefore ?: 0) } 

    // Pickers
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPersonPicker by remember { mutableStateOf(false) }
    
    // Values to initialize the picker with
    var pickerInitialDate by remember { mutableStateOf(LocalDate.now()) }
    var pickerInitialTime by remember { mutableStateOf<LocalTime?>(LocalTime.now()) }
    
    // Find current selected person
    val selectedPerson = selectedPersonId?.let { id -> people.find { it.id == id } }
    
    // When person selection changes, update priority to their priority (if not editing an existing task)
    LaunchedEffect(selectedPersonId) {
        if (task == null && selectedPersonId != null) {
            val person = people.find { it.id == selectedPersonId }
            person?.let {
                priorityId = if (it.isInPipeline) it.pipelinePriorityId else it.priorityId
            }
        }
    }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate.toEpochDay() * 24 * 60 * 60 * 1000
    )
    
    val currentTime = LocalTime.now()
    val timePickerState = rememberTimePickerState(
        initialHour = dueTime?.hour ?: currentTime.hour,
        initialMinute = dueTime?.minute ?: currentTime.minute
    )
    
    // Suggestions Data
    val dateSuggestions = listOf(
        "Today" to 0L,
        "Tomorrow" to 1L,
        "T+2" to 2L,
        "T+7" to 7L,
        "T+15" to 15L
    )
    
    val timeSuggestions = listOf(
        "11 AM" to LocalTime.of(11, 0),
        "2 PM" to LocalTime.of(14, 0),
        "5 PM" to LocalTime.of(17, 0)
    )

    // Reminder Option Helper
    data class ReminderOption(val label: String, val minutes: Int, val isOff: Boolean = false)
    val reminderOptions = listOf(
        ReminderOption("On time", 0),
        ReminderOption("30m before", 30),
        ReminderOption("1h before", 60),
        ReminderOption("2h before", 120),
        ReminderOption("Off", 0, isOff = true)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) },
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        KeyboardDismissibleContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            // Header with title and person assign icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        if (task == null) "New Task" else "Edit Task",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                    // Subheading - linked person status
                    Text(
                        text = selectedPerson?.let { "Linked to ${it.name}" } ?: "Not linked to contact",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedPerson != null) PrimaryBlue else SalesCrmTheme.colors.textMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Person assign icon button
                IconButton(
                    onClick = { if (people.isNotEmpty()) showPersonPicker = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (selectedPerson != null) PrimaryBlue.copy(alpha = 0.1f) else SalesCrmTheme.colors.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = "Link to person",
                        tint = if (selectedPerson != null) PrimaryBlue else SalesCrmTheme.colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))

            // Task Type Selector
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskType.entries.forEach { type ->
                    val isSelected = taskType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { taskType = type },
                        label = { Text(type.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SalesCrmTheme.colors.surfaceVariant,
                            labelColor = SalesCrmTheme.colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.border
                        )
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Description
            DarkFormField(
                label = "Description",
                value = description,
                onValueChange = { description = it },
                placeholder = "What needs to be done?",
                singleLine = false,
                minLines = 2
            )
            
            Spacer(Modifier.height(20.dp))
            
            // === Date Selection (compact) ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Due Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = SalesCrmTheme.colors.textSecondary
                )
                Text(
                    dueDate.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = SalesCrmTheme.colors.textMuted
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dateSuggestions) { (label, daysOffset) ->
                    val targetDate = LocalDate.now().plusDays(daysOffset)
                    val isSelected = dueDate == targetDate
                    SuggestionChip(
                        label = label,
                        selected = isSelected,
                        onClick = { 
                            dueDate = targetDate
                            // If no time is set, default to 11 AM when picking a suggestion
                            if (dueTime == null) {
                                dueTime = LocalTime.of(11, 0)
                            }
                        }
                    )
                }
                item {
                    SuggestionChip(
                        label = "Custom",
                        selected = false,
                         onClick = { 
                             pickerInitialDate = LocalDate.now()
                             pickerInitialTime = LocalTime.now()
                             showDatePicker = true 
                         }
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // === Time Selection (compact) ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = SalesCrmTheme.colors.textSecondary
                )
                if (dueTime != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dueTime!!.toDisplayString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = SalesCrmTheme.colors.textMuted
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Close, 
                            "Clear Time", 
                            tint = SalesCrmTheme.colors.textMuted,
                            modifier = Modifier.size(14.dp).clickable { dueTime = null }
                        )
                    }
                } else {
                    Text(
                        "Not set",
                        style = MaterialTheme.typography.bodySmall,
                        color = SalesCrmTheme.colors.textMuted
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(timeSuggestions) { (label, time) ->
                    val isSelected = dueTime == time
                    SuggestionChip(
                        label = label,
                        selected = isSelected,
                        onClick = { dueTime = time }
                    )
                }
                item {
                    SuggestionChip(
                         label = "Custom",
                         selected = dueTime != null && timeSuggestions.none { it.second == dueTime },
                         onClick = { 
                             pickerInitialDate = dueDate
                             pickerInitialTime = LocalTime.now()
                             showTimePicker = true 
                         }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            
            // === Priority & Reminder Row (left-aligned, no labels) ===
            var priorityExpanded by remember { mutableStateOf(false) }
            var reminderExpanded by remember { mutableStateOf(false) }
            val customPriorities = SalesCrmTheme.priorities
            val selectedPriorityItem = customPriorities.findById(priorityId)
            val priorityColor = Color(selectedPriorityItem?.color ?: 0xFF6B7280)
            val selectedReminderLabel = if (!showReminder) "Off" else reminderOptions.find { !it.isOff && it.minutes == reminderMinutes }?.label ?: "On time"
            val reminderColor = if (showReminder) AccentOrange else SalesCrmTheme.colors.textMuted
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Dropdown
                Box {
                    Surface(
                        onClick = { priorityExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = priorityColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                selectedPriorityItem?.label ?: "Normal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = priorityColor,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = priorityColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false },
                        modifier = Modifier.background(SalesCrmTheme.colors.surface).width(180.dp)
                    ) {
                        Text(
                            "Select Priority",
                            style = MaterialTheme.typography.labelSmall,
                            color = SalesCrmTheme.colors.textMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(color = SalesCrmTheme.colors.border)
                        
                        customPriorities.forEach { pItem ->
                            val pColor = Color(pItem.color)
                            val isSelected = pItem.id == priorityId
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = pColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = pColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        pItem.label,
                                        color = pColor,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    priorityId = pItem.id
                                    priorityExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Reminder Dropdown
                Box {
                    Surface(
                        onClick = { reminderExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        color = reminderColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, reminderColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (showReminder) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = reminderColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                selectedReminderLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = reminderColor,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = reminderColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = reminderExpanded,
                        onDismissRequest = { reminderExpanded = false },
                        modifier = Modifier.background(SalesCrmTheme.colors.surface).width(180.dp)
                    ) {
                        Text(
                            "Set Reminder",
                            style = MaterialTheme.typography.labelSmall,
                            color = SalesCrmTheme.colors.textMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(color = SalesCrmTheme.colors.border)
                        
                        reminderOptions.forEach { option ->
                            val isSelected = if (option.isOff) !showReminder else (showReminder && reminderMinutes == option.minutes)
                            val optionIcon = when {
                                option.isOff -> Icons.Default.NotificationsOff
                                else -> Icons.Default.NotificationsActive
                            }
                            
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        optionIcon,
                                        contentDescription = null,
                                        tint = if (option.isOff) SalesCrmTheme.colors.textMuted else AccentOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (option.isOff) SalesCrmTheme.colors.textMuted else AccentOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                text = {
                                    Text(
                                        option.label,
                                        color = if (option.isOff) SalesCrmTheme.colors.textMuted else SalesCrmTheme.colors.textPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (option.isOff) {
                                        showReminder = false
                                    } else {
                                        showReminder = true
                                        reminderMinutes = option.minutes
                                    }
                                    reminderExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            // Action Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val newTask = Task(
                        id = task?.id ?: 0,
                        linkedPersonId = selectedPersonId,
                        type = taskType,
                        description = description,
                        dueDate = dueDate,
                        dueTime = dueTime,
                        priorityId = priorityId,
                        showReminder = showReminder,
                        reminderMinutesBefore = reminderMinutes,
                        status = task?.status ?: TaskStatus.PENDING,
                        createdAt = task?.createdAt ?: LocalDateTime.now()
                    )
                    
                    // Check if notification permission is needed (Android 13+ with reminder enabled)
                    if (showReminder && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasNotificationPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        
                        if (!hasNotificationPermission) {
                            // Store the task and request permission
                            pendingTask = newTask
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@Button
                        }
                    }
                    
                    // Permission already granted or not needed, save directly
                    onSave(newTask)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    if (task == null) "Create Task" else "Save Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    // Unified Date & Time Picker
    if (showDatePicker || showTimePicker) {
        SimpleDateTimePickerSheet(
            initialDate = pickerInitialDate,
            initialTime = pickerInitialTime,
            onDismiss = { 
                showDatePicker = false
                showTimePicker = false
            },
            onConfirm = { date, time ->
                dueDate = date
                dueTime = time
                showDatePicker = false
                showTimePicker = false
            }
        )
    }
    
    // Person Picker Dialog
    if (showPersonPicker) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredPeople = remember(searchQuery, people) {
            if (searchQuery.isBlank()) people
            else people.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery)
            }
        }
        
        AlertDialog(
            onDismissRequest = { showPersonPicker = false },
            title = { 
                Text(
                    "Select Person", 
                    color = SalesCrmTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name or phone", color = SalesCrmTheme.colors.textMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = SalesCrmTheme.colors.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = salesCrmTextFieldColors()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // People list
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option to remove link
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPersonId = null
                                        showPersonPicker = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = SalesCrmTheme.colors.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = null,
                                        tint = SalesCrmTheme.colors.textMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "No person linked (Standalone task)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SalesCrmTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                        
                        items(filteredPeople) { person ->
                            val isSelected = person.id == selectedPersonId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPersonId = person.id
                                        showPersonPicker = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else SalesCrmTheme.colors.surfaceVariant,
                                border = if (isSelected) BorderStroke(2.dp, PrimaryBlue) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar/Icon
                                    Surface(
                                        shape = CircleShape,
                                        color = if (person.isInPipeline) PrimaryBlue.copy(alpha = 0.2f) else AccentGreen.copy(alpha = 0.2f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                person.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = if (person.isInPipeline) PrimaryBlue else AccentGreen
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            person.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = SalesCrmTheme.colors.textPrimary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (person.isInPipeline) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = PrimaryBlue
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Pipeline",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PrimaryBlue
                                                )
                                                Text(
                                                    " • ${SalesCrmTheme.stages.findById(person.stageId)?.label ?: "Unknown"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SalesCrmTheme.colors.textMuted
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = AccentGreen
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Contact",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentGreen
                                                )
                                            }
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonPicker = false }) {
                    Text("Close", color = SalesCrmTheme.colors.textSecondary)
                }
            },
            containerColor = SalesCrmTheme.colors.surface
        )
    }
}

@Composable
fun SuggestionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) PrimaryBlue else SalesCrmTheme.colors.surfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, SalesCrmTheme.colors.border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color.White else SalesCrmTheme.colors.textSecondary,
             fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ==================== TASK DETAIL SHEET ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    linkedPerson: Person? = null,  // The linked person, if any
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMarkDone: () -> Unit,
    onReschedule: () -> Unit,
    onDelete: () -> Unit, // Delete task
    onResponseChange: (String) -> Unit, // Auto-save response
    currentResponse: String = "",
    onTaskUpdate: ((Task) -> Unit)? = null, // New callback for internal updates
    onCallPerson: ((Person) -> Unit)? = null,
    onWhatsAppPerson: ((Person) -> Unit)? = null,
    onViewPerson: ((Person) -> Unit)? = null,
    currencySymbol: String = "₹",
    budgetMultiplier: Int = 100000
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var responseText by remember { mutableStateOf(currentResponse) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    // Internal Picker States
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickerInitialDate by remember { mutableStateOf(LocalDate.now()) }
    var pickerInitialTime by remember { mutableStateOf<LocalTime?>(LocalTime.now()) }
    
    // Helper to update date/time
    fun updateDateTime(date: LocalDate, time: LocalTime?) {
        val updated = task.copy(dueDate = date, dueTime = time)
        onTaskUpdate?.invoke(updated)
    }

    // Helper to add time
    fun addTime(minutes: Long) {
        val baseDate = task.dueDate
        val baseTime = task.dueTime ?: LocalTime.now()
        val baseDateTime = LocalDateTime.of(baseDate, baseTime)
        val newDateTime = baseDateTime.plusMinutes(minutes)
        updateDateTime(newDateTime.toLocalDate(), newDateTime.toLocalTime())
        android.widget.Toast.makeText(context, "Rescheduled to ${newDateTime.toDisplayString()}", android.widget.Toast.LENGTH_SHORT).show()
    }

    
    if (showDeleteConfirm) {
        ConfirmSheet(
            title = "Delete Task",
            message = "Are you sure you want to delete this task?",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) },
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        KeyboardDismissibleContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            // Header: Task Type on left, Three-dot menu on right
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left side: Task Type and Description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.type.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        task.description.ifBlank { "No description provided." },
                        style = MaterialTheme.typography.headlineSmall,
                        color = SalesCrmTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Right side: Three-dot menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = SalesCrmTheme.colors.textSecondary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(SalesCrmTheme.colors.surface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = SalesCrmTheme.colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text("Edit", color = SalesCrmTheme.colors.textPrimary)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = AccentRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text("Delete", color = AccentRed)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                        if (task.status != TaskStatus.CANCELLED && task.status != TaskStatus.COMPLETED) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = SalesCrmTheme.colors.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text("Cancel Task", color = SalesCrmTheme.colors.textPrimary)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onTaskUpdate?.invoke(task.copy(status = TaskStatus.CANCELLED))
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Meta Info: Date, Time, Reminder icon (if set)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (task.dueTime != null) Icons.Default.Schedule else Icons.Default.DateRange,
                        contentDescription = null,
                        tint = SalesCrmTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    val context = LocalContext.current
                    val haptic = LocalHapticFeedback.current
                    val displayString = if (task.dueTime != null) {
                        LocalDateTime.of(task.dueDate, task.dueTime).toDisplayString()
                    } else {
                        task.dueDate.toDisplayString()
                    }
                    Text(
                        displayString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SalesCrmTheme.colors.textPrimary,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                Toast.makeText(context, task.dueDate.toFullDateTimeString(task.dueTime), Toast.LENGTH_SHORT).show()
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, task.dueDate.toFullDateTimeString(task.dueTime), Toast.LENGTH_SHORT).show()
                            }
                        )
                    )
                }
                
                // Reminder icon (if set)
                if (task.showReminder) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Reminder set",
                        tint = AccentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // === Linked Person Card (if connected) ===
            if (linkedPerson != null) {
                Spacer(Modifier.height(20.dp))
                
                // Compact Person Card - no avatar
                Surface(
                    onClick = { onViewPerson?.invoke(linkedPerson) },
                    shape = RoundedCornerShape(12.dp),
                    color = SalesCrmTheme.colors.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Name and details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                linkedPerson.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = SalesCrmTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            
                            // Budget and Stage as subhead
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (linkedPerson.budget.isNotBlank()) {
                                    Text(
                                        formatBudget(linkedPerson.budget, currencySymbol, budgetMultiplier),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SalesCrmTheme.colors.textSecondary
                                    )
                                }
                                if (linkedPerson.isInPipeline) {
                                    if (linkedPerson.budget.isNotBlank()) {
                                        Text("•", color = SalesCrmTheme.colors.textMuted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        SalesCrmTheme.stages.findById(linkedPerson.stageId)?.label ?: "Unknown",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SalesCrmTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                        
                        // Compact Call and WhatsApp buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Call button
                            if (onCallPerson != null) {
                                Surface(
                                    onClick = { onCallPerson(linkedPerson) },
                                    shape = CircleShape,
                                    color = AccentGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = "Call",
                                            tint = AccentGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            
                            // WhatsApp button
                            if (onWhatsAppPerson != null) {
                                Surface(
                                    onClick = { onWhatsAppPerson(linkedPerson) },
                                    shape = CircleShape,
                                    color = Color(0xFF25D366).copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "WhatsApp",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Response Input Box (auto save)
            DarkFormField(
                label = "Response / Notes",
                value = responseText,
                onValueChange = { 
                    responseText = it
                    onResponseChange(it)
                },
                placeholder = "Add your response or notes...",
                singleLine = false,
                minLines = 3
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Action Buttons: Edit, Reschedule (Icon), Mark as Done
                if (task.status != TaskStatus.COMPLETED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Icon Button
                    Surface(
                        onClick = onEdit,
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = SalesCrmTheme.colors.surfaceVariant,
                        border = BorderStroke(1.dp, SalesCrmTheme.colors.border)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = SalesCrmTheme.colors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Reschedule Icon Button
                    Surface(
                        onClick = { 
                            pickerInitialDate = LocalDate.now()
                            pickerInitialTime = LocalTime.now()
                            showDatePicker = true 
                        },
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = SalesCrmTheme.colors.surfaceVariant,
                        border = BorderStroke(1.dp, SalesCrmTheme.colors.border)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Reschedule",
                                tint = SalesCrmTheme.colors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Mark as Done Button (Primary)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMarkDone()
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Quick Reschedule Chips
                Text(
                    "Add Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = SalesCrmTheme.colors.textSecondary
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    val addTimeOptions = listOf(
                        "+30 min" to 30L,
                        "+2 hr" to 120L,
                        "+6 hr" to 360L,
                        "+24 hr" to 1440L
                    )
                    addTimeOptions.forEach { (label, mins) ->
                        SuggestionChip(
                            label = label,
                            selected = false,
                            onClick = { addTime(mins) }
                        )
                    }
                    // Custom Reschedule option
                    SuggestionChip(
                        label = "Custom",
                        selected = false,
                        onClick = {
                            pickerInitialDate = LocalDate.now()
                            pickerInitialTime = LocalTime.now()
                            showDatePicker = true
                        }
                    )
                }
            }
 else {
                // If completed, show option to mark as pending
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMarkDone()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SalesCrmTheme.colors.textSecondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mark as Pending", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    // === INTERNAL PICKERS ===
    
    // Unified Date & Time Picker for Rescheduling
    if (showDatePicker || showTimePicker) {
        SimpleDateTimePickerSheet(
            initialDate = pickerInitialDate,
            initialTime = pickerInitialTime,
            onDismiss = { 
                showDatePicker = false
                showTimePicker = false
            },
            onConfirm = { date, time ->
                updateDateTime(date, time)
                showDatePicker = false
                showTimePicker = false
            }
        )
    }
}

// ==================== ADD NOTE SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteSheet(
    personId: Int,
    onDismiss: () -> Unit,
    onSave: (Activity) -> Unit
) {
    var content by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) }
    ) {
        KeyboardDismissibleContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Add Comment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textSecondary)
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("Write your comment here...", color = SalesCrmTheme.colors.textMuted) },
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 6,
                colors = salesCrmTextFieldColors()
            )
            
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SalesCrmTheme.colors.textSecondary
                    )
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { 
                        onSave(Activity(
                            id = 0,
                            personId = personId,
                            type = ActivityType.COMMENT,
                            description = content,
                            timestamp = java.time.LocalDateTime.now()
                        ))
                    },
                    enabled = content.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Add Comment", fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }
}

// ==================== ADD ACTIVITY SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivitySheet(
    personId: Int,
    onDismiss: () -> Unit,
    onSave: (Activity) -> Unit
) {
    var selectedType by remember { mutableStateOf(ActivityType.SITE_VISIT) }
    var customTitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var activityTime by remember { mutableStateOf(LocalDateTime.now()) }
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    // Unified Date & Time Picker
    var showDateTimePicker by remember { mutableStateOf(false) }
    
    if (showDateTimePicker) {
        SimpleDateTimePickerSheet(
            initialDate = activityTime.toLocalDate(),
            initialTime = activityTime.toLocalTime(),
            onDismiss = { showDateTimePicker = false },
            onConfirm = { date, time ->
                val newTime = time ?: LocalTime.now()
                activityTime = LocalDateTime.of(date, newTime)
                showDateTimePicker = false
            }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) }
    ) {
        KeyboardDismissibleContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Log Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textSecondary)
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Activity Type Selection
            Text(
                "Activity Type",
                style = MaterialTheme.typography.labelMedium,
                color = SalesCrmTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Activity type chips in a single row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                ActivityType.entries.forEach { type ->
                    ActivityTypeChip(
                        type = type,
                        selected = selectedType == type,
                        onClick = { 
                            selectedType = type
                            // When Custom is selected, reset to current Today and Now
                            if (type == ActivityType.CUSTOM) {
                                activityTime = LocalDateTime.now()
                            }
                        }
                    )
                }
            }
            
            // Custom Title Input (only when Custom is selected)
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedType == ActivityType.CUSTOM,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "Activity Title",
                        style = MaterialTheme.typography.labelMedium,
                        color = SalesCrmTheme.colors.textMuted,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., Meeting, Payment, Negotiation...", color = SalesCrmTheme.colors.textMuted) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = salesCrmTextFieldColors()
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Date & Time Selection
            Text(
                "Date & Time",
                style = MaterialTheme.typography.labelMedium,
                color = SalesCrmTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Surface(
                onClick = { showDateTimePicker = true },
                shape = RoundedCornerShape(12.dp),
                color = SalesCrmTheme.colors.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = SalesCrmTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        activityTime.toDisplayString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit time",
                        tint = SalesCrmTheme.colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Description
            Text(
                "Description (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = SalesCrmTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Add details about this activity...", color = SalesCrmTheme.colors.textMuted) },
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5,
                colors = salesCrmTextFieldColors()
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SalesCrmTheme.colors.textSecondary
                    )
                ) {
                    Text("Cancel")
                }
                
                val actionTitle = if (selectedType == ActivityType.CUSTOM) customTitle.trim() else selectedType.label
                val isValid = selectedType != ActivityType.CUSTOM || customTitle.isNotBlank()
                
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(
                            Activity(
                                id = 0,
                                personId = personId,
                                type = selectedType,
                                title = if (selectedType == ActivityType.CUSTOM) customTitle.trim() else null,
                                description = description,
                                timestamp = activityTime
                            )
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Log Activity", fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }
}

@Composable
private fun ActivityTypeChip(
    type: ActivityType,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (type) {
        ActivityType.SITE_VISIT -> Icons.Default.LocationOn
        ActivityType.WHATSAPP -> Icons.Default.ChatBubbleOutline
        ActivityType.DETAILS_SENT -> Icons.AutoMirrored.Filled.Send
        ActivityType.CUSTOM -> Icons.Default.Edit
        ActivityType.COMMENT -> Icons.Default.Comment
        ActivityType.SYSTEM -> Icons.Default.Info
    }
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) PrimaryBlue else SalesCrmTheme.colors.surfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, SalesCrmTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else SalesCrmTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = type.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) Color.White else SalesCrmTheme.colors.textSecondary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// ==================== EDIT ACTIVITY SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditActivitySheet(
    activity: Activity,
    onDismiss: () -> Unit,
    onSave: (Activity) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(activity.title ?: activity.type.label) }
    var description by remember { mutableStateOf(activity.description) }
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        ConfirmSheet(
            title = "Delete Activity",
            message = "Are you sure you want to delete this activity log?",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                onDelete()
                showDeleteConfirm = false
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) }
    ) {
        KeyboardDismissibleContent {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Edit Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textSecondary)
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Activity Title
            Text(
                "Activity Title",
                style = MaterialTheme.typography.labelMedium,
                color = SalesCrmTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Activity title...", color = SalesCrmTheme.colors.textMuted) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = salesCrmTextFieldColors()
            )
            
            Spacer(Modifier.height(20.dp))
            
            // Timestamp (Read-only)
            Text(
                "Logged At",
                style = MaterialTheme.typography.labelMedium,
                color = SalesCrmTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SalesCrmTheme.colors.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = SalesCrmTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        activity.timestamp.toFullDateTimeString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Description
            Text(
                "Description",
                style = MaterialTheme.typography.labelMedium,
                color = SalesCrmTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text("Description...", color = SalesCrmTheme.colors.textMuted) },
                shape = RoundedCornerShape(12.dp),
                minLines = 2,
                maxLines = 4,
                colors = salesCrmTextFieldColors()
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delete button
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                    border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(20.dp))
                }
                
                // Save button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(activity.copy(title = title.trim(), description = description))
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }
}

// ==================== CONFIRM SHEET ====================


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSheet(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) AccentRed else SalesCrmTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textSecondary)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = SalesCrmTheme.colors.textSecondary
            )
            
            Spacer(Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SalesCrmTheme.colors.textSecondary
                    )
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDestructive) AccentRed else PrimaryBlue
                    )
                ) {
                    Text(confirmText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==================== SORT BY SHEET ====================

enum class SortDirection(val label: String) {
    DESC("Descending"),
    ASC("Ascending")
}

data class SortOption(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBySheet(
    sortOptions: List<SortOption>,
    selectedSortBy: String,
    selectedDirection: SortDirection,
    onSortByChange: (String) -> Unit,
    onDirectionChange: (SortDirection) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sort By",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textSecondary)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Sort Options list with icons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                sortOptions.forEach { option ->
                    val isSelected = option.id == selectedSortBy
                    Surface(
                        onClick = { onSortByChange(option.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon
                            option.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.textMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(14.dp))
                            }
                            
                            // Label
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Check mark
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            HorizontalDivider(color = SalesCrmTheme.colors.border)
            
            Spacer(Modifier.height(24.dp))
            
            // Direction
            Text(
                "DIRECTION",
                style = MaterialTheme.typography.labelSmall,
                color = SalesCrmTheme.colors.textMuted,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SortDirection.entries.forEach { direction ->
                    val isSelected = direction == selectedDirection
                    Surface(
                        onClick = { onDirectionChange(direction) },
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.surfaceVariant,
                        border = if (isSelected) null else BorderStroke(1.dp, SalesCrmTheme.colors.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (direction == SortDirection.DESC) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else SalesCrmTheme.colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                direction.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Color.White else SalesCrmTheme.colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== VIEW MODE SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewModeSheet(
    currentMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SalesCrmTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "View Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SalesCrmTheme.colors.textPrimary
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Choose how you want to view the list",
                style = MaterialTheme.typography.bodyMedium,
                color = SalesCrmTheme.colors.textSecondary
            )
            
            Spacer(Modifier.height(24.dp))
            
            // View mode options
            val viewModes = listOf(
                Triple(ViewMode.CARD, "Card View", Icons.Default.GridView),
                Triple(ViewMode.LIST, "List View", Icons.Default.ViewList),
                Triple(ViewMode.TABLE, "Table View", Icons.Default.TableChart)
            )
            
            viewModes.forEach { (mode, label, icon) ->
                val isSelected = currentMode == mode
                
                Surface(
                    onClick = {
                        onModeSelected(mode)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else SalesCrmTheme.colors.surfaceVariant,
                    border = if (isSelected) BorderStroke(2.dp, PrimaryBlue) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.textPrimary
                            )
                            Text(
                                when (mode) {
                                    ViewMode.CARD -> "Full cards with all details"
                                    ViewMode.LIST -> "Compact list with key info"
                                    ViewMode.TABLE -> "Minimal rows for quick scanning"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = SalesCrmTheme.colors.textMuted
                            )
                        }
                        
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== CUSTOM ITEMS SHEET ====================

enum class CustomItemType(val title: String) {
    STAGES("Pipeline Stages"),
    PRIORITIES("Priorities"),
    SEGMENTS("Segments"),
    SOURCES("Sources")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomItemsSheet(
    type: CustomItemType,
    items: List<CustomItem>,
    onItemsChange: (List<CustomItem>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingItem by remember { mutableStateOf<CustomItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    
    // Working copy of items for live editing
    var workingItems by remember(items) { mutableStateOf(items.toMutableList()) }
    
    ModalBottomSheet(
        onDismissRequest = {
            // Save on dismiss
            if (workingItems != items) {
                onItemsChange(workingItems)
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        type.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                    Text(
                        "${workingItems.size} items",
                        style = MaterialTheme.typography.bodySmall,
                        color = SalesCrmTheme.colors.textMuted
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reset button
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset to Default",
                            tint = SalesCrmTheme.colors.textSecondary
                        )
                    }
                    
                    // Close button
                    IconButton(onClick = {
                        if (workingItems != items) {
                            onItemsChange(workingItems)
                        }
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textSecondary)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Reorderable state
            val reorderableState = rememberReorderableLazyListState(
                onMove = { from, to ->
                    workingItems = workingItems.toMutableList().apply {
                        add(to, removeAt(from))
                        // Update order fields
                        forEachIndexed { index, item ->
                            this[index] = item.copy(order = index)
                        }
                    }
                }
            )

            // Items List
            LazyColumn(
                state = reorderableState.lazyListState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workingItems.size, key = { workingItems[it].id }) { index ->
                    val item = workingItems[index]
                    with(reorderableState) {
                        CustomItemRow(
                            item = item,
                            dragHandleModifier = Modifier.dragHandle(item.id),
                            modifier = Modifier.reorderableItem(item.id),
                            onEdit = { editingItem = item },
                            onDelete = {
                                workingItems = workingItems.filter { it.id != item.id }.toMutableList()
                            }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Add New Button
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SalesCrmTheme.colors.border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SalesCrmTheme.colors.textPrimary)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add New ${type.title.dropLast(1)}", fontWeight = FontWeight.Medium)
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Save Button
            Button(
                onClick = {
                    onItemsChange(workingItems)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
    
    // Edit Item Dialog
    editingItem?.let { item ->
        EditCustomItemDialog(
            item = item,
            onSave = { updated ->
                workingItems = workingItems.map { 
                    if (it.id == updated.id) updated else it 
                }.toMutableList()
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }
    
    // Add Item Dialog
    if (showAddDialog) {
        EditCustomItemDialog(
            item = null,
            onSave = { newItem ->
                val id = "${type.name.lowercase()}_${System.currentTimeMillis()}"
                workingItems = (workingItems + newItem.copy(id = id, order = workingItems.size)).toMutableList()
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
    
    // Reset Confirmation
    if (showResetConfirm) {
        Dialog(onDismissRequest = { showResetConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SalesCrmTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "Reset to Default?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        "This will restore all ${type.title.lowercase()} to their default values. Your custom changes will be lost.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SalesCrmTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showResetConfirm = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SalesCrmTheme.colors.textSecondary
                            )
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                onReset()
                                showResetConfirm = false
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                        ) {
                            Text("Reset", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomItemRow(
    item: CustomItem,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SalesCrmTheme.colors.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(
                Icons.Default.DragIndicator,
                contentDescription = "Drag to reorder",
                modifier = dragHandleModifier
                    .size(24.dp)
                    .padding(4.dp),
                tint = SalesCrmTheme.colors.textMuted
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Color indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(item.color), RoundedCornerShape(8.dp))
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Label
            Text(
                item.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = SalesCrmTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            
            // Edit button
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = SalesCrmTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Delete button
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = AccentRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    
    // Delete confirmation
    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SalesCrmTheme.colors.surface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Delete \"${item.label}\"?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        "This item will be removed. Existing data using this item will keep their values.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SalesCrmTheme.colors.textSecondary
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                onDelete()
                                showDeleteConfirm = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditCustomItemDialog(
    item: CustomItem?,
    onSave: (CustomItem) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var label by remember { mutableStateOf(item?.label ?: "") }
    var selectedColor by remember { mutableStateOf(item?.color ?: colorPalette.first()) }
    val isNew = item == null
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SalesCrmTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (isNew) "Add New Item" else "Edit Item",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                
                Spacer(Modifier.height(20.dp))
                
                // Label Input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label", color = SalesCrmTheme.colors.textMuted) },
                    placeholder = { Text("Enter label...", color = SalesCrmTheme.colors.textMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = salesCrmTextFieldColors()
                )
                
                Spacer(Modifier.height(20.dp))
                
                // Color Selection
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = SalesCrmTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(Modifier.height(12.dp))
                
                // Color Grid (5 columns)
                val columns = 5
                val rows = (colorPalette.size + columns - 1) / columns
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until columns) {
                                val index = row * columns + col
                                if (index < colorPalette.size) {
                                    val color = colorPalette[index]
                                    val isSelected = color == selectedColor
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(color))
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedColor = color },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Preview:",
                        style = MaterialTheme.typography.labelMedium,
                        color = SalesCrmTheme.colors.textMuted
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(selectedColor).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(selectedColor), CircleShape)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                label.ifBlank { "Label" },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(selectedColor),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SalesCrmTheme.colors.textSecondary
                        )
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSave(
                                CustomItem(
                                    id = item?.id ?: "",
                                    label = label.trim(),
                                    color = selectedColor,
                                    order = item?.order ?: 0
                                )
                            )
                        },
                        enabled = label.isNotBlank(),
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(if (isNew) "Add" else "Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDateTimePickerSheet(
    initialDate: LocalDate,
    initialTime: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalTime?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Track which month we are viewing in the day selector
    var viewMonth by remember { mutableStateOf(initialDate.withDayOfMonth(1)) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedTime by remember { mutableStateOf(initialTime ?: LocalTime.of(11, 0)) }
    
    // Keep internal state for "Advanced" pickers
    var showAdvancedDatePicker by remember { mutableStateOf(false) }
    var showAdvancedTimePicker by remember { mutableStateOf(false) }

    if (showAdvancedDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showAdvancedDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { 
                        selectedDate = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000)) 
                    }
                    showAdvancedDatePicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = { 
                TextButton(onClick = { showAdvancedDatePicker = false }) { Text("Cancel", color = SalesCrmTheme.colors.textSecondary) } 
            }
        ) { 
            DatePicker(state = datePickerState)
        }
    }

    if (showAdvancedTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute
        )
        AlertDialog(
            onDismissRequest = { showAdvancedTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timeState.hour, timeState.minute)
                    showAdvancedTimePicker = false
                }) { Text("OK", color = PrimaryBlue) }
            },
            dismissButton = { 
                TextButton(onClick = { showAdvancedTimePicker = false }) { Text("Cancel", color = SalesCrmTheme.colors.textSecondary) } 
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Reschedule Task",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SalesCrmTheme.colors.textPrimary
            )
            Spacer(Modifier.height(24.dp))
            
            // Month Selector with 'More' option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Month", style = MaterialTheme.typography.labelMedium, color = SalesCrmTheme.colors.textSecondary)
                TextButton(onClick = { showAdvancedDatePicker = true }, contentPadding = PaddingValues(0.dp)) {
                    Text("More...", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                }
            }
            Spacer(Modifier.height(4.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Current month and next 5 months
                items(6) { i ->
                    val monthDate = LocalDate.now().plusMonths(i.toLong()).withDayOfMonth(1)
                    val isSelected = viewMonth == monthDate
                    SuggestionChip(
                        label = monthDate.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        selected = isSelected,
                        onClick = { viewMonth = monthDate }
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Day Selector for the picked viewMonth
            Text("Day", style = MaterialTheme.typography.labelMedium, color = SalesCrmTheme.colors.textSecondary)
            Spacer(Modifier.height(8.dp))
            val daysInMonth = viewMonth.month.length(viewMonth.isLeapYear)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(daysInMonth) { i ->
                    val dayNum = i + 1
                    val date = viewMonth.withDayOfMonth(dayNum)
                    val isSelected = selectedDate == date
                    val isToday = date == LocalDate.now()
                    
                    Surface(
                        onClick = { selectedDate = date },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.surfaceVariant,
                        border = if (isToday && !isSelected) BorderStroke(1.dp, PrimaryBlue) else null,
                        modifier = Modifier.width(50.dp).height(60.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                date.format(DateTimeFormatter.ofPattern("EEE")),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else SalesCrmTheme.colors.textSecondary
                            )
                            Text(
                                dayNum.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else SalesCrmTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Time Selector with 'More' option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Time", style = MaterialTheme.typography.labelMedium, color = SalesCrmTheme.colors.textSecondary)
                TextButton(onClick = { showAdvancedTimePicker = true }, contentPadding = PaddingValues(0.dp)) {
                    Text("More...", style = MaterialTheme.typography.labelSmall, color = PrimaryBlue)
                }
            }
            Spacer(Modifier.height(4.dp))
            val times = listOf(
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                LocalTime.of(17, 0),
                LocalTime.of(18, 0),
                LocalTime.of(20, 0)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(times) { time ->
                    val isSelected = selectedTime == time
                    SuggestionChip(
                        label = time.toDisplayString(),
                        selected = isSelected,
                        onClick = { selectedTime = time }
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = { onConfirm(selectedDate, selectedTime) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        }
    }
}

