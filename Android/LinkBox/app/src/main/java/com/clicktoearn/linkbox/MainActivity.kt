package com.clicktoearn.linkbox

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import com.clicktoearn.linkbox.ui.screens.*
import com.clicktoearn.linkbox.ui.theme.LinkBoxTheme
import com.clicktoearn.linkbox.ui.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.ui.components.LinkBoxBottomSheet

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object My : Screen("my", "My", Icons.Default.Folder)
    object History : Screen("history", "History", Icons.Default.History)
    object Trending : Screen("trending", "Trending", Icons.Default.TrendingUp)
    object Earn : Screen("earn", "Earn", Icons.Default.Stars)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object PageEditor : Screen("page_editor/{entityId}/{readOnly}", "Page Editor", Icons.Default.Edit) {
        fun createRoute(entityId: Long, readOnly: Boolean = false) = "page_editor/$entityId/$readOnly"
    }
    object DeepLink : Screen("deep_link/{token}", "Deep Link", Icons.Default.Link) {
        fun createRoute(token: String) = "deep_link/$token"
    }
    object Browser : Screen("browser/{url}/{showUrlBar}", "Browser", Icons.Default.Language) {
        fun createRoute(url: String, showUrlBar: Boolean = true) = 
            "browser/${Uri.encode(url)}/$showUrlBar"
    }
    object LinkSharing : Screen("link_sharing", "My Links", Icons.Default.Share)
}

class MainActivity : ComponentActivity() {
    private var deepLinkToken by mutableStateOf<String?>(null)
    private var deepLinkCounter by mutableStateOf(0L)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle deep link from initial intent
        handleDeepLink(intent)
        
        setContent {
            MainApp(initialDeepLinkToken = deepLinkToken, deepLinkTrigger = deepLinkCounter)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }
    
