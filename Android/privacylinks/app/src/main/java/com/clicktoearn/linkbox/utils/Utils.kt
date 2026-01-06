package com.clicktoearn.linkbox.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

fun copyToClipboard(context: Context, text: String, label: String = "Link") {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            Toast.makeText(context, "Clipboard service not available", Toast.LENGTH_SHORT).show()
            return
        }
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        vibrate(context)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.util.Log.e("Utils", "Failed to copy to clipboard", e)
        Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun vibrate(context: Context) {
    try {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }

        vibrator?.let { v ->
            if (v.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(50)
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("Utils", "Vibration failed", e)
    }
}

fun generateShareUrl(token: String): String {
    return "https://privacy.be6.in/$token"
}
