package com.example.callyzer4.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.callyzer4.data.CallGroup
import com.example.callyzer4.data.CallHistoryItem
import com.example.callyzer4.data.CallType
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallSyncGroupCard(
    group: CallGroup,
    isExpanded: Boolean,
    note: String,
    onToggleExpanded: (String) -> Unit,
    onNoteChanged: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Main call info row
            CallInfoRow(
                group = group,
                isExpanded = isExpanded,
                onToggleExpanded = { onToggleExpanded(group.phoneNumber) }
            )
            
            // Expanded content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Individual call details
                group.calls.take(3).forEach { call ->
                    CallDetailItem(call = call)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Note section
                NoteSection(
                    phoneNumber = group.phoneNumber,
                    note = note,
                    onNoteChanged = onNoteChanged
                )
            }
        }
    }
}

@Composable
private fun CallInfoRow(
    group: CallGroup,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Call type icon
            CallTypeIcon(
                callType = group.calls.firstOrNull()?.callType ?: CallType.INCOMING,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Contact info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = group.contactName ?: group.phoneNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (group.contactName != null) {
                    Text(
                        text = group.phoneNumber,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Time and duration
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastCall = group.calls.firstOrNull()
                    if (lastCall != null) {
                        CallTypeIcon(
                            callType = lastCall.callType,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatCallTime(lastCall.timestamp),
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                        
                        if (lastCall.duration > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatDuration(lastCall.duration),
                                fontSize = 12.sp,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                }
            }
        }
        
        // Call count and expand icon
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Call count badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF1976D2), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${group.totalCalls} call${if (group.totalCalls > 1) "s" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Expand/collapse icon
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun CallDetailItem(
    call: CallHistoryItem
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F9FA)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallTypeIcon(
                    callType = call.callType,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (call.callType) {
                        CallType.INCOMING -> "Incoming"
                        CallType.OUTGOING -> "Outgoing"
                        CallType.MISSED -> "Missed"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(call.timestamp),
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                
                if (call.duration > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = formatDuration(call.duration),
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "More options",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun NoteSection(
    phoneNumber: String,
    note: String,
    onNoteChanged: (String, String) -> Unit
) {
    var noteText by remember { mutableStateOf(note) }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Note header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Note for $phoneNumber",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF666666)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Note input
        OutlinedTextField(
            value = noteText,
            onValueChange = { 
                noteText = it
                onNoteChanged(phoneNumber, it)
            },
            placeholder = {
                Text(
                    text = "Add a persistent note for this contact...",
                    color = Color(0xFF999999)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1976D2),
                unfocusedBorderColor = Color(0xFFE0E0E0)
            ),
            shape = RoundedCornerShape(8.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { /* Save note */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Save Contact Note",
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
            
            OutlinedButton(
                onClick = { /* Undo */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Undone",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun CallTypeIcon(
    callType: CallType,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (callType) {
        CallType.INCOMING -> Icons.Default.ArrowDownward to Color.Green
        CallType.OUTGOING -> Icons.Default.ArrowUpward to Color.Blue
        CallType.MISSED -> Icons.Default.PhoneMissed to Color.Red
    }
    
    Icon(
        imageVector = icon,
        contentDescription = callType.name,
        modifier = modifier,
        tint = color
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

private fun formatDuration(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
