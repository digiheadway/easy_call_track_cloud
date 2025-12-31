package com.calltracker.manager.data

import android.content.Context
import android.os.Environment
import java.io.File
import java.util.Date

class RecordingRepository(private val context: Context) {

    private val PREFS_NAME = "app_prefs"
    private val KEY_RECORDING_PATH = "recording_path"

    // Default path per user request: Music/Recordings/Call Recordings
    // We'll use Environment.getExternalStorageDirectory() as base.
    private val defaultPath = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
        "Recordings/Call Recordings"
    ).absolutePath

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getRecordingPath(): String {
        return prefs.getString(KEY_RECORDING_PATH, defaultPath) ?: defaultPath
    }

    fun setRecordingPath(path: String) {
        prefs.edit().putString(KEY_RECORDING_PATH, path).apply()
    }

    fun clearRecordingPath() {
        prefs.edit().remove(KEY_RECORDING_PATH).apply()
    }

    fun getRecordingFiles(): Array<File> {
        val path = getRecordingPath()
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyArray()
        return dir.listFiles() ?: emptyArray()
    }

    fun findRecordingInList(files: Array<File>, callDate: Long, durationSec: Long, phoneNumber: String): String? {
        val callEndTime = callDate + (durationSec * 1000)
        val tolerance = 30 * 1000L // 30 seconds
        val searchWindowEnd = callEndTime + tolerance

        val candidates = files.filter { file ->
            // Check if file is audio
            file.extension.lowercase() in listOf("mp3", "amr", "wav", "aac", "m4a") &&
            file.lastModified() in (callDate - tolerance)..(searchWindowEnd + tolerance)
        }

        // Refine candidates: 
        val phoneMatch = candidates.find { it.name.contains(phoneNumber) }
        if (phoneMatch != null) return phoneMatch.absolutePath

        if (candidates.isNotEmpty()) {
             // Return closest to callDate
             return candidates.minByOrNull { 
                 kotlin.math.abs(it.lastModified() - callEndTime) 
             }?.absolutePath
        }

        return null
    }

    // Deprecated inefficient method kept for compatibility if needed, but we essentially duplicate logic or delegate
    fun findRecording(callDate: Long, durationSec: Long, phoneNumber: String): String? {
        val files = getRecordingFiles()
        if (files.isEmpty()) return null
        return findRecordingInList(files, callDate, durationSec, phoneNumber)
    }
}
