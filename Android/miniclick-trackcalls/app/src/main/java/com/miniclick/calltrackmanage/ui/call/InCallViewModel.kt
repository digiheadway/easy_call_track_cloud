package com.miniclick.calltrackmanage.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.service.CallTrackInCallService
import com.miniclick.calltrackmanage.ui.home.PersonGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class InCallViewModel @Inject constructor(
    private val callDataRepository: CallDataRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _callerPersonGroup = MutableStateFlow<PersonGroup?>(null)
    val callerPersonGroup: StateFlow<PersonGroup?> = _callerPersonGroup.asStateFlow()

    private val _availableLabels = MutableStateFlow<List<String>>(emptyList())
    val availableLabels: StateFlow<List<String>> = _availableLabels.asStateFlow()

    val callRecordEnabled: Boolean
        get() = settingsRepository.isCallRecordEnabled()

    init {
        Log.d("InCallViewModel", "Initializing InCallViewModel")
        
        // Observe call status to fetch specific person data
        viewModelScope.launch {
            CallTrackInCallService.callStatus.collect { status ->
                if (status != null) {
                    // Normalize and check if we already have this data to avoid redundant DB hits
                    val normalized = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        callDataRepository.normalizePhoneNumber(status.phoneNumber)
                    }
                    if (_callerPersonGroup.value?.number != normalized) {
                        loadCallerData(status.phoneNumber, status.contactName)
                    }
                } else {
                    _callerPersonGroup.value = null
                }
            }
        }

        // PERFORMANCE: Defer label loading - only load when actually needed
        // Labels are loaded on-demand when the user opens the label picker dialog
    }
    
    // Track if labels have been loaded to avoid redundant DB queries
    private var labelsLoaded = false
    
    /**
     * Load labels on-demand when the user wants to edit labels.
     * Called from InCallActivity when label picker dialog is shown.
     */
    fun loadLabelsIfNeeded() {
        if (labelsLoaded) return
        labelsLoaded = true
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val labels = callDataRepository.getDistinctLabels()
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                _availableLabels.value = labels
            }
        }
    }

    /**
     * PERFORMANCE: Load only recent calls (limit 10) to avoid loading entire call history.
     * The in-call screen only displays the last 3 calls anyway.
     */
    private suspend fun loadCallerData(phoneNumber: String, systemName: String?) = withContext(kotlinx.coroutines.Dispatchers.IO) {
        val loadStart = System.currentTimeMillis()
        val normalized = callDataRepository.normalizePhoneNumber(phoneNumber)
        val person = callDataRepository.getPersonByNumber(normalized)
        
        // OPTIMIZATION: Only fetch last 10 calls instead of entire history
        val logs = callDataRepository.getRecentLogsForPhone(normalized, limit = 10)
        
        // Already sorted by the DB query (DESC), no need to sort again
        
        withContext(kotlinx.coroutines.Dispatchers.Main) {
            _callerPersonGroup.value = PersonGroup(
                number = normalized,
                name = person?.contactName ?: systemName,
                photoUri = person?.photoUri,
                calls = logs,
                lastCallDate = logs.firstOrNull()?.callDate ?: 0L,
                totalDuration = logs.sumOf { it.duration.toLong() },
                incomingCount = logs.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                outgoingCount = logs.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                missedCount = logs.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE },
                personNote = person?.personNote,
                label = person?.label
            )
            Log.d("InCallViewModel", "Loaded caller data in ${System.currentTimeMillis() - loadStart}ms")
        }
    }

    fun saveCallNote(compositeId: String, note: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            callDataRepository.updateCallNote(compositeId, note)
            // No need to reload entire history, just update local state if matches
            // (Note: InCall UI currently shows personNote mostly, call notes are in history)
        }
    }

    fun savePersonName(phoneNumber: String, name: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            callDataRepository.updatePersonName(phoneNumber, name)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                _callerPersonGroup.update { it?.copy(name = name) }
            }
        }
    }

    fun savePersonLabel(phoneNumber: String, label: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            callDataRepository.updatePersonLabel(phoneNumber, label)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                _callerPersonGroup.update { it?.copy(label = label) }
            }
        }
    }

    fun savePersonNote(phoneNumber: String, note: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            callDataRepository.updatePersonNote(phoneNumber, note)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                _callerPersonGroup.update { it?.copy(personNote = note) }
            }
        }
    }

    suspend fun getRecordingForLog(log: CallDataEntity) {
        // In-call we probably don't need to trigger a full rescan, 
        // but we can if desired. For now, just a placeholder.
    }
}
