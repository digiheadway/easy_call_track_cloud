package com.miniclick.calltrackmanage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.miniclick.calltrackmanage.service.SyncService
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            val settingsRepository = com.miniclick.calltrackmanage.data.SettingsRepository.getInstance(context)
            if (settingsRepository.isAgreementAccepted()) {
                Log.d(TAG, "Boot/Update detected. Starting sync service and scheduling workers.")
                
                // Re-schedule workers as backup
                CallSyncWorker.enqueue(context)
                RecordingUploadWorker.enqueue(context)
                
                // Start foreground service for reliable sync
                SyncService.start(context)
            } else {
                Log.d(TAG, "Boot detected - agreement not yet accepted, skipping background initialization")
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

