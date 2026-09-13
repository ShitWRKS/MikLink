package com.app.miklink.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.ui.testing.AgentUiTags

@Composable
internal fun TestProfilePingTab(
    runPing: Boolean,
    onRunPingChange: (Boolean) -> Unit,
    pingTarget1: String,
    onPingTarget1Change: (String) -> Unit,
    pingTarget1IsError: Boolean,
    pingTarget2: String,
    onPingTarget2Change: (String) -> Unit,
    pingTarget2IsError: Boolean,
    pingTarget3: String,
    onPingTarget3Change: (String) -> Unit,
    pingTarget3IsError: Boolean,
    showTarget2: Boolean,
    onShowTarget2Change: (Boolean) -> Unit,
    showTarget3: Boolean,
    onShowTarget3Change: (Boolean) -> Unit,
    availableSlots: Int,
    onQuickFill: (String) -> Unit,
    pingCount: String,
    onPingCountChange: (String) -> Unit,
    pingCountIsError: Boolean,
    showGatewayPolicy: Boolean,
    gatewayPolicyFail: Boolean,
    onGatewayPolicyFailChange: (Boolean) -> Unit,
    pingLocalMaxLoss: String,
    onPingLocalMaxLossChange: (String) -> Unit,
    pingLocalMaxLossIsError: Boolean,
    pingLocalMaxAvgRtt: String,
    onPingLocalMaxAvgRttChange: (String) -> Unit,
    pingLocalMaxAvgRttIsError: Boolean,
    pingLocalMaxRtt: String,
    onPingLocalMaxRttChange: (String) -> Unit,
    pingLocalMaxRttIsError: Boolean,
    pingExternalMaxLoss: String,
    onPingExternalMaxLossChange: (String) -> Unit,
    pingExternalMaxLossIsError: Boolean,
    pingExternalMaxAvgRtt: String,
    onPingExternalMaxAvgRttChange: (String) -> Unit,
    pingExternalMaxAvgRttIsError: Boolean,
    pingExternalMaxRtt: String,
    onPingExternalMaxRttChange: (String) -> Unit,
    pingExternalMaxRttIsError: Boolean,
    effectiveLocalMaxAvgRtt: Double?,
    effectiveLocalMaxRtt: Double?,
    effectiveExternalMaxAvgRtt: Double?,
    effectiveExternalMaxRtt: Double?,
    failureColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileSection(
                title = stringResource(R.string.profile_edit_ping_header),
                supportingText = stringResource(R.string.profile_edit_ping_header_description)
            ) {
                ProfileSwitchItem(
                    checked = runPing,
                    onCheckedChange = onRunPingChange,
                    headlineText = stringResource(R.string.profile_edit_run_ping_title),
                    modifier = Modifier.testTag(AgentUiTags.Profile.RUN_PING)
                )
                PingTargetsEditor(
                    pingTarget1 = pingTarget1,
                    onPingTarget1Change = onPingTarget1Change,
                    pingTarget1IsError = pingTarget1IsError,
                    pingTarget2 = pingTarget2,
                    onPingTarget2Change = onPingTarget2Change,
                    pingTarget2IsError = pingTarget2IsError,
                    pingTarget3 = pingTarget3,
                    onPingTarget3Change = onPingTarget3Change,
                    pingTarget3IsError = pingTarget3IsError,
                    showTarget2 = showTarget2,
                    onShowTarget2Change = onShowTarget2Change,
                    showTarget3 = showTarget3,
                    onShowTarget3Change = onShowTarget3Change,
                    availableSlots = availableSlots,
                    onQuickFill = onQuickFill,
                    failureColor = failureColor
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = pingCount,
                    onValueChange = onPingCountChange,
                    label = { Text(stringResource(R.string.profile_edit_ping_count_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AgentUiTags.Profile.PING_COUNT),
                    singleLine = true,
                    supportingText = {
                        if (pingCountIsError) {
                            Text(
                                text = stringResource(R.string.profile_edit_ping_count_invalid),
                                color = failureColor
                            )
                        } else {
                            Text(stringResource(R.string.profile_edit_ping_count_support))
                        }
                    },
                    isError = pingCountIsError
                )
                if (showGatewayPolicy) {
                    GatewayPolicyItem(
                        checked = gatewayPolicyFail,
                        onCheckedChange = onGatewayPolicyFailChange
                    )
                }
            }
        }

        item {
            PingThresholdSection(
                title = stringResource(R.string.profile_edit_ping_local_section_title),
                maxLoss = pingLocalMaxLoss,
                onMaxLossChange = onPingLocalMaxLossChange,
                maxLossIsError = pingLocalMaxLossIsError,
                maxAverageRtt = pingLocalMaxAvgRtt,
                onMaxAverageRttChange = onPingLocalMaxAvgRttChange,
                maxAverageRttIsError = pingLocalMaxAvgRttIsError,
                maxRtt = pingLocalMaxRtt,
                onMaxRttChange = onPingLocalMaxRttChange,
                maxRttIsError = pingLocalMaxRttIsError,
                effectiveMaxAverageRtt = effectiveLocalMaxAvgRtt,
                effectiveMaxRtt = effectiveLocalMaxRtt,
                maxAverageRttTag = AgentUiTags.Profile.PING_LOCAL_MAX_AVG_RTT,
                maxAverageRttSliderTag = AgentUiTags.Profile.PING_LOCAL_MAX_AVG_RTT_SLIDER,
                maxRttSliderTag = AgentUiTags.Profile.PING_LOCAL_MAX_RTT_SLIDER
            )
        }

        item {
            PingThresholdSection(
                title = stringResource(R.string.profile_edit_ping_external_section_title),
                maxLoss = pingExternalMaxLoss,
                onMaxLossChange = onPingExternalMaxLossChange,
                maxLossIsError = pingExternalMaxLossIsError,
                maxAverageRtt = pingExternalMaxAvgRtt,
                onMaxAverageRttChange = onPingExternalMaxAvgRttChange,
                maxAverageRttIsError = pingExternalMaxAvgRttIsError,
                maxRtt = pingExternalMaxRtt,
                onMaxRttChange = onPingExternalMaxRttChange,
                maxRttIsError = pingExternalMaxRttIsError,
                effectiveMaxAverageRtt = effectiveExternalMaxAvgRtt,
                effectiveMaxRtt = effectiveExternalMaxRtt,
                maxAverageRttSliderTag = AgentUiTags.Profile.PING_EXTERNAL_MAX_AVG_RTT_SLIDER,
                maxRttSliderTag = AgentUiTags.Profile.PING_EXTERNAL_MAX_RTT_SLIDER
            )
        }
    }
}

