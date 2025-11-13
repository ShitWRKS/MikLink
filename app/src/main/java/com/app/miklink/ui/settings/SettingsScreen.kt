package com.app.miklink.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val json = viewModel.exportConfig()
                navController.context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importConfig(it) }
    }

    LaunchedEffect(backupStatus) {
        if (backupStatus.isNotBlank()) {
            snackbarHostState.showSnackbar(backupStatus)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Management Section
            SettingsListItem(
                headline = "Manage Clients",
                leadingIcon = Icons.Default.Business,
                onClick = { navController.navigate("client_list") }
            )
            SettingsListItem(
                headline = "Manage Probes",
                leadingIcon = Icons.Default.Router,
                onClick = { navController.navigate("probe_list") }
            )
            SettingsListItem(
                headline = "Manage Test Profiles",
                leadingIcon = Icons.Default.Speed,
                onClick = { navController.navigate("profile_list") }
            )

            Divider()

            // Backup and Restore Section
            Text("Backup and Restore", modifier = Modifier.padding(all = 16.dp), style = MaterialTheme.typography.titleMedium)
            SettingsListItem(
                headline = "Backup Configuration",
                leadingIcon = Icons.Default.CloudUpload,
                onClick = { exportLauncher.launch("mklink_config.json") }
            )
            SettingsListItem(
                headline = "Restore Configuration",
                leadingIcon = Icons.Default.CloudDownload,
                onClick = { importLauncher.launch(arrayOf("application/json")) }
            )
        }
    }
}

@Composable
private fun SettingsListItem(headline: String, leadingIcon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(headline) },
        leadingContent = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}