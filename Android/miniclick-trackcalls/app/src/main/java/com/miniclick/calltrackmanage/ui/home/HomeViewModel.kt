package com.miniclick.calltrackmanage.ui.home

import android.app.Application
import android.provider.CallLog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.MetadataSyncStatus
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.data.SettingsRepository
import android.telephony.SubscriptionManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import java.util.Calendar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.miniclick.calltrackmanage.ui.settings.SimInfo

// Filter enums for chip dropdowns
enum class CallTabFilter { ALL, ATTENDED, NOT_ATTENDED, RESPONDED, NOT_RESPONDED, IGNORED }
enum class PersonTabFilter { ALL, ATTENDED, RESPONDED, NOT_ATTENDED, NOT_RESPONDED, IGNORED }
enum class ConnectedFilter { ALL, CONNECTED, NOT_CONNECTED }
enum class NotesFilter { ALL, WITH_NOTE, WITHOUT_NOTE }
enum class PersonNotesFilter { ALL, WITH_NOTE, WITHOUT_NOTE }
enum class ContactsFilter { ALL, IN_CONTACTS, NOT_IN_CONTACTS }
enum class AttendedFilter { ALL, ATTENDED, NEVER_ATTENDED }
enum class ReviewedFilter { ALL, REVIEWED, NOT_REVIEWED }
enum class CustomNameFilter { ALL, WITH_NAME, WITHOUT_NAME }
enum class ViewMode { CALLS, PERSONS }
enum class DateRange { TODAY, LAST_3_DAYS, LAST_7_DAYS, LAST_14_DAYS, LAST_30_DAYS, THIS_MONTH, PREVIOUS_MONTH, CUSTOM, ALL }
enum class PersonSortBy { LAST_CALL, MOST_CALLS, NAME }
enum class CallSortBy { DATE, DURATION, NUMBER }
enum class SortDirection { ASCENDING, DESCENDING }

data class HomeUiState(
    val callLogs: List<CallDataEntity> = emptyList(),
    val filteredLogs: List<CallDataEntity> = emptyList(),
    val persons: List<PersonDataEntity> = emptyList(),
    val filteredPersons: List<PersonDataEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val searchQuery: String = "",
    val selectedTab: Int = 0,
    val recordings: Map<String, String> = emptyMap(), // compositeId -> Path cache
    val currentRecordingPath: String = "",
    
    // View state
    val viewMode: ViewMode = ViewMode.CALLS,
    val isSearchVisible: Boolean = false,
    val isFiltersVisible: Boolean = false,
    
    // Filter states
    val callTypeFilter: CallTabFilter = CallTabFilter.ALL,
    val personTabFilter: PersonTabFilter = PersonTabFilter.ALL,
    val connectedFilter: ConnectedFilter = ConnectedFilter.ALL,
    val notesFilter: NotesFilter = NotesFilter.ALL,
    val personNotesFilter: PersonNotesFilter = PersonNotesFilter.ALL,
    val contactsFilter: ContactsFilter = ContactsFilter.ALL,
    val minCallCount: Int = 0,
    val attendedFilter: AttendedFilter = AttendedFilter.ALL,
    val reviewedFilter: ReviewedFilter = ReviewedFilter.ALL,
    val customNameFilter: CustomNameFilter = CustomNameFilter.ALL,
    val labelFilter: String = "",

    val dateRange: DateRange = DateRange.LAST_7_DAYS,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,

    val simSelection: String = "Both",
    val trackStartDate: Long = 0,
    val whatsappPreference: String = "Always Ask",
    
    // Sync Status
    val pendingSyncCount: Int = 0,
    val pendingMetadataCount: Int = 0,
    val pendingRecordingCount: Int = 0,
    val pendingNewCallsCount: Int = 0,
    val pendingMetadataUpdatesCount: Int = 0,
    val pendingPersonUpdatesCount: Int = 0,
    val isNetworkAvailable: Boolean = true,
    val isSyncSetup: Boolean = false,
    val personSortBy: PersonSortBy = PersonSortBy.LAST_CALL,
    val personSortDirection: SortDirection = SortDirection.DESCENDING,
    val callSortBy: CallSortBy = CallSortBy.DATE,
    val callSortDirection: SortDirection = SortDirection.DESCENDING,
    val allowPersonalExclusion: Boolean = false,
    val callRecordEnabled: Boolean = true,
    val activeRecordings: List<CallDataEntity> = emptyList(),
    val sim1SubId: Int? = null,
    val sim2SubId: Int? = null,
    val callerPhoneSim1: String = "",
    val callerPhoneSim2: String = "",
    val activeFilterCount: Int = 0,
    val callTypeCounts: Map<CallTabFilter, Int> = emptyMap(),
    val personTypeCounts: Map<PersonTabFilter, Int> = emptyMap(),
    
    // Performance: Pre-computed filtered lists for each tab
    val tabFilteredLogs: Map<CallTabFilter, List<CallDataEntity>> = emptyMap(),
    val tabFilteredPersons: Map<PersonTabFilter, List<PersonDataEntity>> = emptyMap(),
    val personGroups: Map<String, PersonGroup> = emptyMap(),

    // Call Flow (SIM Picker)
    val showCallSimPicker: Boolean = false,
    val callFlowNumber: String? = null,
    val availableSims: List<SimInfo> = emptyList(),

    // Reports Stats
    val reportStats: ReportStats = ReportStats(),

    // Search History
    val searchHistory: List<String> = emptyList(),
    
    // Dialer Settings
    val showDialButton: Boolean = true,
    val callActionBehavior: String = "Direct"
)

