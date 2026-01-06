package com.clicktoearn.linkbox.data.dao

import androidx.room.*
import com.clicktoearn.linkbox.data.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Main Data Access Object for LinkBox core entities.
 * Handles operations for Owners, Entities (Links/Pages/Folders), Sharings,
 * Joined Links, and Points system.
 */
@Dao
interface LinkBoxDao {
    
    // ==================== OWNERS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwner(owner: OwnerEntity): Long

    @Update
    suspend fun updateOwner(owner: OwnerEntity)

    @Delete
    suspend fun deleteOwner(owner: OwnerEntity)

    @Query("SELECT * FROM owners WHERE id = :id")
    suspend fun getOwnerById(id: Long): OwnerEntity?

    @Query("SELECT * FROM owners LIMIT 1")
    suspend fun getFirstOwner(): OwnerEntity?
    
    @Query("SELECT * FROM owners")
    fun getAllOwners(): Flow<List<OwnerEntity>>
    
    @Query("SELECT * FROM owners WHERE email = :email LIMIT 1")
    suspend fun getOwnerByEmail(email: String): OwnerEntity?

    // ==================== ENTITIES (Links, Pages, Folders) ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntity(entity: EntityItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntities(entities: List<EntityItem>): List<Long>

    @Update
    suspend fun updateEntity(entity: EntityItem)

    @Delete
    suspend fun deleteEntity(entity: EntityItem)
    
    @Query("DELETE FROM entities WHERE id = :id")
    suspend fun deleteEntityById(id: Long)

    @Query("SELECT * FROM entities WHERE owner_id = :ownerId AND parent_id IS NULL ORDER BY updatedAt DESC")
    fun getRootEntitiesByOwner(ownerId: Long): Flow<List<EntityItem>>

    @Query("SELECT * FROM entities WHERE parent_id = :parentId ORDER BY updatedAt DESC")
    fun getEntitiesByParent(parentId: Long): Flow<List<EntityItem>>

    @Query("SELECT * FROM entities WHERE id = :id")
    suspend fun getEntityById(id: Long): EntityItem?
    
    @Query("SELECT * FROM entities WHERE owner_id = :ownerId ORDER BY updatedAt DESC")
    fun getAllEntitiesByOwner(ownerId: Long): Flow<List<EntityItem>>
    
    @Query("SELECT * FROM entities WHERE owner_id = :ownerId AND type = :type ORDER BY updatedAt DESC")
    fun getEntitiesByType(ownerId: Long, type: EntityType): Flow<List<EntityItem>>
    
