package com.example.salescrm.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.salescrm.data.*
import com.example.salescrm.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

// ==================== KEYBOARD DISMISSAL WRAPPER ====================

/**
 * A wrapper composable for ModalBottomSheet content that handles keyboard dismissal properly.
 * Use this inside ModalBottomSheet to ensure keyboard is dismissed when:
 * - Tapping anywhere outside input fields
 * - Pressing the back button while keyboard is visible
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun KeyboardDismissibleContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    
    // Helper function to hide keyboard using InputMethodManager (most reliable method)
    fun hideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
        // Also use InputMethodManager as a fallback for reliability
        val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val activity = context as? android.app.Activity
        activity?.currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
    
    // Track keyboard visibility using multiple methods for maximum reliability
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isImeVisible = WindowInsets.isImeVisible || imeBottom > 0
    
    // BackHandler to dismiss keyboard when back is pressed and keyboard is visible
    androidx.activity.compose.BackHandler(enabled = isImeVisible) {
        hideKeyboard()
    }
    
    // Wrap content in Box with tap gesture detection
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    hideKeyboard()
                })
            }
    ) {
        content()
    }
}

// ==================== PERSON CARD ====================

@Composable
fun PersonCard(
    person: Person,
    onClick: () -> Unit,
    visibleColumns: Set<String> = emptySet(),
    currencySymbol: String = "₹",
    budgetMultiplier: Int = 100000,
    modifier: Modifier = Modifier
) {
    val customStages = SalesCrmTheme.stages
    val customPriorities = SalesCrmTheme.priorities
    val customSegments = SalesCrmTheme.segments

    val stageItem = customStages.findById(person.stageId)
    val priorityItem = customPriorities.findById(person.pipelinePriorityId)
    val segmentItem = customSegments.findById(person.segmentId)

    val stageColor = if (person.isInPipeline) Color(stageItem?.color ?: 0xFF6B7280) else Color.Gray
    val priorityColor = if (person.isInPipeline) Color(priorityItem?.color ?: 0xFF6B7280) else Color.Transparent
    val segmentColor = Color(segmentItem?.color ?: 0xFF6B7280)
    
    fun isVisible(id: String) = visibleColumns.contains(id)

    val haptic = LocalHapticFeedback.current

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surface),
        border = BorderStroke(1.dp, SalesCrmTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Name and Priority/Budget Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SalesCrmTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (person.isInPipeline && isVisible("budget") && person.budget.isNotBlank()) {
                        Text(
                            text = formatBudget(person.budget, currencySymbol, budgetMultiplier),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SalesCrmTheme.colors.textPrimary
                        )
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                // Details Row 1: Stage (Pipeline) / Segment (Contact)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (person.isInPipeline && isVisible("stage")) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = stageColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stageItem?.label ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = stageColor,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    
                    if (isVisible("segment")) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = segmentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = segmentItem?.label ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = segmentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Details Row 2: Priority (if pipeline)
                 if (person.isInPipeline && isVisible("priority") && person.pipelinePriorityId != "none") {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = priorityColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = priorityItem?.label ?: "None",
                            style = MaterialTheme.typography.bodySmall,
                            color = priorityColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                 }
                
                // Details Row 3: Phone & Source
                val sourceItem = SalesCrmTheme.sources.findById(person.sourceId)
                if ((isVisible("phone") && person.phone.isNotBlank()) || (isVisible("source") && person.sourceId != "other")) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isVisible("phone") && person.phone.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = SalesCrmTheme.colors.textMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = person.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = SalesCrmTheme.colors.textMuted
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        
                        if (isVisible("source") && person.sourceId != "other" && sourceItem != null) {
                            Text(
                                text = "via ${sourceItem.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SalesCrmTheme.colors.textSecondary
                            )
                        }
                    }
                }
                
                // Labels
                if (isVisible("labels") && person.labels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Label,
                            contentDescription = null,
                            tint = SalesCrmTheme.colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        person.labels.take(3).forEach { label ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SalesCrmTheme.colors.surfaceVariant
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SalesCrmTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
                
                // Note
                if (isVisible("note") && person.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = null,
                            tint = SalesCrmTheme.colors.textMuted,
                            modifier = Modifier.size(14.dp).padding(top=2.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = person.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = SalesCrmTheme.colors.textMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PersonList(
    people: List<Person>,
    viewMode: ViewMode,
    visibleColumns: Set<String>,
    onPersonClick: (Person) -> Unit,
    currencySymbol: String = "₹",
    budgetMultiplier: Int = 100000,
    modifier: Modifier = Modifier
) {
    if (people.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No results found", color = SalesCrmTheme.colors.textMuted)
        }
    } else {
        if (viewMode == ViewMode.TABLE) {
            PersonDataTable(
                people = people,
                visibleColumns = visibleColumns,
                onPersonClick = onPersonClick,
                currencySymbol = currencySymbol,
                budgetMultiplier = budgetMultiplier,
                modifier = modifier
            )
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = if (viewMode == ViewMode.CARD) PaddingValues(horizontal = 6.dp, vertical = 12.dp) else PaddingValues(0.dp),
                verticalArrangement = if (viewMode == ViewMode.CARD) Arrangement.spacedBy(8.dp) else Arrangement.Top
            ) {
                items(people, key = { it.id }) { person ->
                    when (viewMode) {
                        ViewMode.LIST -> {
                            PersonListItem(
                                person = person,
                                onClick = { onPersonClick(person) },
                                visibleColumns = visibleColumns,
                                currencySymbol = currencySymbol,
                                budgetMultiplier = budgetMultiplier
                            )
                        }
                        ViewMode.CARD -> {
                            PersonCard(
                                person = person,
                                onClick = { onPersonClick(person) },
                                visibleColumns = visibleColumns,
                                currencySymbol = currencySymbol,
                                budgetMultiplier = budgetMultiplier
                            )
                        }
                        else -> {} // Table handled above
                    }
                }
            }
        }
    }
}


// ==================== PERSON LIST ITEM (LIST VIEW) ====================

@Composable
fun PersonListItem(
    person: Person,
    onClick: () -> Unit,
    visibleColumns: Set<String> = emptySet(),
    currencySymbol: String = "₹",
    budgetMultiplier: Int = 100000,
    modifier: Modifier = Modifier
) {
    val customStages = SalesCrmTheme.stages
    val customPriorities = SalesCrmTheme.priorities
    val customSegments = SalesCrmTheme.segments

    val stageItem = customStages.findById(person.stageId)
    val priorityItem = customPriorities.findById(if (person.isInPipeline) person.pipelinePriorityId else person.priorityId)
    val segmentItem = customSegments.findById(person.segmentId)

    val priorityColor = Color(priorityItem?.color ?: 0xFF6B7280)
    val stageColor = Color(stageItem?.color ?: 0xFF6B7280)
    val segmentColor = Color(segmentItem?.color ?: 0xFF6B7280)
    
    fun isVisible(id: String) = visibleColumns.contains(id)
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        color = SalesCrmTheme.colors.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = SalesCrmTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (person.isInPipeline && isVisible("priority") && person.pipelinePriorityId != "none") {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(priorityColor, CircleShape)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isVisible("phone") && person.phone.isNotBlank()) {
                        Text(
                            text = person.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = SalesCrmTheme.colors.textMuted
                        )
                    }
                    
                    if (person.isInPipeline && isVisible("stage")) {
                        Text(
                            text = " • ${stageItem?.label ?: "Unknown"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = stageColor,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (isVisible("segment")) {
                        Text(
                            text = " • ${segmentItem?.label ?: "Unknown"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = segmentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Right Side: Budget
            Column(horizontalAlignment = Alignment.End) {
                if (person.isInPipeline && isVisible("budget") && person.budget.isNotBlank()) {
                    Text(
                        text = formatBudget(person.budget, currencySymbol, budgetMultiplier),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                }
            }
            
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SalesCrmTheme.colors.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = SalesCrmTheme.colors.border,
            thickness = 0.5.dp
        )
    }
    }
}

// ==================== PERSON TABLE ROW (TABLE VIEW) ====================

@Composable
fun PersonTableRow(
    person: Person,
    onClick: () -> Unit,
    visibleColumns: Set<String> = emptySet(),
    currencySymbol: String = "₹",
    budgetMultiplier: Int = 100000,
    modifier: Modifier = Modifier
) {
    val customStages = SalesCrmTheme.stages
    val customSegments = SalesCrmTheme.segments
    
    val stageItem = customStages.findById(person.stageId)
    val segmentItem = customSegments.findById(person.segmentId)
    
    val stageColor = Color(stageItem?.color ?: 0xFF6B7280)
    val segmentColor = Color(segmentItem?.color ?: 0xFF6B7280)
    
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        color = SalesCrmTheme.colors.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Name (always visible)
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = SalesCrmTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(Modifier.width(8.dp))
            
            // Stage or Segment badge
            if (person.isInPipeline) {
                Text(
                    text = stageItem?.label ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall,
                    color = stageColor,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = segmentItem?.label ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall,
                    color = segmentColor,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            // Budget (if pipeline and visible)
            if (person.isInPipeline && person.budget.isNotBlank()) {
                Text(
                    text = formatBudget(person.budget, currencySymbol, budgetMultiplier),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
            }
            
            Spacer(Modifier.width(4.dp))
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SalesCrmTheme.colors.textMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        HorizontalDivider(
            color = SalesCrmTheme.colors.border,
            thickness = 0.5.dp
        )
    }
    }
}

// ==================== STAGE TAB ROW ====================

@Composable
fun StageTabRow(
    stages: List<CustomItem>,
    selectedStageId: String?,
    stageCounts: Map<String, Int>,
    onStageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = if (selectedStageId == null) 0 else stages.indexOfFirst { it.id == selectedStageId } + 1,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        contentColor = SalesCrmTheme.colors.textPrimary,
        edgePadding = 16.dp,
        indicator = {},
        divider = {}
    ) {
        // "All" tab
        Tab(
            selected = selectedStageId == null,
            onClick = { onStageSelected(null) },
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                text = "All (${stageCounts.values.sum()})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selectedStageId == null) FontWeight.Bold else FontWeight.Normal,
                color = if (selectedStageId == null) PrimaryBlue else SalesCrmTheme.colors.textSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        
        stages.forEach { stageItem ->
            val isSelected = stageItem.id == selectedStageId
            val count = stageCounts[stageItem.id] ?: 0
            val stageColor = Color(stageItem.color)
            
            Tab(
                selected = isSelected,
                onClick = { onStageSelected(stageItem.id) },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "${stageItem.label} ($count)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) stageColor else SalesCrmTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

// ==================== TASK CARD ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    personName: String? = null,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onViewProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val priorityItem = SalesCrmTheme.priorities.findById(task.priorityId)
    val priorityColor = Color(priorityItem?.color ?: 0xFF6B7280)
    val isOverdue = task.dueDate.isBefore(LocalDate.now()) && task.status == TaskStatus.PENDING
    val isCompleted = task.status == TaskStatus.COMPLETED
    
    // Task type icon
    val taskTypeIcon = when (task.type) {
        TaskType.FOLLOW_UP -> Icons.Default.PhoneCallback
        TaskType.MEETING -> Icons.Default.Groups
        TaskType.TO_DO -> Icons.Default.CheckCircle
    }
    
    val haptic = LocalHapticFeedback.current

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isOverdue) AccentRed.copy(alpha = 0.5f) else SalesCrmTheme.colors.border),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) 
                SalesCrmTheme.colors.surfaceVariant.copy(alpha = 0.5f)
            else 
                SalesCrmTheme.colors.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {
            // Overdue indicator bar
            if (isOverdue) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(AccentRed, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                )
            }

            // Main content - Left side
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Row 1: Task Type Icon - Priority Badge - Reminder Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Task type icon
                    Icon(
                        imageVector = taskTypeIcon,
                        contentDescription = task.type.label,
                        modifier = Modifier.size(16.dp),
                        tint = if (isCompleted) SalesCrmTheme.colors.textMuted else SalesCrmTheme.colors.textPrimary
                    )
                    
                    // Task type label
                    Text(
                        text = task.type.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompleted) SalesCrmTheme.colors.textMuted else SalesCrmTheme.colors.textPrimary
                    )
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = SalesCrmTheme.colors.textMuted
                    )
                    
                    // Priority badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isCompleted) SalesCrmTheme.colors.textMuted else priorityColor
                        )
                        Text(
                            text = priorityItem?.label ?: "None",
                            color = if (isCompleted) SalesCrmTheme.colors.textMuted else priorityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Reminder indicator
                    if (task.showReminder && !isCompleted) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminder set",
                            modifier = Modifier.size(14.dp),
                            tint = AccentOrange
                        )
                    }
                    
                    // Completed checkmark
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier.size(14.dp),
                            tint = AccentGreen
                        )
                    }
                }
                
                Spacer(Modifier.height(6.dp))
                
                // Row 2: Date & Time - Linked Person
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date/Time icon
                    Icon(
                        imageVector = if (task.dueTime != null) Icons.Default.Schedule else Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (isOverdue) AccentRed else SalesCrmTheme.colors.textMuted
                    )
                    Spacer(Modifier.width(4.dp))
                    
                    val context = LocalContext.current
                    val haptic = LocalHapticFeedback.current
                    // Date & Time
                    val displayString = if (task.dueTime != null) {
                        LocalDateTime.of(task.dueDate, task.dueTime).toDisplayString()
                    } else {
                        task.dueDate.toDisplayString()
                    }
                    
                    Text(
                        text = displayString,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue) AccentRed else SalesCrmTheme.colors.textMuted,
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
                    
                    // Linked person indicator
                    personName?.let { name ->
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Row 3: Task Description
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isCompleted) SalesCrmTheme.colors.textMuted else SalesCrmTheme.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
                
                // Row 4: Response (if exists)
                if (task.response.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                PrimaryBlue.copy(alpha = 0.08f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = "Response",
                            modifier = Modifier.size(14.dp),
                            tint = PrimaryBlue
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = task.response,
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Right side - Profile avatar (if linked to person)
            if (personName != null && onViewProfile != null) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 12.dp)
                        .clickable { onViewProfile() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile avatar
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.3f))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = personName.split(" ").take(2).map { it.firstOrNull() ?: ' ' }.joinToString("").uppercase(),
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    // "Profile" text
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ==================== DATE SCROLLER ====================

@Composable
fun DateHorizontalScroller(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    // Create dates from -7 days to +30 days
    val dates = (-7..30).map { today.plusDays(it.toLong()) }
    val todayIndex = 7 // Today is at index 7 (-7 to 0)
    
    // LazyListState for controlling scroll position
    val listState = rememberLazyListState()
    
    // Scroll to center today on initial composition
    LaunchedEffect(Unit) {
        // Scroll so today is roughly centered (adjust offset for centering)
        listState.scrollToItem(todayIndex, scrollOffset = -100)
    }
    
    LazyRow(
        modifier = modifier.fillMaxWidth(), 
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp), 
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(dates.size) { index ->
            val date = dates[index]
            val isSelected = date == selectedDate
            val isToday = date == today
            
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> PrimaryBlue
                            isToday -> SalesCrmTheme.colors.surfaceVariant
                            else -> Color.Transparent
                        }
                    )
                    .clickable { onDateSelected(date) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    date.dayOfWeek.name.take(3), 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else SalesCrmTheme.colors.textMuted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    date.dayOfMonth.toString(), 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else if (isToday) PrimaryBlue else SalesCrmTheme.colors.textPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    date.month.name.take(3), 
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else SalesCrmTheme.colors.textMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ==================== SEARCH & FILTER BAR ====================

@Composable
fun SearchFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onColumnsClick: () -> Unit,
    filterCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery, 
            onValueChange = onSearchChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search...", color = SalesCrmTheme.colors.textMuted) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = SalesCrmTheme.colors.textMuted) },
            trailingIcon = { 
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) { 
                        Icon(Icons.Default.Clear, "Clear", tint = SalesCrmTheme.colors.textMuted) 
                    }
                }
            },
            singleLine = true, 
            shape = RoundedCornerShape(12.dp),
            colors = salesCrmTextFieldColors()
        )
        Spacer(Modifier.width(8.dp))
        BadgedBox(
            badge = { 
                if (filterCount > 0) Badge(containerColor = PrimaryBlue) { Text(filterCount.toString()) } 
            }
        ) {
            IconButton(onClick = onFilterClick) { 
                Icon(Icons.Default.FilterList, "Filter", tint = SalesCrmTheme.colors.textSecondary) 
            }
        }
        IconButton(onClick = onColumnsClick) { 
            Icon(Icons.Default.ViewColumn, "Columns", tint = SalesCrmTheme.colors.textSecondary) 
        }
    }
}

// ==================== UTILITY COMPONENTS ====================

@Composable
fun ActionButton(icon: ImageVector, label: String, color: Color = PrimaryBlue, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(48.dp), 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, Modifier.size(64.dp), tint = SalesCrmTheme.colors.textMuted)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = SalesCrmTheme.colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SalesCrmTheme.colors.textMuted)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), 
        horizontalArrangement = Arrangement.SpaceBetween, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = SalesCrmTheme.colors.textPrimary)
        action?.invoke()
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = PrimaryBlue)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SalesCrmTheme.colors.textMuted)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = SalesCrmTheme.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        onClick?.let { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = SalesCrmTheme.colors.textMuted) }
    }
}

// ==================== LABEL CHIPS ====================

@Composable
fun LabelChip(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
        },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else SalesCrmTheme.colors.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryBlue.copy(alpha = 0.5f) else SalesCrmTheme.colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isSelected) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun LabelsRow(
    availableLabels: List<String>,
    selectedLabels: List<String>,
    onLabelToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(availableLabels) { label ->
            LabelChip(
                label = label,
                isSelected = label in selectedLabels,
                onToggle = { onLabelToggle(label) }
            )
        }
    }
}

// ==================== PRIORITY SELECTOR ====================

@Composable
fun PrioritySelector(
    selectedPriority: Priority,
    onPrioritySelected: (Priority) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Priority.entries.forEach { priority ->
            val isSelected = priority == selectedPriority
            val priorityColor = Color(priority.color)
            
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPrioritySelected(priority) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) priorityColor.copy(alpha = 0.2f) else SalesCrmTheme.colors.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) priorityColor else SalesCrmTheme.colors.border
                )
            ) {
                Text(
                    text = priority.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isSelected) priorityColor else SalesCrmTheme.colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ==================== STAGE SELECTOR ====================

@Composable
fun StageSelector(
    stages: List<PipelineStage>,
    selectedStage: PipelineStage,
    onStageSelected: (PipelineStage) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(stages) { stage ->
            val isSelected = stage == selectedStage
            val stageColor = Color(stage.color)
            
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onStageSelected(stage) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) stageColor.copy(alpha = 0.2f) else SalesCrmTheme.colors.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) stageColor else SalesCrmTheme.colors.border
                )
            ) {
                Text(
                    text = stage.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isSelected) stageColor else SalesCrmTheme.colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ==================== FORM FIELDS ====================

@Composable
fun salesCrmTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SalesCrmTheme.colors.textPrimary,
    unfocusedTextColor = SalesCrmTheme.colors.textPrimary,
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = SalesCrmTheme.colors.border,
    focusedContainerColor = SalesCrmTheme.colors.surfaceVariant.copy(alpha = 0.8f),
    unfocusedContainerColor = SalesCrmTheme.colors.surfaceVariant.copy(alpha = 0.4f),
    cursorColor = PrimaryBlue,
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = SalesCrmTheme.colors.textMuted,
    focusedPlaceholderColor = SalesCrmTheme.colors.textMuted,
    unfocusedPlaceholderColor = SalesCrmTheme.colors.textMuted
)

@Composable
fun DarkFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isRequired: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
    singleLine: Boolean = true,
    minLines: Int = 1,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column {
        Row {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isFocused) PrimaryBlue else SalesCrmTheme.colors.textPrimary
            )
            if (isRequired) {
                Text(" *", color = AccentRed, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            placeholder = { Text(placeholder, color = SalesCrmTheme.colors.textMuted) },
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() }),
            shape = RoundedCornerShape(12.dp),
            colors = salesCrmTextFieldColors(),
            interactionSource = interactionSource
        )
    }
}

// ==================== COMPACT SEARCH ====================

@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val borderColor = if (isFocused) PrimaryBlue else SalesCrmTheme.colors.border
    val backgroundColor = SalesCrmTheme.colors.surfaceVariant
    val textColor = SalesCrmTheme.colors.textPrimary
    
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(40.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(if (isFocused) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp)),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        singleLine = true,
        interactionSource = interactionSource,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null, 
                    tint = SalesCrmTheme.colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SalesCrmTheme.colors.textMuted
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = SalesCrmTheme.colors.textMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onValueChange("") }
                    )
                }
            }
        }
    )
}

// ==================== PHONE FORM FIELD ====================

@Composable
fun PhoneFormField(
    label: String,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    countryCode: String, // ISO code like "US"
    onCountryCodeChange: (String) -> Unit,
    placeholder: String,
    isRequired: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    isError: Boolean = false
) {
    val countryDialCodes = mapOf("US" to "+1", "IN" to "+91", "UK" to "+44", "CA" to "+1", "AU" to "+61")
    val countryFlags = mapOf("US" to "🇺🇸", "IN" to "🇮🇳", "UK" to "🇬🇧", "CA" to "🇨🇦", "AU" to "🇦🇺")
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isError) MaterialTheme.colorScheme.error else SalesCrmTheme.colors.textPrimary
            )
            if (isRequired) {
                Text(" *", color = AccentRed, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            // Country Code Selector (Compact)
            Box {
                Surface(
                    modifier = Modifier
                        .width(72.dp)
                        .height(56.dp)
                        .clickable { expanded = true },
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                    color = SalesCrmTheme.colors.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error else SalesCrmTheme.colors.border)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${countryFlags[countryCode] ?: "🏳️"}${countryDialCodes[countryCode] ?: "+?"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isError) MaterialTheme.colorScheme.error else SalesCrmTheme.colors.textPrimary
                        )
                        Icon(
                            Icons.Default.ArrowDropDown, 
                            null, 
                            tint = if (isError) MaterialTheme.colorScheme.error else SalesCrmTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(SalesCrmTheme.colors.surface)
                ) {
                    countryDialCodes.keys.forEach { code ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "${countryFlags[code]} $code (${countryDialCodes[code]})", 
                                    color = SalesCrmTheme.colors.textPrimary
                                ) 
                            },
                            onClick = {
                                onCountryCodeChange(code)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                placeholder = { Text(placeholder, color = SalesCrmTheme.colors.textMuted) },
                singleLine = true,
                isError = isError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = imeAction),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                colors = salesCrmTextFieldColors()
            )
        }
    }
}
