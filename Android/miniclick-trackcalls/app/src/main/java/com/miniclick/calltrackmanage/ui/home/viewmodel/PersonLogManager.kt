package com.miniclick.calltrackmanage.ui.home.viewmodel

import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.PersonDataEntity

object PersonLogManager {
    fun processPersonFilters(
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
        PersonTabFilter.values().forEach { tabMaps[it] = ArrayList<PersonDataEntity>() }

        val dRange = current.dateRange
        val todayStart = HomeUtils.getStartOfDay(0)
        val threeDaysStart = HomeUtils.getStartOfDay(2)
        val sevenDaysStart = HomeUtils.getStartOfDay(6)
        val fourteenDaysStart = HomeUtils.getStartOfDay(13)
        val thirtyDaysStart = HomeUtils.getStartOfDay(29)
        val thisMonthStart = HomeUtils.getStartOfThisMonth()
        val prevMonthStart = HomeUtils.getStartOfPreviousMonth()
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
                
                // New logic for NEVER_CONNECTED: person has multiple calls but total duration is 0
                if (person.totalCalls > 1 && person.totalDuration == 0L) {
                    tabMaps[PersonTabFilter.NEVER_CONNECTED]?.add(person)
                }
                
                // New logic for MAY_FAILED: last call duration in 1..4 OR has any call in 1..4 range
                val hasShortCall = pCalls.any { it.duration > 0 && it.duration <= 4 }
                val lastDur = person.lastCallDuration ?: 0L
                if ((lastDur > 0 && lastDur <= 4) || hasShortCall) {
                    tabMaps[PersonTabFilter.MAY_FAILED]?.add(person)
                }
                // Categorize based on LAST call status (>3s rule as per request)
                val lastType = person.lastCallType
                
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
        
        return tabMaps
    }
}
