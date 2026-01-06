package com.deviceadmin.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deviceadmin.app.ui.setup.SetupActivity
import com.deviceadmin.app.util.Constants

/**
 * Receiver for secret dial code to access admin panel.
 * Allows access when app icon is hidden.
 * Dial: *#*#1133#*#*
 */
class SecretCodeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SecretCodeReceiver"
        private const val ACTION_SECRET_CODE = "android.provider.Telephony.SECRET_CODE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SECRET_CODE) return
        
        Log.d(TAG, "Secret code received - launching admin panel")
        
        val launchIntent = Intent(context, SetupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Constants.EXTRA_SHOW_ADMIN_PANEL, true)
        }
        context.startActivity(launchIntent)
    }
}
