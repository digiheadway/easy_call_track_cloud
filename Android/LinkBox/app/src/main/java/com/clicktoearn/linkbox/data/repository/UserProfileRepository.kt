package com.clicktoearn.linkbox.data.repository

import com.clicktoearn.linkbox.data.dao.UserProfileDao
import com.clicktoearn.linkbox.data.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for User Profile operations.
 * Handles all user profile data management stored in Room database.
 * 
 * This replaces the DataStore-based profile storage for better
 * organization and future sync capabilities.
 */
class UserProfileRepository(private val dao: UserProfileDao) {
    
    // ==================== CREATE / INITIALIZE ====================
    
    /**
     * Initialize user profile with default values if not exists.
     */
    suspend fun initializeProfile() {
        val existing = dao.getProfileSync()
        if (existing == null) {
            dao.insertProfile(UserProfileEntity())
        }
    }
    
    /**
     * Create or update user profile.
     */
    suspend fun saveProfile(profile: UserProfileEntity) {
        dao.insertProfile(profile)
    }
    
    // ==================== READ ====================
    
    /**
     * Get user profile as a Flow for reactive updates.
     */
    fun getProfile(): Flow<UserProfileEntity?> = dao.getProfile()
    
    /**
     * Get user profile synchronously.
     */
    suspend fun getProfileSync(): UserProfileEntity? = dao.getProfileSync()
    
    /**
     * Get user's display name.
     */
    fun getUserName(): Flow<String> = dao.getProfile().map { it?.name ?: "User" }
    
    /**
     * Get user's email.
     */
    fun getUserEmail(): Flow<String> = dao.getProfile().map { it?.email ?: "" }
    
    /**
     * Check if user is premium.
     */
    fun isPremium(): Flow<Boolean> = dao.getProfile().map { it?.isPremium ?: false }
    
    /**
     * Check if user is verified.
     */
    fun isVerified(): Flow<Boolean> = dao.getProfile().map { it?.isVerified ?: false }
    
    // ==================== UPDATE ====================
    
    /**
     * Update the full profile.
     */
    suspend fun updateProfile(profile: UserProfileEntity) {
        dao.updateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }
    
    /**
     * Update basic profile info.
     */
    suspend fun updateProfileInfo(name: String, email: String, phone: String? = null) {
        val current = dao.getProfileSync() ?: UserProfileEntity()
        dao.updateProfile(
            current.copy(
                name = name,
                email = email,
                phone = phone,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Update user's name.
     */
    suspend fun updateName(name: String) {
        dao.updateName(name)
    }
    
    /**
     * Update user's email.
     */
    suspend fun updateEmail(email: String) {
        dao.updateEmail(email)
    }
    
    /**
     * Update user's phone.
     */
    suspend fun updatePhone(phone: String?) {
        dao.updatePhone(phone)
    }
    
    /**
     * Update user's avatar URL.
     */
    suspend fun updateAvatar(avatarUrl: String?) {
        dao.updateAvatar(avatarUrl)
    }
    
    /**
     * Update premium status.
     */
    suspend fun updatePremiumStatus(isPremium: Boolean) {
        dao.updatePremiumStatus(isPremium)
    }
    
    /**
     * Mark the last sync timestamp.
     */
    suspend fun updateLastSync() {
        dao.updateLastSync()
    }
    
    // ==================== DELETE ====================
    
    /**
     * Clear user profile (for logout).
     */
    suspend fun clearProfile() {
        dao.deleteProfile()
    }
    
    /**
     * Reset profile to default values.
     */
    suspend fun resetProfile() {
        dao.deleteProfile()
        dao.insertProfile(UserProfileEntity())
    }
}
