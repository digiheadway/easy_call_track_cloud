package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import android.telecom.TelecomManager
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.ui.settings.AppInfo
import com.miniclick.calltrackmanage.util.system.WhatsAppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles general application settings extracted from SettingsViewModel.
 */
class GeneralSettingsManager(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val _availableWhatsappApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val availableWhatsappApps: StateFlow<List<AppInfo>> = _availableWhatsappApps.asStateFlow()

    fun fetchWhatsappApps() {
        scope.launch(Dispatchers.IO) {
            val uniqueApps = WhatsAppUtils.fetchAvailableWhatsappApps(application)
            _availableWhatsappApps.update { uniqueApps }
        }
    }

    fun updateThemeMode(mode: String) {
        settingsRepository.setThemeMode(mode)
    }

    fun updateWhatsappPreference(packageName: String) {
        settingsRepository.setWhatsappPreference(packageName)
    }

    fun updateShowDialButton(show: Boolean) {
        settingsRepository.setShowDialButton(show)
    }

    fun updateCallActionBehavior(behavior: String) {
        settingsRepository.setCallActionBehavior(behavior)
    }

    fun isGoogleDialer(): Boolean {
        val telecomManager = application.getSystemService(Application.TELECOM_SERVICE) as? TelecomManager
        return telecomManager?.defaultDialerPackage == "com.google.android.dialer"
    }

    fun isSystemDefaultDialer(): Boolean {
        val telecomManager = application.getSystemService(Application.TELECOM_SERVICE) as? TelecomManager
        return telecomManager?.defaultDialerPackage == application.packageName
    }

    fun updateDialerEnabled(enabled: Boolean) {
        settingsRepository.setDialerEnabled(enabled)
    }
}
