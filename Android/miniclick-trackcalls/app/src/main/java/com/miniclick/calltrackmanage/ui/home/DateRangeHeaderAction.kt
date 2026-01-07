
package com.miniclick.calltrackmanage.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun DateRangeHeaderAction(
    dateRange: DateRange,
    onDateRangeChange: (DateRange, Long?, Long?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { start, end ->
                onDateRangeChange(DateRange.CUSTOM, start, end)
                showDatePicker = false
            }
        )
    }

    val headerIcon = when (dateRange) {
        DateRange.TODAY -> Icons.Default.Today
        DateRange.LAST_3_DAYS -> Icons.Default.Event
        DateRange.LAST_7_DAYS -> Icons.Default.Event
        DateRange.LAST_14_DAYS -> Icons.Default.Event
        DateRange.LAST_30_DAYS -> Icons.Default.Event
        DateRange.THIS_MONTH -> Icons.Default.CalendarMonth
        DateRange.PREVIOUS_MONTH -> Icons.Default.CalendarMonth
        DateRange.CUSTOM -> Icons.Default.DateRange
        DateRange.ALL -> Icons.Default.AllInclusive
    }

    val headerLabel = when (dateRange) {
        DateRange.TODAY -> "Today"
        DateRange.LAST_3_DAYS -> "Last 3 Days"
        DateRange.LAST_7_DAYS -> "Last 7 Days"
        DateRange.LAST_14_DAYS -> "Last 14 Days"
        DateRange.LAST_30_DAYS -> "Last 30 Days"
        DateRange.THIS_MONTH -> "This Month"
        DateRange.PREVIOUS_MONTH -> "Prev Month"
        DateRange.CUSTOM -> "Custom"
        DateRange.ALL -> "All Time"
    }

    Box {
        Surface(
            onClick = { showMenu = true },
            shape = RoundedCornerShape(12.dp),
            color = if (dateRange != DateRange.ALL) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = headerIcon,
                    contentDescription = "Date Range",
                    modifier = Modifier.size(18.dp),
                    tint = if (dateRange != DateRange.ALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = headerLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (dateRange != DateRange.ALL) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = if (dateRange != DateRange.ALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            val options = listOf(
                FilterOption("Today", Icons.Default.Today, dateRange == DateRange.TODAY) { onDateRangeChange(DateRange.TODAY, null, null) },
                FilterOption("Last 3 Days", Icons.Default.Event, dateRange == DateRange.LAST_3_DAYS) { onDateRangeChange(DateRange.LAST_3_DAYS, null, null) },
                FilterOption("Last 7 Days", Icons.Default.Event, dateRange == DateRange.LAST_7_DAYS) { onDateRangeChange(DateRange.LAST_7_DAYS, null, null) },
                FilterOption("Last 14 Days", Icons.Default.Event, dateRange == DateRange.LAST_14_DAYS) { onDateRangeChange(DateRange.LAST_14_DAYS, null, null) },
                FilterOption("Last 30 Days", Icons.Default.Event, dateRange == DateRange.LAST_30_DAYS) { onDateRangeChange(DateRange.LAST_30_DAYS, null, null) },
                FilterOption("This Month", Icons.Default.CalendarMonth, dateRange == DateRange.THIS_MONTH) { onDateRangeChange(DateRange.THIS_MONTH, null, null) },
                FilterOption("Previous Month", Icons.Default.CalendarMonth, dateRange == DateRange.PREVIOUS_MONTH) { onDateRangeChange(DateRange.PREVIOUS_MONTH, null, null) },
                FilterOption("Custom", Icons.Default.DateRange, dateRange == DateRange.CUSTOM) { 
                    showDatePicker = true
                },
                FilterOption("All Time", Icons.Default.AllInclusive, dateRange == DateRange.ALL) { onDateRangeChange(DateRange.ALL, null, null) }
            )

            options.forEach { option ->
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (option.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option.label)
                        }
                    },
                    onClick = {
                        option.onClick()
                        showMenu = false
                    },
                    trailingIcon = if (option.isSelected) {
                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        }
    }
}
