package com.example.salescrm.notification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salescrm.MainActivity
import com.example.salescrm.ui.theme.PrimaryBlue
import com.example.salescrm.ui.theme.SalesCrmTheme
import com.example.salescrm.data.CallLogRepository
import com.example.salescrm.data.UserPreferencesRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PostCallActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_PERSON_ID = "extra_person_id"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_TYPE = "extra_type"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_DEFAULT_COUNTRY = "extra_default_country"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val personId = intent.getIntExtra(EXTRA_PERSON_ID, -1)
        val name = intent.getStringExtra(EXTRA_NAME) ?: "Unknown"
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "Call"
        val duration = intent.getLongExtra(EXTRA_DURATION, 0L)
        val defaultCountry = intent.getStringExtra(EXTRA_DEFAULT_COUNTRY) ?: "US"
        
        // Ensure we show over lock screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        
        setContent {
            SalesCrmTheme {
                // Determine layout state
                val isCallEnded = !type.contains("Ingoing", ignoreCase = true) && !type.contains("Incoming", ignoreCase = true) && !type.contains("Outgoing", ignoreCase = true)
                val headerColor = if (isCallEnded) MaterialTheme.colorScheme.error else PrimaryBlue
                val headerText = if (type.contains("Incoming", ignoreCase = true)) "Incoming Call..." 
                                 else if (type.contains("Outgoing", ignoreCase = true)) "Calling..." 
                                 else "Call Ended"
                val headerIcon = if (type.contains("Incoming", ignoreCase = true)) Icons.Default.CallReceived
                                 else if (type.contains("Outgoing", ignoreCase = true)) Icons.Default.CallMade
                                 else Icons.Default.CallEnd
                                 
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.8f) // Dimmed background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = headerIcon,
                                        contentDescription = null,
                                        tint = headerColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = headerText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = headerColor
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Profile Icon
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = PrimaryBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Name & Phone
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Stats (Only for ended calls)
                                if (isCallEnded) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${CallLogRepository.formatDuration(duration)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                                
                                // Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    // Close Button
                                    OutlinedButton(
                                        onClick = { finish() },
                                        shape = CircleShape,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Close")
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    // Open Profile Button
                                    Button(
                                        onClick = { 
                                            val intent = Intent(this@PostCallActivity, MainActivity::class.java).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                putExtra("open_person_id", personId)
                                                putExtra("action", "open_profile")
                                            }
                                            startActivity(intent)
                                            finish()
                                        },
                                        shape = CircleShape,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                    ) {
                                        Text("View Profile")
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Quick Actions Row - Only show if call ended (don't show "Call Back" during call)
                                if (isCallEnded) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val dialPhone = CallLogRepository.formatPhoneForDialer(phone, defaultCountry)
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialPhone"))
                                                    startActivity(intent)
                                                } catch (e: Exception) { }
                                            }
                                        ) {
                                            Icon(Icons.Default.Call, "Call Back", tint = PrimaryBlue)
                                        }
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        // WhatsApp
                                        IconButton(
                                            onClick = {
                                                try {
                                                    lifecycleScope.launch {
                                                        val userPrefs = UserPreferencesRepository(this@PostCallActivity)
                                                        val defaultWhatsAppPackage = userPrefs.defaultWhatsAppPackage.first()
                                                        startActivity(CallLogRepository.createWhatsAppChooserIntent(this@PostCallActivity, phone, defaultWhatsAppPackage))
                                                    }
                                                } catch (e: Exception) { }
                                            }
                                        ) {
                                            Icon(Icons.Default.Message, "WhatsApp", tint = Color(0xFF25D366))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
