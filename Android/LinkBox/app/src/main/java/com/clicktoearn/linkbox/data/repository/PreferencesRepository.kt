package com.clicktoearn.linkbox.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linkbox_settings")

class PreferencesRepository(private val context: Context) {
    
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        
        // Subscription keys
        private val SUBSCRIPTION_ACTIVE_KEY = booleanPreferencesKey("subscription_active")
        private val SUBSCRIPTION_EXPIRY_KEY = longPreferencesKey("subscription_expiry")
        private val SUBSCRIPTION_PURCHASE_DATE_KEY = longPreferencesKey("subscription_purchase_date")
        
        // Subscription constants
        const val SUBSCRIPTION_PRICE = 199 // 199
        const val SUBSCRIPTION_DURATION_DAYS = 7
        const val SUBSCRIPTION_DURATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 days in milliseconds
    }
    
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE_KEY] ?: true // Default to dark mode
    }
    
    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true
    }
    
    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY] ?: "Demo User"
    }
    
    val userEmail: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY] ?: "demo@example.com"
    }
    
    // Subscription status - checks both flag and expiry
    val isSubscriptionActive: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val isActive = preferences[SUBSCRIPTION_ACTIVE_KEY] ?: false
        val expiryTime = preferences[SUBSCRIPTION_EXPIRY_KEY] ?: 0L
        isActive && System.currentTimeMillis() < expiryTime
    }
    
    val subscriptionExpiryTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[SUBSCRIPTION_EXPIRY_KEY] ?: 0L
    }
    
    val subscriptionPurchaseDate: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[SUBSCRIPTION_PURCHASE_DATE_KEY] ?: 0L
    }
    
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun updateUserProfile(name: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
            preferences[USER_EMAIL_KEY] = email
        }
    }
    
    /**
     * Activates subscription for 7 days from current time
     */
    suspend fun activateSubscription() {
        val currentTime = System.currentTimeMillis()
        val expiryTime = currentTime + SUBSCRIPTION_DURATION_MS
        
        context.dataStore.edit { preferences ->
            preferences[SUBSCRIPTION_ACTIVE_KEY] = true
            preferences[SUBSCRIPTION_EXPIRY_KEY] = expiryTime
            preferences[SUBSCRIPTION_PURCHASE_DATE_KEY] = currentTime
        }
    }
    
    /**
     * Extends subscription by 7 more days from current expiry (or from now if expired)
     */
    suspend fun extendSubscription() {
        context.dataStore.edit { preferences ->
            val currentExpiry = preferences[SUBSCRIPTION_EXPIRY_KEY] ?: 0L
            val currentTime = System.currentTimeMillis()
            
            // If already expired, start from now; otherwise extend from current expiry
            val baseTime = if (currentExpiry > currentTime) currentExpiry else currentTime
            val newExpiry = baseTime + SUBSCRIPTION_DURATION_MS
            
            preferences[SUBSCRIPTION_ACTIVE_KEY] = true
            preferences[SUBSCRIPTION_EXPIRY_KEY] = newExpiry
            if (preferences[SUBSCRIPTION_PURCHASE_DATE_KEY] == null || preferences[SUBSCRIPTION_PURCHASE_DATE_KEY] == 0L) {
                preferences[SUBSCRIPTION_PURCHASE_DATE_KEY] = currentTime
            }
        }
    }
    
    /**
     * Deactivates subscription (useful for testing or manual cancellation)
     */
    suspend fun deactivateSubscription() {
        context.dataStore.edit { preferences ->
            preferences[SUBSCRIPTION_ACTIVE_KEY] = false
        }
    }
}
