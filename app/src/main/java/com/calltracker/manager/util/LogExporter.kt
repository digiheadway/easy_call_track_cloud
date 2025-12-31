package com.calltracker.manager.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.calltracker.manager.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object LogExporter {
    private const val TAG = "LogExporter"

    /**
     * Exports Logcat logs and Database contents to a zip or text file and shares it.
     */
    suspend fun exportAndShareLogs(context: Context) = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val logFile = File(context.cacheDir, "CallCloud_Logs_$timestamp.txt")
            
            FileOutputStream(logFile).use { fos ->
                fos.write("=== CallCloud Session Logs ===\n".toByteArray())
                fos.write("Timestamp: ${Date()}\n".toByteArray())
                fos.write("Device: ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})\n\n".toByteArray())
                
                // 1. Capture Logcat
                fos.write("--- Logcat Output ---\n".toByteArray())
                try {
                    val process = Runtime.getRuntime().exec("logcat -d")
                    val output = process.inputStream.bufferedReader().readText()
                    fos.write(output.toByteArray())
                } catch (e: Exception) {
                    fos.write("Error capturing logcat: ${e.message}\n".toByteArray())
                }
                
                fos.write("\n\n--- Database Summary ---\n".toByteArray())
                try {
                    val db = AppDatabase.getInstance(context)
                    val calls = db.callDataDao().getAllCalls()
                    fos.write("Total Calls in Database: ${calls.size}\n".toByteArray())
                    
                    calls.takeLast(50).forEach { call ->
                        fos.write("Call: ${call.compositeId} | Status: ${call.syncStatus} | Date: ${Date(call.callDate)}\n".toByteArray())
                    }
                } catch (e: Exception) {
                    fos.write("Error summarying database: ${e.message}\n".toByteArray())
                }
            }
            
            saveToDownloads(context, logFile)
            shareFile(context, logFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
        }
    }

    private fun saveToDownloads(context: Context, logFile: File) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            
            val destFile = File(downloadsDir, logFile.name)
            logFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            // Notify system scanner
            android.media.MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
            
            // Show toast on UI thread
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                android.widget.Toast.makeText(context, "Logs saved to Downloads: ${destFile.name}", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to Downloads", e)
        }
    }

    private fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "CallCloud Session Logs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Session Logs")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
