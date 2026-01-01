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
        WHERE c.syncStatus != 'COMPLETED' 
        AND (p.isExcluded IS NULL OR p.isExcluded = 0)
        ORDER BY c.callDate DESC
    """)
    suspend fun getUnsyncedCalls(): List<CallDataEntity>
    
    @Query("SELECT * FROM call_data WHERE phoneNumber = :phoneNumber ORDER BY callDate DESC")
    suspend fun getCallsForNumber(phoneNumber: String): List<CallDataEntity>
    
    @Query("SELECT compositeId FROM call_data")
    suspend fun getAllCompositeIds(): List<String>
    
    // ============================================
    // INSERTS & UPDATES
    // ============================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CallDataEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CallDataEntity>)
    
    @Update
    suspend fun update(entity: CallDataEntity)
    
    @Query("UPDATE call_data SET syncStatus = :status, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateSyncStatus(compositeId: String, status: CallLogStatus, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE call_data SET callNote = :note, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateCallNote(compositeId: String, note: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE call_data SET localRecordingPath = :path, updatedAt = :timestamp WHERE compositeId = :compositeId")
    suspend fun updateRecordingPath(compositeId: String, path: String?, timestamp: Long = System.currentTimeMillis())
    
    // ============================================
    // DELETE
    // ============================================
    
    @Query("DELETE FROM call_data WHERE compositeId = :compositeId")
    suspend fun deleteByCompositeId(compositeId: String)
    
    @Query("DELETE FROM call_data")
    suspend fun deleteAll()
}
