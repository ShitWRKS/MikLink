/*
 * Purpose: Render neighbor discovery results (LLDP/CDP) from typed payload.
 * Inputs: TestSectionSnapshot carrying TestSectionPayload.Neighbors.
 * Outputs: Column of neighbor entries with identity/interface/protocol.
 * Notes: Uses simple layout; fallback text when none.
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

class NeighborsSectionRenderer : SectionRenderer {
    @Composable
    override fun Render(section: TestSectionSnapshot, modifier: Modifier) {
        val payload = section.payload as? TestSectionPayload.Neighbors
        val entries = payload?.entries.orEmpty()
        if (entries.isEmpty()) {
            Text(stringResource(id = R.string.test_details_neighbors_empty), style = MaterialTheme.typography.bodyMedium)
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
            entries.forEach { neighbor ->
                Column {
                    Text(neighbor.identity ?: "-", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(id = R.string.detail_label_interface), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(neighbor.interfaceName ?: "-", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(id = R.string.detail_label_protocol), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(neighbor.discoveredBy ?: "-", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
