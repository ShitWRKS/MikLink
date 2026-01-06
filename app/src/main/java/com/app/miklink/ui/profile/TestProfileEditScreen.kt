package com.app.miklink.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateContentSize
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.R
import com.app.miklink.ui.theme.MikLinkThemeTokens
import com.app.miklink.utils.NetworkValidator

private const val DHCP_GATEWAY_TOKEN = "DHCP_GATEWAY"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestProfileEditScreen(
    navController: NavController,
    viewModel: TestProfileViewModel = hiltViewModel()
) {
    val semantic = MikLinkThemeTokens.semantic

    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    LaunchedEffect(isSaved) {
        if (isSaved) navController.popBackStack()
    }

    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val profileDescription by viewModel.profileDescription.collectAsStateWithLifecycle()

    val runTdr by viewModel.runTdr.collectAsStateWithLifecycle()
    val runLinkStatus by viewModel.runLinkStatus.collectAsStateWithLifecycle()
    val runLldp by viewModel.runLldp.collectAsStateWithLifecycle()
    val runPing by viewModel.runPing.collectAsStateWithLifecycle()
    val runSpeedTest by viewModel.runSpeedTest.collectAsStateWithLifecycle()

    val pingTarget1 by viewModel.pingTarget1.collectAsStateWithLifecycle()
    val pingTarget2 by viewModel.pingTarget2.collectAsStateWithLifecycle()
    val pingTarget3 by viewModel.pingTarget3.collectAsStateWithLifecycle()
    val pingCount by viewModel.pingCount.collectAsStateWithLifecycle()
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

    var pingConfigExpanded by rememberSaveable { mutableStateOf(false) }
    var thresholdsExpanded by rememberSaveable { mutableStateOf(true) } // default: come prima (aperto)

    var showTarget2 by rememberSaveable { mutableStateOf(pingTarget2.isNotBlank()) }
    var showTarget3 by rememberSaveable { mutableStateOf(pingTarget3.isNotBlank()) }

    LaunchedEffect(pingTarget2, pingTarget3) {
        if (pingTarget3.isNotBlank()) showTarget3 = true
        if (pingTarget2.isNotBlank() || pingTarget3.isNotBlank()) showTarget2 = true
    }

    val showGatewayPolicy = remember(pingTarget1, pingTarget2, pingTarget3) {
        listOf(pingTarget1, pingTarget2, pingTarget3).any { it.equals(DHCP_GATEWAY_TOKEN, ignoreCase = true) }
    }

    val linkRateOptions = remember {
        listOf("10M", "100M", "1G", "2.5G", "5G", "10G", "25G", "40G", "50G", "100G")
    }

    val titleRes = if (viewModel.isEditing) R.string.title_edit_profile else R.string.title_add_profile

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::saveProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                enabled = profileName.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
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
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { viewModel.profileName.value = it },
                    label = { Text(stringResource(R.string.profile_edit_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = profileDescription,
                    onValueChange = { viewModel.profileDescription.value = it },
                    label = { Text(stringResource(R.string.profile_edit_description_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { HorizontalDivider() }

            item {
                SwitchListItem(
                    checked = runTdr,
                    onCheckedChange = { viewModel.runTdr.value = it },
                    headlineText = stringResource(R.string.profile_edit_run_tdr_title),
                    supportingText = stringResource(R.string.profile_edit_run_tdr_support)
                )
                SwitchListItem(
                    checked = runLinkStatus,
                    onCheckedChange = { viewModel.runLinkStatus.value = it },
                    headlineText = stringResource(R.string.profile_edit_run_link_title)
                )
                SwitchListItem(
                    checked = runLldp,
                    onCheckedChange = { viewModel.runLldp.value = it },
                    headlineText = stringResource(R.string.profile_edit_run_lldp_title)
                )
                SwitchListItem(
                    checked = runPing,
                    onCheckedChange = { viewModel.runPing.value = it },
                    headlineText = stringResource(R.string.profile_edit_run_ping_title)
                )
                SwitchListItem(
                    checked = runSpeedTest,
                    onCheckedChange = { viewModel.runSpeedTest.value = it },
                    headlineText = stringResource(R.string.profile_edit_run_speed_title),
                    supportingText = stringResource(R.string.profile_edit_run_speed_support)
                )
            }

            if (runPing) {
                item {
                    CollapsibleCard(
                        title = stringResource(R.string.profile_edit_ping_header),
                        subtitle = stringResource(R.string.profile_edit_ping_header_description),
                        expanded = pingConfigExpanded,
                        onExpandedChange = { pingConfigExpanded = it }
                    ) {
                        Text(
                            stringResource(R.string.profile_edit_quick_fill),
                            style = MaterialTheme.typography.labelLarge
                        )

                        val allSlotsFilled = availableSlots == 0
                        val quickBtnPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickFillButton(
                                icon = Icons.Default.Router,
                                label = stringResource(R.string.profile_edit_quick_fill_gateway),
                                enabled = !allSlotsFilled,
                                contentPadding = quickBtnPadding,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.fillLastAvailableTarget(DHCP_GATEWAY_TOKEN) }
                            )
                            QuickFillButton(
                                icon = Icons.Default.Cloud,
                                label = stringResource(R.string.profile_edit_quick_fill_google),
                                enabled = !allSlotsFilled,
                                contentPadding = quickBtnPadding,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.fillLastAvailableTarget("8.8.8.8") }
                            )
                            QuickFillButton(
                                icon = Icons.Default.Storage,
                                label = stringResource(R.string.profile_edit_quick_fill_cloudflare),
                                enabled = !allSlotsFilled,
                                contentPadding = quickBtnPadding,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.fillLastAvailableTarget("1.1.1.1") }
                            )
                        }

                        HorizontalDivider()

                        Text(
                            stringResource(R.string.profile_edit_custom_targets),
                            style = MaterialTheme.typography.labelLarge
                        )

                        PingTargetField(
                            index = 1,
                            value = pingTarget1,
                            onValueChange = { viewModel.pingTarget1.value = it },
                            invalidText = stringResource(R.string.profile_edit_invalid_target_full),
                            failureColor = semantic.failure,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (showTarget2) {
                            OptionalPingTargetRow(
                                index = 2,
                                value = pingTarget2,
                                onValueChange = { viewModel.pingTarget2.value = it },
                                invalidText = stringResource(R.string.profile_edit_invalid_target_short),
                                failureColor = semantic.failure,
                                onRemove = {
                                    viewModel.pingTarget2.value = ""
                                    showTarget2 = false
                                }
                            )
                        }

                        if (showTarget3) {
                            OptionalPingTargetRow(
                                index = 3,
                                value = pingTarget3,
                                onValueChange = { viewModel.pingTarget3.value = it },
                                invalidText = stringResource(R.string.profile_edit_invalid_target_short),
                                failureColor = semantic.failure,
                                onRemove = {
                                    viewModel.pingTarget3.value = ""
                                    showTarget3 = false
                                }
                            )
                        }

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

                        OutlinedTextField(
                            value = pingCount,
                            onValueChange = { viewModel.pingCount.value = it },
                            label = { Text(stringResource(R.string.profile_edit_ping_count_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text(stringResource(R.string.profile_edit_ping_count_support)) },
                            isError = pingCount.toIntOrNull()?.let { it < 1 || it > 20 } ?: pingCount.isNotBlank()
                        )
                    }
                }
            }

            item {
                CollapsibleCard(
                    title = stringResource(R.string.profile_edit_thresholds_title),
                    subtitle = stringResource(R.string.profile_edit_thresholds_description),
                    expanded = thresholdsExpanded,
                    onExpandedChange = { thresholdsExpanded = it }
                ) {
                    // Link
                    Text(stringResource(R.string.profile_edit_link_section_title), style = MaterialTheme.typography.labelLarge)
                    LinkRatePicker(
                        label = stringResource(R.string.profile_edit_link_min_rate_label),
                        value = linkMinRate,
                        options = linkRateOptions,
                        onSelect = { viewModel.linkMinRate.value = it },
                        onCustomValue = { viewModel.linkMinRate.value = it }
                    )

                    HorizontalDivider()

                    // Ping locale
                    Text(stringResource(R.string.profile_edit_ping_local_section_title), style = MaterialTheme.typography.labelLarge)
                    ThresholdRow(
                        leftLabel = "Loss",
                        leftUnit = "%",
                        leftValue = pingLocalMaxLoss,
                        onLeftChange = { viewModel.pingLocalMaxLoss.value = it },
                        rightLabel = "Avg RTT",
                        rightUnit = "ms",
                        rightValue = pingLocalMaxAvgRtt,
                        onRightChange = { viewModel.pingLocalMaxAvgRtt.value = it }
                    )
                    ThresholdRow(
                        leftLabel = "Max RTT",
                        leftUnit = "ms",
                        leftValue = pingLocalMaxRtt,
                        onLeftChange = { viewModel.pingLocalMaxRtt.value = it },
                        rightLabel = "",
                        rightUnit = "",
                        rightValue = "",
                        onRightChange = {},
                        rightEnabled = false
                    )

                    HorizontalDivider()

                    // Ping esterno
                    Text(stringResource(R.string.profile_edit_ping_external_section_title), style = MaterialTheme.typography.labelLarge)
                    ThresholdRow(
                        leftLabel = "Loss",
                        leftUnit = "%",
                        leftValue = pingExternalMaxLoss,
                        onLeftChange = { viewModel.pingExternalMaxLoss.value = it },
                        rightLabel = "Avg RTT",
                        rightUnit = "ms",
                        rightValue = pingExternalMaxAvgRtt,
                        onRightChange = { viewModel.pingExternalMaxAvgRtt.value = it }
                    )
                    ThresholdRow(
                        leftLabel = "Max RTT",
                        leftUnit = "ms",
                        leftValue = pingExternalMaxRtt,
                        onLeftChange = { viewModel.pingExternalMaxRtt.value = it },
                        rightLabel = "",
                        rightUnit = "",
                        rightValue = "",
                        onRightChange = {},
                        rightEnabled = false
                    )

                    if (showGatewayPolicy) {
                        GatewayPolicyItem(
                            checked = gatewayPolicyFail,
                            onCheckedChange = { viewModel.gatewayPolicyFail.value = it }
                        )
                    }

                    HorizontalDivider()

                    // Speed test
                    Text(stringResource(R.string.profile_edit_speed_section_title), style = MaterialTheme.typography.labelLarge)
                    ThresholdRow(
                        leftLabel = "Ping",
                        leftUnit = "ms",
                        leftValue = speedMaxPing,
                        onLeftChange = { viewModel.speedMaxPing.value = it },
                        rightLabel = "Jitter",
                        rightUnit = "ms",
                        rightValue = speedMaxJitter,
                        onRightChange = { viewModel.speedMaxJitter.value = it }
                    )
                    ThresholdRow(
                        leftLabel = "Loss",
                        leftUnit = "%",
                        leftValue = speedMaxLoss,
                        onLeftChange = { viewModel.speedMaxLoss.value = it },
                        rightLabel = "Down",
                        rightUnit = "Mbps",
                        rightValue = speedMinDownload,
                        onRightChange = { viewModel.speedMinDownload.value = it }
                    )
                    ThresholdRow(
                        leftLabel = "Up",
                        leftUnit = "Mbps",
                        leftValue = speedMinUpload,
                        onLeftChange = { viewModel.speedMinUpload.value = it },
                        rightLabel = "",
                        rightUnit = "",
                        rightValue = "",
                        onRightChange = {},
                        rightEnabled = false
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
            trailingContent = {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (expanded) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SwitchListItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    headlineText: String,
    supportingText: String? = null
) {
    ListItem(
        headlineContent = { Text(headlineText) },
        supportingContent = supportingText?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun QuickFillButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PingTargetField(
    index: Int,
    value: String,
    onValueChange: (String) -> Unit,
    invalidText: String,
    failureColor: Color,
    modifier: Modifier = Modifier
) {
    val isInvalid = value.isNotBlank() && !NetworkValidator.isValidTarget(value)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.profile_edit_target_label, index)) },
        placeholder = { Text(stringResource(R.string.profile_edit_target_placeholder)) },
        modifier = modifier,
        singleLine = true,
        isError = isInvalid,
        supportingText = {
            if (isInvalid) Text(invalidText, color = failureColor)
        }
    )
}

@Composable
private fun OptionalPingTargetRow(
    index: Int,
    value: String,
    onValueChange: (String) -> Unit,
    invalidText: String,
    failureColor: Color,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        PingTargetField(
            index = index,
            value = value,
            onValueChange = onValueChange,
            invalidText = invalidText,
            failureColor = failureColor,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.profile_edit_remove_target, index))
        }
    }
}

@Composable
private fun ThresholdRow(
    leftLabel: String,
    leftUnit: String,
    leftValue: String,
    onLeftChange: (String) -> Unit,
    rightLabel: String,
    rightUnit: String,
    rightValue: String,
    onRightChange: (String) -> Unit,
    rightEnabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UnitField(
            label = leftLabel,
            unit = leftUnit,
            value = leftValue,
            onValueChange = onLeftChange,
            modifier = Modifier.weight(1f),
            enabled = true
        )

        if (rightLabel.isNotBlank()) {
            UnitField(
                label = rightLabel,
                unit = rightUnit,
                value = rightValue,
                onValueChange = onRightChange,
                modifier = Modifier.weight(1f),
                enabled = rightEnabled
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun UnitField(
    label: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        enabled = enabled,
        suffix = {
            if (unit.isNotBlank()) Text(unit)
        }
    )
}

@Composable
private fun GatewayPolicyItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text("Gateway DHCP") },
        supportingContent = { Text(stringResource(R.string.profile_edit_gateway_policy_label)) }, // "Fail se non risolto"
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LinkRatePicker(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onCustomValue: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }
    var customValue by rememberSaveable(value) { mutableStateOf(value) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("Custom…") },
                onClick = {
                    expanded = false
                    customValue = value
                    showCustomDialog = true
                }
            )
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCustomValue(customValue)
                        showCustomDialog = false
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text(stringResource(android.R.string.cancel)) }
            },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it },
                    singleLine = true,
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
