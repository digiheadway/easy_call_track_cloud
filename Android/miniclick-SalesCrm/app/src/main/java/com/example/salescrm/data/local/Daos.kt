package com.example.salescrm.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface SalesDao {
    // People
    @Query("SELECT * FROM people ORDER BY id DESC")
    fun getAllPeople(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY id DESC")
    suspend fun getAllPeopleSync(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getPersonById(id: Int): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, dueTime ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, dueTime ASC")
    suspend fun getAllTasksSync(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE linkedPersonId = :personId")
    fun getTasksForPerson(personId: Int): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM activities")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities")
    suspend fun getAllActivitiesSync(): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE personId = :personId ORDER BY timestamp DESC")
    fun getActivitiesForPerson(personId: Int): Flow<List<ActivityEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity): Long
    
    @Delete
    suspend fun deleteActivity(activity: ActivityEntity)


    // Custom Items
    @Query("SELECT * FROM custom_items WHERE type = :type ORDER BY `order` ASC")
    fun getCustomItemsByType(type: String): Flow<List<CustomItemEntity>>
    
    @Query("SELECT * FROM custom_items")
    fun getAllCustomItems(): Flow<List<CustomItemEntity>>
    
    @Query("SELECT * FROM custom_items")
    suspend fun getAllCustomItemsOnce(): List<CustomItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomItems(items: List<CustomItemEntity>)

    @Query("DELETE FROM custom_items WHERE type = :type")
    suspend fun deleteCustomItemsByType(type: String)

    @Query("DELETE FROM custom_items WHERE id = :id AND type = :type")
    suspend fun deleteCustomItemByIdAndType(id: String, type: String)

    @Query("UPDATE people SET lastActivityAt = :timestamp, activityCount = activityCount + 1 WHERE id = :id")
    suspend fun updatePersonActivityStats(id: Int, timestamp: LocalDateTime)

    @Query("UPDATE people SET lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updatePersonSyncTimestamp(id: Int, timestamp: LocalDateTime)

    @Query("UPDATE tasks SET lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateTaskSyncTimestamp(id: Int, timestamp: LocalDateTime)

    @Query("UPDATE activities SET lastSyncedAt = :timestamp WHERE id = :id")
    suspend fun updateActivitySyncTimestamp(id: Int, timestamp: LocalDateTime)

    // Call History
    @Query("""
        SELECT * FROM call_history 
        WHERE timestamp >= :fromDate AND timestamp <= :toDate
        AND (:callType IS NULL OR callType = :callType)
        ORDER BY timestamp DESC
    """)
    fun getFilteredCallHistory(
        fromDate: LocalDateTime, 
        toDate: LocalDateTime, 
        callType: String?
    ): Flow<List<CallHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallHistory(entries: List<CallHistoryEntity>)

    @Query("SELECT MAX(timestamp) FROM call_history")
    suspend fun getLastCallTimestamp(): String?

    @Query("DELETE FROM people")
    suspend fun deleteAllPeople()

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM activities")
    suspend fun deleteAllActivities()

    @Query("DELETE FROM custom_items")
    suspend fun deleteAllCustomItems()
}
