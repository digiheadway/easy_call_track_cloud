package com.miniclick.calltrackmanage.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.miniclick.calltrackmanage.data.db.CallDataEntity
import com.miniclick.calltrackmanage.data.db.RecordingSyncStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncQueueModal(
    pendingNewCalls: Int,
    pendingRelatedData: Int,
    pendingRecordings: Int,
    onSyncAll: () -> Unit,
    onDismiss: () -> Unit,
    onRecordingClick: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Sync, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sync Queue Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            SyncQueueItem(
                label = "New Calls",
                count = pendingNewCalls,
                icon = Icons.Default.Phone,
                status = if (pendingNewCalls > 0) "Pending upload" else "All synced"
            )

            SyncQueueItem(
                label = "Related Data",
                count = pendingRelatedData,
                icon = Icons.Default.Description,
                status = if (pendingRelatedData > 0) "Notes, labels & status updates" else "All updated"
            )

            SyncQueueItem(
                label = "Call Recordings",
                count = pendingRecordings,
                icon = Icons.Default.Mic,
                status = if (pendingRecordings > 0) "Tap to view queue" else "All uploaded",
                onClick = if (pendingRecordings > 0) onRecordingClick else null
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    onSyncAll()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Sync, null)
                Spacer(Modifier.width(8.dp))
                Text("Sync All Now")
            }
        }
    }
}

@Composable
fun SyncQueueItem(
    label: String,
    count: Int,
    icon: ImageVector,
    status: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        if (onClick != null && count > 0) {
            Icon(
                Icons.Default.ChevronRight, 
                contentDescription = "View details",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
        }
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (count > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingQueueModal(
    activeRecordings: List<CallDataEntity>,
    onDismiss: () -> Unit,
    onRetry: (String) -> Unit
) {
    var selectedError by remember { mutableStateOf<String?>(null) }

    val currentError = selectedError
    if (currentError != null) {
        AlertDialog(
            onDismissRequest = { selectedError = null },
            title = { Text("Upload Failure Reason") },
            text = { Text(currentError) },
            confirmButton = {
                TextButton(onClick = { selectedError = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Calculate total size of pending recordings
    val totalSizeBytes = remember(activeRecordings) {
        activeRecordings.sumOf { recording ->
            recording.localRecordingPath?.let { path ->
                try {
                    File(path).length()
                } catch (e: Exception) {
                    0L
                }
            } ?: 0L
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp, top = 8.dp)
        ) {
            // Header with count and size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.Upload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recording Upload Queue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (activeRecordings.isNotEmpty()) {
                        val sizeText = formatFileSize(totalSizeBytes)
                        Text(
                            "${activeRecordings.size} recording${if (activeRecordings.size > 1) "s" else ""} • $sizeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (activeRecordings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No recordings in queue", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "All recordings have been uploaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(activeRecordings, key = { it.compositeId }) { recording ->
                        RecordingQueueItem(
                            recording = recording,
                            onClick = {
                                if (recording.recordingSyncStatus == RecordingSyncStatus.FAILED) {
                                    selectedError = recording.syncError ?: "The recording file might be missing or corrupted, or there was a network error during upload."
                                }
                            },
                            onRetry = { onRetry(recording.compositeId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingQueueItem(
    recording: CallDataEntity,
    onClick: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    
    // Calculate file size
    val fileSize = remember(recording.localRecordingPath) {
        recording.localRecordingPath?.let { path ->
            try {
                File(path).length()
            } catch (e: Exception) {
                0L
            }
        } ?: 0L
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = recording.recordingSyncStatus == RecordingSyncStatus.FAILED, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recording.contactName ?: recording.phoneNumber,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dateFormat.format(Date(recording.callDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (fileSize > 0) {
                    Text(
                        text = " • ${formatFileSize(fileSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Show waiting reason for PENDING status
            if (recording.recordingSyncStatus == RecordingSyncStatus.PENDING) {
                val waitingReason = getWaitingReason(recording)
                if (waitingReason != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = waitingReason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            val (statusText, statusColor) = when (recording.recordingSyncStatus) {
                RecordingSyncStatus.COMPRESSING -> "Preparing" to MaterialTheme.colorScheme.secondary
                RecordingSyncStatus.UPLOADING -> "Uploading" to MaterialTheme.colorScheme.primary
                RecordingSyncStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
                else -> "Waiting" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            if (recording.recordingSyncStatus == RecordingSyncStatus.COMPRESSING || 
                recording.recordingSyncStatus == RecordingSyncStatus.UPLOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = statusColor
                )
                Spacer(Modifier.width(8.dp))
            }
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
            
            if (recording.recordingSyncStatus == RecordingSyncStatus.FAILED) {
                Spacer(Modifier.width(8.dp))
                
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Retry", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Info,
                    contentDescription = "View Error",
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Format file size in human readable format
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * Get a human-readable reason why the recording is waiting
 */
private fun getWaitingReason(recording: CallDataEntity): String? {
    return when {
        recording.localRecordingPath.isNullOrEmpty() -> "Waiting for recording file..."
        else -> "In queue, waiting for upload slot"
    }
}
