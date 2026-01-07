package com.miniclick.calltrackmanage.ui.home

import android.app.Application
import android.provider.CallLog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.CallLogStatus
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

// Filter enums for chip dropdowns
enum class CallTypeFilter { ALL, INCOMING, OUTGOING, MISSED, REJECTED }
enum class ConnectedFilter { ALL, CONNECTED, NOT_CONNECTED }
enum class NotesFilter { ALL, WITH_NOTE, WITHOUT_NOTE }
enum class ContactsFilter { ALL, IN_CONTACTS, NOT_IN_CONTACTS }
enum class AttendedFilter { ALL, ATTENDED, NEVER_ATTENDED }
enum class ViewMode { CALLS, PERSONS }
enum class DateRange { TODAY, LAST_3_DAYS, LAST_7_DAYS, LAST_14_DAYS, LAST_30_DAYS, THIS_MONTH, PREVIOUS_MONTH, CUSTOM, ALL }
enum class PersonSortBy { LAST_CALL, MOST_CALLS }
enum class SortDirection { ASCENDING, DESCENDING }

data class HomeUiState(
    val callLogs: List<CallDataEntity> = emptyList(),
    val filteredLogs: List<CallDataEntity> = emptyList(),
    val persons: List<PersonDataEntity> = emptyList(),
    val filteredPersons: List<PersonDataEntity> = emptyList(),
    val isLoading: Boolean = false,
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
    val callTypeFilter: CallTypeFilter = CallTypeFilter.ALL,
    val connectedFilter: ConnectedFilter = ConnectedFilter.ALL,
    val notesFilter: NotesFilter = NotesFilter.ALL,
    val contactsFilter: ContactsFilter = ContactsFilter.ALL,
    val attendedFilter: AttendedFilter = AttendedFilter.ALL,
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
    val allowPersonalExclusion: Boolean = false,
    val callRecordEnabled: Boolean = true,
    val activeRecordings: List<CallDataEntity> = emptyList(),
    val sim1SubId: Int? = null,
    val sim2SubId: Int? = null,
    val callerPhoneSim1: String = "",
    val callerPhoneSim2: String = ""
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val callDataRepository = CallDataRepository.getInstance(application)
    private val recordingRepository = RecordingRepository.getInstance(application)
    private val settingsRepository = SettingsRepository.getInstance(application)
    private val networkObserver = com.miniclick.calltrackmanage.util.NetworkConnectivityObserver(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            refreshSettings()
            loadRecordingPath()
            loadPersistentState()
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
            callDataRepository.getAllPersonsFlow()
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

    private fun loadPersistentState() {
        val searchVisible = settingsRepository.isSearchVisible()
        val filtersVisible = settingsRepository.isFiltersVisible()
        val searchQuery = settingsRepository.getSearchQuery()
        val labelFilter = settingsRepository.getLabelFilter()
        
        val typeFilter = try { CallTypeFilter.valueOf(settingsRepository.getCallTypeFilter()) } catch(e: Exception) { CallTypeFilter.ALL }
        val connFilter = try { ConnectedFilter.valueOf(settingsRepository.getConnectedFilter()) } catch(e: Exception) { ConnectedFilter.ALL }
        val nFilter = try { NotesFilter.valueOf(settingsRepository.getNotesFilter()) } catch(e: Exception) { NotesFilter.ALL }
        val cFilter = try { ContactsFilter.valueOf(settingsRepository.getContactsFilter()) } catch(e: Exception) { ContactsFilter.ALL }
        val aFilter = try { AttendedFilter.valueOf(settingsRepository.getAttendedFilter()) } catch(e: Exception) { AttendedFilter.ALL }

        _uiState.update { it.copy(
            isSearchVisible = searchVisible,
            isFiltersVisible = filtersVisible,
            searchQuery = searchQuery,
            labelFilter = labelFilter,
            callTypeFilter = typeFilter,
            connectedFilter = connFilter,
            notesFilter = nFilter,
            contactsFilter = cFilter,
            attendedFilter = aFilter,
            dateRange = try { DateRange.valueOf(settingsRepository.getDateRangeFilter()) } catch(e: Exception) { DateRange.LAST_7_DAYS },
            customStartDate = settingsRepository.getCustomStartDate().let { if (it == 0L) null else it },
            customEndDate = settingsRepository.getCustomEndDate().let { if (it == 0L) null else it }
        ) }
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

    fun onTabSelected(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
        triggerFilter()
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
    
    fun setCallTypeFilter(filter: CallTypeFilter) {
        _uiState.update { it.copy(callTypeFilter = filter) }
        settingsRepository.setCallTypeFilter(filter.name)
        triggerFilter()
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

    private var filterJob: kotlinx.coroutines.Job? = null
    private fun triggerFilter() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            // Debounce to prevent UI lag during rapid updates
            kotlinx.coroutines.delay(20)
            
            val currentState = _uiState.value
            val filteredLogs = applyFiltersInternal(currentState)
            val filteredPersons = applyPersonFiltersInternal(currentState)
            
            _uiState.update { it.copy(
                filteredLogs = filteredLogs,
                filteredPersons = filteredPersons
            ) }
        }
    }

    fun normalizePhoneNumber(number: String) = callDataRepository.normalizePhoneNumber(number)

    private fun applyFiltersInternal(current: HomeUiState): List<CallDataEntity> {
        if (current.callLogs.isEmpty()) return emptyList()

        val query = current.searchQuery.lowercase()
        val labelFilter = current.labelFilter
        val typeFilter = current.callTypeFilter
        val connFilter = current.connectedFilter
        val nFilter = current.notesFilter
        val cFilter = current.contactsFilter
        val aFilter = current.attendedFilter
        val dRange = current.dateRange
        val cStart = current.customStartDate ?: 0L
        val cEnd = current.customEndDate ?: Long.MAX_VALUE
        val tStartDate = current.trackStartDate
        val simSel = current.simSelection

        // Pre-calculate date boundaries ONCE outside the loop
        val todayStart = getStartOfDay(0)
        val threeDaysStart = getStartOfDay(2)
        val sevenDaysStart = getStartOfDay(6)
        val fourteenDaysStart = getStartOfDay(13)
        val thirtyDaysStart = getStartOfDay(29)
        val thisMonthStart = getStartOfThisMonth()
        val prevMonthStart = getStartOfPreviousMonth()

        // Cache target sub ID
        val targetSubId = if (simSel == "Both") null else getSubIdForSim(simSel)
        
        // Map for quick person lookup
        val personsMap = current.persons.associateBy { it.phoneNumber }
        
        val filtered = current.callLogs.filter { call ->
            // Date Range Filter (Most restrictive/fastest first)
            val dateRangeMatch = when (dRange) {
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
            if (!dateRangeMatch) return@filter false

            // Track date
            if (tStartDate > 0 && call.callDate < tStartDate) return@filter false

            // Label Filter
            if (labelFilter.isNotEmpty()) {
                val person = personsMap[call.phoneNumber] ?: personsMap[normalizePhoneNumber(call.phoneNumber)]
                val personLabels = person?.label?.split(",")?.map { it.trim() } ?: emptyList()
                if (!personLabels.contains(labelFilter)) return@filter false
            }

            // Call Type Filter
            val typeMatch = when (typeFilter) {
                CallTypeFilter.ALL -> true
                CallTypeFilter.INCOMING -> call.callType == CallLog.Calls.INCOMING_TYPE
                CallTypeFilter.OUTGOING -> call.callType == CallLog.Calls.OUTGOING_TYPE
                CallTypeFilter.MISSED -> call.callType == CallLog.Calls.MISSED_TYPE
                CallTypeFilter.REJECTED -> call.callType == CallLog.Calls.REJECTED_TYPE
            }
            if (!typeMatch) return@filter false
            
            // Connected/Attended Filter (duration based)
            val durationMatch = when {
                connFilter == ConnectedFilter.CONNECTED || aFilter == AttendedFilter.ATTENDED -> call.duration > 0
                connFilter == ConnectedFilter.NOT_CONNECTED || aFilter == AttendedFilter.NEVER_ATTENDED -> call.duration <= 0
                else -> true
            }
            if (!durationMatch) return@filter false
            
            // Notes Filter
            if (nFilter == NotesFilter.WITH_NOTE && call.callNote.isNullOrEmpty()) return@filter false
            if (nFilter == NotesFilter.WITHOUT_NOTE && !call.callNote.isNullOrEmpty()) return@filter false
            
            // Contacts Filter
            if (cFilter == ContactsFilter.IN_CONTACTS && call.contactName.isNullOrEmpty()) return@filter false
            if (cFilter == ContactsFilter.NOT_IN_CONTACTS && !call.contactName.isNullOrEmpty()) return@filter false

            // SIM Filter
            if (targetSubId != null && call.subscriptionId != targetSubId) return@filter false

            // Search Filter (last as it's string based)
            if (query.isNotEmpty()) {
                val matches = call.phoneNumber.contains(query) || 
                              (call.contactName?.lowercase()?.contains(query) == true) ||
                              (call.callNote?.lowercase()?.contains(query) == true)
                if (!matches) return@filter false
            }

            true
        }
        return filtered
    }

    private fun applyPersonFiltersInternal(current: HomeUiState): List<PersonDataEntity> {
        val query = current.searchQuery.lowercase()
        
        val filtered = if (query.isEmpty()) {
            if (current.labelFilter.isNotEmpty()) {
                current.persons.filter { it.label?.split(",")?.map { l -> l.trim() }?.contains(current.labelFilter) == true }
            } else {
                current.persons
            }
        } else {
            current.persons.filter { person ->
                val matchesQuery = (person.phoneNumber.contains(query) ||
                (person.contactName?.lowercase()?.contains(query) == true) ||
                (person.personNote?.lowercase()?.contains(query) == true))
                
                val matchesLabel = if (current.labelFilter.isNotEmpty()) {
                person.label?.split(",")?.map { l -> l.trim() }?.contains(current.labelFilter) == true
            } else true
                
                matchesQuery && matchesLabel
            }
        }

        val sorted = when (current.personSortBy) {
            PersonSortBy.LAST_CALL -> {
                if (current.personSortDirection == SortDirection.DESCENDING) {
                    filtered.sortedByDescending { it.lastCallDate ?: 0L }
                } else {
                    filtered.sortedBy { it.lastCallDate ?: 0L }
                }
            }
            PersonSortBy.MOST_CALLS -> {
                if (current.personSortDirection == SortDirection.DESCENDING) {
                    filtered.sortedByDescending { it.totalCalls }
                } else {
                    filtered.sortedBy { it.totalCalls }
                }
            }
        }
        
        return sorted
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
    
    fun updateCallStatus(compositeId: String, status: CallLogStatus) {
        viewModelScope.launch {
            callDataRepository.updateSyncStatus(compositeId, status)
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

    fun excludeNumber(phoneNumber: String) {
        viewModelScope.launch {
            callDataRepository.updateExclusion(phoneNumber, true)
            // The flow collectors in init will catch this and trigger re-filter
        }
    }

    fun refreshSettings() {
        val simSelection = settingsRepository.getSimSelection()
        val trackStartDate = settingsRepository.getTrackStartDate()
        val whatsappPreference = settingsRepository.getWhatsappPreference()
        val orgId = settingsRepository.getOrganisationId()
        val isSyncSetup = orgId.isNotEmpty()
        val allowPersonalExclusion = settingsRepository.isAllowPersonalExclusion()
        val callRecordEnabled = settingsRepository.isCallRecordEnabled()
        val sim1SubId = settingsRepository.getSim1SubscriptionId().let { if (it == -1) null else it }
        val sim2SubId = settingsRepository.getSim2SubscriptionId().let { if (it == -1) null else it }
        val callerPhone1 = settingsRepository.getCallerPhoneSim1()
        val callerPhone2 = settingsRepository.getCallerPhoneSim2()
        
        if (simSelection != _uiState.value.simSelection || 
            trackStartDate != _uiState.value.trackStartDate ||
            whatsappPreference != _uiState.value.whatsappPreference ||
            isSyncSetup != _uiState.value.isSyncSetup ||
            allowPersonalExclusion != _uiState.value.allowPersonalExclusion ||
            callRecordEnabled != _uiState.value.callRecordEnabled ||
            sim1SubId != _uiState.value.sim1SubId ||
            sim2SubId != _uiState.value.sim2SubId ||
            callerPhone1 != _uiState.value.callerPhoneSim1 ||
            callerPhone2 != _uiState.value.callerPhoneSim2) {
            _uiState.update { it.copy(
                simSelection = simSelection, 
                trackStartDate = trackStartDate,
                whatsappPreference = whatsappPreference,
                isSyncSetup = isSyncSetup,
                allowPersonalExclusion = allowPersonalExclusion,
                callRecordEnabled = callRecordEnabled,
                sim1SubId = sim1SubId,
                sim2SubId = sim2SubId,
                callerPhoneSim1 = callerPhone1,
                callerPhoneSim2 = callerPhone2
            ) }
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
