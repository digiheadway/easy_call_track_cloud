package com.example.callyzer4.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.callyzer4.data.CallGroup
import com.example.callyzer4.data.CallHistoryItem
import com.example.callyzer4.data.CallType
import com.example.callyzer4.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernCallGroupCard(
    group: CallGroup,
    isExpanded: Boolean,
    note: String,
    onToggleExpanded: (String) -> Unit,
    onNoteChanged: (String, String) -> Unit,
    onCallBack: (String) -> Unit,
    onMessage: (String) -> Unit,
    onCopy: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lastCall = group.calls.firstOrNull()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = RoundedCornerShape(18.dp)
                clip = true
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F7FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main caller detail row
            ModernCallerDetailRow(
                group = group,
                note = note,
                isExpanded = isExpanded,
                onToggleExpanded = { onToggleExpanded(group.phoneNumber) }
            )
            
            // Animated expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Fixed height call history container
                    ModernCallHistoryContainer(group = group)
                    
                    // Note input and action buttons
                    ModernExpandedActions(
                        phoneNumber = group.phoneNumber,
                        note = note,
                        onNoteChanged = onNoteChanged,
                        onCallBack = onCallBack,
                        onMessage = onMessage,
                        onCopy = onCopy,
                        onWhatsApp = onWhatsApp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernCallerDetailRow(
    group: CallGroup,
    note: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val lastCall = group.calls.firstOrNull()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() }
            .padding(AppSpacing.xl)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact initials/photo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF90CAF9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.contactName?.take(2)?.uppercase() ?: group.phoneNumber.take(2),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
            // Colorful call type icon
            if (lastCall != null) {
                ModernCallTypeIcon(
                    callType = lastCall.callType,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(28.dp))
            }
            // Contact Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppSpacing.md),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = group.contactName ?: group.phoneNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Gray900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Expand Icon
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(300),
                label = "expandRotation"
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = AppColors.Gray600
            )
        }
        // Show trimmed note if exists
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AppSpacing.sm))
            Text(
                text = note,
                fontSize = AppTypography.xs,
                color = AppColors.Gray600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(start = AppSpacing.sm)
            )
        }
    }
}

@Composable
private fun ModernCallHistoryContainer(
    group: CallGroup
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        color = AppColors.Gray50,
        shape = RoundedCornerShape(AppRadius.md)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            items(group.calls) { call ->
                ModernCallHistoryItem(call = call)
            }
        }
    }
}

@Composable
private fun ModernCallHistoryItem(
    call: CallHistoryItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        ModernCallTypeIcon(
            callType = call.callType,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        
        // Type
        Text(
            text = when (call.callType) {
                CallType.INCOMING -> "Incoming"
                CallType.OUTGOING -> "Outgoing"
                CallType.MISSED -> "Missed"
            },
            fontSize = AppTypography.sm,
            fontWeight = FontWeight.Medium,
            color = AppColors.Gray900,
            modifier = Modifier.weight(1f)
        )
        
        // Duration
        if (call.duration > 0) {
            Text(
                text = formatDuration(call.duration),
                fontSize = AppTypography.xs,
                color = AppColors.Gray600,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.width(AppSpacing.sm))
        
        // Time & Date
        Text(
            text = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(call.timestamp),
            fontSize = AppTypography.xs,
            color = AppColors.Gray500
        )
    }
}

@Composable
private fun ModernExpandedActions(
    phoneNumber: String,
    note: String,
    onNoteChanged: (String, String) -> Unit,
    onCallBack: (String) -> Unit,
    onMessage: (String) -> Unit,
    onCopy: (String) -> Unit,
    onWhatsApp: (String) -> Unit
) {
    var noteText by remember { mutableStateOf(note) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.lg)
    ) {
        // Note input box
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            placeholder = {
                Text(
                    text = "Add a note for this contact...",
                    color = AppColors.Gray500,
                    fontSize = AppTypography.sm
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Blue600,
                unfocusedBorderColor = AppColors.Gray300,
                focusedTextColor = AppColors.Gray900,
                unfocusedTextColor = AppColors.Gray900,
                focusedPlaceholderColor = AppColors.Gray500,
                unfocusedPlaceholderColor = AppColors.Gray500
            ),
            shape = RoundedCornerShape(AppRadius.md),
            maxLines = 3
        )
        
        Spacer(modifier = Modifier.height(AppSpacing.lg))
        
        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save button - 30% screen width
            Button(
                onClick = { onNoteChanged(phoneNumber, noteText) },
                modifier = Modifier.fillMaxWidth(0.3f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Green500,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(AppRadius.md),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(
                    text = "Save",
                    fontSize = AppTypography.sm,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(AppSpacing.md))
            
            // Action icons - Call, Copy, WhatsApp
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                // Call button
                IconButton(
                    onClick = { onCallBack(phoneNumber) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.Green500)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
                
                // Copy button
                IconButton(
                    onClick = { onCopy(phoneNumber) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AppColors.Blue500)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
                
                // WhatsApp button
                IconButton(
                    onClick = { onWhatsApp(phoneNumber) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366))
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernCallTypeIcon(
    callType: CallType,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (callType) {
        CallType.INCOMING -> Icons.Default.ArrowDownward to AppColors.Green500
        CallType.OUTGOING -> Icons.Default.ArrowUpward to AppColors.Blue500
        CallType.MISSED -> Icons.Default.PhoneMissed to AppColors.Red500
    }
    
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(300),
        label = "iconColor"
    )
    
    Icon(
        imageVector = icon,
        contentDescription = callType.name,
        modifier = modifier,
        tint = animatedColor
    )
}

private fun formatCallTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }
}

private fun formatDetailedCallTime(date: Date): String {
    val now = Date()
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    
    calendar.time = date
    today.time = now
    yesterday.time = now
    yesterday.add(Calendar.DAY_OF_YEAR, -1)
    
    return when {
        // Today - show time only
        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        }
        // Yesterday
        calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) &&
        calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) -> {
            "Yesterday ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)}"
        }
        // This year
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(date)
        }
        // Other years
        else -> {
            SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault()).format(date)
        }
    }
}


private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
