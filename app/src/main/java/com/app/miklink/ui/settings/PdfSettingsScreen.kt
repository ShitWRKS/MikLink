package com.app.miklink.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.data.pdf.ExportColumn

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferenze Rapporto PDF") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
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
                    text = "IMPOSTAZIONI GENERALI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = pdfReportTitle,
                    onValueChange = { viewModel.updatePdfReportTitle(it) },
                    label = { Text("Titolo Default Report") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                )
            }

            HorizontalDivider()

            // Section: Data & Content
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CONTENUTO & DATI",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Include Empty Tests
                Row(
                   modifier = Modifier.fillMaxWidth().clickable { viewModel.updatePdfIncludeEmptyTests(!pdfIncludeEmpty) }.padding(vertical = 8.dp),
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Includi Test Vuoti", style = MaterialTheme.typography.titleMedium)
                        Text("Mostra anche test falliti o senza dati", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = pdfIncludeEmpty, onCheckedChange = { viewModel.updatePdfIncludeEmptyTests(it) })
                }
                
                // Hide Empty Columns
                Row(
                   modifier = Modifier.fillMaxWidth().clickable { viewModel.updatePdfHideEmptyColumns(!pdfHideEmptyColumns) }.padding(vertical = 8.dp),
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Nascondi Colonne Vuote", style = MaterialTheme.typography.titleMedium)
                        Text("Non stampare colonne senza dati (override)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = pdfHideEmptyColumns, onCheckedChange = { viewModel.updatePdfHideEmptyColumns(it) })
                }
            }

            HorizontalDivider()

            // Section: Columns
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "COLONNE DA STAMPARE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Seleziona le colonne da includere di default.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExportColumn.values().forEach { col ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newSet = if (currentColumns.contains(col.name)) {
                                    currentColumns - col.name
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
                            onCheckedChange = { checked ->
                                val newSet = if (checked) currentColumns + col.name else currentColumns - col.name
                                viewModel.updatePdfSelectedColumns(newSet)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(col.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
