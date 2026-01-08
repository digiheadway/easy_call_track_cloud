package com.miniclick.calltrackmanage.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

class RecordingRepository private constructor(private val context: Context) {

    private val PREFS_NAME = "app_prefs"
    private val KEY_RECORDING_PATH = "recording_path"
    private val KEY_DETECTED_PATH = "detected_recording_path"
    private val KEY_PATH_VERIFIED = "recording_path_verified"
    
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
            
            // Samsung
            "Call",
            "Recordings/Voice Recorder",
            "Voice Recorder",
            
            // Xiaomi/MIUI / Redmi
            "MIUI/sound_recorder/call_rec",
            "Recordings/MIUI/sound_recorder/call_rec",
            "SoundRecorder/call_rec",
            "MIUI/sound_recorder",
            
            // OnePlus
            "Record/Call",
            "Record/PhoneRecord",
            
            // Oppo/Realme/ColorOS
            "Music/Recordings/Call Recordings",
            "Recordings/Call Recordings",
            "ColorOS/PhoneRecord",
            
            // Vivo
            "Record/Call",
            
            // Huawei/Honor
            "Sounds/CallRecord",
            "record",
            
            // LG
            "Recordings"
        )
        
        // Third-party app paths - searched AFTER device defaults
        private val THIRD_PARTY_PATHS = listOf(
            "ACRCalls",                          // ACR Call Recorder
            "CubeCallRecorder/All",              // Cube ACR
            "CubeCallRecorder/Recordings",       // Cube ACR alternate
            "Truecaller/recordings",             // Truecaller
            "CallU/Recordings",                  // CallU
            "Boldbeast",                         // Boldbeast Recorder
            "NLL/CallRecorder",                  // NLL Call Recorder
            "RMC/CallRecordings",                // RMC Android Call Recorder
            "callrecorder",                      // Generic
            "Automatic Call Recorder",           // Auto Call Recorder
            "Call Recorder - ACR/recordings",    // ACR Pro
            "CallRecorder",                      // Generic
            "Call Recordings"                    // Generic
        )
        
        private val AUDIO_EXTENSIONS = listOf("mp3", "amr", "wav", "aac", "m4a", "ogg", "3gp", "opus")
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Cache for detected path
    private var cachedDetectedPath: String? = null

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
        
        // 1. Search device default paths FIRST
        for (relativePath in DEVICE_DEFAULT_PATHS) {
            val dir = File(storage, relativePath)
            if (dir.exists() && dir.isDirectory && hasAudioFiles(dir)) {
                Log.d(TAG, "Found device default recording path: ${dir.absolutePath}")
                return dir.absolutePath
            }
        }
        
        // 2. Search third-party app paths
        for (relativePath in THIRD_PARTY_PATHS) {
            val dir = File(storage, relativePath)
            if (dir.exists() && dir.isDirectory && hasAudioFiles(dir)) {
                Log.d(TAG, "Found third-party app recording path: ${dir.absolutePath}")
                return dir.absolutePath
            }
        }
        
        // 3. Deep scan common parent directories
        val parentDirs = listOf("Recordings", "Record", "Music", "Call")
        for (parentName in parentDirs) {
            val parentDir = File(storage, parentName)
            if (parentDir.exists() && parentDir.isDirectory) {
                // Check parent itself
                if (hasAudioFiles(parentDir)) {
                    Log.d(TAG, "Found recordings in parent: ${parentDir.absolutePath}")
                    return parentDir.absolutePath
                }
                
                // Check subdirectories
                parentDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                    if (hasAudioFiles(subDir)) {
                        Log.d(TAG, "Found recordings in subdirectory: ${subDir.absolutePath}")
                        return subDir.absolutePath
                    }
                }
            }
        }
        
        Log.d(TAG, "No recording path detected")
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
                     totalScore += 25 // Name match is significant
                     hasIdentityMatch = true
                 }
            }

            // --- 2. Time Match ---
            var timeDiff = kotlin.math.abs(file.lastModified - callDate)
            
            // Try explicit Date in Filename (Highest Trust) - overrides lastModified if valid
            val extractedDate = extractDateFromFilename(fileName)
            val isFilenameDateMatch = if (extractedDate != null) {
                val diff = kotlin.math.abs(extractedDate - callDate)
                if (diff <= 5 * 60 * 1000L) {
                     // Recalculate timeDiff using extracted date for fairness in sorting
                     timeDiff = diff
                     true
                } else {
                     // CRITICAL: If filename has an explicit date (e.g. 2024-01-01) 
                     // and it's > 2 hours away from call date, REJECT IT IMMEDIATELY.
                     // This fixes "Different date recording matched" issues where file metadata is recent
                     // (e.g. copied/restored files) but filename clearly indicates an old date.
                     if (diff > 2 * 60 * 60 * 1000L) return null
                     false
                }
            } else {
                // Fallback to substring matching as before (for simple formats)
                dateStrings.any { fileName.contains(it) }
            }

            if (isFilenameDateMatch) {
                totalScore += 40
            } else {
                // B. Timestamp Check (if no filename match)
                if (timeDiff <= 2 * 60 * 1000L) { // 2 mins (Perfect)
                    totalScore += 30
                } else if (timeDiff <= 15 * 60 * 1000L) { // 15 mins (Good)
                    totalScore += 15
                } else if (timeDiff <= 60 * 60 * 1000L) { // 1 hour (Acceptable)
                    totalScore += 5
                }
            }

            // REJECTION RULES:
            
            // 1. Stricter Time Window for files without Number/Name match:
            // Must be within 5 minutes (was 15). This prevents attaching a random file 
            // from 10 mins ago just because duration matches.
            if (!hasIdentityMatch && timeDiff > 5 * 60 * 1000L) return null
            
            // 2. Safeguard for Same Person, Same Duration, Different Time (e.g. 3PM vs 9PM):
            // If we matched the Name/Phone, BUT the time is way off (> 4 hours), 
            // AND we didn't find the date in the filename -> Reject it.
            // This forces the system to look for the *other* file that is closer in time.
            // We use 4 hours to be safe against timezone issues, but 3PM vs 9PM is 6 hours, so this catches it.
            if (hasIdentityMatch && timeDiff > 4 * 60 * 60 * 1000L && !isFilenameDateMatch) return null
            
            // 3. General sanity check (24h rule)
            if (hasIdentityMatch && timeDiff > 24 * 60 * 60 * 1000L && !isFilenameDateMatch) return null

            
            // --- 3. Duration Match (Lazy Fetch) ---
            // Only check duration if we already have a decent candidate (score > 10)
            if (totalScore > 10) {
                val fileDurationMs = getAudioDuration(file.absolutePath)
                if (fileDurationMs > 0) {
                    val callDurationMs = durationSec * 1000
                    val durDiff = kotlin.math.abs(fileDurationMs - callDurationMs)
                    
                    if (durDiff < 1000) { // < 1 sec diff
                        totalScore += 50 // HUGE Bonus for exact duration match (overrides slight time diffs)
                    } else if (durDiff < 5000) { // < 5 sec diff
                        totalScore += 30
                    } else if (durDiff < 10000) { // < 10 sec diff
                        totalScore += 10
                    } else if (durDiff > 60000) { // > 1 min diff (checking logic)
                         // If we had a perfect time match score (30-40) but duration is WAY off, punish harder.
                         // But for long calls, 1 min might be drift. 
                         // Let's rely on Relative Difference for longer calls?
                         // For now, simple penalty is safe.
                        totalScore -= 20 
                    }
                }
            }

            
            // FINAL SAFEGUARD: Minimum acceptable score to consider this a "match"
            // We need at least 30 points. Examples:
            // - Exact Phone (40) -> PASS
            // - Date in filename (40) -> PASS
            // - Perfect Time (30) -> PASS
            // - Name (25) + Good Duration (10+) -> PASS
            // - Name (25) + Weak Time (5) -> PASS
            // - Just Weak Time (5) or Good Time (15) -> FAIL (Too risky)
            if (totalScore < 30) return null

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

    /**
     * Find a recording
     */
    fun findRecording(
        callDate: Long, 
        durationSec: Long, 
        phoneNumber: String,
        contactName: String? = null
    ): String? {
        val files = getRecordingFiles()
        if (files.isEmpty()) {
            Log.d(TAG, "No recording files found in: ${getRecordingPath()}")
            return null
        }
        Log.d(TAG, "Searching ${files.size} files for recording match")
        return findRecordingInList(files, callDate, durationSec, phoneNumber, contactName)
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
