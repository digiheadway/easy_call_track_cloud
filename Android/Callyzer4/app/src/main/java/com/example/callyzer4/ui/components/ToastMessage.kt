package com.example.callyzer4.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.callyzer4.ui.theme.AppColors
import com.example.callyzer4.ui.theme.AppRadius
import com.example.callyzer4.ui.theme.AppSpacing
import com.example.callyzer4.ui.theme.AppTypography
import kotlinx.coroutines.delay

@Composable
fun NonBlockingToast(
    message: String,
    isSuccess: Boolean = true,
    onDismiss: () -> Unit
) {
    val backgroundColor = if (isSuccess) AppColors.Green500 else AppColors.Red500
    val textColor = Color.White
    val iconColor = Color.White
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm)
            .clickable { onDismiss() },
        shape = RoundedCornerShape(AppRadius.md),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )
            
            Spacer(modifier = Modifier.width(AppSpacing.sm))
            
            Text(
                text = message,
                fontSize = AppTypography.sm,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp),
                    tint = iconColor
                )
            }
        }
    }
}

@Composable
fun ToastMessage(
    message: String,
    isSuccess: Boolean = true,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(AppRadius.lg),
            colors = CardDefaults.cardColors(
                containerColor = if (isSuccess) AppColors.Green500.copy(alpha = 0.1f) else AppColors.Red500.copy(alpha = 0.1f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSuccess) AppColors.Green500 else AppColors.Red500
                )
                
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                
                Text(
                    text = message,
                    fontSize = AppTypography.sm,
                    fontWeight = FontWeight.Medium,
                    color = if (isSuccess) AppColors.Green500 else AppColors.Red500
                )
            }
        }
    }
}

@Composable
fun AutoDismissToast(
    message: String,
    isSuccess: Boolean = true,
    duration: Long = 2000L,
    onDismiss: () -> Unit
) {
    LaunchedEffect(message) {
        delay(duration)
        onDismiss()
    }
    
    ToastMessage(
        message = message,
        isSuccess = isSuccess,
        onDismiss = onDismiss
    )
}
