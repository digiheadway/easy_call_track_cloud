package com.calltracker.manager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Complete call data entity - stores all call information in Room DB.
 * This is the single source of truth for call data.
 */
@Entity(
    tableName = "call_data",
    indices = [
        androidx.room.Index("phoneNumber"),
        androidx.room.Index("callDate")
    ]
)
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
    val reviewed: Boolean = false,  // NEW: Synced bidirectionally with server
    
    // Sync status - Split for independent sync of metadata vs recordings
    @Deprecated("Use metadataSyncStatus and recordingSyncStatus instead")
    val syncStatus: CallLogStatus = CallLogStatus.PENDING,  // Legacy, kept for migration
    val metadataSyncStatus: MetadataSyncStatus = MetadataSyncStatus.PENDING,  // Fast sync
    val recordingSyncStatus: RecordingSyncStatus = RecordingSyncStatus.NOT_APPLICABLE,  // Slow sync
    
    // Timestamps for conflict resolution in bidirectional sync
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),  // Local update time
    val serverUpdatedAt: Long? = null,  // Last known server update time (for conflict resolution)
    val syncError: String? = null       // NEW: Failure reason for sync
)
