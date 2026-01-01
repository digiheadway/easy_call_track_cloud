package com.calltracker.manager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calltracker.manager.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    private val _themeMode = MutableStateFlow("System") // "System", "Light", "Dark"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _themeMode.value = settingsRepository.getThemeMode()
    }

    // You might want to refresh this if it changes elsewhere.
    // Ideally SettingsRepository would expose a Flow, but it uses SharedPreferences.
    // For now, we can rely on onResume to refresh it in MainActivity or make SettingsRepository expose a Flow.
    // Since MainActivity has onResume, we can call refreshTheme() there.
    
    fun refreshTheme() {
        val current = settingsRepository.getThemeMode()
        if (_themeMode.value != current) {
            _themeMode.value = current
        }
    }
}
