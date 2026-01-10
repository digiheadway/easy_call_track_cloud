package com.miniclickcrm.deviceadmin3

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.lifecycleScope
import com.miniclickcrm.deviceadmin3.api.RetrofitClient
import com.miniclickcrm.deviceadmin3.manager.DeviceManager
import com.miniclickcrm.deviceadmin3.manager.PermissionItem
import com.miniclickcrm.deviceadmin3.manager.PermissionManager
import com.miniclickcrm.deviceadmin3.service.MainService
import com.miniclickcrm.deviceadmin3.ui.theme.DeviceAdmin3Theme
import com.miniclickcrm.deviceadmin3.utils.FcmTokenManager
import com.miniclickcrm.deviceadmin3.utils.DeviceInfoManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if device is locked
        val deviceManager = DeviceManager(this)
        if (deviceManager.isLocked()) {
            val intent = Intent(this, LockScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
            return
        }

        // Handle Device Owner provisioning extras
        val adminExtras = intent.getBundleExtra(android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
        if (adminExtras != null) {
            val pairingCode = adminExtras.getString("pairing_code")
            if (!pairingCode.isNullOrEmpty()) {
                val prefs = getSharedPreferences("device_admin_prefs", android.content.Context.MODE_PRIVATE)
                if (prefs.getString("pairing_code", null) == null) {
                    prefs.edit().putString("pairing_code", pairingCode).apply()
                    android.util.Log.d("MainActivity", "Pairing code from provisioning: $pairingCode")
                }
            }
        }

        enableEdgeToEdge()
        permissionManager = PermissionManager(this)
        
        setContent {
            DeviceAdmin3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProtectionDashboard(permissionManager)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Check if device is locked
        val deviceManager = DeviceManager(this)
        if (deviceManager.isLocked()) {
            val intent = Intent(this, LockScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
            return
        }

        // Refresh permissions state when returning from settings
        setContent {
            DeviceAdmin3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProtectionDashboard(permissionManager)
                }
            }
        }
    }
}