    @Query("SELECT * FROM entities WHERE owner_id = :ownerId AND name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchEntities(ownerId: Long, query: String): Flow<List<EntityItem>>
    
    @Query("SELECT COUNT(*) FROM entities WHERE owner_id = :ownerId")
    suspend fun countEntitiesByOwner(ownerId: Long): Int
    
    @Query("SELECT COUNT(*) FROM entities WHERE parent_id = :parentId")
    suspend fun countEntitiesInFolder(parentId: Long): Int

    // ==================== SHARING ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharing(sharing: SharingEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharings(sharings: List<SharingEntity>): List<Long>

    @Update
    suspend fun updateSharing(sharing: SharingEntity)

    @Delete
    suspend fun deleteSharing(sharing: SharingEntity)
    
    @Query("DELETE FROM sharing WHERE id = :id")
    suspend fun deleteSharingById(id: Long)

    @Query("SELECT * FROM sharing WHERE entity_id = :entityId LIMIT 1")
    fun getSharingByEntity(entityId: Long): Flow<SharingEntity?>

    @Query("SELECT * FROM sharing WHERE entity_id = :entityId")
    fun getAllSharingsByEntity(entityId: Long): Flow<List<SharingEntity>>

    @Query("SELECT * FROM sharing WHERE entity_id = :entityId LIMIT 1")
    suspend fun getSharingSync(entityId: Long): SharingEntity?

    @Query("SELECT * FROM sharing WHERE id = :id")
    suspend fun getSharingById(id: Long): SharingEntity?
    
    @Query("SELECT * FROM sharing WHERE token = :token LIMIT 1")
    suspend fun getSharingByToken(token: String): SharingEntity?

    @Query("UPDATE sharing SET clicks = clicks + 1 WHERE id = :sharingId")
    suspend fun incrementClicks(sharingId: Long)
    
    @Query("UPDATE sharing SET views = views + 1 WHERE id = :sharingId")
    suspend fun incrementViews(sharingId: Long)
    
    @Query("UPDATE sharing SET unique_visits = unique_visits + 1 WHERE id = :sharingId")
    suspend fun incrementUniqueVisits(sharingId: Long)
    
    @Query("UPDATE sharing SET downloads = downloads + 1 WHERE id = :sharingId")
    suspend fun incrementDownloads(sharingId: Long)
    
    @Query("UPDATE sharing SET token = :newToken, updatedAt = :timestamp WHERE id = :sharingId")
    suspend fun resetToken(sharingId: Long, newToken: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sharing SET privacy = :privacy, updatedAt = :timestamp WHERE id = :sharingId")
    suspend fun updatePrivacy(sharingId: Long, privacy: PrivacyType, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sharing SET public_upto = :expiryTime, updatedAt = :timestamp WHERE id = :sharingId")
    suspend fun updateLinkExpiry(sharingId: Long, expiryTime: Long?, timestamp: Long = System.currentTimeMillis())

    @Transaction
    @Query("SELECT * FROM entities WHERE owner_id = :ownerId ORDER BY updatedAt DESC")
    fun getEntitiesWithSharing(ownerId: Long): Flow<List<EntityWithSharing>>

    @Query("SELECT * FROM sharing ORDER BY createdAt DESC")
    fun getAllSharings(): Flow<List<SharingEntity>>
    
    @Query("SELECT * FROM sharing WHERE privacy = :privacy ORDER BY createdAt DESC")
    fun getSharingsByPrivacy(privacy: PrivacyType): Flow<List<SharingEntity>>
    
    @Query("SELECT COUNT(*) FROM sharing WHERE entity_id = :entityId")
    suspend fun countSharingsForEntity(entityId: Long): Int

    // ==================== JOINED LINKS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoinedLink(joinedLink: JoinedLinkEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJoinedLinks(joinedLinks: List<JoinedLinkEntity>): List<Long>

    @Update
    suspend fun updateJoinedLink(joinedLink: JoinedLinkEntity)

    @Delete
    suspend fun deleteJoinedLink(joinedLink: JoinedLinkEntity)

    @Query("SELECT * FROM joined_links ORDER BY last_access_time DESC")
    fun getAllJoinedLinks(): Flow<List<JoinedLinkEntity>>

    @Query("SELECT * FROM joined_links WHERE token = :token LIMIT 1")
    suspend fun getJoinedLinkByToken(token: String): JoinedLinkEntity?
    
    @Query("SELECT * FROM joined_links WHERE id = :id")
    suspend fun getJoinedLinkById(id: Long): JoinedLinkEntity?

    @Query("SELECT * FROM joined_links WHERE name LIKE '%' || :query || '%' OR author_name LIKE '%' || :query || '%' ORDER BY last_access_time DESC")
    fun searchJoinedLinks(query: String): Flow<List<JoinedLinkEntity>>

    @Query("DELETE FROM joined_links WHERE id = :id")
    suspend fun deleteJoinedLinkById(id: Long)
    
    @Query("SELECT * FROM joined_links WHERE is_active = 1 ORDER BY last_access_time DESC")
    fun getActiveJoinedLinks(): Flow<List<JoinedLinkEntity>>
    
    @Query("UPDATE joined_links SET access_count = access_count + 1, last_access_time = :accessTime WHERE id = :id")
    suspend fun incrementJoinedLinkAccess(id: Long, accessTime: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM joined_links")
    suspend fun countJoinedLinks(): Int
    
    @Query("SELECT * FROM joined_links WHERE is_starred = 1 ORDER BY last_access_time DESC")
    fun getStarredJoinedLinks(): Flow<List<JoinedLinkEntity>>
    
    @Query("UPDATE joined_links SET is_starred = :isStarred WHERE id = :id")
    suspend fun setJoinedLinkStarred(id: Long, isStarred: Boolean)

    // ==================== USER POINTS ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPoints(userPoints: UserPointsEntity)

    @Update
    suspend fun updateUserPoints(userPoints: UserPointsEntity)

    @Query("SELECT * FROM user_points WHERE id = 1 LIMIT 1")
    fun getUserPoints(): Flow<UserPointsEntity?>

    @Query("SELECT * FROM user_points WHERE id = 1 LIMIT 1")
    suspend fun getUserPointsSync(): UserPointsEntity?
    
    @Query("UPDATE user_points SET current_balance = current_balance + :amount, total_earned = total_earned + :amount, last_updated = :timestamp WHERE id = 1")
    suspend fun addEarnedPoints(amount: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_points SET current_balance = current_balance + :amount, total_purchased = total_purchased + :amount, last_updated = :timestamp WHERE id = 1")
    suspend fun addPurchasedPoints(amount: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_points SET current_balance = current_balance - :amount, total_spent = total_spent + :amount, last_updated = :timestamp WHERE id = 1")
    suspend fun spendPoints(amount: Int, timestamp: Long = System.currentTimeMillis())

    // ==================== POINT TRANSACTIONS ====================
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPointTransaction(transaction: PointTransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPointTransactions(transactions: List<PointTransactionEntity>): List<Long>

    @Query("SELECT * FROM point_transactions ORDER BY created_at DESC")
    fun getAllPointTransactions(): Flow<List<PointTransactionEntity>>

    @Query("SELECT * FROM point_transactions ORDER BY created_at DESC LIMIT :limit")
    fun getRecentPointTransactions(limit: Int): Flow<List<PointTransactionEntity>>
    
    @Query("SELECT * FROM point_transactions WHERE type = :type ORDER BY created_at DESC")
    fun getTransactionsByType(type: PointTransactionType): Flow<List<PointTransactionEntity>>
    
    @Query("SELECT SUM(amount) FROM point_transactions WHERE type = :type")
    suspend fun getTotalAmountByType(type: PointTransactionType): Int?
    
    @Query("DELETE FROM point_transactions WHERE created_at < :beforeTime")
    suspend fun deleteOldTransactions(beforeTime: Long)
}

/**
 * Data class for Entity with its associated Sharings
 */
data class EntityWithSharing(
    @Embedded val entity: EntityItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "entity_id"
    )
    val sharings: List<SharingEntity>
)
