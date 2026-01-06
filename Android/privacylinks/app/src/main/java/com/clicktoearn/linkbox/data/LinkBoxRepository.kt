package com.clicktoearn.linkbox.data

import com.clicktoearn.linkbox.data.local.AssetEntity
import com.clicktoearn.linkbox.data.local.HistoryEntity
import com.clicktoearn.linkbox.data.local.LinkBoxDao
import com.clicktoearn.linkbox.data.local.SharingLinkEntity
import com.clicktoearn.linkbox.data.remote.FirestoreLink
import com.clicktoearn.linkbox.data.remote.FirestoreAsset
import com.clicktoearn.linkbox.data.remote.FirestoreHistory
import com.clicktoearn.linkbox.data.remote.FirestoreUser
import com.clicktoearn.linkbox.data.local.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Query
import java.util.UUID

class LinkBoxRepository(
    private val dao: LinkBoxDao,
    private val firestore: FirebaseFirestore
) {
    // Local Assets
    fun getAssets(parentId: String?): Flow<List<AssetEntity>> = dao.getAssets(parentId)
    fun getAllAssets(): Flow<List<AssetEntity>> = dao.getAllAssets()
    suspend fun insertAsset(asset: AssetEntity) = dao.insertAsset(asset)
    suspend fun insertAssets(assets: List<AssetEntity>) = dao.insertAssets(assets)
    suspend fun deleteAsset(asset: AssetEntity) = dao.deleteAsset(asset)
    suspend fun getAssetById(id: String): AssetEntity? = dao.getAssetById(id)

    // Local Sharing Links
    fun getSharingLinksForAsset(assetId: String): Flow<List<SharingLinkEntity>> = dao.getSharingLinksForAsset(assetId)
    fun getAllSharingLinks(): Flow<List<SharingLinkEntity>> = dao.getAllSharingLinks()
    suspend fun insertSharingLink(link: SharingLinkEntity) = dao.insertSharingLink(link)
    suspend fun insertSharingLinks(links: List<SharingLinkEntity>) = dao.insertSharingLinks(links)
    suspend fun deleteSharingLink(link: SharingLinkEntity) = dao.deleteSharingLink(link)
    suspend fun deleteSharingLinksForAsset(assetId: String) = dao.deleteSharingLinksForAsset(assetId)
    suspend fun updateSharingLink(link: SharingLinkEntity) = dao.updateSharingLink(link)

    // Local History
    fun getHistory(): Flow<List<HistoryEntity>> = dao.getHistory()
    suspend fun insertHistory(history: HistoryEntity) = dao.insertHistory(history)
    suspend fun insertHistories(histories: List<HistoryEntity>) = dao.insertHistories(histories)
    suspend fun deleteHistory(history: HistoryEntity) = dao.deleteHistory(history)
    suspend fun updateHistory(history: HistoryEntity) = dao.updateHistory(history)
    suspend fun deleteHistoryByTokens(tokens: List<String>) = dao.deleteHistoryByTokens(tokens)

    // Points Transactions
    fun getPointsTransactions(): Flow<List<PointsTransactionEntity>> = dao.getPointsTransactions()
    suspend fun insertPointsTransaction(transaction: PointsTransactionEntity) = dao.insertPointsTransaction(transaction)

    // Clear Local Data
    suspend fun clearAllLocalData() {
        dao.clearAssets()
        dao.clearSharingLinks()
        dao.clearHistory()
        dao.clearPointsTransactions()
    }

    // User Profile
    fun getUserProfile() = dao.getUserProfile()
    suspend fun getLocalUserProfile() = dao.getLocalUserProfile()
    suspend fun insertUserProfile(user: UserEntity) = dao.insertUserProfile(user)

    // --- User ID Consistency ---
    /**
     * Get the ID that should be used for cloud ownership.
     * Returns Google UID if available, otherwise null.
     */
    private fun resolveUserId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    }
    
    // Ensure we have a valid user ID (only authenticated)
    private fun ensureAuth(): String {
        return resolveUserId() ?: throw IllegalStateException("User must be authenticated")
    }


    // Used by ViewModel for auth check only
    fun getAuthUserId(): String? = resolveUserId()

    // --- Cloud Asset Sync ---
    suspend fun saveAssetToCloud(asset: AssetEntity): Boolean {
        val userId = resolveUserId() ?: return false
        val firestoreAsset = FirestoreAsset(
            id = asset.id,
            name = asset.name,
            description = asset.description,
            type = asset.type.name,
            content = asset.content,
            parentId = asset.parentId,
            ownerId = userId,
            pointCost = asset.pointCost,
            allowSaveCopy = asset.allowSaveCopy,
            allowFurtherSharing = asset.shareOutsideApp,
            allowScreenCapture = asset.allowScreenCapture,
            exposeUrl = asset.exposeUrl,
            chargeEveryTime = asset.chargeEveryTime,
            sharingEnabled = asset.sharingEnabled,
            createdAt = asset.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        return try {
            firestore.collection("assets").document(asset.id).set(firestoreAsset).await()
            android.util.Log.d("LinkBoxRepository", "saveAssetToCloud: Success for asset ${asset.id}")
            true
        } catch (e: Exception) {
            android.util.Log.w("LinkBoxRepository", "saveAssetToCloud failed: ${e.message}. Attempting recovery...")
            
            // Attempt to ensure user profile exists
            val localUser = dao.getLocalUserProfile()
            val userToSync = localUser ?: UserEntity(id = "current_user", username = "User")
            val profileSynced = syncUserProfileToCloud(userToSync)

            if (profileSynced) {
                // Retry only if profile sync succeeded
                try {
                    android.util.Log.d("LinkBoxRepository", "saveAssetToCloud: Retrying after profile sync...")
                    firestore.collection("assets").document(asset.id).set(firestoreAsset).await()
                    android.util.Log.d("LinkBoxRepository", "saveAssetToCloud: Retry Success")
                    true
                } catch (retryEx: Exception) {
                    android.util.Log.e("LinkBoxRepository", "saveAssetToCloud: Retry Failed", retryEx)
                    false
                }
            } else {
                android.util.Log.e("LinkBoxRepository", "saveAssetToCloud: Recovery failed (Profile sync failed)")
                false
            }
        }
    }

    suspend fun deleteAssetFromCloud(assetId: String) {
        if (resolveUserId() == null) return
        try {
            firestore.collection("assets").document(assetId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCloudAssets(parentId: String?): Flow<List<FirestoreAsset>> = callbackFlow {
        val userId = resolveUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = firestore.collection("assets")
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("parentId", parentId)
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                trySend(snapshot.toObjects(FirestoreAsset::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    fun getAllCloudAssets(): Flow<List<FirestoreAsset>> = callbackFlow {
        val userId = resolveUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = firestore.collection("assets")
            .whereEqualTo("ownerId", userId)
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                trySend(snapshot.toObjects(FirestoreAsset::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    // --- Cloud Link Sync ---
    suspend fun saveLinkToCloud(link: SharingLinkEntity): Boolean {
        resolveUserId() ?: return false
        val firestoreLink = FirestoreLink(
            id = link.id,
            assetId = link.assetId,
            token = link.token,
            name = link.name,
            expiryDate = link.expiry,
            status = link.status,
            newUsers = link.newUsers,
            users = link.users,
            views = link.views,
            createdAt = link.createdAt,
            updatedAt = link.updatedAt
        )
        return try {
            firestore.collection("links").document(link.token).set(firestoreLink).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteLinkFromCloud(token: String) {
        try {
            firestore.collection("links").document(token).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Cloud History Sync ---
    suspend fun saveHistoryToCloud(history: HistoryEntity) {
        val userId = resolveUserId() ?: return
        val firestoreHistory = FirestoreHistory(
            token = history.token,
            userId = userId,
            accessedAt = history.accessedAt,
            isStarred = history.isStarred
        )
        try {
            firestore.collection("history").document("${userId}_${history.token}").set(firestoreHistory).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteCloudHistory(token: String) {
        val userId = ensureAuth()
        try {
            firestore.collection("history").document("${userId}_${token}").delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCloudHistory(): Flow<List<FirestoreHistory>> = callbackFlow {
        val userId = resolveUserId() ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val query = firestore.collection("history")
            .whereEqualTo("userId", userId)
            .orderBy("accessedAt", Query.Direction.DESCENDING)
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                trySend(snapshot.toObjects(FirestoreHistory::class.java))
            }
        }
        awaitClose { registration.remove() }
    }

    // --- User Profile Cloud ---
    suspend fun syncUserProfileToCloud(user: UserEntity): Boolean {
        val userId = resolveUserId() ?: return false
        val firestoreUser = FirestoreUser(
            userId = userId,
            username = user.username,
            email = "", // Optional
            points = user.points,
            accessToken = user.accessToken,
            totalEarned = 0f // Can be tracked
        )
        return try {
            firestore.collection("users").document(userId).set(firestoreUser).await()
            android.util.Log.d("LinkBoxRepository", "syncUserProfileToCloud: Success for user $userId")
            true
        } catch (e: Exception) {
            android.util.Log.e("LinkBoxRepository", "syncUserProfileToCloud: Failed", e)
            false
        }
    }

    suspend fun fetchUserProfileFromCloud(): FirestoreUser? {
        val userId = resolveUserId() ?: return null
        return try {
            firestore.collection("users").document(userId).get().await()
                .toObject(FirestoreUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun findUserByName(username: String): FirestoreUser? {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("name", username)
                .limit(1)
                .get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(FirestoreUser::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchUserById(userId: String): FirestoreUser? {
        return try {
            firestore.collection("users").document(userId).get().await()
                .toObject(FirestoreUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Firestore Sync
    suspend fun shareAssetCloud(link: SharingLinkEntity, asset: AssetEntity): Boolean {
        val assetSaved = saveAssetToCloud(asset) // Ensure asset exists in cloud
        val linkSaved = saveLinkToCloud(link)
        return assetSaved && linkSaved
    }

    suspend fun lookupByToken(token: String): FirestoreLink? {
        return try {
            firestore.collection("links").document(token).get().await()
                .toObject(FirestoreLink::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun lookupLocalLinkByToken(token: String): SharingLinkEntity? {
        return dao.getLinkByToken(token)
    }
    
    fun listenToLink(token: String): Flow<FirestoreLink?> = callbackFlow {
        val registration = firestore.collection("links").document(token)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(FirestoreLink::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun signInAnonymously() {
        // Obsolete: No longer using anonymous auth or device ID for auto-login
    }

    fun signOut() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
    }

    // --- Shared Content Navigation ---
    suspend fun getCloudAsset(assetId: String): FirestoreAsset? {
        return try {
            firestore.collection("assets").document(assetId).get().await()
                .toObject(FirestoreAsset::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun listenToCloudAsset(assetId: String): Flow<FirestoreAsset?> = callbackFlow {
        val registration = firestore.collection("assets").document(assetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(FirestoreAsset::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { registration.remove() }
    }

    fun getSharedFolderContents(ownerId: String, folderId: String): Flow<List<FirestoreAsset>> = callbackFlow {
        val query = firestore.collection("assets")
            .whereEqualTo("ownerId", ownerId)
            .whereEqualTo("parentId", folderId)
        
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                trySend(snapshot.toObjects(FirestoreAsset::class.java))
            }
        }
        awaitClose { registration.remove() }
    }
}
