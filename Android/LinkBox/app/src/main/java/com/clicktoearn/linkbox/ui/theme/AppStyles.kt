package com.clicktoearn.linkbox.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ========== SHAPE DEFINITIONS ==========
object AppShapes {
    val CardSmall = RoundedCornerShape(12.dp)
    val CardMedium = RoundedCornerShape(16.dp)
    val CardLarge = RoundedCornerShape(20.dp)
    val CardExtraLarge = RoundedCornerShape(24.dp)
    
    val ButtonSmall = RoundedCornerShape(8.dp)
    val ButtonMedium = RoundedCornerShape(12.dp)
    val ButtonLarge = RoundedCornerShape(16.dp)
    val ButtonPill = RoundedCornerShape(50)
    
    val ChipShape = RoundedCornerShape(8.dp)
    val BadgeShape = RoundedCornerShape(6.dp)
    val DialogShape = RoundedCornerShape(28.dp)
    val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    
    val StatusIndicator = RoundedCornerShape(4.dp)
}

// ========== ELEVATION DEFINITIONS ==========
object AppElevation {
    val None = 0.dp
    val Low = 2.dp
    val Medium = 4.dp
    val High = 8.dp
    val Floating = 12.dp
}

// ========== SPACING DEFINITIONS ==========
object AppSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

// ========== GRADIENT DEFINITIONS ==========
object AppGradients {
    // Primary gradient - Indigo to Purple
    val primary = Brush.horizontalGradient(
        colors = listOf(PrimaryDark, PurpleStart)
    )
    
    val primaryVertical = Brush.verticalGradient(
        colors = listOf(PrimaryDark, PurpleStart)
    )
    
    // Secondary gradient - Teal
    val secondary = Brush.horizontalGradient(
        colors = listOf(SecondaryDark, CyanStart)
    )
    
    // Gold gradient for points/premium
    val gold = Brush.horizontalGradient(
        colors = listOf(GoldStart, GoldEnd)
    )
    
    val goldVertical = Brush.verticalGradient(
        colors = listOf(GoldStart, GoldEnd)
    )
    
    // Success gradient
    val success = Brush.horizontalGradient(
        colors = listOf(SuccessStart, SuccessEnd)
    )
    
    // Warning gradient
    val warning = Brush.horizontalGradient(
        colors = listOf(WarningStart, WarningEnd)
    )
    
    // Error gradient
    val error = Brush.horizontalGradient(
        colors = listOf(ErrorStart, ErrorEnd)
    )
    
    // Info gradient
    val info = Brush.horizontalGradient(
        colors = listOf(InfoStart, InfoEnd)
    )
    
    // Purple accent gradient
    val purple = Brush.horizontalGradient(
        colors = listOf(PurpleStart, PurpleEnd)
    )
    
    // Inactive/Disabled gradient
    val inactive = Brush.horizontalGradient(
        colors = listOf(InactiveGray, InactiveGrayLight)
    )
    
    // Premium subscription gradient
    val premium = Brush.horizontalGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFA855F7))
    )
    
    val premiumVertical = Brush.verticalGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFA855F7))
    )
    
    // Subtle surface gradient for cards
    fun surfaceLight() = Brush.verticalGradient(
        colors = listOf(SurfaceLight, Color(0xFFF8FAFC))
    )
    
    fun surfaceDark() = Brush.verticalGradient(
        colors = listOf(SurfaceDark, Color(0xFF141419))
    )
}

// ========== CARD STYLES ==========
object CardStyles {
    @Composable
    fun premiumCardColors() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    
    @Composable
    fun elevatedCardColors() = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    
    @Composable
    fun outlinedCardColors() = CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
    
    @Composable
    fun highlightCardColors() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    )
    
    @Composable
    fun successCardColors() = CardDefaults.cardColors(
        containerColor = SuccessSoft.copy(alpha = 0.5f)
    )
    
    @Composable
    fun warningCardColors() = CardDefaults.cardColors(
        containerColor = WarningSoft.copy(alpha = 0.5f)
    )
    
    @Composable
    fun errorCardColors() = CardDefaults.cardColors(
        containerColor = ErrorSoft.copy(alpha = 0.5f)
    )
}

// ========== BUTTON STYLES ==========
object ButtonStyles {
    @Composable
    fun primaryButtonColors() = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
    
    @Composable
    fun secondaryButtonColors() = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary
    )
    
    @Composable
    fun successButtonColors() = ButtonDefaults.buttonColors(
        containerColor = SuccessStart,
        contentColor = Color.White
    )
    
    @Composable
    fun errorButtonColors() = ButtonDefaults.buttonColors(
        containerColor = ErrorStart,
        contentColor = Color.White
    )
    
    @Composable
    fun premiumButtonColors() = ButtonDefaults.buttonColors(
        containerColor = PrimaryDark,
        contentColor = Color.White
    )
    
    @Composable
    fun ghostButtonColors() = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
}

// ========== ANIMATION SPECS ==========
object AnimationSpecs {
    val quickFade = tween<Float>(150)
    val normalFade = tween<Float>(300)
    val slowFade = tween<Float>(500)
    
    val quickScale = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    val bounceScale = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val smoothSlide = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
}

// ========== MODIFIER EXTENSIONS ==========

// Shimmer effect for loading states
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x1A000000),
                Color(0x40FFFFFF),
                Color(0x1A000000)
            ),
            start = Offset(translateAnim - 500, 0f),
            end = Offset(translateAnim, 0f)
        )
    )
}

// Gradient border effect
fun Modifier.gradientBorder(
    brush: Brush,
    width: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier = this
    .clip(shape)
    .drawBehind {
        drawRect(brush = brush)
    }
    .padding(width)

// Soft shadow effect for premium cards
fun Modifier.premiumShadow(
    elevation: Dp = 8.dp,
    shape: Shape = AppShapes.CardMedium
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = Color(0x1A000000),
    spotColor = Color(0x33000000)
)

// Status bar indicator at top of cards
@Composable
fun StatusBar(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val gradient = if (isActive) {
        AppGradients.success
    } else {
        AppGradients.inactive
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(gradient)
    )
}

// Premium badge for highlighting items
@Composable
fun PremiumBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.BadgeShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(AppGradients.gold)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// Points display badge used across screens
@Composable
fun PointsIndicator(
    points: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = AppShapes.ChipShape,
        color = GoldSoft
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "⭐",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "$points",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color(0xFFB45309)
            )
        }
    }
}

// Animated status dot
@Composable
fun StatusDot(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) ActiveGreen else InactiveGray
    
    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}
