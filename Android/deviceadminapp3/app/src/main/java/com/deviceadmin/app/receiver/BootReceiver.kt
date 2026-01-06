package com.deviceadmin.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.deviceadmin.app.data.local.PreferencesManager
import com.deviceadmin.app.data.model.PhoneState
import com.deviceadmin.app.ui.lock.LockScreenActivity
import com.deviceadmin.app.util.Constants
import com.deviceadmin.app.worker.StatusSyncWorker
import java.util.concurrent.TimeUnit

/**
 * Receiver for handling device boot completion.
 * Restores device state and schedules background work after reboot.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        
        Log.d(TAG, "Boot completed - initializing")
        
        // Schedule periodic status sync
        scheduleStatusSync(context)
        
        // Restore lock state if needed
        restoreLockState(context)
    }

    private fun scheduleStatusSync(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<StatusSyncWorker>(
            Constants.STATUS_WORK_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.STATUS_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        // Also run an immediate check
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<StatusSyncWorker>().build()
        )
        
        Log.d(TAG, "Status sync scheduled")
    }

    private fun restoreLockState(context: Context) {
        val prefs = PreferencesManager(context)
        val state = prefs.getDeviceState()
        
        if (state.isSetupComplete && state.phoneState == PhoneState.FROZEN) {
            Log.d(TAG, "Restoring lock state after boot")
            
            val lockIntent = Intent(context, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(lockIntent)
        }
    }
}
