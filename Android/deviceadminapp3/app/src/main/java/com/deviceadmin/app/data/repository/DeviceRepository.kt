package com.deviceadmin.app.data.repository

import android.util.Log
import com.deviceadmin.app.data.local.PreferencesManager
import com.deviceadmin.app.data.model.DeviceState
import com.deviceadmin.app.data.model.DeviceStatusResponse
import com.deviceadmin.app.data.model.PhoneState
import com.deviceadmin.app.data.model.ProtectionState
import com.deviceadmin.app.data.remote.ApiClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing device state.
 * Acts as single source of truth, combining local storage and remote API.
 */
class DeviceRepository(private val preferencesManager: PreferencesManager) {

    companion object {
        private const val TAG = "DeviceRepository"
    }

    private val apiService = ApiClientFactory.deviceApiService

    /**
     * Gets the current device state from local storage.
     */
    fun getLocalState(): DeviceState {
        return preferencesManager.getDeviceState()
    }

    /**
     * Fetches device status from server and updates local state.
     * @return Result containing the updated state or an error.
     */
    suspend fun syncWithServer(): Result<DeviceState> = withContext(Dispatchers.IO) {
        val deviceId = preferencesManager.getDeviceId()
        
        if (deviceId.isEmpty()) {
            Log.w(TAG, "Cannot sync - no device ID configured")
            return@withContext Result.failure(IllegalStateException("No device ID configured"))
        }

        try {
            Log.d(TAG, "Fetching status for device: $deviceId")
            val response = apiService.getDeviceStatus(deviceId)
            
            if (response.isSuccessful) {
                val statusResponse = response.body()
                if (statusResponse != null) {
                    updateLocalStateFromServer(statusResponse)
                    Log.d(TAG, "Successfully synced with server")
                    return@withContext Result.success(preferencesManager.getDeviceState())
                }
            }
            
            Log.e(TAG, "Server error: ${response.code()} - ${response.message()}")
            Result.failure(Exception("Server error: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Network error during sync", e)
            Result.failure(e)
        }
    }

    /**
     * Updates local state from server response.
     */
    private fun updateLocalStateFromServer(response: DeviceStatusResponse) {
        // Get current state as fallback
        val currentState = preferencesManager.getDeviceState()
        
        // Update phone state
        val phoneState = when {
            response.isFreezed == true -> PhoneState.FROZEN
            response.isFreezed == false -> PhoneState.ACTIVE
            else -> currentState.phoneState // Keep current if null
        }
        preferencesManager.setPhoneState(phoneState)
        
        // Update protection state
        val protectionState = when {
            response.isProtected == true -> ProtectionState.ENABLED
            response.isProtected == false -> ProtectionState.DISABLED
            else -> currentState.protectionState // Keep current if null
        }
        preferencesManager.setProtectionState(protectionState)
        
        // Update EMI information
        response.amount?.let { preferencesManager.setEmiAmount(it) }
        preferencesManager.setMessage(response.message)
        preferencesManager.setCallToNumber(response.callTo)
        
        // Update uninstall allowed based on protection
        preferencesManager.setUninstallAllowed(protectionState == ProtectionState.DISABLED)
        
        Log.d(TAG, "State updated - Phone: $phoneState, Protection: $protectionState")
    }

    /**
     * Checks if an app update is available.
     */
    fun checkForUpdate(response: DeviceStatusResponse, currentVersionCode: Int): UpdateInfo? {
        val serverVersion = response.appVersion ?: return null
        val updateUrl = response.updateUrl ?: return null
        
        return if (serverVersion > currentVersionCode) {
            UpdateInfo(serverVersion, updateUrl)
        } else {
            null
        }
    }

    /**
     * Checks if auto-uninstall is requested by server.
     */
    fun isAutoUninstallRequested(response: DeviceStatusResponse): Boolean {
        return response.autoUninstall == true
    }

    // ===== Local State Management =====

    fun saveDeviceId(deviceId: String, backupToGlobal: Boolean = false) {
        preferencesManager.setDeviceId(deviceId)
        if (backupToGlobal) {
            preferencesManager.backupDeviceIdToGlobal(deviceId)
        }
    }

    fun completeSetup() {
        preferencesManager.setSetupComplete(true)
        preferencesManager.setUninstallAllowed(false)
        preferencesManager.setProtectionState(ProtectionState.ENABLED)
    }

    fun removeProtection() {
        preferencesManager.setSetupComplete(false)
        preferencesManager.setUninstallAllowed(true)
        preferencesManager.setProtectionState(ProtectionState.DISABLED)
        preferencesManager.setPhoneState(PhoneState.ACTIVE)
    }

    fun startBreak(durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        preferencesManager.setBreakEndTime(endTime)
    }

    fun isOnBreak(): Boolean {
        return preferencesManager.isOnBreak()
    }
}

/**
 * Information about an available app update.
 */
data class UpdateInfo(
    val versionCode: Int,
    val downloadUrl: String
)
