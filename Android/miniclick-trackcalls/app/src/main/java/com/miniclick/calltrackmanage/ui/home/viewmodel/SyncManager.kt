package com.miniclick.calltrackmanage.ui.home.viewmodel

import android.app.Application
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker

object SyncManager {
    fun syncNow(application: Application) {
        CallSyncWorker.runNow(application)
        RecordingUploadWorker.runNow(application)
    }

    fun quickSync(application: Application) {
        CallSyncWorker.runQuickSync(application)
        RecordingUploadWorker.runNow(application)
    }
}
