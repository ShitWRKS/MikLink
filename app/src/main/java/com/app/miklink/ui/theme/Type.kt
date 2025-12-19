/*
 * Purpose: Define MikLink typography using Manrope as the base UI font and JetBrains Mono for technical/monospace text.
 * Inputs: Font resources in res/font (manrope_regular/medium/semibold, jetbrains_mono_regular/medium).
 * Outputs: Material3 Typography instance for MaterialTheme and helper text styles for monospace content.
 */
package com.app.miklink.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.app.miklink.R

val Manrope = FontFamily(
    Font(resId = R.font.manrope_regular, weight = FontWeight.Normal),
    Font(resId = R.font.manrope_medium, weight = FontWeight.Medium),
    Font(resId = R.font.manrope_semibold, weight = FontWeight.SemiBold)
)

val JetBrainsMono = FontFamily(
    Font(resId = R.font.jetbrains_mono_regular, weight = FontWeight.Normal),
    Font(resId = R.font.jetbrains_mono_medium, weight = FontWeight.Medium)
)

private val BaseTypography = Typography()

val Typography = Typography(
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Normal),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Normal),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Normal),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Medium)
)

val MonoBody = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp
)

val MonoLabel = TextStyle(
    fontFamily = JetBrainsMono,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp
)
