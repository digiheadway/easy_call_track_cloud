package com.example.salescrm.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.salescrm.data.*
import com.example.salescrm.ui.components.*
import com.example.salescrm.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// People view filter type
enum class PeopleViewType(val label: String) {
    PIPELINE("Pipeline"),
    CONTACTS("Contacts"),
    ALL("All")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    people: List<Person>,
    onPersonClick: (Person) -> Unit,
    onAddPerson: (isForPipeline: Boolean) -> Unit,
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
    
    // Main view type: Pipeline, Contacts, or All
    var selectedViewType by remember { mutableStateOf(PeopleViewType.PIPELINE) }
    
    // Filter states
    var selectedSegmentId by remember { mutableStateOf<String?>(null) }
    var selectedStageId by remember { mutableStateOf<String?>(null) }
    var selectedPriorityId by remember { mutableStateOf<String?>(null) }
    var selectedBudgetRange by remember { mutableStateOf<String?>(null) }
    var selectedLabels by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSource by remember { mutableStateOf<String?>(null) }

    // Column visibility - use pipeline columns by default, switch based on view
    var visibleColumns by remember { 
        mutableStateOf(defaultPipelineColumns.filter { it.visible }.map { it.id }.toSet()) 
    }
    
    // Sort state
    var sortBy by remember { mutableStateOf("id") }
    var sortDirection by remember { mutableStateOf(SortDirection.DESC) }
    
    // Sort options
    val sortOptions = listOf(
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
    
    // Filter people based on view type
    val basePeople = when (selectedViewType) {
        PeopleViewType.PIPELINE -> people.filter { it.isInPipeline }
        PeopleViewType.CONTACTS -> people.filter { !it.isInPipeline }
        PeopleViewType.ALL -> people
    }
    
    val budgetRanges = listOf("< $10K", "$10K - $25K", "$25K - $50K", "> $50K")
    
    // Count active filters (excluding view type and segment)
    val activeFilterCount = listOfNotNull(
        selectedStageId.takeIf { selectedViewType != PeopleViewType.CONTACTS },
        selectedPriorityId,
        selectedBudgetRange,
        selectedSource,
        if (selectedLabels.isNotEmpty()) "labels" else null
    ).size
    
    // Apply filters and sorting
    val filteredPeople = basePeople.filter { person ->
        (searchQuery.isEmpty() || 
         person.name.contains(searchQuery, true) || 
         person.phone.contains(searchQuery, true) ||
         person.note.contains(searchQuery, true)) &&
        (selectedSegmentId == null || person.segmentId == selectedSegmentId) &&
        (selectedStageId == null || !person.isInPipeline || person.stageId == selectedStageId) &&
        (selectedPriorityId == null || 
            (if (person.isInPipeline) person.pipelinePriorityId else person.priorityId) == selectedPriorityId) &&
        (selectedSource == null || person.sourceId == selectedSource) &&
        (selectedLabels.isEmpty() || person.labels.any { it in selectedLabels })
    }.let { list ->
        val baseComparator: Comparator<Person> = when (sortBy) {
            "id" -> compareBy { it.id }
            "name" -> compareBy { it.name.lowercase() }
            "budget" -> compareBy { it.budget.toIntOrNull() ?: 0 }
            "last_opened" -> compareBy { it.lastOpenedAt ?: java.time.LocalDate.MIN }
            "last_activity" -> compareBy { it.lastActivityAt ?: java.time.LocalDate.MIN }
            "last_modified" -> compareBy { it.updatedAt }
            "created" -> compareBy { it.createdAt }
            "priority" -> compareBy { 
                customPriorities.findById(
                    if (it.isInPipeline) it.pipelinePriorityId else it.priorityId
                )?.order ?: 0 
            }
            "num_activities" -> compareBy { it.activityCount }
            else -> compareBy { it.name.lowercase() }
        }
        if (sortDirection == SortDirection.DESC) list.sortedWith(baseComparator.reversed())
        else list.sortedWith(baseComparator)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SalesCrmTheme.colors.background)
    ) {
        // ===== HEADER =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Segment (Main) + View Type (Sub)
            var showViewSheet by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.clickable { showViewSheet = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Main heading: Segment name with count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${selectedSegmentId?.let { customSegments.findById(it)?.label } ?: "All Segments"} (${filteredPeople.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SalesCrmTheme.colors.textPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select View",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // Subheading: List type
                    Text(
                        text = selectedViewType.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = SalesCrmTheme.colors.textMuted
                    )
                }
            }
            
            // View Type + Segment Selection Sheet
            if (showViewSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showViewSheet = false },
                    containerColor = SalesCrmTheme.colors.surface
                ) {
                    Column(
                        Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "View",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SalesCrmTheme.colors.textPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        // View Type Options
                        PeopleViewType.entries.forEach { viewType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedViewType = viewType
                                        // Reset stage filter when switching away from pipeline
                                        if (viewType == PeopleViewType.CONTACTS) {
                                            selectedStageId = null
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (viewType) {
                                        PeopleViewType.PIPELINE -> Icons.Default.Star
                                        PeopleViewType.CONTACTS -> Icons.Default.Person
                                        PeopleViewType.ALL -> Icons.Default.People
                                    },
                                    contentDescription = null,
                                    tint = if (selectedViewType == viewType) PrimaryBlue 
                                           else SalesCrmTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    viewType.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selectedViewType == viewType) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedViewType == viewType) PrimaryBlue 
                                            else SalesCrmTheme.colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedViewType == viewType) {
                                    Icon(Icons.Default.Check, null, tint = PrimaryBlue)
                                }
                            }
                        }
                        
