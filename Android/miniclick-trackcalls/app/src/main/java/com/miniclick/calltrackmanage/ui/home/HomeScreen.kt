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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.ui.common.*
import com.miniclick.calltrackmanage.ui.settings.SettingsViewModel
import com.miniclick.calltrackmanage.ui.settings.WhatsAppSelectionModal
import com.miniclick.calltrackmanage.ui.utils.AudioPlayer
import com.miniclick.calltrackmanage.ui.utils.getFileNameFromUri
import com.miniclick.calltrackmanage.ui.utils.getDateHeader
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
    isDialerEnabled: Boolean = true,
    showDialButton: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showFilterModal by remember { mutableStateOf(false) }
    var inspectedCallFilter by remember { mutableStateOf<CallTabFilter?>(null) }
    var inspectedPersonFilter by remember { mutableStateOf<PersonTabFilter?>(null) }
    var genericInfoTitle by remember { mutableStateOf<String?>(null) }
    var genericInfoDesc by remember { mutableStateOf<String?>(null) }
    var showDateJumpSheet by remember { mutableStateOf(false) }
    
    val visibleCallFilters = remember { CallTabFilter.entries.filter { it != CallTabFilter.IGNORED } }
    val visiblePersonFilters = remember { PersonTabFilter.entries.filter { it != PersonTabFilter.IGNORED } }

    val pagerState = rememberPagerState(
        initialPage = visibleCallFilters.indexOf(uiState.callTypeFilter).coerceAtLeast(0),
        pageCount = { visibleCallFilters.size }
    )

    val personPagerState = rememberPagerState(
        initialPage = visiblePersonFilters.indexOf(uiState.personTabFilter).coerceAtLeast(0),
        pageCount = { visiblePersonFilters.size }
    )
    
    // Independent scroll states for each tab
    val scrollStates = remember(visibleCallFilters.size) { 
        List(visibleCallFilters.size) { LazyListState() } 
    }
    val personScrollStates = remember(visiblePersonFilters.size) {
        List(visiblePersonFilters.size) { LazyListState() }
    }
    
    val isFabVisible by remember {
        derivedStateOf {
            if (uiState.viewMode == ViewMode.PERSONS) return@derivedStateOf true
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
            onCustomLookup = { settingsViewModel.showPhoneLookup(it) },
            customLookupEnabled = settingsState.customLookupEnabled
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

    if (uiState.showWhatsappSelectionDialog) {
        WhatsAppSelectionModal(
            currentSelection = uiState.whatsappPreference,
            availableApps = uiState.availableWhatsappApps,
            onSelect = { selection, setAsDefault -> 
                viewModel.selectWhatsappAndOpen(selection, setAsDefault)
            },
            onDismiss = { viewModel.dismissWhatsappSelection() },
            showSetDefault = true
        )
    }

    inspectedCallFilter?.let { filter ->
        val description = when (filter) {
            CallTabFilter.ALL -> "Shows all call records within the selected date range and filters."
            CallTabFilter.ANSWERED -> "Incoming calls that were answered and lasted for more than 3 seconds."
            CallTabFilter.NOT_ANSWERED -> "Incoming calls that were missed, rejected, or lasted 3 seconds or less."
            CallTabFilter.OUTGOING -> "Outgoing calls that were answered by the recipient and lasted more than 3 seconds."
            CallTabFilter.OUTGOING_NOT_CONNECTED -> "Outgoing calls that were not answered or lasted 3 seconds or less."
            CallTabFilter.NEVER_CONNECTED -> "People with multiple calls but still duration total zero."
            CallTabFilter.MAY_FAILED -> "Persons with last call less then 4 seconds or calls with less then 4 second duration."
            CallTabFilter.IGNORED -> "Calls from phone numbers you have manually excluded or marked to ignore."
        }
        TabInfoModal(
            tabName = when(filter) {
                CallTabFilter.NEVER_CONNECTED -> "never conected"
                CallTabFilter.MAY_FAILED -> "May failed calls"
                CallTabFilter.OUTGOING_NOT_CONNECTED -> "Outgoing Not Connected"
                else -> filter.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
            },
            description = description,
            onDismiss = { inspectedCallFilter = null }
        )
    }

    inspectedPersonFilter?.let { filter ->
        val description = when (filter) {
            PersonTabFilter.ALL -> "Shows all contacts who have had call activity within the selected period."
            PersonTabFilter.ANSWERED -> "Contacts whose last incoming call was answered and lasted more than 3 seconds."
            PersonTabFilter.OUTGOING -> "Contacts whose last outgoing call was answered and lasted more than 3 seconds."
            PersonTabFilter.NOT_ANSWERED -> "Contacts whose last incoming call was missed, rejected, or lasted 3 seconds or less."
            PersonTabFilter.OUTGOING_NOT_CONNECTED -> "Contacts whose last outgoing call was not answered or lasted 3 seconds or less."
            PersonTabFilter.NEVER_CONNECTED -> "People with multiple calls but still duration total zero."
            PersonTabFilter.MAY_FAILED -> "Persons with last call less then 4 seconds or calls with less then 4 second duration."
            PersonTabFilter.IGNORED -> "Contacts you have manually excluded from tracking or list display."
        }
        TabInfoModal(
            tabName = when(filter) {
                PersonTabFilter.NEVER_CONNECTED -> "never conected"
                PersonTabFilter.MAY_FAILED -> "May failed calls"
                PersonTabFilter.OUTGOING_NOT_CONNECTED -> "Outgoing Not Connected"
                else -> filter.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
            },
            description = description,
            onDismiss = { inspectedPersonFilter = null }
        )
    }

    genericInfoTitle?.let { title ->
        TabInfoModal(
            tabName = title,
            description = genericInfoDesc ?: "",
            onDismiss = { 
                genericInfoTitle = null
                genericInfoDesc = null
            }
        )
    }

    if (showDateJumpSheet) {
        val scope = rememberCoroutineScope()
        QuickDateJumpBottomSheet(
            dateSummaries = uiState.dateSummaries,
            currentDateRange = uiState.dateRange,
            onDateRangeSelect = { viewModel.setDateRange(it) },
            onDateClick = { summary ->
                showDateJumpSheet = false
                scope.launch {
                    val targetList: List<Any> = if (uiState.viewMode == ViewMode.CALLS) {
                         val filter = visibleCallFilters[pagerState.currentPage]
                         uiState.tabFilteredLogs[filter] ?: emptyList()
                    } else {
                         val filter = visiblePersonFilters[personPagerState.currentPage]
                         uiState.tabFilteredPersons[filter]?.mapNotNull { allPersonGroupsMap[it.phoneNumber] } ?: emptyList()
                    }

                    var index = 0
                    val grouped = if (uiState.viewMode == ViewMode.CALLS) {
                        (targetList as List<CallDataEntity>).groupBy { getDateHeader(it.callDate) }
                    } else {
                        (targetList as List<PersonGroup>).groupBy { getDateHeader(it.lastCallDate) }
                    }
                    
                    var found = false
                    for ((label, items) in grouped) {
                        if (label == summary.dateLabel) {
                            found = true
                            break
                        }
                        index += 1 + items.size
                    }
                    
                    if (found) {
                        val state = if (uiState.viewMode == ViewMode.CALLS) {
                            scrollStates.getOrNull(pagerState.currentPage)
                        } else {
                            personScrollStates.getOrNull(personPagerState.currentPage)
                        }
                        state?.animateScrollToItem(index)
                    }
                }
            },
            onDismiss = { showDateJumpSheet = false },
            viewMode = uiState.viewMode,
            onToggleViewMode = {
                viewModel.toggleViewMode()
                showDateJumpSheet = false
            }
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
                    // Header Row (Unified Transforming Design)
                    CallsHeader(
                        viewMode = uiState.viewMode,
                        onToggleViewMode = viewModel::toggleViewMode,
                        onSearchClick = viewModel::toggleSearchVisibility,
                        onFilterClick = { showFilterModal = true },
                        isSearchActive = uiState.isSearchVisible,
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                        isFilterActive = showFilterModal,
                        filterCount = uiState.activeFilterCount,
                        dateRange = uiState.dateRange,
                        onDateRangeChange = { range, start, end -> viewModel.setDateRange(range, start, end) },
                        currentSort = if (uiState.viewMode == ViewMode.CALLS) uiState.callSortBy.name else uiState.personSortBy.name,
                        sortDirection = if (uiState.viewMode == ViewMode.CALLS) uiState.callSortDirection else uiState.personSortDirection,
                        onSortSelect = { sortBy ->
                            if (uiState.viewMode == ViewMode.CALLS) viewModel.setCallSortBy(CallSortBy.valueOf(sortBy))
                            else viewModel.setPersonSortBy(PersonSortBy.valueOf(sortBy))
                        },
                        onToggleSortDirection = {
                            if (uiState.viewMode == ViewMode.CALLS) viewModel.toggleCallSortDirection()
                            else viewModel.togglePersonSortDirection()
                        },
                        onQuickFilter = { type ->
                            when(type) {
                                "NOT_REVIEWED" -> viewModel.setReviewedFilter(ReviewedFilter.NOT_REVIEWED)
                                "WITHOUT_PERSON_NOTE" -> viewModel.setPersonNotesFilter(PersonNotesFilter.WITHOUT_NOTE)
                                "NOT_IN_CONTACTS" -> viewModel.setContactsFilter(ContactsFilter.NOT_IN_CONTACTS)
                            }
                        },
                        onClearFilters = {
                            viewModel.setConnectedFilter(ConnectedFilter.ALL)
                            viewModel.setNotesFilter(NotesFilter.ALL)
                            viewModel.setPersonNotesFilter(PersonNotesFilter.ALL)
                            viewModel.setContactsFilter(ContactsFilter.ALL)
                            viewModel.setReviewedFilter(ReviewedFilter.ALL)
                            viewModel.setCustomNameFilter(CustomNameFilter.ALL)
                            viewModel.setMinCallCount(0)
                        },
                        onReorderTabs = {
                            // Placeholder for future reorder functionality
                        },
                        isReviewedFilterActive = uiState.reviewedFilter == ReviewedFilter.NOT_REVIEWED,
                        isPersonNotesFilterActive = uiState.personNotesFilter == PersonNotesFilter.WITHOUT_NOTE,
                        isContactsFilterActive = uiState.contactsFilter == ContactsFilter.NOT_IN_CONTACTS,
                        showIgnoredOnly = uiState.showIgnoredOnly,
                        onToggleIgnoredOnly = viewModel::toggleShowIgnoredOnly,
                        onTitleLongClick = {
                            genericInfoTitle = "Call Activity"
                            genericInfoDesc = if (uiState.viewMode == ViewMode.CALLS)
                                "This section shows each call separately as it happened. Great for precise chronological tracking."
                            else
                                "This section groups calls by phone number. Great for seeing the history and status of each contact at a glance."
                        },
                        onViewModeLongClick = {
                            genericInfoTitle = "Switch View Style"
                            genericInfoDesc = "Toggle between seeing individual call logs or grouping them by phone numbers to see contact summaries."
                        }
                    )
                    
                    val scope = rememberCoroutineScope()

                    if (uiState.viewMode == ViewMode.CALLS) {
                        // Show red strip when viewing ignored contacts
                        if (uiState.showIgnoredOnly) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Showing Hidden Contacts Data",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                    Spacer(Modifier.weight(1f))
                                    TextButton(
                                        onClick = { viewModel.toggleShowIgnoredOnly() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text(
                                            "Show Non-Hidden",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
                        }

                        // Normal pager sync
                        LaunchedEffect(pagerState.settledPage) {
                            if (pagerState.settledPage < visibleCallFilters.size) {
                                val newFilter = visibleCallFilters[pagerState.settledPage]
                                if (uiState.callTypeFilter != newFilter) {
                                    viewModel.setCallTypeFilter(newFilter)
                                }
                            }
                        }
                        
                        LaunchedEffect(uiState.callTypeFilter) {
                            val targetIndex = visibleCallFilters.indexOf(uiState.callTypeFilter)
                            if (targetIndex >= 0 && pagerState.targetPage != targetIndex && 
                                !pagerState.isScrollInProgress) {
                                pagerState.animateScrollToPage(targetIndex)
                            }
                        }
                        
                        CallTypeTabs(
                            selectedFilter = if (pagerState.currentPage < visibleCallFilters.size) visibleCallFilters[pagerState.currentPage] else CallTabFilter.ALL,
                            onFilterSelected = { filter ->
                                scope.launch {
                                    val index = visibleCallFilters.indexOf(filter)
                                    if (index >= 0) pagerState.animateScrollToPage(index)
                                }
                            },
                            onFilterLongClick = { inspectedCallFilter = it },
                            counts = uiState.callTypeCounts
                        )
                    } else {
                        // Show red strip when viewing ignored persons
                        if (uiState.showIgnoredOnly) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Showing Hidden Contacts Data",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                    Spacer(Modifier.weight(1f))
                                    TextButton(
                                        onClick = { viewModel.toggleShowIgnoredOnly() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text(
                                            "Show Non-Hidden",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
                        }

                        // Normal pager sync
                        LaunchedEffect(personPagerState.settledPage) {
                            if (personPagerState.settledPage < visiblePersonFilters.size) {
                                val newFilter = visiblePersonFilters[personPagerState.settledPage]
                                if (uiState.personTabFilter != newFilter) {
                                    viewModel.setPersonTabFilter(newFilter)
                                }
                            }
                        }
                        
                        LaunchedEffect(uiState.personTabFilter) {
                            val targetIndex = visiblePersonFilters.indexOf(uiState.personTabFilter)
                            if (targetIndex >= 0 && personPagerState.targetPage != targetIndex && 
                                !personPagerState.isScrollInProgress) {
                                personPagerState.animateScrollToPage(targetIndex)
                            }
                        }

                        PersonTypeTabs(
                            selectedFilter = if (personPagerState.currentPage < visiblePersonFilters.size) visiblePersonFilters[personPagerState.currentPage] else PersonTabFilter.ALL,
                            onFilterSelected = { filter ->
                                scope.launch {
                                    val index = visiblePersonFilters.indexOf(filter)
                                    if (index >= 0) personPagerState.animateScrollToPage(index)
                                }
                            },
                            onFilterLongClick = { inspectedPersonFilter = it },
                            counts = uiState.personTypeCounts
                        )
                    }
                }
            }
            
            syncStatusBar()
            
            // Search Input Row removed - integrated into CallsHeader
            
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

                if (uiState.viewMode == ViewMode.CALLS) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true,
                        beyondViewportPageCount = 1
                    ) { page ->
                        val tab = visibleCallFilters[page]
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
                                        lazyListState = scrollStates[page],
                                        showDialButton = showDialButton,
                                        onJumpClick = { showDateJumpSheet = true }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LaunchedEffect(Unit) {
                        if (uiState.persons.isEmpty()) {
                            viewModel.syncFromSystem()
                        }
                    }

                    HorizontalPager(
                        state = personPagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true,
                        beyondViewportPageCount = 1
                    ) { page ->
                        val tab = visiblePersonFilters[page]
                        val tabPersons = uiState.tabFilteredPersons[tab]?.mapNotNull { allPersonGroupsMap[it.phoneNumber] } ?: emptyList()

                        Box(Modifier.fillMaxSize()) {
                            if (uiState.isLoading) {
                                Column(Modifier.fillMaxSize()) {
                                    repeat(8) {
                                        PersonCardShimmer()
                                    }
                                }
                            } else if (uiState.simSelection == "Off") {
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
                                    onCustomLookup = { settingsViewModel.showPhoneLookup(it) },
                                    lazyListState = personScrollStates[page],
                                    onJumpClick = { showDateJumpSheet = true }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button Overlay
        AnimatedVisibility(
            visible = isDialerEnabled && showDialButton && isFabVisible,
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


