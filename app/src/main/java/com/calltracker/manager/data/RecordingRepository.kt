package com.calltracker.manager.data

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

class RecordingRepository(private val context: Context) {

    private val PREFS_NAME = "app_prefs"
    private val KEY_RECORDING_PATH = "recording_path"
    private val KEY_DETECTED_PATH = "detected_recording_path"
    private val KEY_PATH_VERIFIED = "recording_path_verified"
    
    companion object {
        private const val TAG = "RecordingRepository"
        
        // Device default paths - searched FIRST (priority order)
        private val DEVICE_DEFAULT_PATHS = listOf(
            // Google/Pixel/Stock Android
            "Recordings/Call recordings",
            "Recordings",
            
            // Samsung
            "Call",
            "Recordings/Voice Recorder",
            "Voice Recorder",
            
            // Xiaomi/MIUI
            "MIUI/sound_recorder/call_rec",
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
        
        val dir = File(effectivePath)
        val exists = dir.exists() && dir.isDirectory
        val audioCount = if (exists) countAudioFiles(dir) else 0
        val verified = exists && audioCount > 0
        
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
     * Get all recording files from current path
     */
    fun getRecordingFiles(): Array<File> {
        val path = getRecordingPath()
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyArray()
        return dir.listFiles()?.filter { 
            it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS 
        }?.toTypedArray() ?: emptyArray()
    }

    /**
     * Find a recording file that matches the given call details.
     * 
     * Matching priority:
     * 1. Phone number in filename (strongest match)
     * 2. Contact name in filename
     * 3. Closest to call end time (fallback)
     */
    fun findRecordingInList(
        files: Array<File>, 
        callDate: Long, 
        durationSec: Long, 
        phoneNumber: String,
        contactName: String? = null
    ): String? {
        val callEndTime = callDate + (durationSec * 1000)
        val tolerance = 60 * 1000L // 60 seconds
        val searchWindowEnd = callEndTime + tolerance

        // Filter to audio files within time window
        val candidates = files.filter { file ->
            file.extension.lowercase() in AUDIO_EXTENSIONS &&
            file.lastModified() in (callDate - tolerance)..(searchWindowEnd + tolerance)
        }

        if (candidates.isEmpty()) return null

        // Normalize phone number for matching
        val normalizedPhone = normalizePhoneNumber(phoneNumber)
        val phoneVariants = generatePhoneVariants(normalizedPhone)

        // Priority 1: Phone number match (strongest)
        val phoneMatch = candidates.find { file ->
            val fileName = file.nameWithoutExtension.lowercase()
            phoneVariants.any { variant -> fileName.contains(variant) }
        }
        if (phoneMatch != null) return phoneMatch.absolutePath

        // Priority 2: Contact name match
        if (!contactName.isNullOrBlank()) {
            val normalizedName = contactName.trim().lowercase()
            val nameParts = normalizedName.split(" ").filter { it.length > 1 }
            
            val nameMatch = candidates.find { file ->
                val fileName = file.nameWithoutExtension.lowercase()
                fileName.contains(normalizedName) || 
                nameParts.all { part -> fileName.contains(part) }
            }
            if (nameMatch != null) return nameMatch.absolutePath
        }

        // Priority 3: Closest to call end time (fallback)
        return candidates.minByOrNull { 
            kotlin.math.abs(it.lastModified() - callEndTime) 
        }?.absolutePath
    }

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
     * Normalize phone number
     */
    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9]"), "")
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
