package com.miniclick.calltrackmanage.data.db

/**
 * Status for call metadata sync (fast sync: call details, notes, reviewed, etc.)
 */
enum class MetadataSyncStatus {
    PENDING,        // New call, never synced
    SYNCED,         // Metadata synced to server
    UPDATE_PENDING, // Local changes need to be pushed
    FAILED;         // Sync failed

    companion object {
        fun fromString(value: String): MetadataSyncStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

/**
 * Status for recording sync (slow sync: compression + upload)
 */
enum class RecordingSyncStatus {
    NOT_APPLICABLE,  // No recording for this call (missed, 0 duration)
    PENDING,         // Recording exists, not yet uploaded
    COMPRESSING,     // Currently compressing
    UPLOADING,       // Currently uploading
    COMPLETED,       // Recording uploaded successfully
    FAILED;          // Upload failed

    companion object {
        fun fromString(value: String): RecordingSyncStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: NOT_APPLICABLE
        }
    }
}

/**
 * Legacy status enum for backward compatibility during migration.
 * Will be removed after migration is complete.
 */
@Deprecated("Use MetadataSyncStatus and RecordingSyncStatus instead")
enum class CallLogStatus {
    PENDING,
    COMPRESSING,
    UPLOADING,
    COMPLETED,
    FAILED,
    NOTE_UPDATE_PENDING;

    companion object {
        fun fromString(value: String): CallLogStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}
