package com.app.miklink.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.foundation.selection.selectable
import com.app.miklink.data.repository.IdNumberingStrategy
import com.app.miklink.data.repository.ThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
    val idNumberingStrategy by viewModel.idNumberingStrategy.collectAsStateWithLifecycle()
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showIdStrategyDialog by remember { mutableStateOf(false) }

    val themeLabel = when (themeConfig) {
        ThemeConfig.FOLLOW_SYSTEM -> "Auto"
        ThemeConfig.LIGHT -> "Chiaro"
        ThemeConfig.DARK -> "Scuro"
    }

    val idStrategyLabel = when (idNumberingStrategy) {
        IdNumberingStrategy.CONTINUOUS_INCREMENT -> "Continuo"
        IdNumberingStrategy.FILL_GAPS -> "Riempi Buchi"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Impostazioni", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.3f)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card removed by request: 'Configurazione App'


            // Sezione Sonda
            SettingsSection(
                title = "Sonda MikroTik",
                icon = Icons.Default.Router,
                iconColor = Color(0xFF2196F3)
            ) {
                SettingsCard(
                    headline = "Configura Sonda",
                    subtitle = "Gestisci la sonda di test",
                    leadingIcon = Icons.Default.Router,
                    iconColor = Color(0xFF2196F3),
                    onClick = { navController.navigate("probe_edit/-1") }
                )
            }

            // Sezione Gestione Dati
            SettingsSection(
                title = "Gestione Dati",
                icon = Icons.Default.Storage,
                iconColor = Color(0xFF9C27B0)
            ) {
                SettingsCard(
                    headline = "Gestisci Profili",
                    subtitle = "Crea, modifica o elimina profili di test",
                    leadingIcon = Icons.AutoMirrored.Filled.ListAlt,
                    iconColor = Color(0xFF9C27B0),
                    onClick = { navController.navigate("profile_list") }
                )

                SettingsCard(
                    headline = "Gestisci Clienti",
                    subtitle = "Aggiungi o modifica anagrafica clienti",
                    leadingIcon = Icons.Default.Business,
                    iconColor = Color(0xFF9C27B0),
                    onClick = { navController.navigate("client_list") }
                )
            }

            // Sezione Aspetto
            SettingsSection(
                title = "Aspetto",
                icon = Icons.Default.Palette,
                iconColor = Color(0xFFFF9800)
            ) {
                SettingsCard(
                    headline = "Tema",
                    subtitle = "Chiaro, Scuro o Auto",
                    leadingIcon = Icons.Default.DarkMode,
                    iconColor = Color(0xFFFF9800),
                    onClick = { showThemeDialog = true },
                    trailingContent = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = themeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )
            }
            // Sezione Numerazione ID
            SettingsSection(
                title = "Numerazione ID",
                icon = Icons.Default.Tag,
                iconColor = Color(0xFF4CAF50)
            ) {
                SettingsCard(
                    headline = "Strategia Numerazione",
                    subtitle = "Incremento continuo o riuso ID",
                    leadingIcon = Icons.Default.Numbers,
                    iconColor = Color(0xFF4CAF50),
                    onClick = { showIdStrategyDialog = true },
                    trailingContent = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = idStrategyLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )
            }
            // Sezione Modalità di Filtraggio
            SettingsSection(
                title = "Modalità di Filtraggio",
                icon = Icons.Default.Tune,
                iconColor = Color(0xFF3F51B5)
            ) {
                SettingsCard(
                    headline = "Filtraggio CDP/LLDP",
                    subtitle = "Seleziona la modalità di filtraggio",
                    leadingIcon = Icons.Default.FilterList,
                    iconColor = Color(0xFF3F51B5),
                    onClick = { /* TODO: Implement Filtering Mode Selector */ },
                    trailingContent = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Auto",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )
            }

            // Sezione Info
            SettingsSection(
                title = "Informazioni",
                icon = Icons.Default.Info,
                iconColor = Color(0xFF607D8B)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow("Versione", "1.0.0")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        InfoRow("Build", "Debug")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        InfoRow("Developed by", "MikLink Team")
                    }
                }
            }

            // Spacer finale
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Seleziona Tema") },
            text = {
                Column {
                    ThemeConfig.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (themeConfig == theme),
                                onClick = {
                                    viewModel.updateTheme(theme)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (theme) {
                                    ThemeConfig.FOLLOW_SYSTEM -> "Automatico (Sistema)"
                                    ThemeConfig.LIGHT -> "Chiaro"
                                    ThemeConfig.DARK -> "Scuro"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (showIdStrategyDialog) {
        AlertDialog(
            onDismissRequest = { showIdStrategyDialog = false },
            title = { Text("Strategia Numerazione ID") },
            text = {
                Column {
                    IdNumberingStrategy.entries.forEach { strategy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateIdNumberingStrategy(strategy)
                                    showIdStrategyDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (idNumberingStrategy == strategy),
                                onClick = {
                                    viewModel.updateIdNumberingStrategy(strategy)
                                    showIdStrategyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (strategy) {
                                        IdNumberingStrategy.CONTINUOUS_INCREMENT -> "Incremento Continuo"
                                        IdNumberingStrategy.FILL_GAPS -> "Riempi Buchi"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (strategy) {
                                        IdNumberingStrategy.CONTINUOUS_INCREMENT -> "Gli ID continuano sempre ad incrementare (consigliato)"
                                        IdNumberingStrategy.FILL_GAPS -> "Riutilizza gli ID dei test eliminati"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIdStrategyDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsCard(
    headline: String,
    subtitle: String,
    leadingIcon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}