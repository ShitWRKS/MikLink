package com.app.miklink.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    val vlanId by viewModel.vlanId.collectAsStateWithLifecycle()
    val staticIp by viewModel.staticIp.collectAsStateWithLifecycle()
    val staticSubnet by viewModel.staticSubnet.collectAsStateWithLifecycle()
    val staticGateway by viewModel.staticGateway.collectAsStateWithLifecycle()
    val pingTarget1 by viewModel.pingTarget1.collectAsStateWithLifecycle()
    val pingTarget2 by viewModel.pingTarget2.collectAsStateWithLifecycle()
    val pingTarget3 by viewModel.pingTarget3.collectAsStateWithLifecycle()
    val lastFloor by viewModel.lastFloor.collectAsStateWithLifecycle()
    val lastRoom by viewModel.lastRoom.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (viewModel.isEditing) "Edit Client" else "New Client") }) },
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
            OutlinedTextField(value = notes, onValueChange = { viewModel.notes.value = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())

            // Section: Sticky Fields
            Text("Sticky Fields", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = lastFloor, onValueChange = { viewModel.lastFloor.value = it }, label = { Text("Last Floor") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = lastRoom, onValueChange = { viewModel.lastRoom.value = it }, label = { Text("Last Room") }, modifier = Modifier.weight(1f), singleLine = true)
            }

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

            OutlinedTextField(value = vlanId, onValueChange = { viewModel.vlanId.value = it }, label = { Text("VLAN ID") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (networkMode == NetworkMode.STATIC) {
                OutlinedTextField(value = staticIp, onValueChange = { viewModel.staticIp.value = it }, label = { Text("Static IP Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = staticSubnet, onValueChange = { viewModel.staticSubnet.value = it }, label = { Text("Subnet Mask") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = staticGateway, onValueChange = { viewModel.staticGateway.value = it }, label = { Text("Gateway") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
            
            // Section: Test Targets
            Text("Custom Test Targets", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = pingTarget1, onValueChange = { viewModel.pingTarget1.value = it }, label = { Text("Ping Target 1 (e.g., DHCP_GATEWAY)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = pingTarget2, onValueChange = { viewModel.pingTarget2.value = it }, label = { Text("Ping Target 2") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = pingTarget3, onValueChange = { viewModel.pingTarget3.value = it }, label = { Text("Ping Target 3") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Spacer(Modifier.height(64.dp)) // Spacer for the bottom button
        }
    }
}