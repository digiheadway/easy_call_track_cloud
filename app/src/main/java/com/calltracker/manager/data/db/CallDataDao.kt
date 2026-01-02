package com.calltracker.manager.data.db

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
    
    @Query("SELECT * FROM call_data WHERE syncStatus = 'PENDING' ORDER BY callDate DESC")
    fun getPendingCallsFlow(): Flow<List<CallDataEntity>>
    
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.syncStatus != 'COMPLETED' 
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
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
        ORDER BY c.callDate DESC
    """)
    suspend fun getCallsNeedingMetadataSync(minDate: Long): List<CallDataEntity>
    
    // NEW: Get calls needing recording upload (slow sync)
    @Query("""
        SELECT c.* FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.recordingSyncStatus IN ('PENDING', 'FAILED')
        AND c.callDate >= :minDate
        AND c.localRecordingPath IS NOT NULL
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
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getPendingMetadataSyncCountFlow(minDate: Long): Flow<Int>
    
    // NEW: Count of pending new calls (never synced)
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.metadataSyncStatus = 'PENDING'
        AND c.callDate >= :minDate
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getPendingNewCallsCountFlow(minDate: Long): Flow<Int>
    
    // NEW: Count of pending metadata updates (already synced but edited locally: notes, reviewed)
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.metadataSyncStatus = 'UPDATE_PENDING'
        AND c.callDate >= :minDate
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getPendingMetadataUpdatesCountFlow(minDate: Long): Flow<Int>
    
    // NEW: Count of pending recording uploads
    @Query("""
        SELECT COUNT(*) FROM call_data c 
        LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
        WHERE c.recordingSyncStatus IN ('PENDING', 'FAILED')
        AND c.callDate >= :minDate
        AND c.localRecordingPath IS NOT NULL
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
    """)
    fun getPendingRecordingSyncCountFlow(minDate: Long): Flow<Int>

    @Query("""
        SELECT * FROM call_data 
        WHERE recordingSyncStatus NOT IN ('NOT_APPLICABLE', 'COMPLETED')
        ORDER BY updatedAt DESC
    """)
    fun getActiveRecordingSyncsFlow(): Flow<List<CallDataEntity>>
    
    @Query("SELECT * FROM call_data WHERE phoneNumber = :phoneNumber ORDER BY callDate DESC")
    suspend fun getCallsForNumber(phoneNumber: String): List<CallDataEntity>
    
    @Query("SELECT MAX(callDate) FROM call_data")
    suspend fun getMaxCallDate(): Long?
    
    @Query("SELECT compositeId FROM call_data")
    suspend fun getAllCompositeIds(): List<String>
    
    @Query("SELECT compositeId FROM call_data WHERE callDate >= :minDate")
    suspend fun getCompositeIdsSince(minDate: Long): List<String>
    
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
    
    @Query("UPDATE call_data SET callNote = :note, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateCallNote(compositeId: String, note: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE call_data SET syncError = :error, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateSyncError(compositeId: String, error: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE call_data SET localRecordingPath = :path, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateRecordingPath(compositeId: String, path: String?, timestamp: Long = System.currentTimeMillis())
    
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
    
    // NEW: Mark recording as uploaded
    @Query("UPDATE call_data SET recordingSyncStatus = 'COMPLETED' WHERE compositeId = :compositeId")
    suspend fun markRecordingUploaded(compositeId: String)
    
    // ============================================
    // DELETE
    // ============================================
    
    @Query("DELETE FROM call_data WHERE compositeId = :compositeId")
    suspend fun deleteByCompositeId(compositeId: String)
    
    @Query("DELETE FROM call_data WHERE callDate < :minDate")
    suspend fun deleteBefore(minDate: Long)

    @Query("DELETE FROM call_data")
    suspend fun deleteAll()
}
