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

object AdsManager {
    private const val TAG = "AdsManager"
    
    // ==================== CONFIGURATION ====================
    
    // AdMob Ad Unit IDs (Production)
    private const val ADMOB_APP_OPEN_ID = "ca-app-pub-2002073902256509/6094052821"
    private const val ADMOB_BANNER_ID = "ca-app-pub-2002073902256509/7885646646"
    private const val ADMOB_INTERSTITIAL_ID = "ca-app-pub-2002073902256509/8720216169"
    private const val ADMOB_NATIVE_ID = "ca-app-pub-2002073902256509/5259483304"
    private const val ADMOB_REWARDED_INTERSTITIAL_ID = "ca-app-pub-2002073902256509/7407134493"
    private const val ADMOB_REWARDED_ID = "ca-app-pub-2002073902256509/7803893775"

    // AdMob Test IDs (For Debug builds)
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
        timeoutMs: Long = 8000L
    ) {
        Log.d(TAG, "waitForAd: Start checkAvailable=${checkAdAvailable()}")
        if (checkAdAvailable()) {
            Log.d(TAG, "waitForAd: Ad already available, showing immediately")
            onAdReady()
            return
        }

        Log.d(TAG, "waitForAd: Ad not available, starting load and timer")
        isAdLoading.value = true
        loadAdAction()

        adScope.launch {
            val startTime = System.currentTimeMillis()
            var adReady = false
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (checkAdAvailable()) {
                    adReady = true
                    break
                }
                delay(200)
            }
            
            isAdLoading.value = false
            
            if (adReady) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 1000) {
                     delay(1000 - elapsed)
                }
                onAdReady()
            } else {
                onTimeout()
            }
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        
        // Initialize Facebook Audience Network (if used via mediation)
        try {
            if (!com.facebook.ads.AudienceNetworkAds.isInitialized(context)) {
                com.facebook.ads.AudienceNetworkAds.initialize(context)
                Log.d(TAG, "Facebook Audience Network Initializing...")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Facebook Ads init skipped: ${e.message}")
        }
        
        // Initialize AdMob on main thread via coroutine
        try {
            val appContext = context.applicationContext
            adScope.launch {
                try {
                    MobileAds.initialize(appContext) { status ->
                        Log.d(TAG, "AdMob Initialized: $status")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AdMob initialization failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob init launch failed: ${e.message}")
        }

        isInitialized = true
        
        try {
            setupRemoteConfig()
        } catch (e: Exception) {
            Log.e(TAG, "RemoteConfig setup failed: ${e.message}")
        }
    }
    
    private fun setupRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0 
        }
        
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        val defaults = mapOf(
            KEY_SHOW_APP_OPEN to true,
            KEY_SHOW_INTERSTITIAL to true,
            KEY_SHOW_BANNER to true,
            KEY_SHOW_ADAPTIVE_BANNER to true,
            KEY_SHOW_REWARDED to true,
            KEY_SHOW_REWARDED_INTERSTITIAL to true,
            KEY_SHOW_NATIVE to true,
            KEY_SHOW_NATIVE_VIDEO to true,

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

            KEY_WEBVIEW_AD_INTERVALS to "60,180,600,900,1200",
            KEY_WEBVIEW_AD_REPEAT_INTERVAL to 600,

            KEY_REWARDED_LIMIT_1MIN to 2,
            KEY_REWARDED_LIMIT_5MIN to 5,
            KEY_INTERSTITIAL_LIMIT_1MIN to 2,
            KEY_INTERSTITIAL_LIMIT_5MIN to 5,

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
        
        Log.d(TAG, "Fetching Remote Config...")
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Remote Config Fetched & Activated!")
                } else {
                    Log.w(TAG, "⚠️ Remote Config Fetch Failed (using defaults)")
                }
            }
    }

    internal fun isAdEnabled(key: String): Boolean {
        return Firebase.remoteConfig.getBoolean(key)
    }

    private fun getAdUnitId(prodId: String, testId: String): String {
        // Use test ads in debug builds, production in release
        return if (com.clicktoearn.linkbox.BuildConfig.DEBUG) testId else prodId
    }

    sealed class AdCheckResult {
        object Success : AdCheckResult()
        data class LimitReached(val message: String) : AdCheckResult()
        object Disabled : AdCheckResult()
    }

    private const val PREF_SHARED_OPEN_COUNT = "pref_shared_open_count"
    
    fun shouldShowSharedContentAd(context: Context): Boolean {
        val frequency = Firebase.remoteConfig.getLong(KEY_SHARED_CONTENT_AD_OPEN_EVERY_TIME).toInt()
        if (frequency <= 0) return false
        if (frequency == 1) return true
        
        val prefs = context.getSharedPreferences("ads_prefs", Context.MODE_PRIVATE)
        val count = prefs.getLong(PREF_SHARED_OPEN_COUNT, 0L)
        val nextCount = count + 1
        prefs.edit().putLong(PREF_SHARED_OPEN_COUNT, nextCount).apply()
        
        return (nextCount - 1) % frequency == 0L
    }

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

        val nextFixed = intervals.firstOrNull { it > elapsedSeconds }
        if (nextFixed != null) return nextFixed

        val maxInterval = intervals.last()
        val repeat = Firebase.remoteConfig.getLong(KEY_WEBVIEW_AD_REPEAT_INTERVAL)
        if (repeat <= 0) return null

        val diff = elapsedSeconds - maxInterval
        if (diff < 0) return maxInterval + repeat
        
        val multiplier = (diff / repeat) + 1
        return maxInterval + (multiplier * repeat)
    }

    private fun canShowAd(type: String, placementKey: String? = null): AdCheckResult {
        val now = System.currentTimeMillis()
        val fiveMinAgo = now - 5 * 60 * 1000

        val (showTimes, limit1MinKey, limit5MinKey) = when (type) {
            "rewarded" -> Triple(rewardedShowTimes, KEY_REWARDED_LIMIT_1MIN, KEY_REWARDED_LIMIT_5MIN)
            "interstitial" -> Triple(interstitialShowTimes, KEY_INTERSTITIAL_LIMIT_1MIN, KEY_INTERSTITIAL_LIMIT_5MIN)
            else -> return AdCheckResult.Success
        }

        showTimes.removeAll { it < fiveMinAgo }

        val limit1Min = Firebase.remoteConfig.getLong(limit1MinKey).toInt()
        val limit5Min = Firebase.remoteConfig.getLong(limit5MinKey).toInt()

        if (limit1Min > 0 && showTimes.count { it >= now - 60000 } >= limit1Min) {
            return AdCheckResult.LimitReached("Ad Limit reached. Try in 60s")
        }
        if (limit5Min > 0 && showTimes.size >= limit5Min) {
            return AdCheckResult.LimitReached("Standard Limit reached.")
        }

        if (placementKey != null && placementKey != "splash_start") {
            val lastShow = placementLastShowTime[placementKey] ?: 0L
            val cooldownSeconds = 60L
            if (now - lastShow < cooldownSeconds * 1000) {
                return AdCheckResult.LimitReached("Ad cooldown...")
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
        val adUnitId = getAdUnitId(ADMOB_APP_OPEN_ID, TEST_APP_OPEN)
        Log.d(TAG, "Loading [AppOpenAd] with UnitID: $adUnitId")
        
        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAppOpen = false
                    Log.d(TAG, "✅ [AppOpenAd] loaded successfully!")
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAppOpen = false
                    Log.e(TAG, "❌ [AppOpenAd] failed: ${loadAdError.message}")
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
        if (!isAdEnabled(KEY_SHOW_APP_OPEN)) return
        showInterstitialAd(activity, "splash_start") {
            Log.d(TAG, "Splash Interstitial dismissed.")
        }
    }

    private var pendingInterstitialCallback: (() -> Unit)? = null

    // Interstitial Ad
    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial || !isAdEnabled(KEY_SHOW_INTERSTITIAL)) return

        isLoadingInterstitial = true
        val adUnitId = getAdUnitId(ADMOB_INTERSTITIAL_ID, TEST_INTERSTITIAL)
        Log.d(TAG, "Loading [InterstitialAd] with UnitID: $adUnitId")
        
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    Log.d(TAG, "✅ [InterstitialAd] loaded successfully!")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingInterstitial = false
                    interstitialAd = null
                    Log.e(TAG, "❌ [InterstitialAd] failed: ${error.message}")
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
                onAdDismissed()
                loadInterstitialAd(activity)
            }
        )
    }

    // Rewarded Ad
    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isLoadingRewarded || !isAdEnabled(KEY_SHOW_REWARDED)) return
        
        isLoadingRewarded = true
        val adUnitId = getAdUnitId(ADMOB_REWARDED_ID, TEST_REWARDED)
        Log.d(TAG, "Loading [RewardedAd] with UnitID: $adUnitId")
        
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    Log.d(TAG, "✅ [RewardedAd] loaded successfully!")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingRewarded = false
                    rewardedAd = null
                    Log.e(TAG, "❌ [RewardedAd] failed: ${error.message}")
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, strict: Boolean = false, onRewardEarned: () -> Unit, onAdClosed: ((String?) -> Unit)? = null) {
        if (!isAdEnabled(KEY_SHOW_REWARDED)) {
            if (strict) onAdClosed?.invoke("Ad services disabled") else onRewardEarned()
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
                        if (!rewardEarned) onAdClosed?.invoke("Reward cancelled")
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        rewardedAd = null
                        loadRewardedAd(activity)
                        onAdClosed?.invoke("Failed to display ad.")
                    }
                }
                rewardedAd?.show(activity) { _ ->
                    rewardEarned = true
                    onRewardEarned()
                }
            },
            onTimeout = {
                if (strict) onAdClosed?.invoke("Ad not ready.") else onRewardEarned()
                loadRewardedAd(activity)
            }
        )
    }

    // Rewarded Interstitial Ad
    fun loadRewardedInterstitialAd(context: Context) {
        if (isLoadingRewardedInterstitial || rewardedInterstitialAd != null || !isAdEnabled(KEY_SHOW_REWARDED_INTERSTITIAL)) return
        
        isLoadingRewardedInterstitial = true
        val adUnitId = getAdUnitId(ADMOB_REWARDED_INTERSTITIAL_ID, TEST_REWARDED_INTERSTITIAL)
        Log.d(TAG, "Loading [RewardedInterstitialAd] with UnitID: $adUnitId")
        
        RewardedInterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isLoadingRewardedInterstitial = false
                    Log.d(TAG, "✅ [RewardedInterstitialAd] loaded successfully!")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingRewardedInterstitial = false
                    rewardedInterstitialAd = null
                    Log.e(TAG, "❌ [RewardedInterstitialAd] failed: ${error.message}")
                }
            }
        )
    }

    fun showRewardedInterstitialAd(activity: Activity, placementKey: String? = null, strict: Boolean = false, onRewardEarned: () -> Unit, onAdClosed: ((String?) -> Unit)? = null) {
        if (!isAdEnabled(KEY_SHOW_REWARDED_INTERSTITIAL)) {
            if (strict) onAdClosed?.invoke("Ad disabled") else onRewardEarned()
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
                        } else onAdClosed?.invoke("Reward cancelled")
                        rewardedInterstitialAd = null
                        loadRewardedInterstitialAd(activity)
                    }
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        rewardedInterstitialAd = null
                        loadRewardedInterstitialAd(activity)
                        onAdClosed?.invoke("Failed to display ad.")
                    }
                }
                rewardedInterstitialAd?.show(activity) { _ ->
                    rewardEarned = true
                }
            },
            onTimeout = {
                if (strict) onAdClosed?.invoke("Ad not ready.") else onRewardEarned()
                loadRewardedInterstitialAd(activity)
            }
        )
    }

    fun preloadBannerAds() {}
    
    private fun getAdSize(context: Context, widthDp: Int): AdSize {
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }

    @Composable
    fun BannerAdView(modifier: Modifier = Modifier) {
        if (!isAdEnabled(KEY_SHOW_BANNER)) return
        val adUnitId = getAdUnitId(ADMOB_BANNER_ID, TEST_BANNER)
        val context = LocalContext.current
        
        BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val screenWidth = context.resources.displayMetrics.widthPixels
            val density = context.resources.displayMetrics.density
            val defaultWidth = (screenWidth / density).toInt()
            val availableWidth = if (maxWidth.value.isFinite() && maxWidth.value > 0) maxWidth.value.toInt() else defaultWidth
            if (availableWidth <= 0) return@BoxWithConstraints

            val adView = remember(adUnitId, availableWidth) { 
                AdView(context).apply {
                    setAdSize(getAdSize(context, availableWidth))
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() { Log.d(TAG, "✅ [BannerAd] loaded!") }
                        override fun onAdFailedToLoad(error: LoadAdError) { Log.e(TAG, "❌ [BannerAd] failed: ${error.message}") }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            }

            DisposableEffect(adView) { onDispose { adView.destroy() } }
            AndroidView(modifier = Modifier.wrapContentSize(), factory = { adView })
        }
    }

    @Composable
    fun AdaptiveBannerAdView() { BannerAdView() }

    @Composable
    fun MediumRectangleAdView() {
        if (!isAdEnabled(KEY_SHOW_BANNER)) return
        val adUnitId = getAdUnitId(ADMOB_BANNER_ID, TEST_BANNER)
        val context = LocalContext.current

        val adView = remember(adUnitId) {
            AdView(context).apply {
                setAdSize(AdSize.MEDIUM_RECTANGLE)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() { Log.d(TAG, "✅ [MediumRectangleAd] loaded!") }
                    override fun onAdFailedToLoad(error: LoadAdError) { Log.e(TAG, "❌ [MediumRectangleAd] failed: ${error.message}") }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AndroidView(modifier = Modifier.width(300.dp).height(250.dp), factory = { adView })
        }
    }

    fun loadNativeAd(context: Context, forceRefresh: Boolean = false) {
        if (isLoadingNative || (!forceRefresh && _nativeAd.value != null) || !isAdEnabled(KEY_SHOW_NATIVE_VIDEO)) return
        isLoadingNative = true
        val adUnitId = getAdUnitId(ADMOB_NATIVE_ID, TEST_NATIVE)
        Log.d(TAG, "Loading [NativeAd] with UnitID: $adUnitId")
        
        val adLoader = AdLoader.Builder(context, adUnitId).forNativeAd { ad ->
            _nativeAd.value?.destroy()
            _nativeAd.value = ad
            isLoadingNative = false
            Log.d(TAG, "✅ [NativeAd] loaded!")
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                isLoadingNative = false
                Log.e(TAG, "❌ [NativeAd] failed: ${error.message}")
            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun loadStandardNativeAd(context: Context, forceRefresh: Boolean = false) {
        if (isLoadingStandardNative || (!forceRefresh && _standardNativeAd.value != null) || !isAdEnabled(KEY_SHOW_NATIVE)) return
        isLoadingStandardNative = true
        val adUnitId = getAdUnitId(ADMOB_NATIVE_ID, TEST_NATIVE)
        Log.d(TAG, "Loading [StandardNativeAd] with UnitID: $adUnitId")
        
        val adLoader = AdLoader.Builder(context, adUnitId).forNativeAd { ad ->
            _standardNativeAd.value?.destroy()
            _standardNativeAd.value = ad
            isLoadingStandardNative = false
            Log.d(TAG, "✅ [StandardNativeAd] loaded!")
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                isLoadingStandardNative = false
                Log.e(TAG, "❌ [StandardNativeAd] failed: ${error.message}")
            }
        }).build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    @Composable fun NativeVideoAdView(modifier: Modifier = Modifier) { NativeAdView(modifier) }

    @Composable
    fun NativeAdView(modifier: Modifier = Modifier) {
        if (!isAdEnabled(KEY_SHOW_NATIVE)) return
        val nativeAd by nativeAdFlow.collectAsState()
        val context = LocalContext.current
        LaunchedEffect(Unit) { if (nativeAd == null) loadNativeAd(context) }
        if (nativeAd != null) {
             AndroidView(modifier = modifier.fillMaxWidth(), factory = { ctx ->
                 val inflater = LayoutInflater.from(ctx)
                 val adView = inflater.inflate(R.layout.native_ad_layout_admob, null) as NativeAdView
                 populateNativeAdView(nativeAd!!, adView)
                 adView
             }, update = { view -> populateNativeAdView(nativeAd!!, view) })
        } else { NativeAdPlaceholder(modifier) }
    }
    
    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.mediaView = adView.findViewById(R.id.ad_media)
        
        (adView.headlineView as? TextView)?.text = nativeAd.headline
        (adView.bodyView as? TextView)?.text = nativeAd.body
        (adView.callToActionView as? Button)?.text = nativeAd.callToAction
        (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        
        val mediaContent = nativeAd.mediaContent
        if (mediaContent != null && mediaContent.hasVideoContent()) {
            adView.mediaView?.visibility = View.VISIBLE
            adView.mediaView?.mediaContent = mediaContent
        } else if (mediaContent != null) {
            adView.mediaView?.visibility = View.VISIBLE
            adView.mediaView?.mediaContent = mediaContent
        } else {
            adView.mediaView?.visibility = View.GONE
        }
        
        adView.setNativeAd(nativeAd)
    }

    fun openAdInspector(context: Context) { MobileAds.openAdInspector(context) { } }
    @Composable fun NativeAdPlaceholder(modifier: Modifier = Modifier) { NativeAdShimmer(modifier) }
    @Composable fun RectangleAdPlaceholder(modifier: Modifier = Modifier) { ShimmerBox(modifier = modifier.fillMaxWidth().height(250.dp)) }
}
