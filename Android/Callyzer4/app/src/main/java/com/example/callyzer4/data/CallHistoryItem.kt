package com.example.callyzer4.data

import java.util.Date

data class CallHistoryItem(
    val id: String,
    val phoneNumber: String,
    val contactName: String?,
    val callType: CallType,
    val duration: Long, // in seconds
    val timestamp: Date,
    val isRead: Boolean = false
)

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
}

data class CallGroup(
    val phoneNumber: String,
    val contactName: String?,
    val calls: List<CallHistoryItem>,
    val totalCalls: Int,
    val lastCallDate: Date,
    val totalDuration: Long
)
