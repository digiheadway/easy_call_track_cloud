
package com.miniclick.calltrackmanage.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Date Range",
                tint = if (dateRange != DateRange.ALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
