package com.miniclick.calltrackmanage.ui.home.viewmodel

import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.ui.home.viewmodel.HomeUtils
import java.util.Calendar

object CallLogManager {
    fun processCallFilters(
        current: HomeUiState,
        personsMap: Map<String, PersonDataEntity>,
        normalizer: (String) -> String,
        targetSubId: Int?
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
        
        val todayStart = HomeUtils.getStartOfDay(0)
        val threeDaysStart = HomeUtils.getStartOfDay(2)
        val sevenDaysStart = HomeUtils.getStartOfDay(6)
        val fourteenDaysStart = HomeUtils.getStartOfDay(13)
        val thirtyDaysStart = HomeUtils.getStartOfDay(29)
        val thisMonthStart = HomeUtils.getStartOfThisMonth()
        val prevMonthStart = HomeUtils.getStartOfPreviousMonth()
        
        val cStart = current.customStartDate
        val cEnd = current.customEndDate
        val tStartDate = current.trackStartDate


        val tabMaps = java.util.EnumMap<CallTabFilter, ArrayList<CallDataEntity>>(CallTabFilter::class.java)
        CallTabFilter.values().forEach { tabMaps[it] = ArrayList<CallDataEntity>() }

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
                DateRange.CUSTOM -> {
                    val start = cStart
                    val end = cEnd
                    if (start != null && end != null) call.callDate in start..end else true
                }
                DateRange.ALL -> true
            }
            if (!matchesDate) continue

            var person = personsMap[call.phoneNumber]
            if (person == null) {
                val norm = normalizer(call.phoneNumber)
                person = personsMap[norm]
            }
            
            // Refactoring note: We'll assume for now personsMap can handle bare numbers or exact match.
            // To properly fix, we should pass the normalization function or interface using it.
            
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

            // Filter based on showIgnoredOnly toggle
            if (current.showIgnoredOnly) {
                // Only include ignored calls across all tabs
                if (!isIgnored) continue
                
                // Add to ALL and categorize
                tabMaps[CallTabFilter.ALL]?.add(call)
                tabMaps[CallTabFilter.IGNORED]?.add(call)
                
                // New logic for NEVER_CONNECTED: person has multiple calls but total duration is 0
                if ((person?.totalCalls ?: 0) > 1 && (person?.totalDuration ?: 0L) == 0L) {
                    tabMaps[CallTabFilter.NEVER_CONNECTED]?.add(call)
                }
                
                // New logic for MAY_FAILED: call duration > 0 && <= 4 seconds
                if (call.duration > 0 && call.duration <= 4) {
                    tabMaps[CallTabFilter.MAY_FAILED]?.add(call)
                }

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
            } else {
                // Normal mode: Ignored goes to IGNORED tab, everything else categorized normally
                if (isIgnored) {
                    tabMaps[CallTabFilter.IGNORED]?.add(call)
                } else {
                    tabMaps[CallTabFilter.ALL]?.add(call)
                    
                    // New logic for NEVER_CONNECTED: person has multiple calls but total duration is 0
                    if ((person?.totalCalls ?: 0) > 1 && (person?.totalDuration ?: 0L) == 0L) {
                        tabMaps[CallTabFilter.NEVER_CONNECTED]?.add(call)
                    }
                    
                    // New logic for MAY_FAILED: call duration > 0 && <= 4 seconds
                    if (call.duration > 0 && call.duration <= 4) {
                        tabMaps[CallTabFilter.MAY_FAILED]?.add(call)
                    }

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
}
