package com.clicktoearn.linkbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.clicktoearn.linkbox.ui.components.PointsHeaderButton
import com.clicktoearn.linkbox.ui.components.WalletBottomSheet
import com.clicktoearn.linkbox.ui.components.InsufficientPointsSheet
import com.clicktoearn.linkbox.ui.components.SharedContentShimmer
import com.clicktoearn.linkbox.ui.components.LoginRequiredSheet
import com.clicktoearn.linkbox.ui.components.PremiumUnlockCard
import com.clicktoearn.linkbox.utils.findActivity
import com.clicktoearn.linkbox.utils.setScreenshotDisabled
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import androidx.navigation.NavController
import com.clicktoearn.linkbox.data.local.AssetType
import kotlinx.coroutines.launch

import com.clicktoearn.linkbox.ads.AdsManager

import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.ExperimentalComposeUiApi
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DeepLinkScreen(
    token: String,
    isNewInstall: Boolean = false,
    viewModel: LinkBoxViewModel, 
    navController: NavController,
    onBack: () -> Unit,
    onOpenUrl: (url: String, title: String, allowScreenCapture: Boolean, exposeUrl: Boolean) -> Unit,
    onOpenFolder: (ownerId: String, folderId: String, title: String) -> Unit,
    onOpenFile: (ownerId: String, assetId: String, title: String) -> Unit
) {
    val sharedContent by viewModel.sharedContent.collectAsState()
    val isLoading by viewModel.isLoadingShared.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isPremium = userProfile?.isPremium == true
    val isOwner = sharedContent?.ownerId == currentUserId
    
    // Granular Ad Controls
    // Granular Ad Controls
    val showNativeAboveInfo = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_CONTENT_NATIVE_ABOVE_INFO)
    val showBannerAboveOpenBtn = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_CONTENT_BANNER_ABOVE_OPEN_BTN)
    val showNativeBelowOpenBtn = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_CONTENT_NATIVE_BELOW_OPEN_BTN)
    val useRewardedInterstitial = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_CONTENT_USE_REWARDED_INTERSTITIAL)
    val context = LocalContext.current
    val showAssetOpenAd = !isPremium && !isOwner && 
                            AdsManager.isAdEnabled(AdsManager.KEY_SHARED_CONTENT_AD_ASSET_OPEN)
    val useRewardedForLink = !isPremium && !isOwner && AdsManager.isAdEnabled(AdsManager.KEY_SHARED_CONTENT_AD_LINK_OPEN_REWARDED)
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isUnlocked by remember { mutableStateOf(false) }
    var showWalletSheet by remember { mutableStateOf(false) }
    var showInsufficientSheet by remember { mutableStateOf(false) }
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var showLoginPrompt by remember { mutableStateOf(false) }
    val loginSheetState = rememberModalBottomSheetState()
    
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var loginPromptMessage by remember { mutableStateOf("Join Private Files to save your links and earn rewards!") }
    val userPoints by viewModel.userPoints.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    var showHistoryStrip by remember { mutableStateOf(true) }

    // Screenshot blocking - Always enabled
    DisposableEffect(Unit) {
        viewModel.enableScreenshotProtection()
        
        onDispose {
            viewModel.disableScreenshotProtection()
        }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) isRefreshing = false
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // History item to track starred status
    val history by viewModel.history.collectAsState(initial = emptyList())
    val currentHistoryItem = history.find { it.token == token }
    val isStarred = currentHistoryItem?.isStarred == true

    // Track if we have initiated the load request to prevent premature "Not Found" state
    var hasStartedLoading by remember { mutableStateOf(false) }
    var showNotFoundState by remember { mutableStateOf(false) }

    // Logic to delay the "Not Found" state to prevent flicker
    LaunchedEffect(isLoading, sharedContent, hasStartedLoading) {
        if (!isLoading && sharedContent == null && hasStartedLoading) {
            // Wait slightly longer before showing error (cold starts can be slow)
            kotlinx.coroutines.delay(2000) 
            if (!isLoading && sharedContent == null) {
                showNotFoundState = true
            }
        } else {
            showNotFoundState = false
        }
    }

    LaunchedEffect(token) {
        viewModel.loadSharedContent(token)
        hasStartedLoading = true
    }
    
    // Track referral when content is successfully loaded
    LaunchedEffect(sharedContent) {
        if (sharedContent != null) {
            viewModel.trackReferral(token, isNewInstall)
            com.clicktoearn.linkbox.analytics.AnalyticsManager.logSharedContentOpened(token, isNewInstall)
        }
    }

    LaunchedEffect(currentHistoryItem, sharedContent) {
        val content = sharedContent
        val historyItem = currentHistoryItem
        if (content != null) {
            val effectiveCost = viewModel.getEffectiveCost(content.pointCost, content.ownerId)
            
            if (isOwner || effectiveCost == 0) {
                isUnlocked = true
            } else if (!content.chargeEveryTime && historyItem?.isPaid == true) {
                isUnlocked = true
            }
        }
    }

    if (showWalletSheet) {
        WalletBottomSheet(
            viewModel = viewModel,
            onDismiss = { showWalletSheet = false },
            sheetState = rememberModalBottomSheetState()
        )
    }

    if (showInsufficientSheet) {
        val content = sharedContent!!
        val effectiveCost = viewModel.getEffectiveCost(content.pointCost, content.ownerId)
        
        InsufficientPointsSheet(
            currentBalance = userPoints,
            requiredPoints = effectiveCost,
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
                showInsufficientSheet = false; 
                // Navigate/Action for earning points if possible, or just dismiss
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Shared Content") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    PointsHeaderButton(viewModel = viewModel, onClick = { showWalletSheet = true })
                    
                    IconButton(onClick = { showOptionsSheet = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = padding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Strip for Saved to History
            if (sharedContent != null && showHistoryStrip) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(com.clicktoearn.linkbox.ui.Screen.History.route) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Saved to History",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "View History",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(
                                onClick = { showHistoryStrip = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider()
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { 
                    isRefreshing = true
                    viewModel.refreshSharedContent(token) 
                },
                modifier = Modifier.weight(1f)
            ) {
                // Determine if we should show the "Not Found" state
                if (sharedContent == null && !showNotFoundState) {
                    // Show shimmer loading skeleton while loading OR during the grace period
                    SharedContentShimmer(
                        showNativeAboveInfo = showNativeAboveInfo,
                        showNativeBelowOpenBtn = showNativeBelowOpenBtn
                    )
                } else if (showNotFoundState) {
                    // Only show after the 2s grace period defined in the LaunchedEffect above
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp, top = 24.dp, end = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Top Native Ad for Not Found page
                        if (showNativeAboveInfo) {
                            AdsManager.NativeAdView()
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Icon(
                            imageVector = Icons.Filled.LinkOff, 
                            contentDescription = null, 
                            modifier = Modifier.size(80.dp), 
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Text(
                            text = "Content not found or link expired.", 
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "The link may have been deleted by the owner or it has reached its expiration date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { 
                                isRefreshing = true
                                viewModel.refreshSharedContent(token) 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Loading")
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Bottom Native Ad for Not Found page
                        if (showNativeBelowOpenBtn) {
                            AdsManager.NativeAdView()
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp + padding.calculateBottomPadding()))
                    }
                } else if (sharedContent != null) {
                    val content = sharedContent!!
                    val effectiveCost = viewModel.getEffectiveCost(content.pointCost, content.ownerId)
                    
                    // Main Content Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 24.dp, end = 24.dp, top = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hero: Native Ad
                        if (showNativeAboveInfo) {
                            AdsManager.NativeAdView()
                        }



                        // 1. File/Info Section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (content.assetType) {
                                    AssetType.FOLDER.name -> Icons.Filled.Folder
                                    AssetType.LINK.name -> Icons.Filled.Link
                                    else -> Icons.Filled.Description
                                },
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = content.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            // Screenshot protection always enabled - lock icon removed
                        }
                        
                        if (content.description.isNotBlank()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Description",
                                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = content.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // 2. Metadata Section
                        val typeName = content.assetType.lowercase().replaceFirstChar { it.uppercase() }
                        val costText = if (effectiveCost > 0) "With Access Cost of $effectiveCost Points" else "For Free"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Owner",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildAnnotatedString {
                                    append("$typeName Shared By ")
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                                        append(content.ownerName)
                                    }
                                    append(" $costText")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Adaptive Banner before Open button
                        if (showBannerAboveOpenBtn) {
                            AdsManager.AdaptiveBannerAdView()
                        }

                        // 3. Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reverted to Normal Button
                            Button(
                                onClick = {
                                    if (isUnlocked) {
                                        // Open content logic
                                        val activity = context.findActivity()
                                        val action = {
                                            when (content.assetType) {
                                                AssetType.LINK.name -> onOpenUrl(content.assetContent, content.name, content.allowScreenCapture, content.exposeUrl)
                                                AssetType.FOLDER.name -> onOpenFolder(content.ownerId, content.assetId, content.name)
                                                else -> onOpenFile(content.ownerId, content.assetId, content.name)
                                            }
                                        }

                                        // Use Granular control for Open button ad
                                        // Check frequency strictness for the *action* of opening
                                        val shouldShowAdByFrequency = AdsManager.shouldShowSharedContentAd(context)
                                        
                                        if (activity != null && showAssetOpenAd && shouldShowAdByFrequency) {
                                            val isLink = content.assetType == AssetType.LINK.name
                                            val finalUseRewarded = (isLink && useRewardedForLink) || useRewardedInterstitial
                                            
                                            if (finalUseRewarded) {
                                                AdsManager.showRewardedInterstitialAd(activity, placementKey = "shared_$token", strict = false, onRewardEarned = {
                                                    action()
                                                }, onAdClosed = { message ->
                                                    message?.let { viewModel.showMessage(it) }
                                                })
                                            } else {
                                                AdsManager.showInterstitialAd(activity, placementKey = "shared_$token") {
                                                    action()
                                                }
                                            }
                                        } else {
                                            action()
                                        }
                                        // If it's every-time charge, reset unlock status for next time screen is visited
                                        if (content.chargeEveryTime && !isOwner && effectiveCost > 0) {
                                            isUnlocked = false
                                        }
                                    } else {
                                        // Locked Content Logic: Ad First -> Then Payment/Unlock
                                        val activity = context.findActivity()
                                        
                                        val attemptUnlock = {
                                            // Check balance
                                            if (userPoints >= effectiveCost) {
                                                if (!isLoggedIn) {
                                                    loginPromptMessage = "Please login before spending any points. Your local coins will be added to your account!"
                                                    showLoginPrompt = true
                                                } else {
                                                    viewModel.payForAccess(effectiveCost, content.ownerId, content.name)
                                                    if (!content.chargeEveryTime) {
                                                        viewModel.markAsPaid(token)
                                                    }
                                                    isUnlocked = true
                                                    scope.launch { viewModel.showMessage("Content Unlocked!") }
                                                    
                                                    // Open immediately (Ad already watched)
                                                    when (content.assetType) {
                                                        AssetType.LINK.name -> onOpenUrl(content.assetContent, content.name, content.allowScreenCapture, content.exposeUrl)
                                                        AssetType.FOLDER.name -> onOpenFolder(content.ownerId, content.assetId, content.name)
                                                        else -> onOpenFile(content.ownerId, content.assetId, content.name)
                                                    }
                                                    
                                                    // If it's every-time charge, reset unlock status for next time screen is visited
                                                    if (content.chargeEveryTime && !isOwner && effectiveCost > 0) {
                                                        isUnlocked = false
                                                    }
                                                }
                                            } else {
                                                showInsufficientSheet = true
                                            }
                                        }

                                        // Show Unlock Ad First
                                        // Show Unlock Ad First (Always Interstitial for unlocking to avoid user fatigue)
                                        if (activity != null && showAssetOpenAd) {
                                            AdsManager.showInterstitialAd(activity, placementKey = "shared_$token") {
                                                attemptUnlock()
                                            }
                                        } else {
                                            attemptUnlock()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                val actionText = when (content.assetType) {
                                    AssetType.FOLDER.name -> "Open Folder"
                                    AssetType.LINK.name -> "Open Link"
                                    else -> "Open File"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(actionText, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }

                            // Star Button - Simple icon only, no background/borders
                            if (!isOwner) {
                                IconButton(
                                    onClick = {
                                        // Use toggleStarByToken which works even if no history item exists
                                        val newStarred = viewModel.toggleStarByToken(token, isStarred)
                                        scope.launch {
                                            viewModel.showMessage(
                                                if (newStarred) "Added to Starred" else "Removed from Starred"
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isStarred) Icons.Filled.Star else Icons.Default.StarBorder,
                                        contentDescription = if (isStarred) "Remove from Starred" else "Add to Starred",
                                        tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                        
                        // Native Ad after Open button
                        if (showNativeBelowOpenBtn) {
                            AdsManager.NativeAdView()
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        // 4. Disclaimer
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Disclaimer",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This content is owned by ${content.ownerName}. Report if you find it problematic.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                            TextButton(
                                onClick = { showReportSheet = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                            Text("Report Content", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp + padding.calculateBottomPadding()))
                }
            }
        }
    }
}

    // Bottom Sheet for Options Menu
    if (showOptionsSheet && sharedContent != null) {
        val content = sharedContent!!
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Save Copy Option
                ListItem(
                    headlineContent = { Text("Save Copy") },
                    leadingContent = { Icon(Icons.Filled.Save, contentDescription = null) },
                    trailingContent = if (!content.allowSaveCopy) {
                        { Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else null,
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        headlineColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.clickable {
                        if (!isLoggedIn) {
                            loginPromptMessage = "Login now to save content and share links!"
                            showLoginPrompt = true
                        } else {
                            showOptionsSheet = false
                            if (content.allowSaveCopy) {
                                when(content.assetType) {
                                    AssetType.LINK.name -> viewModel.addLink(content.name, content.assetContent, null)
                                    AssetType.FOLDER.name -> viewModel.addFolder(content.name, null)
                                    else -> viewModel.addFile(content.name, content.assetContent, null)
                                }
                                scope.launch { viewModel.showMessage("Saved to My Files") }
                            } else {
                                scope.launch { viewModel.showMessage("Owner has disabled saving for this content") }
                            }
                        }
                    }
                )

                // Copy URL Option (Link Only)
                if (content.assetType == AssetType.LINK.name) {
                    ListItem(
                        headlineContent = { Text("Copy URL") },
                        leadingContent = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        trailingContent = if (!content.exposeUrl) {
                            { Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else null,
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            headlineColor = MaterialTheme.colorScheme.onSurface,
                            leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.clickable {
                            showOptionsSheet = false
                            if (content.exposeUrl) {
                                com.clicktoearn.linkbox.utils.copyToClipboard(context, content.assetContent, "Link URL")
                                scope.launch { viewModel.showMessage("URL copied to clipboard") }
                            } else {
                                scope.launch { viewModel.showMessage("Owner has disabled exposing the original URL") }
                            }
                        }
                    )
                }

                // Share Option
                ListItem(
                    headlineContent = { Text("Share") },
                    leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                    trailingContent = if (!content.allowFurtherSharing && !(content.assetType == AssetType.LINK.name && content.exposeUrl)) {
                        { Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else null,
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        headlineColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.clickable {
                        if (!isLoggedIn) {
                            loginPromptMessage = "Login now to share links with others!"
                            showLoginPrompt = true
                        } else {
                            showOptionsSheet = false
                            // allowFurtherSharing: Share LinkBox link
                            // exposeUrl: Share original URL (for links)
                            if (content.allowFurtherSharing || (content.assetType == AssetType.LINK.name && content.exposeUrl)) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    val textToShare = if (content.allowFurtherSharing) {
                                        val shareUrl = "https://privacy.be6.in/$token"
                                        "Check this out: $shareUrl"
                                    } else {
                                        // Protect LinkBox link, share original URL instead
                                        "Original Link: ${content.assetContent}"
                                    }
                                    putExtra(Intent.EXTRA_TEXT, textToShare)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                            } else {
                                val message = if (!content.allowFurtherSharing && !(content.assetType == AssetType.LINK.name && content.exposeUrl)) {
                                    "Owner has disabled all sharing"
                                } else {
                                    "Sharing restricted by owner"
                                }
                                scope.launch { viewModel.showMessage(message) }
                            }
                        }
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Report Option
                ListItem(
                    headlineContent = { Text("Report Content") },
                    leadingContent = { Icon(Icons.Filled.Report, contentDescription = null) },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        headlineColor = MaterialTheme.colorScheme.error,
                        leadingIconColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.clickable {
                        showOptionsSheet = false
                        showReportSheet = true
                    }
                )
            }
        }
    }

    if (showReportSheet) {
        ReportContentSheet(
            onDismiss = { showReportSheet = false },
            onSubmit = { reason, contact ->
                scope.launch { 
                    viewModel.submitReport(token, reason, contact)
                    viewModel.showMessage("Report submitted. We will review it shortly.") 
                }
                showReportSheet = false
            }
        )
    }

    if (showLoginPrompt) {
        LoginRequiredSheet(
            onDismiss = { showLoginPrompt = false },
            viewModel = viewModel,
            sheetState = loginSheetState,
            message = loginPromptMessage
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContentSheet(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp) // Extra padding for navigation bar
        ) {
            Text(
                "Report Content",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for reporting") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Contact Details (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Email or Phone") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { onSubmit(reason, contact) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = reason.isNotBlank()
            ) {
                Text("Submit Report")
            }
        }
    }
}


@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
