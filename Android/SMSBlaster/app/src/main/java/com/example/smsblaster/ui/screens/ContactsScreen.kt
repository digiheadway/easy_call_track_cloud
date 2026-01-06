package com.example.smsblaster.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smsblaster.data.model.Contact
import com.example.smsblaster.ui.components.*
import com.example.smsblaster.ui.theme.SMSBlasterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddContact: () -> Unit,
    onImportCsv: () -> Unit,
    onContactClick: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selectedContactIds: Set<Long> = emptySet(),
    onToggleSelection: ((Long) -> Unit)? = null
) {
    val colors = SMSBlasterTheme.colors
    var selectedTagFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Get all unique tags
    val allTags = remember(contacts) {
        contacts.flatMap { it.tags }.distinct().sorted()
    }
    
    // Filter contacts by tags
    val filteredContacts = remember(contacts, selectedTagFilters) {
        if (selectedTagFilters.isEmpty()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.tags.any { it in selectedTagFilters }
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.gradientStart.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(top = 16.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Contacts",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${filteredContacts.size} contacts" + 
                                   if (selectedTagFilters.isNotEmpty()) " (filtered)" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onImportCsv,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = colors.cardBackground,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Import CSV"
                            )
                        }
                        GradientButton(
                            text = "Add",
                            onClick = onAddContact,
                            icon = Icons.Default.PersonAdd,
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SMSBlasterSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = "Search contacts..."
                )
                
                // Tag filters
                if (allTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clear filter chip
                        if (selectedTagFilters.isNotEmpty()) {
                            FilterChip(
                                selected = false,
                                onClick = { selectedTagFilters = emptySet() },
                                label = { Text("Clear") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        
                        // Tag filter chips
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
            }
        }
        
        // Contacts list
        if (filteredContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Outlined.Contacts,
                    title = if (selectedTagFilters.isNotEmpty()) "No Contacts Match" else "No Contacts Yet",
                    description = if (selectedTagFilters.isNotEmpty()) 
                        "Try selecting different tags or clear the filter" 
                    else 
                        "Add contacts to create campaigns and send bulk SMS messages",
                    actionText = if (selectedTagFilters.isEmpty()) "Add Contact" else null,
                    onAction = if (selectedTagFilters.isEmpty()) onAddContact else null
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredContacts,
                    key = { it.id }
                ) { contact ->
                    ContactCard(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        onDelete = { onDeleteContact(contact) },
                        isSelected = if (selectionMode) contact.id in selectedContactIds else false,
                        onSelectionChange = if (selectionMode && onToggleSelection != null) {
                            { _ -> onToggleSelection(contact.id) }
                        } else null
                    )
                }
            }
        }
    }
}
