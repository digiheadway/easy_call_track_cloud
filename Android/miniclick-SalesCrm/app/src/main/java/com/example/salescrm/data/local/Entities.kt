package com.example.salescrm.data.local

import androidx.room.*
import com.example.salescrm.data.TaskStatus
import com.example.salescrm.data.TaskType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val alternativePhone: String = "",
    val address: String = "",
    val about: String = "",
    val note: String = "",
    val labels: List<String> = emptyList(),
    val segmentId: String = "new",
    val sourceId: String = "other",
    val budget: String = "",
    val priorityId: String = "normal",
    val isInPipeline: Boolean = false,
    val stageId: String = "fresh",
    val pipelinePriorityId: String = "normal",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastSyncedAt: LocalDateTime? = null,
    val lastOpenedAt: LocalDateTime? = null,
    val lastActivityAt: LocalDateTime? = null,
    val activityCount: Int = 0
)

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["linkedPersonId"])],
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedPersonId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val linkedPersonId: Int?,
    val type: TaskType,
    val description: String = "",
    val dueDate: LocalDate,
    val dueTime: LocalTime? = null,
    val priorityId: String = "normal",
    val status: TaskStatus = TaskStatus.PENDING,
    val showReminder: Boolean = false,
    val reminderMinutesBefore: Int = 30,
    val response: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastSyncedAt: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null
)

@Entity(
    tableName = "activities",
    indices = [Index(value = ["personId"])],
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personId: Int,
    val type: String, // ActivityType enum name
    val title: String? = null,
    val description: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastSyncedAt: LocalDateTime? = null,
    val recordingPath: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Entity(tableName = "custom_items")
data class CustomItemEntity(
    @PrimaryKey val id: String,
    val type: String, // STAGE, PRIORITY, SEGMENT, SOURCE
    val label: String,
    val color: Long,
    val order: Int = 0
)

@Entity(
    tableName = "call_history",
    foreignKeys = [
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedPersonId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CallHistoryEntity(
    @PrimaryKey val id: Long, // Use system call log ID
    val phoneNumber: String,
    val normalizedNumber: String,
    val contactName: String? = null,
    val callType: String, // Enum name
    val duration: Long,
    val timestamp: LocalDateTime,
    val isNew: Boolean = false,
    val recordingPath: String? = null,
    val linkedPersonId: Int? = null,
    val note: String? = null, // Call specific note
    val subscriptionId: String? = null
)
