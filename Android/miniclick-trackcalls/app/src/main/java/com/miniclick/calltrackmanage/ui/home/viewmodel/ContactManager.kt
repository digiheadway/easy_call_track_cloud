package com.miniclick.calltrackmanage.ui.home.viewmodel

import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.db.MetadataSyncStatus

object ContactManager {
    suspend fun saveCallNote(compositeId: String, note: String, callDataRepository: CallDataRepository) {
        callDataRepository.updateCallNote(compositeId, note.ifEmpty { null })
    }

    suspend fun savePersonNote(phoneNumber: String, note: String, callDataRepository: CallDataRepository) {
        callDataRepository.updatePersonNote(phoneNumber, note.ifEmpty { null })
    }
    
    suspend fun savePersonLabel(phoneNumber: String, label: String, callDataRepository: CallDataRepository) {
        callDataRepository.updatePersonLabel(phoneNumber, label.ifEmpty { null })
    }

    suspend fun savePersonName(phoneNumber: String, name: String, callDataRepository: CallDataRepository) {
        callDataRepository.updatePersonName(phoneNumber, name.ifEmpty { null })
    }
    
    suspend fun updateReviewed(compositeId: String, reviewed: Boolean, callDataRepository: CallDataRepository) {
        callDataRepository.updateReviewed(compositeId, reviewed)
    }

    suspend fun markAllCallsReviewed(phoneNumber: String, callDataRepository: CallDataRepository) {
        callDataRepository.markAllCallsReviewed(phoneNumber)
    }

    suspend fun updateCallStatus(compositeId: String, status: MetadataSyncStatus, callDataRepository: CallDataRepository) {
        callDataRepository.updateMetadataSyncStatus(compositeId, status)
    }

    suspend fun updateExclusion(phoneNumber: String, excludeFromSync: Boolean, excludeFromList: Boolean, callDataRepository: CallDataRepository) {
        callDataRepository.updateExclusionType(phoneNumber, excludeFromSync, excludeFromList)
    }
}
