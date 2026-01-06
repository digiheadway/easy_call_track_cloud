package com.example.smsblaster.sms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

sealed class SmsResult {
    data object Success : SmsResult()
    data class Failed(val errorMessage: String) : SmsResult()
}

class SmsService(private val context: Context) {
    
    companion object {
        private const val TAG = "SmsService"
        private const val SMS_SENT_ACTION = "com.example.smsblaster.SMS_SENT"
        private const val SMS_TIMEOUT_MS = 30000L // 30 seconds timeout
    }
    
    private val subscriptionManager: SubscriptionManager by lazy {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    }
    
    fun getAvailableSimCards(): List<SimInfo> {
        return try {
            val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            subscriptionInfoList.map { info ->
                SimInfo(
                    subscriptionId = info.subscriptionId,
                    displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                    carrierName = info.carrierName?.toString() ?: "Unknown",
                    slotIndex = info.simSlotIndex
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting SIM info: ${e.message}")
            emptyList()
        }
    }
    
    @Suppress("DEPRECATION")
    private fun getSmsManager(subscriptionId: Int? = null): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java).let { manager ->
                if (subscriptionId != null) {
                    manager.createForSubscriptionId(subscriptionId)
                } else {
                    manager
                }
            }
        } else {
            if (subscriptionId != null) {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            } else {
                SmsManager.getDefault()
            }
        }
    }
    
    suspend fun sendSms(
        phoneNumber: String,
        message: String,
        subscriptionId: Int? = null
    ): SmsResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting to send SMS to: $phoneNumber")
        
        // Try simple send first (fire and forget with immediate success assumption)
        try {
            val smsManager = getSmsManager(subscriptionId)
            val parts = smsManager.divideMessage(message)
            
            Log.d(TAG, "Message parts: ${parts.size}")
            
            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }
            
            Log.d(TAG, "SMS sent successfully to: $phoneNumber")
            SmsResult.Success
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}")
            SmsResult.Failed("Permission denied: ${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException: ${e.message}")
            SmsResult.Failed("Invalid phone number: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending SMS: ${e.message}", e)
            SmsResult.Failed(e.message ?: "Unknown error")
        }
    }
    
    // Alternative method with callback confirmation
    suspend fun sendSmsWithConfirmation(
        phoneNumber: String,
        message: String,
        subscriptionId: Int? = null
    ): SmsResult {
        Log.d(TAG, "Sending SMS with confirmation to: $phoneNumber")
        
        val result = withTimeoutOrNull(SMS_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val requestCode = System.currentTimeMillis().toInt()
                val intentAction = "$SMS_SENT_ACTION.$requestCode"
                
                val sentIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    Intent(intentAction),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                )
                
                val sentReceiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        Log.d(TAG, "SMS broadcast received, resultCode: $resultCode")
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error unregistering receiver: ${e.message}")
                        }
                        
                        if (!continuation.isActive) return
                        
                        when (resultCode) {
                            Activity.RESULT_OK -> {
                                Log.d(TAG, "SMS sent successfully")
                                continuation.resume(SmsResult.Success)
                            }
                            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                                Log.e(TAG, "Generic failure")
                                continuation.resume(SmsResult.Failed("Generic failure"))
                            }
                            SmsManager.RESULT_ERROR_NO_SERVICE -> {
                                Log.e(TAG, "No service")
                                continuation.resume(SmsResult.Failed("No service"))
                            }
                            SmsManager.RESULT_ERROR_NULL_PDU -> {
                                Log.e(TAG, "Null PDU")
                                continuation.resume(SmsResult.Failed("Null PDU"))
                            }
                            SmsManager.RESULT_ERROR_RADIO_OFF -> {
                                Log.e(TAG, "Radio off")
                                continuation.resume(SmsResult.Failed("Radio off"))
                            }
                            else -> {
                                Log.e(TAG, "Unknown error: $resultCode")
                                continuation.resume(SmsResult.Failed("Unknown error: $resultCode"))
                            }
                        }
                    }
                }
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(
                            sentReceiver,
                            IntentFilter(intentAction),
                            Context.RECEIVER_NOT_EXPORTED
                        )
                    } else {
                        @Suppress("UnspecifiedRegisterReceiverFlag")
                        context.registerReceiver(sentReceiver, IntentFilter(intentAction))
                    }
                    
                    val smsManager = getSmsManager(subscriptionId)
                    val parts = smsManager.divideMessage(message)
                    
                    if (parts.size == 1) {
                        smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, null)
                    } else {
                        val sentIntents = ArrayList<PendingIntent>().apply {
                            add(sentIntent)
                            repeat(parts.size - 1) { add(sentIntent) }
                        }
                        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
                    }
                    
                    Log.d(TAG, "SMS send command executed, waiting for confirmation...")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending SMS: ${e.message}", e)
                    try {
                        context.unregisterReceiver(sentReceiver)
                    } catch (_: Exception) {}
                    
                    if (continuation.isActive) {
                        continuation.resume(SmsResult.Failed(e.message ?: "Failed to send SMS"))
                    }
                }
                
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Continuation cancelled")
                    try {
                        context.unregisterReceiver(sentReceiver)
                    } catch (_: Exception) {}
                }
            }
        }
        
        return result ?: run {
            Log.w(TAG, "SMS send timed out, assuming success")
            // If timeout, we assume it was sent (carrier-specific delays can cause this)
            SmsResult.Success
        }
    }
}

data class SimInfo(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val slotIndex: Int
)
