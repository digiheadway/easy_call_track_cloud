package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles call tracking and recording settings extracted from SettingsViewModel.
 */
class TrackingManager(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val callDataRepository: CallDataRepository,
    private val recordingRepository: RecordingRepository,
    private val scope: CoroutineScope
) {
    private val _isRecordingPathVerified = MutableStateFlow(false)
    val isRecordingPathVerified: StateFlow<Boolean> = _isRecordingPathVerified.asStateFlow()

    private val _isRecordingPathCustom = MutableStateFlow(false)
    val isRecordingPathCustom: StateFlow<Boolean> = _isRecordingPathCustom.asStateFlow()

    private val _recordingCount = MutableStateFlow(0)
    val recordingCount: StateFlow<Int> = _recordingCount.asStateFlow()

    private val _recordingPath = MutableStateFlow("")
    val recordingPath: StateFlow<String> = _recordingPath.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncStats = MutableStateFlow("")
    val lastSyncStats: StateFlow<String> = _lastSyncStats.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _syncQueueCount = MutableStateFlow(0)
    val syncQueueCount: StateFlow<Int> = _syncQueueCount.asStateFlow()

    init {
        _lastSyncTime.value = settingsRepository.getLastSyncTime()
        refreshRecordingPathInfo()
        
        // Observe sync queue count
        scope.launch {
            callDataRepository.getSyncQueueCountFlow().collect { count: Int ->
                _syncQueueCount.value = count
            }
        }
    }

    fun refreshRecordingPathInfo() {
        scope.launch(Dispatchers.IO) {
            val info = recordingRepository.getPathInfo()
            _recordingPath.update { info.effectivePath }
            _isRecordingPathVerified.update { info.isVerified }
            _isRecordingPathCustom.update { info.isCustom }
            _recordingCount.update { info.recordingCount }
        }
    }

    fun updateCallRecordEnabled(enabled: Boolean, scanOld: Boolean = false, isGoogleDialer: Boolean) {
        scope.launch {
            settingsRepository.setCallRecordEnabled(enabled)
            if (enabled) {
                if (isGoogleDialer) {
                    settingsRepository.setShowRecordingReminder(true)
                }
                
                if (scanOld) {
                    settingsRepository.setRecordingLastEnabledTimestamp(0L)
                    callDataRepository.resetSkippedRecordings()
                    CallSyncWorker.runNow(application)
                    showToast("Scanning all past calls for recordings...")
                } else {
                    settingsRepository.setRecordingLastEnabledTimestamp(System.currentTimeMillis())
                }
            }
            
            if (enabled) {
                RecordingUploadWorker.runNow(application)
            }
        }
    }

    fun updateRecordingPath(path: String) {
        recordingRepository.setCustomPath(path)
        refreshRecordingPathInfo()
        CallSyncWorker.runNow(application)
    }

    fun clearCustomRecordingPath() {
        recordingRepository.clearCustomPath()
        refreshRecordingPathInfo()
    }

    fun updateTrackStartDate(date: Long) {
        settingsRepository.setTrackStartDate(date)
        CallSyncWorker.runNow(application)
    }

    fun syncCallManually() {
        if (settingsRepository.getOrganisationId().isEmpty()) {
            showToast("❌ Pairing Code not set\nPlease join an organisation first")
            return
        }
        
        if (settingsRepository.getCallerPhoneSim1().isEmpty() && settingsRepository.getCallerPhoneSim2().isEmpty()) {
            showToast("❌ Caller Phone not set\nPlease set at least one SIM phone number")
            return
        }

        scope.launch {
            _isSyncing.update { true }
            _lastSyncStats.update { "Refreshing..." }
            
            callDataRepository.syncFromSystemCallLog()
            
            // WorkManager handles the actual sync, but we want to show it's starting
            CallSyncWorker.runNow(application)
            RecordingUploadWorker.runNow(application)
            
            // For manual sync, we could observe the WorkInfo to provide better stats, 
            // but for simplicity we'll just show it started and then success after a bit (or use repository)
            _lastSyncStats.update { "Sync started..." }
            _isSyncing.update { false }
            
            val now = System.currentTimeMillis()
            settingsRepository.setLastSyncTime(now)
            _lastSyncTime.update { now }
        }
    }

    fun retryRecordingUpload(compositeId: String) {
        scope.launch {
            callDataRepository.updateRecordingSyncStatus(compositeId, com.miniclick.calltrackmanage.data.db.RecordingSyncStatus.PENDING)
            callDataRepository.updateSyncError(compositeId, null)
            RecordingUploadWorker.runNow(application)
            showToast("Retrying upload...")
        }
    }

    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
        }
    }
}
