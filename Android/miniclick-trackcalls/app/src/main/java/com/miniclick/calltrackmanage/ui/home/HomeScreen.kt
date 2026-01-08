package com.miniclick.calltrackmanage.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
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
    
    val pagerState = rememberPagerState(
        initialPage = uiState.callTypeFilter.ordinal,
        pageCount = { CallTabFilter.entries.size }
    )
    
    // Independent scroll states for each tab
    val scrollStates = remember { 
        List(CallTabFilter.entries.size) { LazyListState() } 
    }
    
    val isFabVisible by remember {
        derivedStateOf {
            val currentState = scrollStates.getOrNull(pagerState.currentPage)
            currentState == null || (currentState.firstVisibleItemIndex == 0 && currentState.firstVisibleItemScrollOffset == 0)
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

    // Use pre-calculated person groups from ViewModel for better performance
    val allPersonGroupsMap = uiState.personGroups

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Unified Header Unit
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column {
                    // Header Row
                    CallsHeader(
                        onSearchClick = viewModel::toggleSearchVisibility,
                        onFilterClick = { showFilterModal = true },
                        isSearchActive = uiState.isSearchVisible,
                        isFilterActive = showFilterModal,
                        filterCount = uiState.activeFilterCount,
                        dateRange = uiState.dateRange,
                        onDateRangeChange = { range, start, end -> viewModel.setDateRange(range, start, end) }
                    )
                    
                    val scope = rememberCoroutineScope()

                    Spacer(Modifier.height(4.dp))

                    // 1. Pager -> ViewModel Sync (When user settles on a page)
                    LaunchedEffect(pagerState.settledPage) {
                        val newFilter = CallTabFilter.entries[pagerState.settledPage]
                        if (uiState.callTypeFilter != newFilter) {
                            viewModel.setCallTypeFilter(newFilter)
                        }
                    }
                    
                    // 2. ViewModel -> Pager Sync (When external filter changes, e.g. from modal)
                    LaunchedEffect(uiState.callTypeFilter) {
                        if (pagerState.targetPage != uiState.callTypeFilter.ordinal && 
                            !pagerState.isScrollInProgress) {
                            pagerState.animateScrollToPage(uiState.callTypeFilter.ordinal)
                        }
                    }
                    
                    // UI Tabs (Driven by Pager for fluidity)
                    CallTypeTabs(
                        selectedFilter = CallTabFilter.entries[pagerState.currentPage],
                        onFilterSelected = { filter ->
                            scope.launch {
                                pagerState.animateScrollToPage(filter.ordinal)
                            }
                        },
                        counts = uiState.callTypeCounts
                    )
                }
            }
            
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
            
            // Removed unused displayedPersonGroups calculation to save UI thread cycles
            
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
                    userScrollEnabled = true,
                    beyondViewportPageCount = 1
                ) { page ->
                    val tab = CallTabFilter.entries[page]
                    val tabLogs = uiState.tabFilteredLogs[tab] ?: emptyList()
                    
                    Box(Modifier.fillMaxSize()) {
                        when {
                            uiState.isLoading -> {
                                Column(Modifier.fillMaxSize()) {
                                    repeat(10) {
                                        CallRowShimmer()
                                    }
                                }
                            }
                            uiState.simSelection == "Off" -> {
                                // ...
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
                            tabLogs.isEmpty() -> {
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
                                                    Text("Refresh Again")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            else -> {
                                CallLogList(
                                    logs = tabLogs,
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
                                    lazyListState = scrollStates[page]
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button Overlay
        AnimatedVisibility(
            visible = isDialerEnabled && isFabVisible,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            FloatingActionButton(
                onClick = onOpenDialer,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Dialpad, contentDescription = "Dialer")
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

    val pagerState = rememberPagerState(pageCount = { PersonTabFilter.entries.size })

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

    // Use pre-calculated person groups
    val allPersonGroupsMap = uiState.personGroups

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
        // Unified Header Unit
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Column {
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
                
                val scope = rememberCoroutineScope()

                Spacer(Modifier.height(4.dp))

                // 1. Pager -> ViewModel Sync (When user settles on a page)
                LaunchedEffect(pagerState.settledPage) {
                    val newFilter = PersonTabFilter.entries[pagerState.settledPage]
                    if (uiState.personTabFilter != newFilter) {
                        viewModel.setPersonTabFilter(newFilter)
                    }
                }
                
                // 2. ViewModel -> Pager Sync (When external filter changes)
                LaunchedEffect(uiState.personTabFilter) {
                    if (pagerState.targetPage != uiState.personTabFilter.ordinal && 
                        !pagerState.isScrollInProgress) {
                        pagerState.animateScrollToPage(uiState.personTabFilter.ordinal)
                    }
                }

                PersonTypeTabs(
                    selectedFilter = PersonTabFilter.entries[pagerState.currentPage],
                    onFilterSelected = { filter ->
                        scope.launch {
                            pagerState.animateScrollToPage(filter.ordinal)
                        }
                    },
                    counts = uiState.personTypeCounts
                )
            }
        }
        
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
                userScrollEnabled = true,
                beyondViewportPageCount = 1
            ) { page ->
                val tab = PersonTabFilter.entries[page]
                val tabPersons = uiState.tabFilteredPersons[tab]?.mapNotNull { allPersonGroupsMap[it.phoneNumber] } ?: emptyList()

                Box(Modifier.fillMaxSize()) {
                    if (uiState.isLoading) {
                        Column(Modifier.fillMaxSize()) {
                            repeat(8) {
                                PersonCardShimmer()
                            }
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
                    } else if (tabPersons.isEmpty()) {
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
                            persons = tabPersons,
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
