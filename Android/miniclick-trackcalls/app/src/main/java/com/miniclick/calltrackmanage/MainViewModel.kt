package com.miniclick.calltrackmanage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository.getInstance(application)

    private val _themeMode = MutableStateFlow("System") // "System", "Light", "Dark"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _lookupPhoneNumber = MutableStateFlow<String?>(null)
    val lookupPhoneNumber: StateFlow<String?> = _lookupPhoneNumber.asStateFlow()

    private val _isSessionOnboardingDismissed = MutableStateFlow(false)
    val isSessionOnboardingDismissed: StateFlow<Boolean> = _isSessionOnboardingDismissed.asStateFlow()

    // Flag to track if we've auto-shown the cloud sync modal this session
    var hasShownCloudSyncPrompt = false
        private set

    fun markCloudSyncPromptShown() {
        hasShownCloudSyncPrompt = true
    }

    init {
        loadSettings()
        _isSessionOnboardingDismissed.value = settingsRepository.isOnboardingOffline()
    }

    private fun loadSettings() {
        _themeMode.value = settingsRepository.getThemeMode()
    }
    
    fun refreshTheme() {
        val current = settingsRepository.getThemeMode()
        if (_themeMode.value != current) {
            _themeMode.value = current
        }
    }

    fun setLookupPhoneNumber(phone: String) {
        _lookupPhoneNumber.value = phone
    }

    fun clearLookupPhoneNumber() {
        _lookupPhoneNumber.value = null
    }

    fun dismissOnboardingSession() {
        _isSessionOnboardingDismissed.value = true
        settingsRepository.setOnboardingOffline(true)
    }

    fun resetOnboardingSession() {
        _isSessionOnboardingDismissed.value = false
        settingsRepository.setOnboardingOffline(false)
    }
}
