package com.miniclick.calltrackmanage.ui.home.viewmodel

import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.ui.home.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles all report statistics calculations extracted from HomeViewModel.
 * This isolates the heavy computation logic for generating reports.
 * 
 * Responsibilities:
 * - Calculate ReportStats from call data
 * - Generate daily/hourly statistics
 * - Calculate duration buckets
 * - Generate top callers/most talked lists
 * - Calculate last 7 days averages for comparisons
 */
class ReportCalculator(
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Calculate comprehensive report statistics from filtered logs.
     */
    fun calculateReportStats(
        logs: List<CallDataEntity>,
        persons: List<PersonDataEntity>
    ): ReportStats {
        if (logs.isEmpty()) return ReportStats()

        // Basic counts
        val totalCalls = logs.size
        val incomingCalls = logs.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE }
        val outgoingCalls = logs.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE }
        val missedCalls = logs.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE }
        val rejectedCalls = logs.count { it.callType == android.provider.CallLog.Calls.REJECTED_TYPE || it.callType == 5 || it.callType == 6 }
        
        // Connected/Attended (> 3s as per new rule)
        val connectedCalls = logs.count { it.duration > 3 }
        val notConnectedCalls = logs.count { it.duration <= 3 }
        val connectedIncoming = logs.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE && it.duration > 3 }
        val connectedOutgoing = logs.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE && it.duration > 3 }
        
        // Duration stats
        val totalDuration = logs.sumOf { it.duration }
        val avgDuration = if (connectedCalls > 0) totalDuration / connectedCalls else 0L
        val maxDuration = logs.maxOfOrNull { it.duration } ?: 0L
        val incomingDuration = logs.filter { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE }.sumOf { it.duration }
        val outgoingDuration = logs.filter { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE }.sumOf { it.duration }
        
        // Unique contacts
        val uniqueContacts = logs.distinctBy { it.phoneNumber }.size
        val savedContacts = logs.filter { !it.contactName.isNullOrEmpty() }.distinctBy { it.phoneNumber }.size
        val unsavedContacts = logs.filter { it.contactName.isNullOrEmpty() }.distinctBy { it.phoneNumber }.size
        
        // Notes & Reviews
        val callsWithNotes = logs.count { !it.callNote.isNullOrEmpty() }
        val reviewedCalls = logs.count { it.reviewed }
        val personsWithNotes = persons.count { !it.personNote.isNullOrEmpty() }
        val personsWithLabels = persons.count { !it.label.isNullOrEmpty() }
        
        // Recordings
        val callsWithRecordings = logs.count { !it.localRecordingPath.isNullOrEmpty() }

        // Short Calls (low engagement indicator)
        val shortCallThreshold = settingsRepository.getShortCallThresholdSeconds()
        val shortCalls = logs.count { it.duration > 0 && it.duration < shortCallThreshold }
        
        // New metrics for renamed tabs
        val neverConnectedCount = persons.count { it.totalCalls > 1 && it.totalDuration == 0L }
        val mayFailedCount = logs.count { it.duration > 0 && it.duration <= 4 }
        
        // Top callers
        val topCallers = calculateTopCallers(logs)
        
        // Most talked (by duration)
        val mostTalked = calculateMostTalked(logs)
        
        // Daily breakdown
        val dailyStats = calculateDailyStats(logs)
        
        // Hourly breakdown (0-23)
        val hourlyStats = calculateHourlyStats(logs)
        
        // Label distribution
        val labelDistribution = calculateLabelDistribution(persons)

        // Engagement Buckets
        val bucketStats = calculateDurationBuckets(logs)

        // Unique counts
        val connectedUniqueCount = logs.filter { it.duration > 3 }.map { it.phoneNumber }.distinct().size
        val notConnectedUniqueCount = logs.filter { it.duration <= 3 }.map { it.phoneNumber }.distinct().size
        val connectedIncomingUnique = logs.filter { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE && it.duration > 3 }.map { it.phoneNumber }.distinct().size
        val connectedOutgoingUnique = logs.filter { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE && it.duration > 3 }.map { it.phoneNumber }.distinct().size
        val notAnsweredIncomingUnique = logs.filter { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE && it.duration <= 3 }.map { it.phoneNumber }.distinct().size
        val outgoingNotConnectedUnique = logs.filter { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE && it.duration <= 3 }.map { it.phoneNumber }.distinct().size
        val mayFailedUnique = logs.filter { it.duration < 4 }.map { it.phoneNumber }.distinct().size
        
        // Never Connected Unique
        val visibleNumbers = logs.map { it.phoneNumber }.toSet()
        val relevantPersons = persons.filter { it.phoneNumber in visibleNumbers }
        val neverConnectedUniqueCount = relevantPersons.count { it.totalCalls > 0 && it.totalDuration == 0L }

        return ReportStats(
            totalCalls = totalCalls,
            incomingCalls = incomingCalls,
            outgoingCalls = outgoingCalls,
            missedCalls = missedCalls,
            rejectedCalls = rejectedCalls,
            
            connectedCalls = connectedCalls,
            connectedUnique = connectedUniqueCount,
            notConnectedCalls = notConnectedCalls,
            notConnectedUnique = notConnectedUniqueCount,
            
            connectedIncoming = connectedIncoming,
            connectedIncomingUnique = connectedIncomingUnique,
            connectedOutgoing = connectedOutgoing,
            connectedOutgoingUnique = connectedOutgoingUnique,
            
            notAnsweredIncoming = incomingCalls - connectedIncoming,
            notAnsweredIncomingUnique = notAnsweredIncomingUnique,
            
            outgoingNotConnected = outgoingCalls - connectedOutgoing,
            outgoingNotConnectedUnique = outgoingNotConnectedUnique,
            
            totalDuration = totalDuration,
            avgDuration = avgDuration,
            maxDuration = maxDuration,
            incomingDuration = incomingDuration,
            outgoingDuration = outgoingDuration,
            
            uniqueContacts = uniqueContacts,
            savedContacts = savedContacts,
            unsavedContacts = unsavedContacts,
            
            callsWithNotes = callsWithNotes,
            reviewedCalls = reviewedCalls,
            callsWithRecordings = callsWithRecordings,
            personsWithNotes = personsWithNotes,
            personsWithLabels = personsWithLabels,
            totalPersons = persons.size,
            
            neverConnectedCount = neverConnectedCount,
            neverConnectedUnique = neverConnectedUniqueCount,
            mayFailedCount = mayFailedCount,
            mayFailedUnique = mayFailedUnique,
            
            durationBuckets = bucketStats,
            comparisonAverages = emptyMap(), // Will be calculated separately
            
            topCallers = topCallers,
            mostTalked = mostTalked,
            dailyStats = dailyStats,
            hourlyStats = hourlyStats,
            labelDistribution = labelDistribution
        )
    }

    /**
     * Calculate last 7 days averages for comparison metrics.
     */
    fun calculateLast7DaysAverages(allLogs: List<CallDataEntity>): Map<String, Float> {
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

    private fun calculateTopCallers(logs: List<CallDataEntity>): List<TopCaller> {
        return logs
            .groupBy { it.phoneNumber }
            .map { (number, calls) ->
                val name = calls.firstOrNull { !it.contactName.isNullOrEmpty() }?.contactName
                TopCaller(
                    phoneNumber = number,
                    displayName = name ?: number,
                    callCount = calls.size,
                    totalDuration = calls.sumOf { it.duration },
                    incomingCount = calls.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                    outgoingCount = calls.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                    missedCount = calls.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE }
                )
            }
            .sortedByDescending { it.callCount }
            .take(5)
    }

    private fun calculateMostTalked(logs: List<CallDataEntity>): List<TopCaller> {
        return logs
            .groupBy { it.phoneNumber }
            .map { (number, calls) ->
                val name = calls.firstOrNull { !it.contactName.isNullOrEmpty() }?.contactName
                TopCaller(
                    phoneNumber = number,
                    displayName = name ?: number,
                    callCount = calls.size,
                    totalDuration = calls.sumOf { it.duration },
                    incomingCount = calls.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                    outgoingCount = calls.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                    missedCount = calls.count { it.callType == android.provider.CallLog.Calls.MISSED_TYPE }
                )
            }
            .filter { it.totalDuration > 0 }
            .sortedByDescending { it.totalDuration }
            .take(5)
    }

    private fun calculateDailyStats(logs: List<CallDataEntity>): Map<String, DayStat> {
        return logs
            .groupBy { 
                SimpleDateFormat("EEE", Locale.getDefault()).format(Date(it.callDate))
            }
            .mapValues { (_, calls) ->
                DayStat(
                    count = calls.size,
                    duration = calls.sumOf { it.duration }
                )
            }
    }

    private fun calculateHourlyStats(logs: List<CallDataEntity>): Map<Int, Int> {
        return logs
            .groupBy { 
                Calendar.getInstance().apply { timeInMillis = it.callDate }.get(Calendar.HOUR_OF_DAY)
            }
            .mapValues { (_, calls) -> calls.size }
    }

    private fun calculateLabelDistribution(persons: List<PersonDataEntity>): List<Pair<String, Int>> {
        return persons
            .flatMap { person ->
                person.label?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    private fun calculateDurationBuckets(logs: List<CallDataEntity>): List<DurationBucketStat> {
        val buckets = listOf(
            Triple("Over 5 min", 301, Int.MAX_VALUE),
            Triple("3 to 5 min", 181, 300),
            Triple("1 to 3 min", 61, 180),
            Triple("30s to 1 min", 31, 60),
            Triple("10s to 30s", 11, 30),
            Triple("5s to 10s", 6, 10),
            Triple("0s to 5s", 0, 5)
        )
        
        return buckets.mapIndexed { index, (label, min, max) ->
            val inBucket = logs.filter { it.duration in min..max }
            DurationBucketStat(
                label = label,
                total = inBucket.size,
                outCount = inBucket.count { it.callType == android.provider.CallLog.Calls.OUTGOING_TYPE },
                inCount = inBucket.count { it.callType == android.provider.CallLog.Calls.INCOMING_TYPE },
                sortOrder = index
            )
        }
    }
}
