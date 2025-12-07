package com.app.miklink.ui.history

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.alpha
import com.app.miklink.ui.common.TestSectionCard
import com.app.miklink.ui.history.model.ParsedResults
import com.app.miklink.data.db.model.Report
import com.app.miklink.utils.normalizeTime
import com.app.miklink.utils.normalizeLinkSpeed
import com.app.miklink.utils.normalizeLinkStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.print.PrintDocumentAdapter
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    navController: NavController,
    viewModel: ReportDetailViewModel = hiltViewModel()
) {
    val report by viewModel.report.collectAsStateWithLifecycle()
    val parsedResults by viewModel.parsedResults.collectAsStateWithLifecycle()
    val pdfStatus by viewModel.pdfStatus.collectAsStateWithLifecycle()
    val clientName by viewModel.clientName.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    LaunchedEffect(pdfStatus) {
        if (pdfStatus.isNotBlank()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(pdfStatus)
            }
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
                android.util.Log.d("ReportPDF", "LaunchedEffect loading HTML, length=${pdfHtmlToPrint!!.length}")
                webView.loadDataWithBaseURL("http://localhost/", pdfHtmlToPrint!!, "text/html", "UTF-8", null)
            }
        }
    }

    // Invisible WebView for printing
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
                        android.util.Log.d("ReportPDF", "onPageFinished url=$url, hasContent=${pdfHtmlToPrint != null}")
                        // Only print if we have content and URL indicates loaded content (not blank)
                        if (pdfHtmlToPrint != null && url?.startsWith("http") == true) {
                            val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                            if (printManager != null) {
                                val jobName = view?.tag as? String ?: "Report"
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
                title = { 
                    Text("$clientName - ${report?.socketName ?: "..."}")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            try {
                                val pdfFile = viewModel.generatePdfFileForCurrentReport()
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
                                    snackbarHostState.showSnackbar("Errore nella generazione del PDF")
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ReportDetail", "PDF export error", e)
                                snackbarHostState.showSnackbar("Errore: ${e.message}")
                            }
                        }
                    }) {
                        // Use same visual treatment as Settings top icon (primary tint, no extra bg)
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
                ,
                // Match Settings TopAppBar appearance
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                )
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null) {
            CircularProgressIndicator(modifier = Modifier.padding(padding))
        } else {
            // Single scrollable column (no tabs)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HERO SECTION - Large status badge with key info
                item {
                    HeroSection(report = currentReport, results = parsedResults)
                }
                
                // NETWORK INFO - Link & LLDP
                item {
                    NetworkInfoSection(results = parsedResults)
                }
                
                // TEST RESULTS - Ping & TDR
                item {
                    TestResultsSection(results = parsedResults)
                }
                
                // EDIT SECTION - Socket name & notes
                item {
                    EditSection(
                        socketName = viewModel.socketName.collectAsState().value,
                        notes = viewModel.notes.collectAsState().value,
                        onSocketNameChange = { viewModel.socketName.value = it },
                        onNotesChange = { viewModel.notes.value = it },
                        onSave = { viewModel.updateReportDetails() }
                    )
                }
                
                // ACTIONS SECTION - Repeat, Delete
                item {
                    ActionsSection(
                        onRepeat = {
                            // TODO: Implement repeat logic with navigation
                        },
                        onDelete = {
                            // TODO: Implement delete with confirmation
                        }
                    )
                }
            }
        }
    }
}

// HERO SECTION - Visual impact with large status
@Composable
fun HeroSection(report: Report, results: ParsedResults?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (report.overallStatus == "PASS")
                androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                androidx.compose.ui.graphics.Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Large Status Badge
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (report.overallStatus == "PASS")
                    androidx.compose.ui.graphics.Color(0xFF4CAF50)
                else
                    androidx.compose.ui.graphics.Color(0xFFF44336),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Icon(
                        imageVector = if (report.overallStatus == "PASS") 
                            Icons.Default.CheckCircle 
                        else 
                            Icons.Default.Cancel,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // Status Text
            Text(
                text = report.overallStatus,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (report.overallStatus == "PASS")
                    androidx.compose.ui.graphics.Color(0xFF4CAF50)
                else
                    androidx.compose.ui.graphics.Color(0xFFF44336)
            )
            
            // Date & Time
            Text(
                text = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.ITALIAN)
                    .format(Date(report.timestamp)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Ping stat
                val pingResult = results?.ping?.lastOrNull()
                QuickStatItem(
                    label = "Ping Avg",
                    value = normalizeTime(pingResult?.avgRtt),
                    icon = Icons.Default.Wifi
                )
                
                // Packet Loss
                QuickStatItem(
                    label = "Loss",
                    value = "${pingResult?.packetLoss ?: "0"}%",
                    icon = Icons.Default.Wifi
                )
                
                // Link Speed
                val linkSpeed = results?.link?.get("speed")
                QuickStatItem(
                    label = "Speed",
                    value = normalizeLinkSpeed(linkSpeed),
                    icon = Icons.Default.Link
                )
            }
        }
    }
}

