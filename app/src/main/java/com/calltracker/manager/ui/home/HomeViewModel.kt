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

    val dateRange: DateRange = DateRange.LAST_3_DAYS,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,

    val simSelection: String = "Both",
    val trackStartDate: Long = 0,
    val whatsappPreference: String = "Always Ask"
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val callDataRepository = CallDataRepository(application)

    private val recordingRepository = RecordingRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshSettings()
        loadRecordingPath()
        loadDataFromRoom()
        
        // Observe Room DB for real-time updates
        viewModelScope.launch {
            callDataRepository.getAllCallsFlow().collect { calls ->
                _uiState.update { it.copy(callLogs = calls) }
                applyFilters()
            }
        }
        
        viewModelScope.launch {
            callDataRepository.getAllPersonsFlow().collect { persons ->
                _uiState.update { it.copy(persons = persons) }
                applyPersonFilters()
            }
        }
        
        // Sync with system call log in background
        syncFromSystem()
    }

    private fun loadRecordingPath() {
        _uiState.update { it.copy(currentRecordingPath = recordingRepository.getRecordingPath()) }
    }

    fun updateRecordingPath(path: String) {
        recordingRepository.setRecordingPath(path)
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
            val path = recordingRepository.findRecording(call.callDate, call.duration, call.phoneNumber)
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

    fun setDateRange(range: DateRange, start: Long? = null, end: Long? = null) {
        _uiState.update { it.copy(dateRange = range, customStartDate = start, customEndDate = end) }
        applyFilters()
    }

    private fun applyFilters() {
        val current = _uiState.value
        val query = current.searchQuery.lowercase()
        
            // SIM Filter
            val targetSubId = if (current.simSelection == "Both") null else getSubIdForSim(current.simSelection)
            
            val filtered = current.callLogs.filter { call ->
                // Call Type Filter
                val typeMatch = when (current.callTypeFilter) {
                    CallTypeFilter.ALL -> true
                    CallTypeFilter.INCOMING -> call.callType == CallLog.Calls.INCOMING_TYPE
                    CallTypeFilter.OUTGOING -> call.callType == CallLog.Calls.OUTGOING_TYPE
                    CallTypeFilter.MISSED -> call.callType == CallLog.Calls.MISSED_TYPE
                    CallTypeFilter.REJECTED -> call.callType == CallLog.Calls.REJECTED_TYPE
                }
                
                // Connected Filter
                val connectedMatch = when (current.connectedFilter) {
                    ConnectedFilter.ALL -> true
                    ConnectedFilter.CONNECTED -> call.duration > 0
                    ConnectedFilter.NOT_CONNECTED -> call.duration <= 0
                }
                
                // Notes Filter
                val notesMatch = when (current.notesFilter) {
                    NotesFilter.ALL -> true
                    NotesFilter.WITH_NOTE -> !call.callNote.isNullOrEmpty()
                    NotesFilter.WITHOUT_NOTE -> call.callNote.isNullOrEmpty()
                }
                
                // Contacts Filter
                val contactsMatch = when (current.contactsFilter) {
                    ContactsFilter.ALL -> true
                    ContactsFilter.IN_CONTACTS -> !call.contactName.isNullOrEmpty()
                    ContactsFilter.NOT_IN_CONTACTS -> call.contactName.isNullOrEmpty()
                }
                
                // Attended Filter
                val attendedMatch = when (current.attendedFilter) {
                    AttendedFilter.ALL -> true
                    AttendedFilter.ATTENDED -> call.duration > 0
                    AttendedFilter.NEVER_ATTENDED -> call.duration <= 0
                }

                // Search Filter
                val searchMatch = if (query.isEmpty()) true else {
                    call.phoneNumber.contains(query) || 
                    (call.contactName?.lowercase()?.contains(query) == true) ||
                    (call.callNote?.lowercase()?.contains(query) == true)
                }

                // SIM Filter logic
                val simMatch = if (current.simSelection == "Both") true else {
                    if (targetSubId != null) {
                        call.subscriptionId == targetSubId
                    } else {
                        false
                    }
                }

                // Date Range Filter
                val dateRangeMatch = when (current.dateRange) {
                    DateRange.TODAY -> call.callDate >= getStartOfDay(0)
                    DateRange.LAST_3_DAYS -> call.callDate >= getStartOfDay(2)
                    DateRange.LAST_7_DAYS -> call.callDate >= getStartOfDay(6)
                    DateRange.LAST_14_DAYS -> call.callDate >= getStartOfDay(13)
                    DateRange.LAST_30_DAYS -> call.callDate >= getStartOfDay(29)
                    DateRange.THIS_MONTH -> call.callDate >= getStartOfThisMonth()
                    DateRange.PREVIOUS_MONTH -> {
                        val start = getStartOfPreviousMonth()
                        val end = getStartOfThisMonth()
                        call.callDate in start until end
                    }
                    DateRange.CUSTOM -> {
                        val start = current.customStartDate ?: 0L
                        val end = (current.customEndDate ?: Long.MAX_VALUE) 
                        call.callDate in start..end
                    }
                    DateRange.ALL -> true
                }
                
                // Old Settings Date Filter (keep as additional constraint if needed, or replace)
                val trackDateMatch = if (current.trackStartDate > 0) {
                    call.callDate >= current.trackStartDate
                } else true
                
                typeMatch && connectedMatch && notesMatch && contactsMatch && attendedMatch && searchMatch && simMatch && dateRangeMatch && trackDateMatch
            }
        
        _uiState.update { it.copy(filteredLogs = filtered) }
    }

    private fun applyPersonFilters() {
        val current = _uiState.value
        val query = current.searchQuery.lowercase()
        
        val filtered = if (query.isEmpty()) {
            current.persons
        } else {
            current.persons.filter { person ->
                (person.phoneNumber.contains(query) ||
                (person.contactName?.lowercase()?.contains(query) == true) ||
                (person.personNote?.lowercase()?.contains(query) == true))
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
        }
    }

    fun savePersonNote(phoneNumber: String, note: String) {
        viewModelScope.launch {
            callDataRepository.updatePersonNote(phoneNumber, note.ifEmpty { null })
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
        
        if (simSelection != _uiState.value.simSelection || 
            trackStartDate != _uiState.value.trackStartDate ||
            whatsappPreference != _uiState.value.whatsappPreference) {
            _uiState.update { it.copy(
                simSelection = simSelection, 
                trackStartDate = trackStartDate,
                whatsappPreference = whatsappPreference
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
