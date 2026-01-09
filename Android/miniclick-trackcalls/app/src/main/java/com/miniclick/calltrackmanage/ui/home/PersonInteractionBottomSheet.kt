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
import com.miniclick.calltrackmanage.util.audio.AudioPlayer
import com.miniclick.calltrackmanage.util.audio.PlaybackMetadata
import com.miniclick.calltrackmanage.util.formatting.cleanNumber
import com.miniclick.calltrackmanage.util.formatting.formatDurationShort
import com.miniclick.calltrackmanage.util.formatting.formatTime
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
    onCustomLookup: (String) -> Unit,
    customLookupEnabled: Boolean = false
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
                // Avatar with optional Custom Lookup overlay
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .then(
                            if (customLookupEnabled) Modifier.clickable { onCustomLookup(person.number) }
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (customLookupEnabled) {
                        // Show search icon overlay when custom lookup is enabled
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Custom Lookup",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // Show initial letter when custom lookup is disabled
                        val initial = (person.name?.firstOrNull() ?: person.number.lastOrNull() ?: '?')
                            .uppercaseChar()
                        Text(
                            text = initial.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
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
            }

            // Sub-header for Label and Person Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label Section
                if (!person.label.isNullOrEmpty()) {
                    LabelChip(
                        label = person.label, 
                        onClick = { showLabelDialog = true }
                    )
                } else {
                    NoteChip(
                        note = "Add Label",
                        icon = Icons.AutoMirrored.Filled.Label,
                        onClick = { showLabelDialog = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Person Note Section
                if (!person.personNote.isNullOrEmpty()) {
                    NoteChip(
                        note = person.personNote,
                        icon = Icons.AutoMirrored.Filled.StickyNote2,
                        onClick = { showPersonNoteDialog = true },
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                } else {
                    NoteChip(
                        note = "Add Person Note",
                        icon = Icons.AutoMirrored.Filled.NoteAdd,
                        onClick = { showPersonNoteDialog = true },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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


