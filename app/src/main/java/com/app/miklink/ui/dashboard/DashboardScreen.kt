/*
 * UI dashboard screen, input setup/probe state, output expressive setup flow and sticky CTA rendering.
 */
package com.app.miklink.ui.dashboard

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.R
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.ui.components.AppTopBar
import com.app.miklink.ui.components.MinimalListItem
import com.app.miklink.ui.components.PrimaryStickyCTA
import com.app.miklink.ui.components.SetupWizardCard
import com.app.miklink.ui.profile.TestBadge
import com.app.miklink.ui.theme.MikLinkThemeTokens
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val glowIntensity by viewModel.dashboardGlowIntensity.collectAsStateWithLifecycle()

    val isTestButtonEnabled = selectedClient != null && currentProbe != null &&
        selectedProfile != null && socketName.isNotBlank()

    val semantic = MikLinkThemeTokens.semantic
    val normalizedGlow = glowIntensity.coerceIn(0f, 1f)
    val baseGlowColor = when {
        currentProbe == null -> MaterialTheme.colorScheme.primary
        isProbeOnline -> semantic.success
        else -> semantic.failure
    }
    val glowAlpha = when {
        currentProbe == null -> 0.05f + 0.12f * normalizedGlow
        isProbeOnline -> 0.12f + 0.28f * normalizedGlow
        else -> 0.12f + 0.32f * normalizedGlow
    }
    val glowColor by animateColorAsState(
        targetValue = baseGlowColor.copy(alpha = glowAlpha.coerceIn(0f, 1f)),
        label = "dashboard_glow"
    )
    val glowTransition = rememberInfiniteTransition(label = "dashboard_glow_drift")
    val glowDriftX by glowTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dashboard_glow_drift_x"
    )
    val glowDriftY by glowTransition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dashboard_glow_drift_y"
    )
    val glowHeight = 360.dp

    var showClientSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(glowHeight)
                .align(Alignment.TopCenter)
        ) {
            val width = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val height = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val center = Offset(
                x = width * 0.5f + width * 0.06f * glowDriftX,
                y = height * 0.2f + height * 0.05f * glowDriftY
            )
            val haloColor = glowColor.copy(alpha = (glowColor.alpha * 0.55f).coerceIn(0f, 1f))
            val glowRadius = maxOf(width, height) * (0.75f + 0.15f * normalizedGlow)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = center,
                            radius = glowRadius
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(haloColor, Color.Transparent),
                            center = center.copy(y = center.y + height * 0.08f),
                            radius = glowRadius * 1.1f
                        )
                    )
            )
        }

        androidx.compose.material3.Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                AppTopBar(
                    title = stringResource(id = R.string.dashboard_title),
                    subtitle = if (isTestButtonEnabled) {
                        stringResource(id = R.string.dashboard_subtitle_ready)
                    } else {
                        stringResource(id = R.string.dashboard_subtitle_setup)
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = stringResource(id = R.string.app_name),
                            modifier = Modifier.size(28.dp),
                            tint = Color.Unspecified
                        )
                    },
                    containerColor = Color.Transparent,
                    onReport = { navController.navigate("history") },
                    onSettings = { navController.navigate("settings") },
                    reportBadge = true
                )
            },
            bottomBar = {
                PrimaryStickyCTA(
                    label = if (isTestButtonEnabled) {
                        stringResource(id = R.string.dashboard_btn_start_test)
                    } else {
                        stringResource(id = R.string.dashboard_btn_configure_test)
                    },
                    supportingText = if (isTestButtonEnabled) null else stringResource(id = R.string.dashboard_cta_hint),
                    icon = Icons.Default.PlayArrow,
                    enabled = isTestButtonEnabled,
                    onClick = {
                        selectedClient?.let { client ->
                            currentProbe?.let {
                                selectedProfile?.let { profile ->
                                    val encodedSocket = Uri.encode(socketName)
                                    navController.navigate(
                                        "test_execution/${client.clientId}/${profile.profileId}/$encodedSocket"
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SetupWizardCard(
                        clientName = selectedClient?.companyName,
                        clientSubtitle = selectedClient?.location,
                        profileName = selectedProfile?.profileName,
                        profileSubtitle = selectedProfile?.profileDescription,
                        socketValue = socketName,
                        socketPlaceholder = stringResource(id = R.string.dashboard_socket_placeholder),
                        onSelectClient = { showClientSheet = true },
                        onSelectProfile = { showProfileSheet = true },
                        onSocketChange = { viewModel.socketName.value = it },
                        onManageClient = { navController.navigate("client_list") },
                        onManageProfile = { navController.navigate("profile_list") }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showClientSheet) {
        ModalBottomSheet(
            onDismissRequest = { showClientSheet = false },
            sheetState = sheetState,
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            var searchQuery by remember { mutableStateOf("") }
            val filteredClients = remember(clients, searchQuery) {
                if (searchQuery.isBlank()) clients
                else clients.filter {
                    it.companyName.contains(searchQuery, ignoreCase = true) ||
                        (it.location?.contains(searchQuery, ignoreCase = true) == true)
                }
            }
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.dashboard_select_client),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(id = R.string.dashboard_search_client)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
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
                                viewModel.onClientSelected(client)
                                showClientSheet = false
                            }
                        )
                    }
                    if (filteredClients.isEmpty()) {
                        item {
                            Text(
                                stringResource(id = R.string.dashboard_no_clients_found),
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProfileSheet) {
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = sheetState,
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.dashboard_select_profile),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
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
                            },
                            trailingContent = { ProfileTestsRow(profile = profile) }
                        )
                    }
                    if (profiles.isEmpty()) {
                        item {
                            Text(
                                stringResource(id = R.string.dashboard_no_profiles),
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileTestsRow(profile: TestProfile, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (profile.runTdr) {
            TestBadge(label = "TDR", color = Color(0xFF2F6F4E))
        }
        if (profile.runLinkStatus) {
            TestBadge(label = "LINK", color = MaterialTheme.colorScheme.primary)
        }
        if (profile.runLldp) {
            TestBadge(label = "LLDP", color = Color(0xFFFFA726))
        }
        if (profile.runPing) {
            TestBadge(label = "PING", color = MaterialTheme.colorScheme.primary)
        }
        if (profile.runSpeedTest) {
            TestBadge(label = "SPEED", color = MaterialTheme.colorScheme.secondary)
        }
    }
}
