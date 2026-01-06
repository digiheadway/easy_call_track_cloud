package com.example.callyzer3.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import com.example.callyzer3.ui.screens.AddNoteDialog
import com.example.callyzer3.data.CallType
import com.example.callyzer3.data.CallStatus
import com.example.callyzer3.data.CallLog
import com.example.callyzer3.data.Contact
import com.example.callyzer3.viewmodel.CallHistoryViewModel
import com.example.callyzer3.viewmodel.CallHistoryViewModelFactory
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = viewModel<CallHistoryViewModel>(
        factory = CallHistoryViewModelFactory(context)
    )

    val tabItems = listOf(
        TabItem(CallType.ALL, "All"),
        TabItem(CallType.MISSED, "Missed"),
        TabItem(CallType.NEVER_ATTENDED, "Never Attended"),
        TabItem(CallType.REJECTED, "Rejected")
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var currentNotePhoneNumber by remember { mutableStateOf("") }
    var currentNoteText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Text(
            text = "Call History",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(16.dp)
        )

        // Search Bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = {},
            active = false,
            onActiveChange = {},

            placeholder = { Text("Search calls...") },
            leadingIcon = {
                Icon(Search, contentDescription = "Search")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        ) {
            // Search suggestions would go here
        }

        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabItems.forEachIndexed { index, item ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = "${item.title} (${getCallCountForTab(viewModel, item.callType)})",
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> CallListContent(viewModel.getAllCalls(), viewModel, onAddNote = { phoneNumber ->
                currentNotePhoneNumber = phoneNumber
                currentNoteText = viewModel.getContact(phoneNumber)?.notes ?: ""
                showNoteDialog = true
            })
            1 -> CallListContent(viewModel.getMissedCalls(), viewModel, onAddNote = { phoneNumber ->
                currentNotePhoneNumber = phoneNumber
                currentNoteText = viewModel.getContact(phoneNumber)?.notes ?: ""
                showNoteDialog = true
            })
            2 -> CallListContent(viewModel.getNeverAttendedCalls(), viewModel, onAddNote = { phoneNumber ->
                currentNotePhoneNumber = phoneNumber
                currentNoteText = viewModel.getContact(phoneNumber)?.notes ?: ""
                showNoteDialog = true
            })
            3 -> CallListContent(viewModel.getRejectedCalls(), viewModel, onAddNote = { phoneNumber ->
                currentNotePhoneNumber = phoneNumber
                currentNoteText = viewModel.getContact(phoneNumber)?.notes ?: ""
                showNoteDialog = true
            })
        }

        // Note Dialog
        if (showNoteDialog) {
            AddNoteDialog(
                phoneNumber = currentNotePhoneNumber,
                currentNote = currentNoteText,
                onDismiss = { showNoteDialog = false },
                onSave = { phone, note ->
                    viewModel.addOrUpdateContactNote(phone, note)
                }
            )
        }
    }
}

@Composable
private fun CallListContent(
    calls: List<Pair<CallLog, Contact?>>,
    viewModel: CallHistoryViewModel,
    onAddNote: (String) -> Unit
) {
    if (calls.isEmpty()) {
        EmptyStateContent()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(calls) { (callLog, contact) ->
                CallHistoryItem(
                    callLog = callLog,
                    contact = contact,
                    onAddNote = onAddNote,
                    onExclude = { phoneNumber ->
                        viewModel.excludeNumber(phoneNumber)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No calls found",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your call history will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
private fun CallHistoryItem(
    callLog: CallLog,
    contact: Contact?,
    onAddNote: (String) -> Unit,
    onExclude: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Call type icon
            CallTypeIcon(callLog.callStatus)

            Spacer(modifier = Modifier.width(16.dp))

            // Call details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact?.name ?: callLog.phoneNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )

                if (contact?.notes?.isNotBlank() == true) {
                    Text(
                        text = contact.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    Text(
                        text = formatCallTime(callLog.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    if (callLog.duration > 0) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = formatDuration(callLog.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Action buttons
            Column {
                IconButton(onClick = { onAddNote(callLog.phoneNumber) }) {
                    Icon(
                        imageVector = Add,
                        contentDescription = "Add Note",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = { onExclude(callLog.phoneNumber) }) {
                    Icon(
                        imageVector = Close,
                        contentDescription = "Exclude Number",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
private fun CallTypeIcon(callStatus: com.example.callyzer3.data.CallStatus) {
    Icon(
        imageVector = when (callStatus) {
            com.example.callyzer3.data.CallStatus.ANSWERED -> Icons.Default.CheckCircle
            com.example.callyzer3.data.CallStatus.MISSED -> Icons.Default.Cancel
            com.example.callyzer3.data.CallStatus.REJECTED -> Icons.Default.Close
        },
        contentDescription = null,
        tint = when (callStatus) {
            com.example.callyzer3.data.CallStatus.ANSWERED -> Color.Green
            com.example.callyzer3.data.CallStatus.MISSED -> Color.Red
            com.example.callyzer3.data.CallStatus.REJECTED -> Color(0xFFFF9800) // Orange color
        }
    )
}

private fun formatCallTime(dateTime: java.time.LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    return dateTime.format(formatter)
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${seconds}s"
    }
}

private fun getCallCountForTab(viewModel: CallHistoryViewModel, callType: CallType): Int {
    return when (callType) {
        CallType.ALL -> viewModel.getAllCalls().size
        CallType.MISSED -> viewModel.getMissedCalls().size
        CallType.NEVER_ATTENDED -> viewModel.getNeverAttendedCalls().size
        CallType.REJECTED -> viewModel.getRejectedCalls().size
        CallType.OUTGOING -> 0 // TODO: Implement outgoing calls
        CallType.INCOMING -> 0 // TODO: Implement incoming calls
    }
}

data class TabItem(val callType: CallType, val title: String)
