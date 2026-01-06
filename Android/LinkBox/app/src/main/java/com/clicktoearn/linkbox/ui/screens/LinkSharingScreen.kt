package com.clicktoearn.linkbox.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun LinkSharingScreen(
    viewModel: LinkBoxViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val entities by viewModel.myEntities.collectAsState()
    val allSharings by viewModel.allSharings.collectAsState()
    val userPointsState by viewModel.userPoints.collectAsState()
    val currentPoints = userPointsState?.currentBalance ?: 0
    
    val entityNameMap = remember(entities) {
        entities.associate { it.entity.id to it.entity.name }
    }
    
    // Calculate total stats
    val totalLinks = allSharings.size
    val totalViews = allSharings.sumOf { it.views }
    val totalUniqueVisits = allSharings.sumOf { it.uniqueVisits }
    val totalNewUsers = allSharings.sumOf { it.newUsers }
    val activeAccessCount = allSharings.count { it.privacy == PrivacyType.PUBLIC }
    
    // Points earned formula: (views * 0.1) + (newUsers * 10) + (paid access * cost * 0.4)
    val pointsEarned = remember(allSharings) {
        allSharings.sumOf { 
            val base = (it.views * 0.1) + (it.newUsers * 10.0)
            val fromCost = if (it.pointsRequired > 0) it.views * it.pointsRequired * 0.4 else 0.0
            base + fromCost
        }
    }
    
    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Active, 1: Inactive
    var showPointsDetails by remember { mutableStateOf(false) }
    var selectedSharingForDetails by remember { mutableStateOf<SharingEntity?>(null) }
    
    val filteredSharings = remember(allSharings, selectedTabIndex) {
        if (selectedTabIndex == 0) {
            allSharings.filter { it.privacy == PrivacyType.PUBLIC }
        } else {
            allSharings.filter { it.privacy != PrivacyType.PUBLIC }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Links", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    com.clicktoearn.linkbox.ui.components.PointsBadge(
                        points = currentPoints,
                        onClick = { viewModel.openPointsShop() }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Smaller Hero Quick Insights Grid
            item {
                QuickInsightsGrid(
                    totalLinks = totalLinks,
                    totalViews = totalViews,
                    totalUsers = totalUniqueVisits,
                    newUsers = totalNewUsers,
                    activeAccess = activeAccessCount,
                    pointsEarned = String.format("%.1f", pointsEarned)
                )
            }
            
            // Points Earning Details Card
            item {
                EarningInfoCard(onDetailsClick = { showPointsDetails = true })
            }
            
            // Filters Row (Chips)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        label = { Text("Active Links", fontSize = 13.sp, fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(100.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = null
                    )
                    FilterChip(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        label = { Text("Inactive", fontSize = 13.sp, fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(100.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = null
                    )
                }
            }
            
            // Compact Links List
            if (filteredSharings.isEmpty()) {
                item {
                    EmptyLinksState(isActiveTab = selectedTabIndex == 0)
                }
            } else {
                items(filteredSharings, key = { it.id }) { sharing ->
                    CompactLinkCard(
                        sharing = sharing,
                        entityName = entityNameMap[sharing.entityId] ?: "Untitled",
                        onClick = { selectedSharingForDetails = sharing }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
    
    // Modals
    if (showPointsDetails) {
        PointsEarningDetailsSheet(onDismiss = { showPointsDetails = false })
    }
    
    if (selectedSharingForDetails != null) {
        LinkDetailSheet(
            sharing = selectedSharingForDetails!!,
            entityName = entityNameMap[selectedSharingForDetails!!.entityId] ?: "Untitled",
            onDismiss = { selectedSharingForDetails = null },
            onToggle = { enabled -> viewModel.toggleSharingAccess(selectedSharingForDetails!!.id, enabled) },
            onDelete = { 
                viewModel.deleteSharingLink(selectedSharingForDetails!!.id)
                selectedSharingForDetails = null
            },
            onUpdateSettings = { name, points ->
                viewModel.updateSharingSettings(selectedSharingForDetails!!.id, name, points)
            }
        )
    }
}

@Composable
private fun QuickInsightsGrid(
    totalLinks: Int,
    totalViews: Int,
    totalUsers: Int,
    newUsers: Int,
    activeAccess: Int,
    pointsEarned: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            InsightItem(Modifier.weight(1f), "Links", "$totalLinks", Icons.Default.Link, PrimaryDark)
            InsightItem(Modifier.weight(1f), "Views", "$totalViews", Icons.Default.Visibility, InfoStart)
            InsightItem(Modifier.weight(1f), "Users", "$totalUsers", Icons.Default.People, PurpleStart)
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            InsightItem(Modifier.weight(1f), "New Users", "$newUsers", Icons.Default.PersonAdd, SuccessStart)
            InsightItem(Modifier.weight(1f), "Active Access", "$activeAccess", Icons.Default.CheckCircle, ActiveGreen)
            InsightItem(Modifier.weight(1f), "Points Earned", pointsEarned, Icons.Default.Stars, GoldStart)
        }
    }
}

@Composable
private fun InsightItem(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color.copy(alpha = 0.8f))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EarningInfoCard(onDetailsClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Earning from Sharing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Turn your links into rewards", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onDetailsClick) {
                Text("See Details", fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CompactLinkCard(
    sharing: SharingEntity,
    entityName: String,
    onClick: () -> Unit
) {
    val isActive = sharing.privacy == PrivacyType.PUBLIC
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(8.dp).background(if (isActive) SuccessStart else InactiveGray, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sharing.name ?: entityName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${sharing.views} views • ${sharing.uniqueVisits} users", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = InactiveGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EmptyLinksState(isActiveTab: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = InactiveGray)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            if (isActiveTab) "No active links" else "No inactive links",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointsEarningDetailsSheet(onDismiss: () -> Unit) {
    LinkBoxBottomSheet(onDismissRequest = onDismiss, title = "Points from sharing") {
        Column(modifier = Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EarningDetailRow("Free View", "0.1 Pt", "Get rewards for every view")
            EarningDetailRow("New User", "10 Pt", "Huge bonus for each new joiner")
            EarningDetailRow("Set Point Cost", "40%", "You get 40% of points charged")
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Why earn points?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BenefitItem(Icons.Default.Link, "Access links")
                BenefitItem(Icons.Default.Verified, "Get Premium")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Got it")
            }
        }
    }
}

@Composable
private fun EarningDetailRow(label: String, value: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(color = GoldStart.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
            Text(value, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = GoldStart, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BenefitItem(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkDetailSheet(
    sharing: SharingEntity,
    entityName: String,
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onUpdateSettings: (String, Int) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val shareUrl = "https://api.pokipro.com/linkbox/?token=${sharing.token}"
    var showEditSheet by remember { mutableStateOf(false) }
    val displayName = sharing.name ?: entityName
    var newName by remember { mutableStateOf(displayName) }
    var pointsReq by remember { mutableIntStateOf(sharing.pointsRequired) }
    var showCopiedFeedback by remember { mutableStateOf(false) }
    val isActive = sharing.privacy == PrivacyType.PUBLIC

    LinkBoxBottomSheet(onDismissRequest = onDismiss, title = "Link Details") {
        Column(modifier = Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Status and Main Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(if (isActive) "Actively Sharing" else "Sharing Paused", color = if (isActive) SuccessStart else InactiveGray, fontSize = 12.sp)
                }
                Switch(checked = isActive, onCheckedChange = onToggle)
            }
            
            // Share URL
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(shareUrl, modifier = Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { 
                        clipboardManager.setText(AnnotatedString(shareUrl))
                        showCopiedFeedback = true
                    }) {
                        Icon(if (showCopiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            // Detailed Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailStat(Modifier.weight(1f), "Views", "${sharing.views}", InfoStart)
                DetailStat(Modifier.weight(1f), "Users", "${sharing.uniqueVisits}", PurpleStart)
                DetailStat(Modifier.weight(1f), "Points", sharing.pointsRequired.toString(), GoldStart)
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { showEditSheet = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorStart)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        }
    }
    
    if (showEditSheet) {
        LinkBoxBottomSheet(onDismissRequest = { showEditSheet = false }, title = "Edit Link") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Link Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = if (pointsReq == 0) "" else pointsReq.toString(),
                    onValueChange = { pointsReq = it.toIntOrNull() ?: 0 },
                    label = { Text("Points to access") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Button(
                    onClick = {
                        onUpdateSettings(newName, pointsReq)
                        showEditSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
    
    LaunchedEffect(showCopiedFeedback) {
        if (showCopiedFeedback) {
            kotlinx.coroutines.delay(2000)
            showCopiedFeedback = false
        }
    }
}

@Composable
private fun DetailStat(modifier: Modifier, label: String, value: String, color: Color) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
        }
    }
}
