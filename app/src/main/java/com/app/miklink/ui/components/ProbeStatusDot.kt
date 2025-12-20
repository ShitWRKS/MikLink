/*
 * UI status dot indicator, input probe online state, output tappable glow dot rendering.
 */
package com.app.miklink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.ui.theme.MikLinkThemeTokens
import com.app.miklink.ui.theme.softGlow

@Composable
fun ProbeStatusDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    dotSize: androidx.compose.ui.unit.Dp = 12.dp
) {
    val semantic = MikLinkThemeTokens.semantic
    val color = if (isOnline) semantic.success else semantic.failure
    val glowColor = if (isOnline) semantic.successGlow else semantic.failureGlow
    val description = if (isOnline) {
        stringResource(id = R.string.dashboard_probe_online)
    } else {
        stringResource(id = R.string.dashboard_probe_offline)
    }
    val resolvedModifier = if (onClick != null) {
        modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    } else {
        modifier
    }
    Box(
        modifier = resolvedModifier
            .size(dotSize)
            .softGlow(color = glowColor, radius = 20.dp, maxAlpha = 0.3f, breathe = true)
            .background(color, shape = CircleShape)
            .semantics {
                contentDescription = description
            }
            .testTag("probe_status_dot")
    )
}