@Composable
fun QuickStatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// NETWORK INFO SECTION - Always open
@Composable
fun NetworkInfoSection(results: ParsedResults?) {
    if (results?.link == null && results?.lldp == null) return
    
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Network Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            // Link Info
            results?.link?.let { linkMap ->
                Text(
                    "Link",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                linkMap.forEach { (key, value) ->
                    val normalizedValue = when(key.lowercase()) {
                        "status" -> normalizeLinkStatus(value)
                        "speed" -> normalizeLinkSpeed(value)
                        else -> value
                    }
                    InfoRow(label = key, value = normalizedValue)
                }
                Spacer(Modifier.height(16.dp))
            }
            
            // LLDP Info
            results?.lldp?.let { lldpList ->
                if (lldpList.isNotEmpty()) {
                    Text(
                        "LLDP Neighbors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    lldpList.forEach { neighbor ->
                        Text(
                            neighbor.identity ?: "Unknown",
                            fontWeight = FontWeight.Bold
                        )
                        InfoRow("Interface", neighbor.interfaceName ?: "-")
                        InfoRow("Capabilities", neighbor.systemCaps ?: "-")
                        neighbor.vlanId?.let { InfoRow("VLAN", it) }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// TEST RESULTS SECTION - Always open
@Composable
fun TestResultsSection(results: ParsedResults?) {
    if (results?.ping == null && results?.tdr == null) return
    
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Test Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            
            // Ping Results
            results?.ping?.let { pingList ->
                val summary = pingList.lastOrNull()
                Text(
                    "Ping Test",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                InfoRow("Min RTT", normalizeTime(summary?.minRtt))
                InfoRow("Avg RTT", normalizeTime(summary?.avgRtt))
                InfoRow("Max RTT", normalizeTime(summary?.maxRtt))
                InfoRow("Packet Loss", "${summary?.packetLoss ?: "0"}%")
                Spacer(Modifier.height(16.dp))
            }
            
            // TDR Results
            results?.tdr?.let { tdrList ->
                if (tdrList.isNotEmpty()) {
                    Text(
                        "Cable Test (TDR)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    tdrList.forEach { tdr ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("Status")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (tdr.status.contains("ok", true)) 
                                        Icons.Default.CheckCircle 
                                    else 
                                        Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (tdr.status.contains("ok", true))
                                        androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                    else
                                        androidx.compose.ui.graphics.Color(0xFFF44336),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    tdr.status,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// EDIT SECTION
@Composable
fun EditSection(
    socketName: String,
    notes: String,
    onSocketNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Edit Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            OutlinedTextField(
                value = socketName,
                onValueChange = onSocketNameChange,
                label = { Text("Socket Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = onSave,
                modifier = Modifier.align(androidx.compose.ui.Alignment.End)
            ) {
                Text("Save Changes")
            }
        }
    }
}

// ACTIONS SECTION
@Composable
fun ActionsSection(
    onRepeat: () -> Unit,
    onDelete: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Elimina
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Elimina")
                }
                
                // 2. Ripeti
                Button(
                    onClick = onRepeat,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Ripeti Test")
                }
            }
        }
    }
}

@Composable
fun SummaryTab(results: ParsedResults?) {
    // Nuova UI con card espandibili riutilizzate
    if (results == null) {
        Text("Nessun risultato parsato", modifier = Modifier.padding(16.dp))
        return
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // LINK CARD (Map<String, String>?)
        val linkMap = results.link
        if (!linkMap.isNullOrEmpty()) {
            TestSectionCard(
                title = "Link",
                status = "INFO", // Non abbiamo stato persistito: uso INFO
                icon = Icons.Default.Link,
                statusColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.testTag("LinkResultCard"),
                detailsTestTag = "LinkDetailsView"
            ) {
                linkMap.forEach { (k, v) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(k)
                        Text(v, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // LLDP CARD (List<NeighborDetail>?)
        val lldpList = results.lldp
        if (!lldpList.isNullOrEmpty()) {
            TestSectionCard(
                title = "LLDP",
                status = "INFO",
                icon = Icons.Default.Devices,
                statusColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("LldpResultCard"),
                detailsTestTag = "LldpDetailsView"
            ) {
                lldpList.forEach { n ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(n.identity ?: "N/A", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Interface")
                            Text(n.interfaceName ?: "-", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Caps")
                            Text(n.systemCaps ?: "-", fontFamily = FontFamily.Monospace)
                        }
                        if (!n.vlanId.isNullOrBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("VLAN")
                                Text(n.vlanId, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!n.voiceVlanId.isNullOrBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Voice VLAN")
                                Text(n.voiceVlanId, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        // PING CARD (List<PingResult>?)
        val pingList = results.ping
        if (!pingList.isNullOrEmpty()) {
            // Calcolo alcune metriche aggregate (ultimo elemento contiene summary in source test) se presente
            val last = pingList.last()
            TestSectionCard(
                title = "Ping",
                status = when (last.packetLoss?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull()) {
                    null -> "INFO"
                    0.0 -> "PASS"
                    else -> "FAIL"
                },
                icon = Icons.Default.Wifi,
                statusColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("PingResultCard"),
                detailsTestTag = "PingDetailsView"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Avg: ${normalizeTime(last.avgRtt)}")
                    Text("Min: ${normalizeTime(last.minRtt)}")
                    Text("Max: ${normalizeTime(last.maxRtt)}")
                    Text("Loss: ${(last.packetLoss ?: "-")}%)")
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                }
                // Dettaglio ping individuali
                pingList.forEach { p ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ping #${p.seq ?: "?"}")
                        Text(
                            "time=${normalizeTime(p.time)} ttl=${p.ttl ?: "N/A"}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // TDR CARD (List<CableTestResult>?)
        val tdrList = results.tdr
        if (!tdrList.isNullOrEmpty()) {
            TestSectionCard(
                title = "TDR",
                status = when {
                    tdrList.any { it.status.contains("fail", ignoreCase = true) || it.status.contains("short", ignoreCase = true) } -> "FAIL"
                    tdrList.any {
                        val s = it.status.lowercase()
                        s in listOf("ok", "open", "link-ok")
                    } -> "PASS"
                    else -> "INFO"
                },
                icon = Icons.Default.Cable,
                statusColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("TdrResultCard"),
                detailsTestTag = "TdrDetailsView"
            ) {
                tdrList.forEach { r ->
                    Text("Status: ${r.status}", fontWeight = FontWeight.Bold)
                    r.cablePairs?.forEach { pairMap ->
                        pairMap.forEach { (k, v) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(k)
                                Text(v, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun PhysicalLayerTab() {
    // Display TDR results
    Text("Physical Layer Tab Content")
}

@Composable
fun EditTab(viewModel: ReportDetailViewModel) {
    val socketName by viewModel.socketName.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = socketName, onValueChange = { viewModel.socketName.value = it }, label = { Text("Socket Name") }, singleLine = true)
        OutlinedTextField(value = notes, onValueChange = { viewModel.notes.value = it }, label = { Text("Notes") })
        Button(onClick = { viewModel.updateReportDetails() }) {
            Text("Save Changes")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    navController: NavController,
    stateProvider: ReportDetailScreenStateProvider
) {
    val report by stateProvider.report.collectAsStateWithLifecycle()
    val parsedResults by stateProvider.parsedResults.collectAsStateWithLifecycle()
    val pdfStatus by stateProvider.pdfStatus.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "Physical Layer", "Edit")
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(pdfStatus) { if (pdfStatus.isNotBlank()) coroutineScope.launch { snackbarHostState.showSnackbar(pdfStatus) } }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Report #${report?.reportId}") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } }
                ,
                // Make consistent with Settings top bar
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                )
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null) {
            CircularProgressIndicator(modifier = Modifier.padding(padding))
        } else {
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title -> Tab(selected = index == selectedTabIndex, onClick = { selectedTabIndex = index }, text = { Text(title) }) }
                }
                when (selectedTabIndex) {
                    0 -> SummaryTab(results = parsedResults)
                    1 -> PhysicalLayerTab()
                    2 -> EditTabFake(stateProvider)
                }
            }
        }
    }
}

@Composable
private fun EditTabFake(stateProvider: ReportDetailScreenStateProvider) {
    val socketName by stateProvider.socketName.collectAsState()
    val notes by stateProvider.notes.collectAsState()
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = socketName, onValueChange = { stateProvider.socketName.value = it }, label = { Text("Socket Name") }, singleLine = true)
        OutlinedTextField(value = notes, onValueChange = { stateProvider.notes.value = it }, label = { Text("Notes") })
        Button(onClick = { stateProvider.updateReportDetails() }) { Text("Save Changes") }
    }
}
