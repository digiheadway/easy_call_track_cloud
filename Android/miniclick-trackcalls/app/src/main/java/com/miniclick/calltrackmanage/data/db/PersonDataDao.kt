package com.miniclick.calltrackmanage.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDataDao {
    
    // ============================================
    // QUERIES
    // ============================================
    
    // Get all non-excluded persons (for main call list)
    @Query("SELECT * FROM person_data WHERE excludeFromList = 0 ORDER BY lastCallDate DESC")
    fun getAllPersonsFlow(): Flow<List<PersonDataEntity>>
    
    // Get ALL persons including excluded (for ViewModel filtering)
    @Query("SELECT * FROM person_data ORDER BY lastCallDate DESC")
    fun getAllPersonsIncludingExcludedFlow(): Flow<List<PersonDataEntity>>
    
    @Query("SELECT * FROM person_data WHERE excludeFromList = 0 ORDER BY lastCallDate DESC")
    suspend fun getAllPersons(): List<PersonDataEntity>
    
    // Legacy: Get persons excluded with old isExcluded flag (for migration compatibility)
    @Query("SELECT * FROM person_data WHERE isExcluded = 1 ORDER BY lastCallDate DESC")
    fun getLegacyExcludedPersonsFlow(): Flow<List<PersonDataEntity>>
    
    // Get persons with any form of exclusion
    @Query("SELECT * FROM person_data WHERE (excludeFromSync = 1 OR excludeFromList = 1) ORDER BY lastCallDate DESC")
    fun getExcludedPersonsFlow(): Flow<List<PersonDataEntity>>
    
    // Get "No Tracking" persons (excluded from both sync and list)
    @Query("SELECT * FROM person_data WHERE excludeFromSync = 1 AND excludeFromList = 1 ORDER BY lastCallDate DESC")
    fun getNoTrackingPersonsFlow(): Flow<List<PersonDataEntity>>
    
    // Get "Excluded from lists" only persons (hidden from UI but still tracked)
    @Query("SELECT * FROM person_data WHERE excludeFromSync = 0 AND excludeFromList = 1 ORDER BY lastCallDate DESC")
    fun getExcludedFromListOnlyPersonsFlow(): Flow<List<PersonDataEntity>>
    
    @Query("SELECT * FROM person_data WHERE phoneNumber = :phoneNumber")
    suspend fun getByPhoneNumber(phoneNumber: String): PersonDataEntity?
    
    @Query("SELECT * FROM person_data WHERE phoneNumber IN (:phoneNumbers)")
    suspend fun getByPhoneNumbers(phoneNumbers: List<String>): List<PersonDataEntity>
    
    @Query("SELECT * FROM person_data WHERE personNote IS NOT NULL AND personNote != ''")
    suspend fun getPersonsWithNotes(): List<PersonDataEntity>
    
    // Check if number should be excluded from sync
    @Query("SELECT excludeFromSync FROM person_data WHERE phoneNumber = :phoneNumber")
    suspend fun isExcludedFromSync(phoneNumber: String): Boolean?
    
    // ============================================
    // INSERTS & UPDATES
    // ============================================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PersonDataEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PersonDataEntity>)
    
    @Update
    suspend fun update(entity: PersonDataEntity)
    
    @Query("UPDATE person_data SET personNote = :note, updatedAt = :timestamp, needsSync = 1 WHERE phoneNumber = :phoneNumber")
    suspend fun updatePersonNote(phoneNumber: String, note: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE person_data SET contactName = :name, photoUri = :photoUri, updatedAt = :timestamp WHERE phoneNumber = :phoneNumber")
    suspend fun updateContactInfo(phoneNumber: String, name: String?, photoUri: String?, timestamp: Long = System.currentTimeMillis())
    
    // Legacy exclusion update (kept for backward compatibility)
    @Deprecated("Use updateExclusionType instead")
    @Query("UPDATE person_data SET isExcluded = :isExcluded, updatedAt = :timestamp WHERE phoneNumber = :phoneNumber")
    suspend fun updateExclusion(phoneNumber: String, isExcluded: Boolean, timestamp: Long = System.currentTimeMillis())
    
    // Granular exclusion update
    @Query("UPDATE person_data SET excludeFromSync = :excludeFromSync, excludeFromList = :excludeFromList, updatedAt = :timestamp WHERE phoneNumber = :phoneNumber")
    suspend fun updateExclusionType(phoneNumber: String, excludeFromSync: Boolean, excludeFromList: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE person_data SET label = :label, updatedAt = :timestamp, needsSync = 1 WHERE phoneNumber = :phoneNumber")
    suspend fun updateLabel(phoneNumber: String, label: String?, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE person_data SET contactName = :name, updatedAt = :timestamp, needsSync = 1 WHERE phoneNumber = :phoneNumber")
    suspend fun updateName(phoneNumber: String, name: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM person_data WHERE needsSync = 1")
    suspend fun getPendingSyncPersons(): List<PersonDataEntity>

    @Query("SELECT * FROM person_data WHERE needsSync = 1")
    fun getPendingSyncPersonsFlow(): Flow<List<PersonDataEntity>>

    @Query("SELECT COUNT(*) FROM person_data WHERE needsSync = 1")
    fun getPendingSyncPersonsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM person_data WHERE needsSync = 1")
    fun getPendingSyncCountFlow(): Flow<Int>

    @Query("UPDATE person_data SET needsSync = :needsSync WHERE phoneNumber = :phoneNumber")
    suspend fun updateSyncStatus(phoneNumber: String, needsSync: Boolean)

    @Query("UPDATE person_data SET personNote = :note, needsSync = 0 WHERE phoneNumber = :phoneNumber")
    suspend fun updatePersonNoteFromRemote(phoneNumber: String, note: String?)

    @Query("UPDATE person_data SET label = :label, needsSync = 0 WHERE phoneNumber = :phoneNumber")
    suspend fun updateLabelFromRemote(phoneNumber: String, label: String?)

    @Query("UPDATE person_data SET contactName = :name, needsSync = 0 WHERE phoneNumber = :phoneNumber")
    suspend fun updateNameFromRemote(phoneNumber: String, name: String?)
    
    // NEW: Update from server with conflict resolution
    @Query("""
        UPDATE person_data SET 
            personNote = COALESCE(:personNote, personNote),
            label = COALESCE(:label, label),
            contactName = COALESCE(:name, contactName),
            serverUpdatedAt = :serverUpdatedAt,
            needsSync = 0
        WHERE phoneNumber = :phoneNumber AND (serverUpdatedAt IS NULL OR serverUpdatedAt < :serverUpdatedAt)
    """)
    suspend fun updateFromServer(phoneNumber: String, personNote: String?, label: String?, name: String?, serverUpdatedAt: Long)
    
    // ============================================
    // DELETE
    // ============================================
    
    @Query("DELETE FROM person_data WHERE phoneNumber = :phoneNumber")
    suspend fun deleteByPhoneNumber(phoneNumber: String)
    
    @Query("DELETE FROM person_data")
    suspend fun deleteAll()
}
