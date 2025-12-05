package com.app.miklink.ui.history

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.print.PrintDocumentAdapter
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.alpha

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pdfStatus) {
        if (pdfStatus.isNotBlank()) {
            snackbarHostState.showSnackbar(pdfStatus)
        }
    }

    // State for PDF printing
    var pdfHtmlToPrint by remember { mutableStateOf<String?>(null) }
    var pdfJobName by remember { mutableStateOf("") }
    
    // Keep reference to WebView for explicit control
    val webViewRef = remember { mutableStateOf<android.webkit.WebView?>(null) }

    // Load content when pdfHtmlToPrint changes
    LaunchedEffect(pdfHtmlToPrint) {
        webViewRef.value?.let { webView ->
            webView.tag = pdfJobName
            if (pdfHtmlToPrint != null) {
                android.util.Log.d("HistoryPDF", "LaunchedEffect loading HTML, length=${pdfHtmlToPrint!!.length}")
                webView.loadDataWithBaseURL("http://localhost/", pdfHtmlToPrint!!, "text/html", "UTF-8", null)
            }
        }
    }

    // Invisible WebView for printing - Always attached to prevent renderer crash
    AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = true
                // Disable hardware acceleration to prevent crashes on some devices
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        android.util.Log.d("HistoryPDF", "onPageFinished url=$url, hasContent=${pdfHtmlToPrint != null}")
                        // Only print if we have content and URL indicates loaded content (not blank)
                        if (pdfHtmlToPrint != null && url?.startsWith("http") == true) {
                            val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                            if (printManager != null) {
                                val jobName = view?.tag as? String ?: "History_Report"
                                val adapter = createPrintDocumentAdapter(jobName)
                                
                                val wrappedAdapter = object : PrintDocumentAdapter() {
                                    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback?, extras: Bundle?) {
                                        adapter.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
                                    }
                                    override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: WriteResultCallback?) {
                                        adapter.onWrite(pages, destination, cancellationSignal, callback)
                                    }
                                    override fun onFinish() {
                                        adapter.onFinish()
                                        // Cleanup: Clear content and reset state
                                        pdfHtmlToPrint = null
                                        view?.loadUrl("about:blank")
                                    }
                                }
                                
                                printManager.print(jobName, wrappedAdapter, PrintAttributes.Builder().build())
                            }
                        }
                    }
                }
                
                // Store reference
                webViewRef.value = this
            }
        },
        modifier = Modifier.size(1.dp).alpha(0f)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Test History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
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
                        onReportEdit = { reportId ->
                            navController.navigate("report_detail/$reportId")
                        },
                        onReportDelete = { reportId ->
                            showDeleteDialog = reportId
                        },
                        onReportRepeat = { report ->
                            // TODO: Navigate to test with pre-filled params
                        },
                        onExportAll = {
                            coroutineScope.launch {
                                try {
                                    snackbarHostState.showSnackbar("Generazione PDF in corso...")
                                    
                                    // Use iText to generate PDF directly
                                    val pdfFile = viewModel.generatePdfWithITextForClient(clientData)
                                    
                                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                                        // Open PDF with default viewer
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            pdfFile
                                        )
                                        
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        
                                        try {
                                            context.startActivity(intent)
                                            snackbarHostState.showSnackbar("PDF generato con successo!")
                                        } catch (e: android.content.ActivityNotFoundException) {
                                            snackbarHostState.showSnackbar("Nessun visualizzatore PDF trovato")
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Nessun dato da esportare")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("HistoryPDF", "Error generating PDF with iText", e)
                                    snackbarHostState.showSnackbar("Errore generazione PDF: ${e.message}")
                                }
                            }
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
    onReportEdit: (Long) -> Unit,
    onReportDelete: (Long) -> Unit,
    onReportRepeat: (Report) -> Unit,
    onExportAll: () -> Unit,
    viewModel: HistoryViewModel
) {
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
                    // Styled export icon: use same visual treatment as Settings (primary tint, no bg)
                    IconButton(onClick = { onExportAll() }) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Export all",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Expand/collapse button: explicit tint for consistency
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(Date(report.timestamp)),
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
