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
fun SetupGuide(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    asEmptyState: Boolean = false,
    contentWhenDone: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val isDismissed by mainViewModel.isSessionOnboardingDismissed.collectAsState()
    
    val uiState by settingsViewModel.uiState.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    var isDefaultDialer by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                telecomManager.defaultDialerPackage == context.packageName
            } else true
        )
    }
    
    // --- State for Permissions (initialized with actual values to prevent flash) ---
    fun checkPermission(permission: String): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    var hasCallLog by remember { mutableStateOf(checkPermission(Manifest.permission.READ_CALL_LOG)) }
    var hasContacts by remember { mutableStateOf(checkPermission(Manifest.permission.READ_CONTACTS)) }
    var hasPhoneState by remember { mutableStateOf(checkPermission(Manifest.permission.READ_PHONE_STATE)) }
    var hasSimInfo by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
                checkPermission(Manifest.permission.READ_PHONE_NUMBERS) 
            else true
        ) 
    }
    var hasNotifications by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                checkPermission(Manifest.permission.POST_NOTIFICATIONS) 
            else true
        ) 
    }
    var hasStorage by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 
                checkPermission(Manifest.permission.READ_MEDIA_AUDIO) 
            else checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        ) 
    }
    
    // Helpers / Launchers
    fun checkPermissions() {
        hasCallLog = checkPermission(Manifest.permission.READ_CALL_LOG)
        hasContacts = checkPermission(Manifest.permission.READ_CONTACTS)
        hasPhoneState = checkPermission(Manifest.permission.READ_PHONE_STATE)
        hasSimInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) checkPermission(Manifest.permission.READ_PHONE_NUMBERS) else true
        hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) checkPermission(Manifest.permission.POST_NOTIFICATIONS) else true
        hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) checkPermission(Manifest.permission.READ_MEDIA_AUDIO) else checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            isDefaultDialer = telecomManager.defaultDialerPackage == context.packageName
        }
    }

    // Modals State
    var showDatePicker by remember { mutableStateOf(false) }

    // Lifecycle observer to re-check permissions and default dialer when returning to app
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        checkPermissions()
    }
    
    // Initial check
    LaunchedEffect(Unit) {
        checkPermissions()
        settingsViewModel.fetchSimInfo()
    }

    // Auto-show cloud sync modal if not connected and not dismissed
    LaunchedEffect(isDismissed, uiState.pairingCode) {
        if (uiState.pairingCode.isEmpty() && !isDismissed && !uiState.showCloudSyncModal && !mainViewModel.hasShownCloudSyncPrompt) {
            mainViewModel.markCloudSyncPromptShown()
            settingsViewModel.toggleCloudSyncModal(true)
        }
    }
    
    // Refresh SIM info when permissions change
    LaunchedEffect(hasPhoneState, hasSimInfo) {
        if (hasPhoneState || hasSimInfo) {
            settingsViewModel.fetchSimInfo()
        }
    }

    // --- Steps Logic ---
    
    val steps = remember(
        hasCallLog, hasContacts, hasPhoneState, hasSimInfo, hasNotifications, hasStorage,
        uiState.simSelection, uiState.callerPhoneSim1, uiState.callerPhoneSim2,
        uiState.sim1SubId, uiState.sim2SubId, uiState.availableSims,
        uiState.callRecordEnabled, uiState.userDeclinedRecording,
        uiState.skippedSteps, isDefaultDialer
    ) {
        mutableListOf<GuideStep>().apply {
            // 0. Default Dialer (CRITICAL: Must precede runtime permissions for Play Store Approval)
            if (!isDefaultDialer && !uiState.skippedSteps.contains("DEFAULT_DIALER")) {
                add(GuideStep(
                    type = "DEFAULT_DIALER",
                    title = "Set as Default Dialer",
                    description = "To track and archive calls reliably for CRM syncing, MiniClick should be your default phone app.",
                    icon = Icons.Default.Dialpad,
                    actionLabel = "Set as Default",
                    onAction = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
                                if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) {
                                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                                    roleLauncher.launch(intent)
                                }
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = android.content.Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                    putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    secondaryActionLabel = "Skip",
                    onSecondaryAction = { settingsViewModel.setStepSkipped("DEFAULT_DIALER") }
                ))
            }

            if (!hasCallLog && !uiState.skippedSteps.contains("CALL_LOG")) {
                add(GuideStep("CALL_LOG", "Allow Call Log", "Required to track and organize your calls automatically.", Icons.Default.Call, "Allow Access", onAction = { 
                    singlePermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) 
                }))
            }
            if (!hasContacts && !uiState.skippedSteps.contains("CONTACTS")) {
                add(GuideStep("CONTACTS", "All Access Contacts", "Identify callers by name from your contact list.", Icons.Default.People, "Allow Access", onAction = {
                    singlePermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }))
            }
            if (!hasPhoneState && !uiState.skippedSteps.contains("PHONE_STATE")) {
                add(GuideStep("PHONE_STATE", "MiniClick Calls", "Detect active calls to show caller information in real-time.", Icons.Default.Phone, "Allow Access", onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        multiPermissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS))
                    } else {
                        singlePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    }
                }))
            }
            
            // 4. Notifications
            if (!hasNotifications && !uiState.skippedSteps.contains("NOTIFICATIONS")) {
                add(GuideStep(
                    type = "NOTIFICATIONS", 
                    title = "Enable Notifications", 
                    description = "Get updates on sync status and incoming calls.", 
                    icon = Icons.Default.Notifications, 
                    actionLabel = "Allow", 
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    secondaryActionLabel = "Skip",
                    onSecondaryAction = { settingsViewModel.setStepSkipped("NOTIFICATIONS") }
                ))
            }

            // 5. Start Date
            if (!uiState.skippedSteps.contains("START_DATE")) {
                val isSet = uiState.trackStartDate != 0L
                val dateStr = if (isSet) java.text.SimpleDateFormat("MMM dd, yyyy").format(java.util.Date(uiState.trackStartDate)) else ""
                
                add(GuideStep(
                    type = "START_DATE", 
                    title = if (isSet) "Start Date Set" else "Track Start Date", 
                    description = if (isSet) 
                        "Data will be imported from: $dateStr" 
                    else 
                        "Call Data is imported only from the date you select here. No pre-selected date.", 
                    icon = if (isSet) Icons.Default.CheckCircle else Icons.Default.CalendarToday, 
                    actionLabel = if (isSet) "Next" else "Set Tracking Start Date", 
                    onAction = { if (isSet) settingsViewModel.setStepSkipped("START_DATE") else showDatePicker = true },
                    secondaryActionLabel = if (isSet) "Change Date" else "Skip",
                    onSecondaryAction = { if (isSet) showDatePicker = true else settingsViewModel.setStepSkipped("START_DATE") }
                ))
            }

            // 6. SIM Selection
            if (!uiState.skippedSteps.contains("SIM_SELECTION")) {
                val isDone = uiState.simSelection != "Off"
                add(GuideStep(
                    type = "SIM_SELECTION", 
                    title = if (isDone) "SIM Selection Done" else "Select Sim Card to Track", 
                    description = if (isDone) "Your SIM selection is saved." else "Choose which SIM cards you want to monitor calls for.", 
                    icon = if (isDone) Icons.Default.CheckCircle else Icons.Default.SimCard, 
                    actionLabel = if (isDone) "Next" else "Select SIM", 
                    onAction = { if (isDone) settingsViewModel.setStepSkipped("SIM_SELECTION") else settingsViewModel.toggleTrackSimModal(true) },
                    secondaryActionLabel = if (isDone) "Change Selection" else "Skip",
                    onSecondaryAction = { if (isDone) settingsViewModel.toggleTrackSimModal(true) else settingsViewModel.setStepSkipped("SIM_SELECTION") }
                ))
            }

            // 7. Recording (Attach Recording)
            val isRecordingDone = uiState.callRecordEnabled && hasStorage
            val showRecordingStep = (uiState.callRecordEnabled && !hasStorage) || 
                                   (!uiState.callRecordEnabled && !uiState.userDeclinedRecording) ||
                                   (isRecordingDone && !uiState.skippedSteps.contains("RECORDING"))
            
            if (showRecordingStep && !uiState.skippedSteps.contains("RECORDING")) {
                add(GuideStep(
                    type = "RECORDING",
                    title = if (isRecordingDone) "Recording Attached" else "Attach Call Recording?",
                    description = if (isRecordingDone) "Call recordings will be automatically attached to your logs." else "Auto-attach recordings to call logs for easy playback.",
                    icon = if (isRecordingDone) Icons.Default.CheckCircle else Icons.Default.Mic,
                    actionLabel = if (isRecordingDone) "Next" else if (uiState.callRecordEnabled && !hasStorage) "Allow Permission" else "Enable Now",
                    onAction = {
                        if (isRecordingDone) {
                            settingsViewModel.setStepSkipped("RECORDING")
                        } else if (!hasStorage) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                singlePermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                            } else {
                                singlePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        } else {
                            settingsViewModel.updateCallRecordEnabled(true)
                        }
                    },
                    secondaryActionLabel = if (isRecordingDone) "Disable" else "Skip / Don't Attach",
                    onSecondaryAction = {
                        if (isRecordingDone) {
                            settingsViewModel.updateCallRecordEnabled(false)
                        } else {
                            settingsViewModel.updateUserDeclinedRecording(true)
                            settingsViewModel.updateCallRecordEnabled(false)
                            settingsViewModel.setStepSkipped("RECORDING")
                        }
                    }
                ))
            }
            
            // JOIN_ORG step removed to be shown as instant modal instead
        }
    }
    val shouldShowOnboarding = steps.isNotEmpty()
    
    Box(modifier = modifier) {
        if (shouldShowOnboarding) {
            val currentStep = steps.firstOrNull()
            
            AnimatedContent(
                targetState = currentStep,
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
                        onSecondaryAction = step.onSecondaryAction,
                        canDismiss = false
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

    // Modals are handled centrally in MainActivity

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        
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
             Box(
                modifier = Modifier
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                DatePicker(state = datePickerState)
            }
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
    onSecondaryAction: (() -> Unit)? = null,
    canDismiss: Boolean = true
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
                if (canDismiss) {
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
