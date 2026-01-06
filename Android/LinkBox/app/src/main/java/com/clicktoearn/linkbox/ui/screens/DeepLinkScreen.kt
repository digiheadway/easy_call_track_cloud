package com.clicktoearn.linkbox.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicktoearn.linkbox.data.entity.*
import com.clicktoearn.linkbox.ui.components.*
import com.clicktoearn.linkbox.ui.theme.*
import com.clicktoearn.linkbox.ui.viewmodel.LinkBoxViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class DeepLinkState {
    object Loading : DeepLinkState()
    data class Found(val entity: EntityItem, val sharing: SharingEntity) : DeepLinkState()
    object NotFound : DeepLinkState()
    object Expired : DeepLinkState()
    object Private : DeepLinkState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepLinkScreen(
    viewModel: LinkBoxViewModel,
    token: String,
    onNavigateToPageEditor: (Long, Boolean) -> Unit,
    onNavigateToBrowser: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    var state by remember { mutableStateOf<DeepLinkState>(DeepLinkState.Loading) }
    var ownerName by remember { mutableStateOf<String?>(null) }
    var showJoinedDialog by remember { mutableStateOf(false) }
    val userPoints by viewModel.userPoints.collectAsState()
    val showInsufficientPoints by viewModel.showInsufficientPointsDialog.collectAsState()
    val pointsNeeded by viewModel.pointsNeeded.collectAsState()
    
    // Lookup by token on launch
    LaunchedEffect(token) {
        val (entity, sharing) = viewModel.lookupByToken(token)
        state = when {
            entity == null || sharing == null -> DeepLinkState.NotFound
            sharing.privacy == PrivacyType.PRIVATE -> DeepLinkState.Private
            sharing.publicUpto != null && sharing.publicUpto < System.currentTimeMillis() -> DeepLinkState.Expired
            else -> DeepLinkState.Found(entity, sharing)
        }
        
        // Record view if found and fetch owner name
        if (state is DeepLinkState.Found) {
            val found = state as DeepLinkState.Found
            viewModel.recordLinkView(found.sharing.id, found.sharing.token)
            ownerName = found.sharing.ownerName ?: "Unknown"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shared Content") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    PointsBadge(
                        points = userPoints?.currentBalance ?: 0,
                        onClick = { viewModel.openPointsShop() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith 
                fadeOut(animationSpec = tween(300))
            },
            label = "DeepLinkStateTransition"
        ) { currentState ->
            when (currentState) {
                is DeepLinkState.Loading -> {
                    LoadingContent(modifier = Modifier.padding(padding))
                }
                is DeepLinkState.NotFound -> {
                    NotFoundContent(
                        token = token,
                        modifier = Modifier.padding(padding),
                        onBack = onBack
                    )
                }
                is DeepLinkState.Expired -> {
                    ExpiredContent(
                        modifier = Modifier.padding(padding),
                        onBack = onBack
                    )
                }
                is DeepLinkState.Private -> {
                    PrivateContent(
                        modifier = Modifier.padding(padding),
                        onBack = onBack
                    )
                }
                is DeepLinkState.Found -> {
                    val isOwner = currentState.entity.ownerId != 0L && 
                                  currentState.entity.ownerId == (viewModel.currentOwnerId.collectAsState().value ?: -1L)
                    
                    FoundContent(
                        entity = currentState.entity,
                        sharing = currentState.sharing,
                        ownerName = if (isOwner) "You" else (ownerName ?: "Loading..."),
                        isOwner = isOwner,
                        modifier = Modifier.padding(padding),
                        onOpenLink = {
                            val sharing = currentState.sharing
                            val entity = currentState.entity
                            
                            viewModel.openJoinedLink(
                                JoinedLinkEntity(
                                    token = token,
                                    name = entity.name,
                                    type = entity.type,
                                    url = entity.getUrl(),
                                    pointsRequired = sharing.pointsRequired
                                )
                            ) { url ->
                                if (url != null) {
                                    viewModel.recordLinkClick(sharing.id, sharing.token)
                                    // Choose whether to show URL bar based on link criteria
                                    val showUrlBar = !url.contains("hide_ui=true") && !url.contains("google.com")
                                    onNavigateToBrowser(url, showUrlBar)
                                }
                            }
                        },
                        onViewPage = {
                            // Navigate to page editor in read-only mode
                            onNavigateToPageEditor(currentState.entity.id, true)
                        },
                        onJoinLink = {
                            // Join this link to user's collection
                            showJoinedDialog = true
                        },
                        onSaveCopy = {
                            // Save a copy of this content to user's own workspace
                            scope.launch {
                                viewModel.duplicateEntity(currentState.entity)
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Save confirmation sheet
    if (showJoinedDialog && state is DeepLinkState.Found) {
        val foundState = state as DeepLinkState.Found
        LinkBoxBottomSheet(
            onDismissRequest = { showJoinedDialog = false },
            title = "Save to My Links?"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Would you like to save \"${foundState.entity.name}\" to your History for easy access later?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.joinLink(
                                token = token,
                                name = foundState.entity.name,
                                type = foundState.entity.type,
                                url = foundState.entity.getUrl(),
                                authorName = ownerName,
                                pointsRequired = foundState.sharing.pointsRequired
                            )
                            showJoinedDialog = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Link")
                }
            }
        }
    }

    // Insufficient Points Sheet
    if (showInsufficientPoints) {
        InsufficientPointsSheet(
            needed = pointsNeeded,
            onDismiss = { viewModel.closeInsufficientPointsDialog() },
            onWatchAd = {
                viewModel.earnPointsFromAd(1)
                viewModel.closeInsufficientPointsDialog()
            },
            onOpenShop = {
                viewModel.openPointsShop()
                viewModel.closeInsufficientPointsDialog()
            }
        )
    }

    // Points Shop Sheet
    val showPointsShop by viewModel.showPointsShop.collectAsState()
    if (showPointsShop) {
        PointsShopSheet(
            currentPoints = userPoints?.currentBalance ?: 0,
            onDismiss = { viewModel.closePointsShop() },
            onWatchAd = { viewModel.earnPointsFromAd(1) },
            onPurchasePackage = { pkg: PointPackage -> viewModel.purchasePoints(pkg.id, pkg.totalPoints) }
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                "Loading shared content...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotFoundContent(
    token: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ErrorStart.copy(alpha = 0.1f),
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    Icons.Default.LinkOff,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = ErrorStart
                )
            }
            Text(
                "Link Not Found",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                "The link with token '$token' doesn't exist or has been removed by the owner.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onBack) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun ExpiredContent(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = GoldStart.copy(alpha = 0.1f),
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = GoldStart
                )
            }
            Text(
                "Link Expired",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                "This link has expired and is no longer accessible.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onBack) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun PrivateContent(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Private Link",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                "This link is private and requires special access.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(onClick = onBack) {
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun FoundContent(
    entity: EntityItem,
    sharing: SharingEntity,
    ownerName: String,
    isOwner: Boolean = false,
    modifier: Modifier = Modifier,
    onOpenLink: () -> Unit,
    onViewPage: () -> Unit,
    onJoinLink: () -> Unit,
    onSaveCopy: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // File Card (Files Tab style)
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = when (entity.type) {
                                    EntityType.LINK -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    EntityType.PAGE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                    EntityType.FOLDER -> Color(0xFFFFC107).copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (entity.type) {
                                EntityType.LINK -> Icons.Default.Link
                                EntityType.PAGE -> Icons.Default.Description
                                EntityType.FOLDER -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            tint = when (entity.type) {
                                EntityType.LINK -> MaterialTheme.colorScheme.primary
                                EntityType.PAGE -> MaterialTheme.colorScheme.secondary
                                EntityType.FOLDER -> Color(0xFFFFC107)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = entity.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (sharing.pointsRequired > 0 && !isOwner) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Stars,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = GoldStart
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${sharing.pointsRequired} Points Required",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldStart,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (isOwner) {
                            Text(
                                text = "Your Content",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.alpha(0.3f))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(label = "File Type", value = entity.type.name)
                    InfoRow(label = "Last Updated", value = dateFormat.format(Date(entity.updatedAt)))
                    InfoRow(label = "File Owner", value = ownerName)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    when (entity.type) {
                        EntityType.LINK -> onOpenLink()
                        EntityType.PAGE -> onViewPage()
                        EntityType.FOLDER -> { /* Folders not directly openable */ }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Open it",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            if (!isOwner) {
                OutlinedButton(
                    onClick = onJoinLink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save to My Links",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
            
            // Save Copy Button - Only show when owner allows duplication
            if (sharing.allowDuplicate) {
                FilledTonalButton(
                    onClick = onSaveCopy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save a Copy",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))
        
        // Disclaimer
        Text(
            text = "Disclaimer: we as linkbox dont own the content and solely file owner is responsible for it. we follow dmca and all the laws and will remove the content if it harmful, danger or illegal, copyright. Report us to Remove it Asap",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
