package com.clicktoearn.linkbox.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a link that the user has joined/accessed.
 * This stores the history of links the user has opened via tokens.
 */
@Entity(tableName = "joined_links")
data class JoinedLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // The sharing token used to access the link
    val token: String,
    
    // Display name of the link
    val name: String,
    
    // Type of entity (LINK, FOLDER, PAGE)
    val type: EntityType = EntityType.LINK,
    
    // The actual URL/value of the link
    val url: String? = null,
    
    // Author/creator information
    @ColumnInfo(name = "author_name")
    val authorName: String? = null,
    
    // Points required to open this link
    @ColumnInfo(name = "points_required")
    val pointsRequired: Int = 0,
    
    // First time user accessed this link
    @ColumnInfo(name = "first_access_time")
    val firstAccessTime: Long = System.currentTimeMillis(),
    
    // Last time user accessed this link
    @ColumnInfo(name = "last_access_time")
    val lastAccessTime: Long = System.currentTimeMillis(),
    
    // When the link was last updated by the author
    @ColumnInfo(name = "last_updated_time")
    val lastUpdatedTime: Long = System.currentTimeMillis(),
    
    // Total number of times user accessed this link
    @ColumnInfo(name = "access_count")
    val accessCount: Int = 1,
    
    // Whether the link is still active/available
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    // Whether the link is starred/favorited by the user
    @ColumnInfo(name = "is_starred", defaultValue = "0")
    val isStarred: Boolean = false
)
