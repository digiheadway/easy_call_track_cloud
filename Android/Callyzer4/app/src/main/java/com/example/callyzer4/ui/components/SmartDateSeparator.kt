package com.example.callyzer4.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.callyzer4.data.CallGroup
import com.example.callyzer4.ui.theme.AppColors
import com.example.callyzer4.ui.theme.AppRadius
import com.example.callyzer4.ui.theme.AppSpacing
import com.example.callyzer4.ui.theme.AppTypography
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SmartDateSeparator(
    callGroups: List<CallGroup>,
    modifier: Modifier = Modifier
) {
    if (callGroups.isNotEmpty()) {
        val latestCall = callGroups.firstOrNull()?.lastCallDate ?: Date()
        val dateText = formatSmartDate(latestCall)
        val totalCalls = callGroups.sumOf { it.totalCalls }
        val uniqueCallers = callGroups.size
        
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            // Horizontal line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Gray200)
            )
            
            // Date text with stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = AppSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date
                Text(
                    text = dateText,
                    fontSize = AppTypography.sm,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.Gray600
                )
                
                // Stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    // Call count
                    Surface(
                        shape = RoundedCornerShape(size = AppRadius.sm),
                        color = AppColors.Blue100
                    ) {
                        Text(
                            text = "$totalCalls calls",
                            fontSize = AppTypography.xs,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.Blue600,
                            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                        )
                    }
                    
                    // Caller count
                    Surface(
                        shape = RoundedCornerShape(size = AppRadius.sm),
                        color = AppColors.Gray100
                    ) {
                        Text(
                            text = "$uniqueCallers callers",
                            fontSize = AppTypography.xs,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.Gray600,
                            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                        )
                    }
                }
            }
        }
    }
}

private fun formatSmartDate(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    
    calendar.time = date
    today.time = now
    yesterday.time = now
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    
    return when {
        // Today
        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
            "Today (${SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)})"
        }
        // Yesterday
        calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) &&
        calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) -> {
            "Yesterday (${SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)})"
        }
        // This week
        diff < 7 * 24 * 60 * 60 * 1000 -> {
            SimpleDateFormat("EEEE (dd/MM)", Locale.getDefault()).format(date)
        }
        // This year
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM dd (dd/MM)", Locale.getDefault()).format(date)
        }
        // Other years
        else -> {
            SimpleDateFormat("MMM dd, yyyy (dd/MM/yyyy)", Locale.getDefault()).format(date)
        }
    }
}