package com.miniclick.calltrackmanage.data.repository

import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.CallLogStatus
import com.miniclick.calltrackmanage.data.db.MetadataSyncStatus
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import com.miniclick.calltrackmanage.data.db.RecordingSyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the contract for call data operations.
 * This abstraction enables:
 * 1. Easier unit testing with mock implementations
 * 2. Flexibility to swap implementations (e.g., for offline-first vs cloud-first)
 * 3. Clear separation of concerns
 */
interface CallRepository {
    
    // ==================== CALL QUERIES ====================
    
    /**
     * Get all calls as a Flow for real-time UI updates.
     */
    fun getAllCallsFlow(): Flow<List<CallDataEntity>>
    
    /**
     * Get all calls (one-time fetch).
     */
    suspend fun getAllCalls(): List<CallDataEntity>
    
    /**
     * Get a specific call by its composite ID.
     */
    suspend fun getCallByCompositeId(compositeId: String): CallDataEntity?
    
    /**
     * Get all calls for a specific phone number.
     */
    suspend fun getLogsForPhone(phoneNumber: String): List<CallDataEntity>
    
    /**
     * Get calls that don't have a recording path assigned yet.
     */
    suspend fun getCallsMissingRecordings(): List<CallDataEntity>
    
    /**
     * Get pending calls as a Flow.
     */
    fun getPendingCallsFlow(): Flow<List<CallDataEntity>>
    
    // ==================== CALL UPDATES ====================
    
    /**
     * Update call note and trigger sync if needed.
     */
    suspend fun updateCallNote(compositeId: String, note: String?)
    
    /**
     * Update sync status.
     */
    suspend fun updateSyncStatus(compositeId: String, status: CallLogStatus)
    
    /**
     * Update recording path.
     */
    suspend fun updateRecordingPath(compositeId: String, path: String?)
    
    /**
     * Mark call as synced (COMPLETED status).
     */
    suspend fun markAsSynced(compositeId: String)
    
    /**
     * Check if call is synced.
     */
    suspend fun isSynced(compositeId: String): Boolean
    
    /**
     * Update reviewed status.
     */
    suspend fun updateReviewed(compositeId: String, reviewed: Boolean)
    
    /**
     * Mark all calls for a phone number as reviewed.
     */
    suspend fun markAllCallsReviewed(phoneNumber: String)
    
    // ==================== SYNC STATUS ====================
    
    /**
     * Get calls needing metadata sync (fast sync).
     */
    suspend fun getCallsNeedingMetadataSync(): List<CallDataEntity>
    
    /**
     * Get calls needing recording upload (slow sync).
     */
    suspend fun getCallsNeedingRecordingSync(): List<CallDataEntity>
    
    /**
     * Update metadata sync status.
     */
    suspend fun updateMetadataSyncStatus(compositeId: String, status: MetadataSyncStatus)
    
    /**
     * Update recording sync status.
     */
    suspend fun updateRecordingSyncStatus(compositeId: String, status: RecordingSyncStatus)
    
    // ==================== PERSON QUERIES ====================
    
    /**
     * Get all persons as a Flow (excluding hidden ones by default).
     */
    fun getAllPersonsFlow(): Flow<List<PersonDataEntity>>
    
    /**
     * Get all persons including excluded ones as a Flow.
     */
    fun getAllPersonsIncludingExcludedFlow(): Flow<List<PersonDataEntity>>
    
    /**
     * Get all persons (one-time fetch).
     */
    suspend fun getAllPersons(): List<PersonDataEntity>
    
    /**
     * Get a specific person by phone number.
     */
    suspend fun getPersonByPhone(phoneNumber: String): PersonDataEntity?
    
    // ==================== PERSON UPDATES ====================
    
    /**
     * Update person note.
     */
    suspend fun updatePersonNote(phoneNumber: String, note: String?)
    
    /**
     * Update person label.
     */
    suspend fun updatePersonLabel(phoneNumber: String, label: String?)
    
    /**
     * Update person custom name.
     */
    suspend fun updatePersonName(phoneNumber: String, name: String?)
    
    /**
     * Update exclusion type for a person.
     */
    suspend fun updateExclusionType(
        phoneNumber: String,
        excludeFromSync: Boolean,
        excludeFromList: Boolean
    )
    
    // ==================== SYNC COUNTS ====================
    
    /**
     * Get pending changes count as a Flow.
     */
    fun getPendingChangesCountFlow(): Flow<Int>
    
    /**
     * Get pending metadata sync count as a Flow.
     */
    fun getPendingMetadataSyncCountFlow(): Flow<Int>
    
    /**
     * Get pending new calls count as a Flow.
     */
    fun getPendingNewCallsCountFlow(): Flow<Int>
    
    /**
     * Get pending metadata updates count as a Flow.
     */
    fun getPendingMetadataUpdatesCountFlow(): Flow<Int>
    
    /**
     * Get pending person updates count as a Flow.
     */
    fun getPendingPersonUpdatesCountFlow(): Flow<Int>
    
    /**
     * Get pending recording sync count as a Flow.
     */
    fun getPendingRecordingSyncCountFlow(): Flow<Int>
    
    /**
     * Get active recording syncs as a Flow.
     */
    fun getActiveRecordingSyncsFlow(): Flow<List<CallDataEntity>>
    
    // ==================== SYSTEM SYNC ====================
    
    /**
     * Sync calls from system call log to Room database.
     */
    suspend fun syncFromSystemCallLog()
    
    /**
     * Normalize phone number to a consistent format.
     */
    fun normalizePhoneNumber(phone: String): String
}
