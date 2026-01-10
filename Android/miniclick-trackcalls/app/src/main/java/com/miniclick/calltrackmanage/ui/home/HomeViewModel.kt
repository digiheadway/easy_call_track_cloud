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
import com.miniclick.calltrackmanage.ui.home.viewmodel.*



class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val callDataRepository = CallDataRepository.getInstance(application)
    private val recordingRepository = RecordingRepository.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    
    // Performance: Cache for normalized numbers to avoid repeated heavy library calls
    private val normalizationCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val networkObserver = com.miniclick.calltrackmanage.util.network.NetworkConnectivityObserver(application)

    // Caching for expensive PersonGroup generation
    private var lastUsedCallLogs: List<CallDataEntity>? = null
    private var lastUsedPersons: List<PersonDataEntity>? = null
    private var lastUsedTrackStart: Long? = null
    private var lastUsedSimSelection: String? = null
    private var lastComputedGroups: Map<String, PersonGroup> = emptyMap()
    private var lastLogsForGroupsByPhone: Map<String, List<CallDataEntity>> = emptyMap()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadSettingsSync()
        viewModelScope.launch(Dispatchers.IO) {
            refreshSettings()
            loadRecordingPath()
            triggerFilter()
        }
        
        // Trigger immediate sync from system CallLog on app open
        syncFromSystem()
        
        // Observe Room DB for real-time updates
        viewModelScope.launch {
            callDataRepository.getAllCallsFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { calls ->
                    _uiState.update { it.copy(callLogs = calls) }
                    triggerFilter()
                }
        }
        
        viewModelScope.launch {
            // Use getAllPersonsIncludingExcludedFlow to include excluded persons for Ignored tab filtering
            callDataRepository.getAllPersonsIncludingExcludedFlow()
                .distinctUntilChanged()
                .conflate()
                .collect { persons ->
                    _uiState.update { it.copy(persons = persons) }
                    triggerFilter()
                }
        }
        

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
        
        
        // Observe Network Status
        viewModelScope.launch {
            networkObserver.observe().collect { isAvailable ->
                _uiState.update { it.copy(isNetworkAvailable = isAvailable) }
                // If network becomes available and we have pending syncs, trigger sync
                if (isAvailable && _uiState.value.pendingSyncCount > 0 && _uiState.value.isSyncSetup) {
                     syncNow()
                }
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

        // Add more flows here as needed...
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
        viewModelScope.launch {
            refreshSettings()
            _uiState.update { it.copy(isSyncing = true) }
            try {
                SyncManager.syncNow(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _uiState.update { it.copy(isSyncing = false) }
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

    private fun loadSettingsSync() {
        val searchVisible = settingsRepository.isSearchVisible()
        val filtersVisible = settingsRepository.isFiltersVisible()
        val searchQuery = settingsRepository.getSearchQuery()
        val labelFilter = settingsRepository.getLabelFilter()
        
        val typeFilter = try { 
            val saved = settingsRepository.getCallTypeFilter()
            when (saved) {
                "ATTENDED" -> CallTabFilter.ANSWERED
                "RESPONDED" -> CallTabFilter.OUTGOING
                "NOT_ATTENDED" -> CallTabFilter.NOT_ANSWERED
                "NOT_RESPONDED" -> CallTabFilter.OUTGOING_NOT_CONNECTED
                else -> CallTabFilter.valueOf(saved)
            }
        } catch(e: Exception) { CallTabFilter.ALL }
        
        val pTypeFilter = try { 
            val saved = settingsRepository.getPersonTabFilter()
            when (saved) {
                "ATTENDED" -> PersonTabFilter.ANSWERED
                "RESPONDED" -> PersonTabFilter.OUTGOING
                "NOT_ATTENDED" -> PersonTabFilter.NOT_ANSWERED
                "NOT_RESPONDED" -> PersonTabFilter.OUTGOING_NOT_CONNECTED
                else -> PersonTabFilter.valueOf(saved)
            }
        } catch(e: Exception) { PersonTabFilter.ALL }
        val connFilter = try { ConnectedFilter.valueOf(settingsRepository.getConnectedFilter()) } catch(e: Exception) { ConnectedFilter.ALL }
        val nFilter = try { NotesFilter.valueOf(settingsRepository.getNotesFilter()) } catch(e: Exception) { NotesFilter.ALL }
        val pnFilter = try { PersonNotesFilter.valueOf(settingsRepository.getPersonNotesFilter()) } catch(e: Exception) { PersonNotesFilter.ALL }
        val cFilter = try { ContactsFilter.valueOf(settingsRepository.getContactsFilter()) } catch(e: Exception) { ContactsFilter.ALL }
        val aFilter = try { AttendedFilter.valueOf(settingsRepository.getAttendedFilter()) } catch(e: Exception) { AttendedFilter.ALL }
        val rFilter = try { ReviewedFilter.valueOf(settingsRepository.getReviewedFilter()) } catch(e: Exception) { ReviewedFilter.ALL }
        val cnFilter = try { CustomNameFilter.valueOf(settingsRepository.getCustomNameFilter()) } catch(e: Exception) { CustomNameFilter.ALL }

        _uiState.update { it.copy(
            isSearchVisible = searchVisible,
            isFiltersVisible = filtersVisible,
            searchQuery = searchQuery,
            labelFilter = labelFilter,
            callTypeFilter = typeFilter,
            personTabFilter = pTypeFilter,
            connectedFilter = connFilter,
            notesFilter = nFilter,
            personNotesFilter = pnFilter,
            contactsFilter = cFilter,
            attendedFilter = aFilter,
            reviewedFilter = rFilter,
            customNameFilter = cnFilter,
            dateRange = try { DateRange.valueOf(settingsRepository.getDateRangeFilter()) } catch(e: Exception) { DateRange.LAST_7_DAYS },
            customStartDate = settingsRepository.getCustomStartDate().let { if (it == 0L) null else it },
            customEndDate = settingsRepository.getCustomEndDate().let { if (it == 0L) null else it },
            viewMode = try { ViewMode.valueOf(settingsRepository.getViewMode()) } catch(e: Exception) { ViewMode.PERSONS },
            
            // Critical setup flags for UI consistency
            simSelection = settingsRepository.getSimSelection(),
            trackStartDate = settingsRepository.getTrackStartDate(),
            isSyncSetup = settingsRepository.getOrganisationId().isNotEmpty(),
            whatsappPreference = settingsRepository.getWhatsappPreference(),
            callRecordEnabled = settingsRepository.isCallRecordEnabled(),
            callActionBehavior = settingsRepository.getCallActionBehavior(),
            reportCategory = try { ReportCategory.valueOf(settingsRepository.getReportCategory()) } catch(e: Exception) { ReportCategory.OVERVIEW },
            visibleCallFilters = loadVisibleCallFilters(),
            visiblePersonFilters = loadVisiblePersonFilters(),
            allowPersonalExclusion = settingsRepository.isAllowPersonalExclusion()
        ) }
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
        val newMode = if (_uiState.value.viewMode == ViewMode.CALLS) ViewMode.PERSONS else ViewMode.CALLS
        setViewMode(newMode)
    }
    
    fun setCallTypeFilter(filter: CallTabFilter) {
        _uiState.update { it.copy(callTypeFilter = filter) }
        settingsRepository.setCallTypeFilter(filter.name)
        // We only update the active pointer to keep the UI snappy during swiping.
        updateDateSummaries()
    }

    fun setPersonTabFilter(filter: PersonTabFilter) {
        _uiState.update { it.copy(personTabFilter = filter) }
        settingsRepository.setPersonTabFilter(filter.name)
        // Optimization: Do NOT call triggerFilter() here.
        // tabFilteredPersons already contains data for all tabs.
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
            // Debounce to prevent UI lag during rapid updates - 50ms is ideal for responsiveness
            kotlinx.coroutines.delay(50)
            
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
                // Recompute groups only when underlying data changes
                val logsForGroupsByPhone = currentState.callLogs
                    .filter { it.callDate >= trackStart && (targetSubId == null || it.subscriptionId == targetSubId) }
                    .groupBy { it.phoneNumber }
                
                val groups = logsForGroupsByPhone.mapValues { (number, calls) ->
                    val sortedCalls = calls.sortedByDescending { it.callDate }
                    val person = personsMap[number] ?: personsMap[getCachedNormalizedNumber(number)]
                    
                    // Optimized single-pass calculation
                    var totalDuration = 0L
                    var incoming = 0
                    var outgoing = 0
                    var missed = 0
                    
                    for (call in calls) {
                        totalDuration += call.duration
                        when (call.callType) {
                            android.provider.CallLog.Calls.INCOMING_TYPE -> incoming++
                            android.provider.CallLog.Calls.OUTGOING_TYPE -> outgoing++
                            android.provider.CallLog.Calls.MISSED_TYPE, 
                            android.provider.CallLog.Calls.REJECTED_TYPE, 5 -> missed++
                        }
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
            }

            val groups = lastComputedGroups
            val logsForGroupsByPhone = lastLogsForGroupsByPhone
            
            // 3. Perform Single-Pass Call Filtering Across ALL Tabs (This respects Date/Search/Labels for the list views)
            val tabLogs = CallLogManager.processCallFilters(
                currentState, 
                personsMap, 
                ::getCachedNormalizedNumber,
                targetSubId
            )
            
            // 4. Perform Single-Pass Person Filtering
            // We pass unfiltered logsForGroupsByPhone so person-level filters (like "with note") see all history
            // but tabPersons will still only include persons with activity in the selected DateRange
            val tabPersons = PersonLogManager.processPersonFilters(currentState, logsForGroupsByPhone)

            // 6. Calculate Report Statistics (These SHOULD still respect the date range)
            val visibleLogs = tabLogs[CallTabFilter.ALL] ?: emptyList()
            val reportStats = StatsManager.calculateReportStats(
                visibleLogs, 
                currentState.persons, 
                currentState.callLogs,
                settingsRepository
            )

            val activeFilterCount = HomeUtils.calculateFilterCount(currentState)
            
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
                isLoading = false
            ) }
            updateDateSummaries()
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
        
        // Detect active SIMs
        val sims = HomeUtils.detectActiveSims(getApplication())
        
        // Check for critical changes that require re-syncing
        val needsSync = simSelection != _uiState.value.simSelection || 
                        trackStartDate != _uiState.value.trackStartDate ||
                        sim1SubId != _uiState.value.sim1SubId ||
                        sim2SubId != _uiState.value.sim2SubId ||
                        callRecordEnabled != _uiState.value.callRecordEnabled ||
                        callerPhone1 != _uiState.value.callerPhoneSim1 ||
                        callerPhone2 != _uiState.value.callerPhoneSim2
        
        val callActionBehavior = settingsRepository.getCallActionBehavior()

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
            availableWhatsappApps = HomeUtils.fetchWhatsAppApps(getApplication()),
            allowPersonalExclusion = settingsRepository.isAllowPersonalExclusion()
        ) }

        if (needsSync) {
            syncFromSystem()
        } else {
            triggerFilter()
        }
    }


    private fun getSubIdForSim(simName: String): Int? = HomeUtils.getSubIdForSim(simName, _uiState.value)




}
