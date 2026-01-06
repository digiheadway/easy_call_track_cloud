package com.example.callyzer2.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CallDao {

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): LiveData<List<Call>>

    @Query("SELECT * FROM calls WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getCallsByCustomer(customerId: Long): LiveData<List<Call>>

    @Query("SELECT * FROM calls WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getCallsByPhoneNumber(phoneNumber: String): LiveData<List<Call>>

    @Query("SELECT * FROM calls WHERE followUpRequired = 1")
    fun getCallsRequiringFollowUp(): LiveData<List<Call>>

    // Synchronous versions for direct database access
    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCallsSync(): List<Call>

    @Query("SELECT * FROM calls WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getCallsByCustomerSync(customerId: Long): List<Call>

    @Query("SELECT * FROM calls WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getCallsByPhoneNumberSync(phoneNumber: String): List<Call>

    @Query("SELECT * FROM calls WHERE followUpRequired = 1")
    fun getCallsRequiringFollowUpSync(): List<Call>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: Call): Long

    @Update
    suspend fun updateCall(call: Call)

    @Delete
    suspend fun deleteCall(call: Call)
}
