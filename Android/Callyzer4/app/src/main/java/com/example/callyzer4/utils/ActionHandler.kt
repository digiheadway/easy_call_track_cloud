package com.example.callyzer4.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat

class ActionHandler(private val context: Context) {
    
    fun makeCall(phoneNumber: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        try {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                context.startActivity(intent)
                onSuccess()
            } else {
                onError("Call permission required")
            }
        } catch (e: Exception) {
            onError("Unable to make call: ${e.message}")
        }
    }
    
    fun sendMessage(phoneNumber: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                onSuccess()
            } else {
                onError("No messaging app available")
            }
        } catch (e: Exception) {
            onError("Unable to send message: ${e.message}")
        }
    }
    
    fun sendWhatsApp(phoneNumber: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$phoneNumber")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                onSuccess()
            } else {
                onError("WhatsApp not available")
            }
        } catch (e: Exception) {
            onError("Unable to open WhatsApp: ${e.message}")
        }
    }
    
    fun copyToClipboard(phoneNumber: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Phone Number", phoneNumber)
            clipboardManager.setPrimaryClip(clipData)
            onSuccess()
        } catch (e: Exception) {
            onError("Unable to copy to clipboard: ${e.message}")
        }
    }
}
