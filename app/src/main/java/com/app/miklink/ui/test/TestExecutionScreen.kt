/*
 * Purpose: Compose screen for live/completed test execution with legacy UI restored atop the typed snapshot pipeline.
 * Inputs: TestViewModel state flows (uiState, snapshot, logs, isRunning) and NavController for navigation.
 * Outputs: Presentation for running/completed hero, logs toggle, section cards via renderers, and repeat/save actions.
 * Notes: Keeps renderer registry + snapshot intact; all policies are pure UI (ordering, visibility, expandability).
 */
package com.app.miklink.ui.test

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.app.miklink.ui.feature.test_details.SectionRendererRegistry
import com.app.miklink.ui.feature.test_details.renderers.LinkSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.NetworkSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.NeighborsSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.PingSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.SpeedSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.TdrSectionRenderer
import com.app.miklink.ui.test.components.RawLogsPane
import com.app.miklink.ui.test.components.TestExecutionTags
import com.app.miklink.ui.theme.MikLinkThemeTokens
import com.app.miklink.ui.theme.softGlow
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
    var showLogs by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var hasAutoStarted by remember { mutableStateOf(false) }
    val rendererRegistry = rememberRendererRegistry()

    LaunchedEffect(uiState, isRunning) {
        if (uiState is UiState.Idle && !isRunning && !hasAutoStarted) {
            hasAutoStarted = true
            viewModel.startTest()
        }
    }

    Scaffold(
        topBar = {
            LegacyTopBar(
                uiState = uiState,
                isRunning = isRunning,
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            if (uiState is UiState.Success) {
                val report = (uiState as UiState.Success<TestReport>).data
                LegacyBottomActionBar(
                    isFailed = report.overallStatus != "PASS",
                    onClose = { navController.popBackStack() },
                    onRepeat = { showRepeatDialog = true },
                    onSave = {
                        viewModel.saveReportToDb(report)
                        navController.popBackStack()
                    }
                )
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is UiState.Success -> LegacyCompletedContent(
                report = state.data,
                snapshot = snapshot,
                logs = logs,
                showLogs = showLogs,
                onToggleLogs = { showLogs = !showLogs },
                rendererRegistry = rendererRegistry,
                modifier = Modifier.padding(padding)
            )
            is UiState.Error -> ErrorState(
                message = state.message,
                modifier = Modifier.padding(padding)
            )
            else -> LegacyRunningContent(
                snapshot = snapshot,
                logs = logs,
                showLogs = showLogs,
                onToggleLogs = { showLogs = !showLogs },
                rendererRegistry = rendererRegistry,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showRepeatDialog) {
        LegacyRepeatDialog(
            onDismiss = { showRepeatDialog = false },
            onConfirm = {
                showRepeatDialog = false
                viewModel.startTest()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyTopBar(
    uiState: UiState<TestReport>,
    isRunning: Boolean,
    onBack: () -> Unit
) {
    val semantic = MikLinkThemeTokens.semantic
    val (title, icon, container) = when {
        isRunning -> Triple(
            stringResource(id = R.string.test_execution_running_topbar),
            Icons.Default.HourglassEmpty,
            semantic.runningContainer
        )
        uiState is UiState.Success && uiState.data.overallStatus == "PASS" -> Triple(
            stringResource(id = R.string.test_execution_completed_title_pass),
            Icons.Default.CheckCircle,
            semantic.successContainer
        )
        uiState is UiState.Success -> Triple(
            stringResource(id = R.string.test_execution_completed_title_fail),
            Icons.Default.Error,
            semantic.failureContainer
        )
        uiState is UiState.Error -> Triple(
            stringResource(id = R.string.test_execution_title_error),
            Icons.Default.Error,
            MaterialTheme.colorScheme.errorContainer
        )
        uiState is UiState.Idle -> Triple(
            stringResource(id = R.string.test_execution_title_ready),
            Icons.Default.PlayArrow,
            MaterialTheme.colorScheme.surface
        )
        else -> Triple(
            stringResource(id = R.string.test_execution_title_running),
            Icons.Default.Info,
            MaterialTheme.colorScheme.surface
        )
    }
    val contentColor = when {
        isRunning -> semantic.onRunningContainer
        uiState is UiState.Success && uiState.data.overallStatus == "PASS" -> semantic.onSuccessContainer
        uiState is UiState.Success -> semantic.onFailureContainer
        uiState is UiState.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = contentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = container,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        )
    )
}

@Composable
private fun LegacyRunningContent(
    snapshot: TestRunSnapshot?,
    logs: List<String>,
    showLogs: Boolean,
    onToggleLogs: () -> Unit,
    rendererRegistry: SectionRendererRegistry,
    modifier: Modifier = Modifier
) {
    val ordered = TestSectionDisplayPolicy.ordered(snapshot?.sections.orEmpty())
    val visible = TestSectionDisplayPolicy.visibleForRunning(ordered)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InProgressHeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestExecutionTags.HERO_RUNNING)
            )
        }
        item {
            LogsToggleButton(
                labelOn = stringResource(id = R.string.test_toggle_hide_raw_logs),
                labelOff = stringResource(id = R.string.test_toggle_show_raw_logs),
                showing = showLogs,
                onToggle = onToggleLogs,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestExecutionTags.IN_PROGRESS_TOGGLE)
            )
        }
        if (showLogs) {
            item {
                LogsBox(
                    logs = logs,
                    autoScroll = false,
                    colorize = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            item {
                TestSectionList(
                    sections = visible,
                    rendererRegistry = rendererRegistry
                )
            }
        }
    }
}

@Composable
private fun LegacyCompletedContent(
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 88.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CompletedHeroCard(
                socketName = report.socketName ?: "-",
                isFailed = isFailed,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestExecutionTags.HERO_COMPLETED)
            )
        }
        item {
            DetailsHeaderRow(
                showLogs = showLogs,
                onToggle = onToggleLogs
            )
        }
        if (showLogs) {
            item {
                LogsBox(
                    logs = logs,
                    autoScroll = true,
                    colorize = true,
                    minHeight = 160.dp,
                    maxHeight = 400.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            item {
                TestSectionList(
                    sections = sections,
                    rendererRegistry = rendererRegistry
                )
            }
        }
    }
}

@Composable
private fun InProgressHeroCard(modifier: Modifier = Modifier) {
    val semantic = MikLinkThemeTokens.semantic
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .softGlow(color = semantic.runningGlow, radius = 120.dp, maxAlpha = 0.28f, breathe = true)
                    .background(semantic.running, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    strokeWidth = 6.dp,
                    color = semantic.onRunningContainer
                )
            }
            Text(
                text = stringResource(id = R.string.test_execution_running_hero_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = semantic.running,
                trackColor = semantic.runningContainer
            )
        }
    }
}

