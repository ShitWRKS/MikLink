package com.app.miklink.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import com.app.miklink.core.data.pdf.ExportColumn
import com.app.miklink.core.data.pdf.PdfExportConfig
import com.app.miklink.core.data.pdf.PdfPageOrientation
import androidx.compose.ui.res.stringResource
import com.app.miklink.R
import com.app.miklink.ui.testing.AgentSemanticsConfig
import com.app.miklink.ui.testing.AgentUiTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportDialog(
    clientName: String,
    globalIncludeEmpty: Boolean = true,
    globalColumns: Set<String> = ExportColumn.values().map { it.name }.toSet(),
    globalReportTitle: String = stringResource(R.string.pdf_default_title),
    globalHideEmptyColumns: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (PdfExportConfig) -> Unit
) {
    var reportTitle by remember { mutableStateOf(globalReportTitle) }
    
    var showSignatures by remember { mutableStateOf(true) }
    
    val defaultSigLeft = stringResource(R.string.pdf_default_sig_left)
    val defaultSigRight = stringResource(R.string.pdf_default_sig_right)
    
    var signatureLeftLabel by remember { mutableStateOf(defaultSigLeft) }
    var signatureRightLabel by remember { mutableStateOf(defaultSigRight) }
    var selectedOrientation by remember { mutableStateOf(PdfPageOrientation.PORTRAIT) }
    
    var isExpanded by remember { mutableStateOf(false) }
    
    // Local states for Content & Data override
    var localIncludeEmpty by remember { mutableStateOf(globalIncludeEmpty) }
    var localColumns by remember { mutableStateOf(globalColumns) }
    var localHideEmptyColumns by remember { mutableStateOf(globalHideEmptyColumns) }
    var isColumnsExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        modifier = AgentSemanticsConfig.rootModifier().testTag(AgentUiTags.Report.PDF_DIALOG),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pdf_export_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pdf_export_confirm, clientName),
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Collapsible Override Section
                Card(
                     modifier = Modifier
                         .fillMaxWidth()
                         .testTag(AgentUiTags.Report.PDF_OPTIONS)
                         .clickable { isExpanded = !isExpanded },
                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.pdf_override_prefs),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Expanded Content
                        androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Report Title
                                OutlinedTextField(
                                    value = reportTitle,
                                    onValueChange = { reportTitle = it },
                                    label = { Text(stringResource(R.string.pdf_report_title)) },
                                    modifier = Modifier.fillMaxWidth().testTag(AgentUiTags.Report.PDF_TITLE),
                                    singleLine = true
                                )
                                
                                HorizontalDivider()

                                // Page Orientation
                                Column {
                                    Text(stringResource(R.string.pdf_orientation), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedOrientation == PdfPageOrientation.PORTRAIT,
                                            onClick = { selectedOrientation = PdfPageOrientation.PORTRAIT },
                                            modifier = Modifier.testTag(AgentUiTags.Report.PDF_ORIENTATION_PORTRAIT)
                                        )
                                        Text(stringResource(R.string.pdf_orientation_portrait), style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.width(16.dp))
                                        RadioButton(
                                            selected = selectedOrientation == PdfPageOrientation.LANDSCAPE,
                                            onClick = { selectedOrientation = PdfPageOrientation.LANDSCAPE },
                                            modifier = Modifier.testTag(AgentUiTags.Report.PDF_ORIENTATION_LANDSCAPE)
                                        )
                                        Text(stringResource(R.string.pdf_orientation_landscape), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }

                                HorizontalDivider()

                                // Signatures
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(R.string.pdf_signatures), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = showSignatures,
                                            onCheckedChange = { showSignatures = it },
                                            modifier = Modifier.scale(0.8f).testTag(AgentUiTags.Report.PDF_SIGNATURES)
                                        )
                                    }
                                    
                                    if (showSignatures) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = signatureLeftLabel,
                                                onValueChange = { signatureLeftLabel = it },
                                                label = { Text(stringResource(R.string.pdf_sig_left_label)) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = signatureRightLabel,
                                                onValueChange = { signatureRightLabel = it },
                                                label = { Text(stringResource(R.string.pdf_sig_right_label)) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                                
                                HorizontalDivider()
                                
                                // Content & Data Section
                                Column {
                                    Text(
                                        stringResource(R.string.pdf_settings_content_section_title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    
                                    // Include Empty Tests
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(AgentUiTags.Report.PDF_INCLUDE_EMPTY)
                                            .clickable { localIncludeEmpty = !localIncludeEmpty },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.FilterList,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.pdf_settings_include_empty_title),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = localIncludeEmpty,
                                            onCheckedChange = { localIncludeEmpty = it },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                    
                                    Spacer(Modifier.height(4.dp))
                                    
                                    // Hide Empty Columns
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag(AgentUiTags.Report.PDF_HIDE_EMPTY_COLUMNS)
                                            .clickable { localHideEmptyColumns = !localHideEmptyColumns },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.ViewColumn,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.pdf_settings_hide_empty_columns_title),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = localHideEmptyColumns,
                                            onCheckedChange = { localHideEmptyColumns = it },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                }
                                
                                HorizontalDivider()
                                
                                // Columns Selection (Collapsible)
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isColumnsExpanded = !isColumnsExpanded },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                stringResource(R.string.pdf_settings_columns_section_title),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            // i18n-ignore: compact selected/total numeric counter.
                                            Text(
                                                "${localColumns.size}/${ExportColumn.values().size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (localColumns.size == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            if (isColumnsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    androidx.compose.animation.AnimatedVisibility(visible = isColumnsExpanded) {
                                        Column(modifier = Modifier.padding(top = 8.dp)) {
                                            ExportColumn.values().forEach { col ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val newSet = if (localColumns.contains(col.name)) {
                                                                if (localColumns.size > 1) localColumns - col.name else localColumns
                                                            } else {
                                                                localColumns + col.name
                                                            }
                                                            localColumns = newSet
                                                        }
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = localColumns.contains(col.name),
                                                        onCheckedChange = { checked ->
                                                            val newSet = if (checked) {
                                                                localColumns + col.name
                                                            } else {
                                                                if (localColumns.size > 1) localColumns - col.name else localColumns
                                                            }
                                                            localColumns = newSet
                                                        }
                                                    )
                                                    Text(
                                                        getColumnLabel(col),
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val defaultTitle = stringResource(R.string.pdf_default_title)
            Button(
                modifier = Modifier.testTag(AgentUiTags.Report.PDF_CONFIRM),
                onClick = {
                    val orderedColumns = ExportColumn.values().filter { localColumns.contains(it.name) }
                    
                    val config = PdfExportConfig(
                        title = reportTitle.ifBlank { defaultTitle },
                        includeEmptyTests = localIncludeEmpty,
                        columns = orderedColumns,
                        showSignatures = showSignatures,
                        signatureLeftLabel = signatureLeftLabel,
                        signatureRightLabel = signatureRightLabel,
                        orientation = selectedOrientation,
                        hideEmptyColumns = localHideEmptyColumns
                    )
                    onConfirm(config)
                }
            ) {
                Text(stringResource(R.string.pdf_btn_export))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun getColumnLabel(column: ExportColumn): String {
    return when (column) {
        ExportColumn.SOCKET -> stringResource(R.string.pdf_col_socket)
        ExportColumn.DATE -> stringResource(R.string.pdf_col_date)
        ExportColumn.STATUS -> stringResource(R.string.pdf_col_status)
        ExportColumn.LINK_SPEED -> stringResource(R.string.pdf_col_link_speed)
        ExportColumn.NEIGHBOR -> stringResource(R.string.pdf_col_neighbor)
        ExportColumn.PING -> stringResource(R.string.pdf_col_ping)
        ExportColumn.TDR -> stringResource(R.string.pdf_col_tdr)
        ExportColumn.SPEED_TEST -> stringResource(R.string.pdf_col_speed_test)
    }
}
