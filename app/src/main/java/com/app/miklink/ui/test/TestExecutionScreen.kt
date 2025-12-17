/*
 * Purpose: Compose screen for executing tests, showing live section progress, and handling completion actions (repeat/save/close).
 * Inputs: TestViewModel state flows (uiState, sections, isRunning), navigation controller, and mapped TestSection details.
 * Outputs: UI updates for section cards, progress indicators, dialogs, and save/repeat triggers back to the view model.
 * Notes: Displays all planned sections (including PENDING) to avoid hidden steps and keeps logic UI-only without domain side effects.
 */
package com.app.miklink.ui.test

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerInput
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.R
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.ui.common.TestSectionCard
import com.app.miklink.ui.common.UiText
import com.app.miklink.ui.common.asString
import com.app.miklink.ui.test.TestSectionCategory.INFO
import com.app.miklink.ui.test.TestSectionCategory.TEST
import com.app.miklink.ui.test.TestSectionType.LINK
import com.app.miklink.ui.test.TestSectionType.LLDP
import com.app.miklink.ui.test.TestSectionType.NETWORK
import com.app.miklink.ui.test.TestSectionType.PING
import com.app.miklink.ui.test.TestSectionType.SPEED
import com.app.miklink.ui.test.TestSectionType.TDR
import com.app.miklink.ui.test.components.RawLogsPane
import com.app.miklink.ui.test.components.TestExecutionTags
import com.app.miklink.ui.format.SectionDetailFormatter
import com.app.miklink.ui.format.SectionId
import com.app.miklink.utils.UiState

// Helper per estrarre velocità e CPU load da stringhe come:
// "91.9Mbps local-cpu-load:100%" -> ("91.9Mbps", "100%")
private fun parseSpeedAndLoad(fullText: String?): Pair<String, String?> {
    if (fullText.isNullOrBlank()) return "-" to null
    val trimmed = fullText.trim()
    val speed = trimmed.split(" ").firstOrNull()?.trim().orEmpty()
    val marker = "local-cpu-load:"
    val idx = trimmed.indexOf(marker, ignoreCase = true)
    val load = if (idx >= 0) {
        val after = trimmed.substring(idx + marker.length).trim()
        after.split(" ", "\n", "\t").firstOrNull()?.trim()?.ifBlank { null }
    } else null
    return (if (speed.isBlank()) "-" else speed) to load
}

private fun isSpeedDetailLabel(label: String): Boolean {
    val l = label.lowercase()
    return l == "tcp download" || l == "tcp upload" || l == "udp download" || l == "udp upload"
}

