package com.clicktoearn.linkbox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.rotate
import androidx.navigation.NavController
import com.clicktoearn.linkbox.data.local.AssetType
import com.clicktoearn.linkbox.data.local.AssetEntity
import com.clicktoearn.linkbox.ui.Screen
import com.clicktoearn.linkbox.ui.components.BottomSheetActionItem
import com.clicktoearn.linkbox.ui.components.LinkBoxBottomSheet
import com.clicktoearn.linkbox.ui.components.ShareMenuSheet
import com.clicktoearn.linkbox.ui.components.LoginRequiredSheet
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.ads.AdsManager
import com.clicktoearn.linkbox.utils.findActivity
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(viewModel: LinkBoxViewModel, navController: NavController) {
    val assets by viewModel.assets.collectAsState(initial = emptyList())
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState(initial = emptyList())
    val allSharingLinks by viewModel.allSharingLinks.collectAsState(initial = emptyList())
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isPremium = userProfile?.isPremium == true
    var showLoginPrompt by remember { mutableStateOf(false) }
    val loginSheetState = rememberModalBottomSheetState()
    
    var isFabExpanded by remember { mutableStateOf(false) }
    var selectedAsset by remember { mutableStateOf<AssetEntity?>(null) }
    var showLinkPreview by remember { mutableStateOf<AssetEntity?>(null) }
    // Dialog states
    var pendingAssetType by remember { mutableStateOf<AssetType?>(null) }
    var showRenameSheet by remember { mutableStateOf<AssetEntity?>(null) }
    var showEditLinkSheet by remember { mutableStateOf<AssetEntity?>(null) }
    var showAssetInfo by remember { mutableStateOf<AssetEntity?>(null) }
    var showMoveToSheet by remember { mutableStateOf<AssetEntity?>(null) }
    var showFolderMenu by remember { mutableStateOf(false) }
    
    // Hamburger menu states
    var showHamburgerMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("Name (A-Z)") }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Handle device back button when inside a folder
    BackHandler(enabled = currentFolderId != null) {
        val parentId = viewModel.getParentFolderId()
        viewModel.navigateToFolder(parentId)
    }
    
    var tempName by remember { mutableStateOf("") }
    var tempContent by remember { mutableStateOf("") }
    var showShareMenu by remember { mutableStateOf<AssetEntity?>(null) }
    
    val sheetState = rememberModalBottomSheetState()


    val renameSheetState = rememberModalBottomSheetState()
    val editLinkSheetState = rememberModalBottomSheetState()
    val infoSheetState = rememberModalBottomSheetState()
    val createSheetState = rememberModalBottomSheetState()
    val clipboardManager = LocalClipboardManager.current

    // State for tracking newly created file to navigate to

    // State for tracking newly created file to navigate to
    var pendingNewFileId by remember { mutableStateOf<String?>(null) }
    
    // Navigate to page editor when new file is created
    LaunchedEffect(assets, pendingNewFileId) {
        pendingNewFileId?.let { expectedName ->
            val newFile = assets.find { it.name == expectedName && it.type == AssetType.FILE }
            if (newFile != null) {
                pendingNewFileId = null
                navController.navigate(Screen.PageEditor.createRoute(newFile.id, true))
            }
        }
    }

    // New Asset Creation Sheet
    if (pendingAssetType != null) {
        val type = pendingAssetType!!
        val focusRequester = remember { FocusRequester() }
        
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        
        LinkBoxBottomSheet(
            onDismissRequest = { 
                pendingAssetType = null
                tempName = ""
                tempContent = ""
            },
            sheetState = createSheetState,
            title = when (type) {
                AssetType.FOLDER -> "New Folder"
                AssetType.LINK -> "New Link"
                AssetType.FILE -> "New Page"
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
                
                if (type == AssetType.LINK) {
                    OutlinedTextField(
                        value = tempContent,
                        onValueChange = { tempContent = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboardManager.getText()?.text?.let { tempContent = it }
                            }) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
                            }
                        }
                    )
                }
                
                if (type == AssetType.FILE) {
                    Text(
                        text = "You'll be taken to the page editor after creation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            pendingAssetType = null
                            tempName = ""
                            tempContent = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val isLinkValid = type != AssetType.LINK || tempContent.isNotBlank()
                            if (tempName.isNotBlank() && isLinkValid) {
                                when (type) {
                                    AssetType.FILE -> {
                                        viewModel.addFile(tempName, "", currentFolderId)
                                        pendingNewFileId = tempName
                                    }
                                    AssetType.FOLDER -> viewModel.addFolder(tempName, currentFolderId)
                                    AssetType.LINK -> viewModel.addLink(tempName, tempContent, currentFolderId)
                                }
                                tempName = ""
                                tempContent = ""
                                pendingAssetType = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }

    // Rename Sheet (for files, folders, and links)
    if (showRenameSheet != null) {
        val asset = showRenameSheet!!
        val focusRequester = remember { FocusRequester() }
        
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        
        LinkBoxBottomSheet(
            onDismissRequest = { 
                showRenameSheet = null
                tempName = ""
            },
            sheetState = renameSheetState,
            title = when (asset.type) {
                AssetType.FOLDER -> "Rename Folder"
                AssetType.LINK -> "Rename Link"
                AssetType.FILE -> "Rename Page"
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            showRenameSheet = null
                            tempName = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (tempName.isNotBlank()) {
                                val updated = asset.copy(name = tempName)
                                viewModel.updateAsset(updated)
                                showRenameSheet = null
                                tempName = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Edit Link Sheet (for links only - edits URL)
    if (showEditLinkSheet != null) {
        val asset = showEditLinkSheet!!
        val focusRequester = remember { FocusRequester() }
        
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        
        LinkBoxBottomSheet(
            onDismissRequest = { 
                showEditLinkSheet = null
                tempName = ""
                tempContent = ""
            },
            sheetState = editLinkSheetState,
            title = "Edit Link"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
                
                OutlinedTextField(
                    value = tempContent,
                    onValueChange = { tempContent = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { tempContent = it }
                        }) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Paste")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            showEditLinkSheet = null
                            tempName = ""
                            tempContent = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (tempName.isNotBlank() && tempContent.isNotBlank()) {
                                val updated = asset.copy(
                                    name = tempName,
                                    content = tempContent
                                )
                                viewModel.updateAsset(updated)
                                showEditLinkSheet = null
                                tempName = ""
                                tempContent = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Link preview dialog (only for LINK type assets)
    if (showLinkPreview != null) {
        val asset = showLinkPreview!!
        AlertDialog(
            onDismissRequest = { showLinkPreview = null },
            title = { Text(asset.name) },
            text = {
                Column {
                    Text("Link URL:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(asset.content, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Navigate to WebView - no interstitial ad for own assets
                    val encodedUrl = android.net.Uri.encode(asset.content)
                    val encodedTitle = android.net.Uri.encode(asset.name)
                    navController.navigate("webview/$encodedUrl/$encodedTitle/${asset.allowScreenCapture}/${asset.exposeUrl}")
                    showLinkPreview = null
                }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkPreview = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (currentFolderId == null) "My Assets" else (currentFolder?.name ?: "Folder"),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (currentFolderId != null) {
                        IconButton(onClick = { 
                            val parentId = viewModel.getParentFolderId()
                            viewModel.navigateToFolder(parentId)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentFolderId == null) {
                        // Root level - show My Links chip with background
                        
                        FilledTonalButton(
                            onClick = { navController.navigate(Screen.MyLinks.route) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My Links")
                        }
                        
                        // Three-dot menu (last element on right)
                        Box {
                            IconButton(onClick = { showHamburgerMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(
                                expanded = showHamburgerMenu,
                                onDismissRequest = { showHamburgerMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Search") },
                                    onClick = {
                                        showSearchBar = !showSearchBar
                                        showHamburgerMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.Search, null) }
                                )
                                
                                // Sort By with submenu
                                Box {
                                    DropdownMenuItem(
                                        text = { Text("Sort By") },
                                        onClick = { showSortMenu = !showSortMenu },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) },
                                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
                                    )
                                    
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        listOf(
                                            "Name (A-Z)",
                                            "Name (Z-A)",
                                            "Date Created (Newest)",
                                            "Date Created (Oldest)",
                                            "Type"
                                        ).forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    sortOption = option
                                                    showSortMenu = false
                                                    showHamburgerMenu = false
                                                },
                                                trailingIcon = {
                                                    if (sortOption == option) {
                                                        Icon(Icons.Default.Check, null)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Inside folder - show three-dot menu for folder options
                        IconButton(onClick = { showFolderMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Folder Options")
                        }
                        
                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename Folder") },
                                onClick = {
                                    showFolderMenu = false
                                    currentFolder?.let { folder ->
                                        showRenameSheet = folder
                                        tempName = folder.name
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.DriveFileRenameOutline, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Folder") },
                                onClick = {
                                    showFolderMenu = false
                                    currentFolder?.let { showShareMenu = it }
                                },
                                leadingIcon = { Icon(Icons.Filled.Share, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Folder Info") },
                                onClick = {
                                    showFolderMenu = false
                                    currentFolder?.let { showAssetInfo = it }
                                },
                                leadingIcon = { Icon(Icons.Filled.Info, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showFolderMenu = false
                                    currentFolder?.let { folder ->
                                        viewModel.deleteAsset(folder)
                                        viewModel.navigateToFolder(folder.parentId)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val rotation by animateFloatAsState(
                targetValue = if (isFabExpanded) 45f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
            
            Column(horizontalAlignment = Alignment.End) {
                // Mini FABs
                AnimatedVisibility(
                    visible = isFabExpanded,
                    enter = fadeIn(animationSpec = tween(150)) + 
                            expandVertically(expandFrom = Alignment.Bottom, animationSpec = tween(150)) +
                            scaleIn(transformOrigin = TransformOrigin(1f, 1f), animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(100)) + 
                           shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = tween(100)) +
                           scaleOut(transformOrigin = TransformOrigin(1f, 1f), animationSpec = tween(100))
                ) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SpeedDialItem(
                            label = "Page",
                            icon = Icons.Filled.Description,
                            onClick = {
                                isFabExpanded = false
                                pendingAssetType = AssetType.FILE
                                tempName = ""
                                tempContent = ""
                            }
                        )
                        SpeedDialItem(
                            label = "Folder",
                            icon = Icons.Filled.Folder,
                            onClick = {
                                isFabExpanded = false
                                pendingAssetType = AssetType.FOLDER
                                tempName = ""
                                tempContent = ""
                            }
                        )
                        SpeedDialItem(
                            label = "Link",
                            icon = Icons.Filled.Link,
                            onClick = {
                                isFabExpanded = false
                                pendingAssetType = AssetType.LINK
                                tempName = ""
                                tempContent = ""
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                FloatingActionButton(
                    onClick = { 
                        if (!isLoggedIn) {
                            showLoginPrompt = true
                        } else {
                            isFabExpanded = !isFabExpanded 
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.rotate(rotation)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            AnimatedVisibility(
                visible = showSearchBar && currentFolderId == null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search assets...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, "Clear search")
                                    }
                                }
                            }
                        )
                        IconButton(onClick = { 
                            showSearchBar = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, "Close search")
                        }
                    }
                }
            }
            
            // Filter and sort assets
            val filteredAndSortedAssets = remember(assets, searchQuery, sortOption) {
                var filtered = assets
                
                // Apply search filter
                if (searchQuery.isNotEmpty()) {
                    filtered = filtered.filter { 
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                }
                
                // Apply sorting
                when (sortOption) {
                    "Name (A-Z)" -> filtered.sortedBy { it.name.lowercase() }
                    "Name (Z-A)" -> filtered.sortedByDescending { it.name.lowercase() }
                    "Date Created (Newest)" -> filtered.sortedByDescending { it.createdAt }
                    "Date Created (Oldest)" -> filtered.sortedBy { it.createdAt }
                    "Type" -> filtered.sortedBy { it.type.ordinal }
                    else -> filtered
                }
            }
            
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        // Navigate to same folder to trigger reload (if repository has cache)
                        // Or just wait a bit to simulate refresh
                        kotlinx.coroutines.delay(1000)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                if (filteredAndSortedAssets.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.FolderOpen,
                        message = if (searchQuery.isNotEmpty()) {
                            "No assets found matching \"$searchQuery\""
                        } else {
                            "No assets found. Start by adding a file or folder!"
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isPremium && AdsManager.isAdEnabled(AdsManager.KEY_ASSETS_BANNER_TOP)) {
                            item { 
                                AdsManager.AdaptiveBannerAdView()
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        items(filteredAndSortedAssets) { asset ->
                            val hasLinks = allSharingLinks.any { it.assetId == asset.id }
                            AssetItem(
                                asset = asset,
                                hasLinks = hasLinks,
                                onClick = {
                                    val openAction = {
                                        when (asset.type) {
                                            AssetType.FOLDER -> viewModel.navigateToFolder(asset.id)
                                            AssetType.FILE -> {
                                                // Navigate to fullscreen page editor/viewer
                                                navController.navigate(Screen.PageEditor.createRoute(asset.id, false))
                                            }
                                            AssetType.LINK -> {
                                                val uri = android.net.Uri.parse(asset.content)
                                                val token = uri.getQueryParameter("token")
                                                val isSelfLink = token != null && (
                                                    (uri.host == "privacy.be6.in") ||
                                                    (uri.scheme == "linkbox")
                                                )

                                                if (isSelfLink && token != null) {
                                                    navController.navigate(Screen.SharedContent.createRoute(token))
                                                } else {
                                                    showLinkPreview = asset
                                                }
                                            }
                                        }
                                    }

                                    // Open directly without interstitial ad for own assets
                                    openAction()
                                },
                                onMoreClick = { selectedAsset = asset }
                            )
                        }
                        if (!isPremium && AdsManager.isAdEnabled(AdsManager.KEY_ASSETS_NATIVE_BOTTOM)) {
                            item { 
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        AdsManager.NativeAdView()
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        if (selectedAsset != null) {
            val asset = selectedAsset!!
            val assetIcon = when (asset.type) {
                AssetType.FILE -> Icons.Filled.Description
                AssetType.FOLDER -> Icons.Filled.Folder
                AssetType.LINK -> Icons.Filled.Link
            }

            LinkBoxBottomSheet(
                onDismissRequest = { selectedAsset = null },
                sheetState = sheetState,
                title = asset.name,
                icon = assetIcon
            ) {
                // 1. Open
                BottomSheetActionItem(icon = Icons.AutoMirrored.Filled.OpenInNew, label = "Open", onClick = {
                    val openAction = {
                        when (asset.type) {
                            AssetType.FOLDER -> viewModel.navigateToFolder(asset.id)
                            AssetType.FILE -> {
                                navController.navigate(Screen.PageEditor.createRoute(asset.id, false))
                            }
                            AssetType.LINK -> {
                                val uri = android.net.Uri.parse(asset.content)
                                val token = uri.getQueryParameter("token")
                                val isSelfLink = token != null && (
                                    (uri.host == "privacy.be6.in") ||
                                    (uri.scheme == "linkbox")
                                )

                                if (isSelfLink && token != null) {
                                    navController.navigate(Screen.SharedContent.createRoute(token))
                                } else {
                                    val encodedUrl = android.net.Uri.encode(asset.content)
                                    val encodedTitle = android.net.Uri.encode(asset.name)
                                    navController.navigate("webview/$encodedUrl/$encodedTitle/${asset.allowScreenCapture}/${asset.exposeUrl}")
                                }
                            }
                        }
                    }

                    // Open directly without interstitial ad for own assets
                    openAction()
                    selectedAsset = null
                })

                // 2. Edit (Page/Link)
                if (asset.type == AssetType.FILE) {
                    BottomSheetActionItem(
                        icon = Icons.Filled.Edit,
                        label = "Edit",
                        onClick = {
                            if (!isLoggedIn) {
                                showLoginPrompt = true
                            } else {
                                navController.navigate(Screen.PageEditor.createRoute(asset.id, true))
                            }
                            selectedAsset = null
                        }
                    )
                }
                
                if (asset.type == AssetType.LINK) {
                    BottomSheetActionItem(
                        icon = Icons.Filled.Edit,
                        label = "Edit Link",
                        onClick = {
                            if (!isLoggedIn) {
                                showLoginPrompt = true
                            } else {
                                showEditLinkSheet = asset
                                tempName = asset.name
                                tempContent = asset.content
                            }
                            selectedAsset = null
                        }
                    )
                }

                // 3. Rename
                BottomSheetActionItem(
                    icon = Icons.Filled.DriveFileRenameOutline,
                    label = "Rename",
                    onClick = {
                        if (!isLoggedIn) {
                            showLoginPrompt = true
                        } else {
                            showRenameSheet = asset
                            tempName = asset.name
                        }
                        selectedAsset = null
                    }
                )

                // 4. Share
                if (asset.sharingEnabled) {
                    BottomSheetActionItem(
                        icon = Icons.Filled.Share,
                        label = "Share",
                        onClick = {
                            if (!isLoggedIn) {
                                showLoginPrompt = true
                            } else {
                                showShareMenu = asset
                            }
                            selectedAsset = null
                        }
                    )
                }

                // 5. Move to
                BottomSheetActionItem(icon = Icons.AutoMirrored.Filled.DriveFileMove, label = "Move to", onClick = {
                    if (!isLoggedIn) {
                        showLoginPrompt = true
                    } else {
                        showMoveToSheet = asset
                    }
                    selectedAsset = null
                })

                // 6. Duplicate
                BottomSheetActionItem(icon = Icons.Filled.ContentCopy, label = "Duplicate", onClick = {
                    if (!isLoggedIn) {
                        showLoginPrompt = true
                    } else {
                        viewModel.duplicateAsset(asset)
                    }
                    selectedAsset = null
                })

                // 7. Info
                BottomSheetActionItem(icon = Icons.Filled.Info, label = "Info", onClick = {
                    showAssetInfo = asset
                    selectedAsset = null
                })

                // 8. Delete
                BottomSheetActionItem(icon = Icons.Filled.Delete, label = "Delete", contentColor = MaterialTheme.colorScheme.error, onClick = {
                    if (!isLoggedIn) {
                        showLoginPrompt = true
                    } else {
                        viewModel.deleteAsset(asset)
                    }
                    selectedAsset = null
                })
            }
        }
        
        if (showLoginPrompt) {
            LoginRequiredSheet(
                onDismiss = { showLoginPrompt = false },
                viewModel = viewModel,
                sheetState = loginSheetState,
                message = "Login now to create, modify, and share your assets!"
            )
        }
        
        if (showShareMenu != null) {
            val shareAsset = showShareMenu!!
            val existingLinks by viewModel.getSharingLinksForAsset(shareAsset.id).collectAsState(initial = emptyList())
            
            ShareMenuSheet(
                asset = shareAsset,
                existingLinks = existingLinks,
                onDismiss = { showShareMenu = null },
                onUpdateAsset = { updatedAsset ->
                    viewModel.updateAsset(updatedAsset)
                },
                onCreateLink = { name, _, expiryDays ->
                    viewModel.createSharingLinkFull(
                        shareAsset,
                        name,
                        expiryDays
                    )
                },
                onUpdateLink = { link ->
                    viewModel.updateSharingLink(link)
                },
                onDeleteLink = { link ->
                    viewModel.deleteSharingLink(link)
                }
            )
        }
        
        // Asset Info Sheet
        if (showAssetInfo != null) {
            val asset = showAssetInfo!!
            val dateFormatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
            
            LinkBoxBottomSheet(
                onDismissRequest = { showAssetInfo = null },
                sheetState = infoSheetState,
                title = when (asset.type) {
                    AssetType.FILE -> "Page Info"
                    AssetType.FOLDER -> "Folder Info"
                    AssetType.LINK -> "Link Info"
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Name
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Name",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Type",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = when (asset.type) {
                                AssetType.FILE -> "Page"
                                AssetType.FOLDER -> "Folder"
                                AssetType.LINK -> "Link"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // Created Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Created",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = dateFormatter.format(java.util.Date(asset.createdAt)),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    // Content info (for links and files)
                    if (asset.type == AssetType.LINK && asset.content.isNotBlank()) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "URL",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = asset.content.take(30) + if (asset.content.length > 30) "..." else "",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    if (asset.type == AssetType.FILE && asset.content.isNotBlank()) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Content Size",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${asset.content.length} characters",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { showAssetInfo = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
        
        // Move To Folder Picker Sheet
        if (showMoveToSheet != null) {
            val assetToMove = showMoveToSheet!!
            val moveToSheetState = rememberModalBottomSheetState()
            
            // Filter out the asset itself (if it's a folder) and its children
            val availableFolders = allFolders.filter { folder -> 
                folder.id != assetToMove.id && folder.parentId != assetToMove.id
            }
            
            LinkBoxBottomSheet(
                onDismissRequest = { showMoveToSheet = null },
                sheetState = moveToSheetState,
                title = "Move \"${assetToMove.name}\" to..."
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Option to move to root
                    ListItem(
                        headlineContent = { Text("My Assets (Root)") },
                        leadingContent = { 
                            Icon(Icons.Filled.Home, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            viewModel.moveAsset(assetToMove, null)
                            showMoveToSheet = null
                        }
                    )
                    
                    if (availableFolders.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text(
                            text = "Folders",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        
                        availableFolders.forEach { folder ->
                            // Calculate Path
                            val localFolder = folder
                            val pathString = remember(localFolder, allFolders) {
                                val parents = mutableListOf<String>()
                                var current: AssetEntity? = localFolder
                                while (current != null) {
                                    parents.add(0, current.name)
                                    // Use val to avoid smart cast issue
                                    val parentId = current.parentId
                                    current = if (parentId != null) allFolders.find { it.id == parentId } else null
                                }
                                parents.joinToString(" > ")
                            }

                            ListItem(
                                headlineContent = { Text(pathString) },
                                leadingContent = { 
                                    Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.clickable {
                                    viewModel.moveAsset(assetToMove, folder.id)
                                    showMoveToSheet = null
                                }
                            )
                        }
                    }
                    
                    if (availableFolders.isEmpty()) {
                        Text(
                            text = "No other folders available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssetItem(
    asset: AssetEntity,
    hasLinks: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val icon = when (asset.type) {
        AssetType.FILE -> Icons.Filled.Description
        AssetType.FOLDER -> Icons.Filled.Folder
        AssetType.LINK -> Icons.Filled.Link
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = asset.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = asset.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    if (asset.pointCost > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp), // slightly larger for visibility
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = asset.pointCost.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    if (hasLinks) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = "Has links",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, icon: ImageVector, message: String) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
@Composable
fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(end = 4.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            tonalElevation = 4.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}
