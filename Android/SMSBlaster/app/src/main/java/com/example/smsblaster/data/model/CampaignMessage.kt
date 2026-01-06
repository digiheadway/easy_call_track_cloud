package com.example.smsblaster.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED
}

@Entity(
    tableName = "campaign_messages",
    foreignKeys = [
        ForeignKey(
            entity = Campaign::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Contact::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("campaignId"), Index("contactId")]
)
data class CampaignMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val campaignId: Long,
    val contactId: Long,
    val phoneNumber: String,
    val message: String,
    val status: MessageStatus = MessageStatus.PENDING,
    val sentAt: Long? = null,
    val errorMessage: String? = null
)
