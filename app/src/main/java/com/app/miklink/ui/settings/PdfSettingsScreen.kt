package com.app.miklink.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.R
import com.app.miklink.core.data.pdf.ExportColumn
import com.app.miklink.ui.testing.AgentUiTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val pdfReportTitle by viewModel.pdfReportTitle.collectAsStateWithLifecycle()
    val pdfIncludeEmpty by viewModel.pdfIncludeEmptyTests.collectAsStateWithLifecycle()
    val pdfHideEmptyColumns by viewModel.pdfHideEmptyColumns.collectAsStateWithLifecycle()
    val currentColumns by viewModel.pdfSelectedColumns.collectAsStateWithLifecycle()
    
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag(AgentUiTags.Settings.PDF_SCREEN),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_pdf_preferences)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = stringResource(R.string.reset_to_defaults))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section: General Settings
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.pdf_settings_general_section_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = pdfReportTitle,
                    onValueChange = { viewModel.updatePdfReportTitle(it) },
                    label = { Text(stringResource(R.string.pdf_report_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                )
            }

            HorizontalDivider()

            // Section: Data & Content
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.pdf_settings_content_section_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Include Empty Tests - Card style
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updatePdfIncludeEmptyTests(!pdfIncludeEmpty) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.pdf_settings_include_empty_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.pdf_settings_include_empty_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = pdfIncludeEmpty, onCheckedChange = { viewModel.updatePdfIncludeEmptyTests(it) })
                    }
                }
                
                // Hide Empty Columns - Card style
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updatePdfHideEmptyColumns(!pdfHideEmptyColumns) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ViewColumn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.pdf_settings_hide_empty_columns_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.pdf_settings_hide_empty_columns_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = pdfHideEmptyColumns, onCheckedChange = { viewModel.updatePdfHideEmptyColumns(it) })
                    }
                }
            }

            HorizontalDivider()

            // Section: Columns
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.pdf_settings_columns_section_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentColumns.size}/${ExportColumn.values().size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (currentColumns.size == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.pdf_settings_columns_section_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExportColumn.values().forEach { col ->
                    val isLastSelected = currentColumns.size == 1 && currentColumns.contains(col.name)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newSet = if (currentColumns.contains(col.name)) {
                                    if (currentColumns.size > 1) currentColumns - col.name else currentColumns
                                } else {
                                    currentColumns + col.name
                                }
                                viewModel.updatePdfSelectedColumns(newSet)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = currentColumns.contains(col.name),
                            enabled = !isLastSelected,
                            onCheckedChange = { checked ->
                                val newSet = if (checked) {
                                    currentColumns + col.name
                                } else {
                                    if (currentColumns.size > 1) currentColumns - col.name else currentColumns
                                }
                                viewModel.updatePdfSelectedColumns(newSet)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(getColumnLabelResId(col)), 
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isLastSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
    
    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
            title = { Text(stringResource(R.string.reset_to_defaults)) },
            text = { Text(stringResource(R.string.reset_pdf_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetPdfPreferences()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.reset_to_defaults))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun getColumnLabelResId(column: ExportColumn): Int {
    return when (column) {
        ExportColumn.SOCKET -> R.string.pdf_col_socket
        ExportColumn.DATE -> R.string.pdf_col_date
        ExportColumn.STATUS -> R.string.pdf_col_status
        ExportColumn.LINK_SPEED -> R.string.pdf_col_link_speed
        ExportColumn.NEIGHBOR -> R.string.pdf_col_neighbor
        ExportColumn.PING -> R.string.pdf_col_ping
        ExportColumn.TDR -> R.string.pdf_col_tdr
        ExportColumn.SPEED_TEST -> R.string.pdf_col_speed_test
    }
}
