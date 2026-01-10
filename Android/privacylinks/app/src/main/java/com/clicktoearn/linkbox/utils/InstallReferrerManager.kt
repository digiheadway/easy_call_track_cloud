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
    /**
     * Fetches install referrer from Play Store
     * Only fetches once - subsequent calls return cached data
     * 
     * @param onComplete Callback with extracted parameters map and full referrer URL
     */
    fun fetchInstallReferrer(onComplete: (Map<String, String>, String?) -> Unit) {
        // Check if already fetched
        if (isReferrerFetched()) {
            Log.d(TAG, "Referrer already fetched, returning cached token")
            val savedReferrerUrl = getSavedReferrerUrl()
            onComplete(extractParamsFromReferrer(savedReferrerUrl), savedReferrerUrl)
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
                            
                            // Extract params from referrer URL
                            val params = extractParamsFromReferrer(referrerUrl)
                            val token = params["token"]
                            
                            if (token != null) {
                                Log.d(TAG, "Extracted token from referrer: $token")
                            } else {
                                Log.d(TAG, "No token found in referrer URL")
                            }
                            
                            onComplete(params, referrerUrl)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting referrer", e)
                            onComplete(emptyMap(), null)
                        } finally {
                            referrerClient.endConnection()
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Log.w(TAG, "Install Referrer not supported on this device")
                        markReferrerFetched() // Don't retry
                        onComplete(emptyMap(), null)
                        referrerClient.endConnection()
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        Log.w(TAG, "Install Referrer service unavailable")
                        onComplete(emptyMap(), null)
                        referrerClient.endConnection()
                    }
                    else -> {
                        Log.e(TAG, "Install Referrer failed with code: $responseCode")
                        onComplete(emptyMap(), null)
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
     * Extracts parameters from referrer URL
     * 
     * Expected format: "token=abc123&landing=landing1&uniqueid=xyz..."
     * 
     * @param referrerUrl The referrer URL from Play Store
     * @return Map of parameters or empty map if none found
     */
    fun extractParamsFromReferrer(referrerUrl: String?): Map<String, String> {
        if (referrerUrl.isNullOrBlank()) return emptyMap()
        
        try {
            // Log the raw referrer for debugging
            Log.d(TAG, "Parsing referrer URL: $referrerUrl")
            
            // 1. Try parsing the raw URL first (in case it's not encoded)
            var params = parseQueryString(referrerUrl)
            
            // 2. If valid token found, return immediately
            if (params.containsKey("token")) {
                Log.d(TAG, "Found token in raw referrer: ${params["token"]}")
                return params
            }
            
            // 3. Try decoding and parsing
            val decodedUrl = URLDecoder.decode(referrerUrl, "UTF-8")
            if (decodedUrl != referrerUrl) {
                Log.d(TAG, "Parsing decoded referrer: $decodedUrl")
                val decodedParams = parseQueryString(decodedUrl)
                if (decodedParams.isNotEmpty()) {
                    params = decodedParams
                }
            }
            
            return params
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing referrer URL", e)
            return emptyMap()
        }
    }

    private fun parseQueryString(url: String): Map<String, String> {
        return url.split("&").associate {
            val parts = it.split("=", limit = 2)
            val key = parts.getOrNull(0)?.trim() ?: ""
            val value = parts.getOrNull(1)?.trim() ?: ""
            key to value
        }.filterKeys { it.isNotBlank() }
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
