package com.miniclick.calltrackmanage.ui.home

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.MainViewModel
import com.miniclick.calltrackmanage.ui.settings.SettingsViewModel
import com.miniclick.calltrackmanage.ui.settings.TrackSimModal
import com.miniclick.calltrackmanage.ui.settings.JoinOrgModal
import com.miniclick.calltrackmanage.ui.settings.CloudSyncModal
import com.miniclick.calltrackmanage.ui.settings.CreateOrgModal
import com.miniclick.calltrackmanage.ui.settings.AccountInfoModal
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip

data class GuideStep(
    val type: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val actionLabel: String,
    val onAction: () -> Unit,
    val secondaryActionLabel: String? = null,
    val onSecondaryAction: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingGuide(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    asEmptyState: Boolean = false,
    contentWhenDone: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val isDismissed by mainViewModel.isSessionOnboardingDismissed.collectAsState()
    
    val uiState by settingsViewModel.uiState.collectAsState()
    
    // --- State for Permissions ---
    var hasCallLog by remember { mutableStateOf(false) }
    var hasContacts by remember { mutableStateOf(false) }
    var hasPhoneState by remember { mutableStateOf(false) }
    var hasSimInfo by remember { mutableStateOf(false) } // READ_PHONE_NUMBERS
    var hasNotifications by remember { mutableStateOf(false) }
    var hasStorage by remember { mutableStateOf(false) }
    
    // --- Modals State ---
    var showSimModal by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCloudSyncModal by remember { mutableStateOf(false) }
    var showJoinOrgModal by remember { mutableStateOf(false) }
    var showCreateOrgModal by remember { mutableStateOf(false) }
    var showAccountInfoModal by remember { mutableStateOf(false) }
    var accountEditField by remember { mutableStateOf<String?>(null) }

    // --- Helpers / Launchers ---

    fun checkPermissions() {
        hasCallLog = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasContacts = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasPhoneState = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        hasSimInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        checkPermissions()
        if (isGranted) {
            com.miniclick.calltrackmanage.service.SyncService.start(context)
        }
        
        // If storage granted, auto-enable recording setting
        if (hasStorage && !uiState.callRecordEnabled) {
            settingsViewModel.updateCallRecordEnabled(true)
        }
    }
    
    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        checkPermissions()
        if (permissions.containsValue(true)) {
            com.miniclick.calltrackmanage.service.SyncService.start(context)
        }
    }
    
    // Initial check
    LaunchedEffect(Unit) {
        checkPermissions()
        settingsViewModel.fetchSimInfo()
    }
    
    // Refresh SIM info when permissions change
    LaunchedEffect(hasPhoneState, hasSimInfo) {
        if (hasPhoneState || hasSimInfo) {
            settingsViewModel.fetchSimInfo()
        }
    }

    // --- Steps Logic ---
    
    val steps = remember(hasCallLog, hasContacts, hasPhoneState, hasSimInfo, hasNotifications, hasStorage, uiState) {
        mutableListOf<GuideStep>().apply {
            if (!hasCallLog) {
                add(GuideStep("CALL_LOG", "Allow Call Log", "Required to track and organize your calls automatically.", Icons.Default.Call, "Allow Access", onAction = { 
                    singlePermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) 
                }))
            }
            if (!hasContacts) {
                add(GuideStep("CONTACTS", "All Access Contacts", "Identify callers by name from your contact list.", Icons.Default.People, "Allow Access", onAction = {
                    singlePermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }))
            }
            if (!hasPhoneState) {
                add(GuideStep("PHONE_STATE", "Call Manage", "Detect active calls to show caller information in real-time.", Icons.Default.Phone, "Allow Access", onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        multiPermissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS))
                    } else {
                        singlePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    }
                }))
            }
            if (!settingsViewModel.isTrackStartDateSet()) {
                add(GuideStep("START_DATE", "Track Start Date", "Choose when you want to start tracking calls from. Recommended: 3-7 days ago.", Icons.Default.CalendarToday, "Set Date", onAction = {
                    showDatePicker = true
                }))
            }
            if (uiState.simSelection == "Off") {
                add(GuideStep("SIM_SELECTION", "Select Sim Card to Track", "Choose which SIM cards you want to monitor calls for.", Icons.Default.SimCard, "Select SIM", onAction = {
                    showSimModal = true
                }))
            }
            
            val hasPhysicalSim1 = uiState.availableSims.any { it.slotIndex == 0 }
            val hasPhysicalSim2 = uiState.availableSims.any { it.slotIndex == 1 }
            val sim1Needs = hasPhysicalSim1 && (uiState.simSelection == "Sim1" || uiState.simSelection == "Both") && (uiState.callerPhoneSim1.isBlank() || uiState.sim1SubId == null)
            val sim2Needs = hasPhysicalSim2 && (uiState.simSelection == "Sim2" || uiState.simSelection == "Both") && (uiState.callerPhoneSim2.isBlank() || uiState.sim2SubId == null)
            
            if (sim1Needs || sim2Needs) {
                add(GuideStep("SIM_SETUP", "Setup Tracked SIMs", "Configure your selected SIM cards for accurate cloud sync.", Icons.Default.Tune, "Setup Now", onAction = {
                    showSimModal = true
                }))
            }
            
            if ((!uiState.callRecordEnabled && !uiState.userDeclinedRecording) || (!hasStorage && uiState.callRecordEnabled)) {
                 // Logic: If already enabled but missing storage, show it.
                 // If disabled AND not declined, show it.
                 // Wait, simplified: Show if logic requires setup.
                 // If user declined, we hide it unless enabled manually later?
                 // But wait, if callRecordEnabled is false and userDeclinedRecording is false, show.
                 // If callRecordEnabled is true but no storage, show (ignoring decline becuase they turned it on!).
            }
            // Let's refine the condition.
            val showRecordingStep = if (uiState.callRecordEnabled) !hasStorage else !uiState.userDeclinedRecording
            
            if (showRecordingStep) {
                add(GuideStep(
                    type = "RECORDING",
                    title = "Attach Call Recording?",
                    description = "Auto-attach recordings to call logs for easy playback.",
                    icon = Icons.Default.Mic,
                    actionLabel = "Enable Now",
                    onAction = {
                        if (!hasStorage) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                singlePermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                            } else {
                                singlePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        } else {
                            settingsViewModel.updateCallRecordEnabled(true)
                        }
                    },
                    secondaryActionLabel = "Skip / Don't Attach",
                    onSecondaryAction = {
                        settingsViewModel.updateUserDeclinedRecording(true)
                        settingsViewModel.updateCallRecordEnabled(false)
                    }
                ))
            }
            
            if (!hasNotifications) {
                add(GuideStep("NOTIFICATIONS", "Enable Notifications", "Get updates on sync status and incoming calls.", Icons.Default.Notifications, "Allow", onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }))
            }
            
            if (uiState.pairingCode.isBlank()) {
                add(GuideStep(
                    type = "JOIN_ORG",
                    title = "Connect to Cloud",
                    description = "Sync your calls to the organisation dashboard.",
                    icon = Icons.Default.CloudUpload,
                    actionLabel = "Setup Cloud Syncing",
                    secondaryActionLabel = "Keep Everything Offline",
                    onAction = { showCloudSyncModal = true },
                    onSecondaryAction = { mainViewModel.dismissOnboardingSession() }
                ))
            }
        }
    }
    // --- UI Render ---
    val shouldShowOnboarding = steps.isNotEmpty() && !isDismissed
    
    Box(modifier = modifier) {
        if (shouldShowOnboarding) {
            AnimatedContent(
                targetState = steps.firstOrNull(),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(500, delayMillis = 90)) + 
                     scaleIn(initialScale = 0.92f, animationSpec = tween(500, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(400)))
                },
                label = "step_transition",
                modifier = if (asEmptyState) Modifier.fillMaxSize() else Modifier.fillMaxWidth().wrapContentHeight()
            ) { step ->
                if (step != null) {
                    OnboardingPromo(
                        title = step.title,
                        description = step.description,
                        icon = step.icon,
                        actionLabel = step.actionLabel,
                        onAction = step.onAction,
                        onDismiss = { mainViewModel.dismissOnboardingSession() },
                        asEmptyState = asEmptyState,
                        secondaryActionLabel = step.secondaryActionLabel,
                        onSecondaryAction = step.onSecondaryAction
                    )
                }
            }
        } else {
            // Ensure content has its own background to prevent any visual bleed-through
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .zIndex(1f)
            ) {
                contentWhenDone()
            }
        }
    }

    // --- Modals ---
    if (showCloudSyncModal) {
        CloudSyncModal(
            uiState = uiState,
            viewModel = settingsViewModel,
            onDismiss = { showCloudSyncModal = false },
            onOpenAccountInfo = { field -> 
                accountEditField = field
                showAccountInfoModal = true 
            },
            onCreateOrg = {
                showCreateOrgModal = true
            },
            onJoinOrg = {
                showJoinOrgModal = true
            }
        )
    }

    if (showCreateOrgModal) {
        CreateOrgModal(
            onDismiss = { showCreateOrgModal = false }
        )
    }

    if (showJoinOrgModal) {
        JoinOrgModal(
            viewModel = settingsViewModel,
            onDismiss = { showJoinOrgModal = false }
        )
    }

    if (showAccountInfoModal) {
        AccountInfoModal(
            uiState = uiState,
            viewModel = settingsViewModel,
            editField = accountEditField,
            onDismiss = { 
                showAccountInfoModal = false
                accountEditField = null
            }
        )
    }

    if (showSimModal) {
        TrackSimModal(
            uiState = uiState,
            viewModel = settingsViewModel,
            onDismiss = { showSimModal = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis() - 86400000 // Yesterday
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        settingsViewModel.updateTrackStartDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun OnboardingPromo(
    title: String,
    description: String,
    icon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    asEmptyState: Boolean = false,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    if (asEmptyState) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .scale(iconScale),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(Modifier.height(48.dp))
            
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.titleMedium)
            }

            if (secondaryActionLabel != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onSecondaryAction?.invoke() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(secondaryActionLabel, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(iconScale),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Button(
                        onClick = onAction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(actionLabel, style = MaterialTheme.typography.titleMedium)
                    }

                    if (secondaryActionLabel != null) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { onSecondaryAction?.invoke() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(secondaryActionLabel, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
