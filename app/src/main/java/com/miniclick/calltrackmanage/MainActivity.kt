package com.miniclick.calltrackmanage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme // Added
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.miniclick.calltrackmanage.ui.home.CallsScreen
import com.miniclick.calltrackmanage.ui.home.PersonsScreen
import com.miniclick.calltrackmanage.ui.home.ReportsScreen
import com.miniclick.calltrackmanage.ui.settings.SettingsScreen
import com.miniclick.calltrackmanage.ui.theme.CallCloudTheme

import com.miniclick.calltrackmanage.ui.utils.AudioPlayer
import com.miniclick.calltrackmanage.ui.onboarding.OnboardingScreen
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.ui.common.PhoneLookupResultModal
import com.miniclick.calltrackmanage.ui.settings.SettingsViewModel

// Navigation tabs
enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    CALLS("Calls", Icons.Default.Call, Icons.Filled.Call),
    PERSONS("Persons", Icons.Default.People, Icons.Filled.People),
    REPORTS("Reports", Icons.Default.Assessment, Icons.Filled.Assessment),
    SETTINGS("Settings", Icons.Default.Settings, Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var audioPlayer: AudioPlayer
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        viewModel = androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
        audioPlayer = AudioPlayer(context = this)
        
        handleIntent(intent)
        
        enableEdgeToEdge()
        setContent {
            val settingsRepository = remember { SettingsRepository.getInstance(this) }
            var showOnboarding by remember { 
                mutableStateOf(!settingsRepository.isOnboardingCompleted()) 
            }
            
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when(themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            // Allow edge-to-edge content
            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as android.app.Activity).window
                    androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
                    
                    val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !darkTheme
                    controller.isAppearanceLightNavigationBars = !darkTheme
                }
            }

            CallCloudTheme(darkTheme = darkTheme) {
                if (showOnboarding) {
                    OnboardingScreen(
                        onComplete = {
                            settingsRepository.setOnboardingCompleted(true)
                            showOnboarding = false
                            // Trigger initial sync immediately after onboarding
                            CallSyncWorker.runNow(this@MainActivity)
                        }
                    )
                } else {
                    MainScreen(audioPlayer = audioPlayer)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioPlayer.isInitialized) {
            audioPlayer.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTheme()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        intent?.getStringExtra("phone_lookup")?.let { phoneNumber ->
            viewModel.setLookupPhoneNumber(phoneNumber)
        }
    }
}

@Composable
fun MainScreen(audioPlayer: AudioPlayer, viewModel: MainViewModel = viewModel()) {
    val lookupPhoneNumber by viewModel.lookupPhoneNumber.collectAsState()
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(AppTab.CALLS) }
    
    Scaffold(
        bottomBar = {
            Column {

                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == tab) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Only apply bottom padding from scaffold, let screens handle status bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedTab) {
                AppTab.CALLS -> CallsScreen(audioPlayer = audioPlayer)
                AppTab.PERSONS -> PersonsScreen(audioPlayer = audioPlayer)
                AppTab.REPORTS -> ReportsScreen()
                AppTab.SETTINGS -> SettingsScreen()
            }

            if (lookupPhoneNumber != null) {
                PhoneLookupResultModal(
                    phoneNumber = lookupPhoneNumber!!,
                    uiState = settingsState,
                    viewModel = settingsViewModel,
                    onDismiss = { viewModel.clearLookupPhoneNumber() }
                )
            }
        }
    }
}
