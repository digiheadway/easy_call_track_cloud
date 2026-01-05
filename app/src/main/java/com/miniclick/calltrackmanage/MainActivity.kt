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
import com.miniclick.calltrackmanage.ui.home.DialerScreen
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
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.CallDataRepository
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Navigation tabs
enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    DIALER("Dialer", Icons.Default.Dialpad, Icons.Filled.Dialpad),
    CALLS("Calls", Icons.Default.Call, Icons.Filled.Call),
    PERSONS("Persons", Icons.Default.People, Icons.Filled.People),
    REPORTS("Reports", Icons.Default.Assessment, Icons.Filled.Assessment),
    SETTINGS("More", Icons.Default.Settings, Icons.Filled.Settings)
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
                val settingsRepository = SettingsRepository.getInstance(getApplicationContext())
                var showOnboarding by remember { mutableStateOf(!settingsRepository.isOnboardingCompleted()) }
                
                if (showOnboarding) {
                    OnboardingScreen(onComplete = {
                        settingsRepository.setOnboardingCompleted(true)
                        showOnboarding = false
                    })
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
        if (intent == null) return

        // Handle phone lookup from notifications
        intent.getStringExtra("phone_lookup")?.let { phoneNumber ->
            viewModel.setLookupPhoneNumber(phoneNumber)
        }

        // Handle shared recording files (Google Dialer support)
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                processSharedRecording(uri)
            }
        }
    }

    private fun processSharedRecording(uri: Uri) {
        lifecycleScope.launch {
            val context = applicationContext
            val fileName = getFileName(uri)
            val recordingRepo = RecordingRepository.getInstance(context)
            val callRepo = CallDataRepository.getInstance(context)
            
            val importedFile = withContext(Dispatchers.IO) {
                recordingRepo.importSharedRecording(uri, fileName)
            }

            if (importedFile != null) {
                // Tiered search through recent calls to find a match
                val searchTiers = listOf(20, 50, 100, 300, 700, 1000, 3000, 5000)
                var matchedCall: com.miniclick.calltrackmanage.data.db.CallDataEntity? = null
                
                val allRecentCalls = withContext(Dispatchers.IO) {
                    callRepo.getAllCalls().take(5000) 
                }
                
                var checkedCount = 0
                for (tier in searchTiers) {
                    if (checkedCount >= allRecentCalls.size) break
                    
                    val limit = minOf(tier, allRecentCalls.size)
                    for (i in checkedCount until limit) {
                        val call = allRecentCalls[i]
                        val isMatch = recordingRepo.findRecordingInList(
                            arrayOf(importedFile),
                            call.callDate,
                            call.duration,
                            call.phoneNumber,
                            call.contactName
                        ) != null
                        
                        if (isMatch) {
                            matchedCall = call
                            break
                        }
                    }
                    if (matchedCall != null) break
                    checkedCount = limit
                }

                if (matchedCall != null) {
                    // Update DB immediately
                    withContext(Dispatchers.IO) {
                        callRepo.updateRecordingPath(matchedCall.compositeId, importedFile.absolutePath)
                    }
                    
                    val personName = matchedCall.contactName ?: matchedCall.phoneNumber
                    val callTypeStr = when (matchedCall.callType) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> "Incoming Call"
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> "Outgoing Call"
                        else -> "Call"
                    }
                    val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(matchedCall.callDate))
                    
                    Toast.makeText(context, "Attached to $personName's $callTypeStr on $timeStr", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "No Call Log found to attach this recording, Please Attach from Call history in App.", Toast.LENGTH_LONG).show()
                }
                
                // Trigger an immediate sync to upload the new recording
                RecordingUploadWorker.runNow(context)
            } else {
                Toast.makeText(context, "Failed to Attach Call Recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting filename", e)
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(audioPlayer: AudioPlayer, viewModel: MainViewModel = viewModel()) {
    val lookupPhoneNumber by viewModel.lookupPhoneNumber.collectAsState()
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()
    
    // Default to Calls tab since Dialer is now a modal
    var selectedTab by remember { mutableStateOf(AppTab.CALLS) }
    
    // Explicitly exclude DIALER from tabs
    val tabs = remember {
        AppTab.entries.filter { it != AppTab.DIALER }
    }
    
    // Dialer Sheet State
    var showDialerSheet by remember { mutableStateOf(false) }
    val dialerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    Scaffold(
        bottomBar = {
            Column {
                NavigationBar {
                    tabs.forEach { tab ->
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    AppTab.DIALER -> { /* No-op, should not happen */ }
                    AppTab.CALLS -> CallsScreen(
                        audioPlayer = audioPlayer, 
                        onOpenDialer = { showDialerSheet = true }
                    )
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
                
                // Dialer Bottom Sheet
                if (showDialerSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showDialerSheet = false },
                        sheetState = dialerSheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = { BottomSheetDefaults.DragHandle() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // DialerScreen content
                        Box(modifier = Modifier.fillMaxSize()) {
                             DialerScreen(
                                 onIdentifyCallHistory = { 
                                     showDialerSheet = false
                                 },
                                 onClose = { showDialerSheet = false }
                             )
                        }
                    }
                }
            }
        }
    }
}