// Extension to determine if a call status counts as "Attended" or "Responded" based on the new > 3s rule
fun CallDataEntity.isStatusConnected(): Boolean = duration > 3

data class ReportStats(
    val totalCalls: Int = 0,
    val incomingCalls: Int = 0,
    val outgoingCalls: Int = 0,
    val missedCalls: Int = 0,
    val rejectedCalls: Int = 0,
    val connectedCalls: Int = 0,
    val notConnectedCalls: Int = 0,
    val connectedIncoming: Int = 0,
    val connectedOutgoing: Int = 0,
    val totalDuration: Long = 0,
    val avgDuration: Long = 0,
    val maxDuration: Long = 0,
    val incomingDuration: Long = 0,
    val outgoingDuration: Long = 0,
    val uniqueContacts: Int = 0,
    val savedContacts: Int = 0,
    val unsavedContacts: Int = 0,
    val callsWithNotes: Int = 0,
    val reviewedCalls: Int = 0,
    val callsWithRecordings: Int = 0,
    val personsWithNotes: Int = 0,
    val personsWithLabels: Int = 0,
    val shortCalls: Int = 0, // Calls with duration < threshold (low engagement)
    val topCallers: List<TopCaller> = emptyList(),
    val mostTalked: List<TopCaller> = emptyList(),
    val dailyStats: Map<String, DayStat> = emptyMap(),
    val hourlyStats: Map<Int, Int> = emptyMap(),
    val labelDistribution: List<Pair<String, Int>> = emptyList()
)

data class TopCaller(
    val phoneNumber: String,
    val displayName: String,
    val callCount: Int,
    val totalDuration: Long,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int
)

