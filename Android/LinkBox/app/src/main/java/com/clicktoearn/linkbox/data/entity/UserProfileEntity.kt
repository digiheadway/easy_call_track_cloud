package com.clicktoearn.linkbox.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the user's profile data stored locally.
 * This entity stores core user information that can be synced with a remote database.
 * Single row entity with id = 1 for the current user.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Long = 1,
    
    // Basic Information
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,
    
    // Account Status
    @ColumnInfo(name = "is_verified")
    val isVerified: Boolean = false,
    
    @ColumnInfo(name = "is_premium")
    val isPremium: Boolean = false,
    
    // Timestamps
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: Long? = null
)
