package com.clicktoearn.linkbox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Premium gold/amber gradient colors
private val GoldLight = Color(0xFFFFD700)
private val GoldDark = Color(0xFFD4AF37)
private val AmberGlow = Color(0xFFFFA000)
private val PremiumPurple = Color(0xFF7C4DFF)
private val PremiumBlue = Color(0xFF448AFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSubscriptionSheet(
    onDismiss: () -> Unit,
    onSubscribe: (Boolean) -> Unit,
    currentPoints: Int = 0
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDarkTheme = isSystemInDarkTheme()
    val requiredPoints = 2000
    val hasEnoughPoints = currentPoints >= requiredPoints

    // Animation for floating orbs
    val infiniteTransition = rememberInfiniteTransition(label = "orb_animation")
    val orbOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )
    val orbOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated background gradient header with gold/black premium theme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Gradient background - Black to Gold gradient for premium feel
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color(0xFF1A1512),
                                        Color(0xFF2D2419),
                                        Color(0xFF1A1A1A)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF2D2419),
                                        Color(0xFF4A3F2F),
                                        Color.White
                                    )
                                },
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                )

                // Floating decorative orbs with gold colors
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .offset(x = (-15).dp + orbOffset1.dp, y = 40.dp + orbOffset2.dp)
                        .blur(35.dp)
                        .background(
                            color = GoldLight.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 25.dp + orbOffset2.dp, y = 25.dp + orbOffset1.dp)
                        .blur(30.dp)
                        .background(
                            color = AmberGlow.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-5).dp + orbOffset1.dp)
                        .blur(20.dp)
                        .background(
                            color = GoldDark.copy(alpha = 0.5f),
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
                    // Premium crown/star icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GoldLight, GoldDark)
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.5f),
                                        GoldLight.copy(alpha = 0.3f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF2D2419)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Go Premium",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Unlock 7 days of ad-free experience",
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
                Spacer(modifier = Modifier.height(24.dp))

                // Feature benefits
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
                        text = "What's included",
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

                // Premium benefits cards
                PremiumBenefitCard(
                    icon = Icons.Outlined.Block,
                    title = "No Ads",
                    description = "Complete ad-free experience",
                    gradientColors = listOf(
                        GoldLight.copy(alpha = 0.15f),
                        GoldDark.copy(alpha = 0.05f)
                    ),
                    iconTint = GoldLight,
                    isDarkTheme = isDarkTheme,
                    delayMillis = 0
                )

                Spacer(modifier = Modifier.height(12.dp))

                PremiumBenefitCard(
                    icon = Icons.Outlined.Speed,
                    title = "Faster Loading",
                    description = "Priority content delivery",
                    gradientColors = listOf(
                        PremiumPurple.copy(alpha = 0.12f),
                        PremiumBlue.copy(alpha = 0.05f)
                    ),
                    iconTint = PremiumPurple,
                    isDarkTheme = isDarkTheme,
                    delayMillis = 100
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Purchase options
                Text(
                    text = "Choose your payment method",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Points option
                PurchaseOptionCard(
                    title = "Pay with Points",
                    subtitle = if (hasEnoughPoints) "You have $currentPoints pts" else "Need ${requiredPoints - currentPoints} more pts",
                    price = "2000 Pts",
                    icon = Icons.Filled.Stars,
                    iconTint = GoldLight,
                    isEnabled = hasEnoughPoints,
                    isPrimary = true,
                    isDarkTheme = isDarkTheme,
                    onClick = { onSubscribe(true) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Money option
                PurchaseOptionCard(
                    title = "Pay with Money",
                    subtitle = "Support the development",
                    price = "₹ 199",
                    icon = Icons.Filled.Bolt,
                    iconTint = PremiumPurple,
                    isEnabled = true,
                    isPrimary = false,
                    isDarkTheme = isDarkTheme,
                    onClick = { onSubscribe(false) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Maybe later button
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
private fun PremiumBenefitCard(
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
private fun PurchaseOptionCard(
    title: String,
    subtitle: String,
    price: String,
    icon: ImageVector,
    iconTint: Color,
    isEnabled: Boolean,
    isPrimary: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        !isEnabled -> if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Gray.copy(alpha = 0.1f)
        isPrimary -> if (isDarkTheme) Color(0xFF2D2419) else Color(0xFFFFF8E1)
        else -> if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.White
    }

    val borderColor = when {
        !isEnabled -> Color.Transparent
        isPrimary -> GoldLight.copy(alpha = 0.5f)
        else -> if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.2f)
    }

    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = iconTint.copy(alpha = if (isEnabled) 0.15f else 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isEnabled) iconTint else iconTint.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) {
                        if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
                    } else {
                        if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Gray
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) {
                        if (isDarkTheme) Color.White.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.7f)
                    }
                )
            }

            // Price badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isEnabled && isPrimary) {
                    Brush.linearGradient(listOf(GoldLight, GoldDark)).let { Color(0xFFFFD700) }
                } else if (isEnabled) {
                    if (isDarkTheme) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                } else {
                    if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Gray.copy(alpha = 0.15f)
                }
            ) {
                Text(
                    text = price,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled && isPrimary) {
                        Color(0xFF2D2419)
                    } else if (isEnabled) {
                        if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                    } else {
                        if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Gray
                    }
                )
            }
        }
    }
}
