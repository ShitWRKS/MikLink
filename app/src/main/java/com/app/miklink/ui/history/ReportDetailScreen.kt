/*
 * Purpose: Present a single report detail with legacy single-column layout, using parsed ReportData and renderer registry.
 * Inputs: ReportDetailScreenStateProvider (report, parsed results, metadata state) and NavController for navigation.
 * Outputs: Hero, network/test detail cards, edit/actions, PDF/export actions; no data parsing changes.
 */
package com.app.miklink.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.R
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.model.report.ReportData
import com.app.miklink.core.domain.test.model.TestSectionId
import com.app.miklink.core.domain.test.model.TestSectionStatus
import com.app.miklink.ui.feature.test_details.ReportDataToSnapshotMapper
import com.app.miklink.ui.feature.test_details.SectionRendererRegistry
import com.app.miklink.ui.feature.test_details.renderers.LinkSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.NetworkSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.NeighborsSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.PingSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.SpeedSectionRenderer
import com.app.miklink.ui.feature.test_details.renderers.TdrSectionRenderer
import com.app.miklink.utils.normalizeLinkSpeed
import com.app.miklink.utils.normalizeTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReportDetailScreen(
    navController: NavController,
    viewModel: ReportDetailViewModel = hiltViewModel()
) {
    ReportDetailScreen(
        navController = navController,
        stateProvider = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    navController: NavController,
    stateProvider: ReportDetailScreenStateProvider
) {
    val report by stateProvider.report.collectAsStateWithLifecycle()
    val results by stateProvider.parsedResults.collectAsStateWithLifecycle()
    val pdfStatus by stateProvider.pdfStatus.collectAsStateWithLifecycle()
    val socketName by stateProvider.socketName.collectAsStateWithLifecycle()
    val notes by stateProvider.notes.collectAsStateWithLifecycle()
    val clientName by stateProvider.clientName.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pdfStatus) {
        if (pdfStatus.isNotBlank()) {
            coroutineScope.launch { snackbarHostState.showSnackbar(pdfStatus) }
        }
    }

    val snapshotMapper = remember { ReportDataToSnapshotMapper() }
    val rendererRegistry = remember {
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "$clientName - ${report?.socketName ?: "..."}"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { stateProvider.exportReportToPdf() }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                )
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.padding(padding))
        } else {
            val snapshot = results?.let { snapshotMapper.map(it) }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ReportHero(
                        report = currentReport,
                        results = results,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    NetworkInfoCard(
                        snapshot = snapshot,
                        rendererRegistry = rendererRegistry
                    )
                }
                item {
                    TestResultsCard(
                        snapshot = snapshot,
                        rendererRegistry = rendererRegistry
                    )
                }
                item {
                    EditDetailsCard(
                        socketName = socketName,
                        notes = notes,
                        onSocketNameChange = { stateProvider.socketName.value = it },
                        onNotesChange = { stateProvider.notes.value = it },
                        onSave = { stateProvider.updateReportDetails() }
                    )
                }
                item {
                    ActionsCard(
                        onRepeat = { showRepeatDialog = true },
                        onDelete = { showDeleteDialog = true }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(id = R.string.report_detail_delete_title),
            message = stringResource(id = R.string.report_detail_delete_body),
            confirmLabel = stringResource(id = R.string.report_detail_delete_confirm),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                coroutineScope.launch {
                    stateProvider.deleteReport()
                    navController.popBackStack()
                }
            }
        )
    }

    if (showRepeatDialog) {
        val repeatErrorMessage = stringResource(id = R.string.report_detail_repeat_error)
        ConfirmDialog(
            title = stringResource(id = R.string.report_detail_repeat_title),
            message = stringResource(id = R.string.report_detail_repeat_body),
            confirmLabel = stringResource(id = R.string.report_detail_repeat_confirm),
            onDismiss = { showRepeatDialog = false },
            onConfirm = {
                showRepeatDialog = false
                coroutineScope.launch {
                    val route = stateProvider.buildRepeatRoute()
                    if (route != null) {
                        navController.navigate(route)
                    } else {
                        snackbarHostState.showSnackbar(message = repeatErrorMessage)
                    }
                }
            }
        )
    }
}

@Composable
private fun ReportHero(report: TestReport, results: ReportData?, modifier: Modifier = Modifier) {
    val isFailed = report.overallStatus != "PASS"
    val accent = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val background = accent.copy(alpha = 0.1f)
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(96.dp)
                    .drawBehind {
                        val radius = size.minDimension / 2
                        drawCircle(color = accent.copy(alpha = 0.18f), radius = radius * 1.6f, center = center)
                        drawCircle(color = accent.copy(alpha = 0.1f), radius = radius * 2f, center = center)
                    }
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accent)
                    .testTag("report_hero_icon"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFailed) Icons.Default.Cancel else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                text = report.overallStatus,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.ITALY)
                    .format(java.util.Date(report.timestamp)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val pingSummary = results?.pingSamples?.lastOrNull()
                QuickStatItem(
                    label = stringResource(id = R.string.report_detail_stat_ping_avg),
                    value = normalizeTime(pingSummary?.avgRtt),
                    icon = Icons.Default.Wifi
                )
                QuickStatItem(
                    label = stringResource(id = R.string.report_detail_stat_loss),
                    value = "${pingSummary?.packetLoss ?: "0"}%",
                    icon = Icons.Default.Wifi
                )
                QuickStatItem(
                    label = stringResource(id = R.string.report_detail_stat_speed),
                    value = normalizeLinkSpeed(results?.linkStatus?.rate),
                    icon = Icons.Default.Link
                )
            }
        }
    }
}

