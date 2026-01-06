package com.clicktoearn.linkbox.data

import com.clicktoearn.linkbox.data.local.AssetType
import com.clicktoearn.linkbox.data.remote.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.UUID
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * Repository that uses Firestore as the primary data source with in-memory session caching.
 * - All data is fetched from Firestore
 * - Session cache (MutableStateFlow) for fast UI updates
 * - Manual refresh triggers a fresh fetch from server
 * - Uses device ID for user identification instead of Firebase Auth
 */
class FirestoreRepository(
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    // ==================== In-Memory Session Cache ====================
    private val _assets = MutableStateFlow<List<FirestoreAsset>>(emptyList())
    val assets: StateFlow<List<FirestoreAsset>> = _assets.asStateFlow()
    
    private val _links = MutableStateFlow<List<FirestoreLink>>(emptyList())
    val links: StateFlow<List<FirestoreLink>> = _links.asStateFlow()
    
    private val _history = MutableStateFlow<List<FirestoreHistory>>(emptyList())
    val history: StateFlow<List<FirestoreHistory>> = _history.asStateFlow()
    
    private val _userProfile = MutableStateFlow<FirestoreUser?>(null)
    val userProfile: StateFlow<FirestoreUser?> = _userProfile.asStateFlow()

    private suspend fun getAdvertisingId(): String? = withContext(Dispatchers.IO) {
        try {
            com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(context).id
        } catch (e: Exception) {
            null
        }
    }

    private fun getIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) { }
        return null
    }

    fun getUserId(): String? {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: _userProfile.value?.userId
    }

    /**
     * Get the authenticated user ID. Returns null if not authenticated.
     */
    fun getAuthUserId(): String? = getUserId()
    
    /**
     * Ensure user is authenticated and return their ID.
     * Throws exception if user is not authenticated.
     */
    fun ensureAuth(): String {
        return getUserId() ?: throw IllegalStateException("User must be authenticated")
    }

    fun signOut() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        clearSessionCache()
    }

    private fun clearSessionCache() {
        _assets.value = emptyList()
        _links.value = emptyList()
        _history.value = emptyList()
        _userProfile.value = null
    }



    // ==================== User Profile ====================
    suspend fun fetchUserProfile(): FirestoreUser? {
        val userId = getUserId() ?: return null
        return try {
            val user = firestore.collection("users").document(userId).get().await()
                .toObject(FirestoreUser::class.java)
            if (user != null) {
                _userProfile.value = user
            }
            user
        } catch (e: Exception) {
            null
        }
    }



    // Authentication should only happen via Google Sign-In or other Firebase Auth methods.
    // Custom username login via deviceId is no longer supported.

    suspend fun signInWithGoogle(credential: com.google.firebase.auth.AuthCredential): Result<Boolean> {
        return try {
            android.util.Log.d("FirestoreRepository", "signInWithGoogle: Starting Google sign-in flow")
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            auth.signInWithCredential(credential).await()
            val user = auth.currentUser ?: run {
                android.util.Log.e("FirestoreRepository", "signInWithGoogle: No current user after auth")
                return Result.failure(Exception("Authentication failed: No user found after credentials"))
            }
            
            android.util.Log.d("FirestoreRepository", "signInWithGoogle: Firebase Auth successful, userId=${user.uid}")
            
            // Wait for backend to create user document (retry with exponential backoff)
            var firestoreUser: FirestoreUser? = null
            val maxRetries = 3 // Reduced from 8 for better responsiveness
            var retryCount = 0
            var delayMs = 400L // Reduced from 500ms
            
            while (firestoreUser == null && retryCount < maxRetries) {
                if (retryCount > 0) {
                    android.util.Log.d("FirestoreRepository", "signInWithGoogle: Waiting ${delayMs}ms for backend to create user (attempt ${retryCount + 1}/$maxRetries)")
                    kotlinx.coroutines.delay(delayMs)
                    delayMs = (delayMs * 1.5).toLong() // Slower backoff but fewer retries
                }
                
                // Fetch from server to get latest data
                firestoreUser = fetchUserById(user.uid, com.google.firebase.firestore.Source.SERVER)
                retryCount++
            }
            
            val ip = getIpAddress()
            val adId = getAdvertisingId()

            if (firestoreUser == null) {
                android.util.Log.w("FirestoreRepository", "signInWithGoogle: Backend didn't create user after $maxRetries attempts, creating locally as fallback")
                
                // Fallback: Create user locally if backend didn't create it
                val newUser = FirestoreUser(
                    userId = user.uid,
                    username = user.displayName ?: "User",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    points = 0,
                    isGuest = false,
                    firstIp = ip,
                    lastIp = ip,
                    advertisingId = adId
                )
                
                try {
                    android.util.Log.d("FirestoreRepository", "signInWithGoogle: Creating user document locally...")
                    firestore.collection("users").document(newUser.userId).set(newUser).await()
                    android.util.Log.d("FirestoreRepository", "signInWithGoogle: Local user document created successfully")
                    firestoreUser = newUser
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreRepository", "signInWithGoogle: Failed to create user document locally", e)
                    return Result.failure(e)
                }
            } else {
                android.util.Log.d("FirestoreRepository", "signInWithGoogle: ✅ Backend user document found: username=${firestoreUser.username}")
                
                // Update tracking info for backend-created user
                val updates = mutableMapOf<String, Any>(
                    "lastIp" to (ip ?: ""),
                    "advertisingId" to (adId ?: "")
                )
                
                // Update photo URL if not set
                if (firestoreUser.photoUrl.isEmpty() && user.photoUrl != null) {
                    updates["photoUrl"] = user.photoUrl.toString()
                }
                
                // Update email if not set
                if (firestoreUser.email.isEmpty() && user.email != null) {
                    updates["email"] = user.email!!
                }
                
                if (updates.isNotEmpty()) {
                    try {
                        firestore.collection("users").document(user.uid).update(updates).await()
                        android.util.Log.d("FirestoreRepository", "signInWithGoogle: User profile updated with tracking info")
                        
                        // Update local copy
                        firestoreUser = firestoreUser.copy(
                            lastIp = ip,
                            advertisingId = adId,
                            photoUrl = if (user.photoUrl != null) user.photoUrl.toString() else firestoreUser.photoUrl,
                            email = user.email ?: firestoreUser.email
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "signInWithGoogle: Failed to update tracking info", e)
                    }
                }
            }
            
            if (firestoreUser != null) {
                _userProfile.value = firestoreUser
                android.util.Log.d("FirestoreRepository", "signInWithGoogle: ✅ Complete success - User: ${firestoreUser.username}, PhotoUrl: ${firestoreUser.photoUrl}")
            } else {
                android.util.Log.e("FirestoreRepository", "signInWithGoogle: ❌ firestoreUser is null after all attempts")
                return Result.failure(Exception("Failed to initialize user profile"))
            }
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "signInWithGoogle: ❌ FAILED with exception", e)
            Result.failure(e)
        }
    }

    suspend fun linkWithGoogle(credential: com.google.firebase.auth.AuthCredential): Boolean {
        return try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                android.util.Log.e("FirestoreRepository", "linkWithGoogle: No current user")
                return false
            }
            
            // Link the credential to the current anonymous account
            val result = currentUser.linkWithCredential(credential).await()
            val user = result.user ?: return false

            // Update Firestore with Google account info
            val currentProfile = _userProfile.value
            if (currentProfile != null) {
                val ip = getIpAddress()
                val adId = getAdvertisingId()
            
                val updates = mutableMapOf<String, Any>(
                    "lastIp" to (ip ?: ""),
                    "advertisingId" to (adId ?: "")
                )
                
                // Update email if available
                if (!user.email.isNullOrBlank()) {
                    updates["email"] = user.email!!
                }
                
                // Update photo URL if available
                if (user.photoUrl != null) {
                    updates["photoUrl"] = user.photoUrl.toString()
                }
                
                // Update display name if current username is guest-like
                if (currentProfile.username.startsWith("guest_") && !user.displayName.isNullOrBlank()) {
                    updates["username"] = user.displayName!!
                }
                
                firestore.collection("users").document(currentProfile.userId).update(updates).await()
                
                // Refresh the profile
                val updatedProfile = fetchUserById(currentProfile.userId)
                if (updatedProfile != null) {
                    _userProfile.value = updatedProfile
                }
                
                android.util.Log.d("FirestoreRepository", "linkWithGoogle: Successfully linked guest account to Google")
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "linkWithGoogle failed", e)
            false
        }
    }

    suspend fun updateUsername(newUsername: String): Boolean {
        val current = _userProfile.value ?: return false
        if (current.username == newUsername) return true
        
        return try {
            // Check if taken
            if (findUserByUsername(newUsername) != null) return false
            
            val updated = current.copy(username = newUsername)
            firestore.collection("users").document(current.userId).set(updated).await()
            _userProfile.value = updated
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateUserPoints(newPoints: Int) {
        val userId = getUserId() ?: return
        try {
            firestore.collection("users").document(userId)
                .update("points", newPoints).await()
            _userProfile.value = _userProfile.value?.copy(points = newPoints)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun incrementUserPoints(userId: String, amount: Int) {
        try {
            firestore.collection("users").document(userId)
                .update("points", com.google.firebase.firestore.FieldValue.increment(amount.toLong())).await()
            
            // Update local state if it's the current user
            val current = _userProfile.value
            if (current != null && current.userId == userId) {
                _userProfile.value = current.copy(points = current.points + amount)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "incrementUserPoints failed for user $userId", e)
        }
    }

    suspend fun processSubscription(cost: Int, days: Int): Boolean {
        val user = _userProfile.value ?: return false
        if (user.points < cost) return false

        return try {
            val newPoints = user.points - cost
            val currentExpiry = user.premiumExpiry ?: System.currentTimeMillis()
            // If already premium, extend. If not, start from now.
            val start = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
            val newExpiry = start + (days.toLong() * 24 * 60 * 60 * 1000)

            val updates = mapOf(
                "points" to newPoints,
                "isPremium" to true,
                "premiumExpiry" to newExpiry
            )

            firestore.collection("users").document(user.userId).update(updates).await()
            
            _userProfile.value = user.copy(
                points = newPoints,
                isPremium = true,
                premiumExpiry = newExpiry
            )
            
            // Add transaction record
            val transaction = FirestorePointsTransaction(
                id = UUID.randomUUID().toString(),
                userId = user.userId,
                points = -cost,
                type = "SUBSCRIPTION",
                remark = "Premium Subscription ($days days)",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            // Fire and forget transaction log
            firestore.collection("transactions").document(transaction.id).set(transaction)
            
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "processSubscription failed", e)
            false
        }
    }

    suspend fun processContentUnlock(payerId: String, ownerId: String, amount: Int, ownerShare: Int, contentName: String): Boolean {
        try {
            // 1. Deduct from payer
             if (amount > 0) {
                // Update payer points
                firestore.collection("users").document(payerId)
                    .update("points", com.google.firebase.firestore.FieldValue.increment(-amount.toLong())).await()
                
                // Update local if current user
                val current = _userProfile.value
                if (current != null && current.userId == payerId) {
                    _userProfile.value = current.copy(points = current.points - amount)
                }

                // Log payer transaction
                val payerTx = FirestorePointsTransaction(
                    id = UUID.randomUUID().toString(),
                    userId = payerId,
                    points = -amount,
                    type = "CONTENT_ACCESS",
                    remark = "Unlocked: $contentName",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                firestore.collection("transactions").document(payerTx.id).set(payerTx).await()
            }

            // 2. Credit owner
            if (ownerShare > 0) {
                // Update owner points
                firestore.collection("users").document(ownerId)
                    .update("points", com.google.firebase.firestore.FieldValue.increment(ownerShare.toLong())).await()
                
                // Log owner transaction
                val ownerTx = FirestorePointsTransaction(
                    id = UUID.randomUUID().toString(),
                    userId = ownerId,
                    points = ownerShare,
                    type = "CONTENT_EARNING",
                    remark = "Earned from: $contentName",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                firestore.collection("transactions").document(ownerTx.id).set(ownerTx).await()
            }
            
            return true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "processContentUnlock failed", e)
            return false
        }
    }

    suspend fun findUserByUsername(username: String): FirestoreUser? {
        return try {
            // Search by both 'username' (new) and 'name' (legacy) fields
            val snapshotNew = firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get().await()
            
            if (!snapshotNew.isEmpty) {
                return snapshotNew.documents[0].toObject(FirestoreUser::class.java)
            }
            
            val snapshotLegacy = firestore.collection("users")
                .whereEqualTo("name", username)
                .limit(1)
                .get().await()
                
            if (!snapshotLegacy.isEmpty) {
                return snapshotLegacy.documents[0].toObject(FirestoreUser::class.java)
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchUserById(userId: String, source: com.google.firebase.firestore.Source? = null): FirestoreUser? {
        try {
            android.util.Log.d("FirestoreRepository", "fetchUserById: Fetching user with ID: $userId (source=$source)")
            
            var snapshot: com.google.firebase.firestore.DocumentSnapshot? = null
            
            if (source != null) {
                try {
                    snapshot = firestore.collection("users").document(userId).get(source).await()
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreRepository", "fetchUserById: Explicit source=$source fetch failed for $userId", e)
                }
            } else {
                // Default behavior: Cache first
                try {
                    snapshot = firestore.collection("users").document(userId)
                        .get(com.google.firebase.firestore.Source.CACHE).await()
                } catch (e: Exception) {
                    // Cache miss or error
                }
            }
            
            // If we are using default behavior (source==null) AND (snapshot missing or didn't exist)
            // OR if source was EXPLICITLY SERVER
            if ((source == null && (snapshot == null || !snapshot.exists())) || source == com.google.firebase.firestore.Source.SERVER) {
                if (source == null) {
                    android.util.Log.d("FirestoreRepository", "fetchUserById: User not in cache, trying server for ID: $userId")
                }
                
                try {
                    val serverSnapshot = firestore.collection("users").document(userId)
                        .get(com.google.firebase.firestore.Source.SERVER).await()
                    snapshot = serverSnapshot
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreRepository", "fetchUserById: Server fetch failed for $userId", e)
                    // If we failed to get from server, and we have no cached snapshot, return null
                    if (snapshot == null) return null
                }
            }
            
            if (snapshot == null || !snapshot.exists()) {
                android.util.Log.w("FirestoreRepository", "fetchUserById: User document does not exist for ID: $userId (snapshot valid=${snapshot != null})")
                return null
            }
            
            val user = snapshot.toObject(FirestoreUser::class.java)
            if (user == null) {
                android.util.Log.e("FirestoreRepository", "fetchUserById: Failed to deserialize user document for ID: $userId")
                return null
            } else {
                android.util.Log.d("FirestoreRepository", "fetchUserById: Successfully fetched user: '${user.username}' for ID: $userId")
                if (user.username.isBlank()) {
                    android.util.Log.w("FirestoreRepository", "fetchUserById: Username is blank for user ID: $userId")
                }
            }
            return user
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "fetchUserById: Unexpected error fetching user $userId", e)
            return null
        }
    }

    // ==================== Assets ====================
    suspend fun fetchAssets(): List<FirestoreAsset> {
        val userId = getUserId() ?: return emptyList()
        return try {
            val snapshot = firestore.collection("assets")
                .whereEqualTo("ownerId", userId)
                .get().await()
            val list = snapshot.toObjects(FirestoreAsset::class.java)
            _assets.value = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAssets(parentId: String?): Flow<List<FirestoreAsset>> {
        return _assets.map { assets ->
            assets.filter { it.parentId == parentId }
        }
    }

    suspend fun saveAsset(asset: FirestoreAsset): Boolean {
        val userId = getUserId() ?: return false
        android.util.Log.d("FirestoreRepository", "saveAsset: getUserId() returned: $userId")
        android.util.Log.d("FirestoreRepository", "saveAsset: asset.ownerId before processing: '${asset.ownerId}'")
        
        // Ensure we don't overwrite ownerId if it's already set (unless it's null/empty/current_user)
        val finalOwner = if (asset.ownerId.isNotBlank() && asset.ownerId != "current_user") asset.ownerId else userId
        val assetWithOwner = asset.copy(ownerId = finalOwner)
        
        android.util.Log.d("FirestoreRepository", "saveAsset: Final owner ID being saved: $finalOwner")
        
        return try {
            firestore.collection("assets").document(asset.id).set(assetWithOwner).await()
            android.util.Log.d("FirestoreRepository", "saveAsset: ✅ Asset saved successfully with ownerId=$finalOwner")
            
            // Update cache
            val currentList = _assets.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == asset.id }
            if (index >= 0) {
                currentList[index] = assetWithOwner
            } else {
                currentList.add(assetWithOwner)
            }
            _assets.value = currentList
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "saveAsset failed", e)
            false
        }
    }

    suspend fun updateAssetFields(assetId: String, updates: Map<String, Any?>): Boolean {
        return try {
            firestore.collection("assets").document(assetId).update(updates).await()
            // Invalidate cache or fetch fresh
            fetchAssets() 
            true
        } catch (e: Exception) {
             android.util.Log.e("FirestoreRepository", "updateAssetFields failed", e)
            false
        }
    }

    suspend fun deleteAsset(assetId: String): Boolean {
        return try {
            val allAssetIdsToDelete = mutableSetOf<String>()
            val allLinkTokensToDelete = mutableSetOf<String>()
            
            fun collectDeletions(id: String) {
                if (allAssetIdsToDelete.contains(id)) return
                allAssetIdsToDelete.add(id)
                
                // Find children recursively
                val children = _assets.value.filter { it.parentId == id }
                children.forEach { collectDeletions(it.id) }
                
                // Find sharing links for this asset
                val links = _links.value.filter { it.assetId == id }
                links.forEach { allLinkTokensToDelete.add(it.token) }
            }
            
            collectDeletions(assetId)
            
            // Use batches to delete from Firestore
            // Note: Limited to 500 operations per batch
            val chunks = (allAssetIdsToDelete.map { "asset" to it } + 
                         allLinkTokensToDelete.map { "link" to it })
                         .chunked(450) // Staying safe under 500
            
            for (chunk in chunks) {
                val batch = firestore.batch()
                chunk.forEach { (type, id) ->
                    if (type == "asset") {
                        batch.delete(firestore.collection("assets").document(id))
                    } else {
                        batch.delete(firestore.collection("links").document(id))
                    }
                }
                batch.commit().await()
            }
            
            // Update session cache
            _assets.value = _assets.value.filter { it.id !in allAssetIdsToDelete }
            _links.value = _links.value.filter { it.token !in allLinkTokensToDelete }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Recursive deleteAsset failed for $assetId", e)
            false
        }
    }

    suspend fun getAssetById(assetId: String, source: com.google.firebase.firestore.Source = com.google.firebase.firestore.Source.DEFAULT): FirestoreAsset? {
        // Fetch from specified source
        return try {
            firestore.collection("assets").document(assetId).get(source).await()
                .toObject(FirestoreAsset::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Real-time listener for a single asset
    fun listenToAsset(assetId: String): Flow<FirestoreAsset?> = callbackFlow {
        val registration = firestore.collection("assets").document(assetId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(FirestoreAsset::class.java))
            }
        awaitClose { registration.remove() }
    }

    // ==================== Links ====================
    suspend fun fetchLinks(): List<FirestoreLink> {
        return try {
            // Get user's assets first
            val assetIds = _assets.value.map { it.id }
            if (assetIds.isEmpty()) return emptyList()
            
            // OPTIMIZED: Fetch links in parallel chunks
            val allLinks = withContext(Dispatchers.IO) {
                kotlinx.coroutines.coroutineScope {
                    assetIds.chunked(10).map { chunk ->
                        async {
                            try {
                                val snapshot = firestore.collection("links")
                                    .whereIn("assetId", chunk)
                                    .get().await()
                                snapshot.toObjects(FirestoreLink::class.java)
                            } catch (e: Exception) {
                                android.util.Log.e("FirestoreRepository", "fetchLinks chunk failed", e)
                                emptyList<FirestoreLink>()
                            }
                        }
                    }.awaitAll().flatten()
                }
            }
            _links.value = allLinks
            allLinks
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "fetchLinks failed", e)
            emptyList()
        }
    }

    fun getLinksForAsset(assetId: String): Flow<List<FirestoreLink>> {
        return _links.map { links ->
            links.filter { it.assetId == assetId }
        }
    }

    fun getAllLinks(): Flow<List<FirestoreLink>> = _links.asStateFlow()

    suspend fun saveLink(link: FirestoreLink): Boolean {
        return try {
            firestore.collection("links").document(link.token).set(link).await()
            // Update cache
            val currentList = _links.value.toMutableList()
            val index = currentList.indexOfFirst { it.token == link.token }
            if (index >= 0) {
                currentList[index] = link
            } else {
                currentList.add(link)
            }
            _links.value = currentList
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteLink(token: String): Boolean {
        return try {
            firestore.collection("links").document(token).delete().await()
            _links.value = _links.value.filter { it.token != token }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun submitReport(report: HashMap<String, Any>): Boolean {
        return try {
            val reportId = UUID.randomUUID().toString()
            firestore.collection("reports").document(reportId).set(report).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun lookupByToken(token: String, source: com.google.firebase.firestore.Source = com.google.firebase.firestore.Source.DEFAULT): FirestoreLink? {
        // Fetch from specified source
        return try {
            firestore.collection("links").document(token).get(source).await()
                .toObject(FirestoreLink::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Real-time listener for a link
    fun listenToLink(token: String): Flow<FirestoreLink?> = callbackFlow {
        val registration = firestore.collection("links").document(token)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(FirestoreLink::class.java))
            }
        awaitClose { registration.remove() }
    }

    // ==================== History ====================
    suspend fun fetchHistory(source: com.google.firebase.firestore.Source = com.google.firebase.firestore.Source.DEFAULT): List<FirestoreHistory> {
        val userId = getUserId()
        return try {
            val snapshot = firestore.collection("history")
                .whereEqualTo("userId", userId)
                .get(source).await()
            val list = snapshot.toObjects(FirestoreHistory::class.java)
            val sorted = list.sortedByDescending { it.accessedAt }
            _history.value = sorted
            sorted
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "fetchHistory failed", e)
            emptyList()
        }
    }

    fun getHistoryStream(): Flow<List<FirestoreHistory>> = callbackFlow {
        val userId = getUserId()

        val query = firestore.collection("history")
            .whereEqualTo("userId", userId)
            
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirestoreRepository", "History listen failed", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FirestoreHistory::class.java)
                // Sort in memory to avoid index requirement issues
                val sorted = list.sortedByDescending { it.accessedAt }
                _history.value = sorted // Update cache as side effect
                trySend(sorted)
            }
        }
        awaitClose { registration.remove() }
    }

    fun getHistory(): Flow<List<FirestoreHistory>> = _history.asStateFlow()

    suspend fun saveHistory(token: String, isPaid: Boolean? = null): Boolean {
        if (token.startsWith("sample_")) return true // Ignore sample items for cloud
        
        val user = _userProfile.value
        if (user == null || user.isGuest) {
            android.util.Log.d("FirestoreRepository", "saveHistory: Skipping cloud save for guest/non-logged user")
            return true 
        }
        
        val userId = user.userId
        val docId = "${userId}_$token"
        return try {
            // Use set with merge to avoid overwriting isStarred if only updating accessedAt
            val data = mutableMapOf<String, Any>(
                "token" to token,
                "userId" to userId,
                "accessedAt" to System.currentTimeMillis()
            )
            if (isPaid != null) {
                data["isPaid"] = isPaid
            }
            
            firestore.collection("history").document(docId)
                .set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "saveHistory failed", e)
            false
        }
    }

    suspend fun updateHistoryPaidStatus(token: String, isPaid: Boolean): Boolean {
        if (token.startsWith("sample_")) return true
        
        val user = _userProfile.value
        if (user == null || user.isGuest) return true
        
        val userId = user.userId
        val docId = "${userId}_$token"
        return try {
            val data = mapOf("isPaid" to isPaid)
            firestore.collection("history").document(docId)
                .set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteHistory(token: String): Boolean {
        if (token.startsWith("sample_")) return true
        
        val user = _userProfile.value
        if (user == null || user.isGuest) return true
        
        val userId = user.userId
        return try {
            firestore.collection("history").document("${userId}_$token").delete().await()
            // No local cache update - let stream handle it
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateHistoryStarred(token: String, isStarred: Boolean): Boolean {
        if (token.startsWith("sample_")) return true
        
        val user = _userProfile.value
        if (user == null || user.isGuest) {
            android.util.Log.d("FirestoreRepository", "updateHistoryStarred: Skipping cloud update for guest/non-logged user")
            return true
        }
        
        val userId = user.userId
        val docId = "${userId}_$token"
        android.util.Log.d("FirestoreRepository", "updateHistoryStarred: docId=$docId, isStarred=$isStarred")
        return try {
            // Use set with merge to create doc if it doesn't exist
            val data = mapOf(
                "token" to token,
                "userId" to userId,
                "isStarred" to isStarred,
                "accessedAt" to System.currentTimeMillis() // Update last access too
            )
            firestore.collection("history").document(docId)
                .set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            android.util.Log.d("FirestoreRepository", "updateHistoryStarred: SUCCESS")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "updateHistoryStarred: FAILED", e)
            false
        }
    }

    // ==================== Full Refresh ====================
    suspend fun refreshAll() = kotlinx.coroutines.coroutineScope {
        if (getUserId() == null) return@coroutineScope // Skip if not logged in
        
        // Parallelize profile and assets fetch
        val profileDeferred = async { fetchUserProfile() }
        val assetsDeferred = async { fetchAssets() }
        
        // Wait for assets as links depend on them
        assetsDeferred.await()
        
        // Parallelize links fetch with profile fetch completion
        val linksDeferred = async { fetchLinks() }
        
        awaitAll(profileDeferred, linksDeferred)
        // History is handled by stream, no need to explicit fetch
    }

    // ==================== Helper Methods ====================
    fun createAssetId(): String = UUID.randomUUID().toString()
    fun createLinkToken(): String = UUID.randomUUID().toString().substring(0, 8)
    
    // ==================== Referral Tracking ====================
    
    /**
     * Tracks a referral when a user accesses content via a token
     * Handles both new installs and existing user accesses
     * 
     * @param token The sharing link token
     * @param isNewInstall True if this is a new app install, false for existing user
     */
    suspend fun trackReferral(token: String, isNewInstall: Boolean) {
        try {
            // Safe check for authentication before tracking referral
            val firebaseUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val profileUserId = _userProfile.value?.userId
            val currentUserId = firebaseUid ?: profileUserId ?: return // Silently return for guests
            
            if (_userProfile.value == null) return
            
            // Lookup the link to find the owner (referrer)
            val link = lookupByToken(token) ?: return
            val asset = getAssetById(link.assetId) ?: return
            val referrerId = asset.ownerId
            
            // Don't track self-referrals
            if (referrerId == currentUserId) {
                android.util.Log.d("FirestoreRepository", "trackReferral: Skipping self-referral")
                return
            }
            
            // Check if this user has already accessed this link (to avoid duplicate rewards)
            val historyDocId = "${currentUserId}_$token"
            val existingHistory = try {
                firestore.collection("history").document(historyDocId).get().await()
                    .toObject(FirestoreHistory::class.java)
            } catch (e: Exception) {
                null
            }
            
            val isFirstAccess = existingHistory == null
            
            // Update user referral data based on whether user is new or existing
            updateUserReferralData(currentUserId, referrerId, isNewInstall)
            
            // Reward referrer only for New Installs (no rewards for views/regular shares)
            if (isFirstAccess && isNewInstall) {
                val rewardPoints = 20
                val reason = "New install referral"
                rewardReferrer(referrerId, rewardPoints, reason, currentUserId, token, isNewInstall)
            }
            
            // Update link analytics
            updateLinkAnalytics(token, isNewInstall, isFirstAccess)
            
            android.util.Log.d("FirestoreRepository", "trackReferral: Success - isNewInstall=$isNewInstall, isFirstAccess=$isFirstAccess")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "trackReferral failed", e)
        }
    }
    
    /**
     * Updates referral data for the referred user
     * - New users: Set firstReferredBy
     * - Existing users with new install: Update lastReferredBy
     * 
     * @param userId The user who was referred
     * @param referrerId The user who referred them
     * @param isNewInstall True if this is a new app install
     */
    private suspend fun updateUserReferralData(userId: String, referrerId: String, isNewInstall: Boolean) {
        try {
            // Only update if this is a new install (not regular access)
            if (!isNewInstall) {
                android.util.Log.d("FirestoreRepository", "updateUserReferralData: Skipping - not a new install")
                return
            }
            
            // Check if user already exists in Firestore
            val existingUser = fetchUserById(userId)
            val updates = mutableMapOf<String, Any>()
            
            if (existingUser?.firstReferredBy == null) {
                // New user - set firstReferredBy
                updates["firstReferredBy"] = referrerId
                android.util.Log.d("FirestoreRepository", "updateUserReferralData: New user - set firstReferredBy=$referrerId")
            } else {
                // Existing user reinstalling - update lastReferredBy
                updates["lastReferredBy"] = referrerId
                android.util.Log.d("FirestoreRepository", "updateUserReferralData: Existing user reinstall - update lastReferredBy=$referrerId")
            }
            
            if (updates.isNotEmpty()) {
                firestore.collection("users").document(userId).update(updates).await()
                
                // Update local cache if this is the current user
                if (userId == getUserId()) {
                    val current = _userProfile.value
                    if (current != null) {
                        _userProfile.value = if (existingUser?.firstReferredBy == null) {
                            current.copy(firstReferredBy = referrerId)
                        } else {
                            current.copy(lastReferredBy = referrerId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "updateUserReferralData failed", e)
        }
    }
    
    /**
     * Rewards the referrer with points and updates their stats
     * 
     * @param referrerId The user who gets the reward
     * @param points Points to award
     * @param reason Reason for the reward
     * @param referredUserId The user who was referred
     * @param token The link token used
     * @param isNewInstall Whether this was a new install
     */
    private suspend fun rewardReferrer(
        referrerId: String, 
        points: Int, 
        reason: String,
        referredUserId: String,
        token: String,
        isNewInstall: Boolean
    ) {
        try {
            // Increment points and appropriate counter
            val updates = mutableMapOf<String, Any>(
                "points" to com.google.firebase.firestore.FieldValue.increment(points.toLong())
            )
            
            if (isNewInstall) {
                updates["referralCount"] = com.google.firebase.firestore.FieldValue.increment(1)
            } else {
                updates["shareCount"] = com.google.firebase.firestore.FieldValue.increment(1)
            }
            
            firestore.collection("users").document(referrerId).update(updates).await()
            
            // Create referral record
            val referralId = UUID.randomUUID().toString()
            val referral = FirestoreReferral(
                id = referralId,
                referrerId = referrerId,
                referredUserId = referredUserId,
                token = token,
                isNewInstall = isNewInstall,
                rewardPoints = points,
                createdAt = System.currentTimeMillis()
            )
            firestore.collection("referrals").document(referralId).set(referral).await()
            
            // Create points transaction record
            val transactionId = UUID.randomUUID().toString()
            val transaction = FirestorePointsTransaction(
                id = transactionId,
                userId = referrerId,
                points = points,
                type = if (isNewInstall) "REFERRAL_INSTALL" else "REFERRAL_SHARE",
                remark = "$reason - User: $referredUserId",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            firestore.collection("transactions").document(transactionId).set(transaction).await()
            
            android.util.Log.d("FirestoreRepository", "rewardReferrer: Awarded $points points to $referrerId for $reason")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "rewardReferrer failed", e)
        }
    }
    
    /**
     * Updates link analytics when accessed
     * 
     * @param token The link token
     * @param isNewInstall Whether this was a new install
     * @param userId The user who accessed
     * @param isFirstAccess Whether this is the first time this user accessed this link
     */
    private suspend fun updateLinkAnalytics(token: String, isNewInstall: Boolean, isFirstAccess: Boolean) {
        try {
            val updates = mutableMapOf<String, Any>(
                "views" to com.google.firebase.firestore.FieldValue.increment(1),
                "users" to com.google.firebase.firestore.FieldValue.increment(1)
            )
            
            if (isNewInstall) {
                updates["installs"] = com.google.firebase.firestore.FieldValue.increment(1)
            }
            
            if (isFirstAccess) {
                updates["uniqueUsers"] = com.google.firebase.firestore.FieldValue.increment(1)
            }
            
            firestore.collection("links").document(token).update(updates).await()
            
            android.util.Log.d("FirestoreRepository", "updateLinkAnalytics: Updated analytics for token=$token")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "updateLinkAnalytics failed", e)
        }
    }
}
