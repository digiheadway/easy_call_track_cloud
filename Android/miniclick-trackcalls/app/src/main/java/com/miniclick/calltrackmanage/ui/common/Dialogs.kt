package com.miniclick.calltrackmanage.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun NoteDialog(
    title: String,
    initialNote: String,
    label: String,
    buttonText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var note by remember { 
        mutableStateOf(
            TextFieldValue(
                text = initialNote,
                selection = TextRange(initialNote.length)
            )
        ) 
    }
    val focusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(label) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        minLines = 3,
                        maxLines = 5
                    )

                    Button(
                        onClick = { onSave(note.text) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(buttonText)
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LabelPickerDialog(
    currentLabel: String?,
    availableLabels: List<String>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var customLabel by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // Parse current labels (comma-separated)
    val selectedLabels = remember(currentLabel) {
        val list = mutableStateListOf<String>()
        currentLabel?.split(",")?.filter { it.isNotBlank() }?.let { list.addAll(it) }
        list
    }
    
    // Predefined common labels
    val predefinedLabels = listOf("VIP", "Lead", "Customer", "Spam", "Follow-up", "Important", "Personal", "Work")
    
    // Count usage of each label
    val labelUsageCount = remember(availableLabels) {
        availableLabels.flatMap { it.split(",") }.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
    }
    
    // Combine predefined + existing labels, sorted by usage (most used first), then alphabetically
    val allLabels = remember(availableLabels) {
        val allUnique = (availableLabels.flatMap { it.split(",") }.filter { it.isNotBlank() } + predefinedLabels).distinct()
        allUnique.sortedWith(
            compareByDescending<String> { labelUsageCount[it] ?: 0 }
                .thenBy { it }
        )
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 350.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Person Labels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Close", modifier = Modifier.size(20.dp))
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                // Active Labels Display (Chips)
                if (selectedLabels.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedLabels.forEach { label ->
                            InputChip(
                                selected = true,
                                onClick = { selectedLabels.remove(label) },
                                label = { Text(label) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    "Quick Select",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Scrollable label list
                LazyColumn(
                    modifier = Modifier.heightIn(max = 250.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allLabels.size) { index ->
                        val label = allLabels[index]
                        val isSelected = selectedLabels.contains(label)
                        val count = labelUsageCount[label] ?: 0
                        
                        Surface(
                            onClick = { 
                                if (isSelected) selectedLabels.remove(label) 
                                else selectedLabels.add(label)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Label, 
                                    null, 
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    label, 
                                    modifier = Modifier.weight(1f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (count > 0) {
                                    Text(
                                        "($count)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { 
                                        if (it) selectedLabels.add(label) 
                                        else selectedLabels.remove(label)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    item {
                        if (showCustomInput) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customLabel,
                                    onValueChange = { customLabel = it },
                                    placeholder = { Text("Custom label...") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { 
                                        if (customLabel.isNotBlank()) {
                                            val trimmed = customLabel.trim()
                                            if (!selectedLabels.contains(trimmed)) {
                                                selectedLabels.add(trimmed)
                                            }
                                            customLabel = ""
                                            showCustomInput = false
                                        }
                                    },
                                    enabled = customLabel.isNotBlank(),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Add")
                                }
                            }
                            
                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                            }
                        } else {
                            TextButton(
                                onClick = { showCustomInput = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("New Custom Label")
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { onSave(""); onDismiss() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear All")
                    }
                    
                    Button(
                        onClick = { 
                            onSave(selectedLabels.joinToString(","))
                            onDismiss() 
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
