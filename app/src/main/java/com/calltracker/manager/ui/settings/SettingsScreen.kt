package com.calltracker.manager.ui.settings

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = viewModel()
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
    var contactSubject by remember { mutableStateOf("") }
    var accountEditField by remember { mutableStateOf<String?>(null) } // "pairing", "phone1", "phone2", null for all

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
            }
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
            onDismiss = { showExcludedModal = false }
        )
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                } else {
                    {}
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ===============================================
            // 0. SUPPORT CATEGORY
            // ===============================================
            SettingsSection(title = "Support") {
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
            }

            Spacer(Modifier.height(16.dp))

            // ===============================================
            // 1. CALL TRACKING STARTING DATE
            // ===============================================
            val dateString = if (uiState.trackStartDate == 0L) "Default (Yesterday)" else 
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(uiState.trackStartDate))

            SettingsSection(title = "Call Tracking") {
                ListItem(
                    headlineContent = { Text("Call Tracking Starting Date") },
                    supportingContent = { Text(dateString) },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.CalendarToday, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        showDatePicker(context, uiState.trackStartDate) { newDate ->
                            viewModel.updateTrackStartDate(newDate)
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // ===============================================
                // 2. TRACK CALLS OF (SIM Selection)
                // ===============================================
                var showSimDropdown by remember { mutableStateOf(false) }
                
                ListItem(
                    headlineContent = { Text("Track Calls of") },
                    supportingContent = { 
                        val currentSim = if (uiState.simSelection == "Both") "Both SIMs" 
                                        else uiState.simSelection.replace("Sim", "SIM ")
                        Text(currentSim) 
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.SimCard, MaterialTheme.colorScheme.tertiary) 
                    },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { showSimDropdown = true }) {
                                Icon(Icons.Default.ExpandMore, contentDescription = "Select SIM")
                            }
                            DropdownMenu(
                                expanded = showSimDropdown,
                                onDismissRequest = { showSimDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Both SIMs") },
                                    onClick = { 
                                        viewModel.updateSimSelection("Both")
                                        showSimDropdown = false 
                                    },
                                    leadingIcon = { Icon(Icons.Default.DoneAll, null) }
                                )
                                uiState.availableSims.forEach { sim ->
                                    val simLabel = "SIM ${sim.slotIndex + 1}"
                                    val simValue = "Sim${sim.slotIndex + 1}"
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(simLabel)
                                                Text(sim.displayName, style = MaterialTheme.typography.bodySmall)
                                            }
                                        },
                                        onClick = { 
                                            viewModel.updateSimSelection(simValue)
                                            showSimDropdown = false 
                                        },
                                        leadingIcon = { Icon(Icons.Default.SimCard, null) }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===============================================
            // 3. EXCLUDED CONTACTS (Card -> Modal)
            // ===============================================
            val excludedCount = uiState.excludedPersons.size

            SettingsSection(title = "Exclusions") {
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
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showExcludedModal = true }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===============================================
            // 4. CLOUD SYNC SETTINGS (Card -> Modal)
            // ===============================================
            SettingsSection(title = "Cloud") {
                ListItem(
                    headlineContent = { Text("Cloud Sync Settings") },
                    supportingContent = { Text("Account info and syncing settings") },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.CloudSync, MaterialTheme.colorScheme.primary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showCloudSyncModal = true }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===============================================
            // 5. LOCAL SETTINGS (WhatsApp + Recording Path)
            // ===============================================
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

            SettingsSection(title = "Local Settings") {
                // WhatsApp Preference
                var showWhatsappDropdown by remember { mutableStateOf(false) }
                
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
                        Box {
                            IconButton(onClick = { showWhatsappDropdown = true }) {
                                Icon(Icons.Default.ExpandMore, contentDescription = "Select WhatsApp")
                            }
                            DropdownMenu(
                                expanded = showWhatsappDropdown,
                                onDismissRequest = { showWhatsappDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Always Ask") },
                                    onClick = { 
                                        viewModel.updateWhatsappPreference("Always Ask")
                                        showWhatsappDropdown = false 
                                    },
                                    leadingIcon = { Icon(Icons.Default.QuestionMark, null) }
                                )
                                uiState.availableWhatsappApps.forEach { app ->
                                    DropdownMenuItem(
                                        text = { Text(app.label) },
                                        onClick = { 
                                            viewModel.updateWhatsappPreference(app.packageName)
                                            showWhatsappDropdown = false 
                                        },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, null) }
                                    )
                                }
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Recording Path
                ListItem(
                    headlineContent = { Text("Recording Path") },
                    supportingContent = { 
                        val displayPath = if (uiState.recordingPath.isNotEmpty()) {
                            try {
                                Uri.decode(uiState.recordingPath).takeLast(40)
                            } catch(e: Exception) { uiState.recordingPath.takeLast(40) }
                        } else {
                            "Default: Music/Recordings/Call Recordings"
                        }
                        Text(displayPath, maxLines = 1) 
                    },
                    leadingContent = { 
                        SettingsIcon(Icons.Default.FolderOpen, MaterialTheme.colorScheme.tertiary) 
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { folderLauncher.launch(null) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ===============================================
            // 6. PERMISSIONS (Card -> Modal)
            // ===============================================
            val grantedCount = uiState.permissions.count { it.isGranted }
            val totalCount = uiState.permissions.size

            SettingsSection(title = "Permissions") {
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
            // 7. DANGER ZONE
            // ===============================================
            SettingsSection(
                title = "Danger Zone",
                titleColor = MaterialTheme.colorScheme.error
            ) {
                ListItem(
                    headlineContent = { 
                        Text("Clear All App Data", color = MaterialTheme.colorScheme.error) 
                    },
                    supportingContent = { 
                        Text("Delete all call logs, notes, and settings")
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

            Spacer(Modifier.height(32.dp))
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
    onOpenAccountInfo: (String?) -> Unit
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
                    Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Cloud Sync Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            // Sync Now Button (Secondary style, more compact)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { viewModel.syncCallManually() },
                    enabled = !uiState.isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp), 
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Syncing...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sync Now", style = MaterialTheme.typography.labelLarge)
                    }
                }
                
                // Show last sync time if available
                uiState.lastSyncStats?.let { stats ->
                     Text(
                        text = stats,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (stats.contains("Success")) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Account Status / Details
            Text(
                text = "Account Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(12.dp))

            // Pairing Code Row
            ListItem(
                headlineContent = { Text("Pairing Code") },
                supportingContent = { 
                    Text(if (uiState.pairingCode.isEmpty()) "Not set" else uiState.pairingCode) 
                },
                leadingContent = { Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.secondary) },
                trailingContent = {
                    TextButton(onClick = { onOpenAccountInfo("pairing") }) {
                        Text(if (uiState.pairingCode.isEmpty()) "Add" else "Edit")
                    }
                },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )

            // SIM 1 Phone Row
            ListItem(
                headlineContent = { Text("SIM 1 Phone") },
                supportingContent = { 
                    Text(if (uiState.callerPhoneSim1.isEmpty()) "Not set" else uiState.callerPhoneSim1) 
                },
                leadingContent = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.secondary) },
                trailingContent = {
                    IconButton(onClick = { onOpenAccountInfo("phone1") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )

            // SIM 2 Phone Row
            ListItem(
                headlineContent = { Text("SIM 2 Phone") },
                supportingContent = { 
                    Text(if (uiState.callerPhoneSim2.isEmpty()) "Not set" else uiState.callerPhoneSim2) 
                },
                leadingContent = { Icon(Icons.Default.PhoneAndroid, null, tint = MaterialTheme.colorScheme.secondary) },
                trailingContent = {
                    IconButton(onClick = { onOpenAccountInfo("phone2") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )

            Spacer(Modifier.height(16.dp))

            // Reset Status Button
            OutlinedButton(
                onClick = { viewModel.resetSyncStatus() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Restore, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset Sync Status")
            }

            Spacer(Modifier.height(16.dp))

            // Results are now shown below the Sync button
            Spacer(Modifier.height(16.dp))

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

            if (editField == null || editField == "phone1") {
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

                if (editField == null) Spacer(Modifier.height(16.dp))
            }

            if (editField == null || editField == "phone2") {
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
                    viewModel.saveAccountInfo()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Information", style = MaterialTheme.typography.titleMedium)
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
    excludedPersons: List<com.calltracker.manager.data.db.PersonDataEntity>,
    onAddNumbers: (String) -> Unit,
    onRemoveNumber: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddDialog by remember { mutableStateOf(false) }
    var numberInput by remember { mutableStateOf("") }

    // Add Number Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            icon = { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Add Numbers to Exclude") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter phone numbers to exclude. Separate multiple numbers with commas or new lines.",
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddNumbers(numberInput)
                        numberInput = ""
                        showAddDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    numberInput = ""
                    showAddDialog = false 
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PersonOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Excluded Contacts",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${excludedPersons.size} number${if (excludedPersons.size != 1) "s" else ""} excluded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (excludedPersons.isEmpty()) {
                // Empty State
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No Excluded Numbers",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Add' to exclude numbers from tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // List of excluded numbers
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    excludedPersons.forEach { person ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            ListItem(
                                headlineContent = { 
                                    Text(
                                        person.contactName ?: person.phoneNumber,
                                        fontWeight = FontWeight.Medium
                                    ) 
                                },
                                supportingContent = if (person.contactName != null) {
                                    { Text(person.phoneNumber) }
                                } else null,
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Block,
                                        null,
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                },
                                trailingContent = {
                                    IconButton(onClick = { onRemoveNumber(person.phoneNumber) }) {
                                        Icon(
                                            Icons.Default.RemoveCircle, 
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.height(16.dp))

            // Add Button at Bottom
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Number to Exclude", style = MaterialTheme.typography.titleMedium)
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
// HELPER COMPOSABLES
// ===============================================

@Composable
fun SettingsSection(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.15f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun LifecycleEventEffect(
    event: androidx.lifecycle.Lifecycle.Event,
    onEvent: () -> Unit
) {
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == event) {
                onEvent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

fun showDatePicker(context: Context, initialDate: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    if (initialDate != 0L) {
        calendar.timeInMillis = initialDate
    }
    
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            onDateSelected(newDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
