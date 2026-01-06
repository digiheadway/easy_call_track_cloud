package com.example.salescrm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.salescrm.data.*
import com.example.salescrm.ui.components.*
import com.example.salescrm.ui.theme.*
import java.time.LocalDate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// ==================== PIPELINE SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineScreen(
    people: List<Person>,
    onPersonClick: (Person) -> Unit,
    onAddPerson: () -> Unit,
    currencySymbol: String = "₹",
    budgetMultiplier: Int = 100000,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showFiltersRow by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.CARD) }
    var showColumnSettings by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewModeSheet by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val customStages = SalesCrmTheme.stages
    val customPriorities = SalesCrmTheme.priorities
    val customSegments = SalesCrmTheme.segments
    
    // Filter states
    var selectedSegmentId by remember { mutableStateOf<String?>(null) }
    var selectedStageId by remember { mutableStateOf<String?>(null) }
    var selectedPriorityId by remember { mutableStateOf<String?>(null) }
    var selectedBudgetRange by remember { mutableStateOf<String?>(null) }
    var selectedLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSource by remember { mutableStateOf<String?>(null) }

    // Column visibility state
    var visibleColumns by remember { 
        mutableStateOf(defaultPipelineColumns.filter { it.visible }.map { it.id }.toSet()) 
    }
    
    // Sort state with new options
    var sortBy by remember { mutableStateOf("id") }
    var sortDirection by remember { mutableStateOf(SortDirection.DESC) }
    
    // Define all sort options with icons
    val pipelineSortOptions = listOf(
        SortOption("id", "Id", Icons.Default.Tag),
        SortOption("name", "Name", Icons.Default.Person),
        SortOption("budget", "Budget", Icons.Default.AttachMoney),
        SortOption("last_opened", "Last Opened", Icons.Default.Visibility),
        SortOption("last_activity", "Last Activity", Icons.Default.TrendingUp),
        SortOption("last_modified", "Last Modified", Icons.Default.Edit),
        SortOption("created", "Created", Icons.Default.CalendarMonth),
        SortOption("priority", "Priority", Icons.Default.Flag),
        SortOption("num_activities", "Number of Activities", Icons.Default.Analytics)
    )
    
    // Only show people in pipeline
    val pipelinePeople = people.filter { it.isInPipeline }
    
    // Budget ranges for filtering
    val budgetRanges = listOf("< $10K", "$10K - $25K", "$25K - $50K", "> $50K")
    
    // Get active filter count
    val activeFilterCount = listOfNotNull(
        selectedStageId,
        selectedPriorityId,
        selectedBudgetRange,
        selectedSource,
        if (selectedLabels.isNotEmpty()) "labels" else null
    ).size
    
    // Apply filters and sorting with direction support
    val filteredPeople = pipelinePeople.filter { person ->
        (searchQuery.isEmpty() || 
         person.name.contains(searchQuery, true) || 
         person.phone.contains(searchQuery, true) ||
         person.note.contains(searchQuery, true)) &&
        (selectedSegmentId == null || person.segmentId == selectedSegmentId) &&
        (selectedStageId == null || person.stageId == selectedStageId) &&
        (selectedPriorityId == null || person.pipelinePriorityId == selectedPriorityId) &&
        (selectedSource == null || person.sourceId == selectedSource) &&
        (selectedLabels.isEmpty() || person.labels.any { it in selectedLabels })
    }.let { list ->
        val baseComparator: Comparator<Person> = when (sortBy) {
            "id" -> compareBy { it.id }
            "name" -> compareBy { it.name.lowercase() }
            "budget" -> compareBy { it.budget.toIntOrNull() ?: 0 }
            "last_opened" -> compareBy { it.lastOpenedAt ?: LocalDate.MIN }
            "last_activity" -> compareBy { it.lastActivityAt ?: LocalDate.MIN }
            "last_modified" -> compareBy { it.updatedAt }
            "created" -> compareBy { it.createdAt }
            "priority" -> compareBy { customPriorities.findById(it.pipelinePriorityId)?.order ?: 0 }
            "num_activities" -> compareBy { it.activityCount }
            else -> compareBy { it.name.lowercase() }
        }
        
        if (sortDirection == SortDirection.DESC) {
            list.sortedWith(baseComparator.reversed())
        } else {
            list.sortedWith(baseComparator)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SalesCrmTheme.colors.background)
    ) {
        // ===== HEADER: Title/Segment (Left) + Actions (Right) =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Segment Selector as Title
            var showSegmentSheet by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .clickable { showSegmentSheet = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeFilterCount > 0 || searchQuery.isNotEmpty()) {
                        "${selectedSegmentId?.let { customSegments.findById(it)?.label } ?: "All"} (${filteredPeople.size})"
                    } else {
                        "${selectedSegmentId?.let { customSegments.findById(it)?.label } ?: "All Segments"} (${filteredPeople.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Segment",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Segment Selection Bottom Sheet
            if (showSegmentSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSegmentSheet = false },
                    containerColor = SalesCrmTheme.colors.surface
                ) {
                    Column(
                        Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Select Segment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SalesCrmTheme.colors.textPrimary
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        // All Segments option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSegmentId = null
                                    showSegmentSheet = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "All Segments",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SalesCrmTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedSegmentId == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryBlue
                                )
                            }
                        }
                        HorizontalDivider(color = SalesCrmTheme.colors.border)
                        
                        // Individual segments
                        customSegments.forEach { segmentItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSegmentId = segmentItem.id
                                        showSegmentSheet = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            androidx.compose.ui.graphics.Color(segmentItem.color),
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    segmentItem.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SalesCrmTheme.colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedSegmentId == segmentItem.id) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                            HorizontalDivider(color = SalesCrmTheme.colors.border)
                        }
                        
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
            
            // Right Actions: Search, Filter, More
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { 
                    showSearch = !showSearch
                    if (showSearch) {
                        coroutineScope.launch {
                            delay(100)
                            searchFocusRequester.requestFocus()
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (showSearch) PrimaryBlue else SalesCrmTheme.colors.textSecondary
                    )
                }
                
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge(containerColor = PrimaryBlue) {
                                Text(activeFilterCount.toString())
                            }
                        }
                    }
                ) {
                    IconButton(onClick = { showFiltersRow = !showFiltersRow }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (showFiltersRow) PrimaryBlue else SalesCrmTheme.colors.textSecondary
                        )
                    }
                }
                
                // Three-dot menu
                var moreExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { moreExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = SalesCrmTheme.colors.textSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = moreExpanded,
                        onDismissRequest = { moreExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by") },
                            onClick = { 
                                moreExpanded = false
                                showSortSheet = true 
                            },
                            leadingIcon = { Icon(Icons.Default.Sort, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Customize Columns") },
                            onClick = { 
                                moreExpanded = false
                                showColumnSettings = true 
                            },
                            leadingIcon = { Icon(Icons.Default.ViewColumn, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("View Mode") },
                            onClick = { 
                                moreExpanded = false
                                showViewModeSheet = true
                            },
                            leadingIcon = { 
                                Icon(
                                    when (viewMode) {
                                        ViewMode.CARD -> Icons.Default.GridView
                                        ViewMode.LIST -> Icons.Default.ViewList
                                        ViewMode.TABLE -> Icons.Default.TableChart
                                    },
                                    null
                                ) 
                            }
                        )
                    }
                }
            }
        }
        
        // ===== SEARCH BAR (Toggleable) =====
        AnimatedVisibility(
            visible = showSearch,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LaunchedEffect(Unit) {
                searchFocusRequester.requestFocus()
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                CompactSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                    placeholder = "Search pipeline..."
                )
            }
        }
        
        // ===== THIRD ROW: Expandable Filter Tags (Horizontal Dropdowns) =====
        androidx.compose.animation.AnimatedVisibility(
            visible = showFiltersRow,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            // Shared colors (Must be resolved in Composable scope)
            val primaryColor = MaterialTheme.colorScheme.primary
            val textSecondary = SalesCrmTheme.colors.textSecondary
            
            // Common chip colors (Composable call)
            val commonChipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                labelColor = textSecondary,
                selectedLabelColor = primaryColor
            )

            // Reset colors
            val resetChipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentRed.copy(alpha = 0.1f),
                selectedLabelColor = AccentRed,
                selectedLeadingIconColor = AccentRed
            )
            val resetBorder = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = true,
                borderColor = AccentRed.copy(alpha = 0.2f)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reset Button (if filters active)
                if (activeFilterCount > 0) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = {
                                selectedStageId = null
                                selectedPriorityId = null
                                selectedBudgetRange = null
                                selectedLabels = emptySet()
                                selectedSource = null
                            },
                            label = { Icon(Icons.Default.Close, "Reset", modifier = Modifier.size(16.dp)) },
                            colors = resetChipColors,
                            border = resetBorder
                        )
                    }
                }

                // Stage Dropdown
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = selectedStageId != null,
                            onClick = { expanded = true },
                            label = { Text(selectedStageId?.let { customStages.findById(it)?.label } ?: "Stage") },
                            trailingIcon = { 
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            colors = commonChipColors
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Stages") },
                                onClick = { 
                                    selectedStageId = null
                                    expanded = false 
                                }
                            )
                            customStages.forEach { stageItem ->
                                DropdownMenuItem(
                                    text = { Text(stageItem.label) },
                                    onClick = { 
                                        selectedStageId = stageItem.id
                                        expanded = false 
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(androidx.compose.ui.graphics.Color(stageItem.color), androidx.compose.foundation.shape.CircleShape)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Priority Dropdown
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = selectedPriorityId != null,
                            onClick = { expanded = true },
                            label = { Text(selectedPriorityId?.let { customPriorities.findById(it)?.label } ?: "Priority") },
                            trailingIcon = { 
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            colors = commonChipColors
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Priorities") },
                                onClick = { 
                                    selectedPriorityId = null
                                    expanded = false 
                                }
                            )
                            customPriorities.forEach { priorityItem ->
                                DropdownMenuItem(
                                    text = { Text(priorityItem.label) },
                                    onClick = { 
                                        selectedPriorityId = priorityItem.id
                                        expanded = false 
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(androidx.compose.ui.graphics.Color(priorityItem.color), androidx.compose.foundation.shape.CircleShape)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Budget Dropdown
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = selectedBudgetRange != null,
                            onClick = { expanded = true },
                            label = { Text(selectedBudgetRange ?: "Budget") },
                            trailingIcon = { 
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            colors = commonChipColors
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Any Budget") },
                                onClick = { 
                                    selectedBudgetRange = null
                                    expanded = false 
                                }
                            )
                            budgetRanges.forEach { range ->
                                DropdownMenuItem(
                                    text = { Text(range) },
                                    onClick = { 
                                        selectedBudgetRange = range
                                        expanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Labels Dropdown
                item {
                    var expanded by remember { mutableStateOf(false) }
                    val labelText = if (selectedLabels.isEmpty()) "Labels" else "${selectedLabels.size} Label(s)"
                    Box {
                        FilterChip(
                            selected = selectedLabels.isNotEmpty(),
                            onClick = { expanded = true },
                            label = { Text(labelText) },
                            trailingIcon = { 
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            colors = commonChipColors
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Labels") },
                                onClick = { 
                                    selectedLabels = emptySet()
                                    expanded = false 
                                }
                            )
                            SampleData.availableLabels.forEach { label ->
                                val isSelected = label in selectedLabels
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { 
                                        selectedLabels = if (isSelected) selectedLabels - label else selectedLabels + label
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Source Dropdown
                item {
                    var expanded by remember { mutableStateOf(false) }
                    val customSources = SalesCrmTheme.sources
                    Box {
                        FilterChip(
                            selected = selectedSource != null,
                            onClick = { expanded = true },
                            label = { Text(selectedSource?.let { customSources.findById(it)?.label } ?: "Source") },
                            trailingIcon = { 
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            colors = commonChipColors
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sources") },
                                onClick = { 
                                    selectedSource = null
                                    expanded = false 
                                }
                            )
                            customSources.forEach { sourceItem ->
                                DropdownMenuItem(
                                    text = { Text(sourceItem.label) },
                                    onClick = { 
                                        selectedSource = sourceItem.id
                                        expanded = false 
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(androidx.compose.ui.graphics.Color(sourceItem.color), androidx.compose.foundation.shape.CircleShape)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        HorizontalDivider(color = SalesCrmTheme.colors.border, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
        
        if (filteredPeople.isEmpty()) {
            EmptyState(
                Icons.Default.Star,
                "No results found",
                if (searchQuery.isNotEmpty() || activeFilterCount > 0) "Try adjusting your filters"
                else "Tap + to add a new person"
            )
        } else {
            PersonList(
                people = filteredPeople,
                viewMode = viewMode,
                visibleColumns = visibleColumns,
                onPersonClick = onPersonClick,
                currencySymbol = currencySymbol,
                budgetMultiplier = budgetMultiplier
            )
        }
    }
    
    // Column Settings Bottom Sheet
    if (showColumnSettings) {
        ModalBottomSheet(
            onDismissRequest = { showColumnSettings = false },
            containerColor = SalesCrmTheme.colors.surface
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Customize Fields",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "Choose which fields to show in the list",
                    style = MaterialTheme.typography.bodySmall,
                    color = SalesCrmTheme.colors.textSecondary
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Column toggles
                defaultPipelineColumns.forEach { column ->
                    val isChecked = column.id in visibleColumns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                visibleColumns = if (isChecked) visibleColumns - column.id else visibleColumns + column.id
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(column.label, color = SalesCrmTheme.colors.textPrimary)
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                visibleColumns = if (checked) visibleColumns + column.id else visibleColumns - column.id
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryBlue,
                                checkedTrackColor = PrimaryBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                    HorizontalDivider(color = SalesCrmTheme.colors.border)
                }
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        showColumnSettings = false 
                        Toast.makeText(context, "Fields updated", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Done")
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    // Sort By Bottom Sheet
    if (showSortSheet) {
        SortBySheet(
            sortOptions = pipelineSortOptions,
            selectedSortBy = sortBy,
            selectedDirection = sortDirection,
            onSortByChange = { sortBy = it },
            onDirectionChange = { sortDirection = it },
            onDismiss = { 
                showSortSheet = false 
                Toast.makeText(context, "Sorting updated", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // View Mode Bottom Sheet
    if (showViewModeSheet) {
        ViewModeSheet(
            currentMode = viewMode,
            onModeSelected = { 
                viewMode = it 
                Toast.makeText(context, "View mode: ${it.name}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showViewModeSheet = false }
        )
    }
}

// ==================== CONTACTS SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    people: List<Person>,
    onPersonClick: (Person) -> Unit,
    onAddPerson: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showFiltersRow by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.CARD) }
    var showColumnSettings by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showViewModeSheet by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val customSegments = SalesCrmTheme.segments
    val customPriorities = SalesCrmTheme.priorities

    // Filter states
    var selectedSegmentId by remember { mutableStateOf<String?>(null) }
    var selectedLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSource by remember { mutableStateOf<String?>(null) }
    
    // Column visibility state
    var visibleColumns by remember { 
        mutableStateOf(defaultContactColumns.filter { it.visible }.map { it.id }.toSet()) 
    }
    
    // Sort state with new options
    var sortBy by remember { mutableStateOf("id") }
    var sortDirection by remember { mutableStateOf(SortDirection.DESC) }
    
    // Define all sort options with icons
    val contactSortOptions = listOf(
        SortOption("id", "Id", Icons.Default.Tag),
        SortOption("name", "Name", Icons.Default.Person),
        SortOption("budget", "Budget", Icons.Default.AttachMoney),
        SortOption("last_opened", "Last Opened", Icons.Default.Visibility),
        SortOption("last_activity", "Last Activity", Icons.Default.TrendingUp),
        SortOption("last_modified", "Last Modified", Icons.Default.Edit),
        SortOption("created", "Created", Icons.Default.CalendarMonth),
        SortOption("priority", "Priority", Icons.Default.Flag),
        SortOption("num_activities", "Number of Activities", Icons.Default.Analytics)
    )

    // Only show people NOT in pipeline (contacts)
    val contacts = people.filter { !it.isInPipeline }

    val activeFilterCount = listOfNotNull(
        selectedSegmentId,
        selectedSource,
        if (selectedLabels.isNotEmpty()) "labels" else null
    ).size

    // Apply filters with direction support
    val filteredContacts = contacts.filter { person ->
        (searchQuery.isEmpty() || 
         person.name.contains(searchQuery, true) || 
         person.phone.contains(searchQuery, true) ||
         person.labels.any { it.contains(searchQuery, true) }) &&
        (selectedSegmentId == null || person.segmentId == selectedSegmentId) &&
        (selectedSource == null || person.sourceId == selectedSource) &&
        (selectedLabels.isEmpty() || person.labels.any { it in selectedLabels })
    }.let { list ->
        val baseComparator: Comparator<Person> = when (sortBy) {
            "id" -> compareBy { it.id }
            "name" -> compareBy { it.name.lowercase() }
            "budget" -> compareBy { it.budget.toIntOrNull() ?: 0 }
            "last_opened" -> compareBy { it.lastOpenedAt ?: LocalDate.MIN }
            "last_activity" -> compareBy { it.lastActivityAt ?: LocalDate.MIN }
            "last_modified" -> compareBy { it.updatedAt }
            "created" -> compareBy { it.createdAt }
            "priority" -> compareBy { customPriorities.findById(it.priorityId)?.order ?: 0 }
            "num_activities" -> compareBy { it.activityCount }
            else -> compareBy { it.name.lowercase() }
        }
        
        if (sortDirection == SortDirection.DESC) {
            list.sortedWith(baseComparator.reversed())
        } else {
            list.sortedWith(baseComparator)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SalesCrmTheme.colors.background)
    ) {
        // ===== HEADER: Title/Segment (Left) + Actions (Right) =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Segment Selector as Title
            // Left: Segment Selector as Title
            var showSegmentSheet by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .clickable { showSegmentSheet = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeFilterCount > 0 || searchQuery.isNotEmpty()) {
                        "${selectedSegmentId?.let { customSegments.findById(it)?.label } ?: "All"} (${filteredContacts.size})"
                    } else {
                        "${selectedSegmentId?.let { customSegments.findById(it)?.label } ?: "All Contacts"} (${filteredContacts.size})"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Segment",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Segment Selection Bottom Sheet
            if (showSegmentSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSegmentSheet = false },
                    containerColor = SalesCrmTheme.colors.surface
                ) {
                    Column(
                        Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Select Segment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SalesCrmTheme.colors.textPrimary
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        // All Contacts option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSegmentId = null
                                    showSegmentSheet = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "All Contacts",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SalesCrmTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedSegmentId == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryBlue
                                )
                            }
                        }
                        HorizontalDivider(color = SalesCrmTheme.colors.border)
                        
                        // Individual segments
                        customSegments.forEach { segmentItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSegmentId = segmentItem.id
                                        showSegmentSheet = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            androidx.compose.ui.graphics.Color(segmentItem.color),
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    segmentItem.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SalesCrmTheme.colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedSegmentId == segmentItem.id) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                            HorizontalDivider(color = SalesCrmTheme.colors.border)
                        }
                        
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
            
            // Right Actions: Search, Filter, More
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { 
                    showSearch = !showSearch
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (showSearch) PrimaryBlue else SalesCrmTheme.colors.textSecondary
                    )
                }
                
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge(containerColor = PrimaryBlue) {
                                Text(activeFilterCount.toString())
                            }
                        }
                    }
                ) {
                    IconButton(onClick = { showFiltersRow = !showFiltersRow }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (showFiltersRow) PrimaryBlue else SalesCrmTheme.colors.textSecondary
                        )
                    }
                }
                
                // Three-dot menu
                var moreExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { moreExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = SalesCrmTheme.colors.textSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = moreExpanded,
                        onDismissRequest = { moreExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by") },
                            onClick = { 
                                moreExpanded = false
                                showSortSheet = true 
                            },
                            leadingIcon = { Icon(Icons.Default.Sort, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Customize Columns") },
                            onClick = { 
                                moreExpanded = false
                                showColumnSettings = true 
                            },
                            leadingIcon = { Icon(Icons.Default.ViewColumn, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("View Mode") },
                            onClick = { 
                                moreExpanded = false
                                showViewModeSheet = true
                            },
                            leadingIcon = { 
                                Icon(
                                    when (viewMode) {
                                        ViewMode.CARD -> Icons.Default.GridView
                                        ViewMode.LIST -> Icons.Default.ViewList
                                        ViewMode.TABLE -> Icons.Default.TableChart
                                    },
                                    null
                                ) 
                            }
                        )
                    }
                }
            }
        }
        
        // ===== SEARCH BAR (Toggleable) =====
        AnimatedVisibility(
            visible = showSearch,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LaunchedEffect(Unit) {
                searchFocusRequester.requestFocus()
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                CompactSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                    placeholder = "Search contacts..."
                )
            }
        }
        
        // ===== FILTERS ROW (Toggleable) =====
        androidx.compose.animation.AnimatedVisibility(
            visible = showFiltersRow,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
             // Shared colors
            val primaryColor = MaterialTheme.colorScheme.primary
            val textSecondary = SalesCrmTheme.colors.textSecondary
            val commonChipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                labelColor = textSecondary,
                selectedLabelColor = primaryColor
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Labels Dropdown
                item {
                    var expanded by remember { mutableStateOf(false) }
                    val labelText = if (selectedLabels.isEmpty()) "Labels" else "${selectedLabels.size} Label(s)"
                    Box {
                        FilterChip(
                            selected = selectedLabels.isNotEmpty(),
                            onClick = { expanded = true },
                            label = { Text(labelText) },
                            trailingIcon = { 
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            colors = commonChipColors
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Clear Labels") },
                                onClick = { 
                                    selectedLabels = emptySet()
                                    expanded = false 
                                }
                            )
                            SampleData.availableLabels.forEach { label ->
                                val isSelected = label in selectedLabels
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { 
                                        selectedLabels = if (isSelected) selectedLabels - label else selectedLabels + label
                                    },
                                    trailingIcon = {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Source Dropdown
                item {
                    var expanded by remember { mutableStateOf(false) }
                    val customSources = SalesCrmTheme.sources
                    Box {
                        FilterChip(
                            selected = selectedSource != null,
                            onClick = { expanded = true },
                            label = { Text(selectedSource?.let { customSources.findById(it)?.label } ?: "Source") },
                             trailingIcon = { 
                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                ) 
                            },
                            colors = commonChipColors
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Sources") },
                                onClick = { 
                                    selectedSource = null
                                    expanded = false 
                                }
                            )
                            customSources.forEach { sourceItem ->
                                DropdownMenuItem(
                                    text = { Text(sourceItem.label) },
                                    onClick = { 
                                        selectedSource = sourceItem.id
                                        expanded = false 
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(androidx.compose.ui.graphics.Color(sourceItem.color), androidx.compose.foundation.shape.CircleShape)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = SalesCrmTheme.colors.border, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
        
        if (filteredContacts.isEmpty()) {
            EmptyState(
                Icons.Default.Person,
                "No results found",
                if (searchQuery.isNotEmpty() || activeFilterCount > 0) "Try adjusting your filters" else "Tap + to add a contact"
            )
        } else {
            PersonList(
                people = filteredContacts,
                viewMode = viewMode,
                visibleColumns = visibleColumns,
                onPersonClick = onPersonClick
            )
        }
    }
    
    // Column Settings Sheet (Contacts)
    if (showColumnSettings) {
        ModalBottomSheet(
            onDismissRequest = { showColumnSettings = false },
            containerColor = SalesCrmTheme.colors.surface
        ) {
             Column(
                 Modifier
                     .padding(16.dp)
                     .verticalScroll(rememberScrollState())
             ) {
                Text(
                    "Customize Fields",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Choose which fields to show in the list",
                    style = MaterialTheme.typography.bodySmall,
                    color = SalesCrmTheme.colors.textSecondary
                )
                Spacer(Modifier.height(16.dp))
                
                defaultContactColumns.forEach { column ->
                    val isChecked = column.id in visibleColumns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                visibleColumns = if (isChecked) visibleColumns - column.id else visibleColumns + column.id
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(column.label, color = SalesCrmTheme.colors.textPrimary)
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                visibleColumns = if (checked) visibleColumns + column.id else visibleColumns - column.id
                            },
                             colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryBlue,
                                checkedTrackColor = PrimaryBlue.copy(alpha = 0.3f)
                            )
                        )
                    }
                    HorizontalDivider(color = SalesCrmTheme.colors.border)
                }
                
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { 
                        showColumnSettings = false 
                        Toast.makeText(context, "Fields updated", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Done")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    // Sort By Bottom Sheet
    if (showSortSheet) {
        SortBySheet(
            sortOptions = contactSortOptions,
            selectedSortBy = sortBy,
            selectedDirection = sortDirection,
            onSortByChange = { sortBy = it },
            onDirectionChange = { sortDirection = it },
            onDismiss = { 
                showSortSheet = false 
                Toast.makeText(context, "Sorting updated", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // View Mode Bottom Sheet
    if (showViewModeSheet) {
        ViewModeSheet(
            currentMode = viewMode,
            onModeSelected = { 
                viewMode = it 
                Toast.makeText(context, "View mode: ${it.name}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showViewModeSheet = false }
        )
    }
}

// ==================== TASKS SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    tasks: List<Task>,
    people: List<Person>,
    onTaskClick: (Task) -> Unit,
    onToggleTask: (Task) -> Unit,
    onAddTask: () -> Unit,
    viewMode: TaskViewMode,
    onViewModeChange: (TaskViewMode) -> Unit,
    onPersonClick: ((Person) -> Unit)? = null, // Added callback
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    val sortedTasks = tasks.sortedWith(compareBy({ it.status }, { it.dueDate }, { it.dueTime }))
    val tasksForDate = sortedTasks.filter { it.dueDate == selectedDate }
    val pendingTasks = sortedTasks.filter { it.status == TaskStatus.PENDING }
    val completedTasks = sortedTasks.filter { it.status == TaskStatus.COMPLETED }
    
    fun getPersonName(task: Task): String? {
        return task.linkedPersonId?.let { personId ->
            people.find { it.id == personId }?.name
        }
    }

    fun getPerson(task: Task): Person? {
        return task.linkedPersonId?.let { personId ->
            people.find { it.id == personId }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SalesCrmTheme.colors.background)
    ) {
        if (viewMode == TaskViewMode.DATE_WISE) {
            Spacer(Modifier.height(8.dp))
            DateHorizontalScroller(selectedDate = selectedDate, onDateSelected = { selectedDate = it })
            Spacer(Modifier.height(8.dp))
            
            // Summary for selected date
            val dateTaskCount = tasksForDate.size
            val pendingCount = tasksForDate.count { it.status == TaskStatus.PENDING }
            val reminderCount = tasksForDate.count { it.showReminder && it.status == TaskStatus.PENDING }
            
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val context = LocalContext.current
                val haptic = LocalHapticFeedback.current
                Text(
                    "$dateTaskCount tasks on ${selectedDate.toDisplayString()}", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = SalesCrmTheme.colors.textSecondary,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            Toast.makeText(context, selectedDate.toFullDisplayString(), Toast.LENGTH_SHORT).show()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, selectedDate.toFullDisplayString(), Toast.LENGTH_SHORT).show()
                        }
                    )
                )
                if (reminderCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AccentOrange
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$reminderCount reminders",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentOrange
                        )
                    }
                }
            }
            
            HorizontalDivider(color = SalesCrmTheme.colors.border, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
        }
        
        if ((viewMode == TaskViewMode.DATE_WISE && tasksForDate.isEmpty()) || 
            (viewMode == TaskViewMode.LIST && tasks.isEmpty())) {
            EmptyState(
                Icons.Default.Check, 
                "No tasks", 
                if (viewMode == TaskViewMode.DATE_WISE) "No tasks for this date" else "Tap + to add a task"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (viewMode == TaskViewMode.DATE_WISE) {
                    val pendingForDate = tasksForDate.filter { it.status == TaskStatus.PENDING }
                        .sortedWith(compareBy({ it.dueTime == null }, { it.dueTime }))
                    val completedForDate = tasksForDate.filter { it.status == TaskStatus.COMPLETED }
                    
                    if (pendingForDate.isNotEmpty()) {
                        item {
                            Text(
                                "Pending (${pendingForDate.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(pendingForDate, key = { "dp_${it.id}" }) { task ->
                            val person = getPerson(task)
                            TaskCard(
                                task = task, 
                                personName = person?.name, 
                                onToggle = { onToggleTask(task) }, 
                                onClick = { onTaskClick(task) },
                                onViewProfile = if (person != null && onPersonClick != null) { { onPersonClick(person) } } else null
                            )
                        }
                    } else if (completedForDate.isEmpty()) {
                         // This case is handled by EmptyState above, so typically won't be reached
                    }
                    
                    if (completedForDate.isNotEmpty()) {
                        item {
                            if (pendingForDate.isNotEmpty()) Spacer(Modifier.height(16.dp))
                            Text(
                                "Completed (${completedForDate.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = SalesCrmTheme.colors.textMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(completedForDate, key = { "dc_${it.id}" }) { task ->
                            val person = getPerson(task)
                            TaskCard(
                                task = task, 
                                personName = person?.name, 
                                onToggle = { onToggleTask(task) }, 
                                onClick = { onTaskClick(task) },
                                onViewProfile = if (person != null && onPersonClick != null) { { onPersonClick(person) } } else null
                            )
                        }
                    }
                } else {
                    // List view - group by status
                    if (pendingTasks.isNotEmpty()) {
                        item { 
                            Text(
                                "Pending (${pendingTasks.size})", 
                                style = MaterialTheme.typography.titleSmall, 
                                color = PrimaryBlue, 
                                fontWeight = FontWeight.Bold
                            ) 
                        }
                        items(pendingTasks, key = { "p_${it.id}" }) { task ->
                            val person = getPerson(task)
                            TaskCard(
                                task = task, 
                                personName = person?.name, 
                                onToggle = { onToggleTask(task) }, 
                                onClick = { onTaskClick(task) },
                                onViewProfile = if (person != null && onPersonClick != null) { { onPersonClick(person) } } else null
                            )
                        }
                    }
                    if (completedTasks.isNotEmpty()) {
                        item { 
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Completed (${completedTasks.size})", 
                                style = MaterialTheme.typography.titleSmall, 
                                color = SalesCrmTheme.colors.textMuted,
                                fontWeight = FontWeight.Bold
                            ) 
                        }
                        items(completedTasks, key = { "c_${it.id}" }) { task ->
                            val person = getPerson(task)
                            TaskCard(
                                task = task, 
                                personName = person?.name, 
                                onToggle = { onToggleTask(task) }, 
                                onClick = { onTaskClick(task) },
                                onViewProfile = if (person != null && onPersonClick != null) { { onPersonClick(person) } } else null
                            )
                        }
                    }
                }
            }
        }
    }
}
