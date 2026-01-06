package com.clicktoearn.linkbox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicktoearn.linkbox.ui.viewmodel.SortOption
import com.clicktoearn.linkbox.ui.viewmodel.SortDirection
import kotlinx.coroutines.launch
import com.clicktoearn.linkbox.ui.components.PointsBadge
import com.clicktoearn.linkbox.data.entity.EntityType
import com.clicktoearn.linkbox.ui.viewmodel.LinkBoxViewModel
import com.clicktoearn.linkbox.ui.theme.GoldStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: LinkBoxViewModel,
    onNavigateToPageEditor: (Long, Boolean) -> Unit,
    onNavigateToBrowser: (String, Boolean) -> Unit,
    onNavigateToEarn: () -> Unit = {}
) {
    // Tab data with icons
    data class TabInfo(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val tabs = listOf(
        TabInfo("History", Icons.Default.History),
        TabInfo("My", Icons.Default.Folder)
    )
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val selectedTab = pagerState.currentPage
    
    var showFabMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf<com.clicktoearn.linkbox.data.entity.EntityType?>(null) }

    val userPoints by viewModel.userPoints.collectAsState()
    val myShowOnlyStarred by viewModel.myEntitiesShowOnlyStarred.collectAsState()
    val joinedShowOnlyStarred by viewModel.joinedLinksShowOnlyStarred.collectAsState()
    val currentFolderName by viewModel.currentFolderName.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()
    // Find the current folder object
    val currentFolder = allFolders.find { it.id == currentFolderId }
    
    val mySearchQuery by viewModel.myEntitiesSearchQuery.collectAsState()
    val joinedSearchQuery by viewModel.joinedLinksSearchQuery.collectAsState()
    
    val mySortOption by viewModel.myEntitiesSortOption.collectAsState()
    val mySortDirection by viewModel.myEntitiesSortDirection.collectAsState()
    val joinedSortOption by viewModel.joinedLinksSortOption.collectAsState()
    val joinedSortDirection by viewModel.joinedLinksSortDirection.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    
    // Visibility toggles (independent for each tab)
    var showHistorySearchBar by remember { mutableStateOf(false) }
    var showMySearchBar by remember { mutableStateOf(false) }
    
    var showHistoryCategoryChips by remember { mutableStateOf(false) }
    var showMyCategoryChips by remember { mutableStateOf(false) }

    // Folder Context Menu States (for TopAppBar menu)
    var showFolderMenu by remember { mutableStateOf(false) }
    var showFolderRenameDialog by remember { mutableStateOf(false) }
    var showFolderMoveDialog by remember { mutableStateOf(false) }
    var showFolderShareSheet by remember { mutableStateOf(false) }

    // Handle device back button when inside a folder
    BackHandler(enabled = selectedTab == 1 && currentFolderId != null) {
        viewModel.navigateUp()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        // Show folder name when inside a folder in My tab, otherwise show tab switcher
                        if (selectedTab == 1 && currentFolderId != null) {
                            Text(
                                text = currentFolderName ?: "Folder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            // Integrated Tab Switcher as Title (Compact with Icons)
                            Surface(
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(32.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    tabs.forEachIndexed { index, tabInfo ->
                                        val isSelected = selectedTab == index
                                        val background by animateColorAsState(
                                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            label = "tabBackground"
                                        )
                                        val contentColor by animateColorAsState(
                                            targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            label = "tabContentColor"
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(background)
                                                .clickable {
                                                    scope.launch { pagerState.animateScrollToPage(index) }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = tabInfo.icon,
                                                    contentDescription = null,
                                                    tint = contentColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = tabInfo.title,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = contentColor,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        // Three-dot menu on left side
                        var showOptionsMenu by remember { mutableStateOf(false) }
                        if (selectedTab == 1 && currentFolderId != null) {
                            // Show back arrow when in a folder
                            IconButton(onClick = { viewModel.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            // Show menu icon otherwise
                            Box {
                                IconButton(onClick = { showOptionsMenu = true }) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOptionsMenu,
                                    onDismissRequest = { showOptionsMenu = false },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = { 
                                            val isSearchBarVisible = if (selectedTab == 1) showMySearchBar else showHistorySearchBar
                                            Text(if (isSearchBarVisible) "Hide Search & Filters" else "Show Search & Filters") 
                                        },
                                        leadingIcon = { 
                                            val isSearchBarVisible = if (selectedTab == 1) showMySearchBar else showHistorySearchBar
                                            Icon(
                                                if (isSearchBarVisible) Icons.Default.SearchOff else Icons.Default.Search, 
                                                contentDescription = null
                                            ) 
                                        },
                                        onClick = { 
                                            if (selectedTab == 1) showMySearchBar = !showMySearchBar
                                            else showHistorySearchBar = !showHistorySearchBar
                                            showOptionsMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { 
                                            val isChipsVisible = if (selectedTab == 1) showMyCategoryChips else showHistoryCategoryChips
                                            Text(if (isChipsVisible) "Hide Categories" else "Show Categories") 
                                        },
                                        leadingIcon = { 
                                            val isChipsVisible = if (selectedTab == 1) showMyCategoryChips else showHistoryCategoryChips
                                            Icon(
                                                if (isChipsVisible) Icons.Default.VisibilityOff else Icons.Default.Category, 
                                                contentDescription = null
                                            ) 
                                        },
                                        onClick = { 
                                            if (selectedTab == 1) showMyCategoryChips = !showMyCategoryChips
                                            else showHistoryCategoryChips = !showHistoryCategoryChips
                                            showOptionsMenu = false
                                        }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                        onClick = { 
                                            showOptionsMenu = false
                                            // TODO: Navigate to settings
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Refresh") },
                                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                        onClick = { 
                                            showOptionsMenu = false
                                            // Refresh data
                                        }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            // Show coins for History tab
                            PointsBadge(
                                points = userPoints?.currentBalance ?: 0,
                                onClick = { viewModel.openPointsShop() }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        } else {
                            if (currentFolderId != null && currentFolder != null) {
                                // Show 3-dot menu for Folder Actions
                                Box {
                                    IconButton(onClick = { showFolderMenu = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Folder Options",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showFolderMenu,
                                        onDismissRequest = { showFolderMenu = false },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rename") },
                                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                            onClick = { 
                                                showFolderMenu = false
                                                showFolderRenameDialog = true 
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Change Location") },
                                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                                            onClick = { 
                                                showFolderMenu = false
                                                showFolderMoveDialog = true 
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                            onClick = { 
                                                showFolderMenu = false
                                                showFolderShareSheet = true 
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (currentFolder.isPinned) "Unstar" else "Star", color = GoldStart) },
                                            leadingIcon = { 
                                                Icon(
                                                    if (currentFolder.isPinned) Icons.Outlined.StarOutline else Icons.Default.Star, 
                                                    contentDescription = null,
                                                    tint = GoldStart
                                                ) 
                                            },
                                            onClick = { 
                                                showFolderMenu = false
                                                viewModel.toggleEntityPin(currentFolder.id, currentFolder.isPinned)
                                            }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        DropdownMenuItem(
                                            text = { Text("Delete Folder", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = { 
                                                showFolderMenu = false
                                                viewModel.deleteEntity(currentFolder)
                                                viewModel.navigateUp() // Go back after deleting
                                            }
                                        )
                                    }
                                }
                            } else {
                                // Show Earn button for My tab root
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.clickable { onNavigateToEarn() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Stars,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Earn",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Animated Search Bar and Filters
                val isSearchBarVisible = if (selectedTab == 1) showMySearchBar else showHistorySearchBar
                AnimatedVisibility(
                    visible = (selectedTab != 1 || currentFolderId == null) && isSearchBarVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = if (selectedTab == 1) mySearchQuery else joinedSearchQuery,
                            onValueChange = { 
                                if (selectedTab == 1) viewModel.searchMyEntities(it) 
                                else viewModel.searchJoinedLinks(it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            placeholder = { Text("Search files...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (if (selectedTab == 1) mySearchQuery.isNotEmpty() else joinedSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { 
                                            if (selectedTab == 1) viewModel.searchMyEntities("") 
                                            else viewModel.searchJoinedLinks("")
                                        },
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
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = { 
                                if (selectedTab == 1) viewModel.toggleMyEntitiesStarred()
                                else viewModel.toggleJoinedLinksStarred()
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            val activeStarred = if (selectedTab == 1) myShowOnlyStarred else joinedShowOnlyStarred
                            Icon(
                                imageVector = if (activeStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (activeStarred) GoldStart else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    // Small spacer below search bar
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Sorting Dropdown (Floating)
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        offset = androidx.compose.ui.unit.DpOffset(x = 300.dp, y = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                            val currentSortOption = if (selectedTab == 1) mySortOption else joinedSortOption
                            val currentSortDirection = if (selectedTab == 1) mySortDirection else joinedSortDirection
                            
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(option.name.lowercase().replaceFirstChar { it.uppercase() })
                                            if (currentSortOption == option) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    if (currentSortDirection == SortDirection.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        val newDirection = if (currentSortOption == option) {
                                            if (currentSortDirection == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
                                        } else {
                                            SortDirection.ASCENDING
                                        }
                                        
                                        if (selectedTab == 1) {
                                            viewModel.setMyEntitiesSort(option, newDirection)
                                        } else {
                                            viewModel.setJoinedLinksSort(option, newDirection)
                                        }
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (currentSortOption == option) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                    }
                }
            }
        },
        floatingActionButton = {
            // Only show FAB in My tab (index 1)
            if (selectedTab == 1) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showFabMenu,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            FabMenuOption(
                                icon = Icons.Default.Folder,
                                label = "Folder",
                                color = Color(0xFFFFC107),
                                onClick = {
                                    showFabMenu = false
                                    showAddDialog = com.clicktoearn.linkbox.data.entity.EntityType.FOLDER
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FabMenuOption(
                                icon = Icons.Default.Description,
                                label = "Page",
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = {
                                    showFabMenu = false
                                    onNavigateToPageEditor(-1L, false)
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            FabMenuOption(
                                icon = Icons.Default.Link,
                                label = "Link",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    showFabMenu = false
                                    showAddDialog = com.clicktoearn.linkbox.data.entity.EntityType.LINK
                                }
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // Background Overlay when FAB menu is open
            androidx.compose.animation.AnimatedVisibility(
                visible = showFabMenu,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                        .pointerInput(Unit) {
                            detectTapGestures { showFabMenu = false }
                        }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> JoinedScreen(
                        viewModel = viewModel,
                        onNavigateToBrowser = onNavigateToBrowser,
                        onNavigateToPageEditor = onNavigateToPageEditor,
                        isInternalTab = true,
                        showCategoryChips = showHistoryCategoryChips
                    )
                    1 -> MyScreen(
                        viewModel = viewModel,
                        onNavigateToPageEditor = onNavigateToPageEditor,
                        onNavigateToBrowser = onNavigateToBrowser,
                        isInternalTab = true,
                        showCategoryChips = showMyCategoryChips
                    )
                }
            }

            if (showAddDialog != null) {
                AddEntitySheet(
                    type = showAddDialog!!,
                    folders = allFolders,
                    currentParentId = currentFolderId,
                    onDismiss = { showAddDialog = null },
                    onAdd = { name, type, value, parentId ->
                        viewModel.addEntity(name, type, value, parentId)
                        showAddDialog = null
                    }
                )
            }
            
            // Folder Management Dialogs
            if (showFolderRenameDialog && currentFolder != null) {
                RenameSheet(
                    entity = currentFolder,
                    onDismiss = { showFolderRenameDialog = false },
                    onRename = { newName: String ->
                        viewModel.renameEntity(currentFolder.id, newName)
                        showFolderRenameDialog = false
                    }
                )
            }
            
            if (showFolderMoveDialog && currentFolder != null) {
                MoveEntitySheet(
                    entity = currentFolder,
                    folders = allFolders.filter { it.id != currentFolder.id },
                    onDismiss = { showFolderMoveDialog = false },
                    onMove = { newParentId: Long? ->
                        viewModel.moveEntity(currentFolder.id, newParentId)
                        showFolderMoveDialog = false
                        viewModel.navigateUp() // Navigate out since we moved it
                    }
                )
            }
            
            if (showFolderShareSheet && currentFolder != null) {
                ShareSheet(
                    entity = currentFolder,
                    viewModel = viewModel,
                    onDismiss = { showFolderShareSheet = false }
                )
            }
        }
    }
}
