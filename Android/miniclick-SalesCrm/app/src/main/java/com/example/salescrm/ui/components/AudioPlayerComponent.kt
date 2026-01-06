package com.example.salescrm.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salescrm.ui.theme.PrimaryBlue
import com.example.salescrm.ui.theme.SalesCrmTheme
import com.example.salescrm.util.AudioPlayerState
import java.io.File

@Composable
fun GlobalAudioPlayer(
    state: AudioPlayerState,
    modifier: Modifier = Modifier
) {
    val playingPath = state.playingPath ?: return
    val fileName = File(playingPath).name

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (state.isMinimized) {
            // Mini Player
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable { state.setMinimized(false) },
                shape = RoundedCornerShape(12.dp),
                color = SalesCrmTheme.colors.surface,
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, SalesCrmTheme.colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = SalesCrmTheme.colors.textPrimary
                        )
                        Text(
                            text = if (state.isPlaying) "Playing..." else "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = SalesCrmTheme.colors.textMuted
                        )
                    }
                    
                    IconButton(onClick = { state.toggle(playingPath) { } }) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            tint = PrimaryBlue
                        )
                    }
                    
                    IconButton(onClick = { state.stop() }) {
                        Icon(Icons.Default.Close, null, tint = SalesCrmTheme.colors.textMuted)
                    }
                }
            }
        } else {
            // Full Player Modal-like overlay
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = SalesCrmTheme.colors.surface,
                tonalElevation = 12.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, SalesCrmTheme.colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with minimize and close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { state.setMinimized(true) }) {
                            Icon(Icons.Default.ExpandMore, "Minimize", tint = SalesCrmTheme.colors.textMuted)
                        }
                        
                        Text(
                            "Now Playing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SalesCrmTheme.colors.textPrimary
                        )
                        
                        IconButton(onClick = { state.stop() }) {
                            Icon(Icons.Default.Close, "Close", tint = SalesCrmTheme.colors.textMuted)
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Album Art / Icon area
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PrimaryBlue.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // File info
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = SalesCrmTheme.colors.textPrimary
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Progress Slider
                    Slider(
                        value = state.progress,
                        onValueChange = { 
                            val newPos = (it * state.duration).toInt()
                            state.seekTo(newPos)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryBlue,
                            activeTrackColor = PrimaryBlue,
                            inactiveTrackColor = PrimaryBlue.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            formatTime(state.position),
                            style = MaterialTheme.typography.labelSmall,
                            color = SalesCrmTheme.colors.textMuted
                        )
                        Text(
                            formatTime(state.duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = SalesCrmTheme.colors.textMuted
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { state.seekTo((state.position - 5000).coerceAtLeast(0)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Replay5, null, tint = SalesCrmTheme.colors.textPrimary, modifier = Modifier.size(32.dp))
                        }
                        
                        Spacer(Modifier.width(24.dp))
                        
                        FloatingActionButton(
                            onClick = { state.toggle(playingPath) { } },
                            containerColor = PrimaryBlue,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Spacer(Modifier.width(24.dp))
                        
                        IconButton(
                            onClick = { state.seekTo((state.position + 5000).coerceAtMost(state.duration)) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Forward5, null, tint = SalesCrmTheme.colors.textPrimary, modifier = Modifier.size(32.dp))
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
