package com.example.callyzer3.data

import java.time.LocalDateTime

enum class CallType {
    ALL, MISSED, NEVER_ATTENDED, REJECTED, OUTGOING, INCOMING
}

enum class CallStatus {
    ANSWERED, MISSED, REJECTED
}

data class CallLog(
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String? = null,
    val callType: CallType = CallType.ALL,
    val callStatus: CallStatus = CallStatus.MISSED,
    val duration: Long = 0, // in seconds
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val isExcluded: Boolean = false
)

data class Contact(
    val phoneNumber: String,
    val name: String? = null,
    val notes: String? = null,
    val isExcluded: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