    /**
     * Extracts the token from deep link URLs.
     * Supports:
     * - https://api.pokipro.com/linkbox/?token=XXXXXXXX
     * - linkbox://open?token=XXXXXXXX
     */
    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            uri?.let {
                val token = it.getQueryParameter("token")
                if (!token.isNullOrBlank()) {
                    deepLinkToken = token
                    // Increment counter to force LaunchedEffect to re-trigger even for same token
                    deepLinkCounter++
                }
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: LinkBoxViewModel = viewModel(),
    initialDeepLinkToken: String? = null,
    deepLinkTrigger: Long = 0L
) {
    val navController = rememberNavController()
    val screens = listOf(Screen.My, Screen.History, Screen.Trending, Screen.Earn, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = screens.any { it.route == currentDestination?.route }

    val isInitialized by viewModel.isInitialized.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    // Exit confirmation dialog state
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Check if we're on a root screen (no back stack to pop)
    val canGoBack = navController.previousBackStackEntry != null
    
    // BackHandler to intercept back press when about to exit
    BackHandler(enabled = !canGoBack && isInitialized) {
        showExitDialog = true
    }
    
    // Exit Confirmation Dialog
    if (showExitDialog) {
        ExitConfirmationDialog(
            onDismiss = { showExitDialog = false },
            onConfirmExit = {
                showExitDialog = false
                (context as? Activity)?.finish()
            }
        )
    }
    
    // Handle deep link navigation when initialized
    // Using deepLinkTrigger to force re-navigation even when app is already open
    LaunchedEffect(initialDeepLinkToken, deepLinkTrigger, isInitialized) {
        if (isInitialized && !initialDeepLinkToken.isNullOrBlank()) {
            // Pop any existing deep link screens before navigating
            navController.popBackStack(Screen.DeepLink.route, inclusive = true)
            navController.navigate(Screen.DeepLink.createRoute(initialDeepLinkToken)) {
                launchSingleTop = true
            }
        }
    }

    LinkBoxTheme(darkTheme = isDarkMode) {
        Scaffold(
            bottomBar = {
                if (isInitialized) {
                    AnimatedVisibility(
                        visible = showBottomBar,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        NavigationBar {
                            screens.forEach { screen ->
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = null) },
                                    label = { Text(screen.label) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
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
            }
        ) { innerPadding ->
            Crossfade(
                targetState = isInitialized,
                animationSpec = tween(500),
                label = "AppInitialization"
            ) { initialized ->
                if (!initialized) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    val mainTabs = listOf(
                        Screen.My.route, 
                        Screen.History.route, 
                        Screen.Trending.route, 
                        Screen.Earn.route, 
                        Screen.Settings.route
                    )

                    NavHost(
                        navController = navController,
                        startDestination = Screen.My.route,
                        modifier = Modifier.padding(
                            bottom = innerPadding.calculateBottomPadding()
                        ),
                        enterTransition = {
                            fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + 
                            scaleIn(initialScale = 0.95f, animationSpec = tween(400, easing = LinearOutSlowInEasing))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + 
                            scaleIn(initialScale = 0.95f, animationSpec = tween(400, easing = LinearOutSlowInEasing))
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing))
                        }
                    ) {
                        composable(Screen.My.route) {
                            MyScreen(
                                viewModel = viewModel,
                                onNavigateToPageEditor = { entityId, readOnly ->
                                    navController.navigate(Screen.PageEditor.createRoute(entityId, readOnly))
                                },
                                onNavigateToBrowser = { url, showUrlBar ->
                                    navController.navigate(Screen.Browser.createRoute(url, showUrlBar))
                                },
                                isInternalTab = false,
                                onNavigateToLinkSharing = { navController.navigate(Screen.LinkSharing.route) }
                            )
                        }
                        composable(Screen.History.route) {
                            JoinedScreen(
                                viewModel = viewModel,
                                onNavigateToBrowser = { url, showUrlBar ->
                                    navController.navigate(Screen.Browser.createRoute(url, showUrlBar))
                                },
                                onNavigateToPageEditor = { entityId, readOnly ->
                                    navController.navigate(Screen.PageEditor.createRoute(entityId, readOnly))
                                },
                                isInternalTab = false
                            )
                        }
                        composable(Screen.Trending.route) { 
                            TrendingScreen(
                                viewModel = viewModel,
                                onNavigateToBrowser = { url, showUrlBar ->
                                    navController.navigate(Screen.Browser.createRoute(url, showUrlBar))
                                }
                            ) 
                        }
                        composable(Screen.Earn.route) { EarnScreen(viewModel) }
                        composable(Screen.Settings.route) { SettingsScreen(viewModel) }
                        composable(
                            route = Screen.PageEditor.route,
                            arguments = listOf(
                                navArgument("entityId") { type = NavType.LongType },
                                navArgument("readOnly") { type = NavType.BoolType }
                            )
                        ) { backStackEntry ->
                            val entityId = backStackEntry.arguments?.getLong("entityId") ?: -1L
                            val readOnly = backStackEntry.arguments?.getBoolean("readOnly") ?: false
                            PageEditorScreen(viewModel, entityId, readOnly, onBack = { navController.popBackStack() })
                        }
                        
                        // Deep Link Screen - opens shared content by token
                        composable(
                            route = Screen.DeepLink.route,
                            arguments = listOf(
                                navArgument("token") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val token = backStackEntry.arguments?.getString("token") ?: ""
                            DeepLinkScreen(
                                viewModel = viewModel,
                                token = token,
                                onNavigateToPageEditor = { entityId, readOnly ->
                                    navController.navigate(Screen.PageEditor.createRoute(entityId, readOnly)) {
                                        popUpTo(Screen.DeepLink.route) { inclusive = true }
                                    }
                                },
                                onNavigateToBrowser = { url, showUrlBar ->
                                    navController.navigate(Screen.Browser.createRoute(url, showUrlBar))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.Browser.route,
                            arguments = listOf(
                                navArgument("url") { type = NavType.StringType },
                                navArgument("showUrlBar") { type = NavType.BoolType }
                            )
                        ) { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url")?.let { Uri.decode(it) } ?: ""
                            val showUrlBar = backStackEntry.arguments?.getBoolean("showUrlBar") ?: true
                            BrowserScreen(
                                url = url,
                                showUrlBar = showUrlBar,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        
                        // Link Sharing Screen - manage all sharing links
                        composable(Screen.LinkSharing.route) {
                            LinkSharingScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Exit Confirmation Dialog
 * Shows when user presses back button to exit the app
 * Positioned at the bottom of the screen
 * "No" is the focused/primary button, "Yes" is smaller/secondary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit
) {
    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = "Exit LinkBox?"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Exit Icon / Illustration Area
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Are you sure you want to leave?\nWe'd love to have you stay!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Secondary Button (Exit)
                OutlinedButton(
                    onClick = onConfirmExit,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        "Exit App", 
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Primary Button (Stay)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1.5f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "No, Stay",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
