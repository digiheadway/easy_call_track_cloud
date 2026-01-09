package com.miniclick.calltrackmanage.ui.home.state

import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.ui.home.viewmodel.CallTabFilter
import com.miniclick.calltrackmanage.ui.home.viewmodel.DateSectionSummary
import com.miniclick.calltrackmanage.ui.home.PersonGroup
import com.miniclick.calltrackmanage.ui.home.viewmodel.PersonTabFilter
import com.miniclick.calltrackmanage.ui.home.viewmodel.ReportStats
import com.miniclick.calltrackmanage.ui.settings.AppInfo
import com.miniclick.calltrackmanage.ui.settings.SimInfo

/**
 * Data state containing raw data from repositories.
 * This is the source data before filtering.
 */
data class DataState(
    val callLogs: List<CallDataEntity> = emptyList(),
    val persons: List<PersonDataEntity> = emptyList(),
    val recordings: Map<String, String> = emptyMap(), // compositeId -> Path cache
    val currentRecordingPath: String = ""
)

/**
 * Filtered/processed data ready for display.
 * This is computed from DataState based on current filters.
 */
data class FilteredDataState(
    val filteredLogs: List<CallDataEntity> = emptyList(),
    val filteredPersons: List<PersonDataEntity> = emptyList(),
    val tabFilteredLogs: Map<CallTabFilter, List<CallDataEntity>> = emptyMap(),
    val tabFilteredPersons: Map<PersonTabFilter, List<PersonDataEntity>> = emptyMap(),
    val personGroups: Map<String, PersonGroup> = emptyMap(),
    val callTypeCounts: Map<CallTabFilter, Int> = emptyMap(),
    val personTypeCounts: Map<PersonTabFilter, Int> = emptyMap(),
    val dateSummaries: List<DateSectionSummary> = emptyList()
)

/**
 * Sync-related state for tracking pending operations.
 */
data class SyncState(
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0,
    val pendingMetadataCount: Int = 0,
    val pendingRecordingCount: Int = 0,
    val pendingNewCallsCount: Int = 0,
    val pendingMetadataUpdatesCount: Int = 0,
    val pendingPersonUpdatesCount: Int = 0,
    val activeRecordings: List<CallDataEntity> = emptyList(),
    val isNetworkAvailable: Boolean = true,
    val isSyncSetup: Boolean = false
)

/**
 * Settings-related state that affects behavior.
 */
data class SettingsState(
    val simSelection: String = "Both",
    val trackStartDate: Long = 0,
    val whatsappPreference: String = "Always Ask",
    val callRecordEnabled: Boolean = true,
    val callActionBehavior: String = "Direct",
    val allowPersonalExclusion: Boolean = false,
    val sim1SubId: Int? = null,
    val sim2SubId: Int? = null,
    val callerPhoneSim1: String = "",
    val callerPhoneSim2: String = "",
    val availableSims: List<SimInfo> = emptyList(),
    val availableWhatsappApps: List<AppInfo> = emptyList()
)

/**
 * UI interaction state (dialogs, pickers, transient UI state).
 */
data class InteractionState(
    // Call Flow (SIM Picker)
    val showCallSimPicker: Boolean = false,
    val callFlowNumber: String? = null,
    
    // WhatsApp Flow
    val showWhatsappSelectionDialog: Boolean = false,
    val whatsappTargetNumber: String? = null,
    
    // Search
    val searchHistory: List<String> = emptyList()
)
