package com.calltracker.manager.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    var selectedSim by remember { mutableStateOf(uiState.simSelection) }
    var sim1Phone by remember { mutableStateOf(uiState.callerPhoneSim1) }
    var sim2Phone by remember { mutableStateOf(uiState.callerPhoneSim2) }
    var showDropdown by remember { mutableStateOf(false) }
    
    // Calibration State
    var showCalibrationModal by remember { mutableStateOf(false) }
    var calibrationSimIndex by remember { mutableStateOf(-1) } // 0 for Sim1, 1 for Sim2
    
    LaunchedEffect(Unit) {
        viewModel.fetchSimInfo()
    }

    if (showCalibrationModal) {
        CallSelectionModal(
            viewModel = viewModel,
            simName = if (calibrationSimIndex == 0) "SIM 1" else "SIM 2",
            onCallSelected = { subId, hint ->
                viewModel.setSimCalibration(calibrationSimIndex, subId, hint)
                showCalibrationModal = false
            },
            onDismiss = { showCalibrationModal = false }
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Call Tracking Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(24.dp))
            
            // SIM Selection Dropdown
            Text("Select SIM to Track", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedCard(
                    onClick = { showDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val label = when(selectedSim) {
                            "Off" -> "Off"
                            "Both" -> "Both SIMs"
                            else -> selectedSim.replace("Sim", "SIM ")
                        }
                        Text(label, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    // 1. Individual Sims
                    uiState.availableSims.forEach { sim ->
                        val simValue = "Sim${sim.slotIndex + 1}"
                        DropdownMenuItem(
                            text = { Text("SIM ${sim.slotIndex + 1} (${sim.displayName})") },
                            onClick = { selectedSim = simValue; showDropdown = false },
                            leadingIcon = { Icon(Icons.Default.SimCard, null) }
                        )
                    }
                    
                    // 2. Both
                    DropdownMenuItem(
                        text = { Text("Both SIMs") },
                        onClick = { selectedSim = "Both"; showDropdown = false },
                        leadingIcon = { Icon(Icons.Default.DoneAll, null) }
                    )
                    
                    // 3. Off
                    DropdownMenuItem(
                        text = { Text("Off") },
                        onClick = { selectedSim = "Off"; showDropdown = false },
                        leadingIcon = { Icon(Icons.Default.Block, null) }
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Conditional Phone Inputs & Calibration
            if (selectedSim == "Sim1" || selectedSim == "Both") {
                Text("SIM 1 SETUP & CALIBRATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))
                
                // Calibration Status & Button
                val isCalibrated = uiState.sim1SubId != null
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCalibrated) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isCalibrated) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isCalibrated) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isCalibrated) "SIM 1 Identified" else "SIM 1 Not Identified",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Text(
                            text = if (isCalibrated) "Successfully calibrated for tracking." else "We need to know which internal ID belongs to SIM 1. Please select your own call in the next step.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 32.dp)
                        )
                        
                        if (isCalibrated && uiState.sim1CalibrationHint != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 8.dp, start = 32.dp)
                            ) {
                                Text(
                                    text = "Selected: ${uiState.sim1CalibrationHint}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        if (!isCalibrated) {
                            Text(
                                "Warning: Wrong call log can result in wrong calls detection.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, start = 32.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        FilledTonalButton(
                            onClick = { 
                                calibrationSimIndex = 0
                                showCalibrationModal = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isCalibrated) "Recalibrate SIM 1" else "Identify SIM 1 Now")
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = sim1Phone,
                    onValueChange = { sim1Phone = it },
                    label = { Text("SIM 1 Phone Number") },
                    placeholder = { Text("Enter SIM 1 Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )
                
                Spacer(Modifier.height(24.dp))
            }
            
            if (selectedSim == "Sim2" || selectedSim == "Both") {
                Text("SIM 2 SETUP & CALIBRATION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(12.dp))
                
                // Calibration Status & Button
                val isCalibrated = uiState.sim2SubId != null
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCalibrated) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isCalibrated) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isCalibrated) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isCalibrated) "SIM 2 Identified" else "SIM 2 Not Identified",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Text(
                            text = if (isCalibrated) "Successfully calibrated for tracking." else "We need to know which internal ID belongs to SIM 2. Please select your own call in the next step.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 32.dp)
                        )
                        
                        if (isCalibrated && uiState.sim2CalibrationHint != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(top = 8.dp, start = 32.dp)
                            ) {
                                Text(
                                    text = "Selected: ${uiState.sim2CalibrationHint}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        if (!isCalibrated) {
                            Text(
                                "Warning: Wrong call log can result in wrong calls detection.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, start = 32.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        FilledTonalButton(
                            onClick = { 
                                calibrationSimIndex = 1
                                showCalibrationModal = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isCalibrated) "Recalibrate SIM 2" else "Identify SIM 2 Now")
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = sim2Phone,
                    onValueChange = { sim2Phone = it },
                    label = { Text("SIM 2 Phone Number") },
                    placeholder = { Text("Enter SIM 2 Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, null) }
                )
                
                Spacer(Modifier.height(24.dp))
            }
            
            val isSaveEnabled = when(selectedSim) {
                "Off" -> true
                "Sim1" -> sim1Phone.isNotBlank() && uiState.sim1SubId != null
                "Sim2" -> sim2Phone.isNotBlank() && uiState.sim2SubId != null
                "Both" -> sim1Phone.isNotBlank() && sim2Phone.isNotBlank() && uiState.sim1SubId != null && uiState.sim2SubId != null
                else -> false
            }
            
            Button(
                onClick = {
                    viewModel.updateSimSelection(selectedSim)
                    viewModel.updateCallerPhoneSim1(sim1Phone)
                    viewModel.updateCallerPhoneSim2(sim2Phone)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isSaveEnabled,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(if (isSaveEnabled) "Save Tracking Settings" else "Please Identify SIM Call Log First")
            }
            
            Spacer(Modifier.height(48.dp))
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
                .padding(horizontal = 16.dp)
                .heightIn(max = 600.dp)
        ) {
            Text(
                text = "Select a call from $simName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap a call below that you made/received using $simName to calibrate.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(16.dp))
            
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (calls.isEmpty()) {
                 Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No recent calls found in system log")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(calls) { call ->
                        ListItem(
                            headlineContent = { Text(call.number, fontWeight = FontWeight.Bold) },
                            supportingContent = { 
                                val date = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(call.date))
                                Text(date) 
                            },
                            modifier = Modifier.clickable { 
                                val date = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(call.date))
                                onCallSelected(call.subscriptionId, "${call.number} at $date") 
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
             Spacer(Modifier.height(48.dp))
        }
    }
}
