package com.miniclick.calltrackmanage.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.miniclick.calltrackmanage.ui.settings.CloudSyncModal
import com.miniclick.calltrackmanage.ui.settings.CreateOrgModal
import com.miniclick.calltrackmanage.ui.settings.JoinOrgModal
import com.miniclick.calltrackmanage.ui.settings.AccountInfoModal
import com.miniclick.calltrackmanage.ui.settings.SettingsUiState
import com.miniclick.calltrackmanage.ui.settings.SettingsViewModel

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class OnboardingStepType {
    object Welcome : OnboardingStepType()
    object FeatureIntro : OnboardingStepType()
    object PermissionDisclosure : OnboardingStepType()
}

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val type: OnboardingStepType = OnboardingStepType.Welcome
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val settingsViewModel: com.miniclick.calltrackmanage.ui.settings.SettingsViewModel = 
        androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by settingsViewModel.uiState.collectAsState()
    
    // Modals State
    var showCloudSyncModal by remember { mutableStateOf(false) }
    var showCreateOrgModal by remember { mutableStateOf(false) }
    var showJoinOrgModal by remember { mutableStateOf(false) }
    var showAccountInfoModal by remember { mutableStateOf(false) }
    var accountEditField by remember { mutableStateOf<String?>(null) }

    // Fetch SIM info on start
    LaunchedEffect(Unit) {
        settingsViewModel.fetchSimInfo()
    }
    
    // Build the list of onboarding steps
    val steps = remember {
        buildList {
            // Intro Screen
            add(OnboardingStep(
                "Welcome to MiniClick Calls",
                "Your intelligent call management companion. Track, organize, and sync your calls effortlessly.",
                Icons.Default.Cloud,
                OnboardingStepType.Welcome
            ))
            
            // Get Started 1
            add(OnboardingStep(
                "Organize Your Calls",
                "Add private notes and color-coded labels to your calls to keep track of important details.",
                Icons.AutoMirrored.Filled.NoteAdd,
                OnboardingStepType.FeatureIntro
            ))
            
            // Get Started 2
            add(OnboardingStep(
                "Advanced Filtering",
                "Find exactly what you're looking for with powerful filters for dates, types, labels, and more.",
                Icons.Default.FilterList,
                OnboardingStepType.FeatureIntro
            ))
            
            // Get Started 3
            add(OnboardingStep(
                "Attach Recordings",
                "Keep your call recordings organized by attaching them directly to call logs for easy playback.",
                Icons.Default.Mic,
                OnboardingStepType.FeatureIntro
            ))
            
            // Get Started 4
            add(OnboardingStep(
                "Notes During Calls",
                "Instantly see previous notes and caller details even while you are on an active call.",
                Icons.Default.AssignmentInd,
                OnboardingStepType.FeatureIntro
            ))
            
            // Get Started 5
            add(OnboardingStep(
                "Review & Manage",
                "Quickly review your daily call activity and stay on top of your communication goals.",
                Icons.Default.RateReview,
                OnboardingStepType.FeatureIntro
            ))
            
            // Get Started 6
            add(OnboardingStep(
                "Sync to Cloud",
                "Optionally sync your data to the dashboard to access your call history from any device.",
                Icons.Default.CloudSync,
                OnboardingStepType.FeatureIntro
            ))
            
            // NEW: Prominent Disclosure Step (Final Step)
            add(OnboardingStep(
                "Data & Permissions",
                "To provide its core call management and dialer features, MiniClick Calls needs your permission to access specific data.",
                Icons.Default.Security,
                OnboardingStepType.PermissionDisclosure
            ))
            
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { steps.size })
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(Modifier.height(16.dp))
            
            // Progress Indicator
            LinearProgressIndicator(
                progress = { (pagerState.currentPage + 1).toFloat() / steps.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Step Counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Step ${pagerState.currentPage + 1} of ${steps.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (pagerState.currentPage < steps.size - 1) {
                    TextButton(onClick = onComplete) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            // Pager Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true
                ) { page ->
                val step = steps[page]
                
                when (step.type) {
                    is OnboardingStepType.Welcome -> {
                        WelcomeStepContent(
                            step = step,
                            onGetStarted = {
                                scope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            onJoinAsEmployee = {
                                showCloudSyncModal = true
                            }
                        )
                    }
                    is OnboardingStepType.FeatureIntro -> {
                        OnboardingStepContent(
                            step = step,
                            isPermissionGranted = false,
                            onRequestPermission = {},
                            onContinue = {
                                if (page < steps.size - 1) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(page + 1)
                                    }
                                } else {
                                    onComplete()
                                }
                            },
                            buttonText = if (page < steps.size - 1) "Next" else "Done",
                            showSkip = false
                        )
                    }
                    is OnboardingStepType.PermissionDisclosure -> {
                        PermissionDisclosureStepContent(
                            onContinue = onComplete
                        )
                    }

                }
            }
        }
            
            // Page Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(steps.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isSelected) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }

        // --- Modals ---
        if (showCloudSyncModal) {
            CloudSyncModal(
                uiState = uiState,
                viewModel = settingsViewModel,
                onDismiss = { 
                    showCloudSyncModal = false
                    if (settingsViewModel.uiState.value.pairingCode.isNotBlank()) {
                        onComplete()
                    }
                },
                onOpenAccountInfo = { field -> 
                    accountEditField = field
                    showAccountInfoModal = true 
                },
                onCreateOrg = {
                    showCreateOrgModal = true
                },
                onJoinOrg = {
                    showJoinOrgModal = true
                },
                onKeepOffline = { 
                    // Just close and let them continue with normal onboarding
                    showCloudSyncModal = false
                }
            )
        }

        if (showCreateOrgModal) {
            CreateOrgModal(
                onDismiss = { showCreateOrgModal = false }
            )
        }

        if (showJoinOrgModal) {
            JoinOrgModal(
                viewModel = settingsViewModel,
                onDismiss = { showJoinOrgModal = false }
            )
        }

        if (showAccountInfoModal) {
            AccountInfoModal(
                uiState = uiState,
                viewModel = settingsViewModel,
                editField = accountEditField,
                onDismiss = { 
                    showAccountInfoModal = false
                    accountEditField = null
                }
            )
        }
    }
}

