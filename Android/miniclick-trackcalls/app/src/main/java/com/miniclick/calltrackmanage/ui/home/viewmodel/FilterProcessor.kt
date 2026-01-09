package com.miniclick.calltrackmanage.ui.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.ui.home.*
import com.miniclick.calltrackmanage.util.network.NetworkConnectivityObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Handles all filtering logic extracted from HomeViewModel.
 * This reduces HomeViewModel size and isolates filter-related concerns.
 * 
 * Responsibilities:
 * - Process call filters across all tabs
 * - Process person filters across all tabs  
 * - Calculate filter counts
 * - Date range calculations
 * - PersonGroup generation
 */
class FilterProcessor(
    private val settingsRepository: SettingsRepository
) {
    // Cache for normalized numbers to avoid repeated heavy library calls
    private val normalizationCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    
    /**
     * Process call filters across ALL tabs in a single pass.
     * Returns a map of CallTabFilter -> List of matching calls.
     */
    fun processCallFilters(
        currentState: HomeUiState,
        personsMap: Map<String, PersonDataEntity>,
        normalizePhone: (String) -> String
    ): Map<CallTabFilter, List<CallDataEntity>> {
        if (currentState.callLogs.isEmpty()) return emptyMap()

        val query = currentState.searchQuery.trim().lowercase()
        val hasQuery = query.isNotEmpty()
        val labelFilter = currentState.labelFilter
        val hasLabelFilter = labelFilter.isNotEmpty()
        
        val connFilter = currentState.connectedFilter
        val nFilter = currentState.notesFilter
        val pnFilter = currentState.personNotesFilter
        val cFilter = currentState.contactsFilter
        val aFilter = currentState.attendedFilter
        val rFilter = currentState.reviewedFilter
        val cnFilter = currentState.customNameFilter
        val dRange = currentState.dateRange
        
        val todayStart = getStartOfDay(0)
        val threeDaysStart = getStartOfDay(2)
        val sevenDaysStart = getStartOfDay(6)
        val fourteenDaysStart = getStartOfDay(13)
        val thirtyDaysStart = getStartOfDay(29)
        val thisMonthStart = getStartOfThisMonth()
        val prevMonthStart = getStartOfPreviousMonth()
        
        val cStart = currentState.customStartDate ?: 0L
        val cEnd = currentState.customEndDate ?: Long.MAX_VALUE
        val tStartDate = currentState.trackStartDate
        val simSel = currentState.simSelection
        val targetSubId = if (simSel == "Both") null else getSubIdForSim(simSel, currentState)

        val tabMaps = java.util.EnumMap<CallTabFilter, ArrayList<CallDataEntity>>(CallTabFilter::class.java)
        CallTabFilter.entries.forEach { tabMaps[it] = ArrayList() }

        for (call in currentState.callLogs) {
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
                val norm = getCachedNormalizedNumber(call.phoneNumber, normalizePhone)
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

            if (currentState.showIgnoredOnly) {
                if (!isIgnored) continue
                
                tabMaps[CallTabFilter.ALL]?.add(call)
                tabMaps[CallTabFilter.IGNORED]?.add(call)
                
                if ((person?.totalCalls ?: 0) > 1 && (person?.totalDuration ?: 0L) == 0L) {
                    tabMaps[CallTabFilter.NEVER_CONNECTED]?.add(call)
                }
                
                if (call.duration > 0 && call.duration <= 4) {
                    tabMaps[CallTabFilter.MAY_FAILED]?.add(call)
                }

                categorizeCallByType(call, tabMaps)
            } else {
                if (isIgnored) {
                    tabMaps[CallTabFilter.IGNORED]?.add(call)
                } else {
                    tabMaps[CallTabFilter.ALL]?.add(call)
                    
                    if ((person?.totalCalls ?: 0) > 1 && (person?.totalDuration ?: 0L) == 0L) {
                        tabMaps[CallTabFilter.NEVER_CONNECTED]?.add(call)
                    }
                    
                    if (call.duration > 0 && call.duration <= 4) {
                        tabMaps[CallTabFilter.MAY_FAILED]?.add(call)
                    }

                    categorizeCallByType(call, tabMaps)
                }
            }
        }
        
        // Final Sorting
        sortCallTabs(tabMaps, currentState)
        
        return tabMaps
    }

    private fun categorizeCallByType(call: CallDataEntity, tabMaps: java.util.EnumMap<CallTabFilter, ArrayList<CallDataEntity>>) {
        when (call.callType) {
            android.provider.CallLog.Calls.INCOMING_TYPE -> {
                if (call.duration > 3) {
                    tabMaps[CallTabFilter.ANSWERED]?.add(call)
                } else {
                    tabMaps[CallTabFilter.NOT_ANSWERED]?.add(call)
                }
            }
            android.provider.CallLog.Calls.OUTGOING_TYPE -> {
                if (call.duration > 3) {
                    tabMaps[CallTabFilter.OUTGOING]?.add(call)
                } else {
                    tabMaps[CallTabFilter.OUTGOING_NOT_CONNECTED]?.add(call)
                }
            }
            android.provider.CallLog.Calls.MISSED_TYPE, 
            android.provider.CallLog.Calls.REJECTED_TYPE, 
            5 -> { 
                tabMaps[CallTabFilter.NOT_ANSWERED]?.add(call)
            }
        }
    }

    private fun sortCallTabs(
        tabMaps: java.util.EnumMap<CallTabFilter, ArrayList<CallDataEntity>>,
        state: HomeUiState
    ) {
        val isDesc = state.callSortDirection == SortDirection.DESCENDING
        val sortBy = state.callSortBy
        
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
    }

    /**
     * Process person filters across ALL tabs in a single pass.
     */
    fun processPersonFilters(
        currentState: HomeUiState,
        logsByPhone: Map<String, List<CallDataEntity>>
    ): Map<PersonTabFilter, List<PersonDataEntity>> {
        if (currentState.persons.isEmpty()) return emptyMap()

        val query = currentState.searchQuery.trim().lowercase()
        val hasQuery = query.isNotEmpty()
        val labelFilter = currentState.labelFilter
        val hasLabelFilter = labelFilter.isNotEmpty()
        
        val nFilter = currentState.notesFilter
        val pnFilter = currentState.personNotesFilter
        val cFilter = currentState.contactsFilter
        val minCount = currentState.minCallCount
        val rFilter = currentState.reviewedFilter
        val cnFilter = currentState.customNameFilter

        val tabMaps = java.util.EnumMap<PersonTabFilter, ArrayList<PersonDataEntity>>(PersonTabFilter::class.java)
        PersonTabFilter.entries.forEach { tabMaps[it] = ArrayList() }

        val dRange = currentState.dateRange
        val todayStart = getStartOfDay(0)
        val threeDaysStart = getStartOfDay(2)
        val sevenDaysStart = getStartOfDay(6)
        val fourteenDaysStart = getStartOfDay(13)
        val thirtyDaysStart = getStartOfDay(29)
        val thisMonthStart = getStartOfThisMonth()
        val prevMonthStart = getStartOfPreviousMonth()
        val cStart = currentState.customStartDate ?: 0L
        val cEnd = currentState.customEndDate ?: Long.MAX_VALUE

        for (person in currentState.persons) {
            if (person.isNoTracking) continue

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
                
                if (person.totalCalls > 1 && person.totalDuration == 0L) {
                    tabMaps[PersonTabFilter.NEVER_CONNECTED]?.add(person)
                }
                
                val hasShortCall = pCalls.any { it.duration > 0 && it.duration <= 4 }
                val lastDur = person.lastCallDuration ?: 0L
                if ((lastDur > 0 && lastDur <= 4) || hasShortCall) {
                    tabMaps[PersonTabFilter.MAY_FAILED]?.add(person)
                }
                
                categorizePersonByLastCall(person, tabMaps)
            }
        }
        
        sortPersonTabs(tabMaps, currentState)
        
        return tabMaps
    }

    private fun categorizePersonByLastCall(
        person: PersonDataEntity,
        tabMaps: java.util.EnumMap<PersonTabFilter, ArrayList<PersonDataEntity>>
    ) {
        val lastType = person.lastCallType
        val lastDur = person.lastCallDuration ?: 0L
        
        when (lastType) {
            android.provider.CallLog.Calls.INCOMING_TYPE -> {
                if (lastDur > 3) {
                    tabMaps[PersonTabFilter.ANSWERED]?.add(person)
                } else {
                    tabMaps[PersonTabFilter.NOT_ANSWERED]?.add(person)
                }
            }
            android.provider.CallLog.Calls.OUTGOING_TYPE -> {
                if (lastDur > 3) {
                    tabMaps[PersonTabFilter.OUTGOING]?.add(person)
                } else {
                    tabMaps[PersonTabFilter.OUTGOING_NOT_CONNECTED]?.add(person)
                }
            }
            android.provider.CallLog.Calls.MISSED_TYPE,
            android.provider.CallLog.Calls.REJECTED_TYPE,
            5 -> {
                tabMaps[PersonTabFilter.NOT_ANSWERED]?.add(person)
            }
        }
    }

    private fun sortPersonTabs(
        tabMaps: java.util.EnumMap<PersonTabFilter, ArrayList<PersonDataEntity>>,
        state: HomeUiState
    ) {
        val isDesc = state.personSortDirection == SortDirection.DESCENDING
        val sortBy = state.personSortBy
        
        tabMaps.values.forEach { list ->
            if (list.isNotEmpty()) {
                when (sortBy) {
                    PersonSortBy.LAST_CALL -> {
                        if (isDesc) list.sortByDescending { it.lastCallDate }
                        else list.sortBy { it.lastCallDate }
                    }
                    PersonSortBy.TOTAL_CALLS -> {
                        if (isDesc) list.sortByDescending { it.totalCalls }
                        else list.sortBy { it.totalCalls }
                    }
                    PersonSortBy.TOTAL_DURATION -> {
                        if (isDesc) list.sortByDescending { it.totalDuration }
                        else list.sortBy { it.totalDuration }
                    }
                    PersonSortBy.NAME -> {
                        val comparator = compareBy<PersonDataEntity> { it.contactName ?: it.phoneNumber }
                        if (isDesc) list.sortWith(comparator.reversed())
                        else list.sortWith(comparator)
                    }
                }
            }
        }
    }

    /**
     * Generate PersonGroups from filtered logs.
     */
    fun generatePersonGroups(
        logs: List<CallDataEntity>,
        personsMap: Map<String, PersonDataEntity>,
        normalizePhone: (String) -> String
    ): Map<String, PersonGroup> {
        return logs.groupBy { it.phoneNumber }.mapValues { (number, calls) ->
            val sortedCalls = calls.sortedByDescending { it.callDate }
            val person = personsMap[number] ?: personsMap[getCachedNormalizedNumber(number, normalizePhone)]
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
    }

    fun calculateFilterCount(state: HomeUiState): Int {
        var count = 0
        if (state.connectedFilter != ConnectedFilter.ALL) count++
        if (state.notesFilter != NotesFilter.ALL) count++
        if (state.personNotesFilter != PersonNotesFilter.ALL) count++
        if (state.contactsFilter != ContactsFilter.ALL) count++
        if (state.reviewedFilter != ReviewedFilter.ALL) count++
        if (state.customNameFilter != CustomNameFilter.ALL) count++
        if (state.minCallCount > 0) count++
        if (state.labelFilter.isNotEmpty()) count++
        return count
    }

    // ==================== DATE UTILITIES ====================
    
    fun getStartOfDay(daysAgo: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getStartOfThisMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun getStartOfPreviousMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.MONTH, -1)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getSubIdForSim(simName: String, state: HomeUiState): Int? {
        if (simName == "Both") return null
        return when (simName) {
            "Sim1" -> state.sim1SubId
            "Sim2" -> state.sim2SubId
            else -> null
        }
    }

    private fun getCachedNormalizedNumber(number: String, normalizePhone: (String) -> String): String {
        return normalizationCache.getOrPut(number) { normalizePhone(number) }
    }
}
