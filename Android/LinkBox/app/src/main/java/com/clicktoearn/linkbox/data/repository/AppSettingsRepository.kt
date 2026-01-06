package com.clicktoearn.linkbox.data.repository

import com.clicktoearn.linkbox.data.dao.AppSettingsDao
import com.clicktoearn.linkbox.data.entity.AppSettingsEntity
import com.clicktoearn.linkbox.data.entity.PrivacyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for App Settings operations.
 * Handles all app configuration stored in Room database.
 * 
 * This provides a unified settings management that can be:
 * - Backed up and restored
 * - Synced with remote server
 * - Easily accessed from anywhere in the app
 * 
 * Subscription constants are defined here for easy modification.
 */
class AppSettingsRepository(private val dao: AppSettingsDao) {
    
    companion object {
        // Subscription configuration
        const val SUBSCRIPTION_PRICE = 199 // 199
        const val SUBSCRIPTION_DURATION_DAYS = 7
        const val SUBSCRIPTION_DURATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 days in milliseconds
    }
    
    // ==================== CREATE / INITIALIZE ====================
    
    /**
     * Initialize app settings with default values if not exists.
     */
    suspend fun initializeSettings() {
        val existing = dao.getSettingsSync()
        if (existing == null) {
            dao.insertSettings(AppSettingsEntity())
        }
    }
    
    /**
     * Save complete settings.
     */
    suspend fun saveSettings(settings: AppSettingsEntity) {
        dao.insertSettings(settings)
    }
    
    // ==================== READ ====================
    
    /**
     * Get full settings as a Flow for reactive updates.
     */
    fun getSettings(): Flow<AppSettingsEntity?> = dao.getSettings()
    
    /**
     * Get settings synchronously.
     */
    suspend fun getSettingsSync(): AppSettingsEntity? = dao.getSettingsSync()
    
    // ==================== APPEARANCE ====================
    
    /**
     * Get dark mode status as Flow.
     */
    fun isDarkMode(): Flow<Boolean> = dao.getSettings().map { it?.isDarkMode ?: true }
    
    /**
     * Update dark mode setting.
     */
    suspend fun setDarkMode(enabled: Boolean) {
        ensureSettingsExist()
        dao.updateDarkMode(enabled)
    }
    
    /**
     * Get theme color.
     */
    fun getThemeColor(): Flow<String> = dao.getSettings().map { it?.themeColor ?: "default" }
    
    /**
     * Update theme color.
     */
    suspend fun setThemeColor(color: String) {
        ensureSettingsExist()
        dao.updateThemeColor(color)
    }
    
    // ==================== NOTIFICATIONS ====================
    
    /**
     * Get notifications enabled status.
     */
    fun isNotificationsEnabled(): Flow<Boolean> = dao.getSettings().map { it?.notificationsEnabled ?: true }
    
    /**
     * Update notifications enabled status.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        ensureSettingsExist()
        dao.updateNotificationsEnabled(enabled)
    }
    
    /**
     * Get email notifications status.
     */
    fun isEmailNotificationsEnabled(): Flow<Boolean> = dao.getSettings().map { it?.emailNotifications ?: true }
    
    /**
     * Update email notifications status.
     */
    suspend fun setEmailNotifications(enabled: Boolean) {
        ensureSettingsExist()
        dao.updateEmailNotifications(enabled)
    }
    
    /**
     * Get push notifications status.
     */
    fun isPushNotificationsEnabled(): Flow<Boolean> = dao.getSettings().map { it?.pushNotifications ?: true }
    
    /**
     * Update push notifications status.
     */
    suspend fun setPushNotifications(enabled: Boolean) {
        ensureSettingsExist()
        dao.updatePushNotifications(enabled)
    }
    
    // ==================== SUBSCRIPTION ====================
    
    /**
     * Check if subscription is active (considers expiry time).
     */
    fun isSubscriptionActive(): Flow<Boolean> = dao.getSettings().map { settings ->
        val isActive = settings?.subscriptionActive ?: false
        val expiryTime = settings?.subscriptionExpiry ?: 0L
        isActive && System.currentTimeMillis() < expiryTime
    }
    
    /**
     * Get subscription expiry time.
     */
    fun getSubscriptionExpiry(): Flow<Long?> = dao.getSettings().map { it?.subscriptionExpiry }
    
    /**
     * Get subscription purchase date.
     */
    fun getSubscriptionPurchaseDate(): Flow<Long?> = dao.getSettings().map { it?.subscriptionPurchaseDate }
    
    /**
     * Get remaining subscription time in milliseconds.
     */
    fun getSubscriptionRemainingTime(): Flow<Long> = dao.getSettings().map { settings ->
        val expiryTime = settings?.subscriptionExpiry ?: 0L
        val remaining = expiryTime - System.currentTimeMillis()
        if (remaining > 0) remaining else 0L
    }
    
