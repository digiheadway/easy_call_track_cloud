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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.miniclick.calltrackmanage.worker.RecordingUploadWorker
import com.miniclick.calltrackmanage.ui.common.*
import com.miniclick.calltrackmanage.ui.home.*
import com.miniclick.calltrackmanage.ui.settings.*
import com.miniclick.calltrackmanage.ui.theme.CallCloudTheme
import com.miniclick.calltrackmanage.ui.utils.AudioPlayer
import com.miniclick.calltrackmanage.ui.onboarding.OnboardingScreen
import com.miniclick.calltrackmanage.ui.onboarding.AgreementScreen
import com.miniclick.calltrackmanage.data.SettingsRepository
import com.miniclick.calltrackmanage.worker.CallSyncWorker
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.ui.settings.SettingsViewModel
import com.miniclick.calltrackmanage.data.RecordingRepository
import com.miniclick.calltrackmanage.data.CallDataRepository
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
                // val settingsRepository = SettingsRepository.getInstance(getApplicationContext()) // Using viewModel instead
                val isOnboardingCompleted by viewModel.onboardingCompleted.collectAsState()
                val isAgreementAccepted by viewModel.agreementAccepted.collectAsState()
                
                if (!isAgreementAccepted) {
                    AgreementScreen(onAccepted = {
                        viewModel.setAgreementAccepted(true)
                    })
                } else if (!isOnboardingCompleted) {
                    OnboardingScreen(onComplete = {
                        val settingsRepository = SettingsRepository.getInstance(getApplicationContext())
                        settingsRepository.setOnboardingCompleted(true)
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

        // Handle person details from notifications
        intent.getStringExtra("OPEN_PERSON_DETAILS")?.let { phoneNumber ->
            viewModel.setPersonDetailsPhone(phoneNumber)
        }

        // Handle shared recording files
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                processSharedRecording(uri)
            }
        }
        
        // Handle Dialer Intent (ACTION_DIAL or ACTION_VIEW with tel:)
        if (intent.action == Intent.ACTION_DIAL || intent.action == Intent.ACTION_VIEW) {
            if (intent.data?.scheme == "tel") {
                val number = intent.data?.schemeSpecificPart
                if (!number.isNullOrEmpty()) {
                    viewModel.setDialerNumber(number)
                }
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
                
                val matchedCall = withContext(Dispatchers.Default) {
                    val allRecentCalls = callRepo.getAllCalls().take(5000)
                    var matched: com.miniclick.calltrackmanage.data.db.CallDataEntity? = null
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
                                matched = call
                                break
                            }
                        }
                        if (matched != null) break
                        checkedCount = limit
                    }
                    matched
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
    val personDetailsPhone by viewModel.personDetailsPhone.collectAsState()
    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val homeViewModel: HomeViewModel = viewModel()
    val homeState by homeViewModel.uiState.collectAsState()
    
    // Default to Calls tab since Dialer is now a modal
    var selectedTab by remember { mutableStateOf(AppTab.CALLS) }
    
    // Explicitly exclude DIALER from tabs
    val tabs = remember {
        AppTab.entries.filter { it != AppTab.DIALER }
    }
    
    // Dialer State
    var showDialerSheet by remember { mutableStateOf(false) }
    val dialerInitialNumber by viewModel.dialerInitialNumber.collectAsState()
    
    // Auto-open dialer if initial number is set
    LaunchedEffect(dialerInitialNumber) {
        if (!dialerInitialNumber.isNullOrEmpty()) {
            showDialerSheet = true
        }
    }
    
    // Clear initial number when sheet closes
    LaunchedEffect(showDialerSheet) {
        if (!showDialerSheet) {
            viewModel.setDialerNumber(null)
        }
    }
    
    // Sync Queue Modal States - Using SettingsViewModel
    val showSyncQueue = settingsState.showSyncQueue
    val showRecordingQueue = settingsState.showRecordingQueue
    
    // Debug Logging
    val activeProcess by com.miniclick.calltrackmanage.data.ProcessMonitor.activeProcess.collectAsState()
    LaunchedEffect(settingsState.isSyncSetup, settingsState.pendingNewCallsCount, settingsState.pendingMetadataUpdatesCount, settingsState.pendingRecordingCount, activeProcess) {
        Log.d("MainScreen", "Sync Setup: ${settingsState.isSyncSetup}, Active: ${activeProcess?.title}, Pending: NewCalls=${settingsState.pendingNewCallsCount}, Meta=${settingsState.pendingMetadataUpdatesCount}, Person=${settingsState.pendingPersonUpdatesCount}, Rec=${settingsState.pendingRecordingCount}")
    }

    // Modals are handled inside the content Box to ensure proper layering

    // Full-screen Tracking Settings Page
    if (settingsState.showTrackingSettings) {
        TrackingSettingsScreen(
            uiState = settingsState,
            viewModel = settingsViewModel,
            onBack = { settingsViewModel.toggleTrackingSettings(false) }
        )
        return
    }

    // Full-screen Extras Page
    if (settingsState.showExtrasScreen) {
        ExtrasScreen(
            uiState = settingsState,
            viewModel = settingsViewModel,
            onResetOnboarding = { viewModel.resetOnboardingSession() },
            onBack = { settingsViewModel.toggleExtrasScreen(false) }
        )
        return
    }

    // Full-screen Data Management Page
    if (settingsState.showDataManagementScreen) {
        DataManagementScreen(
            uiState = settingsState,
            viewModel = settingsViewModel,
            onBack = { settingsViewModel.toggleDataManagementScreen(false) }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                val syncStatusBar = @Composable {
                    com.miniclick.calltrackmanage.ui.common.GlobalSyncStatusBar(
                        pendingNewCalls = settingsState.pendingNewCallsCount,
                        pendingMetadata = settingsState.pendingMetadataUpdatesCount,
                        pendingPersonUpdates = settingsState.pendingPersonUpdatesCount,
                        pendingRecordings = settingsState.pendingRecordingCount,
                        activeUploads = settingsState.activeRecordings.count { 
                            it.recordingSyncStatus == com.miniclick.calltrackmanage.data.db.RecordingSyncStatus.UPLOADING || 
                            it.recordingSyncStatus == com.miniclick.calltrackmanage.data.db.RecordingSyncStatus.COMPRESSING 
                        },
                        isNetworkAvailable = settingsState.isNetworkAvailable,
                        isSyncSetup = settingsState.isSyncSetup,
                        onSyncNow = { settingsViewModel.syncCallManually() },
                        onShowQueue = { settingsViewModel.toggleSyncQueue(true) },
                        audioPlayer = audioPlayer
                    )
                }

                when (selectedTab) {
                    AppTab.DIALER -> { /* No-op, should not happen */ }
                    AppTab.CALLS -> CallsScreen(
                        audioPlayer = audioPlayer, 
                        onOpenDialer = { showDialerSheet = true },
                        syncStatusBar = syncStatusBar,
                        personDetailsPhone = personDetailsPhone,
                        onClearPersonDetails = { viewModel.setPersonDetailsPhone(null) },
                        isDialerEnabled = settingsState.isDialerEnabled,
                        showDialButton = settingsState.showDialButton
                    )
                    AppTab.PERSONS -> PersonsScreen(
                        audioPlayer = audioPlayer,
                        syncStatusBar = syncStatusBar
                    )
                    AppTab.REPORTS -> ReportsScreen(
                        syncStatusBar = syncStatusBar,
                        onNavigateToTab = { tabIndex ->
                            selectedTab = when(tabIndex) {
                                0 -> AppTab.CALLS
                                1 -> AppTab.PERSONS
                                else -> AppTab.CALLS
                            }
                        }
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        syncStatusBar = syncStatusBar
                    )
                }

                if (settingsState.lookupPhoneNumber != null) {
                    PhoneLookupResultModal(
                        phoneNumber = settingsState.lookupPhoneNumber!!,
                        uiState = settingsState,
                        viewModel = settingsViewModel,
                        onDismiss = { settingsViewModel.showPhoneLookup(null) }
                    )
                }

                // Sync Queue Modals
                if (showSyncQueue) {
                    SyncQueueModal(
                        pendingNewCalls = settingsState.pendingNewCallsCount,
                        pendingRelatedData = settingsState.pendingMetadataUpdatesCount + settingsState.pendingPersonUpdatesCount,
                        pendingRecordings = settingsState.pendingRecordingCount,
                        isSyncSetup = settingsState.isSyncSetup,
                        isNetworkAvailable = settingsState.isNetworkAvailable,
                        onSyncAll = { settingsViewModel.syncCallManually() },
                        onDismiss = { settingsViewModel.toggleSyncQueue(false) },
                        onRecordingClick = {
                            settingsViewModel.toggleSyncQueue(false)
                            settingsViewModel.toggleRecordingQueue(true)
                        }
                    )
                }

                if (showRecordingQueue) {
                    RecordingQueueModal(
                        activeRecordings = settingsState.activeRecordings,
                        onDismiss = { settingsViewModel.toggleRecordingQueue(false) },
                        onRetry = { settingsViewModel.retryRecordingUpload(it) }
                    )
                }

                // --- Unified Settings Modals ---
                
                if (settingsState.showPermissionsModal) {
                    PermissionsModal(
                        permissions = settingsState.permissions,
                        onDismiss = { settingsViewModel.togglePermissionsModal(false) }
                    )
                }

                if (settingsState.showCloudSyncModal) {
                    CloudSyncModal(
                        uiState = settingsState,
                        viewModel = settingsViewModel,
                        onDismiss = { settingsViewModel.toggleCloudSyncModal(false) },
                        onOpenAccountInfo = { field -> 
                            settingsViewModel.toggleAccountInfoModal(true, field)
                        },
                        onCreateOrg = {
                            settingsViewModel.toggleCreateOrgModal(true)
                        },
                        onJoinOrg = {
                            settingsViewModel.toggleJoinOrgModal(true)
                        },
                        onKeepOffline = {
                            settingsViewModel.toggleCloudSyncModal(false)
                            viewModel.dismissOnboardingSession() 
                        }
                    )
                }

                if (settingsState.showAccountInfoModal) {
                    AccountInfoModal(
                        uiState = settingsState,
                        viewModel = settingsViewModel,
                        editField = settingsState.accountEditField,
                        onDismiss = { settingsViewModel.toggleAccountInfoModal(false) }
                    )
                }

                if (settingsState.showCreateOrgModal) {
                    CreateOrgModal(
                        onDismiss = { settingsViewModel.toggleCreateOrgModal(false) }
                    )
                }

                if (settingsState.showJoinOrgModal) {
                    JoinOrgModal(
                        viewModel = settingsViewModel,
                        onDismiss = { settingsViewModel.toggleJoinOrgModal(false) }
                    )
                }

                if (settingsState.showTrackSimModal) {
                    TrackSimModal(
                        uiState = settingsState,
                        viewModel = settingsViewModel,
                        onDismiss = { settingsViewModel.toggleTrackSimModal(false) }
                    )
                }
                
                if (homeState.showCallSimPicker && homeState.callFlowNumber != null) {
                    CallSimPickerModal(
                        number = homeState.callFlowNumber!!,
                        availableSims = homeState.availableSims,
                        onSimSelected = { subId -> homeViewModel.executeCall(subId) },
                        onDismiss = { homeViewModel.cancelCallFlow() }
                    )
                }

                if (settingsState.showCustomLookupModal) {
                    CustomLookupModal(
                        uiState = settingsState,
                        viewModel = settingsViewModel,
                        onDismiss = { settingsViewModel.toggleCustomLookupModal(false) }
                    )
                }

                if (settingsState.showContactModal) {
                    ContactModal(
                        subject = settingsState.contactSubject,
                        onDismiss = { settingsViewModel.toggleContactModal(false) }
                    )
                }

                if (settingsState.showExcludedModal) {
                    ExcludedContactsModal(
                        excludedPersons = settingsState.excludedPersons,
                        onAddNumbers = { settingsViewModel.addExcludedNumbers(it) },
                        onRemoveNumber = { settingsViewModel.unexcludeNumber(it) },
                        onDismiss = { settingsViewModel.toggleExcludedModal(false) },
                        canAddNew = settingsState.allowPersonalExclusion || settingsState.pairingCode.isEmpty(),
                        onAddNumbersWithType = { numbers, isNoTracking -> 
                            settingsViewModel.addExcludedNumbersWithType(numbers, isNoTracking) 
                        },
                        onUpdateExclusionType = { phone, isNoTracking ->
                            settingsViewModel.updateExclusionType(phone, isNoTracking)
                        }
                    )
                }

                if (settingsState.showWhatsappModal) {
                    WhatsAppSelectionModal(
                        currentSelection = settingsState.whatsappPreference,
                        availableApps = settingsState.availableWhatsappApps,
                        onSelect = { selection, _ ->
                            settingsViewModel.updateWhatsappPreference(selection)
                            settingsViewModel.toggleWhatsappModal(false)
                        },
                        onDismiss = { settingsViewModel.toggleWhatsappModal(false) }
                    )
                }

                if (settingsState.showResetConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { settingsViewModel.toggleResetConfirmDialog(false) },
                        title = { Text("Reset Sync Data Status") },
                        text = { Text("This will reset the sync status of all logs. They will be re-synced in the next cycle.") },
                        confirmButton = {
                            TextButton(onClick = {
                                settingsViewModel.resetSyncStatus()
                                settingsViewModel.toggleResetConfirmDialog(false)
                            }) { Text("Confirm") }
                        },
                        dismissButton = {
                            TextButton(onClick = { settingsViewModel.toggleResetConfirmDialog(false) }) { Text("Cancel") }
                        }
                    )
                }

                if (settingsState.showClearDataDialog) {
                    AlertDialog(
                        onDismissRequest = { settingsViewModel.toggleClearDataDialog(false) },
                        icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                        title = { Text("Clear All App Data") },
                        text = { Text("This will permanently delete all logs, notes, and settings. This cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    settingsViewModel.clearAllAppData {
                                        settingsViewModel.toggleClearDataDialog(false)
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Clear All Data") }
                        },
                        dismissButton = {
                            TextButton(onClick = { settingsViewModel.toggleClearDataDialog(false) }) { Text("Cancel") }
                        }
                    )
                }
                
                if (settingsState.showRecordingEnablementDialog) {
                    AlertDialog(
                        onDismissRequest = { settingsViewModel.toggleRecordingDialog(false) },
                        title = { Text("Enable Recording Sync") },
                        text = { Text("Would you like to scan recordings for previous calls as well?") },
                        confirmButton = {
                            Button(onClick = { settingsViewModel.updateCallRecordEnabled(enabled = true, scanOld = true) }) { Text("Scan All") }
                        },
                        dismissButton = {
                            TextButton(onClick = { settingsViewModel.updateCallRecordEnabled(enabled = true, scanOld = false) }) { Text("New Only") }
                        }
                    )
                }

                if (settingsState.showRecordingDisablementDialog) {
                    AlertDialog(
                        onDismissRequest = { settingsViewModel.toggleRecordingDisableDialog(false) },
                        title = { Text("Disable Recording Sync?") },
                        text = { Text("Recording sync will be stopped. Pending uploads will be cancelled.") },
                        confirmButton = {
                            Button(
                                onClick = { settingsViewModel.updateCallRecordEnabled(enabled = false) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("Disable") }
                        },
                        dismissButton = {
                            TextButton(onClick = { settingsViewModel.toggleRecordingDisableDialog(false) }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }

        // Dialer Full Screen Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = showDialerSheet,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                    DialerScreen(
                        initialNumber = dialerInitialNumber ?: "",
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
