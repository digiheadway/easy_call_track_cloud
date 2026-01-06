package com.clicktoearn.linkbox.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Enum representing the type of entity in the workspace.
 */
enum class EntityType {
    LINK,   // A URL/link that can be shared
    PAGE,   // A rich text page with content
    FOLDER  // A container for other entities
}

/**
 * Represents an entity in the user's workspace.
 * Can be a Link, Page, or Folder.
 * 
 * Links contain URLs that can be shared publicly.
 * Pages contain rich text content.
 * Folders organize other entities hierarchically.
 * 
 * Each entity belongs to an owner and can optionally have a parent folder.
 */
@Entity(
    tableName = "entities",
    foreignKeys = [
        ForeignKey(
            entity = OwnerEntity::class,
            parentColumns = ["id"],
            childColumns = ["owner_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EntityItem::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("owner_id"),
        Index("parent_id"),
        Index("type"),
        Index("createdAt"),
        Index("updatedAt"),
        Index(value = ["owner_id", "type"]),
        Index(value = ["owner_id", "parent_id"])
    ]
)
data class EntityItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "owner_id")
    val ownerId: Long,
    
    val type: EntityType,
    
    val name: String,
    
    @ColumnInfo(name = "parent_id")
    val parentId: Long? = null,
    
    // For LINK: the URL
    // For PAGE: the rich text content
    // For FOLDER: can store metadata/description
    val value: String? = null,
    
    // Optional description for any entity type
    val description: String? = null,
    
    // Icon/emoji for visual customization
    val icon: String? = null,
    
    // Color for visual organization (hex format)
    val color: String? = null,
    
    // Whether the entity is pinned/starred
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,
    
    // Whether the entity is archived
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    
    // Soft delete support
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if this entity is a link.
     */
    fun isLink(): Boolean = type == EntityType.LINK
    
    /**
     * Check if this entity is a page.
     */
    fun isPage(): Boolean = type == EntityType.PAGE
    
    /**
     * Check if this entity is a folder.
     */
    fun isFolder(): Boolean = type == EntityType.FOLDER
    
    /**
     * Get the URL if this is a link entity.
     */
    fun getUrl(): String? = if (isLink()) value else null
    
    /**
     * Get the content if this is a page entity.
     */
    fun getContent(): String? = if (isPage()) value else null
}
