package com.miniclick.calltrackmanage.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.ui.common.*
import com.miniclick.calltrackmanage.util.audio.AudioPlayer
import com.miniclick.calltrackmanage.util.formatting.cleanNumber
import com.miniclick.calltrackmanage.util.formatting.getDateHeader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonsList(
    persons: List<PersonGroup>,
    recordings: Map<String, String>,
    audioPlayer: AudioPlayer,
    context: Context,
    viewModel: HomeViewModel,
    whatsappPreference: String,
    onExclude: (String) -> Unit,
    onNoTrack: (String) -> Unit,
    onViewMoreClick: (PersonGroup) -> Unit,
    onAttachRecording: (CallDataEntity) -> Unit,
    canExclude: Boolean = true,
    onCustomLookup: (String) -> Unit,
    lazyListState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    onJumpClick: () -> Unit = {}
) {
    // Hoist uiState collection to parent level to prevent recomposition in each card
    val uiState by viewModel.uiState.collectAsState()
    val callRecordEnabled = uiState.callRecordEnabled
    
    // Pre-compute labels list once for all dialogs
    val allLabels = remember(persons) {
        persons.mapNotNull { it.label }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    var expandedNumber by remember { mutableStateOf<String?>(null) }
    
    // State variables
    var longPressTarget by remember { mutableStateOf<PersonGroup?>(null) }
    var excludeTarget by remember { mutableStateOf<PersonGroup?>(null) }
    var personNoteTarget by remember { mutableStateOf<PersonGroup?>(null) }
    var callNoteTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    var labelTarget by remember { mutableStateOf<PersonGroup?>(null) }
    var nameTarget by remember { mutableStateOf<PersonGroup?>(null) }

    if (personNoteTarget != null) {
        NoteDialog(
            title = "Person Note",
            initialNote = personNoteTarget?.personNote ?: "",
            label = "Note (linked to phone number)",
            buttonText = "Save Person Note",
            onDismiss = { personNoteTarget = null },
            onSave = { note -> 
                personNoteTarget?.let { viewModel.savePersonNote(it.number, note) }
                personNoteTarget = null 
            }
        )
    }

    if (callNoteTarget != null) {
        NoteDialog(
            title = "Call Note",
            initialNote = callNoteTarget?.callNote ?: "",
            label = "Note (only for this call)",
            buttonText = "Save Call Note",
            onDismiss = { callNoteTarget = null },
            onSave = { note -> 
                callNoteTarget?.let { viewModel.saveCallNote(it.compositeId, note) }
                callNoteTarget = null 
            }
        )
    }

    if (labelTarget != null) {
        LabelPickerDialog(
            currentLabel = labelTarget?.label,
            availableLabels = allLabels,
            onDismiss = { labelTarget = null },
            onSave = { label ->
                labelTarget?.let { viewModel.savePersonLabel(it.number, label) }
                labelTarget = null
            }
        )
    }

    // Set Name Dialog
    if (nameTarget != null) {
        NoteDialog(
            title = "Set Name",
            initialNote = nameTarget?.name ?: "",
            label = "Name for this contact",
            buttonText = "Save Name",
            onDismiss = { nameTarget = null },
            onSave = { name ->
                nameTarget?.let { viewModel.savePersonName(it.number, name) }
                nameTarget = null
            }
        )
    }

    // Long Press Menu
    if (longPressTarget != null) {
        AlertDialog(
            onDismissRequest = { longPressTarget = null },
            title = { Text(cleanNumber(longPressTarget!!.number)) },
            text = {
                Column {
                    // Set Name Option
                    TextButton(
                        onClick = {
                            nameTarget = longPressTarget
                            longPressTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Set Name", modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    }

                    // Custom Lookup Option
                    TextButton(
                        onClick = {
                            onCustomLookup(longPressTarget!!.number)
                            longPressTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Custom Lookup", 
                            modifier = Modifier.weight(1f), 
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Re-attach Recordings Option
                    TextButton(
                        onClick = {
                            longPressTarget?.let { viewModel.reAttachRecordingsForPhone(it.number) }
                            longPressTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Re-scan Recordings", 
                            modifier = Modifier.weight(1f), 
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Hide from Lists Option (excludeFromList = true, excludeFromSync = false)
                    if (canExclude) {
                        TextButton(
                            onClick = {
                                longPressTarget?.let { onExclude(it.number) }
                                longPressTarget = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Hide from Lists",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        
                        // Stop Tracking Completely Option
                        TextButton(
                            onClick = {
                                excludeTarget = longPressTarget
                                longPressTarget = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Stop Tracking Completely",
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { longPressTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Stop Tracking Dialog
    if (excludeTarget != null) {
        AlertDialog(
            onDismissRequest = { excludeTarget = null },
            title = { Text("Stop Tracking") },
            text = { Text("Completely stop tracking ${excludeTarget?.let { cleanNumber(it.number) }}? This will hide all existing calls and prevent future calls from being tracked or synced.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        excludeTarget?.let { onNoTrack(it.number) }
                        excludeTarget = null
                    }
                ) {
                    Text("Stop Tracking", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { excludeTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Group persons by last call date
        val dateGroupedPersons: Map<String, List<PersonGroup>> = remember(persons) {
            persons.groupBy { getDateHeader(it.lastCallDate) }
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 240.dp)
        ) {
            dateGroupedPersons.forEach { (header, headerPersons) ->
                stickyHeader {
                    PersonDateSectionHeader(
                        dateLabel = header,
                        uniquePersons = headerPersons.size,
                        totalCalls = headerPersons.sumOf { it.calls.size },
                        onJumpClick = onJumpClick
                    )
                }
                items(headerPersons, key = { it.number }) { p ->
                    val isExpanded = expandedNumber == p.number
                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

                    PersonCard(
                        person = p,
                        isExpanded = isExpanded,
                        recordings = recordings,
                        audioPlayer = audioPlayer,
                        callRecordEnabled = callRecordEnabled,
                        onCardClick = {
                            expandedNumber = if (isExpanded) null else p.number
                        },
                        onLongClick = {
                            longPressTarget = p
                        },
                        onCallClick = {
                            viewModel.initiateCall(p.number)
                        },
                        onCopyClick = {
                            val cleaned = cleanNumber(p.number)
                            val clip = android.content.ClipData.newPlainText("Phone Number", cleaned)
                            clipboardManager.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Copied: $cleaned", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onWhatsAppClick = {
                            viewModel.onWhatsAppClick(p.number)
                        },
                        onPersonNoteClick = { personNoteTarget = p },
                        onCallNoteClick = { callNoteTarget = it },
                        onAddContactClick = {
                            try {
                                val cleaned = cleanNumber(p.number)
                                val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                                    type = android.provider.ContactsContract.Contacts.CONTENT_ITEM_TYPE
                                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, cleaned)
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        onAddToCrmClick = {
                            try {
                                val intent = Intent("com.example.salescrm.ACTION_ADD_LEAD").apply {
                                    putExtra("lead_name", p.name ?: "")
                                    putExtra("lead_phone", p.number)
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: android.content.ActivityNotFoundException) {
                                android.widget.Toast.makeText(context, "SalesCRM app not installed", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "Failed to open CRM", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onViewMoreClick = { onViewMoreClick(p) },
                        onLabelClick = { labelTarget = p },
                        onAttachRecording = onAttachRecording,
                        onMarkAllReviewed = { viewModel.markAllCallsReviewed(p.number) }
                    )
                }
            }
        }

        val mapSize: Int = dateGroupedPersons.size
        val listSize: Int = persons.size
        val totalCount = listSize + mapSize
        VerticalScrollbar(
            lazyListState = lazyListState,
            itemCount = totalCount,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PersonCard(
    person: PersonGroup,
    isExpanded: Boolean,
    recordings: Map<String, String>,
    audioPlayer: AudioPlayer,
    callRecordEnabled: Boolean,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    onCallClick: () -> Unit,
    onCopyClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPersonNoteClick: () -> Unit,
    onCallNoteClick: (CallDataEntity) -> Unit,
    onAddContactClick: () -> Unit,
    onAddToCrmClick: () -> Unit,
    onViewMoreClick: () -> Unit,
    onLabelClick: () -> Unit,
    onAttachRecording: (CallDataEntity) -> Unit,
    onMarkAllReviewed: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onCardClick,
                    onLongClick = onLongClick,
                    interactionSource = interactionSource,
                    indication = if (isExpanded) null else ripple(bounded = true)
                )
        ) {
        Column {
            // Main Row (Always Visible)
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Last Call Type Icon
                val lastCallType = remember(person.calls) { person.calls.maxByOrNull { it.callDate }?.callType }
                val (iconColor, bgColor) = remember(lastCallType) {
                    when (lastCallType) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> Color(0xFF4CAF50) to Color(0xFFE8F5E9)
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> Color(0xFF2196F3) to Color(0xFFE3F2FD)
                        android.provider.CallLog.Calls.MISSED_TYPE -> Color(0xFFF44336) to Color(0xFFFFEBEE)
                        else -> Color(0xFF6750A4) to Color(0xFFEADDFF) // Default primary colors
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = remember(lastCallType) { when (lastCallType) {
                            android.provider.CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                            android.provider.CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                            android.provider.CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                            else -> Icons.Default.Phone
                        }},
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allReviewed = remember(person.calls) { person.calls.isNotEmpty() && person.calls.all { it.reviewed } }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val displayName = remember(person.name, person.number) {
                                person.name?.takeIf { it.isNotBlank() } ?: cleanNumber(person.number)
                            }
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = if (isExpanded) 10 else 1,
                                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
                            )
                            if (allReviewed) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "All Reviewed",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        // Call Count Chip
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onViewMoreClick() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${person.calls.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // Note and Labels row
                    val hasPersonNote = !person.personNote.isNullOrEmpty()
                    val hasLabel = !person.label.isNullOrEmpty()
                    if (hasPersonNote || hasLabel) {
                        if (isExpanded) {
                            FlowRow(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (hasPersonNote) {
                                    NoteChip(
                                        note = person.personNote,
                                        icon = Icons.AutoMirrored.Filled.StickyNote2,
                                        onClick = onPersonNoteClick,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        maxLines = 10
                                    )
                                }

                                if (hasLabel) {
                                    val labelList = remember(person.label) { person.label!!.split(",").map { it.trim() }.filter { it.isNotBlank() } }
                                    labelList.forEach { label ->
                                        LabelChip(label = label, onClick = onLabelClick, maxLines = 10)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasPersonNote) {
                                    NoteChip(
                                        note = person.personNote,
                                        icon = Icons.AutoMirrored.Filled.StickyNote2,
                                        onClick = onPersonNoteClick,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    )
                                }

                                if (hasLabel) {
                                    val labels = remember(person.label) { person.label!!.split(",").map { it.trim() }.filter { it.isNotBlank() } }
                                    if (labels.isNotEmpty()) {
                                        val displayLabel = remember(labels) {
                                            if (labels.size > 1) "${labels.first()} +${labels.size - 1}" else labels.first()
                                        }
                                        LabelChip(label = displayLabel, onClick = onLabelClick)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    
                    // Breakdown row at bottom + Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (person.incomingCount > 0) CallTypeSmallBadge(Icons.AutoMirrored.Filled.CallReceived, person.incomingCount, Color(0xFF4CAF50))
                            if (person.outgoingCount > 0) CallTypeSmallBadge(Icons.AutoMirrored.Filled.CallMade, person.outgoingCount, Color(0xFF2196F3))
                            if (person.missedCount > 0) CallTypeSmallBadge(Icons.AutoMirrored.Filled.CallMissed, person.missedCount, Color(0xFFF44336))
                        }
                        
                        val formattedDate = remember(person.lastCallDate) {
                            // Using a static formatter pattern for efficiency, note: Locale.getDefault() still queried
                            java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(person.lastCallDate))
                        }
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded Section
            if (isExpanded) {
                
                // Action Icons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ActionIconButton(icon = Icons.Default.Call, label = "Call", onClick = onCallClick)
                    ActionIconButton(icon = Icons.Default.ContentCopy, label = "Copy", onClick = onCopyClick)
                    ActionIconButton(icon = Icons.AutoMirrored.Filled.Chat, label = "WhatsApp", onClick = onWhatsAppClick, tint = Color(0xFF25D366))
                    ActionIconButton(icon = Icons.Default.EditNote, label = "Person Note", onClick = onPersonNoteClick)
                    ActionIconButton(icon = Icons.Default.PersonAddAlt, label = "Contact", onClick = onAddContactClick)
                    ActionIconButton(icon = Icons.Default.AppRegistration, label = "CRM", onClick = onAddToCrmClick)
                    ActionIconButton(icon = Icons.AutoMirrored.Filled.Label, label = "Label", onClick = onLabelClick)
                    
                    // Mark All Reviewed - check if all calls are already reviewed
                    val allReviewed = person.calls.all { it.reviewed }
                    ActionIconButton(
                        icon = if (allReviewed) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAddCheck,
                        label = if (allReviewed) "All Done" else "Review All",
                        onClick = onMarkAllReviewed,
                        tint = if (allReviewed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Calls List - entire section is clickable to view full history
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewMoreClick() }
                        .padding(vertical = 8.dp)
                ) {
                    // Header row for Recent Interactions
                    Text(
                        text = "Recent Interactions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    
                    val recentCalls = remember(person.calls) { person.calls.take(5) }
                    recentCalls.forEach { call ->
                        InteractionRow(
                            call = call,
                            recordings = recordings,
                            audioPlayer = audioPlayer,
                            callRecordEnabled = callRecordEnabled,
                            onNoteClick = onCallNoteClick,
                            onAttachRecording = onAttachRecording
                        )
                    }
                    
                    // Only show "View all" button when there are more calls than shown
                    if (person.calls.size > 5) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "View all ${person.calls.size} interactions",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun CallTypeSmallBadge(icon: ImageVector, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, modifier = Modifier.size(10.dp), tint = color)
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// Helper function to get date header for persons (based on lastCallDate)

@Composable
fun PersonDateSectionHeader(
    dateLabel: String,
    uniquePersons: Int,
    totalCalls: Int,
    onJumpClick: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onJumpClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Date Label + Calendar Chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Jump to Date",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Right: Person and Call Counts
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$uniquePersons",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$totalCalls",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
