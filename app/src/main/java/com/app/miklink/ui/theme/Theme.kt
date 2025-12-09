package com.app.miklink.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.Scheme
import com.google.android.material.color.utilities.TonalPalette

private val DarkColorScheme = darkColorScheme(
    primary = MikLinkPrimaryLight,
    onPrimary = Color.Black,
    secondary = MikLinkSecondary,
    onSecondary = Color.White,
    background = TechDarkBackground,
    surface = TechDarkSurface,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = MikLinkPrimary,
    onPrimary = Color.White,
    secondary = MikLinkSecondary,
    onSecondary = Color.White,
    background = TechLightBackground,
    surface = TechLightSurface,
    onBackground = Neutral900,
    onSurface = Neutral900
)

@Composable
fun MikLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    customPrimaryInfo: Int? = null,
    customSecondaryInfo: Int? = null,
    customBackgroundInfo: Int? = null,
    customContentInfo: Int? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = remember(darkTheme, customPrimaryInfo, customSecondaryInfo, customBackgroundInfo, customContentInfo) {
        if (customPrimaryInfo != null) {
            generateHarmoniousScheme(
                primaryColor = customPrimaryInfo,
                secondaryColor = customSecondaryInfo,
                backgroundColor = customBackgroundInfo,
                customContentColor = customContentInfo,
                isDark = darkTheme
            )
        } else {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme && getLuminance(colorScheme.background) > 0.5
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Generates a Material 3 ColorScheme using HCT Tonal Palettes.
 * If backgroundColor is provided, it acts as the seed for Neutral palettes, enforcing harmony.
 */
fun generateHarmoniousScheme(
    primaryColor: Int,
    secondaryColor: Int?,
    backgroundColor: Int?,
    customContentColor: Int? = null,
    isDark: Boolean
): ColorScheme {
    val primaryPalette = TonalPalette.fromInt(primaryColor)
    val secondaryPalette = if (secondaryColor != null) TonalPalette.fromInt(secondaryColor) else TonalPalette.fromInt(primaryColor)
    
    // If background is Custom, use it as Neutral seed. Otherwise use Primary as Neutral seed (standard M3)
    val neutralPalette = if (backgroundColor != null) TonalPalette.fromInt(backgroundColor) else TonalPalette.fromInt(primaryColor)
    val neutralVariantPalette = if (backgroundColor != null) TonalPalette.fromInt(backgroundColor) else TonalPalette.fromInt(primaryColor)

    // Standard Material 3 Tone Mapping
    // https://m3.material.io/styles/color/the-color-system/tokens
    
    val primaryTone = if (isDark) 80 else 40
    val onPrimaryTone = if (isDark) 20 else 100
    val containerTone = if (isDark) 30 else 90
    val onContainerTone = if (isDark) 90 else 10

    val surfaceTone = if (isDark) 6 else 98 // Slightly off-black/white
    val onSurfaceTone = if (isDark) 90 else 10
    
    // If Custom Background is set, we FORCE it.
    // However, for harmony, we want "Surface" to be related.
    // If the user picked "Yellow" (Cyberpunk), we use it.
    val finalBackground = if (backgroundColor != null) Color(backgroundColor) else Color(neutralPalette.tone(surfaceTone))
    val finalSurface = if (backgroundColor != null) Color(backgroundColor) else Color(neutralPalette.tone(surfaceTone))

    // Determine readable text for custom background, or use customContentColor if provided
    val finalOnBackground = if (customContentColor != null) {
         Color(customContentColor)
    } else if (backgroundColor != null) {
        if (getLuminance(finalBackground) > 0.5) Color.Black else Color.White
    } else {
        Color(neutralPalette.tone(onSurfaceTone))
    }

    return ColorScheme(
        primary = Color(primaryPalette.tone(primaryTone)),
        onPrimary = Color(primaryPalette.tone(onPrimaryTone)),
        primaryContainer = Color(primaryPalette.tone(containerTone)),
        onPrimaryContainer = Color(primaryPalette.tone(onContainerTone)),
        inversePrimary = Color(primaryPalette.tone(if (isDark) 40 else 80)),
        
        secondary = Color(secondaryPalette.tone(primaryTone)),
        onSecondary = Color(secondaryPalette.tone(onPrimaryTone)),
        secondaryContainer = Color(secondaryPalette.tone(containerTone)),
        onSecondaryContainer = Color(secondaryPalette.tone(onContainerTone)),
        
        tertiary = Color(primaryPalette.tone(if (isDark) 80 else 40)), // Fallback use primary as tertiary for now
        onTertiary = Color(primaryPalette.tone(if (isDark) 20 else 100)),
        tertiaryContainer = Color(primaryPalette.tone(if (isDark) 30 else 90)),
        onTertiaryContainer = Color(primaryPalette.tone(if (isDark) 90 else 10)),

        background = finalBackground,
        onBackground = finalOnBackground,
        
        surface = finalSurface,
        onSurface = finalOnBackground,
        surfaceVariant = Color(neutralVariantPalette.tone(if (isDark) 30 else 90)), // Important for harmony in containers
        onSurfaceVariant = Color(neutralVariantPalette.tone(if (isDark) 80 else 30)),
        
        surfaceTint = Color(primaryPalette.tone(primaryTone)),
        inverseSurface = Color(neutralPalette.tone(if (isDark) 90 else 20)),
        inverseOnSurface = Color(neutralPalette.tone(if (isDark) 20 else 95)),
        
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        
        outline = Color(neutralVariantPalette.tone(50)),
        outlineVariant = Color(neutralVariantPalette.tone(80)),
        scrim = Color.Black,
        
        // Add Surface Containers for Dialogs and Cards
        surfaceBright = Color(neutralPalette.tone(if (isDark) 24 else 98)),
        surfaceDim = Color(neutralPalette.tone(if (isDark) 6 else 87)),
        surfaceContainerLowest = Color(neutralPalette.tone(if (isDark) 4 else 100)),
        surfaceContainerLow = Color(neutralPalette.tone(if (isDark) 10 else 96)),
        surfaceContainer = Color(neutralPalette.tone(if (isDark) 12 else 94)),
        surfaceContainerHigh = Color(neutralPalette.tone(if (isDark) 17 else 92)),
        surfaceContainerHighest = Color(neutralPalette.tone(if (isDark) 22 else 90))
    )
}

fun getLuminance(color: Color): Double {
    return 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
}

fun isLightChain(color: Color): Boolean {
    return getLuminance(color) > 0.5
}