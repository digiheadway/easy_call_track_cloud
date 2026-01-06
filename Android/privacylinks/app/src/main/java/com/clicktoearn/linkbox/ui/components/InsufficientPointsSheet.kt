package com.clicktoearn.linkbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsufficientPointsSheet(
    currentBalance: Int,
    requiredPoints: Int,
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit,
    onEarnPoints: () -> Unit,
    onBuyPoints: (Int) -> Unit,
    viewModel: LinkBoxViewModel,
    isLoggedIn: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState()
    var showLoginPrompt by remember { mutableStateOf(false) }
    val loginSheetState = rememberModalBottomSheetState()
    val isDark = isSystemInDarkTheme()
    


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = if (isDark) Color(0xFF121212) else Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f), 
                        CircleShape
                    )
            )

            // Header Icon with Glow
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color(0xFFEF4444).copy(alpha = 0.1f), 
                            CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFFEF4444)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Insufficient Points",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF1E1B4B)
            )
            
            Text(
                text = "You need $requiredPoints points to unlock this content",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Balance Comparison Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Your Balance", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = if (isDark) Color.White.copy(0.6f) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Stars, null, modifier = Modifier.size(20.dp), tint = Color(0xFFFFD700))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "$currentBalance", 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                    
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward, 
                        null, 
                        tint = if (isDark) Color.White.copy(0.2f) else Color.Black.copy(0.2f)
                    )
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Required", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$requiredPoints", 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.Black, 
                                color = Color(0xFFEF4444)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.Stars, null, modifier = Modifier.size(20.dp), tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Watch Ad Card (Premium Indigo Theme)
            InsufficientActionCard(
                title = "Watch Ad to Earn",
                description = "Get 5 points instantly",
                icon = Icons.Filled.PlayCircle,
                actionText = "+5 Points",
                onClick = onWatchAd
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Buy Points Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Refill Instantly",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
               InsufficientPlanCard(modifier = Modifier.weight(1f), points = 100, price = "19 Rs", isPopular = false) { 
                   if (!isLoggedIn) showLoginPrompt = true else onBuyPoints(100)
               }
               InsufficientPlanCard(modifier = Modifier.weight(1f), points = 350, price = "49 Rs", isPopular = true) { 
                   if (!isLoggedIn) showLoginPrompt = true else onBuyPoints(350)
               }
               InsufficientPlanCard(modifier = Modifier.weight(1f), points = 1000, price = "99 Rs", isPopular = false) { 
                   if (!isLoggedIn) showLoginPrompt = true else onBuyPoints(1000)
               }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Earn by Sharing
            InsufficientInfoCard(
                title = "Share Link",
                description = "Earn points by sharing",
                icon = Icons.Filled.Share,
                onClick = { if (!isLoggedIn) showLoginPrompt = true else onEarnPoints() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    color = if (isDark) Color.White.copy(0.6f) else Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showLoginPrompt) {
        LoginRequiredSheet(
            onDismiss = { showLoginPrompt = false },
            viewModel = viewModel,
            sheetState = loginSheetState,
            message = "Login now to earn points, buy coins, and share links!"
        )
    }
}

@Composable
private fun InsufficientActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    actionText: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    
    val rewardGradient = if (isDark) {
        listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
    } else {
        listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE))
    }
    
    val accentColor = Color(0xFF6366F1)
    
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

                Spacer(modifier = Modifier.width(12.dp))

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
private fun InsufficientPlanCard(
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
                    imageVector = Icons.Filled.Stars,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
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
                color = Color(0xFFF59E0B)
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

@Composable
private fun InsufficientInfoCard(
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
                        Color(0xFF6366F1).copy(alpha = 0.15f), 
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
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