@Composable
private fun CompletedHeroCard(
    socketName: String,
    isFailed: Boolean,
    modifier: Modifier = Modifier
) {
    val semantic = MikLinkThemeTokens.semantic
    val accent = if (isFailed) semantic.failure else semantic.success
    val onAccent = if (isFailed) semantic.onFailureContainer else semantic.onSuccessContainer
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .softGlow(color = accent, radius = 110.dp, maxAlpha = 0.22f, breathe = false)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFailed) Icons.Default.Cancel else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = onAccent,
                    modifier = Modifier.size(52.dp)
                )
            }
            Text(
                text = if (isFailed) stringResource(id = R.string.test_execution_completed_hero_fail) else stringResource(
                    id = R.string.test_execution_completed_hero_pass
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = if (isFailed) stringResource(id = R.string.test_execution_completed_subtitle_fail) else stringResource(
                    id = R.string.test_execution_completed_subtitle_pass
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DetailsHeaderRow(
    showLogs: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.test_execution_details_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LogsToggleButton(
            labelOn = stringResource(id = R.string.test_toggle_hide_logs),
            labelOff = stringResource(id = R.string.test_toggle_show_logs),
            showing = showLogs,
            onToggle = onToggle,
            modifier = Modifier.testTag(TestExecutionTags.COMPLETED_TOGGLE)
        )
    }
}

@Composable
private fun LogsToggleButton(
    labelOn: String,
    labelOff: String,
    showing: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (showing) Icons.Default.VisibilityOff else Icons.Default.Code,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (showing) labelOn else labelOff)
    }
}

