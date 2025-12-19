/*
 * Purpose: Compose screen for creating/updating clients and previewing socket-id formatting consistently with domain policy.
 * Inputs: ClientEditViewModel state (company details, network mode, socket prefix/separator/padding/suffix, speed test credentials) and navigation controller.
 * Outputs: Persisted client via view model actions and live previews for socket-id and speed test target.
 * Notes: Socket preview delegates to SocketIdLite.format to align with ADR-0004 separators; UI remains stateless beyond local expansion toggles.
 */
package com.app.miklink.ui.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.core.domain.model.NetworkMode
import com.app.miklink.core.domain.policy.socketid.SocketIdLite
import com.app.miklink.R

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
    val socketSuffix by viewModel.socketSuffix.collectAsStateWithLifecycle()
    val socketSeparator by viewModel.socketSeparator.collectAsStateWithLifecycle()
    val socketNumberPadding by viewModel.socketNumberPadding.collectAsStateWithLifecycle()

    // Speed Test configuration
    val speedTestServerAddress by viewModel.speedTestServerAddress.collectAsStateWithLifecycle()
    val speedTestServerUser by viewModel.speedTestServerUser.collectAsStateWithLifecycle()
    val speedTestServerPassword by viewModel.speedTestServerPassword.collectAsStateWithLifecycle()
    var speedTestPasswordVisible by remember { mutableStateOf(false) }

    // UI state - Network aperta, Socket e Speed chiuse di default
    var networkConfigExpanded by remember { mutableStateOf(true) }
    var socketConfigExpanded by remember { mutableStateOf(false) }
    var speedTestConfigExpanded by remember { mutableStateOf(false) }

    // Preview computations
    val socketPreview = remember(socketPrefix, socketSeparator, socketNumberPadding, socketSuffix) {
        SocketIdLite.format(
            prefix = socketPrefix,
            separator = socketSeparator,
            numberPadding = socketNumberPadding,
            suffix = socketSuffix,
            idNumber = 1
        )
    }

    val speedTestPreview = remember(speedTestServerAddress) {
        speedTestServerAddress.takeIf { it.isNotBlank() } ?: "Not configured"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit Client" else "New Client") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = com.app.miklink.R.string.back))
                    }
                },
                actions = {
                    // Quick save action
                    IconButton(
                        onClick = viewModel::saveClient,
                        enabled = companyName.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save"
                        )
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
            // === CLIENT INFO (Always Visible) ===
            SectionHeader(
                icon = Icons.Default.Business,
                title = "Client Info"
            )
            
            OutlinedTextField(
                value = companyName,
                onValueChange = { viewModel.companyName.value = it },
                label = { Text("Company Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = companyName.isBlank()
            )
            
            OutlinedTextField(
                value = location,
                onValueChange = { viewModel.location.value = it },
                label = { Text("Location") },
                placeholder = { Text("e.g., Sede, Filiale Roma") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.notes.value = it },
                label = { Text("Notes") },
                placeholder = { Text("Additional information...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            HorizontalDivider()

            // === NETWORK CONFIGURATION (Default Expanded) ===
            CollapsibleSection(
                icon = Icons.Default.Router,
                title = "Network",
                isExpanded = networkConfigExpanded,
                onToggle = { networkConfigExpanded = !networkConfigExpanded },
                preview = if (!networkConfigExpanded) {
                    if (networkMode == NetworkMode.DHCP) "DHCP" else "Static: ${staticCidr.ifBlank { "Not configured" }}"
                } else null
            ) {
                // Network Mode Toggle
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
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
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
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Static")
                    }
                }

                if (networkMode == NetworkMode.STATIC) {
                    OutlinedTextField(
                        value = staticCidr,
                        onValueChange = { viewModel.staticCidr.value = it },
                        label = { Text("Static IP (CIDR)") },
                        placeholder = { Text("es. 192.168.1.20/24") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("Formato: IP/Subnet") }
                    )
                    
                    OutlinedTextField(
                        value = staticGateway,
                        onValueChange = { viewModel.staticGateway.value = it },
                        label = { Text("Gateway") },
                        placeholder = { Text("es. 192.168.1.1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text("Min Link Rate", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                val options = listOf("10M", "100M", "1G", "10G")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { opt ->
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
            }

            // === SOCKET CONFIGURATION (Default Collapsed with Preview) ===
            CollapsibleSection(
                icon = Icons.Default.Cable,
                title = "Socket Configuration",
                isExpanded = socketConfigExpanded,
                onToggle = { socketConfigExpanded = !socketConfigExpanded },
                preview = if (!socketConfigExpanded) socketPreview else null
            ) {
                OutlinedTextField(
                    value = socketPrefix,
                    onValueChange = { viewModel.socketPrefix.value = it },
                    label = { Text("Prefix") },
                    placeholder = { Text("SW, NET, PR") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = socketSeparator,
                    onValueChange = { viewModel.socketSeparator.value = it },
                    label = { Text("Separator") },
                    placeholder = { Text("-") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Character between parts") }
                )

                Text("Number Padding", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..3).forEach { padding ->
                        FilterChip(
                            selected = socketNumberPadding == padding,
                            onClick = { viewModel.socketNumberPadding.value = padding },
                            label = { 
                                Text(
                                    when(padding) {
                                        1 -> "1 (1)"
                                        2 -> "2 (01)"
                                        3 -> "3 (001)"
                                        else -> "$padding"
                                    }
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = socketSuffix,
                    onValueChange = { viewModel.socketSuffix.value = it },
                    label = { Text("Suffix") },
                    placeholder = { Text("A, B, -1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            socketPreview,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // === SPEED TEST CONFIGURATION (Default Collapsed with Preview) ===
            CollapsibleSection(
                icon = Icons.Default.Speed,
                title = "Speed Test",
                isExpanded = speedTestConfigExpanded,
                onToggle = { speedTestConfigExpanded = !speedTestConfigExpanded },
                preview = if (!speedTestConfigExpanded) speedTestPreview else null
            ) {
                OutlinedTextField(
                    value = speedTestServerAddress,
                    onValueChange = { viewModel.speedTestServerAddress.value = it },
                    label = { Text("Server Address") },
                    placeholder = { Text("192.168.1.1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("IP or hostname of MikroTik server") }
                )

                OutlinedTextField(
                    value = speedTestServerUser,
                    onValueChange = { viewModel.speedTestServerUser.value = it },
                    label = { Text("Username") },
                    placeholder = { Text("admin") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Optional (default: admin)") }
                )

                OutlinedTextField(
                    value = speedTestServerPassword,
                    onValueChange = { viewModel.speedTestServerPassword.value = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Optional") },
                    visualTransformation = if (speedTestPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (speedTestPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                        val description = if (speedTestPasswordVisible) {
                            stringResource(id = R.string.hide_password)
                        } else {
                            stringResource(id = R.string.show_password)
                        }
                        IconButton(onClick = { speedTestPasswordVisible = !speedTestPasswordVisible }) {
                            Icon(icon, contentDescription = description)
                        }
                    }
                )
            }

            Spacer(Modifier.height(64.dp)) // Bottom spacing
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CollapsibleSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    preview: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    }
                },
                supportingContent = preview?.let {
                    {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                trailingContent = {
                    IconButton(onClick = onToggle) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
