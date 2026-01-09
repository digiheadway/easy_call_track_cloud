package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.PersonDataEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Handles all data management operations extracted from SettingsViewModel.
 * 
 * Responsibilities:
 * - Export data to various formats (JSON, CSV)
 * - Import data from backups
 * - Clear data operations
 * - Storage usage tracking
 */
class DataManager(
    private val application: Application,
    private val callDataRepository: CallDataRepository,
    private val settingsRepository: SettingsRepository,
    private val recordingRepository: RecordingRepository,
    private val scope: CoroutineScope
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Export all data to JSON format.
     */
    fun exportDataToJson(
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { onProgress(true) }
            try {
                val calls = callDataRepository.getAllCalls()
                val persons = callDataRepository.getAllPersons()
                
                val exportData = ExportData(
                    version = 1,
                    exportDate = System.currentTimeMillis(),
                    calls = calls,
                    persons = persons
                )
                
                val json = gson.toJson(exportData)
                
                application.contentResolver.openOutputStream(uri)?.use { output ->
                    OutputStreamWriter(output).use { writer ->
                        writer.write(json)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    onComplete(true, "Exported ${calls.size} calls and ${persons.size} persons")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Export failed: ${e.localizedMessage}")
                }
            } finally {
                withContext(Dispatchers.Main) { onProgress(false) }
            }
        }
    }

    /**
     * Export call data to CSV format.
     */
    fun exportDataToCsv(
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { onProgress(true) }
            try {
                val calls = callDataRepository.getAllCalls()
                
                val csvBuilder = StringBuilder()
                // Header
                csvBuilder.appendLine("CompositeId,PhoneNumber,ContactName,CallType,Duration,CallDate,CallNote,Reviewed")
                
                // Data rows
                calls.forEach { call ->
                    val escapedNote = call.callNote?.replace("\"", "\"\"")?.replace("\n", " ") ?: ""
                    csvBuilder.appendLine("""
                        "${call.compositeId}","${call.phoneNumber}","${call.contactName ?: ""}",${call.callType},${call.duration},${call.callDate},"$escapedNote",${call.reviewed}
                    """.trimIndent())
                }
                
                application.contentResolver.openOutputStream(uri)?.use { output ->
                    OutputStreamWriter(output).use { writer ->
                        writer.write(csvBuilder.toString())
                    }
                }
                
                withContext(Dispatchers.Main) {
                    onComplete(true, "Exported ${calls.size} calls to CSV")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Export failed: ${e.localizedMessage}")
                }
            } finally {
                withContext(Dispatchers.Main) { onProgress(false) }
            }
        }
    }

    /**
     * Import data from a JSON backup.
     */
    fun importDataFromJson(
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { onProgress(true) }
            try {
                val json = application.contentResolver.openInputStream(uri)?.use { input ->
                    InputStreamReader(input).use { reader ->
                        reader.readText()
                    }
                }
                
                if (json.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "File is empty")
                    }
                    return@launch
                }
                
                val exportData = gson.fromJson(json, ExportData::class.java)
                
                // Import using the repository's bulk import method
                callDataRepository.importData(exportData.calls, exportData.persons)
                
                withContext(Dispatchers.Main) {
                    onComplete(true, "Imported ${exportData.calls.size} calls and ${exportData.persons.size} persons")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Import failed: ${e.localizedMessage}")
                }
            } finally {
                withContext(Dispatchers.Main) { onProgress(false) }
            }
        }
    }

    /**
     * Clear all local data.
     */
    fun clearAllData(
        onProgress: (Boolean) -> Unit,
        onComplete: (success: Boolean) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { onProgress(true) }
            try {
                callDataRepository.deleteAllData()
                settingsRepository.clearAllSettings()
                recordingRepository.clearCustomPath()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "All data cleared", Toast.LENGTH_SHORT).show()
                    onComplete(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(application, "Failed to clear data: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            } finally {
                withContext(Dispatchers.Main) { onProgress(false) }
            }
        }
    }

    /**
     * Reset sync status for all items.
     */
    fun resetSyncStatus(
        onComplete: () -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            callDataRepository.clearSyncStatus()
            withContext(Dispatchers.Main) {
                Toast.makeText(application, "Sync status reset", Toast.LENGTH_SHORT).show()
                onComplete()
            }
        }
    }

    /**
     * Get storage statistics.
     */
    suspend fun getStorageStats(): StorageStats {
        return withContext(Dispatchers.IO) {
            val calls = callDataRepository.getAllCalls()
            val persons = callDataRepository.getAllPersons()
            val recordingsCount = calls.count { !it.localRecordingPath.isNullOrEmpty() }
            
            StorageStats(
                totalCalls = calls.size,
                totalPersons = persons.size,
                recordingsCount = recordingsCount,
                storageUsedBytes = settingsRepository.getStorageUsedBytes()
            )
        }
    }

    /**
     * Data class for storage statistics.
     */
    data class StorageStats(
        val totalCalls: Int,
        val totalPersons: Int,
        val recordingsCount: Int,
        val storageUsedBytes: Long
    )

    /**
     * Data class for export/import.
     */
    data class ExportData(
        val version: Int,
        val exportDate: Long,
        val calls: List<CallDataEntity>,
        val persons: List<PersonDataEntity>
    )
}
