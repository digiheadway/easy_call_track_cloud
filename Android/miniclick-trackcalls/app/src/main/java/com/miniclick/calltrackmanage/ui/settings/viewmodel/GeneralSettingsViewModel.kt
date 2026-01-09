package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.ui.settings.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    application: Application,
    settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val generalSettingsManager = GeneralSettingsManager(
        application,
        settingsRepository,
        viewModelScope
    )

    val availableWhatsappApps: StateFlow<List<AppInfo>> = generalSettingsManager.availableWhatsappApps

    fun fetchWhatsappApps() {
        generalSettingsManager.fetchWhatsappApps()
    }

    fun updateThemeMode(mode: String) {
        generalSettingsManager.updateThemeMode(mode)
    }

    fun updateWhatsappPreference(packageName: String) {
        generalSettingsManager.updateWhatsappPreference(packageName)
    }

    fun updateShowDialButton(show: Boolean) {
        generalSettingsManager.updateShowDialButton(show)
    }

    fun updateCallActionBehavior(behavior: String) {
        generalSettingsManager.updateCallActionBehavior(behavior)
    }

    fun isGoogleDialer(): Boolean = generalSettingsManager.isGoogleDialer()

    fun isSystemDefaultDialer(): Boolean = generalSettingsManager.isSystemDefaultDialer()

    fun updateDialerEnabled(enabled: Boolean) {
        generalSettingsManager.updateDialerEnabled(enabled)
    }
}
