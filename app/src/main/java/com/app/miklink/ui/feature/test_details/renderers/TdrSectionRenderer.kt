/*
 * Purpose: Render TDR entries with distance/status/description.
 * Inputs: TestSectionSnapshot carrying TestSectionPayload.Tdr.
 * Outputs: List of entries with simple labels.
 * Notes: Shows fallback text when no entries are present.
 */
package com.app.miklink.ui.feature.test_details.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.core.domain.test.model.TestSectionPayload
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.ui.feature.test_details.SectionRenderer

class TdrSectionRenderer : SectionRenderer {
    @Composable
    override fun Render(section: TestSectionSnapshot, modifier: Modifier) {
        val payload = section.payload as? TestSectionPayload.Tdr
        val entries = payload?.entries.orEmpty()
        if (entries.isEmpty()) {
            Text(stringResource(id = R.string.test_details_tdr_empty), style = MaterialTheme.typography.bodyMedium)
            return
        }
        Column(modifier = modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                StatRow(label = stringResource(id = R.string.test_details_tdr_status_label), value = entry.status ?: "-")
                entry.distance?.let {
                    StatRow(label = stringResource(id = R.string.test_details_tdr_distance_label), value = it)
                }
                entry.description?.let {
                    StatRow(label = stringResource(id = R.string.test_details_tdr_pair_label), value = it)
                }
                if (index < entries.lastIndex) {
                    Spacer(modifier = Modifier.padding(bottom = 8.dp))
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
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
