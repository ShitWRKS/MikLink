/*
 * UI step timeline, input list of steps with status, output contained vertical timeline rendering.
 */
package com.app.miklink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.miklink.ui.theme.MikLinkShapeTokens
import com.app.miklink.ui.theme.MikLinkThemeTokens

enum class StepStatus {
    Pending,
    Running,
    Success,
    Failure,
    Skipped,
    Info
}

data class StepTimelineItem(
    val title: String,
    val icon: ImageVector,
    val status: StepStatus,
    val subtitle: String? = null,
    val statusLabel: String? = null
)

@Composable
fun StepTimeline(
    steps: List<StepTimelineItem>,
    modifier: Modifier = Modifier
) {
    if (steps.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        steps.forEachIndexed { index, step ->
            TimelineRow(
                step = step,
                showConnector = index < steps.lastIndex
            )
        }
    }
}

@Composable
private fun TimelineRow(
    step: StepTimelineItem,
    showConnector: Boolean
) {
    val semantic = MikLinkThemeTokens.semantic
    val containerColor = when (step.status) {
        StepStatus.Success -> semantic.successContainer
        StepStatus.Failure -> semantic.failureContainer
        StepStatus.Running -> semantic.runningContainer
        StepStatus.Skipped -> MaterialTheme.colorScheme.surfaceContainerHighest
        StepStatus.Info -> MaterialTheme.colorScheme.tertiaryContainer
        StepStatus.Pending -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val iconColor = when (step.status) {
        StepStatus.Success -> semantic.onSuccessContainer
        StepStatus.Failure -> semantic.onFailureContainer
        StepStatus.Running -> semantic.onRunningContainer
        StepStatus.Skipped -> MaterialTheme.colorScheme.onSurfaceVariant
        StepStatus.Info -> MaterialTheme.colorScheme.onTertiaryContainer
        StepStatus.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val lineColor = when (step.status) {
        StepStatus.Success -> semantic.success
        StepStatus.Failure -> semantic.failure
        StepStatus.Running -> semantic.running
        StepStatus.Skipped -> MaterialTheme.colorScheme.outlineVariant
        StepStatus.Info -> MaterialTheme.colorScheme.tertiary
        StepStatus.Pending -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Card(
                shape = MikLinkShapeTokens.containedSmall,
                colors = CardDefaults.cardColors(containerColor = containerColor)
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .width(2.dp)
                        .height(20.dp)
                        .background(lineColor, shape = MikLinkShapeTokens.pill)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (!step.subtitle.isNullOrBlank()) {
                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!step.statusLabel.isNullOrBlank()) {
                Text(
                    text = step.statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = lineColor
                )
            }
        }
    }
}
