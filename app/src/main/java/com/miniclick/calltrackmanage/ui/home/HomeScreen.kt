package com.miniclick.calltrackmanage.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.data.db.CallLogStatus
import com.miniclick.calltrackmanage.ui.utils.AudioPlayer
import com.miniclick.calltrackmanage.ui.utils.PlaybackMetadata
import com.miniclick.calltrackmanage.ui.settings.SettingsViewModel
import com.miniclick.calltrackmanage.ui.settings.TrackSimModal
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.miniclick.calltrackmanage.ui.common.SyncQueueModal
import com.miniclick.calltrackmanage.ui.common.SyncQueueItem
import com.miniclick.calltrackmanage.ui.common.PhoneLookupResultModal
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple

// ============================================
// CALLS SCREEN (Individual call logs)
// ============================================

@Composable
fun CallsScreen(
    audioPlayer: AudioPlayer,
    viewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Refresh settings whenever this screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }
    
    var showTrackSimModal by remember { mutableStateOf(false) }
    var lookupPhoneNumber by remember { mutableStateOf<String?>(null) }

    if (showTrackSimModal) {
        TrackSimModal(
            uiState = settingsState,
            viewModel = settingsViewModel,
            onDismiss = { showTrackSimModal = false }
        )
    }

    if (lookupPhoneNumber != null) {
        PhoneLookupResultModal(
            phoneNumber = lookupPhoneNumber!!,
            uiState = settingsState,
            viewModel = settingsViewModel,
            onDismiss = { lookupPhoneNumber = null }
        )
    }

    // Permissions

    // Audio Picker
    var attachTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && attachTarget != null) {
            val path = copyUriToInternalStorage(context, uri)
            if (path != null) {
                viewModel.updateRecordingPathForLog(attachTarget!!.compositeId, path)
            }
        }
        attachTarget = null
    }

    com.miniclick.calltrackmanage.ui.settings.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        // No auto-sync here to avoid lag. 
        // Sync is handled by workers triggered by call events or manual pull-to-refresh.
    }


    // Call End Detection
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val state = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE)
                    if (state == android.telephony.TelephonyManager.EXTRA_STATE_IDLE) {
                        viewModel.syncFromSystem()
                    }
                }
            }
        }
        val filter = IntentFilter(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Map of phone number to person data
    val personsMap = remember(uiState.persons) {
        uiState.persons.associateBy { it.phoneNumber }
    }

    // Map of phone number to person group with FULL history (independent of filters)
    val allPersonGroupsMap = remember(uiState.callLogs, personsMap) {
        uiState.callLogs
            .groupBy { it.phoneNumber }
            .mapValues { (number, calls) ->
                val sortedCalls = calls.sortedByDescending { it.callDate }
                val person = personsMap[number]
                PersonGroup(
                    number = number,
                    name = person?.contactName ?: calls.firstOrNull { !it.contactName.isNullOrEmpty() }?.contactName,
                    photoUri = person?.photoUri ?: calls.firstOrNull { !it.photoUri.isNullOrEmpty() }?.photoUri,
                    calls = sortedCalls,
                    lastCallDate = sortedCalls.first().callDate,
                    totalDuration = calls.sumOf { it.duration },
                    incomingCount = calls.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                    outgoingCount = calls.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                    missedCount = calls.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE },
                    personNote = person?.personNote,
                    label = person?.label
                )
            }
    }

    var selectedPersonForDetails by remember { mutableStateOf<PersonGroup?>(null) }
    
    if (selectedPersonForDetails != null) {
        PersonInteractionBottomSheet(
            person = selectedPersonForDetails!!,
            recordings = uiState.recordings,
            audioPlayer = audioPlayer,
            viewModel = viewModel,
            onDismiss = { selectedPersonForDetails = null },
            onAttachRecording = { 
                attachTarget = it
                audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onCustomLookup = { lookupPhoneNumber = it }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row
        CallsHeader(
            onSearchClick = viewModel::toggleSearchVisibility,
            onFilterClick = viewModel::toggleFiltersVisibility,
            isSearchActive = uiState.isSearchVisible,
            isFilterActive = uiState.isFiltersVisible
        )
        var showSyncQueue by remember { mutableStateOf(false) }
        
        if (showSyncQueue) {
            SyncQueueModal(
                pendingNewCalls = uiState.pendingNewCallsCount,
                pendingRelatedData = uiState.pendingMetadataUpdatesCount + uiState.pendingPersonUpdatesCount,
                pendingRecordings = uiState.pendingRecordingCount,
                onSyncAll = viewModel::fullSync,
                onDismiss = { showSyncQueue = false }
            )
        }

        // Sync Status Strip - only show when sync is configured
        if (uiState.isSyncSetup) {
            SyncStatusStrip(
                pendingCount = uiState.pendingSyncCount,
                pendingMetadata = uiState.pendingMetadataCount,
                pendingRecordings = uiState.pendingRecordingCount,
                isNetworkAvailable = uiState.isNetworkAvailable,
                onSyncNow = viewModel::syncNow,
                onShowQueue = { showSyncQueue = true }
            )
        }
        
        // Expandable Search Row
        AnimatedVisibility(
            visible = uiState.isSearchVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SearchInputRow(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )
        }
        
        // Expandable Filters Row
        AnimatedVisibility(
            visible = uiState.isFiltersVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FilterChipsRow(
                callTypeFilter = uiState.callTypeFilter,
                connectedFilter = uiState.connectedFilter,
                notesFilter = uiState.notesFilter,
                contactsFilter = uiState.contactsFilter,
                attendedFilter = uiState.attendedFilter,
                onCallTypeFilterChange = viewModel::setCallTypeFilter,
                onConnectedFilterChange = viewModel::setConnectedFilter,
                onNotesFilterChange = viewModel::setNotesFilter,
                onContactsFilterChange = viewModel::setContactsFilter,
                onAttendedFilterChange = viewModel::setAttendedFilter,
                dateRange = uiState.dateRange,
                onDateRangeChange = { range, start, end -> viewModel.setDateRange(range, start, end) },
                labelFilter = uiState.labelFilter,
                onLabelFilterChange = viewModel::setLabelFilter,
                availableLabels = remember(uiState.persons) { uiState.persons.mapNotNull { it.label }.filter { it.isNotEmpty() }.distinct().sorted() }
            )
        }
        
        // Content
        OnboardingGuide(
            asEmptyState = true,
            modifier = Modifier.weight(1f)
        ) {
            Box(Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.simSelection == "Off") {
                    EmptyState(
                        icon = Icons.Default.SimCard,
                        title = "Select Sim Card to Track",
                        description = "Capture your call logs by selecting which SIM cards to monitor.",
                        action = {
                            Button(
                                onClick = { showTrackSimModal = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Done, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Select SIM Now")
                            }
                        }
                    )
                } else if (uiState.filteredLogs.isEmpty()) {
                    val isFiltered = uiState.searchQuery.isNotEmpty() || 
                                    uiState.callTypeFilter != CallTypeFilter.ALL ||
                                    uiState.connectedFilter != ConnectedFilter.ALL ||
                                    uiState.notesFilter != NotesFilter.ALL ||
                                    uiState.contactsFilter != ContactsFilter.ALL ||
                                    uiState.attendedFilter != AttendedFilter.ALL ||
                                    uiState.labelFilter.isNotEmpty()
                    
                    if (isFiltered) {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            title = "No results found",
                            description = "Try adjusting your filters or search query."
                        )
                    } else if (uiState.isSyncing) {
                        EmptyState(
                            icon = Icons.Default.Sync,
                            title = "Scanning for calls...",
                            description = "We are importing your call history from the device. Please wait a moment."
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Default.History,
                            title = "No calls yet",
                            description = "Your call history will appear here once tracking starts.",
                            action = {
                                Button(
                                    onClick = { viewModel.syncFromSystem() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sync Now")
                                }
                            }
                        )
                    }
                } else {
                    CallLogList(
                        logs = uiState.filteredLogs,
                        recordings = uiState.recordings,
                        personGroupsMap = allPersonGroupsMap,
                        modifier = Modifier.fillMaxSize(),
                        onSaveCallNote = { id, note -> viewModel.saveCallNote(id, note) },
                        onSavePersonNote = { number, note -> viewModel.savePersonNote(number, note) },
                        viewModel = viewModel,
                        audioPlayer = audioPlayer,
                        whatsappPreference = uiState.whatsappPreference,
                        context = context,
                        onViewMoreClick = { selectedPersonForDetails = it },
                        onAttachRecording = { 
                            attachTarget = it
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        canExclude = uiState.allowPersonalExclusion || !uiState.isSyncSetup,
                        onCustomLookup = { lookupPhoneNumber = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsHeader(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    isSearchActive: Boolean,
    isFilterActive: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title
            Text(
                text = "Calls",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Action Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Search Icon
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Filter Icon
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================
// PERSONS SCREEN (Grouped calls by contact)
// ============================================

data class PersonGroup(
    val number: String,
    val name: String?,
    val photoUri: String?,
    val calls: List<CallDataEntity>,
    val lastCallDate: Long,
    val totalDuration: Long,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int,
    val personNote: String? = null,
    val label: String? = null
)

@Composable
fun PersonsScreen(
    audioPlayer: AudioPlayer,
    viewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Refresh settings whenever this screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }
    
    var showTrackSimModal by remember { mutableStateOf(false) }
    var lookupPhoneNumber by remember { mutableStateOf<String?>(null) }

    if (showTrackSimModal) {
        TrackSimModal(
            uiState = settingsState,
            viewModel = settingsViewModel,
            onDismiss = { showTrackSimModal = false }
        )
    }

    if (lookupPhoneNumber != null) {
        PhoneLookupResultModal(
            phoneNumber = lookupPhoneNumber!!,
            uiState = settingsState,
            viewModel = settingsViewModel,
            onDismiss = { lookupPhoneNumber = null }
        )
    }

    // Audio Picker
    var attachTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && attachTarget != null) {
            val path = copyUriToInternalStorage(context, uri)
            if (path != null) {
                viewModel.updateRecordingPathForLog(attachTarget!!.compositeId, path)
            }
        }
        attachTarget = null
    }

    com.miniclick.calltrackmanage.ui.settings.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        // No auto-sync here to avoid lag. 
        // Sync is handled by workers triggered by call events or manual pull-to-refresh.
    }

    // Map of phone number to person data
    val personsMap = remember(uiState.persons) {
        uiState.persons.associateBy { it.phoneNumber }
    }

    // Map of ALL phone numbers to their FULL history groups
    val allPersonGroupsMap = remember(uiState.callLogs, personsMap) {
        uiState.callLogs
            .groupBy { it.phoneNumber }
            .mapValues { (number, calls) ->
                val sortedCalls = calls.sortedByDescending { it.callDate }
                val person = personsMap[number]
                PersonGroup(
                    number = number,
                    name = person?.contactName ?: calls.firstOrNull { !it.contactName.isNullOrEmpty() }?.contactName,
                    photoUri = person?.photoUri ?: calls.firstOrNull { !it.photoUri.isNullOrEmpty() }?.photoUri,
                    calls = sortedCalls,
                    lastCallDate = sortedCalls.first().callDate,
                    totalDuration = calls.sumOf { it.duration },
                    incomingCount = calls.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                    outgoingCount = calls.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                    missedCount = calls.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE },
                    personNote = person?.personNote,
                    label = person?.label
                )
            }
    }

    // Filtered list of groups to display based on UI filters
    val displayedPersonGroups = remember(uiState.filteredLogs, allPersonGroupsMap) {
        val filteredNumbers = uiState.filteredLogs.map { it.phoneNumber }.toSet()
        allPersonGroupsMap.filterKeys { it in filteredNumbers }
            .values
            .sortedByDescending { it.lastCallDate }
    }

    var selectedPersonForDetails by remember { mutableStateOf<PersonGroup?>(null) }
    
    if (selectedPersonForDetails != null) {
        PersonInteractionBottomSheet(
            person = selectedPersonForDetails!!,
            recordings = uiState.recordings,
            audioPlayer = audioPlayer,
            viewModel = viewModel,
            onDismiss = { selectedPersonForDetails = null },
            onAttachRecording = { 
                attachTarget = it
                audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onCustomLookup = { lookupPhoneNumber = it }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        PersonsHeader(
            onSearchClick = viewModel::toggleSearchVisibility,
            onFilterClick = viewModel::toggleFiltersVisibility,
            isSearchActive = uiState.isSearchVisible,
            isFilterActive = uiState.isFiltersVisible
        )
        
        var showSyncQueue by remember { mutableStateOf(false) }
        
        if (showSyncQueue) {
            SyncQueueModal(
                pendingNewCalls = uiState.pendingNewCallsCount,
                pendingRelatedData = uiState.pendingMetadataUpdatesCount + uiState.pendingPersonUpdatesCount,
                pendingRecordings = uiState.pendingRecordingCount,
                onSyncAll = viewModel::fullSync,
                onDismiss = { showSyncQueue = false }
            )
        }

        // Sync Status Strip - only show when sync is configured
        if (uiState.isSyncSetup) {
            SyncStatusStrip(
                pendingCount = uiState.pendingSyncCount,
                pendingMetadata = uiState.pendingMetadataCount,
                pendingRecordings = uiState.pendingRecordingCount,
                isNetworkAvailable = uiState.isNetworkAvailable,
                onSyncNow = viewModel::syncNow,
                onShowQueue = { showSyncQueue = true }
            )
        }
        
        // Expandable Search Row
        AnimatedVisibility(
            visible = uiState.isSearchVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SearchInputRow(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )
        }
        
        // Expandable Filters Row
        AnimatedVisibility(
            visible = uiState.isFiltersVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FilterChipsRow(
                callTypeFilter = uiState.callTypeFilter,
                connectedFilter = uiState.connectedFilter,
                notesFilter = uiState.notesFilter,
                contactsFilter = uiState.contactsFilter,
                attendedFilter = uiState.attendedFilter,
                onCallTypeFilterChange = viewModel::setCallTypeFilter,
                onConnectedFilterChange = viewModel::setConnectedFilter,
                onNotesFilterChange = viewModel::setNotesFilter,
                onContactsFilterChange = viewModel::setContactsFilter,
                onAttendedFilterChange = viewModel::setAttendedFilter,
                dateRange = uiState.dateRange,
                onDateRangeChange = { range, start, end -> viewModel.setDateRange(range, start, end) },
                labelFilter = uiState.labelFilter,
                onLabelFilterChange = viewModel::setLabelFilter,
                availableLabels = remember(uiState.persons) { uiState.persons.mapNotNull { it.label }.filter { it.isNotEmpty() }.distinct().sorted() }
            )
        }
        
        // Content
        OnboardingGuide(
            asEmptyState = true,
            modifier = Modifier.weight(1f)
        ) {
            Box(Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.simSelection == "Off") {
                    EmptyState(
                        icon = Icons.Default.SimCard,
                        title = "Select Sim Card to Track",
                        description = "Capture your call logs by selecting which SIM cards to monitor.",
                        action = {
                            Button(
                                onClick = { showTrackSimModal = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Done, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Select SIM Now")
                            }
                        }
                    )
                } else if (displayedPersonGroups.isEmpty()) {
                    val isFiltered = uiState.searchQuery.isNotEmpty() || 
                                    uiState.callTypeFilter != CallTypeFilter.ALL ||
                                    uiState.connectedFilter != ConnectedFilter.ALL ||
                                    uiState.notesFilter != NotesFilter.ALL ||
                                    uiState.contactsFilter != ContactsFilter.ALL ||
                                    uiState.attendedFilter != AttendedFilter.ALL ||
                                    uiState.labelFilter.isNotEmpty()
    
                    if (isFiltered) {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            title = "No results found",
                            description = "Try adjusting your filters or search query."
                        )
                    } else if (uiState.isSyncing) {
                        EmptyState(
                            icon = Icons.Default.Sync,
                            title = "Identifying contacts...",
                            description = "We are organizing your contacts based on call history. Please wait."
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Default.Group,
                            title = "No contacts yet",
                            description = "People you interact with will appear here.",
                            action = {
                                Button(
                                    onClick = { viewModel.syncFromSystem() },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Refresh List")
                                }
                            }
                        )
                    }
                } else {
                    PersonsList(
                        persons = displayedPersonGroups,
                        recordings = uiState.recordings,
                        audioPlayer = audioPlayer,
                        context = context,
                        viewModel = viewModel,
                        whatsappPreference = uiState.whatsappPreference,
                        onExclude = { viewModel.excludeNumber(it) },
                        onViewMoreClick = { selectedPersonForDetails = it },
                        onAttachRecording = { 
                            attachTarget = it
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        canExclude = uiState.allowPersonalExclusion || !uiState.isSyncSetup,
                        onCustomLookup = { lookupPhoneNumber = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonsHeader(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    isSearchActive: Boolean,
    isFilterActive: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Title
            Text(
                text = "Persons",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Action Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PersonsList(
    persons: List<PersonGroup>,
    recordings: Map<String, String>,
    audioPlayer: AudioPlayer,
    context: Context,
    viewModel: HomeViewModel,
    whatsappPreference: String,
    onExclude: (String) -> Unit,
    onViewMoreClick: (PersonGroup) -> Unit,
    onAttachRecording: (CallDataEntity) -> Unit,
    canExclude: Boolean = true,
    onCustomLookup: (String) -> Unit
) {
    var expandedNumber by remember { mutableStateOf<String?>(null) }
    var excludeTarget by remember { mutableStateOf<PersonGroup?>(null) }
    var longPressTarget by remember { mutableStateOf<PersonGroup?>(null) }
    
    // Note Dialog States
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
        NoteDialog(
            title = "Person Label",
            initialNote = labelTarget?.label ?: "",
            label = "Label (e.g., VIP, Lead, Spam)",
            buttonText = "Save Label",
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
                    
                    // Exclude Option
                    if (canExclude) {
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
                                "Exclude from Tracking",
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
    
    if (excludeTarget != null) {
        AlertDialog(
            onDismissRequest = { excludeTarget = null },
            title = { Text("Exclude Number") },
            text = { Text("Do you want to exclude ${excludeTarget?.let { cleanNumber(it.number) }} from tracking? This will hide all existing calls and future calls from this number.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        excludeTarget?.let { onExclude(it.number) }
                        excludeTarget = null
                    }
                ) {
                    Text("Exclude", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { excludeTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(persons, key = { it.number }) { person ->
            val isExpanded = expandedNumber == person.number
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

            PersonCard(
                person = person,
                isExpanded = isExpanded,
                recordings = recordings,
                audioPlayer = audioPlayer,
                viewModel = viewModel,
                onCardClick = {
                    expandedNumber = if (isExpanded) null else person.number
                },
                onLongClick = {
                    longPressTarget = person
                },
                onCallClick = {
                    try {
                        val cleaned = cleanNumber(person.number)
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onCopyClick = {
                    val cleaned = cleanNumber(person.number)
                    val clip = android.content.ClipData.newPlainText("Phone Number", cleaned)
                    clipboardManager.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "Copied: $cleaned", android.widget.Toast.LENGTH_SHORT).show()
                },
                onWhatsAppClick = {
                    try {
                        val cleaned = cleanNumber(person.number)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$cleaned"))
                        if (whatsappPreference != "Always Ask") {
                            intent.setPackage(whatsappPreference)
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback if preferred package fails
                        if (whatsappPreference != "Always Ask") {
                           try {
                               val cleaned = cleanNumber(person.number)
                               val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$cleaned"))
                               intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                               context.startActivity(intent)
                           } catch(e2: Exception) { e2.printStackTrace() }
                        }
                    }
                },
                onPersonNoteClick = { personNoteTarget = person },
                onCallNoteClick = { callNoteTarget = it },
                onAddContactClick = {
                    try {
                        val cleaned = cleanNumber(person.number)
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
                            putExtra("lead_name", person.name ?: "")
                            putExtra("lead_phone", person.number)
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
                onViewMoreClick = { onViewMoreClick(person) },
                onLabelClick = { labelTarget = person },
                onAttachRecording = onAttachRecording,
                onMarkAllReviewed = { viewModel.markAllCallsReviewed(person.number) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonCard(
    person: PersonGroup,
    isExpanded: Boolean,
    recordings: Map<String, String>,
    audioPlayer: AudioPlayer,
    viewModel: HomeViewModel,
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
    val uiState by viewModel.uiState.collectAsState()
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
                val lastCallType = person.calls.maxByOrNull { it.callDate }?.callType
                val (iconColor, bgColor) = when (lastCallType) {
                    android.provider.CallLog.Calls.INCOMING_TYPE -> Color(0xFF4CAF50) to Color(0xFFE8F5E9)
                    android.provider.CallLog.Calls.OUTGOING_TYPE -> Color(0xFF2196F3) to Color(0xFFE3F2FD)
                    android.provider.CallLog.Calls.MISSED_TYPE -> Color(0xFFF44336) to Color(0xFFFFEBEE)
                    else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                }
                
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (lastCallType) {
                            android.provider.CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Filled.CallReceived
                            android.provider.CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Filled.CallMade
                            android.provider.CallLog.Calls.MISSED_TYPE -> Icons.AutoMirrored.Filled.CallMissed
                            else -> Icons.Default.Phone
                        },
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
                        val allReviewed = person.calls.isNotEmpty() && person.calls.all { it.reviewed }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = person.name ?: cleanNumber(person.number),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                    
                    // Removed separate number display if name exists as per request

                    if (!person.personNote.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.StickyNote2,
                                null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = person.personNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!person.label.isNullOrEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        LabelChip(label = person.label, onClick = onLabelClick)
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
                        
                        Text(
                            text = getRelativeTime(person.lastCallDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded Section
            if (isExpanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                
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
                
                // Calls List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Recent Interactions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val recentCalls = remember(person.calls) { person.calls.take(5) }
                    recentCalls.forEach { call ->
                        // Trigger check for recording path
                        LaunchedEffect(Unit) {
                            if (call.localRecordingPath == null) {
                                viewModel.getRecordingForLog(call)
                            }
                        }

                        InteractionRow(
                            call = call,
                            recordings = recordings,
                            audioPlayer = audioPlayer,
                            callRecordEnabled = uiState.callRecordEnabled,
                            onNoteClick = onCallNoteClick,
                            onAttachRecording = onAttachRecording
                        )
                    }
                    
                    if (person.calls.size > 5) {
                        TextButton(
                            onClick = onViewMoreClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "View all ${person.calls.size} interactions",
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
}

@Composable
fun CallTypeSmallBadge(icon: ImageVector, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, null, modifier = Modifier.size(10.dp), tint = color)
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun LabelChip(label: String, onClick: (() -> Unit)? = null) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick) else Modifier
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}


fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 172800_000 -> "Yesterday"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

// ============================================
// REPORTS SCREEN (Analytics placeholder)
// ============================================

@Composable
fun ReportsScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Calculate stats from filtered call logs
    val stats = remember(uiState.filteredLogs) {
        val logs = uiState.filteredLogs
        mapOf(
            "totalCalls" to logs.size,
            "incomingCalls" to logs.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
            "outgoingCalls" to logs.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
            "missedCalls" to logs.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE },
            "uniqueContacts" to logs.distinctBy { it.phoneNumber }.size,
            "totalDuration" to logs.sumOf { it.duration }.toInt(),
            "withNotes" to logs.count { !it.callNote.isNullOrEmpty() },
            "connectedCalls" to logs.count { it.duration > 0 }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Filter Icon
                IconButton(onClick = viewModel::toggleFiltersVisibility) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (uiState.isFiltersVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Expandable Filters Row
        AnimatedVisibility(
            visible = uiState.isFiltersVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FilterChipsRow(
                callTypeFilter = uiState.callTypeFilter,
                connectedFilter = uiState.connectedFilter,
                notesFilter = uiState.notesFilter,
                contactsFilter = uiState.contactsFilter,
                attendedFilter = uiState.attendedFilter,
                onCallTypeFilterChange = viewModel::setCallTypeFilter,
                onConnectedFilterChange = viewModel::setConnectedFilter,
                onNotesFilterChange = viewModel::setNotesFilter,
                onContactsFilterChange = viewModel::setContactsFilter,
                onAttendedFilterChange = viewModel::setAttendedFilter,
                dateRange = uiState.dateRange,
                onDateRangeChange = { range, start, end -> viewModel.setDateRange(range, start, end) },
                labelFilter = uiState.labelFilter,
                onLabelFilterChange = viewModel::setLabelFilter
            )
        }
        
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.simSelection == "Off") {
                EmptyState(
                    icon = Icons.Default.SimCard,
                    title = "Select Sim Card to Track",
                    description = "Capture your call logs by selecting which SIM cards to monitor.",
                    action = {
                        Button(
                            onClick = { viewModel.toggleFiltersVisibility() }, // Or some other action, but here button is usually implicit
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go to Settings")
                        }
                    }
                )
            } else if (uiState.filteredLogs.isEmpty()) {
                val isFiltered = uiState.searchQuery.isNotEmpty() || 
                                uiState.callTypeFilter != CallTypeFilter.ALL ||
                                uiState.connectedFilter != ConnectedFilter.ALL ||
                                uiState.notesFilter != NotesFilter.ALL ||
                                uiState.contactsFilter != ContactsFilter.ALL ||
                                uiState.attendedFilter != AttendedFilter.ALL ||
                                uiState.labelFilter.isNotEmpty()
                
                if (isFiltered) {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No results found",
                        description = "Try adjusting your filters or search query."
                    )
                } else {
                    EmptyState(
                        icon = Icons.Default.BarChart,
                        title = "No data for reports",
                        description = "Reports will be generated once you have some call activity."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Overview Card
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatItem(
                                        value = stats["totalCalls"].toString(),
                                        label = "Total Calls",
                                        icon = Icons.Default.Call,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    StatItem(
                                        value = stats["uniqueContacts"].toString(),
                                        label = "Contacts",
                                        icon = Icons.Default.People,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    StatItem(
                                        value = formatDurationShort(stats["totalDuration"]?.toLong() ?: 0L),
                                        label = "Talk Time",
                                        icon = Icons.Default.Timer,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                    
                    // Call Types Breakdown
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Call Types",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(16.dp))
                                
                                StatRow(
                                    label = "Incoming",
                                    value = stats["incomingCalls"].toString(),
                                    icon = Icons.AutoMirrored.Filled.CallReceived,
                                    color = Color(0xFF4CAF50)
                                )
                                StatRow(
                                    label = "Outgoing",
                                    value = stats["outgoingCalls"].toString(),
                                    icon = Icons.AutoMirrored.Filled.CallMade,
                                    color = Color(0xFF2196F3)
                                )
                                StatRow(
                                    label = "Missed",
                                    value = stats["missedCalls"].toString(),
                                    icon = Icons.AutoMirrored.Filled.CallMissed,
                                    color = Color(0xFFF44336)
                                )
                            }
                        }
                    }
                    
                    // Connection Stats
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Connection Stats",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(16.dp))
                                
                                val connectedCalls = stats["connectedCalls"] ?: 0
                                val totalCalls = stats["totalCalls"] ?: 1
                                val connectionRate = if (totalCalls > 0) (connectedCalls * 100 / totalCalls) else 0
                                
                                StatRow(
                                    label = "Connected Calls",
                                    value = "$connectedCalls",
                                    icon = Icons.Default.CheckCircle,
                                    color = Color(0xFF4CAF50)
                                )
                                StatRow(
                                    label = "With Notes",
                                    value = stats["withNotes"].toString(),
                                    icon = Icons.AutoMirrored.Filled.StickyNote2,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                
                                Spacer(Modifier.height(12.dp))
                                
                                // Connection rate progress
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Connection Rate",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            "$connectionRate%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { connectionRate / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

fun formatDurationShort(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

// ============================================
// SHARED COMPONENTS
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInputRow(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search number, name, note...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .focusRequester(focusRequester),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipsRow(
    callTypeFilter: CallTypeFilter,
    connectedFilter: ConnectedFilter,
    notesFilter: NotesFilter,
    contactsFilter: ContactsFilter,
    attendedFilter: AttendedFilter,
    onCallTypeFilterChange: (CallTypeFilter) -> Unit,
    onConnectedFilterChange: (ConnectedFilter) -> Unit,
    onNotesFilterChange: (NotesFilter) -> Unit,
    onContactsFilterChange: (ContactsFilter) -> Unit,
    onAttendedFilterChange: (AttendedFilter) -> Unit,
    dateRange: DateRange,
    onDateRangeChange: (DateRange, Long?, Long?) -> Unit,
    labelFilter: String,
    onLabelFilterChange: (String) -> Unit,
    availableLabels: List<String> = emptyList()
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    if (showDatePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { start, end ->
                onDateRangeChange(DateRange.CUSTOM, start, end)
                showDatePicker = false
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date Range Filter
            FilterDropdownChip(
                label = when (dateRange) {
                    DateRange.TODAY -> "Today"
                    DateRange.LAST_3_DAYS -> "Last 3 Days"
                    DateRange.LAST_7_DAYS -> "Last 7 Days"
                    DateRange.LAST_14_DAYS -> "Last 14 Days"
                    DateRange.LAST_30_DAYS -> "Last 30 Days"
                    DateRange.THIS_MONTH -> "This Month"
                    DateRange.PREVIOUS_MONTH -> "Prev Month"
                    DateRange.CUSTOM -> "Custom Range"
                    DateRange.ALL -> "Date: All"
                },
                icon = Icons.Default.CalendarToday,
                isActive = dateRange != DateRange.ALL,
                options = listOf(
                    FilterOption("Today", Icons.Default.Today, dateRange == DateRange.TODAY) { onDateRangeChange(DateRange.TODAY, null, null) },
                    FilterOption("Last 3 Days", Icons.Default.Event, dateRange == DateRange.LAST_3_DAYS) { onDateRangeChange(DateRange.LAST_3_DAYS, null, null) },
                    FilterOption("Last 7 Days", Icons.Default.Event, dateRange == DateRange.LAST_7_DAYS) { onDateRangeChange(DateRange.LAST_7_DAYS, null, null) },
                    FilterOption("Last 14 Days", Icons.Default.Event, dateRange == DateRange.LAST_14_DAYS) { onDateRangeChange(DateRange.LAST_14_DAYS, null, null) },
                    FilterOption("Last 30 Days", Icons.Default.Event, dateRange == DateRange.LAST_30_DAYS) { onDateRangeChange(DateRange.LAST_30_DAYS, null, null) },
                    FilterOption("This Month", Icons.Default.CalendarMonth, dateRange == DateRange.THIS_MONTH) { onDateRangeChange(DateRange.THIS_MONTH, null, null) },
                    FilterOption("Previous Month", Icons.Default.CalendarMonth, dateRange == DateRange.PREVIOUS_MONTH) { onDateRangeChange(DateRange.PREVIOUS_MONTH, null, null) },
                    FilterOption("Custom", Icons.Default.DateRange, dateRange == DateRange.CUSTOM) { 
                        showDatePicker = true
                    },
                    FilterOption("All Time", Icons.Default.AllInclusive, dateRange == DateRange.ALL) { onDateRangeChange(DateRange.ALL, null, null) }
                )
            )

            // Label Filter
            if (availableLabels.isNotEmpty() || labelFilter.isNotEmpty()) {
                FilterDropdownChip(
                    label = if (labelFilter.isEmpty()) "Label" else labelFilter,
                    icon = Icons.AutoMirrored.Filled.Label,
                    isActive = labelFilter.isNotEmpty(),
                    options = listOf(
                        FilterOption("All", Icons.AutoMirrored.Filled.LabelOff, labelFilter.isEmpty()) { onLabelFilterChange("") }
                    ) + availableLabels.map { label ->
                        FilterOption(label, Icons.AutoMirrored.Filled.Label, labelFilter == label) { onLabelFilterChange(label) }
                    }
                )
            }

            // Call Type Filter
            FilterDropdownChip(
                label = when (callTypeFilter) {
                    CallTypeFilter.ALL -> "Call Type"
                    CallTypeFilter.INCOMING -> "Incoming"
                    CallTypeFilter.OUTGOING -> "Outgoing"
                    CallTypeFilter.MISSED -> "Missed"
                    CallTypeFilter.REJECTED -> "Rejected"
                },
                icon = when (callTypeFilter) {
                    CallTypeFilter.ALL -> Icons.Default.Phone
                    CallTypeFilter.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
                    CallTypeFilter.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
                    CallTypeFilter.MISSED -> Icons.AutoMirrored.Filled.CallMissed
                    CallTypeFilter.REJECTED -> Icons.Default.PhoneDisabled
                },
                isActive = callTypeFilter != CallTypeFilter.ALL,
                options = listOf(
                    FilterOption("All", Icons.Default.Phone, callTypeFilter == CallTypeFilter.ALL) { onCallTypeFilterChange(CallTypeFilter.ALL) },
                    FilterOption("Incoming", Icons.AutoMirrored.Filled.CallReceived, callTypeFilter == CallTypeFilter.INCOMING) { onCallTypeFilterChange(CallTypeFilter.INCOMING) },
                    FilterOption("Outgoing", Icons.AutoMirrored.Filled.CallMade, callTypeFilter == CallTypeFilter.OUTGOING) { onCallTypeFilterChange(CallTypeFilter.OUTGOING) },
                    FilterOption("Missed", Icons.AutoMirrored.Filled.CallMissed, callTypeFilter == CallTypeFilter.MISSED) { onCallTypeFilterChange(CallTypeFilter.MISSED) },
                    FilterOption("Rejected", Icons.Default.PhoneDisabled, callTypeFilter == CallTypeFilter.REJECTED) { onCallTypeFilterChange(CallTypeFilter.REJECTED) }
                )
            )
            
            // Connected Filter
            FilterDropdownChip(
                label = when (connectedFilter) {
                    ConnectedFilter.ALL -> "Connection"
                    ConnectedFilter.CONNECTED -> "Connected"
                    ConnectedFilter.NOT_CONNECTED -> "Not Connected"
                },
                icon = when (connectedFilter) {
                    ConnectedFilter.ALL -> Icons.Default.PhoneInTalk
                    ConnectedFilter.CONNECTED -> Icons.Default.CheckCircle
                    ConnectedFilter.NOT_CONNECTED -> Icons.Default.Cancel
                },
                isActive = connectedFilter != ConnectedFilter.ALL,
                options = listOf(
                    FilterOption("All", Icons.Default.PhoneInTalk, connectedFilter == ConnectedFilter.ALL) { onConnectedFilterChange(ConnectedFilter.ALL) },
                    FilterOption("Connected", Icons.Default.CheckCircle, connectedFilter == ConnectedFilter.CONNECTED) { onConnectedFilterChange(ConnectedFilter.CONNECTED) },
                    FilterOption("Not Connected", Icons.Default.Cancel, connectedFilter == ConnectedFilter.NOT_CONNECTED) { onConnectedFilterChange(ConnectedFilter.NOT_CONNECTED) }
                )
            )
            
            // Notes Filter
            FilterDropdownChip(
                label = when (notesFilter) {
                    NotesFilter.ALL -> "Notes"
                    NotesFilter.WITH_NOTE -> "With Note"
                    NotesFilter.WITHOUT_NOTE -> "Without Note"
                },
                icon = when (notesFilter) {
                    NotesFilter.ALL -> Icons.AutoMirrored.Filled.StickyNote2
                    NotesFilter.WITH_NOTE -> Icons.AutoMirrored.Filled.NoteAdd
                    NotesFilter.WITHOUT_NOTE -> Icons.Default.NoteAlt
                },
                isActive = notesFilter != NotesFilter.ALL,
                options = listOf(
                    FilterOption("All", Icons.AutoMirrored.Filled.StickyNote2, notesFilter == NotesFilter.ALL) { onNotesFilterChange(NotesFilter.ALL) },
                    FilterOption("With Note", Icons.AutoMirrored.Filled.NoteAdd, notesFilter == NotesFilter.WITH_NOTE) { onNotesFilterChange(NotesFilter.WITH_NOTE) },
                    FilterOption("Without Note", Icons.Default.NoteAlt, notesFilter == NotesFilter.WITHOUT_NOTE) { onNotesFilterChange(NotesFilter.WITHOUT_NOTE) }
                )
            )
            
            // Contacts Filter
            FilterDropdownChip(
                label = when (contactsFilter) {
                    ContactsFilter.ALL -> "Contacts"
                    ContactsFilter.IN_CONTACTS -> "In Contacts"
                    ContactsFilter.NOT_IN_CONTACTS -> "Unknown"
                },
                icon = when (contactsFilter) {
                    ContactsFilter.ALL -> Icons.Default.Contacts
                    ContactsFilter.IN_CONTACTS -> Icons.Default.PersonAdd
                    ContactsFilter.NOT_IN_CONTACTS -> Icons.Default.PersonOff
                },
                isActive = contactsFilter != ContactsFilter.ALL,
                options = listOf(
                    FilterOption("All", Icons.Default.Contacts, contactsFilter == ContactsFilter.ALL) { onContactsFilterChange(ContactsFilter.ALL) },
                    FilterOption("In Contacts", Icons.Default.PersonAdd, contactsFilter == ContactsFilter.IN_CONTACTS) { onContactsFilterChange(ContactsFilter.IN_CONTACTS) },
                    FilterOption("Unknown", Icons.Default.PersonOff, contactsFilter == ContactsFilter.NOT_IN_CONTACTS) { onContactsFilterChange(ContactsFilter.NOT_IN_CONTACTS) }
                )
            )
            
            // Attended Filter
            FilterDropdownChip(
                label = when (attendedFilter) {
                    AttendedFilter.ALL -> "Attended"
                    AttendedFilter.ATTENDED -> "Attended"
                    AttendedFilter.NEVER_ATTENDED -> "Never Attended"
                },
                icon = when (attendedFilter) {
                    AttendedFilter.ALL -> Icons.Default.History
                    AttendedFilter.ATTENDED -> Icons.Default.Done
                    AttendedFilter.NEVER_ATTENDED -> Icons.Default.Block
                },
                isActive = attendedFilter != AttendedFilter.ALL,
                options = listOf(
                    FilterOption("All", Icons.Default.History, attendedFilter == AttendedFilter.ALL) { onAttendedFilterChange(AttendedFilter.ALL) },
                    FilterOption("Attended", Icons.Default.Done, attendedFilter == AttendedFilter.ATTENDED) { onAttendedFilterChange(AttendedFilter.ATTENDED) },
                    FilterOption("Never Attended", Icons.Default.Block, attendedFilter == AttendedFilter.NEVER_ATTENDED) { onAttendedFilterChange(AttendedFilter.NEVER_ATTENDED) }
                )
            )
        }
    }
}

data class FilterOption(
    val label: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
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
    
    // Note Dialog State
    var callNoteTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    
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
                    .padding(bottom = 16.dp),
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
                    Text(
                        text = person.name ?: cleanNumber(person.number),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    // No separate number display if name exists as per request
                }

                IconButton(onClick = { onCustomLookup(person.number) }) {
                    Icon(
                        imageVector = Icons.Default.ManageSearch,
                        contentDescription = "Custom Lookup",
                        tint = MaterialTheme.colorScheme.primary
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
                contentPadding = PaddingValues(bottom = navBarPadding + 24.dp)
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

            // Right side: Note Toggle, Duration and Play Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                if (call.duration > 0) {
                    Text(
                        text = formatDurationShort(call.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdownChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    options: List<FilterOption>
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        FilterChip(
            selected = isActive,
            onClick = { expanded = true },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (option.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option.label)
                        }
                    },
                    onClick = {
                        option.onClick()
                        expanded = false
                    },
                    trailingIcon = if (option.isSelected) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePickerDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onDateRangeSelected(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = { Text("Select Date Range", modifier = Modifier.padding(16.dp)) },
            modifier = Modifier.weight(1f)
        )
    }
}

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
    onCustomLookup: (String) -> Unit
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
        NoteDialog(
            title = "Person Label",
            initialNote = personGroupsMap[labelTarget?.phoneNumber]?.label ?: "",
            label = "Label (e.g., VIP, Lead, Spam)",
            buttonText = "Save Label",
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
                    
                    // Exclude Option
                    if (canExclude) {
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
                                "Exclude from Tracking",
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
    
    // Exclusion Dialog State
    if (excludeTarget != null) {
        AlertDialog(
            onDismissRequest = { excludeTarget = null },
            title = { Text("Exclude Number") },
            text = { Text("Do you want to exclude ${excludeTarget?.let { cleanNumber(it.phoneNumber) }} from tracking? This will hide all existing calls and future calls from this number.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        excludeTarget?.let { viewModel.excludeNumber(it.phoneNumber) }
                        excludeTarget = null
                    }
                ) {
                    Text("Exclude", color = MaterialTheme.colorScheme.error)
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

    LazyColumn(modifier = modifier) {
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

fun getDateHeader(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)
    
    calendar.timeInMillis = dateMillis
    val logDay = calendar.get(Calendar.DAY_OF_YEAR)
    val logYear = calendar.get(Calendar.YEAR)
    
    return when {
        year == logYear && today == logDay -> "Today"
        year == logYear && today == logDay + 1 -> "Yesterday"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dateMillis))
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

                // COLUMN 3: Metadata (Call Count & Time) - Top and Bottom
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Row 1 (Top): Call Count Chip (like in persons)
                    if (personGroup != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onViewMoreClick() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${personGroup.calls.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        // Spacer to maintain top alignment
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Row 2 (Bottom): Time
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.callDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Inline Player (visible if playing, even if collapsed)
            if (isThisPlaying && !isExpanded) {
                PlaybackControls(
                    audioPlayer = audioPlayer,
                    recordingPath = recordingPath!!,
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
fun NoteDialog(
    title: String,
    initialNote: String,
    label: String,
    buttonText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember { 
        mutableStateOf(
            TextFieldValue(
                text = initialNote,
                selection = TextRange(initialNote.length)
            )
        ) 
    }
    val focusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(label) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        minLines = 3,
                        maxLines = 5
                    )

                    Button(
                        onClick = { onSave(note.text) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(buttonText)
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
fun PlaybackSpeedButton(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f)
    val nextSpeed = speeds[(speeds.indexOf(currentSpeed) + 1).coerceAtLeast(0) % speeds.size]
    
    Surface(
        onClick = { onSpeedChange(nextSpeed) },
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        modifier = Modifier.height(22.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${if (currentSpeed % 1 == 0f) currentSpeed.toInt() else currentSpeed}x",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(millis: Int): String {
    val minutes = (millis / 1000) / 60
    val seconds = (millis / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

/**
 * Normalizes phone number: removes spaces, non-digits (except maybe + initially),
 * and removes the default country code (+91 or 91) if it exists.
 */
fun cleanNumber(number: String): String {
    // Keep only digits and +
    var cleaned = number.filter { it.isDigit() || it == '+' }
    
    // Remove default country code +91 or 91 (if length > 10)
    if (cleaned.startsWith("+91")) {
        cleaned = cleaned.substring(3)
    } else if (cleaned.startsWith("91") && cleaned.length > 10) {
        cleaned = cleaned.substring(2)
    }
    
    // Remove leading 0 if it's a long number
    if (cleaned.startsWith("0") && cleaned.length > 10) {
        cleaned = cleaned.substring(1)
    }
    
    // Final pass to remove any remaining + (if it wasn't part of +91)
    return cleaned.filter { it.isDigit() }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (action != null) {
            Spacer(modifier = Modifier.height(32.dp))
            action()
        }
    }
}

@Composable
fun SyncStatusStrip(
    pendingCount: Int,
    pendingMetadata: Int,
    pendingRecordings: Int,
    isNetworkAvailable: Boolean,
    onSyncNow: () -> Unit,
    onShowQueue: () -> Unit
) {
    AnimatedVisibility(
        visible = pendingCount > 0,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Surface(
            color = if (isNetworkAvailable) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).clickable(enabled = isNetworkAvailable) { onSyncNow() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isNetworkAvailable) Icons.Default.CloudSync else Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isNetworkAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isNetworkAvailable) 
                            "$pendingCount changes to sync" 
                            else "No internet: $pendingCount pending",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isNetworkAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNetworkAvailable) {
                        Text(
                            text = "Sync Now",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onSyncNow() }.padding(horizontal = 8.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onShowQueue,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Sync Queue",
                            modifier = Modifier.size(20.dp),
                            tint = if (isNetworkAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "attached_recording_${System.currentTimeMillis()}.m4a" 
        val file = File(context.filesDir, "attached_recordings")
        if (!file.exists()) file.mkdirs()
        val destFile = File(file, fileName)
        val outputStream = FileOutputStream(destFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
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
