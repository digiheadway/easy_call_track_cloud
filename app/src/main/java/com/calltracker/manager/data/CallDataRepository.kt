package com.calltracker.manager.data

import android.content.Context
import android.provider.CallLog
import android.provider.Settings
import android.util.Log
import com.calltracker.manager.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Unified repository for all call and person data.
 * Room is the single source of truth.
 * System call log is only used to sync new calls.
 */
class CallDataRepository(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val callDataDao = database.callDataDao()
    private val personDataDao = database.personDataDao()
    private val recordingRepository = RecordingRepository(context)
    private val settingsRepository = SettingsRepository(context)
    
    companion object {
        private const val TAG = "CallDataRepository"
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
     * Get all unsynced calls (for upload worker)
     */
    suspend fun getUnsyncedCalls(): List<CallDataEntity> = withContext(Dispatchers.IO) {
        callDataDao.getUnsyncedCalls()
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
    suspend fun updateCallNote(compositeId: String, note: String?) = withContext(Dispatchers.IO) {
        callDataDao.updateCallNote(compositeId, note)
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
    
    // ============================================
    // PERSON DATA - WRITE OPERATIONS
    // ============================================
    
    /**
     * Update person note
     */
    suspend fun updatePersonNote(phoneNumber: String, note: String?) = withContext(Dispatchers.IO) {
        val normalized = normalizePhoneNumber(phoneNumber)
        val existing = personDataDao.getByPhoneNumber(normalized)
        if (existing != null) {
            personDataDao.updatePersonNote(normalized, note)
        } else {
            // Create person entry if doesn't exist
            personDataDao.insert(PersonDataEntity(
                phoneNumber = normalized,
                personNote = note
            ))
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
        Log.d(TAG, "Starting sync from system call log...")
        
        val filterDate = settingsRepository.getTrackStartDate()
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        
        // Get existing IDs once
        val existingIds = callDataDao.getAllCompositeIds().toSet()
        Log.d(TAG, "Existing calls in Room: ${existingIds.size}")
        
        // Fetch from system call log
        val systemCalls = fetchSystemCallLog(filterDate, deviceId)
        Log.d(TAG, "System call log returned: ${systemCalls.size} calls")
        
        val newCalls = mutableListOf<CallDataEntity>()
        val callsToUpdateRecording = mutableListOf<Pair<String, String>>()
        
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
        val unsyncedWithNoPath = callDataDao.getUnsyncedCalls().filter { it.localRecordingPath == null }
        if (unsyncedWithNoPath.isNotEmpty()) {
            val recordingFiles = recordingRepository.getRecordingFiles()
            for (call in unsyncedWithNoPath) {
                if (call.duration > 0) {
                    val recordingPath = recordingRepository.findRecordingInList(
                        recordingFiles,
                        call.callDate,
                        call.duration,
                        call.phoneNumber
                    )
                    if (recordingPath != null) {
                        callDataDao.updateRecordingPath(call.compositeId, recordingPath)
                        Log.d(TAG, "Found recording for ${call.compositeId}: $recordingPath")
                    }
                }
            }
        }
        
        // Update person data in batch
        updatePersonsData(personCallsMap)
        
        Log.d(TAG, "Sync complete. New calls: ${newCalls.size}")
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
                isExcluded = existing?.isExcluded ?: false
            )
            
            personsToUpdate.add(personEntity)
        }
        
        if (personsToUpdate.isNotEmpty()) {
            personDataDao.insertAll(personsToUpdate)
            Log.d(TAG, "Updated ${personsToUpdate.size} persons")
        }
    }
    
    /**
     * Fetch calls from Android system call log
     */
    private fun fetchSystemCallLog(startDate: Long, deviceId: String): List<CallDataEntity> {
        val calls = mutableListOf<CallDataEntity>()
        
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
                    val number = it.getString(numberIdx) ?: "Unknown"
                    val name = it.getString(nameIdx)
                    val type = it.getInt(typeIdx)
                    val date = it.getLong(dateIdx)
                    val duration = it.getLong(durationIdx)
                    val photoUri = it.getString(photoIdx)
                    val subId = if (subIdIdx != -1) it.getInt(subIdIdx) else null
                    
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
                        deviceId = deviceId
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
        return "$type-$cleanDevice-$cleanNumber-$date"
    }
    
    /**
     * Normalize phone number for consistent person lookup
     */
    private fun normalizePhoneNumber(number: String): String {
        return number.filter { it.isDigit() || it == '+' }
    }
    
    // ============================================
    // CLEAR DATA
    // ============================================
    
    /**
     * Clear all sync status (reset all calls to PENDING)
     */
    suspend fun clearSyncStatus() = withContext(Dispatchers.IO) {
        val allCalls = callDataDao.getAllCalls()
        for (call in allCalls) {
            callDataDao.updateSyncStatus(call.compositeId, CallLogStatus.PENDING)
        }
    }
    
    /**
     * Delete all data (for debugging/reset)
     */
    suspend fun deleteAllData() = withContext(Dispatchers.IO) {
        callDataDao.deleteAll()
        personDataDao.deleteAll()
    }
}
