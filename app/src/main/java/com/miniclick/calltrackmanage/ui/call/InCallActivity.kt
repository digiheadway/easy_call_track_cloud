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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miniclick.calltrackmanage.ui.theme.CallCloudTheme
import com.miniclick.calltrackmanage.service.CallTrackInCallService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
                InCallScreen(
                    onEndCall = {
                        val call = CallTrackInCallService.currentCall
                        if (call != null) {
                            call.disconnect()
                        } else {
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InCallScreen(onEndCall: () -> Unit) {
    var callState by remember { mutableStateOf("Calling...") }
    var duration by remember { mutableLongStateOf(0L) }
    var contactName by remember { mutableStateOf("Unknown") }
    var isMuted by remember { mutableStateOf(false) }
    var audioRoute by remember { mutableIntStateOf(CallAudioState.ROUTE_EARPIECE) }
    
    // Poll for call state
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (isActive) {
            val call = CallTrackInCallService.currentCall
            if (call != null) {
                contactName = call.details?.handle?.schemeSpecificPart ?: "Unknown"
                callState = when (call.state) {
                    Call.STATE_ACTIVE -> "Active"
                    Call.STATE_DIALING -> "Dialing"
                    Call.STATE_RINGING -> "Incoming Call"
                    Call.STATE_HOLDING -> "On Hold"
                    Call.STATE_DISCONNECTED -> "Ended"
                    else -> "Connecting..."
                }
                
                // Update Audio State
                val audioState = CallTrackInCallService.getCurrentAudioState()
                if (audioState != null) {
                    isMuted = audioState.isMuted
                    audioRoute = audioState.route
                }

                if (call.state == Call.STATE_DISCONNECTED) {
                     onEndCall() // Auto close
                }
            } else {
                callState = "Ended"
                delay(1000)
                onEndCall()
            }
            
            if (callState == "Active") {
                duration = (System.currentTimeMillis() - startTime) / 1000
            }
            delay(500)
        }
    }

    Scaffold(
        containerColor = Color.Black // iOS Style Black Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // 1. Header Info (Name & Status)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 32.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (duration > 0) String.format("%02d:%02d", duration / 60, duration % 60) else callState,
                    style = MaterialTheme.typography.titleMedium.copy(
                         fontWeight = FontWeight.Normal,
                         fontSize = 20.sp
                    ),
                    color = Color.Gray
                )
            }

            // 2. Controls Area
            if (callState == "Incoming Call") {
                // Incoming: Slide/Buttons to Answer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 64.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = onEndCall,
                            containerColor = Color(0xFFEB4E3D), // iOS Red
                            contentColor = Color.White,
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Decline", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // Answer Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FloatingActionButton(
                            onClick = {
                                val call = CallTrackInCallService.currentCall
                                call?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                            },
                            containerColor = Color(0xFF30D158), // iOS Green
                            contentColor = Color.White,
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accept",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Accept", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                // Active Call: Keypad OR Controls
                
                var showKeypad by remember { mutableStateOf(false) }

                if (showKeypad) {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        InCallKeypad(
                            onKeyPress = { char ->
                                try {
                                    CallTrackInCallService.currentCall?.playDtmfTone(char)
                                    // Stop tone after short delay or relies on system? 
                                    // Usually start/stop is for press/release. For simple click:
                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        CallTrackInCallService.currentCall?.stopDtmfTone()
                                    }, 200)
                                } catch (e: Exception) {
                                    // Ignore
                                }
                            },
                            onHide = { showKeypad = false }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        // End Call Button Small
                         FloatingActionButton(
                            onClick = onEndCall,
                            containerColor = Color(0xFFEB4E3D), 
                            contentColor = Color.White,
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.CallEnd, "End", Modifier.size(32.dp))
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Grid Row 1 (Only Row)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IOSButton(
                                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = "mute",
                                isActive = isMuted,
                                onClick = { CallTrackInCallService.toggleMute(!isMuted) }
                            )
                            IOSButton(
                                icon = Icons.Default.Dialpad,
                                label = "keypad",
                                isActive = showKeypad,
                                onClick = { showKeypad = true }
                            )
                            val isSpeaker = audioRoute == CallAudioState.ROUTE_SPEAKER
                            IOSButton(
                                icon = Icons.Default.VolumeUp,
                                label = "audio",
                                isActive = isSpeaker,
                                onClick = {
                                    val newRoute = if (isSpeaker) CallAudioState.ROUTE_EARPIECE else CallAudioState.ROUTE_SPEAKER
                                    CallTrackInCallService.setAudioRoute(newRoute)
                                }
                            )
                        }
                        
                        // Spacer to push end call button down nicely
                        Spacer(modifier = Modifier.height(180.dp))

                        // End Call
                        FloatingActionButton(
                            onClick = onEndCall,
                            containerColor = Color(0xFFEB4E3D), 
                            contentColor = Color.White,
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                modifier = Modifier.size(32.dp)
                            )
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
