package com.clicktoearn.linkbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicktoearn.linkbox.ui.theme.*
import com.clicktoearn.linkbox.ui.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.data.repository.AppSettingsRepository
import com.clicktoearn.linkbox.ui.components.AuthenticationDialog
import com.clicktoearn.linkbox.ui.components.LinkBoxBottomSheet
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: LinkBoxViewModel) {
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isSubscriptionActive by viewModel.isSubscriptionActive.collectAsState()
    val subscriptionExpiryTime by viewModel.subscriptionExpiryTime.collectAsState()
    val showSubscriptionDialog by viewModel.showSubscriptionDialog.collectAsState()
    val authState by viewModel.authState.collectAsState()
    
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Section
            item {
                SettingsSectionHeader("Profile")
                if (authState == null) {
                    // Not Logged In State
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAuthDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountCircle, 
                                contentDescription = null, 
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Sign In to Sync", 
                                    fontWeight = FontWeight.Bold, 
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Backup your links and access them anywhere", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                } else {
                    // Logged In State
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = userName.take(1).uppercase(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(userName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { showEditProfileDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            // Sign Out Button
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                            TextButton(
                                onClick = { viewModel.signOut() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign Out", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Subscription Section
            item {
                SettingsSectionHeader("Subscription")
                PremiumSubscriptionCard(
                    isActive = isSubscriptionActive,
                    expiryTime = subscriptionExpiryTime ?: 0L,
                    onSubscribeClick = { viewModel.openSubscriptionDialog() },
                    onExtendClick = { viewModel.openSubscriptionDialog() }
                )
            }

            // Appearance Section
            item {
                SettingsSectionHeader("Appearance")
                SettingsToggleItem(
                    icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    title = "Dark Mode",
                    subtitle = "Switch between light and dark themes",
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )
            }

            // App Section
            item {
                SettingsSectionHeader("App")
                SettingsClickItem(
                    icon = Icons.Default.Info,
                    title = "About LinkBox",
                    subtitle = "Version 1.0.0",
                    onClick = { showAboutDialog = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Click to view",
                    onClick = { /* TODO */ }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsClickItem(
                    icon = Icons.Default.Description,
                    title = "Terms of Service",
                    subtitle = "Click to view",
                    onClick = { /* TODO */ }
                )
            }

            // Data Section
            if (authState != null) {
                item {
                    SettingsSectionHeader("Data")
                    SettingsClickItem(
                        icon = Icons.Default.CloudSync,
                        title = "Sync Data to Cloud",
                        subtitle = "Backup local data to Firebase",
                        onClick = { viewModel.syncNow() }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Made with ❤️ by Pokipro",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // About Sheet
    if (showAboutDialog) {
        LinkBoxBottomSheet(
            onDismissRequest = { showAboutDialog = false },
            title = "About LinkBox"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("LinkBox v1.0.0", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("A powerful link management and sharing platform.", style = MaterialTheme.typography.bodyMedium)
                }
                
                Text(
                    "© 2024 LinkBox. All rights reserved.", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = { showAboutDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
    
    // Edit Profile Sheet
    if (showEditProfileDialog) {
        var editedName by remember(userName) { mutableStateOf(userName) }
        var editedEmail by remember(userEmail) { mutableStateOf(userEmail) }
        
        LinkBoxBottomSheet(
            onDismissRequest = { showEditProfileDialog = false },
            title = "Edit Profile"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editedEmail,
                    onValueChange = { editedEmail = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.updateUserProfile(editedName, editedEmail)
                        showEditProfileDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
    
    // Subscription Purchase Sheet
    if (showSubscriptionDialog) {
        SubscriptionPurchaseSheet(
            isCurrentlySubscribed = isSubscriptionActive,
            expiryTime = subscriptionExpiryTime ?: 0L,
            onDismiss = { viewModel.closeSubscriptionDialog() },
            onPurchase = {
                if (isSubscriptionActive) {
                    viewModel.extendSubscription()
                } else {
                    viewModel.purchaseSubscription()
                }
            }
        )
    }
    
    // Auth Dialog
    if (showAuthDialog) {
        AuthenticationDialog(
            onDismiss = { showAuthDialog = false },
            onSignIn = { email, password ->
                viewModel.signIn(
                    email, 
                    password, 
                    onSuccess = { showAuthDialog = false },
                    onError = { errorMessage -> 
                        // Error handling is inside the dialog now via state or could be a snackbar here
                    }
                )
            },
            onSignUp = { email, password, name ->
                viewModel.signUp(
                    email, 
                    password, 
                    name,
                    onSuccess = { showAuthDialog = false },
                    onError = { errorMessage ->
                         // Error handling
                    }
                )
            },
            onAnonymousSignIn = {
                viewModel.signInAnonymously(
                    onSuccess = { showAuthDialog = false },
                    onError = { errorMessage ->
                        // Error handling
                    }
                )
            }
        )
    }
}

@Composable
fun PremiumSubscriptionCard(
    isActive: Boolean,
    expiryTime: Long,
    onSubscribeClick: () -> Unit,
    onExtendClick: () -> Unit
) {
    val currentTime = System.currentTimeMillis()
    val remainingTime = if (expiryTime > currentTime) expiryTime - currentTime else 0L
    val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingTime)
    val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingTime) % 24
    
    val gradientColors = if (isActive) {
        listOf(PrimaryDark, PurpleStart, Color(0xFFA855F7))
    } else {
        listOf(Color(0xFF374151), Color(0xFF4B5563), InactiveGray)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradientColors))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isActive) Icons.Default.WorkspacePremium else Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isActive) "Premium Active" else "Go Premium",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isActive) {
                                "Ad-free experience enabled"
                            } else {
                                "Remove all interstitial & banner ads"
                            },
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                    
                    if (!isActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${AppSettingsRepository.SUBSCRIPTION_PRICE}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isActive) {
                    // Show remaining time
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Time Remaining",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (remainingDays > 0) {
                                        "$remainingDays days, $remainingHours hours"
                                    } else {
                                        "$remainingHours hours remaining"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Extend button
                    Button(
                        onClick = onExtendClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = PrimaryDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extend for 199 (7 days)", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Features list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumFeatureItem("No interstitial ads")
                        PremiumFeatureItem("No banner ads")
                        PremiumFeatureItem("7 days ad-free experience")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Subscribe button
                    Button(
                        onClick = onSubscribeClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Subscribe Now - 199/week", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumFeatureItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPurchaseSheet(
    isCurrentlySubscribed: Boolean,
    expiryTime: Long,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val currentTime = System.currentTimeMillis()
    val newExpiryTime = if (isCurrentlySubscribed && expiryTime > currentTime) {
        expiryTime + AppSettingsRepository.SUBSCRIPTION_DURATION_MS
    } else {
        currentTime + AppSettingsRepository.SUBSCRIPTION_DURATION_MS
    }
    
    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isCurrentlySubscribed) "Extend Premium" else "Go Premium"
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isCurrentlySubscribed) {
                    "Extend your ad-free experience for 7 more days"
                } else {
                    "Enjoy LinkBox without any interruptions from ads"
                },
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Price card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${AppSettingsRepository.SUBSCRIPTION_PRICE}",
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "for 7 days",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
            
            // Benefits
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubscriptionBenefitItem(icon = Icons.Default.Block, text = "No interstitial ads")
                SubscriptionBenefitItem(icon = Icons.Default.HideImage, text = "No banner ads")
                SubscriptionBenefitItem(icon = Icons.Default.Speed, text = "Faster browsing")
            }
            
            Text(
                text = "Valid until: ${dateFormat.format(Date(newExpiryTime))}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryDark
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isCurrentlySubscribed) "Extend Now" else "Subscribe Now",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SubscriptionBenefitItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = SuccessStart,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
