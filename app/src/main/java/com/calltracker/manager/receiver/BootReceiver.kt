package com.calltracker.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.calltracker.manager.service.SyncService
import com.calltracker.manager.worker.CallSyncWorker
import com.calltracker.manager.worker.RecordingUploadWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            Log.d(TAG, "Boot/Update detected. Starting sync service and scheduling workers.")
            
            // Re-schedule workers as backup
            CallSyncWorker.enqueue(context)
            RecordingUploadWorker.enqueue(context)
            
            // Start foreground service for reliable sync
            SyncService.start(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}

