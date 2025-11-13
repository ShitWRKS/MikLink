package com.app.miklink.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.ProbeConfig
import com.app.miklink.data.db.model.TestProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val probes by viewModel.probes.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()

    val selectedClient by viewModel.selectedClient.collectAsStateWithLifecycle()
    val selectedProbe by viewModel.selectedProbe.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val socketName by viewModel.socketName.collectAsStateWithLifecycle()
    val isProbeOnline by viewModel.isProbeOnline.collectAsStateWithLifecycle()

    val isTestButtonEnabled = selectedClient != null && selectedProbe != null && selectedProfile != null && socketName.isNotBlank() && isProbeOnline

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MikLink Dashboard") },
                actions = {
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    navController.navigate("test_execution/clientId=${selectedClient!!.clientId}&probeId=${selectedProbe!!.probeId}&profileId=${selectedProfile!!.profileId}&socketName=$socketName")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                enabled = isTestButtonEnabled
            ) {
                Text("AVVIA TEST")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DropdownPanel(
                label = "Client/Project",
                items = clients,
                selectedItem = selectedClient,
                onItemSelected = { viewModel.selectedClient.value = it },
                itemToString = { it.companyName }
            )

            DropdownPanel(
                label = "Probe",
                items = probes,
                selectedItem = selectedProbe,
                onItemSelected = { viewModel.selectedProbe.value = it },
                itemToString = { it.name },
                leadingIcon = { 
                    if (selectedProbe != null) {
                        Icon(
                            imageVector = if (isProbeOnline) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = if (isProbeOnline) "Online" else "Offline",
                            tint = if (isProbeOnline) Color.Green else Color.Red
                        )
                    }
                }
            )

            DropdownPanel(
                label = "Test Profile",
                items = profiles,
                selectedItem = selectedProfile,
                onItemSelected = { viewModel.selectedProfile.value = it },
                itemToString = { it.profileName }
            )

            OutlinedTextField(
                value = socketName,
                onValueChange = { viewModel.socketName.value = it },
                label = { Text("Socket ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownPanel(
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemToString: (T) -> String,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Text(label, style = MaterialTheme.typography.titleMedium)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedItem?.let(itemToString) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Select $label") },
            leadingIcon = leadingIcon,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemToString(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}
