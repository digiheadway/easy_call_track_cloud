package com.clicktoearn.linkbox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clicktoearn.linkbox.data.local.SharingLinkEntity
import com.clicktoearn.linkbox.utils.copyToClipboard
import com.clicktoearn.linkbox.utils.generateShareUrl
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable sharing link card with three-dot menu.
 * Used in both ShareMenuSheet and LinksScreen for consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharingLinkCard(
    link: SharingLinkEntity,
    pointCost: Int = 0,
    assetName: String? = null,
    showDetails: Boolean = false, // If true, shows more details like stats
    onAssetClick: (() -> Unit)? = null,
    onRename: (SharingLinkEntity) -> Unit,
    onSetExpiry: (SharingLinkEntity) -> Unit,
    onToggleAccess: (SharingLinkEntity) -> Unit,
    onDelete: (SharingLinkEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (showDetails) 0.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (showDetails) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (showDetails) 16.dp else 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                Surface(
                    color = if (link.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.size(if (showDetails) 40.dp else 36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = null,
                            tint = if (link.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (showDetails) 24.dp else 20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.name,
                        style = if (showDetails) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (showDetails) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showDetails && assetName != null) {
                        // Show linked asset name instead of token
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = assetName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (onAssetClick != null) {
                                    Modifier.clickable { onAssetClick() }
                                } else Modifier
                            )
                        }
                    } else if (!showDetails) {
                        Text(
                            text = "${link.views} views • ${link.users} users",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val shareUrl = generateShareUrl(link.token)
                        copyToClipboard(context, shareUrl, "Share Link")
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy Link",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy Link") },
                            onClick = {
                                val shareUrl = generateShareUrl(link.token)
                                copyToClipboard(context, shareUrl, "Share Link")
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename Link") },
                            onClick = {
                                onRename(link)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null) }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (link.expiry != null) "Remove Expiry Date" else "Set Expiry Date"
                                )
                            },
                            onClick = {
                                onSetExpiry(link)
                                showMenu = false
                            },
                            leadingIcon = { 
                                Icon(
                                    if (link.expiry != null) Icons.Filled.EventBusy else Icons.Filled.Event,
                                    null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(if (link.isActive) "Turn Off Access" else "Turn On Access")
                            },
                            onClick = {
                                onToggleAccess(link)
                                showMenu = false
                            },
                            leadingIcon = { 
                                Icon(
                                    if (link.isActive) Icons.Filled.Block else Icons.Filled.CheckCircle,
                                    null
                                )
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDelete(link)
                                showMenu = false
                            },
                            leadingIcon = { 
                                Icon(
                                    Icons.Filled.Delete, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }

            // Detailed view with stats
            if (showDetails) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${link.views} views",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Icon(
                            Icons.Filled.Group,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${link.users} users",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Expiry info
                        if (link.expiry != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val expiryDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(link.expiry))
                            Text(
                                "Expires $expiryDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "$pointCost PTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}



/**
 * Sheet for setting expiry date on a link
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryDateSheet(
    link: SharingLinkEntity,
    onSave: (SharingLinkEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedDateMillis by remember { mutableStateOf(link.expiry) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= System.currentTimeMillis() - 24 * 60 * 60 * 1000 // Allow today
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // Set to end of the day (23:59:59) so it includes the selected day
                    val date = datePickerState.selectedDateMillis
                    if (date != null) {
                        // DatePicker returns UTC midnight. Add nearly 24 hours.
                        selectedDateMillis = date + (24 * 60 * 60 * 1000) - 1
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
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = if (link.expiry != null) "Update Expiry Date" else "Set Expiry Date",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The link will become inactive after the selected date.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            val dateText = selectedDateMillis?.let {
                SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(it))
            } ?: ""
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { },
                    label = { Text("Expiry Date") },
                    placeholder = { Text("Select Date") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        Icon(Icons.Filled.CalendarMonth, null)
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (link.expiry != null) {
                    OutlinedButton(
                        onClick = {
                            onSave(link.copy(expiry = null))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Remove Expiry")
                    }
                } else {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }

                Button(
                    onClick = {
                        if (selectedDateMillis != null) {
                            onSave(link.copy(expiry = selectedDateMillis))
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedDateMillis != null
                ) {
                    Text("Set Expiry")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Sheet for renaming a link
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameLinkSheet(
    link: SharingLinkEntity,
    onSave: (SharingLinkEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var newName by remember { mutableStateOf(link.name) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Rename Link",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Link Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onSave(link.copy(name = newName))
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = newName.isNotBlank()
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
