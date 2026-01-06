package com.example.smsblaster.ui.dialogs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.DialogProperties
import com.example.smsblaster.data.model.Contact
import com.example.smsblaster.data.model.Template
import com.example.smsblaster.ui.components.GradientButton
import com.example.smsblaster.ui.components.SMSBlasterSearchBar
import com.example.smsblaster.ui.components.TagChip
import com.example.smsblaster.ui.theme.SMSBlasterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCampaignDialog(
    templates: List<Template>,
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onCreate: (name: String, templateId: Long, recipientIds: List<Long>) -> Unit
) {
    val colors = SMSBlasterTheme.colors
    var campaignName by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf<Long?>(null) }
    var selectedContactIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var contactSearchQuery by remember { mutableStateOf("") }
    var showTemplateSelector by remember { mutableStateOf(false) }
    var showContactSelector by remember { mutableStateOf(false) }
    var showMessagePreview by remember { mutableStateOf<Contact?>(null) }
    var selectedTagFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Get all unique tags
    val allTags = remember(contacts) {
        contacts.flatMap { it.tags }.distinct().sorted()
    }
    
    // Filter contacts by search and tags
    val filteredContacts = remember(contacts, contactSearchQuery, selectedTagFilters) {
        contacts.filter { contact ->
            val matchesSearch = contactSearchQuery.isBlank() ||
                contact.name.contains(contactSearchQuery, ignoreCase = true) ||
                contact.phone.contains(contactSearchQuery)
            val matchesTags = selectedTagFilters.isEmpty() ||
                contact.tags.any { it in selectedTagFilters }
            matchesSearch && matchesTags
        }
    }
    
    val selectedTemplate = remember(templates, selectedTemplateId) {
        templates.find { it.id == selectedTemplateId }
    }
    
    val selectedContacts = remember(contacts, selectedContactIds) {
        contacts.filter { it.id in selectedContactIds }
    }
    
    val isValid = campaignName.isNotBlank() && 
                  selectedTemplateId != null && 
                  selectedContactIds.isNotEmpty()
    
    // Handle back button
    BackHandler {
        when {
            showMessagePreview != null -> showMessagePreview = null
            showContactSelector -> showContactSelector = false
            showTemplateSelector -> showTemplateSelector = false
            else -> onDismiss()
        }
    }
    
    Dialog(
        onDismissRequest = {
            when {
                showMessagePreview != null -> showMessagePreview = null
                showContactSelector -> showContactSelector = false
                showTemplateSelector -> showTemplateSelector = false
                else -> onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f),
            shape = RoundedCornerShape(24.dp),
            color = colors.cardBackground
        ) {
            when {
                showMessagePreview != null -> {
                    MessagePreviewDialog(
                        contact = showMessagePreview!!,
                        template = selectedTemplate,
                        onDismiss = { showMessagePreview = null }
                    )
                }
                showTemplateSelector -> {
                    TemplateSelectorScreen(
                        templates = templates,
                        selectedTemplateId = selectedTemplateId,
                        onSelectTemplate = { 
                            selectedTemplateId = it
                            showTemplateSelector = false
                        },
                        onBack = { showTemplateSelector = false }
                    )
                }
                showContactSelector -> {
                    ContactSelectorScreen(
                        contacts = filteredContacts,
                        allTags = allTags,
                        selectedContactIds = selectedContactIds,
                        selectedTagFilters = selectedTagFilters,
                        searchQuery = contactSearchQuery,
                        onSearchQueryChange = { contactSearchQuery = it },
                        onToggleTag = { tag ->
                            selectedTagFilters = if (tag in selectedTagFilters) {
                                selectedTagFilters - tag
                            } else {
                                selectedTagFilters + tag
                            }
                        },
                        onToggleContact = { id ->
                            selectedContactIds = if (id in selectedContactIds) {
                                selectedContactIds - id
                            } else {
                                selectedContactIds + id
                            }
                        },
                        onSelectAll = {
                            selectedContactIds = selectedContactIds + filteredContacts.map { it.id }.toSet()
                        },
                        onClearAll = {
                            selectedContactIds = selectedContactIds - filteredContacts.map { it.id }.toSet()
                        },
                        onDone = { showContactSelector = false }
                    )
                }
                else -> {
                    // Main campaign creation form
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            colors.gradientStart.copy(alpha = 0.2f),
                                            colors.cardBackground
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Create Campaign",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }
                        
                        // Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Campaign Name
                            OutlinedTextField(
                                value = campaignName,
                                onValueChange = { campaignName = it },
                                label = { Text("Campaign Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = colors.border
                                ),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Campaign,
                                        contentDescription = null,
                                        tint = colors.textTertiary
                                    )
                                },
                                placeholder = { Text("e.g., Holiday Sale Announcement") }
                            )
                            
                            // Template Selection
                            Text(
                                text = "Message Template",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTemplateSelector = true },
                                shape = RoundedCornerShape(12.dp),
                                color = colors.chipBackground
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedTemplate != null) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedTemplate.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = selectedTemplate.content,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Select a template",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textTertiary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.textTertiary
                                    )
                                }
                            }
                            
                            // Recipients Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recipients",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                TextButton(onClick = { showContactSelector = true }) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Recipients")
                                }
                            }
                            
                            if (selectedContacts.isEmpty()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showContactSelector = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.chipBackground
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.GroupAdd,
                                            contentDescription = null,
                                            tint = colors.textTertiary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "No recipients selected",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textTertiary
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                text = "${selectedContacts.size} recipients selected",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                            TextButton(onClick = { selectedContactIds = emptySet() }) {
                                                Text("Clear All", color = colors.error)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Show first few recipients
                                        selectedContacts.take(5).forEach { contact ->
                                            RecipientChip(
                                                contact = contact,
                                                onClick = {
                                                    if (selectedTemplate != null) {
                                                        showMessagePreview = contact
                                                    }
                                                },
                                                onRemove = {
                                                    selectedContactIds = selectedContactIds - contact.id
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                        
                                        if (selectedContacts.size > 5) {
                                            TextButton(
                                                onClick = { showContactSelector = true },
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            ) {
                                                Text("View all ${selectedContacts.size} recipients")
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Preview hint
                            if (selectedTemplate != null && selectedContacts.isNotEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.info.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = colors.info,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Tap on a recipient to preview their personalized message",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.info
                                        )
                                    }
                                }
                            }
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
                                    .padding(20.dp),
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
                                    text = "Create Campaign",
                                    onClick = {
                                        selectedTemplateId?.let { templateId ->
                                            onCreate(campaignName, templateId, selectedContactIds.toList())
                                        }
                                    },
                                    enabled = isValid,
                                    modifier = Modifier.weight(1f),
                                    icon = Icons.Default.RocketLaunch
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
private fun RecipientChip(
    contact: Contact,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = SMSBlasterTheme.colors
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
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
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagePreviewDialog(
    contact: Contact,
    template: Template?,
    onDismiss: () -> Unit
) {
    val colors = SMSBlasterTheme.colors
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "Message Preview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Contact info
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.chipBackground
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = contact.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    if (contact.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            contact.tags.take(3).forEach { tag ->
                                TagChip(tag = tag)
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Message that will be sent:",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Message bubble
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = template?.formatMessage(contact) ?: "No template selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${template?.formatMessage(contact)?.length ?: 0} characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        GradientButton(
            text = "Done",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateSelectorScreen(
    templates: List<Template>,
    selectedTemplateId: Long?,
    onSelectTemplate: (Long) -> Unit,
    onBack: () -> Unit
) {
    val colors = SMSBlasterTheme.colors
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = "Select Template",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }
        
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No templates yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Create a template first from the Templates tab",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    val isSelected = template.id == selectedTemplateId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTemplate(template.id) }
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
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
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
                            
                            val placeholders = template.getPlaceholders()
                            if (placeholders.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    placeholders.take(5).forEach { placeholder ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "{$placeholder}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactSelectorScreen(
    contacts: List<Contact>,
    allTags: List<String>,
    selectedContactIds: Set<Long>,
    selectedTagFilters: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onToggleContact: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onDone: () -> Unit
) {
    val colors = SMSBlasterTheme.colors
    
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
                IconButton(onClick = onDone) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Column {
                    Text(
                        text = "Select Recipients",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${selectedContactIds.size} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Row {
                TextButton(onClick = onSelectAll) {
                    Text("All")
                }
                TextButton(onClick = onClearAll) {
                    Text("Clear")
                }
            }
        }
        
        // Search
        SMSBlasterSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
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
                        onClick = { onToggleTag(tag) },
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
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Contact list
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No contacts found",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    val isSelected = contact.id in selectedContactIds
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleContact(contact.id) },
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
                                onCheckedChange = { onToggleContact(contact.id) },
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
                                        if (contact.tags.size > 2) {
                                            Text(
                                                text = "+${contact.tags.size - 2}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textTertiary
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
        
        // Done button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = colors.cardBackground
        ) {
            GradientButton(
                text = "Done (${selectedContactIds.size} selected)",
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}
