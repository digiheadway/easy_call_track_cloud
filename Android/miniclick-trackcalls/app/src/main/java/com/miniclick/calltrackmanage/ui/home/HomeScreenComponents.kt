package com.miniclick.calltrackmanage.ui.home

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
                    IconButton(onClick = { showMenu = true }) {
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
        } else {
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

                    Box {
                        IconButton(onClick = { showMenu = true }) {
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
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
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
                                    showDateMenu = true
                                    showMenu = false
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
                                    showMenu = false 
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
                                    showMenu = false 
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
                                    showMenu = false 
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
                                    showMenu = false 
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
                                    showMenu = false 
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
                                    showSortMenu = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Sort, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (sortDirection == SortDirection.DESCENDING) "Order: Newest First" else "Order: Oldest First") },
                                onClick = { 
                                    onToggleSortDirection()
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(if (sortDirection == SortDirection.DESCENDING) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null) }
                            )

                            DropdownMenuItem(
                                text = { Text("Reorder Tabs") },
                                onClick = { 
                                    onReorderTabs()
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.Reorder, null) }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Show Ignored Only") },
                                onClick = { 
                                    onToggleIgnoredOnly()
                                    showMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.Block, null) },
                                trailingIcon = {
                                    if (showIgnoredOnly) {
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }

                        // Date Range Sub-menu
                        DropdownMenu(
                            expanded = showDateMenu,
                            onDismissRequest = { showDateMenu = false }
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
                                        showDateMenu = false 
                                    },
                                    trailingIcon = {
                                        if (range == dateRange) {
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }

                        // Sort By Sub-menu
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
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
                                        showSortMenu = false 
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
                                        showSortMenu = false 
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
                                        showSortMenu = false 
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
                                        showSortMenu = false 
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
                                        showSortMenu = false 
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
                                        showSortMenu = false 
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
                }
            }
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
            Text(
                text = "Jump to Date",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Date Range Selector
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Date Range",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentDateRange.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(Icons.Default.ArrowDropDown, null)
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
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Month Chips (Navigation within the list below)
            val months = remember(dateSummaries) {
                dateSummaries.map { it.month to it.year }.distinct()
            }
            
            if (months.size > 1) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    months.forEach { (month, year) ->
                        val isCurrentYear = Calendar.getInstance().get(Calendar.YEAR) == year
                        val label = if (isCurrentYear) month else "$month $year"
                        FilterChip(
                            selected = false,
                            onClick = { 
                                // Scroll to first date of this month
                                dateSummaries.find { it.month == month && it.year == year }?.let { onDateClick(it) }
                            },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            
            // Available Dates List
            Text(
                text = "Available Dates",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp)
            ) {
                items(dateSummaries) { summary ->
                    Surface(
                        onClick = { onDateClick(summary) },
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${summary.dateLabel}, ${summary.dayOfWeek}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Call, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${summary.totalCalls} Calls",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "  |  ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Icon(Icons.Default.Person, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${summary.uniqueCalls} Unique",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Grouping Info and Toggle
            if (viewMode == ViewMode.PERSONS) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Calls are grouped by phone numbers. Each person is shown under the day they last called you.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(
                                onClick = onToggleViewMode,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Do not group (Show each call)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
