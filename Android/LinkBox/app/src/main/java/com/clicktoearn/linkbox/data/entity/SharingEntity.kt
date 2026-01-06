package com.clicktoearn.linkbox.data.entity

import androidx.room.*

enum class PrivacyType {
    PRIVATE, PUBLIC, TOKEN
}

@Entity(
    tableName = "sharing",
    foreignKeys = [
        ForeignKey(
            entity = EntityItem::class,
            parentColumns = ["id"],
            childColumns = ["entity_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("entity_id"),
        Index("token", unique = true)
    ]
)
data class SharingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "entity_id")
    val entityId: Long,
    val name: String? = null,
    @ColumnInfo(name = "owner_name")
    val ownerName: String? = null,
    val privacy: PrivacyType = PrivacyType.PRIVATE,
    val token: String? = null,
    @ColumnInfo(name = "public_upto")
    val publicUpto: Long? = null,
    
    val clicks: Int = 0,
    val views: Int = 0,
    @ColumnInfo(name = "unique_visits")
    val uniqueVisits: Int = 0,
    @ColumnInfo(name = "new_users")
    val newUsers: Int = 0,
    val referrer: String? = null,
    val downloads: Int = 0,
    
    @ColumnInfo(name = "points_required")
    val pointsRequired: Int = 1,
    
    // Sharing Settings
    @ColumnInfo(name = "sync_changes")
    val syncChanges: Boolean = true,  // Whether changes sync to viewers
    @ColumnInfo(name = "allow_duplicate")
    val allowDuplicate: Boolean = false,  // Allow viewers to duplicate content
    @ColumnInfo(name = "allow_external_sharing")
    val allowExternalSharing: Boolean = false,  // Allow sharing outside app (screenshots, URL copy, etc.)
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
