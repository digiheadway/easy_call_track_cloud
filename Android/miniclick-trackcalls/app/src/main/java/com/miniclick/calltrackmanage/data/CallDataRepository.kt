package com.miniclick.calltrackmanage.data

import android.content.Context
import android.provider.CallLog
import android.provider.Settings
import android.util.Log
import com.miniclick.calltrackmanage.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import com.miniclick.calltrackmanage.network.PersonUpdateDto
import com.miniclick.calltrackmanage.network.CallUpdateDto
import androidx.room.withTransaction

/**
 * Unified repository for all call and person data.
 * Room is the single source of truth.
 * System call log is only used to sync new calls.
 * 
 * Uses singleton pattern to prevent multiple instances and ensure consistent state.
 */
data class CallRemoteUpdate(
    val compositeId: String,
    val reviewed: Boolean,
    val note: String?,
    val callerName: String?,
    val serverUpdatedAt: Long
)

data class PersonRemoteUpdate(
    val phone: String,
    val personNote: String?,
    val label: String?,
    val name: String?,
    val serverUpdatedAt: Long
)

class CallDataRepository private constructor(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val callDataDao = database.callDataDao()
    private val personDataDao = database.personDataDao()
    private val recordingRepository = RecordingRepository.getInstance(context)
    private val settingsRepository = SettingsRepository.getInstance(context)
    
    companion object {
        private const val TAG = "CallDataRepository"
        
        @Volatile
        private var INSTANCE: CallDataRepository? = null
        
        fun getInstance(context: Context): CallDataRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallDataRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // ============================================
    // CALL DATA - READ OPERATIONS
    // ============================================
    
    /**
     * Get all calls as Flow for real-time updates (UI observes this)
     */
    fun getAllCallsFlow(): Flow<List<CallDataEntity>> = callDataDao.getAllCallsFlow()
    
    /**
     * Get all calls (one-time fetch)
     */
    suspend fun getAllCalls(): List<CallDataEntity> = withContext(Dispatchers.IO) {
        callDataDao.getAllCalls()
    }
    
    /**
     * Get a specific call by composite ID
     */
    suspend fun getCallByCompositeId(compositeId: String): CallDataEntity? = withContext(Dispatchers.IO) {
        callDataDao.getByCompositeId(compositeId)
    }
    
    /**
     * Get calls missing local recording paths (to trigger a find)
     */
    suspend fun getCallsMissingRecordings(): List<CallDataEntity> = withContext(Dispatchers.IO) {
        callDataDao.getCallsMissingRecordings()
    }
    
    /**
     * Get pending calls as Flow
     */
    fun getPendingCallsFlow(): Flow<List<CallDataEntity>> = callDataDao.getPendingCallsFlow()
    
    // ============================================
    // CALL DATA - WRITE OPERATIONS
    // ============================================
    
    /**
     * Update call note
     */
    /**
     * Update call note and trigger sync if needed
     */
    suspend fun updateCallNote(compositeId: String, note: String?) = withContext(Dispatchers.IO) {
        callDataDao.updateCallNote(compositeId, note)
        
        // Mark call for metadata update sync (use new metadataSyncStatus field)
        val call = callDataDao.getByCompositeId(compositeId)
        if (call != null && call.metadataSyncStatus == MetadataSyncStatus.SYNCED) {
            callDataDao.updateMetadataSyncStatus(compositeId, MetadataSyncStatus.UPDATE_PENDING)
            Log.d(TAG, "Marked call $compositeId for note sync (UPDATE_PENDING)")
        }
    }
    
    /**
     * Update sync status
     */
    suspend fun updateSyncStatus(compositeId: String, status: CallLogStatus) = withContext(Dispatchers.IO) {
        callDataDao.updateSyncStatus(compositeId, status)
    }
    
    /**
     * Update recording path
     */
    suspend fun updateRecordingPath(compositeId: String, path: String?) = withContext(Dispatchers.IO) {
        callDataDao.updateRecordingPath(compositeId, path)
    }
    
    /**
     * Mark call as synced (COMPLETED status)
     */
    suspend fun markAsSynced(compositeId: String) = withContext(Dispatchers.IO) {
        callDataDao.updateSyncStatus(compositeId, CallLogStatus.COMPLETED)
    }
    
    /**
     * Check if call is synced
     */
    suspend fun isSynced(compositeId: String): Boolean = withContext(Dispatchers.IO) {
        val call = callDataDao.getByCompositeId(compositeId)
        call?.syncStatus == CallLogStatus.COMPLETED
    }
    
    // ============================================
    // NEW SPLIT SYNC METHODS
    // ============================================
    
    /**
     * Get calls needing metadata sync (fast sync)
     */
    suspend fun getCallsNeedingMetadataSync(): List<CallDataEntity> = withContext(Dispatchers.IO) {
        val minDate = settingsRepository.getTrackStartDate()
        callDataDao.getCallsNeedingMetadataSync(minDate)
    }
    
    /**
     * Get calls needing recording upload (slow sync)
     * Only returns calls where metadata is already SYNCED, to avoid "Call not found" errors on server.
     */
    suspend fun getCallsNeedingRecordingSync(): List<CallDataEntity> = withContext(Dispatchers.IO) {
        val minDate = settingsRepository.getTrackStartDate()
        val calls = callDataDao.getCallsNeedingRecordingSync(minDate)
        
        // Ensure metadata is synced first
        calls.filter { it.metadataSyncStatus == MetadataSyncStatus.SYNCED }
    }
    
    /**
     * Update metadata sync status
     */
    suspend fun updateMetadataSyncStatus(compositeId: String, status: MetadataSyncStatus) = withContext(Dispatchers.IO) {
        callDataDao.updateMetadataSyncStatus(compositeId, status)
    }
    
    /**
     * Update recording sync status
     */
    suspend fun updateRecordingSyncStatus(compositeId: String, status: RecordingSyncStatus) = withContext(Dispatchers.IO) {
        callDataDao.updateRecordingSyncStatus(compositeId, status)
        
        // If recording is NOT_FOUND, we want to push this status update to the server metadata
        if (status == RecordingSyncStatus.NOT_FOUND) {
            callDataDao.updateMetadataSyncStatus(compositeId, MetadataSyncStatus.UPDATE_PENDING)
        }
    }

    /**
     * Rescan skipped recordings (mark NOT_APPLICABLE as PENDING)
     */
    suspend fun resetSkippedRecordings() = withContext(Dispatchers.IO) {
        callDataDao.resetSkippedRecordings()
        Log.d(TAG, "Reset all skipped recordings to PENDING")
    }
    
    /**
     * Mark metadata as synced with server timestamp
     */
    suspend fun markMetadataSynced(compositeId: String, serverTime: Long) = withContext(Dispatchers.IO) {
        callDataDao.markMetadataSynced(compositeId, serverTime)
    }
    
    /**
     * Update call from server (bidirectional sync - pull changes)
     */
    suspend fun updateCallFromServer(
        compositeId: String,
        reviewed: Boolean,
        note: String?,
        callerName: String?,
        serverUpdatedAt: Long
    ) = withContext(Dispatchers.IO) {
        callDataDao.updateFromServer(compositeId, reviewed, note, callerName, serverUpdatedAt)
        Log.d(TAG, "Updated call from server: $compositeId (reviewed=$reviewed)")
    }

    /**
     * Update sync error message
     */
    suspend fun updateSyncError(compositeId: String, error: String?) = withContext(Dispatchers.IO) {
        callDataDao.updateSyncError(compositeId, error)
    }

    /**
     * Update multiple calls from server in batch
     */
    suspend fun updateCallsFromServerBatch(updates: List<CallRemoteUpdate>) = withContext(Dispatchers.IO) {
        if (updates.isEmpty()) return@withContext
        database.withTransaction {
            updates.forEach { update ->
                callDataDao.updateFromServer(
                    update.compositeId, 
                    update.reviewed, 
                    update.note, 
                    update.callerName, 
                    update.serverUpdatedAt
                )
            }
        }
        Log.d(TAG, "Batch updated ${updates.size} calls from server")
    }
    
    /**
     * Update person from server (bidirectional sync - pull changes)
     */
    suspend fun updatePersonFromServer(
        phone: String,
        personNote: String?,
        label: String?,
        name: String?,
        serverUpdatedAt: Long
    ) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phone)
        
        // Check if person exists
        val existing = personDataDao.getByPhoneNumber(normalized)
        if (existing != null) {
            personDataDao.updateFromServer(normalized, personNote, label, name, serverUpdatedAt)
        } else {
            // Create new person entry from server data
            personDataDao.insert(PersonDataEntity(
                phoneNumber = normalized,
                contactName = name,
                personNote = personNote,
                label = label,
                serverUpdatedAt = serverUpdatedAt
            ))
        }
        Log.d(TAG, "Updated person from server: $normalized")
    }

    /**
     * Update multiple persons from server in batch
     */
    suspend fun updatePersonsFromServerBatch(updates: List<PersonRemoteUpdate>) = withContext(Dispatchers.IO) {
        if (updates.isEmpty()) return@withContext
        database.withTransaction {
            updates.forEach { update ->
                val normalized = normalizePhoneNumber(update.phone)
                // Check if person exists (sync call inside transaction)
                val existing = personDataDao.getByPhoneNumber(normalized)
                if (existing != null) {
                    personDataDao.updateFromServer(normalized, update.personNote, update.label, update.name, update.serverUpdatedAt)
                } else {
                    personDataDao.insert(PersonDataEntity(
                        phoneNumber = normalized,
                        contactName = update.name,
                        personNote = update.personNote,
                        label = update.label,
                        serverUpdatedAt = update.serverUpdatedAt
                    ))
                }
            }
        }
        Log.d(TAG, "Batch updated ${updates.size} persons from server")
    }
    
    /**
     * Update reviewed status (local change, will be synced)
     */
    suspend fun updateReviewed(compositeId: String, reviewed: Boolean) = withContext(Dispatchers.IO) {
        callDataDao.updateReviewed(compositeId, reviewed)
        Log.d(TAG, "Updated reviewed status for $compositeId: $reviewed")
    }

    /**
     * Mark all calls for a phone number as reviewed
     */
    suspend fun markAllCallsReviewed(phoneNumber: String) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        callDataDao.markAllCallsReviewed(normalized)
        Log.d(TAG, "Marked all calls for $normalized as reviewed")
    }
    
    /**
     * Get pending metadata sync count as Flow
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPendingMetadataSyncCountFlow(): Flow<Int> = 
        combine(
            settingsRepository.getTrackStartDateFlow(),
            settingsRepository.getOrganisationIdFlow()
        ) { minDate, orgId ->
            minDate to orgId
        }.flatMapLatest { (minDate, orgId) ->
            if (orgId.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(0)
            } else {
                callDataDao.getPendingMetadataSyncCountFlow(minDate)
            }
        }
    
    /**
     * Get pending recording sync count as Flow
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPendingRecordingSyncCountFlow(): Flow<Int> = 
        combine(
            settingsRepository.getTrackStartDateFlow(),
            settingsRepository.getOrganisationIdFlow()
        ) { minDate, orgId ->
            minDate to orgId
        }.flatMapLatest { (minDate, orgId) ->
            if (orgId.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(0)
            } else {
                callDataDao.getPendingRecordingSyncCountFlow(minDate)
            }
        }
    
    /**
     * Get pending new calls count as Flow
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPendingNewCallsCountFlow(): Flow<Int> = 
        combine(
            settingsRepository.getTrackStartDateFlow(),
            settingsRepository.getOrganisationIdFlow()
        ) { minDate, orgId ->
            minDate to orgId
        }.flatMapLatest { (minDate, orgId) ->
            if (orgId.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(0)
            } else {
                callDataDao.getPendingNewCallsCountFlow(minDate)
            }
        }

    /**
     * Get pending metadata updates count as Flow (notes, reviewed, etc)
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getPendingMetadataUpdatesCountFlow(): Flow<Int> = 
        combine(
            settingsRepository.getTrackStartDateFlow(),
            settingsRepository.getOrganisationIdFlow()
        ) { minDate, orgId ->
            minDate to orgId
        }.flatMapLatest { (minDate, orgId) ->
            if (orgId.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(0)
            } else {
                callDataDao.getPendingMetadataUpdatesCountFlow(minDate)
            }
        }

    /**
     * Get pending person updates count as Flow
     */
    fun getPendingPersonUpdatesCountFlow(): Flow<Int> = 
        settingsRepository.getOrganisationIdFlow().flatMapLatest { orgId ->
            if (orgId.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(0)
            } else {
                personDataDao.getPendingSyncPersonsCountFlow()
            }
        }

    /**
     * Get active recording syncs as Flow (for monitoring progress)
     */
    fun getActiveRecordingSyncsFlow(): Flow<List<CallDataEntity>> = 
        settingsRepository.getOrganisationIdFlow().flatMapLatest { orgId ->
            if (orgId.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                callDataDao.getActiveRecordingSyncsFlow()
            }
        }
    
    // ============================================
    // PERSON DATA - READ OPERATIONS
    // ============================================
    
    /**
     * Get all persons as Flow for real-time updates
     */
    fun getAllPersonsFlow(): Flow<List<PersonDataEntity>> = personDataDao.getAllPersonsFlow()
    
    /**
     * Get excluded persons as Flow
     */
    fun getExcludedPersonsFlow(): Flow<List<PersonDataEntity>> = personDataDao.getExcludedPersonsFlow()
    
    /**
     * Get all persons (one-time fetch)
     */
    suspend fun getAllPersons(): List<PersonDataEntity> = withContext(Dispatchers.IO) {
        personDataDao.getAllPersons()
    }
    
    /**
     * Get person by phone number
     */
    suspend fun getPersonByNumber(phoneNumber: String): PersonDataEntity? = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        personDataDao.getByPhoneNumber(normalized)
    }
    
    /**
     * Get person note
     */
    suspend fun getPersonNote(phoneNumber: String): String? = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        personDataDao.getByPhoneNumber(normalized)?.personNote
    }
    
    /**
     * Get person data by phone number (for Caller ID overlay)
     */
    suspend fun getPersonData(phoneNumber: String): PersonDataEntity? = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        personDataDao.getByPhoneNumber(normalized)
    }

    suspend fun getPendingSyncPersons(): List<PersonDataEntity> = withContext(Dispatchers.IO) {
        personDataDao.getPendingSyncPersons()
    }

    fun getPendingSyncPersonsFlow(): Flow<List<PersonDataEntity>> = personDataDao.getPendingSyncPersonsFlow()

    suspend fun updatePersonSyncStatus(phoneNumber: String, needsSync: Boolean) = withContext(Dispatchers.IO) {
        personDataDao.updateSyncStatus(phoneNumber, needsSync)
    }

    fun getPendingChangesCountFlow(): Flow<Int> {
        return combine(
            getPendingNewCallsCountFlow(),
            getPendingMetadataUpdatesCountFlow(),
            getPendingPersonUpdatesCountFlow(),
            getPendingRecordingSyncCountFlow()
        ) { newCalls, updates, persons, recordings -> newCalls + updates + persons + recordings }
    }
    
    // ============================================
    // PERSON DATA - WRITE OPERATIONS
    // ============================================
    
    /**
     * Update person note and mark latest call as needing update
     */
    suspend fun updatePersonNote(phoneNumber: String, note: String?) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        
        // 1. Update Person Table (Dao handles needsSync=1)
        val existing = personDataDao.getByPhoneNumber(normalized)
        if (existing != null) {
            personDataDao.updatePersonNote(normalized, note)
        } else {
            personDataDao.insert(PersonDataEntity(
                phoneNumber = normalized,
                personNote = note,
                needsSync = true
            ))
        }
    }

    /**
     * Update person label
     */
    suspend fun updatePersonLabel(phoneNumber: String, label: String?) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        val existing = personDataDao.getByPhoneNumber(normalized)
        if (existing != null) {
            personDataDao.updateLabel(normalized, label)
        } else {
            // Create person entry if doesn't exist
            personDataDao.insert(PersonDataEntity(
                phoneNumber = normalized,
                label = label,
                needsSync = true
            ))
        }
    }

    /**
     * Update person name (custom name set by user, synced to server)
     */
    suspend fun updatePersonName(phoneNumber: String, name: String?) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        val existing = personDataDao.getByPhoneNumber(normalized)
        if (existing != null) {
            personDataDao.updateName(normalized, name)
        } else {
            // Create person entry if doesn't exist
            personDataDao.insert(PersonDataEntity(
                phoneNumber = normalized,
                contactName = name,
                needsSync = true
            ))
        }
    }
    
    /**
     * Save updates fetched from remote
     */
    suspend fun saveRemoteUpdates(personUpdates: List<PersonUpdateDto>, callUpdates: List<CallUpdateDto>) = withContext(Dispatchers.IO) {
         Log.d(TAG, "Saving ${personUpdates.size} person updates and ${callUpdates.size} call updates")
         
         personUpdates.forEach { update ->
                val normalized = normalizePhoneNumber(update.phone)
                Log.d(TAG, "Processing update for ${update.phone} -> Normalized: $normalized. Label: ${update.label}")
                
                var existing = personDataDao.getByPhoneNumber(normalized)
                
                // Fallback: try matching without '+' if normalized starts with it
                if (existing == null && normalized.startsWith("+")) {
                    val stripped = normalized.substring(1)
                    existing = personDataDao.getByPhoneNumber(stripped)
                }
                
                if (existing != null) {
                    val key = existing.phoneNumber
                    if (update.name != null) personDataDao.updateNameFromRemote(key, update.name)
                    if (update.personNote != null) personDataDao.updatePersonNoteFromRemote(key, update.personNote)
                    if (update.label != null) personDataDao.updateLabelFromRemote(key, update.label)
                } else {
                    Log.d(TAG, "Inserting new person entry for $normalized")
                    personDataDao.insert(PersonDataEntity(
                        phoneNumber = normalized,
                        contactName = update.name,
                        personNote = update.personNote,
                        label = update.label
                    ))
                }
         }
         callUpdates.forEach { update ->
             callDataDao.updateCallNote(update.uniqueId, update.note)
         }
    }
    
    /**
     * Update exclusion status for a phone number
     */
    suspend fun updateExclusion(phoneNumber: String, isExcluded: Boolean) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        val existing = personDataDao.getByPhoneNumber(normalized)
        if (existing != null) {
            personDataDao.updateExclusion(normalized, isExcluded)
        } else {
            // Create person entry if doesn't exist
            personDataDao.insert(PersonDataEntity(
                phoneNumber = normalized,
                isExcluded = isExcluded
            ))
        }
    }
    
    // ============================================
    // SYNC WITH SYSTEM CALL LOG
    // ============================================
    
    /**
     * Sync new calls from system call log to Room database.
     * This runs in background and updates Room with any new calls.
     * Also finds and updates recording paths.
     */
    suspend fun syncFromSystemCallLog() = withContext(Dispatchers.IO) {
        try {
            ProcessMonitor.startProcess("Importing Data from System...", isIndeterminate = true)
            Log.d(TAG, "Starting sync from system call log...")
            
            val filterDate = settingsRepository.getTrackStartDate()
            val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            val simSelection = settingsRepository.getSimSelection()
            
            // Skip if tracking is Off
            if (simSelection == "Off") {
                Log.d(TAG, "Call tracking is OFF, skipping sync")
                return@withContext
            }

            // Optimize: Only fetch calls since the latest call in our DB (with a 2-day safety buffer)
            // or the tracking start date, whichever is more recent.
            val latestCallDate = callDataDao.getMaxCallDate() ?: 0L
            val fetchStartDate = maxOf(filterDate, latestCallDate - (2 * 24 * 60 * 60 * 1000L))
            
            // Get existing IDs once (only need those since fetchStartDate)
            val existingIds = callDataDao.getCompositeIdsSince(fetchStartDate).toSet()
            Log.d(TAG, "Existing calls since ${java.util.Date(fetchStartDate)}: ${existingIds.size}")
            
            // Fetch from system call log with SIM filter  
            val systemCalls = fetchSystemCallLog(fetchStartDate, deviceId, simSelection)
            Log.d(TAG, "System call log returned: ${systemCalls.size} calls (SIM: $simSelection)")
            
            ProcessMonitor.updateProgress(0.6f, "Processing ${systemCalls.size} calls")

            val newCalls = mutableListOf<CallDataEntity>()
            
            // Track persons to update
            val personCallsMap = mutableMapOf<String, MutableList<CallDataEntity>>()
            
            for (call in systemCalls) {
                val normalized = normalizePhoneNumber(call.phoneNumber)
                
                // Collect new calls for batch insert
                if (!existingIds.contains(call.compositeId)) {
                    newCalls.add(call)
                }
                
                // Track for person update (all calls from system log should be considered)
                personCallsMap.getOrPut(normalized) { mutableListOf() }.add(call)
            }
            
            // Batch insert new calls
            if (newCalls.isNotEmpty()) {
                callDataDao.insertAll(newCalls)
                Log.d(TAG, "Inserted ${newCalls.size} new calls")
            }
            
            // Update recordings for calls that don't have them yet (including existing ones)
            // We do this after insertion to simplify
            val unsyncedWithNoPath = callDataDao.getCallsMissingRecordings().filter { it.localRecordingPath == null }
            if (unsyncedWithNoPath.isNotEmpty()) {
                ProcessMonitor.startProcess("Finding Recordings of Calls")
                val recordingFiles = recordingRepository.getRecordingFiles()
                val total = unsyncedWithNoPath.size
                
                val updates = mutableMapOf<String, String>()
                
                unsyncedWithNoPath.forEachIndexed { index, call ->
                    // Update progress every 10 items to reduce overhead
                    if (index % 10 == 0) {
                        val progress = index.toFloat() / total
                        ProcessMonitor.updateProgress(progress, "${(progress * 100).toInt()}% Done")
                    }

                    if (call.duration > 0) {
                        val recordingPath = recordingRepository.findRecordingInList(
                            recordingFiles,
                            call.callDate,
                            call.duration,
                            call.phoneNumber,
                            call.contactName
                        )
                        if (recordingPath != null) {
                            updates[call.compositeId] = recordingPath
                        }
                    }
                }

                // Batch update in DB
                if (updates.isNotEmpty()) {
                    Log.d(TAG, "Found ${updates.size} recordings to link. applying batch updates...")
                    callDataDao.updateRecordingPaths(updates)
                }
            }
            
            // Update person data in batch
            updatePersonsData(personCallsMap)
            
            // CRITICAL: Delete any calls from Room that are now before the filter date
            // This handles cases where user changed the start date to a later date
            Log.d(TAG, "Cleaning up calls before $filterDate...")
            callDataDao.deleteBefore(filterDate)
            
            val finalCount = callDataDao.getAllCompositeIds().size
            Log.d(TAG, "Sync complete. New calls: ${newCalls.size}. Remaining in Room: $finalCount")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
        } finally {
            ProcessMonitor.endProcess()
        }
    }
        
    
    /**
     * Update person data based on their calls
     */
    private suspend fun updatePersonsData(personCalls: Map<String, List<CallDataEntity>>) {
        if (personCalls.isEmpty()) return
        
        // Fetch all existing persons for these numbers in one go
        val phoneNumbers = personCalls.keys.toList()
        val existingPersons = personDataDao.getByPhoneNumbers(phoneNumbers).associateBy { it.phoneNumber }
        
        val personsToUpdate = mutableListOf<PersonDataEntity>()
        
        for ((phoneNumber, calls) in personCalls) {
            val sortedCalls = calls.sortedByDescending { it.callDate }
            val lastCall = sortedCalls.firstOrNull() ?: continue
            
            val existing = existingPersons[phoneNumber]
            
            val totalIncoming = calls.count { it.callType == CallLog.Calls.INCOMING_TYPE }
            val totalOutgoing = calls.count { it.callType == CallLog.Calls.OUTGOING_TYPE }
            val totalMissed = calls.count { it.callType == CallLog.Calls.MISSED_TYPE }
            val totalDuration = calls.sumOf { it.duration }
            
            val personEntity = PersonDataEntity(
                phoneNumber = phoneNumber,
                contactName = lastCall.contactName ?: existing?.contactName,
                photoUri = lastCall.photoUri ?: existing?.photoUri,
                personNote = existing?.personNote,  // Preserve existing note
                label = existing?.label, // Preserve existing label
                lastCallType = lastCall.callType,
                lastCallDuration = lastCall.duration,
                lastCallDate = lastCall.callDate,
                lastRecordingPath = lastCall.localRecordingPath,
                lastCallCompositeId = lastCall.compositeId,
                totalCalls = calls.size,
                totalIncoming = totalIncoming,
                totalOutgoing = totalOutgoing,
                totalMissed = totalMissed,
                totalDuration = totalDuration,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isExcluded = existing?.isExcluded ?: false,
                needsSync = existing?.needsSync ?: false
            )
            
            personsToUpdate.add(personEntity)
        }
        
        if (personsToUpdate.isNotEmpty()) {
            personDataDao.insertAll(personsToUpdate)
            Log.d(TAG, "Updated ${personsToUpdate.size} persons")
        }
    }
    
    /**
     * Fetch calls from Android system call log, filtered by SIM selection
     */
    private fun fetchSystemCallLog(startDate: Long, deviceId: String, simSelection: String): List<CallDataEntity> {
        val calls = mutableListOf<CallDataEntity>()
        
        // Get SIM IDs from settings to match subscription IDs
        val sim1SubId = settingsRepository.getSim1SubscriptionId()
        val sim2SubId = settingsRepository.getSim2SubscriptionId()
        
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_PHOTO_URI,
            "subscription_id"
        )
        
        val selection = "${CallLog.Calls.DATE} >= ?"
        val selectionArgs = arrayOf(startDate.toString())
        val sortOrder = "${CallLog.Calls.DATE} DESC"
        
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            
            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                val photoIdx = it.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)
                val subIdIdx = it.getColumnIndex("subscription_id")
                
                while (it.moveToNext()) {
                    val systemId = it.getString(idIdx) ?: continue
                    val rawNumber = it.getString(numberIdx) ?: "Unknown"
                    val number = normalizePhoneNumber(rawNumber)
                    val name = it.getString(nameIdx)
                    val type = it.getInt(typeIdx)
                    val date = it.getLong(dateIdx)
                    val duration = it.getLong(durationIdx)
                    val photoUri = it.getString(photoIdx)
                    val subId = if (subIdIdx != -1) it.getInt(subIdIdx) else null
                    
                    // Filter by SIM selection
                    val shouldInclude = when (simSelection) {
                        "Sim1" -> subId == sim1SubId
                        "Sim2" -> subId == sim2SubId
                        "Both" -> true  // Include all
                        else -> false  // "Off" - exclude all
                    }
                    
                    if (!shouldInclude) {
                        continue  // Skip this call
                    }
                    
                    val compositeId = generateCompositeId(type, deviceId, number, date)
                    
                    calls.add(CallDataEntity(
                        compositeId = compositeId,
                        systemId = systemId,
                        phoneNumber = number,
                        contactName = name,
                        callType = type,
                        callDate = date,
                        duration = duration,
                        photoUri = photoUri,
                        subscriptionId = subId,
                        deviceId = deviceId,
                        recordingSyncStatus = if (duration > 0) RecordingSyncStatus.PENDING else RecordingSyncStatus.NOT_APPLICABLE
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching system call log", e)
        }
        
        return calls
    }
    
    /**
     * Generate composite ID for a call
     */
    private fun generateCompositeId(type: Int, deviceId: String, number: String, date: Long): String {
        val cleanNumber = number.filter { it.isDigit() || it == '+' }
        val cleanDevice = deviceId.ifEmpty { "unknown_dev" }
        
        val typeStr = when (type) {
            CallLog.Calls.INCOMING_TYPE -> "incoming"
            CallLog.Calls.OUTGOING_TYPE -> "outgoing"
            CallLog.Calls.MISSED_TYPE -> "missed"
            5 -> "rejected" // CallLog.Calls.REJECTED_TYPE
            6 -> "blocked"  // CallLog.Calls.BLOCKED_TYPE
            else -> "unknown"
        }
        
        return "$typeStr-$cleanDevice-$cleanNumber-$date"
    }
    
    /**
     * Normalize phone number for consistent person lookup
     */
    fun normalizePhoneNumber(number: String): String {
        val strip = number.filter { it.isDigit() || it == '+' }
        
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            var countryIso = tm?.networkCountryIso
            
            if (countryIso.isNullOrEmpty()) {
                countryIso = java.util.Locale.getDefault().country
            }
            
            if (!countryIso.isNullOrEmpty()) {
                val formatted = android.telephony.PhoneNumberUtils.formatNumberToE164(strip, countryIso.uppercase())
                if (formatted != null) {
                    return formatted
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to normalize number $number: ${e.message}")
        }
        
        return strip
    }
    
    // ============================================
    // CLEAR DATA
    // ============================================
    
    /**
     * Clear all sync status (reset all calls to PENDING for full re-sync)
     * This resets syncStatus, metadataSyncStatus, and recordingSyncStatus
     */
    suspend fun clearSyncStatus() = withContext(Dispatchers.IO) {
        val allCalls = callDataDao.getAllCalls()
        for (call in allCalls) {
            // Reset general sync status
            callDataDao.updateSyncStatus(call.compositeId, CallLogStatus.PENDING)
            // Reset metadata sync status
            callDataDao.updateMetadataSyncStatus(call.compositeId, MetadataSyncStatus.PENDING)
            // Reset recording sync status (only if call had a recording)
            if (call.duration > 0) {
                callDataDao.updateRecordingSyncStatus(call.compositeId, RecordingSyncStatus.PENDING)
            }
            // Clear any sync errors
            callDataDao.updateSyncError(call.compositeId, null)
        }
        Log.d(TAG, "Reset sync status for ${allCalls.size} calls")
    }
    
    /**
     * Delete all data (for debugging/reset)
     */
    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        callDataDao.deleteAll()
        personDataDao.deleteAll()
    }

    /**
     * Import data from a backup.
     */
    suspend fun importData(calls: List<CallDataEntity>, persons: List<PersonDataEntity>) = withContext(Dispatchers.IO) {
        database.withTransaction {
            callDataDao.insertAll(calls)
            personDataDao.insertAll(persons)
        }
        Log.d(TAG, "Imported ${calls.size} calls and ${persons.size} persons from backup")
    }
}
