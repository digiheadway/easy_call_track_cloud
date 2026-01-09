package com.miniclick.calltrackmanage.ui.home.viewmodel

import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.data.SettingsRepository
import java.util.Calendar
import java.util.Locale

object StatsManager {
    fun calculateReportStats(
        logs: List<CallDataEntity>, 
        persons: List<PersonDataEntity>,
        allLogs: List<CallDataEntity>,
        settingsRepository: SettingsRepository
    ): ReportStats {
        if (logs.isEmpty()) return ReportStats()

        var incomingCalls = 0
        var outgoingCalls = 0
        var missedCalls = 0
        var rejectedCalls = 0
        
        var connectedCalls = 0
        var connectedIncoming = 0
        var connectedOutgoing = 0
        
        var totalDuration = 0L
        var maxDuration = 0L
        var incomingDuration = 0L
        var outgoingDuration = 0L
        
        var callsWithNotes = 0
        var reviewedCalls = 0
        var callsWithRecordings = 0
        var mayFailedCount = 0

        val uniquePhoneNumbers = mutableSetOf<String>()
        val savedContactsPhoneNumbers = mutableSetOf<String>()
        val unsavedContactsPhoneNumbers = mutableSetOf<String>()
        
        val connectedUniqueSet = mutableSetOf<String>()
        val connectedIncomingUniqueSet = mutableSetOf<String>()
        val connectedOutgoingUniqueSet = mutableSetOf<String>()
        val notAnsweredIncomingUniqueSet = mutableSetOf<String>()
        val outgoingNotConnectedUniqueSet = mutableSetOf<String>()
        val mayFailedUniqueSet = mutableSetOf<String>()

        val phoneStatsMap = mutableMapOf<String, TopCaller>()
        val dailyStatsMap = mutableMapOf<String, DayStat>()
        val hourlyStatsMap = mutableMapOf<Int, Int>()
        
        val shortCallThreshold = settingsRepository.getShortCallThresholdSeconds()
        val dateFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val calendar = java.util.Calendar.getInstance()

        // Single pass for most log-based stats
        for (call in logs) {
            val isIncoming = call.callType == android.provider.CallLog.Calls.INCOMING_TYPE
            val isOutgoing = call.callType == android.provider.CallLog.Calls.OUTGOING_TYPE
            val isMissed = call.callType == android.provider.CallLog.Calls.MISSED_TYPE
            val isRejected = call.callType == android.provider.CallLog.Calls.REJECTED_TYPE || call.callType == 5 || call.callType == 6
            val isConnected = call.duration > 3

            if (isIncoming) {
                incomingCalls++
                incomingDuration += call.duration
                if (isConnected) {
                    connectedIncoming++
                    connectedIncomingUniqueSet.add(call.phoneNumber)
                } else {
                    notAnsweredIncomingUniqueSet.add(call.phoneNumber)
                }
            } else if (isOutgoing) {
                outgoingCalls++
                outgoingDuration += call.duration
                if (isConnected) {
                    connectedOutgoing++
                    connectedOutgoingUniqueSet.add(call.phoneNumber)
                } else {
                    outgoingNotConnectedUniqueSet.add(call.phoneNumber)
                }
            } else if (isMissed) {
                missedCalls++
            } else if (isRejected) {
                rejectedCalls++
            }

            if (isConnected) {
                connectedCalls++
                connectedUniqueSet.add(call.phoneNumber)
            }
            
            totalDuration += call.duration
            if (call.duration > maxDuration) maxDuration = call.duration
            if (call.duration > 0 && call.duration <= 4) {
                mayFailedCount++
                mayFailedUniqueSet.add(call.phoneNumber)
            }
            
            if (!call.callNote.isNullOrEmpty()) callsWithNotes++
            if (call.reviewed) reviewedCalls++
            if (!call.localRecordingPath.isNullOrEmpty()) callsWithRecordings++

            uniquePhoneNumbers.add(call.phoneNumber)
            if (call.contactName.isNullOrEmpty()) {
                unsavedContactsPhoneNumbers.add(call.phoneNumber)
            } else {
                savedContactsPhoneNumbers.add(call.phoneNumber)
            }

            // Grouping stats
            val stats = phoneStatsMap.getOrPut(call.phoneNumber) {
                TopCaller(call.phoneNumber, call.contactName ?: call.phoneNumber, 0, 0, 0, 0, 0)
            }
            phoneStatsMap[call.phoneNumber] = stats.copy(
                callCount = stats.callCount + 1,
                totalDuration = stats.totalDuration + call.duration,
                incomingCount = stats.incomingCount + (if (isIncoming) 1 else 0),
                outgoingCount = stats.outgoingCount + (if (isOutgoing) 1 else 0),
                missedCount = stats.missedCount + (if (isMissed) 1 else 0)
            )

            val day = dateFormat.format(java.util.Date(call.callDate))
            val dStat = dailyStatsMap.getOrPut(day) { DayStat(0, 0) }
            dailyStatsMap[day] = dStat.copy(count = dStat.count + 1, duration = dStat.duration + call.duration)

            calendar.timeInMillis = call.callDate
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            hourlyStatsMap[hour] = (hourlyStatsMap[hour] ?: 0) + 1
        }

        // Person-based stats
        var personsWithNotes = 0
        var personsWithLabels = 0
        var neverConnectedUniqueCount = 0
        val visibleNumbers = uniquePhoneNumbers
        val labelsList = mutableListOf<String>()

        for (person in persons) {
            val hasNote = !person.personNote.isNullOrEmpty()
            val hasLabel = !person.label.isNullOrEmpty()
            if (hasNote) personsWithNotes++
            if (hasLabel) {
                personsWithLabels++
                labelsList.addAll(person.label!!.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }
            
            if (person.phoneNumber in visibleNumbers) {
                if (person.totalCalls > 0 && person.totalDuration == 0L) {
                    neverConnectedUniqueCount++
                }
            }
        }

        val labelDistribution = labelsList.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
        
        val topCallers = phoneStatsMap.values.sortedByDescending { it.callCount }.take(5)
        val mostTalked = phoneStatsMap.values.filter { it.totalDuration > 0 }.sortedByDescending { it.totalDuration }.take(5)

        // Engagement Buckets (Optimized: single pass over logs for all buckets might be overkill since there's only 7, 
        // but let's keep it simple and much faster than original)
        val bucketStats = listOf(
            Pair("Over 5 min", 301..Int.MAX_VALUE),
            Pair("3 to 5 min", 181..300),
            Pair("1 to 3 min", 61..180),
            Pair("30s to 1 min", 31..60),
            Pair("10s to 30s", 11..30),
            Pair("5s to 10s", 6..10),
            Pair("0s to 5s", 0..5)
        ).mapIndexed { index, (label, range) ->
            val inBucket = logs.filter { it.duration.toInt() in range }
            DurationBucketStat(
                label = label,
                total = inBucket.size,
                outCount = inBucket.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                inCount = inBucket.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                sortOrder = index
            )
        }

        return ReportStats(
            totalCalls = logs.size,
            incomingCalls = incomingCalls,
            outgoingCalls = outgoingCalls,
            missedCalls = missedCalls,
            rejectedCalls = rejectedCalls,
            connectedCalls = connectedCalls,
            connectedUnique = connectedUniqueSet.size,
            notConnectedCalls = incomingCalls + outgoingCalls - connectedCalls,
            notConnectedUnique = (notAnsweredIncomingUniqueSet + outgoingNotConnectedUniqueSet).size,
            connectedIncoming = connectedIncoming,
            connectedIncomingUnique = connectedIncomingUniqueSet.size,
            connectedOutgoing = connectedOutgoing,
            connectedOutgoingUnique = connectedOutgoingUniqueSet.size,
            notAnsweredIncoming = incomingCalls - connectedIncoming,
            notAnsweredIncomingUnique = notAnsweredIncomingUniqueSet.size,
            outgoingNotConnected = outgoingCalls - connectedOutgoing,
            outgoingNotConnectedUnique = outgoingNotConnectedUniqueSet.size,
            totalDuration = totalDuration,
            avgDuration = if (connectedCalls > 0) totalDuration / connectedCalls else 0L,
            maxDuration = maxDuration,
            incomingDuration = incomingDuration,
            outgoingDuration = outgoingDuration,
            uniqueContacts = uniquePhoneNumbers.size,
            savedContacts = savedContactsPhoneNumbers.size,
            unsavedContacts = unsavedContactsPhoneNumbers.size,
            callsWithNotes = callsWithNotes,
            reviewedCalls = reviewedCalls,
            callsWithRecordings = callsWithRecordings,
            personsWithNotes = personsWithNotes,
            personsWithLabels = personsWithLabels,
            totalPersons = persons.size,
            neverConnectedCount = neverConnectedUniqueCount, // Using unique as representative for count in this simplified version
            neverConnectedUnique = neverConnectedUniqueCount,
            mayFailedCount = mayFailedCount,
            mayFailedUnique = mayFailedUniqueSet.size,
            durationBuckets = bucketStats,
            comparisonAverages = calculateLast7DaysAverages(allLogs),
            topCallers = topCallers,
            mostTalked = mostTalked,
            dailyStats = dailyStatsMap,
            hourlyStats = hourlyStatsMap,
            labelDistribution = labelDistribution
        )
    }

    private fun calculateLast7DaysAverages(allLogs: List<CallDataEntity>): Map<String, Float> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = cal.timeInMillis
        val now = System.currentTimeMillis()
        
        val last7DaysLogs = allLogs.filter { it.callDate in sevenDaysAgo..now }
        val days = 7f
        
        val connected = last7DaysLogs.count { it.duration > 3 }
        val outgoing = last7DaysLogs.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE }
        val answered = last7DaysLogs.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE && it.duration > 3 }
        val outgoingNotConn = last7DaysLogs.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE && it.duration <= 3 }
        val notAnswered = last7DaysLogs.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE && it.duration <= 3 }
        val mayFailed = last7DaysLogs.count { it.duration > 0 && it.duration <= 4 }
        
        return mapOf(
            "connected" to (connected / days),
            "outgoing" to (outgoing / days),
            "answered" to (answered / days),
            "outgoingNotConnected" to (outgoingNotConn / days),
            "notAnswered" to (notAnswered / days),
            "mayFailed" to (mayFailed / days)
        )
    }
}
