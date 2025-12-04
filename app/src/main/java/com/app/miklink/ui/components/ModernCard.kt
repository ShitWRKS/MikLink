package com.app.miklink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.app.miklink.ui.theme.CornerRadius
import com.app.miklink.ui.theme.Elevation
import com.app.miklink.ui.theme.Spacing

/**
 * Modern card component with dark theme styling
 *
 * @param modifier Modifier to be applied to the card
 * @param onClick Optional click handler - makes card clickable
 * @param backgroundColor Background color of the card
 * @param cornerRadius Corner radius of the card
 * @param elevation Elevation of the card
 * @param content Content to be displayed inside the card
 */
@Composable
fun ModernCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    cornerRadius: Dp = CornerRadius.large,
    elevation: Dp = Elevation.small,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            content = content
        )
    }
}