@Composable
fun WelcomeStepContent(
    step: OnboardingStep,
    onGetStarted: () -> Unit,
    onJoinAsEmployee: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .size(140.dp)
                .scale(iconScale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    step.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(Modifier.height(40.dp))
        
        Text(
            step.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            step.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onJoinAsEmployee,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text("Login", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun OnboardingStepContent(
    step: OnboardingStep,
    isPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onContinue: () -> Unit,
    buttonText: String = "Continue",
    showSkip: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val isWelcome = step.type is OnboardingStepType.Welcome
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .size(140.dp)
                .scale(if (isWelcome) iconScale else 1f),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    step.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(Modifier.height(40.dp))
        
        Text(
            step.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            step.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(buttonText, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PermissionDisclosureStepContent(
    onContinue: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.PrivacyTip,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            "Privacy & Permissions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            "MiniClick Calls is a communication tool. For it to work, we need your permission for the following:",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(24.dp))
        
        PermissionDisclosureItem(
            icon = Icons.Default.Call,
            title = "Call Logs",
            description = "Used to display your call history, provide dialer features, and enable optional cloud synchronization."
        )
        
        PermissionDisclosureItem(
            icon = Icons.Default.Contacts,
            title = "Contacts",
            description = "Helps identify callers and organize your call logs with names."
        )
        
        PermissionDisclosureItem(
            icon = Icons.Default.Phone,
            title = "Phone State & Numbers",
            description = "Required to detect active calls and identify which SIM card is being used for tracking."
        )
        
        PermissionDisclosureItem(
            icon = Icons.Default.Call,
            title = "Make Calls",
            description = "Allows the app to initiate phone calls directly from the dialer without opening other apps."
        )

        PermissionDisclosureItem(
            icon = Icons.Default.Folder,
            title = "Storage",
            description = "Needed if you choose to attach and manage recording files."
        )

        PermissionDisclosureItem(
            icon = Icons.Default.SettingsPhone,
            title = "Default Phone App",
            description = "As a Smart Dialer, MiniClick can handle your calls directly, providing a rich in-call experience and better call tracking."
        )
        
        Spacer(Modifier.height(32.dp))
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Finish Setup")
        }
        
        Spacer(Modifier.height(16.dp))
        
        Spacer(Modifier.height(32.dp))
        
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PermissionDisclosureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun DateOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
               else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                       else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
