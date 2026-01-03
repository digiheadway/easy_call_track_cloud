package com.miniclick.calltrackmanage.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDataDao {
    
    // ============================================
    // QUERIES
    // ============================================
    
    @Query("SELECT * FROM person_data WHERE isExcluded = 0 ORDER BY lastCallDate DESC")
    fun getAllPersonsFlow(): Flow<List<PersonDataEntity>>
    
    @Query("SELECT * FROM person_data WHERE isExcluded = 0 ORDER BY lastCallDate DESC")
    suspend fun getAllPersons(): List<PersonDataEntity>
    
    @Query("SELECT * FROM person_data WHERE isExcluded = 1 ORDER BY lastCallDate DESC")
    fun getExcludedPersonsFlow(): Flow<List<PersonDataEntity>>
    
    @Query("SELECT * FROM person_data WHERE phoneNumber = :phoneNumber")
    suspend fun getByPhoneNumber(phoneNumber: String): PersonDataEntity?
    
    @Query("SELECT * FROM person_data WHERE phoneNumber IN (:phoneNumbers)")
    suspend fun getByPhoneNumbers(phoneNumbers: List<String>): List<PersonDataEntity>
    
    @Query("SELECT * FROM person_data WHERE personNote IS NOT NULL AND personNote != ''")
    suspend fun getPersonsWithNotes(): List<PersonDataEntity>
    
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
    
    @Query("UPDATE person_data SET isExcluded = :isExcluded, updatedAt = :timestamp WHERE phoneNumber = :phoneNumber")
    suspend fun updateExclusion(phoneNumber: String, isExcluded: Boolean, timestamp: Long = System.currentTimeMillis())

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