    /**
     * Activate subscription for the configured duration.
     */
    suspend fun activateSubscription() {
        ensureSettingsExist()
        val currentTime = System.currentTimeMillis()
        val expiryTime = currentTime + SUBSCRIPTION_DURATION_MS
        dao.updateSubscription(
            isActive = true,
            expiry = expiryTime,
            purchaseDate = currentTime
        )
    }
    
    /**
     * Extend subscription by the configured duration.
     * If expired, starts from now; otherwise extends from current expiry.
     */
    suspend fun extendSubscription() {
        ensureSettingsExist()
        val settings = dao.getSettingsSync() ?: return
        val currentTime = System.currentTimeMillis()
        val currentExpiry = settings.subscriptionExpiry ?: 0L
        
        // Start from current expiry if still active, otherwise from now
        val baseTime = if (currentExpiry > currentTime) currentExpiry else currentTime
        val newExpiry = baseTime + SUBSCRIPTION_DURATION_MS
        
        dao.updateSubscription(
            isActive = true,
            expiry = newExpiry,
            purchaseDate = settings.subscriptionPurchaseDate ?: currentTime
        )
    }
    
    /**
     * Deactivate subscription.
     */
    suspend fun deactivateSubscription() {
        ensureSettingsExist()
        dao.updateSubscriptionStatus(false)
    }
    
    // ==================== PRIVACY ====================
    
    /**
     * Get default link privacy setting.
     */
    fun getDefaultLinkPrivacy(): Flow<PrivacyType> = dao.getSettings().map { settings ->
        try {
            PrivacyType.valueOf(settings?.defaultLinkPrivacy ?: "PUBLIC")
        } catch (e: Exception) {
            PrivacyType.PUBLIC
        }
    }
    
    /**
     * Update default link privacy.
     */
    suspend fun setDefaultLinkPrivacy(privacy: PrivacyType) {
        ensureSettingsExist()
        dao.updateDefaultLinkPrivacy(privacy.name)
    }
    
    /**
     * Get show profile publicly setting.
     */
    fun isProfilePublic(): Flow<Boolean> = dao.getSettings().map { it?.showProfilePublicly ?: true }
    
    /**
     * Update show profile publicly setting.
     */
    suspend fun setProfilePublic(show: Boolean) {
        ensureSettingsExist()
        dao.updateShowProfilePublicly(show)
    }
    
    // ==================== DATA & SYNC ====================
    
    /**
     * Get auto sync enabled status.
     */
    fun isAutoSyncEnabled(): Flow<Boolean> = dao.getSettings().map { it?.autoSyncEnabled ?: true }
    
    /**
     * Update auto sync enabled status.
     */
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        ensureSettingsExist()
        dao.updateAutoSync(enabled)
    }
    
    /**
     * Get sync interval in hours.
     */
    fun getSyncInterval(): Flow<Int> = dao.getSettings().map { it?.syncIntervalHours ?: 6 }
    
    /**
     * Update sync interval.
     */
    suspend fun setSyncInterval(hours: Int) {
        ensureSettingsExist()
        dao.updateSyncInterval(hours)
    }
    
    /**
     * Get WiFi only sync status.
     */
    fun isWifiOnlySync(): Flow<Boolean> = dao.getSettings().map { it?.wifiOnlySync ?: false }
    
    /**
     * Update WiFi only sync status.
     */
    suspend fun setWifiOnlySync(wifiOnly: Boolean) {
        ensureSettingsExist()
        dao.updateWifiOnlySync(wifiOnly)
    }
    
    // ==================== ANALYTICS ====================
    
    /**
     * Get analytics enabled status.
     */
    fun isAnalyticsEnabled(): Flow<Boolean> = dao.getSettings().map { it?.analyticsEnabled ?: true }
    
    /**
     * Update analytics enabled status.
     */
    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        ensureSettingsExist()
        dao.updateAnalyticsEnabled(enabled)
    }
    
    // ==================== FULL UPDATE ====================
    
    /**
     * Update full settings object.
     */
    suspend fun updateSettings(settings: AppSettingsEntity) {
        dao.updateSettings(settings.copy(updatedAt = System.currentTimeMillis()))
    }
    
    // ==================== DELETE / RESET ====================
    
    /**
     * Delete all settings.
     */
    suspend fun deleteSettings() {
        dao.deleteSettings()
    }
    
    /**
     * Reset settings to defaults.
     */
    suspend fun resetSettings() {
        dao.deleteSettings()
        dao.insertSettings(AppSettingsEntity())
    }
    
    // ==================== HELPER ====================
    
    /**
     * Ensure settings row exists.
     */
    private suspend fun ensureSettingsExist() {
        if (dao.getSettingsSync() == null) {
            dao.insertSettings(AppSettingsEntity())
        }
    }
}
