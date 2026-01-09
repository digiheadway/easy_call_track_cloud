package com.miniclick.calltrackmanage.ui.settings.viewmodel

import android.app.Application
import android.widget.Toast
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles all cloud sync and pairing operations extracted from SettingsViewModel.
 * 
 * Responsibilities:
 * - Pairing code verification
 * - Server communication for sync setup
 * - Organization and user ID management
 * - Plan and quota management
 */
class SyncManager(
    private val application: Application,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    
    /**
     * Validates pairing code format.
     * Returns Pair(orgId, userId) if valid, or null if invalid.
     */
    fun validatePairingCode(code: String): Pair<String, String>? {
        val trimmedCode = code.trim().uppercase()
        
        if (!trimmedCode.contains("-")) {
            showToast("Invalid format. Use: ORGID-USERID")
            return null
        }

        val parts = trimmedCode.split("-", limit = 2)
        if (parts.size != 2) {
            showToast("Invalid format. Use: ORGID-USERID")
            return null
        }
        
        val orgId = parts[0].trim()
        val userId = parts[1].trim()

        if (orgId.isEmpty() || userId.isEmpty()) {
            showToast("Both ORGID and USERID are required")
            return null
        }

        // Validate ORGID format (letters/numbers only)
        if (!orgId.matches(Regex("^[A-Z0-9]+$"))) {
            showToast("ORGID must contain only letters and numbers")
            return null
        }

        // Validate USERID format (numbers only)
        if (!userId.matches(Regex("^[0-9]+$"))) {
            showToast("USERID must be a number")
            return null
        }

        return Pair(orgId, userId)
    }

    /**
     * Verify pairing code with server.
     * Returns verification result via callback.
     */
    fun verifyPairingCode(
        pairingCode: String,
        deviceId: String,
        action: String = "verify_pairing_code",
        onProgress: (Boolean) -> Unit,
        onResult: (success: Boolean, employeeName: String?, settings: com.miniclick.calltrackmanage.network.EmployeeSettingsDto?, plan: com.miniclick.calltrackmanage.network.PlanInfoDto?, error: String?) -> Unit
    ) {
        val validated = validatePairingCode(pairingCode)
        if (validated == null) {
            onResult(false, null, null, null, "Invalid pairing code format")
            return
        }

        val (orgId, userId) = validated

        scope.launch(Dispatchers.IO) {
            onProgress(true)
            try {
                val response = NetworkClient.api.verifyPairingCode(
                    action = action,
                    orgId = orgId,
                    userId = userId,
                    deviceId = deviceId,
                    deviceModel = android.os.Build.MODEL,
                    osVersion = android.os.Build.VERSION.RELEASE,
                    batteryLevel = getBatteryLevel()
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true && body.data != null) {
                            val data = body.data
                            
                            // Update enterprise settings in repository
                            updateEnterpriseSettings(data.settings)
                            updatePlanSettings(data.plan)
                            
                            onResult(
                                true,
                                data.employeeName,
                                data.settings,
                                data.plan,
                                null
                            )
                        } else {
                            onResult(false, null, null, null, body?.error ?: body?.message ?: "Verification failed")
                        }
                    } else {
                        onResult(false, null, null, null, "Server error: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, null, null, null, e.localizedMessage ?: "Network error")
                }
            } finally {
                onProgress(false)
            }
        }
    }

    private fun updateEnterpriseSettings(settings: com.miniclick.calltrackmanage.network.EmployeeSettingsDto) {
        settingsRepository.setAllowPersonalExclusion(settings.allowPersonalExclusion == 1)
        settingsRepository.setAllowChangingTrackStartDate(settings.allowChangingTrackingStartDate == 1)
        settingsRepository.setAllowUpdatingTrackSims(settings.allowUpdatingTrackingSims == 1)
        settingsRepository.setDefaultTrackStartDate(settings.defaultTrackingStartingDate)
        settingsRepository.setCallTrackEnabled(settings.callTrack == 1)
        settingsRepository.setCallRecordEnabled(settings.callRecordCrm == 1)

        // Handle default track start date if not allowed to change
        if (settings.allowChangingTrackingStartDate != 1 && !settings.defaultTrackingStartingDate.isNullOrBlank()) {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                sdf.parse(settings.defaultTrackingStartingDate)?.let { date ->
                    settingsRepository.setTrackStartDate(date.time)
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    private fun updatePlanSettings(plan: com.miniclick.calltrackmanage.network.PlanInfoDto) {
        settingsRepository.setPlanExpiryDate(plan.expiryDate)
        settingsRepository.setAllowedStorageGb(plan.allowedStorageGb)
        settingsRepository.setStorageUsedBytes(plan.storageUsedBytes)
    }

    /**
     * Save verified pairing information to repository.
     */
    fun savePairingInfo(orgId: String, userId: String) {
        settingsRepository.setOrganisationId(orgId)
        settingsRepository.setUserId(userId)
    }

    /**
     * Clear pairing information from repository.
     */
    fun clearPairingInfo() {
        settingsRepository.setOrganisationId("")
        settingsRepository.setUserId("")
    }

    /**
     * Check if sync is set up.
     */
    fun isSyncSetup(): Boolean {
        return settingsRepository.getOrganisationId().isNotEmpty()
    }

    /**
     * Get the current pairing code if set.
     */
    fun getCurrentPairingCode(): String {
        val orgId = settingsRepository.getOrganisationId()
        val userId = settingsRepository.getUserId()
        return if (orgId.isNotEmpty() && userId.isNotEmpty()) "$orgId-$userId" else ""
    }

    private fun getBatteryLevel(): Int {
        return try {
            val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                application.registerReceiver(null, ifilter)
            }
            val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        } catch (e: Exception) {
            -1
        }
    }

    private fun showToast(message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
        }
    }
}
