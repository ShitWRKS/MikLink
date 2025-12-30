package com.app.miklink.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.app.miklink.ui.theme.Spacing
import com.app.miklink.R

/**
 * Dismissible info/tip card with icon
 *
 * @param title Title text
 * @param message Message text
 * @param icon Icon to display
 * @param iconColor Color of the icon background
 * @param onDismiss Callback when dismiss button is clicked
 * @param modifier Modifier to be applied
 */
@Composable
fun InfoCard(
    title: String,
    message: String,
    icon: ImageVector,
    iconColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModernCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                IconContainer(
                    icon = icon,
                    backgroundColor = iconColor.copy(alpha = 0.2f),
                    iconTint = iconColor
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
