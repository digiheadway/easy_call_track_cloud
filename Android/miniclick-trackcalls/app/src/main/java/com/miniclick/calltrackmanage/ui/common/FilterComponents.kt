package com.miniclick.calltrackmanage.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.miniclick.calltrackmanage.ui.home.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInputRow(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search number, name, note...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .focusRequester(focusRequester),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallFilterModal(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    availableLabels: List<String> = emptyList()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    // Reset all filters
                viewModel.setCallTypeFilter(CallTabFilter.ALL)
                viewModel.setPersonTabFilter(PersonTabFilter.ALL)
                viewModel.setConnectedFilter(ConnectedFilter.ALL)
                viewModel.setNotesFilter(NotesFilter.ALL)
                viewModel.setPersonNotesFilter(PersonNotesFilter.ALL)
                viewModel.setContactsFilter(ContactsFilter.ALL)
                viewModel.setReviewedFilter(ReviewedFilter.ALL)
                viewModel.setCustomNameFilter(CustomNameFilter.ALL)
                viewModel.setMinCallCount(0)
                viewModel.setLabelFilter("")
                }) {
                    Text("Reset All")
                }
            }

            // Labels Header
            if (availableLabels.isNotEmpty() || uiState.labelFilter.isNotEmpty()) {
                val isLabelApplied = uiState.labelFilter.isNotEmpty()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Filter by Labels",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isLabelApplied) {
                        TextButton(
                            onClick = { viewModel.setLabelFilter("") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                FilterSection(
                    title = "",
                    icon = Icons.AutoMirrored.Filled.Label,
                    currentValue = uiState.labelFilter.ifEmpty { "All" },
                    options = listOf("All") + availableLabels,
                    onSelect = { viewModel.setLabelFilter(if (it == "All") "" else it ) },
                    onClear = { viewModel.setLabelFilter("") },
                    showTitle = false
                )
            }

            // Boolean Toggles Table
            ToggleFilterTable(uiState = uiState, viewModel = viewModel)

            if (uiState.viewMode == ViewMode.PERSONS) {
                FilterSection(
                    title = "Minimum Calls",
                    icon = Icons.Default.VerticalAlignTop,
                    currentValue = if (uiState.minCallCount > 0) "${uiState.minCallCount}+ Calls" else "Any",
                    options = listOf("Any", "5+", "10+", "20+", "50+"),
                    onSelect = { 
                        val count = when(it) {
                            "5+" -> 5
                            "10+" -> 10
                            "20+" -> 20
                            "50+" -> 50
                            else -> 0
                        }
                        viewModel.setMinCallCount(count)
                    },
                    onClear = { viewModel.setMinCallCount(0) }
                )
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Show Results", modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun CallTypeTabs(
    selectedFilter: CallTabFilter,
    onFilterSelected: (CallTabFilter) -> Unit,
    onFilterLongClick: (CallTabFilter) -> Unit = {},
    counts: Map<CallTabFilter, Int> = emptyMap()
) {
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            if (selectedFilter.ordinal < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilter.ordinal]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        // Filter out IGNORED - it will be accessible via three-dot menu toggle
        CallTabFilter.entries.filter { it != CallTabFilter.IGNORED }.forEach { filter ->
            val count = counts[filter] ?: 0
            val isSelected = selectedFilter == filter
            
            val icon = when (filter) {
                CallTabFilter.ALL -> Icons.Default.AllInclusive
                CallTabFilter.ANSWERED -> Icons.Default.CallReceived
                CallTabFilter.NOT_ANSWERED -> Icons.Default.CallMissed
                CallTabFilter.OUTGOING -> Icons.Default.CallMade
                CallTabFilter.OUTGOING_NOT_CONNECTED -> Icons.AutoMirrored.Filled.CallMissedOutgoing
                CallTabFilter.NEVER_CONNECTED -> Icons.Default.LinkOff
                CallTabFilter.MAY_FAILED -> Icons.Default.Warning
                CallTabFilter.IGNORED -> Icons.Default.Block
            }

            Tab(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                text = {
                    Row(
                        modifier = Modifier
                            .pointerInput(filter) {
                                detectTapGestures(
                                    onLongPress = { onFilterLongClick(filter) },
                                    onTap = { onFilterSelected(filter) }
                                )
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when(filter) {
                                CallTabFilter.NEVER_CONNECTED -> "Never Connected"
                                CallTabFilter.MAY_FAILED -> "Failed Calls"
                                CallTabFilter.OUTGOING_NOT_CONNECTED -> "Outgoing Not Connected"
                                else -> filter.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (count > 0) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (count > 999) "999+" else count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primary
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun PersonTypeTabs(
    selectedFilter: PersonTabFilter,
    onFilterSelected: (PersonTabFilter) -> Unit,
    onFilterLongClick: (PersonTabFilter) -> Unit = {},
    counts: Map<PersonTabFilter, Int> = emptyMap()
) {
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        edgePadding = 16.dp,
        divider = {},
        indicator = { tabPositions ->
            if (selectedFilter.ordinal < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFilter.ordinal]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 3.dp
                )
            }
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        // Filter out IGNORED - it will be accessible via three-dot menu toggle
        PersonTabFilter.entries.filter { it != PersonTabFilter.IGNORED }.forEach { filter ->
            val count = counts[filter] ?: 0
            val isSelected = selectedFilter == filter
            
            val icon = when (filter) {
                PersonTabFilter.ALL -> Icons.Default.Group
                PersonTabFilter.ANSWERED -> Icons.Default.CallReceived
                PersonTabFilter.NOT_ANSWERED -> Icons.Default.CallMissed
                PersonTabFilter.OUTGOING -> Icons.Default.CallMade
                PersonTabFilter.OUTGOING_NOT_CONNECTED -> Icons.AutoMirrored.Filled.CallMissedOutgoing
                PersonTabFilter.NEVER_CONNECTED -> Icons.Default.LinkOff
                PersonTabFilter.MAY_FAILED -> Icons.Default.Warning
                PersonTabFilter.IGNORED -> Icons.Default.Block
            }

            Tab(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                text = {
                    Row(
                        modifier = Modifier
                            .pointerInput(filter) {
                                detectTapGestures(
                                    onLongPress = { onFilterLongClick(filter) },
                                    onTap = { onFilterSelected(filter) }
                                )
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when(filter) {
                                PersonTabFilter.NEVER_CONNECTED -> "Never Connected"
                                PersonTabFilter.MAY_FAILED -> "Failed Calls"
                                PersonTabFilter.OUTGOING_NOT_CONNECTED -> "Outgoing Not Connected"
                                else -> filter.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (count > 0) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = if (isSelected) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (count > 999) "999+" else count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (isSelected) 
                                        MaterialTheme.colorScheme.primary
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ToggleFilterTable(
    uiState: HomeUiState,
    viewModel: HomeViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Property",
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            listOf("Not Applied (Default)", "True", "False").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // is Reviewed
        ToggleFilterRow(
            label = "is Reviewed",
            currentValue = uiState.reviewedFilter.name,
            onSelect = { viewModel.setReviewedFilter(ReviewedFilter.valueOf(it)) },
            options = Triple(ReviewedFilter.ALL.name, ReviewedFilter.REVIEWED.name, ReviewedFilter.NOT_REVIEWED.name)
        )

        // Connected
        ToggleFilterRow(
            label = "Connected",
            currentValue = uiState.connectedFilter.name,
            onSelect = { viewModel.setConnectedFilter(ConnectedFilter.valueOf(it)) },
            options = Triple(ConnectedFilter.ALL.name, ConnectedFilter.CONNECTED.name, ConnectedFilter.NOT_CONNECTED.name)
        )

        // Call Note
        ToggleFilterRow(
            label = "Call Note",
            currentValue = uiState.notesFilter.name,
            onSelect = { viewModel.setNotesFilter(NotesFilter.valueOf(it)) },
            options = Triple(NotesFilter.ALL.name, NotesFilter.WITH_NOTE.name, NotesFilter.WITHOUT_NOTE.name)
        )

        // Person Note
        ToggleFilterRow(
            label = "Person Note",
            currentValue = uiState.personNotesFilter.name,
            onSelect = { viewModel.setPersonNotesFilter(PersonNotesFilter.valueOf(it)) },
            options = Triple(PersonNotesFilter.ALL.name, PersonNotesFilter.WITH_NOTE.name, PersonNotesFilter.WITHOUT_NOTE.name)
        )

        // In My Phonebook
        ToggleFilterRow(
            label = "In My Phonebook",
            currentValue = uiState.contactsFilter.name,
            onSelect = { viewModel.setContactsFilter(ContactsFilter.valueOf(it)) },
            options = Triple(ContactsFilter.ALL.name, ContactsFilter.IN_CONTACTS.name, ContactsFilter.NOT_IN_CONTACTS.name)
        )

        // Custom Name Added
        ToggleFilterRow(
            label = "Custom Name Added",
            currentValue = uiState.customNameFilter.name,
            onSelect = { viewModel.setCustomNameFilter(CustomNameFilter.valueOf(it)) },
            options = Triple(CustomNameFilter.ALL.name, CustomNameFilter.WITH_NAME.name, CustomNameFilter.WITHOUT_NAME.name)
        )
    }
}

@Composable
fun ToggleFilterRow(
    label: String,
    currentValue: String,
    onSelect: (String) -> Unit,
    options: Triple<String, String, String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Options: Not Applied, True, False
        listOf(options.first, options.second, options.third).forEach { option ->
            val isSelected = currentValue == option
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    icon: ImageVector,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    showTitle: Boolean = true
) {
    val isFilterApplied = currentValue != "ALL" && currentValue != "All"
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    if (showTitle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (isFilterApplied) {
                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = currentValue == option
                val displayLabel = when (option) {
                    "WITH_NOTE" -> "Note Added"
                    "WITHOUT_NOTE" -> "No Note"
                    "WITH_NAME" -> "Name Added"
                    "WITHOUT_NAME" -> "No Name"
                    "ALL" -> "All"
                    "CONNECTED" -> "Connected"
                    "NOT_CONNECTED" -> "Not Connected"
                    "IN_CONTACTS" -> "In Contacts"
                    "NOT_IN_CONTACTS" -> "Not in Contacts"
                    "LAST_CALL" -> "Last Call"
                    "MOST_CALLS" -> "Most Calls"
                    "NAME" -> "Name"
                    "DATE" -> "Date"
                    "DURATION" -> "Duration"
                    "NUMBER" -> "Number"
                    "ASCENDING" -> "Ascending"
                    "DESCENDING" -> "Descending"
                    "REVIEWED" -> "Reviewed"
                    "NOT_REVIEWED" -> "Not Reviewed"
                    "ANSWERED" -> "Answered"
                    "OUTGOING" -> "Outgoing"
                    else -> option.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
                }
                
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(option) },
                    label = { 
                        Text(
                            text = displayLabel,
                            style = MaterialTheme.typography.bodySmall
                        ) 
                    },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    callTypeFilter: CallTabFilter,
    connectedFilter: ConnectedFilter,
    notesFilter: NotesFilter,
    contactsFilter: ContactsFilter,
    attendedFilter: AttendedFilter,
    onCallTypeFilterChange: (CallTabFilter) -> Unit,
    onConnectedFilterChange: (ConnectedFilter) -> Unit,
    onNotesFilterChange: (NotesFilter) -> Unit,
    onContactsFilterChange: (ContactsFilter) -> Unit,
    onAttendedFilterChange: (AttendedFilter) -> Unit,
    labelFilter: String,
    onLabelFilterChange: (String) -> Unit,
    availableLabels: List<String> = emptyList(),
    onOpenModal: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "More Filters" toggle
            InputChip(
                selected = false,
                onClick = onOpenModal,
                label = { Text("Filters") },
                leadingIcon = { Icon(Icons.Default.Tune, null, modifier = Modifier.size(18.dp)) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp)) }
            )

            VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))

            // Label Filter
            if (labelFilter.isNotEmpty()) {
                FilterChip(
                    selected = true,
                    onClick = { onLabelFilterChange("") },
                    label = { Text(labelFilter) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Call Type Filter
            if (callTypeFilter != CallTabFilter.ALL) {
                FilterChip(
                    selected = true,
                    onClick = { onCallTypeFilterChange(CallTabFilter.ALL) },
                    label = { Text(callTypeFilter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) }
                )
            }
            
            // Connected Filter
            if (connectedFilter != ConnectedFilter.ALL) {
                FilterChip(
                    selected = true,
                    onClick = { onConnectedFilterChange(ConnectedFilter.ALL) },
                    label = { Text(connectedFilter.name.lowercase().replace("_", " ")) },
                    leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp)) }
                )
            }
            
            // Notes Filter
            if (notesFilter != NotesFilter.ALL) {
                FilterChip(
                    selected = true,
                    onClick = { onNotesFilterChange(NotesFilter.ALL) },
                    label = { Text("Call Note") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.StickyNote2, null, modifier = Modifier.size(18.dp)) }
                )
            }
            
            // Contacts Filter
            if (contactsFilter != ContactsFilter.ALL) {
                FilterChip(
                    selected = true,
                    onClick = { onContactsFilterChange(ContactsFilter.ALL) },
                    label = { Text(if (contactsFilter == ContactsFilter.IN_CONTACTS) "Contacts" else "Unknown") },
                    leadingIcon = { Icon(Icons.Default.Contacts, null, modifier = Modifier.size(18.dp)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdownChip(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    options: List<FilterOption>
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        FilterChip(
            selected = isActive,
            onClick = { expanded = true },
            label = { Text(label) },
            leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp)) }
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = { Icon(option.icon, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (option.isSelected) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        option.onClick()
                        expanded = false
                    }
                )
            }
        }
    }
}

data class FilterOption(
    val label: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val onClick: () -> Unit
)
