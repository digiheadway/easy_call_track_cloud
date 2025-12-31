package com.calltracker.manager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Complete call data entity - stores all call information in Room DB.
 * This is the single source of truth for call data.
 */
@Entity(tableName = "call_data")
data class CallDataEntity(
    @PrimaryKey
    val compositeId: String,
    
    // Call details from system
    val systemId: String,          // Original system row ID
    val phoneNumber: String,
    val contactName: String? = null,
    val callType: Int,             // INCOMING, OUTGOING, MISSED, REJECTED
    val callDate: Long,            // Timestamp
    val duration: Long,            // In seconds
    val photoUri: String? = null,
    val subscriptionId: Int? = null,
    val deviceId: String? = null,
    
    // App-specific data
    val callNote: String? = null,
    val localRecordingPath: String? = null,
    
    // Sync status
    val syncStatus: CallLogStatus = CallLogStatus.PENDING,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
