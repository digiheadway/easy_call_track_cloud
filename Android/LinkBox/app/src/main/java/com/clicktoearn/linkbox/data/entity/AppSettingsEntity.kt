package com.clicktoearn.linkbox.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents app-wide settings stored locally in Room.
 * This allows settings to be backed up and restored easily,
 * and can be synced with a remote database in the future.
 * Single row entity with id = 1 for the app settings.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Long = 1,
    
    // Appearance
    @ColumnInfo(name = "is_dark_mode")
    val isDarkMode: Boolean = true,
    
    @ColumnInfo(name = "theme_color")
    val themeColor: String = "default", // default, blue, green, purple, etc.
    
    // Notifications
    @ColumnInfo(name = "notifications_enabled")
    val notificationsEnabled: Boolean = true,
    
    @ColumnInfo(name = "email_notifications")
    val emailNotifications: Boolean = true,
    
    @ColumnInfo(name = "push_notifications")
    val pushNotifications: Boolean = true,
    
    // Subscription
    @ColumnInfo(name = "subscription_active")
    val subscriptionActive: Boolean = false,
    
    @ColumnInfo(name = "subscription_expiry")
    val subscriptionExpiry: Long? = null,
    
    @ColumnInfo(name = "subscription_purchase_date")
    val subscriptionPurchaseDate: Long? = null,
    
    // Privacy
    @ColumnInfo(name = "default_link_privacy")
    val defaultLinkPrivacy: String = "PUBLIC", // PUBLIC, PRIVATE, TOKEN
    
    @ColumnInfo(name = "show_profile_publicly")
    val showProfilePublicly: Boolean = true,
    
    // Data & Sync
    @ColumnInfo(name = "auto_sync_enabled")
    val autoSyncEnabled: Boolean = true,
    
    @ColumnInfo(name = "sync_interval_hours")
    val syncIntervalHours: Int = 6,
    
    @ColumnInfo(name = "wifi_only_sync")
    val wifiOnlySync: Boolean = false,
    
    // Analytics
    @ColumnInfo(name = "analytics_enabled")
    val analyticsEnabled: Boolean = true,
    
    // Timestamps
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
