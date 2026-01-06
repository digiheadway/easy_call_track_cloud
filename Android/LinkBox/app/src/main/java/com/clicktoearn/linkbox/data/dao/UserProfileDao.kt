package com.clicktoearn.linkbox.data.dao

import androidx.room.*
import com.clicktoearn.linkbox.data.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for User Profile operations.
 * Handles all database operations related to user profile data.
 */
@Dao
interface UserProfileDao {
    
    // ==================== Create ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)
    
    // ==================== Read ====================
    
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<UserProfileEntity?>
    
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): UserProfileEntity?
    
    // ==================== Update ====================
    
    @Update
    suspend fun updateProfile(profile: UserProfileEntity)
    
    @Query("UPDATE user_profile SET name = :name, updated_at = :timestamp WHERE id = 1")
    suspend fun updateName(name: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profile SET email = :email, updated_at = :timestamp WHERE id = 1")
    suspend fun updateEmail(email: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profile SET phone = :phone, updated_at = :timestamp WHERE id = 1")
    suspend fun updatePhone(phone: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profile SET avatar_url = :avatarUrl, updated_at = :timestamp WHERE id = 1")
    suspend fun updateAvatar(avatarUrl: String?, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profile SET is_premium = :isPremium, updated_at = :timestamp WHERE id = 1")
    suspend fun updatePremiumStatus(isPremium: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_profile SET last_sync_at = :syncTime WHERE id = 1")
    suspend fun updateLastSync(syncTime: Long = System.currentTimeMillis())
    
    // ==================== Delete ====================
    
    @Query("DELETE FROM user_profile WHERE id = 1")
    suspend fun deleteProfile()
}
