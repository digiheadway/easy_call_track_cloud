package com.calltracker.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioPlayer = AudioPlayer(context = this)
        
        // Start background upload worker
        UploadWorker.enqueue(this)
        
        enableEdgeToEdge()
        setContent {
            CallCloudTheme {
                MainScreen(audioPlayer = audioPlayer)
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
