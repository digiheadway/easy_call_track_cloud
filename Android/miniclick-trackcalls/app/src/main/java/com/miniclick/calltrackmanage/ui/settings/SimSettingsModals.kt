package com.miniclick.calltrackmanage.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSimModal(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Selection state
    var sim1Selected by remember { mutableStateOf(uiState.simSelection == "Sim1" || uiState.simSelection == "Both") }
    var sim2Selected by remember { mutableStateOf(uiState.simSelection == "Sim2" || uiState.simSelection == "Both") }
    
    // Setup modal state
    var showSetupModal by remember { mutableStateOf<Int?>(null) } // null, 1, or 2
    
    LaunchedEffect(Unit) {
        viewModel.fetchSimInfo()
    }
    
    // Calculate which SIMs need setup (only if physically present)
    val hasPhysicalSim1 = uiState.availableSims.any { it.slotIndex == 0 }
    val hasPhysicalSim2 = uiState.availableSims.any { it.slotIndex == 1 }
    
    val sim1NeedsSetup = hasPhysicalSim1 && sim1Selected && (uiState.callerPhoneSim1.isBlank() || uiState.sim1SubId == null)
    val sim2NeedsSetup = hasPhysicalSim2 && sim2Selected && (uiState.callerPhoneSim2.isBlank() || uiState.sim2SubId == null)
    val anySIMNeedsSetup = sim1NeedsSetup || sim2NeedsSetup
    
    // Which SIM to setup next
    val nextSimToSetup = when {
        sim1NeedsSetup -> 1
        sim2NeedsSetup -> 2
        else -> null
    }

    // SIM Setup Modal
    if (showSetupModal != null) {
        SimSetupModal(
            simNumber = showSetupModal!!,
            carrierName = uiState.availableSims.find { it.slotIndex == showSetupModal!! - 1 }?.displayName ?: "",
            phoneNumber = if (showSetupModal == 1) uiState.callerPhoneSim1 else uiState.callerPhoneSim2,
            isCalibrated = if (showSetupModal == 1) uiState.sim1SubId != null else uiState.sim2SubId != null,
            calibrationHint = if (showSetupModal == 1) uiState.sim1CalibrationHint else uiState.sim2CalibrationHint,
            viewModel = viewModel,
            onSave = { phone ->
                if (showSetupModal == 1) {
                    viewModel.updateCallerPhoneSim1(phone)
                } else {
                    viewModel.updateCallerPhoneSim2(phone)
                }
                showSetupModal = null
            },
            onDismiss = { showSetupModal = null }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Text(
                "Call Tracking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Select which SIMs to track",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(28.dp))
            
            // SIM Cards
            uiState.availableSims.forEach { sim ->
                val isSelected = if (sim.slotIndex == 0) sim1Selected else sim2Selected
                val simNumber = sim.slotIndex + 1
                val isSetup = if (sim.slotIndex == 0) 
                    uiState.callerPhoneSim1.isNotBlank() && uiState.sim1SubId != null
                else 
                    uiState.callerPhoneSim2.isNotBlank() && uiState.sim2SubId != null
                
                SimCard(
                    simNumber = simNumber,
                    carrierName = sim.displayName,
                    isSelected = isSelected,
                    isSetup = isSetup,
                    onToggle = { checked ->
                        if (sim.slotIndex == 0) sim1Selected = checked else sim2Selected = checked
                    },
                    onSetupClick = { showSetupModal = simNumber }
                )
                
                Spacer(Modifier.height(12.dp))
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Save / Setup button
            val selectedValue = when {
                sim1Selected && sim2Selected -> "Both"
                sim1Selected -> "Sim1"
                sim2Selected -> "Sim2"
                else -> "Off"
            }
            
            // Primary Action: Save or Setup
            Button(
                onClick = {
                    viewModel.updateSimSelection(selectedValue)
                    if (nextSimToSetup != null) {
                        showSetupModal = nextSimToSetup
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    when {
                        selectedValue == "Off" -> "Disable Tracking"
                        nextSimToSetup != null -> "Setup SIM $nextSimToSetup"
                        else -> "Save Selection"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SimCard(
    simNumber: Int,
    carrierName: String,
    isSelected: Boolean,
    isSetup: Boolean,
    onToggle: (Boolean) -> Unit,
    onSetupClick: () -> Unit
) {
    val containerColor = when {
        isSelected && isSetup -> MaterialTheme.colorScheme.primaryContainer
        isSelected && !isSetup -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    
    Surface(
        onClick = { onToggle(!isSelected) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = isSelected,
                onCheckedChange = null, // Handled by Surface onClick
                colors = CheckboxDefaults.colors(
                    checkedColor = if (isSetup) MaterialTheme.colorScheme.primary 
                                  else MaterialTheme.colorScheme.tertiary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            
            Spacer(Modifier.width(12.dp))
            
            // SIM Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "SIM $simNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            isSelected && isSetup -> MaterialTheme.colorScheme.onPrimaryContainer
                            isSelected -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    // Verified badge (only when selected and setup)
                    if (isSelected && isSetup) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    carrierName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Right side: Setup button OR small edit icon
            if (isSelected) {
                if (!isSetup) {
                    // Setup button when not configured
                    FilledTonalButton(
                        onClick = onSetupClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Text("Setup", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    // Small edit icon when configured
                    IconButton(
                        onClick = onSetupClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit SIM $simNumber",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimSetupModal(
    simNumber: Int,
    carrierName: String,
    phoneNumber: String,
    isCalibrated: Boolean,
    calibrationHint: String?,
    viewModel: SettingsViewModel,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var phone by remember { mutableStateOf(phoneNumber) }
    var showCallPicker by remember { mutableStateOf(false) }
    
    // STARTUP OPTIMIZATION: If phone number is detected/updated while modal is open, prefill it
    LaunchedEffect(phoneNumber) {
        if (phone.isBlank() && phoneNumber.isNotBlank()) {
            phone = phoneNumber
        }
    }
    
    if (showCallPicker) {
        CallSelectionModal(
            viewModel = viewModel,
            simName = "SIM $simNumber",
            onCallSelected = { subId, hint ->
                viewModel.setSimCalibration(simNumber - 1, subId, hint)
                showCallPicker = false
            },
            onDismiss = { showCallPicker = false }
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.SimCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "SIM $simNumber Setup",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (carrierName.isNotBlank()) {
                        Text(
                            carrierName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Phone Number
            Text(
                "Phone Number",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = { Text("Enter your SIM $simNumber number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Phone, null) }
            )
            
            Spacer(Modifier.height(28.dp))
            
            // Calibration
            Text(
                "SIM Identification",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Required to correctly identify calls from this SIM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            
            Card(
                onClick = { showCallPicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCalibrated) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isCalibrated) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = if (isCalibrated) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isCalibrated) "Identified ✓" else "Tap to identify",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        if (isCalibrated && calibrationHint != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                calibrationHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Save button
            val canSave = phone.isNotBlank() && isCalibrated
            
            Button(
                onClick = { onSave(phone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = canSave
            ) {
                Text("Done", style = MaterialTheme.typography.titleMedium)
            }
            
            if (!canSave) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Enter phone number and identify SIM to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallSelectionModal(
    viewModel: SettingsViewModel,
    simName: String,
    onCallSelected: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var calls by remember { mutableStateOf<List<SettingsViewModel.VerificationCall>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.fetchRecentSystemCalls { 
            calls = it
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .heightIn(max = 500.dp)
        ) {
            Text(
                "Select a $simName Call",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                "Tap any call you made using $simName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(24.dp))
            
            if (isLoading) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (calls.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.CallMissed,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No calls found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Make a call from $simName first",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(calls) { call ->
                        val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                        val formattedDate = dateFormat.format(Date(call.date))
                        val hasName = !call.contactName.isNullOrBlank()
                        
                        Card(
                            onClick = { 
                                val displayName = if (hasName) call.contactName else call.number
                                onCallSelected(call.subscriptionId, "$displayName • $formattedDate") 
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color = if (hasName) MaterialTheme.colorScheme.primaryContainer 
                                           else MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (hasName) {
                                            // Show first letter of name
                                            Text(
                                                call.contactName!!.first().uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Call,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (hasName) call.contactName!! else call.number,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        if (hasName) "${call.number} • $formattedDate" else formattedDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

// Keep for backward compatibility
@Composable
fun SimSelectionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
               else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                       else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallSimPickerModal(
    number: String,
    availableSims: List<SimInfo>,
    onSimSelected: (Int?) -> Unit,
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                "Call $number",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Pick a SIM to place this call",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(24.dp))
            
            availableSims.forEach { sim ->
                SimPickerLine(
                    sim = sim,
                    onClick = { onSimSelected(sim.subscriptionId) }
                )
                Spacer(Modifier.height(10.dp))
            }
            
            // System Default Option
            SimPickerLine(
                label = "System Default",
                subLabel = "Device preference",
                icon = Icons.Default.Settings,
                onClick = { onSimSelected(null) }
            )
        }
    }
}

@Composable
private fun SimPickerLine(
    sim: SimInfo,
    onClick: () -> Unit
) {
    SimPickerLine(
        label = "SIM ${sim.slotIndex + 1}: ${sim.displayName}",
        subLabel = sim.carrierName,
        icon = Icons.Default.SimCard,
        onClick = onClick
    )
}

@Composable
private fun SimPickerLine(
    label: String,
    subLabel: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
