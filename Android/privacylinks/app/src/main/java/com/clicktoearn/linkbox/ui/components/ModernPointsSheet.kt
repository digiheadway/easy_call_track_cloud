package com.clicktoearn.linkbox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel

// Reusing colors from previous designs
private val GradientBlue = Color(0xFF4facfe)
private val GradientPurple = Color(0xFF00f2fe)
private val PointsGold = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernPointsSheet(
    viewModel: LinkBoxViewModel,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    val userPoints by viewModel.userPoints.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    
    var showSharingRewards by remember { mutableStateOf(false) }
    var showSpendPoints by remember { mutableStateOf(false) }

    // Animation for floating orbs
    val infiniteTransition = rememberInfiniteTransition(label = "points_orb_animation")
    val orbOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = if (isDarkTheme) Color(0xFF121212) else Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Elegant Header with Points Balance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // Background Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                                } else {
                                    listOf(Color(0xFF6366F1), Color(0xFF818CF8))
                                }
                            )
                        )
                )

                // Decorative blurred circles
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .offset(x = (-30).dp + orbOffset1.dp, y = 20.dp)
                        .blur(40.dp)
                        .background(GradientPurple.copy(alpha = 0.3f), CircleShape)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = PointsGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "$userPoints",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Scrollable Content area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 190.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // 1. Watch Ad Section
                ModernActionCard(
                    title = "Earn Points",
                    description = "Watch a quick ad to get 5 points instantly",
                    icon = Icons.Default.VideoLibrary,
                    actionText = "+5 Points",
                    onClick = {
                        activity?.let { act ->
                            com.clicktoearn.linkbox.ads.AdsManager.showRewardedAd(act, strict = true, 
                                onRewardEarned = {
                                    viewModel.earnPoints(5)
                                },
                                onAdClosed = { message ->
                                    message?.let { viewModel.showMessage(it) }
                                }
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Buy Points Section
                Text(
                    text = "Refill Instantly",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModernPlanCard(
                        modifier = Modifier.weight(1f),
                        points = 100,
                        price = "19 Rs",
                        isPopular = false,
                        onClick = { activity?.let { viewModel.buyPoints(it, 100) } }
                    )
                    ModernPlanCard(
                        modifier = Modifier.weight(1f),
                        points = 350,
                        price = "49 Rs",
                        isPopular = true,
                        onClick = { activity?.let { viewModel.buyPoints(it, 350) } }
                    )
                    ModernPlanCard(
                        modifier = Modifier.weight(1f),
                        points = 1000,
                        price = "99 Rs",
                        isPopular = false,
                        onClick = { activity?.let { viewModel.buyPoints(it, 1000) } }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Earn by Sharing Section
                ModernInfoCard(
                    title = "Share & Earn",
                    description = "Invite friends or share links to earn even more points",
                    icon = Icons.Default.AutoGraph,
                    onClick = { showSharingRewards = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Spending Points Info
                ModernInfoCard(
                    title = "Points Usage",
                    description = "Learn how you can use your points to unlock content",
                    icon = Icons.Default.QuestionMark,
                    onClick = { showSpendPoints = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Maybe Later",
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) 
                        else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Secondary Sheets
    if (showSharingRewards) {
        val childSheetState = rememberModalBottomSheetState()
        SharingRewardsSheet(
            onDismiss = { showSharingRewards = false },
            onSpendPoints = { 
                showSharingRewards = false
                showSpendPoints = true 
            },
            sheetState = childSheetState
        )
    }

    if (showSpendPoints) {
        val childSheetState = rememberModalBottomSheetState()
        SpendPointsSheet(
            onDismiss = { showSpendPoints = false },
            sheetState = childSheetState
        )
    }
}

@Composable
private fun ModernActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    actionText: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    // Premium reward colors - matching the vibrant Indigo theme of the modal
    val rewardGradient = if (isDark) {
        listOf(Color(0xFF1E1B4B), Color(0xFF312E81)) // Deep Indigo
    } else {
        listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE)) // Very soft Indigo tint
    }
    
    val accentColor = Color(0xFF6366F1) // Pure Indigo
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = if (isDark) 0.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(rewardGradient))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        if (isDark) listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
                        else listOf(accentColor.copy(0.3f), accentColor.copy(0.1f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stylish icon container
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = accentColor.copy(alpha = if (isDark) 0.25f else 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isDark) Color.White else accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF1E1B4B)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF4338CA),
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 16.sp
                    )
                }

                // Added Spacer to prevent touching
                Spacer(modifier = Modifier.width(12.dp))

                // Reward Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor
                ) {
                    Text(
                        text = actionText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernInfoCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF1E1E1E) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp, 
            if (isDark) Color.White.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.3f)
        ),
        shadowElevation = if (isDark) 0.dp else 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), 
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = description, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF334155),
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ModernPlanCard(
    modifier: Modifier = Modifier,
    points: Int,
    price: String,
    isPopular: Boolean = false,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = Color(0xFF6366F1)
    
    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(20.dp),
            color = if (isPopular) {
                if (isDark) accentColor.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.08f)
            } else {
                if (isDark) Color(0xFF1E1E1E) else Color.White
            },
            border = androidx.compose.foundation.BorderStroke(
                if (isPopular) 2.dp else 1.dp,
                if (isPopular) accentColor.copy(alpha = 0.6f) 
                else (if (isDark) Color.White.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.2f))
            ),
            shadowElevation = if (isPopular && !isDark) 4.dp else 0.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = PointsGold,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$points", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = "Points", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPopular) accentColor else accentColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = price,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPopular) Color.White else accentColor
                    )
                }
            }
        }

        if (isPopular) {
            Surface(
                modifier = Modifier.align(Alignment.TopCenter),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF59E0B) // Amber for "BEST"
            ) {
                Text(
                    text = "BEST VALUE",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 9.sp
                )
            }
        }
    }
}
