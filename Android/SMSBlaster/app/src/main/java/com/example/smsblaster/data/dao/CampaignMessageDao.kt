package com.example.smsblaster.data.dao

import androidx.room.*
import com.example.smsblaster.data.model.CampaignMessage
import com.example.smsblaster.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignMessageDao {
    @Query("SELECT * FROM campaign_messages WHERE campaignId = :campaignId ORDER BY id ASC")
    fun getMessagesByCampaignId(campaignId: Long): Flow<List<CampaignMessage>>
    
    @Query("SELECT * FROM campaign_messages WHERE campaignId = :campaignId AND status = :status ORDER BY id ASC")
    fun getMessagesByStatus(campaignId: Long, status: MessageStatus): Flow<List<CampaignMessage>>
    
    @Query("SELECT * FROM campaign_messages WHERE campaignId = :campaignId AND status = :status ORDER BY id ASC LIMIT :limit")
    suspend fun getPendingMessages(campaignId: Long, status: MessageStatus = MessageStatus.PENDING, limit: Int = 10): List<CampaignMessage>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CampaignMessage): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CampaignMessage>): List<Long>
    
    @Update
    suspend fun updateMessage(message: CampaignMessage)
    
    @Query("UPDATE campaign_messages SET status = :status, sentAt = :sentAt, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: MessageStatus, sentAt: Long? = null, errorMessage: String? = null)
    
    @Query("DELETE FROM campaign_messages WHERE campaignId = :campaignId")
    suspend fun deleteMessagesByCampaignId(campaignId: Long)
    
    @Query("SELECT COUNT(*) FROM campaign_messages WHERE campaignId = :campaignId")
    suspend fun getMessageCount(campaignId: Long): Int
    
    @Query("SELECT COUNT(*) FROM campaign_messages WHERE campaignId = :campaignId AND status = :status")
    suspend fun getMessageCountByStatus(campaignId: Long, status: MessageStatus): Int
}
