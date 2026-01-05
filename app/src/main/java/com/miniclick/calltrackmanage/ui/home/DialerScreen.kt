package com.miniclick.calltrackmanage.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DialerScreen() {
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current
    
    // Check if app is default dialer
    val isDefaultDialer = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            telecomManager.defaultDialerPackage == context.packageName
        } else {
            false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar info
        if (!isDefaultDialer) {
            Surface(
                onClick = {
                    try {
                        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
                            roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                        } else {
                            Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                            }
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Set as default dialer", style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Phone Number Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = if (phoneNumber.length > 10) 32.sp else 40.sp,
                        fontWeight = FontWeight.Light
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (phoneNumber.isNotEmpty()) {
                    Text(
                        text = "Add to contacts",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, phoneNumber)
                                }
                                context.startActivity(intent)
                            },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Dial Pad
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(keys) { key ->
                DialKey(
                    text = key,
                    onClick = { if (phoneNumber.length < 15) phoneNumber += key },
                    onLongClick = {
                        if (key == "0" && !phoneNumber.contains("+")) {
                            phoneNumber = "+" + phoneNumber
                        }
                    }
                )
            }
        }

        // Action Buttons (Call, Backspace)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* Could add more dialer actions here */ },
                modifier = Modifier.size(64.dp)
            ) {
                // Empty placeholder for symmetry or history icon
            }

            FloatingActionButton(
                onClick = {
                    if (phoneNumber.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        context.startActivity(intent)
                    }
                },
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                modifier = Modifier.size(72.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(
                onClick = {
                    if (phoneNumber.isNotEmpty()) {
                        phoneNumber = phoneNumber.dropLast(1)
                    }
                },
                modifier = Modifier.size(64.dp)
            ) {
                if (phoneNumber.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DialKey(text: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Normal
                )
            )
            // Optional: Subtext for letters (like in real dialers)
            val subtext = when (text) {
                "2" -> "ABC"
                "3" -> "DEF"
                "4" -> "GHI"
                "5" -> "JKL"
                "6" -> "MNO"
                "7" -> "PQRS"
                "8" -> "TUV"
                "9" -> "WXYZ"
                else -> ""
            }
            if (subtext.isNotEmpty()) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
