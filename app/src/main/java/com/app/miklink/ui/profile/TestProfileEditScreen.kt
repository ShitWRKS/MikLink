package com.app.miklink.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.R
import com.app.miklink.ui.testing.AgentUiTags
import com.app.miklink.ui.theme.MikLinkThemeTokens

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

    var selectedTab by rememberSaveable { mutableIntStateOf(GENERAL_TAB) }
    var showTarget2 by rememberSaveable { mutableStateOf(pingTarget2.isNotBlank()) }
    var showTarget3 by rememberSaveable { mutableStateOf(pingTarget3.isNotBlank()) }

    LaunchedEffect(pingTarget2, pingTarget3) {
        if (pingTarget3.isNotBlank()) showTarget3 = true
        if (pingTarget2.isNotBlank() || pingTarget3.isNotBlank()) showTarget2 = true
    }

    val showGatewayPolicy = remember(pingTarget1, pingTarget2, pingTarget3) {
        listOf(pingTarget1, pingTarget2, pingTarget3).any {
            it.equals(DHCP_GATEWAY_TOKEN, ignoreCase = true)
        }
    }
    val tabLabels = listOf(
        stringResource(R.string.profile_edit_tab_general),
        stringResource(R.string.profile_edit_tab_link),
        stringResource(R.string.profile_edit_tab_ping),
        stringResource(R.string.profile_edit_tab_speed)
    )
    val tabTags = listOf(
        AgentUiTags.Profile.TAB_GENERAL,
        AgentUiTags.Profile.TAB_LINK,
        AgentUiTags.Profile.TAB_PING,
        AgentUiTags.Profile.TAB_SPEED
    )
    val titleRes = if (viewModel.isEditing) R.string.title_edit_profile else R.string.title_add_profile
    val hasEnabledTest = viewModel.hasAtLeastOneTestEnabled()

    Scaffold(
        modifier = Modifier.testTag(AgentUiTags.Profile.EDIT),
        topBar = {
            Column {
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
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    tabLabels.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            modifier = Modifier.testTag(tabTags[index]),
                            text = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (!hasEnabledTest) {
                        Text(
                            text = stringResource(R.string.profile_edit_error_no_test_enabled),
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.failure,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = viewModel::saveProfile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AgentUiTags.Profile.SAVE),
                        enabled = viewModel.isValidForSave()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                GENERAL_TAB -> TestProfileGeneralTab(
                    profileName = profileName,
                    onProfileNameChange = { viewModel.profileName.value = it },
                    profileDescription = profileDescription,
                    onProfileDescriptionChange = { viewModel.profileDescription.value = it }
                )

                LINK_TAB -> TestProfileLinkTab(
                    runLinkStatus = runLinkStatus,
                    onRunLinkStatusChange = { viewModel.runLinkStatus.value = it },
                    runTdr = runTdr,
                    onRunTdrChange = { viewModel.runTdr.value = it },
                    runLldp = runLldp,
                    onRunLldpChange = { viewModel.runLldp.value = it },
                    linkMinRate = linkMinRate,
                    effectiveLinkMinRate = viewModel.effectiveLinkMinRateForPreview(),
                    onLinkMinRateChange = { viewModel.linkMinRate.value = it },
                    linkMinRateIsError = viewModel.isLinkMinRateInvalid()
                )

                PING_TAB -> TestProfilePingTab(
                    runPing = runPing,
                    onRunPingChange = { viewModel.runPing.value = it },
                    pingTarget1 = pingTarget1,
                    onPingTarget1Change = { viewModel.pingTarget1.value = it },
                    pingTarget1IsError = viewModel.isPingTargetInvalid(pingTarget1),
                    pingTarget2 = pingTarget2,
                    onPingTarget2Change = { viewModel.pingTarget2.value = it },
                    pingTarget2IsError = viewModel.isPingTargetInvalid(pingTarget2),
                    pingTarget3 = pingTarget3,
                    onPingTarget3Change = { viewModel.pingTarget3.value = it },
                    pingTarget3IsError = viewModel.isPingTargetInvalid(pingTarget3),
                    showTarget2 = showTarget2,
                    onShowTarget2Change = { showTarget2 = it },
                    showTarget3 = showTarget3,
                    onShowTarget3Change = { showTarget3 = it },
                    availableSlots = availableSlots,
                    onQuickFill = viewModel::fillLastAvailableTarget,
                    pingCount = pingCount,
                    onPingCountChange = { viewModel.pingCount.value = it },
                    pingCountIsError = viewModel.isPingCountInvalid(),
                    showGatewayPolicy = showGatewayPolicy,
                    gatewayPolicyFail = gatewayPolicyFail,
                    onGatewayPolicyFailChange = { viewModel.gatewayPolicyFail.value = it },
                    pingLocalMaxLoss = pingLocalMaxLoss,
                    onPingLocalMaxLossChange = { viewModel.pingLocalMaxLoss.value = it },
                    pingLocalMaxLossIsError = viewModel.isPercentageThresholdInvalid(pingLocalMaxLoss),
                    pingLocalMaxAvgRtt = pingLocalMaxAvgRtt,
                    onPingLocalMaxAvgRttChange = { viewModel.pingLocalMaxAvgRtt.value = it },
                    pingLocalMaxAvgRttIsError = viewModel.isNonNegativeThresholdInvalid(pingLocalMaxAvgRtt),
                    pingLocalMaxRtt = pingLocalMaxRtt,
                    onPingLocalMaxRttChange = { viewModel.pingLocalMaxRtt.value = it },
                    pingLocalMaxRttIsError = viewModel.isNonNegativeThresholdInvalid(pingLocalMaxRtt),
                    pingExternalMaxLoss = pingExternalMaxLoss,
                    onPingExternalMaxLossChange = { viewModel.pingExternalMaxLoss.value = it },
                    pingExternalMaxLossIsError = viewModel.isPercentageThresholdInvalid(pingExternalMaxLoss),
                    pingExternalMaxAvgRtt = pingExternalMaxAvgRtt,
                    onPingExternalMaxAvgRttChange = { viewModel.pingExternalMaxAvgRtt.value = it },
                    pingExternalMaxAvgRttIsError = viewModel.isNonNegativeThresholdInvalid(pingExternalMaxAvgRtt),
                    pingExternalMaxRtt = pingExternalMaxRtt,
                    onPingExternalMaxRttChange = { viewModel.pingExternalMaxRtt.value = it },
                    pingExternalMaxRttIsError = viewModel.isNonNegativeThresholdInvalid(pingExternalMaxRtt),
                    effectiveLocalMaxAvgRtt = viewModel.effectivePingLocalMaxAvgRttForPreview(),
                    effectiveLocalMaxRtt = viewModel.effectivePingLocalMaxRttForPreview(),
                    effectiveExternalMaxAvgRtt = viewModel.effectivePingExternalMaxAvgRttForPreview(),
                    effectiveExternalMaxRtt = viewModel.effectivePingExternalMaxRttForPreview(),
                    failureColor = semantic.failure
                )

                SPEED_TAB -> TestProfileSpeedTab(
                    runSpeedTest = runSpeedTest,
                    onRunSpeedTestChange = { viewModel.runSpeedTest.value = it },
                    speedMaxPing = speedMaxPing,
                    onSpeedMaxPingChange = { viewModel.speedMaxPing.value = it },
                    speedMaxPingIsError = viewModel.isNonNegativeThresholdInvalid(speedMaxPing),
                    speedMaxJitter = speedMaxJitter,
                    onSpeedMaxJitterChange = { viewModel.speedMaxJitter.value = it },
                    speedMaxJitterIsError = viewModel.isNonNegativeThresholdInvalid(speedMaxJitter),
                    speedMaxLoss = speedMaxLoss,
                    onSpeedMaxLossChange = { viewModel.speedMaxLoss.value = it },
                    speedMaxLossIsError = viewModel.isPercentageThresholdInvalid(speedMaxLoss),
                    speedMinDownload = speedMinDownload,
                    onSpeedMinDownloadChange = { viewModel.speedMinDownload.value = it },
                    speedMinDownloadIsError = viewModel.isSpeedThroughputInvalid(speedMinDownload),
                    speedMinUpload = speedMinUpload,
                    onSpeedMinUploadChange = { viewModel.speedMinUpload.value = it },
                    speedMinUploadIsError = viewModel.isSpeedThroughputInvalid(speedMinUpload),
                    effectiveMaxPing = viewModel.effectiveSpeedMaxPingForPreview(),
                    effectiveMaxJitter = viewModel.effectiveSpeedMaxJitterForPreview(),
                    effectiveMaxLoss = viewModel.effectiveSpeedMaxLossForPreview(),
                    effectiveMinDownload = viewModel.effectiveSpeedMinDownloadForPreview(),
                    effectiveMinUpload = viewModel.effectiveSpeedMinUploadForPreview()
                )
            }
        }
    }
}

private const val GENERAL_TAB = 0
private const val LINK_TAB = 1
private const val PING_TAB = 2
private const val SPEED_TAB = 3