@Composable
private fun PingTargetsEditor(
    pingTarget1: String,
    onPingTarget1Change: (String) -> Unit,
    pingTarget1IsError: Boolean,
    pingTarget2: String,
    onPingTarget2Change: (String) -> Unit,
    pingTarget2IsError: Boolean,
    pingTarget3: String,
    onPingTarget3Change: (String) -> Unit,
    pingTarget3IsError: Boolean,
    showTarget2: Boolean,
    onShowTarget2Change: (Boolean) -> Unit,
    showTarget3: Boolean,
    onShowTarget3Change: (Boolean) -> Unit,
    availableSlots: Int,
    onQuickFill: (String) -> Unit,
    failureColor: Color
) {
    Text(stringResource(R.string.profile_edit_quick_fill_title), style = MaterialTheme.typography.labelLarge)
    val quickButtonPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickFillButton(
            icon = Icons.Default.Router,
            label = stringResource(R.string.profile_edit_quick_fill_gateway),
            enabled = availableSlots > 0,
            contentPadding = quickButtonPadding,
            modifier = Modifier.weight(1f),
            onClick = { onQuickFill(DHCP_GATEWAY_TOKEN) }
        )
        QuickFillButton(
            icon = Icons.Default.Cloud,
            label = stringResource(R.string.profile_edit_quick_fill_google),
            enabled = availableSlots > 0,
            contentPadding = quickButtonPadding,
            modifier = Modifier.weight(1f),
            onClick = { onQuickFill("8.8.8.8") }
        )
        QuickFillButton(
            icon = Icons.Default.Storage,
            label = stringResource(R.string.profile_edit_quick_fill_cloudflare),
            enabled = availableSlots > 0,
            contentPadding = quickButtonPadding,
            modifier = Modifier.weight(1f),
            onClick = { onQuickFill("1.1.1.1") }
        )
    }
    HorizontalDivider()
    Text(stringResource(R.string.profile_edit_custom_targets_title), style = MaterialTheme.typography.labelLarge)
    PingTargetField(
        index = 1,
        value = pingTarget1,
        onValueChange = onPingTarget1Change,
        isError = pingTarget1IsError,
        invalidText = stringResource(R.string.profile_edit_invalid_target_full),
        failureColor = failureColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AgentUiTags.Profile.PING_TARGET_1)
    )
    if (showTarget2) {
        OptionalPingTargetRow(
            index = 2,
            value = pingTarget2,
            onValueChange = onPingTarget2Change,
            isError = pingTarget2IsError,
            invalidText = stringResource(R.string.profile_edit_invalid_target_short),
            failureColor = failureColor,
            onRemove = {
                onPingTarget2Change("")
                onShowTarget2Change(false)
            }
        )
    }
    if (showTarget3) {
        OptionalPingTargetRow(
            index = 3,
            value = pingTarget3,
            onValueChange = onPingTarget3Change,
            isError = pingTarget3IsError,
            invalidText = stringResource(R.string.profile_edit_invalid_target_short),
            failureColor = failureColor,
            onRemove = {
                onPingTarget3Change("")
                onShowTarget3Change(false)
            }
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!showTarget2) {
            AddTargetButton(index = 2, modifier = Modifier.weight(1f)) {
                onShowTarget2Change(true)
            }
        }
        if (!showTarget3 && showTarget2) {
            AddTargetButton(index = 3, modifier = Modifier.weight(1f)) {
                onShowTarget3Change(true)
            }
        }
    }
}

