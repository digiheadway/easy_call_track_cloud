package com.miniclick.calltrackmanage.ui.home

import com.miniclick.calltrackmanage.ui.home.viewmodel.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import java.util.Calendar
import androidx.activity.compose.BackHandler

@Composable
fun CallsHeader(
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isFilterActive: Boolean,
    filterCount: Int,
    dateRange: DateRange,
    onDateRangeChange: (DateRange, Long?, Long?) -> Unit,
    currentSort: String,
    sortDirection: SortDirection,
    onSortSelect: (String) -> Unit,
    onToggleSortDirection: () -> Unit,
    onQuickFilter: (String) -> Unit,
    onClearFilters: () -> Unit,
    onReorderTabs: () -> Unit,
    isReviewedFilterActive: Boolean = false,
    isPersonNotesFilterActive: Boolean = false,
    isContactsFilterActive: Boolean = false,
    showIgnoredOnly: Boolean = false,
    onToggleIgnoredOnly: () -> Unit = {},
    onTitleLongClick: () -> Unit = {},
    onViewModeLongClick: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    var showMenu by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    // Back button: first clears search input, then closes search bar
    BackHandler(enabled = isSearchActive) {
        if (searchQuery.isNotEmpty()) {
            onSearchQueryChange("")
        } else {
            onSearchClick()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .height(56.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isSearchActive) {
            SearchHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearchClick = onSearchClick,
                onMenuClick = { showMenu = true },
                filterCount = filterCount,
                focusRequester = focusRequester
            )
        } else {
            StandardHeader(
                viewMode = viewMode,
                onToggleViewMode = onToggleViewMode,
                onTitleLongClick = onTitleLongClick,
                onViewModeLongClick = onViewModeLongClick,
                onSearchClick = onSearchClick,
                onMenuClick = { showMenu = true },
                filterCount = filterCount
            )
        }

        // Unified Options Menu for both Search and Normal Modes
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            UnifiedOptionsMenu(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                onShowDateMenu = { 
                    showMenu = false
                    showDateMenu = true 
                },
                onShowSortMenu = {
                    showMenu = false
                    showSortMenu = true
                },
                dateRange = dateRange,
                onDateRangeChange = onDateRangeChange,
                filterCount = filterCount,
                onFilterClick = onFilterClick,
                isReviewedFilterActive = isReviewedFilterActive,
                isPersonNotesFilterActive = isPersonNotesFilterActive,
                isContactsFilterActive = isContactsFilterActive,
                onQuickFilter = onQuickFilter,
                onClearFilters = onClearFilters,
                currentSort = currentSort,
                viewMode = viewMode,
                onSortSelect = onSortSelect,
                sortDirection = sortDirection,
                onToggleSortDirection = onToggleSortDirection,
                onReorderTabs = onReorderTabs,
                showIgnoredOnly = showIgnoredOnly,
                onToggleIgnoredOnly = onToggleIgnoredOnly
            )

            DateOptionsMenu(
                expanded = showDateMenu,
                onDismiss = { showDateMenu = false },
                dateRange = dateRange,
                onDateRangeChange = onDateRangeChange
            )

            SortOptionsMenu(
                expanded = showSortMenu,
                onDismiss = { showSortMenu = false },
                viewMode = viewMode,
                currentSort = currentSort,
                onSortSelect = onSortSelect
            )
        }
    }
}

