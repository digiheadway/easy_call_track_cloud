package com.example.callyzer4.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.callyzer4.data.CallFilter
import com.example.callyzer4.data.CallType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FilterDialog(
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
        var showDatePicker by remember { mutableStateOf(false) }
        var startDate by remember { mutableStateOf(currentFilter.dateRange?.startDate) }
        var endDate by remember { mutableStateOf(currentFilter.dateRange?.endDate) }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Filter Call History") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Search query
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by name or number") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Call types
                    Text(
                        text = "Call Types",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
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
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = callType in selectedCallTypes,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (callType) {
                                        CallType.INCOMING -> "Incoming"
                                        CallType.OUTGOING -> "Outgoing"
                                        CallType.MISSED -> "Missed"
                                    }
                                )
                            }
                        }
                    }
                    
                    // Date range
                    Text(
                        text = "Date Range",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = startDate?.let { 
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                                } ?: "Start Date"
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = endDate?.let { 
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                                } ?: "End Date"
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val dateRange = if (startDate != null && endDate != null) {
                            com.example.callyzer4.data.DateRange(startDate!!, endDate!!)
                        } else null
                        
                        filter = filter.copy(
                            searchQuery = searchQuery,
                            callTypes = selectedCallTypes,
                            dateRange = dateRange
                        )
                        onApplyFilter(filter)
                        onDismiss()
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            modifier = modifier
        )
    }
}
