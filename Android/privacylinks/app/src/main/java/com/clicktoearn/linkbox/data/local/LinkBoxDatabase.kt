package com.clicktoearn.linkbox.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkBoxDao {
    // Assets
    @Query("SELECT * FROM assets WHERE parentId IS :parentId")
    fun getAssets(parentId: String?): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun getAssetById(id: String): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    // Sharing Links
    @Query("SELECT * FROM links WHERE assetId = :assetId")
    fun getSharingLinksForAsset(assetId: String): Flow<List<SharingLinkEntity>>

    @Query("SELECT * FROM links")
    fun getAllSharingLinks(): Flow<List<SharingLinkEntity>>

    @Query("SELECT * FROM links WHERE token = :token")
    suspend fun getLinkByToken(token: String): SharingLinkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharingLink(link: SharingLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharingLinks(links: List<SharingLinkEntity>)

    @Update
    suspend fun updateSharingLink(link: SharingLinkEntity)

    @Delete
    suspend fun deleteSharingLink(link: SharingLinkEntity)

    @Query("DELETE FROM links WHERE assetId = :assetId")
    suspend fun deleteSharingLinksForAsset(assetId: String)

    // History
    @Query("SELECT * FROM history ORDER BY accessedAt DESC")
    fun getHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<HistoryEntity>)

    @Update
    suspend fun updateHistory(history: HistoryEntity)

    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    // User Profile -> Users
    @Query("SELECT * FROM users WHERE id = 'current_user'")
    suspend fun getLocalUserProfile(): UserEntity?

    @Query("SELECT * FROM users WHERE id = 'current_user'")
    fun getUserProfile(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(user: UserEntity)

    // Points Transactions
    @Query("SELECT * FROM points_transactions ORDER BY createdAt DESC")
    fun getPointsTransactions(): Flow<List<PointsTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointsTransaction(transaction: PointsTransactionEntity)

    // Clear Data
    @Query("DELETE FROM assets")
    suspend fun clearAssets()

    @Query("DELETE FROM links")
    suspend fun clearSharingLinks()

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("DELETE FROM points_transactions")
    suspend fun clearPointsTransactions()

    @Query("DELETE FROM history WHERE token IN (:tokens)")
    suspend fun deleteHistoryByTokens(tokens: List<String>)
}

@Database(entities = [AssetEntity::class, SharingLinkEntity::class, HistoryEntity::class, UserEntity::class, PointsTransactionEntity::class], version = 10, exportSchema = false)
abstract class LinkBoxDatabase : RoomDatabase() {
    abstract fun dao(): LinkBoxDao
}
