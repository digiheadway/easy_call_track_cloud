package com.miniclick.calltrackmanage.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.ui.common.*
import com.miniclick.calltrackmanage.ui.utils.formatDurationShort
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: HomeViewModel = viewModel(),
    syncStatusBar: @Composable () -> Unit = {},
    onNavigateToTab: ((Int) -> Unit)? = null // 0 = Calls, 1 = Persons
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterModal by remember { mutableStateOf(false) }

    val availableLabels = remember(uiState.persons) { 
        uiState.persons.mapNotNull { it.label }
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted() 
    }
    
    val stats = uiState.reportStats
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Filter Icon
                    IconButton(onClick = { showFilterModal = true }) {
                        BadgedBox(
                            badge = {
                                if (uiState.activeFilterCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(uiState.activeFilterCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (showFilterModal || uiState.activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Date Range Icon (Last)
                    DateRangeHeaderAction(
                        dateRange = uiState.dateRange,
                        onDateRangeChange = { range, start, end -> viewModel.setDateRange(range, start, end) }
                    )
                }
            }
        }
        
        syncStatusBar()

        if (showFilterModal) {
            CallFilterModal(
                uiState = uiState,
                viewModel = viewModel,
                onDismiss = { showFilterModal = false },
                availableLabels = availableLabels
            )
        }
        
        if (uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { ReportsCardShimmer() }
                item { ReportsCardShimmer() }
                item { ReportsCardShimmer() }
                item { ReportsCardShimmer() }
            }
        } else if (uiState.simSelection == "Off") {
            EmptyState(
                icon = Icons.Default.SimCard,
                title = "Select Sim Card to Track",
                description = "Capture your call logs by selecting which SIM cards to monitor."
            )
        } else if (uiState.filteredLogs.isEmpty()) {
            val isFiltered = uiState.activeFilterCount > 0 || uiState.searchQuery.isNotEmpty()
            
            if (isFiltered) {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No results found",
                    description = "Try adjusting your filters or search query."
                )
            } else {
                EmptyState(
                    icon = Icons.Default.BarChart,
                    title = "No data for reports",
                    description = "Reports will be generated once you have some call activity."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Overview Summary Card - Clickable to Calls
                item {
                    OverviewCard(
                        totalCalls = stats.totalCalls,
                        uniqueContacts = stats.uniqueContacts,
                        totalDuration = stats.totalDuration,
                        onClick = {
                            viewModel.setCallTypeFilter(CallTabFilter.ALL)
                            onNavigateToTab?.invoke(0) // Navigate to Calls
                        }
                    )
                }
                
                // Call Types Breakdown - Each row clickable
                item {
                    CallTypesCard(
                        stats = stats,
                        onTypeClick = { filter ->
                            viewModel.setCallTypeFilter(filter)
                            onNavigateToTab?.invoke(0) // Navigate to Calls
                        }
                    )
                }
                
                // Connection Stats Card
                item {
                    ConnectionStatsCard(stats = stats)
                }
                
                // Duration Stats Card
                item {
                    DurationStatsCard(stats = stats)
                }
                
                // Top Callers (by call count)
                if (stats.topCallers.isNotEmpty()) {
                    item {
                        TopCallersCard(
                            title = "Most Frequent Callers",
                            subtitle = "By number of calls",
                            callers = stats.topCallers,
                            showDuration = false
                        )
                    }
                }
                
                // Most Talked (by duration)
                if (stats.mostTalked.isNotEmpty()) {
                    item {
                        TopCallersCard(
                            title = "Longest Conversations",
                            subtitle = "By total talk time",
                            callers = stats.mostTalked,
                            showDuration = true
                        )
                    }
                }
                
                // Contacts Breakdown
                item {
                    ContactsBreakdownCard(
                        stats = stats,
                        onTypeClick = { filter ->
                            viewModel.setContactsFilter(filter)
                            onNavigateToTab?.invoke(0) // Navigate to Calls
                        }
                    )
                }
                
                // Notes & Activity Card
                item {
                    NotesActivityCard(stats = stats)
                }
                
                // Labels Distribution
                if (stats.labelDistribution.isNotEmpty()) {
                    item {
                        LabelDistributionCard(labels = stats.labelDistribution)
                    }
                }
                
                // Daily Activity
                if (stats.dailyStats.isNotEmpty()) {
                    item {
                        DailyActivityCard(dailyStats = stats.dailyStats)
                    }
                }
                
                // Hourly Activity
                if (stats.hourlyStats.isNotEmpty()) {
                    item {
                        HourlyActivityCard(hourlyStats = stats.hourlyStats)
                    }
                }
            }
        }
    }
}

// ========================= Card Components =========================

