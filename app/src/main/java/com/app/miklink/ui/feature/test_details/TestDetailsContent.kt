/*
 * Purpose: Render test execution details from a typed TestRunSnapshot for both live and history contexts.
 * Inputs: TestRunSnapshot and a renderer registry mapping section ids to composable renderers.
 * Outputs: Composable content showing sections in order with status, payload, and warnings.
 * Notes: Single renderer for live/history per ADR-0011; replace legacy parsing UI.
 */
package com.app.miklink.ui.feature.test_details

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import com.app.miklink.R
import com.app.miklink.core.domain.test.model.TestRunSnapshot
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.core.domain.test.model.TestSectionStatus
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.ui.theme.MikLinkThemeTokens

@Composable
fun TestDetailsContent(
    snapshot: TestRunSnapshot,
    rendererRegistry: SectionRendererRegistry,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        snapshot.sections.forEach { section ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clipToBounds(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                SectionCard(section = section, registry = rendererRegistry)
            }
        }
    }
}

@Composable
private fun SectionCard(
    section: TestSectionSnapshot,
    registry: SectionRendererRegistry
) {
    val semantic = MikLinkThemeTokens.semantic
    val renderer = registry.rendererFor(section.id)
    val defaultTitle = when (section.id) {
        TestSectionId.NETWORK -> stringResource(id = R.string.section_network)
        TestSectionId.LINK -> stringResource(id = R.string.section_link)
        TestSectionId.TDR -> stringResource(id = R.string.section_tdr)
        TestSectionId.NEIGHBORS -> stringResource(id = R.string.section_lldp)
        TestSectionId.PING -> stringResource(id = R.string.section_ping)
        TestSectionId.SPEED -> stringResource(id = R.string.section_speed)
        else -> section.id.name
    }
    val statusTone = when (section.status) {
        TestSectionStatus.PASS -> semantic.successContainer to semantic.onSuccessContainer
        TestSectionStatus.FAIL -> semantic.failureContainer to semantic.onFailureContainer
        TestSectionStatus.RUNNING -> semantic.runningContainer to semantic.onRunningContainer
        TestSectionStatus.SKIP -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TestSectionStatus.INFO -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val (icon, tint) = iconForSection(section.id)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = section.title ?: defaultTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusLabel(section.status),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusTone.second
                    )
                }
            }
            SectionStatusPill(
                label = statusChipLabel(section.status),
                background = statusTone.first,
                contentColor = statusTone.second
            )
        }
        section.warning?.takeIf { it.isNotBlank() }?.let { warning ->
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = semantic.failure
            )
        }
        renderer.Render(section)
    }
}

@Composable
private fun SectionStatusPill(
    label: String,
    background: Color,
    contentColor: Color
) {
    Text(
        text = label,
        color = contentColor,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun iconForSection(id: TestSectionId): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> =
    when (id) {
        TestSectionId.NETWORK -> Icons.Default.NetworkCheck to MaterialTheme.colorScheme.primary
        TestSectionId.LINK -> Icons.Default.Link to MaterialTheme.colorScheme.tertiary
        TestSectionId.TDR -> Icons.Default.Cable to MaterialTheme.colorScheme.primary
        TestSectionId.NEIGHBORS -> Icons.Default.Devices to MaterialTheme.colorScheme.secondary
        TestSectionId.PING -> Icons.Default.Wifi to MaterialTheme.colorScheme.primary
        TestSectionId.SPEED -> Icons.Default.Speed to MaterialTheme.colorScheme.secondary
        else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun statusLabel(status: TestSectionStatus): String =
    when (status) {
        TestSectionStatus.PASS -> stringResource(id = R.string.status_pass)
        TestSectionStatus.FAIL -> stringResource(id = R.string.status_fail)
        TestSectionStatus.SKIP -> stringResource(id = R.string.status_skip)
        TestSectionStatus.RUNNING -> stringResource(id = R.string.test_execution_status_chip_running)
        TestSectionStatus.INFO -> stringResource(id = R.string.status_info)
        else -> status.name
    }

@Composable
private fun statusChipLabel(status: TestSectionStatus): String =
    when (status) {
        TestSectionStatus.PASS -> stringResource(id = R.string.status_pass)
        TestSectionStatus.FAIL -> stringResource(id = R.string.status_fail)
        TestSectionStatus.SKIP -> stringResource(id = R.string.status_skip)
        TestSectionStatus.RUNNING -> stringResource(id = R.string.test_execution_status_chip_running)
        TestSectionStatus.INFO -> stringResource(id = R.string.status_info)
        else -> status.name
    }
