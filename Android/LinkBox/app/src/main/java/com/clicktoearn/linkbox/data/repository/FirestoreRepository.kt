package com.clicktoearn.linkbox.data.repository

import com.clicktoearn.linkbox.data.remote.model.FirestoreEntity
import com.clicktoearn.linkbox.data.remote.model.FirestoreSharing
import com.clicktoearn.linkbox.data.remote.model.FirestoreJoinedLink
import com.clicktoearn.linkbox.data.remote.model.toFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // ==================== ENTITIES ====================

    suspend fun saveEntity(entity: FirestoreEntity) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("entities").document(entity.id)
            .set(entity)
            .await()
    }

    suspend fun getEntities(): List<FirestoreEntity> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val snapshot = db.collection("users").document(userId)
            .collection("entities")
            .get()
            .await()
        return snapshot.toObjects(FirestoreEntity::class.java)
    }

    suspend fun getEntity(userId: String, entityId: String): FirestoreEntity? {
        val snapshot = db.collection("users").document(userId)
            .collection("entities").document(entityId)
            .get()
            .await()
        return snapshot.toObject(FirestoreEntity::class.java)
    }

    suspend fun deleteEntity(entityId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("entities").document(entityId)
            .delete()
            .await()
    }

    // ==================== SHARINGS ====================

    suspend fun saveSharing(sharing: FirestoreSharing) {
        // Save to global 'sharings' collection for public access
        db.collection("sharings").document(sharing.token)
            .set(sharing)
            .await()
            
        // Also link it to user for management (optional, can query by ownerId)
        // Also link it to user for management (optional, can query by ownerId)
    }

    suspend fun saveSharing(sharing: com.clicktoearn.linkbox.data.entity.SharingEntity, userName: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            android.util.Log.e("FirestoreRepo", "Cannot save sharing: No authenticated user")
            return
        }
        val firestoreSharing = sharing.toFirestore(userId, userName)
        saveSharing(firestoreSharing)
    }
    
    suspend fun getSharing(token: String): FirestoreSharing? {
        val snapshot = db.collection("sharings").document(token).get().await()
        return snapshot.toObject(FirestoreSharing::class.java)
    }

    suspend fun incrementSharingStats(token: String, isClick: Boolean) {
        val field = if (isClick) "clicks" else "views"
        db.collection("sharings").document(token)
            .update(field, com.google.firebase.firestore.FieldValue.increment(1))
            .await()
    }

    suspend fun deleteSharing(token: String) {
        db.collection("sharings").document(token).delete().await()
    }

    suspend fun getSharings(): List<FirestoreSharing> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val snapshot = db.collection("sharings")
            .whereEqualTo("ownerId", userId)
            .get()
            .await()
        return snapshot.toObjects(FirestoreSharing::class.java)
    }

    // ==================== JOINED LINKS ====================

    suspend fun saveJoinedLink(joinedLink: FirestoreJoinedLink) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("joined_links").document(joinedLink.token)
            .set(joinedLink)
            .await()
    }

    suspend fun getJoinedLinks(): List<FirestoreJoinedLink> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val snapshot = db.collection("users").document(userId)
            .collection("joined_links")
            .get()
            .await()
        return snapshot.toObjects(FirestoreJoinedLink::class.java)
    }

    suspend fun deleteJoinedLink(token: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("joined_links").document(token)
            .delete()
            .await()
    }
}
