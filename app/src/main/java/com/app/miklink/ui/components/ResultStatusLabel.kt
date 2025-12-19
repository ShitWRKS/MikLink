/*
 * Purpose: Lightweight semantic status label (PASS/FAIL/RUNNING/INFO/SKIP/PENDING) without background pills.
 * Inputs: status string (case-insensitive) and optional modifier.
 * Outputs: Row with icon + uppercase label tinted by MikLink semantic colors for test outcomes.
 */
package com.app.miklink.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.miklink.ui.theme.MikLinkThemeTokens

@Composable
fun ResultStatusLabel(
    status: String,
    modifier: Modifier = Modifier
) {
    val semantic = MikLinkThemeTokens.semantic
    val normalized = status.uppercase()
    val (icon, color) = when (normalized) {
        "PASS" -> Icons.Default.Check to semantic.success
        "FAIL" -> Icons.Default.Close to semantic.failure
        "RUNNING", "IN PROGRESS", "PENDING" -> Icons.Default.HourglassEmpty to semantic.running
        else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(
            text = normalized,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Spacer(modifier = Modifier.width(0.dp))
    }
}
