/*
 * Purpose: Render network configuration payload.
 * Inputs: TestSectionSnapshot carrying TestSectionPayload.Network.
 * Outputs: Key/value rows for mode/address/gateway/dns plus message.
 * Notes: Fallback text when payload missing.
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

class NetworkSectionRenderer : SectionRenderer {
    @Composable
    override fun Render(section: TestSectionSnapshot, modifier: Modifier) {
        val payload = section.payload as? TestSectionPayload.Network
        if (payload == null) {
            Text(stringResource(id = R.string.test_details_network_empty), style = MaterialTheme.typography.bodyMedium)
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
            infoRow(stringResource(id = R.string.detail_label_mode), payload.mode ?: "-")
            infoRow(stringResource(id = R.string.detail_label_address), payload.address ?: "-")
            infoRow(stringResource(id = R.string.detail_label_gateway), payload.gateway ?: "-")
            infoRow(stringResource(id = R.string.detail_label_dns), payload.dns ?: "-")
            payload.message?.let { msg ->
                Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
