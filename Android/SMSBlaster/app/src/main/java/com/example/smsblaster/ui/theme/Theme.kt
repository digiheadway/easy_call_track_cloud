package com.example.smsblaster.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = Color.White,
    tertiary = Accent,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = StatusError,
    onError = Color.White,
    outline = BorderDark,
    outlineVariant = SurfaceElevatedDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = Color.White,
    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = Color.White,
    tertiary = Accent,
    onTertiary = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = StatusError,
    onError = Color.White,
    outline = BorderLight,
    outlineVariant = SurfaceElevatedLight
)

data class ExtendedColors(
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val chipBackground: Color,
    val surfaceElevated: Color,
    val statusDraft: Color = StatusDraft,
    val statusScheduled: Color = StatusScheduled,
    val statusRunning: Color = StatusRunning,
    val statusPaused: Color = StatusPaused,
    val statusCompleted: Color = StatusCompleted,
    val statusFailed: Color = StatusFailed,
    val success: Color = StatusSuccess,
    val warning: Color = StatusWarning,
    val error: Color = StatusError,
    val info: Color = StatusInfo,
    val gradientStart: Color = GradientStart,
    val gradientEnd: Color = GradientEnd
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        cardBackground = CardDark,
        textPrimary = TextPrimaryDark,
        textSecondary = TextSecondaryDark,
        textTertiary = TextTertiaryDark,
        border = BorderDark,
        chipBackground = ChipBackgroundDark,
        surfaceElevated = SurfaceElevatedDark
    )
}

@Composable
fun SMSBlasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val extendedColors = if (darkTheme) {
        ExtendedColors(
            cardBackground = CardDark,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark,
            textTertiary = TextTertiaryDark,
            border = BorderDark,
            chipBackground = ChipBackgroundDark,
            surfaceElevated = SurfaceElevatedDark
        )
    } else {
        ExtendedColors(
            cardBackground = CardLight,
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight,
            textTertiary = TextTertiaryLight,
            border = BorderLight,
            chipBackground = ChipBackgroundLight,
            surfaceElevated = SurfaceElevatedLight
        )
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object SMSBlasterTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}