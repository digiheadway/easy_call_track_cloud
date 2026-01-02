package com.calltracker.manager

import android.app.Application
import android.util.Log
import com.calltracker.manager.service.SyncService
import com.calltracker.manager.worker.CallSyncWorker
import com.calltracker.manager.worker.RecordingUploadWorker

class CallTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "App started - initializing background workers and service")
        scheduleWorkers()
        startSyncService()
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

