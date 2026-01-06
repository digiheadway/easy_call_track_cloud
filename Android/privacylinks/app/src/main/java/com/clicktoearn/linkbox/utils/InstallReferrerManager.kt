package com.clicktoearn.linkbox.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import java.net.URLDecoder

/**
 * Manages Play Install Referrer API to detect deferred deep links
 * 
 * When users click a web link with a token and don't have the app installed,
 * they are redirected to Play Store with the token in the referrer parameter.
 * This manager extracts that token on first app launch for deferred deep linking.
 */
class InstallReferrerManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("install_referrer", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "InstallReferrerManager"
        private const val KEY_REFERRER_FETCHED = "referrer_fetched"
        private const val KEY_REFERRER_URL = "referrer_url"
        private const val KEY_CLICK_TIMESTAMP = "click_timestamp"
        private const val KEY_INSTALL_TIMESTAMP = "install_timestamp"
    }
    
    /**
     * Fetches install referrer from Play Store
     * Only fetches once - subsequent calls return cached data
     * 
     * @param onComplete Callback with extracted token (null if no token found)
     */
    fun fetchInstallReferrer(onComplete: (String?) -> Unit) {
        // Check if already fetched
        if (isReferrerFetched()) {
            Log.d(TAG, "Referrer already fetched, returning cached token")
            onComplete(extractTokenFromReferrer(getSavedReferrerUrl()))
            return
        }
        
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            val response = referrerClient.installReferrer
                            val referrerUrl = response.installReferrer
                            val clickTimestamp = response.referrerClickTimestampSeconds
                            val installTimestamp = response.installBeginTimestampSeconds
                            
                            Log.d(TAG, "Install referrer fetched successfully")
                            Log.d(TAG, "Referrer URL: $referrerUrl")
                            Log.d(TAG, "Click timestamp: $clickTimestamp")
                            Log.d(TAG, "Install timestamp: $installTimestamp")
                            
                            // Save referrer data
                            saveReferrerData(referrerUrl, clickTimestamp, installTimestamp)
                            
                            // Extract token from referrer URL
                            val token = extractTokenFromReferrer(referrerUrl)
                            if (token != null) {
                                Log.d(TAG, "Extracted token from referrer: $token")
                            } else {
                                Log.d(TAG, "No token found in referrer URL")
                            }
                            
                            onComplete(token)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting referrer", e)
                            onComplete(null)
                        } finally {
                            referrerClient.endConnection()
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Log.w(TAG, "Install Referrer not supported on this device")
                        markReferrerFetched() // Don't retry
                        onComplete(null)
                        referrerClient.endConnection()
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        Log.w(TAG, "Install Referrer service unavailable")
                        onComplete(null)
                        referrerClient.endConnection()
                    }
                    else -> {
                        Log.e(TAG, "Install Referrer failed with code: $responseCode")
                        onComplete(null)
                        referrerClient.endConnection()
                    }
                }
            }
            
            override fun onInstallReferrerServiceDisconnected() {
                Log.w(TAG, "Install Referrer service disconnected")
                // Could implement retry logic here if needed
            }
        })
    }
    
    /**
     * Extracts token parameter from referrer URL
     * 
     * Expected format: "token=abc123" or "utm_source=...&token=abc123"
     * 
     * @param referrerUrl The referrer URL from Play Store
     * @return Extracted token or null if not found
     */
    private fun extractTokenFromReferrer(referrerUrl: String?): String? {
        if (referrerUrl.isNullOrBlank()) return null
        
        try {
            // Decode URL in case it's encoded
            val decodedUrl = URLDecoder.decode(referrerUrl, "UTF-8")
            
            // Parse parameters (format: key1=value1&key2=value2)
            val params = decodedUrl.split("&").associate {
                val parts = it.split("=", limit = 2)
                val key = parts.getOrNull(0)?.trim() ?: ""
                val value = parts.getOrNull(1)?.trim() ?: ""
                key to value
            }
            
            // Extract token parameter
            return params["token"]?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing referrer URL", e)
            return null
        }
    }
    
    private fun saveReferrerData(referrerUrl: String, clickTimestamp: Long, installTimestamp: Long) {
        prefs.edit().apply {
            putString(KEY_REFERRER_URL, referrerUrl)
            putLong(KEY_CLICK_TIMESTAMP, clickTimestamp)
            putLong(KEY_INSTALL_TIMESTAMP, installTimestamp)
            putBoolean(KEY_REFERRER_FETCHED, true)
            apply()
        }
    }
    
    private fun markReferrerFetched() {
        prefs.edit().putBoolean(KEY_REFERRER_FETCHED, true).apply()
    }
    
    private fun isReferrerFetched(): Boolean {
        return prefs.getBoolean(KEY_REFERRER_FETCHED, false)
    }
    
    private fun getSavedReferrerUrl(): String? {
        return prefs.getString(KEY_REFERRER_URL, null)
    }
}
