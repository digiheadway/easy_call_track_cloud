package com.example.salescrm.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salescrm.data.*
import com.example.salescrm.data.local.AppDatabase
import com.example.salescrm.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.salescrm.util.AudioPlayer
import com.example.salescrm.util.rememberAudioPlayerState

// ==================== CALLS SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    people: List<Person>,
    onAddActivity: (String, String) -> Unit,  // phoneNumber, content
    onAddToPipeline: (String, String?) -> Unit,  // phoneNumber, contactName
    onPersonClick: (Person) -> Unit,
    onViewCallLog: (CallLogGroup) -> Unit,
    callSettings: CallSettings,
    onUpdateSettings: (CallSettings) -> Unit,
    onImportPastCalls: (suspend (Float) -> Unit) -> Unit,
    crmRepository: CrmRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val callLogRepository = remember { CallLogRepository(context) }
    
    // Permission state - only block on base permissions (Call Log + Contacts)
    var hasBasePermissions by remember { mutableStateOf(callLogRepository.hasBasePermissions()) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasBasePermissions = callLogRepository.hasBasePermissions()
    }
    
    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateRange by remember { mutableStateOf(DateRangePreset.TODAY) }
    var customFromDate by remember { mutableStateOf(LocalDate.now().minusDays(7)) }
    var customToDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCallType by remember { mutableStateOf<CallType?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showCallSettings by remember { mutableStateOf(false) }
    var showTrackingDatePicker by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf(0f) }
    
    // New Dropdown Filters
    var crmFilter by remember { mutableStateOf(CrmFilter.ALL) }
    var contactFilter by remember { mutableStateOf(ContactFilter.ALL) }
    var noteFilter by remember { mutableStateOf(NoteFilter.ALL) }
    
    // Trigger sync when app returns from background (e.g. after a call)
    var resumeTrigger by remember { mutableStateOf(0) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                resumeTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    val activeSims = remember(hasBasePermissions) { 
        if (hasBasePermissions) callLogRepository.getActiveSims() else emptyList() 
    }
    
    // Call log data
    var callGroups by remember { mutableStateOf<List<CallLogGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var hasInitialData by remember { mutableStateOf(false) }
    
    // Calculate date range
    val (fromDate, toDate) = remember(selectedDateRange, customFromDate, customToDate, callSettings.trackingStartDate) {
        val range = when (selectedDateRange) {
            DateRangePreset.TODAY -> LocalDate.now() to LocalDate.now()
            DateRangePreset.LAST_3_DAYS -> LocalDate.now().minusDays(2) to LocalDate.now()
            DateRangePreset.LAST_7_DAYS -> LocalDate.now().minusDays(6) to LocalDate.now()
            DateRangePreset.LAST_15_DAYS -> LocalDate.now().minusDays(14) to LocalDate.now()
            DateRangePreset.LAST_30_DAYS -> LocalDate.now().minusDays(29) to LocalDate.now()
            DateRangePreset.CUSTOM -> customFromDate to customToDate
        }
        
        // Respect tracking start date as an absolute floor
        val floorDate = callSettings.trackingStartDate
        val finalFrom = if (range.first.isBefore(floorDate)) floorDate else range.first
        finalFrom to range.second
    }
    
    // Fetch calls from DB - this is reactive and will update when DB changes
    val dbEntries by crmRepository.getFilteredCallHistory(fromDate, toDate, selectedCallType)
        .collectAsState(initial = emptyList())
    
    // Pre-build phone lookup map for faster matching
    // We use prefixes 's:' for strict (normalized) and 'f:' for fuzzy (stripped)
    val phoneToPersonMap = remember(people) {
        val map = mutableMapOf<String, Person>()
        people.forEach { person ->
            // Strict match
            val normalized = CallLogRepository.normalizePhoneNumber(person.phone)
            if (normalized.isNotEmpty()) map["s:$normalized"] = person
            
            // Fuzzy match (only if no country code)
            if (!person.phone.contains("+")) {
                val stripped = CallLogRepository.stripCountryCode(person.phone)
                if (stripped.isNotEmpty() && !map.containsKey("f:$stripped")) {
                    map["f:$stripped"] = person
                }
            }
            
            // Alt phone
            val altNormalized = CallLogRepository.normalizePhoneNumber(person.alternativePhone)
            if (altNormalized.isNotEmpty()) map["s:$altNormalized"] = person
            
            if (!person.alternativePhone.contains("+") && person.alternativePhone.isNotEmpty()) {
                val altStripped = CallLogRepository.stripCountryCode(person.alternativePhone)
                if (altStripped.isNotEmpty() && !map.containsKey("f:$altStripped")) {
                    map["f:$altStripped"] = person
                }
            }
        }
        map
    }
        
    // STEP 1: Process and display cached data IMMEDIATELY (no waiting for sync)
    LaunchedEffect(dbEntries, phoneToPersonMap, callSettings.simSelection, activeSims) {
        if (dbEntries.isNotEmpty()) {
            // Show data immediately without recording matching
            withContext(Dispatchers.Default) {
                val targetSubId = if (callSettings.simSelection != SimSelection.BOTH) {
                    val simIndex = if (callSettings.simSelection == SimSelection.SIM_1) 0 else 1
                    activeSims.getOrNull(simIndex)?.subscriptionId?.toString()
                } else null

                val matchedEntries = dbEntries.map { entry ->
                    // Try strict match first
                    var matchedPerson = phoneToPersonMap["s:${entry.normalizedNumber}"]
                    
                    // Fallback to fuzzy match
                    if (matchedPerson == null) {
                        val stripped = CallLogRepository.stripCountryCode(entry.phoneNumber)
                        matchedPerson = phoneToPersonMap["f:$stripped"]
                    }

                    if (matchedPerson != null) {
                        entry.copy(
                            linkedPersonId = matchedPerson.id,
                            linkedPersonName = matchedPerson.name
                        )
                    } else entry
                }.let { list ->
                    if (targetSubId != null) {
                        list.filter { it.subscriptionId == targetSubId }
                    } else list
                }
                
                val groups = callLogRepository.groupCallsByNumber(matchedEntries)
                
                withContext(Dispatchers.Main) {
                    callGroups = groups
                    isLoading = false
                    hasInitialData = true
                }
            }
        } else if (!isSyncing) {
            // No data in DB yet, show empty state quickly
            isLoading = false
        }
    }
    
    // STEP 2: Background sync with device call log (doesn't block UI)
    LaunchedEffect(hasBasePermissions, callSettings.simSelection, callSettings.trackingStartDate, callSettings.autoSyncToActivities, resumeTrigger) {
        if (hasBasePermissions && !isSyncing) {
            isSyncing = true
            withContext(Dispatchers.IO) {
                try {
                    // Sync with settings (use 30 days or tracking start date, whichever is shorter)
                    val days = ChronoUnit.DAYS.between(callSettings.trackingStartDate, LocalDate.now()).toInt().coerceIn(7, 30)
                    callLogRepository.syncWithDatabase(
                        salesDao = AppDatabase.getDatabase(context).salesDao(), 
                        days = days,
                        simSelection = callSettings.simSelection,
                        trackingStartDate = callSettings.trackingStartDate,
                        autoSyncToActivities = callSettings.autoSyncToActivities
                    )
                } catch (e: Exception) {
                    android.util.Log.e("CallsScreen", "Sync failed", e)
                } finally {
                    withContext(Dispatchers.Main) {
                        isSyncing = false
                        isLoading = false
                    }
                }
            }
        }
    }
    
    // STEP 3: Lazy load recordings AFTER initial display (deferred, non-blocking)
    LaunchedEffect(hasInitialData, callSettings.recordingPath) {
        if (hasInitialData && callGroups.isNotEmpty()) {
            // Wait a moment for UI to settle before scanning recordings
            delay(500)
            withContext(Dispatchers.IO) {
                try {
                    val allEntries = callGroups.flatMap { it.calls }
                    val entriesWithRecordings = callLogRepository.findRecordingsForCalls(
                        entries = allEntries,
                        customPath = callSettings.recordingPath
                    )
                    
                    // Only update if recordings were found
                    if (entriesWithRecordings.any { it.recordingPath != null }) {
                        val updatedGroups = callLogRepository.groupCallsByNumber(entriesWithRecordings)
                        withContext(Dispatchers.Main) {
                            callGroups = updatedGroups
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CallsScreen", "Recording match failed", e)
                }
            }
        }
    }
    
    // Filtered groups based on search and new dropdown filters
    val filteredGroups = remember(callGroups, searchQuery, crmFilter, contactFilter, noteFilter) {
        callGroups.filter { group ->
            // Search filter
            val matchesSearch = searchQuery.isBlank() || 
                group.displayName.contains(searchQuery, ignoreCase = true) ||
                group.phoneNumber.contains(searchQuery, ignoreCase = true)
            
            if (!matchesSearch) return@filter false
            
            // CRM filter
            val matchesCrm = when (crmFilter) {
                CrmFilter.ALL -> true
                CrmFilter.IN_CRM -> group.linkedPersonId != null
                CrmFilter.NOT_IN_CRM -> group.linkedPersonId == null
            }
            
            if (!matchesCrm) return@filter false
            
            // Contact filter (Saved vs Not Saved)
            // If contactName is null/blank and it's not linked to a CRM person with a name, it's probably not saved
            val isSaved = group.calls.any { it.contactName != null }
            val matchesContact = when (contactFilter) {
                ContactFilter.ALL -> true
                ContactFilter.SAVED -> isSaved
                ContactFilter.NOT_SAVED -> !isSaved
            }
            
            if (!matchesContact) return@filter false
            
            // Note filter
            val matchesNote = when (noteFilter) {
                NoteFilter.ALL -> true
                NoteFilter.HAS_NOTE -> group.hasNote
                NoteFilter.NO_NOTE -> !group.hasNote
            }
            
            matchesNote
        }
    }
    
    // Group by date
    val groupedByDate = remember(filteredGroups) {
        filteredGroups.groupBy { group ->
            group.lastCall?.timestamp?.toLocalDate() ?: LocalDate.now()
        }.toSortedMap(compareByDescending { it })
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SalesCrmTheme.colors.background)
    ) {
        if (!hasBasePermissions) {
            // Permission Request Screen
            PermissionRequestScreen(
                onRequestPermission = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val permissions = mutableListOf(
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.READ_PHONE_STATE
                    )
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    
                    permissionLauncher.launch(permissions.toTypedArray())
                }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                CallsHeader(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    selectedDateRange = selectedDateRange,
                    onDateRangeChange = { selectedDateRange = it },
                    showFilters = showFilters,
                    onToggleFilters = { showFilters = !showFilters },
                    onShowSettings = { showCallSettings = true }
                )
                
                // Filter chips
                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        CallTypeFilterRow(
                            selectedType = selectedCallType,
                            onTypeSelected = { selectedCallType = it }
                        )
                        CallAdvancedFiltersRow(
                            crmFilter = crmFilter,
                            onCrmFilterChange = { crmFilter = it },
                            contactFilter = contactFilter,
                            onContactFilterChange = { contactFilter = it },
                            noteFilter = noteFilter,
                            onNoteFilterChange = { noteFilter = it }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                
                // Content
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else if (filteredGroups.isEmpty()) {
                    EmptyCallsState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        groupedByDate.forEach { (date, groups) ->
                            // Date separator
                            item(key = "date_$date") {
                                DateSeparator(date = date)
                            }
                            
                            // Call groups for this date
                            items(
                                items = groups,
                                key = { it.normalizedNumber + "_" + it.lastCall?.id }
                            ) { group ->
                                CallGroupCard(
                                    group = group,
                                    hasNote = group.hasNote,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onViewCallLog(group)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Call Settings Sheet
        if (showCallSettings) {
            CallSettingsSheet(
                settings = callSettings,
                onSettingsChange = onUpdateSettings,
                isImporting = isImporting,
                importProgress = importProgress,
                onImportPastLogs = {
                    isImporting = true
                    importProgress = 0f
                    onImportPastCalls { progress ->
                        importProgress = progress
                        if (progress >= 1f) {
                            isImporting = false
                        }
                    }
                },
                onShowTrackingDatePicker = { showTrackingDatePicker = true },
                onDismiss = { showCallSettings = false }
            )
        }
        
        // Tracking Start Date Picker
        if (showTrackingDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = callSettings.trackingStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showTrackingDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            onUpdateSettings(callSettings.copy(trackingStartDate = newDate))
                        }
                        showTrackingDatePicker = false
                    }) { Text("Confirm") }
                },
                dismissButton = {
                    TextButton(onClick = { showTrackingDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}


// ==================== PERMISSION REQUEST SCREEN ====================

@Composable
private fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SalesCrmTheme.colors.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = PrimaryBlue
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Title
                Text(
                    text = "Track Your Calls",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description
                Text(
                    text = "To track calls and link them with your leads, we need access to your call log and contacts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SalesCrmTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Benefits
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PermissionBenefit(
                        icon = Icons.Outlined.Link,
                        text = "Link calls to your leads automatically"
                    )
                    PermissionBenefit(
                        icon = Icons.Outlined.Note,
                        text = "Add notes to calls for follow-ups"
                    )
                    PermissionBenefit(
                        icon = Icons.Outlined.Timeline,
                        text = "View call history organized by contact"
                    )
                    PermissionBenefit(
                        icon = Icons.Outlined.Lock,
                        text = "Your data stays on your device"
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Grant Permission Button
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grant Permission",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionBenefit(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = AccentGreen
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = SalesCrmTheme.colors.textSecondary
        )
    }
}

// ==================== CALLS HEADER ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallsHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedDateRange: DateRangePreset,
    onDateRangeChange: (DateRangePreset) -> Unit,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onShowSettings: () -> Unit
) {
    var showDateDropdown by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SalesCrmTheme.colors.surface)
            .padding(16.dp)
    ) {
        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Calls",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                
                IconButton(onClick = onShowSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Call Settings",
                        tint = SalesCrmTheme.colors.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Date range dropdown
            Box {
                FilterChip(
                    selected = true,
                    onClick = { showDateDropdown = true },
                    label = { 
                        Text(
                            text = selectedDateRange.label,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                        selectedLabelColor = PrimaryBlue,
                        selectedLeadingIconColor = PrimaryBlue,
                        selectedTrailingIconColor = PrimaryBlue
                    )
                )
                
                DropdownMenu(
                    expanded = showDateDropdown,
                    onDismissRequest = { showDateDropdown = false }
                ) {
                    DateRangePreset.entries.filter { it != DateRangePreset.CUSTOM }.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.label) },
                            onClick = {
                                onDateRangeChange(preset)
                                showDateDropdown = false
                            },
                            leadingIcon = {
                                if (preset == selectedDateRange) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrimaryBlue
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { 
                Text(
                    "Search calls...",
                    color = SalesCrmTheme.colors.textMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SalesCrmTheme.colors.textMuted
                )
            },
            trailingIcon = {
                Row {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = SalesCrmTheme.colors.textMuted
                            )
                        }
                    }
                    IconButton(onClick = onToggleFilters) {
                        Icon(
                            imageVector = if (showFilters) Icons.Filled.FilterList else Icons.Outlined.FilterList,
                            contentDescription = "Filters",
                            tint = if (showFilters) PrimaryBlue else SalesCrmTheme.colors.textMuted
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = SalesCrmTheme.colors.border,
                focusedContainerColor = SalesCrmTheme.colors.surfaceVariant,
                unfocusedContainerColor = SalesCrmTheme.colors.surfaceVariant
            )
        )
    }
}

// ==================== CALL TYPE FILTER ROW ====================

@Composable
private fun CallTypeFilterRow(
    selectedType: CallType?,
    onTypeSelected: (CallType?) -> Unit
) {
    val filterTypes = listOf(
        null to "All",
        CallType.INCOMING to "Incoming",
        CallType.MISSED to "Missed",
        CallType.OUTGOING to "Outgoing"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filterTypes.forEach { (type, label) ->
            val isSelected = selectedType == type
            val color = type?.color?.let { Color(it) } ?: PrimaryBlue
            
            FilterChip(
                selected = isSelected,
                onClick = { onTypeSelected(type) },
                label = { 
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                leadingIcon = if (type != null) {
                    {
                        Icon(
                            imageVector = when (type) {
                                CallType.INCOMING -> Icons.Default.CallReceived
                                CallType.OUTGOING -> Icons.Default.CallMade
                                CallType.MISSED -> Icons.Default.CallMissed
                                else -> Icons.Default.Phone
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) color else SalesCrmTheme.colors.textMuted
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.15f),
                    selectedLabelColor = color,
                    containerColor = SalesCrmTheme.colors.surfaceVariant,
                    labelColor = SalesCrmTheme.colors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun CallAdvancedFiltersRow(
    crmFilter: CrmFilter,
    onCrmFilterChange: (CrmFilter) -> Unit,
    contactFilter: ContactFilter,
    onContactFilterChange: (ContactFilter) -> Unit,
    noteFilter: NoteFilter,
    onNoteFilterChange: (NoteFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // CRM Filter Dropdown
        FilterDropdown(
            label = crmFilter.label,
            icon = Icons.Default.Groups,
            items = CrmFilter.entries,
            onItemSelected = onCrmFilterChange
        )
        
        // Contact Filter Dropdown
        FilterDropdown(
            label = contactFilter.label,
            icon = Icons.Default.ContactPhone,
            items = ContactFilter.entries,
            onItemSelected = onContactFilterChange
        )
        
        // Note Filter Dropdown
        FilterDropdown(
            label = noteFilter.label,
            icon = Icons.Default.StickyNote2,
            items = NoteFilter.entries,
            onItemSelected = onNoteFilterChange
        )
    }
}

@Composable
private fun <T> FilterDropdown(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<T>,
    onItemSelected: (T) -> Unit
) where T : Enum<T> {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        FilterChip(
            selected = label != "All",
            onClick = { expanded = true },
            label = { Text(text = label, style = MaterialTheme.typography.labelMedium) },
            leadingIcon = { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                selectedLabelColor = PrimaryBlue,
                selectedLeadingIconColor = PrimaryBlue,
                selectedTrailingIconColor = PrimaryBlue,
                containerColor = SalesCrmTheme.colors.surfaceVariant,
                labelColor = SalesCrmTheme.colors.textSecondary
            )
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SalesCrmTheme.colors.surface)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { 
                        val itemLabel = when(item) {
                            is CrmFilter -> item.label
                            is ContactFilter -> item.label
                            is NoteFilter -> item.label
                            else -> item.name
                        }
                        Text(itemLabel, color = SalesCrmTheme.colors.textPrimary) 
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ==================== DATE SEPARATOR ====================

@Composable
private fun DateSeparator(date: LocalDate) {
    val today = LocalDate.now()
    val displayText = when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = SalesCrmTheme.colors.textSecondary
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = SalesCrmTheme.colors.border
        )
    }
}

// ==================== CALL GROUP CARD ====================

@Composable
private fun CallGroupCard(
    group: CallLogGroup,
    hasNote: Boolean,
    onClick: () -> Unit
) {
    val lastCall = group.lastCall ?: return
    val callTypeColor = Color(lastCall.callType.color)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = SalesCrmTheme.colors.surface
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact call type indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(callTypeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (lastCall.callType) {
                        CallType.INCOMING -> Icons.Default.CallReceived
                        CallType.OUTGOING -> Icons.Default.CallMade
                        CallType.MISSED -> Icons.Default.CallMissed
                        CallType.REJECTED -> Icons.Default.CallEnd
                        else -> Icons.Default.Phone
                    },
                    contentDescription = null,
                    tint = callTypeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            // Call info - compact
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = group.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SalesCrmTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // Badges in row
                    if (group.linkedPersonId != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(AccentGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "CRM",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Note indicator
                    if (hasNote) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.StickyNote2,
                            contentDescription = "Has note",
                            modifier = Modifier.size(14.dp),
                            tint = AccentOrange
                        )
                    }
                    
                    // Call count badge
                    if (group.totalCalls > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(PrimaryBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${group.totalCalls}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Compact info row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call type
                    Text(
                        text = lastCall.callType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = callTypeColor,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Duration
                    if (lastCall.duration > 0) {
                        Text(
                            text = " • ${CallLogRepository.formatDurationShort(lastCall.duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SalesCrmTheme.colors.textMuted
                        )
                    }
                    
                    // Time
                    Text(
                        text = " • ${lastCall.timestamp.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SalesCrmTheme.colors.textMuted
                    )
                    
                    // Missed count
                    if (group.missedCount > 0 && group.totalCalls > 1) {
                        Text(
                            text = " • ${group.missedCount} missed",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentRed
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun CallStatChip(
    count: Int,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// ==================== EMPTY STATE ====================

@Composable
private fun EmptyCallsState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.PhoneDisabled,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SalesCrmTheme.colors.textMuted
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No calls found",
                style = MaterialTheme.typography.titleMedium,
                color = SalesCrmTheme.colors.textSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Try adjusting your filters or date range",
                style = MaterialTheme.typography.bodyMedium,
                color = SalesCrmTheme.colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== CALL LOG DETAIL SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogDetailSheet(
    group: CallLogGroup,
    onDismiss: () -> Unit,
    onAddNote: (String) -> Unit,
    onAddToPipeline: () -> Unit,
    onViewPerson: () -> Unit,
    onCall: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var noteContent by remember { mutableStateOf("") }
    var showAddNote by remember { mutableStateOf(false) }
    
    // Find existing notes from call entries
    val existingNotes = remember(group.calls) {
        group.calls.filter { !it.note.isNullOrBlank() }
    }
    
    // Show only last 5 calls to keep modal compact
    val recentCalls = remember(group.calls) { group.calls.take(5) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header - compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SalesCrmTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (group.displayName != group.phoneNumber) {
                        Text(
                            text = group.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = SalesCrmTheme.colors.textMuted
                        )
                    }
                }
                
                // Call button
                FilledIconButton(
                    onClick = onCall,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Call",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Compact stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SalesCrmTheme.colors.surfaceVariant)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactStatItem(
                    value = group.totalCalls.toString(),
                    label = "Total",
                    color = PrimaryBlue
                )
                CompactStatItem(
                    value = group.incomingCount.toString(),
                    label = "In",
                    color = AccentGreen
                )
                CompactStatItem(
                    value = group.outgoingCount.toString(),
                    label = "Out",
                    color = PrimaryBlue
                )
                CompactStatItem(
                    value = group.missedCount.toString(),
                    label = "Missed",
                    color = if (group.missedCount > 0) AccentRed else SalesCrmTheme.colors.textMuted
                )
                CompactStatItem(
                    value = CallLogRepository.formatDurationShort(group.totalDuration),
                    label = "Duration",
                    color = SalesCrmTheme.colors.textSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action row - compact buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (group.linkedPersonId != null) {
                    OutlinedButton(
                        onClick = onViewPerson,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                        border = BorderStroke(1.dp, PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Profile", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = onAddToPipeline,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                        border = BorderStroke(1.dp, AccentGreen)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to CRM", style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                OutlinedButton(
                    onClick = { showAddNote = !showAddNote },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                    border = BorderStroke(1.dp, AccentOrange)
                ) {
                    Icon(Icons.Default.StickyNote2, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Note", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            // Add note section - compact
            AnimatedVisibility(visible = showAddNote) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        placeholder = { Text("Add a note...", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = SalesCrmTheme.colors.border
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showAddNote = false; noteContent = "" },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (noteContent.isNotBlank()) {
                                    onAddNote(noteContent)
                                    noteContent = ""
                                    showAddNote = false
                                }
                            },
                            enabled = noteContent.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Save", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            
            // Existing notes - compact
            if (existingNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Notes (${existingNotes.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SalesCrmTheme.colors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                existingNotes.take(3).forEach { callEntry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = SalesCrmTheme.colors.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.StickyNote2,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AccentOrange
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = callEntry.note ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SalesCrmTheme.colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = callEntry.timestamp.toDisplayString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SalesCrmTheme.colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
            
            // Recent call history - compact
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Recent Calls",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = SalesCrmTheme.colors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            recentCalls.forEach { call ->
                CompactCallHistoryItem(call = call)
            }
            
            if (group.calls.size > 5) {
                Text(
                    text = "+${group.calls.size - 5} more calls",
                    style = MaterialTheme.typography.labelSmall,
                    color = SalesCrmTheme.colors.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactStatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = SalesCrmTheme.colors.textMuted
        )
    }
}

@Composable
private fun CompactCallHistoryItem(call: CallLogEntry) {
    val callTypeColor = Color(call.callType.color)
    val context = LocalContext.current
    val audioState = rememberAudioPlayerState()
    val isThisPlaying = call.recordingPath?.let { audioState.isPlayingPath(it) } ?: false
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small call type icon
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(callTypeColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (call.callType) {
                    CallType.INCOMING -> Icons.Default.CallReceived
                    CallType.OUTGOING -> Icons.Default.CallMade
                    CallType.MISSED -> Icons.Default.CallMissed
                    CallType.REJECTED -> Icons.Default.CallEnd
                    else -> Icons.Default.Phone
                },
                contentDescription = null,
                tint = callTypeColor,
                modifier = Modifier.size(14.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Call info - inline
        Text(
            text = call.callType.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = SalesCrmTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        
        // Duration
        if (call.duration > 0) {
            Text(
                text = CallLogRepository.formatDurationShort(call.duration),
                style = MaterialTheme.typography.labelSmall,
                color = SalesCrmTheme.colors.textMuted
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        // Time and Recording Icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (call.recordingPath != null) {
                Icon(
                    imageVector = if (isThisPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = if (isThisPlaying) "Pause" else "Play recording",
                    tint = if (isThisPlaying) AccentGreen else PrimaryBlue,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { 
                            audioState.toggle(call.recordingPath) { error ->
                                android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Text(
                text = call.timestamp.format(DateTimeFormatter.ofPattern("MMM d, h:mm a")),
                style = MaterialTheme.typography.labelSmall,
                color = SalesCrmTheme.colors.textMuted
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallSettingsSheet(
    settings: CallSettings,
    onSettingsChange: (CallSettings) -> Unit,
    isImporting: Boolean,
    importProgress: Float,
    onImportPastLogs: () -> Unit,
    onShowTrackingDatePicker: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SalesCrmTheme.colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SalesCrmTheme.colors.textMuted.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Call Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SalesCrmTheme.colors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // SIM Selection
            Text(
                "SIM Tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SalesCrmTheme.colors.textPrimary
            )
            
            val simList = remember { CallLogRepository(context).getActiveSims() }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SimSelection.entries.forEach { selection ->
                    val isSelected = settings.simSelection == selection
                    val deviceSimName = when (selection) {
                        SimSelection.SIM_1 -> simList.getOrNull(0)?.displayName?.toString()
                        SimSelection.SIM_2 -> simList.getOrNull(1)?.displayName?.toString()
                        else -> null
                    }
                    
                    Surface(
                        onClick = { onSettingsChange(settings.copy(simSelection = selection)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryBlue.copy(alpha = 0.1f) else SalesCrmTheme.colors.surfaceVariant,
                        border = if (isSelected) BorderStroke(2.dp, PrimaryBlue) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (selection == SimSelection.BOTH) Icons.Default.SimCard else Icons.Default.SdCard,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.textMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                deviceSimName ?: selection.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) PrimaryBlue else SalesCrmTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            HorizontalDivider(color = SalesCrmTheme.colors.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            
            // Tracking Start Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tracking Start Date",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                    Text(
                        "Track calls from this date onwards",
                        style = MaterialTheme.typography.bodySmall,
                        color = SalesCrmTheme.colors.textMuted
                    )
                }
                
                FilterChip(
                    selected = true,
                    onClick = onShowTrackingDatePicker,
                    label = { Text(settings.trackingStartDate.toString()) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                        selectedLabelColor = PrimaryBlue,
                        selectedLeadingIconColor = PrimaryBlue
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Sync Options
            SyncOptionRow(
                title = "Auto Sync Future Call Logs",
                description = "Show calls in contact activities automatically",
                checked = settings.autoSyncToActivities,
                onCheckedChange = { onSettingsChange(settings.copy(autoSyncToActivities = it)) }
            )
            
            SyncOptionRow(
                title = "Start New Contacts with Call History",
                description = "Show past history when creating a new lead",
                checked = settings.startContactsWithCallHistory,
                onCheckedChange = { onSettingsChange(settings.copy(startContactsWithCallHistory = it)) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isImporting) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Importing past calls... ${(importProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LinearProgressIndicator(
                        progress = importProgress,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = PrimaryBlue,
                        trackColor = PrimaryBlue.copy(alpha = 0.1f)
                    )
                }
            } else {
                Button(
                    onClick = onImportPastLogs,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SalesCrmTheme.colors.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp), tint = SalesCrmTheme.colors.textPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Import Past Calls for CRM Leads", color = SalesCrmTheme.colors.textPrimary)
                }
            }
            
            HorizontalDivider(color = SalesCrmTheme.colors.border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
            
            // Recording Path
            Text(
                "Call Recording Path",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = SalesCrmTheme.colors.textPrimary
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = settings.recordingPath,
                onValueChange = { onSettingsChange(settings.copy(recordingPath = it)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { Icon(Icons.Default.FolderOpen, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = SalesCrmTheme.colors.border,
                    focusedLabelColor = PrimaryBlue,
                    unfocusedLabelColor = SalesCrmTheme.colors.textMuted
                ),
                placeholder = { Text("Auto-detect (recommended)") },
                supportingText = {
                    Text("Scanning: Music/Recordings/Call Recording, etc.")
                }
            )
        }
    }
}

@Composable
private fun SyncOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = SalesCrmTheme.colors.textPrimary
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = SalesCrmTheme.colors.textMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SalesCrmTheme.colors.border
            )
        )
    }
}
