package com.miniclick.calltrackmanage.ui.call

import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miniclick.calltrackmanage.ui.theme.CallCloudTheme
import com.miniclick.calltrackmanage.service.CallTrackInCallService
import com.miniclick.calltrackmanage.ui.home.PersonInteractionBottomSheet
import com.miniclick.calltrackmanage.ui.home.PersonGroup
import com.miniclick.calltrackmanage.util.audio.AudioPlayer
import com.miniclick.calltrackmanage.util.formatting.cleanNumber
import com.miniclick.calltrackmanage.util.formatting.getRelativeTime
import com.miniclick.calltrackmanage.ui.common.LabelChip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Label
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.miniclick.calltrackmanage.ui.settings.SettingsViewModel
import com.miniclick.calltrackmanage.ui.common.PhoneLookupResultModal
import com.miniclick.calltrackmanage.ui.common.NoteDialog
import com.miniclick.calltrackmanage.ui.common.LabelPickerDialog
import androidx.compose.material.icons.filled.Search

@dagger.hilt.android.AndroidEntryPoint
class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure activity shows over lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        setContent {
            CallCloudTheme {
                val inCallViewModel: InCallViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val settingsViewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val context = androidx.compose.ui.platform.LocalContext.current
                val audioPlayer = remember { AudioPlayer(context) }
                
                DisposableEffect(Unit) {
                    onDispose {
                        audioPlayer.release()
                    }
                }
                
                InCallScreen(
                    onEndCall = {
                        val call = CallTrackInCallService.currentCall
                        if (call != null) {
                            call.disconnect()
                        } else {
                            finish()
                        }
                    },
                    inCallViewModel = inCallViewModel,
                    settingsViewModel = settingsViewModel,
                    audioPlayer = audioPlayer
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Notify service that in-call UI is visible (for notification priority adjustment)
        CallTrackInCallService.isInCallActivityVisible = true
    }

    override fun onPause() {
        super.onPause()
        // Notify service that in-call UI is no longer visible
        CallTrackInCallService.isInCallActivityVisible = false
    }
}

@Composable
fun InCallScreen(
    onEndCall: () -> Unit,
    inCallViewModel: InCallViewModel,
    settingsViewModel: SettingsViewModel,
    audioPlayer: AudioPlayer
) {
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    var callState by remember { mutableStateOf("Calling...") }
    var duration by remember { mutableLongStateOf(0L) }
    var phoneNumber by remember { mutableStateOf("") }
    var systemContactName by remember { mutableStateOf<String?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var audioRoute by remember { mutableIntStateOf(CallAudioState.ROUTE_EARPIECE) }
    var showHistoryModal by remember { mutableStateOf(false) }
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var showEditLabelDialog by remember { mutableStateOf(false) }
    var editValue by remember { mutableStateOf("") }
    
    val personGroup by inCallViewModel.callerPersonGroup.collectAsState()
    val availableLabels by inCallViewModel.availableLabels.collectAsState()
    
    val callStatus by CallTrackInCallService.callStatus.collectAsState()
    val systemAudioState by CallTrackInCallService.audioState.collectAsState()
    
    // Reactive updates from service
    // PERFORMANCE FIX: Removed 1-second delay on call end for immediate response
    LaunchedEffect(callStatus) {
        android.util.Log.d("InCallActivity", "callStatus update: state=${callStatus?.state}, phone=${callStatus?.phoneNumber}")
        
        // CRITICAL: Check for null status first - this means the call was completely removed
        if (callStatus == null && phoneNumber.isNotEmpty()) {
            android.util.Log.d("InCallActivity", "Call removed (status null), closing activity")
            onEndCall()
            return@LaunchedEffect
        }
        
        callStatus?.let { call ->
            phoneNumber = call.phoneNumber
            systemContactName = call.contactName
            callState = when (call.state) {
                android.telecom.Call.STATE_ACTIVE -> "Active"
                android.telecom.Call.STATE_DIALING -> "Dialing"
                android.telecom.Call.STATE_RINGING -> "Incoming Call"
                android.telecom.Call.STATE_HOLDING -> "On Hold"
                android.telecom.Call.STATE_DISCONNECTED -> "Ended"
                android.telecom.Call.STATE_PULLING_CALL -> "Pulling..."
                android.telecom.Call.STATE_CONNECTING -> "Connecting..."
                else -> "In Call"
            }
            
            // IMMEDIATE dismissal on disconnect - no artificial delay
            if (call.state == android.telecom.Call.STATE_DISCONNECTED) {
                android.util.Log.d("InCallActivity", "Call disconnected, closing activity immediately")
                onEndCall()
            }
        }
    }

    LaunchedEffect(systemAudioState) {
        systemAudioState?.let { state ->
            isMuted = state.isMuted
            audioRoute = state.route
        }
    }

    // Timer for Active calls
    LaunchedEffect(callState, callStatus?.connectTimeMillis) {
        val connectTime = callStatus?.connectTimeMillis ?: 0L
        if (callState == "Active" && connectTime > 0) {
            while (isActive) {
                duration = (System.currentTimeMillis() - connectTime) / 1000
                delay(1000)
            }
        }
    }

    if (showHistoryModal && personGroup != null) {
        PersonInteractionBottomSheet(
            person = personGroup!!,
            recordings = emptyMap(), // Can be updated if we want to show recordings in-call
            audioPlayer = audioPlayer,
            onDismiss = { showHistoryModal = false },
            onSaveCallNote = { id, note -> inCallViewModel.saveCallNote(id, note) },
            onSavePersonName = { phone, name -> inCallViewModel.savePersonName(phone, name) },
            onSavePersonLabel = { phone, label -> inCallViewModel.savePersonLabel(phone, label) },
            onSavePersonNote = { phone, note -> inCallViewModel.savePersonNote(phone, note) },
            onGetRecordingForLog = { log -> inCallViewModel.getRecordingForLog(log) },
            onAttachRecording = { /* Not supported from in-call yet */ },
            onCustomLookup = { settingsViewModel.showPhoneLookup(it) },
            availableLabels = availableLabels,
            callRecordEnabled = inCallViewModel.callRecordEnabled,
            customLookupEnabled = settingsUiState.customLookupEnabled
        )
    }

    if (settingsUiState.lookupPhoneNumber != null) {
        PhoneLookupResultModal(
            phoneNumber = settingsUiState.lookupPhoneNumber!!,
            uiState = settingsUiState,
            viewModel = settingsViewModel,
            onDismiss = { settingsViewModel.showPhoneLookup(null) }
        )
    }

    if (showEditNoteDialog) {
        NoteDialog(
            title = "Person Note",
            initialNote = editValue,
            label = "Note for ${personGroup?.name ?: phoneNumber}",
            buttonText = "Save Note",
            onDismiss = { showEditNoteDialog = false },
            onSave = { 
                inCallViewModel.savePersonNote(phoneNumber, it)
                showEditNoteDialog = false
            }
        )
    }

    if (showEditLabelDialog) {
        // PERFORMANCE: Load labels only when dialog is shown
        LaunchedEffect(Unit) {
            inCallViewModel.loadLabelsIfNeeded()
        }
        
        LabelPickerDialog(
            currentLabel = personGroup?.label,
            availableLabels = availableLabels,
            onDismiss = { showEditLabelDialog = false },
            onSave = { 
                inCallViewModel.savePersonLabel(phoneNumber, it)
                showEditLabelDialog = false
            }
        )
    }

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val displayName = personGroup?.name ?: systemContactName ?: cleanNumber(phoneNumber)
            val cleanedPhone = cleanNumber(phoneNumber)
            
            // 1. FIXED IDENTITY HEADER
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle DP
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF333333), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = displayName.firstOrNull()?.toString()?.uppercase() ?: "?"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1
                    )
                    
