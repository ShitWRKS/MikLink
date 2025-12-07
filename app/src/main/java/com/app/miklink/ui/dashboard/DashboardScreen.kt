package com.app.miklink.ui.dashboard

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
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
import com.app.miklink.ui.components.MinimalListItem
import com.app.miklink.ui.components.ModernSearchBar
import com.app.miklink.ui.components.StatusBadge
import com.app.miklink.ui.navigateDashboard
import com.app.miklink.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val currentProbe by viewModel.currentProbe.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()

    val selectedClient by viewModel.selectedClient.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val socketName by viewModel.socketName.collectAsStateWithLifecycle()
    val isProbeOnline by viewModel.isProbeOnline.collectAsStateWithLifecycle()

    val isTestButtonEnabled = selectedClient != null && currentProbe != null &&
                            selectedProfile != null && socketName.isNotBlank()

    val showProbeOfflineWarning = currentProbe != null && !isProbeOnline

    // Sheet State
    var showClientSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Pulse animation for the button
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { navController.navigateDashboard() }
                    ) {
                        // Logo dell'app invece dell'icona Dashboard
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.app.miklink.R.drawable.logo),
                            contentDescription = "MikLink Logo",
                            modifier = Modifier.size(32.dp),
                            tint = Color.Unspecified // Usa i colori originali del logo se possibile, o rimuovi tint se è un drawable colorato
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("MikLink", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    // Tasto Report più evidente
                    FilledTonalButton(
                        onClick = { navController.navigate("history") },
                        modifier = Modifier.padding(end = 8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Description, 
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("REPORT", fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
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
                    // Status Badges
                    AnimatedVisibility(visible = selectedClient != null || currentProbe != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedClient?.let { client ->
                                StatusBadge(
                                    text = client.companyName,
                                    color = MaterialTheme.colorScheme.primary,
                                    icon = Icons.Default.Business
                                )
                            }
                            currentProbe?.let { probe ->
                                // Show a generic probe label instead of a specific name
                                StatusBadge(
                                    text = "Sonda",
                                    color = if (isProbeOnline) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    icon = if (isProbeOnline) Icons.Default.CheckCircle else Icons.Default.Error
                                )
                            }
                        }
                    }

                    // Warning for offline probe
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
                            selectedClient?.let { client ->
                                currentProbe?.let { probe ->
                                    selectedProfile?.let { profile ->
                                        val encodedSocket = Uri.encode(socketName)
                                        navController.navigate(
                                            "test_execution/${client.clientId}/${probe.probeId}/${profile.profileId}/$encodedSocket"
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .alpha(if (isTestButtonEnabled) buttonAlpha else 1f),
                        enabled = isTestButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTestButtonEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isTestButtonEnabled)
                                MaterialTheme.colorScheme.onPrimary
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Client Selection
            SectionHeader(title = "Cliente", icon = Icons.Default.Business)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectionCard(
                    title = selectedClient?.companyName ?: "Seleziona Cliente",
                    subtitle = selectedClient?.location ?: "Clicca per selezionare",
                    icon = Icons.Default.Business,
                    isSelected = selectedClient != null,
                    onClick = { showClientSheet = true },
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = { navController.navigate("client_list") }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Gestisci Clienti",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. Profile Selection
            SectionHeader(title = "Profilo", icon = Icons.Default.Speed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectionCard(
                    title = selectedProfile?.profileName ?: "Seleziona Profilo",
                    subtitle = selectedProfile?.profileDescription ?: "Clicca per selezionare",
                    icon = Icons.Default.Speed,
                    isSelected = selectedProfile != null,
                    onClick = { showProfileSheet = true },
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = { navController.navigate("profile_list") }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Gestisci Profili",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 3. Socket ID
            SectionHeader(title = "Socket ID", icon = Icons.Default.PowerInput)
            
            OutlinedTextField(
                value = socketName,
                onValueChange = { viewModel.socketName.value = it },
                label = { Text("ID Presa") },
                placeholder = { Text("Es. Ufficio 1, Sala Riunioni") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null)
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            Spacer(Modifier.height(32.dp))
        }
    }

    // Client Bottom Sheet
    if (showClientSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClientSheet = false },
            sheetState = sheetState
        ) {
            var searchQuery by remember { mutableStateOf("") }
            val filteredClients = remember(clients, searchQuery) {
                if (searchQuery.isBlank()) clients
                else clients.filter { 
                    it.companyName.contains(searchQuery, ignoreCase = true) || 
                    (it.location?.contains(searchQuery, ignoreCase = true) == true)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Seleziona Cliente",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                ModernSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Cerca cliente...",
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredClients) { client ->
                        MinimalListItem(
                            title = client.companyName,
                            subtitle = client.location ?: "",
                            icon = Icons.Default.Business,
                            isSelected = selectedClient == client,
                            onClick = {
                                viewModel.selectedClient.value = client
                                showClientSheet = false
                            }
                        )
                    }
                    if (filteredClients.isEmpty()) {
                        item {
                            Text(
                                "Nessun cliente trovato",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Profile Bottom Sheet
    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Seleziona Profilo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(profiles) { profile ->
                        MinimalListItem(
                            title = profile.profileName,
                            subtitle = profile.profileDescription ?: "",
                            icon = Icons.Default.Speed,
                            isSelected = selectedProfile == profile,
                            onClick = {
                                viewModel.selectedProfile.value = profile
                                showProfileSheet = false
                            }
                        )
                    }
                    if (profiles.isEmpty()) {
                        item {
                            Text(
                                "Nessun profilo configurato",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onManageClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        if (onManageClick != null) {
            TextButton(onClick = onManageClick) {
                Text("GESTISCI")
            }
        }
    }
}

@Composable
private fun SelectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelected) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
