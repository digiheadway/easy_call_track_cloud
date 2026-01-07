package com.clicktoearn.linkbox

import com.clicktoearn.linkbox.utils.findActivity
import androidx.compose.ui.platform.LocalContext
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.microsoft.clarity.models.LogLevel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.clicktoearn.linkbox.ui.Screen
import com.clicktoearn.linkbox.ui.bottomNavItems
import com.clicktoearn.linkbox.ui.components.NoInternetScreen
import com.clicktoearn.linkbox.ui.components.ExitConfirmationSheet
import com.clicktoearn.linkbox.ui.screens.*
import com.clicktoearn.linkbox.ui.theme.LinkBoxTheme
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModelFactory
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.clicktoearn.linkbox.ads.AdsManager

class MainActivity : ComponentActivity() {
    private val viewModel: LinkBoxViewModel by viewModels {
        val app = application as LinkBoxApp
        LinkBoxViewModelFactory(app.repository, app.localRepository, app)
    }
    
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Standard Splash Screen API initialization
        val splashScreen = installSplashScreen()
        
        // Keep splash screen on until the first frame is ready
        // This prevents the "blank blue screen" during Compose initialization
        splashScreen.setKeepOnScreenCondition { !isReady }
        
        super.onCreate(savedInstanceState)
        
        // Handle deep link from intent - highest priority
        handleDeepLink(intent)
        
        // Initialize Analytics immediately (lightweight)
        com.clicktoearn.linkbox.analytics.AnalyticsManager.init(this)
        com.clicktoearn.linkbox.analytics.AnalyticsManager.logAppOpen()
        
        // Initialize Clarity in background
        val clarityConfig = ClarityConfig(
           projectId = "uxpn8i1g92",
           logLevel = LogLevel.None
        )
        Clarity.initialize(applicationContext, clarityConfig)
        
        // Initialize Billing (non-blocking)
        viewModel.initializeBilling(this)
        
        // Initialize AdsManager core (non-blocking, moves MobileAds.init to background)
        AdsManager.init(this)
        
        // Set up Compose UI IMMEDIATELY - this is critical for instant rendering
        setupComposeContent(savedInstanceState)
        
