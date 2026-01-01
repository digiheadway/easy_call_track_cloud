package com.calltracker.manager.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permission: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var showPermissionsModal by remember { mutableStateOf(false) }
    
    // Welcome Screen UI
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Cloud,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                "Welcome to CallCloud",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                "Making Calls Data Easy",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Feature Points
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val features = listOf("Organise Calls", "Call & Person Notes", "Meaningfull Reports")
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            feature,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(48.dp))
            
            Button(
                onClick = { showPermissionsModal = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Setup Now", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            TextButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showPermissionsModal) {
        PermissionsDialog(
            onDismiss = { showPermissionsModal = false },
            onAllFinished = {
                showPermissionsModal = false
                onComplete()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsDialog(
    onDismiss: () -> Unit,
    onAllFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTrackSimModal by remember { mutableStateOf(false) }
    
    val settingsViewModel: com.calltracker.manager.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by settingsViewModel.uiState.collectAsState()

    if (showTrackSimModal) {
        com.calltracker.manager.ui.settings.TrackSimModal(
            uiState = uiState,
            viewModel = settingsViewModel,
            onDismiss = onAllFinished // Proceed to finish after SIM setup
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Total list of permissions the app needs
    val allPages = remember {
        buildList {
            add(OnboardingPage("Call Logs", "Organise your call logs effectively.", Icons.Default.Call, Manifest.permission.READ_CALL_LOG))
            add(OnboardingPage("Contacts", "Identify callers by name.", Icons.Default.People, Manifest.permission.READ_CONTACTS))
            add(OnboardingPage("Phone State", "Quickly show new calls.", Icons.Default.Phone, Manifest.permission.READ_PHONE_STATE))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(OnboardingPage("Phone Numbers", "Identify SIM ownership.", Icons.Default.FormatListNumbered, Manifest.permission.READ_PHONE_NUMBERS))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(OnboardingPage("Notifications", "Alerts for sync status.", Icons.Default.Notifications, Manifest.permission.POST_NOTIFICATIONS))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(OnboardingPage("Audio Recordings", "Access recording files.", Icons.Default.AudioFile, Manifest.permission.READ_MEDIA_AUDIO))
            } else {
                add(OnboardingPage("Storage", "Access recording files.", Icons.Default.Storage, Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    // State to track which permissions are currently granted in the system
    var permissionStatuses by remember { mutableStateOf(mapOf<String, Boolean>()) }
    // State to track which permissions we are currently showing in the UI
    var visiblePermissions by remember { mutableStateOf(allPages.mapNotNull { it.permission }) }
    // State to track permissions that just got granted to show the "Done" animation
    var recentlyGranted by remember { mutableStateOf(setOf<String>()) }

    val updateStatuses = {
        val newStatuses = mutableMapOf<String, Boolean>()
        allPages.forEach { page ->
            page.permission?.let { perm ->
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, perm
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                newStatuses[perm] = granted
                
                // If it was just granted and wasn't in our statuses before
                if (granted && permissionStatuses[perm] == false && perm !in recentlyGranted) {
                    recentlyGranted = recentlyGranted + perm
                    // Trigger removal animation after a short delay to show the checkmark
                    scope.launch {
                        kotlinx.coroutines.delay(800)
                        visiblePermissions = visiblePermissions - perm
                    }
                }
            }
        }
        permissionStatuses = newStatuses
    }

    LaunchedEffect(Unit) { 
        updateStatuses() 
        // Sync visible permissions with current system state initially
        visiblePermissions = allPages.mapNotNull { it.permission }.filter { 
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> updateStatuses() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Needs Few Permission",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "to make the app works for you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(16.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "All of your Data will be on your device only untill you setup sync simultaneously.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            if (visiblePermissions.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(bottom = 64.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, "Done", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Everything is ready!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("All permissions granted successfully.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = { showTrackSimModal = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { 
                            Text("Setup Track SIM") 
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    allPages.forEach { page ->
                        page.permission?.let { perm ->
                            AnimatedVisibility(
                                visible = perm in visiblePermissions,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut() + scaleOut(targetScale = 0.8f)
                            ) {
                                PermissionCard(
                                    page = page,
                                    isGranted = recentlyGranted.contains(perm),
                                    onGrant = { permissionLauncher.launch(it) }
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { showTrackSimModal = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Finish Setup")
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    page: OnboardingPage,
    isGranted: Boolean,
    onGrant: (String) -> Unit
) {
    Card(
        onClick = { if (!isGranted) page.permission?.let { onGrant(it) } },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isGranted) Icons.Default.Check else page.icon, 
                        null, 
                        tint = if (isGranted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, 
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(page.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(page.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(Modifier.width(12.dp))
            
            if (isGranted) {
                Icon(
                   imageVector = Icons.Default.CheckCircle,
                   contentDescription = "Granted",
                   tint = MaterialTheme.colorScheme.primary,
                   modifier = Modifier.size(32.dp)
                )
            } else {
                Button(
                    onClick = { page.permission?.let { onGrant(it) } },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Allow", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
