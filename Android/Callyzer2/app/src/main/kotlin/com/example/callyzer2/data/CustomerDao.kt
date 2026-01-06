package com.example.callyzer2.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): LiveData<List<Customer>>

    @Query("SELECT * FROM customers WHERE phoneNumber LIKE :phoneNumber")
    fun findCustomerByPhoneSync(phoneNumber: String): Customer?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    fun searchCustomers(query: String): LiveData<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomersSync(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun getCustomerById(customerId: Long): LiveData<Customer>
}
