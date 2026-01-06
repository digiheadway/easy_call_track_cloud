package com.clicktoearn.linkbox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.clicktoearn.linkbox.data.entity.EntityItem
import com.clicktoearn.linkbox.data.entity.EntityType
import com.clicktoearn.linkbox.data.entity.SharingEntity
import com.clicktoearn.linkbox.data.entity.PrivacyType
import com.clicktoearn.linkbox.ui.theme.*
import com.clicktoearn.linkbox.ui.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.ui.components.LinkBoxBottomSheet
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyScreen(
    viewModel: LinkBoxViewModel, 
    onNavigateToPageEditor: (Long, Boolean) -> Unit,
    onNavigateToBrowser: (String, Boolean) -> Unit,
    isInternalTab: Boolean = false,
    onNavigateToLinkSharing: () -> Unit = {},
    showCategoryChips: Boolean = false
) {
    val entities by viewModel.myEntities.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val currentFolderName by viewModel.currentFolderName.collectAsState()

    // Handle back press to navigate up folders instead of exiting app
    BackHandler(enabled = currentFolderId != null) {
        viewModel.navigateUp()
    }

    var showFabMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf<EntityType?>(null) }
    
    // Entity Context Menu State
    var selectedEntityForMenu by remember { mutableStateOf<EntityItem?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    
    // Pull-to-refresh state
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val currentEntityTypeFilter by viewModel.myEntitiesEntityTypeFilter.collectAsState()
    val showOnlyStarred by viewModel.myEntitiesShowOnlyStarred.collectAsState()
    
    val filterOptions = listOf("All", "Folders", "Links", "Pages")
    val selectedFilterIndex = when (currentEntityTypeFilter) {
        null -> 0
        EntityType.FOLDER -> 1
        EntityType.LINK -> 2
        EntityType.PAGE -> 3
    }

    val myContent = @Composable { padding: PaddingValues ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!isInternalTab) {
                // Background Overlay when FAB menu is open
                AnimatedVisibility(
                    visible = showFabMenu,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.32f))
                            .pointerInput(Unit) {
                                detectTapGestures { showFabMenu = false }
                            }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshData() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Category Filter Chips
                    if (showCategoryChips && currentFolderId == null) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(filterOptions.size) { index ->
                                FilterChip(
                                    selected = selectedFilterIndex == index,
                                    onClick = { 
                                        val type = when (index) {
                                            1 -> EntityType.FOLDER
                                            2 -> EntityType.LINK
                                            3 -> EntityType.PAGE
                                            else -> null
                                        }
                                        viewModel.setMyEntitiesFilter(type)
                                    },
                                    label = { 
                                        Text(
                                            filterOptions[index], 
                                            fontSize = 13.sp,
                                            fontWeight = if (selectedFilterIndex == index) FontWeight.SemiBold else FontWeight.Normal
                                        ) 
                                    },
                                    leadingIcon = if (selectedFilterIndex == index) {
                                        {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = Color.Transparent,
                                        selectedBorderColor = Color.Transparent,
                                        enabled = true,
                                        selected = selectedFilterIndex == index
                                    )
                                )
                            }
                        }
                    }

                    if (entities.isEmpty()) {
                        EmptyMyScreenCard(
                            showingStarred = showOnlyStarred,
                            isSubfolder = currentFolderId != null
                        )
                    } else {
                        AnimatedContent(
                            targetState = currentFolderId,
                            transitionSpec = {
                                if (targetState != null) {
                                    // Navigating into a folder: slide in from right
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it / 2 } + fadeOut())
                                } else {
                                    // Navigating back to root: slide in from left
                                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it / 2 } + fadeOut())
                                }.using(SizeTransform(clip = false))
                            },
                            label = "FolderTransition"
                        ) { _ ->
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(entities) { entityWithSharing ->
                                    EntityRow(
                                        entity = entityWithSharing.entity,
                                        onClick = {
                                            val entity = entityWithSharing.entity
                                            when (entity.type) {
                                                EntityType.FOLDER -> viewModel.navigateToFolder(entity)
                                                EntityType.PAGE -> onNavigateToPageEditor(entity.id, true)
                                                EntityType.LINK -> {
                                                    val url = entity.value
                                                    if (!url.isNullOrBlank()) {
                                                        val fullUrl = if (!url.startsWith("http")) "https://$url" else url
                                                        onNavigateToBrowser(fullUrl, true)
                                                    }
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            selectedEntityForMenu = entityWithSharing.entity
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

            if (!isInternalTab && showAddDialog != null) {
                AddEntitySheet(
                    type = showAddDialog!!,
                    folders = folders,
                    currentParentId = currentFolderId,
                    onDismiss = { showAddDialog = null },
                    onAdd = { name, type, value, parentId ->
                        viewModel.addEntity(name, type, value, parentId)
                        showAddDialog = null
                    }
                )
            }

            if (selectedEntityForMenu != null) {
                EntityContextMenu(
                    entity = selectedEntityForMenu!!,
                    onDismiss = { selectedEntityForMenu = null },
                    onAction = { action ->
                        val entity = selectedEntityForMenu!!
                        when (action) {
                            "Open" -> {
                                selectedEntityForMenu = null
                                when (entity.type) {
                                    EntityType.FOLDER -> viewModel.navigateToFolder(entity)
                                    EntityType.PAGE -> onNavigateToPageEditor(entity.id, true)
                                    EntityType.LINK -> {
                                        val url = entity.value
                                        if (!url.isNullOrBlank()) {
                                            val fullUrl = if (!url.startsWith("http")) "https://$url" else url
                                            onNavigateToBrowser(fullUrl, true)
                                        }
                                    }
                                }
                            }
                            "Edit" -> {
                                selectedEntityForMenu = null
                                if (entity.type == EntityType.PAGE) onNavigateToPageEditor(entity.id, false)
                                // Link/Folder edit TBD
                            }
                            "Delete" -> {
                                selectedEntityForMenu = null
                                viewModel.deleteEntity(entity)
                            }
                            "Change Location" -> {
                                showMoveDialog = true
                            }
                            "Rename" -> {
                                showRenameDialog = true
                            }
                            "Share" -> {
                                showShareSheet = true
                            }
                            "Star", "Unstar" -> {
                                selectedEntityForMenu = null
                                viewModel.toggleEntityPin(entity.id, entity.isPinned)
                            }
                        }
                    }
                )
            }

            if (showMoveDialog && selectedEntityForMenu != null) {
                MoveEntitySheet(
                    entity = selectedEntityForMenu!!,
                    folders = folders.filter { it.id != selectedEntityForMenu!!.id },
                    onDismiss = { 
                        showMoveDialog = false
                        selectedEntityForMenu = null
                    },
                    onMove = { newParentId ->
                        viewModel.moveEntity(selectedEntityForMenu!!.id, newParentId)
                        showMoveDialog = false
                        selectedEntityForMenu = null
                    }
                )
            }

            if (showRenameDialog && selectedEntityForMenu != null) {
                RenameSheet(
                    entity = selectedEntityForMenu!!,
                    onDismiss = {
                        showRenameDialog = false
                        selectedEntityForMenu = null
                    },
                    onRename = { newName ->
                        viewModel.renameEntity(selectedEntityForMenu!!.id, newName)
                        showRenameDialog = false
                        selectedEntityForMenu = null
                    }
                )
            }

            if (showShareSheet && selectedEntityForMenu != null) {
                ShareSheet(
                    entity = selectedEntityForMenu!!,
                    viewModel = viewModel,
                    onDismiss = {
                        showShareSheet = false
                        selectedEntityForMenu = null
                    }
                )
            }
            
            if (isInternalTab) {
        myContent(PaddingValues(0.dp))
    } else {
        Scaffold(
            topBar = {
                if (!isInternalTab) {
                    TopAppBar(
                        title = { Text(currentFolderName, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            if (currentFolderId != null) {
                                IconButton(onClick = { viewModel.navigateUp() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            // Link Sharing button
                            IconButton(onClick = onNavigateToLinkSharing) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = "My Links",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            },
            floatingActionButton = {
                if (!isInternalTab) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        AnimatedVisibility(
                            visible = showFabMenu,
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom) + scaleIn(initialScale = 0.8f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom) + scaleOut(targetScale = 0.8f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                FabMenuOption(
                                    icon = Icons.Default.Description,
                                    label = "Page",
                                    color = MaterialTheme.colorScheme.secondary,
                                    onClick = {
                                        showFabMenu = false
                                        onNavigateToPageEditor(-1L, false)
                                    }
                                )
                                FabMenuOption(
                                    icon = Icons.Default.Link,
                                    label = "Link",
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        showFabMenu = false
                                        showAddDialog = EntityType.LINK
                                    }
                                )
                                FabMenuOption(
                                    icon = Icons.Default.Folder,
                                    label = "Folder",
                                    color = Color(0xFFFFC107),
                                    onClick = {
                                        showFabMenu = false
                                        showAddDialog = EntityType.FOLDER
                                    }
                                )
                            }
                        }
                        
                        FloatingActionButton(
                            onClick = { showFabMenu = !showFabMenu },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            val rotation by animateFloatAsState(
                                targetValue = if (showFabMenu) 45f else 0f,
                                label = "fabRotation"
                            )
                            Icon(
                                imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer { rotationZ = rotation }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            myContent(padding)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    entity: EntityItem,
    viewModel: LinkBoxViewModel,
    onDismiss: () -> Unit
) {
    val sharings by viewModel.getAllSharingsByEntity(entity.id).collectAsState(initial = emptyList())
    val clipboardManager = LocalClipboardManager.current
    
    // Check if any sharing is active
    val hasActiveSharing = sharings.any { it.privacy == PrivacyType.PUBLIC }
    
    // Get first sharing for settings (all sharings for entity share the same settings)
    val currentSharing = sharings.firstOrNull()
    
    // State for expandable settings section
    var showAdvancedSettings by remember { mutableStateOf(false) }
    
    // State for dismissable warning
    var showIllegalContentWarning by remember { mutableStateOf(true) }
    
    // State for read more dialog
    var showExternalSharingInfo by remember { mutableStateOf(false) }

    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = "Share ${entity.name}"
    ) {
        // Illegal Content Warning (dismissable)
        AnimatedVisibility(
            visible = showIllegalContentWarning,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "We do not allow sharing illegal content. Doing so can result in account ban.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showIllegalContentWarning = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Asset Access Via Link Section with Master Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasActiveSharing) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = if (hasActiveSharing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Sharing Access",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hasActiveSharing) "Anyone with the link can view" else "Sharing is off",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = hasActiveSharing,
                        onCheckedChange = { enabled ->
                            // Toggle all sharings
                            sharings.forEach { sharing ->
                                viewModel.toggleSharingAccess(sharing.id, enabled)
                            }
                        }
                    )
                }
                
                // Advanced Settings Section (when sharing is active)
                if (hasActiveSharing && currentSharing != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    // Expand/Collapse Settings Button
                    Surface(
                        onClick = { showAdvancedSettings = !showAdvancedSettings },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sharing Settings",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showAdvancedSettings) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Advanced Settings Content
                    AnimatedVisibility(
                        visible = showAdvancedSettings,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Allow Save Copy Option
                            SharingSettingRow(
                                icon = Icons.Default.ContentCopy,
                                title = "Allow Save Copy",
                                description = "Viewers can save their own copy of this content",
                                checked = currentSharing.allowDuplicate,
                                onCheckedChange = { enabled ->
                                    sharings.forEach { sharing ->
                                        viewModel.updateSharingPermissions(sharing.id, allowDuplicate = enabled)
                                    }
                                }
                            )
                            
                            // 3. Sharing Content Outside App Option
                            SharingSettingRow(
                                icon = Icons.AutoMirrored.Filled.ScreenShare,
                                title = "External Sharing",
                                description = "Allow screenshots, screen recording & URL access",
                                checked = currentSharing.allowExternalSharing,
                                onCheckedChange = { enabled ->
                                    sharings.forEach { sharing ->
                                        viewModel.updateSharingPermissions(sharing.id, allowExternalSharing = enabled)
                                    }
                                },
                                hasReadMore = true,
                                onReadMore = { showExternalSharingInfo = true }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Divider with Create New Link text
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  Sharing Links  ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Links List Header
        if (sharings.isNotEmpty()) {
            Text(
                text = "${sharings.size} Active Link${if (sharings.size > 1) "s" else ""}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // List of sharing links
        sharings.forEachIndexed { index, sharing ->
            SharingLinkItem(
                sharing = sharing,
                index = index + 1,
                onCopy = {
                    val url = "https://api.pokipro.com/linkbox/?token=${sharing.token}"
                    clipboardManager.setText(AnnotatedString(url))
                },
                onDelete = { viewModel.deleteSharingLink(sharing.id) },
                onToggleAccess = { enabled -> viewModel.toggleSharingAccess(sharing.id, enabled) },
                onRename = { newName -> viewModel.updateSharingSettings(sharing.id, newName, sharing.pointsRequired) },
                onSetPoints = { points -> viewModel.updateSharingSettings(sharing.id, sharing.name ?: "Link ${index + 1}", points) }
            )
            if (index < sharings.size - 1) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // Create New Link Button
        Button(
            onClick = { viewModel.createNewSharingLink(entity.id, entity.name) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Link", fontWeight = FontWeight.SemiBold)
        }
    }
    
    // External Sharing Info Dialog
    if (showExternalSharingInfo) {
        LinkBoxBottomSheet(
            onDismissRequest = { showExternalSharingInfo = false },
            title = "About External Sharing Protection"
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoRow(
                    icon = Icons.Default.Link,
                    title = "Protected URL Access",
                    description = "When disabled, shared URLs will only open within LinkBox app. The actual URL won't be visible to viewers for direct access."
                )
                
                InfoRow(
                    icon = Icons.Default.Folder,
                    title = "Folder Content Protection",
                    description = "Prevents viewers from accessing folder contents outside of the app environment."
                )
                
                InfoRow(
                    icon = Icons.Default.Screenshot,
                    title = "Screenshot Prevention",
                    description = "Attempts to prevent screenshots and screen recording of shared content."
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Note: We try our best to protect your content, but cannot guarantee 100% prevention of external sharing due to device-level limitations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                Button(
                    onClick = { showExternalSharingInfo = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got it")
                }
            }
        }
    }
}

@Composable
private fun SharingSettingRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hasCustomize: Boolean = false,
    onCustomize: () -> Unit = {},
    hasReadMore: Boolean = false,
    onReadMore: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (hasCustomize) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onCustomize,
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Customize",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (hasReadMore) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        onClick = onReadMore,
                        color = Color.Transparent
                    ) {
                        Text(
                            text = "Read more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharingLinkItem(
    sharing: SharingEntity,
    index: Int,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onToggleAccess: (Boolean) -> Unit,
    onRename: (String) -> Unit = {},
    onSetPoints: (Int) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPointsDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(sharing.name ?: "Link $index") }
    var pointsInput by remember { mutableStateOf(sharing.pointsRequired.toString()) }

    val isActive = sharing.privacy == PrivacyType.PUBLIC
    val shareUrl = "https://api.pokipro.com/linkbox/?token=${sharing.token}"
    val displayName = sharing.name ?: "Link $index"

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Link info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isActive) SuccessStart.copy(alpha = 0.2f) else InactiveGray.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isActive) "Active" else "Inactive",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            color = if (isActive) SuccessStart else InactiveGray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = shareUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${sharing.clicks} clicks • ${sharing.views} views",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sharing.pointsRequired > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = GoldStart
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${sharing.pointsRequired} points required",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldStart
                        )
                    }
                }
            }

            // Copy button
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(20.dp))
            }

            // Menu button
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            newName = displayName
                            showRenameDialog = true
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isActive) "Turn Off Access" else "Turn On Access") },
                        leadingIcon = { 
                            Icon(
                                if (isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility, 
                                contentDescription = null
                            ) 
                        },
                        onClick = {
                            onToggleAccess(!isActive)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Set Points Cost") },
                        leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = GoldStart) },
                        onClick = {
                            pointsInput = sharing.pointsRequired.toString()
                            showPointsDialog = true
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Link", color = ErrorStart) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorStart) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        LinkBoxBottomSheet(
            onDismissRequest = { showRenameDialog = false },
            title = "Rename Link"
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Link Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onRename(newName)
                    }
                    showRenameDialog = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    // Set Points Dialog
    if (showPointsDialog) {
        LinkBoxBottomSheet(
            onDismissRequest = { showPointsDialog = false },
            title = "Set Points Cost"
        ) {
            Text(
                "Set how many points are required to access this link. Minimum is 1 point.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pointsInput,
                onValueChange = { value -> 
                    if (value.isEmpty() || value.all { it.isDigit() }) {
                        pointsInput = value
                    }
                },
                label = { Text("Points Required (min 1)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = GoldStart) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val points = (pointsInput.toIntOrNull() ?: 1).coerceAtLeast(1)
                    onSetPoints(points)
                    showPointsDialog = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntityRow(
    entity: EntityItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val iconColor = when (entity.type) {
        EntityType.LINK -> MaterialTheme.colorScheme.primary
        EntityType.FOLDER -> Color(0xFFFFC107)
        EntityType.PAGE -> MaterialTheme.colorScheme.secondary
    }
    
    val icon = when (entity.type) {
        EntityType.LINK -> Icons.Default.Link
        EntityType.FOLDER -> Icons.Default.Folder
        EntityType.PAGE -> Icons.Default.Description
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 0.2.dp,
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Smaller Icon Container
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entity.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (entity.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Pinned",
                            tint = GoldStart,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                val subtitle = when(entity.type) {
                    EntityType.LINK -> entity.value?.removePrefix("https://")?.removePrefix("http://")?.split("/")?.firstOrNull() ?: "Link"
                    EntityType.FOLDER -> "Folder"
                    EntityType.PAGE -> "Rich Page"
                }
                
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(
                onClick = onLongClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EntityContextMenu(
    entity: EntityItem,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = entity.name
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val actions = mutableListOf("Open", "Share", "Edit", (if (entity.isPinned) "Unstar" else "Star"), "Rename", "Delete", "Change Location")
            
            actions.forEach { action ->
                val iconColor = when (action) {
                    "Delete" -> ErrorStart
                    "Star", "Unstar" -> GoldStart
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val textColor = when (action) {
                    "Delete" -> ErrorStart
                    else -> MaterialTheme.colorScheme.onSurface
                }
                
                ListItem(
                    headlineContent = { 
                        Text(
                            action,
                            color = textColor
                        ) 
                    },
                    leadingContent = {
                        val icon = when (action) {
                            "Open" -> Icons.AutoMirrored.Filled.OpenInNew
                            "Edit" -> Icons.Default.Edit
                            "Rename" -> Icons.Default.DriveFileRenameOutline
                            "Share" -> Icons.Default.Share
                            "Star" -> Icons.Default.Star
                            "Unstar" -> Icons.Outlined.StarOutline
                            "Change Location" -> Icons.AutoMirrored.Filled.DriveFileMove
                            "Delete" -> Icons.Default.Delete
                            else -> Icons.Default.MoreVert
                        }
                        Icon(
                            icon, 
                            contentDescription = null, 
                            tint = iconColor
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.combinedClickable(onClick = { onAction(action) })
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameSheet(
    entity: EntityItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(entity.name) }

    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = "Rename ${entity.type.name.lowercase()}"
    ) {
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            label = { Text("New Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { if (newName.isNotBlank()) onRename(newName) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rename")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MoveEntitySheet(
    entity: EntityItem,
    folders: List<EntityItem>,
    onDismiss: () -> Unit,
    onMove: (Long?) -> Unit
) {
    var selectedParentId by remember { mutableStateOf<Long?>(entity.parentId) }

    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = "Move ${entity.name}"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Select destination folder:", style = MaterialTheme.typography.bodyMedium)
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                item {
                    ListItem(
                        headlineContent = { Text("Root") },
                        leadingContent = { RadioButton(selected = selectedParentId == null, onClick = { selectedParentId = null }) },
                        modifier = Modifier.combinedClickable(onClick = { selectedParentId = null })
                    )
                }
                items(folders) { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        leadingContent = { RadioButton(selected = selectedParentId == folder.id, onClick = { selectedParentId = folder.id }) },
                        modifier = Modifier.combinedClickable(onClick = { selectedParentId = folder.id })
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onMove(selectedParentId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Move")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddEntitySheet(
    type: EntityType,
    folders: List<EntityItem>,
    currentParentId: Long?,
    onDismiss: () -> Unit,
    onAdd: (String, EntityType, String?, Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<Long?>(currentParentId) }
    var expanded by remember { mutableStateOf(false) }

    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = "Add ${type.name.lowercase().replaceFirstChar { it.uppercase() }}"
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            if (type == EntityType.LINK) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Directory Picker
            Column {
                Text("Directory", style = MaterialTheme.typography.labelLarge)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text(folders.find { it.id == parentId }?.name ?: "Root", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Root") },
                            onClick = { 
                                parentId = null 
                                expanded = false
                            }
                        )
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    parentId = folder.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name, type, value, parentId) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FabMenuOption(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) CardElevatedDark else CardElevatedLight
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun EmptyMyScreenCard(showingStarred: Boolean, isSubfolder: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                if (showingStarred) Icons.Default.StarOutline 
                else if (isSubfolder) Icons.Default.FolderOpen
                else Icons.Default.PrivacyTip,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                if (showingStarred) "No Starred Items" 
                else if (isSubfolder) "Nothing Here"
                else "Your Private Space",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (showingStarred) "Items you star will appear here for easy access." 
                else if (isSubfolder) "This folder is currently empty. Tap + to add something!"
                else "Create privacy-first links that you control. Share access with specific people, set time limits, and revoke permissions anytime.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            if (!showingStarred && !isSubfolder) {
                Spacer(modifier = Modifier.height(24.dp))
                // Feature highlights
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    FeatureHighlight(
                        icon = Icons.Default.Lock,
                        title = "Protected Sharing",
                        description = "Share links without exposing actual URLs"
                    )
                    FeatureHighlight(
                        icon = Icons.Default.Sync,
                        title = "Synced Access",
                        description = "Updates sync automatically to all viewers"
                    )
                    FeatureHighlight(
                        icon = Icons.Default.Block,
                        title = "Revoke Anytime",
                        description = "Disable access instantly when needed"
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Tap + to create your first link",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
