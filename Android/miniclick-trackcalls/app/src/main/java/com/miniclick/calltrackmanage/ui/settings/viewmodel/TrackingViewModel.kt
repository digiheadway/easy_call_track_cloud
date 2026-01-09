package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    application: Application,
    settingsRepository: SettingsRepository,
    callDataRepository: CallDataRepository,
    recordingRepository: RecordingRepository
) : AndroidViewModel(application) {

    private val trackingManager = TrackingManager(
        application,
        settingsRepository,
        callDataRepository,
        recordingRepository,
        viewModelScope
    )

    val recordingPath: StateFlow<String> = trackingManager.recordingPath
    val isRecordingPathVerified: StateFlow<Boolean> = trackingManager.isRecordingPathVerified
    val isRecordingPathCustom: StateFlow<Boolean> = trackingManager.isRecordingPathCustom
    val recordingCount: StateFlow<Int> = trackingManager.recordingCount

    init {
        refreshRecordingPathInfo()
    }

    fun refreshRecordingPathInfo() {
        trackingManager.refreshRecordingPathInfo()
    }

    fun updateCallRecordEnabled(enabled: Boolean, scanOld: Boolean = false, isGoogleDialer: Boolean) {
        trackingManager.updateCallRecordEnabled(enabled, scanOld, isGoogleDialer)
    }

    fun updateRecordingPath(path: String) {
        trackingManager.updateRecordingPath(path)
    }

    fun clearCustomRecordingPath() {
        trackingManager.clearCustomRecordingPath()
    }

    fun updateTrackStartDate(date: Long) {
        trackingManager.updateTrackStartDate(date)
    }

    fun syncCallManually() {
        trackingManager.syncCallManually()
    }

    fun retryRecordingUpload(compositeId: String) {
        trackingManager.retryRecordingUpload(compositeId)
    }
}
