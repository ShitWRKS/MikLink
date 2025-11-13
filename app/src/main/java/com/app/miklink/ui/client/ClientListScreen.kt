package com.app.miklink.ui.client

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    navController: NavController,
    viewModel: ClientListViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    var selectedClientIdForExport by remember { mutableStateOf<Long?>(null) }

    val createProjectReportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { 
            selectedClientIdForExport?.let { clientId ->
                viewModel.exportProjectReportToPdf(clientId, it)
                selectedClientIdForExport = null // Reset after use
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Manage Clients") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("client_add") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Client")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(clients) { client ->
                ListItem(
                    headlineContent = { Text(client.companyName) },
                    supportingContent = { Text(client.location ?: "No location specified") },
                    modifier = Modifier.clickable {
                        navController.navigate("client_edit/${client.clientId}")
                    },
                    trailingContent = {
                        IconButton(onClick = { 
                            selectedClientIdForExport = client.clientId
                            val fileName = "project_report_${client.companyName}.pdf"
                            createProjectReportLauncher.launch(fileName)
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Project Report")
                        }
                    }
                )
            }
        }
    }
}
