package com.clicktoearn.linkbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.clicktoearn.linkbox.data.entity.EntityType
import com.clicktoearn.linkbox.ui.viewmodel.LinkBoxViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.StarOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEditorScreen(viewModel: LinkBoxViewModel, entityId: Long, isReadOnly: Boolean, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    val richTextState = rememberRichTextState()
    var isLoading by remember { mutableStateOf(true) }
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val allFolders by viewModel.allFolders.collectAsState()
    
    // State to hold the full entity object for dialogs
    var currentEntity by remember { mutableStateOf<com.clicktoearn.linkbox.data.entity.EntityItem?>(null) }
    
    // Allow toggling read-only mode locally (e.g. from menu)
    var localReadOnly by remember { mutableStateOf(isReadOnly) }
    
    // Dialog States
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(entityId) {
        if (entityId != -1L) {
            val entity = viewModel.getEntity(entityId)
            if (entity != null) {
                currentEntity = entity
                name = entity.name
                richTextState.setMarkdown(entity.value ?: "")
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (localReadOnly) {
                        Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    } else {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Page Title", style = MaterialTheme.typography.titleLarge) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!localReadOnly) {
                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    if (richTextState.toMarkdown().isBlank()) return@Button
                                    name = "Untitled Page"
                                }
                                scope.launch {
                                    val content = richTextState.toMarkdown()
                                    if (entityId == -1L) {
                                        viewModel.addEntity(name, EntityType.PAGE, content, currentFolderId)
                                    } else {
                                        viewModel.updateEntityContent(entityId, name, content)
                                    }
                                    onBack()
                                }
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    } else if (currentEntity != null) {
                        // Read-Only Mode: Show 3-dot Menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = { 
                                        showMenu = false
                                        localReadOnly = false 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = { 
                                        showMenu = false
                                        showShareSheet = true 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                    onClick = { 
                                        showMenu = false
                                        showRenameDialog = true 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Change Location") },
                                    leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                                    onClick = { 
                                        showMenu = false
                                        showMoveDialog = true 
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (currentEntity!!.isPinned) "Unstar" else "Star", color = com.clicktoearn.linkbox.ui.theme.GoldStart) },
                                    leadingIcon = { 
                                        Icon(
                                            if (currentEntity!!.isPinned) Icons.Outlined.StarOutline else Icons.Default.Star, 
                                            contentDescription = null,
                                            tint = com.clicktoearn.linkbox.ui.theme.GoldStart
                                        ) 
                                    },
                                    onClick = { 
                                        showMenu = false
                                        viewModel.toggleEntityPin(entityId, currentEntity!!.isPinned)
                                        // Update local state is a bit tricky as entity is immutable in flow, 
                                        // but we can just let flow update it if we observed it, 
                                        // but here we just fetched it once. 
                                        // For now, toggle works in backend.
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { 
                                        showMenu = false
                                        viewModel.deleteEntity(currentEntity!!)
                                        onBack()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (!localReadOnly) {
                    EditorToolbar(state = richTextState)
                }

                RichTextEditor(
                    state = richTextState,
                    readOnly = localReadOnly,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Start typing your note here...") },
                    colors = RichTextEditorDefaults.richTextEditorColors(
                        containerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            
            // Dialogs
            if (showRenameDialog && currentEntity != null) {
                RenameSheet(
                    entity = currentEntity!!,
                    onDismiss = { showRenameDialog = false },
                    onRename = { newName ->
                        viewModel.renameEntity(entityId, newName)
                        name = newName // Update local name
                        showRenameDialog = false
                    }
                )
            }
            
            if (showMoveDialog && currentEntity != null) {
                MoveEntitySheet(
                    entity = currentEntity!!,
                    folders = allFolders.filter { it.id != entityId }, // Should not be same
                    onDismiss = { showMoveDialog = false },
                    onMove = { newParentId: Long? ->
                        viewModel.moveEntity(entityId, newParentId)
                        showMoveDialog = false
                    }
                )
            }
            
            if (showShareSheet && currentEntity != null) {
                ShareSheet(
                    entity = currentEntity!!,
                    viewModel = viewModel,
                    onDismiss = { showShareSheet = false }
                )
            }
        }
    }
}


@Composable
fun EditorToolbar(state: RichTextState) {
    val currentSpanStyle = state.currentSpanStyle
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bold
        item {
            ToolbarButton(
                icon = Icons.Default.FormatBold,
                active = currentSpanStyle.fontWeight == FontWeight.Bold,
                onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            )
        }
        // Italic
        item {
            ToolbarButton(
                icon = Icons.Default.FormatItalic,
                active = currentSpanStyle.fontStyle == FontStyle.Italic,
                onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            )
        }
        // Underline
        item {
            ToolbarButton(
                icon = Icons.Default.FormatUnderlined,
                active = currentSpanStyle.textDecoration == TextDecoration.Underline,
                onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
            )
        }
        // Strikethrough
        item {
            ToolbarButton(
                icon = Icons.Default.FormatStrikethrough,
                active = currentSpanStyle.textDecoration == TextDecoration.LineThrough,
                onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
            )
        }
        
        item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }
        
        // Bulleted List
        item {
            ToolbarButton(
                icon = Icons.Default.FormatListBulleted,
                active = state.isUnorderedList,
                onClick = { state.toggleUnorderedList() }
            )
        }
        // Numbered List
        item {
            ToolbarButton(
                icon = Icons.Default.FormatListNumbered,
                active = state.isOrderedList,
                onClick = { state.toggleOrderedList() }
            )
        }
        
        item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }
        
        // Code
        item {
            ToolbarButton(
                icon = Icons.Default.Code,
                active = state.isCodeSpan,
                onClick = { state.toggleCodeSpan() }
            )
        }
    }
}

@Composable
fun ToolbarButton(
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}
