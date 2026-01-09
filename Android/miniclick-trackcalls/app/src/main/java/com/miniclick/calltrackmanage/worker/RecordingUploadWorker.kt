package com.miniclick.calltrackmanage.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.ProcessMonitor
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.data.db.RecordingSyncStatus
import com.miniclick.calltrackmanage.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import android.net.Uri
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

/**
 * RecordingUploadWorker - Background worker for recording uploads
 * 
 * Handles:
 * - Uploading recordings in chunks (1MB per chunk)
 * 
 * This worker runs separately from metadata sync because:
 * 1. Upload is network intensive
 * 2. Should not block fast metadata sync
 * 
 * Runs every 15 minutes and processes recordings in batches of 10.
 */
class RecordingUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val callDataRepository = CallDataRepository.getInstance(context)
    private val recordingRepository = RecordingRepository.getInstance(context)
    private val settingsRepository = SettingsRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "CallTrack:UploadWakeLock")
        // Acquire lock for 30 minutes max (matches periodic interval + buffer)
        wakeLock.acquire(30 * 60 * 1000L)
        Log.d(TAG, "WakeLock acquired for upload pass")
        
        try {
            Log.d(TAG, "Starting recording upload pass...")
            
            val orgId = settingsRepository.getOrganisationId()
            val userId = settingsRepository.getUserId()
    
            if (orgId.isEmpty() || userId.isEmpty()) {
                Log.w(TAG, "Required settings missing. Skipping recording upload.")
                return@withContext Result.success()
            }
    
            if (!settingsRepository.isCallRecordEnabled()) {
                Log.d(TAG, "Recording tracking disabled by organisation. Skipping uploads.")
                return@withContext Result.success()
            }
    
            // Check for plan expiry
            val expiry = settingsRepository.getPlanExpiryDate()
            if (expiry != null) {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val expiryDate = sdf.parse(expiry)
                    if (expiryDate != null && expiryDate.before(java.util.Date())) {
                        Log.w(TAG, "Plan expired on $expiry. Skipping recording uploads.")
                        return@withContext Result.success()
                    }
                } catch (e: Exception) {}
            }
    
            // Check if storage is full
            val allowedGb = settingsRepository.getAllowedStorageGb()
            val usedBytes = settingsRepository.getStorageUsedBytes()
            if (allowedGb > 0f) {
                val usedGb = usedBytes.toDouble() / (1024 * 1024 * 1024)
                if (usedGb >= allowedGb) {
                    Log.w(TAG, "Organization storage is full (${String.format("%.2f", usedGb)} GB / $allowedGb GB). Skipping recording uploads.")
                    return@withContext Result.success()
                }
            }
    
            try {
                // Cleanup stuck recordings (stuck in COMPRESSING or UPLOADING for more than 30 mins)
                // This is now handled by the reset logic below if we find them in needingSync
                
                val lastEnabledTime = settingsRepository.getRecordingLastEnabledTimestamp()
                val allNeedingSync = callDataRepository.getCallsNeedingRecordingSync()
                
                // Separate calls by whether they occurred after the last enablement
                val (needingSync, skipped) = allNeedingSync.partition { it.callDate >= lastEnabledTime }
                
                if (skipped.isNotEmpty()) {
                    Log.d(TAG, "Skipping ${skipped.size} recordings that occurred while recording was disabled.")
                    for (call in skipped) {
                        callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.NOT_APPLICABLE)
                    }
                }
                
                Log.d(TAG, "Found ${needingSync.size} valid recordings needing sync")
                
                if (needingSync.isEmpty()) {
                    return@withContext Result.success()
                }
    
                // Group by status to log info
                val stuckCount = needingSync.count { it.recordingSyncStatus == RecordingSyncStatus.COMPRESSING || it.recordingSyncStatus == RecordingSyncStatus.UPLOADING }
                if (stuckCount > 0) {
                    Log.i(TAG, "Resetting $stuckCount stuck recording syncs to PENDING")
                    for (call in needingSync) {
                        if (call.recordingSyncStatus == RecordingSyncStatus.COMPRESSING || call.recordingSyncStatus == RecordingSyncStatus.UPLOADING) {
                            callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.PENDING)
                        }
                    }
                }
                
                val pendingCalls = needingSync.sortedByDescending { it.callDate }
                
                // Show initial notification
                setForeground(createForegroundInfo("Checking for updated data.."))
                
                val alreadyCompletedSet = mutableSetOf<String>()
                
                // Batch check status with server
                if (pendingCalls.isNotEmpty()) {
                    try {
                        // Check only the first 50 to avoid massive requests, rest will be checked in loop if needed
                        val idList = pendingCalls.take(50).map { it.compositeId }
                        val jsonIds = Gson().toJson(idList)
                        
                        Log.d(TAG, "Checking server status for top ${idList.size} recordings")
                        val statusResp = NetworkClient.api.checkRecordingsStatus("check_recordings_status", jsonIds)
                        
                        if (statusResp.isSuccessful) {
                            val apiResp = statusResp.body()
                            if (apiResp?.success == true) {
                                val completedIds = apiResp.data?.completedIds
                                
                                if (!completedIds.isNullOrEmpty()) {
                                    Log.i(TAG, "Server identified ${completedIds.size} recordings as already completed.")
                                    alreadyCompletedSet.addAll(completedIds)
                                    
                                    for (id in completedIds) {
                                        callDataRepository.updateRecordingSyncStatus(id, RecordingSyncStatus.COMPLETED)
                                        callDataRepository.updateSyncError(id, null)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to check batch recording status", e)
                    }
                }
    
                var uploadedCount = 0
                var failedCount = 0
                var skippedCount = 0
                
                // Process all pending calls in a loop until done or stopped
                val pendingList = pendingCalls.toMutableList()
                val initialTotal = pendingList.size
                var processedCount = 0
                
                ProcessMonitor.startProcess(ProcessMonitor.ProcessIds.UPLOAD_RECORDINGS, "Uploading Call Recordings")
                Log.d(TAG, "Starting processing of ${initialTotal} recordings")
    
                try {
                    while (pendingList.isNotEmpty()) {
                        if (isStopped) {
                            Log.d(TAG, "Worker stopped, pausing uploads")
                            break
                        }
                        
                        // Process in micro-batches of 2 in parallel to utilize bandwidth
                        // and keep system responsive.
                        val currentBatch = pendingList.take(2)
                        pendingList.removeAll(currentBatch)
                        
                        coroutineScope {
                            currentBatch.map { call ->
                                async {
                                    if (isStopped) return@async
                                    
                                    val currentLocalCount = synchronized(this@RecordingUploadWorker) {
                                        processedCount++
                                        processedCount
                                    }
                                    
                                    val progress = currentLocalCount.toFloat() / initialTotal
                                    ProcessMonitor.updateProgress(ProcessMonitor.ProcessIds.UPLOAD_RECORDINGS, progress, "Uploading $currentLocalCount/$initialTotal")
                                    
                                    // Throttle notification updates (every 2 recordings or if it's the last)
                                    if (currentLocalCount % 2 == 0 || currentLocalCount == initialTotal) {
                                        setForeground(createForegroundInfo("Uploading $currentLocalCount/$initialTotal Recordings.."))
                                    }
                                    
                                    if (alreadyCompletedSet.contains(call.compositeId)) {
                                        skippedCount++
                                        return@async
                                    }

                                    try {
                                        val startTime = System.currentTimeMillis()
                                        val recordingPath = call.localRecordingPath
                                        val success = if (recordingPath.isNullOrEmpty()) {
                                            val foundPath = recordingRepository.findRecording(call.callDate, call.duration, call.phoneNumber, call.contactName)
                                            if (foundPath != null) {
                                                callDataRepository.updateRecordingPath(call.compositeId, foundPath)
                                                uploadRecording(foundPath, call.compositeId)
                                            } else {
                                                val hoursSinceCall = (System.currentTimeMillis() - call.callDate) / (1000 * 60 * 60)
                                                if (hoursSinceCall > 3) {
                                                    callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.NOT_FOUND)
                                                }
                                                false
                                            }
                                        } else {
                                            uploadRecording(recordingPath, call.compositeId)
                                        }
                                        
                                        if (success) {
                                            synchronized(this@RecordingUploadWorker) { uploadedCount++ }
                                            val duration = System.currentTimeMillis() - startTime
                                            Log.d(TAG, "Uploaded ${call.compositeId} in ${duration}ms")
                                        } else {
                                            failedCount++
                                        }
                                    } catch (e: kotlinx.coroutines.CancellationException) {
                                        callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.PENDING)
                                        throw e
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error uploading ${call.compositeId}", e)
                                        callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.FAILED)
                                        failedCount++
                                    }
                                }
                            }.awaitAll()
                        }
                    }
                } finally {
                    ProcessMonitor.endProcess(ProcessMonitor.ProcessIds.UPLOAD_RECORDINGS)
                }
    
                Log.d(TAG, "Upload pass finished. Uploaded $uploadedCount this session.")
    
                Result.success(workDataOf(
                    "uploaded_count" to uploadedCount,
                    "total_processed" to processedCount,
                    "remaining" to (pendingCalls.size - uploadedCount)
                ))
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Upload work cancelled", e)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Recording upload worker failed", e)
                Result.retry()
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
                Log.d(TAG, "WakeLock released")
            }
        }
    }
    
    private suspend fun uploadRecording(recordingPath: String, compositeId: String): Boolean {
        // Verify path exists
        val originalSize: Long
        if (recordingPath.startsWith("content://")) {
            try {
                val uri = Uri.parse(recordingPath)
                originalSize = applicationContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { 
                    it.length 
                } ?: run {
                    callDataRepository.updateRecordingSyncStatus(compositeId, RecordingSyncStatus.NOT_FOUND)
                    return false
                }
            } catch (e: Exception) {
                callDataRepository.updateRecordingSyncStatus(compositeId, RecordingSyncStatus.NOT_FOUND)
                return false
            }
        } else {
            val file = File(recordingPath)
            if (!file.exists()) {
                callDataRepository.updateRecordingSyncStatus(compositeId, RecordingSyncStatus.NOT_FOUND)
                return false
            }
            originalSize = file.length()
        }

        // --- COMPRESSION STEP ---
        // Compress large files to reduce upload size
        var uploadPath = recordingPath
        var compressedFile: File? = null
        
        if (originalSize > 300 * 1024) { // Only compress files > 300KB (saves time on small files)
            try {
                callDataRepository.updateRecordingSyncStatus(compositeId, RecordingSyncStatus.COMPRESSING)
                
                val cacheDir = File(applicationContext.cacheDir, "compressed_recordings")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                
                compressedFile = File(cacheDir, "${compositeId}_compressed.m4a")
                
                val compressionSuccess = withContext(Dispatchers.IO) {
                    com.miniclick.calltrackmanage.util.audio.AudioCompressor.compress(
                        applicationContext,
                        recordingPath,
                        compressedFile
                    )
                }
                
                if (compressionSuccess && compressedFile.exists() && compressedFile.length() > 0) {
                    val compressionRatio = (1 - compressedFile.length().toFloat() / originalSize) * 100
                    Log.d(TAG, "Compression successful: ${originalSize / 1024}KB -> ${compressedFile.length() / 1024}KB (${compressionRatio.toInt()}% reduction)")
                    uploadPath = compressedFile.absolutePath
                } else {
                    Log.w(TAG, "Compression failed or produced empty file, using original")
                    compressedFile?.delete()
                    compressedFile = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Compression error, using original file", e)
                compressedFile?.delete()
                compressedFile = null
            }
        }

        callDataRepository.updateRecordingSyncStatus(compositeId, RecordingSyncStatus.UPLOADING)
        val uploadSuccess = uploadFileInChunks(uploadPath, compositeId)
        
        // Cleanup compressed file after upload
        compressedFile?.delete()
        
        if (uploadSuccess) {
            callDataRepository.updateRecordingSyncStatus(compositeId, RecordingSyncStatus.COMPLETED)
            callDataRepository.updateSyncError(compositeId, null)
            return true
        } else {
            callDataRepository.updateRecordingSyncStatus(compositeId, RecordingSyncStatus.FAILED)
            callDataRepository.updateSyncError(compositeId, "Upload failed")
            return false
        }
    }

    private suspend fun uploadFileInChunks(recordingPath: String, uniqueId: String): Boolean {
        val chunkSize = 2 * 1024 * 1024 // 2MB chunks (reduced request overhead)
        val totalSize = getFileLength(recordingPath)
        val totalChunks = if (totalSize > 0) ((totalSize + chunkSize - 1) / chunkSize).toInt() else 0
        
        if (totalChunks == 0) {
            Log.w(TAG, "File is empty or not found, nothing to upload: $uniqueId")
            return false
        }

        // Use 'use' block to ensure InputStream is always closed properly
        val uploadSuccess = try {
            getFileStream(recordingPath)?.use { inputStream ->
                val buffer = ByteArray(chunkSize)
                
                for (i in 0 until totalChunks) {
                    val bytesRead = withContext(Dispatchers.IO) { inputStream.read(buffer) }
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
                        
                        // If server says it's already completed or not expected, treat as success
                        if (errorBody.contains("No recording expected") || errorBody.contains("already completed")) {
                            Log.i(TAG, "Server indicated recording is already complete for $uniqueId. Skipping remaining chunks.")
                            return@use true
                        }
                        
                        Log.e(TAG, "Chunk $i upload failed for $uniqueId: $errorBody")
                        return@use false
                    }
                    
                    Log.d(TAG, "Uploaded chunk ${i + 1}/$totalChunks for $uniqueId")
                }
                true // All chunks uploaded successfully
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file for upload: $uniqueId", e)
            false
        }
        
        if (!uploadSuccess) return false

        // Finalize
        return try {
            val finalResp = NetworkClient.api.finalizeUpload(
                action = "finalize_upload",
                uniqueId = uniqueId,
                totalChunks = totalChunks
            )
            
            val success = finalResp.isSuccessful && finalResp.body()?.success == true
            Log.d(TAG, "Finalize upload for $uniqueId: success=$success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing upload: $uniqueId", e)
            false
        }
    }

    private fun getFileStream(path: String): InputStream? {
        return try {
            if (path.startsWith("content://")) {
                applicationContext.contentResolver.openInputStream(Uri.parse(path))
            } else {
                FileInputStream(path)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open stream for: $path", e)
            null
        }
    }

    private fun getFileLength(path: String): Long {
        return try {
            if (path.startsWith("content://")) {
                applicationContext.contentResolver.openAssetFileDescriptor(Uri.parse(path), "r")?.use { 
                    it.length 
                } ?: 0L
            } else {
                File(path).length()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get length for: $path", e)
            0L
        }
    }
    
    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val channelId = "upload_channel"
        val title = "Call Cloud Sync"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Background Uploads",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open recording queue when notification is tapped
        val contentIntent = android.content.Intent(applicationContext, com.miniclick.calltrackmanage.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_RECORDING_QUEUE", true)
        }
        val pendingIntentFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext, 1002, contentIntent, pendingIntentFlags
        )

        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(com.miniclick.calltrackmanage.R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Log.d(TAG, "Creating ForegroundInfo with DATA_SYNC type")
            return ForegroundInfo(1002, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }
        return ForegroundInfo(1002, notification)
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
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
