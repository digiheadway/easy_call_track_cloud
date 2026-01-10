package com.miniclick.calltrackmanage

import android.app.Application
import android.util.Log
import com.miniclick.calltrackmanage.service.SyncService
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CallTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        val settingsRepository = com.miniclick.calltrackmanage.data.SettingsRepository.getInstance(this)
        if (settingsRepository.isAgreementAccepted()) {
            Log.d(TAG, "App started - agreement already accepted, initializing background workers and service")
            scheduleWorkers()
            startSyncService()
        } else {
            Log.d(TAG, "App started - agreement not yet accepted, skipping background initialization")
        }
    }

    private fun scheduleWorkers() {
        // Enqueue periodic workers as backup
        CallSyncWorker.enqueue(this)
        RecordingUploadWorker.enqueue(this)
    }

    private fun startSyncService() {
        // Start foreground service for reliable background sync
        SyncService.start(this)
    }

    companion object {
        const val TAG = "CallTrackerApp"
    }
}

