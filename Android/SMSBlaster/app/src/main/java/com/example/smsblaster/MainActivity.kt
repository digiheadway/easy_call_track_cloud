package com.example.smsblaster

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsblaster.data.AppDatabase
import com.example.smsblaster.data.model.Campaign
import com.example.smsblaster.data.model.CampaignMessage
import com.example.smsblaster.data.model.Contact
import com.example.smsblaster.data.model.Template
import com.example.smsblaster.sms.SmsService
import com.example.smsblaster.ui.components.GradientButton
import com.example.smsblaster.ui.dialogs.AddEditContactDialog
import com.example.smsblaster.ui.dialogs.AddEditTemplateDialog
import com.example.smsblaster.ui.dialogs.CreateCampaignDialog
import com.example.smsblaster.ui.dialogs.EditCampaignDialog
import com.example.smsblaster.ui.screens.CampaignDetailScreen
import com.example.smsblaster.ui.screens.CampaignsScreen
import com.example.smsblaster.ui.screens.ContactsScreen
import com.example.smsblaster.ui.screens.TemplatesScreen
import com.example.smsblaster.ui.theme.SMSBlasterTheme
import com.example.smsblaster.viewmodel.CampaignViewModel
import com.example.smsblaster.viewmodel.CampaignWithTemplate
import com.example.smsblaster.viewmodel.ContactViewModel
import com.example.smsblaster.viewmodel.TemplateViewModel
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMSBlasterTheme {
                SMSBlasterApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SMSBlasterApp() {
    val context = LocalContext.current
    val colors = SMSBlasterTheme.colors
    
    // Permission state
    var hasPermissions by remember { mutableStateOf(checkPermissions(context)) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.all { it.value }
        if (!hasPermissions) {
            showPermissionRationale = true
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }
    
    if (!hasPermissions) {
        PermissionScreen(
            onRequestPermissions = {
                permissionLauncher.launch(REQUIRED_PERMISSIONS)
            },
            showRationale = showPermissionRationale
        )
    } else {
        MainContent()
    }
}

private fun checkPermissions(context: android.content.Context): Boolean {
    return REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.SEND_SMS,
    Manifest.permission.READ_PHONE_STATE
)

@Composable
fun PermissionScreen(
    onRequestPermissions: () -> Unit,
    showRationale: Boolean
) {
    val colors = SMSBlasterTheme.colors
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(colors.gradientStart, colors.gradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "SMS Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (showRationale) {
                    "SMS Blaster needs permission to send SMS messages through your SIM card. Please grant the permission to continue."
                } else {
                    "To send bulk SMS messages, SMS Blaster needs access to your device's SMS functionality."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PermissionCard(
                    icon = Icons.Outlined.Sms,
                    title = "Send SMS",
                    description = "Send text messages to your contacts"
                )
                PermissionCard(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Phone State",
                    description = "Access SIM card information for sending"
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            GradientButton(
                text = if (showRationale) "Grant Permissions" else "Continue",
                onClick = onRequestPermissions,
                icon = Icons.Default.Security,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = SMSBlasterTheme.colors
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.chipBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    val colors = SMSBlasterTheme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // ViewModels
    val campaignViewModel: CampaignViewModel = viewModel()
    val templateViewModel: TemplateViewModel = viewModel()
    val contactViewModel: ContactViewModel = viewModel()
    
    // SMS Service for SIM info
    val smsService = remember { SmsService(context) }
    val availableSimCards = remember { smsService.getAvailableSimCards() }
    
    // States
    val campaigns by campaignViewModel.campaignsWithTemplates.collectAsState()
    val templates by templateViewModel.templates.collectAsState()
    val contacts by contactViewModel.contacts.collectAsState()
    val isSending by campaignViewModel.isSending.collectAsState()
    
    val campaignSearchQuery by campaignViewModel.searchQuery.collectAsState()
    val templateSearchQuery by templateViewModel.searchQuery.collectAsState()
    val contactSearchQuery by contactViewModel.searchQuery.collectAsState()
    
    // Dialog states
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddTemplateDialog by remember { mutableStateOf(false) }
    var showCreateCampaignDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }
    var editingTemplate by remember { mutableStateOf<Template?>(null) }
    
    // Campaign editing states
    var showEditCampaignDialog by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf<CampaignEditMode>(CampaignEditMode.NONE) }
    
    // Campaign detail state
    var selectedCampaignWithTemplate by remember { mutableStateOf<CampaignWithTemplate?>(null) }
    var campaignMessages by remember { mutableStateOf<List<CampaignMessage>>(emptyList()) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                contactViewModel.importContacts(it) { count, error ->
                    scope.launch {
                        if (error != null) {
                            snackbarHostState.showSnackbar("Error: $error")
                        } else {
                            snackbarHostState.showSnackbar("Successfully imported $count contacts")
                        }
                    }
                }
            }
        }
    )
    
    // Load campaign messages when a campaign is selected
    LaunchedEffect(selectedCampaignWithTemplate) {
        selectedCampaignWithTemplate?.let { cwt ->
            val db = AppDatabase.getDatabase(context)
            db.campaignMessageDao().getMessagesByCampaignId(cwt.campaign.id).collect { messages ->
                campaignMessages = messages
            }
        }
    }
    
    // Update selected campaign when it changes in the database
    LaunchedEffect(selectedCampaignWithTemplate, campaigns) {
        selectedCampaignWithTemplate?.let { current ->
            val updated = campaigns.find { it.campaign.id == current.campaign.id }
            if (updated != null && updated != current) {
                selectedCampaignWithTemplate = updated
            }
        }
    }
    
    // Pager state for tabs
    val pagerState = rememberPagerState(pageCount = { 3 })
    
    val tabs = listOf(
        TabItem("Campaigns", Icons.Outlined.Campaign, Icons.Filled.Campaign),
        TabItem("Templates", Icons.Outlined.Description, Icons.Filled.Description),
        TabItem("Contacts", Icons.Outlined.Contacts, Icons.Filled.Contacts)
    )
    
    // Handle back button
    BackHandler(enabled = selectedCampaignWithTemplate != null) {
        selectedCampaignWithTemplate = null
        campaignMessages = emptyList()
    }
    
    // Show campaign detail or main content
    if (selectedCampaignWithTemplate != null) {
        val cwt = selectedCampaignWithTemplate!!
        
        CampaignDetailScreen(
            campaign = cwt.campaign,
            templateName = cwt.template?.name,
            template = cwt.template,
            messages = campaignMessages,
            isSending = isSending,
            availableSimCards = availableSimCards,
            onBack = { 
                selectedCampaignWithTemplate = null
                campaignMessages = emptyList()
            },
            onStartCampaign = { simId ->
                campaignViewModel.startCampaign(cwt.campaign.id, simSubscriptionId = simId)
            },
            onPauseCampaign = {
                campaignViewModel.pauseCampaign(cwt.campaign.id)
            },
            onResumeCampaign = {
                campaignViewModel.resumeCampaign(cwt.campaign.id)
            },
            onDeleteCampaign = {
                campaignViewModel.deleteCampaign(cwt.campaign)
                selectedCampaignWithTemplate = null
            },
            onDuplicateCampaign = {
                scope.launch {
                    campaignViewModel.duplicateCampaign(cwt.campaign.id) { newCampaignId ->
                        scope.launch {
                            val newCampaign = campaignViewModel.getCampaignById(newCampaignId)
                            if (newCampaign != null) {
                                val template = campaigns.find { it.campaign.id == newCampaignId }?.template
                                selectedCampaignWithTemplate = CampaignWithTemplate(newCampaign, template)
                            }
                        }
                    }
                }
            },
            onEditRecipients = {
                editMode = CampaignEditMode.RECIPIENTS
                showEditCampaignDialog = true
            },
            onEditTemplate = {
                editMode = CampaignEditMode.TEMPLATE
                showEditCampaignDialog = true
            }
        )
        
        // Edit campaign dialog
        if (showEditCampaignDialog) {
            EditCampaignDialog(
                campaign = cwt.campaign,
                currentTemplate = cwt.template,
                templates = templates,
                contacts = contacts,
                editMode = editMode,
                onDismiss = { 
                    showEditCampaignDialog = false
                    editMode = CampaignEditMode.NONE
                },
                onSave = { templateId, recipientIds ->
                    Log.d(TAG, "Updating campaign: templateId=$templateId, recipients=${recipientIds.size}")
                    campaignViewModel.updateCampaignDetails(
                        campaignId = cwt.campaign.id,
                        templateId = templateId,
                        recipientIds = recipientIds
                    )
                    showEditCampaignDialog = false
                    editMode = CampaignEditMode.NONE
                }
            )
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = colors.cardBackground,
                    tonalElevation = 0.dp,
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (pagerState.currentPage == index) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = colors.textTertiary,
                                unselectedTextColor = colors.textTertiary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { page ->
                when (page) {
                    0 -> CampaignsScreen(
                        campaignsWithTemplates = campaigns,
                        searchQuery = campaignSearchQuery,
                        onSearchQueryChange = { campaignViewModel.setSearchQuery(it) },
                        onCreateCampaign = { showCreateCampaignDialog = true },
                        onCampaignClick = { campaign -> 
                            val cwt = campaigns.find { it.campaign.id == campaign.id }
                            if (cwt != null) {
                                selectedCampaignWithTemplate = cwt
                            }
                        },
                        onDeleteCampaign = { campaignViewModel.deleteCampaign(it) }
                    )
                    1 -> TemplatesScreen(
                        templates = templates,
                        searchQuery = templateSearchQuery,
                        onSearchQueryChange = { templateViewModel.setSearchQuery(it) },
                        onCreateTemplate = { showAddTemplateDialog = true },
                        onTemplateClick = { editingTemplate = it },
                        onDeleteTemplate = { templateViewModel.deleteTemplate(it) }
                    )
                    2 -> ContactsScreen(
                        contacts = contacts,
                        searchQuery = contactSearchQuery,
                        onSearchQueryChange = { contactViewModel.setSearchQuery(it) },
                        onAddContact = { showAddContactDialog = true },
                        onImportCsv = { filePickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) },
                        onContactClick = { editingContact = it },
                        onDeleteContact = { contactViewModel.deleteContact(it) }
                    )
                }
            }
        }
        
        // Dialogs
        if (showAddContactDialog || editingContact != null) {
            AddEditContactDialog(
                contact = editingContact,
                onDismiss = {
                    showAddContactDialog = false
                    editingContact = null
                },
                onSave = { name, phone, customKeys, tags ->
                    Log.d(TAG, "Saving contact: name=$name, phone=$phone, customKeys=$customKeys, tags=$tags")
                    if (editingContact != null) {
                        contactViewModel.updateContact(
                            editingContact!!.copy(
                                name = name,
                                phone = phone,
                                customKeys = customKeys,
                                tags = tags,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        contactViewModel.addContact(name, phone, customKeys, tags)
                    }
                    showAddContactDialog = false
                    editingContact = null
                }
            )
        }
        
        if (showAddTemplateDialog || editingTemplate != null) {
            AddEditTemplateDialog(
                template = editingTemplate,
                onDismiss = {
                    showAddTemplateDialog = false
                    editingTemplate = null
                },
                onSave = { name, content ->
                    if (editingTemplate != null) {
                        templateViewModel.updateTemplate(
                            editingTemplate!!.copy(
                                name = name,
                                content = content,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        templateViewModel.addTemplate(name, content)
                    }
                    showAddTemplateDialog = false
                    editingTemplate = null
                }
            )
        }
        
        if (showCreateCampaignDialog) {
            CreateCampaignDialog(
                templates = templates,
                contacts = contacts,
                onDismiss = { showCreateCampaignDialog = false },
                onCreate = { name, templateId, recipientIds ->
                    campaignViewModel.createCampaign(name, templateId, recipientIds) { campaignId ->
                        scope.launch {
                            val createdCampaign = campaignViewModel.getCampaignById(campaignId)
                            if (createdCampaign != null) {
                                val template = templates.find { it.id == templateId }
                                selectedCampaignWithTemplate = CampaignWithTemplate(createdCampaign, template)
                            }
                        }
                    }
                    showCreateCampaignDialog = false
                }
            )
        }
    }
}

enum class CampaignEditMode {
    NONE, RECIPIENTS, TEMPLATE
}

data class TabItem(
    val title: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)