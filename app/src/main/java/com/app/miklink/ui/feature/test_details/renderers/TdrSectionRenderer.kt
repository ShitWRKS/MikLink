/*
 * Purpose: Render TDR entries with distance/status/description.
 * Inputs: TestSectionSnapshot carrying TestSectionPayload.Tdr.
 * Outputs: List of entries with simple labels.
 * Notes: Shows fallback text when no entries are present.
 */
package com.app.miklink.ui.feature.test_details.renderers

import androidx.compose.foundation.layout.Column
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
            entries.forEach { entry ->
                Text(
                    text = stringResource(id = R.string.test_details_tdr_status, entry.status ?: "-"),
                    fontWeight = FontWeight.SemiBold
                )
                entry.distance?.let {
                    Text(stringResource(id = R.string.test_details_tdr_distance, it))
                }
                entry.description?.let {
                    Text(stringResource(id = R.string.test_details_tdr_pair, it))
                }
                Text(
                    text = "",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}
