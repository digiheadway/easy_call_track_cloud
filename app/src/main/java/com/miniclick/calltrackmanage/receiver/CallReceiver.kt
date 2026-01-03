package com.miniclick.calltrackmanage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.miniclick.calltrackmanage.service.CallerIdManager
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Incoming call - show caller ID overlay
                    if (!phoneNumber.isNullOrBlank()) {
                        Log.d(TAG, "Incoming call from $phoneNumber - showing caller ID")
                        CallerIdManager.show(context, phoneNumber)
                    }
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call answered or outgoing call started
                    // Keep overlay visible during call if it was showing
                    Log.d(TAG, "Call in progress (OFFHOOK state)")
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    Log.d(TAG, "Call ended (IDLE state). Triggering immediate sync.")
                    
                    // Hide caller ID overlay
                    CallerIdManager.hide(context)
                    
                    // Trigger immediate sync for metadata
                    CallSyncWorker.runNow(context)
                    
                    // Also trigger recording upload check
                    RecordingUploadWorker.runNow(context)
                }
            }
        }
    }

    companion object {
        private const val TAG = "CallReceiver"
    }
}
