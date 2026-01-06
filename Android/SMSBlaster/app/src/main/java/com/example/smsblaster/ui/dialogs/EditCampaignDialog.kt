package com.example.smsblaster.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.smsblaster.CampaignEditMode
import com.example.smsblaster.data.model.Campaign
import com.example.smsblaster.data.model.Contact
import com.example.smsblaster.data.model.Template
import com.example.smsblaster.ui.components.GradientButton
import com.example.smsblaster.ui.components.SMSBlasterSearchBar
import com.example.smsblaster.ui.theme.SMSBlasterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCampaignDialog(
    campaign: Campaign,
    currentTemplate: Template?,
    templates: List<Template>,
    contacts: List<Contact>,
    editMode: CampaignEditMode,
    onDismiss: () -> Unit,
    onSave: (templateId: Long, recipientIds: List<Long>) -> Unit
) {
    val colors = SMSBlasterTheme.colors
    var selectedTemplateId by remember { mutableStateOf(campaign.templateId ?: 0L) }
    var selectedContactIds by remember { mutableStateOf(campaign.recipientIds.toSet()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Get all unique tags
    val allTags = remember(contacts) {
        contacts.flatMap { it.tags }.distinct().sorted()
    }
    
    // Filter contacts
    val filteredContacts = remember(contacts, searchQuery, selectedTagFilters) {
        contacts.filter { contact ->
            val matchesSearch = searchQuery.isBlank() ||
                contact.name.contains(searchQuery, ignoreCase = true) ||
                contact.phone.contains(searchQuery)
            val matchesTags = selectedTagFilters.isEmpty() ||
                contact.tags.any { it in selectedTagFilters }
            matchesSearch && matchesTags
        }
    }
    
    BackHandler { onDismiss() }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = colors.cardBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                        Text(
                            text = when (editMode) {
                                CampaignEditMode.TEMPLATE -> "Change Template"
                                CampaignEditMode.RECIPIENTS -> "Edit Recipients"
                                else -> "Edit Campaign"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }
                
                when (editMode) {
                    CampaignEditMode.TEMPLATE -> {
                        // Template selection
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(templates, key = { it.id }) { template ->
                                val isSelected = template.id == selectedTemplateId
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedTemplateId = template.id }
                                        .then(
                                            if (isSelected) Modifier.border(
                                                width = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(16.dp)
                                            ) else Modifier
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else colors.chipBackground
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = template.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = template.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textSecondary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    CampaignEditMode.RECIPIENTS -> {
                        // Search
                        SMSBlasterSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Search contacts...",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        // Tag filters
                        if (allTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                allTags.forEach { tag ->
                                    FilterChip(
                                        selected = tag in selectedTagFilters,
                                        onClick = {
                                            selectedTagFilters = if (tag in selectedTagFilters) {
                                                selectedTagFilters - tag
                                            } else {
                                                selectedTagFilters + tag
                                            }
                                        },
                                        label = { Text(tag) },
                                        leadingIcon = if (tag in selectedTagFilters) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                        
                        // Selection actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedContactIds.size} selected",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textSecondary
                            )
                            Row {
                                TextButton(
                                    onClick = {
                                        selectedContactIds = selectedContactIds + filteredContacts.map { it.id }.toSet()
                                    }
                                ) {
                                    Text("Select All")
                                }
                                TextButton(
                                    onClick = {
                                        selectedContactIds = selectedContactIds - filteredContacts.map { it.id }.toSet()
                                    }
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                        
                        // Contact list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredContacts, key = { it.id }) { contact ->
                                val isSelected = contact.id in selectedContactIds
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedContactIds = if (isSelected) {
                                                selectedContactIds - contact.id
                                            } else {
                                                selectedContactIds + contact.id
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else colors.chipBackground
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = {
                                                selectedContactIds = if (isSelected) {
                                                    selectedContactIds - contact.id
                                                } else {
                                                    selectedContactIds + contact.id
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(colors.surfaceElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contact.name.firstOrNull()?.uppercase() ?: "?",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = contact.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = contact.phone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary
                                            )
                                            if (contact.tags.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    contact.tags.take(2).forEach { tag ->
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = colors.chipBackground
                                                        ) {
                                                            Text(
                                                                text = tag,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = colors.textTertiary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    else -> {}
                }
                
                // Bottom action
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = colors.cardBackground
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        GradientButton(
                            text = "Save Changes",
                            onClick = {
                                onSave(selectedTemplateId, selectedContactIds.toList())
                            },
                            enabled = selectedTemplateId > 0 && selectedContactIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
