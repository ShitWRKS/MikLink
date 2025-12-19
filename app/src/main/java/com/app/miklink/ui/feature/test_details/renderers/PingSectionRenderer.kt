/*
 * Purpose: Render typed ping samples grouped by target with summary stats.
 * Inputs: TestSectionSnapshot carrying TestSectionPayload.Ping.
 * Outputs: Composable rows showing per-target metrics and errors.
 * Notes: Groups by target or sequence order when target is null.
 */
package com.app.miklink.ui.feature.test_details.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.core.domain.model.report.PingSample
import com.app.miklink.core.domain.test.model.TestSectionPayload
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.ui.feature.test_details.SectionRenderer
import com.app.miklink.ui.test.components.TestExecutionTags
import androidx.compose.ui.platform.testTag

class PingSectionRenderer : SectionRenderer {
    @Composable
    override fun Render(section: TestSectionSnapshot, modifier: Modifier) {
        val payload = section.payload as? TestSectionPayload.Ping
        val samples = payload?.samples.orEmpty()
        if (samples.isEmpty()) {
            Text(stringResource(id = R.string.test_details_ping_empty), style = MaterialTheme.typography.bodyMedium)
            return
        }
        val grouped = groupSamplesPreserveOrder(samples)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
            grouped.forEach { (target, group) ->
                PingTargetBlock(target = target ?: "-", samples = group)
            }
        }
    }

    @Composable
    private fun PingTargetBlock(target: String, samples: List<PingSample>) {
        val resolvedHost = samples.firstNotNullOfOrNull { it.host?.takeIf { host -> host.isNotBlank() } }
        val rawLoss = samples.lastOrNull { !it.packetLoss.isNullOrBlank() }?.packetLoss ?: "-"
        val lossText = when {
            rawLoss == "-" -> rawLoss
            rawLoss.trim().endsWith("%") -> rawLoss.trim()
            else -> "${rawLoss.trim()}%"
        }
        val sent = samples.mapNotNull { it.sent?.toIntOrNull() }.maxOrNull()?.toString()
            ?: samples.size.toString()
        val min = samples.firstNotNullOfOrNull { it.minRtt?.takeIf { value -> value.isNotBlank() } } ?: "-"
        val avg = samples.firstNotNullOfOrNull { it.avgRtt?.takeIf { value -> value.isNotBlank() } } ?: "-"
        val max = samples.firstNotNullOfOrNull { it.maxRtt?.takeIf { value -> value.isNotBlank() } } ?: "-"
        val lossChipColors = if (lossText.trim().startsWith("0")) {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.detail_label_target_number, target),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (target.equals("DHCP_GATEWAY", ignoreCase = true) && !resolvedHost.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.test_details_ping_gateway_target, resolvedHost),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatRow(stringResource(id = R.string.test_details_ping_sent), sent)
                StatRow(stringResource(id = R.string.detail_label_packet_loss), lossText)
                StatRow(stringResource(id = R.string.detail_label_avg_rtt), avg)
                StatRow(stringResource(id = R.string.detail_label_min_rtt), min)
                StatRow(stringResource(id = R.string.detail_label_max_rtt), max)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.test_details_ping_samples_header),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag(TestExecutionTags.PING_SAMPLES_LIST),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                samples.forEachIndexed { index, sample ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        tonalElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.test_details_ping_sample_label, sample.seq ?: index),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        Text(
                            text = stringResource(
                                id = R.string.test_details_ping_sample_value,
                                sample.time ?: "-",
                                sample.ttl ?: "-"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
    }

    @Composable
    private fun StatRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }

    private fun groupSamplesPreserveOrder(samples: List<PingSample>): Map<String?, List<PingSample>> {
        val order = LinkedHashMap<String?, MutableList<PingSample>>()
        samples.forEach { sample ->
            val key = sample.target ?: sample.host
            order.getOrPut(key) { mutableListOf() }.add(sample)
        }
        return order
    }
}
