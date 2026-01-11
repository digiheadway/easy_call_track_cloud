package com.miniclick.calltrackmanage.ui.home
 
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.MetadataSyncStatus
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.data.SettingsRepository
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import com.miniclick.calltrackmanage.ui.home.viewmodel.*
import android.util.Log



@dagger.hilt.android.lifecycle.HiltViewModel
class HomeViewModel @javax.inject.Inject constructor(
    application: Application,
    private val settingsRepository: com.miniclick.calltrackmanage.data.SettingsRepository,
    private val callDataRepository: com.miniclick.calltrackmanage.data.CallDataRepository,
    private val recordingRepository: com.miniclick.calltrackmanage.data.RecordingRepository,
) : AndroidViewModel(application) {
    
    // Performance: Cache for normalized numbers to avoid repeated heavy library calls
    private val normalizationCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val networkObserver = com.miniclick.calltrackmanage.util.network.NetworkConnectivityObserver(application)

    private val filterMutex = kotlinx.coroutines.sync.Mutex()
    
    // Caching for expensive PersonGroup generation
    private var lastUsedCallLogs: List<CallDataEntity>? = null
    private var lastUsedPersons: List<PersonDataEntity>? = null
    private var lastUsedTrackStart: Long = 0L
    private var lastUsedSimSelection: String = ""
    private var lastComputedGroups: Map<String, PersonGroup> = emptyMap()
    private var lastLogsForGroupsByPhone: Map<String, List<CallDataEntity>> = emptyMap()
    
    private var isFirstLoadComplete = false
    private val viewModelStartTime = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        Log.d("HomeViewModel", "HomeViewModel init")
        _uiState.update { it.copy(isLoading = true) }
        
        // STARTUP OPTIMIZATION: Load minimal settings synchronously for immediate UI
        loadSettingsSync()
        
        // Phase 0 (Instant): Immediate DB Pre-seed read on IO thread
        // This ensures that for warm starts or even fast fresh starts, data is ready ASAP.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val minDate = settingsRepository.getTrackStartDate()
                val initialLogs = callDataRepository.getCallsSince(minDate)
                val initialPersons = callDataRepository.getAllPersonsIncludingExcluded(minDate)
                
                if (initialLogs.isNotEmpty() || initialPersons.isNotEmpty()) {
                    Log.d("HomeViewModel", "Pre-seed: Found ${initialLogs.size} logs, ${initialPersons.size} persons")
                    _uiState.update { it.copy(
                        callLogs = initialLogs, 
                        persons = initialPersons,
                        trackStartDate = minDate
                    ) }
                    triggerFilter()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Pre-seed failed", e)
            }
        }
        
        // STARTUP OPTIMIZATION: Start critical work IMMEDIATELY for fastest data display
        // Only defer truly heavy operations (sync, secondary observers)
        
        // Phase 1 (0ms): Start critical database observers IMMEDIATELY
        viewModelScope.launch {
            startCriticalObservers()
        }
        
        // Phase 2 (0ms): Load recording path in parallel on IO thread
        viewModelScope.launch(Dispatchers.IO) {
            loadRecordingPath()
        }
        
        // Phase 3 (100ms): Start secondary observers (lighter delay)  
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            startSecondaryObservers()
        }
        
        // Phase 4 (300ms): Sync from system (heavy operation - can wait a bit)
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            if (settingsRepository.isSetupGuideCompleted()) {
                syncFromSystem()
            }
        }
        
        // Final Loading Termination (500ms): Ensure shimmers stop even if DB is empty
        // Reduced from 3s to 500ms because local data should be instant.
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            if (_uiState.value.isLoading) {
                Log.d("HomeViewModel", "Loading timeout reached, forcing filter check")
                triggerFilter()
            }
        }

        // Network observer is lightweight, can start immediately
        viewModelScope.launch {
            networkObserver.observe().collect { isAvailable ->
                _uiState.update { it.copy(isNetworkAvailable = isAvailable) }
                if (isAvailable && _uiState.value.pendingSyncCount > 0 && _uiState.value.isSyncSetup) {
                     syncNow()
                }
            }
        }
    }
    
    /**
     * Critical observers needed for basic UI functionality.
     * PERFORMANCE: We now use flatMapLatest to only fetch calls/persons since the Track Start Date.
     * This drastically reduces memory usage and processing time for users with long histories.
     */
    private fun startCriticalObservers() {
        // Call logs - reactive to trackStartDate
        viewModelScope.launch {
            settingsRepository.getTrackStartDateFlow()
                .distinctUntilChanged()
                .flatMapLatest { minDate ->
                    Log.d("HomeViewModel", "Switching Call Flow: minDate=$minDate")
                    callDataRepository.getCallsSinceFlow(minDate)
                }
                .distinctUntilChanged()
                .conflate()
                .collect { calls ->
                    val start = System.currentTimeMillis()
                    _uiState.update { it.copy(callLogs = calls) }
                    triggerFilter()
                    
                    if (!isFirstLoadComplete && calls.isNotEmpty()) {
                        Log.i("HomeViewModel", "FIRST DATA LOAD: Collected ${calls.size} calls in ${System.currentTimeMillis() - start}ms. Startup to first data: ${System.currentTimeMillis() - viewModelStartTime}ms")
                    } else {
                        Log.d("HomeViewModel", "Collected ${calls.size} calls in ${System.currentTimeMillis() - start}ms")
                    }
                }
        }
        
        // Persons - reactive to trackStartDate
        viewModelScope.launch {
            settingsRepository.getTrackStartDateFlow()
                .distinctUntilChanged()
                .flatMapLatest { minDate ->
                   Log.d("HomeViewModel", "Switching Person Flow: minDate=$minDate")
                   callDataRepository.getAllPersonsIncludingExcludedFlow(minDate)
                }
                .distinctUntilChanged()
                .conflate()
                .collect { persons ->
                    val start = System.currentTimeMillis()
                    _uiState.update { it.copy(persons = persons) }
                    triggerFilter()
                    Log.d("HomeViewModel", "Collected ${persons.size} persons in ${System.currentTimeMillis() - start}ms")
                }
        }

        // CRITICAL FOR FRESH START: Observe setup guide completion and SIM IDs
        viewModelScope.launch {
            settingsRepository.getSetupGuideCompletedFlow()
                .distinctUntilChanged()
                .collect { completed ->
                    Log.d("HomeViewModel", "Setup Guide Completed Flow: $completed")
                    refreshSettings()
                    // syncFromSystem() is now handled inside refreshSettings() if needsSync is true.
                    // If refreshSettings didn't trigger it (e.g. no SIM change), we trigger it manually
                    // only if it's the specific moment the user finishes the guide.
                    if (completed && !_uiState.value.isSyncing) {
                        syncFromSystem()
                    }
                }
        }

        viewModelScope.launch {
            combine(
                settingsRepository.getSimSelectionFlow(),
                settingsRepository.getSim1SubscriptionIdFlow(),
                settingsRepository.getSim2SubscriptionIdFlow()
            ) { selection, sim1, sim2 ->
                Triple(selection, sim1, sim2)
            }
            .distinctUntilChanged()
            .collect { result ->
                val (selection, sim1, sim2) = Triple(result.first, result.second, result.third)
                Log.d("HomeViewModel", "SIM Settings Changed: sel=$selection, sim1=$sim1, sim2=$sim2")
                refreshSettings()
            }
        }
    }
    
    /**
     * Secondary observers for sync status, counts, etc.
     */
    private fun startSecondaryObservers() {
        // Observe Pending Syncs (Calls + Persons)
        viewModelScope.launch {
            callDataRepository.getPendingChangesCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { totalPending ->
                    _uiState.update { it.copy(pendingSyncCount = totalPending) }
                }
        }

        // Observe Metadata Sync Queue (Total)
        viewModelScope.launch {
            callDataRepository.getPendingMetadataSyncCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { count ->
                    _uiState.update { it.copy(pendingMetadataCount = count) }
                }
        }

        // Observe Granular Metadata Queues
        viewModelScope.launch {
            callDataRepository.getPendingNewCallsCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { count ->
                    _uiState.update { it.copy(pendingNewCallsCount = count) }
                }
        }
        viewModelScope.launch {
            callDataRepository.getPendingMetadataUpdatesCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { count ->
                    _uiState.update { it.copy(pendingMetadataUpdatesCount = count) }
                }
        }
        viewModelScope.launch {
            callDataRepository.getPendingPersonUpdatesCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { count ->
                    _uiState.update { it.copy(pendingPersonUpdatesCount = count) }
                }
        }

        // Observe Recording Sync Queue
        viewModelScope.launch {
            callDataRepository.getPendingRecordingSyncCountFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { count ->
                    _uiState.update { it.copy(pendingRecordingCount = count) }
                }
        }

        // Observe Active Recordings (Compressing/Uploading)
        viewModelScope.launch {
            callDataRepository.getActiveRecordingSyncsFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { active ->
                    _uiState.update { it.copy(activeRecordings = active) }
                }
        }

        // Reactive Settings Observers
        viewModelScope.launch {
            settingsRepository.getSimSelectionFlow()
                .distinctUntilChanged()
                .collect { refreshSettings() }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val apps = com.miniclick.calltrackmanage.util.system.WhatsAppUtils.fetchAvailableWhatsappApps(ctx)
            _uiState.update { it.copy(availableWhatsappApps = apps) }
        }

        viewModelScope.launch {
            settingsRepository.getTrackStartDateFlow()
                .distinctUntilChanged()
                .collect { refreshSettings() }
        }
    }

    private fun loadRecordingPath() {
        _uiState.update { it.copy(currentRecordingPath = recordingRepository.getRecordingPath()) }
    }

    fun updateRecordingPath(path: String) {
        recordingRepository.setCustomPath(path)
        loadRecordingPath()
        syncFromSystem() // Re-sync to find recordings in new path
    }

    /**
     * Load data directly from Room (instant - no loading spinner needed for this)
     */
    private fun loadDataFromRoom() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val calls = callDataRepository.getAllCalls()
                val persons = callDataRepository.getAllPersons()
                
                _uiState.update { 
                    it.copy(
                        callLogs = calls,
                        persons = persons,
                        isLoading = false
                    )
                }
                triggerFilter()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Sync new calls from system call log to Room (background operation)
     */
    fun syncFromSystem() {
        // Prevent overlapping sync requests to avoid infinite loops and JobInfo warnings
        if (_uiState.value.isSyncing) {
            Log.d("HomeViewModel", "syncFromSystem skipped: sync already in progress")
            return
        }
        
        viewModelScope.launch {
            Log.d("HomeViewModel", "syncFromSystem starting")
            _uiState.update { it.copy(isSyncing = true) }
            try {
                // Background operations handled by SyncManager (enqueues workers)
                SyncManager.syncNow(getApplication())
                // We add a small artificial delay to prevent rapid-fire rescheduling from UI loops
                kotlinx.coroutines.delay(2000)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Sync failed", e)
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
                Log.d("HomeViewModel", "syncFromSystem finished")
            }
        }
    }

    /**
     * Refresh data (called on pull-to-refresh)
     */
    fun refreshData() {
        syncFromSystem()
    }

    suspend fun getRecordingForLog(call: CallDataEntity): String? {
        if (!_uiState.value.callRecordEnabled) return null
        
        val lastEnabledTime = settingsRepository.getRecordingLastEnabledTimestamp()
        if (call.callDate < lastEnabledTime) return null

        // Check if already has recording path
        if (!call.localRecordingPath.isNullOrEmpty()) {
            return call.localRecordingPath
        }
        
        // Skip if no duration (missed call, etc.)
        if (call.duration <= 0) return null

        // Check cache
        val cachedPath = _uiState.value.recordings[call.compositeId]
        if (cachedPath != null) return cachedPath

        // Find in file system
        return withContext(Dispatchers.IO) {
            val path = recordingRepository.findRecording(call.callDate, call.duration, call.phoneNumber, call.contactName)
            if (path != null) {
                // Update cache
                _uiState.update { 
                    it.copy(recordings = it.recordings + (call.compositeId to path))
                }
                // Update Room
                callDataRepository.updateRecordingPath(call.compositeId, path)
            }
            path
        }
    }

    /**
     * Minimal settings needed for the very first frame to avoid shimmer flicker.
     * Heavy reads are moved to refreshSettings in Phase 2.
     */
    private fun loadSettingsSync() {
        val searchVisible = settingsRepository.isSearchVisible()
        val searchQuery = settingsRepository.getSearchQuery()
        val viewMode = try { ViewMode.valueOf(settingsRepository.getViewMode()) } catch(e: Exception) { ViewMode.PERSONS }
        val isSyncSetup = settingsRepository.getOrganisationId().isNotEmpty()
        val simSelection = settingsRepository.getSimSelection()
        val trackStartDate = settingsRepository.getTrackStartDate()
        
        // Always start with ALL filter - no need to persist tab selection
        val typeFilter = CallTabFilter.ALL
        val pTypeFilter = PersonTabFilter.ALL

        _uiState.update { it.copy(
            isSearchVisible = searchVisible,
            searchQuery = searchQuery,
            viewMode = viewMode,
            isSyncSetup = isSyncSetup,
            simSelection = simSelection,
            trackStartDate = trackStartDate,
            callTypeFilter = typeFilter,
            personTabFilter = pTypeFilter,
            visibleCallFilters = loadVisibleCallFilters(),
            visiblePersonFilters = loadVisiblePersonFilters(),
            isLoading = true 
        ) }
    }

    private suspend fun runFilteringAsync() = withContext(Dispatchers.Default) {
        triggerFilter()
    }

    private fun loadVisibleCallFilters(): List<CallTabFilter> {
        val saved = settingsRepository.getCallTabOrder()
        if (saved.isNullOrEmpty()) return CallTabFilter.entries.filter { it != CallTabFilter.IGNORED }
        return saved.split(",").mapNotNull { 
            try { CallTabFilter.valueOf(it) } catch(e: Exception) { null }
        }.filter { it != CallTabFilter.IGNORED }
    }

    private fun loadVisiblePersonFilters(): List<PersonTabFilter> {
        val saved = settingsRepository.getPersonTabOrder()
        if (saved.isNullOrEmpty()) return PersonTabFilter.entries.filter { it != PersonTabFilter.IGNORED }
        return saved.split(",").mapNotNull { 
            try { PersonTabFilter.valueOf(it) } catch(e: Exception) { null }
        }.filter { it != PersonTabFilter.IGNORED }
    }

    fun updateCallTabOrder(newOrder: List<CallTabFilter>) {
        _uiState.update { it.copy(visibleCallFilters = newOrder) }
        settingsRepository.setCallTabOrder(newOrder.joinToString(",") { it.name })
    }

    fun updatePersonTabOrder(newOrder: List<PersonTabFilter>) {
        _uiState.update { it.copy(visiblePersonFilters = newOrder) }
        settingsRepository.setPersonTabOrder(newOrder.joinToString(",") { it.name })
    }

    private fun loadPersistentState() {
        loadSettingsSync()
        triggerFilter()
    }

    // ============================================
    // SEARCH & FILTERS
    // ============================================
    
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        settingsRepository.setSearchQuery(query)
        triggerFilter()
    }

    fun submitSearch(query: String) {
        if (query.length >= 2) {
            settingsRepository.addSearchHistory(query)
            loadSearchHistory()
        }
    }

    fun loadSearchHistory() {
        _uiState.update { it.copy(searchHistory = settingsRepository.getSearchHistory()) }
    }

    fun clearSearchHistory() {
        settingsRepository.clearSearchHistory()
        loadSearchHistory()
    }

    fun onTabSelected(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }
    
    fun toggleSearchVisibility() {
        val newVisible = !_uiState.value.isSearchVisible
        _uiState.update { it.copy(isSearchVisible = newVisible) }
        settingsRepository.setSearchVisible(newVisible)
    }
    
    fun toggleFiltersVisibility() {
        val newVisible = !_uiState.value.isFiltersVisible
        _uiState.update { it.copy(isFiltersVisible = newVisible) }
        settingsRepository.setFiltersVisible(newVisible)
    }
    
    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        settingsRepository.setViewMode(mode.name)
        updateDateSummaries()
    }

    fun toggleViewMode() {
        val currentState = _uiState.value
        val oldMode = currentState.viewMode
        val newMode = if (oldMode == ViewMode.CALLS) ViewMode.PERSONS else ViewMode.CALLS
        
        // Synchronize filters by name to maintain context across view modes (in-memory only)
        if (newMode == ViewMode.PERSONS) {
            try {
                val matchingFilter = PersonTabFilter.valueOf(currentState.callTypeFilter.name)
                if (currentState.personTabFilter != matchingFilter) {
                    _uiState.update { it.copy(personTabFilter = matchingFilter) }
                }
            } catch (e: Exception) {
                // Fallback to ALL if no match
                _uiState.update { it.copy(personTabFilter = PersonTabFilter.ALL) }
            }
        } else {
            try {
                val matchingFilter = CallTabFilter.valueOf(currentState.personTabFilter.name)
                if (currentState.callTypeFilter != matchingFilter) {
                    _uiState.update { it.copy(callTypeFilter = matchingFilter) }
                }
            } catch (e: Exception) {
                // Fallback to ALL if no match
                _uiState.update { it.copy(callTypeFilter = CallTabFilter.ALL) }
            }
        }
        
        setViewMode(newMode)
    }
    
    fun setCallTypeFilter(filter: CallTabFilter) {
        _uiState.update { it.copy(callTypeFilter = filter) }
        // Don't persist tab selection - always start fresh
        updateDateSummaries()
    }

    fun setPersonTabFilter(filter: PersonTabFilter) {
        _uiState.update { it.copy(personTabFilter = filter) }
        // Don't persist tab selection - always start fresh
        updateDateSummaries()
    }
    
    fun setConnectedFilter(filter: ConnectedFilter) {
        _uiState.update { it.copy(connectedFilter = filter) }
        settingsRepository.setConnectedFilter(filter.name)
        triggerFilter()
    }
    
    fun setNotesFilter(filter: NotesFilter) {
        _uiState.update { it.copy(notesFilter = filter) }
        settingsRepository.setNotesFilter(filter.name)
        triggerFilter()
    }

    fun setPersonNotesFilter(filter: PersonNotesFilter) {
        _uiState.update { it.copy(personNotesFilter = filter) }
        settingsRepository.setPersonNotesFilter(filter.name)
        triggerFilter()
    }
    
    fun setContactsFilter(filter: ContactsFilter) {
        _uiState.update { it.copy(contactsFilter = filter) }
        settingsRepository.setContactsFilter(filter.name)
        triggerFilter()
    }
    
    fun setAttendedFilter(filter: AttendedFilter) {
        _uiState.update { it.copy(attendedFilter = filter) }
        settingsRepository.setAttendedFilter(filter.name)
        triggerFilter()
    }

    fun setReviewedFilter(filter: ReviewedFilter) {
        _uiState.update { it.copy(reviewedFilter = filter) }
        settingsRepository.setReviewedFilter(filter.name)
        triggerFilter()
    }

    fun setCustomNameFilter(filter: CustomNameFilter) {
        _uiState.update { it.copy(customNameFilter = filter) }
        settingsRepository.setCustomNameFilter(filter.name)
        triggerFilter()
    }

    fun setMinCallCount(count: Int) {
        _uiState.update { it.copy(minCallCount = count) }
        triggerFilter()
    }

    fun setLabelFilter(label: String) {
        _uiState.update { it.copy(labelFilter = label) }
        settingsRepository.setLabelFilter(label)
        triggerFilter()
    }

    fun setDateRange(range: DateRange, start: Long? = null, end: Long? = null) {
        _uiState.update { it.copy(dateRange = range, customStartDate = start, customEndDate = end) }
        settingsRepository.setDateRangeFilter(range.name)
        settingsRepository.setCustomStartDate(start ?: 0L)
        settingsRepository.setCustomEndDate(end ?: 0L)
        triggerFilter()
    }

    fun toggleShowComparisons() {
        _uiState.update { it.copy(showComparisons = !it.showComparisons) }
    }

    fun setReportCategory(category: ReportCategory) {
        _uiState.update { it.copy(reportCategory = category) }
        settingsRepository.setReportCategory(category.name)
    }

    fun setPersonSortBy(sortBy: PersonSortBy) {
        _uiState.update { it.copy(personSortBy = sortBy) }
        triggerFilter()
    }

    fun togglePersonSortDirection() {
        _uiState.update { 
            it.copy(personSortDirection = if (it.personSortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING) 
        }
        triggerFilter()
    }

    fun setCallSortBy(sortBy: CallSortBy) {
        _uiState.update { it.copy(callSortBy = sortBy) }
        triggerFilter()
    }

    fun toggleCallSortDirection() {
        _uiState.update { 
            it.copy(callSortDirection = if (it.callSortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING) 
        }
        triggerFilter()
    }

    fun toggleShowIgnoredOnly() {
        _uiState.update { 
            it.copy(showIgnoredOnly = !it.showIgnoredOnly) 
        }
        triggerFilter()
    }

    private var filterJob: kotlinx.coroutines.Job? = null
    private fun triggerFilter() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            // Debounce to prevent UI lag - 8ms (half a frame) for extreme responsiveness
            kotlinx.coroutines.delay(8)
            
            // Ensure only one filter cycle runs the heavy logic at a time to prevent ANRs
            filterMutex.withLock {
                if (!isActive) return@withLock
                
                val filterStart = System.currentTimeMillis()
                val currentState = _uiState.value
            
            // 1. Prepare shared lookup data
            val personsMap = currentState.persons.associateBy { it.phoneNumber }
            
            // 2. Efficiently determine if we need to recompute the heavy PersonGroup objects
            val trackStart = currentState.trackStartDate
            val simSel = currentState.simSelection
            val targetSubId = if (simSel == "Both") null else getSubIdForSim(simSel)
            
            val dataChanged = currentState.callLogs !== lastUsedCallLogs || 
                              currentState.persons !== lastUsedPersons ||
                              trackStart != lastUsedTrackStart ||
                              simSel != lastUsedSimSelection

            if (dataChanged) {
                val groupStart = System.currentTimeMillis()
                // Recompute groups only when underlying data changes
                // Optimization: Use the fact that DB is already filtered by trackStart to avoid redundant checks
                val logsForGroupsByPhone = currentState.callLogs
                    .filter { targetSubId == null || it.subscriptionId == targetSubId }
                    .groupBy { it.phoneNumber }
                
                val groups = logsForGroupsByPhone.mapValues { (number, calls) ->
                    val sortedCalls = calls.sortedByDescending { it.callDate }
                    val person = personsMap[number] ?: personsMap[getCachedNormalizedNumber(number)]
                    
                    // PERFORMANCE OPTIMIZATION: 
                    // Use pre-computed stats from PersonDataEntity if available and no SIM filter is active.
                    // If targetSubId is null (Both sims), we can skip the heavy loop.
                    
                    val totalDuration: Long
                    val incoming: Int
                    val outgoing: Int
                    val missed: Int
                    
                    if (person != null && targetSubId == null) {
                        totalDuration = person.totalDuration
                        incoming = person.totalIncoming
                        outgoing = person.totalOutgoing
                        missed = person.totalMissed
                    } else {
                        // Manual fall-back (necessary for single-SIM filtering)
                        var d = 0L
                        var i = 0
                        var o = 0
                        var m = 0
                        for (call in calls) {
                            d += call.duration
                            when (call.callType) {
                                android.provider.CallLog.Calls.INCOMING_TYPE -> i++
                                android.provider.CallLog.Calls.OUTGOING_TYPE -> o++
                                android.provider.CallLog.Calls.MISSED_TYPE, 
                                android.provider.CallLog.Calls.REJECTED_TYPE, 5 -> m++
                            }
                        }
                        totalDuration = d
                        incoming = i
                        outgoing = o
                        missed = m
                    }

                    PersonGroup(
                        number = number,
                        name = person?.contactName?.takeIf { it.isNotBlank() } ?: calls.firstOrNull { !it.contactName.isNullOrBlank() }?.contactName,
                        photoUri = person?.photoUri ?: calls.firstOrNull { !it.photoUri.isNullOrEmpty() }?.photoUri,
                        calls = sortedCalls,
                        lastCallDate = sortedCalls.firstOrNull()?.callDate ?: 0L,
                        totalDuration = totalDuration,
                        incomingCount = incoming,
                        outgoingCount = outgoing,
                        missedCount = missed,
                        personNote = person?.personNote,
                        label = person?.label
                    )
                }
                
                // Update cache
                lastUsedCallLogs = currentState.callLogs
                lastUsedPersons = currentState.persons
                lastUsedTrackStart = trackStart
                lastUsedSimSelection = simSel
                lastComputedGroups = groups
                lastLogsForGroupsByPhone = logsForGroupsByPhone
                Log.d("HomeViewModel", "Computed ${groups.size} groups in ${System.currentTimeMillis() - groupStart}ms")
            }

            val groups = lastComputedGroups
            val logsForGroupsByPhone = lastLogsForGroupsByPhone
            
            // 3. Perform Single-Pass Call Filtering Across ALL Tabs
            val filterPhaseStart = System.currentTimeMillis()
            val tabLogs = CallLogManager.processCallFilters(
                currentState, 
                personsMap, 
                ::getCachedNormalizedNumber,
                targetSubId
            )
            
            // 4. Perform Single-Pass Person Filtering
            val tabPersons = PersonLogManager.processPersonFilters(currentState, logsForGroupsByPhone)

            // 6. Calculate Report Statistics
            val visibleLogs = tabLogs[CallTabFilter.ALL] ?: emptyList()
            val reportStats = StatsManager.calculateReportStats(
                visibleLogs, 
                currentState.persons, 
                currentState.callLogs,
                settingsRepository
            )

            val activeFilterCount = HomeUtils.calculateFilterCount(currentState)
            
            val hasData = tabPersons.values.any { it.isNotEmpty() } || tabLogs.values.any { it.isNotEmpty() }
            val timeSinceStart = System.currentTimeMillis() - viewModelStartTime
            val dbHasData = currentState.callLogs.isNotEmpty() || currentState.persons.isNotEmpty()
            
            // FAST STARTUP: If we have ANY data in the DB, stop shimmer immediately.
            // Don't wait for complex filtering to be "not empty" across all tabs.
            val stillLoading = when {
                hasData -> false 
                dbHasData -> false // If DB has raw data, don't show shimmers
                isFirstLoadComplete -> false 
                // Fresh start protection: If we are literally at zero data and actively syncing,
                // hold the shimmer for up to 5 seconds to give the initial import a chance.
                currentState.isSyncing && !dbHasData && timeSinceStart < 5000 -> true
                timeSinceStart < 500 -> true  // Regular minimum shimmer duration
                else -> false 
            }
            
            if (hasData || dbHasData) isFirstLoadComplete = true

            _uiState.update { it.copy(
                tabFilteredLogs = tabLogs,
                tabFilteredPersons = tabPersons,
                filteredLogs = tabLogs[currentState.callTypeFilter] ?: emptyList(),
                filteredPersons = tabPersons[currentState.personTabFilter] ?: emptyList(),
                activeFilterCount = activeFilterCount,
                callTypeCounts = tabLogs.mapValues { it.value.size },
                personTypeCounts = tabPersons.mapValues { it.value.size },
                personGroups = groups,
                reportStats = reportStats,
                isLoading = stillLoading
            ) }
            updateDateSummaries()
            Log.d("HomeViewModel", "Filter cycle completed in ${System.currentTimeMillis() - filterStart}ms (Logic: ${System.currentTimeMillis() - filterPhaseStart}ms)")
        }
    }
    }

    private fun updateDateSummaries() {
        val summaries = HomeUtils.computeDateSummaries(_uiState.value)
        _uiState.update { it.copy(dateSummaries = summaries) }
    }





    private fun getCachedNormalizedNumber(number: String): String {
        return normalizationCache.getOrPut(number) {
            normalizePhoneNumber(number)
        }
    }

    fun normalizePhoneNumber(number: String) = callDataRepository.normalizePhoneNumber(number)





    // ============================================
    // CALL FLOW (MULTI-SIM)
    // ============================================

    /**
     * Start the call process for a number.
     * Decides whether to show the SIM picker or dial immediately.
     */
    fun initiateCall(number: String) {
        CallFlowManager.initiateCall(
            number = number,
            availableSims = _uiState.value.availableSims,
            callActionBehavior = _uiState.value.callActionBehavior,
            application = getApplication(),
            onShowSimPicker = { num ->
                _uiState.update { it.copy(callFlowNumber = num, showCallSimPicker = true) }
            }
        )
    }

    /**
     * Manually re-trigger recording attachment for a specific number.
     * This forces a re-scan using the new scoring logic.
     */
    fun reAttachRecordingsForPhone(phoneNumber: String) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            withContext(Dispatchers.Main) { 
                android.widget.Toast.makeText(app, "Scanning recordings for $phoneNumber...", android.widget.Toast.LENGTH_SHORT).show() 
            }
            
            val updatedCount = RecordingManager.reAttachRecordingsForPhone(
                phoneNumber = phoneNumber,
                callDataRepository = callDataRepository,
                recordingRepository = recordingRepository,
                normalizePhoneNumber = ::normalizePhoneNumber
            )
            
            withContext(Dispatchers.Main) {
                triggerFilter()
                android.widget.Toast.makeText(app, "Updated $updatedCount recordings for $phoneNumber", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Re-attach recordings for ALL calls. Heavy operation.
     */
    fun reAttachAllRecordings() {
         viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updatedCount = RecordingManager.reAttachAllRecordings(callDataRepository, recordingRepository)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoading = false) }
                 triggerFilter()
                 android.widget.Toast.makeText(getApplication(), "Refreshed recordings. Updated $updatedCount calls.", android.widget.Toast.LENGTH_LONG).show()
            }
         }
    }


    /**
     * Execute the call with a specifically selected SIM.
     */
    fun executeCall(subId: Int?) {
        val number = _uiState.value.callFlowNumber ?: return
        val ctx = getApplication<Application>()
        val forceDialer = _uiState.value.callActionBehavior == "Open in Dialpad"
        
        com.miniclick.calltrackmanage.util.call.CallUtils.makeCall(ctx, number, subId, forceDialer)
        cancelCallFlow()
    }

    /**
     * Dismiss the SIM picker without calling.
     */
    fun cancelCallFlow() {
        _uiState.update { it.copy(
            showCallSimPicker = false,
            callFlowNumber = null
        )}
    }


    // ============================================
    // NOTES
    // ============================================
    
    fun saveCallNote(compositeId: String, note: String) {
        viewModelScope.launch {
            ContactManager.saveCallNote(compositeId, note, callDataRepository)
            quickSync()
        }
    }

    fun savePersonNote(phoneNumber: String, note: String) {
        viewModelScope.launch {
            ContactManager.savePersonNote(phoneNumber, note, callDataRepository)
            quickSync()
        }
    }
    
    fun savePersonLabel(phoneNumber: String, label: String) {
        viewModelScope.launch {
            ContactManager.savePersonLabel(phoneNumber, label, callDataRepository)
            quickSync()
        }
    }

    fun savePersonName(phoneNumber: String, name: String) {
        viewModelScope.launch {
            ContactManager.savePersonName(phoneNumber, name, callDataRepository)
            quickSync()
        }
    }
    
    fun updateReviewed(compositeId: String, reviewed: Boolean) {
        viewModelScope.launch {
            ContactManager.updateReviewed(compositeId, reviewed, callDataRepository)
            quickSync()
        }
    }

    fun markAllCallsReviewed(phoneNumber: String) {
        viewModelScope.launch {
            ContactManager.markAllCallsReviewed(phoneNumber, callDataRepository)
            quickSync()
        }
    }

    // ============================================
    // SYNC STATUS
    // ============================================
    
    fun updateCallStatus(compositeId: String, status: MetadataSyncStatus) {
        viewModelScope.launch {
            ContactManager.updateCallStatus(compositeId, status, callDataRepository)
        }
    }

    fun updateRecordingPathForLog(compositeId: String, path: String?) {
        viewModelScope.launch {
            callDataRepository.updateRecordingPath(compositeId, path)
            SyncManager.quickSync(getApplication())
        }
    }

    fun syncNow() = SyncManager.syncNow(getApplication())

    fun quickSync() = SyncManager.quickSync(getApplication())

    fun fullSync() {
        viewModelScope.launch {
            callDataRepository.syncFromSystemCallLog()
            SyncManager.syncNow(getApplication())
        }
    }

    // ============================================
    // EXCLUSION
    // ============================================

    fun ignoreNumber(phoneNumber: String) {
        viewModelScope.launch {
            ContactManager.updateExclusion(phoneNumber, excludeFromSync = false, excludeFromList = true, callDataRepository)
        }
    }

    fun noTrackNumber(phoneNumber: String) {
        viewModelScope.launch {
            ContactManager.updateExclusion(phoneNumber, excludeFromSync = true, excludeFromList = true, callDataRepository)
        }
    }

    fun onWhatsAppClick(number: String) {
        CallFlowManager.handleWhatsAppClick(
            number = number,
            whatsappPreference = _uiState.value.whatsappPreference,
            availableWhatsappApps = _uiState.value.availableWhatsappApps,
            application = getApplication(),
            onShowSelectionDialog = { num, apps ->
                _uiState.update { it.copy(whatsappTargetNumber = num, availableWhatsappApps = apps, showWhatsappSelectionDialog = true) }
            }
        )
    }

    fun selectWhatsappAndOpen(packageName: String, setAsDefault: Boolean) {
        val number = _uiState.value.whatsappTargetNumber ?: return
        if (setAsDefault) {
            settingsRepository.setWhatsappPreference(packageName)
            _uiState.update { it.copy(whatsappPreference = packageName) }
        }
        
        com.miniclick.calltrackmanage.util.system.WhatsAppUtils.openWhatsApp(
            getApplication(),
            number,
            packageName
        )
        dismissWhatsappSelection()
    }

    fun dismissWhatsappSelection() {
        _uiState.update { it.copy(
            showWhatsappSelectionDialog = false,
            whatsappTargetNumber = null
        )}
    }

    fun refreshSettings() {
        val simSelection = settingsRepository.getSimSelection()
        val trackStartDate = settingsRepository.getTrackStartDate()
        val whatsappPreference = settingsRepository.getWhatsappPreference()
        val isSyncSetup = settingsRepository.getOrganisationId().isNotEmpty()
        val callRecordEnabled = settingsRepository.isCallRecordEnabled()
        val sim1SubId = settingsRepository.getSim1SubscriptionId().let { if (it == -1) null else it }
        val sim2SubId = settingsRepository.getSim2SubscriptionId().let { if (it == -1) null else it }
        val callerPhone1 = settingsRepository.getCallerPhoneSim1()
        val callerPhone2 = settingsRepository.getCallerPhoneSim2()
        
        // Detect active SIMs - Optimization: Use cached value if possible to avoid main thread block
        // We will trigger a background refresh of this soon
        val sims = _uiState.value.availableSims.ifEmpty { 
             // On first run, we might have to do it, but let's try to keep it fast
             HomeUtils.detectActiveSims(getApplication())
        }
        
        // Check for critical changes that require re-syncing
        val needsSync = simSelection != _uiState.value.simSelection || 
                        trackStartDate != _uiState.value.trackStartDate ||
                        sim1SubId != _uiState.value.sim1SubId ||
                        sim2SubId != _uiState.value.sim2SubId ||
                        callRecordEnabled != _uiState.value.callRecordEnabled ||
                        callerPhone1 != _uiState.value.callerPhoneSim1 ||
                        callerPhone2 != _uiState.value.callerPhoneSim2
        
        val callActionBehavior = settingsRepository.getCallActionBehavior()

    // Defer loading of heavy filter settings to here (Background IO)
    val filtersVisible = settingsRepository.isFiltersVisible()
    val labelFilter = settingsRepository.getLabelFilter()
    
    // NOTE: Don't load callTypeFilter/personTabFilter here anymore
    // They are transient (in-memory only) and should not be persisted or restored

    // Load other filters
    val connFilter = try { ConnectedFilter.valueOf(settingsRepository.getConnectedFilter()) } catch(e: Exception) { ConnectedFilter.ALL }
    val nFilter = try { NotesFilter.valueOf(settingsRepository.getNotesFilter()) } catch(e: Exception) { NotesFilter.ALL }
    val pnFilter = try { PersonNotesFilter.valueOf(settingsRepository.getPersonNotesFilter()) } catch(e: Exception) { PersonNotesFilter.ALL }
    val cFilter = try { ContactsFilter.valueOf(settingsRepository.getContactsFilter()) } catch(e: Exception) { ContactsFilter.ALL }
    val aFilter = try { AttendedFilter.valueOf(settingsRepository.getAttendedFilter()) } catch(e: Exception) { AttendedFilter.ALL }
    val rFilter = try { ReviewedFilter.valueOf(settingsRepository.getReviewedFilter()) } catch(e: Exception) { ReviewedFilter.ALL }
    val cnFilter = try { CustomNameFilter.valueOf(settingsRepository.getCustomNameFilter()) } catch(e: Exception) { CustomNameFilter.ALL }
    
    val dateRange = try { DateRange.valueOf(settingsRepository.getDateRangeFilter()) } catch(e: Exception) { DateRange.LAST_7_DAYS }
    val customStartDate = settingsRepository.getCustomStartDate().let { if (it == 0L) null else it }
    val customEndDate = settingsRepository.getCustomEndDate().let { if (it == 0L) null else it }
    val reportCategory = try { ReportCategory.valueOf(settingsRepository.getReportCategory()) } catch(e: Exception) { ReportCategory.OVERVIEW }
    
    val vCallFilters = loadVisibleCallFilters()
    val vPersonFilters = loadVisiblePersonFilters()

    val currentState = _uiState.value
    val newComputedVisibleCallFilters = vCallFilters
    val newComputedVisiblePersonFilters = vPersonFilters

    // Optimization: Only update UI state if something meaningful changed
    // Note: Tab filter comparison removed since they're transient now
    val hasMeaningfulChanges = 
            currentState.visibleCallFilters != newComputedVisibleCallFilters ||
            currentState.visiblePersonFilters != newComputedVisiblePersonFilters ||
            currentState.simSelection != simSelection ||
            currentState.trackStartDate != trackStartDate ||
            currentState.isFiltersVisible != filtersVisible ||
            currentState.labelFilter != labelFilter ||
            currentState.dateRange != dateRange

        if (hasMeaningfulChanges) {
            _uiState.update { it.copy(
                simSelection = simSelection, 
                trackStartDate = trackStartDate,
                whatsappPreference = whatsappPreference,
                isSyncSetup = isSyncSetup,
                callRecordEnabled = callRecordEnabled,
                sim1SubId = sim1SubId,
                sim2SubId = sim2SubId,
                callerPhoneSim1 = callerPhone1,
                callerPhoneSim2 = callerPhone2,
                availableSims = sims,
                callActionBehavior = callActionBehavior,
                // availableWhatsappApps = HomeUtils.fetchWhatsAppApps(getApplication()), // MOVED TO BACKGROUND
                allowPersonalExclusion = settingsRepository.isAllowPersonalExclusion(),
                
                // Extended Settings loaded in background
                isFiltersVisible = filtersVisible,
                labelFilter = labelFilter,
                // Note: callTypeFilter and personTabFilter are NOT updated here - they stay in-memory only
                connectedFilter = connFilter,
                notesFilter = nFilter,
                personNotesFilter = pnFilter,
                contactsFilter = cFilter,
                attendedFilter = aFilter,
                reviewedFilter = rFilter,
                customNameFilter = cnFilter,
                dateRange = dateRange,
                customStartDate = customStartDate,
                customEndDate = customEndDate,
                reportCategory = reportCategory,
                visibleCallFilters = newComputedVisibleCallFilters,
                visiblePersonFilters = newComputedVisiblePersonFilters
            ) }

            // Only trigger sync if settings actually changed and we aren't already syncing
            if (needsSync) {
                Log.i("HomeViewModel", "Settings change detected, triggering syncFromSystem")
                syncFromSystem()
            } else {
                triggerFilter()
            }
        } else {
            // No settings changed, just ensure filters are up to date if called manually
            triggerFilter()
        }
        
        // BACKGROUND REFRESH: Heavy system info like WhatsApp apps and SIMs should be done on IO
        refreshHeavySystemInfo()
    }

    private var heavyInfoJob: kotlinx.coroutines.Job? = null
    private fun refreshHeavySystemInfo() {
        heavyInfoJob?.cancel()
        heavyInfoJob = viewModelScope.launch(Dispatchers.IO) {
            // Wait a bit to let the UI settle
            delay(100)
            
            val sims = HomeUtils.detectActiveSims(getApplication())
            val whatsappApps = HomeUtils.fetchWhatsAppApps(getApplication())
            
            _uiState.update { it.copy(
                availableSims = sims,
                availableWhatsappApps = whatsappApps
            )}
        }
    }


    private fun getSubIdForSim(simName: String): Int? = HomeUtils.getSubIdForSim(simName, _uiState.value)




}
