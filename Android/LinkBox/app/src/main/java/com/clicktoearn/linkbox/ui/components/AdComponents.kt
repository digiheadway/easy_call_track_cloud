package com.clicktoearn.linkbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clicktoearn.linkbox.ui.theme.*
import com.clicktoearn.linkbox.ui.viewmodel.LinkBoxViewModel

/**
 * A placeholder banner ad component that respects subscription status.
 * Replace the inner content with actual AdMob BannerAd when integrating ads.
 *
 * @param viewModel The LinkBoxViewModel to check subscription status
 * @param modifier Modifier for the banner
 * @param onRemoveAdsClick Callback when user clicks "Remove Ads"
 */
@Composable
fun BannerAdPlaceholder(
    viewModel: LinkBoxViewModel,
    modifier: Modifier = Modifier,
    onRemoveAdsClick: () -> Unit = { viewModel.openSubscriptionDialog() }
) {
    val isSubscribed by viewModel.isSubscriptionActive.collectAsState()
    
    // Don't show ad if user is subscribed
    if (isSubscribed) return
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // This is a placeholder - replace with actual AdMob banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ad Space",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                TextButton(
                    onClick = onRemoveAdsClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove Ads", fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * A wrapper composable that conditionally shows content or ads.
 * Use this to wrap screens that should show interstitial ads.
 *
 * @param viewModel The LinkBoxViewModel to check subscription status
 * @param showAd Whether to attempt showing an interstitial ad
 * @param onAdDismissed Callback when ad is dismissed or skipped
 * @param content The main content to show
 */
@Composable
fun InterstitialAdWrapper(
    viewModel: LinkBoxViewModel,
    showAd: Boolean = false,
    onAdDismissed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val isSubscribed by viewModel.isSubscriptionActive.collectAsState()
    
    LaunchedEffect(showAd) {
        if (showAd && !isSubscribed) {
            // TODO: Show actual interstitial ad here
            // For now, just call onAdDismissed
            onAdDismissed()
        } else if (showAd) {
            // User is subscribed, skip ad
            onAdDismissed()
        }
    }
    
    content()
}

/**
 * A reusable "Remove Ads" banner that can be shown in various places.
 */
@Composable
fun RemoveAdsBanner(
    viewModel: LinkBoxViewModel,
    modifier: Modifier = Modifier
) {
    val isSubscribed by viewModel.isSubscriptionActive.collectAsState()
    
    // Don't show if already subscribed
    if (isSubscribed) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(PrimaryDark, PurpleStart)
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Tired of ads?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Go Premium for 199/week",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { viewModel.openSubscriptionDialog() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PrimaryDark
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Remove", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Simple extension to check if ads should be shown
 */
@Composable
fun shouldShowAds(viewModel: LinkBoxViewModel): Boolean {
    val isSubscribed by viewModel.isSubscriptionActive.collectAsState()
    return !isSubscribed
}
