package com.calltracker.manager.data.db

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
    
    @Query("UPDATE person_data SET personNote = :note, updatedAt = :timestamp WHERE phoneNumber = :phoneNumber")
    suspend fun updatePersonNote(phoneNumber: String, note: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE person_data SET contactName = :name, photoUri = :photoUri, updatedAt = :timestamp WHERE phoneNumber = :phoneNumber")
    suspend fun updateContactInfo(phoneNumber: String, name: String?, photoUri: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE person_data SET isExcluded = :isExcluded, updatedAt = :timestamp WHERE phoneNumber = :phoneNumber")
    suspend fun updateExclusion(phoneNumber: String, isExcluded: Boolean, timestamp: Long = System.currentTimeMillis())
    
    // ============================================
    // DELETE
    // ============================================
    
    @Query("DELETE FROM person_data WHERE phoneNumber = :phoneNumber")
    suspend fun deleteByPhoneNumber(phoneNumber: String)
    
    @Query("DELETE FROM person_data")
    suspend fun deleteAll()
}
