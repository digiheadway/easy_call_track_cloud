package com.example.smsblaster.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.smsblaster.data.Converters

enum class CampaignStatus {
    DRAFT,
    SCHEDULED,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED
}

@Entity(
    tableName = "campaigns",
    foreignKeys = [
        ForeignKey(
            entity = Template::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("templateId")]
)
@TypeConverters(Converters::class)
data class Campaign(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val status: CampaignStatus = CampaignStatus.DRAFT,
    val templateId: Long? = null,
    val recipientIds: List<Long> = emptyList(),
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val totalCount: Int = 0,
    val scheduledAt: Long? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val remainingCount: Int get() = totalCount - sentCount - failedCount
    val progress: Float get() = if (totalCount > 0) (sentCount + failedCount).toFloat() / totalCount else 0f
}
