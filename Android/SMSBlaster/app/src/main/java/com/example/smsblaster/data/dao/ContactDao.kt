package com.example.smsblaster.data.dao

import androidx.room.*
import com.example.smsblaster.data.model.Contact
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>
    
    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): Contact?
    
    @Query("SELECT * FROM contacts WHERE id IN (:ids)")
    suspend fun getContactsByIds(ids: List<Long>): List<Contact>
    
    @Query("SELECT * FROM contacts WHERE phone LIKE :query OR name LIKE :query")
    fun searchContacts(query: String): Flow<List<Contact>>
    
    @Query("SELECT DISTINCT tags FROM contacts")
    fun getAllTags(): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>): List<Long>
    
    @Update
    suspend fun updateContact(contact: Contact)
    
    @Delete
    suspend fun deleteContact(contact: Contact)
    
    @Query("DELETE FROM contacts WHERE id IN (:ids)")
    suspend fun deleteContactsByIds(ids: List<Long>)
    
    @Query("SELECT COUNT(*) FROM contacts")
    fun getContactCount(): Flow<Int>
}