// Colors
private val GradientStart = Color(0xFF1A1A2E)
private val GradientEnd = Color(0xFF16213E)
private val AccentBlue = Color(0xFF0F4C75)
private val AccentCyan = Color(0xFF3282B8)
private val SuccessGreen = Color(0xFF00C853)
private val WarningOrange = Color(0xFFFF9800)
private val ErrorRed = Color(0xFFE53935)
private val SurfaceCard = Color(0xFF1F2937)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionDashboard(permissionManager: PermissionManager) {
    val context = LocalContext.current
    val activity = context as? Activity
    val deviceManager = remember { DeviceManager(context) }
    val prefs = remember { context.getSharedPreferences("device_admin_prefs", android.content.Context.MODE_PRIVATE) }
    
    // States
    var permissionItems by remember { mutableStateOf(permissionManager.getAllPermissionItems()) }
    var allPermissionsGranted by remember { mutableStateOf(permissionManager.areAllPermissionsGranted()) }
    var isPaired by remember { mutableStateOf(prefs.getString("pairing_code", null) != null) }
    var pairingCode by remember { mutableStateOf(prefs.getString("pairing_code", "") ?: "") }
    var isProtectionEnabled by remember { mutableStateOf(prefs.getBoolean("is_protected", false)) }
    var isLoading by remember { mutableStateOf(false) }

    // Sync state with preferences
    LaunchedEffect(Unit) {
        while(true) {
            isPaired = prefs.getString("pairing_code", null) != null
            isProtectionEnabled = prefs.getBoolean("is_protected", false)
            pairingCode = prefs.getString("pairing_code", "") ?: ""
            delay(2000) 
        }
    }
    
    // Bottom Sheet States
    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showPairingSheet by remember { mutableStateOf(false) }
    var showMasterPinSheet by remember { mutableStateOf(false) }
    var masterPinInput by remember { mutableStateOf("") }
    var pairingError by remember { mutableStateOf<String?>(null) }
    
    // Auto-Setup via IMEI
    LaunchedEffect(allPermissionsGranted, isPaired) {
        if (allPermissionsGranted && !isPaired && !isLoading) {
             val deviceInfo = DeviceInfoManager(context)
             val (imei1, imei2) = deviceInfo.getImeis()
             
             if (imei1 != null) {
                 // Only attempt if we have IMEI permissions and data
                 try {
                     // Only run this once or debounced ideally, but here we depend on !isPaired
                     val fcmToken = FcmTokenManager.getTokenSync(context)
                     val response = RetrofitClient.apiService.checkStatus(
                         pairingCode = "AUTO_CHECK_IMEI", 
                         fcmToken = fcmToken,
                         imei = imei1,
                         imei2 = imei2,
                         deviceName = deviceInfo.getDeviceName(),
                         deviceModel = deviceInfo.getDeviceModel()
                     )
                     
                     if (response.success && !response.pairingcode.isNullOrEmpty()) {
                         // Found existing pairing!
                          val saved = prefs.edit()
                                .putString("pairing_code", response.pairingcode)
                                .putString("device_message", response.data?.message)
                                .commit()
                          if (saved) {
                              pairingCode = response.pairingcode
                              isPaired = true
                              isProtectionEnabled = response.data?.is_protected == true
                              deviceManager.setProtected(isProtectionEnabled)
                              
                              Toast.makeText(context, "Device Auto-Restored Successfully!", Toast.LENGTH_LONG).show()
                          }
                     }
                 } catch(e: Exception) {
                     // Silent fail for auto-check
                 }
             }
        }
    }
    
    // Refresh permissions periodically
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            permissionItems = permissionManager.getAllPermissionItems()
            allPermissionsGranted = permissionManager.areAllPermissionsGranted()
        }
    }

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
                .padding(20.dp)
                .statusBarsPadding()
        ) {
            // Header
            HeaderSection(isProtectionEnabled, isPaired)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Permission Status Card
            PermissionStatusCard(
                permissionManager = permissionManager,
                allGranted = allPermissionsGranted,
                onClick = { showPermissionsSheet = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Pairing Status Card
            PairingStatusCard(
                isPaired = isPaired,
                pairingCode = pairingCode,
                enabled = allPermissionsGranted,
                onClick = { if (!isPaired && allPermissionsGranted) showPairingSheet = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Protection Toggle Card
            ProtectionToggleCard(
                isEnabled = isProtectionEnabled,
                // If already enabled, allow clicking to turn OFF (PIN check will handle auth)
                // If not enabled, require permissions and pairing to turn ON
                enabled = if (isProtectionEnabled) true else (allPermissionsGranted && isPaired),
                onToggle = {
                    if (!isProtectionEnabled) {
                        // Turning ON - just enable
                        isProtectionEnabled = true
                        deviceManager.setProtected(true)
                        
                        // Start protection service
                        MainService.start(context)
                        
                        Toast.makeText(context, "Protection Activated!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Turning OFF - check if unlocked on server
                        val isLockedOnServer = prefs.getBoolean("is_freezed", false)
                        val isProtectedOnServer = prefs.getBoolean("is_protected", false)
                        
                        if (isLockedOnServer || isProtectedOnServer) {
                            // Still locked on server -> ask master PIN
                            showMasterPinSheet = true
                        } else {
                            // Unlocked on server -> just disable
                            isProtectionEnabled = false
                            deviceManager.setProtected(false)
                            Toast.makeText(context, "Protection Disabled", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Status Footer
            StatusFooter(
                permissionsGranted = allPermissionsGranted,
                isPaired = isPaired,
                isProtected = isProtectionEnabled
            )
        }
    }

    // Permissions Bottom Sheet
    if (showPermissionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPermissionsSheet = false },
            containerColor = SurfaceCard,
            contentColor = Color.White
        ) {
            PermissionsBottomSheet(
                permissionItems = permissionItems,
                onGrantPermission = { permissionId ->
                    activity?.let { permissionManager.requestPermissionById(it, permissionId) }
                },
                onDismiss = { showPermissionsSheet = false }
            )
        }
    }

    // Pairing Bottom Sheet
    if (showPairingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPairingSheet = false },
            containerColor = SurfaceCard,
            contentColor = Color.White
        ) {
            PairingBottomSheet(
                isLoading = isLoading,
                error = pairingError,
                onPair = { code ->
                    isLoading = true
                    pairingError = null
                    
                    (context as? MainActivity)?.lifecycleScope?.launch {
                        try {
                            // Get FCM token first
                            val fcmToken = FcmTokenManager.getTokenSync(context)
                             val deviceInfo = DeviceInfoManager(context)
                             val (imei1, imei2) = deviceInfo.getImeis()
                            
                            val response = RetrofitClient.apiService.checkStatus(
                                pairingCode = code, 
                                fcmToken = fcmToken,
                                imei = imei1,
                                imei2 = imei2,
                                deviceName = deviceInfo.getDeviceName(),
                                deviceModel = deviceInfo.getDeviceModel()
                            )
                            
                            if (response.success && response.data != null) {
                                // Save pairing code using commit() for immediate, reliable save
                                // This is important for MIUI devices where apply() may not persist
                                val saved = prefs.edit()
                                    .putString("pairing_code", code)
                                    .putString("device_message", response.data.message)
                                    .commit()
                                
                                if (saved) {
                                    pairingCode = code
                                    isPaired = true
                                    showPairingSheet = false
                                    
                                    // If response indicates protection, enable it
                                    if (response.data.is_protected) {
                                         isProtectionEnabled = true
                                         deviceManager.setProtected(true)
                                         MainService.start(context)
                                    }

                                    Toast.makeText(context, "Device Paired Successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    pairingError = "Failed to save pairing code. Please try again."
                                }
                            } else {
                                pairingError = response.error ?: "Invalid pairing code"
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Pairing error", e)
                            pairingError = "Connection error: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                onDismiss = { showPairingSheet = false }
            )
        }
    }



    // Master PIN Bottom Sheet
    if (showMasterPinSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showMasterPinSheet = false
                masterPinInput = ""
            },
            containerColor = SurfaceCard,
            contentColor = Color.White
        ) {
            MasterPinBottomSheet(
                pinInput = masterPinInput,
                onPinChange = { masterPinInput = it },
                onVerify = {
                    val securityManager = com.miniclickcrm.deviceadmin3.manager.SecurityManager(context)
                    
                    if (securityManager.isMasterCode(masterPinInput) || 
                        securityManager.verifyCode(masterPinInput) ||
                        masterPinInput == prefs.getString("master_pin", "000000")) {
                        isProtectionEnabled = false
                        deviceManager.setProtected(false)
                        
                        showMasterPinSheet = false
                        masterPinInput = ""
                        
                        Toast.makeText(context, "Protection Disabled", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid Master PIN", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { 
                    showMasterPinSheet = false 
                    masterPinInput = ""
                }
            )
        }
    }
}

@Composable
fun HeaderSection(isProtected: Boolean, isPaired: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isProtected) SuccessGreen.copy(alpha = 0.2f)
                    else AccentBlue.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isProtected) Icons.Filled.Lock else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (isProtected) SuccessGreen else AccentCyan,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = "Device Protection",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = if (isProtected) "Protection Active" 
                       else if (isPaired) "Ready to Activate"
                       else "Setup Required",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PermissionStatusCard(
    permissionManager: PermissionManager,
    allGranted: Boolean,
    onClick: () -> Unit
) {
    val (granted, total) = permissionManager.getGrantedPermissionsCount()
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (allGranted) SuccessGreen.copy(alpha = 0.2f)
                        else WarningOrange.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (allGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (allGranted) SuccessGreen else WarningOrange,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permissions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = if (allGranted) "All permissions granted" 
                           else "$granted of $total permissions granted",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
        
        if (!allGranted) {
            LinearProgressIndicator(
                progress = { granted.toFloat() / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = WarningOrange,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun PairingStatusCard(
    isPaired: Boolean,
    pairingCode: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled && !isPaired,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCard,
            disabledContainerColor = SurfaceCard.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isPaired -> SuccessGreen.copy(alpha = 0.2f)
                            enabled -> AccentCyan.copy(alpha = 0.2f)
                            else -> Color.Gray.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPaired) Icons.Filled.Done else Icons.Outlined.Add,
                    contentDescription = null,
                    tint = when {
                        isPaired -> SuccessGreen
                        enabled -> AccentCyan
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Device Pairing",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = when {
                        isPaired -> "Paired: $pairingCode"
                        enabled -> "Tap to enter pairing code"
                        else -> "Grant permissions first"
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            if (!isPaired && enabled) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            } else if (isPaired) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SuccessGreen.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ProtectionToggleCard(
    isEnabled: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEnabled) "DEVICE PROTECTED" else "PROTECTION INACTIVE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) SuccessGreen else Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = { onToggle(!isEnabled) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) ErrorRed else SuccessGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnabled) "Turn Off Protection" else "Turn On Protection",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun StatusFooter(
    permissionsGranted: Boolean,
    isPaired: Boolean,
    isProtected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatusIndicator(
            icon = Icons.Default.Check,
            label = "Permissions",
            isActive = permissionsGranted
        )
        StatusIndicator(
            icon = Icons.Default.Done,
            label = "Paired",
            isActive = isPaired
        )
        StatusIndicator(
            icon = Icons.Default.Lock,
            label = "Protected",
            isActive = isProtected
        )
    }
}

@Composable
fun StatusIndicator(
    icon: ImageVector,
    label: String,
    isActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) SuccessGreen.copy(alpha = 0.2f)
                    else Color.Gray.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) SuccessGreen else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// Bottom Sheet Contents
@Composable
fun PermissionsBottomSheet(
    permissionItems: List<PermissionItem>,
    onGrantPermission: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Permissions Required",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Grant all permissions to enable device protection features.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(permissionItems) { item ->
                PermissionItemRow(
                    item = item,
                    onGrant = { onGrantPermission(item.id) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Done", color = AccentCyan)
        }
    }
}

@Composable
fun PermissionItemRow(
    item: PermissionItem,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (item.isGranted) SuccessGreen.copy(alpha = 0.2f)
                    else ErrorRed.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isGranted) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = if (item.isGranted) SuccessGreen else ErrorRed,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = item.description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        
        if (!item.isGranted) {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PairingBottomSheet(
    isLoading: Boolean,
    error: String?,
    onPair: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    
    // Track if pair was triggered to prevent double-tap
    var pairTriggered by remember { mutableStateOf(false) }
    
    fun triggerPair() {
        if (codeInput.isNotBlank() && !isLoading && !pairTriggered) {
            pairTriggered = true
            focusManager.clearFocus()
            onPair(codeInput.trim())
        }
    }
    
    // Reset pairTriggered when loading completes or error occurs
    LaunchedEffect(isLoading, error) {
        if (!isLoading) {
            pairTriggered = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Enter Pairing Code",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Enter the pairing code provided by your administrator to link this device.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = codeInput,
            onValueChange = { codeInput = it.uppercase().trim() },
            label = { Text("Pairing Code") },
            placeholder = { Text("e.g., ABC123") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            isError = error != null,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                autoCorrect = false
            ),
            keyboardActions = KeyboardActions(
                onDone = { triggerPair() }
            ),
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
        
        if (error != null) {
            Text(
                text = error,
                fontSize = 12.sp,
                color = ErrorRed,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Use Box with explicit clickable for better MIUI compatibility
            val buttonEnabled = codeInput.isNotBlank() && !isLoading && !pairTriggered
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (buttonEnabled) AccentCyan else AccentCyan.copy(alpha = 0.5f)
                    )
                    .clickable(
                        enabled = buttonEnabled,
                        interactionSource = interactionSource,
                        indication = androidx.compose.material.ripple.rememberRipple(
                            bounded = true,
                            color = Color.White
                        ),
                        onClick = { triggerPair() }
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Pair Device",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MasterPinBottomSheet(
    pinInput: String,
    onPinChange: (String) -> Unit,
    onVerify: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Master PIN Required",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Enter the master PIN to disable protection. Contact your administrator if you don't have the PIN.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = pinInput,
            onValueChange = { if (it.length <= 6) onPinChange(it) },
            label = { Text("Master PIN") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = onVerify,
                enabled = pinInput.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
            ) {
                Text("Disable Protection")
            }
        }
    }
}