package com.deviceadmin.app

import android.app.Application
import android.util.Log

/**
 * Application class for DeviceAdmin app.
 * Initializes WorkManager and other app-wide components.
 */
class DeviceAdminApplication : Application() {

    companion object {
        private const val TAG = "DeviceAdminApp"
        
        @Volatile
        private var instance: DeviceAdminApplication? = null
        
        fun getInstance(): DeviceAdminApplication {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }

    override fun onCreate() {
        Log.d(TAG, ">>> Application.onCreate() STARTED")
        try {
            super.onCreate()
            Log.d(TAG, ">>> super.onCreate() completed")
            
            instance = this
            Log.d(TAG, ">>> instance set")
            
            Log.d(TAG, ">>> Application.onCreate() COMPLETED SUCCESSFULLY")
        } catch (e: Exception) {
            Log.e(TAG, ">>> CRASH in Application.onCreate()", e)
            throw e
        }
    }
}
