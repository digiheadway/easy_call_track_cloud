package com.example.deviceadmin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val workRequest = PeriodicWorkRequestBuilder<StatusWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "StatusCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            
            // Check persistence
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lastStatus = sharedPrefs.getString("last_status", "UNLOCK")
            if (lastStatus == "LOCK") {
                val lockIntent = Intent(context, LockActivity::class.java)
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(lockIntent)
            }
        }
    }
}
