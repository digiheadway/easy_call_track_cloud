package com.example.salescrm.data

import com.example.salescrm.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

class CrmRepository(private val salesDao: SalesDao) {

    // People
    val allPeople: Flow<List<Person>> = salesDao.getAllPeople().map { list -> list.map { it.toDomain() } }
    
    suspend fun getAllPeopleSync(): List<Person> {
        return salesDao.getAllPeopleSync().map { it.toDomain() }
    }
    
    suspend fun savePerson(person: Person): Long {
        return salesDao.insertPerson(person.copy(updatedAt = java.time.LocalDateTime.now()).toEntity())
    }

    suspend fun deletePerson(person: Person) {
        salesDao.deletePerson(person.toEntity())
    }

    // Tasks
    val allTasks: Flow<List<Task>> = salesDao.getAllTasks().map { list -> list.map { it.toDomain() } }
    
    suspend fun getAllTasksSync(): List<Task> {
        return salesDao.getAllTasksSync().map { it.toDomain() }
    }
    
    fun getTasksForPerson(personId: Int): Flow<List<Task>> = 
        salesDao.getTasksForPerson(personId).map { list -> list.map { it.toDomain() } }

    suspend fun saveTask(task: Task): Long {
        val entity = task.copy(updatedAt = java.time.LocalDateTime.now()).toEntity()
        return if (task.id == 0) {
            salesDao.insertTask(entity)
        } else {
            salesDao.updateTask(entity)
            task.id.toLong()
        }
    }

    suspend fun deleteTask(task: Task) {
        salesDao.deleteTask(task.toEntity())
    }

    // Activities (Includes what were previously Notes/Comments)
    val allActivities: Flow<List<Activity>> = salesDao.getAllActivities().map { list -> list.map { it.toDomain() } }

    fun getActivitiesForPerson(personId: Int): Flow<List<Activity>> = 
        salesDao.getActivitiesForPerson(personId).map { list -> list.map { it.toDomain() } }

    suspend fun saveActivity(activity: Activity): Long {
        return salesDao.insertActivity(activity.copy(updatedAt = java.time.LocalDateTime.now()).toEntity())
    }

    suspend fun deleteActivity(activity: Activity) {
        salesDao.deleteActivity(activity.toEntity())
    }


    // Custom Items
    fun getCustomStages(): Flow<List<CustomItem>> = 
        salesDao.getCustomItemsByType("STAGE").map { list -> 
            list.map { it.toDomain() }.takeIf { it.isNotEmpty() } ?: defaultCustomStages 
        }

    fun getCustomPriorities(): Flow<List<CustomItem>> = 
        salesDao.getCustomItemsByType("PRIORITY").map { list -> 
            list.map { it.toDomain() }.takeIf { it.isNotEmpty() } ?: defaultCustomPriorities 
        }

    fun getCustomSegments(): Flow<List<CustomItem>> = 
        salesDao.getCustomItemsByType("SEGMENT").map { list -> 
            list.map { it.toDomain() }.takeIf { it.isNotEmpty() } ?: defaultCustomSegments 
        }

    fun getCustomSources(): Flow<List<CustomItem>> = 
        salesDao.getCustomItemsByType("SOURCE").map { list -> 
            list.map { it.toDomain() }.takeIf { it.isNotEmpty() } ?: defaultCustomSources 
        }

    suspend fun saveCustomStages(stages: List<CustomItem>) {
        salesDao.deleteCustomItemsByType("STAGE")
        salesDao.insertCustomItems(stages.map { it.toEntity("STAGE") })
    }

    suspend fun saveCustomPriorities(priorities: List<CustomItem>) {
        salesDao.deleteCustomItemsByType("PRIORITY")
        salesDao.insertCustomItems(priorities.map { it.toEntity("PRIORITY") })
    }

    suspend fun saveCustomSegments(segments: List<CustomItem>) {
        salesDao.deleteCustomItemsByType("SEGMENT")
        salesDao.insertCustomItems(segments.map { it.toEntity("SEGMENT") })
    }

    suspend fun saveCustomSources(sources: List<CustomItem>) {
        salesDao.deleteCustomItemsByType("SOURCE")
        salesDao.insertCustomItems(sources.map { it.toEntity("SOURCE") })
    }

    // Call History
    fun getFilteredCallHistory(
        fromDate: LocalDate,
        toDate: LocalDate,
        callType: CallType?
    ): Flow<List<CallLogEntry>> {
        val startDateTime = fromDate.atStartOfDay()
        val endDateTime = toDate.atTime(23, 59, 59)
        return salesDao.getFilteredCallHistory(startDateTime, endDateTime, callType?.name)
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun saveCallHistory(entries: List<CallLogEntry>) {
        salesDao.insertCallHistory(entries.map { it.toEntity() })
    }

    suspend fun clearAllData() {
        salesDao.deleteAllPeople()
        salesDao.deleteAllTasks()
        salesDao.deleteAllActivities()
        salesDao.deleteAllCustomItems()
    }
}
