package com.miniclick.calltrackmanage.ui.settings.viewmodel

import com.google.gson.GsonBuilder
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles custom phone lookup settings extracted from SettingsViewModel.
 */
class LookupManager(
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val _customLookupResponse = MutableStateFlow<String?>(null)
    val customLookupResponse: StateFlow<String?> = _customLookupResponse.asStateFlow()

    private val _isFetchingCustomLookup = MutableStateFlow(false)
    val isFetchingCustomLookup: StateFlow<Boolean> = _isFetchingCustomLookup.asStateFlow()

    fun fetchCustomLookup(url: String) {
        scope.launch {
            _isFetchingCustomLookup.update { true }
            _customLookupResponse.update { "Fetching..." }
            try {
                val response = NetworkClient.api.fetchData(url)
                if (response.isSuccessful) {
                    val body = response.body()
                    val json = GsonBuilder().setPrettyPrinting().create().toJson(body)
                    _customLookupResponse.update { json }
                } else {
                    val error = "Error: ${response.code()} ${response.message()}"
                    _customLookupResponse.update { error }
                }
            } catch (e: Exception) {
                _customLookupResponse.update { "Error: ${e.localizedMessage}" }
            } finally {
                _isFetchingCustomLookup.update { false }
            }
        }
    }

    fun fetchCustomLookupForPhone(phoneNumber: String) {
        val baseUrl = settingsRepository.getCustomLookupUrl().ifEmpty { 
            "https://prop.digiheadway.in/api/calls/caller_id.php?phone={phone}"
        }
        val url = if (baseUrl.contains("{phone}")) {
            baseUrl.replace("{phone}", phoneNumber)
        } else if (baseUrl.contains("phone=")) {
            if (baseUrl.endsWith("=")) baseUrl + phoneNumber else baseUrl
        } else {
            val separator = if (baseUrl.contains("?")) "&" else "?"
            baseUrl + separator + "phone=" + phoneNumber
        }
        fetchCustomLookup(url)
    }

    fun clearCustomLookupResponse() {
        _customLookupResponse.update { null }
    }

    fun updateCustomLookupUrl(url: String) {
        settingsRepository.setCustomLookupUrl(url)
    }

    fun updateCustomLookupEnabled(enabled: Boolean) {
        settingsRepository.setCustomLookupEnabled(enabled)
    }

    fun updateCustomLookupCallerIdEnabled(enabled: Boolean) {
        settingsRepository.setCustomLookupCallerIdEnabled(enabled)
    }
}
