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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable

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

    // Search and filter state
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    
    // Repeat test confirmation
    var showRepeatDialog by remember { mutableStateOf(false) }
    var pendingRepeatReport by remember { mutableStateOf<Report?>(null) }

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
            Column {
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
                
                // Search bar
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { 
                        searchText = it
                        viewModel.updateSearchQuery(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Cerca per presa, cliente...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchText = ""
                                viewModel.updateSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
                
                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterStatus == FilterStatus.ALL,
                        onClick = { viewModel.updateFilterStatus(FilterStatus.ALL) },
                        label = { Text("Tutti") },
                        leadingIcon = if (filterStatus == FilterStatus.ALL) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = filterStatus == FilterStatus.PASS,
                        onClick = { viewModel.updateFilterStatus(FilterStatus.PASS) },
                        label = { Text("Solo PASS") },
                        leadingIcon = if (filterStatus == FilterStatus.PASS) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.2f),
                            selectedLabelColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        )
                    )
                    FilterChip(
                        selected = filterStatus == FilterStatus.FAIL,
                        onClick = { viewModel.updateFilterStatus(FilterStatus.FAIL) },
                        label = { Text("Solo FAIL") },
                        leadingIcon = if (filterStatus == FilterStatus.FAIL) {
                            { Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = androidx.compose.ui.graphics.Color(0xFFF44336).copy(alpha = 0.2f),
                            selectedLabelColor = androidx.compose.ui.graphics.Color(0xFFF44336)
                        )
                    )
                }
            }
        }
    ) { padding ->
        if (reportsByClient.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Nessun Report di Test",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Inizia un test per vedere i risultati qui",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(8.dp))
                                Text("Suggerimenti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(16.dp))
                            Row { Text("•  ", style = MaterialTheme.typography.bodyMedium); Text("Vai alla Dashboard per avviare un nuovo test", style = MaterialTheme.typography.bodyMedium) }
                            Spacer(Modifier.height(8.dp))
                            Row { Text("•  ", style = MaterialTheme.typography.bodyMedium); Text("I report vengono salvati automaticamente", style = MaterialTheme.typography.bodyMedium) }
                            Spacer(Modifier.height(8.dp))
                            Row { Text("•  ", style = MaterialTheme.typography.bodyMedium); Text("Puoi esportarli in PDF per condividerli", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate("dashboard") },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Vai alla Dashboard")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            pendingRepeatReport = report
                            showRepeatDialog = true
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
                        onExportSingleReport = { report ->
                            coroutineScope.launch {
                                try {
                                    snackbarHostState.showSnackbar("Generazione PDF...")
                                    val pdfFile = viewModel.generatePdfForSingleReport(report)
                                    
                                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
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
                                            snackbarHostState.showSnackbar("PDF generato!")
                                        } catch (e: android.content.ActivityNotFoundException) {
                                            snackbarHostState.showSnackbar("Nessun visualizzatore PDF trovato")
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Errore generazione PDF")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("HistoryPDF", "Error generating single PDF", e)
                                    snackbarHostState.showSnackbar("Errore: ${e.message}")
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
    
    // Repeat test confirmation dialog
    if (showRepeatDialog && pendingRepeatReport != null) {
        AlertDialog(
            onDismissRequest = { 
                showRepeatDialog = false
                pendingRepeatReport = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Ripetere il test?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Stai per ripetere il test per:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Presa: ${pendingRepeatReport?.socketName ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        "Cosa vuoi fare?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "\u2022 Sostituisci: elimina il test precedente e crea uno nuovo",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "\u2022 Nuovo Test: mantieni entrambi i test",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        "Assicurati di riposizionarti sulla stessa presa e verificare la connessione del cavo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Nuovo Test button (secondary choice, outlined) - LEFT
                    OutlinedButton(
                        onClick = {
                            val report = pendingRepeatReport
                            showRepeatDialog = false
                            pendingRepeatReport = null
                            
                            if (report != null) {
                                coroutineScope.launch {
                                    val route = viewModel.getRepeatTestRoute(report)
                                    if (route != null) {
                                        navController.navigate(route)
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            "Impossibile ripetere il test: profilo o sonda non trovati"
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Nuovo Test")
                    }
                    
                    // Sostituisci button (preferred choice, primary color) - RIGHT
                    Button(
                        onClick = {
                            val report = pendingRepeatReport
                            showRepeatDialog = false
                            pendingRepeatReport = null
                            
                            if (report != null) {
                                coroutineScope.launch {
                                    // Delete old report first
                                    viewModel.deleteReport(report.reportId)
                                    
                                    // Then navigate to new test
                                    val route = viewModel.getRepeatTestRoute(report)
                                    if (route != null) {
                                        navController.navigate(route)
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            "Impossibile ripetere il test: profilo o sonda non trovati"
                                        )
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Sostituisci")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRepeatDialog = false
                    pendingRepeatReport = null
                }) {
                    Text("Annulla")
                }
            }
        )
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
    onExportSingleReport: (Report) -> Unit = {},
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
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clientData.client?.companyName ?: "Unknown Client",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text("${clientData.totalTests} tests")
                        }
                        if (clientData.passedTests > 0) {
                            Badge(
                                containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                    )
                                    Text(
                                        "${clientData.passedTests}",
                                        color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (clientData.failedTests > 0) {
                            Badge(
                                containerColor = androidx.compose.ui.graphics.Color(0xFFF44336).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = androidx.compose.ui.graphics.Color(0xFFF44336)
                                    )
                                    Text(
                                        "${clientData.failedTests}",
                                        color = androidx.compose.ui.graphics.Color(0xFFF44336),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                            onRepeat = { onReportRepeat(report) },
                            onExportPdf = { onExportSingleReport(report) }
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
    onRepeat: () -> Unit,
    onExportPdf: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.socketName ?: "Unnamed Socket",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("d MMM, HH:mm", Locale.ITALIAN)
                    .format(Date(report.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(8.dp))

        // Enhanced Pass/Fail badge
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = if (report.overallStatus == "PASS")
                androidx.compose.ui.graphics.Color(0xFF4CAF50)
            else
                androidx.compose.ui.graphics.Color(0xFFF44336),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (report.overallStatus == "PASS") Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    report.overallStatus,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Action buttons: PDF, Repeat, Overflow menu
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExportPdf) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    "Export PDF",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onRepeat) {
                Icon(
                    Icons.Default.Refresh,
                    "Repeat test",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            
            // Overflow menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Elimina", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}
