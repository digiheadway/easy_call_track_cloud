package com.miniclick.calltrackmanage.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full screen for Tracking Settings.
 * Displays all tracking-related configuration options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingSettingsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Handle back button
    androidx.activity.compose.BackHandler {
        onBack()
    }
    
    // Storage permission state
    val hasStoragePermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, 
            android.Manifest.permission.READ_MEDIA_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, 
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Folder picker launcher
    val folderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) { e.printStackTrace() }
            viewModel.updateRecordingPath(it.toString())
        }
    }

    // Storage permission launcher
    val storagePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleRecordingDialog(true)
        } else {
            Toast.makeText(context, "Storage permission required for recordings", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Warning dialog state for changing recording path
    var showPathWarningDialog by remember { mutableStateOf(false) }

    // Recording path warning modal
    if (showPathWarningDialog) {
        ConfirmationModal(
            title = "Change Recording Path?",
            message = "Current path is verified with ${uiState.recordingCount} recordings found. Changing this may break recording detection. Only change if recordings are not being found correctly.",
            confirmText = "Change Anyway",
            icon = Icons.Default.Warning,
            onConfirm = {
                showPathWarningDialog = false
                val initialUri = try { Uri.parse(uiState.recordingPath) } catch (e: Exception) { null }
                folderLauncher.launch(initialUri)
            },
            onDismiss = { showPathWarningDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Tracking Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // ===================================================================
            // CALL TRACKING SECTION
            // ===================================================================
            SettingsSection(title = "Call Tracking") {
                // Track Start Date
                val dateString = if (uiState.trackStartDate == 0L) "Default (Yesterday)" else 
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(uiState.trackStartDate))
                val isDateLocked = !uiState.allowChangingTrackingStartDate && uiState.pairingCode.isNotEmpty()
                
                ListItem(
                    headlineContent = { Text("Call Tracking Starting Date") },
                    supportingContent = { 
                        Column {
                            Text(dateString)
                            Text(
                                "Only calls from this date onwards will be tracked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.CalendarToday, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        if (isDateLocked) {
                            Icon(
                                Icons.Default.Lock, 
                                contentDescription = "Locked", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                modifier = Modifier.size(20.dp)
                            )
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

                // Track Calls (SIM Selection)
                val isSimLocked = !uiState.allowUpdatingTrackingSims && uiState.pairingCode.isNotEmpty()
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
                
                ListItem(
                    headlineContent = { Text("Track Calls") },
                    supportingContent = { 
                        Column {
                            if (isSimLocked) {
                                Text(currentSim + phoneInfo, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(currentSim + phoneInfo)
                            }
                            Text(
                                "Select which SIM cards to track",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.SimCard, MaterialTheme.colorScheme.tertiary) 
                    },
                    trailingContent = {
                        if (isSimLocked) {
                            Icon(
                                Icons.Default.Lock, 
                                contentDescription = "Locked", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { 
                        if (isSimLocked) {
                            Toast.makeText(context, "Locked by your organisation", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.toggleTrackSimModal(true)
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===================================================================
            // IGNORED NUMBERS SECTION
            // ===================================================================
            SettingsSection(title = "Privacy & Exclusions") {
                val excludedCount = uiState.excludedPersons.size
                
                ListItem(
                    headlineContent = { Text("Excluded Numbers") },
                    supportingContent = { 
                        Column {
                            Text(
                                if (excludedCount == 0) "No numbers excluded" 
                                else "$excludedCount number${if (excludedCount > 1) "s" else ""} excluded"
                            )
                            Text(
                                "Manage numbers to stop tracking or hide from list",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.PersonOff, MaterialTheme.colorScheme.error) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { viewModel.toggleExcludedModal(true) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===================================================================
            // RECORDING SETTINGS SECTION
            // ===================================================================
            SettingsSection(title = "Recording Settings") {
                // Attach Recordings Toggle
                ListItem(
                    headlineContent = { Text("Attach Recordings") },
                    supportingContent = { 
                        if (uiState.callRecordEnabled && !hasStoragePermission) {
                            Text("Permission Required to attach recordings", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Upload call recordings to the cloud")
                        }
                    },
                    leadingContent = { 
                        SettingsIcon(
                            Icons.Default.Mic, 
                            if (uiState.callRecordEnabled && !hasStoragePermission) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.tertiary
                        ) 
                    },
                    trailingContent = {
                        Switch(
                            checked = uiState.callRecordEnabled && hasStoragePermission,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val hasStorage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            context, 
                                            android.Manifest.permission.READ_MEDIA_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    } else {
                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                            context, 
                                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
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

                // Recording Path (visible when recording is enabled)
                AnimatedVisibility(
                    visible = uiState.callRecordEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        
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
                                        val initialUri = try { Uri.parse(uiState.recordingPath) } catch (e: Exception) { null }
                                        folderLauncher.launch(initialUri)
                                    }
                                }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ===================================================================
            // TRACKING STATUS INFO
            // ===================================================================
            SettingsSection(title = "Tracking Status") {
                // Tracking Status Display
                val trackingStatus = when {
                    uiState.simSelection == "Off" -> "Disabled"
                    !uiState.callTrackEnabled -> "Disabled by Organisation"
                    else -> "Active"
                }
                val trackingStatusColor = when {
                    uiState.simSelection == "Off" -> MaterialTheme.colorScheme.error
                    !uiState.callTrackEnabled -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                
                ListItem(
                    headlineContent = { Text("Call Tracking Status") },
                    supportingContent = { 
                        Text(
                            trackingStatus,
                            color = trackingStatusColor,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingContent = { 
                        SettingsIcon(
                            if (trackingStatus == "Active") Icons.Default.CheckCircle else Icons.Default.Cancel,
                            trackingStatusColor
                        ) 
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Recording Status Display
                val recordingStatus = when {
                    !uiState.callRecordEnabled -> "Disabled"
                    !hasStoragePermission -> "Permission Required"
                    else -> "Active"
                }
                val recordingStatusColor = when {
                    !uiState.callRecordEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    !hasStoragePermission -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                
                ListItem(
                    headlineContent = { Text("Recording Sync Status") },
                    supportingContent = { 
                        Text(
                            recordingStatus,
                            color = recordingStatusColor,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingContent = { 
                        SettingsIcon(
                            when {
                                recordingStatus == "Active" -> Icons.Default.CloudDone
                                recordingStatus == "Permission Required" -> Icons.Default.Warning
                                else -> Icons.Default.CloudOff
                            },
                            recordingStatusColor
                        ) 
                    }
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
