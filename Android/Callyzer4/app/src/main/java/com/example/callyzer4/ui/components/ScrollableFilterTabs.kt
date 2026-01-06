package com.example.callyzer4.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.callyzer4.ui.theme.AppColors
import com.example.callyzer4.ui.theme.AppRadius
import com.example.callyzer4.ui.theme.AppSpacing
import com.example.callyzer4.ui.theme.AppTypography

@Composable
fun ScrollableFilterTabs(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("All", "Connected", "Missed", "Rejected", "Outgoing Failed")
    val scrollState = rememberScrollState()
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppColors.Gray50,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            tabs.forEach { tab ->
                ModernFilterTab(
                    text = tab,
                    isSelected = selectedTab == tab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun ModernFilterTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.Blue600 else AppColors.Gray600,
        animationSpec = tween(200),
        label = "tabColor"
    )
    
    val animatedBackground by animateColorAsState(
        targetValue = if (isSelected) AppColors.Blue100 else Color.Transparent,
        animationSpec = tween(200),
        label = "tabBackground"
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(size = AppRadius.md))
            .background(animatedBackground)
            .clickable { onClick() }
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                fontSize = AppTypography.sm,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = animatedColor
            )
            
            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically(
                    animationSpec = tween(200),
                    expandFrom = Alignment.Top
                ) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(
                    animationSpec = tween(200),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(AppColors.Blue600, RoundedCornerShape(size = 1.dp))
                )
            }
        }
    }
}
