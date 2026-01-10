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
import com.miniclick.calltrackmanage.util.audio.AudioPlayer
import com.miniclick.calltrackmanage.ui.onboarding.OnboardingScreen
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
import dagger.hilt.android.AndroidEntryPoint

// Navigation tabs
enum class AppTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    DIALER("Dialer", Icons.Default.Dialpad, Icons.Filled.Dialpad),
    CALLS("Calls", Icons.Default.Call, Icons.Filled.Call),
    REPORTS("Reports", Icons.Default.Assessment, Icons.Filled.Assessment),
    SETTINGS("More", Icons.Default.Settings, Icons.Filled.Settings)
}

@AndroidEntryPoint
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
                val isOnboardingCompleted by viewModel.onboardingCompleted.collectAsState()
                val isAgreementAccepted by viewModel.agreementAccepted.collectAsState()

                if (!isOnboardingCompleted) {
                    OnboardingScreen(onComplete = {
                        val settingsRepository = SettingsRepository.getInstance(getApplicationContext())
                        settingsRepository.setOnboardingCompleted(true)
                    })
                } else if (!isAgreementAccepted) {
                    com.miniclick.calltrackmanage.ui.onboarding.AgreementScreen(onAccepted = {
                        viewModel.setAgreementAccepted(true)
                        // Start background services now that we have consent
                        com.miniclick.calltrackmanage.service.SyncService.start(getApplicationContext())
                        com.miniclick.calltrackmanage.worker.CallSyncWorker.enqueue(getApplicationContext())
                        com.miniclick.calltrackmanage.worker.RecordingUploadWorker.enqueue(getApplicationContext())
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
        
        val settingsRepository = com.miniclick.calltrackmanage.data.SettingsRepository.getInstance(this)
        if (settingsRepository.isAgreementAccepted()) {
            com.miniclick.calltrackmanage.service.SyncService.start(this)
        }
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
        
        // Handle sync queue from notification
        if (intent.getBooleanExtra("OPEN_SYNC_QUEUE", false)) {
            viewModel.setOpenSyncQueue(true)
            // Clear the flag to prevent re-triggering on config change
            intent.removeExtra("OPEN_SYNC_QUEUE")
        }
        
        // Handle recording queue from notification
        if (intent.getBooleanExtra("OPEN_RECORDING_QUEUE", false)) {
            viewModel.setOpenRecordingQueue(true)
            // Clear the flag to prevent re-triggering on config change
            intent.removeExtra("OPEN_RECORDING_QUEUE")
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
                                listOf(
                                    RecordingRepository.RecordingSourceFile(
                                        name = importedFile.name,
                                        lastModified = importedFile.lastModified(),
                                        absolutePath = importedFile.absolutePath,
                                        isLocal = true
                                    )
                                ),
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
    
    // Use persisted tab from ViewModel
    val selectedTab by viewModel.selectedTab.collectAsState()
    
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
    
    // Handle notification-triggered queue opening
    val openSyncQueue by viewModel.openSyncQueue.collectAsState()
    val openRecordingQueue by viewModel.openRecordingQueue.collectAsState()
    
    LaunchedEffect(openSyncQueue) {
        if (openSyncQueue) {
            settingsViewModel.toggleSyncQueue(true)
            viewModel.setOpenSyncQueue(false)
        }
    }
    
    LaunchedEffect(openRecordingQueue) {
        if (openRecordingQueue) {
            settingsViewModel.toggleRecordingQueue(true)
            viewModel.setOpenRecordingQueue(false)
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


    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (!settingsState.showTrackingSettings && !settingsState.showExtrasScreen && !settingsState.showDataManagementScreen) {
                    Column {
                        NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { viewModel.setSelectedTab(tab) },
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
                            it.recordingSyncStatus == com.miniclick.calltrackmanage.data.db.RecordingSyncStatus.UPLOADING
                        },
                        isNetworkAvailable = settingsState.isNetworkAvailable,
                        isIgnoringBatteryOptimizations = settingsState.isIgnoringBatteryOptimizations,
                        isSyncSetup = settingsState.isSyncSetup,
                        onSyncNow = { settingsViewModel.syncCallManually() },
                        onShowQueue = { settingsViewModel.toggleSyncQueue(true) },
                        onShowDeviceGuide = { settingsViewModel.toggleDevicePermissionGuide(true) },
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
                    AppTab.REPORTS -> ReportsScreen(
                        syncStatusBar = syncStatusBar,
                        onNavigateToTab = { tabIndex ->
                            viewModel.setSelectedTab(AppTab.CALLS)
                        }
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        syncStatusBar = syncStatusBar
                    )
                }
            }
        }
    }

    // --- Full Screen Overlays (Outside Scaffold to cover BottomBar) ---

    // Tracking Settings
    androidx.compose.animation.AnimatedVisibility(
        visible = settingsState.showTrackingSettings,
        enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + androidx.compose.animation.fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            TrackingSettingsScreen(
                uiState = settingsState,
                viewModel = settingsViewModel,
                onBack = { settingsViewModel.toggleTrackingSettings(false) }
            )
        }
    }

    // Extras
    androidx.compose.animation.AnimatedVisibility(
        visible = settingsState.showExtrasScreen,
        enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }) + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }) + androidx.compose.animation.fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            ExtrasScreen(
                uiState = settingsState,
                viewModel = settingsViewModel,
                onResetOnboarding = { viewModel.resetOnboardingSession() },
                onBack = { settingsViewModel.toggleExtrasScreen(false) }
            )
        }
    }

    // Data Management Bottom Sheet (Modal)
    if (settingsState.showDataManagementScreen) {
        DataManagementBottomSheet(
            uiState = settingsState,
            viewModel = settingsViewModel,
            onDismiss = { settingsViewModel.toggleDataManagementScreen(false) }
        )
    }

    // Dialer Overlay
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


    // --- Unified Modals (Always on Top) ---

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

    if (settingsState.showDevicePermissionGuide) {
        com.miniclick.calltrackmanage.ui.common.DevicePermissionGuideSheet(
            isIgnoringBatteryOptimizations = settingsState.isIgnoringBatteryOptimizations,
            onDismiss = { settingsViewModel.toggleDevicePermissionGuide(false) }
        )
    }

    // Unified Settings Modals
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
        ConfirmationModal(
            title = "Reset Sync Data Status",
            message = "This will reset the sync status of all logs. They will be re-synced in the next cycle.",
            confirmText = "Confirm Reset",
            onConfirm = {
                settingsViewModel.resetSyncStatus()
                settingsViewModel.toggleResetConfirmDialog(false)
            },
            onDismiss = { settingsViewModel.toggleResetConfirmDialog(false) },
            icon = Icons.Default.Restore
        )
    }

    if (settingsState.showClearDataDialog) {
        ConfirmationModal(
            title = "Clear All App Data",
            message = "This will permanently delete all logs, notes, and settings. This cannot be undone.",
            confirmText = "Clear All Data",
            isDestructive = true,
            icon = Icons.Default.DeleteForever,
            onConfirm = {
                settingsViewModel.clearAllAppData {
                    settingsViewModel.toggleClearDataDialog(false)
                }
            },
            onDismiss = { settingsViewModel.toggleClearDataDialog(false) }
        )
    }
    
    if (settingsState.showRecordingEnablementDialog) {
        RecordingActionModal(
            isEnable = true,
            onConfirm = { scanOld ->
                settingsViewModel.updateCallRecordEnabled(enabled = true, scanOld = scanOld)
            },
            onDismiss = { settingsViewModel.toggleRecordingDialog(false) }
        )
    }

    if (settingsState.showRecordingDisablementDialog) {
        RecordingActionModal(
            isEnable = false,
            onConfirm = {
                settingsViewModel.updateCallRecordEnabled(enabled = false)
            },
            onDismiss = { settingsViewModel.toggleRecordingDisableDialog(false) }
        )
    }

    // --- Back Handlers ---

    // 1. If any full-screen overlay is open, close it (HIGHEST PRIORITY)
    // NOTE: DataManagementBottomSheet handles its own dismiss via ModalBottomSheet
    androidx.activity.compose.BackHandler(enabled = settingsState.showTrackingSettings || settingsState.showExtrasScreen) {
        when {
            settingsState.showTrackingSettings -> settingsViewModel.toggleTrackingSettings(false)
            settingsState.showExtrasScreen -> settingsViewModel.toggleExtrasScreen(false)
        }
    }

    // 2. If Dialer is open, handle it
    androidx.activity.compose.BackHandler(enabled = showDialerSheet) {
        showDialerSheet = false
    }

    // 3. Return to CALLS tab if not already there
    // NOTE: Don't block back press if only DataManagement is open, as the BottomSheet will handle it
    val anyFullscreenOpen = settingsState.showTrackingSettings || settingsState.showExtrasScreen
    androidx.activity.compose.BackHandler(enabled = selectedTab != AppTab.CALLS && !showDialerSheet && !anyFullscreenOpen && !settingsState.showDataManagementScreen) {
        viewModel.setSelectedTab(AppTab.CALLS)
    }
}
}
