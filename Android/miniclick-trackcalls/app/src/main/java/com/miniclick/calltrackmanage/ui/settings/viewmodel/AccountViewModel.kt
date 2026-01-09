package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    application: Application,
    settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val syncManager = SyncManager(
        application,
        settingsRepository,
        viewModelScope
    )

    fun verifyPairingCode(
        pairingCode: String,
        deviceId: String,
        onProgress: (Boolean) -> Unit,
        onResult: (success: Boolean, employeeName: String?, settings: com.miniclick.calltrackmanage.network.EmployeeSettingsDto?, plan: com.miniclick.calltrackmanage.network.PlanInfoDto?, error: String?) -> Unit
    ) {
        syncManager.verifyPairingCode(pairingCode, deviceId, onProgress = onProgress, onResult = onResult)
    }

    fun savePairingInfo(orgId: String, userId: String) {
        syncManager.savePairingInfo(orgId, userId)
    }

    fun clearPairingInfo() {
        syncManager.clearPairingInfo()
    }

    fun isSyncSetup(): Boolean = syncManager.isSyncSetup()

    fun getCurrentPairingCode(): String = syncManager.getCurrentPairingCode()
}
