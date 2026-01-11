package com.miniclick.calltrackmanage.ui.common

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import com.miniclick.calltrackmanage.service.CallTrackInCallService
import com.miniclick.calltrackmanage.util.audio.AudioPlayer
import android.telecom.Call
import kotlinx.coroutines.delay

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
    // Collect all running processes
    val allProcesses by com.miniclick.calltrackmanage.data.ProcessMonitor.allProcesses.collectAsState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        val settingsViewModel: com.miniclick.calltrackmanage.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val uiState by settingsViewModel.uiState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    Icons.Default.DynamicFeed,
                    null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Process Status", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Running processes & sync queue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Battery Optimization Warning in Modal
            if (!uiState.isIgnoringBatteryOptimizations && isSyncSetup) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onDismiss()
                            settingsViewModel.toggleDevicePermissionGuide(true)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BatteryAlert,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Background Sync Restricted",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Tap to fix for reliable call tracking",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
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
                                "Cloud sync paused. Data is saved locally.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Active Processes Section
            val runningProcesses = allProcesses.values.filter { 
                it.status == com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessStatus.RUNNING 
            }
            
            if (runningProcesses.isNotEmpty()) {
                Text(
                    "ACTIVE PROCESSES",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                runningProcesses.forEach { process ->
                    ProcessStatusRow(
                        title = process.title,
                        details = process.details,
                        progress = process.progress,
                        isIndeterminate = process.isIndeterminate,
                        status = process.status,
                        icon = getProcessIcon(process.id)
                    )
                }
            }

            // Pending Queue Section
            val totalPending = pendingNewCalls + pendingRelatedData + pendingRecordings
            val hasPendingItems = totalPending > 0 || allProcesses.values.any { 
                it.status == com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessStatus.PENDING 
            }
            
            if (hasPendingItems || isSyncSetup) {
                Text(
                    if (isSyncSetup) "SYNC QUEUE" else "PENDING ITEMS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Call Log Sync
                ProcessQueueItem(
                    label = "Call Log Sync",
                    count = pendingNewCalls,
                    icon = Icons.Default.PhoneCallback,
                    description = when {
                        pendingNewCalls > 0 -> "$pendingNewCalls new calls to sync"
                        else -> "All synced ✓"
                    },
                    isComplete = pendingNewCalls == 0
                )

                // Metadata Sync (Notes, Labels, etc.)
                ProcessQueueItem(
                    label = "Metadata & Notes",
                    count = pendingRelatedData,
                    icon = Icons.Default.Edit,
                    description = when {
                        pendingRelatedData > 0 -> "$pendingRelatedData changes pending"
                        else -> "All synced ✓"
                    },
                    isComplete = pendingRelatedData == 0
                )

                // Recording Upload
                val syncedRecordings = allProcesses[com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessIds.UPLOAD_RECORDINGS]?.progress ?: 0f
                ProcessQueueItem(
                    label = "Recording Uploads",
                    count = pendingRecordings,
                    icon = Icons.Default.Mic,
                    description = when {
                        pendingRecordings > 0 -> {
                            val activeCount = runningProcesses.find { it.id == com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessIds.UPLOAD_RECORDINGS }?.details?.split("/")?.lastOrNull()?.toIntOrNull() ?: 0
                            if (activeCount > 0 && activeCount < pendingRecordings) {
                                "$activeCount eligible for upload, ${pendingRecordings - activeCount} waiting for metadata sync"
                            } else {
                                "$pendingRecordings recordings in queue"
                            }
                        }
                        else -> "All uploaded ✓"
                    },
                    isComplete = pendingRecordings == 0,
                    onClick = if (pendingRecordings > 0) onRecordingClick else null
                )
            }

            // All Clear State
            if (runningProcesses.isEmpty() && totalPending == 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "All Processes Complete",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isSyncSetup) "All data backed up to cloud" else "All data saved locally",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Sync Now Button (only when sync is setup and items pending)
            if (isSyncSetup && totalPending > 0) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        onSyncAll()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isNetworkAvailable
                ) {
                    Icon(Icons.Default.CloudSync, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Now")
                }
            }
            
            // Setup prompt for non-sync users
            if (!isSyncSetup) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "💡 Enable cloud backup in Settings → Organisation Setup",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ProcessStatusRow(
    title: String,
    details: String?,
    progress: Float,
    isIndeterminate: Boolean,
    status: com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessStatus,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, 
                        null, 
                        modifier = Modifier.size(18.dp), 
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (details != null) {
                        Text(
                            details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Progress bar
            Spacer(Modifier.height(8.dp))
            if (isIndeterminate) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun ProcessQueueItem(
    label: String,
    count: Int,
    icon: ImageVector,
    description: String,
    isComplete: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isComplete) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.secondaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isComplete) Icons.Default.CheckCircle else icon, 
                null, 
                modifier = Modifier.size(18.dp), 
                tint = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isComplete) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (onClick != null && count > 0) {
            Icon(
                Icons.Default.ChevronRight, 
                contentDescription = "View details",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
        }
        
        if (count > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun getProcessIcon(processId: String): ImageVector {
    return when (processId) {
        com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessIds.IMPORT_CALL_LOG -> Icons.Default.PhoneCallback
        com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessIds.FIND_RECORDINGS -> Icons.Default.Search
        com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessIds.SYNC_METADATA -> Icons.Default.CloudSync
        com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessIds.SYNC_PERSONS -> Icons.Default.People
        com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessIds.UPLOAD_RECORDINGS -> Icons.Default.CloudUpload
        com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessIds.ATTACH_RECORDING -> Icons.Default.AttachFile
        com.miniclick.calltrackmanage.data.ProcessMonitor.ProcessIds.PULL_SERVER_UPDATES -> Icons.Default.CloudDownload
        else -> Icons.Default.Sync
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
                RecordingSyncStatus.UPLOADING -> "Uploading" to MaterialTheme.colorScheme.primary
                RecordingSyncStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
                RecordingSyncStatus.NOT_FOUND -> "Not Found" to MaterialTheme.colorScheme.error
                else -> "Waiting" to MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            if (recording.recordingSyncStatus == RecordingSyncStatus.UPLOADING) {
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
    isIgnoringBatteryOptimizations: Boolean = true,
    isSyncSetup: Boolean = true,
    isSetupGuideCompleted: Boolean = true,
    onSyncNow: () -> Unit,
    onShowQueue: () -> Unit,
    onShowDeviceGuide: () -> Unit,
    audioPlayer: AudioPlayer? = null
) {
    val activeProcess by com.miniclick.calltrackmanage.data.ProcessMonitor.activeProcess.collectAsState()
    val callStatus by CallTrackInCallService.callStatus.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Audio Player State
    val isAudioPlaying = audioPlayer?.isPlaying?.collectAsState()?.value ?: false
    val audioMetadata = audioPlayer?.metadata?.collectAsState()?.value
    val audioPos = audioPlayer?.currentPosition?.collectAsState()?.value ?: 0
    val audioDur = audioPlayer?.duration?.collectAsState()?.value ?: 0
    
    // Track duration for active calls
    var activeDuration by remember { mutableStateOf(0L) }
    LaunchedEffect(callStatus?.state) {
        if (callStatus?.state == Call.STATE_ACTIVE) {
            val startTime = System.currentTimeMillis()
            while (callStatus?.state == Call.STATE_ACTIVE) {
                activeDuration = (System.currentTimeMillis() - startTime) / 1000
                delay(1000)
            }
        } else {
            activeDuration = 0
        }
    }

    val totalPending = pendingNewCalls + pendingMetadata + pendingPersonUpdates
    val hasPendingData = totalPending > 0 || pendingRecordings > 0 || activeUploads > 0
    
    // Determine if we have status-level items (Sync, Processes, Network)
    // We suppress these during setup guide to avoid cluttering the UI with "restricted" warnings
    val hasStatusItems = isSetupGuideCompleted && (activeProcess != null || 
                         (isSyncSetup && hasPendingData && (!isNetworkAvailable || !isIgnoringBatteryOptimizations)) ||
                         (isSyncSetup && hasPendingData))
    
    // Determine if we have priority items that need NO delay (Live Calls, Audio Player)
    val hasPriorityItems = callStatus != null || isAudioPlaying

    // Debounced visibility state to prevent "flashing"
    var showBar by remember { mutableStateOf(false) }
    
    // Internal timer to ensure minimum visibility
    var lastShownTime by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(hasPriorityItems, hasStatusItems) {
        if (hasPriorityItems) {
            showBar = true
            lastShownTime = System.currentTimeMillis()
        } else if (hasStatusItems) {
            if (!showBar) {
                // 1. Debounce Show: Wait to see if it's just a momentary blip
                delay(400)
                if (hasStatusItems) {
                    showBar = true
                    lastShownTime = System.currentTimeMillis()
                }
            }
        } else {
            // 2. Debounce Hide: Wait before hiding to bridge gaps between phases (e.g. Import -> Find)
            delay(1000)
            
            // 3. Enforce Minimum Display: If it hasn't been up for 2s, wait longer
            val elapsed = System.currentTimeMillis() - lastShownTime
            if (elapsed < 2000) {
                delay(2000 - elapsed)
            }
            
            if (!hasStatusItems && !hasPriorityItems) {
                showBar = false
            }
        }
    }

    AnimatedVisibility(
        visible = showBar,
        modifier = modifier,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val process = activeProcess
        val isLocalProcess = process != null
        
        // Color scheme:
        val backgroundColor = when {
            callStatus != null -> Color(0xFF30D158) // Green for calls
            isAudioPlaying -> MaterialTheme.colorScheme.tertiaryContainer 
            isLocalProcess -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            !isIgnoringBatteryOptimizations -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
            !isNetworkAvailable && isSyncSetup -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        }
        
        val contentColor = when {
            callStatus != null -> Color.White
            isAudioPlaying -> MaterialTheme.colorScheme.onTertiaryContainer
            isLocalProcess -> MaterialTheme.colorScheme.onSurfaceVariant
            !isIgnoringBatteryOptimizations -> MaterialTheme.colorScheme.onErrorContainer
            !isNetworkAvailable && isSyncSetup -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    when {
                        callStatus != null -> {
                            try {
                                val intent = android.content.Intent(context, com.miniclick.calltrackmanage.ui.call.InCallActivity::class.java)
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                        isAudioPlaying -> audioPlayer?.togglePlayPause()
                        process != null -> onShowQueue()
                        !isIgnoringBatteryOptimizations -> onShowDeviceGuide()
                        else -> onShowQueue()
                    }
                },
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
                    // Ongoing Call - HIGHEST PRIORITY
                    callStatus != null -> {
                        val stateStr = when (callStatus!!.state) {
                            Call.STATE_RINGING -> "Incoming Call..."
                            Call.STATE_DIALING -> "Dialing..."
                            Call.STATE_ACTIVE -> "Active Call - ${String.format("%02d:%02d", activeDuration / 60, activeDuration % 60)}"
                            Call.STATE_DISCONNECTED -> "Call Ended"
                            else -> "In Call"
                        }
                        "${callStatus!!.phoneNumber} • $stateStr"
                    }
                    // Audio Playing
                    isAudioPlaying && audioMetadata != null -> {
                        val name = audioMetadata.name ?: audioMetadata.phoneNumber
                        val pos = audioPos / 1000
                        val dur = audioDur / 1000
                        val timeStr = String.format("%d:%02d / %d:%02d", pos / 60, pos % 60, dur / 60, dur % 60)
                        "Playing: $name • $timeStr"
                    }
                    isAudioPlaying -> "Playing Recording..."
                    // Local processes (no network needed)
                    process != null -> {
                        val progressPct = if (process.isIndeterminate) "" else " ${(process.progress * 100).toInt()}%"
                        val detailText = if (!process.details.isNullOrEmpty()) ": ${process.details}" else progressPct
                        "${process.title}$detailText"
                    }
                    // Battery optimization issue
                    !isIgnoringBatteryOptimizations -> "Background sync restricted • Tap to fix"
                    // Network issue (only matters if sync is setup)
                    !isNetworkAvailable && isSyncSetup -> {
                        val pendingTotal = totalPending + pendingRecordings
                        if (pendingTotal > 0) "Offline • Backup Paused ($pendingTotal items)" else "Offline"
                    }
                    // Active uploads
                    activeUploads > 0 -> {
                        "Uploading $pendingRecordings recording${if (pendingRecordings > 1) "s" else ""}..."
                    }
                    // Pending cloud backup
                    totalPending > 0 -> {
                        val parts = mutableListOf<String>()
                        if (pendingNewCalls > 0) parts.add("$pendingNewCalls calls")
                        if (pendingMetadata > 0) parts.add("$pendingMetadata updates")
                        if (pendingPersonUpdates > 0) parts.add("$pendingPersonUpdates contacts")
                        "Syncing: ${parts.joinToString(", ")}"
                    }
                    // Recordings in queue
                    pendingRecordings > 0 -> "Queue: $pendingRecordings recording${if (pendingRecordings > 1) "s" else ""} waiting"
                    // Fallback (shouldn't happen)
                    else -> "All Synced ✓"
                }

                val icon = when {
                    callStatus != null -> if (callStatus!!.state == Call.STATE_RINGING) Icons.Default.Call else Icons.AutoMirrored.Filled.CallMade
                    isAudioPlaying -> Icons.Default.PlayArrow
                    process != null -> Icons.Default.Sync  // Local processing
                    !isNetworkAvailable && isSyncSetup -> Icons.Default.CloudOff
                    activeUploads > 0 -> Icons.Default.CloudUpload
                    !isIgnoringBatteryOptimizations -> Icons.Default.BatteryAlert
                    pendingNewCalls > 0 -> Icons.Default.PhoneCallback
                    pendingMetadata > 0 -> Icons.Default.EditNote
                    pendingPersonUpdates > 0 -> Icons.Default.Person
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
                 // Prioritize audio bar first
                if (isAudioPlaying) {
                     Spacer(Modifier.width(12.dp))
                     LinearProgressIndicator(
                            progress = { if (audioDur > 0) audioPos.toFloat() / audioDur.toFloat() else 0f },
                            modifier = Modifier
                                .width(60.dp)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = contentColor,
                            trackColor = contentColor.copy(alpha = 0.2f),
                     )
                } else if (process != null || (isNetworkAvailable && (activeUploads > 0 || totalPending > 0))) {
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
