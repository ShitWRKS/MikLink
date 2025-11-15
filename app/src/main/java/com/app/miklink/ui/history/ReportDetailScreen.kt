package com.app.miklink.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.ui.common.TestSectionCard
import com.app.miklink.ui.history.model.ParsedResults
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
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "Physical Layer", "Edit")
    
    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportReportToPdf(it) }
    }

    LaunchedEffect(pdfStatus) {
        if (pdfStatus.isNotBlank()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(pdfStatus)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Report #${report?.reportId}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        val fileName = "report_${report?.reportId ?: ""}.pdf"
                        createDocumentLauncher.launch(fileName)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                }
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null) {
            CircularProgressIndicator(modifier = Modifier.padding(padding))
        } else {
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = index == selectedTabIndex,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) })
                    }
                }
                when (selectedTabIndex) {
                    0 -> SummaryTab(report = currentReport, results = parsedResults)
                    1 -> PhysicalLayerTab(results = parsedResults)
                    2 -> EditTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SummaryTab(report: com.app.miklink.data.db.model.Report, results: ParsedResults?) {
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
                    Text("Avg: ${last.avgRtt ?: "-"}")
                    Text("Min: ${last.minRtt ?: "-"}")
                    Text("Max: ${last.maxRtt ?: "-"}")
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
                            "time=${p.time ?: "N/A"} ttl=${p.ttl ?: "N/A"}",
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
                status = if (tdrList.any { it.status.contains("fail", true) }) "FAIL" else "INFO",
                icon = Icons.Default.Cable,
                statusColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("TdrResultCard"),
                detailsTestTag = "TdrDetailsView"
            ) {
                tdrList.forEach { r ->
                    Text("Status: ${r.status}", fontWeight = FontWeight.Bold)
                    r.cablePairs.forEach { pairMap ->
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
fun PhysicalLayerTab(results: ParsedResults?) {
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
                    0 -> SummaryTab(report = currentReport, results = parsedResults)
                    1 -> PhysicalLayerTab(results = parsedResults)
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
