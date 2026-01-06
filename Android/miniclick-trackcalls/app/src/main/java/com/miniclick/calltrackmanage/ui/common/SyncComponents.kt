package com.miniclick.calltrackmanage.ui.common

import androidx.compose.animation.*
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
import com.miniclick.calltrackmanage.ui.settings.SettingsUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncQueueModal(
    pendingNewCalls: Int,
    pendingRelatedData: Int,
    pendingRecordings: Int,
    isSyncSetup: Boolean = true,
    isNetworkAvailable: Boolean = true,
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
            // Header - Different based on sync setup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    if (isSyncSetup) Icons.Default.CloudSync else Icons.Default.PhoneAndroid,
                    null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (isSyncSetup) "Cloud Backup Status" else "Local Data Status", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold
                    )
                    if (!isSyncSetup) {
                        Text(
                            "All data is saved locally on your device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Network status banner (only if sync is setup and offline)
            if (isSyncSetup && !isNetworkAvailable) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "You're offline",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Data is saved locally. Will backup when online.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            val totalPending = pendingNewCalls + pendingRelatedData + pendingRecordings

            if (isSyncSetup) {
                // Cloud sync mode: Show what needs to be backed up
                SyncQueueItem(
                    label = "New Calls",
                    count = pendingNewCalls,
                    icon = Icons.Default.Phone,
                    status = if (pendingNewCalls > 0) "Ready for cloud backup" else "All backed up ✓"
                )

                SyncQueueItem(
                    label = "Notes & Labels",
                    count = pendingRelatedData,
                    icon = Icons.Default.Edit,
                    status = if (pendingRelatedData > 0) "Changes pending backup" else "All changes backed up ✓"
                )

                SyncQueueItem(
                    label = "Call Recordings",
                    count = pendingRecordings,
                    icon = Icons.Default.Mic,
                    status = if (pendingRecordings > 0) "Tap to view upload queue" else "All recordings uploaded ✓",
                    onClick = if (pendingRecordings > 0) onRecordingClick else null
                )

                Spacer(Modifier.height(8.dp))

                // Reassuring message
                if (totalPending > 0) {
                    Text(
                        "✓ Your data is saved locally and will backup to cloud automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        onSyncAll()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isNetworkAvailable && totalPending > 0
                ) {
                    Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (totalPending > 0) "Backup Now" else "All Backed Up")
                }
            } else {
                // Offline/local mode: Show local data status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "All Data Saved Locally",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Your call logs, notes, and recordings are stored on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "To enable cloud backup, go to Settings → Organisation Setup",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
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
                val sortedRecordings = remember(activeRecordings) {
                    activeRecordings.sortedByDescending { it.callDate }
                }
                val displayList = remember(sortedRecordings) { sortedRecordings.take(100) }
                val remaining = sortedRecordings.size - displayList.size

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(displayList, key = { it.compositeId }) { recording ->
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

                    if (remaining > 0) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$remaining remaining recordings...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
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
                RecordingSyncStatus.NOT_FOUND -> "Not Found" to MaterialTheme.colorScheme.error
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

@Composable
fun GlobalSyncStatusBar(
    modifier: Modifier = Modifier,
    pendingNewCalls: Int,
    pendingMetadata: Int,
    pendingPersonUpdates: Int,
    pendingRecordings: Int,
    activeUploads: Int,
    isNetworkAvailable: Boolean,
    isSyncSetup: Boolean = true,
    onSyncNow: () -> Unit,
    onShowQueue: () -> Unit
) {
    val activeProcess by com.miniclick.calltrackmanage.data.ProcessMonitor.activeProcess.collectAsState()
    
    val totalPending = pendingNewCalls + pendingMetadata + pendingPersonUpdates
    
    // Only show bar if:
    // 1. There's an active local process (importing, finding recordings)
    // 2. Sync is setup AND there are pending items or network issues
    val showBar = activeProcess != null || 
                  (isSyncSetup && (totalPending > 0 || pendingRecordings > 0 || activeUploads > 0 || !isNetworkAvailable))

    android.util.Log.d("GlobalSyncStatusBar", "showBar: $showBar, activeProcess: ${activeProcess?.title}, totalPending: $totalPending, pendingRecordings: $pendingRecordings, activeUploads: $activeUploads, isNetworkAvailable: $isNetworkAvailable, isSyncSetup: $isSyncSetup")

    AnimatedVisibility(
        visible = showBar,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val process = activeProcess
        val isLocalProcess = process != null
        
        // Color scheme:
        // - Local process (import/find) = neutral surface color
        // - Cloud backup pending = primary container
        // - No network = error container (only if sync setup)
        val backgroundColor = when {
            isLocalProcess -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            !isNetworkAvailable && isSyncSetup -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        }
        
        val contentColor = when {
            isLocalProcess -> MaterialTheme.colorScheme.onSurfaceVariant
            !isNetworkAvailable && isSyncSetup -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onShowQueue),
            color = backgroundColor,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Determine what to show - prioritize local processes, then network, then cloud backup
                val text = when {
                    // Local processes (no network needed)
                    process != null -> {
                        val progressPct = if (process.isIndeterminate) "" else " ${(process.progress * 100).toInt()}%"
                        val detailText = if (!process.details.isNullOrEmpty()) ": ${process.details}" else progressPct
                        "${process.title}$detailText"
                    }
                    // Network issue (only matters if sync is setup)
                    !isNetworkAvailable && isSyncSetup -> {
                        val pendingTotal = totalPending + pendingRecordings
                        if (pendingTotal > 0) "Offline • $pendingTotal saved locally" else "Offline"
                    }
                    // Active uploads
                    activeUploads > 0 -> "Uploading $activeUploads recording${if (activeUploads > 1) "s" else ""}..."
                    // Pending cloud backup
                    totalPending > 0 -> "Backing up $totalPending item${if (totalPending > 1) "s" else ""}..."
                    // Recordings in queue
                    pendingRecordings > 0 -> "$pendingRecordings recording${if (pendingRecordings > 1) "s" else ""} pending upload"
                    // Fallback (shouldn't happen)
                    else -> "Processing..."
                }

                val icon = when {
                    process != null -> Icons.Default.Sync  // Local processing
                    !isNetworkAvailable && isSyncSetup -> Icons.Default.CloudOff
                    activeUploads > 0 -> Icons.Default.CloudUpload
                    totalPending > 0 -> Icons.Default.CloudSync
                    pendingRecordings > 0 -> Icons.Default.Mic
                    else -> Icons.Default.Sync
                }

                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(14.dp), 
                    tint = contentColor
                )
                
                Spacer(Modifier.width(8.dp))
                
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    fontWeight = FontWeight.Medium
                )
                
                // Show progress indicator only when actively doing something
                if (process != null || (isNetworkAvailable && (activeUploads > 0 || totalPending > 0))) {
                    Spacer(Modifier.width(12.dp))
                    
                    if (process != null && !process.isIndeterminate) {
                         LinearProgressIndicator(
                            progress = { process.progress },
                            modifier = Modifier
                                .width(60.dp)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = contentColor,
                            trackColor = contentColor.copy(alpha = 0.2f),
                        )
                    } else if (process != null || activeUploads > 0 || totalPending > 0) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(48.dp)
                                .height(2.dp)
                                .clip(CircleShape),
                            color = contentColor,
                            trackColor = contentColor.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}
