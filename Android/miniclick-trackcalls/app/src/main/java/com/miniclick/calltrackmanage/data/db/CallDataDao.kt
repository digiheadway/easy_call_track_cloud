package com.miniclick.calltrackmanage.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDataDao {
    
    // ============================================
    // QUERIES
    // ============================================
    
    @Query("""
        SELECT c.* FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE (p.isExcluded IS NULL OR p.isExcluded = 0)
        AND c.callDate >= :minDate
        ORDER BY c.callDate DESC
    """)
    fun getCallsSinceFlow(minDate: Long): Flow<List<CallDataEntity>>

    @Query("""
        SELECT c.* FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE (p.isExcluded IS NULL OR p.isExcluded = 0)
        ORDER BY c.callDate DESC
    """)
    fun getAllCallsFlow(): Flow<List<CallDataEntity>>
    
    @Query("""
        SELECT c.* FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE (p.isExcluded IS NULL OR p.isExcluded = 0)
        ORDER BY c.callDate DESC
    """)
    suspend fun getAllCalls(): List<CallDataEntity>
    
    @Query("SELECT * FROM call_data WHERE compositeId = :compositeId")
    suspend fun getByCompositeId(compositeId: String): CallDataEntity?
    
    @Query("SELECT * FROM call_data WHERE compositeId IN (:ids)")
    suspend fun getByCompositeIds(ids: List<String>): List<CallDataEntity>
    
    @Query("SELECT * FROM call_data WHERE syncStatus = :status ORDER BY callDate DESC")
    suspend fun getByStatus(status: CallLogStatus): List<CallDataEntity>
    
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE (c.metadataSyncStatus IN ('PENDING', 'UPDATE_PENDING', 'FAILED') 
               OR (c.recordingSyncStatus IN ('PENDING', 'FAILED', 'UPLOADING') AND c.metadataSyncStatus = 'COMPLETED'))
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.excludeFromList IS NULL OR p.excludeFromList = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getPendingSyncCountFlow(): Flow<Int>
    
    @Query("""
        SELECT c.* FROM call_data c
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber
        WHERE c.syncStatus = 'PENDING'
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
        ORDER BY c.callDate DESC
    """)
    fun getPendingCallsFlow(): Flow<List<CallDataEntity>>
    
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.syncStatus != 'COMPLETED' 
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getUnsyncedCallsCountFlow(): Flow<Int>

    @Query("""
        SELECT c.* FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.recordingSyncStatus = 'PENDING' 
        AND c.localRecordingPath IS NULL
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
        ORDER BY c.callDate DESC
    """)
    suspend fun getCallsMissingRecordings(): List<CallDataEntity>
    
    // NEW: Get calls needing metadata sync (fast sync)
    @Query("""
        SELECT c.* FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.metadataSyncStatus IN ('PENDING', 'UPDATE_PENDING', 'FAILED')
        AND c.callDate >= :minDate
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
        ORDER BY c.callDate DESC
    """)
    suspend fun getCallsNeedingMetadataSync(minDate: Long): List<CallDataEntity>
    
    // NEW: Get calls needing recording upload (slow sync)
    @Query("""
        SELECT c.* FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.recordingSyncStatus IN ('PENDING', 'FAILED', 'UPLOADING')
        AND c.metadataSyncStatus = 'SYNCED'
        AND c.callDate >= :minDate
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
        ORDER BY c.callDate ASC
    """)
    suspend fun getCallsNeedingRecordingSync(minDate: Long): List<CallDataEntity>
    
    // NEW: Count of pending metadata syncs
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.metadataSyncStatus IN ('PENDING', 'UPDATE_PENDING', 'FAILED')
        AND c.callDate >= :minDate
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getPendingMetadataSyncCountFlow(minDate: Long): Flow<Int>
    
    // NEW: Count of pending new calls (never synced)
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.metadataSyncStatus = 'PENDING'
        AND c.callDate >= :minDate
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.excludeFromList IS NULL OR p.excludeFromList = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getPendingNewCallsCountFlow(minDate: Long): Flow<Int>
    
    // NEW: Count of pending metadata updates (already synced but edited locally: notes, reviewed)
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.metadataSyncStatus = 'UPDATE_PENDING'
        AND c.callDate >= :minDate
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.excludeFromList IS NULL OR p.excludeFromList = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getPendingMetadataUpdatesCountFlow(minDate: Long): Flow<Int>
    
    // NEW: Count of pending recording uploads
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.recordingSyncStatus IN ('PENDING', 'FAILED', 'UPLOADING')
        AND c.metadataSyncStatus = 'SYNCED'
        AND c.callDate >= :minDate
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.excludeFromList IS NULL OR p.excludeFromList = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getPendingRecordingSyncCountFlow(minDate: Long): Flow<Int>

    @Query("""
        SELECT c.* FROM call_data c
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber
        WHERE c.recordingSyncStatus NOT IN ('NOT_APPLICABLE', 'COMPLETED')
        AND c.metadataSyncStatus = 'SYNCED'
        AND (p.excludeFromList IS NULL OR p.excludeFromList = 0)
        AND (p.excludeFromSync IS NULL OR p.excludeFromSync = 0)
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
        ORDER BY c.updatedAt DESC
    """)
    fun getActiveRecordingSyncsFlow(): Flow<List<CallDataEntity>>
    
    @Query("SELECT * FROM call_data WHERE phoneNumber = :phoneNumber ORDER BY callDate DESC")
    suspend fun getCallsForNumber(phoneNumber: String): List<CallDataEntity>
    
    @Query("SELECT MAX(callDate) FROM call_data")
    suspend fun getMaxCallDate(): Long?
    
    @Query("SELECT MIN(callDate) FROM call_data")
    suspend fun getMinCallDate(): Long?
    
    @Query("SELECT compositeId FROM call_data")
    suspend fun getAllCompositeIds(): List<String>
    
    @Query("SELECT compositeId FROM call_data WHERE callDate >= :minDate")
    suspend fun getCompositeIdsSince(minDate: Long): List<String>
    
    @Query("SELECT systemId FROM call_data WHERE callDate >= :minDate")
    suspend fun getSystemIdsSince(minDate: Long): List<String>
    
    @Query("""
        SELECT 
            COUNT(*) as totalCalls,
            SUM(CASE WHEN callType = 1 THEN 1 ELSE 0 END) as totalIncoming,
            SUM(CASE WHEN callType = 2 THEN 1 ELSE 0 END) as totalOutgoing,
            SUM(CASE WHEN callType IN (3, 5, 6) THEN 1 ELSE 0 END) as totalMissed,
            SUM(duration) as totalDuration
        FROM call_data 
        WHERE phoneNumber = :phoneNumber
    """)
    suspend fun getCallStatsForNumber(phoneNumber: String): CallStatsResult?

    @Query("""
        SELECT 
            phoneNumber,
            COUNT(*) as totalCalls,
            SUM(CASE WHEN callType = 1 THEN 1 ELSE 0 END) as totalIncoming,
            SUM(CASE WHEN callType = 2 THEN 1 ELSE 0 END) as totalOutgoing,
            SUM(CASE WHEN callType IN (3, 5, 6) THEN 1 ELSE 0 END) as totalMissed,
            SUM(duration) as totalDuration
        FROM call_data 
        WHERE phoneNumber IN (:phoneNumbers)
        GROUP BY phoneNumber
    """)
    suspend fun getCallStatsForNumbers(phoneNumbers: List<String>): List<CallStatsBatchResult>

data class CallStatsBatchResult(
    val phoneNumber: String,
    val totalCalls: Int,
    val totalIncoming: Int,
    val totalOutgoing: Int,
    val totalMissed: Int,
    val totalDuration: Long
)

data class CallStatsResult(
    val totalCalls: Int,
    val totalIncoming: Int,
    val totalOutgoing: Int,
    val totalMissed: Int,
    val totalDuration: Long
)
    
    // ============================================
    // INSERTS & UPDATES
    // ============================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CallDataEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CallDataEntity>)
    
    @Update
    suspend fun update(entity: CallDataEntity)
    
    // Legacy sync status update (deprecated)
    @Query("UPDATE call_data SET syncStatus = :status, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateSyncStatus(compositeId: String, status: CallLogStatus, timestamp: Long = System.currentTimeMillis())
    
    // NEW: Update metadata sync status
    @Query("UPDATE call_data SET metadataSyncStatus = :status, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateMetadataSyncStatus(compositeId: String, status: MetadataSyncStatus, timestamp: Long = System.currentTimeMillis())
    
    // NEW: Update recording sync status
    @Query("UPDATE call_data SET recordingSyncStatus = :status, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateRecordingSyncStatus(compositeId: String, status: RecordingSyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE call_data SET recordingSyncStatus = :status, updatedAt = :timestamp WHERE compositeId IN (:compositeIds)")
    suspend fun updateRecordingSyncStatusBatch(compositeIds: List<String>, status: RecordingSyncStatus, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE call_data SET callNote = :note, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateCallNote(compositeId: String, note: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE call_data SET syncError = :error, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateSyncError(compositeId: String, error: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE call_data SET syncError = :error, updatedAt = :timestamp WHERE compositeId IN (:compositeIds)")
    suspend fun updateSyncErrorBatch(compositeIds: List<String>, error: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE call_data SET localRecordingPath = :path, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateRecordingPath(compositeId: String, path: String?, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun updateRecordingPaths(updates: Map<String, String?>, timestamp: Long = System.currentTimeMillis()) {
        updates.forEach { (id, path) ->
            updateRecordingPath(id, path, timestamp)
        }
    }
    
    // NEW: Update reviewed status (local change, mark for sync)
    @Query("UPDATE call_data SET reviewed = :reviewed, metadataSyncStatus = 'UPDATE_PENDING', updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateReviewed(compositeId: String, reviewed: Boolean, timestamp: Long = System.currentTimeMillis())
    
    // NEW: Mark all calls for a number as reviewed
    @Query("UPDATE call_data SET reviewed = 1, metadataSyncStatus = 'UPDATE_PENDING', updatedAt = :timestamp WHERE phoneNumber = :phoneNumber AND reviewed = 0")
    suspend fun markAllCallsReviewed(phoneNumber: String, timestamp: Long = System.currentTimeMillis())
    
    // NEW: Update from server (no need to sync back)
    @Query("""
        UPDATE call_data SET 
            reviewed = :reviewed, 
            callNote = :note, 
            contactName = :callerName,
            serverUpdatedAt = :serverUpdatedAt,
            metadataSyncStatus = 'SYNCED'
        WHERE compositeId = :compositeId AND (serverUpdatedAt IS NULL OR serverUpdatedAt < :serverUpdatedAt)
    """)
    suspend fun updateFromServer(compositeId: String, reviewed: Boolean, note: String?, callerName: String?, serverUpdatedAt: Long)
    
    // NEW: Mark metadata as synced
    @Query("UPDATE call_data SET metadataSyncStatus = 'SYNCED', serverUpdatedAt = :serverTime WHERE compositeId = :compositeId")
    suspend fun markMetadataSynced(compositeId: String, serverTime: Long = System.currentTimeMillis())

    @Query("UPDATE call_data SET metadataSyncStatus = 'SYNCED', serverUpdatedAt = :serverTime WHERE compositeId IN (:compositeIds)")
    suspend fun markMetadataSyncedBatch(compositeIds: List<String>, serverTime: Long = System.currentTimeMillis())
    
    // NEW: Mark recording as uploaded
    @Query("UPDATE call_data SET recordingSyncStatus = 'COMPLETED', serverRecordingStatus = 'completed' WHERE compositeId = :compositeId")
    suspend fun markRecordingUploaded(compositeId: String)
    
    // NEW: Update server recording status specifically
    @Query("UPDATE call_data SET serverRecordingStatus = :status WHERE compositeId = :compositeId")
    suspend fun updateServerRecordingStatus(compositeId: String, status: String?)

    // NEW: Update metadata received confirmation
    @Query("UPDATE call_data SET metadataReceived = :received WHERE compositeId = :compositeId")
    suspend fun updateMetadataReceived(compositeId: String, received: Boolean = true)
    
    // NEW: Update processing status
    @Query("UPDATE call_data SET processingStatus = :status, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateProcessingStatus(compositeId: String, status: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE call_data SET processingStatus = NULL WHERE processingStatus IS NOT NULL AND updatedAt < :threshold")
    suspend fun clearStaleProcessingStatuses(threshold: Long)
    
    // ============================================
    // DELETE
    // ============================================
    
    @Query("DELETE FROM call_data WHERE compositeId = :compositeId")
    suspend fun deleteByCompositeId(compositeId: String)
    
    @Query("DELETE FROM call_data WHERE callDate < :minDate")
    suspend fun deleteBefore(minDate: Long)

    @Query("DELETE FROM call_data")
    suspend fun deleteAll()

    // NEW: Rescan skipped recordings (mark NOT_APPLICABLE/NOT_FOUND as PENDING)
    @Query("UPDATE call_data SET recordingSyncStatus = 'PENDING' WHERE recordingSyncStatus IN ('NOT_APPLICABLE', 'NOT_FOUND') AND duration > 0")
    suspend fun resetSkippedRecordings()

    // NEW: Reset all sync statuses for troubleshooting
    @Query("""
        UPDATE call_data SET 
            metadataSyncStatus = 'PENDING', 
            recordingSyncStatus = CASE WHEN duration > 0 THEN 'PENDING' ELSE 'NOT_APPLICABLE' END,
            metadataReceived = 0,
            serverRecordingStatus = NULL,
            syncError = NULL,
            updatedAt = :timestamp
    """)
    suspend fun resetAllSyncStatuses(timestamp: Long = System.currentTimeMillis())

    // NEW: Reset only metadata sync
    @Query("UPDATE call_data SET metadataSyncStatus = 'PENDING', metadataReceived = 0, updatedAt = :timestamp")
    suspend fun resetMetadataSync(timestamp: Long = System.currentTimeMillis())
    
    // NEW: Get calls with null processing status that are pending
    @Query("SELECT * FROM call_data WHERE (metadataSyncStatus IN ('PENDING', 'UPDATE_PENDING') OR recordingSyncStatus = 'PENDING') AND processingStatus IS NULL")
    suspend fun getPendingCallsWithNoProcessing(): List<CallDataEntity>
}
