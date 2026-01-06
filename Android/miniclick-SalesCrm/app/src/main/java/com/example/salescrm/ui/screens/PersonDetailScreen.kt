package com.example.salescrm.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.salescrm.data.*
import com.example.salescrm.ui.components.*
import com.example.salescrm.ui.theme.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.salescrm.util.AudioPlayer
import com.example.salescrm.util.rememberAudioPlayerState


private enum class TimelineFilter(val label: String) {
    ALL("All"),
    TASKS_AND_NOTES("Tasks & Notes")
}

private sealed interface TimelineItem {
    val date: LocalDate
    val id: String
    val type: TimelineFilter
    val sortTimestamp: LocalDateTime
}

private data class ActivityItem(val activity: Activity) : TimelineItem {
    override val date: LocalDate get() = activity.timestamp.toLocalDate()
    override val id: String get() = "activity_${activity.id}"
    override val type: TimelineFilter get() = if (activity.type == ActivityType.COMMENT) TimelineFilter.TASKS_AND_NOTES else TimelineFilter.ALL
    override val sortTimestamp: LocalDateTime get() = activity.timestamp
}

private data class CompletedTaskItem(val task: Task) : TimelineItem {
    override val date: LocalDate get() = (task.completedAt ?: task.createdAt).toLocalDate()
    override val id: String get() = "completed_task_${task.id}"
    override val type: TimelineFilter get() = TimelineFilter.TASKS_AND_NOTES
    override val sortTimestamp: LocalDateTime get() = task.completedAt ?: task.createdAt
}

private data class UpcomingTaskItem(val task: Task) : TimelineItem {
    override val date: LocalDate get() = task.dueDate
    override val id: String get() = "upcoming_task_${task.id}"
    override val type: TimelineFilter get() = TimelineFilter.TASKS_AND_NOTES
    override val sortTimestamp: LocalDateTime get() = LocalDateTime.of(task.dueDate, task.dueTime ?: LocalTime.MIN)
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PersonDetailSheet(
    person: Person,
    tasks: List<Task>,
    activities: List<Activity>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStageChange: (String) -> Unit,
    onPriorityChange: (String) -> Unit,
    onSegmentChange: (String) -> Unit,
    onLabelAdd: (String) -> Unit,
    onLabelRemove: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onToggleTask: (Task) -> Unit,
    onAddComment: () -> Unit,
    onCommentChange: (Activity, String) -> Unit,
    onCommentDelete: (Activity) -> Unit,
    onBudgetChange: (String) -> Unit,
    onTogglePipeline: () -> Unit,
    onDeletePerson: () -> Unit,
    onCall: () -> Unit = {},
    onWhatsApp: () -> Unit = {},
    onAddActivity: () -> Unit = {},
    onActivityClick: (Activity) -> Unit = {},
    currencySymbol: String = "₹",
    budgetMultiplier: Int = 100000,
    defaultCountry: String = "US",
    defaultWhatsAppPackage: String = "always_ask"
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Dialog States
    var showLabelDialog by remember { mutableStateOf(false) }
    var showMainNoteDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showDeletePersonConfirm by remember { mutableStateOf(false) }
    var showTogglePipelineConfirm by remember { mutableStateOf(false) }
    var showDeleteCommentConfirm by remember { mutableStateOf<Activity?>(null) }
    var editingComment by remember { mutableStateOf<Activity?>(null) }
    if (showLabelDialog) {
        LabelManagementDialog(
            currentLabels = person.labels,
            onAdd = onLabelAdd,
            onRemove = onLabelRemove,
            onDismiss = { showLabelDialog = false }
        )
    }

    if (showMainNoteDialog) {
        EditMainNoteSheet(
            initialNote = person.note,
            onConfirm = { 
                onNoteChange(it)
                showMainNoteDialog = false
            },
            onDismiss = { showMainNoteDialog = false }
        )
    }
    
    if (showBudgetDialog) {
        EditBudgetSheet(
            initialBudget = person.budget,
            currencySymbol = currencySymbol,
            onConfirm = {
                onBudgetChange(it)
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false }
        )
    }

    if (showDeletePersonConfirm) {
        ConfirmSheet(
            title = "Delete Person",
            message = "Are you sure you want to delete ${person.name}? This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                onDeletePerson()
                showDeletePersonConfirm = false
            },
            onDismiss = { showDeletePersonConfirm = false }
        )
    }

