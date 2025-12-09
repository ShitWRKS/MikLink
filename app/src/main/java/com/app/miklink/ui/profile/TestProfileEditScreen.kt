package com.app.miklink.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.utils.NetworkValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestProfileEditScreen(
    navController: NavController,
    viewModel: TestProfileViewModel = hiltViewModel()
) {
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    if (isSaved) {
        LaunchedEffect(Unit) { navController.popBackStack() }
    }

    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val profileDescription by viewModel.profileDescription.collectAsStateWithLifecycle()
    val runTdr by viewModel.runTdr.collectAsStateWithLifecycle()
    val runLinkStatus by viewModel.runLinkStatus.collectAsStateWithLifecycle()
    val runLldp by viewModel.runLldp.collectAsStateWithLifecycle()
    val runPing by viewModel.runPing.collectAsStateWithLifecycle()
    val pingTarget1 by viewModel.pingTarget1.collectAsStateWithLifecycle()
    val pingTarget2 by viewModel.pingTarget2.collectAsStateWithLifecycle()
    val pingTarget3 by viewModel.pingTarget3.collectAsStateWithLifecycle()
    val pingCount by viewModel.pingCount.collectAsStateWithLifecycle()
    val runSpeedTest by viewModel.runSpeedTest.collectAsStateWithLifecycle()
    val availableSlots by viewModel.availableSlots.collectAsStateWithLifecycle()

    var pingConfigExpanded by remember { mutableStateOf(false) }
    var showTarget2 by remember { mutableStateOf(pingTarget2.isNotBlank()) }
    var showTarget3 by remember { mutableStateOf(pingTarget3.isNotBlank()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit Profile" else "Add Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                enabled = profileName.isNotBlank()
            ) {
                Text("Save Profile")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(value = profileName, onValueChange = { viewModel.profileName.value = it }, label = { Text("Profile Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = profileDescription, onValueChange = { viewModel.profileDescription.value = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }

            item { HorizontalDivider() }

            item {
                SwitchListItem(checked = runTdr, onCheckedChange = { viewModel.runTdr.value = it }, headlineText = "Run TDR (Cable-Test)", supportingText = "Requires compatible probe")
                SwitchListItem(checked = runLinkStatus, onCheckedChange = { viewModel.runLinkStatus.value = it }, headlineText = "Run Link Status Test")
                SwitchListItem(checked = runLldp, onCheckedChange = { viewModel.runLldp.value = it }, headlineText = "Run LLDP/CDP Neighbor Test")
                SwitchListItem(checked = runPing, onCheckedChange = { viewModel.runPing.value = it }, headlineText = "Run Ping Test")
                SwitchListItem(checked = runSpeedTest, onCheckedChange = { viewModel.runSpeedTest.value = it }, headlineText = "Run Speed Test", supportingText = "Richiede configurazione server nel client")
            }

            if (runPing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text("Ping Configuration", style = MaterialTheme.typography.titleMedium) },
                                supportingContent = { Text("Configure ping targets (max 3)") },
                                trailingContent = {
                                    IconButton(onClick = { pingConfigExpanded = !pingConfigExpanded }) {
                                        Icon(
                                            if (pingConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (pingConfigExpanded) "Collapse" else "Expand"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (pingConfigExpanded) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Quick Fill Toggles
                                    Text("Quick Fill", style = MaterialTheme.typography.labelLarge)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val allSlotsFilled = availableSlots == 0
                                        
                                        ElevatedButton(
                                            onClick = { viewModel.fillLastAvailableTarget("DHCP_GATEWAY") },
                                            enabled = !allSlotsFilled,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Text("Gateway", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        
                                        ElevatedButton(
                                            onClick = { viewModel.fillLastAvailableTarget("8.8.8.8") },
                                            enabled = !allSlotsFilled,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Text("Google", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        
                                        ElevatedButton(
                                            onClick = { viewModel.fillLastAvailableTarget("1.1.1.1") },
                                            enabled = !allSlotsFilled,
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Text("Cloudflare", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    HorizontalDivider()

                                    // Custom Targets
                                    Text("Custom Targets", style = MaterialTheme.typography.labelLarge)
                                    
                                    // Target 1 (always visible)
                                    OutlinedTextField(
                                        value = pingTarget1,
                                        onValueChange = { viewModel.pingTarget1.value = it },
                                        label = { Text("Target 1") },
                                        placeholder = { Text("IP, hostname, or DHCP_GATEWAY") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        isError = pingTarget1.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget1),
                                        supportingText = {
                                            if (pingTarget1.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget1)) {
                                                Text("Invalid IP/hostname. No http://, https://, or www.", color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    )

                                    // Target 2 (optional)
                                    if (showTarget2) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            OutlinedTextField(
                                                value = pingTarget2,
                                                onValueChange = { viewModel.pingTarget2.value = it },
                                                label = { Text("Target 2") },
                                                placeholder = { Text("IP, hostname, or DHCP_GATEWAY") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                isError = pingTarget2.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget2),
                                                supportingText = {
                                                    if (pingTarget2.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget2)) {
                                                        Text("Invalid IP/hostname", color = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            )
                                            IconButton(
                                                onClick = {
                                                    viewModel.pingTarget2.value = ""
                                                    showTarget2 = false
                                                },
                                                modifier = Modifier.padding(top = 8.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Remove Target 2")
                                            }
                                        }
                                    }

                                    // Target 3 (optional)
                                    if (showTarget3) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            OutlinedTextField(
                                                value = pingTarget3,
                                                onValueChange = { viewModel.pingTarget3.value = it },
                                                label = { Text("Target 3") },
                                                placeholder = { Text("IP, hostname, or DHCP_GATEWAY") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                isError = pingTarget3.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget3),
                                                supportingText = {
                                                    if (pingTarget3.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget3)) {
                                                        Text("Invalid IP/hostname", color = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            )
                                            IconButton(
                                                onClick = {
                                                    viewModel.pingTarget3.value = ""
                                                    showTarget3 = false
                                                },
                                                modifier = Modifier.padding(top = 8.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Remove Target 3")
                                            }
                                        }
                                    }

                                    // Add target buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (!showTarget2) {
                                            OutlinedButton(
                                                onClick = { showTarget2 = true },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Add Target 2")
                                            }
                                        }
                                        if (!showTarget3 && showTarget2) {
                                            OutlinedButton(
                                                onClick = { showTarget3 = true },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Add Target 3")
                                            }
                                        }
                                    }

                                    HorizontalDivider()

                                    // Ping Count
                                    OutlinedTextField(
                                        value = pingCount,
                                        onValueChange = { viewModel.pingCount.value = it },
                                        label = { Text("Ping Count (1-20)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        supportingText = { Text("Numero di ping per ogni target (default: 4)") },
                                        isError = pingCount.toIntOrNull()?.let { it < 1 || it > 20 } ?: (pingCount.isNotBlank())
                                    )
                                    
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchListItem(checked: Boolean, onCheckedChange: (Boolean) -> Unit, headlineText: String, supportingText: String? = null) {
    ListItem(
        headlineContent = { Text(headlineText) },
        supportingContent = { supportingText?.let { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth()
    )
}
