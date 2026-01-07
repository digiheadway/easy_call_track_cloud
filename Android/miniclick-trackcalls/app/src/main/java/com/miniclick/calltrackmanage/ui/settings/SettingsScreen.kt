package com.miniclick.calltrackmanage.ui.settings

import android.content.Intent
import android.widget.Toast
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.MainViewModel
import com.miniclick.calltrackmanage.ui.common.SyncQueueModal
import com.miniclick.calltrackmanage.ui.common.RecordingQueueModal
import com.miniclick.calltrackmanage.ui.common.JsonTableView
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.graphics.Color


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel(),
    syncStatusBar: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Modal states
    var showPermissionsModal by remember { mutableStateOf(false) }
    var showCloudSyncModal by remember { mutableStateOf(false) }
    var showAccountInfoModal by remember { mutableStateOf(false) }
    var showExcludedModal by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showContactModal by remember { mutableStateOf(false) }
    var showCreateOrgModal by remember { mutableStateOf(false) }
    var showJoinOrgModal by remember { mutableStateOf(false) }
    var showTrackSimModal by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var contactSubject by remember { mutableStateOf("") }
    var accountEditField by remember { mutableStateOf<String?>(null) } // "pairing", "phone1", "phone2", null for all
    var showCustomLookupModal by remember { mutableStateOf(false) }

    val hasStoragePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val folderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                 context.contentResolver.takePersistableUriPermission(it, 
                     Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            } catch (e: Exception) { e.printStackTrace() }
            viewModel.updateRecordingPath(it.toString())
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(it) }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    val storagePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleRecordingDialog(true)
        } else {
            android.widget.Toast.makeText(context, "Storage permission required for recordings", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (onBack != null) {
        androidx.activity.compose.BackHandler {
            onBack()
        }
    }

    LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.onResume()
    }

    // Permissions Modal
    if (showPermissionsModal) {
        PermissionsModal(
            permissions = uiState.permissions,
            onDismiss = { showPermissionsModal = false }
        )
    }

    // Cloud Sync Modal
    if (showCloudSyncModal) {
        CloudSyncModal(
            uiState = uiState,
            viewModel = viewModel,
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
    
    // Create Org Modal
    if (showCreateOrgModal) {
        CreateOrgModal(
            onDismiss = { showCreateOrgModal = false }
        )
    }

    // Track SIM Modal
    if (showTrackSimModal) {
        TrackSimModal(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showTrackSimModal = false }
        )
    }

    // Join Org Modal
    if (showJoinOrgModal) {
        JoinOrgModal(
            viewModel = viewModel,
            onDismiss = { showJoinOrgModal = false }
        )
    }

    // Account Info Modal
    if (showAccountInfoModal) {
        AccountInfoModal(
            uiState = uiState,
            viewModel = viewModel,
            editField = accountEditField,
            onDismiss = { 
                showAccountInfoModal = false
                accountEditField = null
            }
        )
    }

    // Custom Lookup Modal
    if (showCustomLookupModal) {
        CustomLookupModal(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showCustomLookupModal = false }
        )
    }

    // Contact Modal
    if (showContactModal) {
        ContactModal(
            subject = contactSubject,
            onDismiss = { showContactModal = false }
        )
    }

    // Excluded Contacts Modal
    if (showExcludedModal) {
        ExcludedContactsModal(
            excludedPersons = uiState.excludedPersons,
            onAddNumbers = { viewModel.addExcludedNumbers(it) },
            onRemoveNumber = { viewModel.unexcludeNumber(it) },
            onDismiss = { showExcludedModal = false },
            canAddNew = uiState.allowPersonalExclusion || uiState.pairingCode.isEmpty(),
            onAddNumbersWithType = { numbers, isNoTracking -> 
                viewModel.addExcludedNumbersWithType(numbers, isNoTracking) 
            },
            onUpdateExclusionType = { phone, isNoTracking ->
                viewModel.updateExclusionType(phone, isNoTracking)
            }
        )
    }

    // Reset Sync Data Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Sync Data Status") },
            text = { Text("This will reset the sync status of all logs. They will be re-synced in the next cycle. Use this if you are missing data on the cloud.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetSyncStatus()
                    showResetConfirmDialog = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Using centralized modals in MainActivity

    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All App Data") },
            text = { 
                Text(
                    "This will permanently delete all call logs, person notes, sync history, and settings. " +
                    "This action cannot be undone.\n\nAre you sure you want to continue?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllAppData {
                            showClearDataDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Recording Enablement Dialog
    if (uiState.showRecordingEnablementDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleRecordingDialog(false) },
            title = { Text("Enable Recording Sync") },
            text = { Text("Would you like to scan and sync recordings for previous calls as well, or only for new calls starting now?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.updateCallRecordEnabled(enabled = true, scanOld = true) }
                ) { Text("Scan All") }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.updateCallRecordEnabled(enabled = true, scanOld = false) }
                ) { Text("New Only") }
            }
        )
    }

    // Recording Disablement Dialog
    if (uiState.showRecordingDisablementDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleRecordingDisableDialog(false) },
            title = { Text("Disable Recording Sync?") },
            text = { Text("Recording sync will be stopped. Any currently pending uploads will be cancelled. New calls will not be checked for recordings.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.updateCallRecordEnabled(enabled = false) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleRecordingDisableDialog(false) }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More") },
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                } else {
                    {}
                },
                actions = {
                    if (uiState.pairingCode.isNotEmpty()) {
                        IconButton(onClick = { viewModel.toggleRecordingQueue(true) }) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Recording Queue",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            syncStatusBar()
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
            // ===============================================
            // 1. CLOUD & ACCOUNT
            // ===============================================
            SettingsSection(title = "Cloud & Account") {
                // The Sync Card is technically part of this section but we keep its distinctive Card look
                SyncCloudCard(uiState, viewModel, { viewModel.toggleSyncQueue(true) }, { showCloudSyncModal = true }, { showResetConfirmDialog = true })
            }

            if (uiState.pairingCode.isNotEmpty()) {
                val lastSyncText = remember(uiState.lastSyncTime) {
                    if (uiState.lastSyncTime == 0L) "never synced"
                    else {
                        val diff = System.currentTimeMillis() - uiState.lastSyncTime
                        val minutes = diff / (1000 * 60)
                        if (minutes < 60) {
                            if (minutes <= 0) "synced just now"
                            else "synced $minutes minute${if (minutes > 1) "s" else ""} ago"
                        } else {
                            val sdf = SimpleDateFormat("h:mm a, d MMM", Locale.getDefault())
                            "synced at ${sdf.format(Date(uiState.lastSyncTime))}"
                        }
                    }
                }
                
                Text(
                    text = lastSyncText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===============================================
            // 2. TRACKING CONFIGURATION
            // ===============================================
            val dateString = if (uiState.trackStartDate == 0L) "Default (Yesterday)" else 
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(uiState.trackStartDate))

            SettingsSection(title = "Tracking Configuration") {
                val isDateLocked = !uiState.allowChangingTrackingStartDate && uiState.pairingCode.isNotEmpty()
                ListItem(
                    headlineContent = { Text("Call Tracking Starting Date") },
                    supportingContent = { Text(dateString) },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.CalendarToday, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        if (isDateLocked) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable {
                        if (isDateLocked) {
                            Toast.makeText(context, "Locked by your organisation", Toast.LENGTH_SHORT).show()
                        } else {
                            showDatePicker(context, uiState.trackStartDate) { newDate ->
                                viewModel.updateTrackStartDate(newDate)
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                val isSimLocked = !uiState.allowUpdatingTrackingSims && uiState.pairingCode.isNotEmpty()
                ListItem(
                    headlineContent = { Text("Track Calls") },
                    supportingContent = { 
                        val currentSim = when(uiState.simSelection) {
                            "Off" -> "Off"
                            "Both" -> "Both SIMs"
                            else -> uiState.simSelection.replace("Sim", "SIM ")
                        }
                        val phoneInfo = if (uiState.simSelection != "Off") {
                            val details = mutableListOf<String>()
                            if (uiState.simSelection == "Sim1" || uiState.simSelection == "Both") {
                                details.add("S1: ${uiState.callerPhoneSim1.ifEmpty { "Not set" }}")
                            }
                            if (uiState.simSelection == "Sim2" || uiState.simSelection == "Both") {
                                details.add("S2: ${uiState.callerPhoneSim2.ifEmpty { "Not set" }}")
                            }
                            " (" + details.joinToString(" | ") + ")"
                        } else ""
                        
                        if (isSimLocked) {
                            Text(currentSim + phoneInfo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(currentSim + phoneInfo) 
                        }
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.SimCard, MaterialTheme.colorScheme.tertiary) 
                    },
                    trailingContent = {
                        if (isSimLocked) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { 
                        if (isSimLocked) {
                            Toast.makeText(context, "Locked by your organisation", Toast.LENGTH_SHORT).show()
                        } else {
                            showTrackSimModal = true
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                val excludedCount = uiState.excludedPersons.size
                val isExclusionLocked = !uiState.allowPersonalExclusion && uiState.pairingCode.isNotEmpty()
                ListItem(
                    headlineContent = { Text("Excluded Contacts") },
                    supportingContent = { 
                        Text(
                            if (excludedCount == 0) "No numbers excluded" 
                            else "$excludedCount number${if (excludedCount > 1) "s" else ""} excluded"
                        )
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.PersonOff, MaterialTheme.colorScheme.error) 
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isExclusionLocked) {
                                Icon(Icons.Default.Lock, contentDescription = "Adding locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { showExcludedModal = true }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Attach Recordings Toggle
                ListItem(
                    headlineContent = { Text("Attach Recordings") },
                    supportingContent = { 
                        if (uiState.callRecordEnabled && !hasStoragePermission) {
                            Text("Permission Required to attach recordings", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Upload recordings to the cloud")
                        }
                    },
                    leadingContent = { 
                        SettingsIcon(
                            Icons.Default.Mic, 
                            if (uiState.callRecordEnabled && !hasStoragePermission) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        ) 
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.callRecordEnabled && hasStoragePermission,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val hasStorage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    } else {
                                        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    }
                                    
                                    if (hasStorage) {
                                        viewModel.toggleRecordingDialog(true)
                                    } else {
                                        storagePermissionLauncher.launch(
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) 
                                                android.Manifest.permission.READ_MEDIA_AUDIO 
                                            else 
                                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                                        )
                                    }
                                } else {
                                    viewModel.toggleRecordingDisableDialog(true)
                                }
                            }
                        )
                    }
                )

                // Recording Path with verification
                var showPathWarningDialog by remember { mutableStateOf(false) }
                
                // Warning Dialog for editing verified path
                if (showPathWarningDialog) {
                    AlertDialog(
                        onDismissRequest = { showPathWarningDialog = false },
                        icon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFEAB308)) },
                        title = { Text("Change Recording Path?") },
                        text = {
                            Column {
                                Text(
                                    "Current path is verified with ${uiState.recordingCount} recordings found.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Changing this may break recording detection. Only change if recordings are not being found correctly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showPathWarningDialog = false
                                    folderLauncher.launch(null)
                                }
                            ) {
                                Text("Change Anyway")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPathWarningDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                AnimatedVisibility(
                    visible = uiState.callRecordEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ListItem(
                        headlineContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Recording Path")
                                if (uiState.isRecordingPathVerified) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        supportingContent = { 
                            Column {
                                val displayPath = if (uiState.recordingPath.isNotEmpty()) {
                                    try {
                                        val decoded = Uri.decode(uiState.recordingPath)
                                        // Show relative path from storage
                                        if (decoded.contains("/storage/emulated/0/")) {
                                            decoded.replace("/storage/emulated/0/", "")
                                        } else {
                                            decoded.takeLast(40)
                                        }
                                    } catch(e: Exception) { uiState.recordingPath.takeLast(40) }
                                } else {
                                    "Not detected"
                                }
                                
                                Text(displayPath, maxLines = 1)
                                
                                // Status line
                                val statusText = buildString {
                                    if (uiState.isRecordingPathCustom) {
                                        append("Custom")
                                    } else {
                                        append("Auto-detected")
                                    }
                                    if (uiState.recordingCount > 0) {
                                        append(" • ${uiState.recordingCount} recordings")
                                    }
                                }
                                
                                Text(
                                    statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isRecordingPathVerified) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        leadingContent = { 
                            SettingsIcon(Icons.Default.FolderOpen, MaterialTheme.colorScheme.tertiary) 
                        },
                        trailingContent = {
                            Row {
                                if (uiState.isRecordingPathCustom) {
                                    // Show reset button if custom path is set
                                    IconButton(onClick = { viewModel.clearCustomRecordingPath() }) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Use Auto-Detected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (uiState.isRecordingPathVerified && uiState.recordingCount > 0) {
                                    showPathWarningDialog = true
                                } else {
                                    folderLauncher.launch(null)
                                }
                            }
                    )
                }
            }

            // ===============================================
            // POST-CALL PROMPTS
            // ===============================================
            SettingsSection(title = "Post-Call Prompts") {
                // 1. Google Dialer Recording Reminder (Only if recording is enabled AND it's a Google Dialer phone)
                if (uiState.callRecordEnabled && uiState.isGoogleDialer) {
                    ListItem(
                        headlineContent = { Text("Recording Share Reminder") },
                        supportingContent = { 
                            Text("Reminder to share recording after calls") 
                        },
                        leadingContent = { 
                            SettingsIcon(Icons.Default.NotificationsActive, MaterialTheme.colorScheme.primary) 
                        },
                        trailingContent = {
                            Switch(
                                checked = uiState.showRecordingReminder,
                                onCheckedChange = { viewModel.updateShowRecordingReminder(it) }
                            )
                        }
                    )
                }

                // 2. Note for Unknowns
                ListItem(
                    headlineContent = { Text("Note for Unknowns") },
                    supportingContent = { 
                        Text("Prompt for note/label after unknown calls") 
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.EditNote, MaterialTheme.colorScheme.secondary) 
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.showUnknownNoteReminder,
                            onCheckedChange = { viewModel.updateShowUnknownNoteReminder(it) }
                        )
                    }
                )
            }
            // 3. FEATURES
            // ===============================================
            SettingsSection(title = "Features") {

                // WhatsApp Preference
                var showWhatsappModal by remember { mutableStateOf(false) }
                
                ListItem(
                    headlineContent = { Text("Default WhatsApp") },
                    supportingContent = { 
                        val currentLabel = if (uiState.whatsappPreference == "Always Ask") "Always Ask"
                                         else uiState.availableWhatsappApps.find { it.packageName == uiState.whatsappPreference }?.label 
                                              ?: uiState.whatsappPreference
                        Text(currentLabel) 
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.AutoMirrored.Filled.Message, MaterialTheme.colorScheme.secondary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { 
                        viewModel.fetchWhatsappApps()
                        showWhatsappModal = true 
                    }
                )
                
                if (showWhatsappModal) {
                    WhatsAppSelectionModal(
                        currentSelection = uiState.whatsappPreference,
                        availableApps = uiState.availableWhatsappApps,
                        onSelect = { selection ->
                            viewModel.updateWhatsappPreference(selection)
                            showWhatsappModal = false
                        },
                        onDismiss = { showWhatsappModal = false }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Dialer Toggle
                ListItem(
                    headlineContent = { Text("Display Dialer") },
                    supportingContent = { Text("Show dialer tab for making calls") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Dialpad, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.isDialerEnabled,
                            onCheckedChange = { viewModel.updateDialerEnabled(it) }
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Caller ID Overlay Toggle
                
                
                // Caller ID Overlay Toggle
                ListItem(
                    headlineContent = { Text("Caller ID Overlay") },
                    supportingContent = { 
                        if (!uiState.isOverlayPermissionGranted && uiState.callerIdEnabled) {
                            Text(
                                "Tap to grant overlay permission",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Show contact info during calls")
                        }
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.ContactPhone, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.callerIdEnabled && uiState.isOverlayPermissionGranted,
                            onCheckedChange = { enabled ->
                                val needsOverlayPermission = viewModel.updateCallerIdEnabled(enabled)
                                if (needsOverlayPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    // Request overlay permission via Settings
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                    Toast.makeText(context, "Please grant overlay permission, then return and enable Caller ID", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Custom Lookup") },
                    supportingContent = { 
                        Text(if (uiState.customLookupEnabled) "Custom data lookup active" else "Configure custom phone data lookup")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.ManageSearch, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showCustomLookupModal = true }
                )
            }

            // ===============================================
            // 4. DEVICE
            // ===============================================
            SettingsSection(title = "Device") {
                // Theme
                var showThemeModal by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text("App Theme") },
                    supportingContent = { Text(uiState.themeMode) },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.DarkMode, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showThemeModal = true }
                )
                
                if (showThemeModal) {
                    ThemeSelectionModal(
                        currentTheme = uiState.themeMode,
                        onSelect = { mode ->
                            viewModel.updateThemeMode(mode)
                            showThemeModal = false
                        },
                        onDismiss = { showThemeModal = false }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Permissions moved here
                val grantedCount = uiState.permissions.count { it.isGranted }
                val totalCount = uiState.permissions.size
                ListItem(
                    headlineContent = { Text("App Permissions") },
                    supportingContent = { 
                        Text(
                            "$grantedCount of $totalCount permissions granted",
                            color = if (grantedCount == totalCount) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    },
                    leadingContent = { 
                        SettingsIcon(
                            if (grantedCount == totalCount) Icons.Default.CheckCircle else Icons.Default.Warning,
                            if (grantedCount == totalCount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        ) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showPermissionsModal = true }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===============================================
            // 5. DATA MANAGEMENT
            // ===============================================
            SettingsSection(title = "Data Management") {
                ListItem(
                    headlineContent = { Text("Export Data") },
                    supportingContent = { Text("Backup your local call logs and notes to a file") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Download, MaterialTheme.colorScheme.primary) 
                    },
                    modifier = Modifier.clickable { 
                        exportLauncher.launch("miniclick_backup_${System.currentTimeMillis()}.json")
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Import Data") },
                    supportingContent = { Text("Restore data from a previously exported backup file") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Upload, MaterialTheme.colorScheme.secondary) 
                    },
                    modifier = Modifier.clickable { 
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===============================================
            // 6. SUPPORT
            // ===============================================
            SettingsSection(title = "Support") {
                // Support items
                ListItem(
                    headlineContent = { Text("Report Bug") },
                    supportingContent = { Text("Something not working? Let us know") },
                   leadingContent = { 
                        SettingsIcon(Icons.Default.BugReport, MaterialTheme.colorScheme.error) 
                    },
                    modifier = Modifier.clickable { 
                        contactSubject = "Bug Report"
                        showContactModal = true 
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                ListItem(
                    headlineContent = { Text("Request Feature/Improvement") },
                    supportingContent = { Text("Have an idea? We'd love to hear it") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Lightbulb, Color(0xFFEAB308)) 
                    },
                    modifier = Modifier.clickable { 
                        contactSubject = "Feature Request"
                        showContactModal = true 
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Export Session Logs") },
                    supportingContent = { Text("Download and share app logs for support") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.HistoryEdu, MaterialTheme.colorScheme.primary) 
                    },
                    modifier = Modifier.clickable { 
                        viewModel.exportLogs()
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                ListItem(
                    headlineContent = { Text("Reset Onboarding") },
                    supportingContent = { Text("View the initial app setup tutorial again") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Replay, MaterialTheme.colorScheme.secondary) 
                    },
                    modifier = Modifier.clickable { 
                        mainViewModel.resetOnboardingSession()
                        viewModel.resetOnboarding()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Privacy Policy") },
                    supportingContent = { Text("Review how we handle your data") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.Security, MaterialTheme.colorScheme.primary) 
                    },
                    modifier = Modifier.clickable { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://miniclickcrm.com/privacy"))
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Delete Account") },
                    supportingContent = { Text("Request permanent deletion of your account and data") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.NoAccounts, MaterialTheme.colorScheme.error) 
                    },
                    modifier = Modifier.clickable { 
                        contactSubject = "Account Deletion Request"
                        showContactModal = true 
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { 
                        Text("Clear All App Data", color = MaterialTheme.colorScheme.error) 
                    },
                    supportingContent = { 
                        Text("Delete all call logs, notes, and settings locally")
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.DeleteForever, MaterialTheme.colorScheme.error) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickable { showClearDataDialog = true }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===============================================
            // 7. TROUBLESHOOTING
            // ===============================================
            SettingsSection(title = "Troubleshooting") {
                ListItem(
                    headlineContent = { Text("Recheck Recordings") },
                    supportingContent = { Text("Force a re-scan of local folders for call recordings") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.ManageSearch, MaterialTheme.colorScheme.primary) 
                    },
                    modifier = Modifier.clickable { 
                        viewModel.recheckRecordings()
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Re-import Call History") },
                    supportingContent = { Text("Re-fetch call logs from system and sync them") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.History, MaterialTheme.colorScheme.secondary) 
                    },
                    modifier = Modifier.clickable { 
                        viewModel.reImportCallHistory()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text("Re-import Recordings") },
                    supportingContent = { Text("Reset recording sync status and retry all uploads") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.CloudSync, MaterialTheme.colorScheme.tertiary) 
                    },
                    modifier = Modifier.clickable { 
                        viewModel.reImportRecordings()
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
    }
}

// ===============================================
// CLOUD SYNC MODAL
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncModal(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onOpenAccountInfo: (String?) -> Unit,
    onCreateOrg: () -> Unit,
    onJoinOrg: () -> Unit,
    onKeepOffline: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var newPairingCode by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Sync to Cloud",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            if (uiState.pairingCode.isEmpty()) {
                // STATE 1: NOT JOINED - ONBOARDING
                
                // Benefits List
                val benefits = listOf(
                    "Access on any device",
                    "Cloud Backup",
                    "Give access to team",
                    "Better management"
                )
                
                benefits.forEach { benefit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Check, 
                            null, 
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(benefit, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // Option 1: Create Organisation
                Button(
                    onClick = { 
                        onDismiss()
                        onCreateOrg() 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                   Column(horizontalAlignment = Alignment.CenterHorizontally) {
                       Text("Get 7 Days Free Trial Now", style = MaterialTheme.typography.titleMedium)
                       Text(
                           "Create account to start tracking", 
                           style = MaterialTheme.typography.labelSmall, 
                           color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                   }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Option 2: Join Organisation
                OutlinedButton(
                    onClick = { 
                        onDismiss()
                        onJoinOrg() 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text("Login", style = MaterialTheme.typography.titleMedium)
                }

                if (onKeepOffline != null) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = { 
                            onDismiss()
                            onKeepOffline() 
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Keep Everything Offline", color = MaterialTheme.colorScheme.primary)
                    }
                }

            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoModal(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    editField: String? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = when(editField) {
                        "pairing" -> "Edit Pairing Code"
                        "phone1" -> "Edit SIM 1 Phone"
                        "phone2" -> "Edit SIM 2 Phone"
                        "phones" -> "Edit Device Phones"
                        else -> "Account Information"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            if (editField == null || editField == "pairing") {
                OutlinedTextField(
                    value = uiState.pairingCode,
                    onValueChange = { viewModel.updatePairingCode(it) },
                    label = { Text("Pairing Code") },
                    placeholder = { Text("e.g., UPTOWN-356") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Key, null) }
                )
                
                Text(
                    text = "Format: ORGID-USERID (e.g., UPTOWN-356)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )

                if (editField == null) Spacer(Modifier.height(16.dp))
            }

            if (editField == null || editField == "phone1" || editField == "phones") {
                OutlinedTextField(
                    value = uiState.callerPhoneSim1,
                    onValueChange = { viewModel.updateCallerPhoneSim1(it) },
                    label = { Text("SIM 1 Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )

                if (editField == null || editField == "phones") Spacer(Modifier.height(16.dp))
            }

            if (editField == null || editField == "phone2" || editField == "phones") {
                OutlinedTextField(
                    value = uiState.callerPhoneSim2,
                    onValueChange = { viewModel.updateCallerPhoneSim2(it) },
                    label = { Text("SIM 2 Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, null) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { 
                    viewModel.saveAccountInfo(onSuccess = {
                        onDismiss()
                    })
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                enabled = !uiState.isVerifying
            ) {
                if (uiState.isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Verifying...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save & Verify", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

// ===============================================
// EXCLUDED CONTACTS MODAL
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedContactsModal(
    excludedPersons: List<com.miniclick.calltrackmanage.data.db.PersonDataEntity>,
    onAddNumbers: (String) -> Unit,
    onRemoveNumber: (String) -> Unit,
    onDismiss: () -> Unit,
    canAddNew: Boolean = true,
    onAddNumbersWithType: ((String, Boolean) -> Unit)? = null,
    onUpdateExclusionType: ((String, Boolean) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddDialog by remember { mutableStateOf(false) }
    var numberInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var addAsNoTracking by remember { mutableStateOf(true) }
    
    val noTrackingPersons = excludedPersons.filter { it.excludeFromSync && it.excludeFromList }
    val excludedFromListOnlyPersons = excludedPersons.filter { !it.excludeFromSync && it.excludeFromList }
    
    val currentList = if (selectedTab == 0) noTrackingPersons else excludedFromListOnlyPersons

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            icon = { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Add Numbers to Exclude") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter phone numbers. Separate with commas or new lines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = numberInput,
                        onValueChange = { numberInput = it },
                        label = { Text("Phone Numbers") },
                        placeholder = { Text("+1234567890") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text("Exclusion Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = addAsNoTracking,
                            onClick = { addAsNoTracking = true },
                            label = { 
                                Column {
                                    Text("No Tracking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Text("Stop recording", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            leadingIcon = if (addAsNoTracking) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                            modifier = Modifier.weight(1f)
                        )
                        
                        FilterChip(
                            selected = !addAsNoTracking,
                            onClick = { addAsNoTracking = false },
                            label = { 
                                Column {
                                    Text("Hide Only", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Text("Keep recording", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            leadingIcon = if (!addAsNoTracking) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (onAddNumbersWithType != null) {
                        onAddNumbersWithType(numberInput, addAsNoTracking)
                    } else {
                        onAddNumbers(numberInput)
                    }
                    // Switch to the relevant tab so the user sees the new entry
                    selectedTab = if (addAsNoTracking) 0 else 1
                    
                    numberInput = ""
                    showAddDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { numberInput = ""; showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Privacy Exclusions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${excludedPersons.size} number${if (excludedPersons.size != 1) "s" else ""} excluded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))
            
            TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("No Tracking")
                        Spacer(Modifier.width(6.dp))
                        Badge { Text("${noTrackingPersons.size}") }
                    }
                })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Hidden")
                        Spacer(Modifier.width(6.dp))
                        Badge { Text("${excludedFromListOnlyPersons.size}") }
                    }
                })
            }
            
            Spacer(Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (selectedTab == 0) Icons.Default.CloudOff else Icons.Default.VisibilityOff,
                        null,
                        tint = if (selectedTab == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedTab == 0) "Calls won't be tracked or synced" else "Calls recorded but hidden from list",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!canAddNew) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Adding new exclusions is disabled by your organisation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (currentList.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (selectedTab == 0) Icons.Default.CloudOff else Icons.Default.VisibilityOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        Text(if (selectedTab == 0) "No Numbers in No Tracking" else "No Hidden Numbers", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(if (selectedTab == 0) "Add numbers to stop tracking" else "Add numbers to hide from lists", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                }
            } else {
                Column(modifier = Modifier.weight(1f, fill = false).heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                    currentList.forEach { person ->
                        val isNoTracking = person.excludeFromSync && person.excludeFromList
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isNoTracking) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(person.contactName ?: person.phoneNumber, fontWeight = FontWeight.Medium) },
                                supportingContent = {
                                    Column {
                                        if (person.contactName != null) Text(person.phoneNumber, style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.height(4.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = if (isNoTracking) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                                            Text(if (isNoTracking) "No Tracking" else "Hidden", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = if (isNoTracking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                leadingContent = { Icon(if (isNoTracking) Icons.Default.CloudOff else Icons.Default.VisibilityOff, null, tint = if (isNoTracking) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) },
                                trailingContent = {
                                    Row {
                                        if (onUpdateExclusionType != null) {
                                            IconButton(onClick = { onUpdateExclusionType(person.phoneNumber, !isNoTracking) }) {
                                                Icon(Icons.Default.SwapHoriz, "Change type", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        IconButton(onClick = { onRemoveNumber(person.phoneNumber) }) {
                                            Icon(Icons.Default.RemoveCircle, "Remove", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (canAddNew) {
                Button(onClick = { addAsNoTracking = selectedTab == 0; showAddDialog = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Number", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}


// ===============================================
// PERMISSIONS MODAL
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsModal(
    permissions: List<PermissionState>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "App Permissions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val grantedCount = permissions.count { it.isGranted }
                    Text(
                        text = "$grantedCount of ${permissions.size} granted",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (grantedCount == permissions.size) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            permissions.forEach { perm ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (perm.isGranted) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    ListItem(
                        headlineContent = { 
                            Text(perm.name, fontWeight = FontWeight.Medium) 
                        },
                        supportingContent = {
                            Text(
                                if (perm.isGranted) "Permission granted" else "Required for app functionality",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                if (perm.isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (perm.isGranted) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = if (!perm.isGranted) {
                            {
                                FilledTonalButton(
                                    onClick = { 
                                        // Open app settings
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Grant")
                                }
                            }
                        } else null
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactModal(
    subject: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (subject == "Bug Report") Icons.Default.BugReport else Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = if (subject == "Bug Report") MaterialTheme.colorScheme.error else Color(0xFFEAB308),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = subject,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
            
            Text(
                if (subject == "Account Deletion Request") 
                    "You can delete your account directly through the web panel or request it here via support."
                else 
                    "How would you like to contact us?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // WhatsApp Us
            Card(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919138331357?text=Hi, I want to ${subject.lowercase()}"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF25D366).copy(alpha = 0.1f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.3f))
            ) {
                ListItem(
                    headlineContent = { Text("WhatsApp Us", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("+91 9138331357") },
                    leadingContent = { 
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color(0xFF25D366)) 
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Email Us
            Card(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:digiheadway@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "$subject - SalesCRM App")
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                ListItem(
                    headlineContent = { Text("Email Us", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("digiheadway@gmail.com") },
                    leadingContent = { 
                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) 
                    },
                    colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

// ===============================================
// CREATE ORG MODAL
// ===============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrgModal(
    onDismiss: () -> Unit
) {
     val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
     val context = LocalContext.current
     
     ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
         Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
              Text(
                text = "Start 7 Day Free Trial",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "takes 10 minutes only",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            val steps = listOf(
                "Step 1 - Create account on Website",
                "Step 2 - Start your 7-day free trial",
                "Step 3 - Enter Pairing Code here"
            )
            
            steps.forEach { step -> 
                 Text(
                    text = step,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.miniclickcrm.com"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Create Account & Start Trial")
            }
             Spacer(Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinOrgModal(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pairingCode by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    
    // Client-side format validation
    val isFormatValid = remember(pairingCode) {
        val trimmed = pairingCode.trim().uppercase()
        if (!trimmed.contains("-")) return@remember false
        val parts = trimmed.split("-", limit = 2)
        if (parts.size != 2) return@remember false
        val orgId = parts[0].trim()
        val userId = parts[1].trim()
        orgId.isNotEmpty() && userId.isNotEmpty() && 
        orgId.matches(Regex("^[A-Z0-9]+$")) && 
        userId.matches(Regex("^[0-9]+$"))
    }
    
    val buttonText = when {
        uiState.isVerifying -> "Verifying..."
        uiState.verificationStatus == "verified" -> "Connect"
        isFormatValid -> "Check Pairing Code"
        else -> "Connect Organisation"
    }
    
    val buttonEnabled = when {
        uiState.isVerifying -> false
        uiState.verificationStatus == "verified" -> true
        else -> isFormatValid
    }
    
    // Reset verification when modal closes
    LaunchedEffect(Unit) {
        viewModel.resetVerificationState()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Join Organisation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Enter the pairing code provided by your administrator to connect your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(24.dp))
            
            OutlinedTextField(
                value = pairingCode,
                onValueChange = { 
                    pairingCode = it.uppercase()
                    viewModel.resetVerificationState()
                },
                label = { Text("Pairing Code") },
                placeholder = { Text("e.g., ORGID-123") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !uiState.isVerifying,
                leadingIcon = { Icon(Icons.Default.Key, null) },
                supportingText = {
                    when {
                        !isFormatValid && pairingCode.isNotEmpty() -> {
                            Text(
                                "Format: ORGID-USERID (e.g., GOOGLE-123)",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        isFormatValid && uiState.verificationStatus == null -> {
                            Text(
                                "Format looks good! Click to verify →",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                isError = uiState.verificationStatus == "failed"
            )
            
            // Show verification result
            if (uiState.verificationStatus == "verified") {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Verified Successfully",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Organisation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    uiState.verifiedOrgName ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Employee",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    uiState.verifiedEmployeeName ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = {
                    when {
                        uiState.verificationStatus == "verified" -> {
                            // Step 3: Connect (save)
                            viewModel.connectVerifiedOrganisation(onSuccess = onDismiss)
                        }
                        else -> {
                            // Step 2: Verify with backend
                            viewModel.verifyPairingCodeOnly(pairingCode)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = buttonEnabled,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (uiState.isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(buttonText)
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

// ===============================================
// HELPER COMPOSABLES
// ===============================================

