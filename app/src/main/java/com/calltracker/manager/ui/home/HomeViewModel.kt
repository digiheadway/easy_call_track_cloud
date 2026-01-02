package com.calltracker.manager.ui.home

import android.app.Application
import android.provider.CallLog
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calltracker.manager.data.CallDataRepository
import com.calltracker.manager.data.RecordingRepository
import com.calltracker.manager.data.db.CallDataEntity
import com.calltracker.manager.data.db.CallLogStatus
import com.calltracker.manager.data.db.PersonDataEntity
import com.calltracker.manager.data.SettingsRepository
import android.telephony.SubscriptionManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlinx.coroutines.flow.combine

// Filter enums for chip dropdowns
enum class CallTypeFilter { ALL, INCOMING, OUTGOING, MISSED, REJECTED }
enum class ConnectedFilter { ALL, CONNECTED, NOT_CONNECTED }
enum class NotesFilter { ALL, WITH_NOTE, WITHOUT_NOTE }
enum class ContactsFilter { ALL, IN_CONTACTS, NOT_IN_CONTACTS }
enum class AttendedFilter { ALL, ATTENDED, NEVER_ATTENDED }
enum class ViewMode { CALLS, PERSONS }
enum class DateRange { TODAY, LAST_3_DAYS, LAST_7_DAYS, LAST_14_DAYS, LAST_30_DAYS, THIS_MONTH, PREVIOUS_MONTH, CUSTOM, ALL }

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

    val dateRange: DateRange = DateRange.LAST_3_DAYS,
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
    val allowPersonalExclusion: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val callDataRepository = CallDataRepository(application)
    private val recordingRepository = RecordingRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val networkObserver = com.calltracker.manager.util.NetworkConnectivityObserver(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshSettings()
        loadRecordingPath()
        
        // Observe Room DB for real-time updates
        viewModelScope.launch {
            callDataRepository.getAllCallsFlow().collect { calls ->
                _uiState.update { it.copy(callLogs = calls) }
                triggerFilter()
            }
        }
        
        viewModelScope.launch {
            callDataRepository.getAllPersonsFlow().collect { persons ->
                _uiState.update { it.copy(persons = persons) }
                triggerFilter()
            }
        }
        

        // Observe Pending Syncs (Calls + Persons)
        viewModelScope.launch {
            callDataRepository.getPendingChangesCountFlow().collect { totalPending ->
                _uiState.update { it.copy(pendingSyncCount = totalPending) }
            }
        }

        // Observe Metadata Sync Queue (Total)
        viewModelScope.launch {
            callDataRepository.getPendingMetadataSyncCountFlow().collect { count ->
                _uiState.update { it.copy(pendingMetadataCount = count) }
            }
        }

        // Observe Granular Metadata Queues
        viewModelScope.launch {
            callDataRepository.getPendingNewCallsCountFlow().collect { count ->
                _uiState.update { it.copy(pendingNewCallsCount = count) }
            }
        }
        viewModelScope.launch {
            callDataRepository.getPendingMetadataUpdatesCountFlow().collect { count ->
                _uiState.update { it.copy(pendingMetadataUpdatesCount = count) }
            }
        }
        viewModelScope.launch {
            callDataRepository.getPendingPersonUpdatesCountFlow().collect { count ->
                _uiState.update { it.copy(pendingPersonUpdatesCount = count) }
            }
        }

        // Observe Recording Sync Queue
        viewModelScope.launch {
            callDataRepository.getPendingRecordingSyncCountFlow().collect { count ->
                _uiState.update { it.copy(pendingRecordingCount = count) }
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
                applyFilters()
                applyPersonFilters()
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

    /**
     * Get recording for a call (with caching)
     */
    suspend fun getRecordingForLog(call: CallDataEntity): String? {
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

    // ============================================
    // SEARCH & FILTERS
    // ============================================
    
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
        applyPersonFilters()
    }

    fun onTabSelected(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
        applyFilters()
    }
    
    fun toggleSearchVisibility() {
        _uiState.update { it.copy(isSearchVisible = !it.isSearchVisible) }
    }
    
    fun toggleFiltersVisibility() {
        _uiState.update { it.copy(isFiltersVisible = !it.isFiltersVisible) }
    }
    
    fun setViewMode(mode: ViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }
    
    fun setCallTypeFilter(filter: CallTypeFilter) {
        _uiState.update { it.copy(callTypeFilter = filter) }
        applyFilters()
    }
    
    fun setConnectedFilter(filter: ConnectedFilter) {
        _uiState.update { it.copy(connectedFilter = filter) }
        applyFilters()
    }
    
    fun setNotesFilter(filter: NotesFilter) {
        _uiState.update { it.copy(notesFilter = filter) }
        applyFilters()
    }
    
    fun setContactsFilter(filter: ContactsFilter) {
        _uiState.update { it.copy(contactsFilter = filter) }
        applyFilters()
    }
    
    fun setAttendedFilter(filter: AttendedFilter) {
        _uiState.update { it.copy(attendedFilter = filter) }
        applyFilters()
    }

    fun setLabelFilter(label: String) {
        _uiState.update { it.copy(labelFilter = label) }
        applyFilters()
        applyPersonFilters() // Labels affect both lists
    }

    fun setDateRange(range: DateRange, start: Long? = null, end: Long? = null) {
        _uiState.update { it.copy(dateRange = range, customStartDate = start, customEndDate = end) }
        triggerFilter()
    }

    private var filterJob: kotlinx.coroutines.Job? = null
    private fun triggerFilter() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            // Give a tiny delay to debounce rapid updates (e.g. multi-stat updates)
            kotlinx.coroutines.delay(16)
            applyFilters()
            applyPersonFilters()
        }
    }

    fun normalizePhoneNumber(number: String) = callDataRepository.normalizePhoneNumber(number)

    private fun applyFilters() {
        val current = _uiState.value
        if (current.callLogs.isEmpty()) {
            _uiState.update { it.copy(filteredLogs = emptyList()) }
            return
        }

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
                if (person?.label != labelFilter) return@filter false
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
        
        _uiState.update { it.copy(filteredLogs = filtered) }
    }

    private fun applyPersonFilters() {
        val current = _uiState.value
        val query = current.searchQuery.lowercase()
        
        val filtered = if (query.isEmpty()) {
            if (current.labelFilter.isNotEmpty()) {
                current.persons.filter { it.label == current.labelFilter }
            } else {
                current.persons
            }
        } else {
            current.persons.filter { person ->
                val matchesQuery = (person.phoneNumber.contains(query) ||
                (person.contactName?.lowercase()?.contains(query) == true) ||
                (person.personNote?.lowercase()?.contains(query) == true))
                
                val matchesLabel = if (current.labelFilter.isNotEmpty()) person.label == current.labelFilter else true
                
                matchesQuery && matchesLabel
            }
        }
        
        _uiState.update { it.copy(filteredPersons = filtered) }
    }

    // ============================================
    // NOTES
    // ============================================
    
    fun saveCallNote(compositeId: String, note: String) {
        viewModelScope.launch {
            callDataRepository.updateCallNote(compositeId, note.ifEmpty { null })
            syncNow()
        }
    }

    fun savePersonNote(phoneNumber: String, note: String) {
        viewModelScope.launch {
            callDataRepository.updatePersonNote(phoneNumber, note.ifEmpty { null })
            syncNow()
        }
    }
    
    fun savePersonLabel(phoneNumber: String, label: String) {
        viewModelScope.launch {
            callDataRepository.updatePersonLabel(phoneNumber, label.ifEmpty { null })
            syncNow()
        }
    }

    fun savePersonName(phoneNumber: String, name: String) {
        viewModelScope.launch {
            callDataRepository.updatePersonName(phoneNumber, name.ifEmpty { null })
            syncNow()
        }
    }
    
    fun updateReviewed(compositeId: String, reviewed: Boolean) {
        viewModelScope.launch {
            callDataRepository.updateReviewed(compositeId, reviewed)
            syncNow()
        }
    }

    fun markAllCallsReviewed(phoneNumber: String) {
        viewModelScope.launch {
            callDataRepository.markAllCallsReviewed(phoneNumber)
            syncNow()
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
            syncNow()
        }
    }

    fun syncNow() {
        val application = getApplication<android.app.Application>()
        com.calltracker.manager.worker.CallSyncWorker.runNow(application)
        com.calltracker.manager.worker.RecordingUploadWorker.runNow(application)
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

    private fun refreshSettings() {
        val simSelection = settingsRepository.getSimSelection()
        val trackStartDate = settingsRepository.getTrackStartDate()
        val whatsappPreference = settingsRepository.getWhatsappPreference()
        val orgId = settingsRepository.getOrganisationId()
        val isSyncSetup = orgId.isNotEmpty()
        val allowPersonalExclusion = settingsRepository.isAllowPersonalExclusion()
        
        if (simSelection != _uiState.value.simSelection || 
            trackStartDate != _uiState.value.trackStartDate ||
            whatsappPreference != _uiState.value.whatsappPreference ||
            isSyncSetup != _uiState.value.isSyncSetup ||
            allowPersonalExclusion != _uiState.value.allowPersonalExclusion) {
            _uiState.update { it.copy(
                simSelection = simSelection, 
                trackStartDate = trackStartDate,
                whatsappPreference = whatsappPreference,
                isSyncSetup = isSyncSetup,
                allowPersonalExclusion = allowPersonalExclusion
            ) }
            applyFilters()
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
