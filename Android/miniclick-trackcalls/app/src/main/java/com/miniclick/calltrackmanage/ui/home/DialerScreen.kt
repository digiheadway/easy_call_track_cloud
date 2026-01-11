package com.miniclick.calltrackmanage.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
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

import androidx.compose.material.icons.filled.Close

@Composable
fun DialerScreen(
    initialNumber: String = "",
    onIdentifyCallHistory: () -> Unit, 
    onClose: () -> Unit = {},
    viewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    androidx.activity.compose.BackHandler(onBack = onClose)

    var phoneNumber by remember(initialNumber) { mutableStateOf(initialNumber) }
    val context = LocalContext.current
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isDefaultDialer by remember { mutableStateOf(false) }
    
    // Launcher for Default Dialer Role
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            isDefaultDialer = telecomManager.defaultDialerPackage == context.packageName
        }
    }

    // Launcher for Call Permission
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && phoneNumber.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        } else if (phoneNumber.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            context.startActivity(intent)
        }
    }

    // Check status whenever the screen is resumed
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                    isDefaultDialer = telecomManager.defaultDialerPackage == context.packageName
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Row: Default Dialer Warning + Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Default Dialer Warning (if needed)
            if (!isDefaultDialer) {
                Surface(
                    onClick = {
                        val packageName = context.packageName
                        try {
                            var intentLaunched = false
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                try {
                                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
                                    if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_DIALER)) {
                                        val intent = roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_DIALER)
                                        roleLauncher.launch(intent)
                                        intentLaunched = true
                                    }
                                } catch (e: Exception) {
                                    // Fallback to older method if RoleManager fails
                                    android.util.Log.e("DialerScreen", "RoleManager failed", e)
                                }
                            }
                            
                            if (!intentLaunched && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                try {
                                    val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                        putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Failed to launch settings: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            } else if (!intentLaunched) {
                                 android.widget.Toast.makeText(context, "Default dialer not supported on only this Android version", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Set as default dialer", style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Close Button
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        // Phone Number Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = if (phoneNumber.length > 10) 28.sp else 36.sp,
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
                            .padding(top = 4.dp)
                            .clickable {
                                val intent = Intent(Intent.ACTION_INSERT).apply {
                                    type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
                                    putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, phoneNumber)
                                }
                                context.startActivity(intent)
                            },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Dial Pad
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onIdentifyCallHistory,
                modifier = Modifier.size(56.dp)
            ) {
                 Icon(
                     imageVector = Icons.Default.History,
                     contentDescription = "History",
                     tint = MaterialTheme.colorScheme.onSurfaceVariant,
                     modifier = Modifier.size(28.dp)
                 )
            }

            FloatingActionButton(
                onClick = {
                    if (phoneNumber.isNotEmpty()) {
                        viewModel.initiateCall(phoneNumber)
                    }
                },
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White,
                modifier = Modifier.size(64.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Call, "Call", modifier = Modifier.size(28.dp))
            }

            IconButton(
                onClick = {
                    if (phoneNumber.isNotEmpty()) {
                        phoneNumber = phoneNumber.dropLast(1)
                    }
                },
                modifier = Modifier.size(56.dp)
            ) {
                if (phoneNumber.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, "Backspace", modifier = Modifier.size(24.dp))
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = text, style = MaterialTheme.typography.headlineSmall)
            val subtext = when (text) {
                "2" -> "ABC"; "3" -> "DEF"; "4" -> "GHI"; "5" -> "JKL"; "6" -> "MNO"; "7" -> "PQRS"; "8" -> "TUV"; "9" -> "WXYZ"; else -> ""
            }
            if (subtext.isNotEmpty()) {
                Text(text = subtext, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}