@Composable
private fun SearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    filterCount: Int,
    focusRequester: FocusRequester
) {
    // Search Bar with matching theme colors
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { 
                    Text(
                        "Search calls, numbers...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear, 
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Three-dot menu in search mode
            IconButton(onClick = onMenuClick) {
                BadgedBox(
                    badge = {
                        if (filterCount > 0) {
                            Badge { Text(filterCount.toString()) }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = if (filterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardHeader(
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit,
    onTitleLongClick: () -> Unit,
    onViewModeLongClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    filterCount: Int
) {
    // Normal Header
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onTitleLongClick() })
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Calls",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onToggleViewMode,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(24.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { onViewModeLongClick() },
                                onTap = { onToggleViewMode() }
                            )
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch View Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = if (viewMode == ViewMode.CALLS) "Showing Each Call Separately" else "Grouped by Phone Numbers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onMenuClick) {
                BadgedBox(
                    badge = {
                        if (filterCount > 0) {
                            Badge { Text(filterCount.toString()) }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = if (filterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }
    }
}

@Composable
private fun UnifiedOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onShowDateMenu: () -> Unit,
    onShowSortMenu: () -> Unit,
    dateRange: DateRange,
    onDateRangeChange: (DateRange, Long?, Long?) -> Unit,
    filterCount: Int,
    onFilterClick: () -> Unit,
    isReviewedFilterActive: Boolean,
    isPersonNotesFilterActive: Boolean,
    isContactsFilterActive: Boolean,
    onQuickFilter: (String) -> Unit,
    onClearFilters: () -> Unit,
    currentSort: String,
    viewMode: ViewMode,
    onSortSelect: (String) -> Unit,
    sortDirection: SortDirection,
    onToggleSortDirection: () -> Unit,
    onReorderTabs: () -> Unit,
    showIgnoredOnly: Boolean,
    onToggleIgnoredOnly: () -> Unit
) {
    // We pass expanded to DropdownMenu, but we control 'showMenu' (which is expanded here) from parent
    // The parent manages 'expanded' state. if false, this composable shouldn't be called.
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        // Date Range with Sub-menu
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Date: ${dateRange.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }}")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp))
                }
            },
            onClick = { 
                onShowDateMenu()
            },
            leadingIcon = { Icon(Icons.Default.DateRange, null) }
        )

        // Filters with count
        DropdownMenuItem(
            text = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Filters")
                    if (filterCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = filterCount.toString(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            },
            onClick = { 
                onFilterClick()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.FilterList, null) }
        )
        
        HorizontalDivider()

        // Quick Filters Section Label
        Text(
            text = "Quick Filters",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        DropdownMenuItem(
            text = { Text("Non-Reviewed Only") },
            onClick = { 
                onQuickFilter("NOT_REVIEWED")
                onDismiss() 
            },
            leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
            trailingIcon = {
                if (isReviewedFilterActive) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
        DropdownMenuItem(
            text = { Text("Without Person Note") },
            onClick = { 
                onQuickFilter("WITHOUT_PERSON_NOTE")
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.SpeakerNotesOff, null) },
            trailingIcon = {
                if (isPersonNotesFilterActive) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
        DropdownMenuItem(
            text = { Text("Non-Contacts Only") },
            onClick = { 
                onQuickFilter("NOT_IN_CONTACTS")
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.NoAccounts, null) },
            trailingIcon = {
                if (isContactsFilterActive) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
        DropdownMenuItem(
            text = { Text("Clear Filters") },
            onClick = { 
                onClearFilters()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.ClearAll, null) }
        )

        HorizontalDivider()

        // Sorting Section Label
        Text(
            text = "Sorting",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        DropdownMenuItem(
            text = { 
                val sortLabel = when (currentSort) {
                    "DATE" -> "Call Time"
                    "DURATION" -> "Call Duration"
                    "NUMBER" -> "Number"
                    "LAST_CALL" -> "Last Call Time"
                    "TOTAL_CALLS" -> "Total Number of Calls"
                    "TOTAL_DURATION" -> "Total Call Duration"
                    "NAME" -> "Name"
                    else -> currentSort
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sort: $sortLabel")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp))
                }
            },
            onClick = { 
                onShowSortMenu()
            },
            leadingIcon = { Icon(Icons.Default.Sort, null) }
        )
        DropdownMenuItem(
            text = { Text(if (sortDirection == SortDirection.DESCENDING) "Order: Newest First" else "Order: Oldest First") },
            onClick = { 
                onToggleSortDirection()
                onDismiss() 
            },
            leadingIcon = { Icon(if (sortDirection == SortDirection.DESCENDING) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null) }
        )

        DropdownMenuItem(
            text = { Text("Reorder Tabs") },
            onClick = { 
                onReorderTabs()
                onDismiss() 
            },
            leadingIcon = { Icon(Icons.Default.Reorder, null) }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text("Show Ignored Only") },
            onClick = { 
                onToggleIgnoredOnly()
                onDismiss() 
            },
            leadingIcon = { Icon(Icons.Default.Block, null) },
            trailingIcon = {
                if (showIgnoredOnly) {
                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
private fun DateOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    dateRange: DateRange,
    onDateRangeChange: (DateRange, Long?, Long?) -> Unit
) {
    // Date Range Sub-menu
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DateRange.entries.filter { it != DateRange.CUSTOM }.forEach { range ->
            DropdownMenuItem(
                text = { 
                    Text(
                        range.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                        fontWeight = if (range == dateRange) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                onClick = { 
                    onDateRangeChange(range, null, null)
                    onDismiss()
                },
                trailingIcon = {
                    if (range == dateRange) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}

@Composable
private fun SortOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewMode: ViewMode,
    currentSort: String,
    onSortSelect: (String) -> Unit
) {
    // Sort By Sub-menu
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (viewMode == ViewMode.CALLS) {
            // Call view sorting
            DropdownMenuItem(
                text = { 
                    Text(
                        "Call Time",
                        fontWeight = if (currentSort == "DATE") FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                onClick = { 
                    onSortSelect("DATE")
                    onDismiss()
                },
                trailingIcon = {
                    if (currentSort == "DATE") {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            DropdownMenuItem(
                text = { 
                    Text(
                        "Call Duration",
                        fontWeight = if (currentSort == "DURATION") FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                onClick = { 
                    onSortSelect("DURATION")
                    onDismiss()
                },
                trailingIcon = {
                    if (currentSort == "DURATION") {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        } else {
            // Grouped view sorting
            DropdownMenuItem(
                text = { 
                    Text(
                        "Last Call Time",
                        fontWeight = if (currentSort == "LAST_CALL") FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                onClick = { 
                    onSortSelect("LAST_CALL")
                    onDismiss()
                },
                trailingIcon = {
                    if (currentSort == "LAST_CALL") {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            DropdownMenuItem(
                text = { 
                    Text(
                        "Total Number of Calls",
                        fontWeight = if (currentSort == "TOTAL_CALLS") FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                onClick = { 
                    onSortSelect("TOTAL_CALLS")
                    onDismiss()
                },
                trailingIcon = {
                    if (currentSort == "TOTAL_CALLS") {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            DropdownMenuItem(
                text = { 
                    Text(
                        "Total Call Duration",
                        fontWeight = if (currentSort == "TOTAL_DURATION") FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                onClick = { 
                    onSortSelect("TOTAL_DURATION")
                    onDismiss()
                },
                trailingIcon = {
                    if (currentSort == "TOTAL_DURATION") {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            DropdownMenuItem(
                text = { 
                    Text(
                        "Name",
                        fontWeight = if (currentSort == "NAME") FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                onClick = { 
                    onSortSelect("NAME")
                    onDismiss()
                },
                trailingIcon = {
                    if (currentSort == "NAME") {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}

// PersonsHeader removed in favor of unified CallsHeader design
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabInfoModal(
    tabName: String,
    description: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Text(
                text = tabName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(12.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified // Default
            )
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(
                    "Got it",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickDateJumpBottomSheet(
    dateSummaries: List<DateSectionSummary>,
    currentDateRange: DateRange,
    onDateRangeSelect: (DateRange) -> Unit,
    onDateClick: (DateSectionSummary) -> Unit,
    onDismiss: () -> Unit,
    viewMode: ViewMode,
    onToggleViewMode: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Month filter state
    var selectedMonth by remember { mutableStateOf<Pair<String, Int>?>(null) }
    
    // Filter dates by selected month
    val filteredDates = remember(dateSummaries, selectedMonth) {
        if (selectedMonth == null) dateSummaries
        else dateSummaries.filter { it.month == selectedMonth!!.first && it.year == selectedMonth!!.second }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row: Title + Date Range Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Jump to Date",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Compact Date Range Dropdown
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Surface(
                        onClick = { expanded = true },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentDateRange.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown, 
                                null, 
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DateRange.entries.filter { it != DateRange.CUSTOM }.forEach { range ->
                            DropdownMenuItem(
                                text = { Text(range.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    onDateRangeSelect(range)
                                    selectedMonth = null // Reset month filter on range change
                                    expanded = false
                                },
                                trailingIcon = {
                                    if (range == currentDateRange) {
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Month Filter Chips
            val months = remember(dateSummaries) {
                dateSummaries.map { it.month to it.year }.distinct()
            }
            
            if (months.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // "All" chip
                    FilterChip(
                        selected = selectedMonth == null,
                        onClick = { selectedMonth = null },
                        label = { Text("All") }
                    )
                    
                    months.forEach { (month, year) ->
                        val isCurrentYear = Calendar.getInstance().get(Calendar.YEAR) == year
                        val label = if (isCurrentYear) month else "$month $year"
                        val isSelected = selectedMonth?.first == month && selectedMonth?.second == year
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                selectedMonth = if (isSelected) null else (month to year)
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            
            // Available Dates List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedMonth != null) "${filteredDates.size} dates" else "${dateSummaries.size} dates",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 350.dp)
            ) {
                items(filteredDates) { summary ->
                    Surface(
                        onClick = { onDateClick(summary) },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${summary.dateLabel}, ${summary.dayOfWeek}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${summary.totalCalls} calls • ${summary.uniqueCalls} unique",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight, 
                                null, 
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
            
            // Grouping Info (Compact)
            if (viewMode == ViewMode.PERSONS) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Grouped by person",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onToggleViewMode,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Show calls", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