        // Handle Ad Consent (UMP) and ad loading asynchronously - doesn't block UI
        initializeAdConsent(savedInstanceState)
    }
    
    private fun initializeAdConsent(savedInstanceState: Bundle?) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        
        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this@MainActivity
                ) { loadAndShowError ->
                    if (loadAndShowError != null) {
                        android.util.Log.w("MainActivity", "${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                    }
                    if (consentInformation.canRequestAds()) {
                        loadAds(savedInstanceState)
                    }
                }
            },
            { requestConsentError ->
                android.util.Log.w("MainActivity", "${requestConsentError.errorCode}: ${requestConsentError.message}")
                loadAds(savedInstanceState)
            }
        )
    }

    private fun loadAds(savedInstanceState: Bundle?) {
        window.decorView.post {
            val app = application as LinkBoxApp
            if (app.isPremiumCached()) {
                return@post
            }

            // Preload banner ads first for instant display
            AdsManager.preloadBannerAds()
            
            // Then load full-screen ads
            AdsManager.loadInterstitialAd(this)
            AdsManager.loadRewardedAd(this)
            AdsManager.loadRewardedInterstitialAd(this)
            
            // Preload Native Ads
            AdsManager.loadNativeAd(this)
            AdsManager.loadStandardNativeAd(this)
            
            // Show app open ad after a short delay, but ONLY if not a deep link launch
            if (pendingDeepLinkToken == null && savedInstanceState == null) {
                window.decorView.postDelayed({
                    AdsManager.loadAndShowAppOpenAd(this)
                }, 500)
            }
        }
    }
    
    private fun setupComposeContent(savedInstanceState: Bundle?) {
        setContent {
            // Mark as ready once Compose starts
            SideEffect { isReady = true }
            
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            LinkBoxTheme(darkTheme = isDarkMode) {
                // Background Box to prevent window background leak
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)) {
                    
                    // Screenshot Protection Observer
                    val isScreenshotBlocked by viewModel.isScreenshotBlocked.collectAsState()
                    val context = LocalContext.current
                    LaunchedEffect(isScreenshotBlocked) {
                        val activity = context.findActivity()
                        if (activity != null) {
                            if (isScreenshotBlocked) {
                                activity.window.setFlags(
                                    android.view.WindowManager.LayoutParams.FLAG_SECURE,
                                    android.view.WindowManager.LayoutParams.FLAG_SECURE
                                )
                            } else {
                                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                            }
                        }
                    }
                
                    val navController = rememberNavController()

                
                // Analytics: Track Screen Views
                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { entry ->
                        val route = entry.destination.route
                        // Filter out internal routes or make them readable if needed
                        if (route != null) {
                            com.clicktoearn.linkbox.analytics.AnalyticsManager.logScreenView(route)
                        }
                    }
                }

                var showExitSheet by remember { mutableStateOf(false) }
                
                // Handle back press
                BackHandler {
                    val currentRoute = navController.currentDestination?.route
                    if (bottomNavItems.any { it.route == currentRoute }) {
                        showExitSheet = true
                    } else {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0)
                            }
                        }
                    }
                }

                // No blocking loading screen - UI shows immediately
                // Home screen and deep link screens have their own shimmers
                
                // Determine start destination:
                // - If there's a pending deep link token, go to SharedContent
                // - Otherwise always go to Home (shimmers handle loading state)
                // - Login is only for specific write actions, not blocking startup
                val app = application as LinkBoxApp
                val deferredToken by app.deferredToken.collectAsState()
                
                // Always start at Home to ensure it's at the root of the backstack
                // UNLESS we have a pending deep link token, then we start there to avoid shimmer flash
                val initialDeferredToken = remember { app.getPendingDeferredToken() }
                val initialDeepLinkToken = remember { pendingDeepLinkToken }
                
                val startDestination = if (initialDeepLinkToken != null) {
                    Screen.SharedContent.createRoute(initialDeepLinkToken, false)
                } else if (initialDeferredToken != null) {
                    Screen.SharedContent.createRoute(initialDeferredToken, true)
                } else {
                    Screen.Home.route
                }

                // Handle deep links for NEW intents (updates)
                LaunchedEffect(pendingDeepLinkToken, deferredToken) {
                    val pendingDeferred = deferredToken
                    val pendingToken = pendingDeepLinkToken
                    
                    // Skip if this is the initial token already handled by startDestination
                    if (pendingToken != null && pendingToken == initialDeepLinkToken) {
                        return@LaunchedEffect
                    }
                    if (pendingDeferred != null && pendingDeferred == initialDeferredToken) {
                        return@LaunchedEffect
                    }
                    
                    // For subsequent updates, we need to ensure the graph is ready
                    // A simple retry mechanism or check usually suffices, but since we filtered out
                    // the startup case, the graph should be ready by the time a NEW intent arrives.
                    
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    
                    if (pendingDeferred != null) {
                        val route = Screen.SharedContent.createRoute(pendingDeferred, true)
                        if (currentRoute != route) {
                            android.util.Log.d("MainActivity", "LaunchedEffect: Navigating to deferred: $pendingDeferred")
                            app.clearPendingDeferredToken()
                            viewModel.prefetchSharedContent(pendingDeferred)
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    } else if (pendingToken != null) {
                        val route = Screen.SharedContent.createRoute(pendingToken, false)
                        if (currentRoute != route) {
                            android.util.Log.d("MainActivity", "LaunchedEffect: Navigating to regular: $pendingToken")
                            pendingDeepLinkToken = null
                            viewModel.prefetchSharedContent(pendingToken)
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                    }
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                
                LaunchedEffect(Unit) {
                    viewModel.userMessage.collect { message ->
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        snackbarHostState.showSnackbar(
                            message = message,
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        
                        // Only show bottom bar on main tabs
                        if (bottomNavItems.any { it.route == currentDestination?.route }) {
                            NavigationBar {
                                bottomNavItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                                        label = { Text(screen.title) },
                                        selected = currentDestination?.route == screen.route,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
                            val targetRoute = targetState.destination.route
                            val initialRoute = initialState.destination.route
                            val isTabSwitch = bottomNavItems.any { it.route == targetRoute } && 
                                            bottomNavItems.any { it.route == initialRoute }
                            
                            if (targetRoute == Screen.SharedContent.route) {
                                EnterTransition.None
                            } else if (isTabSwitch) {
                                EnterTransition.None
                            } else {
                                slideInHorizontally(
                                    initialOffsetX = { 300 },
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(250))
                            }
                        },
                        exitTransition = {
                            val targetRoute = targetState.destination.route
                            val initialRoute = initialState.destination.route
                            val isTabSwitch = bottomNavItems.any { it.route == targetRoute } && 
                                            bottomNavItems.any { it.route == initialRoute }
                                            
                            if (initialRoute == Screen.SharedContent.route) {
                                ExitTransition.None
                            } else if (isTabSwitch) {
                                ExitTransition.None
                            } else {
                                slideOutHorizontally(
                                    targetOffsetX = { -300 },
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(250))
                            }
                        },
                        popEnterTransition = {
                            val targetRoute = targetState.destination.route
                            val initialRoute = initialState.destination.route
                            val isTabSwitch = bottomNavItems.any { it.route == targetRoute } && 
                                            bottomNavItems.any { it.route == initialRoute }
                                            
                            if (targetRoute == Screen.SharedContent.route) {
                                EnterTransition.None
                            } else if (isTabSwitch) {
                                EnterTransition.None
                            } else {
                                slideInHorizontally(
                                    initialOffsetX = { -300 },
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(250))
                            }
                        },
                        popExitTransition = {
                            val targetRoute = targetState.destination.route
                            val initialRoute = initialState.destination.route
                            val isTabSwitch = bottomNavItems.any { it.route == targetRoute } && 
                                            bottomNavItems.any { it.route == initialRoute }
                            
                            if (initialRoute == Screen.SharedContent.route) {
                                ExitTransition.None
                            } else if (isTabSwitch) {
                                ExitTransition.None
                            } else {
                                slideOutHorizontally(
                                    targetOffsetX = { 300 },
                                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(250))
                            }
                        }
                    ) {
                        composable(Screen.Home.route) { HomeScreen(viewModel, navController) }
                        composable(Screen.MyAssets.route) { FilesScreen(viewModel, navController) }
                        composable(Screen.MyLinks.route) { LinksScreen(viewModel, onBack = { navController.popBackStack() }) }
                        composable(Screen.History.route) { HistoryScreen(viewModel, navController) }
                        composable(Screen.Profile.route) { ProfileScreen(viewModel, navController) }
                        composable(
                            route = Screen.SharedContent.route,
                            arguments = listOf(
                                navArgument("token") { type = NavType.StringType },
                                navArgument("isNewInstall") { 
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry ->
                            val token = backStackEntry.arguments?.getString("token") ?: ""
                            val isNewInstall = backStackEntry.arguments?.getBoolean("isNewInstall") ?: false
                            DeepLinkScreen(
                                token = token,
                                isNewInstall = isNewInstall,
                                viewModel = viewModel,
                                navController = navController,
                                onBack = { 
                                    if (!navController.popBackStack()) {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(0)
                                        }
                                    }
                                },
                                onOpenUrl = { url, title, allowScreenCapture, exposeUrl ->
                                    val encodedUrl = android.net.Uri.encode(url)
                                    val encodedTitle = android.net.Uri.encode(title)
                                    navController.navigate("webview/$encodedUrl/$encodedTitle/$allowScreenCapture/$exposeUrl")
                                },
                                onOpenFolder = { ownerId, folderId, title ->
                                    val safeTitle = android.net.Uri.encode(title)
                                    navController.navigate(Screen.SharedFolder.createRoute(ownerId, folderId, safeTitle))
                                },
                                onOpenFile = { ownerId, assetId, title ->
                                    val safeTitle = android.net.Uri.encode(title)
                                    navController.navigate(Screen.SharedFile.createRoute(ownerId, assetId, safeTitle))
                                }
                            )
                        }
                        
                        composable(
                            route = Screen.PageEditor.route,
                            arguments = listOf(
                                navArgument("assetId") { type = NavType.StringType },
                                navArgument("editMode") { type = NavType.BoolType }
                            )
                        ) { backStackEntry ->
                            val assetId = backStackEntry.arguments?.getString("assetId") ?: ""
                            val editMode = backStackEntry.arguments?.getBoolean("editMode") ?: false
                            PageEditorScreen(
                                assetId = assetId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                initialEditMode = editMode
                            )
                        }
                        
                        composable(
                            route = "webview/{url}/{title}/{allowScreenCapture}/{exposeUrl}",
                            arguments = listOf(
                                navArgument("url") { type = NavType.StringType },
                                navArgument("title") { type = NavType.StringType },
                                navArgument("allowScreenCapture") { type = NavType.BoolType },
                                navArgument("exposeUrl") { type = NavType.BoolType }
                            )
                        ) { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url") ?: ""
                            val title = backStackEntry.arguments?.getString("title") ?: ""
                            val allowScreenCapture = backStackEntry.arguments?.getBoolean("allowScreenCapture") ?: true
                            val exposeUrl = backStackEntry.arguments?.getBoolean("exposeUrl") ?: true
                            WebViewScreen(url, title, viewModel, onBack = { navController.popBackStack() }, allowScreenCapture, exposeUrl)
                        }

                        composable(
                            route = Screen.SharedFolder.route,
                            arguments = listOf(
                                navArgument("ownerId") { type = NavType.StringType },
                                navArgument("folderId") { type = NavType.StringType },
                                navArgument("title") { type = NavType.StringType },
                                navArgument("restrictScreenshot") { type = NavType.BoolType; defaultValue = false },
                                navArgument("restrictUrl") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { backStackEntry ->
                            val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""
                            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
                            val title = backStackEntry.arguments?.getString("title") ?: ""
                            val restrictUrl = backStackEntry.arguments?.getBoolean("restrictUrl") ?: false
                            SharedFolderScreen(ownerId, folderId, title, viewModel, navController, onBack = { navController.popBackStack() }, restrictUrl = restrictUrl)
                        }

                        composable(
                            route = Screen.SharedFile.route,
                            arguments = listOf(
                                navArgument("ownerId") { type = NavType.StringType },
                                navArgument("assetId") { type = NavType.StringType },
                                navArgument("title") { type = NavType.StringType },
                                navArgument("restrictScreenshot") { type = NavType.BoolType; defaultValue = false }
                            )
                        ) { backStackEntry ->
                            val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""
                            val assetId = backStackEntry.arguments?.getString("assetId") ?: ""
                            val title = backStackEntry.arguments?.getString("title") ?: ""

                            SharedFileScreen(ownerId, assetId, title, viewModel, onBack = { navController.popBackStack() })
                    }
                }
            }
            
            // Global No Internet overlay - Outside Scaffold to cover Everything
            val isConnected by viewModel.isConnected.collectAsState()
            if (!isConnected) {
                NoInternetScreen(
                    onRetry = {
                        viewModel.checkConnectivity()
                    }
                )
            }
            
                val isAdLoading by AdsManager.isAdLoading.collectAsState()
                if (isAdLoading) {
                    com.clicktoearn.linkbox.ui.components.AdLoadingDialog(isVisible = true)
                }
                
                if (showExitSheet) {
                    ExitConfirmationSheet(
                        onDismiss = { showExitSheet = false },
                        onConfirmExit = { finish() }
                    )
                }
                }
            }
        }
    }
}
    
    private var pendingDeepLinkToken by mutableStateOf<String?>(null)


    
    /**
     * Handle deep link from intent
     * Extracts token from various URL formats:
     * - https://privacy.be6.in/{token}
     * - https://privacy.be6.in/open?token={token}
     */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        
        android.util.Log.d("MainActivity", "Deep link received: $data")
        
        // Extract token from different URL formats
        val token = when {
            // Query parameter: ?token=abc123
            data.getQueryParameter("token") != null -> {
                data.getQueryParameter("token")
            }
            // Path segment: privacy.be6.in/{token}
            data.pathSegments.isNotEmpty() -> {
                val lastSegment = data.pathSegments.last()
                // Ignore known file paths
                if (lastSegment in listOf("open.php", "open", "index.html", "linkbox")) {
                    null
                } else {
                    lastSegment
                }
            }
            else -> null
        }
        
        if (token != null) {
            android.util.Log.d("MainActivity", "Token extracted from deep link: $token")
            pendingDeepLinkToken = token
            
            // Start prefetching content immediately for faster loading
            viewModel.prefetchSharedContent(token)
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }
}
