package com.app.miklink.ui.test

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.data.db.model.Report
import com.app.miklink.utils.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestExecutionScreen(
    navController: NavController,
    viewModel: TestViewModel // injected from NavGraph scoped backStackEntry
) {
    val log by viewModel.log.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var showRawLog by rememberSaveable { mutableStateOf(false) }

    // Riabilitazione autostart: avvia il test automaticamente alla prima composizione
    // ma solo se lo stato è Idle e non in esecuzione
    LaunchedEffect(Unit) {
        if (uiState is UiState.Idle && !isRunning) {
            // small guard: only start when Idle and not running
            viewModel.startTest()
        }
    }

    LaunchedEffect(log.size) {
        if (showRawLog) return@LaunchedEffect
        // autoscroll rimosso per evitare comportamenti strani all'avvio
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        if (isRunning) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Test in corso...")
                        } else when (uiState) {
                            is UiState.Idle -> {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Pronto per il Test")
                            }
                            is UiState.Success -> {
                                val report = (uiState as UiState.Success<Report>).data
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
                            is UiState.Loading -> { Text("Preparazione test...") }
                            is UiState.Error -> Text("Errore Test")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = when {
                        isRunning -> MaterialTheme.colorScheme.primaryContainer
                        uiState is UiState.Success && (uiState as UiState.Success<Report>).data.overallStatus == "PASS" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        uiState is UiState.Success -> Color(0xFFF44336).copy(alpha = 0.2f)
                        uiState is UiState.Error -> MaterialTheme.colorScheme.errorContainer
                        uiState is UiState.Idle -> MaterialTheme.colorScheme.surface
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            )
        },
        bottomBar = {
            if (uiState is UiState.Success) {
                val report = (uiState as UiState.Success<Report>).data
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
                            onClick = viewModel::startTest,
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
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFailed)
                                    MaterialTheme.colorScheme.error
                                else
                                    Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(
                                if (isFailed) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
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
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is UiState.Success -> {
                    TestCompletedView(report = state.data, sections = sections, log = log, listState = listState, showRawLog = showRawLog, onToggleRawLog = { showRawLog = !showRawLog })
                }
                is UiState.Loading -> {
                    if (isRunning) {
                        TestInProgressView(log = log, sections = sections, listState = listState, showRawLog = showRawLog, onToggleRawLog = { showRawLog = !showRawLog })
                    }
                }
                is UiState.Idle -> {
                    // Stato iniziale: mostra messaggio di benvenuto invece dell'header
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
}

@Composable
fun TestInProgressView(
    log: List<String>,
    sections: List<TestSection>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    showRawLog: Boolean,
    onToggleRawLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header con progress indicator
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

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Toggle log grezzi / sections
        TextButton(onClick = onToggleRawLog) {
            Icon(if (showRawLog) Icons.Default.VisibilityOff else Icons.Default.Code, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (showRawLog) "Nascondi log grezzi" else "Mostra log grezzi")
        }

        Spacer(Modifier.height(8.dp))

        if (showRawLog) {
            // Raw log
            RawLogsPane(log = log, modifier = Modifier.fillMaxSize())
        } else {
            // Sections cards
            val infoSections = sections.filter { it.category == TestSectionCategory.INFO }
            val testSections = sections.filter { it.category == TestSectionCategory.TEST }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (infoSections.isNotEmpty()) {
                    item { Text("Informazioni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(infoSections) { section ->
                        val (icon, color) = when (section.type) {
                            TestSectionType.NETWORK -> Icons.Default.SettingsEthernet to MaterialTheme.colorScheme.primary
                            TestSectionType.LLDP -> Icons.Default.Devices to MaterialTheme.colorScheme.secondary
                            else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        TestSectionCard(title = section.title, status = section.status, icon = icon, statusColor = color) {
                            section.details.forEach { d ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(d.label)
                                    Text(d.value, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item { HorizontalDivider() }
                }
                if (testSections.isNotEmpty()) {
                    item { Text("Test", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(testSections) { section ->
                        val (icon, color) = when (section.type) {
                            TestSectionType.LINK -> Icons.Default.Link to MaterialTheme.colorScheme.tertiary
                            TestSectionType.PING -> Icons.Default.Wifi to Color(0xFF2196F3)
                            TestSectionType.TRACEROUTE -> Icons.Default.Timeline to MaterialTheme.colorScheme.primary
                            TestSectionType.TDR -> Icons.Default.Cable to MaterialTheme.colorScheme.primary
                            else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        TestSectionCard(title = section.title, status = section.status, icon = icon, statusColor = color) {
                            section.details.forEach { d ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(d.label)
                                    Text(d.value, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TestLogItem(message: String) {
    val (icon, iconColor, backgroundColor) = when {
        message.contains("SUCCESSO", ignoreCase = true) || message.contains("✓") ->
            Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), Color(0xFF4CAF50).copy(alpha = 0.1f))
        message.contains("FALLITO", ignoreCase = true) || message.contains("FAIL", ignoreCase = true) || message.contains("✗") ->
            Triple(Icons.Default.Error, Color(0xFFF44336), Color(0xFFF44336).copy(alpha = 0.1f))
        message.contains("TDR") || message.contains("Cable") ->
            Triple(Icons.Default.Cable, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
        message.contains("Link") || message.contains("Stato") ->
            Triple(Icons.Default.Link, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.tertiaryContainer)
        message.contains("LLDP") || message.contains("CDP") ->
            Triple(Icons.Default.Devices, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer)
        message.contains("Ping") ->
            Triple(Icons.Default.Wifi, Color(0xFF2196F3), Color(0xFF2196F3).copy(alpha = 0.1f))
        message.contains("FASE") || message.contains("---") ->
            Triple(Icons.Default.Info, MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surfaceVariant)
        else ->
            Triple(Icons.Default.Circle, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun TestCompletedView(
    report: Report,
    sections: List<TestSection>,
    log: List<String>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    showRawLog: Boolean,
    onToggleRawLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPassed = report.overallStatus == "PASS"
    val resultColor = if (isPassed) Color(0xFF4CAF50) else Color(0xFFF44336)
    val backgroundColor = resultColor.copy(alpha = 0.1f)

    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Header risultato
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

                // Statistiche rapide
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(
                        label = "Sonda",
                        value = report.probeName ?: "N/A",
                        icon = Icons.Default.Router
                    )
                    StatChip(
                        label = "Presa",
                        value = report.socketName ?: "N/A",
                        icon = Icons.Default.PowerInput
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Toggle log grezzi / sections
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
            TextButton(onClick = onToggleRawLog) {
                Icon(if (showRawLog) Icons.Default.VisibilityOff else Icons.Default.Code, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (showRawLog) "Nascondi log" else "Mostra log", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(8.dp))

        // If no sections available OR showRawLog, fallback to raw log
        if (sections.isEmpty() || showRawLog) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(log) { message ->
                    TestLogItem(message = message)
                }
            }
            return
        }

        // Render INFO sections first (NETWORK, LLDP)
        val infoSections = sections.filter { it.category == TestSectionCategory.INFO }
        val testSections = sections.filter { it.category == TestSectionCategory.TEST }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Network info card
            infoSections.filter { it.type == TestSectionType.NETWORK }.forEach { section ->
                TestSectionCard(title = section.title, status = section.status, icon = Icons.Default.SettingsEthernet, statusColor = MaterialTheme.colorScheme.primary) {
                    section.details.forEach { d ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.label)
                            Text(d.value, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // LLDP card
            infoSections.filter { it.type == TestSectionType.LLDP }.forEach { section ->
                TestSectionCard(title = section.title, status = section.status, icon = Icons.Default.Devices, statusColor = MaterialTheme.colorScheme.secondary) {
                    section.details.forEach { d ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.label)
                            Text(d.value, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Test cards in fixed order: Link, Ping, Traceroute, TDR
            val linkSec = testSections.find { it.type == TestSectionType.LINK }
            if (linkSec != null) {
                TestSectionCard(title = linkSec.title, status = linkSec.status, icon = Icons.Default.Link, statusColor = MaterialTheme.colorScheme.tertiary) {
                    linkSec.details.forEach { d ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.label)
                            Text(d.value, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val pingSec = testSections.find { it.type == TestSectionType.PING }
            if (pingSec != null) {
                TestSectionCard(title = pingSec.title, status = pingSec.status, icon = Icons.Default.Wifi, statusColor = Color(0xFF2196F3)) {
                    pingSec.details.forEach { d ->
                        when {
                            d.label == "---" -> {
                                // Separatore visivo
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
                            d.label.startsWith("Ping #") -> {
                                // Dettaglio ping individuale con stile monospace
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        d.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        d.value,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            else -> {
                                // Dettaglio normale
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        d.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        d.value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val tracerSec = testSections.find { it.type == TestSectionType.TRACEROUTE }
            if (tracerSec != null) {
                TestSectionCard(title = tracerSec.title, status = tracerSec.status, icon = Icons.Default.Timeline, statusColor = MaterialTheme.colorScheme.primary) {
                    tracerSec.details.forEach { d ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.label)
                            Text(d.value, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val tdrSec = testSections.find { it.type == TestSectionType.TDR }
            if (tdrSec != null) {
                TestSectionCard(title = tdrSec.title, status = tdrSec.status, icon = Icons.Default.Cable, statusColor = MaterialTheme.colorScheme.primary) {
                    tdrSec.details.forEach { d ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.label)
                            Text(d.value, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    icon: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (bg, fg, ic) = when (status.uppercase()) {
        "PASS" -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.Default.Check)
        "FAIL" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Icons.Default.Close)
        "PARTIAL", "INFO" -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Info)
        "SKIPPED" -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.SkipNext)
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Info)
    }
    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(ic, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(status.uppercase(), color = fg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TestSectionCard(title: String, status: String, icon: ImageVector, statusColor: Color, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header sempre visibile
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusChip(status)
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Comprimi" else "Espandi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Contenuto espandibile
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun RawLogsPane(log: List<String>, modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) state.animateScrollToItem(log.lastIndex)
    }
    Surface(modifier = modifier, tonalElevation = 2.dp, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        LazyColumn(state = state, modifier = Modifier.fillMaxSize().padding(12.dp)) {
            items(log.size) { idx ->
                val line = log[idx]
                val color = when {
                    line.contains("ERRORE", true) || line.contains("FALLITO", true) || line.contains("FAIL", true) -> MaterialTheme.colorScheme.error
                    line.contains("SUCCESSO", true) || line.contains("PASS", true) -> Color(0xFF2E7D32)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(line, color = color, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