@Composable
private fun LogsBox(
    logs: List<String>,
    autoScroll: Boolean,
    colorize: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = 140.dp,
    maxHeight: Dp = 260.dp
) {
    RawLogsPane(
        logs = logs,
        emptyLabel = stringResource(id = R.string.test_logs_empty),
        title = stringResource(id = R.string.test_logs_title),
        modifier = modifier.testTag(TestExecutionTags.LOGS_BOX),
        autoScroll = autoScroll,
        colorize = colorize,
        minHeight = minHeight,
        maxHeight = maxHeight
    )
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (info.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.test_execution_group_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            info.forEach { section ->
                SectionCard(section, rendererRegistry)
            }
            HorizontalDivider()
        }
        if (tests.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.test_execution_group_tests),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
        title = section.title ?: section.id.name,
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
private fun sectionIcon(id: TestSectionId): androidx.compose.ui.graphics.vector.ImageVector =
    when (id) {
        TestSectionId.NETWORK -> Icons.Default.SettingsEthernet
        TestSectionId.LINK -> Icons.Default.Link
        TestSectionId.TDR -> Icons.Default.Cable
        TestSectionId.NEIGHBORS -> Icons.Default.Devices
        TestSectionId.PING -> Icons.Default.Wifi
        TestSectionId.SPEED -> Icons.Default.Speed
        else -> Icons.Default.Error
    }

@Composable
private fun statusLabel(status: TestSectionStatus): String =
    when (status) {
        TestSectionStatus.PASS -> stringResource(id = R.string.status_pass)
        TestSectionStatus.FAIL -> stringResource(id = R.string.status_fail)
        TestSectionStatus.SKIP -> stringResource(id = R.string.status_skip)
        TestSectionStatus.RUNNING -> stringResource(id = R.string.test_execution_status_chip_running)
        TestSectionStatus.INFO -> stringResource(id = R.string.status_info)
        else -> status.name
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
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

@Composable
private fun LegacyBottomActionBar(
    isFailed: Boolean,
    onClose: () -> Unit,
    onRepeat: () -> Unit,
    onSave: () -> Unit
) {
    val semantic = MikLinkThemeTokens.semantic
    BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestExecutionTags.BOTTOM_CLOSE),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(id = R.string.test_execution_action_close), maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = onRepeat,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestExecutionTags.BOTTOM_REPEAT),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(id = R.string.test_execution_action_repeat_short), maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TestExecutionTags.BOTTOM_SAVE),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFailed) semantic.failure else semantic.success,
                    contentColor = if (isFailed) semantic.onFailure else semantic.onSuccess
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Icon(
                    if (isFailed) Icons.Default.Error else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(id = R.string.test_execution_action_save), maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LegacyRepeatDialog(
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
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
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
    LegacyRunningContent(
        snapshot = TestRunSnapshot(
            sections = listOf(
                TestSectionSnapshot(TestSectionId.NETWORK, TestSectionStatus.PASS, title = "Network"),
                TestSectionSnapshot(TestSectionId.NEIGHBORS, TestSectionStatus.PASS, title = "LLDP/CDP"),
                TestSectionSnapshot(TestSectionId.LINK, TestSectionStatus.RUNNING, title = "Link")
            ),
            percent = 35
        ),
        logs = listOf("[Init] Avvio test", "[Link] in corso"),
        showLogs = false,
        onToggleLogs = {},
        rendererRegistry = registry
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewExecutionCompleted() {
    val registry = rememberRendererRegistry()
    LegacyCompletedContent(
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
        logs = listOf("[Result] PASS", "Ping target=DHCP_GATEWAY loss=0%"),
        showLogs = true,
        onToggleLogs = {},
        rendererRegistry = registry
    )
}
