package com.clicktoearn.linkbox.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicktoearn.linkbox.data.entity.SharingEntity
import com.clicktoearn.linkbox.data.entity.PrivacyType
import com.clicktoearn.linkbox.ui.theme.*
import com.clicktoearn.linkbox.ui.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.ui.components.LinkBoxBottomSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarnScreen(viewModel: LinkBoxViewModel) {
    val entities by viewModel.myEntities.collectAsState()
    val allSharings by viewModel.allSharings.collectAsState()
    
    val entityNameMap = remember(entities) {
        entities.associate { it.entity.id to it.entity.name }
    }
    
    val totalClicks = allSharings.sumOf { it.clicks }
    val totalViews = allSharings.sumOf { it.views }
    val totalUniqueVisits = allSharings.sumOf { it.uniqueVisits }
    val totalDownloads = allSharings.sumOf { it.downloads }
    val activeLinks = allSharings.count { it.privacy == PrivacyType.PUBLIC }
    val userPointsState by viewModel.userPoints.collectAsState()
    val currentPoints = userPointsState?.currentBalance ?: 0

    // State Hoisting
    var showMenu by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredSharings = remember(allSharings, searchQuery, entityNameMap) {
        if (searchQuery.isBlank()) {
            allSharings
        } else {
            allSharings.filter { sharing ->
                val displayName = sharing.name ?: entityNameMap[sharing.entityId] ?: ""
                displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { 
                        Text(
                            "Earn", 
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp 
                        ) 
                    },
                    navigationIcon = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (showSearchBar) "Hide Search" else "Show Search") },
                                    leadingIcon = { 
                                        Icon(
                                            if (showSearchBar) Icons.Default.SearchOff else Icons.Default.Search, 
                                            contentDescription = null
                                        ) 
                                    },
                                    onClick = { 
                                        showSearchBar = !showSearchBar
                                        if (!showSearchBar) searchQuery = ""
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    },
                    actions = {
                        com.clicktoearn.linkbox.ui.components.PointsBadge(
                            points = viewModel.userPoints.collectAsState().value?.currentBalance ?: 0,
                            onClick = { viewModel.openPointsShop() }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                
                // Search Bar
                AnimatedVisibility(
                    visible = showSearchBar,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            placeholder = { Text("Search links...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    }
                }
                if (showSearchBar) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Points Card with Gradient - Only show if not searching or if user wants to see it?
            // Usually dashboard stats are always useful.
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppGradients.premium)
                            .clip(AppShapes.CardLarge)
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        "Total Points",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "$currentPoints",
                                        color = Color.White,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Surface(
                                    shape = AppShapes.CardSmall,
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        modifier = Modifier.padding(12.dp).size(28.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Column {
                                    Text("Active Links", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text("$activeLinks", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Column {
                                    Text("Total Links", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    Text("${allSharings.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Insights Section
            item {
                Text("Insights", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        InsightCard(
                            icon = Icons.Default.TouchApp,
                            title = "Clicks",
                            value = "$totalClicks",
                            color = SuccessStart
                        )
                    }
                    item {
                        InsightCard(
                            icon = Icons.Default.Visibility,
                            title = "Views",
                            value = "$totalViews",
                            color = InfoStart
                        )
                    }
                    item {
                        InsightCard(
                            icon = Icons.Default.People,
                            title = "Unique",
                            value = "$totalUniqueVisits",
                            color = PurpleStart
                        )
                    }
                    item {
                        InsightCard(
                            icon = Icons.Default.Download,
                            title = "Downloads",
                            value = "$totalDownloads",
                            color = WarningStart
                        )
                    }
                }
            }

            // Links Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Sharing Links", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "${filteredSharings.size} links",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredSharings.isEmpty()) {
                item { EmptyLinksCard() }
            }

            items(filteredSharings) { sharing ->
                SharingLinkCard(
                    sharing = sharing,
                    entityName = entityNameMap[sharing.entityId] ?: "Unknown",
                    onDelete = { viewModel.deleteSharingLink(sharing.id) },
                    onToggle = { enabled -> viewModel.toggleSharingAccess(sharing.id, enabled) },
                    onUpdate = { name, points -> viewModel.updateSharingSettings(sharing.id, name, points) }
                )
            }
        }
    }
}

@Composable
fun InsightCard(icon: ImageVector, title: String, value: String, color: Color) {
    Card(
        modifier = Modifier.width(140.dp),
        shape = AppShapes.CardMedium,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = color)
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyLinksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No sharing links yet", fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Create sharing links from your content to start tracking analytics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharingLinkCard(
    sharing: SharingEntity,
    entityName: String,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onUpdate: (String, Int) -> Unit = { _, _ -> }
) {
    val clipboardManager = LocalClipboardManager.current
    val shareUrl = "https://api.pokipro.com/linkbox/?token=${sharing.token}"
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val isActive = sharing.privacy == PrivacyType.PUBLIC
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val displayName = sharing.name ?: entityName
    var newName by remember { mutableStateOf(displayName) }
    var pointsReq by remember { mutableIntStateOf(sharing.pointsRequired) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header with gradient accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        if (isActive) {
                            AppGradients.success
                        } else {
                            AppGradients.inactive
                        }
                    )
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (isActive) SuccessStart else InactiveGray,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isActive) "Active" else "Inactive",
                                fontSize = 12.sp,
                                color = if (isActive) SuccessStart else InactiveGray
                            )
                            Text(
                                text = " • ${dateFormat.format(Date(sharing.createdAt))}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit Settings") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    newName = displayName
                                    pointsReq = sharing.pointsRequired
                                    showRenameDialog = true
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isActive) "Deactivate" else "Activate") },
                                leadingIcon = {
                                    Icon(
                                        if (isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                },
                                onClick = { onToggle(!isActive); showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = ErrorStart) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorStart) },
                                onClick = { onDelete(); showMenu = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // URL Row with Copy
                Surface(
                    shape = AppShapes.CardSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = shareUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalIconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(shareUrl)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ModernStatItem(
                        value = "${sharing.clicks}",
                        label = "Clicks",
                        color = SuccessStart,
                        modifier = Modifier.weight(1f)
                    )
                    ModernStatItem(
                        value = "${sharing.views}",
                        label = "Views",
                        color = InfoStart,
                        modifier = Modifier.weight(1f)
                    )
                    ModernStatItem(
                        value = "${sharing.uniqueVisits}",
                        label = "Unique",
                        color = PurpleStart,
                        modifier = Modifier.weight(1f)
                    )
                    ModernStatItem(
                        value = "${sharing.downloads}",
                        label = "Downloads",
                        color = WarningStart,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Edit Settings Sheet
    if (showRenameDialog) {
        LinkBoxBottomSheet(
            onDismissRequest = { showRenameDialog = false },
            title = "Edit Link Settings"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Link Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = if (pointsReq == 0) "" else pointsReq.toString(),
                    onValueChange = { 
                        pointsReq = it.toIntOrNull() ?: 0
                    },
                    label = { Text("Points Required to Access") },
                    placeholder = { Text("0 for free") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = GoldStart) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onUpdate(newName, pointsReq)
                        }
                        showRenameDialog = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Settings")
                }
            }
        }
    }
}

@Composable
fun ModernStatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Surface(
            shape = AppShapes.ButtonSmall,
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp)
            ) {
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = color
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
