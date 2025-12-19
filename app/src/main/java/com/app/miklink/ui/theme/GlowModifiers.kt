/*
 * Purpose: Provide a reusable soft glow modifier for hero/state indicators with optional breathing animation.
 * Inputs: Color for the glow, radius/maxAlpha tuning, and breathe flag to enable subtle alpha oscillation.
 * Outputs: Modifier that draws a radial gradient behind content without altering layout size.
 */
package com.app.miklink.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.softGlow(
    color: Color,
    radius: Dp = 28.dp,
    maxAlpha: Float = 0.22f,
    breathe: Boolean = true
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "softGlowTransition")
    val animatedAlpha by if (breathe) {
        transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "softGlowAlpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    val resolvedAlpha = (animatedAlpha * maxAlpha).coerceIn(0f, 1f)

    drawBehind {
        val glowRadius = radius.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = resolvedAlpha), Color.Transparent),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = center
        )
    }
}
