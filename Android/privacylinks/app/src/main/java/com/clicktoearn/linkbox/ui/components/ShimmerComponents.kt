package com.clicktoearn.linkbox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier for loading states
 * Creates an animated gradient that simulates loading content
 */
@Composable
fun shimmerBrush(showShimmer: Boolean = true): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_anim"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

/**
 * Shimmer loading placeholder for shared content screen
 * Displays loading UI that matches the actual content structure
 */
@Composable
fun SharedContentShimmer() {
    val brush = shimmerBrush()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ad placeholder shimmer
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
        
        // File/Info Section shimmer
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon placeholder
            Spacer(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Title placeholder
            Spacer(
                modifier = Modifier
                    .height(28.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }
        
        // Description placeholder
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        
        // Divider placeholder
        Spacer(modifier = Modifier.height(8.dp))
        
        // Metadata rows shimmer
        repeat(4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Spacer(
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Divider placeholder
        Spacer(modifier = Modifier.height(8.dp))
        
        // Action button shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
            Spacer(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
        }
        
        // Rectangle ad placeholder shimmer
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Disclaimer card shimmer
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brush)
        )
    }
}

/**
 * Simple shimmer box for custom use
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp)
) {
    val brush = shimmerBrush()
    Spacer(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * Shimmer effect specifically designed for Native Ads
 * Mimics the structure of native_ad_layout.xml
 */
@Composable
fun NativeAdShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    ) {
        // Ad Badge & Advertiser
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(24.dp, 16.dp)) // Ad badge
            Spacer(modifier = Modifier.width(8.dp))
            ShimmerBox(modifier = Modifier.size(100.dp, 16.dp)) // Advertiser name
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Media View
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(0.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Icon + Text
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(
                modifier = Modifier.size(48.dp), 
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                ShimmerBox(modifier = Modifier.size(180.dp, 20.dp)) // Headline
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(modifier = Modifier.size(240.dp, 16.dp)) // Body
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Call to Action
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp) // Button shape
        )
    }
}
