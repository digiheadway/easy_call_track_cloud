package com.miniclick.calltrackmanage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@dagger.hilt.android.lifecycle.HiltViewModel
class MainViewModel @javax.inject.Inject constructor(
    application: Application,
    private val settingsRepository: com.miniclick.calltrackmanage.data.SettingsRepository
) : AndroidViewModel(application) {

    private val _themeMode = MutableStateFlow("System") // "System", "Light", "Dark"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _lookupPhoneNumber = MutableStateFlow<String?>(null)
    val lookupPhoneNumber: StateFlow<String?> = _lookupPhoneNumber.asStateFlow()

    private val _personDetailsPhone = MutableStateFlow<String?>(null)
    val personDetailsPhone: StateFlow<String?> = _personDetailsPhone.asStateFlow()

    private val _dialerInitialNumber = MutableStateFlow<String?>(null)
    val dialerInitialNumber: StateFlow<String?> = _dialerInitialNumber.asStateFlow()

    private val _isSessionOnboardingDismissed = MutableStateFlow(false)
    val isSessionOnboardingDismissed: StateFlow<Boolean> = _isSessionOnboardingDismissed.asStateFlow()

    private val _selectedTab = MutableStateFlow(
        try {
            AppTab.valueOf(settingsRepository.getSelectedTab())
        } catch (e: Exception) {
            AppTab.CALLS
        }
    )
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()
    // Observe onboarding completion status
    val onboardingCompleted: StateFlow<Boolean> = settingsRepository.getOnboardingCompletedFlow()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.isOnboardingCompleted()
        )


    val agreementAccepted: StateFlow<Boolean> = settingsRepository.getAgreementAcceptedFlow()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = settingsRepository.isAgreementAccepted()
        )

    fun setAgreementAccepted(accepted: Boolean) {
        settingsRepository.setAgreementAccepted(accepted)
    }

    // Flag to track if we've auto-shown the cloud sync modal this session
    var hasShownCloudSyncPrompt = false
        private set

    fun markCloudSyncPromptShown() {
        hasShownCloudSyncPrompt = true
    }

    init {
        // STARTUP OPTIMIZATION: Load theme immediately (small read) for proper theming
        _themeMode.value = settingsRepository.getThemeMode()
        
        // STARTUP OPTIMIZATION: Defer Flow collection until after first frame
        viewModelScope.launch {
            kotlinx.coroutines.delay(50)
            
            // Initialize from repo and observe changes for consistency
            settingsRepository.getOnboardingCompletedFlow().collect {
                if (settingsRepository.isOnboardingOffline()) {
                    _isSessionOnboardingDismissed.value = true
                }
            }
        }
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

    fun setPersonDetailsPhone(phone: String?) {
        _personDetailsPhone.value = phone
    }

    fun setDialerNumber(number: String?) {
        _dialerInitialNumber.value = number
    }

    fun dismissOnboardingSession() {
        _isSessionOnboardingDismissed.value = true
        settingsRepository.setOnboardingOffline(true)
    }

    fun resetOnboardingSession() {
        _isSessionOnboardingDismissed.value = false
        settingsRepository.setOnboardingOffline(false)
    }

    fun setSelectedTab(tab: AppTab) {
        _selectedTab.value = tab
        settingsRepository.setSelectedTab(tab.name)
    }
    
    // Notification-triggered queue opening
    private val _openSyncQueue = MutableStateFlow(false)
    val openSyncQueue: StateFlow<Boolean> = _openSyncQueue.asStateFlow()
    
    private val _openRecordingQueue = MutableStateFlow(false)
    val openRecordingQueue: StateFlow<Boolean> = _openRecordingQueue.asStateFlow()
    
    fun setOpenSyncQueue(open: Boolean) {
        _openSyncQueue.value = open
    }
    
    fun setOpenRecordingQueue(open: Boolean) {
        _openRecordingQueue.value = open
    }
}
