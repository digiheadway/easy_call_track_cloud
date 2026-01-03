package com.miniclick.calltrackmanage.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Person data entity - stores person-level information.
 * Keyed by normalized phone number.
 */
@Entity(
    tableName = "person_data",
    indices = [
        androidx.room.Index("lastCallDate"),
        androidx.room.Index("isExcluded")
    ]
)
data class PersonDataEntity(
    @PrimaryKey
    val phoneNumber: String,       // Normalized phone number (digits only)
    
    // Person details
    val contactName: String? = null,
    val photoUri: String? = null,
    val personNote: String? = null,
    val label: String? = null,
    
    // Last call summary
    val lastCallType: Int? = null,
    val lastCallDuration: Long? = null,
    val lastCallDate: Long? = null,
    val lastRecordingPath: String? = null,
    val lastCallCompositeId: String? = null,
    
    // Stats
    val totalCalls: Int = 0,
    val totalIncoming: Int = 0,
    val totalOutgoing: Int = 0,
    val totalMissed: Int = 0,
    val totalDuration: Long = 0,
    
    // Exclusion flag
    val isExcluded: Boolean = false,
    
    // Timestamps for sync
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),  // Local update time
    val serverUpdatedAt: Long? = null,  // Last known server update time (for conflict resolution)
    
    // Sync status
    val needsSync: Boolean = false
)
