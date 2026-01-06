package com.example.salescrm.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.salescrm.data.*

// Composition Local for custom theme colors that need to be accessed directly
data class SalesCrmColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isDark: Boolean
)

val LocalSalesCrmColors = compositionLocalOf {
    SalesCrmColors(
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        border = DarkBorder,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textMuted = TextMuted,
        isDark = true
    )
}

// Composition Locals for custom pipeline items
val LocalCustomStages = compositionLocalOf { defaultCustomStages }
val LocalCustomPriorities = compositionLocalOf { fixedPriorities } // Fixed priorities, not customizable
val LocalCustomSegments = compositionLocalOf { defaultCustomSegments }
val LocalCustomSources = compositionLocalOf { defaultCustomSources }

// Dark color scheme
private val SalesDarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    tertiaryContainer = DarkSurfaceVariant,
    onTertiaryContainer = TextPrimary,
    error = AccentRed,
    onError = Color.White,
    errorContainer = AccentRed.copy(alpha = 0.2f),
    onErrorContainer = AccentRed,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = TextMuted,
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = PrimaryBlueLight,
    surfaceTint = PrimaryBlue
)

// Light color scheme
private val SalesLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlue.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryBlue,
    secondary = AccentPurple,
    onSecondary = Color.White,
    secondaryContainer = AccentPurple.copy(alpha = 0.1f),
    onSecondaryContainer = AccentPurple,
    tertiary = AccentGreen,
    onTertiary = Color.White,
    tertiaryContainer = AccentGreen.copy(alpha = 0.1f),
    onTertiaryContainer = AccentGreen,
    error = AccentRed,
    onError = Color.White,
    errorContainer = AccentRed.copy(alpha = 0.1f),
    onErrorContainer = AccentRed,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = LightTextMuted,
    inverseSurface = LightTextPrimary,
    inverseOnSurface = LightBackground,
    inversePrimary = PrimaryBlueLight,
    surfaceTint = PrimaryBlue
)

// Dark CRM Colors
private val darkCrmColors = SalesCrmColors(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    border = DarkBorder,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    isDark = true
)

// Light CRM Colors
private val lightCrmColors = SalesCrmColors(
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    border = LightBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    isDark = false
)

@Composable
fun SalesCrmTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    customStages: List<CustomItem> = defaultCustomStages,
    customPriorities: List<CustomItem> = defaultCustomPriorities,
    customSegments: List<CustomItem> = defaultCustomSegments,
    customSources: List<CustomItem> = defaultCustomSources,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }
    
    val colorScheme = if (useDarkTheme) SalesDarkColorScheme else SalesLightColorScheme
    val crmColors = if (useDarkTheme) darkCrmColors else lightCrmColors
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = crmColors.background.toArgb()
            window.navigationBarColor = crmColors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(
        LocalSalesCrmColors provides crmColors,
        LocalCustomStages provides customStages,
        LocalCustomPriorities provides fixedPriorities, // Always use fixed priorities
        LocalCustomSegments provides customSegments,
        LocalCustomSources provides customSources
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Extension property to easily access CRM colors and custom items
object SalesCrmTheme {
    val colors: SalesCrmColors
        @Composable
        get() = LocalSalesCrmColors.current
    
    val stages: List<CustomItem>
        @Composable
        get() = LocalCustomStages.current
        
    val priorities: List<CustomItem>
        @Composable
        get() = LocalCustomPriorities.current
        
    val segments: List<CustomItem>
        @Composable
        get() = LocalCustomSegments.current
        
    val sources: List<CustomItem>
        @Composable
        get() = LocalCustomSources.current
}