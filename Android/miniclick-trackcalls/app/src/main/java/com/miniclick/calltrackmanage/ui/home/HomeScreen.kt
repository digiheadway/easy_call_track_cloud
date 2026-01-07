package com.miniclick.calltrackmanage.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.ui.common.*
import com.miniclick.calltrackmanage.ui.settings.SettingsViewModel
import com.miniclick.calltrackmanage.ui.utils.AudioPlayer
import com.miniclick.calltrackmanage.ui.utils.getFileNameFromUri
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CallsScreen(
    audioPlayer: AudioPlayer,
    viewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onOpenDialer: () -> Unit = {},
    syncStatusBar: @Composable () -> Unit = {},
    personDetailsPhone: String? = null,
    onClearPersonDetails: () -> Unit = {},
    isDialerEnabled: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showFilterModal by remember { mutableStateOf(false) }
    
    val lazyListState = rememberLazyListState()
    val isFabVisible by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
        }
    }

    val availableLabels = remember(uiState.persons) { 
        uiState.persons.mapNotNull { it.label }
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted() 
    }

    // Refresh settings whenever this screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }
    
    // Modals are handled centrally in MainActivity via SettingsViewModel

    // Audio Picker
    var attachTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && attachTarget != null) {
            val fileName = getFileNameFromUri(context, uri)
            val importedFile = RecordingRepository.getInstance(context).importSharedRecording(uri, fileName)
            if (importedFile != null) {
                viewModel.updateRecordingPathForLog(attachTarget!!.compositeId, importedFile.absolutePath)
                // Trigger immediate upload for this manually attached file
                RecordingUploadWorker.runNow(context)
            }
        }
        attachTarget = null
    }

    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.refreshSettings()
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
                val person = personsMap[number] ?: personsMap[viewModel.normalizePhoneNumber(number)]
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
    
    // Handle opening person details from external triggers (e.g. notifications)
    LaunchedEffect(personDetailsPhone, allPersonGroupsMap) {
        if (personDetailsPhone != null && allPersonGroupsMap.isNotEmpty()) {
            val group = allPersonGroupsMap[personDetailsPhone] ?: allPersonGroupsMap[viewModel.normalizePhoneNumber(personDetailsPhone)]
            if (group != null) {
                selectedPersonForDetails = group
                onClearPersonDetails()
            }
        }
    }
    
    selectedPersonForDetails?.let { person ->
        // Use the latest data from the map if it changed (reactive updates)
        val latestPerson = allPersonGroupsMap[person.number] ?: person
        PersonInteractionBottomSheet(
            person = latestPerson,
            recordings = uiState.recordings,
            audioPlayer = audioPlayer,
            viewModel = viewModel,
            onDismiss = { selectedPersonForDetails = null },
            onAttachRecording = { 
                attachTarget = it
                audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onCustomLookup = { settingsViewModel.showPhoneLookup(it) }
        )
    }

    if (showFilterModal) {
        CallFilterModal(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showFilterModal = false },
            availableLabels = availableLabels
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            AnimatedVisibility(
                visible = isDialerEnabled && isFabVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = onOpenDialer,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Dialpad, contentDescription = "Dialer")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header Row
            CallsHeader(
                onSearchClick = viewModel::toggleSearchVisibility,
                onFilterClick = { showFilterModal = true },
                isSearchActive = uiState.isSearchVisible,
                isFilterActive = showFilterModal,
                filterCount = uiState.activeFilterCount,
                dateRange = uiState.dateRange,
                onDateRangeChange = { range, start, end -> viewModel.setDateRange(range, start, end) },
                totalCallsCount = uiState.filteredLogs.size
            )
            
            val pagerState = rememberPagerState(pageCount = { CallTabFilter.entries.size })
            
            // Sync with ViewModel
            LaunchedEffect(uiState.callTypeFilter) {
                if (pagerState.currentPage != uiState.callTypeFilter.ordinal) {
                    pagerState.animateScrollToPage(uiState.callTypeFilter.ordinal)
                }
            }
            
            LaunchedEffect(pagerState.currentPage) {
                if (pagerState.currentPage != uiState.callTypeFilter.ordinal) {
                    viewModel.setCallTypeFilter(CallTabFilter.entries[pagerState.currentPage])
                }
            }

            CallTypeTabs(
                selectedFilter = uiState.callTypeFilter,
                onFilterSelected = viewModel::setCallTypeFilter,
                counts = uiState.callTypeCounts
            )
            
            syncStatusBar()
            
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
            
            // Removed FilterChipsRow as it's now integrated into the Modal
            
            // Grouped persons logic needed by both views
            val displayedPersonGroups = remember(uiState.filteredLogs, allPersonGroupsMap) {
                val filteredNumbers = uiState.filteredLogs.map { it.phoneNumber }.toSet()
                allPersonGroupsMap.filterKeys { it in filteredNumbers }
                    .values
                    .sortedByDescending { it.lastCallDate }
            }
            
            // Content
            SetupGuide(
                asEmptyState = true,
                modifier = Modifier.weight(1f)
            ) {
                LaunchedEffect(Unit) {
                    viewModel.refreshSettings()
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true
                ) { page ->
                    Box(Modifier.fillMaxSize()) {
                        when {
                            uiState.isLoading -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            uiState.simSelection == "Off" -> {
                                EmptyState(
                                    icon = Icons.Default.SimCard,
                                    title = "SIM Tracking is Off",
                                    description = "You must select at least one SIM card to Capture and track calls.",
                                    action = {
                                        Button(
                                            onClick = { settingsViewModel.toggleTrackSimModal(true) },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Settings, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Turn On Tracking")
                                        }
                                    }
                                )
                            }
                            uiState.filteredLogs.isEmpty() -> {
                                val isFiltered = uiState.activeFilterCount > 0 || uiState.searchQuery.isNotEmpty()
                                when {
                                    isFiltered -> {
                                        EmptyState(
                                            icon = Icons.Default.SearchOff,
                                            title = "No results found",
                                            description = "Try adjusting your filters or search query."
                                        )
                                    }
                                    uiState.isSyncing -> {
                                        EmptyState(
                                            icon = Icons.Default.Sync,
                                            title = "Scanning for calls...",
                                            description = "We are importing your call history from the device. Please wait a moment."
                                        )
                                    }
                                    else -> {
                                        EmptyState(
                                            icon = Icons.Default.History,
                                            title = "No calls yet",
                                            description = "Your call history will appear here once you make or receive calls.",
                                            action = {
                                                Button(
                                                    onClick = { viewModel.syncFromSystem() },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.CloudDownload, null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Fetch Call Logs")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            else -> {
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
                                    onCustomLookup = { settingsViewModel.showPhoneLookup(it) },
                                    lazyListState = lazyListState
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
fun PersonsScreen(
    audioPlayer: AudioPlayer,
    viewModel: HomeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    syncStatusBar: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showFilterModal by remember { mutableStateOf(false) }

    val availableLabels = remember(uiState.persons) { 
        uiState.persons.mapNotNull { it.label }
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted() 
    }

    // Refresh settings whenever this screen is displayed
    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }
    
    // Modals are handled centrally in MainActivity

    // Audio Picker
    var attachTarget by remember { mutableStateOf<CallDataEntity?>(null) }
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && attachTarget != null) {
            val fileName = getFileNameFromUri(context, uri)
            val importedFile = RecordingRepository.getInstance(context).importSharedRecording(uri, fileName)
            if (importedFile != null) {
                viewModel.updateRecordingPathForLog(attachTarget!!.compositeId, importedFile.absolutePath)
                // Trigger immediate upload for this manually attached file
                RecordingUploadWorker.runNow(context)
            }
        }
        attachTarget = null
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
                val person = personsMap[number] ?: personsMap[viewModel.normalizePhoneNumber(number)]
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

    // Filtered list of persons to display based on UI filters
    val filteredPersons = remember(uiState.filteredPersons, allPersonGroupsMap) {
        uiState.filteredPersons.map { person ->
            allPersonGroupsMap[person.phoneNumber] ?: PersonGroup(
                number = person.phoneNumber,
                name = person.contactName,
                photoUri = person.photoUri,
                calls = emptyList(),
                lastCallDate = person.lastCallDate ?: 0L,
                totalDuration = person.totalDuration,
                incomingCount = person.totalIncoming,
                outgoingCount = person.totalOutgoing,
                missedCount = person.totalMissed,
                personNote = person.personNote,
                label = person.label
            )
        }
    }

    var selectedPersonForDetails by remember { mutableStateOf<PersonGroup?>(null) }
    
    selectedPersonForDetails?.let { person ->
        // Use the latest data from the map if it changed (reactive updates)
        val latestPerson = allPersonGroupsMap[person.number] ?: person
        PersonInteractionBottomSheet(
            person = latestPerson,
            recordings = uiState.recordings,
            audioPlayer = audioPlayer,
            viewModel = viewModel,
            onDismiss = { selectedPersonForDetails = null },
            onAttachRecording = { 
                attachTarget = it
                audioPickerLauncher.launch(arrayOf("audio/*"))
            },
            onCustomLookup = { settingsViewModel.showPhoneLookup(it) }
        )
    }

    if (showFilterModal) {
        CallFilterModal(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showFilterModal = false },
            availableLabels = availableLabels
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        PersonsHeader(
            onSearchClick = viewModel::toggleSearchVisibility,
            onFilterClick = { showFilterModal = true },
            isSearchActive = uiState.isSearchVisible,
            isFilterActive = showFilterModal,
            filterCount = uiState.activeFilterCount,
            dateRange = uiState.dateRange,
            onDateRangeChange = { range, start, end -> viewModel.setDateRange(range, start, end) }
        )
        
        val pagerState = rememberPagerState(pageCount = { PersonTabFilter.entries.size })
            
        // Sync with ViewModel
        LaunchedEffect(uiState.personTabFilter) {
            if (pagerState.currentPage != uiState.personTabFilter.ordinal) {
                pagerState.animateScrollToPage(uiState.personTabFilter.ordinal)
            }
        }
        
        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.currentPage != uiState.personTabFilter.ordinal) {
                viewModel.setPersonTabFilter(PersonTabFilter.entries[pagerState.currentPage])
            }
        }

        PersonTypeTabs(
            selectedFilter = uiState.personTabFilter,
            onFilterSelected = viewModel::setPersonTabFilter,
            counts = uiState.personTypeCounts
        )
        
        syncStatusBar()
        
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
        
        // Removed FilterChipsRow
        
        // Content
        SetupGuide(
            asEmptyState = true,
            modifier = Modifier.weight(1f)
        ) {
            LaunchedEffect(Unit) {
                viewModel.refreshSettings()
                if (uiState.persons.isEmpty()) {
                    viewModel.syncFromSystem()
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (uiState.simSelection == "Off") {
                        // ... sim off ...
                        EmptyState(
                            icon = Icons.Default.SimCard,
                            title = "SIM Tracking is Off",
                            description = "You must select at least one SIM card to Capture and track calls.",
                            action = {
                                Button(
                                    onClick = { settingsViewModel.toggleTrackSimModal(true) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Settings, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Turn On Tracking")
                                }
                            }
                        )
                    } else if (filteredPersons.isEmpty()) {
                        val isFiltered = uiState.activeFilterCount > 0 || uiState.searchQuery.isNotEmpty()
                        if (isFiltered) {
                            EmptyState(
                                icon = Icons.Default.SearchOff,
                                title = "No results found",
                                description = "Try adjusting your filters or search query."
                            )
                        } else {
                            EmptyState(
                                icon = Icons.Default.Group,
                                title = "No contacts yet",
                                description = "Identify your contacts from call activity."
                            )
                        }
                    } else {
                        PersonsList(
                            persons = filteredPersons,
                            recordings = uiState.recordings,
                            audioPlayer = audioPlayer,
                            context = context,
                            viewModel = viewModel,
                            whatsappPreference = uiState.whatsappPreference,
                            onExclude = { viewModel.ignoreNumber(it) },
                            onNoTrack = { viewModel.noTrackNumber(it) },
                            onViewMoreClick = { selectedPersonForDetails = it },
                            onAttachRecording = { 
                                attachTarget = it
                                audioPickerLauncher.launch(arrayOf("audio/*"))
                            },
                            canExclude = uiState.allowPersonalExclusion || !uiState.isSyncSetup,
                            onCustomLookup = { settingsViewModel.showPhoneLookup(it) }
                        )
                    }
                }
            }
        }
    }
}
