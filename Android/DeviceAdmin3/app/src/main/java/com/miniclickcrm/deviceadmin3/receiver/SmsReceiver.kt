package com.miniclickcrm.deviceadmin3.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.miniclickcrm.deviceadmin3.LockScreenActivity
import com.miniclickcrm.deviceadmin3.manager.DeviceManager

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody
                Log.d("SmsReceiver", "SMS received: $body")
                
                // Example logic for remote control
                // Verify Sender: Must match Manager Number
                val prefs = context.getSharedPreferences("device_admin_prefs", Context.MODE_PRIVATE)
                val managerNumber = prefs.getString("call_to", "")
                val sender = sms.originatingAddress

                if (managerNumber.isNullOrEmpty() || sender == null || !android.telephony.PhoneNumberUtils.compare(context, managerNumber, sender)) {
                    Log.d("SmsReceiver", "Ignored SMS from unauthorized sender: $sender")
                    continue
                }

                // In production, this should be encrypted/validated
                // Commands (Authorized Sender Only)
                if (body.contains("LOCK_DEVICE_FORCE")) {
                    abortBroadcast()
                    lockDevice(context)
                } else if (body.contains("UNLOCK_DEVICE_FORCE")) {
                    abortBroadcast()
                    unlockDevice(context)
                } else if (body.contains("REMOVE_PROTECTION_FORCE")) {
                    abortBroadcast()
                    removeProtection(context)
                }
            }
        }
    }

    private fun lockDevice(context: Context) {
        // Implementation to trigger lock state
        val intent = Intent(context, LockScreenActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("LOCKED", true)
        context.startActivity(intent)
    }

    private fun unlockDevice(context: Context) {
        // Implementation to trigger unlock state
        val intent = Intent("com.miniclickcrm.deviceadmin3.UNLOCK")
        context.sendBroadcast(intent)
    }

    private fun removeProtection(context: Context) {
        val deviceManager = DeviceManager(context)
        deviceManager.removeProtection()
    }
}
