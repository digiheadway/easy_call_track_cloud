package com.miniclick.calltrackmanage.data.repository

/**
 * Interface defining the contract for recording file operations.
 * This abstraction enables easier testing and flexibility in implementation.
 */
interface RecordingRepository {
    
    /**
     * Get the effective recording path (custom if set, otherwise detected).
     */
    fun getRecordingPath(): String
    
    /**
     * Get custom path set by user (null if not set).
     */
    fun getCustomPath(): String?
    
    /**
     * Check if custom path is set.
     */
    fun isCustomPathSet(): Boolean
    
    /**
     * Set custom recording path.
     */
    fun setCustomPath(path: String)
    
    /**
     * Clear custom path (will use auto-detection).
     */
    fun clearCustomPath()
    
    /**
     * Get the auto-detected recording path.
     */
    fun getDetectedPath(): String
    
    /**
     * Scan file system for a recording path with audio files.
     */
    suspend fun scanForRecordingPath(): String?
    
    /**
     * Force re-scan for recording path.
     */
    suspend fun rescanPath(): String?
    
    /**
     * Verify if current path has recordings.
     */
    suspend fun verifyPath(): Boolean
    
    /**
     * Check if path is verified (cached status).
     */
    fun isPathVerified(): Boolean
    
    /**
     * Get path info for display in settings.
     */
    fun getPathInfo(): RecordingPathInfo
    
    /**
     * Data class for recording path information display.
     */
    data class RecordingPathInfo(
        val path: String,
        val isCustom: Boolean,
        val isVerified: Boolean,
        val audioFileCount: Int
    )
    
    /**
     * Get all recording files from configured paths.
     */
    suspend fun getRecordingFiles(): List<RecordingFile>
    
    /**
     * Data class representing a recording source file.
     */
    data class RecordingFile(
        val path: String,
        val name: String,
        val lastModified: Long,
        val size: Long,
        val duration: Long? = null
    )
    
    /**
     * Find a recording file matching the given call details.
     * Uses smart matching with phone number, timestamp, and duration.
     * 
     * @param callDate The timestamp when the call started (milliseconds)
     * @param durationSec The duration of the call (seconds)
     * @param phoneNumber The phone number involved in the call
     * @param contactName Optional contact name for additional matching
     * @return Path to the matching recording file, or null if not found
     */
    suspend fun findRecording(
        callDate: Long,
        durationSec: Long,
        phoneNumber: String,
        contactName: String? = null
    ): String?
    
    /**
     * Find a recording in a pre-loaded list of files.
     * This is more efficient when processing multiple calls.
     */
    fun findRecordingInList(
        files: List<RecordingFile>,
        callDate: Long,
        durationSec: Long,
        phoneNumber: String,
        contactName: String? = null
    ): String?
    
    /**
     * Get the audio duration of a file in milliseconds.
     */
    fun getAudioDuration(path: String): Long?
}
