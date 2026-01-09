package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LookupViewModel @Inject constructor(
    application: Application,
    settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val lookupManager = LookupManager(
        settingsRepository,
        viewModelScope
    )

    val customLookupResponse: StateFlow<String?> = lookupManager.customLookupResponse
    val isFetchingCustomLookup: StateFlow<Boolean> = lookupManager.isFetchingCustomLookup

    fun fetchCustomLookup(url: String) {
        lookupManager.fetchCustomLookup(url)
    }

    fun fetchCustomLookupForPhone(phoneNumber: String) {
        lookupManager.fetchCustomLookupForPhone(phoneNumber)
    }

    fun clearCustomLookupResponse() {
        lookupManager.clearCustomLookupResponse()
    }

    fun updateCustomLookupUrl(url: String) {
        lookupManager.updateCustomLookupUrl(url)
    }

    fun updateCustomLookupEnabled(enabled: Boolean) {
        lookupManager.updateCustomLookupEnabled(enabled)
    }

    fun updateCustomLookupCallerIdEnabled(enabled: Boolean) {
        lookupManager.updateCustomLookupCallerIdEnabled(enabled)
    }
}
