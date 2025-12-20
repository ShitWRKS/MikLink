/*
 * UI logs bottom sheet, input log lines and content slot, output persistent sheet with filters and copy.
 */
package com.app.miklink.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.ui.test.components.RawLogsPane
import android.content.ClipData
import android.content.ClipboardManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsBottomSheet(
    logs: List<String>,
    modifier: Modifier = Modifier,
    peekHeight: Dp = 84.dp,
    sheetTitle: String = stringResource(id = R.string.logs_sheet_title),
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(sheetState)
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }
    var followLogs by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf("") }
    val filteredLogs = remember(logs, filter) {
        if (filter.isBlank()) logs else logs.filter { it.contains(filter, ignoreCase = true) }
    }

    val clipLabel = stringResource(id = R.string.logs_sheet_title)

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        topBar = topBar,
        sheetPeekHeight = peekHeight,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        sheetTonalElevation = 4.dp,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(
                    text = stringResource(id = R.string.logs_sheet_handle_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = sheetTitle,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(id = R.string.logs_sheet_count, filteredLogs.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = followLogs,
                            onClick = { followLogs = !followLogs },
                            label = { Text(stringResource(id = R.string.logs_follow)) }
                        )
                        IconButton(
                            onClick = {
                                if (filteredLogs.isNotEmpty()) {
                                    val clip = ClipData.newPlainText(clipLabel, filteredLogs.joinToString("\n"))
                                    clipboardManager?.setPrimaryClip(clip)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(id = R.string.logs_copy),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(id = R.string.logs_filter_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.FilterAlt, contentDescription = null) },
                    trailingIcon = {
                        if (filter.isNotBlank()) {
                            IconButton(onClick = { filter = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                RawLogsPane(
                    logs = filteredLogs,
                    emptyLabel = stringResource(id = R.string.test_logs_empty),
                    title = null,
                    autoScroll = followLogs,
                    colorize = true,
                    minHeight = 160.dp,
                    maxHeight = 420.dp
                )
            }
        }
    ) { padding ->
        content(padding)
    }
}
