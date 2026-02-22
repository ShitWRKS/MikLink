/*
 * Purpose: Compose screen for creating/updating clients and previewing socket-id formatting consistently with domain policy.
 * Inputs: ClientEditViewModel state (company details, network mode, socket prefix/separator/padding/suffix, speed test credentials) and navigation controller.
 * Outputs: Persisted client via view model actions and live previews for socket-id and speed test target.
 * Notes: Socket preview delegates to SocketIdLite.format to align with ADR-0004 separators; UI remains stateless beyond local expansion toggles.
 */
package com.app.miklink.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    if (isSaved) {
        LaunchedEffect(Unit) { navController.popBackStack() }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
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
    val isSaveEnabled = companyName.isNotBlank()

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

    val notConfiguredText = stringResource(R.string.client_edit_not_configured)
    val speedTestPreview = remember(speedTestServerAddress, notConfiguredText) {
        speedTestServerAddress.takeIf { it.isNotBlank() } ?: notConfiguredText
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.isEditing) {
                            stringResource(R.string.client_edit_title_edit)
                        } else {
                            stringResource(R.string.client_edit_title_new)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Quick save action
                    IconButton(
                        onClick = viewModel::saveClient,
                        enabled = isSaveEnabled
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = stringResource(R.string.save)
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
                enabled = isSaveEnabled
            ) {
                Text(stringResource(R.string.client_edit_save))
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
            
            LabeledTextField(
                value = companyName,
                onValueChange = { viewModel.companyName.value = it },
                labelResId = R.string.client_edit_company_label,
                modifier = Modifier.fillMaxWidth(),
                isError = companyName.isBlank()
            )
            
            LabeledTextField(
                value = location,
                onValueChange = { viewModel.location.value = it },
                labelResId = R.string.client_edit_location_label,
                placeholderResId = R.string.client_edit_location_placeholder,
                modifier = Modifier.fillMaxWidth()
            )
            
            LabeledTextField(
                value = notes,
                onValueChange = { viewModel.notes.value = it },
                labelResId = R.string.report_detail_edit_notes,
                placeholderResId = R.string.client_edit_notes_placeholder,
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )

            HorizontalDivider()

            // === NETWORK CONFIGURATION (Default Expanded) ===
            CollapsibleSection(
                icon = Icons.Default.Router,
                title = stringResource(R.string.client_edit_section_network),
                isExpanded = networkConfigExpanded,
                onToggle = { networkConfigExpanded = !networkConfigExpanded },
                preview = if (!networkConfigExpanded) {
                    if (networkMode == NetworkMode.DHCP) {
                        stringResource(R.string.detail_value_dhcp)
                    } else {
                        val cidrPreview = staticCidr.ifBlank { stringResource(R.string.client_edit_not_configured) }
                        stringResource(R.string.client_edit_network_preview_static, cidrPreview)
                    }
                } else null
            ) {
                // Network Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NetworkModeButton(
                        selected = networkMode == NetworkMode.DHCP,
                        onClick = { viewModel.networkMode.value = NetworkMode.DHCP },
                        icon = Icons.Default.Wifi,
                        labelResId = R.string.detail_value_dhcp,
                        modifier = Modifier.weight(1f)
                    )

                    NetworkModeButton(
                        selected = networkMode == NetworkMode.STATIC,
                        onClick = { viewModel.networkMode.value = NetworkMode.STATIC },
                        icon = Icons.Default.Settings,
                        labelResId = R.string.detail_value_static,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (networkMode == NetworkMode.STATIC) {
                    LabeledTextField(
                        value = staticCidr,
                        onValueChange = { viewModel.staticCidr.value = it },
                        labelResId = R.string.client_edit_static_ip_label,
                        placeholderResId = R.string.client_edit_static_ip_placeholder,
                        supportingResId = R.string.client_edit_static_ip_support,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    LabeledTextField(
                        value = staticGateway,
                        onValueChange = { viewModel.staticGateway.value = it },
                        labelResId = R.string.detail_label_gateway,
                        placeholderResId = R.string.client_edit_gateway_placeholder,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(8.dp))
                SectionLabel(R.string.client_edit_min_link_rate)
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
                title = stringResource(R.string.client_edit_section_socket),
                isExpanded = socketConfigExpanded,
                onToggle = { socketConfigExpanded = !socketConfigExpanded },
                preview = if (!socketConfigExpanded) socketPreview else null
            ) {
                LabeledTextField(
                    value = socketPrefix,
                    onValueChange = { viewModel.socketPrefix.value = it },
                    labelResId = R.string.client_edit_prefix_label,
                    placeholderResId = R.string.client_edit_prefix_placeholder,
                    modifier = Modifier.fillMaxWidth()
                )

                LabeledTextField(
                    value = socketSeparator,
                    onValueChange = { viewModel.socketSeparator.value = it },
                    labelResId = R.string.client_edit_separator_label,
                    placeholderResId = R.string.client_edit_separator_placeholder,
                    supportingResId = R.string.client_edit_separator_support,
                    modifier = Modifier.fillMaxWidth()
                )

                SectionLabel(R.string.client_edit_padding_label)
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

                LabeledTextField(
                    value = socketSuffix,
                    onValueChange = { viewModel.socketSuffix.value = it },
                    labelResId = R.string.client_edit_suffix_label,
                    placeholderResId = R.string.client_edit_suffix_placeholder,
                    modifier = Modifier.fillMaxWidth()
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
                title = stringResource(R.string.client_edit_section_speed),
                isExpanded = speedTestConfigExpanded,
                onToggle = { speedTestConfigExpanded = !speedTestConfigExpanded },
                preview = if (!speedTestConfigExpanded) speedTestPreview else null
            ) {
                LabeledTextField(
                    value = speedTestServerAddress,
                    onValueChange = { viewModel.speedTestServerAddress.value = it },
                    labelResId = R.string.client_edit_server_address_label,
                    placeholderResId = R.string.client_edit_server_address_placeholder,
                    supportingResId = R.string.client_edit_server_address_support,
                    modifier = Modifier.fillMaxWidth()
                )

                LabeledTextField(
                    value = speedTestServerUser,
                    onValueChange = { viewModel.speedTestServerUser.value = it },
                    labelResId = R.string.client_edit_username_label,
                    placeholderResId = R.string.client_edit_username_placeholder,
                    supportingResId = R.string.client_edit_username_support,
                    modifier = Modifier.fillMaxWidth()
                )

                LabeledTextField(
                    value = speedTestServerPassword,
                    onValueChange = { viewModel.speedTestServerPassword.value = it },
                    labelResId = R.string.client_edit_password_label,
                    supportingResId = R.string.client_edit_password_support,
                    modifier = Modifier.fillMaxWidth(),
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
private fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    labelResId: Int,
    placeholderResId: Int? = null,
    supportingResId: Int? = null,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelResId)) },
        placeholder = placeholderResId?.let { { Text(stringResource(it)) } },
        supportingText = supportingResId?.let { { Text(stringResource(it)) } },
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon
    )
}

@Composable
private fun SectionLabel(resId: Int) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun NetworkModeButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    labelResId: Int,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(labelResId))
    }
}

@Composable
private fun SectionHeader(
    icon: ImageVector,
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
    icon: ImageVector,
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
                        val description = if (isExpanded) {
                            stringResource(R.string.collapse)
                        } else {
                            stringResource(R.string.expand)
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = description
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
