package com.example.smsblaster.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.smsblaster.data.model.Campaign
import com.example.smsblaster.data.model.CampaignMessage
import com.example.smsblaster.data.model.CampaignStatus
import com.example.smsblaster.data.model.MessageStatus
import com.example.smsblaster.data.model.Template
import com.example.smsblaster.sms.SimInfo
import com.example.smsblaster.ui.components.GradientButton
import com.example.smsblaster.ui.components.StatusChip
import com.example.smsblaster.ui.theme.SMSBlasterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignDetailScreen(
    campaign: Campaign,
    templateName: String?,
    template: Template?,
    messages: List<CampaignMessage>,
    isSending: Boolean,
    availableSimCards: List<SimInfo>,
    onBack: () -> Unit,
    onStartCampaign: (simSubscriptionId: Int?) -> Unit,
    onPauseCampaign: () -> Unit,
    onResumeCampaign: () -> Unit,
    onDeleteCampaign: () -> Unit,
    onDuplicateCampaign: () -> Unit,
    onEditRecipients: () -> Unit,
    onEditTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SMSBlasterTheme.colors
    val animatedProgress by animateFloatAsState(
        targetValue = campaign.progress,
        label = "progress"
    )
    
    var showSimSelector by remember { mutableStateOf(false) }
    var showMessagePreview by remember { mutableStateOf<CampaignMessage?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    
    // Handle back button
    BackHandler {
        when {
            showMessagePreview != null -> showMessagePreview = null
            showSimSelector -> showSimSelector = false
            else -> onBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = campaign.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (templateName != null) {
                            Text(
                                text = "Template: $templateName",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    StatusChip(status = campaign.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Box {
                        IconButton(onClick = { showMoreOptions = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = colors.textSecondary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMoreOptions,
                            onDismissRequest = { showMoreOptions = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Duplicate Campaign") },
                                onClick = {
                                    showMoreOptions = false
                                    onDuplicateCampaign()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Recipients") },
                                onClick = {
                                    showMoreOptions = false
                                    onEditRecipients()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.People, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Change Template") },
                                onClick = {
                                    showMoreOptions = false
                                    onEditTemplate()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Description, contentDescription = null)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete Campaign", color = colors.error) },
                                onClick = {
                                    showMoreOptions = false
                                    showDeleteConfirmation = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = colors.error)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Progress circle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Progress",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${(campaign.progress * 100).toInt()}% completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 8.dp,
                                color = colors.success,
                                trackColor = colors.chipBackground,
                            )
                            Text(
                                text = "${campaign.sentCount}/${campaign.totalCount}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard(
                            value = campaign.totalCount.toString(),
                            label = "Total",
                            color = colors.info
                        )
                        StatCard(
                            value = campaign.sentCount.toString(),
                            label = "Sent",
                            color = colors.success
                        )
                        StatCard(
                            value = campaign.remainingCount.toString(),
                            label = "Remaining",
                            color = colors.warning
                        )
                        StatCard(
                            value = campaign.failedCount.toString(),
                            label = "Failed",
                            color = colors.error
                        )
                    }
                }
            }
            
            // Template preview
            if (template != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.chipBackground
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Template Preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                            TextButton(onClick = onEditTemplate) {
                                Text("Change", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text(
                            text = template.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (campaign.status) {
                    CampaignStatus.DRAFT -> {
                        GradientButton(
                            text = if (isSending) "Starting..." else "Start Campaign",
                            onClick = {
                                if (availableSimCards.size > 1) {
                                    showSimSelector = true
                                } else {
                                    onStartCampaign(availableSimCards.firstOrNull()?.subscriptionId)
                                }
                            },
                            enabled = !isSending && campaign.totalCount > 0,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.PlayArrow
                        )
                    }
                    CampaignStatus.RUNNING -> {
                        Button(
                            onClick = onPauseCampaign,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.warning
                            )
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pause")
                        }
                    }
                    CampaignStatus.PAUSED -> {
                        GradientButton(
                            text = "Resume",
                            onClick = onResumeCampaign,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.PlayArrow
                        )
                    }
                    else -> {
                        if (campaign.remainingCount > 0) {
                            GradientButton(
                                text = "Retry Failed",
                                onClick = {
                                    if (availableSimCards.size > 1) {
                                        showSimSelector = true
                                    } else {
                                        onStartCampaign(availableSimCards.firstOrNull()?.subscriptionId)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Refresh
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Messages list header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recipients",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = "${messages.size} recipients",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Messages list
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Sms,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recipients yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageItem(
                            message = message,
                            onClick = { showMessagePreview = message }
                        )
                    }
                }
            }
        }
    }
    
    // SIM Selector Dialog
    if (showSimSelector) {
        AlertDialog(
            onDismissRequest = { showSimSelector = false },
            title = { Text("Select SIM Card") },
            text = {
                Column {
                    availableSimCards.forEach { sim ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSimSelector = false
                                    onStartCampaign(sim.subscriptionId)
                                }
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.chipBackground
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SimCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = sim.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = sim.carrierName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSimSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Message Preview Dialog
    if (showMessagePreview != null) {
        Dialog(onDismissRequest = { showMessagePreview = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardBackground
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Message Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        IconButton(onClick = { showMessagePreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Recipient info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = showMessagePreview!!.phoneNumber.takeLast(2),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = showMessagePreview!!.phoneNumber,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = showMessagePreview!!.status.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = when (showMessagePreview!!.status) {
                                    MessageStatus.SENT, MessageStatus.DELIVERED -> colors.success
                                    MessageStatus.FAILED -> colors.error
                                    MessageStatus.PENDING -> colors.textSecondary
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Message bubble
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = showMessagePreview!!.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    if (showMessagePreview!!.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = colors.error.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = showMessagePreview!!.errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.error
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "${showMessagePreview!!.message.length} characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary
                    )
                }
            }
        }
    }
    
    // Delete confirmation
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Campaign?") },
            text = { Text("This will permanently delete the campaign and all its messages. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteCampaign()
                    }
                ) {
                    Text("Delete", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = SMSBlasterTheme.colors
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun MessageItem(
    message: CampaignMessage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SMSBlasterTheme.colors
    val (statusColor, statusIcon) = when (message.status) {
        MessageStatus.PENDING -> colors.textTertiary to Icons.Outlined.Schedule
        MessageStatus.SENT -> colors.success to Icons.Outlined.CheckCircle
        MessageStatus.DELIVERED -> colors.success to Icons.Filled.CheckCircle
        MessageStatus.FAILED -> colors.error to Icons.Outlined.Error
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = colors.chipBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (message.errorMessage != null) {
                    Text(
                        text = message.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = message.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
