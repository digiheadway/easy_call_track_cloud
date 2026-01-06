package com.example.callyzer4.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatisticsCard(
    statistics: Map<String, Any>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Call Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(getStatItems(statistics)) { item ->
                    StatItem(
                        icon = item.icon,
                        label = item.label,
                        value = item.value,
                        color = item.color
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class StatItemData(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val color: Color
)

private fun getStatItems(statistics: Map<String, Any>): List<StatItemData> {
    return listOf(
        StatItemData(
            icon = Icons.Default.Phone,
            label = "Total Calls",
            value = statistics["totalCalls"]?.toString() ?: "0",
            color = Color.Blue
        ),
        StatItemData(
            icon = Icons.Default.ArrowDownward,
            label = "Incoming",
            value = statistics["incomingCalls"]?.toString() ?: "0",
            color = Color.Green
        ),
        StatItemData(
            icon = Icons.Default.ArrowUpward,
            label = "Outgoing",
            value = statistics["outgoingCalls"]?.toString() ?: "0",
            color = Color.Blue
        ),
        StatItemData(
            icon = Icons.Default.PhoneMissed,
            label = "Missed",
            value = statistics["missedCalls"]?.toString() ?: "0",
            color = Color.Red
        ),
        StatItemData(
            icon = Icons.Default.People,
            label = "Contacts",
            value = statistics["uniqueContacts"]?.toString() ?: "0",
            color = Color(0xFF9C27B0) // Purple color
        )
    )
}
