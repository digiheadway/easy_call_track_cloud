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
        androidx.room.Index("isExcluded"),
        androidx.room.Index("excludeFromSync"),
        androidx.room.Index("excludeFromList")
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
    
    // Legacy exclusion flag (kept for backward compatibility)
    @Deprecated("Use excludeFromSync and excludeFromList instead")
    val isExcluded: Boolean = false,
    
    // Granular exclusion types (matching admin panel)
    // excludeFromSync = true -> "No Tracking" - completely stop tracking calls for this number
    // excludeFromList = true -> "Excluded from lists" - track but hide from UI
    val excludeFromSync: Boolean = false,
    val excludeFromList: Boolean = false,
    
    // Timestamps for sync
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),  // Local update time
    val serverUpdatedAt: Long? = null,  // Last known server update time (for conflict resolution)
    
    // Sync status
    val needsSync: Boolean = false
) {
    // Helper computed properties
    val isNoTracking: Boolean get() = excludeFromSync && excludeFromList
    val isExcludedFromListOnly: Boolean get() = !excludeFromSync && excludeFromList
    val hasAnyExclusion: Boolean get() = excludeFromSync || excludeFromList
}
