package com.miniclick.calltrackmanage.ui.home.viewmodel

import android.app.Application
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity

object RecordingManager {
    suspend fun reAttachRecordingsForPhone(
        phoneNumber: String,
        callDataRepository: CallDataRepository,
        recordingRepository: RecordingRepository,
        normalizePhoneNumber: (String) -> String
    ): Int {
        val normalizedNumber = normalizePhoneNumber(phoneNumber)
        val logs = callDataRepository.getLogsForPhone(normalizedNumber)
        var updatedCount = 0
        
        val allFiles = recordingRepository.getRecordingFiles()
        
        logs.forEach { log ->
            if (log.duration <= 0 || log.callType >= 3) return@forEach
            
            val bestMatch = recordingRepository.findRecordingInList(
                files = allFiles,
                callDate = log.callDate,
                durationSec = log.duration,
                phoneNumber = log.phoneNumber,
                contactName = log.contactName
            )
            
            if (bestMatch != null && bestMatch != log.localRecordingPath) {
                callDataRepository.updateRecordingPath(log.compositeId, bestMatch)
                updatedCount++
            }
        }
        return updatedCount
    }

    suspend fun reAttachAllRecordings(
        callDataRepository: CallDataRepository,
        recordingRepository: RecordingRepository
    ): Int {
        val allLogs = callDataRepository.getAllCalls()
        var updatedCount = 0
        
        allLogs.forEach { log ->
            val bestMatch = recordingRepository.findRecording(
                callDate = log.callDate,
                durationSec = log.duration,
                phoneNumber = log.phoneNumber,
                contactName = log.contactName
            )
            
            if (bestMatch != null && bestMatch != log.localRecordingPath) {
                callDataRepository.updateRecordingPath(log.compositeId, bestMatch)
                updatedCount++
            }
        }
        return updatedCount
    }
}
