package com.miniclickcrm.deviceadmin3.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.miniclickcrm.deviceadmin3.manager.DeviceManager

class MyAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        showToast(context, "Device Admin: Enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        showToast(context, "Device Admin: Disabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val deviceManager = DeviceManager(context)
        return if (deviceManager.isProtected()) {
            "Protection is active. Please disable protection from the app first using the Master PIN."
        } else {
            "Are you sure you want to deactivate Device Admin?"
        }
    }

    override fun onPasswordChanged(context: Context, intent: Intent) {
        super.onPasswordChanged(context, intent)
    }

    private fun showToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
