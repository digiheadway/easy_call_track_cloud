package com.miniclick.calltrackmanage.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

class RecordingRepository private constructor(private val context: Context) {

    private val PREFS_NAME = "app_prefs"
    private val KEY_RECORDING_PATH = "recording_path"
    private val KEY_DETECTED_PATH = "detected_recording_path"
    private val KEY_PATH_VERIFIED = "recording_path_verified"
    private val KEY_LEARNED_FOLDER = "learned_recording_folder"
    
    // --- Performance Optimization: MediaStore Caching ---
    // Caches the results of the last MediaStore scan to avoid redundant IO
    // during batch processing (e.g., in RecordingUploadWorker).
    private var lastMediaStoreScanTime: Long = 0
    private var lastMediaStoreResults: List<RecordingSourceFile> = emptyList()
    private var lastScanBuffer: Int = 0
    private var lastCallDate: Long = 0
    private val CACHE_EXPIRY_MS = 30_000 // 30 seconds is enough for one sync pass

    companion object {
        private const val TAG = "RecordingRepository"
        
        // Compiled Patterns for Performance using Regex class since they are reused
        private val PATTERN_ONEPLUS = java.util.regex.Pattern.compile("(\\d{10})")
        private val PATTERN_STANDARD = java.util.regex.Pattern.compile("(\\d{8}[_\\-]\\d{6})")

        @Volatile
        private var INSTANCE: RecordingRepository? = null
        
        fun getInstance(context: Context): RecordingRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecordingRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Device default paths - searched FIRST (priority order)
        private val DEVICE_DEFAULT_PATHS = listOf(
            // Google/Pixel/Stock Android
            "Recordings/Call recordings",
            "Recordings/CallCloud",              // Our imported recordings
            "Recordings",
            "Download",                          // Some Pixel versions, OnePlus O Dialer
            
            // Samsung (OneUI)
            "Recordings/Call",                   // Samsung newer (OneUI 4+)
            "Call",                              // Samsung older
            "DCIM/Call",                         // Some Samsung variants
            "Recordings/Voice Recorder",
            "Voice Recorder",
            
            // Xiaomi/MIUI/Redmi/Poco
            "MIUI/sound_recorder/call_rec",
            "Recordings/MIUI/sound_recorder/call_rec",
            "SoundRecorder/call_rec",
            "MIUI/sound_recorder",
            "Recorder",
            
            // OnePlus (OxygenOS / O Dialer)
            "Music/Recordings/Call recordings",  // OnePlus O Dialer primary path
            "Music/Recordings/Call Recordings",  // Case variant
            "Record/Call",
            "Record/PhoneRecord",
            "Recordings/PhoneRecord",
            "Call recordings",                   // OnePlus legacy path
            "Download/Record",                   // Some OnePlus versions save to Download
            "Download/PhoneRecord",              // OnePlus O Dialer variant
            "Android/data/com.oneplus.communication.data/files/Record/PhoneRecord", // Android 11+
            "Android/media/com.oneplus.communication.data/files/Record/PhoneRecord", // Media folder variant
            
            // Oppo/Realme/ColorOS
            "Music/Recordings/Call Recordings",
            "Recordings/Call Recordings",
            "ColorOS/PhoneRecord",
            "ColorOS/Recordings",
            "DCIM/Recorder",                     // Some ColorOS variants
            "Android/media/com.coloros.soundrecorder", // ColorOS 12+ hidden
            
            // Vivo (FuntouchOS)
            "Record/Call",
            "VoiceRecorder/Calls",               // Vivo specific
            "VoiceRecorder",
            
            // Huawei/Honor (EMUI/HarmonyOS)
            "Sounds/CallRecord",
            "Record",
            "HuaweiBackup/CallRecord",           // Huawei backup path
            "record",
            
            // Motorola/LG/Asus
            "Recordings",
            "Recordings/Call",
            "Record/Call"
        )
        
        // Third-party app paths - searched AFTER device defaults
        private val THIRD_PARTY_PATHS = listOf(
            "ACR",                               // ACR Phone (Callyzer path)
            "ACRCalls",                          // ACR Call Recorder
            "CubeCallRecorder/All",              // Cube ACR
            "CubeCallRecorder/Recordings",       // Cube ACR alternate
            "Truecaller/Recording",              // Truecaller (case-sensitive fix)
            "Truecaller/recordings",             // Truecaller alternate
            "CallU/Recordings",                  // CallU
            "BoldBeast",                         // Boldbeast Recorder (case fix)
            "Boldbeast",                         // Boldbeast alternate
            "NLL/CallRecorder",                  // NLL Call Recorder
            "RMC/CallRecordings",                // RMC Android Call Recorder
            "callrecorder",                      // Generic
            "Automatic Call Recorder",           // Auto Call Recorder
            "AutomaticCallRecorder",             // Auto Call Recorder alternate
            "Call Recorder - ACR/recordings",    // ACR Pro
            "CallRecorder",                      // Generic
            "Call Recordings",                   // Generic
            "IntCall",                           // Call Recorder - IntCall
            ".blackbox",                         // Blackbox Call Recorder (hidden)
            "SpyCaller"                          // Some spy/auto recorders
        )
        
        private val AUDIO_EXTENSIONS = listOf("mp3", "amr", "wav", "aac", "m4a", "ogg", "3gp", "opus")
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Cache for detected path
    private var cachedDetectedPath: String? = null

    /**
     * Get a flow that emits the current recording count.
     * Updates whenever called or periodically.
     */
    fun getRecordingCountFlow(): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(getPathInfo().recordingCount)
            kotlinx.coroutines.delay(10000) // Refresh every 10 seconds
        }
    }

    /**
     * Get the effective recording path (custom if set, otherwise detected)
     */
    fun getRecordingPath(): String {
        // Custom path takes priority
        val customPath = getCustomPath()
        if (!customPath.isNullOrEmpty()) {
            return customPath
        }
        
        // Return detected path
        return getDetectedPath() ?: getDefaultPath()
    }

    /**
     * Get custom path set by user (null if not set)
     */
    fun getCustomPath(): String? {
        return prefs.getString(KEY_RECORDING_PATH, null)
    }

    /**
     * Check if custom path is set
     */
    fun isCustomPathSet(): Boolean {
        return !getCustomPath().isNullOrEmpty()
    }

    /**
     * Set custom recording path
     */
    fun setCustomPath(path: String) {
        prefs.edit()
            .putString(KEY_RECORDING_PATH, path)
            .putBoolean(KEY_PATH_VERIFIED, false) // Reset verification
            .apply()
    }

    /**
     * Clear custom path (will use auto-detection)
     */
    fun clearCustomPath() {
        prefs.edit()
            .remove(KEY_RECORDING_PATH)
            .remove(KEY_PATH_VERIFIED)
            .apply()
    }

    /**
     * Get the auto-detected recording path
     */
    fun getDetectedPath(): String? {
        // Check cache first
        cachedDetectedPath?.let { return it }
        
        // Check saved detected path
        val saved = prefs.getString(KEY_DETECTED_PATH, null)
        if (!saved.isNullOrEmpty()) {
            val dir = File(saved)
            if (dir.exists() && hasAudioFiles(dir)) {
                cachedDetectedPath = saved
                return saved
            }
        }
        
        // Scan for paths
        val detected = scanForRecordingPath()
        if (detected != null) {
            prefs.edit().putString(KEY_DETECTED_PATH, detected).apply()
            cachedDetectedPath = detected
        }
        
        return detected
    }

    /**
     * Scan for recording path - Device defaults FIRST, then third-party
     */
    private fun scanForRecordingPath(): String? {
        val storage = Environment.getExternalStorageDirectory()
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        
        // 1. Brand-Specific Priority logic
        // We prioritize paths that match the device manufacturer
        val prioritizedPaths = mutableListOf<String>()
        val others = mutableListOf<String>()
        
        for (relativePath in DEVICE_DEFAULT_PATHS) {
            val isMatch = when {
                manufacturer.contains("samsung") && relativePath.contains("Samsung", ignoreCase = true) -> true
                manufacturer.contains("samsung") && (relativePath.startsWith("Recordings/Call") || relativePath.startsWith("Call")) -> true
                (manufacturer.contains("oneplus") || manufacturer.contains("oppo") || manufacturer.contains("realme")) && 
                    (relativePath.contains("OnePlus", ignoreCase = true) || relativePath.contains("ColorOS", ignoreCase = true) || relativePath.contains("Music/Recordings")) -> true
                (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")) && 
                    relativePath.contains("MIUI", ignoreCase = true) -> true
                manufacturer.contains("vivo") && (relativePath.contains("Record/Call") || relativePath.contains("VoiceRecorder")) -> true
                (manufacturer.contains("huawei") || manufacturer.contains("honor")) && relativePath.contains("record", ignoreCase = true) -> true
                else -> false
            }
            
            if (isMatch) prioritizedPaths.add(relativePath) else others.add(relativePath)
        }

        // 2. Search Prioritized Paths FIRST
        for (relativePath in (prioritizedPaths + others)) {
            val dir = File(storage, relativePath)
            if (dir.exists() && dir.isDirectory && hasAudioFiles(dir)) {
                Log.d(TAG, "Found recording path (Priority: ${prioritizedPaths.contains(relativePath)}): ${dir.absolutePath}")
                return dir.absolutePath
            }
        }
        
        // 3. Search third-party app paths
        for (relativePath in THIRD_PARTY_PATHS) {
            val dir = File(storage, relativePath)
            if (dir.exists() && dir.isDirectory && hasAudioFiles(dir)) {
                Log.d(TAG, "Found third-party app recording path: ${dir.absolutePath}")
                return dir.absolutePath
            }
        }
        
        // 4. Deep scan common parent directories (Last resort)
        val parentDirs = listOf("Recordings", "Record", "Music", "Call")
        for (parentName in parentDirs) {
            val parentDir = File(storage, parentName)
            if (parentDir.exists() && parentDir.isDirectory) {
                if (hasAudioFiles(parentDir)) return parentDir.absolutePath
                
                parentDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                    if (hasAudioFiles(subDir)) return subDir.absolutePath
                }
            }
        }
        
        return null
    }

    /**
     * Check if directory has audio files
     */
    private fun hasAudioFiles(dir: File): Boolean {
        return dir.listFiles()?.any { file ->
            file.isFile && file.extension.lowercase() in AUDIO_EXTENSIONS
        } == true
    }

    /**
     * Count audio files in a directory
     */
    private fun countAudioFiles(dir: File): Int {
        return dir.listFiles()?.count { file ->
            file.isFile && file.extension.lowercase() in AUDIO_EXTENSIONS
        } ?: 0
    }

    /**
     * Force re-scan for recording path
     */
    fun rescanPath(): String? {
        cachedDetectedPath = null
        prefs.edit().remove(KEY_DETECTED_PATH).apply()
        return getDetectedPath()
    }

    /**
     * Verify if current path has recordings (for verification badge)
     */
    fun verifyPath(): Boolean {
        val path = getRecordingPath()
        val dir = File(path)
        val hasRecordings = dir.exists() && dir.isDirectory && hasAudioFiles(dir)
        
        // Save verification status
        prefs.edit().putBoolean(KEY_PATH_VERIFIED, hasRecordings).apply()
        
        Log.d(TAG, "Path verification: $path -> $hasRecordings")
        return hasRecordings
    }

    /**
     * Check if path is verified (cached status)
     */
    fun isPathVerified(): Boolean {
        return prefs.getBoolean(KEY_PATH_VERIFIED, false)
    }

    /**
     * Get path info for display in settings
     */
    fun getPathInfo(): PathInfo {
        val customPath = getCustomPath()
        val detectedPath = getDetectedPath()
        val effectivePath = customPath ?: detectedPath ?: getDefaultPath()
        
        var verified = false
        var audioCount = 0
        
        if (effectivePath.startsWith("content://")) {
            try {
                val treeUri = Uri.parse(effectivePath)
                val docFile = DocumentFile.fromTreeUri(context, treeUri)
                if (docFile != null && docFile.exists() && docFile.isDirectory) {
                    audioCount = docFile.listFiles().count { 
                        val name = it.name?.lowercase() ?: ""
                        AUDIO_EXTENSIONS.any { ext -> name.endsWith(".$ext") }
                    }
                    verified = audioCount > 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking DocumentFile path: $effectivePath", e)
            }
        } else {
            val dir = File(effectivePath)
            val exists = dir.exists() && dir.isDirectory
            audioCount = if (exists) countAudioFiles(dir) else 0
            verified = exists && audioCount > 0
        }
        
        // Update verification status
        if (verified) {
            prefs.edit().putBoolean(KEY_PATH_VERIFIED, true).apply()
        }
        
        return PathInfo(
            effectivePath = effectivePath,
            detectedPath = detectedPath,
            customPath = customPath,
            isCustom = customPath != null,
            isVerified = verified,
            recordingCount = audioCount
        )
    }

    /**
     * Get default recording path
     */
    private fun getDefaultPath(): String {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Recordings/Call Recordings"
        ).absolutePath
    }

    /**
     * Data class for recording source file abstraction
     */
    data class RecordingSourceFile(
        val name: String,
        val lastModified: Long,
        val absolutePath: String, // Can be local path or content URI
        val isLocal: Boolean
    )

    /**
     * Get all recording files from current path and our public CallCloud folder
     */
    fun getRecordingFiles(): List<RecordingSourceFile> {
        val allFiles = mutableListOf<RecordingSourceFile>()
        val storage = Environment.getExternalStorageDirectory()
        
        // 1. Get files from the primary detected/custom path
        val path = getRecordingPath()
        if (path.startsWith("content://")) {
            try {
                val treeUri = Uri.parse(path)
                val docDir = DocumentFile.fromTreeUri(context, treeUri)
                docDir?.listFiles()?.filter { 
                    val name = it.name?.lowercase() ?: ""
                    it.isFile && AUDIO_EXTENSIONS.any { ext -> name.endsWith(".$ext") }
                }?.forEach { docFile ->
                    allFiles.add(RecordingSourceFile(
                        name = docFile.name ?: "Unknown",
                        lastModified = docFile.lastModified(),
                        absolutePath = docFile.uri.toString(),
                        isLocal = false
                    ))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error listing DocumentFile files: $path", e)
            }
        } else {
            val primaryDir = File(path)
            if (primaryDir.exists() && primaryDir.isDirectory) {
                primaryDir.listFiles()?.filter { 
                    it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS 
                }?.forEach { file ->
                    allFiles.add(RecordingSourceFile(
                        name = file.name,
                        lastModified = file.lastModified(),
                        absolutePath = file.absolutePath,
                        isLocal = true
                    ))
                }
            }
        }
        
        // 2. ALWAYS include our public CallCloud folder (prevents loss on reinstall/path change)
        val callCloudDir = File(storage, "Recordings/CallCloud")
        if (callCloudDir.exists() && callCloudDir.isDirectory) {
            callCloudDir.listFiles()?.filter { 
                it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS 
            }?.forEach { file ->
                // Avoid duplicates if primary path is also CallCloud
                if (allFiles.none { it.absolutePath == file.absolutePath }) {
                    allFiles.add(RecordingSourceFile(
                        name = file.name,
                        lastModified = file.lastModified(),
                        absolutePath = file.absolutePath,
                        isLocal = true
                    ))
                }
            }
        }
        
        return allFiles
    }

    /**
     * Normalize phone number
     */
    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
    }

    /**
     * Try to extract a timestamp from the filename using common recording patterns.
     */
    private fun extractDateFromFilename(fileName: String): Long? {
        // Pattern 1: OnePlus / ODialer: yyMMddHHmm (e.g. 2601090249)
        // Usually part of a longer string but often 10 digits
        try {
            // Find sequence of 10 digits
            val matcher = PATTERN_ONEPLUS.matcher(fileName)
            if (matcher.find()) {
                val dateStr = matcher.group(1) ?: return null
                // Validate if it looks like yyMMddHHmm
                // Year 2020-2030 (20-30), Month 01-12, Day 01-31, Hour 00-23, Min 00-59
                // Simple validation:
                val year = dateStr.substring(0, 2).toInt()
                val month = dateStr.substring(2, 4).toInt()
                val day = dateStr.substring(4, 6).toInt()
                
                if (year in 20..40 && month in 1..12 && day in 1..31) {
                     val sdf = java.text.SimpleDateFormat("yyMMddHHmm", java.util.Locale.getDefault())
                     return sdf.parse(dateStr)?.time
                }
            }
            
            // Pattern 2: Standard yyyyMMdd_HHmmss or similar
            val matcherLong = PATTERN_STANDARD.matcher(fileName)
            if (matcherLong.find()) {
                 val dateStr = matcherLong.group(1)?.replace("-", "")?.replace("_", "") ?: return null
                 val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault())
                 return sdf.parse(dateStr)?.time
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return null
    }

    /**
     * Find a recording file that matches the given call details.
     */
    fun findRecordingInList(
        files: List<RecordingSourceFile>, 
        callDate: Long, 
        durationSec: Long, 
        phoneNumber: String,
        contactName: String? = null
    ): String? {
        // Normalize phone details
        val normalizedPhone = normalizePhoneNumber(phoneNumber)
        
        // Safety: If phone number is too short (e.g. < 7 digits), don't match by phone alone to avoid noise
        val validPhoneForMatching = normalizedPhone.length >= 7
        
        // Prepare filename date patterns
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = callDate
        val year = calendar.get(java.util.Calendar.YEAR).toString()
        val month = String.format("%02d", calendar.get(java.util.Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(java.util.Calendar.DAY_OF_MONTH))
        val hour = String.format("%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY))
        val minute = String.format("%02d", calendar.get(java.util.Calendar.MINUTE))
        
        val dateStrings = listOf(
            "$year$month$day",
            "$year-$month-$day",
            "${year}_${month}_$day",
            "$day$month$year",
            "$day-$month-$year",
            // OnePlus format (YYMMDDHHmm) e.g., 2601090249
            "${year.takeLast(2)}$month$day$hour$minute",
            "${year.takeLast(2)}$month$day"
        )
        
        // Pre-filter to reduce search space (Optimization)
        // Keep files that are either:
        // 1. Within 2 hours (generous window for simple time matching)
        // 2. OR Contain the phone number (for backups/shares that might have lost timestamps)
        val candidateFiles = files.filter { file ->
            val timeDiff = kotlin.math.abs(file.lastModified - callDate)
            val isInTimeWindow = timeDiff <= 2 * 60 * 60 * 1000L // 2 hours
            
            val isPhoneMatch = validPhoneForMatching && (
                file.name.contains(normalizedPhone) || 
                (normalizedPhone.length > 9 && file.name.contains(normalizedPhone.takeLast(9)))
            )
            
            val isNameMatch = !contactName.isNullOrBlank() && contactName.split(" ").any { 
                it.length > 2 && file.name.contains(it, ignoreCase = true) 
            }

            isInTimeWindow || isPhoneMatch || isNameMatch
        }

        // Helper to score candidates
        fun scoreCandidate(file: RecordingSourceFile): MatchCandidate? {
            val fileName = file.name.lowercase()
            var totalScore = 0
            
            // --- 1. Identity Match (Phone/Name) ---
            var hasIdentityMatch = false
            
            if (validPhoneForMatching) {
                if (fileName.contains(normalizedPhone)) {
                    totalScore += 40 // Exact phone match is very strong
                    hasIdentityMatch = true
                } else if (normalizedPhone.length > 9 && fileName.contains(normalizedPhone.takeLast(9))) {
                    totalScore += 20 // Partial phone match
                    hasIdentityMatch = true
                }
            }
            
            if (!contactName.isNullOrBlank()) {
                 val nameParts = contactName.lowercase().split(" ").filter { it.length > 2 }
                 if (nameParts.isNotEmpty() && nameParts.any { fileName.contains(it) }) {
                     totalScore += 20 // Name match (Callyzer: +20)
                     hasIdentityMatch = true
                 }
            }

            // --- 2. Time Match (Callyzer-style weights) ---
            var timeDiff = kotlin.math.abs(file.lastModified - callDate)
            
            // Try explicit Date in Filename (Highest Trust) - overrides lastModified if valid
            val extractedDate = extractDateFromFilename(fileName)
            if (extractedDate != null) {
                val diff = kotlin.math.abs(extractedDate - callDate)
                if (diff <= 5 * 60 * 1000L) {
                     // Recalculate timeDiff using extracted date for fairness in sorting
                     timeDiff = diff
                }
            }

            // Callyzer-style time scoring (highest weight factor)
            when {
                timeDiff <= 5 * 1000L -> totalScore += 100     // ≤5 seconds: +100 (almost certain match)
                timeDiff <= 30 * 1000L -> totalScore += 80     // ≤30 seconds: +80
                timeDiff <= 60 * 1000L -> totalScore += 60     // ≤1 minute: +60
                timeDiff <= 2 * 60 * 1000L -> totalScore += 40 // ≤2 minutes: +40
                timeDiff <= 5 * 60 * 1000L -> totalScore += 20 // ≤5 minutes: +20
                timeDiff <= 15 * 60 * 1000L -> totalScore += 10 // ≤15 minutes: +10
            }

            // --- 3. Folder Context Bonus (Callyzer: +30) ---
            // Files from known recording folders are more likely to be actual call recordings
            val knownFolderKeywords = listOf(
                "call", "recording", "recorder", "miui/sound", "phonerecord",
                "voicerecorder", "callcloud", "acr", "cube", "truecaller"
            )
            val filePath = file.absolutePath.lowercase()
            if (knownFolderKeywords.any { filePath.contains(it) }) {
                totalScore += 30 // Known recorder folder bonus
            }

            // NOTE: Removed strict rejection rules to allow more flexible matching.
            // Merged calls (conference calls, call waiting) may have:
            // - Different durations (file contains multiple calls merged)
            // - Different filenames (recorder may use different naming for merged calls)
            // The scoring system will still prefer better matches, but we don't hard-reject.

            // --- 4. Duration Match (Callyzer: +40) ---
            // Check duration for all candidates (important signal)
            val fileDurationMs = getAudioDuration(file.absolutePath)
            if (fileDurationMs > 0) {
                val callDurationMs = durationSec * 1000
                val durDiff = kotlin.math.abs(fileDurationMs - callDurationMs)
                
                when {
                    durDiff < 1000 -> totalScore += 40   // <1 sec diff: +40 (exact match)
                    durDiff < 3000 -> totalScore += 30   // <3 sec diff: +30
                    durDiff < 5000 -> totalScore += 20   // <5 sec diff: +20 (Callyzer threshold)
                    durDiff < 10000 -> totalScore += 10  // <10 sec diff: +10
                }
            }

            
            // FINAL SAFEGUARD: Callyzer-style threshold of 100
            // A match should have either:
            // - Timestamp within 5s (100) -> PASS
            // - Timestamp within 30s (80) + name (20) -> PASS
            // - Timestamp within 60s (60) + phone (50) -> PASS (110)
            // - Timestamp within 60s (60) + folder (30) + duration (20+) -> PASS (110+)
            // - Just good timing alone might not be enough without other signals
            if (totalScore < 100) return null

            return MatchCandidate(file, totalScore, timeDiff)
        }

        // Execute scoring
        return candidateFiles.asSequence()
            .mapNotNull { scoreCandidate(it) }
            .sortedWith(compareByDescending<MatchCandidate> { it.score }.thenBy { it.timeDiff })
            .firstOrNull()
            ?.file?.absolutePath
    }

    private fun getAudioDuration(path: String): Long {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            if (path.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(path))
            } else {
                retriever.setDataSource(path)
            }
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
    }

    private data class MatchCandidate(val file: RecordingSourceFile, val score: Int, val timeDiff: Long)

    // ==================== MEDIASTORE QUERY (Android 10+) ====================
    
    /**
     * Query MediaStore for audio files within a time window.
     * This is the PRIMARY detection method for Android 10+ as it works regardless of file path.
     * 
     * @param callDate The timestamp when the call started (milliseconds)
     * @param durationSec The call duration in seconds
     * @param bufferSeconds How much buffer around the call time to search (default 5 minutes)
     * @return List of potential recording files from MediaStore
     */
    private fun findRecordingViaMediaStore(
        callDate: Long, 
        durationSec: Long,
        bufferSeconds: Int = 300
    ): List<RecordingSourceFile> {
        // --- Enhanced Cache Check for Batch Syncs ---
        val now = System.currentTimeMillis()
        val windowStart = callDate - (bufferSeconds * 1000L)
        val windowEnd = callDate + (durationSec * 1000L) + (bufferSeconds * 1000L)

        // Reuse cache if: 
        // 1. It was scanned very recently (within 60s)
        // 2. The previously requested buffer was at least as large as current
        // 3. The new call date is within the bounds of the last scan's primary window (+/- 4 mins)
        if (now - lastMediaStoreScanTime < 60_000 && 
            bufferSeconds <= lastScanBuffer && 
            kotlin.math.abs(callDate - lastCallDate) < 4 * 60 * 1000L) { 
            Log.d(TAG, "Reusing cached MediaStore results (${lastMediaStoreResults.size} files) for call at ${java.util.Date(callDate)}")
            return lastMediaStoreResults
        }

        val results = mutableListOf<RecordingSourceFile>()
        
        try {
            // Calculate time window in SECONDS (MediaStore uses seconds for DATE_ADDED)
            val callDateSeconds = callDate / 1000
            val startWindow = callDateSeconds - bufferSeconds
            val endWindow = callDateSeconds + durationSec + bufferSeconds
            
            // Get the appropriate content URI based on Android version
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE
            )
            
            // Query for audio files added within our time window
            val selection = "${MediaStore.Audio.Media.DATE_ADDED} >= ? AND ${MediaStore.Audio.Media.DATE_ADDED} <= ?"
            val selectionArgs = arrayOf(startWindow.toString(), endWindow.toString())
            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val dateAdded = cursor.getLong(dateColumn) // in seconds
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    
                    // Only include audio files with recognized extensions
                    val extension = name.substringAfterLast('.', "").lowercase()
                    if (extension in AUDIO_EXTENSIONS) {
                        results.add(RecordingSourceFile(
                            name = name,
                            lastModified = dateAdded * 1000, // Convert to milliseconds
                            absolutePath = contentUri.toString(),
                            isLocal = false
                        ))
                    }
                }
            }
            
            Log.d(TAG, "MediaStore query (±${bufferSeconds}s) found ${results.size} audio files")
            
            // --- Update Cache ---
            lastMediaStoreScanTime = now
            lastMediaStoreResults = results
            lastScanBuffer = bufferSeconds
            lastCallDate = callDate
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query failed", e)
        }
        
        return results
    }
    
    // ==================== LEARNING SYSTEM ====================
    
    /**
     * Save the folder where a successful match was found.
     * This folder will be prioritized in future searches.
     */
    private fun saveLearnedFolder(filePath: String) {
        val folder = if (filePath.startsWith("content://")) {
            // For content URIs, we can't easily extract folder, skip learning
            return
        } else {
            File(filePath).parent ?: return
        }
        
        prefs.edit().putString(KEY_LEARNED_FOLDER, folder).apply()
        Log.d(TAG, "Learned folder saved: $folder")
    }
    
    /**
     * Get files from the learned folder (where previous matches were found).
     */
    private fun getLearnedFolderFiles(): List<RecordingSourceFile> {
        val learnedPath = prefs.getString(KEY_LEARNED_FOLDER, null) ?: return emptyList()
        val folder = File(learnedPath)
        
        if (!folder.exists() || !folder.isDirectory) {
            return emptyList()
        }
        
        return folder.listFiles()?.filter { 
            it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS 
        }?.map { file ->
            RecordingSourceFile(
                name = file.name,
                lastModified = file.lastModified(),
                absolutePath = file.absolutePath,
                isLocal = true
            )
        } ?: emptyList()
    }
    
    /**
     * Get files from CallCloud backup folder only.
     */
    private fun getCallCloudFiles(): List<RecordingSourceFile> {
        val storage = Environment.getExternalStorageDirectory()
        val callCloudDir = File(storage, "Recordings/CallCloud")
        
        if (!callCloudDir.exists() || !callCloudDir.isDirectory) {
            return emptyList()
        }
        
        return callCloudDir.listFiles()?.filter { 
            it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS 
        }?.map { file ->
            RecordingSourceFile(
                name = file.name,
                lastModified = file.lastModified(),
                absolutePath = file.absolutePath,
                isLocal = true
            )
        } ?: emptyList()
    }

    // ==================== TIERED RECORDING FINDER ====================
    
    /**
     * Find a recording using a multi-tiered approach:
     * 
     * Tier 1: CallCloud backup folder (fastest, our managed folder)
     * Tier 2: Learned folder (previous successful match location)
     * Tier 3: MediaStore query with ±5 min window (works on Android 10+)
     * Tier 4: Traditional file path scan (legacy fallback)
     * Tier 5: MediaStore with wider ±30 min window (last resort)
     * 
     * This tiered approach ensures maximum compatibility across Android versions
     * and device manufacturers while optimizing for speed.
     */
    fun findRecording(
        callDate: Long, 
        durationSec: Long, 
        phoneNumber: String,
        contactName: String? = null
    ): String? {
        Log.d(TAG, "Starting tiered recording search for call: phone=$phoneNumber, duration=${durationSec}s")
        
        // ===== TIER 1: CallCloud Backup Folder (Fastest) =====
        // These are recordings we imported from Google Dialer or user shares
        val callCloudFiles = getCallCloudFiles()
        if (callCloudFiles.isNotEmpty()) {
            findRecordingInList(callCloudFiles, callDate, durationSec, phoneNumber, contactName)?.let { path ->
                Log.d(TAG, "✓ Found via Tier 1 (CallCloud): $path")
                return path
            }
        }
        
        // ===== TIER 2: Learned Folder (Previous Success) =====
        // Check the folder where we previously found successful matches
        val learnedFiles = getLearnedFolderFiles()
        if (learnedFiles.isNotEmpty()) {
            findRecordingInList(learnedFiles, callDate, durationSec, phoneNumber, contactName)?.let { path ->
                Log.d(TAG, "✓ Found via Tier 2 (Learned Folder): $path")
                return path
            }
        }
        
        // ===== TIER 3: MediaStore Query - Standard Window (±5 min) =====
        // This is the PRIMARY method for Android 10+ as it works regardless of file path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mediaStoreFiles = findRecordingViaMediaStore(callDate, durationSec, bufferSeconds = 300)
            if (mediaStoreFiles.isNotEmpty()) {
                findRecordingInList(mediaStoreFiles, callDate, durationSec, phoneNumber, contactName)?.let { path ->
                    Log.d(TAG, "✓ Found via Tier 3 (MediaStore ±5min): $path")
                    saveLearnedFolder(path) // Remember this location
                    return path
                }
            }
        }
        
        // ===== TIER 4: Traditional File Path Scan =====
        // Fallback for Android 9 and below, or when MediaStore doesn't find it
        val pathFiles = getRecordingFiles()
        if (pathFiles.isNotEmpty()) {
            findRecordingInList(pathFiles, callDate, durationSec, phoneNumber, contactName)?.let { path ->
                Log.d(TAG, "✓ Found via Tier 4 (File Scan): $path")
                saveLearnedFolder(path) // Remember this location
                return path
            }
        }
        
        // ===== TIER 5: MediaStore Query - Wider Window (±30 min) =====
        // Last resort with expanded time window for edge cases
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val widerMediaStoreFiles = findRecordingViaMediaStore(callDate, durationSec, bufferSeconds = 1800)
            if (widerMediaStoreFiles.isNotEmpty()) {
                findRecordingInList(widerMediaStoreFiles, callDate, durationSec, phoneNumber, contactName)?.let { path ->
                    Log.d(TAG, "✓ Found via Tier 5 (MediaStore ±30min): $path")
                    saveLearnedFolder(path)
                    return path
                }
            }
        }
        
        Log.d(TAG, "✗ Recording not found after all tiers")
        return null
    }

    /**
     * Import a shared recording from a Uri (e.g., from Google Dialer share)
     */
    fun importSharedRecording(uri: android.net.Uri, fileName: String?): File? {
        try {
            // Save to Public Recordings folder so it persists after uninstall
            val publicRecordingsInfo = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS)
            val destinationDir = File(publicRecordingsInfo, "CallCloud")
            
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }
            
            val finalFileName = fileName ?: "SharedRecording_${System.currentTimeMillis()}.wav"
            val destinationFile = File(destinationDir, finalFileName)
            
            val success = copyUriToFile(uri, destinationFile)
            if (success) {
                Log.d(TAG, "Successfully imported recording to public storage: ${destinationFile.absolutePath}")
                return destinationFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import shared recording", e)
        }
        return null
    }

    /**
     * Copy content from a Uri to a file
     */
    private fun copyUriToFile(uri: android.net.Uri, destinationFile: File): Boolean {
        return try {
            // 1. Try to read original last modified time
            var originalLastModified: Long = 0
            try {
                DocumentFile.fromSingleUri(context, uri)?.let {
                    originalLastModified = it.lastModified()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read original timestamp from URI")
            }

            // 2. Copy Data
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 3. Apply timestamp if found (Crucial for matching logic)
            if (originalLastModified > 0) {
                destinationFile.setLastModified(originalLastModified)
                Log.d(TAG, "Restored original timestamp to imported file: $originalLastModified")
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to file", e)
            false
        }
    }

    /**
     * Generate phone number variants for matching
     */
    private fun generatePhoneVariants(normalizedPhone: String): List<String> {
        val variants = mutableListOf(normalizedPhone)
        
        if (normalizedPhone.length > 10) {
            variants.add(normalizedPhone.takeLast(10))
            if (normalizedPhone.length > 9) {
                variants.add(normalizedPhone.takeLast(9))
            }
        }
        
        val noLeadingZeros = normalizedPhone.trimStart('0')
        if (noLeadingZeros != normalizedPhone) {
            variants.add(noLeadingZeros)
        }
        
        return variants.filter { it.isNotEmpty() }
    }

    /**
     * Data class for path information
     */
    data class PathInfo(
        val effectivePath: String,
        val detectedPath: String?,
        val customPath: String?,
        val isCustom: Boolean,
        val isVerified: Boolean,
        val recordingCount: Int
    )
}
