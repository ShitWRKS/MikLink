package com.app.miklink.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.data.db.model.NetworkMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditScreen(
    navController: NavController,
    viewModel: ClientEditViewModel = hiltViewModel()
) {
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    if (isSaved) {
        LaunchedEffect(Unit) { navController.popBackStack() }
    }

    val companyName by viewModel.companyName.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val networkMode by viewModel.networkMode.collectAsStateWithLifecycle()
    val staticIp by viewModel.staticIp.collectAsStateWithLifecycle()
    val staticSubnet by viewModel.staticSubnet.collectAsStateWithLifecycle()
    val staticGateway by viewModel.staticGateway.collectAsStateWithLifecycle()
    val staticCidr by viewModel.staticCidr.collectAsStateWithLifecycle()
    val minLinkRate by viewModel.minLinkRate.collectAsStateWithLifecycle()
    val socketPrefix by viewModel.socketPrefix.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit Client" else "New Client") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::saveClient,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                enabled = companyName.isNotBlank()
            ) {
                Text("Save Client")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Client Info
            Text("Client Info", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = companyName, onValueChange = { viewModel.companyName.value = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = location, onValueChange = { viewModel.location.value = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = socketPrefix, onValueChange = { viewModel.socketPrefix.value = it }, label = { Text("Socket ID Prefix") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = notes, onValueChange = { viewModel.notes.value = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())


            // Section: Network Settings
            Text("Network Settings", style = MaterialTheme.typography.titleMedium)

            // WORKAROUND for SegmentedButton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.networkMode.value = NetworkMode.DHCP },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (networkMode == NetworkMode.DHCP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (networkMode == NetworkMode.DHCP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("DHCP")
                }

                Button(
                    onClick = { viewModel.networkMode.value = NetworkMode.STATIC },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (networkMode == NetworkMode.STATIC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (networkMode == NetworkMode.STATIC) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Static")
                }
            }


            if (networkMode == NetworkMode.STATIC) {
                OutlinedTextField(value = staticCidr, onValueChange = { viewModel.staticCidr.value = it }, label = { Text("Static IP (CIDR), es. 172.16.0.1/24") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                // Campi legacy opzionali
                OutlinedTextField(value = staticIp, onValueChange = { viewModel.staticIp.value = it }, label = { Text("[Legacy] Static IP Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = staticSubnet, onValueChange = { viewModel.staticSubnet.value = it }, label = { Text("[Legacy] Subnet Mask") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = staticGateway, onValueChange = { viewModel.staticGateway.value = it }, label = { Text("Gateway") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }

            // Min Link Rate (lista fissa)
            Text("Min Link Rate PASS", style = MaterialTheme.typography.titleMedium)
            val options = listOf("10M", "100M", "1G", "10G")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { opt: String ->
                    FilterChip(
                        selected = minLinkRate == opt,
                        onClick = { viewModel.minLinkRate.value = opt },
                        label = { Text(opt) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(64.dp)) // Spacer for the bottom button
        }
    }
}