package com.example.deviceadmin

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class HideIconWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val hide = inputData.getBoolean("hide", true)
        Log.i("HideIconWorker", "Background job started. Hiding icon: $hide")
        
        try {
            if (hide) {
                AppIconManager.switchToStealthMode(applicationContext)
            } else {
                AppIconManager.switchToSetupMode(applicationContext)
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("HideIconWorker", "Error toggling icon", e)
            return Result.failure()
        }
    }
}
