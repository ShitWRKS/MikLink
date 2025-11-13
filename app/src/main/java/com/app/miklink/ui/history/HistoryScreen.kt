package com.app.miklink.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.data.db.model.Client
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    
    var showDialog by remember { mutableStateOf(false) }
    var selectedClientId by remember { mutableStateOf<Long?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { selectedClientId?.let { clientId -> viewModel.exportProjectReportToPdf(clientId, it) } }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Test Report History") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Project Report")
            }
        }
    ) { padding ->
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Select Client for Report") },
                text = {
                    LazyColumn {
                        items(clients) { client ->
                            ListItem(
                                headlineContent = { Text(client.companyName) },
                                modifier = Modifier.clickable {
                                    selectedClientId = client.clientId
                                    showDialog = false
                                    createDocumentLauncher.launch("Project_Report_${client.companyName}.pdf")
                                }
                            )
                        }
                    }
                },
                confirmButton = { }
            )
        }

        LazyColumn(modifier = Modifier.padding(padding)) {
            if (reports.isEmpty()) {
                item { Text("No reports found.", modifier = Modifier.padding(padding)) }
            }
            items(reports) { report ->
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(report.timestamp))
                ListItem(
                    headlineContent = { Text(report.socketName ?: "No Socket ID") },
                    supportingContent = { Text("Tested on: $formattedDate") },
                    leadingContent = {
                        Icon(
                            imageVector = if (report.overallStatus == "Success") Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = report.overallStatus,
                            tint = if (report.overallStatus == "Success") Color(0xFF388E3C) else MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { navController.navigate("report_detail/${report.reportId}") }
                )
            }
        }
    }
}
