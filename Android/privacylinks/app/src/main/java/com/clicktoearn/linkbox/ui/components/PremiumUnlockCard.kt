package com.clicktoearn.linkbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PremiumUnlockCard(
    isUnlocked: Boolean,
    cost: Int,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Premium Gradient
    val gradient = Brush.horizontalGradient(
        colors = if (isUnlocked) {
            listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            listOf(
                MaterialTheme.colorScheme.primary,
                Color(0xFF8E24AA) // Purple accent for premium feel
            )
        }
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp) // Taller for premium feel
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onAction),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Icon and Text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isUnlocked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked) MaterialTheme.colorScheme.onPrimary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = if (isUnlocked) "Content Unlocked" else "Unlock Content",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                        if (!isUnlocked) {
                            Text(
                                text = "Premium access required",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        } else {
                             Text(
                                text = "Ready to view",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Right Side: Cost or CTA
                if (!isUnlocked) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp), // Pill shape
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stars, 
                                contentDescription = null, 
                                tint = Color(0xFFFFC107), // Gold
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$cost",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                     Button(
                         onClick = onAction,
                         colors = ButtonDefaults.buttonColors(
                             containerColor = MaterialTheme.colorScheme.primary
                         ),
                         contentPadding = PaddingValues(horizontal = 24.dp)
                     ) {
                         Text("OPEN")
                     }
                }
            }
        }
    }
}