@Composable
fun OverviewCard(
    totalCalls: Int,
    uniqueContacts: Int,
    totalDuration: Long,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View all",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = totalCalls.toString(),
                    label = "Total Calls",
                    icon = Icons.Default.Call,
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    value = uniqueContacts.toString(),
                    label = "Contacts",
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.secondary
                )
                StatItem(
                    value = formatDurationShort(totalDuration),
                    label = "Talk Time",
                    icon = Icons.Default.Timer,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun CallTypesCard(
    stats: ReportStats,
    onTypeClick: (CallTabFilter) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Call Types",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            ClickableStatRow(
                label = "Attended (Incoming >3s)",
                value = stats.connectedIncoming.toString(),
                icon = Icons.AutoMirrored.Filled.CallReceived,
                color = Color(0xFF4CAF50),
                onClick = { onTypeClick(CallTabFilter.ATTENDED) }
            )
            ClickableStatRow(
                label = "Responded (Outgoing >3s)",
                value = stats.connectedOutgoing.toString(),
                icon = Icons.AutoMirrored.Filled.CallMade,
                color = Color(0xFF2196F3),
                onClick = { onTypeClick(CallTabFilter.RESPONDED) }
            )
            ClickableStatRow(
                label = "NOT ATTENDED (Incoming <=3s)",
                value = (stats.incomingCalls - stats.connectedIncoming).toString(),
                icon = Icons.AutoMirrored.Filled.CallMissed,
                color = Color(0xFFF44336),
                onClick = { onTypeClick(CallTabFilter.NOT_ATTENDED) }
            )
            ClickableStatRow(
                label = "Not Responded (Outgoing <=3s)",
                value = (stats.outgoingCalls - stats.connectedOutgoing).toString(),
                icon = Icons.AutoMirrored.Filled.PhoneMissed,
                color = Color(0xFFFF9800),
                onClick = { onTypeClick(CallTabFilter.NOT_RESPONDED) }
            )
            if (stats.rejectedCalls > 0) {
                ClickableStatRow(
                    label = "Rejected / Blocked",
                    value = stats.rejectedCalls.toString(),
                    icon = Icons.Default.Block,
                    color = Color(0xFF9E9E9E),
                    onClick = { onTypeClick(CallTabFilter.IGNORED) }
                )
            }
        }
    }
}

