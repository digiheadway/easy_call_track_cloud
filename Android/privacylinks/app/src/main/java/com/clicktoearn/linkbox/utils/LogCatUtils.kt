package com.clicktoearn.linkbox.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogCatUtils {

    fun shareLogFile(context: Context) {
        android.widget.Toast.makeText(context, "Generating logs...", android.widget.Toast.LENGTH_SHORT).show()
        
        // Run in background to avoid blocking UI
        Thread {
            try {
                val logFile = saveLogCat(context)
                if (logFile == null || !logFile.exists() || logFile.length() == 0L) {
                    val msg = if (logFile == null) "Failed to create file" else "Log file is empty"
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(context, "Error: $msg", android.widget.Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }
                
                val uri = try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        logFile
                    )
                } catch (e: Exception) {
                    Log.e("LogCatUtils", "Failed to get URI: ${e.message}")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(context, "Error getting URI: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                    return@Thread
                }
                
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Private Files Debug Log")
                    val deviceInfo = "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
                    putExtra(Intent.EXTRA_TEXT, "Debug logs attached.\n\n$deviceInfo")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                val chooser = Intent.createChooser(intent, "Share Logs via")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                
            } catch (e: Exception) {
                Log.e("LogCatUtils", "Error sharing logs", e)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun saveLogCat(context: Context): File? {
        // Use internal cache dir which is always available and matches <cache-path>
        val logsDir = File(context.cacheDir, "logs")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
        
        val fileName = "PrivateFiles_Log_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.txt"
        val logFile = File(logsDir, fileName)
        
        try {
            // -d dumps the log to the screen and exits
            // -v threadtime includes date, invocation time, priority/tag, and PID/TID of the thread issuing the message
            val process = Runtime.getRuntime().exec("logcat -d -v threadtime")
            val bufferedReader = process.inputStream.bufferedReader()
            val writer = FileWriter(logFile)
            
            writer.write("--- DEVICE INFO ---\n")
            writer.write("Manufacturer: ${android.os.Build.MANUFACTURER}\n")
            writer.write("Model: ${android.os.Build.MODEL}\n")
            writer.write("Android Version: ${android.os.Build.VERSION.RELEASE}\n")
            writer.write("SDK: ${android.os.Build.VERSION.SDK_INT}\n")
            writer.write("-------------------\n\n")
            
            writer.use { w ->
                bufferedReader.useLines { lines ->
                    lines.forEach { line ->
                        w.append(line).append("\n")
                    }
                }
            }
            return logFile
        } catch (e: IOException) {
            Log.e("LogCatUtils", "Failed to save logcat", e)
            return null
        }
    }
}
