package com.miniclick.calltrackmanage.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.ui.unit.dp
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.CallLogStatus
import com.miniclick.calltrackmanage.ui.common.*
import com.miniclick.calltrackmanage.ui.utils.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallLogList(
    logs: List<CallDataEntity>,
    recordings: Map<String, String>,
    personGroupsMap: Map<String, PersonGroup>,
    modifier: Modifier = Modifier,
    onSaveCallNote: (String, String) -> Unit,
    onSavePersonNote: (String, String) -> Unit,
    viewModel: HomeViewModel,
    audioPlayer: AudioPlayer,
    whatsappPreference: String,
    context: Context,
    onViewMoreClick: (PersonGroup) -> Unit,
    onAttachRecording: (CallDataEntity) -> Unit,
    canExclude: Boolean = true,
    onCustomLookup: (String) -> Unit,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Note Dialog States
    var callNoteTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    var personNoteTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    var labelTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    var nameTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    var longPressTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    var excludeTarget by remember { mutableStateOf<CallDataEntity?>(null) }

    if (callNoteTarget != null) {
        NoteDialog(
            title = "Call Note",
            initialNote = callNoteTarget?.callNote ?: "",
            label = "Note (only for this call)",
            buttonText = "Save Call Note",
            onDismiss = { callNoteTarget = null },
            onSave = { note -> 
                callNoteTarget?.let { onSaveCallNote(it.compositeId, note) }
                callNoteTarget = null 
            }
        )
    }

    if (personNoteTarget != null) {
        NoteDialog(
            title = "Person Note",
            initialNote = personGroupsMap[personNoteTarget?.phoneNumber]?.personNote ?: "",
            label = "Note (linked to phone number)",
            buttonText = "Save Person Note",
            onDismiss = { personNoteTarget = null },
            onSave = { note -> 
                personNoteTarget?.let { onSavePersonNote(it.phoneNumber, note) }
                personNoteTarget = null 
            }
        )
    }

    if (labelTarget != null) {
        LabelPickerDialog(
            currentLabel = personGroupsMap[labelTarget?.phoneNumber]?.label,
            availableLabels = uiState.persons.mapNotNull { it.label }.filter { it.isNotEmpty() }.distinct().sorted(),
            onDismiss = { labelTarget = null },
            onSave = { label ->
                labelTarget?.let { viewModel.savePersonLabel(it.phoneNumber, label) }
                labelTarget = null
            }
        )
    }

    // Set Name Dialog
    if (nameTarget != null) {
        NoteDialog(
            title = "Set Name",
            initialNote = personGroupsMap[nameTarget?.phoneNumber]?.name ?: "",
            label = "Name for this contact",
            buttonText = "Save Name",
            onDismiss = { nameTarget = null },
            onSave = { name ->
                nameTarget?.let { viewModel.savePersonName(it.phoneNumber, name) }
                nameTarget = null
            }
        )
    }

    // Long Press Menu
    if (longPressTarget != null) {
        AlertDialog(
            onDismissRequest = { longPressTarget = null },
            title = { Text(cleanNumber(longPressTarget!!.phoneNumber)) },
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
                            onCustomLookup(longPressTarget!!.phoneNumber)
                            longPressTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.ManageSearch,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Custom Lookup", 
                            modifier = Modifier.weight(1f), 
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Hide from Lists Option (excludeFromList = true, excludeFromSync = false)
                    if (canExclude) {
                        TextButton(
                            onClick = {
                                longPressTarget?.let { viewModel.ignoreNumber(it.phoneNumber) }
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
                        
                        // Stop Tracking Option (excludeFromSync = true, excludeFromList = true)
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

    // Expanded State
    var expandedLogId by remember { mutableStateOf<String?>(null) }
    
    // Exclusion Dialog State (for Stop Tracking Completely)
    if (excludeTarget != null) {
        AlertDialog(
            onDismissRequest = { excludeTarget = null },
            title = { Text("Stop Tracking") },
            text = { Text("Completely stop tracking ${excludeTarget?.let { cleanNumber(it.phoneNumber) }}? This will hide all existing calls and prevent future calls from being tracked or synced.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        excludeTarget?.let { viewModel.noTrackNumber(it.phoneNumber) }
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

    // Grouping with counts
    val groupedLogs = remember(logs) {
        logs.groupBy { getDateHeader(it.callDate) }
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        groupedLogs.forEach { (header, headerLogs) ->
            item {
                DateSectionHeader(
                    dateLabel = header,
                    totalCalls = headerLogs.size,
                    uniqueCalls = headerLogs.distinctBy { it.phoneNumber }.size
                )
            }
            items(headerLogs, key = { it.compositeId }) { log ->
                val isExpanded = expandedLogId == log.compositeId
                // Trigger check on expand
                if (isExpanded) {
                    LaunchedEffect(log.compositeId) {
                        viewModel.getRecordingForLog(log)
                    }
                }
                
                // Check for recording - use localRecordingPath from entity or recordings cache
                val recordingPath = log.localRecordingPath ?: recordings[log.compositeId]
                val hasRecording = !recordingPath.isNullOrEmpty()
                
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                
                CallLogItem(
                    log = log,
                    isExpanded = isExpanded,
                    hasRecording = hasRecording,
                    recordingPath = recordingPath,
                    onCardClick = { 
                        expandedLogId = if (isExpanded) null else log.compositeId
                    },
                    onLongClick = {
                        longPressTarget = log
                    },
                    onPlayClick = { path -> 
                        val callTypeStr = when (log.callType) {
                            android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming"
                            android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                            android.provider.CallLog.Calls.MISSED_TYPE -> "Missed"
                            5 -> "Rejected"
                            6 -> "Blocked"
                            else -> "Call"
                        }
                        val timeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(java.util.Date(log.callDate))
                        audioPlayer.play(
                            path,
                            PlaybackMetadata(
                                name = log.contactName,
                                phoneNumber = log.phoneNumber,
                                callTime = timeStr,
                                callType = callTypeStr
                            )
                        )
                    },
                    onCallNoteClick = { callNoteTarget = log },
                    onPersonNoteClick = { personNoteTarget = log },
                    onCallClick = {
                        try {
                             val cleaned = cleanNumber(log.phoneNumber)
                             val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
                             intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                             context.startActivity(intent)
                        } catch (e: Exception) {
                             e.printStackTrace()
                        }
                    },
                    onCopyClick = {
                        val cleaned = cleanNumber(log.phoneNumber)
                        val clip = android.content.ClipData.newPlainText("Phone Number", cleaned)
                        clipboardManager.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Copied: $cleaned", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onWhatsAppClick = {
                        try {
                            val cleaned = cleanNumber(log.phoneNumber)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$cleaned"))
                            if (whatsappPreference != "Always Ask") {
                                intent.setPackage(whatsappPreference)
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Fallback
                            if (whatsappPreference != "Always Ask") {
                               try {
                                   val cleaned = cleanNumber(log.phoneNumber)
                                   val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$cleaned"))
                                   intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                   context.startActivity(intent)
                               } catch(e2: Exception) { e2.printStackTrace() }
                            }
                        }
                    },
                    onAddContactClick = {
                        try {
                            val cleaned = cleanNumber(log.phoneNumber)
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
                            val cleaned = cleanNumber(log.phoneNumber)
                            val intent = Intent("com.example.salescrm.ACTION_ADD_LEAD").apply {
                                putExtra("lead_name", log.contactName ?: "")
                                putExtra("lead_phone", cleaned)
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
                    onViewMoreClick = {
                        personGroupsMap[log.phoneNumber]?.let { onViewMoreClick(it) }
                    },
                    audioPlayer = audioPlayer,
                    personGroup = personGroupsMap[log.phoneNumber],
                    onLabelClick = { labelTarget = log },
                    onAttachRecording = onAttachRecording,
                    onReviewedToggle = { viewModel.updateReviewed(log.compositeId, !log.reviewed) },
                    callRecordEnabled = uiState.callRecordEnabled
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CallLogItem(
    log: CallDataEntity,
    isExpanded: Boolean,
    hasRecording: Boolean,
    recordingPath: String?,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlayClick: (String) -> Unit,
    onCallNoteClick: () -> Unit,
    onPersonNoteClick: () -> Unit,
    onCallClick: () -> Unit,
    onCopyClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onAddToCrmClick: () -> Unit,
    onViewMoreClick: () -> Unit,
    audioPlayer: AudioPlayer,
    personGroup: PersonGroup?,
    onLabelClick: () -> Unit,
    onAttachRecording: (CallDataEntity) -> Unit,
    onReviewedToggle: () -> Unit,
    callRecordEnabled: Boolean
) {
    val currentFile by audioPlayer.currentFile.collectAsState()
    
    // Check if this recording is currently playing
    val isThisPlaying = currentFile == recordingPath && recordingPath != null
    
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
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // COLUMN 1: Call Type Icon (Centered) - Colors match Persons screen
                val (iconColor, bgColor) = when (log.callType) {
                    android.provider.CallLog.Calls.INCOMING_TYPE -> Color(0xFF4CAF50) to Color(0xFFE8F5E9) // Green
                    android.provider.CallLog.Calls.OUTGOING_TYPE -> Color(0xFF2196F3) to Color(0xFFE3F2FD) // Blue
                    android.provider.CallLog.Calls.MISSED_TYPE -> Color(0xFFF44336) to Color(0xFFFFEBEE) // Red
                    5 -> Color(0xFFFF9800) to Color(0xFFFFF3E0) // Orange for Rejected
                    6 -> Color(0xFF757575) to Color(0xFFEEEEEE) // Gray for Blocked
                    else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (log.callType) {
                            android.provider.CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                            android.provider.CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                            android.provider.CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                            5 -> Icons.Default.PhoneDisabled // Rejected
                            6 -> Icons.Default.Block // Blocked
                            else -> Icons.Default.Phone
                        },
                        contentDescription = null,
                        tint = iconColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // COLUMN 2: Info (Name & Notes) - Centered Vertically
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Row 1: Contact Name / Number with Reviewed Checkmark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = personGroup?.name ?: log.contactName ?: cleanNumber(log.phoneNumber),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (log.reviewed) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Reviewed",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4CAF50) // Green checkmark
                            )
                        }
                    }

                    // Row 2: Duration, Notes (Optional, Multiline wrapping)
                    FlowRow(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Duration with Timer Icon
                        if (log.duration > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Timer,
                                    null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = formatDuration(log.duration),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val hasCallNote = !log.callNote.isNullOrEmpty()
                        val hasPersonNote = personGroup?.personNote?.isNotEmpty() == true
                        
                        if (hasCallNote) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.clickable { onCallNoteClick() }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.StickyNote2,
                                    null,
                                    modifier = Modifier.size(12.dp).padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = log.callNote!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        
                        if (hasPersonNote) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.clickable { onPersonNoteClick() }
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    modifier = Modifier.size(12.dp).padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = personGroup!!.personNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        // Label Chip
                        if (!personGroup?.label.isNullOrEmpty()) {
                            LabelChip(label = personGroup!!.label!!, onClick = onLabelClick)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // COLUMN 3: Time & Status Badge
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.callDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    log.syncStatus.let { 
                        Spacer(Modifier.height(4.dp))
                        StatusIndicator(it) 
                    }
                }
            }
            
            // Player Row (Only if NOT expanded AND is playing)
            if (!isExpanded && hasRecording && recordingPath != null && isThisPlaying) {
                PlaybackControls(
                    audioPlayer = audioPlayer,
                    recordingPath = recordingPath,
                    isExpanded = false,
                    log = log
                )
            }

            // Expanded Section
            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // Action Icons Row - Horizontal Scroll
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Call
                    ActionIconButton(
                        icon = Icons.Default.Call,
                        label = "Call",
                        onClick = onCallClick
                    )
                    
                    // Copy Phone
                    ActionIconButton(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy",
                        onClick = onCopyClick
                    )
                    
                    // WhatsApp
                    ActionIconButton(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        label = "WhatsApp",
                        onClick = onWhatsAppClick,
                        tint = Color(0xFF25D366)
                    )
                    
                    // Call Note
                    ActionIconButton(
                        icon = Icons.Default.EditNote,
                        label = "Call Note",
                        onClick = onCallNoteClick
                    )

                    // Person Note
                    ActionIconButton(
                        icon = Icons.Default.Person,
                        label = "Person Note",
                        onClick = onPersonNoteClick
                    )
                    
                    // Add to Phonebook
                    ActionIconButton(
                        icon = Icons.Default.PersonAddAlt,
                        label = "Contact",
                        onClick = onAddContactClick
                    )
                    
                    // Add to CRM
                    ActionIconButton(
                        icon = Icons.Default.AppRegistration,
                        label = "CRM",
                        onClick = onAddToCrmClick
                    )
                    
                    // Label
                    ActionIconButton(
                        icon = Icons.AutoMirrored.Filled.Label,
                        label = "Label",
                        onClick = onLabelClick
                    )
                    
                    // Reviewed Toggle
                    ActionIconButton(
                        icon = if (log.reviewed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        label = if (log.reviewed) "Reviewed" else "Review",
                        onClick = onReviewedToggle,
                        tint = if (log.reviewed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Recording Player Row (if recording exists)
                if (callRecordEnabled) {
                    if (hasRecording && recordingPath != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PlaybackControls(
                            audioPlayer = audioPlayer,
                            recordingPath = recordingPath,
                            isExpanded = true,
                            log = log
                        )
                    } else if (log.duration > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAttachRecording(log) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Attach Recording",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // View all interactions button
                if (personGroup != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    TextButton(
                        onClick = onViewMoreClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "View all ${personGroup.calls.size} interactions",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                null,
                                modifier = Modifier.size(12.dp)
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
fun DateSectionHeader(
    dateLabel: String,
    totalCalls: Int,
    uniqueCalls: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Date Label
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // Right: Call Counts
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    text = "$uniqueCalls",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActionIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = tint
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusIndicator(status: CallLogStatus) {
    val (color, icon) = when (status) {
        CallLogStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) to Icons.Outlined.Timer
        CallLogStatus.COMPRESSING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f) to Icons.Default.CloudSync
        CallLogStatus.UPLOADING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) to Icons.Default.CloudUpload
        CallLogStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.6f) to Icons.Default.CheckCircle
        CallLogStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f) to Icons.Default.Error
        CallLogStatus.NOTE_UPDATE_PENDING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f) to Icons.AutoMirrored.Filled.StickyNote2
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(13.dp)
    )
}

@Composable
fun PlaybackControls(
    audioPlayer: AudioPlayer,
    recordingPath: String,
    isExpanded: Boolean,
    log: CallDataEntity
) {
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val progress by audioPlayer.progress.collectAsState()
    val currentPos by audioPlayer.currentPosition.collectAsState()
    val duration by audioPlayer.duration.collectAsState()
    val currentFile by audioPlayer.currentFile.collectAsState()
    
    val isThisPlaying = currentFile == recordingPath

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isExpanded) 12.dp else 16.dp, 
                end = if (isExpanded) 12.dp else 16.dp, 
                bottom = 8.dp,
                top = if (isExpanded) 8.dp else 0.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isExpanded) {
            // Play/Pause Button for Expanded view
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (isThisPlaying) {
                            audioPlayer.togglePlayPause()
                        } else {
                            val callTypeStr = when (log.callType) {
                                android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming"
                                android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                                android.provider.CallLog.Calls.MISSED_TYPE -> "Missed"
                                5 -> "Rejected"
                                6 -> "Blocked"
                                else -> "Call"
                            }
                            val timeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(java.util.Date(log.callDate))
                            audioPlayer.play(
                                recordingPath,
                                PlaybackMetadata(
                                    name = log.contactName,
                                    phoneNumber = log.phoneNumber,
                                    callTime = timeStr,
                                    callType = callTypeStr
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isThisPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        // Seekbar
        Slider(
            value = if (isThisPlaying) progress else 0f,
            onValueChange = { if (isThisPlaying) audioPlayer.seekTo(it) },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        
        Spacer(Modifier.width(8.dp))
        
        // Time Display
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isThisPlaying) {
                    "${formatTime(currentPos)}/${formatTime(duration)}"
                } else {
                    "0:00"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isThisPlaying) {
                Spacer(Modifier.width(4.dp))
                val speed by audioPlayer.speed.collectAsState()
                PlaybackSpeedButton(
                    currentSpeed = speed,
                    onSpeedChange = { audioPlayer.setPlaybackSpeed(it) }
                )
            }
        }
    }
}

fun getDateHeader(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)
    
    calendar.timeInMillis = dateMillis
    val logDay = calendar.get(Calendar.DAY_OF_YEAR)
    val logYear = calendar.get(Calendar.YEAR)
    
    val diff = System.currentTimeMillis() - dateMillis
    
    return when {
        year == logYear && today == logDay -> "Today"
        year == logYear && today == logDay + 1 -> "Yesterday"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dateMillis))
    }
}

@Composable
fun InteractionRow(
    call: CallDataEntity,
    recordings: Map<String, String>,
    audioPlayer: AudioPlayer,
    callRecordEnabled: Boolean,
    onNoteClick: (CallDataEntity) -> Unit,
    onAttachRecording: (CallDataEntity) -> Unit
) {
    val isPlaying by audioPlayer.isPlaying.collectAsState()
    val progress by audioPlayer.progress.collectAsState()
    val currentFile by audioPlayer.currentFile.collectAsState()
    val currentPos by audioPlayer.currentPosition.collectAsState()
    val duration by audioPlayer.duration.collectAsState()

    val recordingPath = call.localRecordingPath ?: recordings[call.compositeId]
    val hasRecording = !recordingPath.isNullOrEmpty()
    val isShowingPlayer = currentFile == recordingPath && recordingPath != null

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when (call.callType) {
                            android.provider.CallLog.Calls.MISSED_TYPE -> Color(0xFFFFEBEE)
                            5 -> Color(0xFFFFEBEE) // Rejected
                            6 -> Color(0xFFECEFF1) // Blocked
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (call.callType) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                        android.provider.CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                        5 -> Icons.Default.PhoneDisabled // Rejected
                        6 -> Icons.Default.Block // Blocked
                        else -> Icons.Default.Phone
                    },
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = when (call.callType) {
                        android.provider.CallLog.Calls.MISSED_TYPE -> Color(0xFFF44336)
                        5 -> Color(0xFFF44336) // Rejected
                        6 -> Color(0xFF607D8B) // Blocked
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(Modifier.width(12.dp))

            // Time & Note
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(call.callDate)),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                
                if (!call.callNote.isNullOrEmpty()) {
                    Text(
                        text = call.callNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNoteClick(call) }
                            .padding(top = 2.dp)
                    )
                }
            }

            // Right side: Play Button, Duration and Note Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (callRecordEnabled) {
                    if (hasRecording) {
                        IconButton(
                            onClick = {
                                if (isShowingPlayer) {
                                    audioPlayer.togglePlayPause()
                                } else {
                                    val callTypeStr = when (call.callType) {
                                        android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming"
                                        android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                                        android.provider.CallLog.Calls.MISSED_TYPE -> "Missed"
                                        5 -> "Rejected"
                                        6 -> "Blocked"
                                        else -> "Call"
                                    }
                                    val timeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(java.util.Date(call.callDate))
                                    audioPlayer.play(
                                        recordingPath!!,
                                        PlaybackMetadata(
                                            name = call.contactName,
                                            phoneNumber = call.phoneNumber,
                                            callTime = timeStr,
                                            callType = callTypeStr
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isShowingPlayer && isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else if (call.duration > 0) {
                        IconButton(
                            onClick = { onAttachRecording(call) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach Recording",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (call.duration > 0) {
                    Text(
                        text = formatDurationShort(call.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Note Icon
                IconButton(
                    onClick = { onNoteClick(call) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (call.callNote.isNullOrEmpty()) Icons.AutoMirrored.Filled.NoteAdd else Icons.Default.EditNote,
                        contentDescription = "Add/Edit Note",
                        tint = if (call.callNote.isNullOrEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Inline Player Section
        if (isShowingPlayer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = progress,
                    onValueChange = { audioPlayer.seekTo(it) },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Spacer(Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${formatTime(currentPos)}/${formatTime(duration)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    val speed by audioPlayer.speed.collectAsState()
                    PlaybackSpeedButton(
                        currentSpeed = speed,
                        onSpeedChange = { audioPlayer.setPlaybackSpeed(it) }
                    )
                }
            }
        }
    }
}

