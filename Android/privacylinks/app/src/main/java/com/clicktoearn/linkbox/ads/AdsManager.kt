package com.clicktoearn.linkbox.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.clicktoearn.linkbox.R
import com.clicktoearn.linkbox.ui.components.NativeAdShimmer
import com.clicktoearn.linkbox.ui.components.ShimmerBox
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive

object AdsManager {
    private const val TAG = "AdsManager"
    
    // ==================== CONFIGURATION ====================
    
    // AdMob Ad Unit IDs (Provided by USER)
    private const val ADMOB_APP_OPEN_ID = "" // Disabled - no unit in new panel
    private const val ADMOB_BANNER_ID = "/22649815059/linkbox_app/banner"
    private const val ADMOB_INTERSTITIAL_ID = "/22649815059/linkbox_app/interstitial"
    private const val ADMOB_NATIVE_ID = "/22649815059/linkbox_app/native"
    private const val ADMOB_REWARDED_INTERSTITIAL_ID = "/22649815059/linkbox_app/rewarded_interstitial"
    private const val ADMOB_REWARDED_ID = "/22649815059/linkbox_app/rewarded"

    // AdMob Test IDs (For Debugging)
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    // Remote Config Keys - Toggles
    const val KEY_SHOW_APP_OPEN = "show_app_open"
    const val KEY_SHOW_INTERSTITIAL = "show_interstitial"
    const val KEY_SHOW_BANNER = "show_banner"
    const val KEY_SHOW_ADAPTIVE_BANNER = "show_adaptive_banner"
    const val KEY_SHOW_REWARDED = "show_rewarded"
    const val KEY_SHOW_REWARDED_INTERSTITIAL = "show_rewarded_interstitial"
    const val KEY_SHOW_NATIVE = "show_native"
    const val KEY_SHOW_NATIVE_VIDEO = "show_native_video"

    // Time-based Limit Keys
    const val KEY_REWARDED_LIMIT_1MIN = "rewarded_limit_1min"
    const val KEY_REWARDED_LIMIT_5MIN = "rewarded_limit_5min"
    const val KEY_INTERSTITIAL_LIMIT_1MIN = "interstitial_limit_1min"
    const val KEY_INTERSTITIAL_LIMIT_5MIN = "interstitial_limit_5min"

    // Granular Placement Keys (Restored for compatibility)
    // Granular Placement Keys
    const val KEY_ASSETS_BANNER_TOP = "assets_banner_top"
    const val KEY_ASSETS_NATIVE_BOTTOM = "assets_native_bottom"
    const val KEY_HISTORY_BANNER_TOP = "history_banner_top"
    const val KEY_HISTORY_NATIVE_BOTTOM = "history_native_bottom"
    const val KEY_HISTORY_AD_OPEN_ASSETS = "history_ad_open_assets"
    const val KEY_HISTORY_AD_LINK_OPEN_REWARDED = "history_ad_link_open_rewarded"
    const val KEY_HISTORY_AD_OPEN_EVERY_TIME = "history_ad_open_every_time"
    const val KEY_HISTORY_AD_OPEN_AGAIN_TIME = "history_ad_open_again_time"
    const val KEY_HISTORY_AD_OPEN_SUB_ELEMENTS = "history_ad_open_sub_elements"
    const val KEY_SHARED_CONTENT_NATIVE_ABOVE_INFO = "shared_content_native_above_info"
    const val KEY_SHARED_CONTENT_BANNER_ABOVE_OPEN_BTN = "shared_content_banner_above_open_btn"
    const val KEY_SHARED_CONTENT_NATIVE_BELOW_OPEN_BTN = "shared_content_native_below_open_btn"
    const val KEY_SHARED_CONTENT_USE_REWARDED_INTERSTITIAL = "shared_content_use_rewarded_interstitial"
    const val KEY_SHARED_CONTENT_AD_ASSET_OPEN = "shared_content_ad_asset_open"
    const val KEY_SHARED_CONTENT_AD_LINK_OPEN_REWARDED = "shared_content_ad_link_open_rewarded"
    const val KEY_SHARED_CONTENT_AD_OPEN_EVERY_TIME = "shared_content_ad_open_every_time"
    const val KEY_SHARED_CONTENT_AD_SUB_ASSET_OPEN = "shared_content_ad_sub_asset_open"
    const val KEY_SHARED_CONTENT_AD_OPEN_ASSET_OPEN_AGAIN_TIME = "shared_content_ad_open_asset_open_again_time"
    const val KEY_SHARED_FOLDER_NATIVE_TOP = "shared_folder_native_top"
    const val KEY_SHARED_FOLDER_BANNER_BOTTOM = "shared_folder_banner_bottom"
    const val KEY_SHARED_FOLDER_AD_SUB_ASSET_OPEN = "shared_folder_ad_sub_asset_open"
    const val KEY_SHARED_FOLDER_AD_LINK_OPEN_REWARDED = "shared_folder_ad_link_open_rewarded"
    const val KEY_SHARED_FOLDER_AD_OPEN_AGAIN_TIME = "shared_folder_ad_open_again_time"
    const val KEY_SHOW_EXIT_INTERSTITIAL = "show_exit_interstitial"
    const val KEY_SHOW_EXIT_NATIVE = "show_exit_native"
    const val KEY_APP_WIDE_AD_APP_OPEN_AD = "app_wide_ad_app_open_ad"
    const val KEY_WEBVIEW_AD_INTERVALS = "webview_ad_intervals"
    const val KEY_WEBVIEW_AD_REPEAT_INTERVAL = "webview_ad_repeat_interval"
    const val KEY_HOME_LAYOUT_CONFIG = "home_layout_config"

