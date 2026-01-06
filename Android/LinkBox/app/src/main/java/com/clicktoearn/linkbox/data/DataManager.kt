package com.clicktoearn.linkbox.data

import android.content.Context
import com.clicktoearn.linkbox.data.database.AppDatabase
import com.clicktoearn.linkbox.data.repository.*

/**
 * Data Manager provides a centralized access point to all repositories.
 * 
 * This singleton class manages the initialization of the database
 * and all repositories, ensuring consistent access throughout the app.
 * 
 * Usage:
 * ```
 * val dataManager = DataManager.getInstance(context)
 * val links = dataManager.linkBoxRepository.getAllSharings()
 * ```
 * 
 * For ViewModel usage, prefer injecting repositories directly or
 * using the repositories from the ViewModel itself.
 */
class DataManager private constructor(context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    
    /**
     * Repository for core LinkBox operations:
     * - Owners, Entities (Links/Pages/Folders), Sharings
     * - Joined Links
     * - Points system
     */
    val linkBoxRepository: LinkBoxRepository by lazy {
        LinkBoxRepository(database.linkBoxDao())
    }
    
    /**
     * Repository for user profile management.
     */
    val userProfileRepository: UserProfileRepository by lazy {
        UserProfileRepository(database.userProfileDao())
    }
    
    /**
     * Repository for app settings (stored in Room).
     * Handles appearance, notifications, subscription, privacy, sync settings.
     */
    val appSettingsRepository: AppSettingsRepository by lazy {
        AppSettingsRepository(database.appSettingsDao())
    }
    
    /**
     * Repository for link analytics tracking.
     */
    val analyticsRepository: AnalyticsRepository by lazy {
        AnalyticsRepository(database.analyticsDao())
    }
    
    /**
     * Legacy preferences repository (DataStore-based).
     * Kept for backward compatibility. Consider migrating to AppSettingsRepository.
     */
    @Deprecated("Use appSettingsRepository instead for Room-based settings")
    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: DataManager? = null
        
        /**
         * Get the singleton instance of DataManager.
         * 
         * @param context Application context (will be converted to application context)
         * @return The DataManager singleton instance
         */
        fun getInstance(context: Context): DataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        /**
         * Clear the singleton instance.
         * Useful for testing or when clearing all data.
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
