package com.miniclick.calltrackmanage.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.ui.common.*
import com.miniclick.calltrackmanage.ui.home.viewmodel.*
import com.miniclick.calltrackmanage.util.formatting.formatDurationShort
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun ReportsScreen(
    viewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
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

                    // Date Range Icon
                    DateRangeHeaderAction(
                        dateRange = uiState.dateRange,
                        onDateRangeChange = { range, start, end -> viewModel.setDateRange(range, start, end) }
                    )

                    var showMainHeadMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMainHeadMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMainHeadMenu,
                            onDismissRequest = { showMainHeadMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Show Comparisons") },
                                onClick = { 
                                    showMainHeadMenu = false
                                    viewModel.toggleShowComparisons()
                                },
                                leadingIcon = { 
                                    Icon(
                                        if (uiState.showComparisons) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, 
                                        contentDescription = null,
                                        tint = if (uiState.showComparisons) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Full Report") },
                                onClick = { 
                                    showMainHeadMenu = false
                                    // Placeholder for full report export
                                },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
        
        syncStatusBar()

        // Report Categorization Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ReportCategory.entries.toTypedArray()) { category ->
                FilterChip(
                    selected = uiState.reportCategory == category,
                    onClick = { viewModel.setReportCategory(category) },
                    label = { 
                        Text(
                            text = category.name.replace("_", " ").lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        ) 
                    },
                    leadingIcon = if (uiState.reportCategory == category) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

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
                // Filter based on category
                val category = uiState.reportCategory
                
                // OVERVIEW Section
                if (category == ReportCategory.OVERVIEW) {
                    item {
                        OverviewCard(
                            stats = stats,
                            onClick = {
                                viewModel.setCallTypeFilter(CallTabFilter.ALL)
                                onNavigateToTab?.invoke(0)
                            }
                        )
                    }
                    
                    item {
                        CallStatsTableCard(
                            stats = stats,
                            dateRange = uiState.dateRange,
                            onTypeClick = { filter ->
                                viewModel.setCallTypeFilter(filter)
                                onNavigateToTab?.invoke(0)
                            }
                        )
                    }
                    
                    item {
                        DurationStatsCard(stats = stats, onNavigateToTab = onNavigateToTab)
                    }
                    
                    item {
                        NotesActivityCard(stats = stats, onNavigateToTab = onNavigateToTab)
                    }
                    
                    item {
                        EngagementTableCard(stats = stats)
                    }
                    
                    if (stats.mostTalked.isNotEmpty()) {
                        item {
                            TopCallersCard(
                                title = "Longest Calls",
                                subtitle = "By total talk time",
                                callers = stats.mostTalked,
                                showDuration = true,
                                onNavigateToTab = onNavigateToTab
                            )
                        }
                    }

                    if (stats.topCallers.isNotEmpty()) {
                        item {
                            TopCallersCard(
                                title = "Most Calls",
                                subtitle = "By number of calls",
                                callers = stats.topCallers,
                                showDuration = false,
                                onNavigateToTab = onNavigateToTab
                            )
                        }
                    }
                }
                
                // DAILY AVERAGE Section
                if (category == ReportCategory.DAILY_AVERAGE) {
                    if (stats.dailyStats.isNotEmpty()) {
                        item {
                            DailyActivityCard(dailyStats = stats.dailyStats)
                        }
                        
                        item {
                            DailyAverageSummaryCard(stats = stats)
                        }
                    } else {
                        item {
                            EmptyState(
                                icon = Icons.Default.CalendarToday,
                                title = "No daily data",
                                description = "Daily averages will appear here as you track more calls."
                            )
                        }
                    }
                }
                
                // DATA ANALYSIS Section
                if (category == ReportCategory.DATA_ANALYSIS) {
                    if (stats.hourlyStats.isNotEmpty()) {
                        item {
                            HourlyActivityCard(hourlyStats = stats.hourlyStats)
                        }
                    }
                    
                    if (stats.labelDistribution.isNotEmpty()) {
                        item {
                            LabelDistributionCard(labels = stats.labelDistribution)
                        }
                    }

                    item {
                        ConnectionStatsCard(
                            stats = stats,
                            viewModel = viewModel,
                            onNavigateToTab = onNavigateToTab
                        )
                    }
                }
                
                // FREQUENCY Section
                if (category == ReportCategory.FREQUENCY) {
                    // Top Callers (by call count)
                    if (stats.topCallers.isNotEmpty()) {
                        item {
                            TopCallersCard(
                                title = "Most Frequent Callers",
                                subtitle = "By number of calls",
                                callers = stats.topCallers,
                                showDuration = false,
                                onNavigateToTab = onNavigateToTab
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
                                showDuration = true,
                                onNavigateToTab = onNavigateToTab
                            )
                        }
                    }

                    if (stats.topCallers.isEmpty() && stats.mostTalked.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.TrendingUp,
                                title = "No frequency data",
                                description = "Frequency analysis will appear once you have call activity."
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========================= Card Components =========================

private fun shareReportData(context: Context, title: String, data: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Report: $title\n\n$data")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export $title")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to export data", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ReportCardHeader(
    title: String,
    onDrillDown: (() -> Unit)? = null,
    exportData: String = ""
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Drill Down") },
                    onClick = { 
                        showMenu = false
                        onDrillDown?.invoke()
                    },
                    leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Check Data") },
                    onClick = { showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Export") },
                    onClick = { 
                        showMenu = false
                        if (exportData.isNotEmpty()) {
                            shareReportData(context, title, exportData)
                        } else {
                            Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
fun OverviewCard(
    stats: ReportStats,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportCardHeader(
                title = "Overview",
                onDrillDown = onClick,
                exportData = "Total: ${stats.totalCalls}, Unique: ${stats.uniqueContacts}, Talk Time: ${formatDurationShort(stats.totalDuration)}"
            )
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatItem(
                    value = stats.totalCalls.toString(),
                    label = "Total Calls",
                    icon = Icons.Default.Call,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onClick
                )
                StatItem(
                    value = stats.uniqueContacts.toString(),
                    label = "Unique Calls",
                    icon = Icons.Default.People,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onClick
                )
                StatItem(
                    value = formatDurationShort(stats.totalDuration),
                    label = "Talk Time",
                    icon = Icons.Default.Timer,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onClick
                )
                StatItem(
                    value = formatDurationShort(stats.avgDuration),
                    label = "Avg Talk Time",
                    icon = Icons.Default.AvTimer,
                    color = MaterialTheme.colorScheme.error,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
fun CallStatsTableCard(
    stats: ReportStats,
    dateRange: DateRange,
    onTypeClick: (CallTabFilter) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportCardHeader(title = "Calls Stats")
            Spacer(Modifier.height(8.dp))
            
            // Header
            Row(Modifier.fillMaxWidth()) {
                Text("Type", Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("Total", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("Unique", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("Trend", Modifier.weight(0.5f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            
            // Rows
            CallStatTableRow("Connected", stats.connectedCalls, stats.connectedUnique, stats.comparisonAverages["connected"], dateRange)
            CallStatTableRow("Outgoings", stats.outgoingCalls, -1, stats.comparisonAverages["outgoing"], dateRange) // Unique outgoing not standard, can use outgoingUnique if needed
            CallStatTableRow("Answered", stats.connectedIncoming, stats.connectedIncomingUnique, stats.comparisonAverages["answered"], dateRange) // Incoming Answered
            CallStatTableRow("Out Not Conn", stats.outgoingNotConnected, stats.outgoingNotConnectedUnique, stats.comparisonAverages["outgoingNotConnected"], dateRange)
            CallStatTableRow("Not Answered", stats.notAnsweredIncoming, stats.notAnsweredIncomingUnique, stats.comparisonAverages["notAnswered"], dateRange)
            CallStatTableRow("May be Failed", stats.mayFailedCount, stats.mayFailedUnique, stats.comparisonAverages["mayFailed"], dateRange)
            
            Spacer(Modifier.height(16.dp))
            
            // Rates
            RateRow("Outgoing Connection Rate", stats.connectedOutgoing, stats.outgoingCalls)
            RateRow("Answer Rate (Calls)", stats.connectedIncoming, stats.incomingCalls)
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun CallStatTableRow(
    label: String,
    total: Int,
    unique: Int,
    avg: Float?,
    range: DateRange
) {
    val showTrend = avg != null && avg > 0
    val trendUp = if (showTrend) total >= (avg!! * getDaysMultiplier(range)) else false
    // Simple logic: if total is greater than daily avg * 1 (for Today) -> Up.
    // Actually, user said "based on total calls of last 7 days average".
    // We treat 'avg' as Daily Average of last 7 days.
    // We compare 'total' (of current selection) vs 'avg'.
    // If current selection is multiple days, typically we compare daily avg vs daily avg.
    // But let's assume "Today" view for specific iconography request.
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium)
        Text(total.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Text(if (unique >= 0) unique.toString() else "-", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
             if (showTrend) {
                Icon(
                    imageVector = if (trendUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (trendUp) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(16.dp)
                )
             }
        }
    }
}

private fun getDaysMultiplier(range: DateRange): Float {
    return when(range) {
        DateRange.TODAY -> 1f
        DateRange.LAST_3_DAYS -> 3f
        DateRange.LAST_7_DAYS -> 7f
        else -> 1f // Default to 1 to compare Total vs Daily Avg (which works for Today)
    }
}


@Composable
fun RateRow(label: String, numerator: Int, denominator: Int) {
    val rate = if (denominator > 0) (numerator * 100 / denominator) else 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("$rate%", fontWeight = FontWeight.Bold)
    }
    LinearProgressIndicator(
        progress = { rate / 100f },
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = if (rate > 50) Color(0xFF4CAF50) else Color(0xFFFF9800),
    )
}

@Composable
fun EngagementTableCard(stats: ReportStats) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportCardHeader(title = "Engagement")
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth()) {
                Text("Duration", Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("Total", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("Out", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Text("In", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            
            stats.durationBuckets.forEach { bucket ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(bucket.label, Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)
                    Text(bucket.total.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    Text(bucket.outCount.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, color = Color(0xFF2196F3))
                    Text(bucket.inCount.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}

@Composable
fun ConnectionStatsCard(
    stats: ReportStats,
    viewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigateToTab: ((Int) -> Unit)? = null
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportCardHeader(
                title = "Connection Analysis",
                onDrillDown = { 
                    viewModel.setCallTypeFilter(CallTabFilter.ALL)
                    onNavigateToTab?.invoke(0)
                },
                exportData = """
                    Connected: ${stats.connectedCalls}
                    Not Connected: ${stats.notConnectedCalls}
                    Rate: ${if (stats.totalCalls > 0) (stats.connectedCalls * 100 / stats.totalCalls) else 0}%
                """.trimIndent()
            )
            Spacer(Modifier.height(16.dp))
            
            ClickableStatRow(
                label = "Connected Calls",
                value = "${stats.connectedCalls}",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50),
                onClick = {
                    viewModel.setConnectedFilter(ConnectedFilter.CONNECTED)
                    onNavigateToTab?.invoke(0)
                }
            )
            ClickableStatRow(
                label = "Not Connected",
                value = "${stats.notConnectedCalls}",
                icon = Icons.Default.Cancel,
                color = Color(0xFFF44336),
                onClick = {
                    viewModel.setConnectedFilter(ConnectedFilter.NOT_CONNECTED)
                    onNavigateToTab?.invoke(0)
                }
            )
            
            Spacer(Modifier.height(12.dp))
            
            // Connection rate progress
            val connectionRate = if (stats.totalCalls > 0) 
                (stats.connectedCalls * 100 / stats.totalCalls) else 0
            
            Column(
                modifier = Modifier.clickable { 
                    // No direct filter for Rate, drill down to ALL
                }
            ) {
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
fun DurationStatsCard(
    stats: ReportStats,
    onNavigateToTab: ((Int) -> Unit)? = null
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportCardHeader(
                title = "Duration Insights",
                onDrillDown = { onNavigateToTab?.invoke(0) },
                exportData = """
                    Total: ${formatDurationShort(stats.totalDuration)}
                    Average: ${formatDurationShort(stats.avgDuration)}
                    Longest: ${formatDurationShort(stats.maxDuration)}
                """.trimIndent()
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
    showDuration: Boolean,
    onNavigateToTab: ((Int) -> Unit)? = null
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportCardHeader(
                title = title,
                onDrillDown = { onNavigateToTab?.invoke(0) },
                exportData = callers.joinToString("\n") { "${it.displayName}: ${it.callCount} calls" }
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
            ReportCardHeader(
                title = "Contacts Breakdown",
                onDrillDown = { onTypeClick(ContactsFilter.ALL) },
                exportData = """
                    Saved: ${stats.savedContacts}
                    Unsaved: ${stats.unsavedContacts}
                """.trimIndent()
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
fun NotesActivityCard(
    stats: ReportStats,
    viewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigateToTab: ((Int) -> Unit)? = null
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportCardHeader(
                title = "Notes & Activity",
                onDrillDown = { onNavigateToTab?.invoke(0) },
                exportData = ""
            )
            Spacer(Modifier.height(16.dp))
            
            // Format: 100% (77/84)
            fun formatPercent(count: Int, total: Int): String {
                val p = if (total > 0) (count * 100 / total) else 0
                return "$p% ($count/$total)"
            }

            ClickableStatRow(
                label = "Reviewed Calls",
                value = formatPercent(stats.reviewedCalls, stats.totalCalls),
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50),
                onClick = {
                    viewModel.setReviewedFilter(ReviewedFilter.REVIEWED)
                    onNavigateToTab?.invoke(0)
                }
            )
            ClickableStatRow(
                label = "Person with Notes",
                value = formatPercent(stats.personsWithNotes, stats.totalPersons),
                icon = Icons.Default.PersonPin,
                color = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    viewModel.setPersonNotesFilter(PersonNotesFilter.WITH_NOTE)
                    onNavigateToTab?.invoke(1)
                }
            )
            ClickableStatRow(
                label = "Person with Labels",
                value = formatPercent(stats.personsWithLabels, stats.totalPersons),
                icon = Icons.AutoMirrored.Filled.Label,
                color = Color(0xFF9C27B0),
                onClick = {
                    onNavigateToTab?.invoke(1)
                }
            )
            ClickableStatRow(
                label = "Calls with Notes",
                value = formatPercent(stats.callsWithNotes, stats.totalCalls),
                icon = Icons.AutoMirrored.Filled.StickyNote2,
                color = MaterialTheme.colorScheme.secondary,
                onClick = {
                    viewModel.setNotesFilter(NotesFilter.WITH_NOTE)
                    onNavigateToTab?.invoke(0)
                }
            )
            ClickableStatRow(
                label = "Calls with Recordings",
                value = formatPercent(stats.callsWithRecordings, stats.totalCalls),
                icon = Icons.Default.Mic,
                color = Color(0xFFF44336),
                onClick = { onNavigateToTab?.invoke(0) }
            )
            
            if (stats.callsWithRecordings > 0) {
                 ClickableStatRow(
                    label = "Call With Recording",
                    value = formatPercent(stats.callsWithRecordings, stats.totalCalls),
                    icon = Icons.Default.GraphicEq, // Alternative icon
                    color = Color(0xFFE91E63),
                    onClick = { onNavigateToTab?.invoke(0) }
                )
            }
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
            ReportCardHeader(
                title = "Label Distribution",
                exportData = labels.joinToString("\n") { "${it.first}: ${it.second}" }
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
            ReportCardHeader(title = "Daily Activity")
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
            ReportCardHeader(title = "Hourly Activity")
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

// ========================= New Report Cards =========================

@Composable
fun ComparisonsCard(stats: ReportStats) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportCardHeader(title = "Comparisons")
            Text(
                text = "vs. past similar data",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            
            ComparisonItem(
                label = "Connected Rate",
                current = "${if (stats.totalCalls > 0) (stats.connectedCalls * 100 / stats.totalCalls) else 0}%",
                trend = "+5%", // Mock trend for visual demo
                isPositive = true
            )
            ComparisonItem(
                label = "Avg. Duration",
                current = formatDurationShort(stats.avgDuration),
                trend = "-12s", // Mock trend for visual demo
                isPositive = false
            )
            ComparisonItem(
                label = "Daily Avg Calls",
                current = if (stats.dailyStats.isNotEmpty()) (stats.totalCalls / stats.dailyStats.size).toString() else "0",
                trend = "+2", // Mock trend for visual demo
                isPositive = true
            )
        }
    }
}

@Composable
fun ComparisonItem(
    label: String,
    current: String,
    trend: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = current, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = trend,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun DailyAverageSummaryCard(stats: ReportStats) {
    val avgCalls = if (stats.dailyStats.isNotEmpty()) stats.totalCalls / stats.dailyStats.size else 0
    val avgDuration = if (stats.dailyStats.isNotEmpty()) stats.totalDuration / stats.dailyStats.size else 0
    
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ReportCardHeader(
                title = "Daily Averages",
                exportData = "Avg Calls/Day: $avgCalls\nAvg Time/Day: ${formatDurationShort(avgDuration)}"
            )
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = avgCalls.toString(),
                    label = "Calls/Day",
                    icon = Icons.Default.Call,
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    value = formatDurationShort(avgDuration),
                    label = "Time/Day",
                    icon = Icons.Default.Timer,
                    color = MaterialTheme.colorScheme.secondary
                )
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
