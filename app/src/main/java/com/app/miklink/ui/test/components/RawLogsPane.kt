/*
 * Purpose: Render a bounded, scrollable list of execution logs with auto-scroll to the latest entry.
 * Inputs: List of sanitized log lines, optional title, empty state label, and modifiers for layout/styling.
 * Outputs: Compose UI showing logs with semantics tags for instrumentation tests.
 */
package com.app.miklink.ui.test.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RawLogsPane(
    logs: List<String>,
    emptyLabel: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    autoScroll: Boolean = true,
    colorize: Boolean = false,
    minHeight: Dp = 140.dp,
    maxHeight: Dp = 260.dp
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (autoScroll && logs.isNotEmpty()) {
            listState.scrollToItem(logs.lastIndex)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .clip(RoundedCornerShape(12.dp))
            .testTag(TestExecutionTags.LOG_PANE),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (logs.isEmpty()) {
                Text(
                    text = emptyLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    itemsIndexed(logs) { index, line ->
                        val color = when {
                            colorize && line.contains("ERRORE", true) -> MaterialTheme.colorScheme.error
                            colorize && line.contains("FAIL", true) -> MaterialTheme.colorScheme.error
                            colorize && line.contains("FALLITO", true) -> MaterialTheme.colorScheme.error
                            colorize && (line.contains("SUCCESSO", true) || line.contains("PASS", true)) -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .testTag("${TestExecutionTags.LOG_LINE}-$index")
                        )
                    }
                }
            }
        }
    }
}