private fun isFinalStatus(status: String): Boolean = status.uppercase() in setOf("PASS", "FAIL", "SKIP")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestExecutionScreen(
    navController: NavController,
    viewModel: TestViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showRepeatDialog by remember { mutableStateOf(false) }
    var hasAutoStarted by rememberSaveable { mutableStateOf(false) }
    var showRawLogs by rememberSaveable { mutableStateOf(false) }
    var showLogs by rememberSaveable { mutableStateOf(false) }

    // Avvio automatico alla prima composizione quando lo stato è Idle
    LaunchedEffect(uiState, isRunning) {
        if (uiState is UiState.Idle && !isRunning && !hasAutoStarted) {
            hasAutoStarted = true
            viewModel.startTest()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        when {
                            isRunning -> {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Test in corso...")
                            }
                            uiState is UiState.Idle -> {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Pronto per il Test")
                            }
                            uiState is UiState.Success -> {
                                val report = (uiState as UiState.Success<TestReport>).data
                                if (report.overallStatus == "PASS") {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Test Completato")
                                } else {
                                    Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFF44336))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Test Fallito")
                                }
                            }
                            uiState is UiState.Loading -> {
                                Text("Preparazione test...")
                            }
                            uiState is UiState.Error -> {
                                Text("Errore Test")
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = com.app.miklink.R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = when {
                        isRunning -> MaterialTheme.colorScheme.primaryContainer
                        uiState is UiState.Success && (uiState as UiState.Success<TestReport>).data.overallStatus == "PASS" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        uiState is UiState.Success -> Color(0xFFF44336).copy(alpha = 0.2f)
                        uiState is UiState.Error -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            )
        },
        bottomBar = {
            if (uiState is UiState.Success) {
                val report = (uiState as UiState.Success<TestReport>).data
                val isFailed = report.overallStatus != "PASS"
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CHIUDI", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { showRepeatDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("RIPETI", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = {
                                viewModel.saveReportToDb(report)
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                if (isFailed) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("SALVA", maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is UiState.Success -> {
                    TestCompletedView(
                        report = state.data,
                        sections = sections,
                        logs = logs,
                        showLogs = showLogs,
                        onToggleLogs = { showLogs = !showLogs },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is UiState.Loading -> {
                    if (isRunning) {
                        TestInProgressView(
                            sections = sections,
                            listState = listState,
                            logs = logs,
                            showRawLogs = showRawLogs,
                            onToggleRawLogs = { showRawLogs = !showRawLogs },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is UiState.Idle -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Test di Certificazione MikLink",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Clicca 'AVVIA TEST' per iniziare la certificazione del dispositivo",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }

    if (showRepeatDialog) {
        AlertDialog(
            onDismissRequest = { showRepeatDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Ripetere il test?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Attenzione: ripetendo il test verranno persi i dati attuali.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Assicurati di:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "• Riposizionarti sulla stessa presa",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Verificare la connessione del cavo",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRepeatDialog = false
                        viewModel.startTest()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Ripeti Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRepeatDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
fun TestInProgressView(
    sections: List<TestSection>,
    listState: LazyListState,
    logs: List<String>,
    showRawLogs: Boolean,
    onToggleRawLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 6.dp
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Test in esecuzione...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = onToggleRawLogs,
            modifier = Modifier
                .align(Alignment.End)
                .testTag(TestExecutionTags.IN_PROGRESS_TOGGLE)
        ) {
            Text(
                text = if (showRawLogs) stringResource(id = com.app.miklink.R.string.test_toggle_hide_raw_logs) else stringResource(
                    id = com.app.miklink.R.string.test_toggle_show_raw_logs
                )
            )
        }

        if (showRawLogs) {
            RawLogsPane(
                logs = logs,
                emptyLabel = stringResource(id = com.app.miklink.R.string.test_logs_empty),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        if (sections.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("In attesa dei risultati...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            val infoSections = sections.filter { it.category == INFO }
            val testSections = sections.filter { it.category == TEST }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (infoSections.isNotEmpty()) {
                    item {
                        Text("Informazioni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(infoSections) { section ->
                        val (icon, color) = when (section.type) {
                            NETWORK -> Icons.Default.SettingsEthernet to MaterialTheme.colorScheme.primary
                            LLDP -> Icons.Default.Devices to MaterialTheme.colorScheme.secondary
                            else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val expandable = isFinalStatus(section.status)
                        TestSectionCard(
                            title = mapTestSectionTitle(section.type, section.title),
                            status = section.status,
                            icon = icon,
                            statusColor = color,
                            expandable = expandable
                        ) {
                            if (expandable) TestSectionDetails(section)
                        }
                    }
                    item { HorizontalDivider() }
                }
                if (testSections.isNotEmpty()) {
                    item {
                        Text("Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(testSections) { section ->
                        val (icon, color) = when (section.type) {
                            LINK -> Icons.Default.Link to MaterialTheme.colorScheme.tertiary
                            PING -> Icons.Default.Wifi to Color(0xFF2196F3)
                            TDR -> Icons.Default.Cable to MaterialTheme.colorScheme.primary
                            SPEED -> Icons.Default.Speed to Color(0xFFFF9800)
                            else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val expandable = isFinalStatus(section.status)
                        TestSectionCard(
                            title = mapTestSectionTitle(section.type, section.title),
                            status = section.status,
                            icon = icon,
                            statusColor = color,
                            expandable = expandable
                        ) {
                            if (expandable) TestSectionDetails(section)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestSectionDetails(section: TestSection) {
    val sectionId = SectionId.fromTestSectionType(section.type)
    if (section.type == PING) {
        PingSectionDetails(section, sectionId)
        return
    }

    section.details.forEach { d ->
        when {
            d.label == "---" -> {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Text(
                    d.value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
            }
            else -> {
                val formatted = SectionDetailFormatter.format(sectionId, d.label, d.value) ?: return@forEach
                val labelText = formatted.label.asString()
                val valueText = formatted.value.asString()
                when {
                    isSpeedDetailLabel(labelText) -> {
                        val (speed, load) = parseSpeedAndLoad(valueText)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(labelText)
                            Text(speed, fontWeight = FontWeight.Bold)
                        }
                        if (!load.isNullOrBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CPU Load")
                                Text(load, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    labelText.equals("Avviso", ignoreCase = true) || labelText.equals(stringResource(id = R.string.detail_label_warning), ignoreCase = true) -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(valueText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(labelText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                valueText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class PingTargetRowData(
    val name: String,
    val loss: String?,
    val min: String?,
    val avg: String?,
    val max: String?,
    val error: String?
)

@Composable
private fun PingSectionDetails(section: TestSection, sectionId: SectionId) {
    val summaryKeys = listOf("Packet Loss", "Min RTT", "Avg RTT", "Max RTT")
    val summaryDetails = summaryKeys.mapNotNull { key ->
        val rawDetail = section.details.firstOrNull { it.label.equals(key, ignoreCase = true) } ?: return@mapNotNull null
        SectionDetailFormatter.format(sectionId, rawDetail.label, rawDetail.value)
    }

    if (summaryDetails.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            summaryDetails.forEach { detail ->
                val labelText = detail.label.asString()
                StatChip(
                    label = labelText,
                    value = detail.value.asString(),
                    icon = iconForPingStat(labelText)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    val targets = section.details.filter { it.label.startsWith("Target ") }
    if (targets.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            targets.map { parsePingTargetDetail(it) }.forEach { target ->
                PingTargetRow(target)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    val consumedLabels = summaryKeys + targets.map { it.label } + listOf("targets")
    section.details.filterNot { detail ->
        detail.label == "---" || consumedLabels.any { consumed -> detail.label.equals(consumed, ignoreCase = true) }
    }.forEach { detail ->
        val formatted = SectionDetailFormatter.format(sectionId, detail.label, detail.value) ?: return@forEach
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatted.label.asString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatted.value.asString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun iconForPingStat(label: String): ImageVector =
    when {
        label.contains("loss", ignoreCase = true) -> Icons.Default.Warning
        label.contains("min", ignoreCase = true) -> Icons.Default.Speed
        label.contains("avg", ignoreCase = true) -> Icons.Default.Speed
        label.contains("max", ignoreCase = true) -> Icons.Default.Speed
        else -> Icons.Default.Info
    }

private fun parsePingTargetDetail(detail: TestDetail): PingTargetRowData {
    val name = detail.label.removePrefix("Target").trim()
    val raw = detail.value
    if (raw.startsWith("ERR", ignoreCase = true)) {
        val error = raw.substringAfter(":", raw).trim().ifBlank { raw }
        return PingTargetRowData(name, null, null, null, null, error)
    }
    val values = mutableMapOf<String, String>()
    raw.split(" ", ";").map { it.trim() }.filter { it.contains("=") }.forEach { token ->
        val parts = token.split("=", limit = 2)
        if (parts.size == 2) {
            values[parts[0].lowercase()] = parts[1]
        }
    }
    return PingTargetRowData(
        name = name,
        loss = values["loss"],
        min = values["min"],
        avg = values["avg"],
        max = values["max"],
        error = null
    )
}

@Composable
private fun PingTargetRow(data: PingTargetRowData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Target ${data.name}", fontWeight = FontWeight.Bold)
                if (data.error == null) {
                    Text(
                        data.loss ?: "-",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (data.error != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(data.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(data.min ?: "-", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Avg", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(data.avg ?: "-", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Max", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(data.max ?: "-", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, icon: ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TestCompletedView(
    report: TestReport,
    sections: List<TestSection>,
    logs: List<String>,
    showLogs: Boolean,
    onToggleLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPassed = report.overallStatus == "PASS"
    val resultColor = if (isPassed) com.app.miklink.ui.theme.TechGreen else com.app.miklink.ui.theme.TechRed
    val backgroundColor = resultColor.copy(alpha = 0.1f)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.75f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow_alpha"
                    )
                    val glowRadius by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 22f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow_radius"
                    )

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .drawBehind {
                                drawCircle(
                                    color = resultColor.copy(alpha = glowAlpha),
                                    radius = (size.minDimension / 2f) + glowRadius
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(resultColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPassed) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (isPassed) "TEST SUPERATO" else "TEST FALLITO",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = resultColor
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (isPassed)
                            "Tutti i test sono stati completati con successo"
                        else
                            "Alcuni test hanno rilevato problemi",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        StatChip(
                            label = "Presa",
                            value = report.socketName ?: "N/A",
                            icon = Icons.Default.PowerInput
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dettagli Test",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onToggleLogs,
                    modifier = Modifier.testTag(TestExecutionTags.COMPLETED_TOGGLE)
                ) {
                    Text(
                        text = if (showLogs) stringResource(id = com.app.miklink.R.string.test_toggle_hide_logs) else stringResource(
                            id = com.app.miklink.R.string.test_toggle_show_logs
                        )
                    )
                }
            }
        }

        if (showLogs) {
            item {
                RawLogsPane(
                    logs = logs,
                    emptyLabel = stringResource(id = com.app.miklink.R.string.test_logs_empty),
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(id = com.app.miklink.R.string.test_logs_title)
                )
            }
        }

        if (sections.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Dettagli non disponibili...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            return@LazyColumn
        }

        val infoSections = sections.filter { it.category == INFO }
        val testSections = sections.filter { it.category == TEST }

        items(infoSections.filter { it.type == NETWORK }) { section ->
            TestSectionCard(
                title = mapTestSectionTitle(section.type, section.title),
                status = section.status,
                icon = Icons.Default.SettingsEthernet,
                statusColor = MaterialTheme.colorScheme.primary
            ) {
                TestSectionDetails(section)
            }
        }

        items(infoSections.filter { it.type == LLDP }) { section ->
            TestSectionCard(
                title = mapTestSectionTitle(section.type, section.title),
                status = section.status,
                icon = Icons.Default.Devices,
                statusColor = MaterialTheme.colorScheme.secondary
            ) {
                TestSectionDetails(section)
            }
        }

        item {
            val linkSec = testSections.find { it.type == LINK }
            if (linkSec != null) {
                TestSectionCard(
                    title = linkSec.title,
                    status = linkSec.status,
                    icon = Icons.Default.Link,
                    statusColor = MaterialTheme.colorScheme.tertiary
                ) {
                    TestSectionDetails(linkSec)
                }
            }
        }

        item {
            val pingSec = testSections.find { it.type == PING }
            if (pingSec != null) {
                val expandByDefault = !pingSec.status.equals("PASS", ignoreCase = true)
                TestSectionCard(
                    title = pingSec.title,
                    status = pingSec.status,
                    icon = Icons.Default.Wifi,
                    statusColor = Color(0xFF2196F3),
                    initialExpanded = expandByDefault
                ) {
                    TestSectionDetails(pingSec)
                }
            }
        }

        item {
            val tdrSec = testSections.find { it.type == TDR }
            if (tdrSec != null) {
                TestSectionCard(
                    title = tdrSec.title,
                    status = tdrSec.status,
                    icon = Icons.Default.Cable,
                    statusColor = MaterialTheme.colorScheme.primary
                ) {
                    TestSectionDetails(tdrSec)
                }
            }
        }

        item {
            val speedSec = testSections.find { it.type == SPEED }
            if (speedSec != null) {
                TestSectionCard(
                    title = speedSec.title,
                    status = speedSec.status,
                    icon = Icons.Default.Speed,
                    statusColor = Color(0xFFFF9800)
                ) {
                    TestSectionDetails(speedSec)
                }
            }
        }
    }
}
