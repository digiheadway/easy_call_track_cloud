package com.miniclickcrm.deviceadmin3.utils

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenManager {
    
    private const val PREFS_NAME = "fcm_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"

    /**
     * Get token asynchronously with callback
     */
    fun getToken(context: Context, callback: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Token: $token")
                saveToken(context, token)
                callback(token)
            } else {
                Log.e("FCM", "Failed to get token", task.exception)
                callback(getSavedToken(context))
            }
        }
    }
    
    /**
     * Get token synchronously (returns cached token, or null if not available)
     * Use this from coroutines/background threads
     */
    fun getToken(context: Context): String? {
        return getSavedToken(context)
    }
    
    /**
     * Get token using coroutines (suspending function)
     */
    suspend fun getTokenAsync(context: Context): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveToken(context, token)
            token
        } catch (e: Exception) {
            Log.e("FCM", "Failed to get token", e)
            getSavedToken(context)
        }
    }
    
    /**
     * Get token synchronously - blocks until token is available
     * Returns cached token if fetching fails
     */
    fun getTokenSync(context: Context): String? {
        return kotlinx.coroutines.runBlocking {
            getTokenAsync(context)
        }
    }

    fun saveToken(context: Context, token: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    fun getSavedToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
    }
}
