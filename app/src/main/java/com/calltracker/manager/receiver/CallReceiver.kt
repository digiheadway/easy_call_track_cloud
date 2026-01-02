package com.calltracker.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.calltracker.manager.worker.CallSyncWorker
import com.calltracker.manager.worker.RecordingUploadWorker

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            if (TelephonyManager.EXTRA_STATE_IDLE == state) {
                Log.d(TAG, "Call ended (IDLE state). Triggering immediate sync.")
                
                // Trigger immediate sync for metadata
                CallSyncWorker.runNow(context)
                
                // Also trigger recording upload check, though it might take a moment 
                // for the file to be saved by the system/recorder.
                // CallSyncWorker also triggers RecordingUpload checks internally if it finds new calls,
                // but explicit trigger doesn't hurt.
                RecordingUploadWorker.runNow(context)
            }
        }
    }

    companion object {
        private const val TAG = "CallReceiver"
    }
}
