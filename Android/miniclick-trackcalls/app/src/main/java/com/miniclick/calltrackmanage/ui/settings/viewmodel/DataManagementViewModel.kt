package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    application: Application,
    callDataRepository: CallDataRepository,
    settingsRepository: SettingsRepository,
    recordingRepository: com.miniclick.calltrackmanage.data.RecordingRepository
) : AndroidViewModel(application) {

    private val dataManager = DataManager(
        application,
        callDataRepository,
        settingsRepository,
        recordingRepository,
        viewModelScope
    )

    fun exportDataToJson(
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        dataManager.exportDataToJson(uri, onProgress, onComplete)
    }

    fun exportDataToCsv(
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        dataManager.exportDataToCsv(uri, onProgress, onComplete)
    }

    fun importDataFromJson(
        uri: Uri,
        onProgress: (Boolean) -> Unit,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        dataManager.importDataFromJson(uri, onProgress, onComplete)
    }

    fun clearAllData(
        onProgress: (Boolean) -> Unit,
        onComplete: (success: Boolean) -> Unit
    ) {
        dataManager.clearAllData(onProgress, onComplete)
    }

    fun resetSyncStatus(onComplete: () -> Unit) {
        dataManager.resetSyncStatus(onComplete)
    }

    suspend fun getStorageStats(): DataManager.StorageStats {
        return dataManager.getStorageStats()
    }
}
