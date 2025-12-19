/*
 * Purpose: Render link status payload with rate/status info.
 * Inputs: TestSectionSnapshot carrying TestSectionPayload.Link.
 * Outputs: Simple key/value rows for status and rate.
 * Notes: Uses Material theme colors; fallback text when data missing.
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
import com.app.miklink.utils.normalizeLinkSpeed
import com.app.miklink.utils.normalizeLinkStatus

class LinkSectionRenderer : SectionRenderer {
    @Composable
    override fun Render(section: TestSectionSnapshot, modifier: Modifier) {
        val payload = section.payload as? TestSectionPayload.Link
        val data = payload?.data
        if (data == null) {
            Text(stringResource(id = R.string.test_details_link_empty), style = MaterialTheme.typography.bodyMedium)
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth()) {
            InfoRow(stringResource(id = R.string.detail_label_status), normalizeLinkStatus(data.status))
            InfoRow(stringResource(id = R.string.detail_label_speed), normalizeLinkSpeed(data.rate))
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
