package com.clicktoearn.linkbox.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.clicktoearn.linkbox.data.entity.*
import com.clicktoearn.linkbox.ui.components.*
import com.clicktoearn.linkbox.ui.theme.*
import com.clicktoearn.linkbox.ui.viewmodel.LinkBoxViewModel
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinedScreen(
    viewModel: LinkBoxViewModel,
    onNavigateToBrowser: (String, Boolean) -> Unit,
    onNavigateToPageEditor: (Long, Boolean) -> Unit = { _, _ -> },
    isInternalTab: Boolean = false,
    showCategoryChips: Boolean = false
) {
    val joinedLinks by viewModel.joinedLinks.collectAsState()
    val userPoints by viewModel.userPoints.collectAsState()
    val showPointsShop by viewModel.showPointsShop.collectAsState()
    val showOnlyStarred by viewModel.joinedLinksShowOnlyStarred.collectAsState()
    val showInsufficientPoints by viewModel.showInsufficientPointsDialog.collectAsState()
    val pointsNeeded by viewModel.pointsNeeded.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    var selectedLinkForOptions by remember { mutableStateOf<JoinedLinkEntity?>(null) }

    // Initialize points on first load
    LaunchedEffect(Unit) {
        viewModel.initializePoints()
    }

    val content = @Composable { padding: PaddingValues ->
        val currentFilter by viewModel.joinedLinksEntityTypeFilter.collectAsState()
        val filterOptions = listOf("All", "Folders", "Links", "Pages")
        val selectedFilter = when (currentFilter) {
            null -> 0
            EntityType.FOLDER -> 1
            EntityType.LINK -> 2
            EntityType.PAGE -> 3
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Category Filter Chips (conditionally shown)
            if (showCategoryChips) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(filterOptions.size) { index ->
                        FilterChip(
                            selected = selectedFilter == index,
                            onClick = { 
                                val type = when (index) {
                                    1 -> EntityType.FOLDER
                                    2 -> EntityType.LINK
                                    3 -> EntityType.PAGE
                                    else -> null
                                }
                                viewModel.setJoinedLinksFilter(type)
                            },
                            label = { 
                                Text(
                                    filterOptions[index], 
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedFilter == index) FontWeight.SemiBold else FontWeight.Normal
                                ) 
                            },
                            leadingIcon = if (selectedFilter == index) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.Transparent,
                                selectedBorderColor = Color.Transparent,
                                enabled = true,
                                selected = selectedFilter == index
                            )
                        )
                    }
                }
            }

            if (joinedLinks.isEmpty()) {
                EmptyJoinedLinksCard(showingStarred = showOnlyStarred)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(joinedLinks) { joinedLink ->
                        JoinedLinkCard(
                            joinedLink = joinedLink,
                            onOpen = {
                                // For pages, look up by token and navigate to page editor
                                if (joinedLink.type == EntityType.PAGE) {
                                    viewModel.openJoinedPageLink(joinedLink) { entityId ->
                                        if (entityId != null) {
                                            onNavigateToPageEditor(entityId, true)
                                        }
                                    }
                                } else {
                                    viewModel.openJoinedLink(joinedLink) { url ->
                                        if (url != null) {
                                            val showUrlBar = !url.contains("hide_ui=true")
                                            onNavigateToBrowser(url, showUrlBar)
                                        }
                                    }
                                }
                            },
                            onLongClick = {
                                selectedLinkForOptions = joinedLink
                            }
                        )
                    }
                }
            }
            }
        }
    }

    if (isInternalTab) {
        content(PaddingValues(0.dp))
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("History", fontWeight = FontWeight.Bold) },
                    actions = {
                        // Points Badge
                        PointsBadge(
                            points = userPoints?.currentBalance ?: 0,
                            onClick = { viewModel.openPointsShop() }
                        )
                        // Star Filter Toggle
                        IconButton(onClick = { viewModel.toggleJoinedLinksStarred() }) {
                            Icon(
                                imageVector = if (showOnlyStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = if (showOnlyStarred) "Show all links" else "Show starred links",
                                tint = if (showOnlyStarred) GoldStart else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        ) { padding ->
            content(padding)
        }
    }

    // Options Modal Bottom Sheet
    if (selectedLinkForOptions != null) {
        LinkOptionsBottomSheet(
            joinedLink = selectedLinkForOptions!!,
            onDismiss = { selectedLinkForOptions = null },
            onToggleStar = {
                viewModel.toggleJoinedLinkStar(selectedLinkForOptions!!.id, selectedLinkForOptions!!.isStarred)
                selectedLinkForOptions = null
            },
            onDelete = {
                viewModel.deleteJoinedLink(selectedLinkForOptions!!.id)
                selectedLinkForOptions = null
            }
        )
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

    // Points Shop Bottom Sheet
    if (showPointsShop) {
        PointsShopSheet(
            currentPoints = userPoints?.currentBalance ?: 0,
            onDismiss = { viewModel.closePointsShop() },
            onWatchAd = { viewModel.earnPointsFromAd(1) },
            onPurchasePackage = { pkg -> viewModel.purchasePoints(pkg.id, pkg.totalPoints) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JoinedLinkCard(
    joinedLink: JoinedLinkEntity,
    onOpen: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onOpen,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 0.2.dp,
        border = BorderStroke(0.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Smaller Icon / Avatar
                val (icon, iconColor) = when (joinedLink.type) {
                    EntityType.FOLDER -> Icons.Default.Folder to SuccessStart
                    EntityType.PAGE -> Icons.Default.Description to PurpleStart
                    else -> Icons.Default.Link to MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = joinedLink.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        if (joinedLink.isStarred) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldStart,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = joinedLink.authorName ?: "Shared Link",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                if (joinedLink.pointsRequired > 0) {
                    Surface(
                        color = GoldStart.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Stars,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = GoldEnd
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${joinedLink.pointsRequired}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = GoldEnd
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onLongClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Update,
                        contentDescription = "Updated",
                        modifier = Modifier.size(10.dp),
                        tint = SuccessStart.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Updated ${getRelativeTime(joinedLink.lastUpdatedTime)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = "Accessed",
                        modifier = Modifier.size(10.dp),
                        tint = InfoStart.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Opened ${getRelativeTime(joinedLink.lastAccessTime)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkOptionsBottomSheet(
    joinedLink: JoinedLinkEntity,
    onDismiss: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        title = joinedLink.name
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text(if (joinedLink.isStarred) "Remove from starred" else "Add to starred") },
                leadingContent = { 
                    Icon(
                        if (joinedLink.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline, 
                        contentDescription = null,
                        tint = if (joinedLink.isStarred) GoldStart else MaterialTheme.colorScheme.onSurface
                    ) 
                },
                modifier = Modifier.clickable { onToggleStar() }
            )
            
            ListItem(
                headlineContent = { Text("Remove from shared list", color = ErrorStart) },
                leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorStart) },
                modifier = Modifier.clickable { onDelete() }
            )
        }
    }
}

@Composable
fun EmptyJoinedLinksCard(showingStarred: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (showingStarred) GoldStart.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(100.dp)
        ) {
            Icon(
                if (showingStarred) Icons.Outlined.StarOutline else Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.padding(24.dp),
                tint = if (showingStarred) GoldStart else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            if (showingStarred) "No starred links" else "No shared links yet",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (showingStarred) 
                "Star your favorite links to quickly access them here." 
            else 
                "Links you access will appear here.\nKeep track of your link history!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}


private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        diff < 2_592_000_000 -> "${diff / 604_800_000}w ago"
        else -> "${diff / 2_592_000_000}mo ago"
    }
}
