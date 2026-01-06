package com.clicktoearn.linkbox.data.dao

import androidx.room.*
import com.clicktoearn.linkbox.data.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for App Settings operations.
 * Handles all database operations related to app configuration and settings.
 */
@Dao
interface AppSettingsDao {
    
    // ==================== Create / Initialize ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettingsEntity)
    
    // ==================== Read ====================
    
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>
    
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettingsEntity?
    
    // ==================== Update - Appearance ====================
    
    @Query("UPDATE app_settings SET is_dark_mode = :isDarkMode, updated_at = :timestamp WHERE id = 1")
    suspend fun updateDarkMode(isDarkMode: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_settings SET theme_color = :themeColor, updated_at = :timestamp WHERE id = 1")
    suspend fun updateThemeColor(themeColor: String, timestamp: Long = System.currentTimeMillis())
    
    // ==================== Update - Notifications ====================
    
    @Query("UPDATE app_settings SET notifications_enabled = :enabled, updated_at = :timestamp WHERE id = 1")
    suspend fun updateNotificationsEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_settings SET email_notifications = :enabled, updated_at = :timestamp WHERE id = 1")
    suspend fun updateEmailNotifications(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_settings SET push_notifications = :enabled, updated_at = :timestamp WHERE id = 1")
    suspend fun updatePushNotifications(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    // ==================== Update - Subscription ====================
    
    @Query("""
        UPDATE app_settings 
        SET subscription_active = :isActive, 
            subscription_expiry = :expiry, 
            subscription_purchase_date = :purchaseDate,
            updated_at = :timestamp 
        WHERE id = 1
    """)
    suspend fun updateSubscription(
        isActive: Boolean,
        expiry: Long?,
        purchaseDate: Long?,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("UPDATE app_settings SET subscription_active = :isActive, updated_at = :timestamp WHERE id = 1")
    suspend fun updateSubscriptionStatus(isActive: Boolean, timestamp: Long = System.currentTimeMillis())
    
    // ==================== Update - Privacy ====================
    
    @Query("UPDATE app_settings SET default_link_privacy = :privacy, updated_at = :timestamp WHERE id = 1")
    suspend fun updateDefaultLinkPrivacy(privacy: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_settings SET show_profile_publicly = :show, updated_at = :timestamp WHERE id = 1")
    suspend fun updateShowProfilePublicly(show: Boolean, timestamp: Long = System.currentTimeMillis())
    
    // ==================== Update - Data & Sync ====================
    
    @Query("UPDATE app_settings SET auto_sync_enabled = :enabled, updated_at = :timestamp WHERE id = 1")
    suspend fun updateAutoSync(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_settings SET sync_interval_hours = :hours, updated_at = :timestamp WHERE id = 1")
    suspend fun updateSyncInterval(hours: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_settings SET wifi_only_sync = :wifiOnly, updated_at = :timestamp WHERE id = 1")
    suspend fun updateWifiOnlySync(wifiOnly: Boolean, timestamp: Long = System.currentTimeMillis())
    
    // ==================== Update - Analytics ====================
    
    @Query("UPDATE app_settings SET analytics_enabled = :enabled, updated_at = :timestamp WHERE id = 1")
    suspend fun updateAnalyticsEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    // ==================== Full Update ====================
    
    @Update
    suspend fun updateSettings(settings: AppSettingsEntity)
    
    // ==================== Delete ====================
    
    @Query("DELETE FROM app_settings WHERE id = 1")
    suspend fun deleteSettings()
}
