package com.example.salescrm.data

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.SubscriptionManager
import android.telephony.SubscriptionInfo
import androidx.core.content.ContextCompat
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.salescrm.data.local.toEntity
import com.example.salescrm.data.local.SalesDao
import com.example.salescrm.data.local.ActivityEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * CallLogRepository - Fetches and processes call log data from device
 */
class CallLogRepository(private val context: Context) {
    
    /**
     * Sync system call logs with Room database
     */
    suspend fun syncWithDatabase(
        salesDao: SalesDao, 
        days: Int = 30,
        simSelection: SimSelection = SimSelection.BOTH,
        trackingStartDate: LocalDate? = null,
        autoSyncToActivities: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) return@withContext
        
        val fromDate = LocalDate.now().minusDays(days.toLong())
        val toDate = LocalDate.now()
        
        val entries = fetchCallLogInternal(
            fromDate = fromDate,
            toDate = toDate,
            callTypeFilter = null,
            simSelection = simSelection,
            trackingStartDate = trackingStartDate
        )
        
        if (entries.isNotEmpty()) {
            val entities = entries.map { it.toEntity() }
            salesDao.insertCallHistory(entities)
            
            // Auto-sync future call log to timeline if enabled
            if (autoSyncToActivities) {
                reconcileCallsWithActivities(salesDao, entries.take(100)) // Use a larger window to ensure all recent calls are caught
            }
        }
    }

    /**
     * Match recent call logs with CRM people and create activity entries if they don't exist
     */
    suspend fun reconcileCallsWithActivities(
        salesDao: SalesDao,
        recentCalls: List<CallLogEntry>
    ) = withContext(Dispatchers.IO) {
        val people = salesDao.getAllPeopleSync()
        if (people.isEmpty()) return@withContext
        
        val phoneToPersonMap = people.associateBy { normalizePhoneNumber(it.phone) }
        
        // Get existing activities for these people to avoid duplicates
        // We only care about SYSTEM call activities
        val recentActivities = salesDao.getAllActivitiesSync()
            .filter { it.type == ActivityType.SYSTEM.name && it.title?.contains("Call") == true }
            
        val existingKeys = recentActivities.map { "${it.personId}|${it.title}|${it.timestamp}" }.toSet()
        
        recentCalls.forEach { call ->
            val normalized = normalizePhoneNumber(call.phoneNumber)
            val person = phoneToPersonMap[normalized]
            
            if (person != null) {
                val title = "Call (${call.callType.label})"
                val key = "${person.id}|$title|${call.timestamp}"
                
                if (!existingKeys.contains(key)) {
                    val description = formatCallDescription(call)
                    salesDao.insertActivity(ActivityEntity(
                        id = 0, // Auto-generate
                        personId = person.id,
                        type = ActivityType.SYSTEM.name,
                        title = title,
                        description = description,
                        timestamp = call.timestamp,
                        recordingPath = call.recordingPath
                    ))
                    
                    // Update person's last activity stats
                    salesDao.updatePersonActivityStats(person.id, call.timestamp)
                }
            }
        }
    }
    
    /**
     * Check if call log permission is granted
     */
    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if contacts permission is granted
     */
    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasBasePermissions(): Boolean {
        return hasCallLogPermission() && hasContactsPermission()
    }

    fun hasRecordingPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(): Boolean {
        val hasPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        return hasBasePermissions() && hasPhoneState && hasRecordingPermission()
    }
    
    /**
     * Get list of active SIMs on the device
     */
    fun getActiveSims(): List<SubscriptionInfo> {
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun fetchCallLog(
        fromDate: LocalDate,
        toDate: LocalDate,
        callTypeFilter: CallType? = null,
        simSelection: SimSelection = SimSelection.BOTH,
        trackingStartDate: LocalDate? = null
    ): List<CallLogEntry> {
        return fetchCallLogInternal(fromDate, toDate, callTypeFilter, simSelection, trackingStartDate)
    }

    /**
     * Internal fetch logic
     */
    private fun fetchCallLogInternal(
        fromDate: LocalDate,
        toDate: LocalDate,
        callTypeFilter: CallType?,
        simSelection: SimSelection,
        trackingStartDate: LocalDate?
    ): List<CallLogEntry> {
        if (!hasCallLogPermission()) {
            return emptyList()
        }
        
        val entries = mutableListOf<CallLogEntry>()
        val resolver: ContentResolver = context.contentResolver
        
        // Convert dates to timestamps
        val effectiveFromDate = if (trackingStartDate != null && trackingStartDate.isAfter(fromDate)) trackingStartDate else fromDate
        val fromTimestamp = effectiveFromDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toTimestamp = toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        // Target sub IDs for SIM selection
        val targetSubIds = if (simSelection != SimSelection.BOTH) {
            val activeSims = getActiveSims()
            val simIndex = if (simSelection == SimSelection.SIM_1) 0 else 1
            val subId = activeSims.getOrNull(simIndex)?.subscriptionId?.toString()
            if (subId != null) setOf(subId) else null
        } else null

        // Build selection clause
        val selectionList = mutableListOf<String>()
        val selectionArgsList = mutableListOf<String>()
        
        selectionList.add("${CallLog.Calls.DATE} >= ?")
        selectionArgsList.add(fromTimestamp.toString())
        
        selectionList.add("${CallLog.Calls.DATE} < ?")
        selectionArgsList.add(toTimestamp.toString())
        
        val selection = selectionList.joinToString(" AND ")
        val selectionArgs = selectionArgsList.toTypedArray()
        
        // Query call log
        val projection = mutableListOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE,
            CallLog.Calls.IS_READ
        )
        
        // Add PHONE_ACCOUNT_ID if available
        projection.add(CallLog.Calls.PHONE_ACCOUNT_ID)

        val cursor: Cursor? = resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection.toTypedArray(),
            selection,
            selectionArgs,
            "${CallLog.Calls.DATE} DESC"
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndex(CallLog.Calls._ID)
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            val isReadIndex = it.getColumnIndex(CallLog.Calls.IS_READ)
            val subIdIndex = it.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
            
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val number = it.getString(numberIndex) ?: ""
                val name = it.getString(nameIndex)
                val type = it.getInt(typeIndex)
                val duration = it.getLong(durationIndex)
                val date = it.getLong(dateIndex)
                val isRead = it.getInt(isReadIndex) == 1
                val subId = it.getString(subIdIndex)
                
                // SIM Filtering
                if (targetSubIds != null && subId != null) {
                    // Match either direct subId or handle case where it's a stringly typed subId
                    if (!targetSubIds.contains(subId)) {
                        continue
                    }
                }

                val callType = mapCallType(type)
                
                // Apply call type filter
                if (callTypeFilter != null && callType != callTypeFilter) {
                    continue
                }
                
                val timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(date),
                    ZoneId.systemDefault()
                )
                
                entries.add(
                    CallLogEntry(
                        id = id,
                        phoneNumber = number,
                        normalizedNumber = normalizePhoneNumber(number),
                        contactName = name,
                        callType = callType,
                        duration = duration,
                        timestamp = timestamp,
                        isNew = !isRead && callType == CallType.MISSED,
                        subscriptionId = subId
                    )
                )
            }
        }
        
        return entries
    }
    
    /**
     * Find call recordings in the filesystem that match these call entries
     * Optimized with HashMap for O(n) lookups instead of O(n×m)
     */
    fun findRecordingsForCalls(entries: List<CallLogEntry>, customPath: String = ""): List<CallLogEntry> {
        if (entries.isEmpty()) return entries
        
        val searchPaths = if (customPath.isNotBlank()) {
            listOf(customPath)
        } else {
            // Common call recording paths for various brands
            listOf(
                "/storage/emulated/0/Music/Recordings/Call Recording",
                "/storage/emulated/0/Music/Recordings/Call Recordings",
                "/storage/emulated/0/Recordings/Call Recordings",
                "/storage/emulated/0/Recordings",
                "/storage/emulated/0/MIUI/sound_recorder/call_rec",
                "/storage/emulated/0/call_rec",
                "/storage/emulated/0/Record/Call",
                "/storage/emulated/0/Sounds/CallRecording",
                "/storage/emulated/0/CallRecordings",
                "/storage/emulated/0/Call",
                "/storage/emulated/0/Recordings/Voice Recorder",
                "/storage/emulated/0/Music/Recordings",
                "/storage/emulated/0/Recorder"
            )
        }
        
        // Collect all recording files (limit depth to 4 for better subfolder support)
        val allFiles = mutableListOf<File>()
        searchPaths.forEach { path ->
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                // Using a simpler recursive approach
                fun walkDirs(currentDir: File, depth: Int) {
                    if (depth > 4) return
                    val files = currentDir.listFiles() ?: return
                    files.forEach { file ->
                        if (file.isFile) {
                            allFiles.add(file)
                        } else if (file.isDirectory) {
                            walkDirs(file, depth + 1)
                        }
                    }
                }
                walkDirs(dir, 0)
            }
        }
        
        if (allFiles.isEmpty()) return entries
        
        android.util.Log.d("CallLogRepo", "Found ${allFiles.size} recording files")
        
        // Build a map of phone number suffixes to files for O(1) lookups
        // Key: last 10 digits of phone number found in filename
        val phoneToFilesMap = mutableMapOf<String, MutableList<Pair<File, Long>>>()
        
        val digitPattern = Regex("\\d{10,}")
        allFiles.forEach { file ->
            val fileName = file.name
            // Extract phone numbers from filename
            digitPattern.findAll(fileName).forEach { match ->
                val digits = match.value.takeLast(10)
                phoneToFilesMap.getOrPut(digits) { mutableListOf() }
                    .add(file to file.lastModified())
            }
        }
        
        // Match entries using HashMap lookup
        return entries.map { entry ->
            val last10 = entry.normalizedNumber.takeLast(10)
            if (last10.length < 10) return@map entry
            
            val matchingFiles = phoneToFilesMap[last10] ?: return@map entry
            if (matchingFiles.isEmpty()) return@map entry
            
            // Find best match by timestamp proximity
            val callEndTime = entry.timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() + (entry.duration * 1000)
            
            val bestMatch = matchingFiles.minByOrNull { (_, modifiedTime) ->
                Math.abs(modifiedTime - callEndTime)
            }?.first
            
            if (bestMatch != null) {
                val diffSeconds = Math.abs(bestMatch.lastModified() - callEndTime) / 1000
                // Only accept if within 1 hour of call end time
                if (diffSeconds < 3600) {
                    entry.copy(recordingPath = bestMatch.absolutePath)
                } else {
                    // Just use first match if no time-based match
                    entry.copy(recordingPath = matchingFiles.first().first.absolutePath)
                }
            } else entry
        }
    }
    
    /**
     * Fetch call log entries and match with CRM people
     */
    fun fetchCallLogWithMatches(
        people: List<Person>,
        fromDate: LocalDate = LocalDate.now().minusDays(3),
        toDate: LocalDate = LocalDate.now(),
        callTypeFilter: CallType? = null,
        simSelection: SimSelection = SimSelection.BOTH,
        trackingStartDate: LocalDate? = null
    ): List<CallLogEntry> {
        val entries = fetchCallLog(fromDate, toDate, callTypeFilter, simSelection, trackingStartDate)
        
        // Create maps for strict and fuzzy matching
        val strictMap = mutableMapOf<String, Person>()
        val fuzzyMap = mutableMapOf<String, Person>()
        
        people.forEach { person ->
            // Primary Phone
            val norm = normalizePhoneNumber(person.phone)
            if (norm.isNotEmpty()) {
                strictMap[norm] = person
            }
            
            // Fuzzy match: Only if stored number has no country code (no +)
            if (!person.phone.contains("+")) {
                val stripped = stripCountryCode(person.phone)
                if (stripped.isNotEmpty()) {
                    // Only add if not already present to avoid unpredictable overrides
                    if (!fuzzyMap.containsKey(stripped)) {
                        fuzzyMap[stripped] = person
                    }
                }
            }

            // Alternative Phone
            if (person.alternativePhone.isNotEmpty()) {
                val altNorm = normalizePhoneNumber(person.alternativePhone)
                if (altNorm.isNotEmpty()) {
                    strictMap[altNorm] = person
                }
                
                if (!person.alternativePhone.contains("+")) {
                    val altStripped = stripCountryCode(person.alternativePhone)
                    if (altStripped.isNotEmpty() && !fuzzyMap.containsKey(altStripped)) {
                        fuzzyMap[altStripped] = person
                    }
                }
            }
        }
        
        // Match entries with people
        return entries.map { entry ->
            // 1. Try strict match
            var matchedPerson = strictMap[entry.normalizedNumber]
            
            // 2. Try fuzzy match if strict failed
            if (matchedPerson == null) {
                val stripped = stripCountryCode(entry.phoneNumber)
                matchedPerson = fuzzyMap[stripped]
            }
            
            if (matchedPerson != null) {
                entry.copy(
                    linkedPersonId = matchedPerson.id,
                    linkedPersonName = matchedPerson.name
                )
            } else {
                entry
            }
        }
    }
    
    /**
     * Group call entries by phone number
     */
    fun groupCallsByNumber(
        entries: List<CallLogEntry>
    ): List<CallLogGroup> {
        return entries
            .groupBy { it.normalizedNumber }
            .map { (normalizedNumber, calls) ->
                val sortedCalls = calls.sortedByDescending { it.timestamp }
                val firstCall = sortedCalls.first()
                val displayName = firstCall.linkedPersonName 
                    ?: firstCall.contactName 
                    ?: firstCall.phoneNumber
                
                CallLogGroup(
                    phoneNumber = firstCall.phoneNumber,
                    normalizedNumber = normalizedNumber,
                    displayName = displayName,
                    calls = sortedCalls,
                    linkedPersonId = firstCall.linkedPersonId,
                    hasNote = calls.any { !it.note.isNullOrBlank() }
                )
            }
            .sortedByDescending { it.lastCall?.timestamp }
    }

    /**
     * Generate ActivityLog entries from call history for a person
     */
    fun syncCallHistoryForPerson(
        person: Person,
        settings: CallSettings,
        existingLogs: List<Activity> = emptyList()
    ): List<Activity> {
        if (!hasCallLogPermission()) return emptyList()

        val fromDate = settings.trackingStartDate
        val entries = fetchCallLog(
            fromDate = fromDate,
            toDate = LocalDate.now(),
            simSelection = settings.simSelection,
            trackingStartDate = fromDate
        ).filter { normalizePhoneNumber(it.phoneNumber) == normalizePhoneNumber(person.phone) }

        // Link recordings
        val entriesWithRecordings = findRecordingsForCalls(entries, settings.recordingPath)

        // Create a set of existing call log timestamps to avoid duplicates
        // We use a string key of "Call|<timestamp>" for deduplication
        val existingCallKeys = existingLogs
            .filter { it.type == ActivityType.SYSTEM && it.title?.contains("Call") == true }
            .map { "${it.title}|${it.timestamp}" }
            .toSet()

        val newLogs = mutableListOf<Activity>()

        entriesWithRecordings.forEach { entry ->
            val title = "Call (${entry.callType.label})"
            val key = "$title|${entry.timestamp}"
            
            if (!existingCallKeys.contains(key)) {
                val description = formatCallDescription(entry)
                newLogs.add(Activity(
                    personId = person.id,
                    type = ActivityType.SYSTEM,
                    title = title,
                    description = description,
                    timestamp = entry.timestamp,
                    recordingPath = entry.recordingPath,
                    metadata = mapOf(
                        "duration" to entry.duration.toString(),
                        "call_id" to entry.id.toString(),
                        "type" to entry.callType.name
                    )
                ))
            }
        }

        return newLogs
    }

    private fun formatCallDescription(entry: CallLogEntry): String {
        val duration = formatDuration(entry.duration)
        return when (entry.callType) {
            CallType.MISSED -> "Missed call"
            CallType.REJECTED -> "Call rejected"
            CallType.BLOCKED -> "Call blocked"
            else -> "${entry.callType.label} call lasting $duration"
        }
    }
    
    /**
     * Check if a phone number is saved in device contacts
     */
    fun isSavedContact(phoneNumber: String): Boolean {
        if (!hasContactsPermission()) return false
        
        return try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(phoneNumber)
                .build()
            
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )
            
            cursor?.use {
                it.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get contact name from device contacts
     */
    fun getContactName(phoneNumber: String): String? {
        if (!hasContactsPermission()) return null
        
        val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            .buildUpon()
            .appendPath(phoneNumber)
            .build()
        
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                return it.getString(nameIndex)
            }
        }
        
        return null
    }
    
    /**
     * Map Android call type to our CallType enum
     */
    private fun mapCallType(type: Int): CallType {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
            CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
            CallLog.Calls.VOICEMAIL_TYPE -> CallType.VOICEMAIL
            else -> CallType.UNKNOWN
        }
    }
    
    /**
     * Normalize phone number for matching
     * Removes all non-digit characters except leading +
     */
    
    /**
     * Format duration as human readable string
     */
    companion object {
        // List of common country codes (CC) to check for stripping
        private val commonCountryCodes = listOf("91", "1", "44", "61", "971", "65", "81", "86")

        /**
         * Normalize phone number for matching
         * Strips country codes and standardizes formats to identify the "core" number.
         */
        /**
         * Normalize phone number for matching
         * Returns cleaner valid phone number (digits and leading +).
         * Does NOT strip country codes blindly to avoid collisions.
         */
        fun normalizePhoneNumber(phone: String): String {
            if (phone.isBlank()) return ""
            var digits = phone.filter { it.isDigit() || it == '+' }
            // Ensure only one leading +
            if (digits.count { it == '+' } > 1) {
                digits = "+" + digits.replace("+", "")
            }
            if (digits.lastIndexOf('+') > 0) { // + in middle
                 digits = digits.replace("+", "")
            }
            return digits
        }

        /**
         * Aggressively strip country codes to get the "local" number.
         * Used as a fallback matching strategy.
         */
        fun stripCountryCode(phone: String): String {
            if (phone.isBlank()) return ""
            
            // Remove all non-digits
            var digits = phone.filter { it.isDigit() }
            
            // 1. Handle long numbers (standard mobile/landline)
            if (digits.length >= 10) {
                return digits.takeLast(10)
            }
            
            // 2. Handle potential short codes with country prefixes
            for (cc in commonCountryCodes) {
                if (digits.startsWith(cc) && digits.length >= cc.length + 3) {
                    val remaining = digits.substring(cc.length)
                    if (remaining.length in 3..6) {
                        return remaining
                    }
                }
            }
            
            // 3. Handle leading zero
            if (digits.startsWith("0") && digits.length > 3) {
                return digits.substring(1)
            }
            
            return digits
        }

        fun formatDuration(seconds: Long): String {
            return when {
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
                else -> {
                    val hours = seconds / 3600
                    val mins = (seconds % 3600) / 60
                    "${hours}h ${mins}m"
                }
            }
        }
        
        fun formatDurationShort(seconds: Long): String {
            return when {
                seconds == 0L -> "0s"
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> "${seconds / 60}m"
                else -> "${seconds / 3600}h"
            }
        }

        /**
         * Formats phone number for dialer by stripping default country code.
         * Use this before Intent.ACTION_DIAL to avoid redundant country codes.
         */
        fun formatPhoneForDialer(phone: String?, defaultCountry: String): String {
            if (phone.isNullOrBlank()) return ""
            
            val countryDialCodes = mapOf("US" to "+1", "IN" to "+91", "UK" to "+44", "CA" to "+1", "AU" to "+61")
            val dialCode = countryDialCodes[defaultCountry] ?: return phone
            
            // Cleanup: remove all non-digits except +
            val cleaned = phone.replace(Regex("[^0-9+]"), "")
            
            // Case 1: Starts with +CC (e.g. +919068062563)
            if (cleaned.startsWith(dialCode)) {
                val remaining = cleaned.substring(dialCode.length).trim()
                if (remaining.isNotEmpty()) return remaining
            }
            
            // Case 2: Starts with CC without + (e.g. 919068062563)
            val dialCodeNoPlus = dialCode.removePrefix("+")
            if (dialCodeNoPlus.isNotEmpty() && cleaned.startsWith(dialCodeNoPlus)) {
                val remaining = cleaned.substring(dialCodeNoPlus.length).trim()
                // Extra check: if stripping CC leaves 10 digits, it's very likely CC + LocalNumber
                if (remaining.length >= 10) {
                    return remaining
                }
            }
            
            return phone
        }

        /**
         * Creates a WhatsApp intent with app chooser.
         * This allows users with multiple WhatsApp apps (WhatsApp, WhatsApp Business, clones) 
         * to choose which app to use for sending messages.
         * 
         * @param context The Android context
         * @param phone The phone number to message (will be cleaned automatically)
         * @param defaultPackage Optional package name to use as default (bypass chooser)
         * @return An Intent that shows app chooser for WhatsApp-compatible apps
         */
        fun createWhatsAppChooserIntent(
            context: android.content.Context, 
            phone: String, 
            defaultPackage: String = "always_ask"
        ): android.content.Intent {
            // Clean the phone number - remove all non-digits except +
            val cleanNumber = phone.replace(Regex("[^0-9+]"), "").let { num ->
                // Ensure it starts with country code for international format
                if (num.startsWith("+")) num.removePrefix("+") else num
            }
            
            // Generate list of installed WhatsApp apps
            val installedPackages = getInstalledWhatsAppApps(context)
            
            // Check if default package is set and installed
            if (defaultPackage != "always_ask" && installedPackages.contains(defaultPackage)) {
                return createWhatsAppIntentForPackage(phone, defaultPackage)
            }
            
            // Base URI (wa.me is the modern standard)
            val waUri = android.net.Uri.parse("https://wa.me/$cleanNumber")
            
            if (installedPackages.isEmpty()) {
                // Fallback: just use a generic VIEW intent for the URL
                return android.content.Intent(android.content.Intent.ACTION_VIEW, waUri)
            }
            
            if (installedPackages.size == 1) {
                // If only one app is installed, open it directly without chooser
                return createWhatsAppIntentForPackage(phone, installedPackages[0])
            }
            
            // Multiple apps: Create a chooser with explicit intents for each app
            // and ALSO add a generic one for whatsapp:// scheme which targets clones better
            val specificIntents = installedPackages.map { pkg ->
                createWhatsAppIntentForPackage(phone, pkg)
            }.toMutableList()
            
            // The first one becomes the "base" for the chooser
            val baseIntent = specificIntents.removeAt(0)
            
            val chooser = android.content.Intent.createChooser(baseIntent, "Send via WhatsApp")
            
            // Add the rest as initial intents
            if (specificIntents.isNotEmpty()) {
                chooser.putExtra(android.content.Intent.EXTRA_INITIAL_INTENTS, specificIntents.toTypedArray())
            }
            
            return chooser
        }

        /**
         * Known WhatsApp package names for filtering or direct launch.
         * Includes WhatsApp, WhatsApp Business, and some popular clones.
         */
        val WHATSAPP_PACKAGES = listOf(
            "com.whatsapp",              // Regular WhatsApp
            "com.whatsapp.w4b",          // WhatsApp Business
            "com.gbwhatsapp",            // GBWhatsApp (clone)
            "com.yowhatsapp",            // YoWhatsApp (clone)
            "com.fmwhatsapp",            // FMWhatsApp (clone)
            "com.whatsapp.plus"          // WhatsApp Plus (clone)
        )

        /**
         * Information about an installed WhatsApp app.
         */
        data class WhatsAppAppInfo(val packageName: String, val label: String)

        /**
         * Gets list of installed WhatsApp apps on the device with their labels.
         */
        fun getInstalledWhatsAppAppsWithLabels(context: android.content.Context): List<WhatsAppAppInfo> {
            val pm = context.packageManager
            val installed = getInstalledWhatsAppApps(context)
            return installed.map { pkg ->
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(info).toString()
                    WhatsAppAppInfo(pkg, label)
                } catch (e: Exception) {
                    WhatsAppAppInfo(pkg, pkg)
                }
            }
        }

        /**
         * Gets list of installed WhatsApp apps on the device.
         * Useful for showing a custom picker or checking availability.
         * 
         * @param context The Android context
         * @return List of package names of installed WhatsApp apps
         */
        fun getInstalledWhatsAppApps(context: android.content.Context): List<String> {
            val pm = context.packageManager
            
            // 1. First check our known packages list
            val knownInstalled = WHATSAPP_PACKAGES.filter { packageName ->
                try {
                    pm.getPackageInfo(packageName, 0)
                    true
                } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
                    false
                }
            }
            
            // 2. Also query for apps that explicitly handle wa.me URLs (catches clones/dual apps)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/"))
            val resolved = pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            val resolvedPackages = resolved.map { it.activityInfo.packageName }
            
            // Combine and remove duplicates
            return (knownInstalled + resolvedPackages).distinct().filter { 
                // Basic sanity check to avoid non-messenger results if any
                it.contains("whatsapp", ignoreCase = true) || it.contains("wa.me", ignoreCase = true) || WHATSAPP_PACKAGES.contains(it)
            }
        }

        /**
         * Creates a WhatsApp intent targeting a specific WhatsApp package.
         * Use this when you want to send to a specific WhatsApp app directly.
         * 
         * @param phone The phone number to message
         * @param packageName The package name of the WhatsApp app to use
         * @return An Intent targeting the specific WhatsApp app
         */
        fun createWhatsAppIntentForPackage(phone: String, packageName: String): android.content.Intent {
            val cleanNumber = phone.replace(Regex("[^0-9+]"), "").let { num ->
                if (num.startsWith("+")) num.removePrefix("+") else num
            }
            
            return android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://wa.me/$cleanNumber")
                setPackage(packageName)
            }
        }
    }
}
