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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.miniclick.calltrackmanage.ui.settings.SettingsUiState
import com.miniclick.calltrackmanage.ui.settings.AccountInfoModal
import com.miniclick.calltrackmanage.util.permissions.DevicePermissionGuide
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
    var hasCallPhone by remember { mutableStateOf(checkPermission(Manifest.permission.CALL_PHONE)) }
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
        hasCallPhone = checkPermission(Manifest.permission.CALL_PHONE)
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
    var showPermissionFallbackSheet by remember { mutableStateOf(false) }
    var fallbackPermissionType by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Track if a permission request was started (to detect if dialog didn't appear)
    var permissionRequestStartTime by rememberSaveable { mutableStateOf<Long?>(null) }
    var permissionRequestType by rememberSaveable { mutableStateOf<String?>(null) }

    // Lifecycle observer to re-check permissions and default dialer when returning to app
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkPermissions()
                
                // FALLBACK: If we were waiting for a permission and we are back, 
                // but it's still not granted, show the fallback sheet after a tiny delay
                // (to give system time to update its state)
                if (permissionRequestStartTime != null && permissionRequestType != null) {
                    val waitTime = System.currentTimeMillis() - (permissionRequestStartTime ?: 0)
                    if (waitTime > 20) { // If we actually went to background and came back
                        android.util.Log.d("SetupGuide", "App resumed during pending permission $permissionRequestType")
                        
                        // If it's the dialer and still not default, show fallback
                        val isGranted = when (permissionRequestType) {
                            "DEFAULT_DIALER" -> isDefaultDialer
                            "CALL_LOG" -> hasCallLog
                            "CONTACTS" -> hasContacts
                            "PHONE_STATE" -> hasPhoneState && hasCallPhone
                            "NOTIFICATIONS" -> hasNotifications
                            "RECORDING" -> hasStorage
                            else -> true
                        }

                        if (!isGranted) {
                            android.util.Log.d("SetupGuide", "Permission $permissionRequestType still not granted after resume, showing fallback")
                            fallbackPermissionType = permissionRequestType
                            showPermissionFallbackSheet = true
                            permissionRequestStartTime = null
                            permissionRequestType = null
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val duration = System.currentTimeMillis() - (permissionRequestStartTime ?: 0)
        checkPermissions()
        
        // If it was denied instantly (< 250ms), the dialog likely didn't show
        if (!isGranted && duration < 250) {
            android.util.Log.w("SetupGuide", "Permission $permissionRequestType denied instantly ($duration ms). Showing fallback.")
            fallbackPermissionType = permissionRequestType
            showPermissionFallbackSheet = true
        }

        // If storage granted, auto-enable recording setting
        if (hasStorage && !uiState.callRecordEnabled) {
            settingsViewModel.updateCallRecordEnabled(true)
        }

        // Clear tracking
        permissionRequestStartTime = null
        permissionRequestType = null
    }
    
    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val duration = System.currentTimeMillis() - (permissionRequestStartTime ?: 0)
        checkPermissions()

        val allGranted = permissions.values.all { it }
        if (!allGranted && duration < 250) {
            android.util.Log.w("SetupGuide", "Multi-permission $permissionRequestType denied instantly ($duration ms). Showing fallback.")
            fallbackPermissionType = permissionRequestType
            showPermissionFallbackSheet = true
        }

        // Clear tracking
        permissionRequestStartTime = null
        permissionRequestType = null
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        android.util.Log.d("SetupGuide", "roleLauncher result received, permissionRequestType=$permissionRequestType")
        
        // Check permission status directly (not from state which may be stale)
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
        val isNowDefaultDialer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            telecomManager.defaultDialerPackage == context.packageName
        } else true
        
        android.util.Log.d("SetupGuide", "isNowDefaultDialer=$isNowDefaultDialer")
        
        // Update state
        isDefaultDialer = isNowDefaultDialer
        checkPermissions()
        
        // Check result
        if (isNowDefaultDialer) {
            // User granted - enable dial button
            android.util.Log.d("SetupGuide", "User granted default dialer, enabling dial button")
            settingsViewModel.updateShowDialButton(true)
        } else if (permissionRequestType == "DEFAULT_DIALER") {
            // User declined/cancelled the dialog - show fallback instructions
            android.util.Log.d("SetupGuide", "User declined, showing fallback sheet")
            fallbackPermissionType = "DEFAULT_DIALER"
            showPermissionFallbackSheet = true
        } else {
            android.util.Log.d("SetupGuide", "No action: isNowDefaultDialer=$isNowDefaultDialer, permissionRequestType=$permissionRequestType")
        }
        
        // Reset tracking
        permissionRequestStartTime = null
        permissionRequestType = null
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

    // Timeout detection for when system permission dialog fails to appear
    LaunchedEffect(permissionRequestStartTime) {
        if (permissionRequestStartTime != null) {
            kotlinx.coroutines.delay(3000) // 3 second timeout
            if (permissionRequestStartTime != null && !showPermissionFallbackSheet) {
                android.util.Log.w("SetupGuide", "Permission request timed out. Showing fallback sheet.")
                fallbackPermissionType = permissionRequestType
                showPermissionFallbackSheet = true
                
                // Clear tracking
                permissionRequestStartTime = null
                permissionRequestType = null
            }
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
        hasCallLog, hasContacts, hasPhoneState, hasCallPhone, hasSimInfo, hasNotifications, hasStorage,
        uiState.simSelection, uiState.callerPhoneSim1, uiState.callerPhoneSim2,
        uiState.sim1SubId, uiState.sim2SubId, uiState.availableSims,
        uiState.callRecordEnabled, uiState.userDeclinedRecording,
        uiState.skippedSteps, isDefaultDialer, uiState.isIgnoringBatteryOptimizations
    ) {
        mutableListOf<GuideStep>().apply {
            // 0. Default Dialer (CRITICAL: Must precede runtime permissions for Play Store Approval)
            if (!isDefaultDialer && !uiState.skippedSteps.contains("DEFAULT_DIALER")) {
                add(GuideStep(
                    type = "DEFAULT_DIALER",
                    title = "Set as Default Dialer",
                    description = "For better tracking and in-call features, use MiniClick as your default phone app.",
                    icon = Icons.Default.Dialpad,
                    actionLabel = "Set as Default",
                    onAction = {
                        permissionRequestStartTime = System.currentTimeMillis()
                        permissionRequestType = "DEFAULT_DIALER"
                        var dialogLaunched = false
                        
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
                                if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) {
                                    val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                                    roleLauncher.launch(intent)
                                    dialogLaunched = true
                                } else {
                                    // Role not available - show fallback
                                    fallbackPermissionType = "DEFAULT_DIALER"
                                    showPermissionFallbackSheet = true
                                }
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = android.content.Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                    putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                                }
                                roleLauncher.launch(intent)
                                dialogLaunched = true
                            }
                        } catch (e: android.content.ActivityNotFoundException) {
                            // Dialog not available on this device - show fallback
                            fallbackPermissionType = "DEFAULT_DIALER"
                            showPermissionFallbackSheet = true
                        } catch (e: Exception) {
                            // Other error - show fallback
                            fallbackPermissionType = "DEFAULT_DIALER"
                            showPermissionFallbackSheet = true
                        }
                        
                        // If dialog wasn't launched, reset tracking
                        if (!dialogLaunched) {
                            permissionRequestStartTime = null
                            permissionRequestType = null
                        }
                    },
                    secondaryActionLabel = "Skip for Now",
                    onSecondaryAction = { 
                        // Skip step and disable dial button since not using as default dialer
                        settingsViewModel.setStepSkipped("DEFAULT_DIALER") 
                        settingsViewModel.updateShowDialButton(false)
                    }
                ))
            }

            if (!hasCallLog && !uiState.skippedSteps.contains("CALL_LOG")) {
                add(GuideStep("CALL_LOG", "Allow Call Log", "Required to track and organize your calls automatically.", Icons.Default.Call, "Allow Access", onAction = { 
                    permissionRequestStartTime = System.currentTimeMillis()
                    permissionRequestType = "CALL_LOG"
                    singlePermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) 
                }))
            }
            if (!hasContacts && !uiState.skippedSteps.contains("CONTACTS")) {
                add(GuideStep("CONTACTS", "All Access Contacts", "Identify callers by name from your contact list.", Icons.Default.People, "Allow Access", onAction = {
                    permissionRequestStartTime = System.currentTimeMillis()
                    permissionRequestType = "CONTACTS"
                    singlePermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }))
            }
            if ((!hasPhoneState || !hasCallPhone) && !uiState.skippedSteps.contains("PHONE_STATE")) {
                add(GuideStep("PHONE_STATE", "MiniClick Calls", "Detect active calls and enable direct calling.", Icons.Default.Phone, "Allow Access", onAction = {
                    permissionRequestStartTime = System.currentTimeMillis()
                    permissionRequestType = "PHONE_STATE"
                    val perms = mutableListOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        perms.add(Manifest.permission.READ_PHONE_NUMBERS)
                    }
                    multiPermissionLauncher.launch(perms.toTypedArray())
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
                            permissionRequestStartTime = System.currentTimeMillis()
                            permissionRequestType = "NOTIFICATIONS"
                            singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ))
            }


            // 5. Start Date
            if (!uiState.isTrackStartDateSet && !uiState.skippedSteps.contains("START_DATE")) {
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

            // 6. SIM Selection / Phone Confirmation
            val isSimSelectionDone = uiState.simSelection != "Off"
            val needsSimSetup = !isSimSelectionDone || 
                               (uiState.simSelection == "Sim1" && uiState.callerPhoneSim1.isBlank()) ||
                               (uiState.simSelection == "Sim2" && uiState.callerPhoneSim2.isBlank()) ||
                               (uiState.simSelection == "Both" && (uiState.callerPhoneSim1.isBlank() || uiState.callerPhoneSim2.isBlank()))

            if (needsSimSetup && !uiState.skippedSteps.contains("SIM_SELECTION")) {
                add(GuideStep(
                    type = "SIM_SELECTION", 
                    title = if (!isSimSelectionDone) "Select SIM to Track" else "Confirm Phone Number", 
                    description = if (!isSimSelectionDone) "Choose which SIM cards you want to monitor calls for." 
                                  else "Ensure your phone number is correct for identifying synced calls.", 
                    icon = if (isSimSelectionDone) Icons.Default.PhoneAndroid else Icons.Default.SimCard, 
                    actionLabel = if (!isSimSelectionDone) "Select SIM" else "Setup Number", 
                    onAction = { settingsViewModel.toggleTrackSimModal(true) },
                    secondaryActionLabel = "Skip",
                    onSecondaryAction = { settingsViewModel.setStepSkipped("SIM_SELECTION") }
                ))
            }

            // 7. Recording (Attach Recording)
            val isRecordingDone = uiState.callRecordEnabled && hasStorage
            val showRecordingStep = !isRecordingDone && !uiState.skippedSteps.contains("RECORDING")
            
            if (showRecordingStep) {
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
                            permissionRequestStartTime = System.currentTimeMillis()
                            permissionRequestType = "RECORDING"
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
    
    // Auto-mark setup as complete when no more steps are left
    LaunchedEffect(shouldShowOnboarding) {
        if (!shouldShowOnboarding) {
            // Only mark complete if it wasn't already (to avoid redundant sync triggers)
            if (!uiState.isSetupGuideCompleted) {
                settingsViewModel.setSetupComplete(true)
            }
        }
    }
    
    Box(modifier = modifier) {
        if (shouldShowOnboarding) {
            val currentStep = steps.firstOrNull()
            
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    // Faster animations for snappier permission flow transitions
                    (fadeIn(animationSpec = tween(250, delayMillis = 50)) + 
                     scaleIn(initialScale = 0.94f, animationSpec = tween(250, delayMillis = 50)))
                    .togetherWith(fadeOut(animationSpec = tween(200)))
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
                        onDismiss = { 
                            // Skip this specific step when clicking X
                            settingsViewModel.setStepSkipped(step.type)
                        },
                        asEmptyState = asEmptyState,
                        secondaryActionLabel = step.secondaryActionLabel,
                        onSecondaryAction = step.onSecondaryAction,
                        canDismiss = true
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

    // Permission Fallback Bottom Sheet - shown when device dialog doesn't appear
    if (showPermissionFallbackSheet) {
        PermissionFallbackSheet(
            permissionType = fallbackPermissionType,
            onDismiss = { 
                showPermissionFallbackSheet = false
                fallbackPermissionType = null
                permissionRequestStartTime = null
                permissionRequestType = null
            },
            onSkip = {
                when (fallbackPermissionType) {
                    "DEFAULT_DIALER" -> {
                        settingsViewModel.setStepSkipped("DEFAULT_DIALER")
                        settingsViewModel.updateShowDialButton(false)
                    }
                }
                showPermissionFallbackSheet = false
                fallbackPermissionType = null
            },
            onOpenSettings = {
                try {
                    when (fallbackPermissionType) {
                        "DEFAULT_DIALER" -> {
                            // Open default apps settings
                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                            context.startActivity(intent)
                        }
                        else -> {
                            // Open app settings for regular permissions
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to app settings
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
                showPermissionFallbackSheet = false
                fallbackPermissionType = null
            }
        )
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

/**
 * Bottom sheet shown when a permission dialog doesn't appear on the device.
 * Provides manual instructions for the user to enable the setting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionFallbackSheet(
    permissionType: String?,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    val (title, description, settingsLabel) = when (permissionType) {
        "DEFAULT_DIALER" -> Triple(
            "Couldn't Open Dialog",
            "Your device may not show the default dialer selection automatically. You can manually set MiniClick as your default phone app in Settings.\n\nGo to: Settings → Apps → Default apps → Phone app → Select MiniClick",
            "Open Default Apps"
        )
        "CALL_LOG" -> Triple(
            "Call Log Access",
            "Standard permission dialog didn't appear. Please enable Call Log access manually in Settings to track your calls.",
            "Open App Settings"
        )
        "CONTACTS" -> Triple(
            "Contacts Access",
            "Standard permission dialog didn't appear. Please enable Contacts access manually in Settings to identify callers.",
            "Open App Settings"
        )
        "PHONE_STATE" -> Triple(
            "Phone State Access",
            "Standard permission dialog didn't appear. Please enable Phone permissions manually in Settings to record and detect calls.",
            "Open App Settings"
        )
        "NOTIFICATIONS" -> Triple(
            "Notifications Access",
            "Standard permission dialog didn't appear. Please enable Notifications manually in Settings.",
            "Open App Settings"
        )
        else -> Triple(
            "Permission Required",
            "Please enable this permission manually in your device settings to continue using MiniClick.",
            "Open Settings"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Warning Icon
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Primary Action - Open Settings
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(settingsLabel)
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Secondary Action - Skip
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for Now", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