    if (showTogglePipelineConfirm) {
        ConfirmSheet(
            title = if (person.isInPipeline) "Move to Contacts" else "Add to Pipeline",
            message = if (person.isInPipeline) 
                "Are you sure you want to move ${person.name} to Contacts? They will no longer appear in the pipeline."
                else "Are you sure you want to add ${person.name} to the Pipeline?",
            confirmText = if (person.isInPipeline) "Move" else "Add",
            onConfirm = {
                onTogglePipeline()
                showTogglePipelineConfirm = false
            },
            onDismiss = { showTogglePipelineConfirm = false }
        )
    }

    showDeleteCommentConfirm?.let { comment ->
        ConfirmSheet(
            title = "Delete Comment",
            message = "Are you sure you want to delete this comment?",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                onCommentDelete(comment)
                showDeleteCommentConfirm = null
            },
            onDismiss = { showDeleteCommentConfirm = null }
        )
    }
    
    editingComment?.let { comment ->
        EditNoteSheet(
            initialContent = comment.description,
            onConfirm = { content ->
                onCommentChange(comment, content)
                editingComment = null
            },
            onDelete = {
                showDeleteCommentConfirm = comment
                editingComment = null
            },
            onDismiss = { editingComment = null }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted) },
        modifier = Modifier.fillMaxHeight(0.95f) 
    ) {
        // Fixed Header in the BottomSheet (Only Name/Nav and Budget/Labels Row)
        Column(Modifier.fillMaxWidth()) {
            // 1. Top Bar: Back, Name, Edit, Fullscreen
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SalesCrmTheme.colors.textPrimary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        person.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SalesCrmTheme.colors.textPrimary,
                        maxLines = 1
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Edit", tint = SalesCrmTheme.colors.textSecondary)
                    }
                    // Fullscreen button removed or kept? User said "I don't want half screen mode always open 90%". 
                    // So we might not need "Full Screen" button if it's already max.
                    // But maybe user wants "Full screen" vs "90%". 
                    // Let's keep it but it might not be needed if skipPartiallyExpanded=true.
                    // Actually, skipPartiallyExpanded=true forces it to expand state. 
                }
            }
            
            // Address Row
            if (person.address.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = SalesCrmTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        person.address, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = SalesCrmTheme.colors.textMuted,
                        maxLines = 1
                    )
                }
            }
            
            // 2. Budget, Priority, Labels Row (Horizontal Scroll)
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 // 1. Budget Chip
                 item {
                     Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable { showBudgetDialog = true }
                    ) {
                        Text(
                            if (person.budget.isBlank()) "+ Budget" else formatBudget(person.budget, currencySymbol, budgetMultiplier), 
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                 }
                 
                  // 2. Priority Dropdown Chip
                  item {
                      var priorityExpanded by remember { mutableStateOf(false) }
                      val customPriorities = SalesCrmTheme.priorities
                      val priorityItem = customPriorities.findById(if (person.isInPipeline) person.pipelinePriorityId else person.priorityId)
                      val priorityColor = Color(priorityItem?.color ?: 0xFF6B7280)
                      
                      Box {
                          Surface(
                             shape = RoundedCornerShape(8.dp),
                             color = priorityColor.copy(alpha = 0.1f),
                             border = androidx.compose.foundation.BorderStroke(1.dp, priorityColor.copy(alpha = 0.3f)),
                             modifier = Modifier.clickable { priorityExpanded = true }
                         ) {
                             Row(
                                 modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Icon(
                                     Icons.Default.Flag,
                                     contentDescription = null,
                                     tint = priorityColor,
                                     modifier = Modifier.size(14.dp)
                                 )
                                 Spacer(Modifier.width(4.dp))
                                 Text(
                                     priorityItem?.label ?: "None", 
                                     style = MaterialTheme.typography.labelMedium,
                                     color = priorityColor,
                                     fontWeight = FontWeight.Medium
                                 )
                                 Icon(
                                     Icons.Default.ArrowDropDown,
                                     null,
                                     tint = priorityColor,
                                     modifier = Modifier.size(16.dp)
                                 )
                             }
                         }
                        
                        DropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false },
                            modifier = Modifier.background(SalesCrmTheme.colors.surface).width(160.dp)
                        ) {
                            // Subhead
                            Text(
                                "Priority",
                                style = MaterialTheme.typography.labelSmall,
                                color = SalesCrmTheme.colors.textMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            HorizontalDivider(color = SalesCrmTheme.colors.border)
                            
                            customPriorities.forEach { pItem ->
                                val pColor = Color(pItem.color)
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Flag,
                                            null,
                                            tint = pColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    text = { 
                                        val currentPId = if (person.isInPipeline) person.pipelinePriorityId else person.priorityId
                                        Text(
                                            pItem.label, 
                                            color = pColor,
                                            fontWeight = if (pItem.id == currentPId) FontWeight.Bold else FontWeight.Normal
                                        ) 
                                    },
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPriorityChange(pItem.id)
                                        priorityExpanded = false
                                    }
                                )
                            }
                        }
                     }
                 }
                 
                 // 3. Labels
                 items(person.labels) { label ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SalesCrmTheme.colors.surfaceVariant,
                        modifier = Modifier.clickable { showLabelDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Label,
                                contentDescription = null,
                                tint = SalesCrmTheme.colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                label, 
                                style = MaterialTheme.typography.labelMedium,
                                color = SalesCrmTheme.colors.textSecondary
                            )
                        }
                    }
                }
                
                // 4. Add Label
                item {
                     Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SalesCrmTheme.colors.surfaceVariant,
                        modifier = Modifier.clickable { showLabelDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add, "Add Label", 
                                tint = SalesCrmTheme.colors.textMuted, 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Label",
                                style = MaterialTheme.typography.labelMedium,
                                color = SalesCrmTheme.colors.textMuted
                            )
                        }
                    }
                }
            }
            
            HorizontalDivider(color = SalesCrmTheme.colors.border)
        }
        
        var tasksNotesOnly by remember { mutableStateOf(false) }

        // Include activities and tasks in the key to trigger recomputation when they change
        val timelineItems by remember(person.id, tasksNotesOnly, activities, tasks) {
            derivedStateOf {
                val activityItems = activities.filter { it.personId == person.id }.map { ActivityItem(it) }
                val cTasks = tasks.filter { it.linkedPersonId == person.id && it.status == TaskStatus.COMPLETED }
                    .map { CompletedTaskItem(it) }
                val uTasks = tasks.filter { it.linkedPersonId == person.id && it.status != TaskStatus.COMPLETED }
                    .map { UpcomingTaskItem(it) }
                
                val filtered = (activityItems + cTasks + uTasks)
                    .filter { !tasksNotesOnly || it.type == TimelineFilter.TASKS_AND_NOTES }
                
                // Divide into Upcoming and History
                val upcoming = filtered.filterIsInstance<UpcomingTaskItem>()
                    .sortedBy { it.sortTimestamp }
                
                val history = filtered.filter { it !is UpcomingTaskItem }
                    .sortedByDescending { it.sortTimestamp }
                    
                upcoming + history
            }
        }

        // 3. Scrollable Content
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            modifier = Modifier.fillMaxSize().background(SalesCrmTheme.colors.background)
        ) {
            
            // === STAGE ===
            item {
                if (person.isInPipeline) {
                    var expanded by remember { mutableStateOf(false) }
                    val customStages = SalesCrmTheme.stages
                    val stageItem = customStages.findById(person.stageId)
                    val stageColor = Color(stageItem?.color ?: 0xFF6B7280)
                    var anchorWidth by remember { mutableStateOf(0) }
                    
                    Box(Modifier.fillMaxWidth().padding(bottom = 12.dp).onGloballyPositioned { anchorWidth = it.size.width }) {
                        Surface(
                           modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                           shape = RoundedCornerShape(8.dp),
                           color = stageColor.copy(alpha = 0.15f),
                           border = androidx.compose.foundation.BorderStroke(1.dp, stageColor)
                       ) {
                           Row(
                               Modifier.padding(12.dp).fillMaxWidth(),
                               verticalAlignment = Alignment.CenterVertically,
                               horizontalArrangement = Arrangement.SpaceBetween
                           ) {
                               Text(
                                   stageItem?.label ?: "Unknown",
                                   color = stageColor,
                                   fontWeight = FontWeight.Bold
                               )
                               Icon(
                                   Icons.Default.ArrowDropDown, 
                                   null, 
                                   tint = stageColor
                               )
                           }
                       }
                       
                       DropdownMenu(
                           expanded = expanded,
                           onDismissRequest = { expanded = false },
                           modifier = Modifier
                               .background(SalesCrmTheme.colors.surface)
                               .width(with(LocalDensity.current) { anchorWidth.toDp() })
                       ) {
                           // Subhead
                           Text(
                               "Stage",
                               style = MaterialTheme.typography.labelSmall,
                               color = SalesCrmTheme.colors.textMuted,
                               modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                           )
                           HorizontalDivider(color = SalesCrmTheme.colors.border)
                           
                           customStages.forEach { stageItem ->
                               val sColor = Color(stageItem.color)
                               DropdownMenuItem(
                                   text = { 
                                       Text(
                                           stageItem.label, 
                                           color = sColor,
                                           fontWeight = if (stageItem.id == person.stageId) FontWeight.Bold else FontWeight.Normal
                                       ) 
                                   },
                                   onClick = { 
                                       onStageChange(stageItem.id)
                                       expanded = false
                                   }
                               )
                           }
                       }
                    }
               }
            }

            // === MAIN NOTE ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showMainNoteDialog = true },
                    colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp), 
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Notes, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        if (person.note.isBlank()) {
                            Text("Add main note...", style = MaterialTheme.typography.bodyMedium, color = SalesCrmTheme.colors.textMuted, fontStyle = FontStyle.Italic)
                        } else {
                            Text(person.note, style = MaterialTheme.typography.bodyMedium, color = SalesCrmTheme.colors.textPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            
            // === QUICK ACTIONS (Horizontally Scrollable) ===
            item {
                val cleanPhone = person.phone.replace(Regex("[^0-9]"), "")
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    // 1. Call
                    item {
                        QuickActionButton(Icons.Default.Call, "Call", AccentGreen) {
                            onCall()
                            try { 
                                val dialPhone = CallLogRepository.formatPhoneForDialer(person.phone, defaultCountry)
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialPhone"))) 
                            } catch (e: Exception) {}
                        }
                    }
                    // 2. WhatsApp
                    item {
                        QuickActionButton(Icons.AutoMirrored.Filled.Send, "WhatsApp", Color(0xFF25D366)) {
                            onWhatsApp()
                            try { context.startActivity(CallLogRepository.createWhatsAppChooserIntent(context, person.phone, defaultWhatsAppPackage)) } catch (e: Exception) {}
                        }
                    }
                    // 3. Add Task
                    item {
                        QuickActionButton(Icons.Default.AddTask, "Add Task", PrimaryBlue) {
                            onAddTask()
                        }
                    }
                    // 4. Add Comment
                    item {
                        QuickActionButton(Icons.Default.NoteAdd, "Add Comment", AccentPurple) {
                            onAddComment()
                        }
                    }
                    // 5. Log Activity
                    item {
                        QuickActionButton(Icons.Default.History, "+ Activity", AccentOrange) {
                            onAddActivity()
                        }
                    }
                    // 6. Copy Phone
                    item {
                        QuickActionButton(Icons.Default.ContentCopy, "Copy", SalesCrmTheme.colors.textSecondary) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Phone", person.phone)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Phone copied!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = SalesCrmTheme.colors.border)
            }
            

            

            

            
            



            // Show Timeline section
            if (timelineItems.isNotEmpty() || tasksNotesOnly) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionHeader(
                        title = "Timeline",
                        action = {
                            Surface(
                                onClick = { tasksNotesOnly = !tasksNotesOnly },
                                color = if (tasksNotesOnly) PrimaryBlue.copy(alpha = 0.1f) else SalesCrmTheme.colors.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = if (tasksNotesOnly) BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (tasksNotesOnly) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "Tasks & Notes",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (tasksNotesOnly) FontWeight.Bold else FontWeight.Normal,
                                        color = if (tasksNotesOnly) PrimaryBlue else SalesCrmTheme.colors.textMuted
                                    )
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                itemsIndexed(timelineItems, key = { _, item -> item.id }) { index, item ->
                    val isLast = index == timelineItems.lastIndex
                    val haptic = LocalHapticFeedback.current

                    // Determine indicator color
                    val indicatorColor = when {
                        index == 0 && item is ActivityItem -> PrimaryBlue
                        item is CompletedTaskItem -> AccentGreen
                        item is UpcomingTaskItem -> AccentOrange
                        item is ActivityItem && item.activity.type == ActivityType.COMMENT -> AccentPurple
                        else -> SalesCrmTheme.colors.textMuted
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                    ) {
                        // Header Row (Dot aligned with Title/Subhead)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timeline connector + Dot
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Top line segment (connects from previous)
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight(0.5f)
                                            .align(Alignment.TopCenter)
                                            .background(SalesCrmTheme.colors.border.copy(alpha = 0.4f))
                                    )
                                }
                                // Bottom line segment (connects to body)
                                // We always show this if there's a body or if it's not the last item
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .fillMaxHeight(0.5f)
                                        .align(Alignment.BottomCenter)
                                        .background(SalesCrmTheme.colors.border.copy(alpha = 0.4f))
                                )
                                
                                // Circle dot
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(indicatorColor, CircleShape)
                                )
                            }
                            
                            Spacer(Modifier.width(12.dp))

                            // Header content (Title + Timestamp)
                            when (item) {
                                is ActivityItem -> {
                                    val activity = item.activity
                                    val isComment = activity.type == ActivityType.COMMENT
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isComment) "Comment" else (activity.title ?: activity.type.label),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isComment) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isComment) AccentPurple else if (index == 0) PrimaryBlue else SalesCrmTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = activity.timestamp.toDisplayString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SalesCrmTheme.colors.textMuted,
                                            modifier = Modifier.clickable {
                                                Toast.makeText(context, activity.timestamp.toFullDateTimeString(), Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                                is CompletedTaskItem -> {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Completed Task",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val displayTime = item.task.completedAt ?: item.task.createdAt
                                        Text(
                                            displayTime.toDisplayString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SalesCrmTheme.colors.textMuted,
                                            modifier = Modifier.clickable {
                                                Toast.makeText(context, displayTime.toFullDateTimeString(), Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                                is UpcomingTaskItem -> {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Upcoming Task",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AccentOrange,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            item.date.toDisplayString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SalesCrmTheme.colors.textMuted,
                                            modifier = Modifier.clickable {
                                                Toast.makeText(context, item.date.toFullDateTimeString(item.task.dueTime), Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Body Row (Content below dot)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            // Vertical Line only
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (!isLast) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .fillMaxHeight()
                                            .background(SalesCrmTheme.colors.border.copy(alpha = 0.4f))
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = if (isLast) 0.dp else 16.dp)
                            ) {
                                when (item) {
                                    is ActivityItem -> {
                                        val activity = item.activity
                                        if (activity.type == ActivityType.COMMENT) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                                    .clickable { editingComment = activity },
                                                colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surface),
                                                border = BorderStroke(1.dp, SalesCrmTheme.colors.border)
                                            ) {
                                                Text(
                                                    text = activity.description,
                                                    color = SalesCrmTheme.colors.textPrimary,
                                                    modifier = Modifier.padding(12.dp),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        } else {
                                            Column(modifier = Modifier.clickable { onActivityClick(activity) }) {
                                                if (activity.description.isNotBlank()) {
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        activity.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = SalesCrmTheme.colors.textSecondary,
                                                        maxLines = 2
                                                    )
                                                }
                                                if (activity.recordingPath != null) {
                                                    val audioState = rememberAudioPlayerState()
                                                    val isThisPlaying = audioState.isPlayingPath(activity.recordingPath!!)
                                                    
                                                    Spacer(Modifier.height(8.dp))
                                                    Surface(
                                                        onClick = {
                                                            audioState.toggle(activity.recordingPath!!) { error ->
                                                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        color = if (isThisPlaying) AccentGreen.copy(alpha = 0.1f) else SalesCrmTheme.colors.surfaceVariant,
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = BorderStroke(1.dp, if (isThisPlaying) AccentGreen.copy(alpha = 0.5f) else SalesCrmTheme.colors.border)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isThisPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                                contentDescription = null,
                                                                tint = if (isThisPlaying) AccentGreen else PrimaryBlue,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                            Text(
                                                                if (isThisPlaying) "Playing..." else "Play Recording",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = if (isThisPlaying) AccentGreen else PrimaryBlue
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    is CompletedTaskItem -> {
                                        TaskCard(
                                            task = item.task,
                                            personName = null,
                                            onToggle = { onToggleTask(item.task) },
                                            onClick = { onTaskClick(item.task) },
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        )
                                    }
                                    is UpcomingTaskItem -> {
                                        TaskCard(
                                            task = item.task,
                                            personName = null,
                                            onToggle = { onToggleTask(item.task) },
                                            onClick = { onTaskClick(item.task) },
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
             // === ADDITIONAL INFO SECTION ===
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader("Additional Info")
                Spacer(Modifier.height(12.dp))
            }
            
            item {
                // Modern Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SalesCrmTheme.colors.border.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // User ID
                        InfoItemRow(
                            icon = Icons.Default.Tag,
                            label = "User ID",
                            value = "#${person.id}",
                            valueColor = SalesCrmTheme.colors.textSecondary
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = SalesCrmTheme.colors.border.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                        
                        // Source
                        val sourceItem = SalesCrmTheme.sources.findById(person.sourceId)
                        if (person.sourceId != "other" && sourceItem != null) {
                            InfoItemRow(
                                icon = Icons.Default.Public,
                                label = "Source",
                                value = sourceItem.label,
                                valueColor = PrimaryBlue
                            )
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = SalesCrmTheme.colors.border.copy(alpha = 0.3f))
                            Spacer(Modifier.height(12.dp))
                        }
                        
                        // Created Date
                        InfoItemRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Created",
                            value = person.createdAt.toDisplayString(),
                            onLongClick = { 
                                Toast.makeText(context, person.createdAt.toFullDateTimeString(), Toast.LENGTH_SHORT).show() 
                            }
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = SalesCrmTheme.colors.border.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                        
                        // Modified Date
                        InfoItemRow(
                            icon = Icons.Default.Update,
                            label = "Last Modified",
                            value = person.updatedAt.toDisplayString(),
                            onLongClick = { 
                                Toast.makeText(context, person.updatedAt.toFullDateTimeString(), Toast.LENGTH_SHORT).show() 
                            }
                        )
                        
                        // Last Activity
                        val lastActivity = activities.filter { it.personId == person.id }.maxByOrNull { it.timestamp }
                        if (lastActivity != null) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = SalesCrmTheme.colors.border.copy(alpha = 0.3f))
                            Spacer(Modifier.height(12.dp))
                            
                            InfoItemRow(
                                icon = Icons.Default.History,
                                label = "Last Activity",
                                value = lastActivity.timestamp.toDisplayString(),
                                onLongClick = { Toast.makeText(context, lastActivity.timestamp.toFullDateTimeString(), Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
            }
            
            // === ACTIONS SECTION ===
            item {
                // Pipeline Toggle Button
                Button(
                    onClick = { showTogglePipelineConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (person.isInPipeline) SalesCrmTheme.colors.surfaceVariant else PrimaryBlue
                    )
                ) {
                    Icon(
                        if (person.isInPipeline) Icons.Default.PersonAdd else Icons.Default.Star,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (person.isInPipeline) "Move to Contacts" else "Add to Pipeline",
                        color = if (person.isInPipeline) SalesCrmTheme.colors.textPrimary else Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Add to Phonebook Button
                Button(
                    onClick = {
                        val intent = Intent(android.provider.ContactsContract.Intents.Insert.ACTION).apply {
                            type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(android.provider.ContactsContract.Intents.Insert.NAME, person.name)
                            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, person.phone)
                            if (person.address.isNotBlank()) {
                                putExtra(android.provider.ContactsContract.Intents.Insert.POSTAL, person.address)
                            }
                        }
                        try { context.startActivity(intent) } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open contacts", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SalesCrmTheme.colors.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.ContactPhone,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = SalesCrmTheme.colors.textPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Add to Phonebook",
                        color = SalesCrmTheme.colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Delete Button
                OutlinedButton(
                    onClick = { showDeletePersonConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Person", fontWeight = FontWeight.Medium)
                }
                
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}




@Composable
fun QuickActionButton(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(50.dp).clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SalesCrmTheme.colors.textSecondary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoItemRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = SalesCrmTheme.colors.textPrimary,
    onLongClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth().combinedClickable(
            onClick = {
                onLongClick?.invoke()
            },
            onLongClick = {
                if (onLongClick != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            }
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = SalesCrmTheme.colors.textMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = SalesCrmTheme.colors.textMuted
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AppDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = SalesCrmTheme.colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = SalesCrmTheme.colors.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, SalesCrmTheme.colors.border)
        ) {
            Row(
                Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedValue, color = SalesCrmTheme.colors.textPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, null, tint = SalesCrmTheme.colors.textSecondary)
            }
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SalesCrmTheme.colors.surface),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = SalesCrmTheme.colors.textPrimary) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LabelManagementDialog(
    currentLabels: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    // Filter available tags based on search query
    val filteredTags = remember(searchQuery) {
        SampleData.availableLabels.filter { 
            searchQuery.isBlank() || it.contains(searchQuery, ignoreCase = true) 
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted.copy(alpha = 0.5f)) },
        windowInsets = WindowInsets.ime
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Manage Tags",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(SalesCrmTheme.colors.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // === SELECTED TAGS (Above Search) ===
            if (currentLabels.isNotEmpty()) {
                Text(
                    "SELECTED",
                    style = MaterialTheme.typography.labelSmall,
                    color = SalesCrmTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(currentLabels) { label ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = PrimaryBlue,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                        .clickable { 
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onRemove(label) 
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // === SEARCH BAR ===
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SalesCrmTheme.colors.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, SalesCrmTheme.colors.border.copy(alpha = 0.5f))
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search or create new tag...", color = SalesCrmTheme.colors.textMuted) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = PrimaryBlue
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = SalesCrmTheme.colors.textMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                onAdd(searchQuery.trim())
                                searchQuery = ""
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }) {
                                Icon(Icons.Default.AddCircle, "Add", tint = PrimaryBlue)
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // === ALL TAGS (Live Filtered) ===
            Text(
                if (searchQuery.isBlank()) "ALL TAGS" else "RESULTS",
                style = MaterialTheme.typography.labelSmall,
                color = SalesCrmTheme.colors.textMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(12.dp))

            if (filteredTags.isEmpty() && searchQuery.isNotBlank()) {
                // No matches - offer to create
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SalesCrmTheme.colors.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, SalesCrmTheme.colors.border.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No matching tags",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SalesCrmTheme.colors.textMuted
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                onAdd(searchQuery.trim())
                                searchQuery = ""
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = PrimaryBlue
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Create \"$searchQuery\"",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredTags.forEach { label ->
                        val isSelected = currentLabels.contains(label)
                        Surface(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isSelected) onRemove(label) else onAdd(label) 
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.surfaceVariant.copy(alpha = 0.5f),
                            border = if (!isSelected) BorderStroke(1.dp, SalesCrmTheme.colors.border.copy(alpha = 0.5f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) Color.White else SalesCrmTheme.colors.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMainNoteSheet(
    initialNote: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
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
                    "Edit Main Note",
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
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("Enter important note here...", color = SalesCrmTheme.colors.textMuted) },
                shape = RoundedCornerShape(12.dp),
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
                    onClick = { onConfirm(note) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteSheet(
    initialContent: String,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
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
                    "Edit Note",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                Row {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, "Delete", tint = AccentRed)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textSecondary)
                    }
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
                placeholder = { Text("Enter note content...", color = SalesCrmTheme.colors.textMuted) },
                shape = RoundedCornerShape(12.dp),
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
                    onClick = { onConfirm(content) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBudgetSheet(
    initialBudget: String,
    currencySymbol: String = "$",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var budget by remember { mutableStateOf(initialBudget) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    "Edit Budget",
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
                value = budget,
                onValueChange = { newValue ->
                    // Only allow digits
                    budget = newValue.filter { it.isDigit() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Enter amount", color = SalesCrmTheme.colors.textMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                prefix = { Text("$currencySymbol ", color = SalesCrmTheme.colors.textSecondary) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                colors = salesCrmTextFieldColors()
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Enter numeric value only",
                style = MaterialTheme.typography.labelSmall,
                color = SalesCrmTheme.colors.textMuted
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
                    onClick = { onConfirm(budget) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
            }
        }
    }
}
