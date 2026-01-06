package com.clicktoearn.linkbox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clicktoearn.linkbox.viewmodel.LinkBoxViewModel

@Composable
fun PointsHeaderButton(viewModel: LinkBoxViewModel, onClick: () -> Unit) {
    val userPoints by viewModel.userPoints.collectAsState()
    
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .padding(end = 16.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Stars,
                contentDescription = "Points",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$userPoints",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Rename of old PointsInfoSheet, focused on Sharing Rewards
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharingRewardsSheet(
    onDismiss: () -> Unit,
    onSpendPoints: () -> Unit,
    sheetState: SheetState
) {
    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = "Sharing Rewards"
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            
            Text(
                text = "How much you earn",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            

            EarningWayItem(
                icon = Icons.Filled.PersonAdd,
                title = "20 Points per New Install",
                desc = "Earn when a new user installs the app via your link."
            )
            EarningWayItem(
                icon = Icons.Filled.Stars,
                title = "40% of Link Cost",
                desc = "Earn 40% of the points users spend to unlock your link."
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                onClick = onSpendPoints,
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "What can you do with points?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Filled.ChevronRight, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Got it")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendPointsSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    LinkBoxBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = "Spending Points"
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            
            Text(
                text = "What you can do",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            SpendingWayItem(
                icon = Icons.Filled.LockOpen,
                title = "Unlock Content",
                desc = "Use points to access premium files and links shared by others."
            )
            SpendingWayItem(
                icon = Icons.Filled.NoAccounts,
                title = "Remove Ads",
                desc = "Enjoy an ad-free experience for a limited time."
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Got it")
            }
        }
    }
}

// New Wallet Sheet for Header Chip
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletBottomSheet(
    viewModel: LinkBoxViewModel,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    ModernPointsSheet(
        viewModel = viewModel,
        onDismiss = onDismiss,
        sheetState = sheetState
    )
}

@Composable
fun BuyPlanCard(
    modifier: Modifier = Modifier, 
    points: Int, 
    price: String, 
    isPopular: Boolean = false,
    onClick: () -> Unit
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp) // Consistent top padding for alignment
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (isPopular) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                 else MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                if (isPopular) 2.dp else 1.dp, 
                if (isPopular) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Stars,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$points",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Points",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
        
        if (isPopular) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Text(
                    text = "BEST VALUE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EarningWayItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun SpendingWayItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
