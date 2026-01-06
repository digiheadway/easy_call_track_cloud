package com.example.callyzer4.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.callyzer4.data.CallFilter
import com.example.callyzer4.data.CallType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EnhancedFilterDialog(
    isOpen: Boolean,
    currentFilter: CallFilter,
    onDismiss: () -> Unit,
    onApplyFilter: (CallFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isOpen) {
        var filter by remember { mutableStateOf(currentFilter) }
        var searchQuery by remember { mutableStateOf(currentFilter.searchQuery) }
        var selectedCallTypes by remember { mutableStateOf(currentFilter.callTypes) }
        var startDate by remember { mutableStateOf(currentFilter.dateRange?.startDate) }
        var endDate by remember { mutableStateOf(currentFilter.dateRange?.endDate) }
        var minDuration by remember { mutableStateOf(currentFilter.minDuration?.toString() ?: "") }
        var maxDuration by remember { mutableStateOf(currentFilter.maxDuration?.toString() ?: "") }
        
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter Call History",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                    
                    // Search query
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { 
                                Text(
                                    text = "Search by name or number",
                                    color = Color(0xFF666666)
                                )
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Default.Search, 
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1976D2),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    
                    // Call types section
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Call Types",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Column(
                                modifier = Modifier.selectableGroup()
                            ) {
                                CallType.values().forEach { callType ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = callType in selectedCallTypes,
                                                onClick = {
                                                    selectedCallTypes = if (callType in selectedCallTypes) {
                                                        selectedCallTypes - callType
                                                    } else {
                                                        selectedCallTypes + callType
                                                    }
                                                },
                                                role = Role.Checkbox
                                            )
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = callType in selectedCallTypes,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = Color(0xFF1976D2),
                                                uncheckedColor = Color(0xFF666666)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = when (callType) {
                                                CallType.INCOMING -> "Incoming"
                                                CallType.OUTGOING -> "Outgoing"
                                                CallType.MISSED -> "Missed"
                                            },
                                            fontSize = 14.sp,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Duration filter section
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Call Duration (seconds)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = minDuration,
                                    onValueChange = { minDuration = it },
                                    label = { Text("Min Duration") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF1976D2),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                
                                OutlinedTextField(
                                    value = maxDuration,
                                    onValueChange = { maxDuration = it },
                                    label = { Text("Max Duration") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF1976D2),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Reset filter
                                filter = CallFilter()
                                searchQuery = ""
                                selectedCallTypes = CallType.values().toSet()
                                startDate = null
                                endDate = null
                                minDuration = ""
                                maxDuration = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF666666)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Reset",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Button(
                            onClick = {
                                val dateRange = if (startDate != null && endDate != null) {
                                    com.example.callyzer4.data.DateRange(startDate!!, endDate!!)
                                } else null
                                
                                filter = filter.copy(
                                    searchQuery = searchQuery,
                                    callTypes = selectedCallTypes,
                                    dateRange = dateRange,
                                    minDuration = minDuration.toLongOrNull(),
                                    maxDuration = maxDuration.toLongOrNull()
                                )
                                onApplyFilter(filter)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Apply Filter",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
