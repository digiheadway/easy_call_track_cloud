package com.example.callyzer4.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
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
fun DateChangeSeparator(
    currentGroup: CallGroup,
    previousGroup: CallGroup?,
    modifier: Modifier = Modifier
) {
    val shouldShowSeparator = shouldShowDateSeparator(currentGroup, previousGroup)
    
    if (shouldShowSeparator) {
        val dateText = formatDateForSeparator(currentGroup.lastCallDate)
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Gray200)
            )
            // Date pill with icon
            Row(
                modifier = Modifier
                    .background(AppColors.Gray100, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 7.dp)
                    .align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Date",
                    tint = AppColors.Gray700,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateText,
                    fontSize = AppTypography.base,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Gray700
                )
            }
        }
    }
}

private fun shouldShowDateSeparator(currentGroup: CallGroup, previousGroup: CallGroup?): Boolean {
    if (previousGroup == null) return true
    
    val currentDate = getDateOnly(currentGroup.lastCallDate)
    val previousDate = getDateOnly(previousGroup.lastCallDate)
    
    return currentDate != previousDate
}

private fun getDateOnly(date: Date): String {
    val calendar = Calendar.getInstance()
    calendar.time = date
    return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)}-${calendar.get(Calendar.DAY_OF_MONTH)}"
}

private fun formatDateForSeparator(date: Date): String {
    val now = Date()
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
