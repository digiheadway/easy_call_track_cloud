package com.clicktoearn.linkbox.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clicktoearn.linkbox.data.local.SharingLinkEntity
import com.clicktoearn.linkbox.ui.Screen
import com.clicktoearn.linkbox.ui.components.*
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.clicktoearn.linkbox.ads.AdsManager
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinksScreen(viewModel: LinkBoxViewModel, onBack: () -> Unit) {
    val sharingLinks by viewModel.allSharingLinks.collectAsState(initial = emptyList())
    val allAssets by viewModel.allAssets.collectAsState(initial = emptyList())
    val assetMap = remember(allAssets) { allAssets.associateBy { it.id } }
    
    var renameLink by remember { mutableStateOf<SharingLinkEntity?>(null) }
    var expiryLink by remember { mutableStateOf<SharingLinkEntity?>(null) }
    var filterActive by remember { mutableStateOf<Boolean?>(null) } // null = all, true = active, false = inactive

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showWalletSheet by remember { mutableStateOf(false) }
    var showRewardsSheet by remember { mutableStateOf(false) }
    var showSpendPoints by remember { mutableStateOf(false) }
    val walletSheetState = rememberModalBottomSheetState()
    val rewardsSheetState = rememberModalBottomSheetState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isPremium = userProfile?.isPremium == true
    var showLoginPrompt by remember { mutableStateOf(false) }
    val loginSheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }



    // Calculate metrics
    val totalViews = sharingLinks.sumOf { it.views }
    val totalUsers = sharingLinks.sumOf { it.users }
    
    // Filtered list
    val filteredLinks = when (filterActive) {
        true -> sharingLinks.filter { it.isActive }
        false -> sharingLinks.filter { !it.isActive }
        null -> sharingLinks
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Shared Links") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    PointsHeaderButton(viewModel) { showWalletSheet = true }
                }
            )
        }
        ) { padding ->
        // Calculate detailed earnings for Performance Summary
        val viewsEarnings = totalViews * 0.1
        val usersEarnings = totalUsers * 5.0
        val linkCostEarnings = sharingLinks.sumOf { link -> 
            val cost = assetMap[link.assetId]?.pointCost ?: 0
            link.users * cost * 0.4 
        }
        val totalEarnings = viewsEarnings + usersEarnings + linkCostEarnings
        
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    kotlinx.coroutines.delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (filteredLinks.isEmpty() && sharingLinks.isEmpty()) {
                // Empty state with just the Earn Points strip
                Column(modifier = Modifier.fillMaxSize()) {
                    // Earn Points Strip
                    Surface(
                        onClick = { showRewardsSheet = true },
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AutoGraph,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Earn Points by Sharing Links",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "Learn how you can earn and spend points",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    EmptyState(
                        icon = Icons.Filled.Link,
                        message = "You haven't shared any links yet."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        if (!isPremium) {
                            AdsManager.AdaptiveBannerAdView()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Earn Points Strip
                    item {
                        Surface(
                            onClick = { showRewardsSheet = true },
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.AutoGraph,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Earn Points by Sharing Links",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        "Learn how you can earn and spend points",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // Performance Summary Section (no card container - directly on page)
                    if (sharingLinks.isNotEmpty()) {
                        item {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Analytics,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Insights",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MetricCard(
                                        label = "Total Links",
                                        value = "${sharingLinks.size}",
                                        icon = Icons.Filled.Link,
                                        modifier = Modifier.width(140.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    )
                                    MetricCard(
                                        label = "Total Views",
                                        value = "$totalViews",
                                        icon = Icons.Filled.Visibility,
                                        modifier = Modifier.width(140.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                    MetricCard(
                                        label = "Unique Users",
                                        value = "$totalUsers",
                                        icon = Icons.Filled.Group,
                                        modifier = Modifier.width(140.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                    MetricCard(
                                        label = "Total Earned",
                                        value = String.format("%.0f", totalEarnings),
                                        suffix = "pts",
                                        icon = Icons.Filled.Stars,
                                        modifier = Modifier.width(140.dp),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    )
                                }
                            }
                        }
                        
                        // Filter Chips
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = filterActive == null,
                                    onClick = { filterActive = null },
                                    label = { Text("All") }
                                )
                                FilterChip(
                                    selected = filterActive == true,
                                    onClick = { filterActive = true },
                                    label = { Text("Active") }
                                )
                                FilterChip(
                                    selected = filterActive == false,
                                    onClick = { filterActive = false },
                                    label = { Text("Inactive") }
                                )
                            }
                        }
                    }
                    
                    // Links list - directly in page scroll (no separate card/container)
                    if (filteredLinks.isEmpty() && sharingLinks.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No links match the filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredLinks) { link ->
                            val asset = assetMap[link.assetId]
                            SharingLinkCard(
                                link = link,
                                pointCost = asset?.pointCost ?: 0,
                                assetName = asset?.name,
                                showDetails = true,
                                onRename = { 
                                    if (!isLoggedIn) showLoginPrompt = true else renameLink = it 
                                },
                                onSetExpiry = { 
                                    if (!isLoggedIn) showLoginPrompt = true else expiryLink = it 
                                },
                                onToggleAccess = { 
                                    if (!isLoggedIn) {
                                        showLoginPrompt = true
                                    } else {
                                        val newStatus = if (it.status == "ACTIVE") "INACTIVE" else "ACTIVE"
                                        viewModel.updateSharingLink(it.copy(status = newStatus))
                                    }
                                },
                                onDelete = { 
                                    if (!isLoggedIn) showLoginPrompt = true else viewModel.deleteSharingLink(it) 
                                }
                            )
                        }
                        
                        item {
                            if (!isPremium) {
                                AdsManager.AdaptiveBannerAdView()
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Rename Link Sheet
        renameLink?.let { link ->
            RenameLinkSheet(
                link = link,
                onSave = { updatedLink ->
                    viewModel.updateSharingLink(updatedLink)
                    renameLink = null
                },
                onDismiss = { renameLink = null }
            )
        }

        // Expiry Date Sheet
        expiryLink?.let { link ->
            ExpiryDateSheet(
                link = link,
                onSave = { updatedLink ->
                    viewModel.updateSharingLink(updatedLink)
                    expiryLink = null
                },
                onDismiss = { expiryLink = null }
            )
        }
    }

    if (showWalletSheet) {
        WalletBottomSheet(
            viewModel = viewModel,
            onDismiss = { showWalletSheet = false },
            sheetState = walletSheetState
        )
    }

    if (showRewardsSheet) {
        SharingRewardsSheet(
            onDismiss = { showRewardsSheet = false },
            onSpendPoints = { showSpendPoints = true },
            sheetState = rewardsSheetState
        )
    }

    if (showSpendPoints) {
        val spendPointsState = rememberModalBottomSheetState()
        SpendPointsSheet(
            onDismiss = { showSpendPoints = false },
            sheetState = spendPointsState
        )
    }

    if (showLoginPrompt) {
        LoginRequiredSheet(
            onDismiss = { showLoginPrompt = false },
            viewModel = viewModel,
            sheetState = loginSheetState,
            message = "Login now to manage your shared links!"
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    suffix: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                if (suffix.isNotEmpty()) {
                    Text(
                        text = " $suffix",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
