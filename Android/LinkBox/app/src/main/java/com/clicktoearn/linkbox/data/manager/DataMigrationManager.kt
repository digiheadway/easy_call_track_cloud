package com.clicktoearn.linkbox.data.manager

import com.clicktoearn.linkbox.data.remote.model.toEntityItem
import com.clicktoearn.linkbox.data.remote.model.toSharingEntity
import com.clicktoearn.linkbox.data.remote.model.toJoinedLinkEntity
import com.clicktoearn.linkbox.data.remote.model.toFirestore
import com.clicktoearn.linkbox.data.repository.FirestoreRepository
import com.clicktoearn.linkbox.data.repository.LinkBoxRepository
import kotlinx.coroutines.flow.first

class DataMigrationManager(
    private val linkBoxRepository: LinkBoxRepository,
    private val firestoreRepository: FirestoreRepository
) {

    suspend fun uploadAllLocalData(ownerId: Long, userName: String) {
        // Sync Entities
        val entities = linkBoxRepository.getAllEntitiesByOwner(ownerId).first()
        entities.forEach { entity ->
            try {
                firestoreRepository.saveEntity(entity.toFirestore())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Sync Sharings
        val sharings = linkBoxRepository.getAllSharings().first()
        sharings.forEach { sharing ->
            try {
                firestoreRepository.saveSharing(sharing, userName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Sync Joined Links
        val joinedLinks = linkBoxRepository.getAllJoinedLinks().first()
        joinedLinks.forEach { joinedLink ->
            try {
                firestoreRepository.saveJoinedLink(joinedLink.toFirestore())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun downloadAllCloudData(ownerId: Long) {
        // Sync Entities
        try {
            val firestoreEntities = firestoreRepository.getEntities()
            val entities = firestoreEntities.map { it.toEntityItem(ownerId) }
            if (entities.isNotEmpty()) {
                linkBoxRepository.restoreEntities(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sync Sharings
        try {
            val firestoreSharings = firestoreRepository.getSharings()
            val sharings = firestoreSharings.map { it.toSharingEntity() }
            if (sharings.isNotEmpty()) {
                linkBoxRepository.restoreSharings(sharings)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Sync Joined Links
        try {
            val firestoreJoinedLinks = firestoreRepository.getJoinedLinks()
            val joinedLinks = firestoreJoinedLinks.map { it.toJoinedLinkEntity() }
            if (joinedLinks.isNotEmpty()) {
                linkBoxRepository.insertJoinedLinks(joinedLinks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
