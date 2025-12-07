package com.app.miklink.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.scale
import com.app.miklink.data.pdf.ExportColumn
import com.app.miklink.data.pdf.PdfExportConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun PdfExportDialog(
    clientName: String,
    globalIncludeEmpty: Boolean = true,
    globalColumns: Set<String> = ExportColumn.values().map { it.name }.toSet(),
    globalReportTitle: String = "Collaudo Cablaggio di Rete",
    globalHideEmptyColumns: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (PdfExportConfig) -> Unit
) {
    var reportTitle by remember { mutableStateOf(globalReportTitle) }
    
    var showSignatures by remember { mutableStateOf(true) }
    var signatureLeftLabel by remember { mutableStateOf("Collaudatore") }
    var signatureRightLabel by remember { mutableStateOf("Elettricista") }
    var selectedOrientation by remember { mutableStateOf(com.app.miklink.data.pdf.PdfPageOrientation.PORTRAIT) }
    
    var isExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Esporta PDF") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Genera report PDF per $clientName?",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Collapsible Override Section
                Card(
                     modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
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
                                    "Override Preferenze",
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
                                    label = { Text("Titolo Report") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                HorizontalDivider()

                                // Page Orientation
                                Column {
                                    Text("Orientamento Pagina", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = selectedOrientation == com.app.miklink.data.pdf.PdfPageOrientation.PORTRAIT,
                                            onClick = { selectedOrientation = com.app.miklink.data.pdf.PdfPageOrientation.PORTRAIT }
                                        )
                                        Text("Portrait", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.width(16.dp))
                                        RadioButton(
                                            selected = selectedOrientation == com.app.miklink.data.pdf.PdfPageOrientation.LANDSCAPE,
                                            onClick = { selectedOrientation = com.app.miklink.data.pdf.PdfPageOrientation.LANDSCAPE }
                                        )
                                        Text("Landscape", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }

                                HorizontalDivider()

                                // Signatures
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Firme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                        Switch(
                                            checked = showSignatures,
                                            onCheckedChange = { showSignatures = it },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                    
                                    if (showSignatures) {
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = signatureLeftLabel,
                                                onValueChange = { signatureLeftLabel = it },
                                                label = { Text("Sx") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = signatureRightLabel,
                                                onValueChange = { signatureRightLabel = it },
                                                label = { Text("Dx") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
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
            Button(
                onClick = {
                    val orderedColumns = ExportColumn.values().filter { globalColumns.contains(it.name) }
                    
                    val config = PdfExportConfig(
                        title = reportTitle.ifBlank { "Collaudo Cablaggio di Rete" },
                        includeEmptyTests = globalIncludeEmpty,
                        columns = orderedColumns,
                        showSignatures = showSignatures,
                        signatureLeftLabel = signatureLeftLabel,
                        signatureRightLabel = signatureRightLabel,
                        orientation = selectedOrientation,
                        hideEmptyColumns = globalHideEmptyColumns
                    )
                    onConfirm(config)
                }
            ) {
                Text("Esporta PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}


