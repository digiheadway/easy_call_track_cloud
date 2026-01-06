package com.clicktoearn.linkbox.data.remote.model

import com.google.firebase.Timestamp

data class FirestoreEntity(
    val id: String = "",
    val name: String = "",
    val type: String = "LINK", // LINK, FOLDER, PAGE
    val value: String? = null,
    val description: String? = null,
    val parentId: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class FirestoreSharing(
    val token: String = "",
    val entityId: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val name: String = "",
    val privacy: String = "PUBLIC", // PUBLIC, PRIVATE, TOKEN
    val pointsRequired: Int = 0,
    val publicUpto: Long? = null,
    val syncChanges: Boolean = true,
    val allowDuplicate: Boolean = false,
    val allowExternalSharing: Boolean = false,
    val views: Int = 0,
    val clicks: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class FirestoreJoinedLink(
    val id: String = "",
    val token: String = "",
    val name: String = "",
    val type: String = "LINK",
    val url: String? = null,
    val authorName: String? = null,
    val pointsRequired: Int = 0,
    val isStarred: Boolean = false,
    val isActive: Boolean = true,
    val firstAccessTime: Long = 0,
    val lastAccessTime: Long = 0,
    val lastUpdatedTime: Long = 0,
    val accessCount: Int = 0
)

// ==================== MAPPERS ====================

fun com.clicktoearn.linkbox.data.entity.EntityItem.toFirestore(): FirestoreEntity {
    return FirestoreEntity(
        id = this.id.toString(),
        name = this.name,
        type = this.type.name,
        value = this.value,
        description = this.description,
        parentId = this.parentId?.toString(),
        isPinned = this.isPinned,
        createdAt = Timestamp(this.createdAt / 1000, 0),
        updatedAt = Timestamp(this.updatedAt / 1000, 0)
    )
}

fun com.clicktoearn.linkbox.data.entity.SharingEntity.toFirestore(userId: String, userName: String): FirestoreSharing {
    return FirestoreSharing(
        token = this.token ?: "",
        entityId = this.entityId.toString(),
        ownerId = userId,
        ownerName = userName,
        name = this.name ?: "",
        privacy = this.privacy.name,
        pointsRequired = this.pointsRequired,
        publicUpto = this.publicUpto,
        syncChanges = this.syncChanges,
        allowDuplicate = this.allowDuplicate,
        allowExternalSharing = this.allowExternalSharing,
        views = this.views,
        clicks = this.clicks,
        createdAt = Timestamp(this.createdAt / 1000, 0),
        updatedAt = Timestamp(this.updatedAt / 1000, 0)
    )
}

fun FirestoreEntity.toEntityItem(ownerId: Long): com.clicktoearn.linkbox.data.entity.EntityItem {
    return com.clicktoearn.linkbox.data.entity.EntityItem(
        id = this.id.toLongOrNull() ?: 0L,
        ownerId = ownerId,
        type = try { com.clicktoearn.linkbox.data.entity.EntityType.valueOf(this.type) } catch (e: Exception) { com.clicktoearn.linkbox.data.entity.EntityType.LINK },
        name = this.name,
        value = this.value,
        description = this.description,
        parentId = this.parentId?.toLongOrNull(),
        isPinned = this.isPinned,
        createdAt = this.createdAt.seconds * 1000 + this.createdAt.nanoseconds / 1000000,
        updatedAt = this.updatedAt.seconds * 1000 + this.updatedAt.nanoseconds / 1000000
    )
}

fun FirestoreSharing.toSharingEntity(): com.clicktoearn.linkbox.data.entity.SharingEntity {
    return com.clicktoearn.linkbox.data.entity.SharingEntity(
        token = this.token,
        entityId = this.entityId.toLongOrNull() ?: 0L,
        name = this.name,
        ownerName = this.ownerName,
        privacy = try { com.clicktoearn.linkbox.data.entity.PrivacyType.valueOf(this.privacy) } catch (e: Exception) { com.clicktoearn.linkbox.data.entity.PrivacyType.PUBLIC },
        pointsRequired = this.pointsRequired,
        publicUpto = this.publicUpto,
        syncChanges = this.syncChanges,
        allowDuplicate = this.allowDuplicate,
        allowExternalSharing = this.allowExternalSharing,
        views = this.views,
        clicks = this.clicks,
        createdAt = this.createdAt.seconds * 1000 + this.createdAt.nanoseconds / 1000000,
        updatedAt = this.updatedAt.seconds * 1000 + this.updatedAt.nanoseconds / 1000000
    )
}

fun com.clicktoearn.linkbox.data.entity.JoinedLinkEntity.toFirestore(): FirestoreJoinedLink {
    return FirestoreJoinedLink(
        id = this.id.toString(),
        token = this.token,
        name = this.name,
        type = this.type.name,
        url = this.url,
        authorName = this.authorName,
        pointsRequired = this.pointsRequired,
        isStarred = this.isStarred,
        isActive = this.isActive,
        firstAccessTime = this.firstAccessTime,
        lastAccessTime = this.lastAccessTime,
        lastUpdatedTime = this.lastUpdatedTime,
        accessCount = this.accessCount
    )
}

fun FirestoreJoinedLink.toJoinedLinkEntity(): com.clicktoearn.linkbox.data.entity.JoinedLinkEntity {
    return com.clicktoearn.linkbox.data.entity.JoinedLinkEntity(
        id = this.id.toLongOrNull() ?: 0L,
        token = this.token,
        name = this.name,
        type = try { com.clicktoearn.linkbox.data.entity.EntityType.valueOf(this.type) } catch (e: Exception) { com.clicktoearn.linkbox.data.entity.EntityType.LINK },
        url = this.url,
        authorName = this.authorName,
        pointsRequired = this.pointsRequired,
        isStarred = this.isStarred,
        isActive = this.isActive,
        firstAccessTime = this.firstAccessTime,
        lastAccessTime = this.lastAccessTime,
        lastUpdatedTime = this.lastUpdatedTime,
        accessCount = this.accessCount
    )
}

