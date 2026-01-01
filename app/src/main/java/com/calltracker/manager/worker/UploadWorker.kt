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
        val simSelection = settingsRepository.getSimSelection() // "Both", "Sim1", "Sim2"

        if (orgId.isEmpty() || userId.isEmpty()) {
            Log.w(TAG, "Required settings missing: orgId='$orgId', userId='$userId'. Skipping sync pass.")
            return@withContext Result.success()
        }

        // 1. Fetch updates FROM server first (Bidirectional Sync)
        fetchContactsFromServer(orgId, userId)

        // 2. Sync any new calls from system log
        callDataRepository.syncFromSystemCallLog()
        
        // 3. Process new or failed calls
        val allUnsyncedCalls = callDataRepository.getUnsyncedCalls()
        val unsyncedCalls = allUnsyncedCalls.filter { call ->
            // Filter by SIM selection and ignore those that ONLY need note updates for now
            if (call.syncStatus == CallLogStatus.NOTE_UPDATE_PENDING) return@filter false
            
            val phoneForCall = getPhoneForCall(call, phone1, phone2)
            when (simSelection) {
                "Sim1" -> phoneForCall == phone1
                "Sim2" -> phoneForCall == phone2
                else -> true 
            }
        }
        
        var successCount = 0
        for (call in unsyncedCalls) {
            try {
                val activePhone = getPhoneForCall(call, phone1, phone2)
                uploadCall(call, orgId, userId, activePhone)
                successCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload call ${call.compositeId}", e)
                callDataRepository.updateSyncStatus(call.compositeId, CallLogStatus.FAILED)
            }
        }

        // 4. Update notes for already synced calls (NOTE_UPDATE_PENDING)
        val noteUpdateCalls = allUnsyncedCalls.filter { it.syncStatus == CallLogStatus.NOTE_UPDATE_PENDING }
        for (call in noteUpdateCalls) {
            try {
                updateCallNotesOnly(call)
                successCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notes for call ${call.compositeId}", e)
            }
        }

        // 5. Update person labels/notes (Person Level Sync)
        syncPendingPersons(orgId, userId)

        if (unsyncedCalls.isNotEmpty() || successCount > 0) {
            settingsRepository.setLastSyncTime(System.currentTimeMillis())
        }
        
        val outputData = workDataOf(
            "total_calls" to allUnsyncedCalls.size,
            "synced_now" to successCount
        )
        
        Result.success(outputData)
    }

    private suspend fun fetchContactsFromServer(orgId: String, userId: String) {
        try {
            Log.d(TAG, "Fetching contacts from server...")
            val deviceId = android.provider.Settings.Secure.getString(
                applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"

            val resp = NetworkClient.api.fetchContacts("fetch_contacts", orgId, userId, deviceId)
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body?.get("success") == true) {
                    val contactsData = body["contacts"] as? List<Map<String, Any>>
                    if (contactsData != null) {
                        Log.d(TAG, "Received ${contactsData.size} contacts from server")
                        // Map to internal DTOs or handle directly
                        val updates = contactsData.map {
                            com.calltracker.manager.network.PersonUpdateDto(
                                phone = it["phone"] as String,
                                name = it["name"] as? String,
                                personNote = it["person_note"] as? String,
                                label = it["label"] as? String
                            )
                        }

                        // Parse Call Updates
                        val callsData = body["call_updates"] as? List<Map<String, Any>>
                        val callUpdates = callsData?.map {
                            com.calltracker.manager.network.CallUpdateDto(
                                uniqueId = it["unique_id"] as String,
                                note = it["note"] as? String ?: ""
                            )
                        } ?: emptyList()

                        callDataRepository.saveRemoteUpdates(updates, callUpdates)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch contacts from server", e)
        }
    }

    private suspend fun updateCallNotesOnly(call: CallDataEntity) {
        val person = callDataRepository.getPersonByNumber(call.phoneNumber)
        val resp = NetworkClient.api.updateNote(
            action = "update_note",
            uniqueId = call.compositeId,
            note = call.callNote,
            personNote = person?.personNote,
            label = person?.label
        )
        if (resp.isSuccessful && resp.body()?.get("success") == true) {
            callDataRepository.markAsSynced(call.compositeId)
            Log.d(TAG, "Updated notes for already synced call: ${call.compositeId}")
        }
    }

    private suspend fun syncPendingPersons(orgId: String, userId: String) {
        val pendingPersons = callDataRepository.getPendingSyncPersons()
        if (pendingPersons.isEmpty()) return

        Log.d(TAG, "Syncing ${pendingPersons.size} persons with pending updates")
        for (person in pendingPersons) {
            try {
                // To update a person we need a unique_id of their last call or any call
                // If they have no calls yet, we can't use update_note action.
                // But usually persons are created/updated via call logs.
                val lastCallId = person.lastCallCompositeId
                if (lastCallId != null) {
                    val resp = NetworkClient.api.updateNote(
                        action = "update_note",
                        uniqueId = lastCallId,
                        note = null, // Don't overwrite call note
                        personNote = person.personNote,
                        label = person.label
                    )
                    if (resp.isSuccessful && resp.body()?.get("success") == true) {
                        callDataRepository.updatePersonSyncStatus(person.phoneNumber, false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync person ${person.phoneNumber}", e)
            }
        }
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
        val deviceId = android.provider.Settings.Secure.getString(
            applicationContext.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
        
        val startResp = NetworkClient.api.startCall(
            action = "start_call",
            uniqueId = call.compositeId,
            orgId = orgId,
            userId = userId,
            deviceId = deviceId,
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
        
        if (callNote != null || personNote != null || person?.label != null) {
            Log.d(TAG, "Updating notes for ${call.compositeId}")
            val noteResp = NetworkClient.api.updateNote(
                action = "update_note",
                uniqueId = call.compositeId,
                note = callNote,
                personNote = personNote,
                label = person?.label
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

        fun runNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "CallUploadWorker_Immediate",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
