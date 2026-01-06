package com.example.deviceadmin

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.deviceadmin.databinding.ActivityLockBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class LockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockBinding
    private val MASTER_PIN = "1133"
    private val BREAK_DURATION_MINUTES = 2 // Reduced from 5 to prevent abuse
    private val handler = Handler(Looper.getMainLooper())
    private var isUnlocked = false
    private var isOnBreak = false
    private var isLockTaskEnabled = false
    private var isBreakPinMode = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isUnlocked = true
            stopLockTaskIfNeeded()
            finish()
        }
    }

    private val bringToFrontRunnable = object : Runnable {
        override fun run() {
            if (!isUnlocked && !isFinishing && !isOnBreak) {
                bringToFront()
                // Increase frequency to 100ms for near-instant snap-back if not in Lock Task Mode
                handler.postDelayed(this, 100) 
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            window.insetsController?.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        // Load saved state from prefs
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val amount = prefs.getInt("emi_amount", 4500)
        val savedMessage = prefs.getString("message", null)
        val callTo = prefs.getString("call_to", null)

        // Set EMI Message
        if (!savedMessage.isNullOrEmpty()) {
            binding.emiMessage.text = savedMessage
        } else {
            binding.emiMessage.text = "Due to Pending EMI Amount of ₹$amount"
        }

        // Set Support Button
        if (!callTo.isNullOrEmpty()) {
            binding.callSupportButton.visibility = View.VISIBLE
            binding.callSupportButton.text = "Call Support: $callTo"
        } else {
            binding.callSupportButton.visibility = View.GONE
        }

        // Set Device ID
        val deviceId = prefs.getString("device_id", "Unknown")
        binding.deviceIdDisplay.text = "Device ID: $deviceId"

        // Check Status Button
        binding.checkStatusButton.setOnClickListener {
            checkServerStatus()
        }

        // Call Support Button
        binding.callSupportButton.setOnClickListener {
            val currentCallTo = getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("call_to", null)
            if (!currentCallTo.isNullOrEmpty()) {
                if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(android.Manifest.permission.CALL_PHONE), 101)
                } else {
                    makeCall(currentCallTo)
                }
            }
        }

        // Break Button - now requires PIN
        binding.breakButton.setOnClickListener {
            // Show PIN input for break
            isBreakPinMode = true
            binding.pinContainer.visibility = View.VISIBLE
            binding.pinInput.setText("")
            binding.pinInput.hint = "Enter PIN for Break"
            binding.unlockButton.text = "Start $BREAK_DURATION_MINUTES Min Break"
            binding.pinInput.requestFocus()
        }

        // Master PIN Button (shows PIN input)
        binding.masterPinButton.setOnClickListener {
            if (binding.pinContainer.visibility == View.VISIBLE && !isBreakPinMode) {
                binding.pinContainer.visibility = View.GONE
            } else {
                isBreakPinMode = false
                binding.pinContainer.visibility = View.VISIBLE
                binding.pinInput.setText("")
                binding.pinInput.hint = "Enter Master PIN"
                binding.unlockButton.text = "Unlock"
                binding.pinInput.requestFocus()
                
                // Show keyboard
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(binding.pinInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }

        // Unlock Button - handles both unlock and break
        binding.unlockButton.setOnClickListener {
            val enteredPin = binding.pinInput.text.toString().trim()
            if (enteredPin == MASTER_PIN) {
                if (isBreakPinMode) {
                    // Grant break
                    giveBreak()
                    isBreakPinMode = false
                    binding.pinContainer.visibility = View.GONE
                    binding.pinInput.hint = "Enter Master PIN"
                    binding.unlockButton.text = "Unlock"
                } else {
                    // Full unlock
                    Toast.makeText(this, "Device Unlocked!", Toast.LENGTH_SHORT).show()
                    isUnlocked = true
                    
                    // Update Prefs
                    val prefsEdit = getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
                    prefsEdit.putString("last_status", "UNLOCK")
                    prefsEdit.putString("phone_state", "Active")
                    prefsEdit.putString("protection", "disabled")
                    prefsEdit.putBoolean("setup_complete", false) // Reset setup complete so they can reconfigure
                    prefsEdit.putBoolean("uninstall_allowed", true)
                    prefsEdit.apply()
                    
                    // Re-enable Status Bar and Clear Restrictions
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val adminComponent = ComponentName(this@LockActivity, MyDeviceAdminReceiver::class.java)
                    
                    if (dpm.isDeviceOwnerApp(packageName)) {
                        try {
                            dpm.setStatusBarDisabled(adminComponent, false)
                            dpm.setUninstallBlocked(adminComponent, packageName, false)
                            
                            // Clear ALL restrictions
                            dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_FACTORY_RESET)
                            dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_SAFE_BOOT)
                            dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_ADD_USER)
                            dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_DEBUGGING_FEATURES)
                            dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
                            dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
                            dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_ADJUST_VOLUME)
                            dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_APPS_CONTROL)
                            dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
                            
                            Toast.makeText(this@LockActivity, "All Protections Removed", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception);

                    }

                    // Ensure icon is visible
                    AppIconManager.switchToSetupMode(this@LockActivity)

                    stopLockTaskIfNeeded()
                    
                    // Open MainActivity to allow further management
                    val mainIntent = Intent(this@LockActivity, MainActivity::class.java)
                    mainIntent.putExtra("SHOW_ADMIN_PANEL", true)
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(mainIntent)
                    
                    finish()
                }
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                binding.pinInput.setText("")
            }
        }

        // Register receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, IntentFilter("com.example.deviceadmin.UNLOCK_DEVICE"), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(unlockReceiver, IntentFilter("com.example.deviceadmin.UNLOCK_DEVICE"))
        }

        // Try lock task mode if device owner
        startLockTaskIfPossible()

        // Fallback for non-device-owner
        if (!isLockTaskEnabled) {
            handler.post(bringToFrontRunnable)
        }
        
        // Auto-check on open (onCreate)
        checkServerStatus()
    }

    override fun onStart() {
        super.onStart()
        // Auto-check on resume/screen-on
        checkServerStatus()
    }

    private fun makeCall(number: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error making call", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
             val callTo = getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("call_to", null)
             if (callTo != null) makeCall(callTo)
        }
    }

    private fun checkServerStatus() {
        binding.checkStatusButton.isEnabled = false
        binding.checkStatusButton.text = "Checking..."

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", "unknown_device") ?: "unknown_device"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build()
                    
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.miniclickcrm.com/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val service = retrofit.create(ApiService::class.java)
                val response = service.getStatus(deviceId).execute()

                withContext(Dispatchers.Main) {
                    binding.checkStatusButton.isEnabled = true
                    binding.checkStatusButton.text = "Check Status"

                    if (response.isSuccessful) {
                        val statusResponse = response.body()
                        
                        // Use last known state as default instead of 'true'
                        val currentIsFreezed = prefs.getString("phone_state", "Active") == "Freeze"
                        val currentIsProtected = prefs.getString("protection", "enabled") == "enabled"
                        
                        val isFreezed = statusResponse?.isFreezed ?: currentIsFreezed
                        val isProtected = statusResponse?.isProtected ?: currentIsProtected
                        val message = statusResponse?.message
                        val callTo = statusResponse?.callTo
                        val amount = statusResponse?.amount ?: prefs.getInt("emi_amount", 4500)

                        // Normalize values
                        val normalizedState = if (isFreezed) "Freeze" else "Active"
                        val normalizedProtection = if (isProtected) "enabled" else "disabled"

                        // Save to prefs
                        prefs.edit()
                            .putString("phone_state", normalizedState)
                            .putString("last_status", if (normalizedState == "Freeze") "LOCK" else "UNLOCK")
                            .putString("protection", normalizedProtection)
                            .putString("message", message)
                            .putString("call_to", callTo)
                            .putInt("emi_amount", amount)
                            .apply()

                        // Update UI
                        if (!message.isNullOrEmpty()) {
                            binding.emiMessage.text = message
                        } else {
                            binding.emiMessage.text = "Due to Pending EMI Amount of ₹$amount"
                        }
                        
                        if (!callTo.isNullOrEmpty()) {
                            binding.callSupportButton.visibility = View.VISIBLE
                            binding.callSupportButton.text = "Call Support: $callTo"
                        } else {
                            binding.callSupportButton.visibility = View.GONE
                        }

                        if (normalizedState == "Active" || normalizedProtection == "disabled") {
                            Toast.makeText(this@LockActivity, if (normalizedProtection == "disabled") "Protection Disabled by Server!" else "Device Unlocked by Server!", Toast.LENGTH_SHORT).show()
                            isUnlocked = true
                            stopLockTaskIfNeeded()
                            finish()
                        } else {
                            // Still locked - ensure lock task
                            Toast.makeText(this@LockActivity, "Device is still locked by administrator", Toast.LENGTH_SHORT).show()
                            if (!isLockTaskEnabled) {
                                startLockTaskIfPossible()
                            }
                        }
                        
                        // Handle icon/protection if needed (mostly handled in Worker, but UI can reflect)
                    } 
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.checkStatusButton.isEnabled = true
                    binding.checkStatusButton.text = "Check Status"
                    Toast.makeText(this@LockActivity, "Network Error. Please check connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun giveBreak() {
        isOnBreak = true
        handler.removeCallbacks(bringToFrontRunnable)
        stopLockTaskIfNeeded()
        
        Toast.makeText(this, "$BREAK_DURATION_MINUTES minute break started.", Toast.LENGTH_LONG).show()
        
        // Save break end time
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val breakEndTime = System.currentTimeMillis() + (BREAK_DURATION_MINUTES * 60 * 1000L)
        prefs.edit().putLong("break_end_time", breakEndTime).apply()
        
        // Schedule re-lock check after break duration
        val lockRequest = OneTimeWorkRequestBuilder<StatusWorker>()
            .setInitialDelay(BREAK_DURATION_MINUTES.toLong(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueue(lockRequest)
        
        finish()
    }

    private fun startLockTaskIfPossible() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        
        if (dpm.isDeviceOwnerApp(packageName)) {
            val componentName = ComponentName(this@LockActivity, MyDeviceAdminReceiver::class.java)
            
            // Allow this package to enter lock task mode
            val packages = dpm.getLockTaskPackages(componentName)
            if (!packages.contains(packageName)) {
                dpm.setLockTaskPackages(componentName, arrayOf(packageName))
            }
            
            // STRICT MODE: Disable Power Menu (Global Actions), Notifications, Home, etc.
            // This effectively hides "Power Off" and "Restart" options from the long-press menu.
            // Note: Hardware hard-reset (holding power for 10s) cannot be blocked.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setLockTaskFeatures(componentName, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }
            
            // Add other restrictions to prevent bypassing
            dpm.addUserRestriction(componentName, android.os.UserManager.DISALLOW_FACTORY_RESET)
            dpm.addUserRestriction(componentName, android.os.UserManager.DISALLOW_SAFE_BOOT)
            dpm.addUserRestriction(componentName, android.os.UserManager.DISALLOW_ADD_USER)
            
            // CRITICAL: Disable USB Debugging to prevent ADB bypass
            dpm.addUserRestriction(componentName, android.os.UserManager.DISALLOW_DEBUGGING_FEATURES)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dpm.addUserRestriction(componentName, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
            }
            dpm.addUserRestriction(componentName, android.os.UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
            dpm.addUserRestriction(componentName, android.os.UserManager.DISALLOW_ADJUST_VOLUME)
            
            try {
                // Disable Status Bar (prevents swiping down for Settings/WiFi)
                dpm.setStatusBarDisabled(componentName, true)
                
                startLockTask()
                isLockTaskEnabled = true
            } catch (e: Exception) {
                // Should not happen if isDeviceOwnerApp is true
            }
        }
    }

    private fun stopLockTaskIfNeeded() {
        if (isLockTaskEnabled) {
            try {
                stopLockTask()
                isLockTaskEnabled = false
                
                // Optional: Clear restrictions when unlocking? 
                // Usually for this use case, we might want to keep some restrictions or clear features.
                // For now, let's leave restrictions as they are safer, or we can clear LOCK_TASK_FEATURES to default.
                // But since we are leaving the lock screen, normal usage applies. 
                // The restrictions (DISALLOW_*) usually persist until cleared.
                // Should we clear them? User asked to "disable reboot", usually implying while locked.
                // However, "Device Admin" apps often enforce these policies globally. 
                // Let's keep them legally enforceable as long as the device is managed. 
                // If we want to strictly revert:
                /*
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                if (dpm.isDeviceOwnerApp(packageName)) {
                     val comp = ComponentName(this, MyDeviceAdminReceiver::class.java)
                     dpm.clearUserRestriction(comp, android.os.UserManager.DISALLOW_FACTORY_RESET)
                     // ...
                }
                */
            } catch (e: Exception) { }
        }
    }

    private fun bringToFront() {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME)
        } catch (e: Exception) { }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || 
            keyCode == KeyEvent.KEYCODE_HOME || 
            keyCode == KeyEvent.KEYCODE_APP_SWITCH ||
            keyCode == KeyEvent.KEYCODE_MENU) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isUnlocked && !isLockTaskEnabled && !isOnBreak) {
            bringToFront()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isUnlocked && !isFinishing && !isLockTaskEnabled && !isOnBreak) {
            val intent = Intent(this, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isUnlocked && !isFinishing && !isLockTaskEnabled && !isOnBreak) {
            val intent = Intent(this, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(bringToFrontRunnable)
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: Exception) { }
    }

    override fun onBackPressed() { }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isUnlocked && !isLockTaskEnabled && !isOnBreak) {
            val intent = Intent(this, LockActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
    }
}
