package com.miniclick.calltrackmanage.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton to track global background processes for UI feedback.
 */
object ProcessMonitor {
    
    data class ProcessState(
        val title: String,      // e.g. "Importing Data from System"
        val progress: Float,    // 0.0 to 1.0f
        val details: String? = null, // e.g. "Processing 150/300"
        val isIndeterminate: Boolean = false
    )

    private val _activeProcess = MutableStateFlow<ProcessState?>(null)
    val activeProcess = _activeProcess.asStateFlow()

    fun startProcess(title: String, isIndeterminate: Boolean = false) {
        android.util.Log.d("ProcessMonitor", "Starting process: $title (indeterminate=$isIndeterminate)")
        _activeProcess.value = ProcessState(title, 0f, null, isIndeterminate)
    }

    fun updateProgress(progress: Float, details: String? = null) {
        _activeProcess.update { current ->
            if (current != null) {
                // Log only on significant changes to avoid spam
                if (details != null && details != current.details) {
                    android.util.Log.d("ProcessMonitor", "Updating process: ${current.title} -> $details")
                }
                current.copy(
                    progress = progress, 
                    details = details ?: current.details,
                    isIndeterminate = false // Automatically switch to determinate if we have progress
                )
            } else current
        }
    }

    fun endProcess() {
        android.util.Log.d("ProcessMonitor", "Ending process")
        _activeProcess.value = null
    }
}
