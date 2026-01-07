
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
import androidx.compose.ui.graphics.vector.ImageVector

data class FilterOption(
    val label: String,
    val icon: ImageVector,
    val isSelected: Boolean,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePickerDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val datePickerState = rememberDateRangePickerState()
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (datePickerState.selectedStartDateMillis != null && datePickerState.selectedEndDateMillis != null) {
                        onDateRangeSelected(datePickerState.selectedStartDateMillis!!, datePickerState.selectedEndDateMillis!!)
                    }
                },
                enabled = datePickerState.selectedStartDateMillis != null && datePickerState.selectedEndDateMillis != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(state = datePickerState)
    }
}

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



    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = headerIcon,
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
