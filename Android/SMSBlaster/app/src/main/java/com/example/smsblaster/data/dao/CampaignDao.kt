package com.example.smsblaster.data.dao

import androidx.room.*
import com.example.smsblaster.data.model.Campaign
import com.example.smsblaster.data.model.CampaignStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns ORDER BY updatedAt DESC")
    fun getAllCampaigns(): Flow<List<Campaign>>
    
    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun getCampaignById(id: Long): Campaign?
    
    @Query("SELECT * FROM campaigns WHERE status = :status ORDER BY updatedAt DESC")
    fun getCampaignsByStatus(status: CampaignStatus): Flow<List<Campaign>>
    
    @Query("SELECT * FROM campaigns WHERE name LIKE :query")
    fun searchCampaigns(query: String): Flow<List<Campaign>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: Campaign): Long
    
    @Update
    suspend fun updateCampaign(campaign: Campaign)
    
    @Delete
    suspend fun deleteCampaign(campaign: Campaign)
    
    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun deleteCampaignById(id: Long)
    
    @Query("UPDATE campaigns SET status = :status, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateCampaignStatus(id: Long, status: CampaignStatus, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE campaigns SET sentCount = sentCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementSentCount(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE campaigns SET failedCount = failedCount + 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun incrementFailedCount(id: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM campaigns")
    fun getCampaignCount(): Flow<Int>
}
