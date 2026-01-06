package com.deviceadmin.app.ui.setup

import android.app.DownloadManager
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.deviceadmin.app.R
import com.deviceadmin.app.data.local.PreferencesManager
import com.deviceadmin.app.data.model.PhoneState
import com.deviceadmin.app.data.model.ProtectionState
import com.deviceadmin.app.data.repository.DeviceRepository
import com.deviceadmin.app.databinding.ActivitySetupBinding
import com.deviceadmin.app.receiver.DeviceAdminReceiver
import com.deviceadmin.app.ui.lock.LockScreenActivity
import com.deviceadmin.app.util.AppIconManager
import com.deviceadmin.app.util.Constants
import com.deviceadmin.app.util.DevicePolicyHelper
import com.deviceadmin.app.util.PermissionHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Setup Activity - Main entry point and admin control panel.
 * Handles device registration, permission setup, and protection management.
 */
class SetupActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SetupActivity"
    }

    private lateinit var binding: ActivitySetupBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: DeviceRepository
    private lateinit var policyHelper: DevicePolicyHelper

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionUI()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, ">>> SetupActivity.onCreate() STARTED")
        try {
            super.onCreate(savedInstanceState)
            Log.d(TAG, ">>> super.onCreate() completed")
            
            binding = ActivitySetupBinding.inflate(layoutInflater)
            Log.d(TAG, ">>> binding inflated")
            
            setContentView(binding.root)
            Log.d(TAG, ">>> setContentView completed")

            initDependencies()
            Log.d(TAG, ">>> initDependencies completed")
            
            setupClickListeners()
            Log.d(TAG, ">>> setupClickListeners completed")
            
            handleLaunchIntent()
            Log.d(TAG, ">>> handleLaunchIntent completed")
            
            Log.d(TAG, ">>> SetupActivity.onCreate() COMPLETED SUCCESSFULLY")
        } catch (e: Exception) {
            Log.e(TAG, ">>> CRASH in SetupActivity.onCreate()", e)
            throw e
        }
    }

    private fun initDependencies() {
        Log.d(TAG, ">>> initDependencies() started")
        try {
            prefsManager = PreferencesManager(this)
            Log.d(TAG, ">>> PreferencesManager created")
            
            repository = DeviceRepository(prefsManager)
            Log.d(TAG, ">>> DeviceRepository created")
            
            policyHelper = DevicePolicyHelper(this)
            Log.d(TAG, ">>> DevicePolicyHelper created")
        } catch (e: Exception) {
            Log.e(TAG, ">>> CRASH in initDependencies()", e)
            throw e
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUI()
    }

    private fun handleLaunchIntent() {
        val showAdminPanel = intent.getBooleanExtra(Constants.EXTRA_SHOW_ADMIN_PANEL, false)
        val state = prefsManager.getDeviceState()

        // Check if device should be locked
        if (state.isSetupComplete && state.phoneState == PhoneState.FROZEN && 
            state.protectionState == ProtectionState.ENABLED && !showAdminPanel) {
            navigateToLockScreen()
            return
        }

        // Perform server sync if setup is complete
        if (state.isSetupComplete && !showAdminPanel) {
            performServerSync()
        } else {
            showSetupUI(state)
        }
    }

    private fun performServerSync() {
        binding.setupContainer.visibility = View.GONE
        
        lifecycleScope.launch {
            val result = repository.syncWithServer()
            
            result.onSuccess { state ->
                if (state.phoneState == PhoneState.FROZEN && 
                    state.protectionState == ProtectionState.ENABLED) {
                    navigateToLockScreen()
                } else if (state.protectionState == ProtectionState.DISABLED) {
                    showSetupUI(state)
                } else {
                    openDownloadsAndFinish()
                }
            }.onFailure {
                // Network error - use local state
                openDownloadsAndFinish()
            }
        }
    }

    private fun showSetupUI(state: com.deviceadmin.app.data.model.DeviceState) {
        binding.setupContainer.visibility = View.VISIBLE
        animateEntrance()
        
        // Show device ID state
        val deviceId = state.deviceId
        if (deviceId.isNotEmpty()) {
            showSavedDeviceId(deviceId)
        } else {
            showDeviceIdInput()
        }
        
        // Show admin zone if setup is complete
        binding.adminZoneContainer.visibility = if (state.isSetupComplete) View.VISIBLE else View.GONE
        
        // Hide icon toggle on Android 11+
        binding.hideIconCard.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            View.GONE
        } else {
            View.VISIBLE
        }
        
        updatePermissionUI()
        updateProtectionButton()
    }

    private fun setupClickListeners() {
        // Permission rows
        binding.permissionDeviceAdmin.setOnClickListener {
            animateClick(it)
            if (!policyHelper.isDeviceAdmin()) {
                requestDeviceAdmin()
            }
        }

        binding.permissionAccessibility.setOnClickListener {
            animateClick(it)
            if (!PermissionHelper.isAccessibilityServiceEnabled(this)) {
                requestAccessibilityPermission()
            }
        }

        binding.permissionOverlay.setOnClickListener {
            animateClick(it)
            if (!PermissionHelper.isOverlayPermissionGranted(this)) {
                requestOverlayPermission()
            }
        }

        // Save device ID
        binding.saveDeviceIdButton.setOnClickListener {
            animateClick(it)
            saveDeviceId()
        }

        // Edit device ID
        binding.editDeviceIdButton.setOnClickListener {
            animateClick(it)
            showDeviceIdInput()
        }

        // Complete setup
        binding.completeSetupButton.setOnClickListener {
            animateClick(it)
            completeSetup()
        }

        // Test lock screen
        binding.testLockButton.setOnClickListener {
            animateClick(it)
            testLockScreen()
        }

        // Remove protection
        binding.removeSetupButton.setOnClickListener {
            animateClick(it)
            showRemoveProtectionDialog()
        }
    }

    // ===== Device ID Management =====

    private fun saveDeviceId() {
        val deviceId = binding.deviceIdInput.text?.toString()?.trim()

        if (deviceId.isNullOrEmpty()) {
            binding.deviceIdInputLayout.error = "Please enter a Device ID"
            return
        }

        binding.deviceIdInputLayout.error = null
        repository.saveDeviceId(deviceId, policyHelper.isDeviceOwner())
        showSavedDeviceId(deviceId)
        Toast.makeText(this, "Device ID saved", Toast.LENGTH_SHORT).show()
    }

    private fun showSavedDeviceId(deviceId: String) {
        binding.deviceIdInputCard.visibility = View.GONE
        binding.deviceIdSavedCard.visibility = View.VISIBLE
        binding.savedDeviceIdText.text = deviceId
        binding.editDeviceIdButton.visibility = if (prefsManager.isSetupComplete()) View.GONE else View.VISIBLE
        updateProtectionButton()
    }

    private fun showDeviceIdInput() {
        val currentId = binding.savedDeviceIdText.text.toString()
        binding.deviceIdSavedCard.visibility = View.GONE
        binding.deviceIdInputCard.visibility = View.VISIBLE
        binding.deviceIdInput.setText(currentId)
        binding.deviceIdInput.requestFocus()
        updateProtectionButton()
    }

    // ===== Permission Handling =====

    private fun updatePermissionUI() {
        updatePermissionBadge(
            binding.permissionDeviceAdminStatus,
            policyHelper.isDeviceAdmin()
        )
        updatePermissionBadge(
            binding.permissionAccessibilityStatus,
            PermissionHelper.isAccessibilityServiceEnabled(this)
        )
        updatePermissionBadge(
            binding.permissionOverlayStatus,
            PermissionHelper.isOverlayPermissionGranted(this)
        )
        updateProtectionButton()
    }

    private fun updatePermissionBadge(view: android.widget.TextView, granted: Boolean) {
        if (granted) {
            view.text = "Granted"
            view.setTextColor(getColor(R.color.success))
            view.setBackgroundResource(R.drawable.badge_granted)
        } else {
            view.text = "Grant"
            view.setTextColor(getColor(R.color.error))
            view.setBackgroundResource(R.drawable.badge_pending)
        }
    }

    private fun updateProtectionButton() {
        val hasAllPermissions = policyHelper.isDeviceAdmin() &&
                PermissionHelper.isAccessibilityServiceEnabled(this) &&
                PermissionHelper.isOverlayPermissionGranted(this)
        
        val hasDeviceId = prefsManager.getDeviceId().isNotEmpty()
        val canComplete = hasAllPermissions && hasDeviceId

        binding.completeSetupButton.isEnabled = canComplete
        binding.completeSetupButton.alpha = if (canComplete) 1f else 0.5f
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, policyHelper.adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for device protection.")
        }
        deviceAdminLauncher.launch(intent)
    }

    private fun requestAccessibilityPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Accessibility Required")
            .setMessage("Enable Accessibility Service for protection features.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestOverlayPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Overlay Required")
            .setMessage("Allow 'Display over other apps' to show lock screen.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ===== Setup Completion =====

    private fun completeSetup() {
        repository.completeSetup()
        policyHelper.enableProtection()
        scheduleBackgroundWork()

        val shouldHide = binding.hideIconSwitch.isChecked
        
        MaterialAlertDialogBuilder(this)
            .setTitle("🛡️ Protection Added")
            .setMessage("Device is now protected. The app will disguise as 'My Downloads'.")
            .setPositiveButton("OK") { _, _ ->
                if (shouldHide && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    AppIconManager.hideIcon(this)
                }
                finishAndRemoveTask()
            }
            .setCancelable(false)
            .show()
    }

    private fun scheduleBackgroundWork() {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.deviceadmin.app.worker.StatusSyncWorker>(
            Constants.STATUS_WORK_INTERVAL_MINUTES,
            java.util.concurrent.TimeUnit.MINUTES
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.STATUS_WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        // Run immediate check
        androidx.work.WorkManager.getInstance(this).enqueue(
            androidx.work.OneTimeWorkRequestBuilder<com.deviceadmin.app.worker.StatusSyncWorker>().build()
        )
    }

    // ===== Admin Zone =====

    private fun testLockScreen() {
        prefsManager.setPhoneState(PhoneState.FROZEN)
        navigateToLockScreen()
    }

    private fun showRemoveProtectionDialog() {
        val state = prefsManager.getDeviceState()
        
        // If protection already disabled by server, skip PIN
        if (state.protectionState == ProtectionState.DISABLED) {
            removeProtection()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_pin_input, null)
        val pinInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.pinInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("🔐 Remove Protection")
            .setMessage("Enter Master PIN to remove protection:")
            .setView(dialogView as View)
            .setPositiveButton("Verify") { _, _ ->
                if (pinInput.text.toString() == Constants.MASTER_PIN) {
                    removeProtection()
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeProtection() {
        repository.removeProtection()
        policyHelper.disableProtection()
        AppIconManager.showIcon(this)
        
        showSetupUI(prefsManager.getDeviceState())

        MaterialAlertDialogBuilder(this)
            .setTitle("✅ Protection Removed")
            .setMessage("You can now uninstall the app from Settings.")
            .setPositiveButton("Open App Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ===== Navigation =====

    private fun navigateToLockScreen() {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun openDownloadsAndFinish() {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to file picker
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivity(intent)
            } catch (_: Exception) { }
        }
        finishAndRemoveTask()
    }

    // ===== Animations =====

    private fun animateEntrance() {
        binding.setupContainer.alpha = 0f
        binding.setupContainer.translationY = 50f
        binding.setupContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .start()
    }

    private fun animateClick(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
}
