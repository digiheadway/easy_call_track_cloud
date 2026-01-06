package com.clicktoearn.linkbox.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import com.clicktoearn.linkbox.ui.Screen
import androidx.navigation.NavController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicktoearn.linkbox.data.local.AssetEntity
import com.clicktoearn.linkbox.ui.components.BottomSheetActionItem
import com.clicktoearn.linkbox.ui.components.LinkBoxBottomSheet
import com.clicktoearn.linkbox.ui.components.LoginRequiredSheet
import com.clicktoearn.linkbox.ui.components.MarkdownContent
import com.clicktoearn.linkbox.ui.components.ShareMenuSheet
import com.clicktoearn.linkbox.utils.findActivity
import com.clicktoearn.linkbox.utils.setScreenshotDisabled
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEditorScreen(
    assetId: String,
    viewModel: LinkBoxViewModel,
    onBack: () -> Unit,
    initialEditMode: Boolean = false
) {
    val assets by viewModel.assets.collectAsState()
    val asset = remember(assetId, assets) {
        assets.find { it.id == assetId }
    }
    
    // Screenshot blocking based on asset's allowScreenCapture setting
    val view = androidx.compose.ui.platform.LocalView.current
    val allowScreenCapture = asset?.allowScreenCapture ?: true
    
    DisposableEffect(allowScreenCapture) {
        val activity = view.context.findActivity()
        if (!allowScreenCapture) {
            android.util.Log.d("PrivacyLinks", "Blocking screenshots on PageEditorScreen")
            activity?.setScreenshotDisabled(true)
        }
        onDispose {
            android.util.Log.d("PrivacyLinks", "Removing screenshot block on PageEditorScreen")
            activity?.setScreenshotDisabled(false)
        }
    }
    
    var isEditMode by remember { mutableStateOf(initialEditMode) }
    var content by remember(asset) { mutableStateOf(asset?.content ?: "") }
    var title by remember(asset) { mutableStateOf(asset?.name ?: "") }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    
    var showMoreMenu by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showAssetInfo by remember { mutableStateOf(false) }
    var showMoveToSheet by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf("") }
    
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var showLoginPrompt by remember { mutableStateOf(false) }
    val loginSheetState = rememberModalBottomSheetState()
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    
    // Track changes
    LaunchedEffect(content, title) {
        hasUnsavedChanges = asset?.let { 
            content != it.content || title != it.name 
        } ?: false
    }
    
    // Auto-focus when entering edit mode
    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            focusRequester.requestFocus()
        }
    }
    
    // Handle save
    fun saveChanges() {
        asset?.let {
            viewModel.updateAsset(it.copy(name = title, content = content))
            hasUnsavedChanges = false
        }
    }
    
    // Handle back press with unsaved changes
    fun handleBack() {
        if (hasUnsavedChanges && isEditMode) {
            saveChanges()
        }
        if (isEditMode) {
            isEditMode = false
            focusManager.clearFocus()
        } else {
            onBack()
        }
    }
    
    if (asset == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading page...")
            }
        }
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                        AnimatedVisibility(visible = isEditMode) {
                            Text(
                                text = if (hasUnsavedChanges) "Editing • Unsaved changes" else "Editing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Edit/Save Toggle
                    AnimatedContent(
                        targetState = isEditMode,
                        transitionSpec = {
                            fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                        },
                        label = "edit_toggle"
                    ) { editMode ->
                        if (editMode) {
                            IconButton(onClick = { 
                                saveChanges()
                                isEditMode = false
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            IconButton(onClick = { 
                                if (!isLoggedIn) {
                                    showLoginPrompt = true
                                } else {
                                    isEditMode = true 
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit"
                                )
                            }
                        }
                    }
                    
                    // More options menu
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main content area
            AnimatedContent(
                targetState = isEditMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith 
                        fadeOut(animationSpec = tween(200))
                },
                label = "content_switch"
            ) { editMode ->
                if (editMode) {
                    // Edit Mode - Full screen text editor
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Formatting toolbar
                        EditorToolbar(
                            onBoldClick = { content = "$content**bold**" },
                            onItalicClick = { content = "$content*italic*" },
                            onHeadingClick = { content = "$content\n# Heading\n" },
                            onBulletClick = { content = "$content\n- Item\n" },
                            onCodeClick = { content = "$content`code`" },
                            onLinkClick = { content = "$content[link](url)" }
                        )
                        
                        HorizontalDivider()
                        
                        // Text editor
                        BasicTextField(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Default,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Default
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (content.isEmpty()) {
                                        Text(
                                            text = "Start writing your content here...\n\nYou can use Markdown formatting:\n• **bold** for bold\n• *italic* for italic\n• # Heading for headings\n• - Item for bullet lists\n• `code` for inline code",
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                } else {
                    // View Mode - Rich text display
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        if (content.isEmpty()) {
                            // Empty state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "This page is empty",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap the edit button to add content",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    FilledTonalButton(onClick = { isEditMode = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Start Editing")
                                    }
                                }
                            }
                        } else {
                            // Render markdown-style content
                            MarkdownContent(content = content)
                        }
                    }
                }
            }
        }
    }
    
    // More Menu Sheet
    if (showMoreMenu) {
        val moreSheetState = rememberModalBottomSheetState()
        LinkBoxBottomSheet(
            onDismissRequest = { showMoreMenu = false },
            sheetState = moreSheetState,
            title = asset.name
        ) {
            BottomSheetActionItem(
                icon = Icons.Filled.DriveFileRenameOutline,
                label = "Rename",
                onClick = {
                    if (!isLoggedIn) {
                        showLoginPrompt = true
                    } else {
                        tempTitle = title
                        showRenameSheet = true
                    }
                    showMoreMenu = false
                }
            )
            BottomSheetActionItem(
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                label = "Move to...",
                onClick = {
                    showMoveToSheet = true
                    showMoreMenu = false
                }
            )
            BottomSheetActionItem(
                icon = Icons.Filled.Share,
                label = "Share",
                onClick = {
                    if (!isLoggedIn) {
                        showLoginPrompt = true
                    } else {
                        showShareMenu = true
                    }
                    showMoreMenu = false
                }
            )
            BottomSheetActionItem(
                icon = Icons.Filled.Info,
                label = "Info",
                onClick = {
                    showAssetInfo = true
                    showMoreMenu = false
                }
            )
            BottomSheetActionItem(
                icon = Icons.Filled.Delete,
                label = "Delete",
                contentColor = MaterialTheme.colorScheme.error,
                onClick = {
                    if (!isLoggedIn) {
                        showLoginPrompt = true
                    } else {
                        showDeleteDialog = true
                    }
                    showMoreMenu = false
                }
            )
        }
    }

    // Rename Sheet (consistent with FilesScreen)
    if (showRenameSheet) {
        val renameSheetState = rememberModalBottomSheetState()
        val renameFocusRequester = remember { FocusRequester() }
        
        LaunchedEffect(Unit) {
            renameFocusRequester.requestFocus()
        }
        
        LinkBoxBottomSheet(
            onDismissRequest = { 
                showRenameSheet = false
                tempTitle = ""
            },
            sheetState = renameSheetState,
            title = "Rename Page"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(renameFocusRequester)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { 
                            showRenameSheet = false
                            tempTitle = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (tempTitle.isNotBlank()) {
                                title = tempTitle
                                hasUnsavedChanges = true
                                saveChanges()
                                showRenameSheet = false
                                tempTitle = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Page?") },
            text = { 
                Text("Are you sure you want to delete \"$title\"? This action cannot be undone.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAsset(asset)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Share Menu Sheet
    if (showShareMenu) {
        val sharingLinks by viewModel.getSharingLinksForAsset(asset.id).collectAsState(initial = emptyList())
        ShareMenuSheet(
            asset = asset,
            existingLinks = sharingLinks,
            onDismiss = { showShareMenu = false },
            onUpdateAsset = { updatedAsset ->
                viewModel.updateAsset(updatedAsset)
            },
            onCreateLink = { name, _, expiryDays ->
                viewModel.createSharingLinkFull(
                    asset,
                    name,
                    expiryDays
                )
            },
            onUpdateLink = { link ->
                viewModel.updateSharingLink(link)
            },
            onDeleteLink = { link ->
                viewModel.deleteSharingLink(link)
            }
        )
    }

    // Asset Info Sheet
    if (showAssetInfo) {
        val infoSheetState = rememberModalBottomSheetState()
        val dateFormatter = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
        
        LinkBoxBottomSheet(
            onDismissRequest = { showAssetInfo = false },
            sheetState = infoSheetState,
            title = "Page Info"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Name", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                    Text(asset.name, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Created", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                    Text(dateFormatter.format(java.util.Date(asset.createdAt)), style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Content Size", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                    Text("${asset.content.length} characters", style = MaterialTheme.typography.bodyLarge)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showAssetInfo = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }

    // Move To Folder Picker Sheet
    if (showMoveToSheet) {
        val moveToSheetState = rememberModalBottomSheetState()
        val allFolders by viewModel.allFolders.collectAsState(initial = emptyList())
        
        LinkBoxBottomSheet(
            onDismissRequest = { showMoveToSheet = false },
            sheetState = moveToSheetState,
            title = "Move \"${asset.name}\" to..."
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ListItem(
                    headlineContent = { Text("My Assets (Root)") },
                    leadingContent = { Icon(Icons.Filled.Home, null) },
                    modifier = Modifier.clickable {
                        viewModel.moveAsset(asset, null)
                        showMoveToSheet = false
                    }
                )
                
                val folders = allFolders.filter { it.id != asset.id }
                if (folders.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    folders.forEach { folder ->
                        ListItem(
                            headlineContent = { Text(folder.name) },
                            leadingContent = { Icon(Icons.Filled.Folder, null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                viewModel.moveAsset(asset, folder.id)
                                showMoveToSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLoginPrompt) {
        LoginRequiredSheet(
            onDismiss = { showLoginPrompt = false },
            viewModel = viewModel,
            sheetState = loginSheetState,
            message = "Login now to fully edit and share this page!"
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditorToolbar(
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onHeadingClick: () -> Unit,
    onBulletClick: () -> Unit,
    onCodeClick: () -> Unit,
    onLinkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToolbarButton(
            icon = Icons.Filled.FormatBold,
            contentDescription = "Bold",
            onClick = onBoldClick
        )
        ToolbarButton(
            icon = Icons.Filled.FormatItalic,
            contentDescription = "Italic",
            onClick = onItalicClick
        )
        ToolbarButton(
            icon = Icons.Filled.Title,
            contentDescription = "Heading",
            onClick = onHeadingClick
        )
        ToolbarButton(
            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
            contentDescription = "Bullet List",
            onClick = onBulletClick
        )
        ToolbarButton(
            icon = Icons.Filled.Code,
            contentDescription = "Code",
            onClick = onCodeClick
        )
        ToolbarButton(
            icon = Icons.Filled.Link,
            contentDescription = "Link",
            onClick = onLinkClick
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun tween(durationMillis: Int) = androidx.compose.animation.core.tween<Float>(durationMillis)
