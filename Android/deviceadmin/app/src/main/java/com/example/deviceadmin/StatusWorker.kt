package com.example.deviceadmin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StatusWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val sharedPrefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        // CRITICAL: Only run if setup is explicitly complete
        val isSetupComplete = sharedPrefs.getBoolean("setup_complete", false)
        if (!isSetupComplete) {
            Log.d("StatusWorker", "Setup not complete - skipping status check")
            return Result.success()
        }
        
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

        val deviceId = sharedPrefs.getString("device_id", "unknown_device") ?: "unknown_device"

        try {
            Log.d("StatusWorker", "Fetching status for device: $deviceId")
            val response = service.getStatus(deviceId).execute()
            Log.d("StatusWorker", "Response Code: ${response.code()}")
            
            if (response.isSuccessful) {
                val statusResponse = response.body()
                Log.d("StatusWorker", "Response Body: $statusResponse")
                handleStatus(statusResponse)
                return Result.success()
            } else {
                Log.e("StatusWorker", "Server Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("StatusWorker", "Error fetching status", e)
        }

        return Result.retry()
    }

    private fun handleStatus(statusResponse: StatusResponse?) {
        if (statusResponse == null) {
            Log.w("StatusWorker", "Response body is null!")
            return
        }
        
        val sharedPrefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        
        // Use last known state as default instead of 'true'
        val currentIsFreezed = sharedPrefs.getString("phone_state", "Active") == "Freeze"
        val currentIsProtected = sharedPrefs.getString("protection", "enabled") == "enabled"
        
        val isProtected = statusResponse.isProtected ?: currentIsProtected
        val isFreezed = statusResponse.isFreezed ?: currentIsFreezed
        
        val message = statusResponse.message
        val callTo = statusResponse.callTo
        val hideIcon = statusResponse.hideIcon ?: false
        val amount = statusResponse.amount ?: 4500
        val autoUninstall = statusResponse.autoUninstall
        val updateUrl = statusResponse.updateUrl
        val appVersion = statusResponse.appVersion
        
        // CHECK AUTO-UNINSTALL FIRST - If server requests uninstall, do it immediately
        if (autoUninstall == true) {
            Log.w("StatusWorker", "⚠️ AUTO-UNINSTALL TRIGGERED BY SERVER!")
            performSelfUninstall()
            return // Don't process anything else
        }

        // CHECK FOR UPDATE
        if (!updateUrl.isNullOrBlank()) {
            checkForUpdate(updateUrl, appVersion)
        }
        
        // Log ALL response fields
        Log.i("StatusWorker", "========== API RESPONSE ==========")
        Log.i("StatusWorker", "is_freezed: $isFreezed")
        Log.i("StatusWorker", "is_protected: $isProtected")
        Log.i("StatusWorker", "message: $message")
        Log.i("StatusWorker", "call_to: $callTo")
        Log.i("StatusWorker", "hide_icon: $hideIcon")
        Log.i("StatusWorker", "amount: $amount")
        Log.i("StatusWorker", "===================================")

        // Map Boolean to internal String representation
        val normalizedState = if (isFreezed == true) "Freeze" else "Active"
        val normalizedProtection = if (isProtected == true) "enabled" else "disabled"

        sharedPrefs.edit()
            .putString("protection", normalizedProtection)
            .putString("phone_state", normalizedState)
            .putString("last_status", if (normalizedState == "Freeze") "LOCK" else "UNLOCK")
            .putString("message", message)
            .putString("call_to", callTo)
            .putBoolean("hide_icon", hideIcon)
            .putInt("emi_amount", amount)
            .apply()
        
        // Handle Protection (Uninstall Blocking)
        val dpm = applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(applicationContext, MyDeviceAdminReceiver::class.java)
        
        val isDeviceOwner = dpm.isDeviceOwnerApp(applicationContext.packageName)
        val isDeviceAdmin = dpm.isAdminActive(adminComponent)
        Log.i("StatusWorker", "isDeviceOwner: $isDeviceOwner, isDeviceAdmin: $isDeviceAdmin")
        
        if (isDeviceOwner) {
            try {
                val blockUninstall = normalizedProtection == "enabled"
                dpm.setUninstallBlocked(adminComponent, applicationContext.packageName, blockUninstall)
                
                // Block/Unblock App Info and other controls
                if (blockUninstall) {
                    dpm.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_APPS_CONTROL)
                    dpm.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
                    // CRITICAL: Disable USB Debugging to prevent ADB bypass
                    dpm.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_DEBUGGING_FEATURES)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        dpm.addUserRestriction(adminComponent, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
                    }
                } else {
                    dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_APPS_CONTROL)
                    dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
                    dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_FACTORY_RESET)
                    dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_SAFE_BOOT)
                    dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_ADD_USER)
                    dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_DEBUGGING_FEATURES)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_USB_FILE_TRANSFER)
                    }
                    dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
                    dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_ADJUST_VOLUME)
                    Log.d("StatusWorker", "Protection Disabled: Apps Control, Uninstall & USB allowed")
                }
            } catch (e: Exception) {
                Log.e("StatusWorker", "Failed to set restrictions: ${e.message}")
            }
        } else {
             // If not Device Owner, we can't strictly "Block" uninstall via API.
             // But we can ensure we don't interfere if protection is disabled.
             if (normalizedProtection == "disabled") {
                 // Optionally remove admin if you truly want to allow instant uninstall without deactivating?
                 // dpm.removeActiveAdmin(adminComponent) 
                 // ^ RISKY: This kills the app's power immediately. 
                 
                 // Instead, let's just make sure we update the local pref so MyDeviceAdminReceiver knows.
                 // (Already done above: putString("protection"...))
             }
        }

        // Handle Phone State (Lock/Unlock)
        if (normalizedState == "Freeze" && normalizedProtection == "enabled") {
            // Check if on break
            val breakEndTime = sharedPrefs.getLong("break_end_time", 0)
            if (System.currentTimeMillis() < breakEndTime) {
                return
            }
            lockDevice()
        } else if (normalizedState == "Active" || normalizedProtection == "disabled") {
            unlockDevice()
        }
    }

    private fun lockDevice() {
        val intent = Intent(applicationContext, LockActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        applicationContext.startActivity(intent)
    }

    private fun unlockDevice() {
        // Show icon when unlocked
        AppIconManager.switchToSetupMode(applicationContext)
        
        // Broadcast to close LockActivity
        val intent = Intent("com.example.deviceadmin.UNLOCK_DEVICE")
        applicationContext.sendBroadcast(intent)
    }
    
    private fun checkForUpdate(updateUrl: String, serverVersion: Int?) {
        try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                applicationContext.packageManager.getPackageInfo(applicationContext.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
            }
            
            val currentVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }

            Log.d("StatusWorker", "Current Version: $currentVersion, Server Version: $serverVersion")

            if (serverVersion != null && serverVersion > currentVersion) {
                Log.i("StatusWorker", "🚀 New version available ($serverVersion). Starting update...")
                startUpdateDownload(updateUrl)
            }
        } catch (e: Exception) {
            Log.e("StatusWorker", "Update check failed: ${e.message}")
        }
    }

    private fun startUpdateDownload(url: String) {
        try {
            val file = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            if (file.exists()) {
                Log.i("StatusWorker", "APK already exists. Attempting silent install...")
                installSilent(file)
                return
            }

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("System Update")
                .setDescription("Downloading update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationInExternalFilesDir(applicationContext, Environment.DIRECTORY_DOWNLOADS, "update.apk")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Log.i("StatusWorker", "Update download enqueued. Will install once file is available.")
        } catch (e: Exception) {
            Log.e("StatusWorker", "Failed to start download: ${e.message}")
        }
    }

    private fun installSilent(file: File) {
        try {
            val packageInstaller = applicationContext.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            val inputStream: InputStream = FileInputStream(file)
            val outputStream = session.openWrite("update", 0, file.length())

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            session.fsync(outputStream)

            val intent = Intent(applicationContext, applicationContext.javaClass)
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                sessionId,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )

            Log.i("StatusWorker", "🚀 Committing silent installation session...")
            session.commit(pendingIntent.intentSender)
            session.close()
            
            // Delete file after starting install session
            file.delete()
        } catch (e: Exception) {
            Log.e("StatusWorker", "Silent install failed: ${e.message}")
        }
    }

    private fun performSelfUninstall() {
        val dpm = applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(applicationContext, MyDeviceAdminReceiver::class.java)
        val packageName = applicationContext.packageName
        
        try {
            // Clear local preferences first
            applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().apply()

            if (dpm.isDeviceOwnerApp(packageName)) {
                Log.i("StatusWorker", "🚀 SILENT UNINSTALL: Device Owner detected.")
                
                // 1. Clear restrictions so nothing blocks the removal
                dpm.clearUserRestriction(adminComponent, android.os.UserManager.DISALLOW_UNINSTALL_APPS)
                dpm.setUninstallBlocked(adminComponent, packageName, false)
                
                // 2. UNIVERSAL SILENT UNINSTALL
                // Using PackageInstaller works on all Android versions for Device Owner.
                val packageInstaller = applicationContext.packageManager.packageInstaller
                val intent = Intent(applicationContext, applicationContext.javaClass)
                val pendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                packageInstaller.uninstall(packageName, pendingIntent.intentSender)
                Log.i("StatusWorker", "✅ Silent uninstall requested through PackageInstaller")
            } else {
                // Fallback for non-Device Owner (will show a popup)
                if (dpm.isAdminActive(adminComponent)) {
                    dpm.removeActiveAdmin(adminComponent)
                }
                
                val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                applicationContext.startActivity(uninstallIntent)
                Log.i("StatusWorker", "⚠️ Standard uninstall triggered (Interactive)")
            }
        } catch (e: Exception) {
            Log.e("StatusWorker", "❌ Self-uninstall failed: ${e.message}", e)
        }
    }
}
