package com.miniclick.calltrackmanage.ui.home.viewmodel

import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.ui.home.PersonGroup
import com.miniclick.calltrackmanage.ui.settings.SimInfo
import com.miniclick.calltrackmanage.ui.settings.AppInfo

// Filter enums for chip dropdowns
enum class CallTabFilter { ALL, ANSWERED, NOT_ANSWERED, OUTGOING, OUTGOING_NOT_CONNECTED, NEVER_CONNECTED, MAY_FAILED, IGNORED }
enum class PersonTabFilter { ALL, ANSWERED, NOT_ANSWERED, OUTGOING, OUTGOING_NOT_CONNECTED, NEVER_CONNECTED, MAY_FAILED, IGNORED }
enum class ConnectedFilter { ALL, CONNECTED, NOT_CONNECTED }
enum class NotesFilter { ALL, WITH_NOTE, WITHOUT_NOTE }
enum class PersonNotesFilter { ALL, WITH_NOTE, WITHOUT_NOTE }
enum class ContactsFilter { ALL, IN_CONTACTS, NOT_IN_CONTACTS }
enum class AttendedFilter { ALL, ATTENDED, NEVER_ATTENDED }
enum class ReviewedFilter { ALL, REVIEWED, NOT_REVIEWED }
enum class CustomNameFilter { ALL, WITH_NAME, WITHOUT_NAME }
enum class ViewMode { CALLS, PERSONS }
enum class DateRange { TODAY, LAST_3_DAYS, LAST_7_DAYS, LAST_14_DAYS, LAST_30_DAYS, THIS_MONTH, PREVIOUS_MONTH, CUSTOM, ALL }
enum class PersonSortBy { LAST_CALL, TOTAL_CALLS, TOTAL_DURATION, NAME }
enum class CallSortBy { DATE, DURATION, NUMBER }
enum class SortDirection { ASCENDING, DESCENDING }
enum class ReportCategory { OVERVIEW, DAILY_AVERAGE, DATA_ANALYSIS, FREQUENCY }

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
    val viewMode: ViewMode = ViewMode.PERSONS,
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

    // WhatsApp Flow
    val showWhatsappSelectionDialog: Boolean = false,
    val whatsappTargetNumber: String? = null,

    // Reports Stats
    val reportStats: ReportStats = ReportStats(),

    // Search History
    val searchHistory: List<String> = emptyList(),
    
    // Dialer Settings
    val callActionBehavior: String = "Direct",
    val availableWhatsappApps: List<AppInfo> = emptyList(),
    val reportCategory: ReportCategory = ReportCategory.OVERVIEW,
    val showComparisons: Boolean = false,
    val showIgnoredOnly: Boolean = false,
    val dateSummaries: List<DateSectionSummary> = emptyList(),
    val visibleCallFilters: List<CallTabFilter> = CallTabFilter.entries.filter { it != CallTabFilter.IGNORED },
    val visiblePersonFilters: List<PersonTabFilter> = PersonTabFilter.entries.filter { it != PersonTabFilter.IGNORED }
)

// Extension to determine if a call status counts as "Attended" or "Responded" based on the new > 3s rule
fun CallDataEntity.isStatusConnected(): Boolean = duration > 3

data class ReportStats(
    val totalCalls: Int = 0,
    val incomingCalls: Int = 0,
    val outgoingCalls: Int = 0,
    val missedCalls: Int = 0,
    val rejectedCalls: Int = 0,
    
    // Connected
    val connectedCalls: Int = 0,
    val connectedUnique: Int = 0,
    val notConnectedCalls: Int = 0,
    val notConnectedUnique: Int = 0,
    
    // Specific Types (Total | Unique)
    val connectedIncoming: Int = 0,
    val connectedIncomingUnique: Int = 0,
    val connectedOutgoing: Int = 0,
    val connectedOutgoingUnique: Int = 0,
    val notAnsweredIncoming: Int = 0,
    val notAnsweredIncomingUnique: Int = 0,
    val outgoingNotConnected: Int = 0,
    val outgoingNotConnectedUnique: Int = 0,
    
    // Durations
    val totalDuration: Long = 0,
    val avgDuration: Long = 0, // of connected calls
    val maxDuration: Long = 0,
    val incomingDuration: Long = 0,
    val outgoingDuration: Long = 0,
    
    // Contacts
    val uniqueContacts: Int = 0,
    val savedContacts: Int = 0,
    val unsavedContacts: Int = 0,
    
    // Notes & Activity
    val callsWithNotes: Int = 0,
    val reviewedCalls: Int = 0,
    val callsWithRecordings: Int = 0,
    val personsWithNotes: Int = 0,
    val personsWithLabels: Int = 0,
    val totalPersons: Int = 0, // For percentages
    
    // Special
    val neverConnectedCount: Int = 0,
    val neverConnectedUnique: Int = 0,
    val mayFailedCount: Int = 0,
    val mayFailedUnique: Int = 0,
    
    // Engagement
    val durationBuckets: List<DurationBucketStat> = emptyList(),
    
    // Comparisons (Daily Averages for Last 7 Days)
    val comparisonAverages: Map<String, Float> = emptyMap(),
    
    val topCallers: List<TopCaller> = emptyList(),
    val mostTalked: List<TopCaller> = emptyList(),
    val dailyStats: Map<String, DayStat> = emptyMap(),
    val hourlyStats: Map<Int, Int> = emptyMap(),
    val labelDistribution: List<Pair<String, Int>> = emptyList()
)

data class DurationBucketStat(
    val label: String,
    val total: Int,
    val outCount: Int,
    val inCount: Int,
    val sortOrder: Int
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

data class DateSectionSummary(
    val dateLabel: String,
    val dayOfWeek: String,
    val totalCalls: Int,
    val uniqueCalls: Int,
    val dateMillis: Long,
    val month: String,
    val year: Int
)
