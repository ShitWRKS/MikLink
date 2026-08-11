package com.app.miklink.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.app.miklink.R
import com.app.miklink.core.domain.validation.StrictLinkRateParser
import com.app.miklink.ui.testing.AgentUiTags

@Composable
internal fun TestProfileLinkTab(
    runLinkStatus: Boolean,
    onRunLinkStatusChange: (Boolean) -> Unit,
    runTdr: Boolean,
    onRunTdrChange: (Boolean) -> Unit,
    runLldp: Boolean,
    onRunLldpChange: (Boolean) -> Unit,
    linkMinRate: String,
    effectiveLinkMinRate: String?,
    onLinkMinRateChange: (String) -> Unit,
    linkMinRateIsError: Boolean
) {
    val presets = LINK_RATE_PRESETS
    val effectiveLabel = effectiveLinkMinRate.orEmpty()
    val effectiveRateMbps = effectiveLinkMinRate?.let(StrictLinkRateParser::parseMbps)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileSection(title = stringResource(R.string.profile_edit_link_status_section_title)) {
                ProfileSwitchItem(
                    checked = runLinkStatus,
                    onCheckedChange = onRunLinkStatusChange,
                    headlineText = stringResource(R.string.profile_edit_run_link_title),
                    modifier = Modifier.testTag(AgentUiTags.Profile.RUN_LINK)
                )
                LinkRatePicker(
                    label = stringResource(R.string.profile_edit_link_min_rate_label),
                    value = linkMinRate,
                    options = presets,
                    onValueChange = onLinkMinRateChange,
                    isError = linkMinRateIsError
                )
                LinkThresholdPreview(
                    configuredValue = effectiveLabel,
                    effectiveRateMbps = effectiveRateMbps,
                    presetLabels = presets,
                    presetRatesMbps = presets.mapNotNull(StrictLinkRateParser::parseMbps),
                    onConfiguredValueChange = onLinkMinRateChange
                )
            }
        }
        item {
            ProfileSection(title = stringResource(R.string.profile_edit_tdr_section_title)) {
                ProfileSwitchItem(
                    checked = runTdr,
                    onCheckedChange = onRunTdrChange,
                    headlineText = stringResource(R.string.profile_edit_run_tdr_title),
                    supportingText = stringResource(R.string.profile_edit_run_tdr_support),
                    modifier = Modifier.testTag(AgentUiTags.Profile.RUN_TDR)
                )
            }
        }
        item {
            ProfileSection(title = stringResource(R.string.profile_edit_lldp_section_title)) {
                ProfileSwitchItem(
                    checked = runLldp,
                    onCheckedChange = onRunLldpChange,
                    headlineText = stringResource(R.string.profile_edit_run_lldp_title),
                    modifier = Modifier.testTag(AgentUiTags.Profile.RUN_NEIGHBORS)
                )
            }
        }
    }
}

@Composable
private fun LinkRatePicker(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    isError: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }
    var customValue by rememberSaveable(value) { mutableStateOf(value) }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AgentUiTags.Profile.LINK_MIN_RATE),
            singleLine = true,
            isError = isError,
            supportingText = if (isError) {
                { Text(stringResource(R.string.profile_edit_threshold_invalid_link_rate)) }
            } else {
                null
            },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_edit_custom)) },
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
                        onValueChange(customValue)
                        showCustomDialog = false
                    },
                    enabled = StrictLinkRateParser.isValidOptional(customValue)
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(label) },
            text = {
                val customIsError = !StrictLinkRateParser.isValidOptional(customValue)
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it },
                    singleLine = true,
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = customIsError,
                    supportingText = if (customIsError) {
                        { Text(stringResource(R.string.profile_edit_threshold_invalid_link_rate)) }
                    } else {
                        null
                    }
                )
            }
        )
    }
}

private val LINK_RATE_PRESETS =
    listOf("10M", "100M", "1G", "2.5G", "5G", "10G", "25G", "40G", "50G", "100G")
