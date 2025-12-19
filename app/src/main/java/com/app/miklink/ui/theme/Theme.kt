/*
 * Purpose: Provide MikLink MaterialTheme with system-driven light/dark, semantic status tokens, and safe window styling.
 * Inputs: Theme toggles (darkTheme, dynamicColor) and optional custom color overrides for white-label scenarios.
 * Outputs: Composition-local MaterialTheme plus MikLinkThemeTokens.semantic for consistent PASS/FAIL/RUNNING usage.
 */
package com.app.miklink.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.Immutable
import androidx.core.view.WindowCompat

@Immutable
data class MikLinkSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val successGlow: Color,
    val failure: Color,
    val onFailure: Color,
    val failureContainer: Color,
    val onFailureContainer: Color,
    val failureGlow: Color,
    val running: Color,
    val onRunning: Color,
    val runningContainer: Color,
    val onRunningContainer: Color,
    val runningGlow: Color
)

private val LightColorScheme = lightColorScheme(
    primary = MikLinkPrimaryLight,
    onPrimary = MikLinkOnPrimaryLight,
    primaryContainer = MikLinkPrimaryContainerLight,
    onPrimaryContainer = MikLinkOnPrimaryContainerLight,
    secondary = MikLinkSecondary,
    onSecondary = MikLinkOnPrimary,
    background = TechLightBackground,
    surface = TechLightSurface,
    surfaceVariant = TechLightSurfaceVariant,
    onBackground = TechLightTextHigh,
    onSurface = TechLightTextHigh,
    onSurfaceVariant = TechLightTextMedium,
    outline = TechLightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = MikLinkPrimaryDark,
    onPrimary = MikLinkOnPrimaryDark,
    primaryContainer = MikLinkPrimaryContainerDark,
    onPrimaryContainer = MikLinkOnPrimaryContainerDark,
    secondary = MikLinkSecondary,
    onSecondary = MikLinkOnPrimary,
    background = TechDarkBackground,
    surface = TechDarkSurface,
    surfaceVariant = TechDarkSurfaceVariant,
    onBackground = TechTextHigh,
    onSurface = TechTextHigh,
    onSurfaceVariant = TechTextMedium,
    outline = TechDarkOutline
)

private val LightSemanticColors = MikLinkSemanticColors(
    success = StatusSuccess,
    onSuccess = StatusOnSuccess,
    successContainer = StatusSuccessContainer,
    onSuccessContainer = StatusOnSuccessContainer,
    successGlow = GlowSuccess,
    failure = StatusFailure,
    onFailure = StatusOnFailure,
    failureContainer = StatusFailureContainer,
    onFailureContainer = StatusOnFailureContainer,
    failureGlow = GlowFailure,
    running = StatusRunningLight,
    onRunning = StatusOnRunningLight,
    runningContainer = StatusRunningLightContainer,
    onRunningContainer = StatusOnRunningLightContainer,
    runningGlow = GlowRunning
)

private val DarkSemanticColors = MikLinkSemanticColors(
    success = StatusSuccess,
    onSuccess = StatusOnSuccess,
    successContainer = StatusSuccessContainer,
    onSuccessContainer = StatusOnSuccessContainer,
    successGlow = GlowSuccess,
    failure = StatusFailure,
    onFailure = StatusOnFailure,
    failureContainer = StatusFailureContainer,
    onFailureContainer = StatusOnFailureContainer,
    failureGlow = GlowFailure,
    running = StatusRunningDark,
    onRunning = StatusOnRunningDark,
    runningContainer = StatusRunningDarkContainer,
    onRunningContainer = StatusOnRunningDarkContainer,
    runningGlow = GlowRunning
)

private val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

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
    val context = LocalContext.current
    val colorScheme = remember(
        context,
        darkTheme,
        dynamicColor,
        customPrimaryInfo,
        customSecondaryInfo,
        customBackgroundInfo,
        customContentInfo
    ) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            customPrimaryInfo != null -> buildCustomColorScheme(
                primaryColor = customPrimaryInfo,
                secondaryColor = customSecondaryInfo,
                backgroundColor = customBackgroundInfo,
                customContentColor = customContentInfo,
                isDark = darkTheme
            )
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }

    val semanticColors = remember(darkTheme) { if (darkTheme) DarkSemanticColors else LightSemanticColors }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme && getLuminance(colorScheme.background) > 0.5
        }
    }

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

fun buildCustomColorScheme(
    primaryColor: Int,
    secondaryColor: Int?,
    backgroundColor: Int?,
    customContentColor: Int? = null,
    isDark: Boolean
): ColorScheme {
    val primary = Color(primaryColor)
    val secondary = secondaryColor?.let { Color(it) } ?: primary
    val background = backgroundColor?.let { Color(it) } ?: if (isDark) TechDarkBackground else TechLightBackground
    val surface = backgroundColor?.let { Color(it) } ?: if (isDark) TechDarkSurface else TechLightSurface
    val onBackground = customContentColor?.let { Color(it) } ?: if (isDark) Color.White else TechLightTextHigh
    val onSurface = onBackground
    val onPrimary = primary.contrastColor()
    val onSecondary = secondary.contrastColor()

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            background = background,
            surface = surface,
            onBackground = onBackground,
            onSurface = onSurface
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            background = background,
            surface = surface,
            onBackground = onBackground,
            onSurface = onSurface
        )
    }
}

fun getLuminance(color: Color): Double {
    return 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
}

fun isLightChain(color: Color): Boolean {
    return getLuminance(color) > 0.5
}

private fun Color.contrastColor(): Color {
    return if (getLuminance(this) > 0.5) Color.Black else Color.White
}

object MikLinkThemeTokens {
    val semantic: MikLinkSemanticColors
        @Composable get() = LocalSemanticColors.current
}