                    if (displayName != cleanedPhone) {
                        Text(
                            text = cleanedPhone,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray.copy(alpha = 0.8f)
                        )
                    }

                    if (systemContactName != null && systemContactName != displayName && systemContactName != cleanedPhone) {
                        Text(
                            text = systemContactName!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                    
                    Text(
                        text = if (duration > 0) String.format("%02d:%02d", duration / 60, duration % 60) else callState,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (callState == "Incoming Call") Color(0xFF30D158) else Color.White,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))

            // Action Row (Utility icons) 
            // Removed from here as requested to make it cleaner

            // 3. SCROLLABLE DATA AREA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val currentPerson = personGroup
                if (currentPerson != null) {
                    // Labels Section
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (!currentPerson.label.isNullOrEmpty()) {
                            LabelChip(label = currentPerson.label, onClick = { 
                                showEditLabelDialog = true
                            }, color = Color(0xFF333333))
                        }
                        
                        TextButton(
                            onClick = { showEditLabelDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (currentPerson.label.isNullOrEmpty()) "Add Label" else "Edit", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    // Note Section
                    if (!currentPerson.personNote.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .clickable { 
                                    editValue = currentPerson.personNote ?: ""
                                    showEditNoteDialog = true
                                }
                        ) {
                            Icon(Icons.Default.StickyNote2, null, tint = Color.Gray, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentPerson.personNote!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    } else {
                        TextButton(onClick = { 
                            editValue = ""
                            showEditNoteDialog = true 
                        }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Note", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Recent History Card
                    if (currentPerson.calls.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Recent Calls", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                                Text("${currentPerson.calls.size}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            Spacer(Modifier.height(8.dp))
                            currentPerson.calls.take(3).forEach { call ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val icon = when (call.callType) {
                                            android.provider.CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived
                                            android.provider.CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
                                            android.provider.CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
                                            else -> Icons.Default.Call
                                        }
                                        Icon(icon, null, tint = if (call.callType == android.provider.CallLog.Calls.MISSED_TYPE) Color(0xFFEB4E3D) else Color.Gray, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(getRelativeTime(call.callDate), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                    if (call.duration > 0) {
                                        Text(String.format("%02d:%02d", call.duration / 60, call.duration % 60), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                } else if (phoneNumber.isNotEmpty() && settingsUiState.customLookupEnabled) {
                    Button(
                        onClick = { settingsViewModel.showPhoneLookup(phoneNumber) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Online Lookup Caller")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. Fixed Controls Area
            val isIncoming = callStatus?.isIncoming == true
            val isActive = callStatus?.state == android.telecom.Call.STATE_ACTIVE
            val isConnecting = callStatus?.state == android.telecom.Call.STATE_CONNECTING || 
                              callStatus?.state == android.telecom.Call.STATE_DIALING
            
            if (isIncoming && !isActive) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(onClick = onEndCall, containerColor = Color(0xFFEB4E3D), shape = CircleShape) {
                            Icon(Icons.Default.CallEnd, "Decline", Modifier.size(32.dp), tint = Color.White)
                        }
                        Text("Decline", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    }
                    
                    if (callStatus?.state == android.telecom.Call.STATE_RINGING) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FloatingActionButton(onClick = { CallTrackInCallService.currentCall?.answer(0) }, containerColor = Color(0xFF30D158), shape = CircleShape) {
                                Icon(Icons.Default.Call, "Accept", Modifier.size(32.dp), tint = Color.White)
                            }
                            Text("Accept", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                        }
                    } else {
                        // Show "Connecting..." or similar if it's an incoming call being answered
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF30D158), modifier = Modifier.size(56.dp))
                            Text("Connecting...", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            } else if (callStatus?.state == android.telecom.Call.STATE_DISCONNECTED) {
                // Show nothing or "Ended" footer
                Box(modifier = Modifier.fillMaxWidth().height(100.dp))
            } else {
                var showKeypad by remember { mutableStateOf(false) }
                if (showKeypad) {
                    InCallKeypad(
                        onKeyPress = { char ->
                            val call = CallTrackInCallService.currentCall
                            if (call != null) {
                                call.playDtmfTone(char)
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ try { call.stopDtmfTone() } catch (e: Exception) {} }, 300)
                            }
                        },
                        onHide = { showKeypad = false }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        // Utility Row (History, Keypad, Note)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            IOSButton(icon = Icons.Default.History, label = "history", isActive = showHistoryModal, onClick = { showHistoryModal = true })
                            IOSButton(icon = Icons.Default.Dialpad, label = "keypad", isActive = showKeypad, onClick = { showKeypad = true })
                            IOSButton(icon = Icons.Default.StickyNote2, label = "note", isActive = !personGroup?.personNote.isNullOrEmpty(), onClick = { 
                                editValue = personGroup?.personNote ?: ""
                                showEditNoteDialog = true 
                            })
                        }

                        // Primary Row (Mute, End Call, Speaker)
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IOSButton(icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, label = "mute", isActive = isMuted, onClick = { CallTrackInCallService.toggleMute(!isMuted) })
                            
                            FloatingActionButton(
                                onClick = onEndCall, 
                                containerColor = Color(0xFFEB4E3D), 
                                shape = CircleShape, 
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(Icons.Default.CallEnd, "End", Modifier.size(32.dp), tint = Color.White)
                            }
                            
                            val isSpeaker = audioRoute == CallAudioState.ROUTE_SPEAKER
                            IOSButton(icon = Icons.Default.VolumeUp, label = "speaker", isActive = isSpeaker, onClick = {
                                val newRoute = if (isSpeaker) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER
                                CallTrackInCallService.setAudioRoute(newRoute)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InCallKeypad(onKeyPress: (Char) -> Unit, onHide: () -> Unit) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('*', '0', '#')
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF333333).copy(alpha = 0.8f), CircleShape)
                            .clickable { onKeyPress(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onHide) {
             Text("Hide", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun IOSButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = if (isActive) Color.White else Color(0xFF333333),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = if (isActive) Color.Black else Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = Color.White
        )
    }
}
