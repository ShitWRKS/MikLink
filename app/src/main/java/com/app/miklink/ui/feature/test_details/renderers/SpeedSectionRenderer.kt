/*
 * Purpose: Render speed test payload metrics.
 * Inputs: TestSectionSnapshot carrying TestSectionPayload.Speed.
 * Outputs: Key/value rows for TCP/UDP throughput, ping, jitter, loss, server, warning.
 * Notes: Shows fallback text when payload missing.
 */
package com.app.miklink.ui.feature.test_details.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.test.model.TestSectionPayload
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.ui.feature.test_details.SectionRenderer

class SpeedSectionRenderer : SectionRenderer {
    @Composable
    override fun Render(section: TestSectionSnapshot, modifier: Modifier) {
        val payload = section.payload as? TestSectionPayload.Speed
        val data = payload?.data
        if (data == null) {
            Text(stringResource(id = R.string.test_details_speed_empty), style = MaterialTheme.typography.bodyMedium)
            return
        }
        val warningText = data.warning ?: if (data.isCpuSaturated()) {
            stringResource(id = R.string.test_details_speed_warning_fallback)
        } else {
            null
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier.fillMaxWidth()) {
            infoRow(stringResource(id = R.string.detail_label_server), data.serverAddress ?: "-")
            infoRow(
                stringResource(id = R.string.test_details_speed_tcp_download),
                data.tcpDownload.cleanedThroughput()
            )
            infoRow(
                stringResource(id = R.string.test_details_speed_tcp_upload),
                data.tcpUpload.cleanedThroughput()
            )
            infoRow(
                stringResource(id = R.string.test_details_speed_udp_download),
                data.udpDownload.cleanedThroughput()
            )
            infoRow(
                stringResource(id = R.string.test_details_speed_udp_upload),
                data.udpUpload.cleanedThroughput()
            )
            infoRow(stringResource(id = R.string.test_details_speed_ping), data.ping ?: "-")
            infoRow(stringResource(id = R.string.test_details_speed_jitter), data.jitter ?: "-")
            infoRow(stringResource(id = R.string.test_details_speed_loss), data.loss ?: "-")
            warningText?.let { text ->
                WarningCard(text = text)
            }
        }
    }

    @Composable
    private fun infoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(0.55f)
                    .padding(start = 8.dp),
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    private fun WarningCard(text: String) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(id = R.string.detail_label_warning)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(id = R.string.test_details_speed_warning_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    private fun String?.cleanedThroughput(): String =
        this?.let { original ->
            val stripped = stripCpuTokens(original)
            stripped.takeIf { it.isNotBlank() } ?: original
        } ?: "-"

    private fun SpeedTestData.isCpuSaturated(): Boolean {
        val probe = listOf(
            status,
            ping,
            jitter,
            loss,
            tcpDownload,
            tcpUpload,
            udpDownload,
            udpUpload,
            warning
        ).joinToString(" ") { it ?: "" }
        return probe.contains("local-cpu-load:100%", ignoreCase = true) ||
            probe.contains("remote-cpu-load:100%", ignoreCase = true)
    }

    private fun stripCpuTokens(value: String): String {
        val cleaned = localCpuRegex.replace(remoteCpuRegex.replace(value, ""), "")
        return cleaned.replace("\\s{2,}".toRegex(), " ").replace(", ,", ",").trim().trim(',')
    }

    private val localCpuRegex = Regex("local-cpu-load[:=]\\s*([^,\\s)]+)", RegexOption.IGNORE_CASE)
    private val remoteCpuRegex = Regex("remote-cpu-load[:=]\\s*([^,\\s)]+)", RegexOption.IGNORE_CASE)

}
