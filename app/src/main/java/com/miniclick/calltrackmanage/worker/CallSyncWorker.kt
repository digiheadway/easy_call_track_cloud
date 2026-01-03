package com.miniclick.calltrackmanage.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.data.db.MetadataSyncStatus
import com.miniclick.calltrackmanage.data.db.RecordingSyncStatus
import com.miniclick.calltrackmanage.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

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
        Log.d(TAG, "Starting metadata sync pass...")
        
        val orgId = settingsRepository.getOrganisationId()
        val userId = settingsRepository.getUserId()
        val phone1 = settingsRepository.getCallerPhoneSim1()
        val phone2 = settingsRepository.getCallerPhoneSim2()
        val simSelection = settingsRepository.getSimSelection()

        if (orgId.isEmpty() || userId.isEmpty()) {
            Log.w(TAG, "Required settings missing: orgId='$orgId', userId='$userId'. Skipping sync.")
            return@withContext Result.success()
        }

        val deviceId = android.provider.Settings.Secure.getString(
            applicationContext.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        try {
            // Phase 0: Fetch Config FROM server (Excluded contacts, Settings)
            fetchConfigFromServer(orgId, userId)

            // Phase 1: PULL - Fetch updates from server (delta sync)
            val lastSyncTime = settingsRepository.getLastSyncTime()
            pullServerUpdates(orgId, userId, deviceId, lastSyncTime)
            
            // Phase 2: Sync new calls from system log to Room
            callDataRepository.syncFromSystemCallLog()
            
            // Phase 3: PUSH - Sync pending calls to server (metadata only)
            val pendingCalls = callDataRepository.getCallsNeedingMetadataSync()
            Log.d(TAG, "Found ${pendingCalls.size} calls needing metadata sync")
            
            var syncedCount = 0
            for (call in pendingCalls) {
                try {
                    val activePhone = getPhoneForCall(call.subscriptionId, phone1, phone2)

                    // Check if number is excluded
                    if (settingsRepository.isNumberExcluded(call.phoneNumber)) {
                        Log.d(TAG, "Skipping excluded number: ${call.phoneNumber}. Marking as SYNCED to clear queue.")
                        // Mark as synced so it doesn't stay in the pending queue
                        callDataRepository.markMetadataSynced(call.compositeId, System.currentTimeMillis())
                        // Also ensure recording status is NOT_APPLICABLE
                        callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.NOT_APPLICABLE)
                        continue
                    }
                    
                    // Filter by SIM selection
                    val shouldSync = when (simSelection) {
                        "Sim1" -> activePhone == phone1
                        "Sim2" -> activePhone == phone2
                        else -> true
                    }
                    
                    if (!shouldSync) {
                        Log.d(TAG, "Skipping call ${call.compositeId} - SIM filter")
                        continue
                    }
                    
                    when (call.metadataSyncStatus) {
                        MetadataSyncStatus.PENDING -> {
                            // New call - sync to server
                            syncNewCall(call, orgId, userId, deviceId, activePhone)
                        }
                        MetadataSyncStatus.UPDATE_PENDING -> {
                            // Existing call with local changes - push updates
                            pushCallUpdates(call)
                        }
                        MetadataSyncStatus.FAILED -> {
                            // Retry failed sync
                            syncNewCall(call, orgId, userId, deviceId, activePhone)
                        }
                        else -> {}
                    }
                    syncedCount++
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync call ${call.compositeId}", e)
                    callDataRepository.updateMetadataSyncStatus(call.compositeId, MetadataSyncStatus.FAILED)
                }
            }
            
            // Phase 4: Push pending person updates
            pushPendingPersonUpdates(orgId, userId)
            
            // Update last sync time
            settingsRepository.setLastSyncTime(System.currentTimeMillis())
            
            val pendingRecordingCount = callDataRepository.getCallsNeedingRecordingSync().size
            if (pendingRecordingCount > 0) {
                Log.d(TAG, "Triggering RecordingUploadWorker for $pendingRecordingCount pending recordings")
                RecordingUploadWorker.runNow(applicationContext)
            }

            Result.success(workDataOf(
                "synced_calls" to syncedCount,
                "pending_recordings" to pendingRecordingCount
            ))
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Sync work cancelled", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Metadata sync failed", e)
            Result.retry()
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
                val body = response.body()
                if (body?.get("success") == true) {
                    // Process call updates
                    @Suppress("UNCHECKED_CAST")
                    val callUpdates = body["call_updates"] as? List<Map<String, Any>> ?: emptyList()
                    Log.d(TAG, "Received ${callUpdates.size} call updates from server")
                    
                    for (update in callUpdates) {
                        val uniqueId = update["unique_id"] as? String ?: continue
                        val reviewed = (update["reviewed"] as? Number)?.toInt() == 1
                        val note = update["note"] as? String
                        val callerName = update["caller_name"] as? String
                        val serverUpdatedAt = (update["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        
                        callDataRepository.updateCallFromServer(uniqueId, reviewed, note, callerName, serverUpdatedAt)
                    }
                    
                    // Process person updates
                    @Suppress("UNCHECKED_CAST")
                    val personUpdates = body["person_updates"] as? List<Map<String, Any>> ?: emptyList()
                    Log.d(TAG, "Received ${personUpdates.size} person updates from server")
                    
                    for (update in personUpdates) {
                        val phone = update["phone"] as? String ?: continue
                        val personNote = update["person_note"] as? String
                        val label = update["label"] as? String
                        val name = update["name"] as? String
                        val serverUpdatedAt = (update["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        
                        callDataRepository.updatePersonFromServer(phone, personNote, label, name, serverUpdatedAt)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull server updates", e)
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
            }.format(java.util.Date(call.callDate))
        )
        
        if (response.isSuccessful && response.body()?.get("success") == true) {
            val serverTime = (response.body()?.get("server_time") as? Number)?.toLong() ?: System.currentTimeMillis()
            
            // Mark metadata as synced
            callDataRepository.markMetadataSynced(call.compositeId, serverTime)
            
            // Determine if recording upload is needed
            val uploadStatus = response.body()?.get("upload_status")?.toString() ?: "pending"
            if (uploadStatus != "completed" && call.duration > 0) {
                callDataRepository.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.PENDING)
            }
            
            Log.d(TAG, "Successfully synced call metadata: ${call.compositeId}")
            callDataRepository.updateSyncError(call.compositeId, null) // Clear error on success
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
            uniqueId = call.compositeId,
            reviewed = call.reviewed,
            note = call.callNote,
            callerName = call.contactName,
            updatedAt = call.updatedAt
        )
        
        if (response.isSuccessful && response.body()?.get("success") == true) {
            val serverTime = (response.body()?.get("server_time") as? Number)?.toLong() ?: System.currentTimeMillis()
            callDataRepository.markMetadataSynced(call.compositeId, serverTime)
            callDataRepository.updateSyncError(call.compositeId, null) // Clear error
            Log.d(TAG, "Successfully pushed call updates: ${call.compositeId}")
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
                
                if (response.isSuccessful && response.body()?.get("success") == true) {
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

    companion object {
        private const val TAG = "CallSyncWorker"
        
        /**
         * Enqueue periodic sync (runs every 2 hours)
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
         * Run sync immediately (triggered by events like call end, note edit, etc.)
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
    }

    private suspend fun fetchConfigFromServer(orgId: String, userId: String) {
        try {
            Log.d(TAG, "Fetching config from server...")
            val resp = NetworkClient.api.fetchConfig("fetch_config", orgId, userId)
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body?.get("success") == true) {
                    @Suppress("UNCHECKED_CAST")
                    val settingsData = body["settings"] as? Map<String, Any>
                    if (settingsData != null) {
                        // Robust parsing helper
                        fun parseBool(key: String): Boolean {
                            val v = settingsData[key]
                            return when (v) {
                                is Boolean -> v
                                is Number -> v.toInt() == 1
                                is String -> v == "1" || v.equals("true", ignoreCase = true)
                                else -> false
                            }
                        }

                        val allowChanging = parseBool("allow_changing_tracking_start_date")
                        val defaultDateStr = settingsData["default_tracking_starting_date"] as? String
                        
                        settingsRepository.setAllowPersonalExclusion(parseBool("allow_personal_exclusion"))
                        settingsRepository.setAllowChangingTrackStartDate(allowChanging)
                        settingsRepository.setAllowUpdatingTrackSims(parseBool("allow_updating_tracking_sims"))
                        settingsRepository.setDefaultTrackStartDate(defaultDateStr)
                        
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
                        Log.d(TAG, "Settings updated from server")
                    }

                    @Suppress("UNCHECKED_CAST")
                    val excluded = body["excluded_contacts"] as? List<String>
                    if (excluded != null) {
                        settingsRepository.setExcludedContacts(excluded.toSet())
                        Log.d(TAG, "Received ${excluded.size} excluded contacts from server")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch config from server", e)
        }
    }
}
