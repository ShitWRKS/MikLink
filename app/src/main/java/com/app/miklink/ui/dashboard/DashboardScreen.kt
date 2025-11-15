package com.app.miklink.ui.dashboard

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val currentProbe by viewModel.currentProbe.collectAsStateWithLifecycle() // MODIFICATO
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()

    val selectedClient by viewModel.selectedClient.collectAsStateWithLifecycle()
    // RIMOSSO: selectedProbe
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val socketName by viewModel.socketName.collectAsStateWithLifecycle()
    val isProbeOnline by viewModel.isProbeOnline.collectAsStateWithLifecycle()

    val isTestButtonEnabled = selectedClient != null && currentProbe != null &&
                            selectedProfile != null && socketName.isNotBlank()

    // Warning per sonda offline (non bloccante)
    val showProbeOfflineWarning = currentProbe != null && !isProbeOnline

    // Animazione pulsante quando pronto
    val infiniteTransition = rememberInfiniteTransition(label = "button_pulse")
    val buttonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Dashboard", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("history") }) {
                        Badge {
                            Icon(Icons.Default.History, contentDescription = "Storico")
                        }
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    // Status chips
                    AnimatedVisibility(visible = selectedClient != null || currentProbe != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedClient != null) {
                                StatusChip(
                                    icon = Icons.Default.Business,
                                    label = selectedClient!!.companyName,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (currentProbe != null) {
                                StatusChip(
                                    icon = if (isProbeOnline) Icons.Default.CheckCircle else Icons.Default.Error,
                                    label = currentProbe!!.name,
                                    color = if (isProbeOnline) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                    }

                    // Warning chip per sonda offline
                    AnimatedVisibility(visible = showProbeOfflineWarning) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Sonda offline: il test potrebbe fallire",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val encodedSocket = Uri.encode(socketName)
                            navController.navigate(
                                "test_execution/${selectedClient!!.clientId}/${currentProbe!!.probeId}/${selectedProfile!!.profileId}/$encodedSocket"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .alpha(if (isTestButtonEnabled) buttonAlpha else 1f),
                        enabled = isTestButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTestButtonEnabled)
                                Color(0xFF4CAF50)
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isTestButtonEnabled)
                                Color.White
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isTestButtonEnabled) "AVVIA TEST" else "CONFIGURA TEST",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Cable,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Certificazione MikLink",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configura e avvia un nuovo test",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Client selection
            SelectionCard(
                title = "1. Seleziona Cliente",
                icon = Icons.Default.Business,
                items = clients,
                selectedItem = selectedClient,
                onItemSelected = { viewModel.selectedClient.value = it },
                itemToString = { it.companyName },
                onManageClick = { navController.navigate("client_list") },
                emptyMessage = "Nessun cliente configurato"
            )

            // Profile selection (numerazione aggiornata: 3→2)
            SelectionCard(
                title = "2. Seleziona Profilo Test",
                icon = Icons.Default.Checklist,
                items = profiles,
                selectedItem = selectedProfile,
                onItemSelected = { viewModel.selectedProfile.value = it },
                itemToString = { it.profileName },
                onManageClick = { navController.navigate("profile_list") },
                emptyMessage = "Nessun profilo configurato"
            )

            // Socket ID input
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PowerInput,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "3. Inserisci ID Presa",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = socketName,
                        onValueChange = { viewModel.socketName.value = it },
                        label = { Text("ID Presa (es. Ufficio 1, Sala Riunioni)") },
                        placeholder = { Text("Inserisci identificativo presa...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SelectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemToString: (T) -> String,
    onManageClick: () -> Unit,
    emptyMessage: String,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selectedItem != null)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                TextButton(onClick = onManageClick) {
                    Text("GESTISCI")
                }
            }

            Spacer(Modifier.height(12.dp))

            if (items.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                // Selezione con RadioButton List
                var expanded by remember { mutableStateOf(false) }

                // Campo di selezione (clickable per espandere/collassare)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        leadingIcon?.invoke()
                        if (leadingIcon != null) {
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = selectedItem?.let(itemToString) ?: "Seleziona...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selectedItem != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Chiudi" else "Apri",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Lista espandibile con RadioButton
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items.forEach { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onItemSelected(item)
                                        expanded = false
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (item == selectedItem)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (item == selectedItem)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = item == selectedItem,
                                        onClick = {
                                            onItemSelected(item)
                                            expanded = false
                                        }
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = itemToString(item),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (item == selectedItem)
                                            FontWeight.Bold
                                        else
                                            FontWeight.Normal
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
