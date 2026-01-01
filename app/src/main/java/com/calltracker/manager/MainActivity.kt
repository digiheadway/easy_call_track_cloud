package com.calltracker.manager

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
import com.calltracker.manager.ui.home.CallsScreen
import com.calltracker.manager.ui.home.PersonsScreen
import com.calltracker.manager.ui.home.ReportsScreen
import com.calltracker.manager.ui.settings.SettingsScreen
import com.calltracker.manager.ui.theme.CallCloudTheme

import com.calltracker.manager.ui.utils.AudioPlayer
import com.calltracker.manager.worker.UploadWorker
import com.calltracker.manager.ui.onboarding.OnboardingScreen
import com.calltracker.manager.data.SettingsRepository

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
        super.onCreate(savedInstanceState)
        viewModel = androidx.lifecycle.ViewModelProvider(this)[MainViewModel::class.java]
        audioPlayer = AudioPlayer(context = this)
        
        // Start background upload worker
        UploadWorker.enqueue(this)
        
        enableEdgeToEdge()
        setContent {
            val settingsRepository = remember { SettingsRepository(this) }
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
        // Request notification permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}

@Composable
fun MainScreen(audioPlayer: AudioPlayer) {
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
        }
    }
}
