package com.example.callyzer4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.callyzer4.ui.components.*
import com.example.callyzer4.ui.theme.AppSpacing
import com.example.callyzer4.ui.theme.Callyzer4Theme
import com.example.callyzer4.utils.ActionHandler
import com.example.callyzer4.viewmodel.CallHistoryViewModel

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            // Handle permission denial
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Enable edge-to-edge with proper status bar handling
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Request permissions
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS
            )
        )
        
        setContent {
            Callyzer4Theme {
                CallHistoryScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    viewModel: CallHistoryViewModel = viewModel()
) {
    val callGroups by viewModel.callGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val notes by viewModel.notes.collectAsState()
    
    val context = LocalContext.current
    val actionHandler = remember { ActionHandler(context) }
    
    var selectedFilterTab by remember { mutableStateOf("All") }
    var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilterDialog by remember { mutableStateOf(false) }
    
    // Toast state
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var isToastSuccess by remember { mutableStateOf(true) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Modern Header
        ModernHeader(
            onFilterClick = { showFilterDialog = true }
        )
        
        // Scrollable Filter Tabs
        ScrollableFilterTabs(
            selectedTab = selectedFilterTab,
            onTabSelected = { tab ->
                selectedFilterTab = tab
                val newFilter = when (tab) {
                    "Connected" -> filter.copy(callTypes = setOf(com.example.callyzer4.data.CallType.INCOMING, com.example.callyzer4.data.CallType.OUTGOING))
                    "Missed" -> filter.copy(callTypes = setOf(com.example.callyzer4.data.CallType.MISSED))
                    "Rejected" -> filter.copy(callTypes = setOf(com.example.callyzer4.data.CallType.MISSED))
                    "Outgoing Failed" -> filter.copy(callTypes = setOf(com.example.callyzer4.data.CallType.OUTGOING))
                    else -> filter.copy(callTypes = com.example.callyzer4.data.CallType.values().toSet())
                }
                viewModel.updateFilter(newFilter)
            }
        )
        
        // Remove the old date separator - we'll add individual ones between groups
        
        // Call Groups List
        when {
            isLoading && callGroups.isEmpty() -> {
                LoadingState()
            }
            callGroups.isEmpty() -> {
                EmptyState(
                    onRefresh = { viewModel.loadCallHistory() }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(
                        items = callGroups,
                        key = { _, group -> group.phoneNumber }
                    ) { index, group ->
                        val previousGroup = if (index > 0) callGroups[index - 1] else null
                        
                        // Date separator when day changes
                        DateChangeSeparator(
                            currentGroup = group,
                            previousGroup = previousGroup
                        )
                        
                        ModernCallGroupCard(
                            group = group,
                            isExpanded = expandedGroups.contains(group.phoneNumber),
                            note = notes[group.phoneNumber] ?: "",
                            onToggleExpanded = { phoneNumber ->
                                expandedGroups = if (expandedGroups.contains(phoneNumber)) {
                                    expandedGroups - phoneNumber
                                } else {
                                    expandedGroups + phoneNumber
                                }
                            },
                            onNoteChanged = { phoneNumber, note ->
                                viewModel.saveNote(phoneNumber, note)
                                toastMessage = "Note saved successfully"
                                isToastSuccess = true
                                showToast = true
                            },
                            onCallBack = { phoneNumber ->
                                actionHandler.makeCall(
                                    phoneNumber = phoneNumber,
                                    onSuccess = {
                                        toastMessage = "Calling $phoneNumber..."
                                        isToastSuccess = true
                                        showToast = true
                                    },
                                    onError = { error: String ->
                                        toastMessage = error
                                        isToastSuccess = false
                                        showToast = true
                                    }
                                )
                            },
                            onMessage = { phoneNumber ->
                                actionHandler.sendMessage(
                                    phoneNumber = phoneNumber,
                                    onSuccess = {
                                        toastMessage = "Opening messaging app..."
                                        isToastSuccess = true
                                        showToast = true
                                    },
                                    onError = { error: String ->
                                        toastMessage = error
                                        isToastSuccess = false
                                        showToast = true
                                    }
                                )
                            },
                            onCopy = { phoneNumber ->
                                actionHandler.copyToClipboard(
                                    phoneNumber = phoneNumber,
                                    onSuccess = {
                                        toastMessage = "Phone number copied to clipboard"
                                        isToastSuccess = true
                                        showToast = true
                                    },
                                    onError = { error: String ->
                                        toastMessage = error
                                        isToastSuccess = false
                                        showToast = true
                                    }
                                )
                            },
                            onWhatsApp = { phoneNumber ->
                                actionHandler.sendWhatsApp(
                                    phoneNumber = phoneNumber,
                                    onSuccess = {
                                        toastMessage = "Opening WhatsApp..."
                                        isToastSuccess = true
                                        showToast = true
                                    },
                                    onError = { error: String ->
                                        toastMessage = error
                                        isToastSuccess = false
                                        showToast = true
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Enhanced Filter Dialog
    if (showFilterDialog) {
        EnhancedFilterDialog(
            isOpen = showFilterDialog,
            currentFilter = filter,
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { newFilter ->
                viewModel.updateFilter(newFilter)
                showFilterDialog = false
            }
        )
    }
    
        // Non-blocking Toast Message
        if (showToast) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = AppSpacing.lg)
            ) {
                NonBlockingToast(
                    message = toastMessage,
                    isSuccess = isToastSuccess,
                    onDismiss = { showToast = false }
                )
            }
        }
    }
}