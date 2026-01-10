package com.clicktoearn.linkbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clicktoearn.linkbox.data.local.AssetType
import com.clicktoearn.linkbox.data.local.HistoryEntity
import com.clicktoearn.linkbox.ui.Screen
import com.clicktoearn.linkbox.ui.components.PointsHeaderButton
import com.clicktoearn.linkbox.ui.components.WalletBottomSheet
import com.clicktoearn.linkbox.ui.components.InsufficientPointsSheet
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.viewmodel.UiHistoryItem
import com.clicktoearn.linkbox.ads.AdsManager
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.clicktoearn.linkbox.utils.findActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: LinkBoxViewModel, navController: NavController) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState(initial = emptyList())
    var showStarredOnly by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()
    val isPremium = userProfile?.isPremium == true
    
    var showWalletSheet by remember { mutableStateOf(false) }
    var showInsufficientSheet by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var tokenToReport by remember { mutableStateOf("") }
    var requiredPointsForAccess by remember { mutableStateOf(0) }
    val userPoints by viewModel.userPoints.collectAsState()
    val pointsSheetState = rememberModalBottomSheetState()
    
    // Hamburger menu states
    var showHamburgerMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("Date Accessed (Newest)") }
    var showSortMenu by remember { mutableStateOf(false) }
    var fileTypeFilter by remember { mutableStateOf(setOf<AssetType>()) }
    var showFileTypeMenu by remember { mutableStateOf(false) }
    
    val isRefreshing by viewModel.isRefreshing.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.refreshHistory(isManual = false)
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Force screenshot blocking on History Screen
    DisposableEffect(Unit) {
        viewModel.enableScreenshotProtection()
        onDispose {
            viewModel.disableScreenshotProtection()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Link History") },
                actions = {
                    PointsHeaderButton(viewModel = viewModel) { showWalletSheet = true }
                    
                    // Three-dot menu (last element on right)
                    Box {
                        IconButton(onClick = { showHamburgerMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showHamburgerMenu,
                            onDismissRequest = { showHamburgerMenu = false }
                        ) {
                            // Star filter option
                            DropdownMenuItem(
                                text = { Text(if (showStarredOnly) "Show All" else "Show Starred Only") },
                                onClick = {
                                    showStarredOnly = !showStarredOnly
                                    showHamburgerMenu = false
                                },
                                leadingIcon = { 
                                    Icon(
                                        if (showStarredOnly) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        null,
                                        tint = if (showStarredOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    ) 
                                }
                            )
                            
                            HorizontalDivider()
                            
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
                                        "Date Accessed (Newest)",
                                        "Date Accessed (Oldest)",
                                        "Name (A-Z)",
                                        "Name (Z-A)",
                                        "Point Cost"
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
                            
                            // File Type Filters with submenu
                            Box {
                                DropdownMenuItem(
                                    text = { Text("File Type Filters") },
                                    onClick = { showFileTypeMenu = !showFileTypeMenu },
                                    leadingIcon = { Icon(Icons.Default.FilterList, null) },
                                    trailingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
                                )
                                
                                DropdownMenu(
                                    expanded = showFileTypeMenu,
                                    onDismissRequest = { showFileTypeMenu = false }
                                ) {
                                    listOf(
                                        AssetType.FILE to "Pages",
                                        AssetType.FOLDER to "Folders",
                                        AssetType.LINK to "Links"
                                    ).forEach { (type, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                fileTypeFilter = if (fileTypeFilter.contains(type)) {
                                                    fileTypeFilter - type
                                                } else {
                                                    fileTypeFilter + type
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    if (fileTypeFilter.contains(type)) 
                                                        Icons.Default.CheckBox 
                                                    else 
                                                        Icons.Default.CheckBoxOutlineBlank,
                                                    null
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            AnimatedVisibility(
                visible = showSearchBar,
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
                            placeholder = { Text("Search history...") },
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
            
            // Split history into visible and hidden
            val currentTime = remember(history) { System.currentTimeMillis() }
            val (visibleHistory, hiddenHistory) = remember(history) {
                history.partition { item ->
                    val isExpired = item.expiryDate != null && item.expiryDate < currentTime
                    val isPaused = item.linkStatus == "PAUSED"
                    val isRevoked = item.isDeleted || !item.sharingEnabled
                    !isExpired && !isPaused && !isRevoked
                }
            }

            // Filter and sort history
            val filteredAndSortedHistory = remember(visibleHistory, showStarredOnly, searchQuery, sortOption, fileTypeFilter) {
                var filtered = visibleHistory
                
                // Apply starred filter
                if (showStarredOnly) {
                    filtered = filtered.filter { it.isStarred }
                }
                
                // Apply search filter
                if (searchQuery.isNotEmpty()) {
                    filtered = filtered.filter { 
                        it.assetName.contains(searchQuery, ignoreCase = true) ||
                        it.ownerName.contains(searchQuery, ignoreCase = true)
                    }
                }
                
                // Apply file type filter
                if (fileTypeFilter.isNotEmpty()) {
                    filtered = filtered.filter { item ->
                        try {
                            fileTypeFilter.contains(AssetType.valueOf(item.assetType))
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
                
                // Apply sorting
                when (sortOption) {
                    "Date Accessed (Newest)" -> filtered.sortedByDescending { it.accessedAt }
                    "Date Accessed (Oldest)" -> filtered.sortedBy { it.accessedAt }
                    "Name (A-Z)" -> filtered.sortedBy { it.assetName.lowercase() }
                    "Name (Z-A)" -> filtered.sortedByDescending { it.assetName.lowercase() }
                    "Point Cost" -> filtered.sortedByDescending { it.pointCost }
                    else -> filtered
                }
            }
            

            var showHiddenItemsSheet by remember { mutableStateOf(false) }

            if (hiddenHistory.isNotEmpty()) {
                Surface(
                    onClick = { showHiddenItemsSheet = true },
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Some Items are not Showing in the list",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            if (showHiddenItemsSheet) {
                 HiddenItemsSheet(
                    history = hiddenHistory,
                    currentTime = currentTime,
                    onDismiss = { showHiddenItemsSheet = false },
                    onDeleteItems = { items ->
                        viewModel.deleteHistoryItems(items)
                        // Sheet will update or close automatically as items are removed from hiddenHistory
                    }
                 )
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.refreshHistory()
                },
                modifier = Modifier.weight(1f)
            ) {
                if (filteredAndSortedHistory.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.History,
                        message = when {
                            searchQuery.isNotEmpty() -> "No history found matching \"$searchQuery\""
                            fileTypeFilter.isNotEmpty() -> "No history found for selected file types"
                            history.isEmpty() -> "Your history is empty. Accessed links will appear here."
                            else -> "No starred links yet."
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            if (!isPremium && AdsManager.isAdEnabled(AdsManager.KEY_HISTORY_BANNER_TOP)) {
                                AdsManager.AdaptiveBannerAdView()
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                        items(filteredAndSortedHistory, key = { it.token }) { item ->
                            HistoryItem(
                                item = item,
                                onStarClick = { 
                                    viewModel.toggleHistoryStarred(item)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (!item.isStarred) "Added to Starred" else "Removed from Starred"
                                        )
                                    }
                                },
                                onDeleteClick = { 
                                    viewModel.deleteHistory(item)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Removed from history")
                                    }
                                },
                                onReportClick = {
                                    tokenToReport = item.token
                                    showReportSheet = true
                                },
                                onClick = {
                                    val activity = context.findActivity()
                                    val action = {
                                        navController.navigate(Screen.SharedContent.createRoute(item.token))
                                    }
                                    
                                    // Show ad when opening from history
                                    val isLink = item.assetType == AssetType.LINK.name
                                    val useRewarded = isLink && AdsManager.isAdEnabled(AdsManager.KEY_HISTORY_AD_LINK_OPEN_REWARDED)
                                    val showAd = !isPremium && 
                                                 AdsManager.isAdEnabled(AdsManager.KEY_HISTORY_AD_OPEN_ASSETS)
                                    
                                    val shouldShowFrequency = AdsManager.shouldShowHistoryAd(context)
                                    
                                    if (activity != null && showAd && shouldShowFrequency) {
                                        if (useRewarded) {
                                            AdsManager.showRewardedInterstitialAd(activity, placementKey = "history_${item.token}", strict = false, onRewardEarned = action, onAdClosed = { message ->
                                                message?.let { viewModel.showMessage(it) }
                                            })
                                        } else {
                                            AdsManager.showInterstitialAd(activity, placementKey = "history_${item.token}", onAdDismissed = action)
                                        }
                                    } else {
                                        action()
                                    }
                                }
                            )
                        }
                        item {
                            if (!isPremium && AdsManager.isAdEnabled(AdsManager.KEY_HISTORY_NATIVE_BOTTOM)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        AdsManager.NativeAdView()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showWalletSheet) {
            WalletBottomSheet(
                viewModel = viewModel,
                onDismiss = { showWalletSheet = false },
                sheetState = pointsSheetState
            )
        }
        
        if (showInsufficientSheet) {
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()
            InsufficientPointsSheet(
                currentBalance = userPoints,
                requiredPoints = requiredPointsForAccess,
                onDismiss = { showInsufficientSheet = false },
                isLoggedIn = isLoggedIn,
                viewModel = viewModel,
                onWatchAd = { 
                    AdsManager.showRewardedAd(context as Activity, strict = true, 
                        onRewardEarned = {
                            viewModel.earnPoints(5) 
                            showInsufficientSheet = false
                        },
                        onAdClosed = { message ->
                            message?.let { viewModel.showMessage(it) }
                        }
                    )
                },
                onEarnPoints = { 
                    showInsufficientSheet = false
                    navController.navigate(Screen.MyLinks.route) // Navigate to share links
                },
                onBuyPoints = { points ->
                    showInsufficientSheet = false
                    val activity = context.findActivity()
                    if (activity != null) {
                        viewModel.buyPoints(activity, points)
                    } else {
                        viewModel.showMessage("Failed to start purchase flow")
                    }
                }
            )
        }
        if (showReportSheet) {
            ReportContentSheet(
                onDismiss = { showReportSheet = false },
                onSubmit = { reason, contact ->
                    scope.launch { 
                        viewModel.submitReport(tokenToReport, reason, contact)
                        snackbarHostState.showSnackbar("Report submitted. We will review it shortly.") 
                    }
                    showReportSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenItemsSheet(
    history: List<UiHistoryItem>,
    currentTime: Long,
    onDismiss: () -> Unit,
    onDeleteItems: (List<UiHistoryItem>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    // Group items
    val (revoked, expired, paused) = remember(history) {
        val revokedList = history.filter { item -> item.isDeleted || !item.sharingEnabled }
        val remaining = history - revokedList.toSet()
        val expiredList = remaining.filter { item -> item.expiryDate != null && item.expiryDate < currentTime }
        val remaining2 = remaining - expiredList.toSet()
        val pausedList = remaining2.filter { item -> item.linkStatus == "PAUSED" }
        Triple(revokedList, expiredList, pausedList)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Hidden Items",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            if (paused.isNotEmpty()) {
                HiddenItemRow(
                    label = "Access Paused",
                    count = paused.size,
                    subDescription = "They can be shown again if access is given again",
                    onDelete = { onDeleteItems(paused) }
                )
            }
            
            if (expired.isNotEmpty()) {
                 HiddenItemRow(
                    label = "Expired",
                    count = expired.size,
                    subDescription = null,
                    onDelete = { onDeleteItems(expired) }
                )
            }
            
            if (revoked.isNotEmpty()) {
                 HiddenItemRow(
                    label = "Revoked/Removed",
                    count = revoked.size,
                    subDescription = null, 
                    onDelete = { onDeleteItems(revoked) }
                )
            }

            if (expired.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onDeleteItems(expired) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove All Expired Links")
                }
            }
        }
    }
}

@Composable
fun HiddenItemRow(
    label: String,
    count: Int,
    subDescription: String?,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* no-op */ }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$label - $count Links",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            if (subDescription != null) {
                Text(
                    text = subDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
}

@Composable
fun HistoryItem(
    item: com.clicktoearn.linkbox.viewmodel.UiHistoryItem,
    onStarClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit,
    onClick: () -> Unit
) {

    
    val iconVector = remember(item.assetType) {
        try {
            when (AssetType.valueOf(item.assetType)) {
                AssetType.FILE -> Icons.Default.Description
                AssetType.FOLDER -> Icons.Default.Folder
                AssetType.LINK -> Icons.Default.Link
            }
        } catch (e: Exception) {
            Icons.AutoMirrored.Filled.Help // Default for unknown/loading
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Type Icon
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.assetName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.isStarred) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Starred",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "by ${item.ownerName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (item.isStarred) "Remove Star" else "Add Star") },
                            onClick = {
                                onStarClick()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (item.isStarred) Icons.Default.Star else Icons.Default.StarOutline,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Information") },
                            onClick = {
                                showInfo = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Report Content") },
                            onClick = {
                                onReportClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Remove from History") },
                            onClick = {
                                onDeleteClick()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        }
    }

    if (showInfo) {
        val detailDate = remember(item.accessedAt) {
            SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(item.accessedAt))
        }
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Link Details") },
            text = {
                Column {
                    DetailRow("Name", item.assetName)
                    DetailRow("Description", item.description.ifEmpty { "No description" })
                    DetailRow("Owner", item.ownerName)
                    DetailRow("Type", item.assetType)
                    DetailRow("Point Cost", if (item.pointCost == 0) "Free" else "${item.pointCost} coins")
                    DetailRow("Accessed On", detailDate)
                    if (item.allowFurtherSharing) {
                        DetailRow("Token", item.token)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