data class DayStat(
    val count: Int,
    val duration: Long
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val callDataRepository = CallDataRepository.getInstance(application)
    private val recordingRepository = RecordingRepository.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    
    // Performance: Cache for normalized numbers to avoid repeated heavy library calls
    private val normalizationCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val networkObserver = com.miniclick.calltrackmanage.util.NetworkConnectivityObserver(application)

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
                callDataRepository.syncFromSystemCallLog()
                syncNow()
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
        
        val typeFilter = try { CallTabFilter.valueOf(settingsRepository.getCallTypeFilter()) } catch(e: Exception) { CallTabFilter.ALL }
        val pTypeFilter = try { PersonTabFilter.valueOf(settingsRepository.getPersonTabFilter()) } catch(e: Exception) { PersonTabFilter.ALL }
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
            customEndDate = settingsRepository.getCustomEndDate().let { if (it == 0L) null else it }
        ) }
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
    }
    
    fun setCallTypeFilter(filter: CallTabFilter) {
        _uiState.update { it.copy(callTypeFilter = filter) }
        settingsRepository.setCallTypeFilter(filter.name)
        // Optimization: Do NOT call triggerFilter() here. 
        // tabFilteredLogs already contains data for all tabs.
        // We only update the active pointer to keep the UI snappy during swiping.
    }

    fun setPersonTabFilter(filter: PersonTabFilter) {
        _uiState.update { it.copy(personTabFilter = filter) }
        settingsRepository.setPersonTabFilter(filter.name)
        // Optimization: Do NOT call triggerFilter() here.
        // tabFilteredPersons already contains data for all tabs.
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

    private var filterJob: kotlinx.coroutines.Job? = null
    private fun triggerFilter() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            // Debounce to prevent UI lag during rapid updates - 50ms is ideal for responsiveness
            kotlinx.coroutines.delay(50)
            
            val currentState = _uiState.value
            
            // 1. Prepare shared lookup data
            val personsMap = currentState.persons.associateBy { it.phoneNumber }
            val logsByPhone = currentState.callLogs.groupBy { it.phoneNumber }
            
            // 2. Perform Single-Pass Call Filtering Across ALL Tabs
            val tabLogs = processCallFilters(currentState, personsMap)
            
            // 3. Perform Single-Pass Person Filtering Across ALL Tabs
            val tabPersons = processPersonFilters(currentState, logsByPhone)
            
            // 4. Get base logs for reporting and grouping (ALL tab)
            val allTabLogs = tabLogs[CallTabFilter.ALL] ?: emptyList()

            // 5. Compute PersonGroups for ONLY the filtered calls (Huge optimization for history)
            // This map contains summary info for each contact based on the current global filter (e.g. date range)
            val groups = allTabLogs.groupBy { it.phoneNumber }.mapValues { (number, calls) ->
                val sortedCalls = calls.sortedByDescending { it.callDate }
                val person = personsMap[number] ?: personsMap[getCachedNormalizedNumber(number)]
                PersonGroup(
                    number = number,
                    name = person?.contactName ?: calls.firstOrNull { !it.contactName.isNullOrEmpty() }?.contactName,
                    photoUri = person?.photoUri ?: calls.firstOrNull { !it.photoUri.isNullOrEmpty() }?.photoUri,
                    calls = sortedCalls,
                    lastCallDate = sortedCalls.firstOrNull()?.callDate ?: 0L,
                    totalDuration = calls.sumOf { it.duration },
                    incomingCount = calls.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                    outgoingCount = calls.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                    missedCount = calls.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE || it.callType == android.provider.CallLog.Calls.REJECTED_TYPE || it.callType == 5 },
                    personNote = person?.personNote,
                    label = person?.label
                )
            }

            // 6. Calculate Report Statistics (Heavy Operation)
            // Use ALL tab logs for reports so the stats are consistent regardless of which tab you're viewing
            val reportStats = calculateReportStats(allTabLogs, currentState.persons)

            val activeFilterCount = calculateFilterCount(currentState)
            
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
        }
    }

    private fun calculateReportStats(logs: List<CallDataEntity>, persons: List<PersonDataEntity>): ReportStats {
        if (logs.isEmpty()) return ReportStats()

        // Basic counts
        val totalCalls = logs.size
        val incomingCalls = logs.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE }
        val outgoingCalls = logs.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE }
        val missedCalls = logs.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE }
        val rejectedCalls = logs.count { it.callType == android.provider.CallLog.Calls.REJECTED_TYPE || it.callType == 5 || it.callType == 6 }
        
        // Connected/Attended (> 3s as per new rule)
        val connectedCalls = logs.count { it.duration > 3 }
        val notConnectedCalls = logs.count { it.duration <= 3 }
        val connectedIncoming = logs.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE && it.duration > 3 }
        val connectedOutgoing = logs.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE && it.duration > 3 }
        
        // Duration stats
        val totalDuration = logs.sumOf { it.duration }
        val avgDuration = if (connectedCalls > 0) totalDuration / connectedCalls else 0L
        val maxDuration = logs.maxOfOrNull { it.duration } ?: 0L
        val incomingDuration = logs.filter { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE }.sumOf { it.duration }
        val outgoingDuration = logs.filter { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE }.sumOf { it.duration }
        
        // Unique contacts
        val uniqueContacts = logs.distinctBy { it.phoneNumber }.size
        val savedContacts = logs.filter { !it.contactName.isNullOrEmpty() }.distinctBy { it.phoneNumber }.size
        val unsavedContacts = logs.filter { it.contactName.isNullOrEmpty() }.distinctBy { it.phoneNumber }.size
        
        // Notes & Reviews
        val callsWithNotes = logs.count { !it.callNote.isNullOrEmpty() }
        val reviewedCalls = logs.count { it.reviewed }
        val personsWithNotes = persons.count { !it.personNote.isNullOrEmpty() }
        val personsWithLabels = persons.count { !it.label.isNullOrEmpty() }
        
        // Recordings
        val callsWithRecordings = logs.count { !it.localRecordingPath.isNullOrEmpty() }

        // Short Calls (low engagement indicator)
        val shortCallThreshold = settingsRepository.getShortCallThresholdSeconds()
        val shortCalls = logs.count { it.duration > 0 && it.duration < shortCallThreshold }
        
        // Top callers
        val topCallers = logs
            .groupBy { it.phoneNumber }
            .map { (number, calls) ->
                val name = calls.firstOrNull { !it.contactName.isNullOrEmpty() }?.contactName
                TopCaller(
                    phoneNumber = number,
                    displayName = name ?: number,
                    callCount = calls.size,
                    totalDuration = calls.sumOf { it.duration },
                    incomingCount = calls.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                    outgoingCount = calls.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                    missedCount = calls.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE }
                )
            }
            .sortedByDescending { it.callCount }
            .take(5)
        
        // Most talked (by duration)
        val mostTalked = logs
            .groupBy { it.phoneNumber }
            .map { (number, calls) ->
                val name = calls.firstOrNull { !it.contactName.isNullOrEmpty() }?.contactName
                TopCaller(
                    phoneNumber = number,
                    displayName = name ?: number,
                    callCount = calls.size,
                    totalDuration = calls.sumOf { it.duration },
                    incomingCount = calls.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                    outgoingCount = calls.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                    missedCount = calls.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE }
                )
            }
            .filter { it.totalDuration > 0 }
            .sortedByDescending { it.totalDuration }
            .take(5)
        
        // Daily breakdown
        val dailyStats = logs
            .groupBy { 
                java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(java.util.Date(it.callDate))
            }
            .mapValues { (_, calls) ->
                DayStat(
                    count = calls.size,
                    duration = calls.sumOf { it.duration }
                )
            }
        
        // Hourly breakdown (0-23)
        val hourlyStats = logs
            .groupBy { 
                java.util.Calendar.getInstance().apply { timeInMillis = it.callDate }.get(java.util.Calendar.HOUR_OF_DAY)
            }
            .mapValues { (_, calls) -> calls.size }
        
        // Label distribution
        val labelDistribution = persons
            .flatMap { person ->
                person.label?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }

        return ReportStats(
            totalCalls = totalCalls,
            incomingCalls = incomingCalls,
            outgoingCalls = outgoingCalls,
            missedCalls = missedCalls,
            rejectedCalls = rejectedCalls,
            connectedCalls = connectedCalls,
            notConnectedCalls = notConnectedCalls,
            connectedIncoming = connectedIncoming,
            connectedOutgoing = connectedOutgoing,
            totalDuration = totalDuration,
            avgDuration = avgDuration,
            maxDuration = maxDuration,
            incomingDuration = incomingDuration,
            outgoingDuration = outgoingDuration,
            uniqueContacts = uniqueContacts,
            savedContacts = savedContacts,
            unsavedContacts = unsavedContacts,
            callsWithNotes = callsWithNotes,
            reviewedCalls = reviewedCalls,
            callsWithRecordings = callsWithRecordings,
            personsWithNotes = personsWithNotes,
            personsWithLabels = personsWithLabels,
            shortCalls = shortCalls,
            topCallers = topCallers,
            mostTalked = mostTalked,
            dailyStats = dailyStats,
            hourlyStats = hourlyStats,
            labelDistribution = labelDistribution
        )
    }

    private fun getCachedNormalizedNumber(number: String): String {
        return normalizationCache.getOrPut(number) {
            normalizePhoneNumber(number)
        }
    }

    fun normalizePhoneNumber(number: String) = callDataRepository.normalizePhoneNumber(number)

    private fun processCallFilters(
        current: HomeUiState,
        personsMap: Map<String, PersonDataEntity>
    ): Map<CallTabFilter, List<CallDataEntity>> {
        if (current.callLogs.isEmpty()) return emptyMap()

        val query = current.searchQuery.trim().lowercase()
        val hasQuery = query.isNotEmpty()
        val labelFilter = current.labelFilter
        val hasLabelFilter = labelFilter.isNotEmpty()
        
        val connFilter = current.connectedFilter
        val nFilter = current.notesFilter
        val pnFilter = current.personNotesFilter
        val cFilter = current.contactsFilter
        val aFilter = current.attendedFilter
        val rFilter = current.reviewedFilter
        val cnFilter = current.customNameFilter
        val dRange = current.dateRange
        
        val todayStart = getStartOfDay(0)
        val threeDaysStart = getStartOfDay(2)
        val sevenDaysStart = getStartOfDay(6)
        val fourteenDaysStart = getStartOfDay(13)
        val thirtyDaysStart = getStartOfDay(29)
        val thisMonthStart = getStartOfThisMonth()
        val prevMonthStart = getStartOfPreviousMonth()
        
        val cStart = current.customStartDate ?: 0L
        val cEnd = current.customEndDate ?: Long.MAX_VALUE
        val tStartDate = current.trackStartDate
        val simSel = current.simSelection
        val targetSubId = if (simSel == "Both") null else getSubIdForSim(simSel)

        val tabMaps = java.util.EnumMap<CallTabFilter, ArrayList<CallDataEntity>>(CallTabFilter::class.java)
        CallTabFilter.entries.forEach { tabMaps[it] = ArrayList<CallDataEntity>() }

        for (call in current.callLogs) {
            if (tStartDate > 0 && call.callDate < tStartDate) continue
            if (targetSubId != null && call.subscriptionId != targetSubId) continue

            val matchesDate = when (dRange) {
                DateRange.TODAY -> call.callDate >= todayStart
                DateRange.LAST_3_DAYS -> call.callDate >= threeDaysStart
                DateRange.LAST_7_DAYS -> call.callDate >= sevenDaysStart
                DateRange.LAST_14_DAYS -> call.callDate >= fourteenDaysStart
                DateRange.LAST_30_DAYS -> call.callDate >= thirtyDaysStart
                DateRange.THIS_MONTH -> call.callDate >= thisMonthStart
                DateRange.PREVIOUS_MONTH -> call.callDate in prevMonthStart until thisMonthStart
                DateRange.CUSTOM -> call.callDate in cStart..cEnd
                DateRange.ALL -> true
            }
            if (!matchesDate) continue

            var person = personsMap[call.phoneNumber]
            if (person == null) {
                val norm = getCachedNormalizedNumber(call.phoneNumber)
                person = personsMap[norm]
            }

            if (person?.isNoTracking == true) continue
            
            if (hasLabelFilter) {
                val personLabels = person?.label?.split(",")?.map { it.trim() } ?: emptyList()
                if (!personLabels.contains(labelFilter)) continue
            }

            val isConnected = call.duration > 0
            if (connFilter == ConnectedFilter.CONNECTED && !isConnected) continue
            if (connFilter == ConnectedFilter.NOT_CONNECTED && isConnected) continue
            
            if (nFilter == NotesFilter.WITH_NOTE && call.callNote.isNullOrEmpty()) continue
            if (nFilter == NotesFilter.WITHOUT_NOTE && !call.callNote.isNullOrEmpty()) continue
            
            if (pnFilter == PersonNotesFilter.WITH_NOTE && person?.personNote.isNullOrEmpty()) continue
            if (pnFilter == PersonNotesFilter.WITHOUT_NOTE && !person?.personNote.isNullOrEmpty()) continue

            if (rFilter == ReviewedFilter.REVIEWED && !call.reviewed) continue
            if (rFilter == ReviewedFilter.NOT_REVIEWED && call.reviewed) continue

            if (cnFilter == CustomNameFilter.WITH_NAME && person?.contactName.isNullOrEmpty()) continue
            if (cnFilter == CustomNameFilter.WITHOUT_NAME && !person?.contactName.isNullOrEmpty()) continue

            if (cFilter == ContactsFilter.IN_CONTACTS && call.contactName.isNullOrEmpty()) continue
            if (cFilter == ContactsFilter.NOT_IN_CONTACTS && !call.contactName.isNullOrEmpty()) continue
            
            if (aFilter == AttendedFilter.ATTENDED && !isConnected) continue
            if (aFilter == AttendedFilter.NEVER_ATTENDED && isConnected) continue

            if (hasQuery) {
                val matches = call.phoneNumber.contains(query) || 
                              (call.contactName?.lowercase()?.contains(query) == true) ||
                              (call.callNote?.lowercase()?.contains(query) == true)
                if (!matches) continue
            }

            val isIgnored = person?.hasAnyExclusion == true || call.callType == 6

            if (isIgnored) {
                tabMaps[CallTabFilter.IGNORED]?.add(call)
            } else {
                tabMaps[CallTabFilter.ALL]?.add(call)
                
                when (call.callType) {
                    android.provider.CallLog.Calls.INCOMING_TYPE -> {
                        if (call.duration > 3) {
                            tabMaps[CallTabFilter.ATTENDED]?.add(call)
                        } else {
                            tabMaps[CallTabFilter.NOT_ATTENDED]?.add(call)
                        }
                    }
                    android.provider.CallLog.Calls.OUTGOING_TYPE -> {
                        if (call.duration > 3) {
                            tabMaps[CallTabFilter.RESPONDED]?.add(call)
                        } else {
                            tabMaps[CallTabFilter.NOT_RESPONDED]?.add(call)
                        }
                    }
                    android.provider.CallLog.Calls.MISSED_TYPE, 
                    android.provider.CallLog.Calls.REJECTED_TYPE, 
                    5 -> { 
                        tabMaps[CallTabFilter.NOT_ATTENDED]?.add(call)
                    }
                }
            }
        }
        
        // Final Sorting for all call tabs
        val isDesc = current.callSortDirection == SortDirection.DESCENDING
        val sortBy = current.callSortBy
        
        tabMaps.values.forEach { list ->
            if (list.isNotEmpty()) {
                when (sortBy) {
                    CallSortBy.DATE -> {
                        if (isDesc) list.sortByDescending { it.callDate }
                        else list.sortBy { it.callDate }
                    }
                    CallSortBy.DURATION -> {
                        if (isDesc) list.sortByDescending { it.duration }
                        else list.sortBy { it.duration }
                    }
                    CallSortBy.NUMBER -> {
                        if (isDesc) list.sortByDescending { it.phoneNumber }
                        else list.sortBy { it.phoneNumber }
                    }
                }
            }
        }
        
        return tabMaps
    }

    private fun processPersonFilters(
        current: HomeUiState,
        logsByPhone: Map<String, List<CallDataEntity>>
    ): Map<PersonTabFilter, List<PersonDataEntity>> {
        if (current.persons.isEmpty()) return emptyMap()

        val query = current.searchQuery.trim().lowercase()
        val hasQuery = query.isNotEmpty()
        val labelFilter = current.labelFilter
        val hasLabelFilter = labelFilter.isNotEmpty()
        
        val nFilter = current.notesFilter
        val pnFilter = current.personNotesFilter
        val cFilter = current.contactsFilter
        val minCount = current.minCallCount
        val rFilter = current.reviewedFilter
        val cnFilter = current.customNameFilter

        val tabMaps = java.util.EnumMap<PersonTabFilter, ArrayList<PersonDataEntity>>(PersonTabFilter::class.java)
        PersonTabFilter.entries.forEach { tabMaps[it] = ArrayList<PersonDataEntity>() }

        val dRange = current.dateRange
        val todayStart = getStartOfDay(0)
        val threeDaysStart = getStartOfDay(2)
        val sevenDaysStart = getStartOfDay(6)
        val fourteenDaysStart = getStartOfDay(13)
        val thirtyDaysStart = getStartOfDay(29)
        val thisMonthStart = getStartOfThisMonth()
        val prevMonthStart = getStartOfPreviousMonth()
        val cStart = current.customStartDate ?: 0L
        val cEnd = current.customEndDate ?: Long.MAX_VALUE

        for (person in current.persons) {
            if (person.isNoTracking) continue

            // Date range filter apply in person based on last call
            val lastDate = person.lastCallDate ?: 0L
            val matchesDate = when (dRange) {
                DateRange.TODAY -> lastDate >= todayStart
                DateRange.LAST_3_DAYS -> lastDate >= threeDaysStart
                DateRange.LAST_7_DAYS -> lastDate >= sevenDaysStart
                DateRange.LAST_14_DAYS -> lastDate >= fourteenDaysStart
                DateRange.LAST_30_DAYS -> lastDate >= thirtyDaysStart
                DateRange.THIS_MONTH -> lastDate >= thisMonthStart
                DateRange.PREVIOUS_MONTH -> lastDate in prevMonthStart until thisMonthStart
                DateRange.CUSTOM -> lastDate in cStart..cEnd
                DateRange.ALL -> true
            }
            if (!matchesDate) continue
            
            if (hasLabelFilter) {
                val personLabels = person.label?.split(",")?.map { it.trim() } ?: emptyList()
                if (!personLabels.contains(labelFilter)) continue
            }

            if (minCount > 0 && person.totalCalls < minCount) continue
            
            if (pnFilter == PersonNotesFilter.WITH_NOTE && person.personNote.isNullOrEmpty()) continue
            if (pnFilter == PersonNotesFilter.WITHOUT_NOTE && !person.personNote.isNullOrEmpty()) continue

            if (cnFilter == CustomNameFilter.WITH_NAME && person.contactName.isNullOrEmpty()) continue
            if (cnFilter == CustomNameFilter.WITHOUT_NAME && !person.contactName.isNullOrEmpty()) continue

            if (cFilter == ContactsFilter.IN_CONTACTS && person.contactName.isNullOrEmpty()) continue
            if (cFilter == ContactsFilter.NOT_IN_CONTACTS && !person.contactName.isNullOrEmpty()) continue

            val pCalls = logsByPhone[person.phoneNumber] ?: emptyList()
            
            if (nFilter == NotesFilter.WITH_NOTE && pCalls.none { !it.callNote.isNullOrEmpty() }) continue
            if (nFilter == NotesFilter.WITHOUT_NOTE && pCalls.any { !it.callNote.isNullOrEmpty() }) continue

            if (rFilter == ReviewedFilter.REVIEWED && pCalls.any { !it.reviewed }) continue
            if (rFilter == ReviewedFilter.NOT_REVIEWED && pCalls.any { it.reviewed }) continue

            if (hasQuery) {
                val matches = person.phoneNumber.contains(query) || 
                             (person.contactName?.lowercase()?.contains(query) == true) ||
                             (person.personNote?.lowercase()?.contains(query) == true)
                if (!matches) continue
            }

            val isIgnored = person.hasAnyExclusion

            if (isIgnored) {
                tabMaps[PersonTabFilter.IGNORED]?.add(person)
            } else {
                tabMaps[PersonTabFilter.ALL]?.add(person)
                
                // Categorize based on LAST call status (>3s rule as per request)
                val lastType = person.lastCallType
                val lastDur = person.lastCallDuration ?: 0L
                
                when (lastType) {
                    android.provider.CallLog.Calls.INCOMING_TYPE -> {
                        if (lastDur > 3) {
                            tabMaps[PersonTabFilter.ATTENDED]?.add(person)
                        } else {
                            tabMaps[PersonTabFilter.NOT_ATTENDED]?.add(person)
                        }
                    }
                    android.provider.CallLog.Calls.OUTGOING_TYPE -> {
                        if (lastDur > 3) {
                            tabMaps[PersonTabFilter.RESPONDED]?.add(person)
                        } else {
                            tabMaps[PersonTabFilter.NOT_RESPONDED]?.add(person)
                        }
                    }
                    android.provider.CallLog.Calls.MISSED_TYPE,
                    android.provider.CallLog.Calls.REJECTED_TYPE,
                    5 -> {
                        tabMaps[PersonTabFilter.NOT_ATTENDED]?.add(person)
                    }
                }
            }
        }
        
        // Final Sorting for all tabs
        val isDesc = current.personSortDirection == SortDirection.DESCENDING
        val sortBy = current.personSortBy
        
        tabMaps.values.forEach { list ->
            if (list.isNotEmpty()) {
                when (sortBy) {
                    PersonSortBy.LAST_CALL -> {
                        if (isDesc) list.sortByDescending { it.lastCallDate }
                        else list.sortBy { it.lastCallDate }
                    }
                    PersonSortBy.MOST_CALLS -> {
                        if (isDesc) list.sortByDescending { it.totalCalls }
                        else list.sortBy { it.totalCalls }
                    }
                    PersonSortBy.NAME -> {
                        val comparator = compareBy<PersonDataEntity> { it.contactName ?: it.phoneNumber }
                        if (isDesc) list.sortWith(comparator.reversed())
                        else list.sortWith(comparator)
                    }
                }
            }
        }
        
        return tabMaps
    }

    // ============================================
    // CALL FLOW (MULTI-SIM)
    // ============================================

    /**
     * Start the call process for a number.
     * Decides whether to show the SIM picker or dial immediately.
     */
    fun initiateCall(number: String) {
        if (number.isBlank()) return
        
        val currentState = _uiState.value
        val activeSims = currentState.availableSims

        if (activeSims.size > 1) {
            // Multiple SIMs - show picker
            _uiState.update { it.copy(
                callFlowNumber = number,
                showCallSimPicker = true
            )}
        } else {
            // Single SIM or unknown - dial directly using default
            val subId = activeSims.firstOrNull()?.subscriptionId
            val forceDialer = currentState.callActionBehavior == "Open in Dialpad"
            com.miniclick.calltrackmanage.ui.utils.CallUtils.makeCall(getApplication(), number, subId, forceDialer)
        }
    }

    /**
     * Execute the call with a specifically selected SIM.
     */
    fun executeCall(subId: Int?) {
        val number = _uiState.value.callFlowNumber ?: return
        val ctx = getApplication<Application>()
        val forceDialer = _uiState.value.callActionBehavior == "Open in Dialpad"
        
        com.miniclick.calltrackmanage.ui.utils.CallUtils.makeCall(ctx, number, subId, forceDialer)
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

    private fun calculateFilterCount(uiState: HomeUiState): Int {
        var count = 0
        if (uiState.connectedFilter != ConnectedFilter.ALL) count++
        if (uiState.notesFilter != NotesFilter.ALL) count++
        if (uiState.personNotesFilter != PersonNotesFilter.ALL) count++
        if (uiState.contactsFilter != ContactsFilter.ALL) count++
        if (uiState.reviewedFilter != ReviewedFilter.ALL) count++
        if (uiState.customNameFilter != CustomNameFilter.ALL) count++
        if (uiState.minCallCount > 0) count++
        if (uiState.labelFilter.isNotEmpty()) count++
        return count
    }

    // ============================================
    // NOTES
    // ============================================
    
    fun saveCallNote(compositeId: String, note: String) {
        viewModelScope.launch {
            callDataRepository.updateCallNote(compositeId, note.ifEmpty { null })
            quickSync()  // Use quick sync - no need to import system calls for note edit
        }
    }

    fun savePersonNote(phoneNumber: String, note: String) {
        viewModelScope.launch {
            callDataRepository.updatePersonNote(phoneNumber, note.ifEmpty { null })
            quickSync()
        }
    }
    
    fun savePersonLabel(phoneNumber: String, label: String) {
        viewModelScope.launch {
            callDataRepository.updatePersonLabel(phoneNumber, label.ifEmpty { null })
            quickSync()
        }
    }

    fun savePersonName(phoneNumber: String, name: String) {
        viewModelScope.launch {
            callDataRepository.updatePersonName(phoneNumber, name.ifEmpty { null })
            quickSync()
        }
    }
    
    fun updateReviewed(compositeId: String, reviewed: Boolean) {
        viewModelScope.launch {
            callDataRepository.updateReviewed(compositeId, reviewed)
            quickSync()
        }
    }

    fun markAllCallsReviewed(phoneNumber: String) {
        viewModelScope.launch {
            callDataRepository.markAllCallsReviewed(phoneNumber)
            quickSync()
        }
    }

    // ============================================
    // SYNC STATUS
    // ============================================
    
    fun updateCallStatus(compositeId: String, status: MetadataSyncStatus) {
        viewModelScope.launch {
            callDataRepository.updateMetadataSyncStatus(compositeId, status)
        }
    }

    fun updateRecordingPathForLog(compositeId: String, path: String?) {
        viewModelScope.launch {
            callDataRepository.updateRecordingPath(compositeId, path)
            quickSync()  // Use quick sync - recording path update doesn't need system import
        }
    }

    /**
     * Full sync - imports from system call log + pushes pending changes
     * Use for: pull-to-refresh, after call ends, app start
     */
    fun syncNow() {
        val application = getApplication<android.app.Application>()
        com.miniclick.calltrackmanage.worker.CallSyncWorker.runNow(application)
        com.miniclick.calltrackmanage.worker.RecordingUploadWorker.runNow(application)
    }

    /**
     * Quick sync - only pushes pending metadata changes, skips system import
     * Use for: note edit, label change, reviewed status update
     * Avoids showing "Importing Data" / "Finding Recordings" in status bar
     */
    fun quickSync() {
        val application = getApplication<android.app.Application>()
        com.miniclick.calltrackmanage.worker.CallSyncWorker.runQuickSync(application)
        com.miniclick.calltrackmanage.worker.RecordingUploadWorker.runNow(application)
    }

    /**
     * Look for pending sync again and check server for updates
     * This is a "Force Refresh & Sync"
     */
    fun fullSync() {
        viewModelScope.launch {
            // First refresh local scan from system
            callDataRepository.syncFromSystemCallLog()
            // Then trigger workers
            syncNow()
        }
    }

    // ============================================
    // EXCLUSION
    // ============================================

    fun ignoreNumber(phoneNumber: String) {
        viewModelScope.launch {
            callDataRepository.updateExclusionType(phoneNumber, excludeFromSync = false, excludeFromList = true)
            // The flow collectors in init will catch this and trigger re-filter
        }
    }

    fun noTrackNumber(phoneNumber: String) {
        viewModelScope.launch {
            callDataRepository.updateExclusionType(phoneNumber, excludeFromSync = true, excludeFromList = true)
            // The flow collectors in init will catch this and trigger re-filter
        }
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
        val ctx = getApplication<Application>()
        val sims = if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            val sm = ContextCompat.getSystemService(ctx, SubscriptionManager::class.java)
            sm?.activeSubscriptionInfoList?.map { 
                SimInfo(it.simSlotIndex, it.displayName.toString(), it.carrierName.toString(), it.subscriptionId)
            }?.sortedBy { it.slotIndex } ?: emptyList()
        } else {
            emptyList()
        }
        
        // Check for critical changes that require re-syncing
        val needsSync = simSelection != _uiState.value.simSelection || 
                        trackStartDate != _uiState.value.trackStartDate ||
                        sim1SubId != _uiState.value.sim1SubId ||
                        sim2SubId != _uiState.value.sim2SubId ||
                        callRecordEnabled != _uiState.value.callRecordEnabled ||
                        callerPhone1 != _uiState.value.callerPhoneSim1 ||
                        callerPhone2 != _uiState.value.callerPhoneSim2
        
        val showDialButton = settingsRepository.isShowDialButton()
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
            showDialButton = showDialButton,
            callActionBehavior = callActionBehavior
        ) }

        if (needsSync) {
            syncFromSystem()
        } else {
            triggerFilter()
        }
    }


    @SuppressLint("MissingPermission")
    private fun getSubIdForSim(simName: String): Int? {
        try {
            if (simName == "Both") return null
            val slotIndex = simName.replace("Sim", "").toIntOrNull()?.minus(1) ?: return null
            val subManager = getApplication<Application>().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            
            if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
               val info = subManager.activeSubscriptionInfoList?.find { it.simSlotIndex == slotIndex }
               return info?.subscriptionId
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getStartOfDay(daysAgo: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfThisMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfPreviousMonth(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
