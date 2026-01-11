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
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    val excludeFromSync: Boolean? = null,
    val excludeFromList: Boolean? = null,
    val serverUpdatedAt: Long
)

class CallDataRepository private constructor(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val callDataDao = database.callDataDao()
    private val personDataDao = database.personDataDao()
    private val recordingRepository = RecordingRepository.getInstance(context)
    private val settingsRepository = SettingsRepository.getInstance(context)
    
    private val syncMutex = Mutex()
    
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
     * Get all calls since a specific date as Flow
     */
    fun getCallsSinceFlow(minDate: Long): Flow<List<CallDataEntity>> = callDataDao.getCallsSinceFlow(minDate)
    suspend fun getCallsSince(minDate: Long): List<CallDataEntity> = callDataDao.getCallsSince(minDate)

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
     * Get all calls for a specific number.
     */
    suspend fun getLogsForPhone(phoneNumber: String): List<CallDataEntity> = withContext(Dispatchers.IO) {
        callDataDao.getCallsForNumber(phoneNumber)
    }
    
    /**
     * Get calls missing local recording paths (to trigger a find)
     */
    suspend fun getCallsMissingRecordings(): List<CallDataEntity> = withContext(Dispatchers.IO) {
        callDataDao.getCallsMissingRecordings()
    }
    
    /**
     * Get count of items pending sync as a flow.
     */
    fun getSyncQueueCountFlow(): Flow<Int> {
        return combine(
            callDataDao.getPendingSyncCountFlow(),
            personDataDao.getPendingSyncCountFlow()
        ) { callCount, personCount ->
            callCount + personCount
        }
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
        if (path != null) {
            callDataDao.updateRecordingSyncStatus(compositeId, RecordingSyncStatus.PENDING)
        }
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

    suspend fun updateRecordingSyncStatusBatch(compositeIds: List<String>, status: RecordingSyncStatus) = withContext(Dispatchers.IO) {
        if (compositeIds.isEmpty()) return@withContext
        callDataDao.updateRecordingSyncStatusBatch(compositeIds, status)
        
        // If recording is NOT_FOUND, we want to push this status update to the server metadata
        if (status == RecordingSyncStatus.NOT_FOUND) {
            database.withTransaction {
                compositeIds.forEach { id ->
                    callDataDao.updateMetadataSyncStatus(id, MetadataSyncStatus.UPDATE_PENDING)
                }
            }
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

    suspend fun markMetadataSyncedBatch(compositeIds: List<String>, serverTime: Long) = withContext(Dispatchers.IO) {
        if (compositeIds.isEmpty()) return@withContext
        callDataDao.markMetadataSyncedBatch(compositeIds, serverTime)
    }

    /**
     * Update metadata received status
     */
    suspend fun updateMetadataReceived(compositeId: String, received: Boolean) = withContext(Dispatchers.IO) {
        callDataDao.updateMetadataReceived(compositeId, received)
    }

    /**
     * Update server recording status
     */
    suspend fun updateServerRecordingStatus(compositeId: String, status: String?) = withContext(Dispatchers.IO) {
        callDataDao.updateServerRecordingStatus(compositeId, status)
    }

    /**
     * Update processing status
     */
    suspend fun updateProcessingStatus(compositeId: String, status: String?) = withContext(Dispatchers.IO) {
        callDataDao.updateProcessingStatus(compositeId, status)
    }

    suspend fun clearStaleProcessingStatuses(timeoutMillis: Long = 2 * 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - timeoutMillis
        callDataDao.clearStaleProcessingStatuses(threshold)
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

    suspend fun updateSyncErrorBatch(compositeIds: List<String>, error: String?) = withContext(Dispatchers.IO) {
        if (compositeIds.isEmpty()) return@withContext
        callDataDao.updateSyncErrorBatch(compositeIds, error)
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
                    
                    // Update exclusion if provided
                    if (update.excludeFromSync != null && update.excludeFromList != null) {
                        personDataDao.updateExclusionType(normalized, update.excludeFromSync, update.excludeFromList)
                        @Suppress("DEPRECATION")
                        personDataDao.updateExclusion(normalized, update.excludeFromSync && update.excludeFromList)
                    }
                } else {
                    val excludeSync = update.excludeFromSync ?: false
                    val excludeList = update.excludeFromList ?: false
                    
                    personDataDao.insert(PersonDataEntity(
                        phoneNumber = normalized,
                        contactName = update.name,
                        personNote = update.personNote,
                        label = update.label,
                        excludeFromSync = excludeSync,
                        excludeFromList = excludeList,
                        isExcluded = excludeSync && excludeList,
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
     * Get persons since a specific date as Flow
     */
    fun getPersonsSinceFlow(minDate: Long): Flow<List<PersonDataEntity>> = personDataDao.getPersonsSinceFlow(minDate)

    /**
     * Get ALL persons including excluded since a specific date as Flow
     */
    fun getAllPersonsIncludingExcludedFlow(minDate: Long): Flow<List<PersonDataEntity>> = personDataDao.getAllPersonsIncludingExcludedFlow(minDate)
    suspend fun getAllPersonsIncludingExcluded(minDate: Long): List<PersonDataEntity> = personDataDao.getAllPersonsIncludingExcluded(minDate)

    /**
     * Get ALL persons including excluded (for ViewModel filtering with Ignored tab)
     */
    fun getAllPersonsIncludingExcludedFlow(): Flow<List<PersonDataEntity>> = personDataDao.getAllPersonsIncludingExcludedFlow()
    
    /**
     * Get excluded persons as Flow
     */
    fun getExcludedPersonsFlow(): Flow<List<PersonDataEntity>> = personDataDao.getExcludedPersonsFlow()
    
    /**
     * Get "No Tracking" persons (excluded from both sync and list)
     */
    fun getNoTrackingPersonsFlow(): Flow<List<PersonDataEntity>> = personDataDao.getNoTrackingPersonsFlow()
    
    /**
     * Get "Excluded from lists" only persons (hidden from UI but still tracked)
     */
    fun getExcludedFromListOnlyPersonsFlow(): Flow<List<PersonDataEntity>> = personDataDao.getExcludedFromListOnlyPersonsFlow()
    
    /**
     * Check if a number should be excluded from sync/tracking
     */
    suspend fun isExcludedFromSync(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        personDataDao.isExcludedFromSync(normalized) ?: false
    }
    
    /**
     * Get all persons (one-time fetch)
     */
    suspend fun getAllPersons(): List<PersonDataEntity> = withContext(Dispatchers.IO) {
        personDataDao.getAllPersons()
    }
    
    /**
     * Get person by phone number with robust fallback for normalization differences
     */
    suspend fun getPersonByNumber(phoneNumber: String): PersonDataEntity? = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        getPersonRobust(normalized)
    }

    /**
     * Robust lookup helper for person data
     */
    private suspend fun getPersonRobust(normalized: String): PersonDataEntity? {
        // 0. Primary match
        personDataDao.getByPhoneNumber(normalized)?.let { return it }

        // 1. Try common normalization variants
        val variants = mutableListOf<String>()
        if (normalized.startsWith("+")) {
            variants.add(normalized.substring(1))
        } else {
            variants.add("+$normalized")
        }
        
        for (v in variants) {
            personDataDao.getByPhoneNumber(v)?.let { return it }
        }

        // 2. Suffix match (last 10 digits) - prevents creating duplicates for numbers with different country codes
        if (normalized.length >= 10) {
            val suffix = normalized.takeLast(10)
            // Use optimized DB query instead of getAllPersons().find
            val candidates = personDataDao.getByPhoneNumberSuffix(suffix)
            if (candidates.isNotEmpty()) {
                return candidates.first()
            }
        }

        return null
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
        val existing = getPersonRobust(normalized)
        if (existing != null) {
            personDataDao.updatePersonNote(existing.phoneNumber, note)
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
        val existing = getPersonRobust(normalized)
        if (existing != null) {
            personDataDao.updateLabel(existing.phoneNumber, label)
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
        val existing = getPersonRobust(normalized)
        if (existing != null) {
            personDataDao.updateName(existing.phoneNumber, name)
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
                    if (!update.name.isNullOrBlank()) personDataDao.updateNameFromRemote(key, update.name)
                    if (update.personNote != null) personDataDao.updatePersonNoteFromRemote(key, update.personNote)
                    if (update.label != null) personDataDao.updateLabelFromRemote(key, update.label)
                } else {
                    Log.d(TAG, "Inserting new person entry for $normalized")
                    personDataDao.insert(PersonDataEntity(
                        phoneNumber = normalized,
                        contactName = update.name?.takeIf { it.isNotBlank() },
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
     * Update exclusion status for a phone number (legacy - sets both types for "No Tracking")
     */
    @Suppress("DEPRECATION")
    suspend fun updateExclusion(phoneNumber: String, isExcluded: Boolean) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        val existing = personDataDao.getByPhoneNumber(normalized)
        if (existing != null) {
            // Update both legacy and new columns
            personDataDao.updateExclusion(normalized, isExcluded)
            personDataDao.updateExclusionType(normalized, isExcluded, isExcluded) // "No Tracking" = both true
        } else {
            // Create person entry if doesn't exist
            personDataDao.insert(PersonDataEntity(
                phoneNumber = normalized,
                isExcluded = isExcluded,
                excludeFromSync = isExcluded,
                excludeFromList = isExcluded
            ))
        }
    }
    
    /**
     * Update exclusion type for a phone number (granular control)
     * @param excludeFromSync - true to stop tracking/syncing calls for this number ("No Tracking")
     * @param excludeFromList - true to hide from call lists UI ("Excluded from lists")
     */
    @Suppress("DEPRECATION")
    suspend fun updateExclusionType(phoneNumber: String, excludeFromSync: Boolean, excludeFromList: Boolean) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        val existing = personDataDao.getByPhoneNumber(normalized)
        if (existing != null) {
            personDataDao.updateExclusionType(normalized, excludeFromSync, excludeFromList)
            // Also update legacy column for backward compatibility
            personDataDao.updateExclusion(normalized, excludeFromSync && excludeFromList)
        } else {
            // Create person entry if doesn't exist
            personDataDao.insert(PersonDataEntity(
                phoneNumber = normalized,
                isExcluded = excludeFromSync && excludeFromList,
                excludeFromSync = excludeFromSync,
                excludeFromList = excludeFromList
            ))
        }
    }
    
    /**
     * Remove exclusion (clear both exclusion types)
     */
    suspend fun removeExclusion(phoneNumber: String) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        val existing = personDataDao.getByPhoneNumber(normalized)
        if (existing != null) {
            personDataDao.updateExclusionType(normalized, false, false)
            @Suppress("DEPRECATION")
            personDataDao.updateExclusion(normalized, false)
        }
    }
    
    // ============================================
    // SYNC WITH SYSTEM CALL LOG
    // ============================================
    
    private fun getLatestSystemCallDate(): Long {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
             return 0L
        }
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.DATE),
                 null, null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                    if (dateIdx != -1) return it.getLong(dateIdx)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query latest system call date", e)
        }
        return 0L
    }

    private fun getOldestSystemCallDate(): Long {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
             return Long.MAX_VALUE
        }
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.DATE),
                 null, null,
                "${CallLog.Calls.DATE} ASC LIMIT 1"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                    if (dateIdx != -1) return it.getLong(dateIdx)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query oldest system call date", e)
        }
        return Long.MAX_VALUE
    }

    /**
     * Sync new calls from system call log to Room database.
     * This runs in background and updates Room with any new calls.
     * Also finds and updates recording paths.
     */
    suspend fun syncFromSystemCallLog() = withContext(Dispatchers.IO) {
        if (syncMutex.isLocked) {
            Log.d(TAG, "Sync already in progress, skipping this request.")
            return@withContext
        }
        
        syncMutex.withLock {
            try {
            // 1. Initial Checks (Permissions, Settings)
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return@withContext
            }

            val simSelection = settingsRepository.getSimSelection()
            if (simSelection == "Off") {
                Log.d(TAG, "Call tracking is OFF, skipping sync")
                return@withContext
            }

            val filterDate = settingsRepository.getTrackStartDate()
            
            // 2. Local Data State
            val latestCallDate = callDataDao.getMaxCallDate() ?: 0L
            val minCallDate = callDataDao.getMinCallDate() ?: Long.MAX_VALUE
            
            // 3. SMART IMPORT CHECK 🧠
            // We skip the full scan if:
            // a) The filter date hasn't moved backwards (we don't need older calls)
            // b) OR if we already have the oldest possible calls from the system log (for "All" users)
            // AND the system has no calls newer than our latest call.
            
            val isAtOldestEnd = if (filterDate == 0L) {
                if (minCallDate == Long.MAX_VALUE) false // Empty DB
                else getOldestSystemCallDate() >= minCallDate // We have reached the start of system log
            } else {
                filterDate >= minCallDate // Cutoff is within or after our current range
            }

            if (isAtOldestEnd) {
                 val systemLatest = getLatestSystemCallDate()
                 // Allow tiny buffer (1s) for timestamp differences
                 if (systemLatest <= latestCallDate + 1000) {
                      Log.d(TAG, "Smart Sync: Database up-to-date. Skipping full scan.")
                      
                      // Even if we skip the call log scan, we should still check if any PENDING recordings 
                      // can be found now (e.g. if permissions were just granted)
                      syncRecordingsOnly()
                      return@withContext
                 }
            }

            ProcessMonitor.startProcess(ProcessMonitor.ProcessIds.IMPORT_CALL_LOG, "Checking for new calls...", isIndeterminate = true)
            Log.d(TAG, "Starting sync from system call log...")
            
            val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)

            // Optimize: Only fetch calls since the latest call in our DB (with a 2-day safety buffer)
            // unless the filter date has been moved earlier than our current data.
            val fetchStartDate = if (filterDate < minCallDate) {
                // Window expanded backwards - fetch from new start date
                filterDate
            } else {
                // Window is within current data range or shrinking - only fetch new calls
                maxOf(filterDate, latestCallDate - (2 * 24 * 60 * 60 * 1000L))
            }
            
            // Get existing IDs once (only need those since fetchStartDate)
            val existingIds = callDataDao.getCompositeIdsSince(fetchStartDate).toSet()
            val existingSystemIds = callDataDao.getSystemIdsSince(fetchStartDate).toSet()
            Log.d(TAG, "Existing calls since ${java.util.Date(fetchStartDate)}: ${existingIds.size} (System IDs: ${existingSystemIds.size})")
            
            // Fetch from system call log with SIM filter  
            val systemCalls = fetchSystemCallLog(fetchStartDate, deviceId, simSelection)
            Log.d(TAG, "System call log returned: ${systemCalls.size} calls (SIM: $simSelection)")

            val newCalls = mutableListOf<CallDataEntity>()
            
            // Track persons to update
            val personCallsMap = mutableMapOf<String, MutableList<CallDataEntity>>()
            
            for (call in systemCalls) {
                val normalized = normalizePhoneNumber(call.phoneNumber)
                
                // Collect new calls for batch insert
                if (!existingIds.contains(call.compositeId) && !existingSystemIds.contains(call.systemId)) {
                    newCalls.add(call)
                }
                
                // Track for person update (all calls from system log should be considered)
                personCallsMap.getOrPut(normalized) { mutableListOf() }.add(call)
            }
            
            // Batch insert new calls
            if (newCalls.isNotEmpty()) {
                ProcessMonitor.updateProgress(ProcessMonitor.ProcessIds.IMPORT_CALL_LOG, 0.6f, "Found ${newCalls.size} new call${if (newCalls.size == 1) "" else "s"}")
                callDataDao.insertAll(newCalls)
                Log.d(TAG, "Inserted ${newCalls.size} new calls")
            } else {
                ProcessMonitor.updateProgress(ProcessMonitor.ProcessIds.IMPORT_CALL_LOG, 0.6f, "All calls up to date")
                Log.d(TAG, "No new calls to insert")
            }
            
            // CRITICAL UI PERFORMANCE: Update person data immediately after calls are inserted
            // This ensures the "Grouped by Phone" view appears quickly, without waiting 
            // for the potentially slow recording matching process below.
            updatePersonsData(personCallsMap)
            
            // CRITICAL: Delete any calls from Room that are now before the filter date
            // This handles cases where user changed the start date to a later date
            Log.d(TAG, "Cleaning up calls before $filterDate...")
            callDataDao.deleteBefore(filterDate)

            // 4. Update recordings for calls that don't have them yet (ONLY if feature enabled & permitted)
            syncRecordingsOnly()
            
            val finalCount = callDataDao.getAllCompositeIds().size
            Log.d(TAG, "Sync complete. New calls: ${newCalls.size}. Remaining in Room: $finalCount")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
        } finally {
            ProcessMonitor.endProcess(ProcessMonitor.ProcessIds.IMPORT_CALL_LOG)
            ProcessMonitor.endProcess(ProcessMonitor.ProcessIds.FIND_RECORDINGS)
        }
    }
}

    /**
     * Finds and links local recording files to pending calls.
     * Uses optimized batch processing for large call sets.
     */
    private suspend fun syncRecordingsOnly() = withContext(Dispatchers.IO) {
        val isRecordingEnabled = settingsRepository.isCallRecordEnabled()
        val storagePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val hasStoragePermission = androidx.core.content.ContextCompat.checkSelfPermission(context, storagePermission) == 
                                   android.content.pm.PackageManager.PERMISSION_GRANTED

        val unsyncedWithNoPath = if (isRecordingEnabled && hasStoragePermission) {
            callDataDao.getCallsMissingRecordings().filter { it.localRecordingPath == null && it.duration > 0 }
        } else {
            emptyList()
        }
        
        if (unsyncedWithNoPath.isEmpty()) return@withContext
        
        val total = unsyncedWithNoPath.size
        Log.d(TAG, "Starting recording sync for $total calls")
        
        ProcessMonitor.startProcess(ProcessMonitor.ProcessIds.FIND_RECORDINGS, "Finding Recordings", isIndeterminate = false)
        
        try {
            // Use batch processing for large sets (>10 calls), sequential for small sets
            if (total > 10) {
                syncRecordingsBatch(unsyncedWithNoPath)
            } else {
                syncRecordingsSequential(unsyncedWithNoPath)
            }
        } finally {
            // Give a small delay for UI to show completion before hiding
            kotlinx.coroutines.delay(800)
            ProcessMonitor.endProcess(ProcessMonitor.ProcessIds.FIND_RECORDINGS)
            
            // Clear batch cache to free memory
            recordingRepository.clearBatchCache()
        }
    }
    
    /**
     * Batch processing mode for recording sync (optimized for 10+ calls)
     * Uses parallel processing, phone index, and duration caching.
     */
    private suspend fun syncRecordingsBatch(calls: List<CallDataEntity>) {
        val total = calls.size
        Log.d(TAG, "Using BATCH mode for $total calls")
        
        // Convert to CallInfo for batch processing
        val callInfos = calls.map { call ->
            RecordingRepository.CallInfo(
                compositeId = call.compositeId,
                callDate = call.callDate,
                durationSec = call.duration,
                phoneNumber = call.phoneNumber,
                contactName = call.contactName
            )
        }
        
        // Process in batch with progress callback
        val results = recordingRepository.findRecordingsBatch(callInfos) { current, totalCount, found ->
            val progress = current.toFloat() / totalCount
            ProcessMonitor.updateProgress(
                ProcessMonitor.ProcessIds.FIND_RECORDINGS,
                progress,
                "Scanning $current/$totalCount ($found matched)"
            )
        }
        
        // Apply results in batch
        val foundUpdates = mutableMapOf<String, String>()
        val notFoundIds = mutableListOf<String>()
        
        for (result in results) {
            if (result.recordingPath != null) {
                foundUpdates[result.compositeId] = result.recordingPath
            } else {
                notFoundIds.add(result.compositeId)
            }
            
            // Incremental save every 50 items
            if (foundUpdates.size >= 50) {
                callDataDao.updateRecordingPaths(foundUpdates)
                foundUpdates.clear()
            }
            if (notFoundIds.size >= 50) {
                callDataDao.updateRecordingSyncStatusBatch(notFoundIds, RecordingSyncStatus.NOT_FOUND)
                notFoundIds.clear()
            }
        }
        
        // Apply remaining
        if (foundUpdates.isNotEmpty()) {
            callDataDao.updateRecordingPaths(foundUpdates)
        }
        if (notFoundIds.isNotEmpty()) {
            Log.d(TAG, "Marking ${notFoundIds.size} recordings as NOT_FOUND")
            callDataDao.updateRecordingSyncStatusBatch(notFoundIds, RecordingSyncStatus.NOT_FOUND)
        }
        
        val totalFound = results.count { it.recordingPath != null }
        Log.d(TAG, "Batch sync complete: $totalFound/${results.size} recordings found")
    }
    
    /**
     * Sequential processing mode for recording sync (for small batches <10 calls)
     */
    private suspend fun syncRecordingsSequential(calls: List<CallDataEntity>) {
        val total = calls.size
        Log.d(TAG, "Using SEQUENTIAL mode for $total calls")
        
        val foundUpdates = mutableMapOf<String, String>()
        val notFoundIds = mutableListOf<String>()
        var totalFound = 0
        
        calls.forEachIndexed { index, call ->
            if (!kotlinx.coroutines.currentCoroutineContext().isActive) throw kotlinx.coroutines.CancellationException()
            
            // Update progress
            val updateFrequency = if (total < 20) 1 else 10
            if (index % updateFrequency == 0 || index == total - 1) {
                val progress = (index + 1).toFloat() / total
                val currentFound = totalFound + foundUpdates.size
                ProcessMonitor.updateProgress(
                    ProcessMonitor.ProcessIds.FIND_RECORDINGS, 
                    progress, 
                    "Scanning ${index + 1}/$total ($currentFound matched)"
                )
            }

            val recordingPath = recordingRepository.findRecording(
                call.callDate,
                call.duration,
                call.phoneNumber,
                call.contactName
            )
            if (recordingPath != null) {
                foundUpdates[call.compositeId] = recordingPath
            } else {
                notFoundIds.add(call.compositeId)
            }
            
            // INCREMENTAL SAVE
            if (foundUpdates.size >= 10) {
                callDataDao.updateRecordingPaths(foundUpdates)
                totalFound += foundUpdates.size
                foundUpdates.clear()
            }
            if (notFoundIds.size >= 10) {
                callDataDao.updateRecordingSyncStatusBatch(notFoundIds, RecordingSyncStatus.NOT_FOUND)
                notFoundIds.clear()
            }
        }

        // Apply remaining updates
        if (foundUpdates.isNotEmpty()) {
            callDataDao.updateRecordingPaths(foundUpdates)
        }
        if (notFoundIds.isNotEmpty()) {
            Log.d(TAG, "Marking ${notFoundIds.size} recordings as NOT_FOUND")
            callDataDao.updateRecordingSyncStatusBatch(notFoundIds, RecordingSyncStatus.NOT_FOUND)
        }
    }
        
    
    /**
     * Update person data based on their calls
     */
    private suspend fun updatePersonsData(personCalls: Map<String, List<CallDataEntity>>) {
        if (personCalls.isEmpty()) return
        
        // Fetch all existing persons for these numbers once
        val phoneNumbers = personCalls.keys.toList()
        val existingPersons = personDataDao.getByPhoneNumbers(phoneNumbers).associateBy { it.phoneNumber }
        
        // NEW: Fetch accurate stats from the DB for all these numbers to avoid double-counting bug
        val accurateStats = callDataDao.getCallStatsForNumbers(phoneNumbers).associateBy { it.phoneNumber }
        
        val personsToUpdate = mutableListOf<PersonDataEntity>()
        
        for ((phoneNumber, calls) in personCalls) {
            val sortedCalls = calls.sortedByDescending { it.callDate }
            val incomingLatest = sortedCalls.firstOrNull() ?: continue
            
            // Robust lookup to match existing person data
            val existing = existingPersons[phoneNumber] ?: getPersonRobust(phoneNumber)
            val stats = accurateStats[phoneNumber]
            
            // Only update "Last Call" info if the latest call in this batch is newer than existing
            val isNewer = incomingLatest.callDate > (existing?.lastCallDate ?: 0L)
            
            val personEntity = PersonDataEntity(
                phoneNumber = phoneNumber,
                contactName = if (isNewer) {
                    incomingLatest.contactName?.takeIf { it.isNotBlank() } ?: existing?.contactName?.takeIf { it.isNotBlank() }
                } else {
                    existing?.contactName?.takeIf { it.isNotBlank() } ?: incomingLatest.contactName?.takeIf { it.isNotBlank() }
                },
                photoUri = if (isNewer) (incomingLatest.photoUri ?: existing?.photoUri) else existing?.photoUri,
                personNote = existing?.personNote,  // Preserve existing note
                label = existing?.label, // Preserve existing label
                lastCallType = if (isNewer) incomingLatest.callType else (existing?.lastCallType ?: incomingLatest.callType),
                lastCallDuration = if (isNewer) incomingLatest.duration else (existing?.lastCallDuration ?: incomingLatest.duration),
                lastCallDate = if (isNewer) incomingLatest.callDate else (existing?.lastCallDate ?: incomingLatest.callDate),
                lastRecordingPath = if (isNewer) incomingLatest.localRecordingPath else (existing?.lastRecordingPath ?: incomingLatest.localRecordingPath),
                lastCallCompositeId = if (isNewer) incomingLatest.compositeId else (existing?.lastCallCompositeId ?: incomingLatest.compositeId),
                
                // Use accurate stats from DB
                totalCalls = stats?.totalCalls ?: (existing?.totalCalls ?: 0),
                totalIncoming = stats?.totalIncoming ?: (existing?.totalIncoming ?: 0),
                totalOutgoing = stats?.totalOutgoing ?: (existing?.totalOutgoing ?: 0),
                totalMissed = stats?.totalMissed ?: (existing?.totalMissed ?: 0),
                totalDuration = stats?.totalDuration ?: (existing?.totalDuration ?: 0L),
                
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isExcluded = existing?.isExcluded ?: false,
                excludeFromSync = existing?.excludeFromSync ?: false,
                excludeFromList = existing?.excludeFromList ?: false,
                needsSync = existing?.needsSync ?: (existing == null) // If new person, mark for first sync
            )
            personsToUpdate.add(personEntity)
        }
        
        if (personsToUpdate.isNotEmpty()) {
            personDataDao.insertAll(personsToUpdate)
            Log.d(TAG, "Updated ${personsToUpdate.size} persons with fresh stats")
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
        
        val baseProjection = listOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_PHOTO_URI
        )
        
        // Potential SIM identity columns across different manufacturers
        val potentialSimColumns = listOf("subscription_id", "sim_id", "sim_slot", "phone_id", "sub_id")
        
        // DYNAMIC DETECTION: Find which SIM columns are actually supported by this device
        // We query for 0 rows with null projection to get the column list safely
        val supportedSimColumns = mutableListOf<String>()
        try {
            context.contentResolver.query(CallLog.Calls.CONTENT_URI, null, "${CallLog.Calls._ID} = -1", null, null)?.use {
                val allColumnNames = it.columnNames.toSet()
                potentialSimColumns.forEach { col ->
                    if (allColumnNames.contains(col)) {
                        supportedSimColumns.add(col)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CallDataRepository", "Error detecting SIM columns", e)
        }

        val projection = (baseProjection + supportedSimColumns).toTypedArray()
        
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
                
                // Identify which SIM column is actually present in the device's call log
                val subIdIdx = supportedSimColumns.map { col -> it.getColumnIndex(col) }.find { idx -> idx != -1 } ?: -1
                
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
                    // Fallback: If no subId column is found, we assume it's the intended SIM if 
                    // only one SIM is configured or "Both" is selected.
                    val shouldInclude = when (simSelection) {
                        "Sim1" -> subId == null || subId == sim1SubId || subId == -1
                        "Sim2" -> subId == null || subId == sim2SubId || subId == -1
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
                        contactName = name?.takeIf { it.isNotBlank() },
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
     * FULL RESET: Resets all local data statuses to PENDING for a complete fresh sync.
     * This implements "Forgot server, sync statuses of local data" troubleshooting.
     */
    suspend fun resetAllSyncStatuses() = withContext(Dispatchers.IO) {
        callDataDao.resetAllSyncStatuses()
        settingsRepository.setLastSyncTime(0L)
        Log.d(TAG, "Full reset of all sync statuses completed.")
    }

    /**
     * RESET METADATA ONLY: Resets only the metadata sync status.
     * This implements "Fetch meta data all" (by triggering a re-sync of all metadata).
     */
    suspend fun resetMetadataSync() = withContext(Dispatchers.IO) {
        callDataDao.resetMetadataSync()
        settingsRepository.setLastSyncTime(0L)
        Log.d(TAG, "Metadata sync reset completed.")
    }

    /**
     * RE-IMPORT FROM SYSTEM: Triggers a fresh scan of the system call log.
     */
    suspend fun reimportFromSystem() = withContext(Dispatchers.IO) {
        ProcessMonitor.startProcess(ProcessMonitor.ProcessIds.IMPORT_CALL_LOG, "Re-importing from system...", isIndeterminate = true)
        try {
            // Force import by ignoring smart sync markers if we had any (none currently, but we can bypass the time check)
            // We just call the existing sync method, but maybe we should clear IDs first?
            // User just wants to "Re import", so we'll run the sync.
            syncFromSystemCallLog()
        } finally {
            ProcessMonitor.endProcess(ProcessMonitor.ProcessIds.IMPORT_CALL_LOG)
        }
    }

    /**
     * RE-ATTACH RECORDINGS: Clears local recording paths and rescans the filesystem.
     */
    suspend fun reattachRecordings() = withContext(Dispatchers.IO) {
        val calls = callDataDao.getAllCalls()
        val updates = mutableMapOf<String, String?>()
        calls.forEach { updates[it.compositeId] = null }
        callDataDao.updateRecordingPaths(updates)
        
        // Now trigger the sync which includes finding recordings
        syncFromSystemCallLog()
        Log.d(TAG, "Re-attach recordings completed.")
    }

    /**
     * CONTINUE RUNNER: Looking for unsynced data with pending status and null processing status.
     * This implements "Looking for unsynced data pending status any while processing status is null".
     */
    suspend fun runContinueRunner(applicationContext: Context) = withContext(Dispatchers.IO) {
        // First cleanup any stale processing statuses
        clearStaleProcessingStatuses()
        
        val pending = callDataDao.getPendingCallsWithNoProcessing()
        if (pending.isNotEmpty()) {
            Log.d(TAG, "Continue Runner: Found ${pending.size} pending items needing processing. Triggering sync.")
            CallSyncWorker.runNow(applicationContext)
        }
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
