package com.clicktoearn.linkbox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clicktoearn.linkbox.data.local.AssetEntity
import com.clicktoearn.linkbox.data.local.AssetType
import com.clicktoearn.linkbox.data.local.SharingLinkEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareMenuSheet(
    asset: AssetEntity,
    existingLinks: List<SharingLinkEntity>,
    onDismiss: () -> Unit,
    onUpdateAsset: (AssetEntity) -> Unit,
    onCreateLink: (name: String, description: String, expiryDays: Int?) -> Unit,
    onUpdateLink: (SharingLinkEntity) -> Unit,
    onDeleteLink: (SharingLinkEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Asset Settings State
    var sharingEnabled by remember(asset) { mutableStateOf(asset.sharingEnabled) }
    var allowSaveCopy by remember(asset) { mutableStateOf(asset.allowSaveCopy) }

    var shareOutsideApp by remember(asset) { mutableStateOf(asset.shareOutsideApp) }
    var assetDescription by remember(asset) { mutableStateOf(asset.description) }
    // allowScreenCapture removed - always false (screenshots always blocked)
    var exposeUrl by remember(asset) { mutableStateOf(asset.exposeUrl) }
    var chargeEveryTime by remember(asset) { mutableStateOf(asset.chargeEveryTime) }
    
    // We store point cost as an Int in the state, initialized from asset
    var pointCost by remember(asset) { mutableIntStateOf(asset.pointCost) }

    var showDescriptionModal by remember { mutableStateOf(false) }
    var showPrivacyModal by remember { mutableStateOf(false) }
    var showAccessCostModal by remember { mutableStateOf(false) }
    var linkToRename by remember { mutableStateOf<SharingLinkEntity?>(null) }
    var linkToSetExpiry by remember { mutableStateOf<SharingLinkEntity?>(null) }

    // Helper function to save asset settings
    fun saveAssetSettings() {
        // Screenshots are always blocked (allowScreenCapture = false)
        val effectiveShareOutsideApp = if (allowSaveCopy) true else shareOutsideApp
        val effectiveExposeUrl = if (allowSaveCopy) true else exposeUrl
        
        val updatedAsset = asset.copy(
            sharingEnabled = sharingEnabled,
            allowSaveCopy = allowSaveCopy,
            shareOutsideApp = effectiveShareOutsideApp,
            description = assetDescription,
            pointCost = pointCost,
            allowScreenCapture = false,  // Always block screenshots
            exposeUrl = effectiveExposeUrl,
            chargeEveryTime = chargeEveryTime
        )

        // Only update if something actually changed
        if (updatedAsset != asset) {
            onUpdateAsset(updatedAsset)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            saveAssetSettings()
            onDismiss()
        },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // Header
                item {
                    val icon = when (asset.type) {
                        AssetType.FILE -> Icons.Filled.Description
                        AssetType.FOLDER -> Icons.Filled.Folder
                        AssetType.LINK -> Icons.Filled.Link
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Share ${asset.name}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Description Strip
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDescriptionModal = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (assetDescription.isEmpty()) {
                                Text(
                                    text = "Add Description",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = assetDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { showDescriptionModal = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.alpha(0.5f))
                }

                // Privacy Settings Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyModal = true }
                            .padding(top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Privacy Settings",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showPrivacyModal = true }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Manage", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                 // Access Cost Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccessCostModal = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Access Cost",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (pointCost > 0) "$pointCost Points • ${if (chargeEveryTime) "Everytime" else "One-time"}" else "Free Access",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { showAccessCostModal = true }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Set up", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Access via Links Toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sharingEnabled = !sharingEnabled }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Access Via Links",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = sharingEnabled,
                            onCheckedChange = { sharingEnabled = it },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp).alpha(0.5f))
                }

                // Section: Sharing Links List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Links",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${existingLinks.size} link${if (existingLinks.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (existingLinks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No links created yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(existingLinks) { link ->
                        SharingLinkCard(
                            link = link,
                            pointCost = pointCost,
                            showDetails = false,
                            onRename = { linkToRename = it },
                            onSetExpiry = { linkToSetExpiry = it },
                            onToggleAccess = { 
                                val newStatus = if (it.status == "ACTIVE") "INACTIVE" else "ACTIVE"
                                onUpdateLink(it.copy(status = newStatus))
                            },
                            onDelete = { onDeleteLink(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    Button(
                        onClick = {
                            val nextName = if (existingLinks.isEmpty()) asset.name else "${asset.name} #${existingLinks.size + 1}"
                            onCreateLink(nextName, "", null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = sharingEnabled
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Link")
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }

    if (showDescriptionModal) {
        ModalBottomSheet(onDismissRequest = { showDescriptionModal = false }) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
                    .imePadding()
            ) {
                Text("Asset Description", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = assetDescription,
                    onValueChange = { assetDescription = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showDescriptionModal = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Description")
                }
            }
        }
    }

    if (showPrivacyModal) {
        PrivacySettingsSheet(
            allowSaveCopy = allowSaveCopy,
            onAllowSaveCopyChange = { allowSaveCopy = it },
            allowFurtherSharing = shareOutsideApp,
            onAllowFurtherSharingChange = { shareOutsideApp = it },
            exposeUrl = exposeUrl,
            onExposeUrlChange = { exposeUrl = it },
            isLink = asset.type == AssetType.LINK,
            onDismiss = { showPrivacyModal = false }
        )
    }

    if (showAccessCostModal) {
        AccessCostSheet(
            currentCost = pointCost,
            onCostChange = { pointCost = it },
            isChargeEveryTime = chargeEveryTime,
            onChargeFrequencyChange = { chargeEveryTime = it },
            onDismiss = { showAccessCostModal = false }
        )
    }

    // Link rename sheet
    linkToRename?.let { link ->
        RenameLinkSheet(
            link = link,
            onSave = { updatedLink ->
                onUpdateLink(updatedLink)
                linkToRename = null
            },
            onDismiss = { linkToRename = null }
        )
    }

    // Link expiry sheet
    linkToSetExpiry?.let { link ->
        ExpiryDateSheet(
            link = link,
            onSave = { updatedLink ->
                onUpdateLink(updatedLink)
                linkToSetExpiry = null
            },
            onDismiss = { linkToSetExpiry = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacySettingsSheet(
    allowSaveCopy: Boolean,
    onAllowSaveCopyChange: (Boolean) -> Unit,
    allowFurtherSharing: Boolean,
    onAllowFurtherSharingChange: (Boolean) -> Unit,
    exposeUrl: Boolean,
    onExposeUrlChange: (Boolean) -> Unit,
    isLink: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Text("Privacy Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            
            SettingsToggleItem(
                title = "Allow Copying",
                description = "Users can save a copy to their 'My Files'. Disables all restrictions below.",
                checked = allowSaveCopy,
                onCheckedChange = onAllowSaveCopyChange
            )
            
            if (!allowSaveCopy) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Text("Security Restrictions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                // Screenshot blocking is now always enabled - toggle removed

                SettingsToggleItem(
                    title = "Restrict Link Forwarding",
                    description = "Stop users from sharing this link with others.",
                    checked = !allowFurtherSharing,
                    onCheckedChange = { onAllowFurtherSharingChange(!it) }
                )

                if (isLink) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Text("Link Privacy", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingsToggleItem(
                        title = "Hide Destination URL",
                        description = "Viewer cannot see or copy the actual destination URL.",
                        checked = !exposeUrl,
                        onCheckedChange = { onExposeUrlChange(!it) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessCostSheet(
    currentCost: Int,
    onCostChange: (Int) -> Unit,
    isChargeEveryTime: Boolean,
    onChargeFrequencyChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var costText by remember { mutableStateOf(currentCost.toString()) }
    
    ModalBottomSheet(
        onDismissRequest = {
            onCostChange(costText.toIntOrNull() ?: 0)
            onDismiss()
        },
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Text("Access Cost", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                "Earn points by sharing links. Use points to remove ads or access premium content.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = costText,
                onValueChange = { 
                    if (it.all { char -> char.isDigit() }) costText = it
                },
                label = { Text("Point Cost") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Icon(Icons.Filled.Stars, null, tint = MaterialTheme.colorScheme.primary) },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Billing Frequency (Coins)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FrequencyOption(
                    title = "One-time charge",
                    description = "Users pay coins once and get permanent access.",
                    selected = !isChargeEveryTime,
                    onClick = { onChargeFrequencyChange(false) }
                )
                FrequencyOption(
                    title = "Every-time charge",
                    description = "Users pay coins every time they open the content.",
                    selected = isChargeEveryTime,
                    onClick = { onChargeFrequencyChange(true) }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    onCostChange(costText.toIntOrNull() ?: 0)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun FrequencyOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.85f)
        )
    }
}
