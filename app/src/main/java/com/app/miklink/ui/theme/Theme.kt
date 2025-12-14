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
    val onBackground = customContentColor?.let { Color(it) } ?: if (isDark) Color.White else Neutral900
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