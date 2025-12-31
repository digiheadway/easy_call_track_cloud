package com.calltracker.manager.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.calltracker.manager.data.CallDataRepository
import com.calltracker.manager.data.RecordingRepository
import com.calltracker.manager.data.SettingsRepository
import com.calltracker.manager.data.db.CallDataEntity
import com.calltracker.manager.data.db.CallLogStatus
import com.calltracker.manager.network.NetworkClient
import com.calltracker.manager.util.AudioCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val callDataRepository = CallDataRepository(context)
    private val recordingRepository = RecordingRepository(context)
    private val settingsRepository = SettingsRepository(context)

    init {
        Log.d(TAG, "Worker initialized")
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting sync pass...")
        
        val orgId = settingsRepository.getOrganisationId()
        val userId = settingsRepository.getUserId()
        val phone1 = settingsRepository.getCallerPhoneSim1()
        val phone2 = settingsRepository.getCallerPhoneSim2()

        if (orgId.isEmpty() || userId.isEmpty() || (phone1.isEmpty() && phone2.isEmpty())) {
            Log.w(TAG, "Required settings missing: orgId='$orgId', userId='$userId', phones='$phone1 / $phone2'. Skipping sync pass.")
            return@withContext Result.success()
        }

        // First, sync any new calls from system
        callDataRepository.syncFromSystemCallLog()
        
        // Get unsynced calls from Room
        val unsyncedCalls = callDataRepository.getUnsyncedCalls()
        Log.d(TAG, "Found ${unsyncedCalls.size} unsynced calls to process")

        var successCount = 0
        for (call in unsyncedCalls) {
            try {
                Log.d(TAG, "Processing call: ${call.compositeId} to ${call.phoneNumber}")
                val activePhone = getPhoneForCall(call, phone1, phone2)
                uploadCall(call, orgId, userId, activePhone)
                successCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload call ${call.compositeId}", e)
                callDataRepository.updateSyncStatus(call.compositeId, CallLogStatus.FAILED)
            }
        }

        Log.d(TAG, "Sync pass completed. Synced $successCount calls.")
        
        val outputData = workDataOf(
            "unsynced_calls" to unsyncedCalls.size,
            "synced_now" to successCount
        )
        
        Result.success(outputData)
    }

    private suspend fun uploadCall(call: CallDataEntity, orgId: String, userId: String, devicePhone: String) {
        val typeStr = when (call.callType) {
            android.provider.CallLog.Calls.INCOMING_TYPE -> "incoming"
            android.provider.CallLog.Calls.OUTGOING_TYPE -> "outgoing"
            android.provider.CallLog.Calls.MISSED_TYPE -> "missed"
            else -> "unknown"
        }

        // 1. Start Call
        Log.d(TAG, "Starting call on server: ${call.compositeId}")
        val startResp = NetworkClient.api.startCall(
            action = "start_call",
            uniqueId = call.compositeId,
            orgId = orgId,
            userId = userId,
            devicePhone = devicePhone,
            callerName = call.contactName,
            caller = call.phoneNumber,
            type = typeStr,
            duration = call.duration.toInt()
        )
        Log.d(TAG, "Server response code: ${startResp.code()}, body: ${startResp.body()}")
        
        val responseBody = startResp.body()
        val isSuccess = responseBody?.get("success")?.toString()?.toBoolean() ?: false
        
        if (!startResp.isSuccessful || !isSuccess) {
            val error = responseBody?.get("message") ?: responseBody?.get("error") ?: startResp.errorBody()?.string() ?: "Unknown error"
            Log.e(TAG, "Start call rejected by server for ${call.compositeId}. HTTP: ${startResp.code()}, Error: $error")
            callDataRepository.updateSyncStatus(call.compositeId, CallLogStatus.FAILED)
            return
        }
        
        val uploadStatus = responseBody?.get("upload_status")?.toString() ?: "pending"
        Log.d(TAG, "Start call confirmed for ${call.compositeId}, upload_status: $uploadStatus")

        // 2. Find and Upload Recording (only if upload_status is not 'completed')
        if (uploadStatus != "completed") {
            // Use recording path from Room if available, otherwise search
            var recordingPath = call.localRecordingPath
            if (recordingPath.isNullOrEmpty()) {
                recordingPath = recordingRepository.findRecording(call.callDate, call.duration, call.phoneNumber)
                if (recordingPath != null) {
                    callDataRepository.updateRecordingPath(call.compositeId, recordingPath)
                }
            }
            
            if (recordingPath != null) {
                val originalFile = File(recordingPath)
                if (originalFile.exists()) {
                    callDataRepository.updateSyncStatus(call.compositeId, CallLogStatus.COMPRESSING)
                    
                    val compressedFile = File(applicationContext.cacheDir, "compressed_${call.compositeId}.mp3")
                    val success = AudioCompressor.compressAudio(originalFile, compressedFile)
                    val fileToUpload = if (success && compressedFile.exists()) compressedFile else originalFile
                    
                    callDataRepository.updateSyncStatus(call.compositeId, CallLogStatus.UPLOADING)
                    uploadFileInChunks(fileToUpload, call.compositeId)
                    
                    if (compressedFile.exists()) {
                        compressedFile.delete()
                    }
                }
            }
        } else {
            Log.d(TAG, "Skipping recording upload for ${call.compositeId} - upload_status: completed")
        }

        // 3. Update Notes (get from Room)
        val callNote = call.callNote
        val person = callDataRepository.getPersonByNumber(call.phoneNumber)
        val personNote = person?.personNote
        
        if (callNote != null || personNote != null) {
            Log.d(TAG, "Updating notes for ${call.compositeId}")
            val noteResp = NetworkClient.api.updateNote(
                action = "update_note",
                uniqueId = call.compositeId,
                note = callNote,
                personNote = personNote
            )
            Log.d(TAG, "Update note status: ${noteResp.code()}, Body: ${noteResp.body()}")
        }

        // 4. Mark as Synced in Room
        callDataRepository.markAsSynced(call.compositeId)
        Log.d(TAG, "Successfully uploaded call ${call.compositeId}")
    }

    private suspend fun uploadFileInChunks(file: File, uniqueId: String) {
        val chunkSize = 1024 * 1024 // 1MB chunks
        val totalSize = file.length()
        val totalChunks = ((totalSize + chunkSize - 1) / chunkSize).toInt()

        val fis = FileInputStream(file)
        val buffer = ByteArray(chunkSize)

        for (i in 0 until totalChunks) {
            val bytesRead = withContext(Dispatchers.IO) { fis.read(buffer) }
            if (bytesRead == -1) break

            val chunkData = if (bytesRead < chunkSize) buffer.copyOf(bytesRead) else buffer
            val requestFile = chunkData.toRequestBody("audio/*".toMediaTypeOrNull(), 0, bytesRead)
            val body = MultipartBody.Part.createFormData("chunk", "$i", requestFile)

            val actionPart = "upload_chunk".toRequestBody("text/plain".toMediaTypeOrNull())
            val uidPart = uniqueId.toRequestBody("text/plain".toMediaTypeOrNull())
            val indexPart = i.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            val resp = NetworkClient.api.uploadChunk(actionPart, uidPart, indexPart, body)
            if (!resp.isSuccessful) {
                Log.e(TAG, "Chunk $i upload failed")
                fis.close()
                return
            }
        }
        fis.close()

        // Finalize
        val finalResp = NetworkClient.api.finalizeUpload(
            action = "finalize_upload",
            uniqueId = uniqueId,
            totalChunks = totalChunks
        )
        Log.d(TAG, "Finalize upload status: ${finalResp.code()}, Body: ${finalResp.body()}")
    }

    private fun getPhoneForCall(call: CallDataEntity, phone1: String, phone2: String): String {
        // Find which sim slot this subId belongs to
        if (call.subscriptionId == null || call.subscriptionId == -1) {
             return if (phone1.isNotEmpty()) phone1 else phone2
        }

        try {
            val subscriptionManager = applicationContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
            val info = subscriptionManager.getActiveSubscriptionInfo(call.subscriptionId)
            if (info != null) {
                // slotIndex is 0 for SIM 1, 1 for SIM 2
                return when (info.simSlotIndex) {
                    0 -> if (phone1.isNotEmpty()) phone1 else phone2
                    1 -> if (phone2.isNotEmpty()) phone2 else phone1
                    else -> if (phone1.isNotEmpty()) phone1 else phone2
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to check SIM info", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error determining SIM for call", e)
        }

        return if (phone1.isNotEmpty()) phone1 else phone2
    }

    companion object {
        private const val TAG = "UploadWorker"
        
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UploadWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "CallUploadWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
