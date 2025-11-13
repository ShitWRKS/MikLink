package com.app.miklink.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
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

    var selectedTabIndex by remember { mutableStateOf(0) }
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
        if (report == null) {
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
                    0 -> SummaryTab(report = report!!, results = parsedResults)
                    1 -> PhysicalLayerTab(results = parsedResults)
                    2 -> EditTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SummaryTab(report: com.app.miklink.data.db.model.Report, results: ParsedResults?) {
    // Display summary information: Link, LLDP, Ping
    Text("Summary Tab Content")
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
