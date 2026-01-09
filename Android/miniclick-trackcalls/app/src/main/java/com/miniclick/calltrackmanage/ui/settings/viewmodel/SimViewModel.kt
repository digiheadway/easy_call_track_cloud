package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.ui.settings.SimInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SimViewModel @Inject constructor(
    application: Application,
    settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val simManager = SimManager(
        application,
        settingsRepository,
        viewModelScope
    )

    val availableSims: StateFlow<List<SimInfo>> = simManager.availableSims
    val sim1SubId: StateFlow<Int?> = simManager.sim1SubId
    val sim2SubId: StateFlow<Int?> = simManager.sim2SubId

    fun fetchSimInfo() {
        simManager.fetchSimInfo()
    }

    fun setSimCalibration(simIndex: Int, subscriptionId: Int, hint: String) {
        simManager.setSimCalibration(simIndex, subscriptionId, hint)
    }
}
