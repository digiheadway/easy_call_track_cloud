package com.clicktoearn.linkbox

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.clicktoearn.linkbox.data.FirestoreRepository
import com.clicktoearn.linkbox.data.LinkBoxRepository
import com.clicktoearn.linkbox.data.local.LinkBoxDatabase
import com.clicktoearn.linkbox.utils.InstallReferrerManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class LinkBoxApp : Application() {
    
    // Application-level coroutine scope for background tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Room Database (local-first storage)
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            LinkBoxDatabase::class.java,
            "linkbox_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure Firestore settings for optimal performance - this is fast and local
        configureFirestore()
        
        // Initialize deferred token from prefs
        _deferredToken.value = getPendingDeferredToken()

        // Initialize crucial SDKs immediately
        com.clicktoearn.linkbox.ads.AdsManager.init(this)
        
        // Track install only once on first app launch (background, non-blocking)
        trackInstallOnce()
        
        // Defer non-critical initialization to reduce cold start time
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            detectDeferredDeepLink()
        }

    }
    
    /**
     * Configure Firestore with optimized cache settings for faster data access
     */
    private fun configureFirestore() {
        val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.persistentCacheSettings {
                    // 100MB cache size for better offline experience
                    setSizeBytes(100 * 1024 * 1024)
                }
            )
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
        android.util.Log.d("LinkBoxApp", "Firestore configured with 100MB cache")
    }
    
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("linkbox_prefs", Context.MODE_PRIVATE)
    }
    

    
    fun getSavedUsername(): String? = prefs.getString("saved_username", null)
    
    fun saveUsername(username: String) {
        prefs.edit().putString("saved_username", username).apply()
    }
    
    fun clearSavedUsername() {
        prefs.edit().remove("saved_username").apply()
    }
    
    // ==================== Cached Points (for instant UI) ====================
    fun getCachedPoints(): Int = prefs.getInt("cached_points", 0)
    
    fun saveCachedPoints(points: Int) {
        prefs.edit().putInt("cached_points", points).apply()
    }

    fun clearCachedPoints() {
        prefs.edit().remove("cached_points").apply()
    }

    // ==================== Cached Premium Status (for instant Ad gating) ====================
    fun isPremiumCached(): Boolean = prefs.getBoolean("cached_is_premium", false)
    
    fun savePremiumStatus(isPremium: Boolean) {
        prefs.edit().putBoolean("cached_is_premium", isPremium).apply()
    }

    private var _sampleDataInjectedInSession = false
    fun hasInjectedSampleData(): Boolean = _sampleDataInjectedInSession
    fun markSampleDataInjected() { _sampleDataInjectedInSession = true }
    fun resetSessionFlags() {
        _sampleDataInjectedInSession = false
    }
    
    // Cloud repository (Firestore - for sync and shared content)
    val repository by lazy {
        FirestoreRepository(
            firestore = FirebaseFirestore.getInstance(),
            context = this
        )
    }
    
    // Local repository (Room - for local-first storage)
    val localRepository by lazy {
        LinkBoxRepository(
            dao = database.dao(),
            firestore = FirebaseFirestore.getInstance()
        )
    }
    
    /**
     * Detects deferred deep link from Play Store install referrer
     * Runs on first app launch to extract token from referrer parameter
     */
    private fun detectDeferredDeepLink() {
        // Only check for deferred deep link on the very first app launch
        if (!isFirstAppLaunch()) return

        val referrerManager = InstallReferrerManager(this)
        referrerManager.fetchInstallReferrer { params, _ ->
            val token = params["token"]
            
            // Pass other params to analytics or logic if needed
            val landingPage = params["landing"] ?: "landingpage0"
            val uniqueId = params["uniqueid"]
            
            if (token != null) {
                android.util.Log.d("LinkBoxApp", "Deferred deep link detected with token: $token")
                android.util.Log.d("LinkBoxApp", "Landing Page: $landingPage, Unique ID: $uniqueId")
                
                // Store the token for MainActivity to process
                setPendingDeferredToken(token, true)
                
                // You could also store uniqueId if needed for tracking
                if (uniqueId != null) {
                    prefs.edit().putString("install_unique_id", uniqueId).apply()
                }
            }
            // Always mark app as launched so we don't check again
            markAppLaunched()
        }
    }
    
    private val _deferredToken = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val deferredToken: kotlinx.coroutines.flow.StateFlow<String?> = _deferredToken.asStateFlow()

    /**
     * Stores a pending deferred deep link token with install status
     * This token will be processed by MainActivity after user login
     */
    private fun setPendingDeferredToken(token: String, isNewInstall: Boolean) {
        prefs.edit().apply {
            putString("pending_deferred_token", token)
            putBoolean("pending_deferred_is_new_install", isNewInstall)
            apply()
        }
        _deferredToken.value = token
    }
    
    /**
     * Gets the pending deferred deep link token if available
     */
    fun getPendingDeferredToken(): String? {
        return prefs.getString("pending_deferred_token", null)
    }
    
    /**
     * Gets whether the pending deferred deep link is from a new install
     */
    fun isPendingDeferredNewInstall(): Boolean {
        return prefs.getBoolean("pending_deferred_is_new_install", false)
    }
    
    /**
     * Clears the pending deferred deep link token after processing
     */
    fun clearPendingDeferredToken() {
        prefs.edit().apply {
            remove("pending_deferred_token")
            remove("pending_deferred_is_new_install")
            apply()
        }
        _deferredToken.value = null
    }
    
    /**
     * Checks if this is the first app launch (for new install tracking)
     */
    private fun isFirstAppLaunch(): Boolean {
        return !prefs.getBoolean("app_launched_before", false)
    }
    
    /**
     * Marks that the app has been launched (no longer first launch)
     */
    private fun markAppLaunched() {
        prefs.edit().putBoolean("app_launched_before", true).apply()
    }
    
    /**
     * Tracks app install by calling the backend API only once on first app open.
     * This is non-blocking and runs in the background.
     */
    private fun trackInstallOnce() {
        // Check if install has already been tracked
        if (prefs.getBoolean("install_tracked", false)) {
            android.util.Log.d("LinkBoxApp", "Install already tracked, skipping")
            return
        }
        
        // Use InstallReferrerManager to get referrer info
        val referrerManager = InstallReferrerManager(this)
        
        // Fetch referrer first (or get cached) then track install
        referrerManager.fetchInstallReferrer { _, referrerUrl -> 
            // Launch background coroutine to call the install tracking URL
            applicationScope.launch {
                try {
                    val baseUrl = "https://privacy.be6.in/log_flow.php?action=install"
                    val urlBuilder = StringBuilder(baseUrl)
                    
                    if (!referrerUrl.isNullOrBlank()) {
                        try {
                            val encodedReferrer = java.net.URLEncoder.encode(referrerUrl, "UTF-8")
                            urlBuilder.append("&referrer=").append(encodedReferrer)
                        } catch (e: Exception) {
                            android.util.Log.e("LinkBoxApp", "Error encoding referrer", e)
                        }
                    }
                    
                    val finalUrl = urlBuilder.toString()
                    android.util.Log.d("LinkBoxApp", "Tracking install with URL: $finalUrl")
                    
                    val url = URL(finalUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    
                    val responseCode = connection.responseCode
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Mark install as tracked so it never triggers again
                        prefs.edit().putBoolean("install_tracked", true).apply()
                        android.util.Log.d("LinkBoxApp", "Install tracked successfully")
                    } else {
                        android.util.Log.e("LinkBoxApp", "Install tracking failed: HTTP $responseCode")
                    }
                    
                    connection.disconnect()
                } catch (e: Exception) {
                    android.util.Log.e("LinkBoxApp", "Install tracking error: ${e.message}")
                }
            }
        }
    }
}
