/*
 * Purpose: Render the test execution experience for running and completed states.
 * Inputs: TestViewModel state, TestRunSnapshot updates, and execution logs.
 * Outputs: Status hero, progressive test cards, detailed renderers, and completion actions.
 * Notes: Running view reveals cards as sections start; details expand only on final statuses.
 */
package com.app.miklink.ui.test

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.app.miklink.ui.components.StatusHero
import com.app.miklink.ui.components.StatusHeroState
import com.app.miklink.ui.feature.test_details.SectionRendererRegistry
import com.app.miklink.ui.feature.test_details.renderers.LinkSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.NetworkSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.NeighborsSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.PingSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.SpeedSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.TdrSectionRenderer
import com.app.miklink.ui.test.components.RawLogsPane
import com.app.miklink.ui.test.components.TestExecutionTags
import com.app.miklink.ui.theme.MikLinkTheme
import com.app.miklink.ui.theme.MikLinkThemeTokens
import com.app.miklink.utils.UiState

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
    var showLogs by rememberSaveable { mutableStateOf(false) }
    val rendererRegistry = rememberRendererRegistry()

    LaunchedEffect(uiState, isRunning) {
        if (uiState is UiState.Idle && !isRunning && !hasAutoStarted) {
            hasAutoStarted = true
            viewModel.startTest()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = topBarTitle(uiState, isRunning),
                subtitle = topBarSubtitle(uiState, isRunning),
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            val report = (uiState as? UiState.Success<TestReport>)?.data
            if (report != null) {
                CompletedActionBar(
                    isFailed = report.overallStatus != "PASS",
                    onClose = { navController.popBackStack() },
                    onRepeat = { showRepeatDialog = true },
                    onSave = {
                        viewModel.saveReportToDb(report)
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (val state = uiState) {
                is UiState.Success -> CompletedContent(
                    report = state.data,
                    snapshot = snapshot,
                    logs = logs,
                    showLogs = showLogs,
                    onToggleLogs = { showLogs = !showLogs },
                    rendererRegistry = rendererRegistry,
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.Error -> ErrorState(
                    message = state.message,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                else -> RunningContent(
                    snapshot = snapshot,
                    logs = logs,
                    showLogs = showLogs,
                    onToggleLogs = { showLogs = !showLogs },
                    rendererRegistry = rendererRegistry,
                    modifier = Modifier.fillMaxSize()
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
    logs: List<String>,
    showLogs: Boolean,
    onToggleLogs: () -> Unit,
    rendererRegistry: SectionRendererRegistry,
    modifier: Modifier = Modifier
) {
    val sections = TestSectionDisplayPolicy.visibleForRunning(
        TestSectionDisplayPolicy.ordered(snapshot?.sections.orEmpty())
    )
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onToggleLogs,
                    modifier = Modifier.testTag(TestExecutionTags.IN_PROGRESS_TOGGLE)
                ) {
                    Text(
                        text = stringResource(
                            id = if (showLogs) R.string.test_toggle_hide_logs else R.string.test_toggle_show_logs
                        )
                    )
                }
            }
        }
        if (showLogs) {
            item {
                RawLogsPane(
                    logs = logs,
                    emptyLabel = stringResource(id = R.string.test_logs_empty),
                    title = null,
                    autoScroll = true,
                    colorize = true
                )
            }
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    sections.forEach { section ->
                        SectionCard(section, rendererRegistry)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedContent(
    report: TestReport,
    snapshot: TestRunSnapshot?,
    logs: List<String>,
    showLogs: Boolean,
    onToggleLogs: () -> Unit,
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
            bottom = 24.dp
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
                supportingContent = null
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onToggleLogs,
                    modifier = Modifier.testTag(TestExecutionTags.COMPLETED_TOGGLE)
                ) {
                    Text(
                        text = stringResource(
                            id = if (showLogs) R.string.test_toggle_hide_logs else R.string.test_toggle_show_logs
                        )
                    )
                }
            }
        }
        if (showLogs) {
            item {
                RawLogsPane(
                    logs = logs,
                    emptyLabel = stringResource(id = R.string.test_logs_empty),
                    title = null,
                    autoScroll = false,
                    colorize = true
                )
            }
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KpiItemIcon(
                value = passed,
                icon = Icons.Default.CheckCircle,
                contentDescription = stringResource(id = R.string.test_execution_kpi_passed_label),
                color = semantic.success
            )
            KpiItemIcon(
                value = failed,
                icon = Icons.Default.Cancel,
                contentDescription = stringResource(id = R.string.test_execution_kpi_failed_label),
                color = semantic.failure
            )
            KpiItemIcon(
                value = skipped,
                icon = Icons.Default.RemoveCircle,
                contentDescription = stringResource(id = R.string.test_execution_kpi_skipped_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KpiItemIcon(
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = color
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
                        .fillMaxWidth()
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
            }
        }
    }
}

@Composable
private fun RepeatDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val semantic = MikLinkThemeTokens.semantic
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = semantic.failure
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
    val semantic = MikLinkThemeTokens.semantic
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = semantic.failureContainer,
            contentColor = semantic.onFailureContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = semantic.failure
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
    val registry = rememberRendererRegistry()
    MikLinkTheme(darkTheme = false, dynamicColor = false) {
        RunningContent(
            snapshot = TestRunSnapshot(
                sections = listOf(
                    TestSectionSnapshot(TestSectionId.NETWORK, TestSectionStatus.PASS, title = "Network"),
                    TestSectionSnapshot(TestSectionId.NEIGHBORS, TestSectionStatus.PASS, title = "LLDP/CDP"),
                    TestSectionSnapshot(TestSectionId.LINK, TestSectionStatus.RUNNING, title = "Link")
                ),
                percent = 35
            ),
            logs = listOf("Avvio test...", "Link status OK"),
            showLogs = true,
            onToggleLogs = {},
            rendererRegistry = registry
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
            logs = listOf("Link OK", "Ping OK"),
            showLogs = false,
            onToggleLogs = {},
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
            logs = listOf("Link FAIL"),
            showLogs = true,
            onToggleLogs = {},
            rendererRegistry = registry
        )
    }
}
