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
import androidx.compose.ui.res.stringResource
import com.app.miklink.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.utils.NetworkValidator
import com.app.miklink.ui.theme.MikLinkThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestProfileEditScreen(
    navController: NavController,
    viewModel: TestProfileViewModel = hiltViewModel()
) {
    val semantic = MikLinkThemeTokens.semantic
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
    val linkMinRate by viewModel.linkMinRate.collectAsStateWithLifecycle()
    val pingLocalMaxLoss by viewModel.pingLocalMaxLoss.collectAsStateWithLifecycle()
    val pingLocalMaxAvgRtt by viewModel.pingLocalMaxAvgRtt.collectAsStateWithLifecycle()
    val pingLocalMaxRtt by viewModel.pingLocalMaxRtt.collectAsStateWithLifecycle()
    val pingExternalMaxLoss by viewModel.pingExternalMaxLoss.collectAsStateWithLifecycle()
    val pingExternalMaxAvgRtt by viewModel.pingExternalMaxAvgRtt.collectAsStateWithLifecycle()
    val pingExternalMaxRtt by viewModel.pingExternalMaxRtt.collectAsStateWithLifecycle()
    val gatewayPolicyFail by viewModel.gatewayPolicyFail.collectAsStateWithLifecycle()
    val speedMaxPing by viewModel.speedMaxPing.collectAsStateWithLifecycle()
    val speedMaxJitter by viewModel.speedMaxJitter.collectAsStateWithLifecycle()
    val speedMaxLoss by viewModel.speedMaxLoss.collectAsStateWithLifecycle()
    val speedMinDownload by viewModel.speedMinDownload.collectAsStateWithLifecycle()
    val speedMinUpload by viewModel.speedMinUpload.collectAsStateWithLifecycle()

    var pingConfigExpanded by remember { mutableStateOf(false) }
    var showTarget2 by remember { mutableStateOf(pingTarget2.isNotBlank()) }
    var showTarget3 by remember { mutableStateOf(pingTarget3.isNotBlank()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) stringResource(id = com.app.miklink.R.string.title_edit_profile) else stringResource(id = com.app.miklink.R.string.title_add_profile)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = com.app.miklink.R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::saveProfile,
                modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                enabled = profileName.isNotBlank()
                ) {
                Text(stringResource(id = com.app.miklink.R.string.save))
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
                OutlinedTextField(value = profileName, onValueChange = { viewModel.profileName.value = it }, label = { Text(stringResource(R.string.profile_edit_name_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = profileDescription, onValueChange = { viewModel.profileDescription.value = it }, label = { Text(stringResource(R.string.profile_edit_description_label)) }, modifier = Modifier.fillMaxWidth())
            }

            item { HorizontalDivider() }

            item {
                SwitchListItem(checked = runTdr, onCheckedChange = { viewModel.runTdr.value = it }, headlineText = stringResource(R.string.profile_edit_run_tdr_title), supportingText = stringResource(R.string.profile_edit_run_tdr_support))
                SwitchListItem(checked = runLinkStatus, onCheckedChange = { viewModel.runLinkStatus.value = it }, headlineText = stringResource(R.string.profile_edit_run_link_title))
                SwitchListItem(checked = runLldp, onCheckedChange = { viewModel.runLldp.value = it }, headlineText = stringResource(R.string.profile_edit_run_lldp_title))
                SwitchListItem(checked = runPing, onCheckedChange = { viewModel.runPing.value = it }, headlineText = stringResource(R.string.profile_edit_run_ping_title))
                SwitchListItem(checked = runSpeedTest, onCheckedChange = { viewModel.runSpeedTest.value = it }, headlineText = stringResource(R.string.profile_edit_run_speed_title), supportingText = stringResource(R.string.profile_edit_run_speed_support))
            }

            if (runPing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.profile_edit_ping_header), style = MaterialTheme.typography.titleMedium) },
                                supportingContent = { Text(stringResource(R.string.profile_edit_ping_header_description)) },
                                trailingContent = {
                                    IconButton(onClick = { pingConfigExpanded = !pingConfigExpanded }) {
                                        Icon(
                                            if (pingConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (pingConfigExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand)
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
                                    Text(stringResource(R.string.profile_edit_quick_fill), style = MaterialTheme.typography.labelLarge)
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
                                                Text(stringResource(R.string.profile_edit_quick_fill_gateway), style = MaterialTheme.typography.labelSmall)
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
                                                Text(stringResource(R.string.profile_edit_quick_fill_google), style = MaterialTheme.typography.labelSmall)
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
                                                Text(stringResource(R.string.profile_edit_quick_fill_cloudflare), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    HorizontalDivider()

                                    // Custom Targets
                                    Text(stringResource(R.string.profile_edit_custom_targets), style = MaterialTheme.typography.labelLarge)
                                    
                                    // Target 1 (always visible)
                                    OutlinedTextField(
                                        value = pingTarget1,
                                        onValueChange = { viewModel.pingTarget1.value = it },
                                        label = { Text(stringResource(R.string.profile_edit_target_label, 1)) },
                                        placeholder = { Text(stringResource(R.string.profile_edit_target_placeholder)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        isError = pingTarget1.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget1),
                                        supportingText = {
                                            if (pingTarget1.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget1)) {
                                                Text(stringResource(R.string.profile_edit_invalid_target_full), color = semantic.failure)
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
                                                label = { Text(stringResource(R.string.profile_edit_target_label, 2)) },
                                                placeholder = { Text(stringResource(R.string.profile_edit_target_placeholder)) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                isError = pingTarget2.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget2),
                                                supportingText = {
                                                    if (pingTarget2.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget2)) {
                                                        Text(stringResource(R.string.profile_edit_invalid_target_short), color = semantic.failure)
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
                                                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.profile_edit_remove_target, 2))
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
                                                label = { Text(stringResource(R.string.profile_edit_target_label, 3)) },
                                                placeholder = { Text(stringResource(R.string.profile_edit_target_placeholder)) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                isError = pingTarget3.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget3),
                                                supportingText = {
                                                    if (pingTarget3.isNotBlank() && !NetworkValidator.isValidTarget(pingTarget3)) {
                                                        Text(stringResource(R.string.profile_edit_invalid_target_short), color = semantic.failure)
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
                                                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.profile_edit_remove_target, 3))
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
                                                Text(stringResource(R.string.profile_edit_add_target, 2))
                                            }
                                        }
                                        if (!showTarget3 && showTarget2) {
                                            OutlinedButton(
                                                onClick = { showTarget3 = true },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(stringResource(R.string.profile_edit_add_target, 3))
                                            }
                                        }
                                    }

                                    HorizontalDivider()

                                    // Ping Count
                                    OutlinedTextField(
                                        value = pingCount,
                                        onValueChange = { viewModel.pingCount.value = it },
                                        label = { Text(stringResource(R.string.profile_edit_ping_count_label)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        supportingText = { Text(stringResource(R.string.profile_edit_ping_count_support)) },
                                        isError = pingCount.toIntOrNull()?.let { it < 1 || it > 20 } ?: (pingCount.isNotBlank())
                                    )
                                    
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.profile_edit_thresholds_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.profile_edit_thresholds_description), style = MaterialTheme.typography.bodySmall)

                        Text(stringResource(R.string.profile_edit_link_section_title), style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = linkMinRate,
                            onValueChange = { viewModel.linkMinRate.value = it },
                            label = { Text(stringResource(R.string.profile_edit_link_min_rate_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        HorizontalDivider()

                        Text(stringResource(R.string.profile_edit_ping_local_section_title), style = MaterialTheme.typography.labelLarge)
                        ThresholdRow(
                            leftLabel = "Loss %",
                            leftValue = pingLocalMaxLoss,
                            onLeftChange = { viewModel.pingLocalMaxLoss.value = it },
                            rightLabel = "Avg RTT ms",
                            rightValue = pingLocalMaxAvgRtt,
                            onRightChange = { viewModel.pingLocalMaxAvgRtt.value = it }
                        )
                        ThresholdRow(
                            leftLabel = "Max RTT ms",
                            leftValue = pingLocalMaxRtt,
                            onLeftChange = { viewModel.pingLocalMaxRtt.value = it },
                            rightLabel = "-",
                            rightValue = "",
                            onRightChange = {},
                            rightEnabled = false
                        )

                        Text(stringResource(R.string.profile_edit_ping_external_section_title), style = MaterialTheme.typography.labelLarge)
                        ThresholdRow(
                            leftLabel = "Loss %",
                            leftValue = pingExternalMaxLoss,
                            onLeftChange = { viewModel.pingExternalMaxLoss.value = it },
                            rightLabel = "Avg RTT ms",
                            rightValue = pingExternalMaxAvgRtt,
                            onRightChange = { viewModel.pingExternalMaxAvgRtt.value = it }
                        )
                        ThresholdRow(
                            leftLabel = "Max RTT ms",
                            leftValue = pingExternalMaxRtt,
                            onLeftChange = { viewModel.pingExternalMaxRtt.value = it },
                            rightLabel = "Gateway DHCP",
                            rightValue = "",
                            onRightChange = {},
                            rightEnabled = false,
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.profile_edit_gateway_policy_label), style = MaterialTheme.typography.bodySmall)
                                    Switch(
                                        checked = gatewayPolicyFail,
                                        onCheckedChange = { viewModel.gatewayPolicyFail.value = it }
                                    )
                                }
                            }
                        )

                        HorizontalDivider()

                        Text(stringResource(R.string.profile_edit_speed_section_title), style = MaterialTheme.typography.labelLarge)
                        ThresholdRow(
                            leftLabel = "Ping ms",
                            leftValue = speedMaxPing,
                            onLeftChange = { viewModel.speedMaxPing.value = it },
                            rightLabel = "Jitter ms",
                            rightValue = speedMaxJitter,
                            onRightChange = { viewModel.speedMaxJitter.value = it }
                        )
                        ThresholdRow(
                            leftLabel = "Loss %",
                            leftValue = speedMaxLoss,
                            onLeftChange = { viewModel.speedMaxLoss.value = it },
                            rightLabel = "Down Mbps",
                            rightValue = speedMinDownload,
                            onRightChange = { viewModel.speedMinDownload.value = it }
                        )
                        ThresholdRow(
                            leftLabel = "Up Mbps",
                            leftValue = speedMinUpload,
                            onLeftChange = { viewModel.speedMinUpload.value = it },
                            rightLabel = "",
                            rightValue = "",
                            onRightChange = {},
                            rightEnabled = false
                        )
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

@Composable
private fun ThresholdRow(
    leftLabel: String,
    leftValue: String,
    onLeftChange: (String) -> Unit,
    rightLabel: String,
    rightValue: String,
    onRightChange: (String) -> Unit,
    rightEnabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = leftValue,
            onValueChange = onLeftChange,
            label = { Text(leftLabel) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        if (rightLabel.isNotBlank()) {
            OutlinedTextField(
                value = rightValue,
                onValueChange = onRightChange,
                label = { Text(rightLabel) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = rightEnabled
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        trailingContent?.let { content ->
            Spacer(modifier = Modifier.width(4.dp))
            content()
        }
    }
}
