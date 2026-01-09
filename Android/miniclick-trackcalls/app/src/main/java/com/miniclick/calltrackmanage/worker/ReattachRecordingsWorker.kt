package com.miniclick.calltrackmanage.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.ProcessMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReattachRecordingsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val callDataRepository = CallDataRepository.getInstance(context)
    private val recordingRepository = RecordingRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        ProcessMonitor.startProcess(ProcessMonitor.ProcessIds.FIND_RECORDINGS, "Scanning Recordings")
        setForeground(createForegroundInfo("Preparing to scan recordings..."))
        
        try {
            val allLogs = callDataRepository.getAllCalls()
            val total = allLogs.size
            if (total == 0) return@withContext Result.success()

            var updatedCount = 0
            var processed = 0
            
            Log.d(TAG, "Starting re-attach scan for $total calls")
            
            // OPTIMIZATION: Fetch file list ONCE instead of for every call
            val allFiles = recordingRepository.getRecordingFiles()
            Log.d(TAG, "Loaded ${allFiles.size} recording files for matching")
            
            // OPTIMIZATION 2: Group files by Day ID to avoid iterating entire list for every call (O(N*M) -> O(N))
            val DAY_MS = 24 * 60 * 60 * 1000L
            val filesByDay = allFiles.groupBy { it.lastModified / DAY_MS }
            
            // ZIPPER-STYLE: Sort calls chronologically and track matched files
            // This prevents the same recording from being matched to multiple calls
            val sortedLogs = allLogs
                .filter { it.duration > 0 && it.callType < 3 } // Only Incoming(1) and Outgoing(2) with duration
                .sortedBy { it.callDate }
            
            val matchedFilePaths = mutableSetOf<String>()
            
            Log.d(TAG, "Processing ${sortedLogs.size} valid calls (sorted chronologically)")
            
            sortedLogs.forEach { log ->
                 if (isStopped) return@forEach
                 processed++
                 if (processed % 40 == 0) { // Update less frequently to save UI overhead
                     val details = "Scanning recordings $processed / ${sortedLogs.size}"
                     ProcessMonitor.updateProgress(ProcessMonitor.ProcessIds.FIND_RECORDINGS, processed.toFloat() / sortedLogs.size, details)
                     setForeground(createForegroundInfo(details))
                 }
                 
                 val callDay = log.callDate / DAY_MS
                 // Check Day, Day-1, Day+1 to handle timezone/midnight overlaps
                 val relevantFiles = mutableListOf<RecordingRepository.RecordingSourceFile>()
                 filesByDay[callDay]?.let { relevantFiles.addAll(it) }
                 filesByDay[callDay - 1]?.let { relevantFiles.addAll(it) }
                 filesByDay[callDay + 1]?.let { relevantFiles.addAll(it) }
                 
                 // ZIPPER-STYLE: Filter out already matched files to prevent double-matching
                 val availableFiles = relevantFiles.filter { it.absolutePath !in matchedFilePaths }
                 
                 // Skip if no files nearby (huge speedup for old history)
                 if (availableFiles.isEmpty()) return@forEach

                 val bestMatch = recordingRepository.findRecordingInList(
                     files = availableFiles,
                     callDate = log.callDate,
                     durationSec = log.duration,
                     phoneNumber = log.phoneNumber,
                     contactName = log.contactName
                 )
                 
                 if (bestMatch != null && bestMatch != log.localRecordingPath) {
                     callDataRepository.updateRecordingPath(log.compositeId, bestMatch)
                     matchedFilePaths.add(bestMatch) // Mark this file as used
                     updatedCount++
                     Log.d(TAG, "Matched: ${log.phoneNumber} -> ${bestMatch.substringAfterLast("/")}")
                 }
                 
                 if (isStopped) return@forEach
            }
            
            Log.d(TAG, "Re-attach complete. Updated $updatedCount recordings. ${matchedFilePaths.size} unique files matched.")
            
            if (updatedCount > 0) {
                RecordingUploadWorker.runNow(applicationContext)
            }
            
            Result.success(workDataOf("updated_count" to updatedCount))
            
        } catch (e: Exception) {
            Log.e(TAG, "Re-attach failed", e)
            Result.failure()
        } finally {
            ProcessMonitor.endProcess(ProcessMonitor.ProcessIds.FIND_RECORDINGS)
        }
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val channelId = "sync_channel"
        val title = "Scanning Recordings"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Sync Service",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open sync queue when notification is tapped
        val contentIntent = android.content.Intent(applicationContext, com.miniclick.calltrackmanage.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SYNC_QUEUE", true)
        }
        val pendingIntentFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext, 1004, contentIntent, pendingIntentFlags
        )

        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setSmallIcon(com.miniclick.calltrackmanage.R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setProgress(0, 0, true)
            .build()
            
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return ForegroundInfo(1004, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }
        return ForegroundInfo(1004, notification)
    }

    companion object {
        private const val TAG = "ReattachRecordingsWorker"

        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReattachRecordingsWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "ReattachRecordings",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
