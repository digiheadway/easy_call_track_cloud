package com.miniclick.calltrackmanage.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.ui.common.*
import com.miniclick.calltrackmanage.ui.utils.AudioPlayer
import com.miniclick.calltrackmanage.ui.utils.PlaybackMetadata
import com.miniclick.calltrackmanage.ui.utils.cleanNumber
import com.miniclick.calltrackmanage.ui.utils.formatDurationShort
import com.miniclick.calltrackmanage.ui.utils.formatTime
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonInteractionBottomSheet(
    person: PersonGroup,
    recordings: Map<String, String>,
    audioPlayer: AudioPlayer,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onAttachRecording: (CallDataEntity) -> Unit,
    onCustomLookup: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Note Dialog States
    var callNoteTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var showPersonNoteDialog by remember { mutableStateOf(false) }
    
    // Dialogs
    if (callNoteTarget != null) {
        NoteDialog(
            title = "Call Note",
            initialNote = callNoteTarget?.callNote ?: "",
            label = "Note (only for this call)",
            buttonText = "Save Call Note",
            onDismiss = { callNoteTarget = null },
            onSave = { note -> 
                viewModel.saveCallNote(callNoteTarget!!.compositeId, note)
                callNoteTarget = null 
            }
        )
    }

    if (showNameDialog) {
        NoteDialog(
            title = "Set Name",
            initialNote = person.name ?: "",
            label = "Name for this contact",
            buttonText = "Save Name",
            onDismiss = { showNameDialog = false },
            onSave = { name ->
                viewModel.savePersonName(person.number, name)
                showNameDialog = false
            }
        )
    }

    if (showLabelDialog) {
        LabelPickerDialog(
            currentLabel = person.label,
            availableLabels = uiState.persons.mapNotNull { it.label }.filter { it.isNotEmpty() }.distinct().sorted(),
            onDismiss = { showLabelDialog = false },
            onSave = { label ->
                viewModel.savePersonLabel(person.number, label)
                showLabelDialog = false
            }
        )
    }

    if (showPersonNoteDialog) {
        NoteDialog(
            title = "Person Note",
            initialNote = person.personNote ?: "",
            label = "Note (linked to phone number)",
            buttonText = "Save Person Note",
            onDismiss = { showPersonNoteDialog = false },
            onSave = { note ->
                viewModel.savePersonNote(person.number, note)
                showPersonNoteDialog = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .padding(horizontal = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = (person.name?.firstOrNull() ?: person.number.lastOrNull() ?: '?')
                        .uppercaseChar()
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = person.name ?: cleanNumber(person.number),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showNameDialog = true }
                        )
                        IconButton(onClick = { showNameDialog = true }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (person.name != null) {
                        Text(
                            text = cleanNumber(person.number),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { onCustomLookup(person.number) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ManageSearch,
                        contentDescription = "Custom Lookup",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Sub-header for Label and Person Note
            if (!person.label.isNullOrEmpty() || !person.personNote.isNullOrEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!person.label.isNullOrEmpty()) {
                        LabelChip(label = person.label, onClick = { showLabelDialog = true })
                    } else {
                         AssistChip(
                            onClick = { showLabelDialog = true },
                            label = { Text("Add Label", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null, modifier = Modifier.size(14.dp)) }
                        )
                    }

                    if (!person.personNote.isNullOrEmpty()) {
                        AssistChip(
                            onClick = { showPersonNoteDialog = true },
                            label = { Text(person.personNote, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.StickyNote2, null, modifier = Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = MaterialTheme.colorScheme.primary,
                                leadingIconContentColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        AssistChip(
                            onClick = { showPersonNoteDialog = true },
                            label = { Text("Add Person Note", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            } else {
                 Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { showLabelDialog = true },
                        label = { Text("Add Label", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null, modifier = Modifier.size(14.dp)) }
                    )
                    AssistChip(
                        onClick = { showPersonNoteDialog = true },
                        label = { Text("Add Person Note", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Call History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "${person.calls.size} calls",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Flat list without date grouping
            val sortedCalls = remember(person.calls) {
                person.calls.sortedByDescending { it.callDate }
            }
            
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = navBarPadding + 60.dp)
            ) {
                items(sortedCalls, key = { it.compositeId }) { log ->
                    // Trigger check for recording path
                    LaunchedEffect(log.compositeId) {
                        if (log.localRecordingPath == null) {
                            viewModel.getRecordingForLog(log)
                        }
                    }

                    InteractionRow(
                        call = log,
                        recordings = recordings,
                        audioPlayer = audioPlayer,
                        callRecordEnabled = uiState.callRecordEnabled,
                        onNoteClick = { callNoteTarget = it },
                        onAttachRecording = onAttachRecording
                    )
                }
            }
        }
    }
}