                        HorizontalDivider(
                            color = SalesCrmTheme.colors.border,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Text(
                            "Segment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SalesCrmTheme.colors.textSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        // All Segments option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSegmentId = null
                                    showViewSheet = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "All Segments",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SalesCrmTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedSegmentId == null) {
                                Icon(Icons.Default.Check, null, tint = PrimaryBlue)
                            }
                        }
                        
                        // Individual segments
                        customSegments.forEach { segment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSegmentId = segment.id
                                        showViewSheet = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(segment.color), CircleShape)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    segment.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SalesCrmTheme.colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selectedSegmentId == segment.id) {
                                    Icon(Icons.Default.Check, null, tint = PrimaryBlue)
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
            
            // Right: Actions
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
                        Icons.Default.Search,
                        "Search",
                        tint = if (showSearch) PrimaryBlue else SalesCrmTheme.colors.textSecondary
                    )
                }
                
                BadgedBox(
                    badge = {
                        if (activeFilterCount > 0) {
                            Badge(containerColor = PrimaryBlue) { Text(activeFilterCount.toString()) }
                        }
                    }
                ) {
                    IconButton(onClick = { showFiltersRow = !showFiltersRow }) {
                        Icon(
                            Icons.Default.FilterList,
                            "Filter",
                            tint = if (showFiltersRow) PrimaryBlue else SalesCrmTheme.colors.textSecondary
                        )
                    }
                }
                
                var moreExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { moreExpanded = true }) {
                        Icon(Icons.Default.MoreVert, "More", tint = SalesCrmTheme.colors.textSecondary)
                    }
                    DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Sort by") },
                            onClick = { moreExpanded = false; showSortSheet = true },
                            leadingIcon = { Icon(Icons.Default.Sort, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Customize Columns") },
                            onClick = { moreExpanded = false; showColumnSettings = true },
                            leadingIcon = { Icon(Icons.Default.ViewColumn, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("View Mode") },
                            onClick = { moreExpanded = false; showViewModeSheet = true },
                            leadingIcon = { 
                                Icon(
                                    when (viewMode) {
                                        ViewMode.CARD -> Icons.Default.GridView
                                        ViewMode.LIST -> Icons.Default.ViewList
                                        ViewMode.TABLE -> Icons.Default.TableChart
                                    }, null
                                ) 
                            }
                        )
                    }
                }
            }
        }
        
        // ===== SEARCH BAR =====
        AnimatedVisibility(visible = showSearch, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            LaunchedEffect(Unit) { searchFocusRequester.requestFocus() }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                CompactSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                    placeholder = "Search..."
                )
            }
        }
        
        // ===== FILTERS ROW =====
        AnimatedVisibility(visible = showFiltersRow, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                labelColor = SalesCrmTheme.colors.textSecondary,
                selectedLabelColor = primaryColor
            )
            val resetColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AccentRed.copy(alpha = 0.1f),
                selectedLabelColor = AccentRed
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reset button
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
                            label = { Icon(Icons.Default.Close, "Reset", Modifier.size(16.dp)) },
                            colors = resetColors
                        )
                    }
                }
                
                // Stage filter (only for Pipeline or All)
                if (selectedViewType != PeopleViewType.CONTACTS) {
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = selectedStageId != null,
                                onClick = { expanded = true },
                                label = { Text(selectedStageId?.let { customStages.findById(it)?.label } ?: "Stage") },
                                trailingIcon = { Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp)) },
                                colors = chipColors
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("All Stages") }, onClick = { selectedStageId = null; expanded = false })
                                customStages.forEach { stage ->
                                    DropdownMenuItem(
                                        text = { Text(stage.label) },
                                        onClick = { selectedStageId = stage.id; expanded = false },
                                        leadingIcon = { Box(Modifier.size(12.dp).background(Color(stage.color), CircleShape)) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Priority filter
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = selectedPriorityId != null,
                            onClick = { expanded = true },
                            label = { Text(selectedPriorityId?.let { customPriorities.findById(it)?.label } ?: "Priority") },
                            trailingIcon = { Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp)) },
                            colors = chipColors
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("All Priorities") }, onClick = { selectedPriorityId = null; expanded = false })
                            customPriorities.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.label) },
                                    onClick = { selectedPriorityId = p.id; expanded = false },
                                    leadingIcon = { Box(Modifier.size(12.dp).background(Color(p.color), CircleShape)) }
                                )
                            }
                        }
                    }
                }
                
                // Labels filter
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = selectedLabels.isNotEmpty(),
                            onClick = { expanded = true },
                            label = { Text(if (selectedLabels.isEmpty()) "Labels" else "${selectedLabels.size} Label(s)") },
                            trailingIcon = { Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp)) },
                            colors = chipColors
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Clear Labels") }, onClick = { selectedLabels = emptySet(); expanded = false })
                            SampleData.availableLabels.forEach { label ->
                                val isSelected = label in selectedLabels
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { selectedLabels = if (isSelected) selectedLabels - label else selectedLabels + label },
                                    trailingIcon = { if (isSelected) Icon(Icons.Default.Check, null, tint = primaryColor) }
                                )
                            }
                        }
                    }
                }
                
                // Source filter
                item {
                    var expanded by remember { mutableStateOf(false) }
                    val customSources = SalesCrmTheme.sources
                    Box {
                        FilterChip(
                            selected = selectedSource != null,
                            onClick = { expanded = true },
                            label = { Text(selectedSource?.let { customSources.findById(it)?.label } ?: "Source") },
                            trailingIcon = { Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp)) },
                            colors = chipColors
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("All Sources") }, onClick = { selectedSource = null; expanded = false })
                            customSources.forEach { src ->
                                DropdownMenuItem(
                                    text = { Text(src.label) },
                                    onClick = { selectedSource = src.id; expanded = false },
                                    leadingIcon = { Box(Modifier.size(12.dp).background(Color(src.color), CircleShape)) }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        HorizontalDivider(color = SalesCrmTheme.colors.border, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
        
        // ===== CONTENT =====
        if (filteredPeople.isEmpty()) {
            EmptyState(
                if (selectedViewType == PeopleViewType.PIPELINE) Icons.Default.Star else Icons.Default.Person,
                "No results found",
                if (searchQuery.isNotEmpty() || activeFilterCount > 0) "Try adjusting your filters" else "Tap + to add"
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
    
    // ===== BOTTOM SHEETS =====
    
    // Column Settings
    if (showColumnSettings) {
        val columns = if (selectedViewType == PeopleViewType.CONTACTS) defaultContactColumns else defaultPipelineColumns
        ModalBottomSheet(onDismissRequest = { showColumnSettings = false }, containerColor = SalesCrmTheme.colors.surface) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Customize Fields", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SalesCrmTheme.colors.textPrimary)
                Spacer(Modifier.height(16.dp))
                columns.forEach { col ->
                    val isChecked = col.id in visibleColumns
                    Row(
                        Modifier.fillMaxWidth().clickable { visibleColumns = if (isChecked) visibleColumns - col.id else visibleColumns + col.id }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(col.label, color = SalesCrmTheme.colors.textPrimary)
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { visibleColumns = if (it) visibleColumns + col.id else visibleColumns - col.id },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue, checkedTrackColor = PrimaryBlue.copy(alpha = 0.3f))
                        )
                    }
                    HorizontalDivider(color = SalesCrmTheme.colors.border)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { showColumnSettings = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("Done") }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
    
    // Sort Sheet
    if (showSortSheet) {
        SortBySheet(
            sortOptions = sortOptions,
            selectedSortBy = sortBy,
            selectedDirection = sortDirection,
            onSortByChange = { sortBy = it },
            onDirectionChange = { sortDirection = it },
            onDismiss = { showSortSheet = false; Toast.makeText(context, "Sorting updated", Toast.LENGTH_SHORT).show() }
        )
    }
    
    // View Mode Sheet
    if (showViewModeSheet) {
        ViewModeSheet(
            currentMode = viewMode,
            onModeSelected = { viewMode = it; Toast.makeText(context, "View mode: ${it.name}", Toast.LENGTH_SHORT).show() },
            onDismiss = { showViewModeSheet = false }
        )
    }
}
