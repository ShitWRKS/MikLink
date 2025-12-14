package com.app.miklink.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.core.domain.model.report.ReportData
import com.app.miklink.ui.common.TestSectionCard
import com.app.miklink.ui.test.TestSectionType
import com.app.miklink.ui.test.mapTestSectionTitle
import com.app.miklink.utils.normalizeLinkSpeed
import com.app.miklink.utils.normalizeLinkStatus
import com.app.miklink.utils.normalizeTime
import kotlinx.coroutines.launch

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

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(pdfStatus) {
        if (pdfStatus.isNotBlank()) {
            coroutineScope.launch { snackbarHostState.showSnackbar(pdfStatus) }
        }
    }

    val tabs = listOf("Summary", "Edit")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Report #${report?.reportId ?: "..."}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { stateProvider.exportReportToPdf() }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TopAppBarDefaults.topAppBarColors().containerColor
                )
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null) {
            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.padding(padding))
        } else {
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> SummaryTab(results)
                    else -> EditTab(
                        socketName = socketName,
                        notes = notes,
                        onSocketNameChange = { stateProvider.socketName.value = it },
                        onNotesChange = { stateProvider.notes.value = it },
                        onSave = { stateProvider.updateReportDetails() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTab(results: ReportData?) {
    if (results == null) {
        Text("Nessun risultato parsato", modifier = Modifier.padding(16.dp))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        results.linkStatus?.let { link ->
            TestSectionCard(
                title = mapTestSectionTitle(TestSectionType.LINK, "Link"),
                status = "INFO",
                icon = Icons.Filled.Link,
                statusColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier,
                detailsTestTag = "LinkDetailsView"
            ) {
                InfoRow("Status", normalizeLinkStatus(link.status))
                link.rate?.let { InfoRow("Speed", normalizeLinkSpeed(it)) }
            }
        }

        val neighbors = results.neighbors
        if (neighbors.isNotEmpty()) {
            TestSectionCard(
                title = mapTestSectionTitle(TestSectionType.LLDP, "LLDP"),
                status = "INFO",
                icon = Icons.Filled.Devices,
                statusColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier,
                detailsTestTag = "LldpDetailsView"
            ) {
                neighbors.forEach { n ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(n.identity ?: "N/A", fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Interface")
                            Text(n.interfaceName ?: "-", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discovered")
                            Text(n.discoveredBy ?: "-", fontFamily = FontFamily.Monospace)
                        }
                        n.vlanId?.let {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("VLAN")
                                Text(it, fontWeight = FontWeight.Bold)
                            }
                        }
                        n.voiceVlanId?.let {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Voice VLAN")
                                Text(it, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        val pingList = results.pingSamples
        if (pingList.isNotEmpty()) {
            val last = pingList.last()
            TestSectionCard(
                title = mapTestSectionTitle(TestSectionType.PING, "Ping"),
                status = when (last.packetLoss?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull()) {
                    null -> "INFO"
                    0.0 -> "PASS"
                    else -> "FAIL"
                },
                icon = Icons.Filled.Wifi,
                statusColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("PingResultCard"),
                detailsTestTag = "PingDetailsView"
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Avg: ${normalizeTime(last.avgRtt)}")
                    Text("Min: ${normalizeTime(last.minRtt)}")
                    Text("Max: ${normalizeTime(last.maxRtt)}")
                    Text("Loss: ${(last.packetLoss ?: "-")}%")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
                }
                pingList.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ping #${p.seq ?: "?"}")
                        Text(
                            "time=${normalizeTime(p.time)} ttl=${p.ttl ?: "N/A"}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        val tdrList = results.tdr
        if (tdrList.isNotEmpty()) {
            TestSectionCard(
                title = mapTestSectionTitle(TestSectionType.TDR, "TDR"),
                status = when {
                    tdrList.any { (it.status ?: "").contains("fail", ignoreCase = true) || (it.status ?: "").contains("short", ignoreCase = true) } -> "FAIL"
                    tdrList.any { (it.status ?: "").lowercase() in listOf("ok", "open", "link-ok") } -> "PASS"
                    else -> "INFO"
                },
                icon = Icons.Filled.Cable,
                statusColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("TdrResultCard"),
                detailsTestTag = "TdrDetailsView"
            ) {
                tdrList.forEach { r ->
                    Text("Status: ${r.status ?: "-"}", fontWeight = FontWeight.Bold)
                    r.distance?.let {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Distance")
                            Text(it, fontWeight = FontWeight.Bold)
                        }
                    }
                    r.description?.let {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pair")
                            Text(it, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }

        results.speedTest?.let { speed ->
            TestSectionCard(
                title = "Speed Test",
                status = speed.status ?: "INFO",
                icon = Icons.Filled.Speed,
                statusColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("SpeedTestCard"),
                detailsTestTag = "SpeedDetailsView"
            ) {
                speed.serverAddress?.let { InfoRow("Server", it) }
                speed.tcpDownload?.let { InfoRow("TCP Download", it) }
                speed.tcpUpload?.let { InfoRow("TCP Upload", it) }
                speed.udpDownload?.let { InfoRow("UDP Download", it) }
                speed.udpUpload?.let { InfoRow("UDP Upload", it) }
                speed.ping?.let { InfoRow("Ping", it) }
                speed.jitter?.let { InfoRow("Jitter", it) }
                speed.loss?.let { InfoRow("Loss", it) }
                speed.warning?.let { InfoRow("Warning", it) }
            }
        }
    }
}

@Composable
private fun EditTab(
    socketName: String,
    notes: String,
    onSocketNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Edit Details", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = socketName,
            onValueChange = onSocketNameChange,
            label = { Text("Socket Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = onSave) { Text("Save") }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
