package com.app.miklink.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.data.db.model.Report
import com.app.miklink.ui.history.model.ReportsByClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val reportsByClient by viewModel.reportsByClient.collectAsStateWithLifecycle()
    val pdfStatus by viewModel.pdfStatus.collectAsStateWithLifecycle()

    var expandedClientId by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pdfStatus) {
        if (pdfStatus.isNotBlank()) {
            snackbarHostState.showSnackbar(pdfStatus)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Test History") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        if (reportsByClient.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No test reports yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Run your first test to see reports here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reportsByClient, key = { it.client?.clientId ?: -1 }) { clientData ->
                    ClientReportsCard(
                        clientData = clientData,
                        isExpanded = expandedClientId == clientData.client?.clientId,
                        onToggleExpand = {
                            expandedClientId = if (expandedClientId == clientData.client?.clientId) null
                            else clientData.client?.clientId
                        },
                        onExportBatch = { clientReports ->
                            // Export batch PDF for this client
                        },
                        onReportEdit = { reportId ->
                            navController.navigate("report_detail/$reportId")
                        },
                        onReportDelete = { reportId ->
                            showDeleteDialog = reportId
                        },
                        onReportRepeat = { report ->
                            // TODO: Navigate to test with pre-filled params
                        },
                        viewModel = viewModel
                    )
                }
            }
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { reportId ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                title = { Text("Delete Report?") },
                text = { Text("This action cannot be undone. The report will be permanently deleted.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteReport(reportId)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ClientReportsCard(
    clientData: ReportsByClient,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onExportBatch: (ReportsByClient) -> Unit,
    onReportEdit: (Long) -> Unit,
    onReportDelete: (Long) -> Unit,
    onReportRepeat: (Report) -> Unit,
    viewModel: HistoryViewModel
) {
    val exportBatchLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportClientReports(clientData, it) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clientData.client?.companyName ?: "Unknown Client",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text("${clientData.totalTests} tests")
                        }
                        if (clientData.passedTests > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text("✓ ${clientData.passedTests}")
                            }
                        }
                        if (clientData.failedTests > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text("✗ ${clientData.failedTests}")
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        exportBatchLauncher.launch("${clientData.client?.companyName ?: "Client"}_Reports.pdf")
                    }) {
                        Icon(Icons.Default.PictureAsPdf, "Export all")
                    }
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider()
                    clientData.reports.forEach { report ->
                        ReportListItem(
                            report = report,
                            onEdit = { onReportEdit(report.reportId) },
                            onDelete = { onReportDelete(report.reportId) },
                            onRepeat = { onReportRepeat(report) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ReportListItem(
    report: Report,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.socketName ?: "Unnamed Socket",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("${report.floor ?: "-"}/${report.room ?: "-"}")
                    append(" • ")
                    append(
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(report.timestamp))
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Badge(
            containerColor = if (report.overallStatus == "PASS")
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                report.overallStatus,
                fontWeight = FontWeight.Bold
            )
        }

        Row {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit report", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete report", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onRepeat) {
                Icon(Icons.Default.Refresh, "Repeat test", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
