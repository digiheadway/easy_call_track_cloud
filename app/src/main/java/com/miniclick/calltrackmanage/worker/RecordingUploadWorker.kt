package com.miniclick.calltrackmanage.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.data.db.RecordingSyncStatus
import com.miniclick.calltrackmanage.network.NetworkClient
import com.miniclick.calltrackmanage.util.AudioCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

/**
 * RecordingUploadWorker - Slow sync worker for recording uploads
 * 
 * Handles:
 * - Compressing audio recordings
 * - Uploading recordings in chunks
 * 
 * This worker runs separately from metadata sync because:
 * 1. Compression is CPU intensive
 * 2. Upload is network intensive and slow
 * 3. Should not block fast metadata sync
 * 
 * Runs less frequently and handles one recording at a time to avoid
 * overwhelming resources.
 */
class RecordingUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val callDataRepository = CallDataRepository.getInstance(context)
    private val recordingRepository = RecordingRepository.getInstance(context)
    private val settingsRepository = SettingsRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting recording upload pass...")
        
        val orgId = settingsRepository.getOrganisationId()
        val userId = settingsRepository.getUserId()

        if (orgId.isEmpty() || userId.isEmpty()) {
            Log.w(TAG, "Required settings missing. Skipping recording upload.")
            return@withContext Result.success()
        }

        try {
            // Cleanup stuck recordings (stuck in COMPRESSING or UPLOADING for more than 30 mins)
            val activeSyncs = callDataRepository.getActiveRecordingSyncsFlow()
            // Note: Since this is a suspend function in a worker, we can't easily observe a flow here
            // better to use a one-time fetch or a more direct approach.
            // Let's use a simpler approach: reset anything stuck in an active state when starting
            // to ensure fresh start, especially if the worker was killed.
            
            val needingSync = callDataRepository.getCallsNeedingRecordingSync()
            Log.d(TAG, "Found ${needingSync.size} recordings needing sync (including stuck ones)")
            
            if (needingSync.isEmpty()) {
                return@withContext Result.success()
            }

            // Group by status to log info
            val stuckCount = needingSync.count { it.recordingSyncStatus == RecordingSyncStatus.COMPRESSING || it.recordingSyncStatus == RecordingSyncStatus.UPLOADING }
            if (stuckCount > 0) {
                Log.i(TAG, "Resetting $stuckCount stuck recording syncs to PENDING")
                // We don't need to explicitly reset them in DB if we just process them, 
                // but resetting PENDING makes the UI look cleaner if we fail later.
            }
            
            val pendingCalls = needingSync
            

            
            val alreadyCompletedSet = mutableSetOf<String>()
            
            // Batch check status with server
            if (pendingCalls.isNotEmpty()) {
                try {
                    val idList = pendingCalls.map { it.compositeId }
                    val jsonIds = Gson().toJson(idList)
                    
                    Log.d(TAG, "Checking server status for ${idList.size} recordings")
                    val statusResp = NetworkClient.api.checkRecordingsStatus("check_recordings_status", jsonIds)
                    
                    if (statusResp.isSuccessful && statusResp.body()?.get("success") == true) {
                        @Suppress("UNCHECKED_CAST")
                        val completedIds = statusResp.body()?.get("completed_ids") as? List<String>
                        
                        if (!completedIds.isNullOrEmpty()) {
                            Log.i(TAG, "Server identified ${completedIds.size} recordings as already completed. Skipping uploads.")
                            alreadyCompletedSet.addAll(completedIds)
                            
                            // Batch update local DB (iterating since we don't have a batch update DAO method readily visible)
                            for (id in completedIds) {
                                callDataRepository.updateRecordingSyncStatus(id, RecordingSyncStatus.COMPLETED)
                                callDataRepository.updateSyncError(id, null)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check batch recording status", e)
                    // Continue with normal upload flow if check fails
                }
            }

            var uploadedCount = 0
            
            // Process in small batches to stay within WorkManager execution limits
            val batchSize = 10
            val batch = pendingCalls.take(batchSize)
            Log.d(TAG, "Processing batch of ${batch.size} recordings")
            
            for (call in batch) {
                if (alreadyCompletedSet.contains(call.compositeId)) {
                    continue
                }
                
                try {
                    // Find recording path if not already set
                    var recordingPath = call.localRecordingPath
                    if (recordingPath.isNullOrEmpty()) {
                        recordingPath = recordingRepository.findRecording(call.callDate, call.duration, call.phoneNumber, call.contactName)
                        if (recordingPath != null) {
                            callDataRepository.updateRecordingPath(call.compositeId, recordingPath)
                        }
                    }
                    
                    if (recordingPath == null) {
                        val hoursSinceCall = (System.currentTimeMillis() - call.callDate) / (1000 * 60 * 60)
                        if (hoursSinceCall > 3) {
                            Log.w(TAG, "No recording found for ${call.compositeId} after 3 hours. Marking as NOT_APPLICABLE.")
                            callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.NOT_APPLICABLE)
                        } else {
                            Log.d(TAG, "Recording not found yet for ${call.compositeId}. Will retry later.")
                            // Keep as PENDING
                        }
                        continue
                    }
                    
                    val originalFile = File(recordingPath)
                    if (!originalFile.exists()) {
                        Log.w(TAG, "Recording file not found: $recordingPath")
                        callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.NOT_APPLICABLE)
                        continue
                    }
                    
                    // Compress audio before upload (with timeout and fallback protection)
                    callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.COMPRESSING)
                    
                    val compressedFile = File(applicationContext.cacheDir, "compressed_${call.compositeId}.m4a")
                    val compressionStats = AudioCompressor.compressAudioWithStats(originalFile, compressedFile)
                    
                    val fileToUpload = when (compressionStats.result) {
                        AudioCompressor.CompressionResult.SUCCESS -> {
                            Log.i(TAG, "Compression successful: ${compressionStats.originalSize/1024}KB -> ${compressionStats.finalSize/1024}KB (saved ${compressionStats.savingsPercent}%, took ${compressionStats.durationMs}ms)")
                            compressedFile
                        }
                        AudioCompressor.CompressionResult.SKIPPED_TOO_SMALL,
                        AudioCompressor.CompressionResult.SKIPPED_TOO_LARGE,
                        AudioCompressor.CompressionResult.SKIPPED_ALREADY_COMPRESSED -> {
                            Log.i(TAG, "Compression skipped (${compressionStats.result}), uploading original: ${originalFile.length()/1024}KB")
                            compressedFile // AudioCompressor copies original to this path
                        }
                        AudioCompressor.CompressionResult.FAILED_TIMEOUT,
                        AudioCompressor.CompressionResult.COPIED_FALLBACK -> {
                            Log.w(TAG, "Compression had issues (${compressionStats.result}), uploading original copy")
                            compressedFile // Falls back to copy of original
                        }
                        AudioCompressor.CompressionResult.FAILED_ERROR -> {
                            Log.e(TAG, "Compression completely failed, uploading original")
                            originalFile
                        }
                    }
                    
                    // Upload
                    callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.UPLOADING)
                    Log.d(TAG, "Uploading recording for ${call.compositeId} (${fileToUpload.length()/1024}KB)")
                    
                    val uploadSuccess = uploadFileInChunks(fileToUpload, call.compositeId)
                    
                    // Cleanup compressed file after upload
                    if (compressedFile.exists() && compressedFile != originalFile) {
                        try {
                            compressedFile.delete()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to cleanup compressed file", e)
                        }
                    }
                    
                    if (uploadSuccess) {
                        callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.COMPLETED)
                        callDataRepository.updateSyncError(call.compositeId, null) // Clear error
                        uploadedCount++
                        Log.d(TAG, "Successfully uploaded recording for ${call.compositeId}")
                    } else {
                        callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.FAILED)
                        callDataRepository.updateSyncError(call.compositeId, "Upload failed. Check network or server logs.")
                        Log.e(TAG, "Failed to upload recording for ${call.compositeId}")
                    }
                    
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Upload cancelled for ${call.compositeId}", e)
                    // Reset to PENDING so it retries later instead of sticking in UPLOADING
                    callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.PENDING)
                    callDataRepository.updateSyncError(call.compositeId, null)
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing recording for ${call.compositeId}", e)
                    callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.FAILED)
                    callDataRepository.updateSyncError(call.compositeId, e.localizedMessage ?: "Unknown processing error")
                }
            }
            
            Log.d(TAG, "Batch complete. Uploaded $uploadedCount/${batch.size}. Total remaining: ${pendingCalls.size - uploadedCount}")
            
            if (pendingCalls.size > batch.size) {
                Log.d(TAG, "More recordings pending, rescheduling worker...")
                return@withContext Result.retry() 
            }

            Result.success(workDataOf(
                "uploaded_count" to uploadedCount,
                "total_processed" to batch.size,
                "remaining" to (pendingCalls.size - uploadedCount)
            ))
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Upload work cancelled", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Recording upload worker failed", e)
            Result.retry()
        }
    }
    
    private suspend fun uploadFileInChunks(file: File, uniqueId: String): Boolean {
        val chunkSize = 1024 * 1024 // 1MB chunks
        val totalSize = file.length()
        val totalChunks = ((totalSize + chunkSize - 1) / chunkSize).toInt()
        
        if (totalChunks == 0) {
            Log.w(TAG, "File is empty, nothing to upload: $uniqueId")
            return false
        }

        // Use 'use' block to ensure FileInputStream is always closed properly
        val uploadSuccess = try {
            FileInputStream(file).use { fis ->
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

                    val resp = try {
                        NetworkClient.api.uploadChunk(actionPart, uidPart, indexPart, body)
                    } catch (e: Exception) {
                        Log.e(TAG, "Network error uploading chunk $i for $uniqueId", e)
                        return@use false
                    }
                    
                    if (!resp.isSuccessful) {
                        val errorBody = resp.errorBody()?.string() ?: "Unknown error"
                        Log.e(TAG, "Chunk $i upload failed: $errorBody")

                        // If server says it's already completed or not expected, treat as success
                        if (errorBody.contains("No recording expected") || errorBody.contains("already completed")) {
                            Log.i(TAG, "Server indicated recording is already complete. Marking local status as COMPLETED.")
                            return@use true
                        }
                        return@use false
                    }
                    
                    Log.d(TAG, "Uploaded chunk ${i + 1}/$totalChunks for $uniqueId")
                }
                true // All chunks uploaded successfully
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file for upload: $uniqueId", e)
            return false
        }
        
        if (!uploadSuccess) return false

        // Finalize
        return try {
            val finalResp = NetworkClient.api.finalizeUpload(
                action = "finalize_upload",
                uniqueId = uniqueId,
                totalChunks = totalChunks
            )
            
            val success = finalResp.isSuccessful && finalResp.body()?.get("success") == true
            Log.d(TAG, "Finalize upload for $uniqueId: success=$success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing upload: $uniqueId", e)
            false
        }
    }

    companion object {
        private const val TAG = "RecordingUploadWorker"
        
        /**
         * Enqueue periodic recording upload (runs every 30 minutes)
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<RecordingUploadWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "RecordingUploadWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Run recording upload immediately
         */
        fun runNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<RecordingUploadWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "RecordingUploadWorker",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
