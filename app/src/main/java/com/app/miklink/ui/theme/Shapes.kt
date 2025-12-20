/*
 * UI shape scale, input expressive corner sizes, output reusable shapes and MaterialTheme shapes.
 */
package com.app.miklink.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object MikLinkShapeTokens {
    val containedSmall = RoundedCornerShape(16.dp)
    val containedMedium = RoundedCornerShape(24.dp)
    val containedLarge = RoundedCornerShape(32.dp)
    val containedXLarge = RoundedCornerShape(40.dp)
    val pill = RoundedCornerShape(999.dp)
}

val MikLinkShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = MikLinkShapeTokens.containedSmall,
    medium = MikLinkShapeTokens.containedMedium,
    large = MikLinkShapeTokens.containedLarge,
    extraLarge = MikLinkShapeTokens.containedXLarge
)
