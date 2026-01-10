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
 * 
 * Status hierarchy:
 * - NOT_APPLICABLE: Duration 0 (missed/rejected) - no recording expected
 * - NOT_ALLOWED: Employee disabled recording attachment in app settings
 * - DISABLED: Org-level restriction (storage full, plan expired, org disabled recording)
 * - NOT_FOUND: Recording expected but file not found after search
 * - PENDING: Recording exists, waiting to upload
 * - UPLOADING: Currently uploading
 * - COMPLETED: Recording uploaded successfully
 * - FAILED: Upload failed (network error, server error, etc.)
 */
enum class RecordingSyncStatus {
    NOT_APPLICABLE,  // No recording expected (duration 0, missed call)
    NOT_ALLOWED,     // Employee explicitly disabled attach recording in app
    DISABLED,        // Org-level block: storage full, plan expired, call_record_crm=0
    NOT_FOUND,       // Recording expected but file not found
    PENDING,         // Recording exists, not yet uploaded
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
