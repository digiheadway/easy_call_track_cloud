package com.deviceadmin.app.ui.lock

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.deviceadmin.app.data.local.PreferencesManager
import com.deviceadmin.app.data.model.PhoneState
import com.deviceadmin.app.data.model.ProtectionState
import com.deviceadmin.app.data.repository.DeviceRepository
import com.deviceadmin.app.databinding.ActivityLockScreenBinding
import com.deviceadmin.app.ui.setup.SetupActivity
import com.deviceadmin.app.util.AppIconManager
import com.deviceadmin.app.util.Constants
import com.deviceadmin.app.util.DevicePolicyHelper
import android.Manifest
import android.widget.Toast
import kotlinx.coroutines.launch

/**
 * Lock Screen Activity - Displays when device is frozen.
 * Provides EMI info, status check, call support, and master PIN unlock.
 */
class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: DeviceRepository
    private lateinit var policyHelper: DevicePolicyHelper

    private val handler = Handler(Looper.getMainLooper())
    private var isUnlocked = false
    private var isOnBreak = false
    private var isLockTaskActive = false
    private var isBreakPinMode = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleUnlock("Server")
        }
    }

    private val bringToFrontRunnable = object : Runnable {
        override fun run() {
            if (!isUnlocked && !isFinishing && !isOnBreak) {
                bringToFront()
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("LockScreenActivity", ">>> onCreate VERSION 4 STARTED")
        super.onCreate(savedInstanceState)
        Log.d("LockScreenActivity", ">>> super.onCreate() done")
        
        Toast.makeText(this, "LockScreen VERSION 4", Toast.LENGTH_SHORT).show()
        
        try {
            binding = ActivityLockScreenBinding.inflate(layoutInflater)
            Log.d("LockScreenActivity", ">>> binding inflated")
            setContentView(binding.root)
            Log.d("LockScreenActivity", ">>> setContentView done")
        } catch (e: Exception) {
            Log.e("LockScreenActivity", ">>> CRASH during inflation/setContentView", e)
        }

        try {
            configureWindowFlags()
            Log.d("LockScreenActivity", ">>> configureWindowFlags done")
        } catch (e: Exception) {
            Log.e("LockScreenActivity", ">>> Error in configureWindowFlags", e)
        }

        initDependencies()
        Log.d("LockScreenActivity", ">>> initDependencies done")
        loadDisplayData()
        Log.d("LockScreenActivity", ">>> loadDisplayData done")
        setupClickListeners()
        Log.d("LockScreenActivity", ">>> setupClickListeners done")
        registerUnlockReceiver()
        Log.d("LockScreenActivity", ">>> registerUnlockReceiver done")
        initLockTaskMode()
        Log.d("LockScreenActivity", ">>> initLockTaskMode done")
        
        // Auto-check status on launch
        checkServerStatus()
        Log.d("LockScreenActivity", ">>> checkServerStatus called")
    }

    private fun initDependencies() {
        prefsManager = PreferencesManager(this)
        repository = DeviceRepository(prefsManager)
        policyHelper = DevicePolicyHelper(this)
    }

    private fun configureWindowFlags() {
        // Show when locked and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    private fun loadDisplayData() {
        val state = prefsManager.getDeviceState()

        // Set EMI message
        binding.emiMessage.text = state.message ?: "Due to Pending EMI Amount of ₹${state.emiAmount}"

        // Set call support button
        val callTo = state.callToNumber
        if (!callTo.isNullOrEmpty()) {
            binding.callSupportButton.visibility = View.VISIBLE
            binding.callSupportButton.text = "Call Support: $callTo"
        } else {
            binding.callSupportButton.visibility = View.GONE
        }

        // Set device ID
        binding.deviceIdDisplay.text = "Device ID: ${state.deviceId.ifEmpty { "Unknown" }}"
    }

    private fun setupClickListeners() {
        // Check Status
        binding.checkStatusButton.setOnClickListener {
            checkServerStatus()
        }

        // Call Support
        binding.callSupportButton.setOnClickListener {
            val number = prefsManager.getCallToNumber()
            if (!number.isNullOrEmpty()) {
                makePhoneCall(number)
            }
        }

        // Break Button
        binding.breakButton.setOnClickListener {
            isBreakPinMode = true
            showPinInput("Enter PIN for Break", "Start ${Constants.BREAK_DURATION_MINUTES} Min Break")
        }

        // Master PIN Button
        binding.masterPinButton.setOnClickListener {
            isBreakPinMode = false
            togglePinInput()
        }

        // Unlock/Break Submit Button
        binding.unlockButton.setOnClickListener {
            val enteredPin = binding.pinInput.text.toString().trim()
            handlePinSubmit(enteredPin)
        }
    }

    // ===== PIN Handling =====

    private fun togglePinInput() {
        if (binding.pinContainer.visibility == View.VISIBLE && !isBreakPinMode) {
            binding.pinContainer.visibility = View.GONE
        } else {
            showPinInput("Enter Master PIN", "Unlock")
        }
    }

    private fun showPinInput(hint: String, buttonText: String) {
        binding.pinContainer.visibility = View.VISIBLE
        binding.pinInput.setText("")
        binding.pinInput.hint = hint
        binding.unlockButton.text = buttonText
        binding.pinInput.requestFocus()
    }

    private fun handlePinSubmit(pin: String) {
        if (pin != Constants.MASTER_PIN) {
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            binding.pinInput.setText("")
            return
        }

        if (isBreakPinMode) {
            grantBreak()
        } else {
            performMasterUnlock()
        }
        
        binding.pinContainer.visibility = View.GONE
        isBreakPinMode = false
    }

    private fun grantBreak() {
        isOnBreak = true
        repository.startBreak(Constants.BREAK_DURATION_MINUTES)
        
        handler.removeCallbacks(bringToFrontRunnable)
        stopLockTaskIfNeeded()
        
        Toast.makeText(this, "${Constants.BREAK_DURATION_MINUTES} minute break started", Toast.LENGTH_LONG).show()
        
        // Schedule re-lock check after break
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.deviceadmin.app.worker.StatusSyncWorker>()
            .setInitialDelay(Constants.BREAK_DURATION_MINUTES.toLong(), java.util.concurrent.TimeUnit.MINUTES)
            .build()
        androidx.work.WorkManager.getInstance(this).enqueue(workRequest)
        
        finish()
    }

    private fun performMasterUnlock() {
        // Update state
        repository.removeProtection()
        
        // Clear all restrictions
        policyHelper.disableProtection()
        policyHelper.setStatusBarDisabled(false)
        
        // Show icon
        AppIconManager.showIcon(this)
        
        Toast.makeText(this, "Device Unlocked - All Protections Removed", Toast.LENGTH_SHORT).show()
        
        stopLockTaskIfNeeded()
        isUnlocked = true
        
        // Navigate to setup for further management
        val intent = Intent(this, SetupActivity::class.java).apply {
            putExtra(Constants.EXTRA_SHOW_ADMIN_PANEL, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }

    // ===== Server Status Check =====

    private fun checkServerStatus() {
        binding.checkStatusButton.isEnabled = false
        binding.checkStatusButton.text = "Checking..."

        lifecycleScope.launch {
            val result = repository.syncWithServer()
            
            binding.checkStatusButton.isEnabled = true
            binding.checkStatusButton.text = "Check Status"

            result.onSuccess { state ->
                loadDisplayData() // Refresh display with new data
                
                if (state.phoneState == PhoneState.ACTIVE || 
                    state.protectionState == ProtectionState.DISABLED) {
                    handleUnlock(if (state.protectionState == ProtectionState.DISABLED) 
                        "Protection Disabled" else "Server")
                } else {
                    Toast.makeText(this@LockScreenActivity, 
                        "Device is still locked by administrator", Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(this@LockScreenActivity, 
                    "Network Error. Please check connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleUnlock(source: String) {
        Toast.makeText(this, "$source Unlocked Device!", Toast.LENGTH_SHORT).show()
        isUnlocked = true
        stopLockTaskIfNeeded()
        finish()
    }

    // ===== Phone Call =====

    private fun makePhoneCall(number: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 101)
            return
        }

        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error making call", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            prefsManager.getCallToNumber()?.let { makePhoneCall(it) }
        }
    }

    // ===== Lock Task Mode =====

    private fun initLockTaskMode() {
        if (!policyHelper.isDeviceOwner()) {
            // Fallback for non-device owner
            handler.post(bringToFrontRunnable)
            return
        }

        try {
            policyHelper.enableLockTaskMode()
            policyHelper.setStatusBarDisabled(true)
            super.startLockTask()
            isLockTaskActive = true
        } catch (e: Exception) {
            // Fallback
            handler.post(bringToFrontRunnable)
        }
    }

    private fun stopLockTaskIfNeeded() {
        if (isLockTaskActive) {
            try {
                stopLockTask()
                isLockTaskActive = false
            } catch (_: Exception) { }
        }
    }

    private fun bringToFront() {
        try {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
        } catch (_: Exception) { }
    }

    // ===== Lifecycle & Navigation Blocking =====

    private fun registerUnlockReceiver() {
        val filter = IntentFilter(Constants.ACTION_UNLOCK_DEVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, filter)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isUnlocked && !isLockTaskActive && !isOnBreak) {
            bringToFront()
        }
    }

    override fun onPause() {
        super.onPause()
        restartIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        restartIfNeeded()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        restartIfNeeded()
    }

    private fun restartIfNeeded() {
        if (!isUnlocked && !isFinishing && !isLockTaskActive && !isOnBreak) {
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Block back button
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(bringToFrontRunnable)
        try {
            unregisterReceiver(unlockReceiver)
        } catch (_: Exception) { }
    }
}
