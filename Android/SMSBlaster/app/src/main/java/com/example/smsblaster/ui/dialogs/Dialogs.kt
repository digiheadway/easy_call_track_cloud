package com.example.smsblaster.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.smsblaster.data.model.Contact
import com.example.smsblaster.data.model.Template
import com.example.smsblaster.ui.components.GradientButton
import com.example.smsblaster.ui.components.TagChip
import com.example.smsblaster.ui.theme.SMSBlasterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactDialog(
    contact: Contact? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, customKeys: Map<String, String>, tags: List<String>) -> Unit
) {
    val colors = SMSBlasterTheme.colors
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var customKeys by remember { mutableStateOf(contact?.customKeys ?: emptyMap()) }
    var tags by remember { mutableStateOf(contact?.tags ?: emptyList()) }
    var newTag by remember { mutableStateOf("") }
    var newCustomKey by remember { mutableStateOf("") }
    var newCustomValue by remember { mutableStateOf("") }
    
    val isValid = name.isNotBlank() && phone.isNotBlank()
    
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (contact == null) "Add Contact" else "Edit Contact",
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Name field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = colors.border
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = colors.textTertiary
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Phone field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = colors.border
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null,
                                tint = colors.textTertiary
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Tags section
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (tags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tags) { tag ->
                                TagChip(
                                    tag = tag,
                                    onRemove = { tags = tags - tag }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTag,
                            onValueChange = { newTag = it },
                            label = { Text("Add tag") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = colors.border
                            ),
                            singleLine = true
                        )
                        FilledIconButton(
                            onClick = {
                                if (newTag.isNotBlank()) {
                                    tags = tags + newTag.trim()
                                    newTag = ""
                                }
                            },
                            enabled = newTag.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add tag")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Custom keys section
                    Text(
                        text = "Custom Fields",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    
                    Text(
                        text = "Use these in templates as {key_name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    customKeys.forEach { (key, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.chipBackground)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textPrimary
                                )
                            }
                            IconButton(onClick = { 
                                customKeys = customKeys - key 
                            }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Remove",
                                    tint = colors.textTertiary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomKey,
                            onValueChange = { newCustomKey = it },
                            label = { Text("Key") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = colors.border
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newCustomValue,
                            onValueChange = { newCustomValue = it },
                            label = { Text("Value") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = colors.border
                            ),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    TextButton(
                        onClick = {
                            if (newCustomKey.isNotBlank() && newCustomValue.isNotBlank()) {
                                customKeys = customKeys + (newCustomKey.trim() to newCustomValue.trim())
                                newCustomKey = ""
                                newCustomValue = ""
                            }
                        },
                        enabled = newCustomKey.isNotBlank() && newCustomValue.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom Field")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        text = if (contact == null) "Add Contact" else "Save",
                        onClick = { onSave(name, phone, customKeys, tags) },
                        enabled = isValid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTemplateDialog(
    template: Template? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, content: String) -> Unit
) {
    val colors = SMSBlasterTheme.colors
    var name by remember { mutableStateOf(template?.name ?: "") }
    var content by remember { mutableStateOf(template?.content ?: "") }
    
    val isValid = name.isNotBlank() && content.isNotBlank()
    val placeholders = remember(content) {
        Regex("\\{([^}]+)\\}").findAll(content).map { it.groupValues[1] }.toList()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = colors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (template == null) "Create Template" else "Edit Template",
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Name field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Template Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = colors.border
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = colors.textTertiary
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Content field
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Message Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = colors.border
                        ),
                        maxLines = 10
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Placeholder hints
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Use placeholders",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Type {name} to insert contact name, {phone} for phone number, or any custom field like {company}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            
                            if (placeholders.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Detected placeholders:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(placeholders) { placeholder ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "{$placeholder}",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Character count
                    Text(
                        text = "${content.length} characters | ~${(content.length / 160) + 1} SMS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        text = if (template == null) "Create" else "Save",
                        onClick = { onSave(name, content) },
                        enabled = isValid,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
