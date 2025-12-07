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
import com.app.miklink.ui.theme.isLightChain
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

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
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                SettingsCard(
                    headline = "Configura Sonda",
                    subtitle = "Gestisci la sonda di test",
                    leadingIcon = Icons.Default.Router,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("probe_edit/-1") }
                )
            }

            // Sezione Gestione Dati
            SettingsSection(
                title = "Gestione Dati",
                icon = Icons.Default.Storage,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                SettingsCard(
                    headline = "Gestisci Profili",
                    subtitle = "Crea, modifica o elimina profili di test",
                    leadingIcon = Icons.AutoMirrored.Filled.ListAlt,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("profile_list") }
                )

                SettingsCard(
                    headline = "Gestisci Clienti",
                    subtitle = "Aggiungi o modifica anagrafica clienti",
                    leadingIcon = Icons.Default.Business,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("client_list") }
                )
            }

            // Sezione Aspetto
            SettingsSection(
                title = "Aspetto",
                icon = Icons.Default.Palette,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                SettingsCard(
                    headline = "Tema",
                    subtitle = "Chiaro, Scuro o Auto",
                    leadingIcon = Icons.Default.DarkMode,
                    iconColor = MaterialTheme.colorScheme.primary,
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

            
            // Sezione Report PDF
            SettingsSection(
                title = "Report PDF",
                icon = Icons.Default.PictureAsPdf,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                SettingsCard(
                    headline = "Preferenze Rapporto PDF",
                    subtitle = "Configura colonne, titoli e opzioni di stampa",
                    leadingIcon = Icons.Default.SettingsApplications,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("pdf_settings") }
                )
            }

            // Sezione Numerazione ID
            SettingsSection(
                title = "Numerazione ID",
                icon = Icons.Default.Tag,
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                SettingsCard(
                    headline = "Strategia Numerazione",
                    subtitle = "Incremento continuo o riuso ID",
                    leadingIcon = Icons.Default.Numbers,
                    iconColor = MaterialTheme.colorScheme.primary,
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
                iconColor = MaterialTheme.colorScheme.primary
            ) {
                SettingsCard(
                    headline = "Filtraggio CDP/LLDP",
                    subtitle = "Seleziona la modalità di filtraggio",
                    leadingIcon = Icons.Default.FilterList,
                    iconColor = MaterialTheme.colorScheme.primary,
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
                iconColor = MaterialTheme.colorScheme.secondary
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
                        InfoRow("Developed by", "Shitworks mostly with Vibe Coding")
                    }
                }
            }

            // Spacer finale
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentConfig = themeConfig,
            currentPalette = viewModel.customPalette.collectAsStateWithLifecycle().value,
            onDismiss = { showThemeDialog = false },
            onSave = { config, primary, secondary, background, content ->
                viewModel.updateTheme(config)
                viewModel.updateCustomPalette(primary, secondary, background, content)
                showThemeDialog = false
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSelectionDialog(
    currentConfig: ThemeConfig,
    currentPalette: com.app.miklink.data.repository.UserPreferencesRepository.CustomPalette,
    onDismiss: () -> Unit,
    onSave: (ThemeConfig, Int?, Int?, Int?, Int?) -> Unit
) {
    var selectedConfig by remember { mutableStateOf(currentConfig) }
    var primaryColor by remember { mutableStateOf(currentPalette.primary) }
    var secondaryColor by remember { mutableStateOf(currentPalette.secondary) }
    var backgroundColor by remember { mutableStateOf(currentPalette.background) }
    var customContentColor by remember { mutableStateOf(currentPalette.content) }

    // Presets
    data class Preset(val name: String, val primary: Int, val secondary: Int, val background: Int? = null, val content: Int? = null)
    val presets = listOf(
        Preset("Inspired", 0xFF37474F.toInt(), 0xFF0066CC.toInt()), // Changed Secondary to Blue to avoid Red
        Preset("Classic Blue", 0xFF0066CC.toInt(), 0xFF004C99.toInt()),
        Preset("Alarm Orange", 0xFFEF6C00.toInt(), 0xFFE65100.toInt(), 0xFF121212.toInt(), 0xFFFFFFFF.toInt())
    )

    // Common Colors for Grid (Removed Reds/Greens)
    val colors = listOf(
        0xFF37474F, 0xFF0066CC, 0xFFEF6C00, // Presets
        0xFF7B1FA2, 0xFF512DA8, 0xFF303F9F, // Purples/Pinks
        0xFF0288D1, 0xFF0097A7, 0xFF00796B, // Blues
        0xFFAFB42B, 0xFFFBC02D, 0xFFFFA000, 0xFFF57C00, // Yellows/Oranges
        0xFF5D4037, 0xFF616161, 0xFF455A64, 0xFFFFFFFF, 0xFF000000  // Greys + White/Black for Content
    ).map { it.toInt() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(selectedConfig, primaryColor, secondaryColor, backgroundColor, customContentColor) }) {
                Text("Applica")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
        title = { Text("Personalizza Tema") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Selector
                Column {
                    Text("Modalità", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ThemeConfig.entries.forEach { config ->
                            FilterChip(
                                selected = selectedConfig == config,
                                onClick = { selectedConfig = config },
                                label = { 
                                    Text(when(config) {
                                        ThemeConfig.FOLLOW_SYSTEM -> "Auto"
                                        ThemeConfig.LIGHT -> "Light"
                                        ThemeConfig.DARK -> "Dark"
                                    })
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Presets
                Column {
                    Text("Presets", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presets.forEach { preset ->
                            SuggestionChip(
                                onClick = {
                                    primaryColor = preset.primary
                                    secondaryColor = preset.secondary
                                    backgroundColor = preset.background
                                    customContentColor = preset.content
                                },
                                label = { Text(preset.name) },
                                icon = {
                                    Box(
                                        Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.primary))
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Custom Colors
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Colori Personalizzati", style = MaterialTheme.typography.labelMedium)
                    
                    // Primary Picker
                    ColorPickerRow("Primario", colors, primaryColor) { primaryColor = it }
                    
                    // Secondary Picker
                    ColorPickerRow("Secondario", colors, secondaryColor) { secondaryColor = it }

                    // Background Picker
                    ColorPickerRow("Sfondo (Opzionale)", colors, backgroundColor) { 
                        backgroundColor = if (backgroundColor == it) null else it 
                    }
                    
                    // Content Picker
                    ColorPickerRow("Testo/Icone (Opzionale)", colors, customContentColor) { 
                        customContentColor = if (customContentColor == it) null else it 
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(label: String, colors: List<Int>, selectedColor: Int?, onSelect: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            colors.forEach { colorInt ->
                val color = Color(colorInt)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onSelect(colorInt) }
                        .then(
                            if (selectedColor == colorInt) Modifier.background(Color.White.copy(alpha = 0.5f)) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor == colorInt) {
                        Icon(Icons.Default.Check, null, tint = if (isLightChain(color)) Color.Black else Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}