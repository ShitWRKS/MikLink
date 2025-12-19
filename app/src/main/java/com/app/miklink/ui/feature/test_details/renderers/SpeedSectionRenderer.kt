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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.miklink.R
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = modifier.fillMaxWidth()) {
            infoRow(stringResource(id = R.string.detail_label_server), data.serverAddress ?: "-")
            infoRow(stringResource(id = R.string.test_details_speed_tcp_download), data.tcpDownload ?: "-")
            infoRow(stringResource(id = R.string.test_details_speed_tcp_upload), data.tcpUpload ?: "-")
            infoRow(stringResource(id = R.string.test_details_speed_udp_download), data.udpDownload ?: "-")
            infoRow(stringResource(id = R.string.test_details_speed_udp_upload), data.udpUpload ?: "-")
            infoRow(stringResource(id = R.string.test_details_speed_ping), data.ping ?: "-")
            infoRow(stringResource(id = R.string.test_details_speed_jitter), data.jitter ?: "-")
            infoRow(stringResource(id = R.string.test_details_speed_loss), data.loss ?: "-")
            data.warning?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    @Composable
    private fun infoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
