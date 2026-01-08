package com.miniclick.calltrackmanage.ui.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.content.ContextCompat

object CallUtils {
    
    fun makeCall(context: Context, phoneNumber: String, subId: Int? = null, forceDialer: Boolean = false) {
        val cleaned = cleanNumber(phoneNumber)
        if (cleaned.isEmpty()) return

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val isDefaultDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            telecomManager.defaultDialerPackage == context.packageName
        } else {
            false
        }

        try {
            val uri = Uri.fromParts("tel", cleaned, null)
            val extras = Bundle()
            
            // If subId is provided, try to find the corresponding PhoneAccountHandle
            if (subId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val handle = getPhoneAccountHandle(context, subId)
                if (handle != null) {
                    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                }
            }

            if (!forceDialer && isDefaultDialer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // If we are the default dialer, place the call through TelecomManager
                telecomManager.placeCall(uri, extras)
            } else if (!forceDialer && hasCallPermission) {
                // If we have permission but NOT default dialer, use ACTION_CALL
                val intent = Intent(Intent.ACTION_CALL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (subId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val handle = getPhoneAccountHandle(context, subId)
                        if (handle != null) {
                            putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                        }
                    }
                }
                context.startActivity(intent)
            } else {
                // Fallback: Open dialer
                val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Final fallback
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                e2.printStackTrace()
                 Toast.makeText(context, "Could not initiate call: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getPhoneAccountHandle(context: Context, subId: Int): android.telecom.PhoneAccountHandle? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val callManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        
        try {
            val handles = telecomManager.callCapablePhoneAccounts
            for (handle in handles) {
                val account = telecomManager.getPhoneAccount(handle)
                val accountId = handle.id
                
                // On most Android versions, the account ID contains the subId or matches it
                if (accountId == subId.toString()) {
                    return handle
                }
                
                // Fallback: check communication via subscription ID if available in account
                // This is device specific but often works
                if (account?.label?.toString()?.contains(subId.toString()) == true) {
                    return handle
                }
            }
            
            // Final attempt: Use subscription ID directly if matching handle exists
            // Some systems use the slot index or subId as handle ID
            return handles.find { it.id == subId.toString() } ?: handles.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun cleanNumber(number: String): String {
        return number.replace(Regex("[^0-9+*#]"), "")
    }
}
