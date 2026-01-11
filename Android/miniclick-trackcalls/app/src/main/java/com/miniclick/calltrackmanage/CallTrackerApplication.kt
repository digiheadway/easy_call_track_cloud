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
        
        // STARTUP OPTIMIZATION: Move background work off the main thread and defer it 
        // to allow the first activity frame to render immediately.
        // We use a global scope here because it matches the Application lifecycle.
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(2000) // 2 second delay for non-critical background work
            
            val settingsRepository = com.miniclick.calltrackmanage.data.SettingsRepository.getInstance(applicationContext)
            if (settingsRepository.isAgreementAccepted() && settingsRepository.isSetupGuideCompleted()) {
                Log.d(TAG, "Deferred background initialization starting...")
                scheduleWorkers()
                startSyncService()
            }
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

