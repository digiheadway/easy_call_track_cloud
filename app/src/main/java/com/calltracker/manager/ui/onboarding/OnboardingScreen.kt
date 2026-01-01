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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permission: String? = null
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    var permissionsGranted by remember { mutableStateOf(false) }
    
    val pages = remember {
        buildList {
            add(OnboardingPage(
                title = "Welcome to CallCloud",
                description = "Track and manage your call logs with cloud sync. Let's set up your app in a few simple steps.",
                icon = Icons.Default.Cloud
            ))
            add(OnboardingPage(
                title = "Call Logs Access",
                description = "We need permission to read your call logs to track and organize your calls.",
                icon = Icons.Default.Call,
                permission = Manifest.permission.READ_CALL_LOG
            ))
            add(OnboardingPage(
                title = "Contacts Access",
                description = "Access to contacts helps us show caller names and information.",
                icon = Icons.Default.People,
                permission = Manifest.permission.READ_CONTACTS
            ))
            add(OnboardingPage(
                title = "Phone State",
                description = "We need this to detect incoming and outgoing calls.",
                icon = Icons.Default.Phone,
                permission = Manifest.permission.READ_PHONE_STATE
            ))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(OnboardingPage(
                    title = "Notifications",
                    description = "Get notified about call recordings and sync status.",
                    icon = Icons.Default.Notifications,
                    permission = Manifest.permission.POST_NOTIFICATIONS
                ))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(OnboardingPage(
                    title = "Audio Files",
                    description = "Access audio files to manage call recordings.",
                    icon = Icons.Default.AudioFile,
                    permission = Manifest.permission.READ_MEDIA_AUDIO
                ))
            } else {
                add(OnboardingPage(
                    title = "Storage Access",
                    description = "Access storage to manage call recordings.",
                    icon = Icons.Default.Storage,
                    permission = Manifest.permission.READ_EXTERNAL_STORAGE
                ))
            }
            add(OnboardingPage(
                title = "All Set!",
                description = "You're ready to start tracking your calls. Sync to cloud and access from anywhere.",
                icon = Icons.Default.CheckCircle
            ))
        }
    }

    val currentPageData = pages[currentPage]
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted || currentPageData.permission == null) {
            if (currentPage < pages.size - 1) {
                currentPage++
            } else {
                onComplete()
            }
        }
        permissionsGranted = isGranted
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentPage) 12.dp else 8.dp)
                            .background(
                                color = if (index == currentPage) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    )
                    if (index < pages.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Icon
            Icon(
                imageVector = currentPageData.icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = currentPageData.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = currentPageData.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            if (currentPageData.permission != null) {
                Button(
                    onClick = {
                        permissionLauncher.launch(currentPageData.permission)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Grant Permission")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for Now")
                }
            } else {
                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(if (currentPage == pages.size - 1) "Get Started" else "Continue")
                }
            }

            // Back button (except on first page)
            if (currentPage > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { currentPage-- },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
