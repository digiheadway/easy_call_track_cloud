package com.example.salescrm.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.salescrm.data.*
import com.example.salescrm.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime

// ==================== PERSON DATA TABLE (REAL TABLE VIEW) ====================

private fun getColumnWidth(columnId: String): androidx.compose.ui.unit.Dp = when(columnId) {
    "name" -> 160.dp
    "phone" -> 130.dp
    "email" -> 150.dp
    "stage" -> 120.dp
    "segment" -> 120.dp
    "budget" -> 100.dp
    "priority" -> 100.dp
    "labels" -> 150.dp
    "address" -> 200.dp
    "source" -> 120.dp
    "note" -> 250.dp
    "created", "modified", "lastActivity" -> 120.dp
    else -> 100.dp
}

@Composable
fun PersonDataTable(
    people: List<Person>,
    visibleColumns: Set<String>,
    onPersonClick: (Person) -> Unit,
    currencySymbol: String,
    budgetMultiplier: Int,
    modifier: Modifier = Modifier
) {
    // Determine active columns based on visibility and default config order
    val allConfigs = (defaultPipelineColumns + defaultContactColumns).distinctBy { it.id }
    val activeColumns = allConfigs.filter { it.id in visibleColumns }
    
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .background(SalesCrmTheme.colors.background)
    ) {
        // HEADER ROW
        Column {
            Row(
                modifier = Modifier
                    .background(SalesCrmTheme.colors.surfaceVariant)
            ) {
                activeColumns.forEach { column ->
                    Box(
                        modifier = Modifier
                            .width(getColumnWidth(column.id))
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = column.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SalesCrmTheme.colors.textSecondary
                        )
                    }
                }
            }
            HorizontalDivider(color = SalesCrmTheme.colors.border, thickness = 1.dp)
        }
        
        // DATA ROWS
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(people, key = { it.id }) { person ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SalesCrmTheme.colors.surface)
                            .clickable { onPersonClick(person) }
                    ) {
                        activeColumns.forEach { column ->
                            PersonDataCell(
                                person = person,
                                columnId = column.id,
                                width = getColumnWidth(column.id),
                                currencySymbol = currencySymbol,
                                budgetMultiplier = budgetMultiplier
                            )
                        }
                    }
                    HorizontalDivider(color = SalesCrmTheme.colors.border, thickness = 0.5.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonDataCell(
    person: Person,
    columnId: String,
    width: androidx.compose.ui.unit.Dp,
    currencySymbol: String,
    budgetMultiplier: Int
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        when (columnId) {
            "name" -> {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = SalesCrmTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            "budget" -> {
                 if (person.isInPipeline && person.budget.isNotBlank()) {
                    Text(
                        text = formatBudget(person.budget, currencySymbol, budgetMultiplier),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                 } else {
                     Text("-", color = SalesCrmTheme.colors.textMuted)
                 }
            }
            "stage" -> {
                if (person.isInPipeline) {
                    val customStages = SalesCrmTheme.stages
                    val stageItem = customStages.findById(person.stageId)
                    val stageColor = Color(stageItem?.color ?: 0xFF6B7280)
                    Surface(
                        color = stageColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stageItem?.label ?: "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = stageColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                     Text("-", color = SalesCrmTheme.colors.textMuted)
                }
            }
            "priority" -> {
                if (person.isInPipeline) {
                    val customPriorities = SalesCrmTheme.priorities
                    val priorityItem = customPriorities.findById(person.pipelinePriorityId)
                    val priorityColor = Color(priorityItem?.color ?: 0xFF6B7280)
                    Text(
                        text = priorityItem?.label ?: "None",
                        style = MaterialTheme.typography.bodyMedium,
                        color = priorityColor
                    )
                } else {
                    Text("-", color = SalesCrmTheme.colors.textMuted)
                }
            }
            "segment" -> {
                val customSegments = SalesCrmTheme.segments
                val segmentItem = customSegments.findById(person.segmentId)
                val segmentColor = Color(segmentItem?.color ?: 0xFF6B7280)
                Surface(
                    color = segmentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = segmentItem?.label ?: "Unknown",
                        style = MaterialTheme.typography.bodySmall,
                        color = segmentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            "labels" -> {
                if (person.labels.isNotEmpty()) {
                    Text(
                        text = person.labels.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = SalesCrmTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text("-", color = SalesCrmTheme.colors.textMuted)
                }
            }
            "phone" -> {
                Text(
                    text = person.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = SalesCrmTheme.colors.textSecondary
                )
            }
            "source" -> {
                val customSources = SalesCrmTheme.sources
                val sourceItem = customSources.findById(person.sourceId)
                Text(
                    text = sourceItem?.label ?: "Other",
                    style = MaterialTheme.typography.bodySmall,
                    color = SalesCrmTheme.colors.textSecondary
                )
            }
            "created" -> {
                val context = LocalContext.current
                val haptic = LocalHapticFeedback.current
                Text(
                    text = person.createdAt.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = SalesCrmTheme.colors.textSecondary,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            Toast.makeText(context, person.createdAt.toFullDateTimeString(), Toast.LENGTH_SHORT).show()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, person.createdAt.toFullDateTimeString(), Toast.LENGTH_SHORT).show()
                        }
                    )
                )
            }
            "modified" -> {
                val context = LocalContext.current
                val haptic = LocalHapticFeedback.current
                Text(
                    text = person.updatedAt.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = SalesCrmTheme.colors.textSecondary,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            Toast.makeText(context, person.updatedAt.toFullDateTimeString(), Toast.LENGTH_SHORT).show()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, person.updatedAt.toFullDateTimeString(), Toast.LENGTH_SHORT).show()
                        }
                    )
                )
            }
            "lastActivity" -> {
                val context = LocalContext.current
                val haptic = LocalHapticFeedback.current
                val date = person.lastActivityAt ?: person.updatedAt
                Text(
                    text = date.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = SalesCrmTheme.colors.textSecondary,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            Toast.makeText(context, date.toFullDateTimeString(), Toast.LENGTH_SHORT).show()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, date.toFullDateTimeString(), Toast.LENGTH_SHORT).show()
                        }
                    )
                )
            }
            else -> {
                Text(
                     text = "-", 
                     style = MaterialTheme.typography.bodySmall,
                     color = SalesCrmTheme.colors.textMuted
                )
            }
        }
    }
}
