/*
 * UI test execution screen, input view model state/snapshots, output expressive running/completed rendering.
 */
package com.app.miklink.ui.test

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.R
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.test.model.TestRunSnapshot
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionSnapshot
import com.app.miklink.core.domain.test.model.TestSectionStatus
import com.app.miklink.ui.common.TestSectionCard
import com.app.miklink.ui.components.AppTopBar
import com.app.miklink.ui.components.LogsBottomSheet
import com.app.miklink.ui.components.StatusHero
import com.app.miklink.ui.components.StatusHeroState
import com.app.miklink.ui.components.StepStatus
import com.app.miklink.ui.components.StepTimeline
import com.app.miklink.ui.components.StepTimelineItem
import com.app.miklink.ui.feature.test_details.SectionRendererRegistry
import com.app.miklink.ui.feature.test_details.renderers.LinkSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.NetworkSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.NeighborsSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.PingSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.SpeedSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.TdrSectionRenderer
import com.app.miklink.ui.test.components.TestExecutionTags
import com.app.miklink.ui.theme.MikLinkTheme
import com.app.miklink.ui.theme.MikLinkThemeTokens
import com.app.miklink.utils.UiState

private val SheetPeekHeight = 92.dp
private val CompletedActionsHeight = 176.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestExecutionScreen(
    navController: NavController,
    viewModel: TestViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    var showRepeatDialog by remember { mutableStateOf(false) }
    var hasAutoStarted by remember { mutableStateOf(false) }
    val rendererRegistry = rememberRendererRegistry()

    LaunchedEffect(uiState, isRunning) {
        if (uiState is UiState.Idle && !isRunning && !hasAutoStarted) {
            hasAutoStarted = true
            viewModel.startTest()
        }
    }

    LogsBottomSheet(
        logs = logs,
        peekHeight = SheetPeekHeight,
        topBar = {
            AppTopBar(
                title = topBarTitle(uiState, isRunning),
                subtitle = topBarSubtitle(uiState, isRunning),
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is UiState.Success -> CompletedContent(
                    report = state.data,
                    snapshot = snapshot,
                    rendererRegistry = rendererRegistry,
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.Error -> ErrorState(
                    message = state.message,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                else -> RunningContent(
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (uiState is UiState.Success) {
                val report = (uiState as UiState.Success<TestReport>).data
                CompletedActionBar(
                    isFailed = report.overallStatus != "PASS",
                    onClose = { navController.popBackStack() },
                    onRepeat = { showRepeatDialog = true },
                    onSave = {
                        viewModel.saveReportToDb(report)
                        navController.popBackStack()
                    },
                    onExport = {
                        viewModel.saveReportToDb(report)
                        navController.navigate("history")
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = SheetPeekHeight)
                        .fillMaxWidth()
                )
            }
        }
    }

    if (showRepeatDialog) {
        RepeatDialog(
            onDismiss = { showRepeatDialog = false },
            onConfirm = {
                showRepeatDialog = false
                viewModel.startTest()
            }
        )
    }
}

@Composable
private fun RunningContent(
    snapshot: TestRunSnapshot?,
    modifier: Modifier = Modifier
) {
    val sections = TestSectionDisplayPolicy.visibleForRunning(
        TestSectionDisplayPolicy.ordered(snapshot?.sections.orEmpty())
    )
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = SheetPeekHeight + 24.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StatusHero(
                status = StatusHeroState.Running,
                title = stringResource(id = R.string.test_execution_running_hero_title),
                subtitle = stringResource(id = R.string.test_execution_hero_running_subtitle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestExecutionTags.HERO_RUNNING)
            )
        }
        item {
            if (sections.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = stringResource(id = R.string.test_execution_no_details),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                StepTimeline(
                    steps = sections.map { section -> section.toTimelineItem() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CompletedContent(
    report: TestReport,
    snapshot: TestRunSnapshot?,
    rendererRegistry: SectionRendererRegistry,
    modifier: Modifier = Modifier
) {
    val sections = TestSectionDisplayPolicy.ordered(snapshot?.sections.orEmpty())
    val isFailed = report.overallStatus != "PASS"
    val failureReason = if (isFailed) resolveFailureReason(snapshot) else null

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            top = 16.dp,
            bottom = SheetPeekHeight + CompletedActionsHeight
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StatusHero(
                status = if (isFailed) StatusHeroState.Failure else StatusHeroState.Success,
                title = if (isFailed) {
                    stringResource(id = R.string.test_execution_completed_hero_fail)
                } else {
                    stringResource(id = R.string.test_execution_completed_hero_pass)
                },
                subtitle = if (isFailed) {
                    stringResource(id = R.string.test_execution_hero_fail_subtitle)
                } else {
                    stringResource(id = R.string.test_execution_hero_pass_subtitle)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestExecutionTags.HERO_COMPLETED),
                supportingContent = {
                    if (!report.socketName.isNullOrBlank()) {
                        Text(
                            text = stringResource(id = R.string.test_execution_stat_socket, report.socketName ?: "-"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
        if (isFailed) {
            item {
                FailureReasonCard(reason = failureReason)
            }
        }
        item {
            KpiRow(sections = sections)
        }
        item {
            TestSectionList(
                sections = sections,
                rendererRegistry = rendererRegistry
            )
        }
    }
}

@Composable
private fun FailureReasonCard(reason: String?) {
    val semantic = MikLinkThemeTokens.semantic
    val resolved = reason ?: stringResource(id = R.string.test_execution_fail_reason_fallback)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = semantic.failureContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.test_execution_fail_reason_title),
                style = MaterialTheme.typography.labelLarge,
                color = semantic.onFailureContainer
            )
            Text(
                text = resolved,
                style = MaterialTheme.typography.bodyMedium,
                color = semantic.onFailureContainer
            )
        }
    }
}

@Composable
private fun KpiRow(sections: List<TestSectionSnapshot>) {
    val semantic = MikLinkThemeTokens.semantic
    val passed = sections.count { it.status == TestSectionStatus.PASS }
    val failed = sections.count { it.status == TestSectionStatus.FAIL }
    val skipped = sections.count { it.status == TestSectionStatus.SKIP }
    val total = sections.size

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KpiItem(
                value = passed,
                label = stringResource(id = R.string.test_execution_kpi_passed_label),
                color = semantic.success
            )
            KpiItem(
                value = failed,
                label = stringResource(id = R.string.test_execution_kpi_failed_label),
                color = semantic.failure
            )
            KpiItem(
                value = skipped,
                label = stringResource(id = R.string.test_execution_kpi_skipped_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            KpiItem(
                value = total,
                label = stringResource(id = R.string.test_execution_kpi_total_label),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun KpiItem(
    value: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TestSectionList(
    sections: List<TestSectionSnapshot>,
    rendererRegistry: SectionRendererRegistry
) {
    if (sections.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = stringResource(id = R.string.test_execution_no_details),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }
    val info = sections.filter { it.id == TestSectionId.NETWORK || it.id == TestSectionId.NEIGHBORS }
    val tests = sections.filter { it.id !in listOf(TestSectionId.NETWORK, TestSectionId.NEIGHBORS) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (info.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.test_execution_group_info),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            info.forEach { section ->
                SectionCard(section, rendererRegistry)
            }
            HorizontalDivider()
        }
        if (tests.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.test_execution_group_tests),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            tests.forEach { section ->
                SectionCard(section, rendererRegistry)
            }
        }
    }
}

@Composable
private fun SectionCard(
    section: TestSectionSnapshot,
    rendererRegistry: SectionRendererRegistry
) {
    val expandable = TestSectionDisplayPolicy.isExpandable(section.status)
    val sectionTag = "${TestExecutionTags.SECTION_CARD_PREFIX}_${section.id.name.lowercase()}"
    TestSectionCard(
        title = sectionTitle(section),
        status = statusLabel(section.status),
        icon = sectionIcon(section.id),
        statusColor = statusTint(section.status),
        expandable = expandable,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(sectionTag)
    ) {
        rendererRegistry.rendererFor(section.id).Render(section, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CompletedActionBar(
    isFailed: Boolean,
    onClose: () -> Unit,
    onRepeat: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = MikLinkThemeTokens.semantic
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestExecutionTags.BOTTOM_CLOSE)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(id = R.string.test_execution_action_close))
                }
                OutlinedButton(
                    onClick = onRepeat,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestExecutionTags.BOTTOM_REPEAT)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(id = R.string.test_execution_action_repeat_short))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestExecutionTags.BOTTOM_SAVE),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFailed) semantic.failure else semantic.success,
                        contentColor = if (isFailed) semantic.onFailure else semantic.onSuccess
                    )
                ) {
                    Icon(
                        if (isFailed) Icons.Default.Error else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(id = R.string.test_execution_action_save))
                }
                FilledTonalButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(id = R.string.test_execution_action_export))
                }
            }
        }
    }
}

@Composable
private fun RepeatDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(text = stringResource(id = R.string.test_execution_repeat_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(id = R.string.test_execution_repeat_body))
                Text(stringResource(id = R.string.test_execution_repeat_warning_one), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(id = R.string.test_execution_repeat_warning_two), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(id = R.string.test_execution_action_repeat_long))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(id = R.string.test_execution_title_error),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TestSectionSnapshot.toTimelineItem(): StepTimelineItem {
    return StepTimelineItem(
        title = sectionTitle(this),
        icon = sectionIcon(id),
        status = status.toStepStatus(),
        subtitle = warning,
        statusLabel = statusLabel(status)
    )
}

@Composable
private fun sectionTitle(section: TestSectionSnapshot): String {
    return section.title ?: when (section.id) {
        TestSectionId.NETWORK -> stringResource(id = R.string.section_network)
        TestSectionId.LINK -> stringResource(id = R.string.section_link)
        TestSectionId.TDR -> stringResource(id = R.string.section_tdr)
        TestSectionId.NEIGHBORS -> stringResource(id = R.string.section_lldp)
        TestSectionId.PING -> stringResource(id = R.string.section_ping)
        TestSectionId.SPEED -> stringResource(id = R.string.section_speed)
        TestSectionId.SAVING_REPORT -> stringResource(id = R.string.test_execution_section_saving)
        TestSectionId.REPORT -> stringResource(id = R.string.test_execution_section_report)
        else -> stringResource(id = R.string.test_execution_section_unknown)
    }
}

@Composable
private fun sectionIcon(id: TestSectionId): androidx.compose.ui.graphics.vector.ImageVector =
    when (id) {
        TestSectionId.NETWORK -> Icons.Default.SettingsEthernet
        TestSectionId.LINK -> Icons.Default.Link
        TestSectionId.TDR -> Icons.Default.Cable
        TestSectionId.NEIGHBORS -> Icons.Default.Devices
        TestSectionId.PING -> Icons.Default.Wifi
        TestSectionId.SPEED -> Icons.Default.Speed
        TestSectionId.SAVING_REPORT -> Icons.Default.Save
        TestSectionId.REPORT -> Icons.Default.Description
        else -> Icons.Default.Info
    }

@Composable
private fun statusLabel(status: TestSectionStatus): String =
    when (status) {
        TestSectionStatus.PASS -> stringResource(id = R.string.status_pass)
        TestSectionStatus.FAIL -> stringResource(id = R.string.status_fail)
        TestSectionStatus.SKIP -> stringResource(id = R.string.status_skip)
        TestSectionStatus.RUNNING -> stringResource(id = R.string.test_execution_status_chip_running)
        TestSectionStatus.INFO -> stringResource(id = R.string.status_info)
        TestSectionStatus.PENDING -> stringResource(id = R.string.status_pending)
    }

private fun TestSectionStatus.toStepStatus(): StepStatus =
    when (this) {
        TestSectionStatus.PASS -> StepStatus.Success
        TestSectionStatus.FAIL -> StepStatus.Failure
        TestSectionStatus.RUNNING -> StepStatus.Running
        TestSectionStatus.SKIP -> StepStatus.Skipped
        TestSectionStatus.INFO -> StepStatus.Info
        TestSectionStatus.PENDING -> StepStatus.Pending
    }

@Composable
private fun statusTint(status: TestSectionStatus): Color =
    run {
        val semantic = MikLinkThemeTokens.semantic
        when (status) {
            TestSectionStatus.PASS -> semantic.success
            TestSectionStatus.FAIL -> semantic.failure
            TestSectionStatus.RUNNING -> semantic.running
            TestSectionStatus.SKIP -> MaterialTheme.colorScheme.outline
            TestSectionStatus.INFO -> MaterialTheme.colorScheme.secondary
            TestSectionStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

@Composable
private fun topBarTitle(uiState: UiState<TestReport>, isRunning: Boolean): String {
    return when {
        isRunning -> stringResource(id = R.string.test_execution_running_topbar)
        uiState is UiState.Success && uiState.data.overallStatus == "PASS" -> stringResource(
            id = R.string.test_execution_completed_title_pass
        )
        uiState is UiState.Success -> stringResource(id = R.string.test_execution_completed_title_fail)
        uiState is UiState.Error -> stringResource(id = R.string.test_execution_title_error)
        uiState is UiState.Idle -> stringResource(id = R.string.test_execution_title_ready)
        else -> stringResource(id = R.string.test_execution_title_running)
    }
}

@Composable
private fun topBarSubtitle(uiState: UiState<TestReport>, isRunning: Boolean): String {
    return when {
        isRunning -> stringResource(id = R.string.test_execution_subtitle_running)
        uiState is UiState.Success && uiState.data.overallStatus == "PASS" -> stringResource(
            id = R.string.test_execution_subtitle_success
        )
        uiState is UiState.Success -> stringResource(id = R.string.test_execution_subtitle_failure)
        uiState is UiState.Error -> stringResource(id = R.string.test_execution_subtitle_error)
        uiState is UiState.Idle -> stringResource(id = R.string.test_execution_subtitle_ready)
        else -> ""
    }
}

private fun resolveFailureReason(snapshot: TestRunSnapshot?): String? {
    val firstWarning = snapshot?.sections?.firstOrNull { it.status == TestSectionStatus.FAIL }?.warning
    return firstWarning ?: snapshot?.notes
}

@Composable
private fun rememberRendererRegistry(): SectionRendererRegistry {
    return remember {
        SectionRendererRegistry(
            renderers = mapOf(
                TestSectionId.NETWORK to NetworkSectionRenderer(),
                TestSectionId.LINK to LinkSectionRenderer(),
                TestSectionId.TDR to TdrSectionRenderer(),
                TestSectionId.NEIGHBORS to NeighborsSectionRenderer(),
                TestSectionId.PING to PingSectionRenderer(),
                TestSectionId.SPEED to SpeedSectionRenderer()
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewExecutionRunning() {
    MikLinkTheme(darkTheme = false, dynamicColor = false) {
        RunningContent(
            snapshot = TestRunSnapshot(
                sections = listOf(
                    TestSectionSnapshot(TestSectionId.NETWORK, TestSectionStatus.PASS, title = "Network"),
                    TestSectionSnapshot(TestSectionId.NEIGHBORS, TestSectionStatus.PASS, title = "LLDP/CDP"),
                    TestSectionSnapshot(TestSectionId.LINK, TestSectionStatus.RUNNING, title = "Link")
                ),
                percent = 35
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewExecutionCompleted() {
    val registry = rememberRendererRegistry()
    MikLinkTheme(darkTheme = false, dynamicColor = false) {
        CompletedContent(
            report = TestReport(
                reportId = 1,
                clientId = 1,
                timestamp = System.currentTimeMillis(),
                socketName = "Presa 12",
                notes = "",
                probeName = "Probe",
                profileName = "Default",
                overallStatus = "PASS",
                resultsJson = ""
            ),
            snapshot = TestRunSnapshot(
                sections = listOf(
                    TestSectionSnapshot(TestSectionId.NETWORK, TestSectionStatus.PASS, title = "Network"),
                    TestSectionSnapshot(TestSectionId.LINK, TestSectionStatus.PASS, title = "Link"),
                    TestSectionSnapshot(
                        id = TestSectionId.PING,
                        status = TestSectionStatus.PASS,
                        title = "Ping",
                        payload = com.app.miklink.core.domain.test.model.TestSectionPayload.Ping(
                            samples = listOf(
                                com.app.miklink.core.domain.model.report.PingSample(
                                    target = "DHCP_GATEWAY",
                                    host = "192.168.1.1",
                                    avgRtt = "10ms",
                                    minRtt = "8ms",
                                    maxRtt = "12ms",
                                    packetLoss = "0%",
                                    sent = "5",
                                    seq = "0",
                                    time = "10ms",
                                    ttl = "64"
                                )
                            )
                        )
                    )
                ),
                percent = 100
            ),
            rendererRegistry = registry
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewExecutionCompletedDark() {
    val registry = rememberRendererRegistry()
    MikLinkTheme(darkTheme = true, dynamicColor = false) {
        CompletedContent(
            report = TestReport(
                reportId = 1,
                clientId = 1,
                timestamp = System.currentTimeMillis(),
                socketName = "Presa 12",
                notes = "",
                probeName = "Probe",
                profileName = "Default",
                overallStatus = "FAIL",
                resultsJson = ""
            ),
            snapshot = TestRunSnapshot(
                sections = listOf(
                    TestSectionSnapshot(TestSectionId.NETWORK, TestSectionStatus.PASS, title = "Network"),
                    TestSectionSnapshot(TestSectionId.LINK, TestSectionStatus.FAIL, title = "Link", warning = "Cable down")
                ),
                percent = 100
            ),
            rendererRegistry = registry
        )
    }
}
