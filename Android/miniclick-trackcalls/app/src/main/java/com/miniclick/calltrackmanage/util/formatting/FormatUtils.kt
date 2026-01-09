package com.miniclick.calltrackmanage.util.formatting

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.text.SimpleDateFormat
import java.util.*

fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}

fun formatDurationShort(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(millis: Int): String {
    val minutes = (millis / 1000) / 60
    val seconds = (millis / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Normalizes phone number: removes spaces, non-digits (except maybe + initially),
 * and removes the default country code (+91 or 91) if it exists.
 */
fun cleanNumber(number: String): String {
    // Keep only digits and +
    var cleaned = number.filter { it.isDigit() || it == '+' }
    
    // Remove default country code +91 or 91 (if length > 10)
    if (cleaned.startsWith("+91")) {
        cleaned = cleaned.substring(3)
    } else if (cleaned.startsWith("91") && cleaned.length > 10) {
        cleaned = cleaned.substring(2)
    }
    
    // Remove leading 0 if it's a long number
    if (cleaned.startsWith("0") && cleaned.length > 10) {
        cleaned = cleaned.substring(1)
    }
    
    // Final pass to remove any remaining + (if it wasn't part of +91)
    return cleaned.filter { it.isDigit() }
}

fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 172800_000 -> "Yesterday"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

fun getDateHeader(dateMillis: Long): String {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    val year = calendar.get(Calendar.YEAR)
    
    calendar.timeInMillis = dateMillis
    val logDay = calendar.get(Calendar.DAY_OF_YEAR)
    val logYear = calendar.get(Calendar.YEAR)
    
    return when {
        year == logYear && today == logDay -> "Today"
        year == logYear && today == logDay + 1 -> "Yesterday"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(dateMillis))
    }
}