@Composable
private fun QuickStatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NetworkInfoCard(
    snapshot: com.app.miklink.core.domain.test.model.TestRunSnapshot?,
    rendererRegistry: SectionRendererRegistry
) {
    val networkSection = snapshot?.sections?.find { it.id == TestSectionId.NETWORK }
    val lldpSection = snapshot?.sections?.find { it.id == TestSectionId.NEIGHBORS }
    val linkSection = snapshot?.sections?.find { it.id == TestSectionId.LINK }
    if (networkSection == null && lldpSection == null && linkSection == null) return

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.report_detail_network_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            networkSection?.let {
                rendererRegistry.rendererFor(TestSectionId.NETWORK).Render(it, Modifier.fillMaxWidth())
            }
            linkSection?.let {
                rendererRegistry.rendererFor(TestSectionId.LINK).Render(it, Modifier.fillMaxWidth())
            }
            lldpSection?.let {
                rendererRegistry.rendererFor(TestSectionId.NEIGHBORS).Render(it, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TestResultsCard(
    snapshot: com.app.miklink.core.domain.test.model.TestRunSnapshot?,
    rendererRegistry: SectionRendererRegistry
) {
    val pingSection = snapshot?.sections?.find { it.id == TestSectionId.PING }
    val tdrSection = snapshot?.sections?.find { it.id == TestSectionId.TDR }
    if (pingSection == null && tdrSection == null) return

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.report_detail_tests_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            pingSection?.let {
                Text(
                    text = stringResource(id = R.string.report_detail_ping_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                rendererRegistry.rendererFor(TestSectionId.PING).Render(
                    it.copy(status = TestSectionStatus.PASS),
                    Modifier.fillMaxWidth()
                )
            }
            tdrSection?.let {
                HorizontalDivider()
                Text(
                    text = stringResource(id = R.string.report_detail_tdr_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                rendererRegistry.rendererFor(TestSectionId.TDR).Render(
                    it.copy(status = TestSectionStatus.PASS),
                    Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun EditDetailsCard(
    socketName: String,
    notes: String,
    onSocketNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.report_detail_edit_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = socketName,
                onValueChange = onSocketNameChange,
                label = { Text(stringResource(id = R.string.report_detail_edit_socket)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(id = R.string.report_detail_edit_notes)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onSave) {
                    Text(stringResource(id = R.string.report_detail_edit_save))
                }
            }
        }
    }
}

@Composable
private fun ActionsCard(
    onRepeat: () -> Unit,
    onDelete: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.report_detail_actions_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.report_detail_actions_delete))
                }
                Button(
                    onClick = onRepeat,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.report_detail_actions_repeat))
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewReportDetail() {
    val provider = remember { FakeReportDetailProvider() }
    ReportDetailScreen(
        navController = androidx.navigation.compose.rememberNavController(),
        stateProvider = provider
    )
}

private class FakeReportDetailProvider : ReportDetailScreenStateProvider {
    override val report: StateFlow<TestReport?> = MutableStateFlow(
        TestReport(
            reportId = 1,
            clientId = 1,
            timestamp = System.currentTimeMillis(),
            socketName = "Presa 1",
            notes = "Nota di test",
            probeName = "Probe",
            profileName = "Default",
            overallStatus = "PASS",
            resultsJson = ""
        )
    )
    override val parsedResults: StateFlow<ReportData?> = MutableStateFlow(
        ReportData(
            linkStatus = com.app.miklink.core.domain.model.report.LinkStatusData(status = "link-ok", rate = "1Gbps"),
            neighbors = emptyList(),
            pingSamples = listOf(
                com.app.miklink.core.domain.model.report.PingSample(
                    target = "DHCP_GATEWAY",
                    host = "192.168.1.1",
                    avgRtt = "10ms",
                    minRtt = "8ms",
                    maxRtt = "12ms",
                    packetLoss = "0",
                    sent = "5",
                    seq = "0",
                    time = "10ms",
                    ttl = "64"
                )
            ),
            tdr = listOf(com.app.miklink.core.domain.model.report.TdrEntry(status = "ok")),
            extra = mapOf("client" to "Client")
        )
    )
    override val pdfStatus: StateFlow<String> = MutableStateFlow("")
    override val clientName: StateFlow<String> = MutableStateFlow("Client Preview")
    override val socketName: MutableStateFlow<String> = MutableStateFlow("Presa 1")
    override val notes: MutableStateFlow<String> = MutableStateFlow("Nota di test")
    override fun updateReportDetails() {}
    override fun exportReportToPdf() {}
    override suspend fun buildRepeatRoute(): String? = null
    override suspend fun deleteReport() {}
}