    // IDs for Remote Config
    const val KEY_ID_APP_OPEN = "id_app_open"
    const val KEY_ID_INTERSTITIAL = "id_interstitial"
    const val KEY_ID_BANNER = "id_banner"
    const val KEY_ID_REWARDED = "id_rewarded"
    const val KEY_ID_REWARDED_INTERSTITIAL = "id_rewarded_interstitial"
    const val KEY_ID_NATIVE = "id_native"

    // Ad State
    private var appOpenAd: AppOpenAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null

    private var isLoadingAppOpen = false
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false
    private var isLoadingRewardedInterstitial = false
    private var isInitialized = false

    private val _nativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAdFlow = _nativeAd.asStateFlow()
    private var isLoadingNative = false
    
    private val _standardNativeAd = MutableStateFlow<NativeAd?>(null)
    val standardNativeAdFlow = _standardNativeAd.asStateFlow()
    private var isLoadingStandardNative = false

    // Limit Tracking
    private val rewardedShowTimes = mutableListOf<Long>()
    private val interstitialShowTimes = mutableListOf<Long>()
    private val placementLastShowTime = mutableMapOf<String, Long>()
    private var currentInterstitialPlacementKey: String? = null

    // UI Loading State
    val isAdLoading = MutableStateFlow(false)
    private val adScope = MainScope()

    private fun waitForAd(
        loadAdAction: () -> Unit,
        checkAdAvailable: () -> Boolean,
        onAdReady: () -> Unit,
        onTimeout: () -> Unit,
        timeoutMs: Long = 4000L
    ) {
        if (checkAdAvailable()) {
            onAdReady()
            return
        }

        isAdLoading.value = true
        loadAdAction()

        adScope.launch {
            val startTime = System.currentTimeMillis()
            var adReady = false
            
            // Poll for ad availability
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (checkAdAvailable()) {
                    adReady = true
                    break
                }
                delay(200) // Check every 200ms
            }
            
            // Ensure minimum display time of 1s if it was super fast (optional, but requested "minimum 2-3s")
            // The user said "minimum 2-3 seconds". Let's at least give it 2s to look deliberate if it loaded instantly 
            // BUT only if we actually showed the loader.
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 2000) {
                 delay(2000 - elapsed)
            }

            isAdLoading.value = false
            
