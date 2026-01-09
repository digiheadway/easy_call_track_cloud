package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.ui.settings.SimInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles all SIM-related logic extracted from SettingsViewModel.
 */
class SimManager(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val _availableSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val availableSims: StateFlow<List<SimInfo>> = _availableSims.asStateFlow()

    private val _sim1SubId = MutableStateFlow<Int?>(null)
    val sim1SubId: StateFlow<Int?> = _sim1SubId.asStateFlow()

    private val _sim2SubId = MutableStateFlow<Int?>(null)
    val sim2SubId: StateFlow<Int?> = _sim2SubId.asStateFlow()

    private val _sim1CalibrationHint = MutableStateFlow<String?>("")
    val sim1CalibrationHint: StateFlow<String?> = _sim1CalibrationHint.asStateFlow()

    private val _sim2CalibrationHint = MutableStateFlow<String?>("")
    val sim2CalibrationHint: StateFlow<String?> = _sim2CalibrationHint.asStateFlow()

    init {
        _sim1SubId.value = settingsRepository.getSim1SubscriptionId()
        _sim2SubId.value = settingsRepository.getSim2SubscriptionId()
        _sim1CalibrationHint.value = settingsRepository.getSim1CalibrationHint()
        _sim2CalibrationHint.value = settingsRepository.getSim2CalibrationHint()
    }

    fun fetchSimInfo() {
        scope.launch(Dispatchers.IO) {
            val ctx = application
            if (ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_PHONE_STATE) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return@launch
            }

            try {
                val subscriptionManager = ContextCompat.getSystemService(ctx, SubscriptionManager::class.java)
                val infoList = subscriptionManager?.activeSubscriptionInfoList
                if (infoList != null) {
                    val sims = infoList.map { 
                        SimInfo(
                            slotIndex = it.simSlotIndex, 
                            displayName = it.displayName.toString(), 
                            carrierName = it.carrierName.toString(),
                            subscriptionId = it.subscriptionId
                        )
                    }.sortedBy { it.slotIndex }
                    
                    _availableSims.update { sims }
                    
                    // Update state with current detected IDs
                    sims.find { it.slotIndex == 0 }?.let { sim ->
                        val currentSim1Id = settingsRepository.getSim1SubscriptionId()
                        _sim1SubId.update { currentSim1Id }
                        
                        // Auto-populate phone number if missing
                        if (settingsRepository.getCallerPhoneSim1().isBlank()) {
                            val number = getSimNumber(ctx, sim.subscriptionId)
                            if (!number.isNullOrBlank()) {
                                settingsRepository.setCallerPhoneSim1(number)
                            }
                        }
                    }
                    sims.find { it.slotIndex == 1 }?.let { sim ->
                        val currentSim2Id = settingsRepository.getSim2SubscriptionId()
                        _sim2SubId.update { currentSim2Id }
                        
                        // Auto-populate phone number if missing
                        if (settingsRepository.getCallerPhoneSim2().isBlank()) {
                            val number = getSimNumber(ctx, sim.subscriptionId)
                            if (!number.isNullOrBlank()) {
                                settingsRepository.setCallerPhoneSim2(number)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getSimNumber(ctx: Context, subId: Int): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_PHONE_NUMBERS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return null
        } else if (ActivityCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_PHONE_STATE) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return null

        return try {
            val subscriptionManager = ContextCompat.getSystemService(ctx, SubscriptionManager::class.java)
            if (subscriptionManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    subscriptionManager.getPhoneNumber(subId)
                } else {
                    @Suppress("DEPRECATION")
                    subscriptionManager.getActiveSubscriptionInfo(subId)?.number
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun setSimCalibration(simIndex: Int, subscriptionId: Int, hint: String) {
        if (simIndex == 0) {
            settingsRepository.setSim1SubscriptionId(subscriptionId)
            settingsRepository.setSim1CalibrationHint(hint)
            _sim1SubId.update { subscriptionId }
            _sim1CalibrationHint.update { hint }
        } else if (simIndex == 1) {
            settingsRepository.setSim2SubscriptionId(subscriptionId)
            settingsRepository.setSim2CalibrationHint(hint)
            _sim2SubId.update { subscriptionId }
            _sim2CalibrationHint.update { hint }
        }
        fetchSimInfo()
    }
}
