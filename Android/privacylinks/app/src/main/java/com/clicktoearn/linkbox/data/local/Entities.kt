package com.clicktoearn.linkbox.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class AssetType {
    FILE, FOLDER, LINK
}

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "current_user",
    val name: String,
    val type: AssetType,
    val content: String,
    val parentId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Settings
    val isStarred: Boolean = false,
    val sharingEnabled: Boolean = true,
    val pointCost: Int = 0,
    val allowSaveCopy: Boolean = false,
    val shareOutsideApp: Boolean = true,
    val description: String = "",
    val allowScreenCapture: Boolean = false,
    val exposeUrl: Boolean = false,
    val chargeEveryTime: Boolean = true
)

@Entity(tableName = "links", indices = [androidx.room.Index(value = ["token"], unique = true)])
data class SharingLinkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val token: String,
    val name: String,
    val expiry: Long?,
    val status: String = "ACTIVE",
    val views: Int = 0,
    val users: Int = 0,
    val newUsers: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isActive: Boolean
        get() = status == "ACTIVE"
}

/**
 * Local-first history entity.
 * Only stores the token and local flags.
 * Asset details (name, type, owner) are fetched from network/cache when displaying.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val token: String,
    val accessedAt: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false,
    val isPaid: Boolean = false
)

@Entity(tableName = "points_transactions")
data class PointsTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val points: Int,
    val type: String,
    val remark: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "current_user",
    val username: String = "", // "username"
    val photoUrl: String = "",
    val points: Int = 10,
    val accessToken: String? = null,
    val joinedAt: Long = System.currentTimeMillis(),
    val isDarkMode: Boolean = false,
    val isPremium: Boolean = false,
    val premiumExpiry: Long? = null,
    val remoteId: String? = null,
    val isGuest: Boolean = false
)