            if (adReady) {
                onAdReady()
            } else {
                onTimeout()
            }
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        
        // Offload heavy AdMob initialization to a background thread
        // This prevents blocking the main thread during cold start
        Thread {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob Initialized: $status")
            }
        }.start()

        isInitialized = true
        setupRemoteConfig()
    }

    private fun setupRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0 
        }
        
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        val defaults = mapOf(
            KEY_SHOW_APP_OPEN to false,
            KEY_SHOW_INTERSTITIAL to true,
            KEY_SHOW_BANNER to true,
            KEY_SHOW_ADAPTIVE_BANNER to true,
            KEY_SHOW_REWARDED to true,
            KEY_SHOW_REWARDED_INTERSTITIAL to true,
            KEY_SHOW_NATIVE to true,
            KEY_SHOW_NATIVE_VIDEO to true,
            
            KEY_ID_APP_OPEN to ADMOB_APP_OPEN_ID,
            KEY_ID_INTERSTITIAL to ADMOB_INTERSTITIAL_ID,
            KEY_ID_BANNER to ADMOB_BANNER_ID,
            KEY_ID_REWARDED to ADMOB_REWARDED_ID,
            KEY_ID_REWARDED_INTERSTITIAL to ADMOB_REWARDED_INTERSTITIAL_ID,
            KEY_ID_NATIVE to ADMOB_NATIVE_ID,

            // Granular Placements
            KEY_ASSETS_BANNER_TOP to false,
            KEY_ASSETS_NATIVE_BOTTOM to true,
            KEY_HISTORY_BANNER_TOP to false,
            KEY_HISTORY_NATIVE_BOTTOM to true,
            KEY_HISTORY_AD_OPEN_ASSETS to true,
            KEY_HISTORY_AD_LINK_OPEN_REWARDED to true,
            KEY_HISTORY_AD_OPEN_EVERY_TIME to 1,
            KEY_HISTORY_AD_OPEN_AGAIN_TIME to 60,
            KEY_HISTORY_AD_OPEN_SUB_ELEMENTS to true,
            KEY_SHARED_CONTENT_NATIVE_ABOVE_INFO to false,
            KEY_SHARED_CONTENT_BANNER_ABOVE_OPEN_BTN to true,
            KEY_SHARED_CONTENT_NATIVE_BELOW_OPEN_BTN to true,
            KEY_SHARED_CONTENT_USE_REWARDED_INTERSTITIAL to true,
            KEY_SHARED_CONTENT_AD_ASSET_OPEN to true,
            KEY_SHARED_CONTENT_AD_LINK_OPEN_REWARDED to true,
            KEY_SHARED_CONTENT_AD_OPEN_EVERY_TIME to 1,
            KEY_SHARED_CONTENT_AD_SUB_ASSET_OPEN to true,
            KEY_SHARED_CONTENT_AD_OPEN_ASSET_OPEN_AGAIN_TIME to 60,
            KEY_SHARED_FOLDER_NATIVE_TOP to true,
            KEY_SHARED_FOLDER_BANNER_BOTTOM to true,
            KEY_SHARED_FOLDER_AD_SUB_ASSET_OPEN to true,
            KEY_SHARED_FOLDER_AD_LINK_OPEN_REWARDED to true,
            KEY_SHARED_FOLDER_AD_OPEN_AGAIN_TIME to 60,
            KEY_SHOW_EXIT_INTERSTITIAL to false,
            KEY_SHOW_EXIT_NATIVE to false,
            KEY_APP_WIDE_AD_APP_OPEN_AD to true,

            // WebView Ads
            KEY_WEBVIEW_AD_INTERVALS to "60,180,600,900,1200",
            KEY_WEBVIEW_AD_REPEAT_INTERVAL to 600,

            // Ad Limits (0 means unlimited, or set a high number)
            KEY_REWARDED_LIMIT_1MIN to 2,
            KEY_REWARDED_LIMIT_5MIN to 5,
            KEY_INTERSTITIAL_LIMIT_1MIN to 2,
            KEY_INTERSTITIAL_LIMIT_5MIN to 5,

            // Home Layout fallback
            KEY_HOME_LAYOUT_CONFIG to """
                [
                  {"type": "ad_native_video", "id": "hero_video", "visible": true},
                  {"type": "section", "id": "supers", "visible": true},
                  {"type": "ad_native", "id": "mid_native", "visible": true},
                  {"type": "section", "id": "apps", "visible": true},
                  {"type": "section", "id": "games", "visible": true},
                  {"type": "ad_banner", "id": "bottom_banner", "visible": true},
                  {"type": "section", "id": "content", "visible": true}
                ]
            """.trimIndent()
        )
        
        remoteConfig.setDefaultsAsync(defaults)
        remoteConfig.fetchAndActivate()
    }

    internal fun isAdEnabled(key: String): Boolean {
        // Firebase.remoteConfig.getBoolean() returns the default value if not fetched yet
        // This respects the defaults set in setDefaultsAsync()
        return Firebase.remoteConfig.getBoolean(key)
    }

    private fun getAdUnitId(key: String, prodId: String, testId: String): String {
        val remoteId = Firebase.remoteConfig.getString(key)
        val finalId = if (remoteId.isNotBlank()) remoteId else prodId
        return if (com.clicktoearn.linkbox.BuildConfig.DEBUG) testId else finalId
    }

    sealed class AdCheckResult {
        object Success : AdCheckResult()
        data class LimitReached(val message: String) : AdCheckResult()
        object Disabled : AdCheckResult()
    }

    // Persistent counter for Shared Content Ad Frequency
    private const val PREF_SHARED_OPEN_COUNT = "pref_shared_open_count"
    
    fun shouldShowSharedContentAd(context: Context): Boolean {
        val frequency = Firebase.remoteConfig.getLong(KEY_SHARED_CONTENT_AD_OPEN_EVERY_TIME).toInt()
        Log.d(TAG, "SharedContentAd Frequency Config: $frequency")
        if (frequency <= 0) return false
        if (frequency == 1) return true
        
        val prefs = context.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val count = prefs.getLong(PREF_SHARED_OPEN_COUNT, 0L)
        val nextCount = count + 1
        prefs.edit().putLong(PREF_SHARED_OPEN_COUNT, nextCount).apply()
        
        Log.d(TAG, "SharedContentAd Open Count: $nextCount")
        // logic: 1st, then (1+N)th... means (count % N == 1)
        // Example N=2: 1 -> 1%2!=0 wait. (1-1)%2 == 0 -> true.
        // Let's trace nextCount: 1, 2, 3, 4, 5
        // N=2: 1->True, 2->False, 3->True. Formula: (nextCount - 1) % frequency == 0
        // (1-1)%2 = 0 (True). (2-1)%2 = 1 (False). (3-1)%2 = 0 (True).
        return (nextCount - 1) % frequency == 0L
    }

    // Persistent counter for History Ad Frequency
    private const val PREF_HISTORY_OPEN_COUNT = "pref_history_open_count"

    fun shouldShowHistoryAd(context: Context): Boolean {
        val frequency = Firebase.remoteConfig.getLong(KEY_HISTORY_AD_OPEN_EVERY_TIME).toInt()
        if (frequency <= 0) return false
        if (frequency == 1) return true

        val prefs = context.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val count = prefs.getLong(PREF_HISTORY_OPEN_COUNT, 0L)
        val nextCount = count + 1
        prefs.edit().putLong(PREF_HISTORY_OPEN_COUNT, nextCount).apply()
        
        return (nextCount - 1) % frequency == 0L
    }

    fun getNextWebViewAdTime(elapsedSeconds: Long): Long? {
        val intervalsString = Firebase.remoteConfig.getString(KEY_WEBVIEW_AD_INTERVALS)
        if (intervalsString.isBlank()) return null

        val intervals = try {
            intervalsString.split(",").map { it.trim().toLong() }.sorted()
        } catch (e: Exception) {
            emptyList<Long>()
        }

        if (intervals.isEmpty()) return null

        // Find first interval strictly greater than elapsedSeconds
        val nextFixed = intervals.firstOrNull { it > elapsedSeconds }
        if (nextFixed != null) return nextFixed

        // If we are past all fixed intervals, use repeat interval
        val maxInterval = intervals.last()
        val repeat = Firebase.remoteConfig.getLong(KEY_WEBVIEW_AD_REPEAT_INTERVAL)
        if (repeat <= 0) return null

        // Calculate next repeat slot
        // diff = elapsed - max
        // If elapsed == max, next is max + repeat
        // If elapsed = max + 1, next is max + repeat
        // If elapsed = max + repeat + 1, next is max + 2*repeat
        
        val diff = elapsedSeconds - maxInterval
        // If elapsed < maxInterval (shouldn't happen given logic above), return max
        if (diff < 0) return maxInterval + repeat
        
        val multiplier = (diff / repeat) + 1
        return maxInterval + (multiplier * repeat)
    }


    private fun canShowAd(type: String, placementKey: String? = null): AdCheckResult {
        val now = System.currentTimeMillis()
        val oneMinAgo = now - 60 * 1000
        val fiveMinAgo = now - 5 * 60 * 1000

        val (showTimes, limit1MinKey, limit5MinKey) = when (type) {
            "rewarded" -> Triple(rewardedShowTimes, KEY_REWARDED_LIMIT_1MIN, KEY_REWARDED_LIMIT_5MIN)
            "interstitial" -> Triple(interstitialShowTimes, KEY_INTERSTITIAL_LIMIT_1MIN, KEY_INTERSTITIAL_LIMIT_5MIN)
            else -> return AdCheckResult.Success
        }

        // Clean up old timestamps
        showTimes.removeAll { it < fiveMinAgo }

        val limit1Min = Firebase.remoteConfig.getLong(limit1MinKey).toInt()
        val limit5Min = Firebase.remoteConfig.getLong(limit5MinKey).toInt()

        val count1Min = showTimes.count { it >= oneMinAgo }
        val count5Min = showTimes.count { it >= fiveMinAgo }

        if (limit1Min > 0 && count1Min >= limit1Min) {
            Log.d(TAG, "$type ad limit reached for 1 minute: $count1Min >= $limit1Min")
            val nextAvailable = showTimes.filter { it >= oneMinAgo }.minOrNull()?.let { (it + 60*1000 - now) / 1000 + 1 } ?: 60
            return AdCheckResult.LimitReached("Ad Limit reached. Try in $nextAvailable seconds")
        }
        if (limit5Min > 0 && count5Min >= limit5Min) {
            Log.d(TAG, "$type ad limit reached for 5 minutes: $count5Min >= $limit5Min")
            val oldestInLimit = showTimes.minOrNull() ?: fiveMinAgo
            val waitSeconds = (oldestInLimit + 5*60*1000 - now) / 1000 + 1
            val waitMinutes = waitSeconds / 60
            val waitSecRemaining = waitSeconds % 60
            val timeText = if (waitMinutes > 0) "$waitMinutes min $waitSecRemaining sec" else "$waitSecRemaining sec"
            return AdCheckResult.LimitReached("Standard Limit reached. Try in $timeText")
        }

        if (placementKey != null) {
            val lastShow = placementLastShowTime[placementKey] ?: 0L
            // Default to 60s if no specific cooldown key is provided or found
            val cooldownSeconds = if (type == "interstitial" && placementKey.startsWith("history")) {
                Firebase.remoteConfig.getLong(KEY_HISTORY_AD_OPEN_AGAIN_TIME)
            } else if (type == "interstitial" && placementKey.startsWith("shared_folder")) {
                Firebase.remoteConfig.getLong(KEY_SHARED_FOLDER_AD_OPEN_AGAIN_TIME)
            } else if (type == "interstitial" && placementKey.startsWith("shared")) {
                Firebase.remoteConfig.getLong(KEY_SHARED_CONTENT_AD_OPEN_ASSET_OPEN_AGAIN_TIME)
            } else {
                60L
            }
            
            if (now - lastShow < cooldownSeconds * 1000) {
                Log.d(TAG, "Ad skipped for placement $placementKey (last show < ${cooldownSeconds}s ago)")
                val waitSeconds = (lastShow + cooldownSeconds * 1000 - now) / 1000 + 1
                return AdCheckResult.LimitReached("Please wait $waitSeconds seconds before next ad")
            }
        }

        return AdCheckResult.Success
    }

    private fun recordAdShown(type: String, placementKey: String? = null) {
        val now = System.currentTimeMillis()
        when (type) {
            "rewarded" -> rewardedShowTimes.add(now)
            "interstitial" -> interstitialShowTimes.add(now)
        }
        if (placementKey != null) {
            placementLastShowTime[placementKey] = now
        }
    }

    // App Open Ad
    fun loadAppOpenAd(context: Context) {
        if (isLoadingAppOpen || appOpenAd != null || !isAdEnabled(KEY_SHOW_APP_OPEN)) return
        
        isLoadingAppOpen = true
        val adUnitId = getAdUnitId(KEY_ID_APP_OPEN, ADMOB_APP_OPEN_ID, TEST_APP_OPEN)
        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAppOpen = false
                    Log.d(TAG, "App Open Ad loaded.")
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAppOpen = false
                    Log.e(TAG, "App Open Ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onAdDismissed: (() -> Unit)? = null) {
        if (appOpenAd != null) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    onAdDismissed?.invoke()
                    loadAppOpenAd(activity)
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    appOpenAd = null
                    onAdDismissed?.invoke()
                }
            }
            appOpenAd?.show(activity)
        } else {
            onAdDismissed?.invoke()
            loadAppOpenAd(activity)
        }
    }

    fun loadAndShowAppOpenAd(activity: Activity) {
        if (!isAdEnabled(KEY_SHOW_APP_OPEN)) {
            Log.d(TAG, "App Open Ad is disabled in Remote Config.")
            return
        }

        if (appOpenAd != null) {
            Log.d(TAG, "App Open Ad already loaded, showing it.")
            showAppOpenAd(activity)
            return
        }

        if (isLoadingAppOpen) {
            Log.d(TAG, "App Open Ad is already loading.")
            return
        }

        isLoadingAppOpen = true
        val adUnitId = getAdUnitId(KEY_ID_APP_OPEN, ADMOB_APP_OPEN_ID, TEST_APP_OPEN)
        Log.d(TAG, "Loading and showing App Open Ad with ID: $adUnitId")
        
        AppOpenAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    isLoadingAppOpen = false
                    appOpenAd = ad
                    Log.d(TAG, "App Open Ad loaded successfully, now showing.")
                    showAppOpenAd(activity)
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAppOpen = false
                    Log.e(TAG, "loadAndShowAppOpenAd failed: ${loadAdError.message} (Code: ${loadAdError.code})")
                }
            }
        )
    }

    private var pendingInterstitialCallback: (() -> Unit)? = null

    // Interstitial Ad
    fun loadInterstitialAd(context: Context) {
        if (isLoadingInterstitial || interstitialAd != null || !isAdEnabled(KEY_SHOW_INTERSTITIAL)) return

        isLoadingInterstitial = true
        val adUnitId = getAdUnitId(KEY_ID_INTERSTITIAL, ADMOB_INTERSTITIAL_ID, TEST_INTERSTITIAL)
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "Interstitial loaded.")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingInterstitial = false
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, placementKey: String? = null, onAdDismissed: () -> Unit) {
        if (!isAdEnabled(KEY_SHOW_INTERSTITIAL)) {
            onAdDismissed()
            return
        }
        
        waitForAd(
            loadAdAction = { loadInterstitialAd(activity) },
            checkAdAvailable = { interstitialAd != null },
            onAdReady = {
                if (canShowAd("interstitial", placementKey) is AdCheckResult.Success) {
                    pendingInterstitialCallback = onAdDismissed
                    currentInterstitialPlacementKey = placementKey
                    var adWasShownProperly = false
                    
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            adWasShownProperly = true
                            Log.d(TAG, "Interstitial ad displayed successfully")
                        }
                        override fun onAdDismissedFullScreenContent() {
                            if (adWasShownProperly) {
                                recordAdShown("interstitial", currentInterstitialPlacementKey)
                            }
                            interstitialAd = null
                            currentInterstitialPlacementKey = null
                            pendingInterstitialCallback?.invoke()
                            pendingInterstitialCallback = null
                            loadInterstitialAd(activity)
                        }
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            adWasShownProperly = false
                            interstitialAd = null
                            currentInterstitialPlacementKey = null
                            pendingInterstitialCallback?.invoke()
                            pendingInterstitialCallback = null
                            loadInterstitialAd(activity)
                        }
                    }
                    interstitialAd?.show(activity)
                } else {
                    onAdDismissed()
                    loadInterstitialAd(activity)
                }
            },
            onTimeout = {
                Log.d(TAG, "Interstitial Ad timeout - proceeding without ad")
                onAdDismissed()
                loadInterstitialAd(activity)
            }
        )
    }

    // Rewarded Ad
    fun loadRewardedAd(context: Context) {
        if (isLoadingRewarded || rewardedAd != null || !isAdEnabled(KEY_SHOW_REWARDED)) return
        
        isLoadingRewarded = true
        val adUnitId = getAdUnitId(KEY_ID_REWARDED, ADMOB_REWARDED_ID, TEST_REWARDED)
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    Log.d(TAG, "Rewarded ad loaded.")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingRewarded = false
                    rewardedAd = null
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, strict: Boolean = false, onRewardEarned: () -> Unit, onAdClosed: ((String?) -> Unit)? = null) {
        if (!isAdEnabled(KEY_SHOW_REWARDED)) {
            if (strict) onAdClosed?.invoke("Ad services are currently disabled")
            else onRewardEarned()
            return
        }

        val adCheck = canShowAd("rewarded")
        if (adCheck is AdCheckResult.LimitReached) {
            if (strict) onAdClosed?.invoke(adCheck.message)
            else onRewardEarned()
            return
        }

        waitForAd(
            loadAdAction = { loadRewardedAd(activity) },
            checkAdAvailable = { rewardedAd != null },
            onAdReady = {
                var rewardEarned = false
                rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        recordAdShown("rewarded")
                        rewardedAd = null
                        loadRewardedAd(activity)
                        if (!rewardEarned) {
                            onAdClosed?.invoke("Reward cancelled for closing ad early")
                        }
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        rewardedAd = null
                        loadRewardedAd(activity)
                        onAdClosed?.invoke("Failed to display ad. Please try again.")
                    }
                }
                rewardedAd?.show(activity) { rewardItem ->
                    rewardEarned = true
                    onRewardEarned()
                }
            },
            onTimeout = {
                if (strict) {
                    onAdClosed?.invoke("Ad not ready yet. Please try again.")
                } else {
                    onRewardEarned()
                }
                loadRewardedAd(activity)
            }
        )
    }

    // Rewarded Interstitial Ad
    fun loadRewardedInterstitialAd(context: Context) {
        if (isLoadingRewardedInterstitial || rewardedInterstitialAd != null || !isAdEnabled(KEY_SHOW_REWARDED_INTERSTITIAL)) return
        
        isLoadingRewardedInterstitial = true
        val adUnitId = getAdUnitId(KEY_ID_REWARDED_INTERSTITIAL, ADMOB_REWARDED_INTERSTITIAL_ID, TEST_REWARDED_INTERSTITIAL)
        RewardedInterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isLoadingRewardedInterstitial = false
                    Log.d(TAG, "Rewarded interstitial loaded.")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingRewardedInterstitial = false
                    rewardedInterstitialAd = null
                    Log.e(TAG, "Rewarded interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    fun showRewardedInterstitialAd(activity: Activity, placementKey: String? = null, strict: Boolean = false, onRewardEarned: () -> Unit, onAdClosed: ((String?) -> Unit)? = null) {
        if (!isAdEnabled(KEY_SHOW_REWARDED_INTERSTITIAL)) {
            if (strict) onAdClosed?.invoke("Ad services are currently disabled")
            else onRewardEarned()
            return
        }

        val adCheck = canShowAd("rewarded", placementKey)
        if (adCheck is AdCheckResult.LimitReached) {
            if (strict) onAdClosed?.invoke(adCheck.message)
            else onRewardEarned()
            return
        }

        waitForAd(
            loadAdAction = { loadRewardedInterstitialAd(activity) },
            checkAdAvailable = { rewardedInterstitialAd != null },
            onAdReady = {
                var rewardEarned = false
                rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        if (rewardEarned) {
                            recordAdShown("rewarded", placementKey)
                            onRewardEarned()
                        } else {
                            onAdClosed?.invoke("Reward cancelled for closing ad early")
                        }
                        rewardedInterstitialAd = null
                        loadRewardedInterstitialAd(activity)
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        rewardedInterstitialAd = null
                        loadRewardedInterstitialAd(activity)
                        onAdClosed?.invoke("Failed to display ad. Please try again.")
                    }
                }
                rewardedInterstitialAd?.show(activity) { rewardItem ->
                    rewardEarned = true
                }
            },
            onTimeout = {
                if (strict) {
                    onAdClosed?.invoke("Ad not ready yet. Please try again.")
                } else {
                    onRewardEarned()
                }
                loadRewardedInterstitialAd(activity)
            }
        )
    }

    // Placeholder for compatibility
    fun preloadBannerAds() {}
    
    private fun getAdSize(context: Context, widthDp: Int): AdSize {
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    @Composable
    fun BannerAdView(modifier: Modifier = Modifier) {
        if (!isAdEnabled(KEY_SHOW_BANNER)) return
        val adUnitId = getAdUnitId(KEY_ID_BANNER, ADMOB_BANNER_ID, TEST_BANNER)
        val context = LocalContext.current
        
        BoxWithConstraints(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Calculate available width in DP. 
            // If infinite (e.g. in scrolling container with no width constraint), fall back to screen width.
            val screenWidth = context.resources.displayMetrics.widthPixels
            val density = context.resources.displayMetrics.density
            val defaultWidth = (screenWidth / density).toInt()
            
            val availableWidth = if (maxWidth.value.isFinite() && maxWidth.value > 0) {
                maxWidth.value.toInt()
            } else {
                defaultWidth
            }
            
            // Prevent ad requests for 0 width (can happen during layout passes)
            if (availableWidth <= 0) return@BoxWithConstraints

            val adView = remember(adUnitId, availableWidth) { 
                AdView(context).apply {
                    setAdSize(getAdSize(context, availableWidth))
                    this.adUnitId = adUnitId
                    
                    // Create extra bundle for Collapsible Banner
                    val extras = android.os.Bundle()
                    extras.putString("collapsible", "bottom")
                    
                    val adRequest = AdRequest.Builder()
                        .addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras)
                        .build()
                        
                    loadAd(adRequest)
                }
            }

            // Cleanup AdView when it leaves the composition
            DisposableEffect(adView) {
                onDispose {
                    adView.destroy()
                }
            }

            AndroidView(
                modifier = Modifier.wrapContentSize(),
                factory = { adView }
            )
        }
    }

    @Composable
    fun AdaptiveBannerAdView() {
        BannerAdView()
    }

    @Composable
    fun MediumRectangleAdView() {
        if (!isAdEnabled(KEY_SHOW_BANNER)) return
        val adUnitId = getAdUnitId(KEY_ID_BANNER, ADMOB_BANNER_ID, TEST_BANNER) // Using Banner ID for simplicity
        val context = LocalContext.current

        val adView = remember(adUnitId) {
            AdView(context).apply {
                setAdSize(AdSize.MEDIUM_RECTANGLE)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.width(300.dp).height(250.dp),
                factory = { adView }
            )
        }
    }

    // NATIVE ADS
    fun loadNativeAd(context: Context, forceRefresh: Boolean = false) {
        if (isLoadingNative || (!forceRefresh && _nativeAd.value != null) || !isAdEnabled(KEY_SHOW_NATIVE_VIDEO)) return
        
        isLoadingNative = true
        val adUnitId = getAdUnitId(KEY_ID_NATIVE, ADMOB_NATIVE_ID, TEST_NATIVE)
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad : NativeAd ->
                val oldAd = _nativeAd.value
                _nativeAd.value = ad
                isLoadingNative = false
                // Destroy the old ad to free resources and prevent memory leaks
                oldAd?.destroy()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingNative = false
                    Log.e(TAG, "Native ad failed to load: ${adError.message}")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .build()
            )
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun loadStandardNativeAd(context: Context, forceRefresh: Boolean = false) {
        if (isLoadingStandardNative || (!forceRefresh && _standardNativeAd.value != null) || !isAdEnabled(KEY_SHOW_NATIVE)) return
        
        isLoadingStandardNative = true
        val adUnitId = getAdUnitId(KEY_ID_NATIVE, ADMOB_NATIVE_ID, TEST_NATIVE)
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad : NativeAd ->
                _standardNativeAd.value = ad
                isLoadingStandardNative = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingStandardNative = false
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .build()
            )
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    @Composable
    fun NativeVideoAdView(modifier: Modifier = Modifier) {
        NativeAdView(modifier)
    }

    @Composable
    fun NativeAdView(modifier: Modifier = Modifier) {
        if (!isAdEnabled(KEY_SHOW_NATIVE)) return
        
        val nativeAd by nativeAdFlow.collectAsState()
        val context = LocalContext.current

        // Auto-refresh logic: Refresh every 60 seconds
        LaunchedEffect(Unit) {
            while (true) {
                delay(60_000L) // 60 seconds
                // Only refresh if the screen is active (this coroutine is active)
                loadNativeAd(context, forceRefresh = true)
            }
        }

        if (nativeAd != null) {
             AndroidView(
                modifier = modifier.fillMaxWidth(),
                factory = { ctx ->
                     val inflater = LayoutInflater.from(ctx)
                     val adViewView = inflater.inflate(R.layout.native_ad_layout_admob, null) as NativeAdView
                     populateNativeAdView(nativeAd!!, adViewView)
                     adViewView
                },
                update = { view ->
                    if (nativeAd != null) {
                        populateNativeAdView(nativeAd!!, view)
                    }
                }
             )
        } else {
            // Show placeholder or shimmer while loading initially
             NativeAdPlaceholder(modifier)
        }
    }
    
    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.mediaView = adView.findViewById(R.id.ad_media)

        // Headline (required)
        (adView.headlineView as? TextView)?.text = nativeAd.headline

        // Body (optional)
        if (nativeAd.body != null) {
            (adView.bodyView as? TextView)?.apply {
                text = nativeAd.body
                visibility = View.VISIBLE
            }
        } else {
            adView.bodyView?.visibility = View.GONE
        }

        // Call to Action (optional but usually present)
        if (nativeAd.callToAction != null) {
            (adView.callToActionView as? android.widget.Button)?.apply {
                text = nativeAd.callToAction
                visibility = View.VISIBLE
            }
        } else {
            adView.callToActionView?.visibility = View.GONE
        }

        // App Icon (optional)
        if (nativeAd.icon != null) {
            (adView.iconView as? ImageView)?.apply {
                setImageDrawable(nativeAd.icon?.drawable)
                visibility = View.VISIBLE
            }
        } else {
            adView.iconView?.visibility = View.GONE
        }

        // Media View (optional - show only if has media content)
        val mediaView = adView.mediaView
        if (mediaView != null && nativeAd.mediaContent != null) {
            mediaView.mediaContent = nativeAd.mediaContent
            mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
            mediaView.visibility = View.VISIBLE
        } else {
            mediaView?.visibility = View.GONE
        }

        adView.setNativeAd(nativeAd)
    }

    fun openAdInspector(context: Context) {
        MobileAds.openAdInspector(context) { error ->
            if (error != null) {
                Log.e(TAG, "Ad Inspector failed to open: ${error.message}")
            } else {
                Log.d(TAG, "Ad Inspector closed.")
            }
        }
    }

    @Composable
    fun NativeAdPlaceholder(modifier: Modifier = Modifier) {
        NativeAdShimmer(modifier)
    }

    @Composable
    fun RectangleAdPlaceholder(modifier: Modifier = Modifier) {
        ShimmerBox(modifier = modifier.fillMaxWidth().height(250.dp))
    }
}
