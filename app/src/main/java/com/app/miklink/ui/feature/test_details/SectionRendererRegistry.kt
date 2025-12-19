/*
 * Purpose: Provide lookup of section-specific renderers for typed TestRunSnapshot sections.
 * Inputs: Section id.
 * Outputs: Renderer implementation able to render the given payload/status.
 * Notes: Default renderer shows a fallback text when no payload is available.
 */
package com.app.miklink.ui.feature.test_details

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.app.miklink.R
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionSnapshot

interface SectionRenderer {
    @Composable
    fun Render(section: TestSectionSnapshot, modifier: Modifier = Modifier)
}

class SectionRendererRegistry(
    private val renderers: Map<TestSectionId, SectionRenderer>,
    private val fallback: SectionRenderer = object : SectionRenderer {
        @Composable
        override fun Render(section: TestSectionSnapshot, modifier: Modifier) {
            Text(stringResource(id = R.string.test_details_no_content))
        }
    }
) {
    fun rendererFor(id: TestSectionId): SectionRenderer = renderers[id] ?: fallback
}
