package com.example.callyzer4.repository

import android.content.Context
import android.provider.CallLog
import com.example.callyzer4.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CallHistoryRepository(private val context: Context) {
    
    suspend fun getCallHistory(): List<CallHistoryItem> = withContext(Dispatchers.IO) {
        val calls = mutableListOf<CallHistoryItem>()
        
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndex(CallLog.Calls._ID)
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            
            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val number = it.getString(numberIndex) ?: ""
                val name = it.getString(nameIndex)
                val type = when (it.getInt(typeIndex)) {
                    CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    else -> CallType.INCOMING
                }
                val duration = it.getLong(durationIndex)
                val timestamp = Date(it.getLong(dateIndex))
                
                calls.add(
                    CallHistoryItem(
                        id = id,
                        phoneNumber = number,
                        contactName = name,
                        callType = type,
                        duration = duration,
                        timestamp = timestamp
                    )
                )
            }
        }
        
        calls
    }
    
    suspend fun getFilteredCallHistory(filter: CallFilter): List<CallHistoryItem> = withContext(Dispatchers.IO) {
        val allCalls = getCallHistory()
        
        allCalls.filter { call ->
            // Filter by call type
            call.callType in filter.callTypes &&
            // Filter by date range
            (filter.dateRange == null || call.timestamp in filter.dateRange.startDate..filter.dateRange.endDate) &&
            // Filter by search query
            (filter.searchQuery.isEmpty() || 
             call.phoneNumber.contains(filter.searchQuery, ignoreCase = true) ||
             call.contactName?.contains(filter.searchQuery, ignoreCase = true) == true) &&
            // Filter by duration
            (filter.minDuration == null || call.duration >= filter.minDuration) &&
            (filter.maxDuration == null || call.duration <= filter.maxDuration)
        }
    }
    
    suspend fun getGroupedCallHistory(filter: CallFilter = CallFilter()): List<CallGroup> = withContext(Dispatchers.IO) {
        val filteredCalls = getFilteredCallHistory(filter)
        
        filteredCalls.groupBy { it.phoneNumber }
            .map { (phoneNumber, calls) ->
                val contactName = calls.firstOrNull()?.contactName
                val totalCalls = calls.size
                val lastCallDate = calls.maxByOrNull { it.timestamp }?.timestamp ?: Date()
                val totalDuration = calls.sumOf { it.duration }
                
                CallGroup(
                    phoneNumber = phoneNumber,
                    contactName = contactName,
                    calls = calls.sortedByDescending { it.timestamp },
                    totalCalls = totalCalls,
                    lastCallDate = lastCallDate,
                    totalDuration = totalDuration
                )
            }
            .sortedByDescending { it.lastCallDate }
    }
    
    suspend fun getCallStatistics(): Map<String, Any> = withContext(Dispatchers.IO) {
        val allCalls = getCallHistory()
        
        mapOf(
            "totalCalls" to allCalls.size,
            "incomingCalls" to allCalls.count { it.callType == CallType.INCOMING },
            "outgoingCalls" to allCalls.count { it.callType == CallType.OUTGOING },
            "missedCalls" to allCalls.count { it.callType == CallType.MISSED },
            "totalDuration" to allCalls.sumOf { it.duration },
            "uniqueContacts" to allCalls.map { it.phoneNumber }.distinct().size
        )
    }
}
