package com.deviceadmin.app.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.deviceadmin.app.data.local.PreferencesManager
import com.deviceadmin.app.data.model.PhoneState
import com.deviceadmin.app.data.model.ProtectionState
import com.deviceadmin.app.data.remote.ApiClientFactory
import com.deviceadmin.app.ui.lock.LockScreenActivity
import com.deviceadmin.app.util.AppIconManager
import com.deviceadmin.app.util.Constants
import com.deviceadmin.app.util.DevicePolicyHelper

/**
 * Background worker for syncing device status with the server.
 * Runs every 15 minutes and handles lock/unlock, protection changes, etc.
 */
class StatusSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "StatusSyncWorker"
    }

    private val prefsManager = PreferencesManager(applicationContext)
    private val policyHelper = DevicePolicyHelper(applicationContext)
    private val apiService = ApiClientFactory.deviceApiService

    override suspend fun doWork(): Result {
        // Only run if setup is complete
        if (!prefsManager.isSetupComplete()) {
            Log.d(TAG, "Setup not complete - skipping sync")
            return Result.success()
        }

        val deviceId = prefsManager.getDeviceId()
        if (deviceId.isEmpty()) {
            Log.w(TAG, "No device ID - skipping sync")
            return Result.success()
        }

        return try {
            Log.d(TAG, "Fetching status for device: $deviceId")
            val response = apiService.getDeviceStatus(deviceId)

            if (response.isSuccessful) {
                response.body()?.let { status ->
                    handleStatusResponse(status)
                }
                Result.success()
            } else {
                Log.e(TAG, "Server error: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error", e)
            Result.retry()
        }
    }

    private fun handleStatusResponse(status: com.deviceadmin.app.data.model.DeviceStatusResponse) {
        Log.d(TAG, "Processing status - frozen: ${status.isFreezed}, protected: ${status.isProtected}")
        
        // Handle auto-uninstall request first
        if (status.autoUninstall == true) {
            Log.w(TAG, "Auto-uninstall requested by server")
            performSelfUninstall()
            return
        }

        // Get current state as fallback
        val currentState = prefsManager.getDeviceState()
        
        // Determine new states
        val newPhoneState = when {
            status.isFreezed == true -> PhoneState.FROZEN
            status.isFreezed == false -> PhoneState.ACTIVE
            else -> currentState.phoneState
        }
        
        val newProtectionState = when {
            status.isProtected == true -> ProtectionState.ENABLED
            status.isProtected == false -> ProtectionState.DISABLED
            else -> currentState.protectionState
        }
        
        // Update preferences
        prefsManager.setPhoneState(newPhoneState)
        prefsManager.setProtectionState(newProtectionState)
        prefsManager.setUninstallAllowed(newProtectionState == ProtectionState.DISABLED)
        
        status.amount?.let { prefsManager.setEmiAmount(it) }
        prefsManager.setMessage(status.message)
        prefsManager.setCallToNumber(status.callTo)
        
        // Apply device policy changes
        applyPolicyChanges(newProtectionState)
        
        // Handle lock/unlock
        if (newPhoneState == PhoneState.FROZEN && newProtectionState == ProtectionState.ENABLED) {
            if (!prefsManager.isOnBreak()) {
                launchLockScreen()
            }
        } else if (newPhoneState == PhoneState.ACTIVE || newProtectionState == ProtectionState.DISABLED) {
            unlockDevice()
        }
        
        // Check for updates
        status.updateUrl?.let { url ->
            status.appVersion?.let { version ->
                checkAndApplyUpdate(url, version)
            }
        }
    }

    private fun applyPolicyChanges(protection: ProtectionState) {
        if (protection == ProtectionState.ENABLED) {
            policyHelper.enableProtection()
        } else {
            policyHelper.disableProtection()
        }
    }

    private fun launchLockScreen() {
        Log.d(TAG, "Launching lock screen")
        val intent = Intent(applicationContext, LockScreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        applicationContext.startActivity(intent)
    }

    private fun unlockDevice() {
        Log.d(TAG, "Unlocking device")
        
        // Show app icon
        AppIconManager.showIcon(applicationContext)
        
        // Broadcast unlock
        val intent = Intent(Constants.ACTION_UNLOCK_DEVICE)
        applicationContext.sendBroadcast(intent)
    }

    private fun checkAndApplyUpdate(updateUrl: String, serverVersion: Int) {
        try {
            val pInfo = applicationContext.packageManager.getPackageInfo(
                applicationContext.packageName, 0
            )
            val currentVersion = pInfo.longVersionCode.toInt()
            
            if (serverVersion > currentVersion) {
                Log.i(TAG, "Update available: $currentVersion -> $serverVersion")
                // TODO: Implement silent update download
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
        }
    }

    private fun performSelfUninstall() {
        Log.i(TAG, "Performing self-uninstall")
        
        // Clear preferences
        prefsManager.clearAll()
        
        // Disable protection first
        policyHelper.disableProtection()
        
        // TODO: Implement silent uninstall via PackageInstaller
    }
}
