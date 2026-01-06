package com.clicktoearn.linkbox.data.repository

import com.clicktoearn.linkbox.data.dao.LinkBoxDao
import com.clicktoearn.linkbox.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository for core LinkBox operations.
 * Provides a clean API for ViewModel to access database operations.
 * 
 * Responsibilities:
 * - Owner management
 * - Entity (Links, Pages, Folders) CRUD operations
 * - Sharing link management
 * - Joined links tracking
 * - Points system management
 */
import com.clicktoearn.linkbox.data.remote.model.toFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Repository for core LinkBox operations.
 * Provides a clean API for ViewModel to access database operations.
 * 
 * Responsibilities:
 * - Owner management
 * - Entity (Links, Pages, Folders) CRUD operations
 * - Sharing link management
 * - Joined links tracking
 * - Points system management
 */
class LinkBoxRepository(
    private val dao: LinkBoxDao,
    private val firestoreRepository: FirestoreRepository? = null // Optional for backward compatibility/testing
) {

    // ==================== OWNER OPERATIONS ====================
    
    suspend fun createOwner(owner: OwnerEntity): Long = dao.insertOwner(owner)
    suspend fun updateOwner(owner: OwnerEntity) = dao.updateOwner(owner)
    suspend fun deleteOwner(owner: OwnerEntity) = dao.deleteOwner(owner)
    suspend fun getOwner(id: Long): OwnerEntity? = dao.getOwnerById(id)
    suspend fun getFirstOwner(): OwnerEntity? = dao.getFirstOwner()
    suspend fun getOwnerByEmail(email: String): OwnerEntity? = dao.getOwnerByEmail(email)
    fun getAllOwners(): Flow<List<OwnerEntity>> = dao.getAllOwners()

    // ==================== ENTITY OPERATIONS ====================
    
    suspend fun createEntity(entity: EntityItem): Long {
        val id = dao.insertEntity(entity)
        // Cloud Sync
        try {
            if (firestoreRepository != null) {
                val entityWithId = entity.copy(id = id)
                firestoreRepository.saveEntity(entityWithId.toFirestore())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }

    suspend fun createEntities(entities: List<EntityItem>): List<Long> {
        val ids = dao.insertEntities(entities)
        // Cloud Sync (Best effort for bulk)
        // Completing complex bulk sync is skipped for now to keep UI responsive
        return ids
    }

    suspend fun updateEntity(entity: EntityItem) {
        dao.updateEntity(entity)
        try {
            firestoreRepository?.saveEntity(entity.toFirestore())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteEntity(entity: EntityItem) {
        dao.deleteEntity(entity)
        try {
            firestoreRepository?.deleteEntity(entity.id.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteEntityById(id: Long) {
        dao.deleteEntityById(id)
        try {
            firestoreRepository?.deleteEntity(id.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun getEntity(id: Long): EntityItem? = dao.getEntityById(id)
    
    fun getRootEntities(ownerId: Long): Flow<List<EntityItem>> = dao.getRootEntitiesByOwner(ownerId)
    fun getSubEntities(parentId: Long): Flow<List<EntityItem>> = dao.getEntitiesByParent(parentId)
    fun getAllEntitiesByOwner(ownerId: Long): Flow<List<EntityItem>> = dao.getAllEntitiesByOwner(ownerId)
    fun getEntitiesByType(ownerId: Long, type: EntityType): Flow<List<EntityItem>> = dao.getEntitiesByType(ownerId, type)
    fun searchEntities(ownerId: Long, query: String): Flow<List<EntityItem>> = dao.searchEntities(ownerId, query)
    
    suspend fun countEntitiesByOwner(ownerId: Long): Int = dao.countEntitiesByOwner(ownerId)
    suspend fun countEntitiesInFolder(parentId: Long): Int = dao.countEntitiesInFolder(parentId)
    
    fun getEntitiesWithSharing(ownerId: Long) = dao.getEntitiesWithSharing(ownerId)

    // ==================== SHARING OPERATIONS ====================
    
    suspend fun createSharing(sharing: SharingEntity, userName: String): Long {
        val id = dao.insertSharing(sharing)
        try {
            val sharingWithId = sharing.copy(id = id)
            firestoreRepository?.saveSharing(sharingWithId, userName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id 
    }

    suspend fun createSharings(sharings: List<SharingEntity>): List<Long> = dao.insertSharings(sharings)

    suspend fun updateSharing(sharing: SharingEntity, userName: String) {
        dao.updateSharing(sharing)
        try {
            firestoreRepository?.saveSharing(sharing, userName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSharing(sharing: SharingEntity) {
        dao.deleteSharing(sharing)
        try {
            sharing.token?.let { firestoreRepository?.deleteSharing(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSharingById(id: Long) = dao.deleteSharingById(id)
    
    fun getSharing(entityId: Long): Flow<SharingEntity?> = dao.getSharingByEntity(entityId)
    fun getAllSharingsByEntity(entityId: Long): Flow<List<SharingEntity>> = dao.getAllSharingsByEntity(entityId)
    suspend fun getSharingSync(entityId: Long): SharingEntity? = dao.getSharingSync(entityId)
    suspend fun getSharingById(id: Long): SharingEntity? = dao.getSharingById(id)
    suspend fun getSharingByToken(token: String): SharingEntity? = dao.getSharingByToken(token)
    
    fun getAllSharings(): Flow<List<SharingEntity>> = dao.getAllSharings()
    fun getSharingsByPrivacy(privacy: PrivacyType): Flow<List<SharingEntity>> = dao.getSharingsByPrivacy(privacy)
    
    suspend fun recordClick(sharingId: Long, token: String? = null) {
        dao.incrementClicks(sharingId)
        token?.let { firestoreRepository?.incrementSharingStats(it, true) }
    }

    suspend fun recordView(sharingId: Long, token: String? = null) {
        dao.incrementViews(sharingId)
        token?.let { firestoreRepository?.incrementSharingStats(it, false) }
    }

    suspend fun recordUniqueVisit(sharingId: Long) = dao.incrementUniqueVisits(sharingId)
    suspend fun recordDownload(sharingId: Long) = dao.incrementDownloads(sharingId)
    
    suspend fun resetSharingToken(sharingId: Long, newToken: String) = dao.resetToken(sharingId, newToken)
    suspend fun updateSharingPrivacy(sharingId: Long, privacy: PrivacyType, userName: String) {
        dao.updatePrivacy(sharingId, privacy)
        try {
            val sharing = dao.getSharingById(sharingId)
            if (sharing != null) {
                firestoreRepository?.saveSharing(sharing, userName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun updateLinkExpiry(sharingId: Long, expiryTime: Long?) = dao.updateLinkExpiry(sharingId, expiryTime)
    
    // ==================== RESTORE OPERATIONS (NO SYNC) ====================

    suspend fun restoreEntities(entities: List<EntityItem>) {
        dao.insertEntities(entities)
    }

    suspend fun restoreSharings(sharings: List<SharingEntity>) {
        dao.insertSharings(sharings)
    }

    suspend fun countSharingsForEntity(entityId: Long): Int = dao.countSharingsForEntity(entityId)

    // ==================== JOINED LINKS OPERATIONS ====================
    
    suspend fun insertJoinedLink(joinedLink: JoinedLinkEntity): Long {
        val id = dao.insertJoinedLink(joinedLink)
        try {
            firestoreRepository?.saveJoinedLink(joinedLink.copy(id = id).toFirestore())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return id
    }
    suspend fun insertJoinedLinks(joinedLinks: List<JoinedLinkEntity>): List<Long> = dao.insertJoinedLinks(joinedLinks)
    suspend fun updateJoinedLink(joinedLink: JoinedLinkEntity) {
        dao.updateJoinedLink(joinedLink)
        try {
            firestoreRepository?.saveJoinedLink(joinedLink.toFirestore())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun deleteJoinedLink(joinedLink: JoinedLinkEntity) {
        dao.deleteJoinedLink(joinedLink)
        try {
            firestoreRepository?.deleteJoinedLink(joinedLink.token)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun deleteJoinedLinkById(id: Long) {
        val linked = dao.getJoinedLinkById(id)
        dao.deleteJoinedLinkById(id)
        try {
            linked?.token?.let { firestoreRepository?.deleteJoinedLink(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getAllJoinedLinks(): Flow<List<JoinedLinkEntity>> = dao.getAllJoinedLinks()
    fun getActiveJoinedLinks(): Flow<List<JoinedLinkEntity>> = dao.getActiveJoinedLinks()
    suspend fun getJoinedLinkByToken(token: String): JoinedLinkEntity? = dao.getJoinedLinkByToken(token)
    suspend fun getJoinedLinkById(id: Long): JoinedLinkEntity? = dao.getJoinedLinkById(id)
    fun searchJoinedLinks(query: String): Flow<List<JoinedLinkEntity>> = dao.searchJoinedLinks(query)
    
    suspend fun incrementJoinedLinkAccess(id: Long) = dao.incrementJoinedLinkAccess(id)
    suspend fun countJoinedLinks(): Int = dao.countJoinedLinks()
    
    fun getStarredJoinedLinks(): Flow<List<JoinedLinkEntity>> = dao.getStarredJoinedLinks()
    suspend fun toggleJoinedLinkStar(id: Long, isStarred: Boolean) = dao.setJoinedLinkStarred(id, isStarred)
    
    /**
     * Joins a link - creates new entry or updates existing one.
     * @return The ID of the joined link
     */
    suspend fun joinLink(
        token: String, 
        name: String, 
        url: String?, 
        authorName: String?, 
        pointsRequired: Int = 0,
        type: EntityType = EntityType.LINK
    ): Long {
        val existing = getJoinedLinkByToken(token)
        return if (existing != null) {
            // Update existing
            updateJoinedLink(
                existing.copy(
                    name = name,
                    url = url,
                    authorName = authorName,
                    lastAccessTime = System.currentTimeMillis(),
                    accessCount = existing.accessCount + 1
                )
            )
            existing.id
        } else {
            // Create new
            insertJoinedLink(
                JoinedLinkEntity(
                    token = token,
                    name = name,
                    type = type,
                    url = url,
                    authorName = authorName,
                    pointsRequired = pointsRequired
                )
            )
        }
    }

    // ==================== USER POINTS OPERATIONS ====================
    
    fun getUserPoints(): Flow<UserPointsEntity?> = dao.getUserPoints()
    suspend fun getUserPointsSync(): UserPointsEntity? = dao.getUserPointsSync()
    
    suspend fun initializeUserPoints() {
        val existing = dao.getUserPointsSync()
        if (existing == null) {
            dao.insertUserPoints(UserPointsEntity())
        }
    }
    
    /**
     * Add points to user's balance.
     * Automatically creates a transaction record.
     */
    suspend fun addPoints(
        amount: Int, 
        type: PointTransactionType, 
        description: String? = null, 
        referenceId: String? = null
    ) {
        val current = dao.getUserPointsSync() ?: run {
            dao.insertUserPoints(UserPointsEntity())
            UserPointsEntity()
        }
        
        val newBalance = current.currentBalance + amount
        
        val updated = when (type) {
            PointTransactionType.EARNED_AD, PointTransactionType.EARNED_REWARD -> 
                current.copy(
                    currentBalance = newBalance,
                    totalEarned = current.totalEarned + amount,
                    lastUpdated = System.currentTimeMillis()
                )
            PointTransactionType.PURCHASED -> 
                current.copy(
                    currentBalance = newBalance,
                    totalPurchased = current.totalPurchased + amount,
                    lastUpdated = System.currentTimeMillis()
                )
            PointTransactionType.REFUND -> 
                current.copy(
                    currentBalance = newBalance,
                    lastUpdated = System.currentTimeMillis()
                )
            else -> current
        }
        
        dao.insertUserPoints(updated)
        dao.insertPointTransaction(
            PointTransactionEntity(
                type = type,
                amount = amount,
                description = description,
                referenceId = referenceId
            )
        )
    }
    
    /**
     * Spend points from user's balance.
     * Automatically creates a transaction record.
     * @return true if successful, false if insufficient balance
     */
    suspend fun spendPoints(
        amount: Int, 
        description: String? = null, 
        referenceId: String? = null
    ): Boolean {
        val current = dao.getUserPointsSync() ?: return false
        if (current.currentBalance < amount) return false
        
        val updated = current.copy(
            currentBalance = current.currentBalance - amount,
            totalSpent = current.totalSpent + amount,
            lastUpdated = System.currentTimeMillis()
        )
        
        dao.insertUserPoints(updated)
        dao.insertPointTransaction(
            PointTransactionEntity(
                type = PointTransactionType.SPENT,
                amount = -amount,
                description = description,
                referenceId = referenceId
            )
        )
        return true
    }
    
    /**
     * Check if user has enough points.
     */
    suspend fun hasEnoughPoints(amount: Int): Boolean {
        val current = dao.getUserPointsSync() ?: return false
        return current.currentBalance >= amount
    }
    
    fun getAllPointTransactions(): Flow<List<PointTransactionEntity>> = dao.getAllPointTransactions()
    fun getRecentPointTransactions(limit: Int): Flow<List<PointTransactionEntity>> = dao.getRecentPointTransactions(limit)
    fun getTransactionsByType(type: PointTransactionType): Flow<List<PointTransactionEntity>> = dao.getTransactionsByType(type)
    
    suspend fun getTotalAmountByType(type: PointTransactionType): Int = dao.getTotalAmountByType(type) ?: 0
    suspend fun deleteOldTransactions(beforeTime: Long) = dao.deleteOldTransactions(beforeTime)
}
