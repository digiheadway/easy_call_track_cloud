package com.example.deviceadmin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class StatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceId = Utils.getDeviceId(applicationContext)
        if (deviceId.isEmpty()) return Result.success()

        val url = "https://api.miniclickcrm.com/admin/status.php?id=$deviceId"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result.retry()

                val body = response.body?.string() ?: return Result.failure()
                val statusResponse = Gson().fromJson(body, StatusResponse::class.java)

                handleStatusUpdate(statusResponse)
            }
        } catch (e: IOException) {
            Log.e("StatusWorker", "Error fetching status", e)
            return Result.retry()
        }

        return Result.success()
    }

    private fun handleStatusUpdate(status: StatusResponse) {
        val context = applicationContext
        Utils.setFreezed(context, status.is_freezed)
        Utils.setProtected(context, status.is_protected)
        Utils.setEmiAmount(context, status.emi_amount ?: "$0.00")
        Utils.setServerMessage(context, status.server_message ?: "")

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, MyDeviceAdminReceiver::class.java)
        
        if (dpm.isAdminActive(admin)) {
            DevicePolicyHandler.applyEnterprisePolicies(context, dpm, admin, status.is_protected)
        }

        if (status.is_freezed) {
            LockActivity.start(context)
        }

        if (status.auto_uninstall) {
            // Logic for auto-uninstall (requires device owner)
            triggerAutoUninstall()
        }

        if (!status.update_url.isNullOrEmpty()) {
            // Logic for silent update (requires device owner)
            triggerUpdate(status.update_url)
        }
    }

    private fun triggerAutoUninstall() {
        // Placeholder for silent uninstall logic
    }

    private fun triggerUpdate(url: String) {
        // Placeholder for silent update logic
    }

    data class StatusResponse(
        val is_freezed: Boolean,
        val is_protected: Boolean,
        val auto_uninstall: Boolean,
        val update_url: String?,
        val emi_amount: String?,
        val server_message: String?
    )
}