@Composable
private fun PingThresholdSection(
    title: String,
    maxLoss: String,
    onMaxLossChange: (String) -> Unit,
    maxLossIsError: Boolean,
    maxAverageRtt: String,
    onMaxAverageRttChange: (String) -> Unit,
    maxAverageRttIsError: Boolean,
    maxRtt: String,
    onMaxRttChange: (String) -> Unit,
    maxRttIsError: Boolean,
    effectiveMaxAverageRtt: Double?,
    effectiveMaxRtt: Double?,
    maxAverageRttTag: String? = null,
    maxAverageRttSliderTag: String,
    maxRttSliderTag: String
) {
    ProfileSection(
        title = title,
        supportingText = stringResource(R.string.profile_edit_ping_thresholds_support)
    ) {
        ThresholdRow(
            leftLabel = stringResource(R.string.profile_edit_threshold_loss),
            leftUnit = "%",
            leftValue = maxLoss,
            onLeftChange = onMaxLossChange,
            leftIsError = maxLossIsError,
            rightLabel = stringResource(R.string.profile_edit_threshold_avg_rtt),
            rightUnit = "ms",
            rightValue = maxAverageRtt,
            onRightChange = onMaxAverageRttChange,
            rightIsError = maxAverageRttIsError,
            rightTag = maxAverageRttTag
        )
        ThresholdRow(
            leftLabel = stringResource(R.string.profile_edit_threshold_max_rtt),
            leftUnit = "ms",
            leftValue = maxRtt,
            onLeftChange = onMaxRttChange,
            leftIsError = maxRttIsError
        )
        PingThresholdPreview(
            maxAverageRttMs = effectiveMaxAverageRtt,
            maxRttMs = effectiveMaxRtt,
            onMaxAverageRttChange = onMaxAverageRttChange,
            onMaxRttChange = onMaxRttChange,
            maxAverageRttSliderTag = maxAverageRttSliderTag,
            maxRttSliderTag = maxRttSliderTag
        )
    }
}

@Composable
private fun PingTargetField(
    index: Int,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    invalidText: String,
    failureColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.profile_edit_target_label, index)) },
        placeholder = { Text(stringResource(R.string.profile_edit_target_placeholder)) },
        modifier = modifier,
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            { Text(invalidText, color = failureColor) }
        } else {
            null
        }
    )
}

@Composable
private fun OptionalPingTargetRow(
    index: Int,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
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
            isError = isError,
            invalidText = invalidText,
            failureColor = failureColor,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove, modifier = Modifier.padding(top = 8.dp)) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(R.string.profile_edit_remove_target, index)
            )
        }
    }
}

@Composable
private fun AddTargetButton(index: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.profile_edit_add_target, index))
    }
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
private fun GatewayPolicyItem(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.profile_edit_gateway_dhcp)) },
        supportingContent = { Text(stringResource(R.string.profile_edit_gateway_policy_label)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth()
    )
}

internal const val DHCP_GATEWAY_TOKEN = "DHCP_GATEWAY"
