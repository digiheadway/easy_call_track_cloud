package com.miniclick.calltrackmanage.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton to track global background processes for UI feedback.
 * Now supports multiple concurrent processes.
 */
object ProcessMonitor {
    
    data class ProcessState(
        val id: String,             // Unique identifier for process type
        val title: String,          // e.g. "Importing Data from System"
        val progress: Float,        // 0.0 to 1.0f
        val details: String? = null, // e.g. "Processing 150/300"
        val isIndeterminate: Boolean = false,
        val status: ProcessStatus = ProcessStatus.RUNNING,
        val startTime: Long = System.currentTimeMillis()
    )
    
    enum class ProcessStatus {
        PENDING,    // Waiting to start
        RUNNING,    // Actively running
        COMPLETED,  // Successfully completed
        FAILED      // Failed with error
    }
    
    // Process IDs for different types of operations
    object ProcessIds {
        const val IMPORT_CALL_LOG = "import_call_log"
        const val FIND_RECORDINGS = "find_recordings"
        const val SYNC_METADATA = "sync_metadata"
        const val SYNC_PERSONS = "sync_persons"
        const val UPLOAD_RECORDINGS = "upload_recordings"
        const val ATTACH_RECORDING = "attach_recording"
        const val PULL_SERVER_UPDATES = "pull_server_updates"
    }

    // Legacy: Single active process (for backward compatibility)
    private val _activeProcess = MutableStateFlow<ProcessState?>(null)
    val activeProcess = _activeProcess.asStateFlow()
    
    // New: Multiple processes tracked
    private val _allProcesses = MutableStateFlow<Map<String, ProcessState>>(emptyMap())
    val allProcesses = _allProcesses.asStateFlow()

    /**
     * Start or update a process by ID
     */
    fun startProcess(id: String, title: String, isIndeterminate: Boolean = false) {
        android.util.Log.d("ProcessMonitor", "Starting process: $id - $title (indeterminate=$isIndeterminate)")
        val process = ProcessState(
            id = id,
            title = title,
            progress = 0f,
            isIndeterminate = isIndeterminate,
            status = ProcessStatus.RUNNING
        )
        _allProcesses.update { it + (id to process) }
        // Also update legacy single process for backward compatibility
        _activeProcess.value = process
    }
    
    /**
     * Legacy: Start process without ID (uses title as ID)
     */
    fun startProcess(title: String, isIndeterminate: Boolean = false) {
        val id = when {
            title.contains("Importing", ignoreCase = true) -> ProcessIds.IMPORT_CALL_LOG
            title.contains("Finding Recording", ignoreCase = true) -> ProcessIds.FIND_RECORDINGS
            title.contains("Uploading", ignoreCase = true) -> ProcessIds.UPLOAD_RECORDINGS
            title.contains("Syncing", ignoreCase = true) -> ProcessIds.SYNC_METADATA
            else -> title.lowercase().replace(" ", "_")
        }
        startProcess(id, title, isIndeterminate)
    }

    /**
     * Update progress for a specific process
     */
    fun updateProgress(id: String, progress: Float, details: String? = null) {
        _allProcesses.update { processes ->
            val current = processes[id]
            if (current != null) {
                if (details != null && details != current.details) {
                    android.util.Log.d("ProcessMonitor", "Updating process $id: $details")
                }
                processes + (id to current.copy(
                    progress = progress,
                    details = details ?: current.details,
                    isIndeterminate = false
                ))
            } else processes
        }
        // Update legacy
        _activeProcess.update { current ->
            if (current != null && current.id == id) {
                current.copy(progress = progress, details = details ?: current.details, isIndeterminate = false)
            } else current
        }
    }
    
    /**
     * Legacy: Update progress for current active process
     */
    fun updateProgress(progress: Float, details: String? = null) {
        val currentId = _activeProcess.value?.id ?: return
        updateProgress(currentId, progress, details)
    }

    /**
     * Mark a process as completed
     */
    fun completeProcess(id: String, message: String? = null) {
        android.util.Log.d("ProcessMonitor", "Completing process: $id")
        _allProcesses.update { processes ->
            val current = processes[id]
            if (current != null) {
                processes + (id to current.copy(
                    progress = 1f,
                    status = ProcessStatus.COMPLETED,
                    details = message ?: "Completed"
                ))
            } else processes
        }
        // If this was the active process, pick another one from allProcesses or set to null
        if (_activeProcess.value?.id == id) {
            val otherProcess = _allProcesses.value.values.find { it.id != id && it.status == ProcessStatus.RUNNING }
            _activeProcess.value = otherProcess
        }
        // Auto-remove completed process after short delay
        CoroutineHelper.launch {
            delay(2000)
            _allProcesses.update { it - id }
        }
    }

    /**
     * Mark a process as failed
     */
    fun failProcess(id: String, error: String? = null) {
        android.util.Log.d("ProcessMonitor", "Process failed: $id - $error")
        _allProcesses.update { processes ->
            val current = processes[id]
            if (current != null) {
                processes + (id to current.copy(
                    status = ProcessStatus.FAILED,
                    details = error ?: "Failed"
                ))
            } else processes
        }
        // Pick another if this was active
        if (_activeProcess.value?.id == id) {
            val otherProcess = _allProcesses.value.values.find { it.id != id && it.status == ProcessStatus.RUNNING }
            _activeProcess.value = otherProcess
        }
    }

    /**
     * End a process (remove it)
     */
    fun endProcess(id: String) {
        android.util.Log.d("ProcessMonitor", "Ending process: $id")
        _allProcesses.update { it - id }
        if (_activeProcess.value?.id == id) {
            val otherProcess = _allProcesses.value.values.find { it.id != id && it.status == ProcessStatus.RUNNING }
            _activeProcess.value = otherProcess
        }
    }
    
    /**
     * Legacy: End current active process
     */
    fun endProcess() {
        val currentId = _activeProcess.value?.id
        android.util.Log.d("ProcessMonitor", "Ending active process: $currentId")
        if (currentId != null) {
            _allProcesses.update { it - currentId }
            // Pick next running process
            val nextProcess = _allProcesses.value.values.find { it.status == ProcessStatus.RUNNING }
            _activeProcess.value = nextProcess
        } else {
            _activeProcess.value = null
        }
    }

    /**
     * Set pending count for a process type (creates a pending entry if not running)
     */
    fun setPendingCount(id: String, title: String, count: Int) {
        if (count <= 0) {
            // Remove if exists and not running
            _allProcesses.update { processes ->
                val current = processes[id]
                if (current?.status == ProcessStatus.PENDING) {
                    processes - id
                } else processes
            }
            return
        }
        
        _allProcesses.update { processes ->
            val current = processes[id]
            if (current == null || current.status == ProcessStatus.PENDING) {
                // Create or update pending entry
                processes + (id to ProcessState(
                    id = id,
                    title = title,
                    progress = 0f,
                    details = "$count pending",
                    status = ProcessStatus.PENDING
                ))
            } else {
                // Don't overwrite running process
                processes
            }
        }
    }
    
    /**
     * Clear all processes (for reset)
     */
    fun clearAll() {
        _allProcesses.value = emptyMap()
        _activeProcess.value = null
    }
}

private object CoroutineHelper {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    fun launch(block: suspend CoroutineScope.() -> Unit) {
        scope.launch { block() }
    }
}
