package com.miniclick.calltrackmanage.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.*

enum class HealthStatus {
    GOOD, WARNING, ERROR
}

data class HealthItem(
    val title: String,
    val status: HealthStatus,
    val detail: String,
    val icon: ImageVector
)

@Composable
fun SystemHealthDashboard(
    uiState: SettingsUiState,
    onPermissionClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    // Calculate health items
    val healthItems = remember(uiState) {
        buildList {
            // 1. Permissions Status
            val grantedCount = uiState.permissions.count { it.isGranted }
            val totalCount = uiState.permissions.size
            val permStatus = when {
                grantedCount == totalCount -> HealthStatus.GOOD
                grantedCount >= totalCount - 1 -> HealthStatus.WARNING
                else -> HealthStatus.ERROR
            }
            add(HealthItem(
                "Permissions",
                permStatus,
                "$grantedCount/$totalCount granted",
                Icons.Default.Security
            ))
            
            // 2. Sync Status
            val syncStatus = when {
                uiState.pairingCode.isEmpty() -> HealthStatus.WARNING
                uiState.isSyncing -> HealthStatus.GOOD
                uiState.lastSyncTime > 0 && (System.currentTimeMillis() - uiState.lastSyncTime) < 24 * 60 * 60 * 1000 -> HealthStatus.GOOD
                uiState.lastSyncTime > 0 -> HealthStatus.WARNING
                else -> HealthStatus.ERROR
            }
            val syncDetail = when {
                uiState.pairingCode.isEmpty() -> "Not linked"
                uiState.isSyncing -> "Syncing now..."
                uiState.lastSyncTime > 0 -> {
                    val diff = System.currentTimeMillis() - uiState.lastSyncTime
                    val hours = diff / (1000 * 60 * 60)
                    if (hours < 1) "Recent" else "${hours}h ago"
                }
                else -> "Never synced"
            }
            add(HealthItem(
                "Cloud Sync",
                syncStatus,
                syncDetail,
                Icons.Default.CloudSync
            ))
            
            // 3. Tracking Status
            val trackingStatus = when {
                uiState.simSelection == "Off" -> HealthStatus.WARNING
                uiState.sim1SubId != null || uiState.sim2SubId != null -> HealthStatus.GOOD
                else -> HealthStatus.WARNING
            }
            val trackingDetail = when {
                uiState.simSelection == "Off" -> "Disabled"
                uiState.simSelection == "Both" -> "Dual SIM"
                uiState.simSelection.startsWith("Sim") -> uiState.simSelection.replace("Sim", "SIM ")
                else -> "Not configured"
            }
            add(HealthItem(
                "Call Tracking",
                trackingStatus,
                trackingDetail,
                Icons.Default.PhoneInTalk
            ))
            
            // 4. Overlay Permission (for Caller ID)
            val overlayStatus = if (uiState.isOverlayPermissionGranted) HealthStatus.GOOD else HealthStatus.WARNING
            add(HealthItem(
                "Caller ID",
                overlayStatus,
                if (uiState.callerIdEnabled && uiState.isOverlayPermissionGranted) "Active" 
                else if (!uiState.isOverlayPermissionGranted) "No permission"
                else "Disabled",
                Icons.Default.ContactPhone
            ))
        }
    }
    
    val overallStatus = remember(healthItems) {
        when {
            healthItems.any { it.status == HealthStatus.ERROR } -> HealthStatus.ERROR
            healthItems.any { it.status == HealthStatus.WARNING } -> HealthStatus.WARNING
            else -> HealthStatus.GOOD
        }
    }
    
    val statusColor = when (overallStatus) {
        HealthStatus.GOOD -> Color(0xFF10B981)
        HealthStatus.WARNING -> Color(0xFFF59E0B)
        HealthStatus.ERROR -> Color(0xFFEF4444)
    }
    
    val statusText = when (overallStatus) {
        HealthStatus.GOOD -> "All Systems Operational"
        HealthStatus.WARNING -> "Needs Attention"
        HealthStatus.ERROR -> "Action Required"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            when (overallStatus) {
                                HealthStatus.GOOD -> Icons.Default.CheckCircle
                                HealthStatus.WARNING -> Icons.Default.Warning
                                HealthStatus.ERROR -> Icons.Default.Error
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "System Health",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Health Items Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                healthItems.take(2).forEach { item ->
                    HealthStatusChip(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (item.title) {
                                "Permissions" -> onPermissionClick()
                                "Cloud Sync" -> onSyncClick()
                            }
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                healthItems.drop(2).forEach { item ->
                    HealthStatusChip(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    )
                }
            }
        }
    }
}

@Composable
fun HealthStatusChip(
    item: HealthItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val statusColor = when (item.status) {
        HealthStatus.GOOD -> Color(0xFF10B981)
        HealthStatus.WARNING -> Color(0xFFF59E0B)
        HealthStatus.ERROR -> Color(0xFFEF4444)
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    item.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    item.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun SettingsSection(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
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
    icon: ImageVector,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = 0.15f),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun LifecycleEventEffect(
    event: Lifecycle.Event,
    onEvent: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == event) {
                currentOnEvent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun SyncCloudCard(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onShowSyncQueue: () -> Unit,
    onSetupCloud: () -> Unit,
    onShowResetConfirm: () -> Unit
) {
    val isPlanExpired = remember(uiState.planExpiryDate) {
        uiState.planExpiryDate?.let { expiry ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expiryDate = sdf.parse(expiry)
                expiryDate != null && expiryDate.before(Date())
            } catch (e: Exception) { false }
        } ?: false
    }

    val isStorageFull = remember(uiState.allowedStorageGb, uiState.storageUsedBytes) {
        if (uiState.allowedStorageGb <= 0f) false
        else {
            val usedGb = uiState.storageUsedBytes.toDouble() / (1024 * 1024 * 1024)
            usedGb >= uiState.allowedStorageGb
        }
    }

    Column {
        if (isPlanExpired || isStorageFull) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = when {
                            isPlanExpired && isStorageFull -> "Plan expired and storage is full. Syncing is disabled."
                            isPlanExpired -> "Your plan has expired. Please renew to continue syncing."
                            isStorageFull -> "Organization storage is full. Recordings will not be synced."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Sync to Cloud",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val statusText = if (uiState.pairingCode.isNotEmpty()) "Linked to : ${uiState.pairingCode}" else "linked to : Not linked yet"
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    
                    if (uiState.pairingCode.isNotEmpty()) {
                        IconButton(onClick = onShowSyncQueue) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Show Sync Queue",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                val descriptionText = if (uiState.pairingCode.isNotEmpty()) {
                    val enabledFeatures = mutableListOf<String>()
                    if (uiState.callTrackEnabled) enabledFeatures.add("Calls")
                    if (uiState.callRecordEnabled) enabledFeatures.add("Recordings")
                    enabledFeatures.add("Notes")
                    
                    val featuresStr = if (enabledFeatures.size > 1) {
                        val last = enabledFeatures.removeAt(enabledFeatures.size - 1)
                        "Syncing ${enabledFeatures.joinToString(", ")} and $last"
                    } else {
                        "Syncing ${enabledFeatures.firstOrNull() ?: "nothing"}"
                    }
                    
                    val statusSuffix = when {
                        !uiState.callTrackEnabled && !uiState.callRecordEnabled -> " (Tracking Disabled)"
                        !uiState.callTrackEnabled -> " (Call Tracking Off)"
                        !uiState.callRecordEnabled -> " (Recordings Off)"
                        else -> ""
                    }
                    featuresStr + statusSuffix
                } else "Sync calls, recordings, note to cloud to access from any device."
                
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { 
                            if (uiState.pairingCode.isNotEmpty()) {
                                viewModel.syncCallManually()
                            } else {
                                onSetupCloud()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (uiState.pairingCode.isNotEmpty()) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("Syncing...")
                            } else {
                                Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sync Now")
                            }
                        } else {
                            Text("Setup Cloud Sync")
                        }
                    }

                    if (uiState.pairingCode.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        var showCardMenu by remember { mutableStateOf(false) }
                        Box {
                            FilledTonalIconButton(
                                onClick = { showCardMenu = true },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Settings, "Sync Settings")
                            }
                            DropdownMenu(
                                expanded = showCardMenu,
                                onDismissRequest = { showCardMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Reset Sync Data") },
                                    onClick = { 
                                        showCardMenu = false
                                        onShowResetConfirm()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Restore, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Leave Organisation") },
                                    onClick = { 
                                        showCardMenu = false
                                        viewModel.leaveOrganisation()
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null) }
                                )
                            }
                        }
                    }
                }

                // Sync Progress & Status
                if (uiState.pairingCode.isNotEmpty()) {
                    if (uiState.isSyncing) {
                        // Show progress bar when syncing
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                            
                            uiState.lastSyncStats?.let { stats ->
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stats,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // Show static status when not syncing
                        uiState.lastSyncStats?.let { stats ->
                            Text(
                                text = stats,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

fun showDatePicker(context: android.content.Context, initialDate: Long, onDateSelected: (Long) -> Unit) {
    val calendar = Calendar.getInstance()
    if (initialDate > 0) calendar.timeInMillis = initialDate
    
    val dialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val result = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            onDateSelected(result)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    dialog.show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLookupModal(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var urlInput by remember { mutableStateOf(uiState.customLookupUrl.ifEmpty { "" }) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Custom Lookup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Configure an external URL to fetch additional data for phone numbers during calls and in history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Enable Toggles
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Enable Custom Lookup",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.customLookupEnabled,
                    onCheckedChange = { viewModel.updateCustomLookupEnabled(it) }
                )
            }
            
            Spacer(Modifier.height(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Enable for Caller ID",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = uiState.customLookupCallerIdEnabled,
                        onCheckedChange = { viewModel.updateCustomLookupCallerIdEnabled(it) }
                    )
                }
                Text(
                    text = "Shows custom data in the caller ID overlay. Clicking the overlay will open detailed lookup if no other local data exists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Lookup URL template") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://example.com/api?phone={phone}") },
                supportingText = { Text("Use {phone} as placeholder for the number") },
                singleLine = false,
                maxLines = 3
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.updateCustomLookupUrl(urlInput)
                        viewModel.fetchCustomLookup(urlInput)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = urlInput.isNotBlank() && !uiState.isFetchingCustomLookup,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isFetchingCustomLookup) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Fetch")
                }
                
                OutlinedButton(
                    onClick = { viewModel.updateCustomLookupUrl(urlInput) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            }

            if (uiState.customLookupResponse != null) {
                Spacer(Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !uiState.isRawView,
                            onClick = { viewModel.toggleRawView(false) },
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                        ) {
                            Text("Formatted", fontSize = 12.sp)
                        }
                        SegmentedButton(
                            selected = uiState.isRawView,
                            onClick = { viewModel.toggleRawView(true) },
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        ) {
                            Text("Raw", fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                if (uiState.isRawView) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.customLookupResponse ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                } else {
                    com.miniclick.calltrackmanage.ui.common.JsonTableView(json = uiState.customLookupResponse ?: "{}")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppSelectionModal(
    currentSelection: String,
    availableApps: List<AppInfo>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Default WhatsApp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Choose which app to open when tapping WhatsApp links",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(24.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Always Ask option
                SelectionChip(
                    label = "Always Ask",
                    isSelected = currentSelection == "Always Ask",
                    onClick = { onSelect("Always Ask") }
                )
                
                // Available WhatsApp apps
                availableApps.forEach { app ->
                    SelectionChip(
                        label = app.label,
                        isSelected = currentSelection == app.packageName,
                        onClick = { onSelect(app.packageName) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionModal(
    currentTheme: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "App Theme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Choose your preferred color scheme",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(24.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectionChip(
                    label = "System",
                    subtitle = "Follow device theme",
                    icon = Icons.Default.SettingsSystemDaydream,
                    isSelected = currentTheme == "System",
                    onClick = { onSelect("System") }
                )
                SelectionChip(
                    label = "Light",
                    subtitle = "Always use light mode",
                    icon = Icons.Default.LightMode,
                    isSelected = currentTheme == "Light",
                    onClick = { onSelect("Light") }
                )
                SelectionChip(
                    label = "Dark",
                    subtitle = "Always use dark mode",
                    icon = Icons.Default.DarkMode,
                    isSelected = currentTheme == "Dark",
                    onClick = { onSelect("Dark") }
                )
            }
        }
    }
}

@Composable
fun SelectionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                           else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
