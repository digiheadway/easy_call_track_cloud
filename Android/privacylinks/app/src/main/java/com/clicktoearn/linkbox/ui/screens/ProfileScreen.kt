package com.clicktoearn.linkbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.clicktoearn.linkbox.ui.components.PointsHeaderButton
import com.clicktoearn.linkbox.ui.components.WalletBottomSheet
import com.clicktoearn.linkbox.ui.components.InsufficientPointsSheet
import com.clicktoearn.linkbox.ui.components.ComingSoonSheet
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.ui.components.LinkBoxBottomSheet
import com.clicktoearn.linkbox.ui.components.LoginRequiredSheet
import com.clicktoearn.linkbox.ui.components.PremiumSubscriptionSheet
import com.clicktoearn.linkbox.data.local.UserEntity

import androidx.navigation.NavController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: LinkBoxViewModel, navController: NavController) {
    val userProfile by viewModel.userProfile.collectAsState()
    
    var showEditProfile by remember { mutableStateOf(false) }
    var showSubscription by remember { mutableStateOf(false) }
    var showInsufficientPointsSheet by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showWalletSheet by remember { mutableStateOf(false) }
    var showComingSoon by remember { mutableStateOf(false) }
    val pointsSheetState = rememberModalBottomSheetState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var showLoginPrompt by remember { mutableStateOf(false) }
    val loginSheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // Show logged-out UI if no profile
    if (userProfile == null) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile") },
                    actions = {
                        PointsHeaderButton(viewModel) { showWalletSheet = true }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // Login Prompt Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    onClick = { showLoginPrompt = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Sign In",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "Sync your data, earn points, and unlock features",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Watch Ad Card (for non-logged in users too)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    onClick = {
                        activity?.let { act ->
                            com.clicktoearn.linkbox.ads.AdsManager.showRewardedAd(act, strict = true, 
                                onRewardEarned = {
                                    viewModel.earnPoints(5)
                                },
                                onAdClosed = { message ->
                                    message?.let { viewModel.showMessage(it) }
                                }
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Watch Ad to Earn Points",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "Get 5 points instantly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            "+5",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subscription Card (for non-logged in users too)
                val darkBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF141414),
                        Color(0xFF2E2E2E)
                    )
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(darkBrush)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700), // Gold
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Remove Ad for 7 Days",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Remove ad-free experience in the app.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showSubscription = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFD700), // Gold
                                    contentColor = Color.Black
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Buy Now", style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Settings available without login
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                val isDarkMode by viewModel.isDarkMode.collectAsState()
                ProfileMenuItem(
                    icon = Icons.Filled.DarkMode,
                    label = "Appearance (Dark Mode)",
                    trailing = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    }
                ) {
                    viewModel.toggleDarkMode()
                }
                ProfileMenuItem(icon = Icons.Filled.Info, label = "About") {
                    showAbout = true
                }
                ProfileMenuItem(icon = Icons.Filled.Security, label = "Privacy Policy") {
                    val url = URLEncoder.encode("https://privacy.be6.in/privacy-policy.html", StandardCharsets.UTF_8.toString())
                    navController.navigate("webview/$url/Privacy%20Policy/true/true")
                }
                ProfileMenuItem(icon = Icons.Filled.Description, label = "Terms of Service") {
                    val url = URLEncoder.encode("https://privacy.be6.in/terms-of-usage.html", StandardCharsets.UTF_8.toString())
                    navController.navigate("webview/$url/Terms%20of%20Service/true/true")
                }
                }
            }
        }
    } else {
        // Logged-in UI
        val profile = userProfile!!

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Profile") },
                    actions = {
                        PointsHeaderButton(viewModel) { showWalletSheet = true }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                // User Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Photo
                        if (profile.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = profile.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = profile.username,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Points: ${profile.points}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showWalletSheet = true }
                            )
                            if (profile.isPremium) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = MaterialTheme.shapes.extraSmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "PREMIUM",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { 
                            if (!isLoggedIn) {
                                showLoginPrompt = true
                            } else {
                                showEditProfile = true 
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))

                // Subscription Card
                if (!profile.isPremium) {
                    // Theme Gradient for Premium
                    val darkBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF141414),
                            Color(0xFF2E2E2E)
                        )
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(darkBrush)
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700), // Gold
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Remove Ad for 7 Days",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Remove ad-free experience in the app.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { showSubscription = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFD700), // Gold
                                        contentColor = Color.Black
                                    ),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Buy Now", style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Premium Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            profile.premiumExpiry?.let { expiry ->
                                val date = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(expiry))
                                Text(
                                    text = "Expires on: $date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ProfileMenuItem(
                    icon = Icons.Filled.DarkMode,
                    label = "Appearance (Dark Mode)",
                    trailing = {
                        Switch(
                            checked = profile.isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    }
                ) {
                    viewModel.toggleDarkMode()
                }
                ProfileMenuItem(icon = Icons.Filled.Info, label = "About") {
                    showAbout = true
                }
                ProfileMenuItem(icon = Icons.Filled.Security, label = "Privacy Policy") {
                    val url = URLEncoder.encode("https://privacy.be6.in/privacy-policy.html", StandardCharsets.UTF_8.toString())
                    navController.navigate("webview/$url/Privacy%20Policy/true/true")
                }
                ProfileMenuItem(icon = Icons.Filled.Description, label = "Terms of Service") {
                    val url = URLEncoder.encode("https://privacy.be6.in/terms-of-usage.html", StandardCharsets.UTF_8.toString())
                    navController.navigate("webview/$url/Terms%20of%20Service/true/true")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = "Logout",
                    color = MaterialTheme.colorScheme.error
                ) {
                    showLogoutConfirm = true
                }
            }
        }
        }

        if (showEditProfile) {
            EditProfileSheet(
                user = profile,
                onDismiss = { showEditProfile = false },
                onUpdate = { newName ->
                    viewModel.updateUsername(newName)
                    showEditProfile = false
                }
            )
        }



        if (showLogoutConfirm) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirm = false },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.logout(context)
                        showLogoutConfirm = false
                    }) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

    }

    // Common bottom sheets for both logged-in and logged-out
    if (showWalletSheet) {
        WalletBottomSheet(
            viewModel = viewModel,
            onDismiss = { showWalletSheet = false },
            sheetState = pointsSheetState
        )
    }

    if (showAbout) {
        AboutSheet(onDismiss = { showAbout = false })
    }

    if (showSubscription) {
        PremiumSubscriptionSheet(
            onDismiss = { showSubscription = false },
            onSubscribe = { useCoins ->
                if (useCoins) {
                    viewModel.subscribe(useCoins = true)
                } else {
                    activity?.let { viewModel.buyPremium(it) }
                }
                showSubscription = false
            },
            currentPoints = userProfile?.points ?: 0
        )
    }

    if (showInsufficientPointsSheet) {
        InsufficientPointsSheet(
            currentBalance = userProfile?.points ?: 0,
            requiredPoints = 100,
            onDismiss = { showInsufficientPointsSheet = false },
            isLoggedIn = isLoggedIn,
            viewModel = viewModel,
            onWatchAd = { 
                activity?.let { act ->
                    com.clicktoearn.linkbox.ads.AdsManager.showRewardedAd(act, strict = true, 
                        onRewardEarned = {
                            viewModel.earnPoints(5) 
                            showInsufficientPointsSheet = false
                        },
                        onAdClosed = { message ->
                            message?.let { viewModel.showMessage(it) }
                        }
                    )
                }
            },
            onEarnPoints = { 
                showInsufficientPointsSheet = false
                navController.navigate(com.clicktoearn.linkbox.ui.Screen.MyLinks.route)
            },
            onBuyPoints = { points ->
                showInsufficientPointsSheet = false
                activity?.let { act ->
                    if (points > 0) {
                        viewModel.buyPoints(act, points)
                    } else {
                        showWalletSheet = true
                    }
                }
            }
        )
    }

    if (showLoginPrompt) {
        LoginRequiredSheet(
            onDismiss = { showLoginPrompt = false },
            viewModel = viewModel,
            sheetState = loginSheetState,
            message = "Login now to fully manage your profile and unlock features!"
        )
    }

    if (showComingSoon) {
        ComingSoonSheet(
            onDismiss = { showComingSoon = false },
            title = "Premium Subscription",
            message = "Paid subscription service is coming soon! We're working hard to bring you premium features."
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color)
            Spacer(modifier = Modifier.weight(1f))
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(user: UserEntity, onDismiss: () -> Unit, onUpdate: (String) -> Unit) {
    var name by remember { mutableStateOf(user.username) }
    val sheetState = rememberModalBottomSheetState()

    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = "Edit Username"
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onUpdate(name) },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank()
        ) {
            Text("Update Username")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionSheet(onDismiss: () -> Unit, onSubscribe: (Boolean) -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = "Purchase Remove Ads"
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "Choose your preferred way to unlock 7 days of ad-free experience.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Points Option
            Surface(
                onClick = { onSubscribe(true) },
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Buy With Points", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Instant activation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                    Text("2000 Pts", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Money Option
            Surface(
                onClick = { onSubscribe(false) },
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Buy With Money", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Support the app", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                    Text("₹ 199", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()

    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = "About"
    ) {
        Text(
            "Private Files is a privacy-first link sharing and storage solution. " +
            "Share files, folders, and links securely with point-based access control.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Version: ${com.clicktoearn.linkbox.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
        Text("Developed by ClickToEarn Team", style = MaterialTheme.typography.bodySmall)
    }
}
