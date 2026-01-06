package com.clicktoearn.linkbox.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clicktoearn.linkbox.data.local.AssetType
import com.clicktoearn.linkbox.data.remote.FirestoreAsset
import com.clicktoearn.linkbox.ui.Screen
import com.clicktoearn.linkbox.utils.findActivity
import com.clicktoearn.linkbox.utils.setScreenshotDisabled
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.clicktoearn.linkbox.ads.AdsManager

import com.clicktoearn.linkbox.ui.components.PointsHeaderButton
import com.clicktoearn.linkbox.ui.components.WalletBottomSheet
import com.clicktoearn.linkbox.ui.components.InsufficientPointsSheet
import com.clicktoearn.linkbox.ui.components.LoginRequiredSheet
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedFolderScreen(
    ownerId: String,
    folderId: String,
    title: String,
    viewModel: LinkBoxViewModel,
    navController: NavController,
    onBack: () -> Unit,
    restrictUrl: Boolean = false
) {
    var assets by remember { mutableStateOf<List<FirestoreAsset>>(emptyList()) }
    var folderAsset by remember { mutableStateOf<FirestoreAsset?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val userPoints by viewModel.userPoints.collectAsState()
    var showWalletSheet by remember { mutableStateOf(false) }
    var showInsufficientSheet by remember { mutableStateOf(false) }
    var pendingAsset by remember { mutableStateOf<FirestoreAsset?>(null) }
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var showLoginPrompt by remember { mutableStateOf(false) }
    val loginSheetState = rememberModalBottomSheetState()

    LaunchedEffect(ownerId, folderId) {
        isLoading = true
        // Fetch folder's own settings for screenshot protection
        folderAsset = viewModel.getCloudAsset(folderId)
        assets = viewModel.getSharedFolderContents(ownerId, folderId) ?: emptyList()
        isLoading = false
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            // Use Toast instead of Snackbar to ensure visibility over modals (bottom sheets)
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Screenshot blocking - Always enabled
    val effectiveExposeUrl = (folderAsset?.exposeUrl ?: true) && !restrictUrl
    
    DisposableEffect(Unit) {
        viewModel.enableScreenshotProtection()
        
        onDispose {
            viewModel.disableScreenshotProtection()
        }
    }

    if (showWalletSheet) {
        WalletBottomSheet(
            viewModel = viewModel,
            onDismiss = { showWalletSheet = false },
            sheetState = rememberModalBottomSheetState()
        )
    }

    if (showInsufficientSheet && pendingAsset != null) {
        val asset = pendingAsset!!
        val effectiveCost = viewModel.getEffectiveCost(asset.pointCost, asset.ownerId)

        
        InsufficientPointsSheet(
            currentBalance = userPoints,
            requiredPoints = effectiveCost,
            onDismiss = { 
                showInsufficientSheet = false 
                pendingAsset = null
            },
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
                showInsufficientSheet = false; 
            },
            onBuyPoints = { points ->
                showInsufficientSheet = false
                val activity = context.findActivity()
                if (activity != null) {
                    if (points > 0) {
                        viewModel.buyPoints(activity, points)
                    } else {
                        showWalletSheet = true
                    }
                } else {
                    viewModel.showMessage("Failed to start purchase flow")
                }
            }
        )
    }

    if (showLoginPrompt) {
        LoginRequiredSheet(
            onDismiss = { showLoginPrompt = false },
            viewModel = viewModel,
            sheetState = loginSheetState,
            message = "Please login before spending any points. Your local coins will be added to your account!"
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Screenshot protection always enabled - lock icon removed
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                     PointsHeaderButton(viewModel = viewModel, onClick = { showWalletSheet = true })
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        folderAsset = viewModel.getCloudAsset(folderId)
                        assets = viewModel.getSharedFolderContents(ownerId, folderId) ?: emptyList()
                        isRefreshing = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                if (isLoading && assets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (folderAsset != null && !folderAsset!!.sharingEnabled) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Sharing has been disabled for this folder", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                } else if (assets.isEmpty()) {
                     Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val userProfile = viewModel.userProfile.collectAsState().value
                            val isPremium = userProfile?.isPremium == true
                            val isOwner = viewModel.isOwner(ownerId)
                            val showNativeTop = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_FOLDER_NATIVE_TOP)

                            if (showNativeTop) {
                                AdsManager.NativeAdView()
                                Spacer(modifier = Modifier.height(64.dp))
                            }
                            
                            Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("This folder is empty", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    // Granular Ad Controls
                    val userProfile = viewModel.userProfile.collectAsState().value
                    val isPremium = userProfile?.isPremium == true
                    val isOwner = viewModel.isOwner(ownerId)
                    
                    val showNativeTop = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_FOLDER_NATIVE_TOP)
                    val showBannerBottom = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_FOLDER_BANNER_BOTTOM)
                    val showSubAssetOpenAd = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_FOLDER_AD_SUB_ASSET_OPEN)
                    val useRewardedForLink = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_FOLDER_AD_LINK_OPEN_REWARDED)


                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                           if (showNativeTop) {
                               AdsManager.NativeAdView()
                               Spacer(modifier = Modifier.height(8.dp))
                           }
                        }
                    
                        items(assets) { asset ->
                            val effectiveCost = viewModel.getEffectiveCost(asset.pointCost, asset.ownerId)
                            
                            SharedAssetItem(
                                asset = asset,
                                cost = effectiveCost,
                                onClick = {
                                    val performAccess = {
                                        // Simple navigation function
                                        val navigate = {
                                            val safeTitle = android.net.Uri.encode(asset.name)
                                            val nextRestrictUrl = !effectiveExposeUrl

                                            when (asset.type) {
                                                AssetType.FOLDER.name -> {
                                                    navController.navigate(Screen.SharedFolder.createRoute(ownerId, asset.id, safeTitle, true, nextRestrictUrl))
                                                }
                                                AssetType.FILE.name -> {
                                                    navController.navigate(Screen.SharedFile.createRoute(ownerId, asset.id, safeTitle, true))
                                                }
                                                AssetType.LINK.name -> {
                                                    val encodedUrl = android.net.Uri.encode(asset.content)
                                                    val linkExposeUrl = asset.exposeUrl && effectiveExposeUrl
                                                    navController.navigate("webview/$encodedUrl/$safeTitle/false/$linkExposeUrl")
                                                }
                                            }
                                        }

                                        if (effectiveCost > 0 && !viewModel.isOwner(asset.ownerId)) {
                                            if (userPoints >= effectiveCost) {
                                                // Check login before spending points
                                                if (!isLoggedIn) {
                                                    pendingAsset = asset
                                                    showLoginPrompt = true
                                                } else {
                                                    viewModel.payForAccess(effectiveCost, asset.ownerId, asset.name)
                                                    navigate() // Just navigate, ad already shown
                                                }
                                            } else {
                                                pendingAsset = asset
                                                showInsufficientSheet = true
                                            }
                                        } else {
                                            navigate()
                                        }
                                    }
                                    
                                    val activity = context.findActivity()
                                    // Calculate frequency check lazily at click time
                                    val checkFrequency = AdsManager.shouldShowSharedContentAd(context)
                                    android.util.Log.d("SharedFolderScreen", "AdCheck: activity=${activity!=null}, showSubAsset=$showSubAssetOpenAd, freq=$checkFrequency, isLink=${asset.type == AssetType.LINK.name}, useRewarded=$useRewardedForLink")

                                    if (activity != null && showSubAssetOpenAd && checkFrequency) {
                                        val isLink = asset.type == AssetType.LINK.name
                                        if (isLink && useRewardedForLink) {
                                            AdsManager.showRewardedInterstitialAd(activity, placementKey = "shared_folder_${asset.id}", strict = false, onRewardEarned = {
                                                performAccess()
                                            }, onAdClosed = { message ->
                                                message?.let { viewModel.showMessage(it) }
                                            })
                                        } else {
                                            AdsManager.showInterstitialAd(activity, placementKey = "shared_folder_${asset.id}") {
                                                performAccess()
                                            }
                                        }
                                    } else {
                                        performAccess()
                                    }
                                }
                            )
                        }

                        item {
                            if (showBannerBottom) {
                                AdsManager.AdaptiveBannerAdView()
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SharedAssetItem(
    asset: FirestoreAsset,
    cost: Int,
    onClick: () -> Unit
) {
    val icon = when (asset.type) {
        AssetType.FILE.name -> Icons.Filled.Description
        AssetType.FOLDER.name -> Icons.Filled.Folder
        AssetType.LINK.name -> Icons.Filled.Link
        else -> Icons.Filled.Description
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = asset.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = asset.type.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (cost > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                         Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = cost.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
