@file:Suppress("DEPRECATION")
package com.clicktoearn.linkbox.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

// Premium gradient colors
private val GradientBlue = Color(0xFF667EEA)
private val GradientPurple = Color(0xFF764BA2)
private val GradientPink = Color(0xFFFF6B9D)
private val GradientOrange = Color(0xFFFFA06C)

// Google brand colors
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun LoginRequiredSheet(
    onDismiss: () -> Unit,
    viewModel: LinkBoxViewModel,
    sheetState: SheetState,
    title: String = "Unlock Full Access",
    message: String = "Sign in to get the most out of LinkBox."
) {
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showTraditionalSignIn by remember { mutableStateOf(false) }
    var triggerOneTap by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    // Animation for floating orbs
    val infiniteTransition = rememberInfiniteTransition(label = "orb_animation")
    val orbOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )
    val orbOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2"
    )

    // Traditional Google Sign-In launcher (fallback)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            isLoading = true
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.signInWithGoogle(credential) { success, errorMessage ->
                        isLoading = false
                        if (success) {
                            onDismiss()
                        } else {
                            error = errorMessage ?: "Google authentication failed."
                        }
                    }
                } else {
                    isLoading = false
                    error = "Could not get ID Token."
                }
            } catch (e: ApiException) {
                isLoading = false
                error = "Sign-in failed: ${e.statusCode}"
                android.widget.Toast.makeText(context, "Sign-in Error (${e.statusCode}). Check SHA-1 configuration.", android.widget.Toast.LENGTH_LONG).show()
                android.util.Log.e("LoginSheet", "Google Sign-In Error: ${e.statusCode}", e)
            }
        } else {
            isLoading = false
        }
    }

    // Google One Tap Sign-In
    GoogleOneTapSignIn(
        viewModel = viewModel,
        onSuccess = {
            isLoading = false
            onDismiss()
        },
        onError = { errorMessage ->
            isLoading = false
            error = errorMessage
            showTraditionalSignIn = true
        },
        onDismiss = {
            showTraditionalSignIn = true
            isLoading = false
        },
        trigger = triggerOneTap
    )

    // Auto-trigger One Tap when sheet opens
    LaunchedEffect(Unit) {
        triggerOneTap = true
        isLoading = true
    }

    // Safety watchdog
    LaunchedEffect(isLoading, showTraditionalSignIn) {
        if (isLoading && !showTraditionalSignIn) {
            kotlinx.coroutines.delay(10000L)
            if (isLoading && !showTraditionalSignIn) {
                android.util.Log.w("LoginSheet", "One Tap timed out or ignored, showing fallback button")
                showTraditionalSignIn = true
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = if (isDarkTheme) Color(0xFF1A1A2E) else Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated background gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color(0xFF1E3A5F),
                                        Color(0xFF2D1B4E),
                                        Color(0xFF1A1A2E)
                                    )
                                } else {
                                    listOf(
                                        GradientBlue.copy(alpha = 0.8f),
                                        GradientPurple.copy(alpha = 0.7f),
                                        Color.White
                                    )
                                },
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                )

                // Floating decorative orbs
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .offset(x = (-20).dp + orbOffset1.dp, y = 30.dp + orbOffset2.dp)
                        .blur(40.dp)
                        .background(
                            color = if (isDarkTheme) GradientPink.copy(alpha = 0.3f)
                            else GradientPink.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 30.dp + orbOffset2.dp, y = 20.dp + orbOffset1.dp)
                        .blur(35.dp)
                        .background(
                            color = if (isDarkTheme) GradientOrange.copy(alpha = 0.3f)
                            else GradientOrange.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-10).dp + orbOffset1.dp)
                        .blur(25.dp)
                        .background(
                            color = if (isDarkTheme) GradientBlue.copy(alpha = 0.4f)
                            else GradientBlue.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )

                // Content overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated lock icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Main content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 200.dp)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top spacing
                Spacer(modifier = Modifier.height(24.dp))

                // Error message
                if (error != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Loading state during One Tap
                if (isLoading && !showTraditionalSignIn) {
                    PremiumLoadingIndicator()
                }

                // Google Sign-in button (above features)
                if (showTraditionalSignIn) {
                    GoogleSignInButton(
                        onClick = {
                            isLoading = true
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(context.getString(com.clicktoearn.linkbox.R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        isLoading = isLoading,
                        isDarkTheme = isDarkTheme
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider with "or sign in to unlock" text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) 
                        else Color.Gray.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "What you'll get",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) 
                        else Color.Gray.copy(alpha = 0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Feature cards with glassmorphism effect
                ModernFeatureCard(
                    icon = Icons.Outlined.History,
                    title = "Save History",
                    description = "Keep your links forever",
                    gradientColors = listOf(
                        Color(0xFF667EEA).copy(alpha = 0.1f),
                        Color(0xFF764BA2).copy(alpha = 0.05f)
                    ),
                    iconTint = GradientBlue,
                    isDarkTheme = isDarkTheme,
                    delayMillis = 0
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernFeatureCard(
                    icon = Icons.Outlined.CloudSync,
                    title = "Cloud Sync",
                    description = "Access from any device",
                    gradientColors = listOf(
                        Color(0xFF11998E).copy(alpha = 0.1f),
                        Color(0xFF38EF7D).copy(alpha = 0.05f)
                    ),
                    iconTint = Color(0xFF11998E),
                    isDarkTheme = isDarkTheme,
                    delayMillis = 100
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModernFeatureCard(
                    icon = Icons.Outlined.WorkspacePremium,
                    title = "Premium Features",
                    description = "Unlock exclusive rewards",
                    gradientColors = listOf(
                        Color(0xFFFF6B9D).copy(alpha = 0.1f),
                        Color(0xFFFFA06C).copy(alpha = 0.05f)
                    ),
                    iconTint = GradientPink,
                    isDarkTheme = isDarkTheme,
                    delayMillis = 200
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Maybe Later button at the very end
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        "Maybe Later",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ModernFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradientColors: List<Color>,
    iconTint: Color,
    isDarkTheme: Boolean,
    delayMillis: Int
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMillis.toLong())
        visible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "card_alpha"
    )
    val translateY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "card_translate"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = translateY
            },
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.White,
        shadowElevation = if (isDarkTheme) 0.dp else 2.dp,
        border = if (isDarkTheme) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.1f)
            )
        } else null
    ) {
        Box(
            modifier = Modifier.background(
                Brush.horizontalGradient(gradientColors)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = iconTint.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = iconTint
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isDarkTheme: Boolean
) {
    val buttonElevation by animateFloatAsState(
        targetValue = if (isLoading) 0f else 4f,
        animationSpec = tween(200),
        label = "button_elevation"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDarkTheme) Color(0xFF2A2A3D) else Color.White,
            contentColor = if (isDarkTheme) Color.White else Color(0xFF1F1F1F),
            disabledContainerColor = if (isDarkTheme) Color(0xFF2A2A3D).copy(alpha = 0.6f)
            else Color.White.copy(alpha = 0.8f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = buttonElevation.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp
        ),
        border = if (isDarkTheme) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.2f)
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFFDDDDDD)
            )
        }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = if (isDarkTheme) Color.White else GoogleBlue,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Connecting...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        } else {
            // Google "G" logo using canvas
            GoogleLogo(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                "Continue with Google",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val size = this.size.minDimension
        val strokeWidth = size * 0.2f
        val center = Offset(size / 2, size / 2)

        // Draw the colored arcs of the Google "G"
        // Blue (top right)
        drawArc(
            color = GoogleBlue,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(size - strokeWidth, size - strokeWidth),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Green (bottom right)
        drawArc(
            color = GoogleGreen,
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(size - strokeWidth, size - strokeWidth),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Yellow (bottom left)
        drawArc(
            color = GoogleYellow,
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(size - strokeWidth, size - strokeWidth),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Red (top left)
        drawArc(
            color = GoogleRed,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(size - strokeWidth, size - strokeWidth),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        // Blue horizontal bar
        drawLine(
            color = GoogleBlue,
            start = Offset(center.x, center.y - strokeWidth / 2),
            end = Offset(size - strokeWidth / 2, center.y - strokeWidth / 2),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
private fun PremiumLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { rotationZ = rotation },
                color = GradientBlue,
                strokeWidth = 3.dp,
                trackColor = GradientPurple.copy(alpha = 0.2f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Signing you in...",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = GradientBlue
        )
    }
}
