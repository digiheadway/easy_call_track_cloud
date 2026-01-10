package com.miniclickcrm.deviceadmin3.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import com.miniclickcrm.deviceadmin3.MainActivity
import com.miniclickcrm.deviceadmin3.LockScreenActivity
import com.miniclickcrm.deviceadmin3.R
import com.miniclickcrm.deviceadmin3.api.RetrofitClient
import com.miniclickcrm.deviceadmin3.manager.DeviceManager
import com.miniclickcrm.deviceadmin3.utils.FcmTokenManager
import kotlinx.coroutines.*

class MainService : Service() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MainService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, MainService::class.java)
            context.stopService(intent)
        }
    }

    private val CHANNEL_ID = "DeviceAdminServiceChannel"
    private val TAG = "MainService"
    
    // Check interval: 15 minutes (in milliseconds)
    private val CHECK_INTERVAL_MS = 15 * 60 * 1000L
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var statusCheckRunnable: Runnable? = null
    private var persistenceRunnable: Runnable? = null
    private val PERSISTENCE_INTERVAL_MS = 5000L // 5 seconds

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Protection Active")
            .setContentText("Your device is protected by DeviceAdmin.")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(createPendingIntent())
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        // Start periodic status check
        startPeriodicStatusCheck()
        
        // Start persistence check
        startPersistenceCheck()
        
        // Do an immediate check on service start
        checkStatusNow()

        return START_STICKY
    }
    
    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    private fun startPeriodicStatusCheck() {
        statusCheckRunnable?.let { handler.removeCallbacks(it) }
        
        statusCheckRunnable = object : Runnable {
            override fun run() {
                checkStatusNow()
                handler.postDelayed(this, CHECK_INTERVAL_MS)
            }
        }
        
        handler.postDelayed(statusCheckRunnable!!, CHECK_INTERVAL_MS)
        Log.d(TAG, "Periodic status check started (interval: ${CHECK_INTERVAL_MS / 1000 / 60} minutes)")
    }

    private fun startPersistenceCheck() {
        persistenceRunnable?.let { handler.removeCallbacks(it) }
        
        persistenceRunnable = object : Runnable {
            override fun run() {
                val deviceManager = DeviceManager(this@MainService)
                // Lock status is maintained by specific Activity and Launcher alias
                // We just keep the service alive for status checks
                
                handler.postDelayed(this, PERSISTENCE_INTERVAL_MS)
            }
        }
        
        handler.post(persistenceRunnable!!)
        Log.d(TAG, "Persistence check started")
    }
    
    private fun checkStatusNow() {
        serviceScope.launch {
            try {
                val prefs = getSharedPreferences("device_admin_prefs", Context.MODE_PRIVATE)
                val pairingCode = prefs.getString("pairing_code", null)
                
                if (pairingCode.isNullOrEmpty()) {
                    Log.d(TAG, "No pairing code set, skipping status check")
                    return@launch
                }
                
                val fcmToken = FcmTokenManager.getToken(this@MainService)
                
                Log.d(TAG, "Checking status for pairing code: $pairingCode")
                
                val deviceInfoManager = com.miniclickcrm.deviceadmin3.utils.DeviceInfoManager(this@MainService)
                val (imei1, imei2) = deviceInfoManager.getImeis()
                val deviceName = deviceInfoManager.getDeviceName()
                val deviceModel = deviceInfoManager.getDeviceModel()
                
                Log.d(TAG, "Checking status using: Code=$pairingCode, IMEI1=$imei1")
                
                val response = RetrofitClient.apiService.checkStatus(
                    pairingCode = pairingCode,
                    fcmToken = fcmToken,
                    imei = imei1,
                    imei2 = imei2,
                    deviceName = deviceName,
                    deviceModel = deviceModel
                )
                
                if (response.success && response.data != null) {
                    Log.d(TAG, "Status check successful: is_freezed=${response.data.is_freezed}")
                    
                    // Save device data
                    prefs.edit().apply {
                        putBoolean("is_freezed", response.data.is_freezed)
                        putBoolean("is_protected", response.data.is_protected)
                        putInt("amount", response.data.amount)
                        putString("message", response.data.message)
                        putString("call_to", response.data.call_to)
                        putLong("last_status_check", System.currentTimeMillis())
                        apply()
                    }
                    
                    // Apply states based on API response
                    val deviceManager = DeviceManager(this@MainService)
                    
                    // 1. Handle Freezed state (Locked)
                    if (response.data.is_freezed) {
                        deviceManager.freezeDevice(response.data.message)
                    } else {
                        deviceManager.unfreezeDevice()
                    }
                    
                    // 2. Handle Protected state (Administrative restrictions)
                    deviceManager.setProtected(response.data.is_protected)
                    
                    // 3. Store offline unlock codes
                    response.data.unlock_codes?.let { codes ->
                        val securityManager = com.miniclickcrm.deviceadmin3.manager.SecurityManager(this@MainService)
                        securityManager.storeUnlockCodes(codes)
                    }
                    
                    // Update notification with current status
                    updateNotification(response.data.is_freezed, response.data.is_protected, response.data.message)
                    
                } else {
                    Log.e(TAG, "Status check failed: ${response.error}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking status: ${e.message}", e)
            }
        }
    }
    
    private fun updateNotification(isFreezed: Boolean, isProtected: Boolean, message: String) {
        val statusText = when {
            isFreezed -> "Device LOCKED: $message"
            isProtected -> "Device Protection: ACTIVE"
            else -> "App Active (Unprotected)"
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Protection")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(createPendingIntent())
            .build()
            
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        statusCheckRunnable?.let { handler.removeCallbacks(it) }
        persistenceRunnable?.let { handler.removeCallbacks(it) }

        serviceScope.cancel()
        Log.d(TAG, "MainService destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Device Admin Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