@Composable
fun ConnectionStatsCard(stats: ReportStats) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Connection Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            StatRow(
                label = "Connected Calls",
                value = "${stats.connectedCalls}",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            )
            StatRow(
                label = "Not Connected",
                value = "${stats.notConnectedCalls}",
                icon = Icons.Default.Cancel,
                color = Color(0xFFF44336)
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Connection rate progress
            val connectionRate = if (stats.totalCalls > 0) 
                (stats.connectedCalls * 100 / stats.totalCalls) else 0
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Connection Rate",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        "$connectionRate%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            connectionRate >= 75 -> Color(0xFF4CAF50)
                            connectionRate >= 50 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { connectionRate / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        connectionRate >= 75 -> Color(0xFF4CAF50)
                        connectionRate >= 50 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
            
            // Incoming vs Outgoing attendance
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            
            val incomingRate = if (stats.incomingCalls > 0) 
                (stats.connectedIncoming * 100 / stats.incomingCalls) else 0
            val outgoingRate = if (stats.outgoingCalls > 0) 
                (stats.connectedOutgoing * 100 / stats.outgoingCalls) else 0
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniProgressStat(
                    label = "Incoming Attended",
                    value = "$incomingRate%",
                    progress = incomingRate / 100f,
                    color = Color(0xFF4CAF50)
                )
                MiniProgressStat(
                    label = "Outgoing Responded",
                    value = "$outgoingRate%",
                    progress = outgoingRate / 100f,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
fun DurationStatsCard(stats: ReportStats) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Duration Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DurationStatItem(
                    label = "Total",
                    duration = stats.totalDuration,
                    icon = Icons.Default.Timer
                )
                DurationStatItem(
                    label = "Average",
                    duration = stats.avgDuration,
                    icon = Icons.Default.AvTimer
                )
                DurationStatItem(
                    label = "Longest",
                    duration = stats.maxDuration,
                    icon = Icons.Default.Star
                )
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            
            // Incoming vs Outgoing duration
            val totalConnected = stats.incomingDuration + stats.outgoingDuration
            val incomingPercent = if (totalConnected > 0) (stats.incomingDuration * 100 / totalConnected).toInt() else 50
            
            Text(
                "Time Distribution",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (incomingPercent > 0) {
                    Box(
                        modifier = Modifier
                            .weight(incomingPercent.coerceAtLeast(1).toFloat())
                            .fillMaxHeight()
                            .background(Color(0xFF4CAF50))
                    )
                }
                if (100 - incomingPercent > 0) {
                    Box(
                        modifier = Modifier
                            .weight((100 - incomingPercent).coerceAtLeast(1).toFloat())
                            .fillMaxHeight()
                            .background(Color(0xFF2196F3))
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Incoming ${formatDurationShort(stats.incomingDuration)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Outgoing ${formatDurationShort(stats.outgoingDuration)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun TopCallersCard(
    title: String,
    subtitle: String,
    callers: List<TopCaller>,
    showDuration: Boolean
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            
            callers.forEachIndexed { index, caller ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when (index) {
                                    0 -> Color(0xFFFFD700)
                                    1 -> Color(0xFFC0C0C0)
                                    2 -> Color(0xFFCD7F32)
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (index < 3) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = caller.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row {
                            Text(
                                text = "${caller.callCount} calls",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (showDuration) {
                                Text(
                                    text = " • ${formatDurationShort(caller.totalDuration)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    // Call type indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (caller.incomingCount > 0) {
                            MiniCallBadge(
                                count = caller.incomingCount,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        if (caller.outgoingCount > 0) {
                            MiniCallBadge(
                                count = caller.outgoingCount,
                                color = Color(0xFF2196F3)
                            )
                        }
                        if (caller.missedCount > 0) {
                            MiniCallBadge(
                                count = caller.missedCount,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                }
                if (index < callers.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 40.dp))
                }
            }
        }
    }
}

@Composable
fun ContactsBreakdownCard(
    stats: ReportStats,
    onTypeClick: (ContactsFilter) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Contacts Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.uniqueContacts.toString(),
                    label = "Total",
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onTypeClick(ContactsFilter.ALL) }
                )
                StatItem(
                    value = stats.savedContacts.toString(),
                    label = "Saved",
                    icon = Icons.Default.PersonAdd,
                    color = Color(0xFF4CAF50),
                    onClick = { onTypeClick(ContactsFilter.IN_CONTACTS) }
                )
                StatItem(
                    value = stats.unsavedContacts.toString(),
                    label = "Unsaved",
                    icon = Icons.Default.PersonOff,
                    color = Color(0xFFFF9800),
                    onClick = { onTypeClick(ContactsFilter.NOT_IN_CONTACTS) }
                )
            }
        }
    }
}

@Composable
fun NotesActivityCard(stats: ReportStats) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Notes & Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            StatRow(
                label = "Calls with Notes",
                value = stats.callsWithNotes.toString(),
                icon = Icons.AutoMirrored.Filled.StickyNote2,
                color = MaterialTheme.colorScheme.secondary
            )
            StatRow(
                label = "Reviewed Calls",
                value = stats.reviewedCalls.toString(),
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            )
            StatRow(
                label = "Calls with Recordings",
                value = stats.callsWithRecordings.toString(),
                icon = Icons.Default.Mic,
                color = Color(0xFFF44336)
            )
            if (stats.shortCalls > 0) {
                StatRow(
                    label = "Short Calls (<10s)",
                    value = stats.shortCalls.toString(),
                    icon = Icons.Default.Warning,
                    color = Color(0xFFFF9800) // Orange warning color
                )
            }
            StatRow(
                label = "Persons with Notes",
                value = stats.personsWithNotes.toString(),
                icon = Icons.Default.PersonPin,
                color = MaterialTheme.colorScheme.tertiary
            )
            StatRow(
                label = "Persons with Labels",
                value = stats.personsWithLabels.toString(),
                icon = Icons.AutoMirrored.Filled.Label,
                color = Color(0xFF9C27B0)
            )
        }
    }
}

@Composable
fun LabelDistributionCard(labels: List<Pair<String, Int>>) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Label Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(labels) { (label, count) ->
                    AssistChip(
                        onClick = { },
                        label = { Text("$label ($count)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DailyActivityCard(dailyStats: Map<String, DayStat>) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Calls per day of week",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            
            val maxCount = dailyStats.values.maxOfOrNull { it.count } ?: 1
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEach { day ->
                    val stat = dailyStats[day]
                    val count = stat?.count ?: 0
                    val heightFraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(60.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(heightFraction.coerceAtLeast(0.05f))
                                    .align(Alignment.BottomCenter)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyActivityCard(hourlyStats: Map<Int, Int>) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hourly Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Peak hours for calls",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            
            // Find top 3 hours
            val topHours = hourlyStats.entries
                .sortedByDescending { it.value }
                .take(3)
            
            val peakHour = topHours.firstOrNull()
            
            if (peakHour != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Peak Hour",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formatHour(peakHour.key),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${peakHour.value} calls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Other popular hours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topHours.drop(1).forEach { (hour, count) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatHour(hour),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$count calls",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========================= Helper Components =========================

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun ClickableStatRow(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun MiniProgressStat(
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(120.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DurationStatItem(
    label: String,
    duration: Long,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatDurationShort(duration),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MiniCallBadge(count: Int, color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}
