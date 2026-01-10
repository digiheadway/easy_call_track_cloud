package com.miniclickcrm.deviceadmin3

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.miniclickcrm.deviceadmin3.manager.SecurityManager
import com.miniclickcrm.deviceadmin3.ui.theme.DeviceAdmin3Theme
import androidx.lifecycle.lifecycleScope
import com.miniclickcrm.deviceadmin3.api.RetrofitClient
import com.miniclickcrm.deviceadmin3.manager.DeviceManager
import kotlinx.coroutines.launch
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LockScreenActivity : ComponentActivity() {
 
    private var unlockReceiver: android.content.BroadcastReceiver? = null
    private var isMakingCall = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make it full screen and show over lockscreen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        
        // Block interaction with status bar (pulling down)
        // Note: This requires SYSTEM_ALERT_WINDOW or being device owner
        
        val deviceManager = DeviceManager(this)
        if (deviceManager.isDeviceOwner()) {
            try {
                val dpm = getSystemService(android.content.Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                val adminComponent = android.content.ComponentName(this, com.miniclickcrm.deviceadmin3.receiver.MyAdminReceiver::class.java)
                
                // Whitelist our app and common dialers to allow calling while in Lock Task Mode
                val whitelistedPackages = arrayOf(
                    packageName,
                    "com.android.dialer",
                    "com.google.android.dialer",
                    "com.android.phone",
                    "com.android.server.telecom",
                    "com.samsung.android.dialer",
                    "com.samsung.android.incallui"
                )
                dpm.setLockTaskPackages(adminComponent, whitelistedPackages)
                startLockTask()
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Create a broadcast receiver to finish when unfrozen
        unlockReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                finish()
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, android.content.IntentFilter("com.miniclickcrm.deviceadmin3.UNLOCK_DEVICE"), android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(unlockReceiver, android.content.IntentFilter("com.miniclickcrm.deviceadmin3.UNLOCK_DEVICE"))
        }

        setContent {
            DeviceAdmin3Theme {
                LockScreenContent(
                    onUnlock = { finish() },
                    onMakeCall = { number -> makeCall(number) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset the flag so protection resumes if they return
        isMakingCall = false
        
        // Try to keep it in front
        val deviceManager = DeviceManager(this)
        if (deviceManager.isLocked() && deviceManager.isDeviceOwner()) {
            try {
                startLockTask()
            } catch (e: Exception) {}
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isMakingCall) return

        // Relaunch if we are supposed to be locked
        val deviceManager = DeviceManager(this)
        if (deviceManager.isLocked()) {
            val intent = Intent(this, LockScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            if (isMakingCall) return

            val deviceManager = DeviceManager(this)
            if (deviceManager.isLocked()) {
                // Relaunch if we lost focus while locked
                // (Note: Removed ACTION_CLOSE_SYSTEM_DIALOGS as it causes crashes on newer Androids)
                
                // Relaunch
                val intent = Intent(this, LockScreenActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }
    }

    // Override back button to prevent closing
    override fun onBackPressed() {
        // Do nothing
    }

    // Block certain keys (like volume) if needed
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true // Consume the event
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        unlockReceiver?.let { unregisterReceiver(it) }
    }

    private fun makeCall(phoneNumber: String) {
        try {
            val telUri = Uri.parse("tel:${phoneNumber.trim()}")
            val hasCallPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            // Set flag to allow leaving this activity
            isMakingCall = true

            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL, telUri)
            } else {
                Intent(Intent.ACTION_DIAL, telUri)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            isMakingCall = false
            Toast.makeText(this, "Could not open dialer", Toast.LENGTH_SHORT).show()
        }
    }
}

// Colors
private val GradientStart = Color(0xFF1A1A2E)
private val GradientEnd = Color(0xFF16213E)
private val AccentCyan = Color(0xFF3282B8)
private val SuccessGreen = Color(0xFF2ECC71)
private val ErrorRed = Color(0xFFE53935)
private val WarningYellow = Color(0xFFFFD93D)

@Composable
fun LockScreenContent(onUnlock: () -> Unit, onMakeCall: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("device_admin_prefs", android.content.Context.MODE_PRIVATE)
    val securityManager = remember { SecurityManager(context) }
    val deviceManager = remember { DeviceManager(context) }
    val scope = rememberCoroutineScope()
    
    var message by remember { mutableStateOf(prefs.getString("message", "Your device has been frozen due to loan non-compliance.") ?: "") }
    var amount by remember { mutableIntStateOf(prefs.getInt("amount", 0)) }
    var callTo by remember { mutableStateOf(prefs.getString("call_to", "") ?: "") }
    var pairingCode by remember { mutableStateOf(prefs.getString("pairing_code", "N/A") ?: "N/A") }
    
    var showPinInput by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Icon
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Text(
                text = "DEVICE FROZEN",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Pairing Code
            Text(
                text = "Pairing Code: $pairingCode",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Pending Amount (if > 0)
            if (amount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WarningYellow.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Pending Amount: ₹$amount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningYellow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Message
            Text(
                text = message,
                fontSize = 18.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Refresh Status Button
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.apiService.checkStatus(pairingCode)
                            }
                            if (response.success && response.data != null) {
                                prefs.edit().apply {
                                    putInt("amount", response.data.amount)
                                    putString("message", response.data.message)
                                    putBoolean("is_freezed", response.data.is_freezed)
                                    putString("call_to", response.data.call_to)
                                    apply()
                                }
                                
                                // Sync protection status
                                deviceManager.setProtected(response.data.is_protected)
                                
                                amount = response.data.amount
                                message = response.data.message
                                callTo = response.data.call_to ?: ""
                                
                                if (!response.data.is_freezed) {
                                    deviceManager.unfreezeDevice()
                                    onUnlock()
                                } else {
                                    Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REFRESH STATUS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Call Manager Button (if call_to is set)
            if (callTo.isNotEmpty()) {
                Button(
                    onClick = {
                        onMakeCall(callTo)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CALL $callTo", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Unlock Code Section
            TextButton(onClick = { showPinInput = !showPinInput }) {
                Text(
                    text = if (showPinInput) "Hide Unlock Code" else "Unlock Using Unlock Code",
                    color = AccentCyan,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
            
            if (showPinInput) {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6) pinInput = it },
                    label = { Text("Enter 6-Digit Code") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = AccentCyan,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = AccentCyan
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        val isMaster = securityManager.isMasterCode(pinInput)
                        val isSecurityCode = securityManager.verifyCode(pinInput)
                        val isRecovery = pinInput == securityManager.getRecoveryKey()

                        if (isMaster || isSecurityCode || isRecovery) {
                            deviceManager.unfreezeDevice()
                            deviceManager.setProtected(false)
                            onUnlock()
                            if (isMaster) {
                                Toast.makeText(context, "Super Admin Unlock Successful", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Device Unlocked & Protection Disabled", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Invalid code", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    enabled = pinInput.length >= 4
                ) {
                    Text("UNLOCK NOW", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
