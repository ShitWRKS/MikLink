package com.app.miklink.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.app.miklink.ui.test.TestSectionCategory.*
import com.app.miklink.ui.test.TestSectionType.*
import com.app.miklink.utils.UiState
import com.app.miklink.ui.common.TestSectionCard

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
    var showRepeatDialog by remember { mutableStateOf(false) }
    var hasAutoStarted by remember { mutableStateOf(false) }

    // Riabilitazione autostart: avvia il test automaticamente alla prima composizione
    // ma solo se lo stato è Idle, non in esecuzione, e non è già stato avviato automaticamente
    LaunchedEffect(Unit) {
        if (uiState is UiState.Idle && !isRunning && !hasAutoStarted) {
            hasAutoStarted = true
            viewModel.startTest()
        }
    }

    LaunchedEffect(log.size) {
        if (showRawLog) return@LaunchedEffect
        // autoscroll rimosso per evitare comportamenti strani all'avvio
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors() // rimosso containerColor rosso per coerenza stilistica
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
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is UiState.Success -> {
                    TestCompletedView(
                        report = state.data,
                        sections = sections,
                        log = log,
                        showRawLog = showRawLog,
                        onToggleRawLog = { showRawLog = !showRawLog },
                        modifier = Modifier.fillMaxSize()
                    )
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
    
    // Repeat test confirmation dialog
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
    log: List<String>,
    sections: List<TestSection>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    showRawLog: Boolean,
    onToggleRawLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp).fillMaxWidth(),
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
            RawLogsPane(log = log, modifier = Modifier.fillMaxWidth().weight(1f))
        } else {
            // Sections cards
            val infoSections = sections.filter { it.category == INFO }
            val testSections = sections.filter { it.category == TEST }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (infoSections.isNotEmpty()) {
                    item { Text("Informazioni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(infoSections) { section ->
                        val (icon, color) = when (section.type) {
                            NETWORK -> Icons.Default.SettingsEthernet to MaterialTheme.colorScheme.primary
                            LLDP -> Icons.Default.Devices to MaterialTheme.colorScheme.secondary
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
                            LINK -> Icons.Default.Link to MaterialTheme.colorScheme.tertiary
                            PING -> Icons.Default.Wifi to Color(0xFF2196F3)
                            TDR -> Icons.Default.Cable to MaterialTheme.colorScheme.primary
                            SPEED -> Icons.Default.Speed to Color(0xFFFF9800)
                            else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        // Passiamo i dettagli della sezione così com'erano (persistiti dal ViewModel)
                        TestSectionCard(title = section.title, status = section.status, icon = icon, statusColor = color) {
                            if (section.type == PING) {
                                // Mostra il chip LOSS solo se esiste un valore significativo (non '-' e non vuoto)
                                val lossText = section.details.firstOrNull { it.label == "Packet Loss" }?.value ?: ""
                                if (lossText.isNotBlank() && lossText != "-" && lossText.any { it.isDigit() }) {
                                    val isZeroLoss = lossText.trim().startsWith("0")
                                    val chipBg = if (isZeroLoss) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                    val chipFg = if (isZeroLoss) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    Surface(color = chipBg, shape = RoundedCornerShape(12.dp)) {
                                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(if (isZeroLoss) Icons.Default.CheckCircle else Icons.Default.Error, contentDescription = null, tint = chipFg, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(text = "LOSS ${lossText}", color = chipFg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            // Dettagli generici + stile speciale per Ping rows
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
                                    d.label.startsWith("Ping #") -> {
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
                }
            }
        }
    }
}

@Composable
fun TestCompletedView(
    report: Report,
    sections: List<TestSection>,
    log: List<String>,
    showRawLog: Boolean,
    onToggleRawLog: () -> Unit,
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
                    val infiniteTransition = rememberInfiniteTransition(label = "status_pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(scale)
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
        }

        if (sections.isEmpty() || showRawLog) {
            item {
                // FIX: Non possiamo usare LazyColumn dentro LazyColumn
                // Usiamo Column + verticalScroll per i log nella schermata risultati
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 400.dp),
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        log.forEach { line ->
                            val color = when {
                                line.contains("ERRORE", true) || line.contains("FALLITO", true) || line.contains("FAIL", true) -> MaterialTheme.colorScheme.error
                                line.contains("SUCCESSO", true) || line.contains("PASS", true) -> Color(0xFF2E7D32)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text(
                                text = line,
                                color = color,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
            return@LazyColumn
        }

        val infoSections = sections.filter { it.category == INFO }
        val testSections = sections.filter { it.category == TEST }

        // Network info card
        items(infoSections.filter { it.type == NETWORK }) { section ->
            TestSectionCard(
                title = section.title,
                status = section.status,
                icon = Icons.Default.SettingsEthernet,
                statusColor = MaterialTheme.colorScheme.primary
            ) {
                section.details.forEach { d ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(d.label)
                        Text(d.value, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // LLDP card
        items(infoSections.filter { it.type == LLDP }) { section ->
            TestSectionCard(
                title = section.title,
                status = section.status,
                icon = Icons.Default.Devices,
                statusColor = MaterialTheme.colorScheme.secondary
            ) {
                section.details.forEach { d ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(d.label)
                        Text(d.value, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Link card (single, fixed order)
        item {
            val linkSec = testSections.find { it.type == LINK }
            if (linkSec != null) {
                TestSectionCard(
                    title = linkSec.title,
                    status = linkSec.status,
                    icon = Icons.Default.Link,
                    statusColor = MaterialTheme.colorScheme.tertiary
                ) {
                    linkSec.details.forEach { d ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.label)
                            Text(d.value, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Ping card (single, fixed order)
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
                    // Chip di sintesi Packet Loss (se presente e significativo)
                    val lossText = pingSec.details.firstOrNull { it.label == "Packet Loss" }?.value ?: ""
                    if (lossText.isNotBlank() && lossText != "-" && lossText.any { it.isDigit() }) {
                        val isZeroLoss = lossText.trim().startsWith("0")
                        val chipBg = if (isZeroLoss) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        val chipFg = if (isZeroLoss) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        Surface(color = chipBg, shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (isZeroLoss) Icons.Default.CheckCircle else Icons.Default.Error, contentDescription = null, tint = chipFg, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = "LOSS ${lossText}", color = chipFg, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Dettagli ping con stile
                    pingSec.details.forEach { d ->
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
                            d.label.startsWith("Ping #") -> {
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
        }

        // TDR card (single, fixed order)
        item {
            val tdrSec = testSections.find { it.type == TDR }
            if (tdrSec != null) {
                TestSectionCard(
                    title = tdrSec.title,
                    status = tdrSec.status,
                    icon = Icons.Default.Cable,
                    statusColor = MaterialTheme.colorScheme.primary
                ) {
                    tdrSec.details.forEach { d ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.label)
                            Text(d.value, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Speed Test card (single, fixed order)
        item {
            val speedSec = testSections.find { it.type == SPEED }
            if (speedSec != null) {
                TestSectionCard(
                    title = speedSec.title,
                    status = speedSec.status,
                    icon = Icons.Default.Speed,
                    statusColor = Color(0xFFFF9800)
                ) {
                    speedSec.details.forEach { d ->
                        // Gestione formattata per righe speed e warning
                        when {
                            isSpeedDetailLabel(d.label) -> {
                                val (speed, load) = parseSpeedAndLoad(d.value)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(d.label)
                                    Text(speed, fontWeight = FontWeight.Bold)
                                }
                                if (!load.isNullOrBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("CPU Load")
                                        Text(load, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            d.label.equals("Avviso", ignoreCase = true) -> {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(d.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            else -> {
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
private fun RawLogsPane(log: List<String>, modifier: Modifier = Modifier) {
    val state = rememberLazyListState()
    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) state.animateScrollToItem(log.lastIndex)
    }
    Surface(modifier = modifier, tonalElevation = 2.dp, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        LazyColumn(state = state, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
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
