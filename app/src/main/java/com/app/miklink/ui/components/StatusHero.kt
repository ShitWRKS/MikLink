/*
 * UI status hero, input state/title/subtitle, output expressive hero card with glow and indicator.
 */
package com.app.miklink.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.app.miklink.ui.theme.MikLinkShapeTokens
import com.app.miklink.ui.theme.MikLinkThemeTokens
import com.app.miklink.ui.theme.softGlow

enum class StatusHeroState {
    Running,
    Success,
    Failure
}

@Composable
fun StatusHero(
    status: StatusHeroState,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    supportingContent: (@Composable () -> Unit)? = null
) {
    val semantic = MikLinkThemeTokens.semantic
    val accent = when (status) {
        StatusHeroState.Running -> semantic.running
        StatusHeroState.Success -> semantic.success
        StatusHeroState.Failure -> semantic.failure
    }
    val onAccent = when (status) {
        StatusHeroState.Running -> semantic.onRunning
        StatusHeroState.Success -> semantic.onSuccess
        StatusHeroState.Failure -> semantic.onFailure
    }
    val glow = when (status) {
        StatusHeroState.Running -> semantic.runningGlow
        StatusHeroState.Success -> semantic.successGlow
        StatusHeroState.Failure -> semantic.failureGlow
    }
    val icon = when (status) {
        StatusHeroState.Running -> Icons.Default.HourglassEmpty
        StatusHeroState.Success -> Icons.Default.CheckCircle
        StatusHeroState.Failure -> Icons.Default.Cancel
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconHalo(
                icon = icon,
                glow = glow,
                color = accent,
                onColor = onAccent,
                glowAlpha = if (status == StatusHeroState.Running) 0.28f else 0.18f,
                breathe = true
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = accent,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            if (status == StatusHeroState.Running) {
                ScanlineIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
            supportingContent?.invoke()
        }
    }
}

@Composable
private fun IconHalo(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    glow: Color,
    color: Color,
    onColor: Color,
    glowAlpha: Float,
    breathe: Boolean
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(92.dp)
            .softGlow(color = glow, radius = 120.dp, maxAlpha = glowAlpha, breathe = breathe)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onColor,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun ScanlineIndicator(
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color
) {
    val transition = rememberInfiniteTransition(label = "scanline")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanline_progress"
    )
    Canvas(
        modifier = modifier
            .clip(MikLinkShapeTokens.pill)
    ) {
        drawRoundRect(
            color = trackColor,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2, size.height / 2),
            style = Fill
        )
        val lineWidth = size.width * 0.28f
        val startX = (size.width + lineWidth) * progress - lineWidth
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, color, Color.Transparent)
            ),
            topLeft = androidx.compose.ui.geometry.Offset(startX, 0f),
            size = androidx.compose.ui.geometry.Size(lineWidth, size.height)
        )
    }
}
