package com.miniclick.calltrackmanage.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.data.db.MetadataSyncStatus
import com.miniclick.calltrackmanage.data.db.RecordingSyncStatus
import com.miniclick.calltrackmanage.network.NetworkClient
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import com.miniclick.calltrackmanage.data.CallRemoteUpdate
import com.miniclick.calltrackmanage.data.PersonRemoteUpdate

/**
 * CallSyncWorker - Fast metadata sync worker
 * 
 * Handles:
 * - Syncing new calls to server (metadata only, no recordings)
 * - Pushing local changes (notes, reviewed, labels)
 * - Pulling server changes (bidirectional sync)
 * 
 * This worker runs frequently and is fast because it only syncs metadata.
 * Recording uploads are handled by RecordingUploadWorker separately.
 */
class CallSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val callDataRepository = CallDataRepository.getInstance(context)
    private val settingsRepository = SettingsRepository.getInstance(context)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "CallTrack:SyncWakeLock")
        // Acquire wake lock with a timeout to prevent infinite battery drain in case of bugs (e.g. 10 mins)
        wakeLock.acquire(10 * 60 * 1000L)
        Log.d(TAG, "WakeLock acquired for sync")

        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Missing READ_CALL_LOG permission. Skipping sync pass.")
                return@withContext Result.success()
            }
            
            // Check if this is a quick sync (skip system import)
            val skipSystemImport = inputData.getBoolean(KEY_SKIP_SYSTEM_IMPORT, false)
            
            Log.d(TAG, "Starting sync pass... (skipSystemImport=$skipSystemImport)")
            
            // PHASE 0: Sync from system call log to local Room DB (OFFLINE-FIRST)
            // Skip this phase for quick syncs (e.g., after note edit) to avoid showing
            // "Importing Data" / "Finding Recordings" status for minor operations
            if (!skipSystemImport) {
                // Update notification for local import
                setForeground(createForegroundInfo("Importing call history..."))
                try {
                    callDataRepository.syncFromSystemCallLog()
                    Log.d(TAG, "Local call import completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to import calls from system", e)
                }
            } else {
                Log.d(TAG, "Skipping system call log import (quick sync mode)")
            }
            
            // Update notification for network sync
            setForeground(createForegroundInfo("Syncing with cloud..."))
            
            // Check if server sync is configured
            val orgId = settingsRepository.getOrganisationId()
            val userId = settingsRepository.getUserId()
            val phone1 = settingsRepository.getCallerPhoneSim1()
            val phone2 = settingsRepository.getCallerPhoneSim2()
            val simSelection = settingsRepository.getSimSelection()

            if (orgId.isEmpty() || userId.isEmpty()) {
                Log.d(TAG, "Server sync not configured (no pairing). Local import done.")
                return@withContext Result.success()
            }

            // Cleanup any stale processing statuses (stuck from crashes)
            callDataRepository.clearStaleProcessingStatuses()

            if (!settingsRepository.isCallTrackEnabled()) {
                Log.d(TAG, "Call tracking disabled by organisation. Skipping server sync phases.")
                // Still fetch config to see if it gets re-enabled later
                try {
                    fetchConfigFromServer(orgId, userId)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch config while tracking is disabled", e)
                }
                return@withContext Result.success()
            }

            val deviceId = android.provider.Settings.Secure.getString(
                applicationContext.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"

            try {
                // Phase 1: Fetch Config FROM server (Excluded contacts, Settings) 
                // We do this first as other phases might depend on the latest settings/exclusion list
                fetchConfigFromServer(orgId, userId)

                coroutineScope {
                    // Phase 2: PULL - Fetch updates from server (delta sync)
                    val lastSyncTime = settingsRepository.getLastSyncTime()
                    val pullJob = async { pullServerUpdates(orgId, userId, deviceId, lastSyncTime) }
                    
                    // Phase 3 & 4: PUSH - Sync pending calls and persons in parallel with PULL
                    val pushJob = async {
                        var localSynced = 0
                        val allPendingCalls = callDataRepository.getCallsNeedingMetadataSync()
                        Log.d(TAG, "Parallel Push: Processing ${allPendingCalls.size} calls")
                        
                        val (newCalls, updateCalls) = allPendingCalls.partition { 
                            it.metadataSyncStatus == MetadataSyncStatus.PENDING || it.metadataSyncStatus == MetadataSyncStatus.FAILED 
                        }
                        
                        // 3a. Process Updates
                        for (call in updateCalls) {
                            if (callDataRepository.isExcludedFromSync(call.phoneNumber)) {
                                callDataRepository.markMetadataSynced(call.compositeId, System.currentTimeMillis())
                                continue
                            }
                            try {
                                pushCallUpdates(call)
                                localSynced++
                            } catch (e: Exception) { Log.e(TAG, "Update push failed for ${call.compositeId}", e) }
                        }
                        
                        // 3b. Process New Calls (BATCHED)
                        val callsToSync = newCalls.filter { call ->
                            if (callDataRepository.isExcludedFromSync(call.phoneNumber)) {
                                callDataRepository.markMetadataSynced(call.compositeId, System.currentTimeMillis())
                                callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.NOT_APPLICABLE)
                                false
                            } else {
                                val activePhone = getPhoneForCall(call.subscriptionId, phone1, phone2)
                                when (simSelection) {
                                    "Sim1" -> activePhone == phone1
                                    "Sim2" -> activePhone == phone2
                                    else -> true
                                }
                            }
                        }
                        
                        if (callsToSync.isNotEmpty()) {
                            val chunks = callsToSync.chunked(100)
                            for ((index, batch) in chunks.withIndex()) {
                                try {
                                    syncBatchCalls(batch, orgId, userId, deviceId, phone1, phone2)
                                    localSynced += batch.size
                                } catch (e: Exception) { Log.e(TAG, "Batch sync failed", e) }
                            }
                        }
                        localSynced
                    }
                    
                    val pushPersonsJob = async {
                        pushPendingPersonUpdates(orgId, userId)
                    }

                    // Wait for all parallel operations to complete
                    val (pullRes, syncedCount, _) = awaitAll(pullJob, pushJob, pushPersonsJob)
                    
                    // Update sync time only after successful pass
                    settingsRepository.setLastSyncTime(System.currentTimeMillis())
                    
                    val finalSyncedCount = syncedCount as? Int ?: 0
                    val pendingRecordingCount = callDataRepository.getCallsNeedingRecordingSync().size
                    
                    if (pendingRecordingCount > 0) {
                        Log.d(TAG, "Triggering RecordingUploadWorker for $pendingRecordingCount pending recordings")
                        RecordingUploadWorker.runNow(applicationContext)
                    }

                    Result.success(workDataOf(
                        "synced_calls" to finalSyncedCount,
                        "pending_recordings" to pendingRecordingCount
                    ))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Sync work cancelled", e)
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Metadata sync failed", e)
                Result.retry()
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
                Log.d(TAG, "WakeLock released")
            }
        }
    }
    
    private suspend fun pullServerUpdates(orgId: String, userId: String, deviceId: String, lastSyncTime: Long) {
        try {
            Log.d(TAG, "Pulling updates since $lastSyncTime")
            
            val response = NetworkClient.api.fetchUpdates(
                action = "fetch_updates",
                orgId = orgId,
                userId = userId,
                deviceId = deviceId,
                lastSyncTime = lastSyncTime
            )
            
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true && apiResponse.data != null) {
                    val data = apiResponse.data
                    
                    // Process call updates from DTOs
                    val callUpdates = data.callUpdates?.map { update ->
                        CallRemoteUpdate(
                            compositeId = update.uniqueId,
                            reviewed = false, // Server DTO for CallUpdateDto only has unique_id and note currently
                            note = update.note,
                            callerName = null,
                            serverUpdatedAt = data.serverTime
                        )
                    } ?: emptyList()
                    
                    if (callUpdates.isNotEmpty()) {
                        callDataRepository.updateCallsFromServerBatch(callUpdates)
                    }
                    
                    // Process person updates from DTOs
                    val personUpdates = data.personUpdates?.map { update ->
                        PersonRemoteUpdate(
                            phone = update.phone,
                            personNote = update.personNote,
                            label = update.label,
                            name = update.name,
                            excludeFromSync = null,
                            excludeFromList = null,
                            serverUpdatedAt = data.serverTime
                        )
                    } ?: emptyList()
                    
                    if (personUpdates.isNotEmpty()) {
                        callDataRepository.updatePersonsFromServerBatch(personUpdates)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull server updates", e)
        }
    }

    private suspend fun syncBatchCalls(
        calls: List<com.miniclick.calltrackmanage.data.db.CallDataEntity>,
        orgId: String,
        userId: String,
        deviceId: String,
        phone1: String,
        phone2: String
    ) {
        // Prepare payload
        val payloadList = calls.map { call ->
            val activePhone = getPhoneForCall(call.subscriptionId, phone1, phone2)
            val typeStr = when (call.callType) {
                android.provider.CallLog.Calls.INCOMING_TYPE -> "incoming"
                android.provider.CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                android.provider.CallLog.Calls.MISSED_TYPE -> "missed"
                5 -> "rejected"
                6 -> "blocked"
                else -> "unknown"
            }
            val callTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(call.callDate))
            
            mapOf(
                "unique_id" to call.compositeId,
                "caller_name" to (call.contactName ?: ""),
                "caller" to call.phoneNumber,
                "type" to typeStr,
                "duration" to call.duration,
                "call_time" to callTime,
                "device_phone" to activePhone,
                "upload_status" to mapRecordingStatusToServer(call.recordingSyncStatus)
            )
        }
        
        val callsJson = com.google.gson.Gson().toJson(payloadList)
        
        Log.d(TAG, "Sending batch of ${calls.size} calls")
        val response = NetworkClient.api.batchSyncCalls(
            action = "batch_sync_calls", // Server must handle this action
            orgId = orgId,
            userId = userId,
            deviceId = deviceId,
            devicePhone = "", // Passed inside each call object now, or global device phone? API def had it. We can leave empty here if server looks at JSON.
            callsJson = callsJson
        )
        
        if (response.isSuccessful) {
            val apiResponse = response.body()
            if (apiResponse?.success == true && apiResponse.data != null) {
                val data = apiResponse.data
                val serverTime = data.serverTime
                val syncedIds = data.syncedIds
                
                // 1. Mark metadata synced in batch
                callDataRepository.markMetadataSyncedBatch(syncedIds, serverTime)
                
                // 2. Mark metadata as received
                syncedIds.forEach { id ->
                    callDataRepository.updateMetadataReceived(id, true)
                }

                // 3. Update recording statuses if provided
                data.recordingStatuses?.forEach { (id, status) ->
                    callDataRepository.updateServerRecordingStatus(id, status)
                }
                
                // 4. Clear errors in batch
                callDataRepository.updateSyncErrorBatch(syncedIds, null)
                
                // 5. Update recording status in batch (for those needing it)
                val idsNeedingRecording = calls.filter { call -> 
                    syncedIds.contains(call.compositeId) && call.duration > 0 
                }.map { it.compositeId }
                
                if (idsNeedingRecording.isNotEmpty()) {
                    callDataRepository.updateRecordingSyncStatusBatch(idsNeedingRecording, RecordingSyncStatus.PENDING)
                }
                
                Log.d(TAG, "Batch sync success: ${syncedIds.size} calls synced")
            } else {
                 Log.w(TAG, "Batch sync returned success=false or no data. Marking all as synced.")
                 // Fallback: This logic might be risky if server actually failed. 
                 // But preserving "always mark synced if success==false" logic from original code (?)
                 // Original code was: "Batch sync returned success but no synced_ids list... Fallback"
                 // Wait, original check was `isList != null`. If null, fallback.
                 // Here `data` might be null.
                 
                val errorMsg = apiResponse?.error ?: apiResponse?.message ?: "Batch sync unknown error"
                Log.e(TAG, "Batch sync error from server: $errorMsg")
                throw Exception(errorMsg)
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: "Batch sync failed"
            Log.e(TAG, "Batch sync failed: $errorMsg")
            // Throw to trigger retry or fallback (although we catch in loop)
            throw Exception(errorMsg)
        }
    }
    
    private suspend fun syncNewCall(
        call: com.miniclick.calltrackmanage.data.db.CallDataEntity,
        orgId: String,
        userId: String,
        deviceId: String,
        devicePhone: String
    ) {
        val typeStr = when (call.callType) {
            android.provider.CallLog.Calls.INCOMING_TYPE -> "incoming"
            android.provider.CallLog.Calls.OUTGOING_TYPE -> "outgoing"
            android.provider.CallLog.Calls.MISSED_TYPE -> "missed"
            5 -> "rejected" // REJECTED_TYPE
            6 -> "blocked"  // BLOCKED_TYPE
            else -> "unknown"
        }
        
        Log.d(TAG, "Syncing new call: ${call.compositeId}")
        
        val response = NetworkClient.api.startCall(
            action = "start_call",
            uniqueId = call.compositeId,
            orgId = orgId,
            userId = userId,
            deviceId = deviceId,
            devicePhone = devicePhone,
            callerName = call.contactName,
            caller = call.phoneNumber,
            type = typeStr,
            duration = call.duration.toInt(),
            callTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date(call.callDate)),
            uploadStatus = mapRecordingStatusToServer(call.recordingSyncStatus)
        )
        
        if (response.isSuccessful) {
            val apiResponse = response.body()
            if (apiResponse?.success == true && apiResponse.data != null) {
                val data = apiResponse.data
                val serverTime = System.currentTimeMillis() // StartCallResponse doesn't strictly have server_time, use current or parse from created_ts if needed. Assuming current is fine or use created_ts if exists. created_ts is string.
                
                // Mark metadata as synced
                callDataRepository.markMetadataSynced(call.compositeId, serverTime)
                
                // Confirm metadata received
                callDataRepository.updateMetadataReceived(call.compositeId, true)
                
                // Update server recording status
                callDataRepository.updateServerRecordingStatus(call.compositeId, data.uploadStatus)
                
                // Determine if recording upload is needed
                val uploadStatus = data.uploadStatus
                if (uploadStatus != "completed" && call.duration > 0) {
                    callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.PENDING)
                }
                
                Log.d(TAG, "Successfully synced call metadata: ${call.compositeId}")
                callDataRepository.updateSyncError(call.compositeId, null) // Clear error on success
            } else {
                 val errorMsg = apiResponse?.error ?: apiResponse?.message ?: "Unknown error"
                 Log.e(TAG, "Failed to sync call: $errorMsg")
                 callDataRepository.updateMetadataSyncStatus(call.compositeId, MetadataSyncStatus.FAILED)
                 callDataRepository.updateSyncError(call.compositeId, errorMsg)
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: "Unknown error"
            Log.e(TAG, "Failed to sync call: $errorMsg")
            callDataRepository.updateMetadataSyncStatus(call.compositeId, MetadataSyncStatus.FAILED)
            callDataRepository.updateSyncError(call.compositeId, errorMsg)
        }
    }
    
    private suspend fun pushCallUpdates(call: com.miniclick.calltrackmanage.data.db.CallDataEntity) {
        Log.d(TAG, "Pushing updates for call: ${call.compositeId}")
        
        val response = NetworkClient.api.updateCall(
            action = "update_call",
            unique_id = call.compositeId,
            reviewed = call.reviewed,
            note = call.callNote,
            callerName = call.contactName,
            uploadStatus = mapRecordingStatusToServer(call.recordingSyncStatus),
            updatedAt = call.updatedAt
        )
        
        if (response.isSuccessful) {
            val apiResponse = response.body()
            if (apiResponse?.success == true) {
                val serverTime = apiResponse.data?.serverTime ?: System.currentTimeMillis()
                callDataRepository.markMetadataSynced(call.compositeId, serverTime)
                callDataRepository.updateSyncError(call.compositeId, null) // Clear error
                Log.d(TAG, "Successfully pushed call updates: ${call.compositeId}")
            } else {
                 val errorMsg = apiResponse?.error ?: apiResponse?.message ?: "Unknown error"
                 Log.e(TAG, "Failed to push call updates: $errorMsg")
                 callDataRepository.updateSyncError(call.compositeId, errorMsg)
            }
        } else {
            val errorMsg = response.errorBody()?.string() ?: "Unknown error"
            Log.e(TAG, "Failed to push call updates: $errorMsg")
            callDataRepository.updateSyncError(call.compositeId, errorMsg)
        }
    }
    
    private suspend fun pushPendingPersonUpdates(orgId: String, userId: String) {
        val pendingPersons = callDataRepository.getPendingSyncPersons()
        if (pendingPersons.isEmpty()) return
        
        Log.d(TAG, "Pushing ${pendingPersons.size} person updates")
        
        for (person in pendingPersons) {
            try {
                val response = NetworkClient.api.updatePerson(
                    action = "update_person",
                    phone = person.phoneNumber,
                    orgId = orgId,
                    personNote = person.personNote,
                    label = person.label,
                    name = person.contactName,
                    updatedAt = person.updatedAt
                )

                // Also update the label on the last call so it appears in the Calls UI immediately
                if (!person.lastCallCompositeId.isNullOrEmpty()) {
                    try {
                        NetworkClient.api.updateNote(
                            action = "update_note",
                            uniqueId = person.lastCallCompositeId,
                            note = null,
                            personNote = null,
                            label = person.label
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update label on last call: ${person.lastCallCompositeId}", e)
                    }
                }
                
                if (response.isSuccessful && response.body()?.success == true) {
                    callDataRepository.updatePersonSyncStatus(person.phoneNumber, false)
                    Log.d(TAG, "Successfully pushed person update: ${person.phoneNumber}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push person update: ${person.phoneNumber}", e)
            }
        }
    }
    
    private fun getPhoneForCall(subscriptionId: Int?, phone1: String, phone2: String): String {
        if (subscriptionId == null || subscriptionId == -1) {
            return if (phone1.isNotEmpty()) phone1 else phone2
        }

        try {
            val subscriptionManager = applicationContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as android.telephony.SubscriptionManager
            val info = subscriptionManager.getActiveSubscriptionInfo(subscriptionId)
            if (info != null) {
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

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val channelId = "sync_channel"
        val title = "Call Cloud Sync"

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
            applicationContext, 1003, contentIntent, pendingIntentFlags
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
            return ForegroundInfo(1003, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }
        return ForegroundInfo(1003, notification)
    }

    companion object {
        private const val TAG = "CallSyncWorker"
        private const val KEY_SKIP_SYSTEM_IMPORT = "skip_system_import"
        
        /**
         * Enqueue periodic sync (runs every 30 minutes)
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<CallSyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "CallSyncWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Run full sync immediately (imports from system call log + pushes pending changes)
         * Use this after call ends, pull-to-refresh, or when app starts
         */
        fun runNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<CallSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "CallSyncWorker_Immediate",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /**
         * Run quick sync (only pushes pending metadata changes, skips system import)
         * Use this for lightweight operations like note edit, label change, etc.
         * Avoids showing "Importing Data" / "Finding Recordings" status for minor operations
         */
        fun runQuickSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val inputData = workDataOf(KEY_SKIP_SYSTEM_IMPORT to true)

            val request = OneTimeWorkRequestBuilder<CallSyncWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "CallSyncWorker_QuickSync",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    private fun getBatteryLevel(): Int {
        return try {
            val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                applicationContext.registerReceiver(null, ifilter)
            }
            val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        } catch (e: Exception) {
            -1
        }
    }

    private suspend fun fetchConfigFromServer(orgId: String, userId: String) {
        try {
            Log.d(TAG, "Fetching config from server...")
            val osVersion = android.os.Build.VERSION.RELEASE
            val batteryPct = getBatteryLevel()
            val deviceModel = android.os.Build.MODEL
            
            val resp = NetworkClient.api.fetchConfig("fetch_config", orgId, userId, osVersion, if (batteryPct >= 0) batteryPct else null, deviceModel)
            if (resp.isSuccessful) {
                val apiResponse = resp.body()
                if (apiResponse?.success == true && apiResponse.data != null) {
                    val data = apiResponse.data
                    val settings = data.settings
                    
                    val allowChanging = settings.allowChangingTrackingStartDate == 1
                    val defaultDateStr = settings.defaultTrackingStartingDate
                    
                    settingsRepository.setAllowPersonalExclusion(settings.allowPersonalExclusion == 1)
                    settingsRepository.setAllowChangingTrackStartDate(allowChanging)
                    settingsRepository.setAllowUpdatingTrackSims(settings.allowUpdatingTrackingSims == 1)
                    settingsRepository.setDefaultTrackStartDate(defaultDateStr)
                    settingsRepository.setCallTrackEnabled(settings.callTrack == 1)
                    settingsRepository.setCallRecordEnabled(settings.callRecordCrm == 1)
                    
                    // Handle forced mobile network upload
                    settingsRepository.setForceUploadOverMobile(data.forceUploadOverMobile == 1)
                    
                    // If not allowed to change, and a SPECIFIC default date is provided, enforce it
                    if (!allowChanging && !defaultDateStr.isNullOrBlank()) {
                        try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            sdf.parse(defaultDateStr)?.let { date ->
                                // Only update if it's different from current to avoid unnecessary Flow triggers
                                if (settingsRepository.getTrackStartDate() != date.time) {
                                    Log.d(TAG, "Enforcing server-side tracking start date: $defaultDateStr")
                                    settingsRepository.setTrackStartDate(date.time)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse default date: $defaultDateStr", e)
                        }
                    }

                    val plan = data.plan
                    settingsRepository.setPlanExpiryDate(plan.expiryDate)
                    settingsRepository.setAllowedStorageGb(plan.allowedStorageGb)
                    settingsRepository.setStorageUsedBytes(plan.storageUsedBytes)

                    val excluded = data.excludedContacts
                    if (excluded != null) {
                        settingsRepository.setExcludedContacts(excluded.toSet())
                        Log.d(TAG, "Received ${excluded.size} excluded contacts from server")
                    }
                    Log.d(TAG, "Settings updated from server")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch config from server", e)
        }
    }

    private fun mapRecordingStatusToServer(status: com.miniclick.calltrackmanage.data.db.RecordingSyncStatus): String? {
        return when (status) {
            com.miniclick.calltrackmanage.data.db.RecordingSyncStatus.COMPLETED -> "completed"
            com.miniclick.calltrackmanage.data.db.RecordingSyncStatus.NOT_FOUND -> "not_found"
            com.miniclick.calltrackmanage.data.db.RecordingSyncStatus.FAILED -> "failed"
            com.miniclick.calltrackmanage.data.db.RecordingSyncStatus.NOT_APPLICABLE -> "completed"
            else -> null
        }
    }
}

