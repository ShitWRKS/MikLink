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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
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
        selectedProfile != null && socketName.isNotBlank() && isProbeOnline

    // Smart CTA Logic
    val (ctaLabel, ctaIcon, ctaAction, ctaEnabled, ctaSubtitle) = when {
        currentProbe == null -> {
            val label = stringResource(id = R.string.dashboard_btn_probe_missing)
            val subtitle = stringResource(id = R.string.dashboard_hint_probe_missing)
            val action = { navController.navigate("probe_config") }
            TestCtaState(label, Icons.Default.Settings, action, true, subtitle)
        }
        !isProbeOnline -> {
            val label = stringResource(id = R.string.dashboard_btn_probe_offline)
            val subtitle = stringResource(id = R.string.dashboard_hint_probe_offline)
            TestCtaState(label, Icons.Default.WifiOff, {}, false, subtitle)
        }
        !isTestButtonEnabled -> {
            val label = stringResource(id = R.string.dashboard_btn_configure_test)
            val subtitle = stringResource(id = R.string.dashboard_cta_hint)
            TestCtaState(label, Icons.Default.PlayArrow, {}, false, subtitle)
        }
        else -> {
            val label = stringResource(id = R.string.dashboard_btn_start_test)
            val subtitle = stringResource(id = R.string.dashboard_cta_ready)
            val action = {
                selectedClient?.let { client ->
                    selectedProfile?.let { profile ->
                        val encodedSocket = Uri.encode(socketName)
                        navController.navigate(
                            "test_execution/${client.clientId}/${profile.profileId}/$encodedSocket"
                        )
                    }
                } ?: Unit
            }
            TestCtaState(label, Icons.Default.PlayArrow, action, true, subtitle)
        }
    }

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

        Scaffold(
            modifier = Modifier.testTag(DashboardTags.SCREEN),
            containerColor = Color.Transparent,
            topBar = {
                AppTopBar(
                    title = stringResource(id = R.string.dashboard_title),
                    subtitle = if (isTestButtonEnabled) {
                        stringResource(id = R.string.dashboard_subtitle_ready)
                    } else if (currentProbe == null) {
                        stringResource(id = R.string.dashboard_probe_missing)
                    } else null,
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
                    reportTestTag = DashboardTags.HISTORY_BUTTON,
                    settingsTestTag = DashboardTags.SETTINGS_BUTTON,
                    reportBadge = true
                )
            },
            bottomBar = {
                PrimaryStickyCTA(
                    label = ctaLabel,
                    supportingText = if (ctaEnabled) null else ctaSubtitle,
                    icon = ctaIcon,
                    enabled = ctaEnabled,
                    onClick = ctaAction,
                    buttonTestTag = DashboardTags.START_TEST_BUTTON
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
                        clientSelectorTag = DashboardTags.CLIENT_SELECTOR,
                        profileSelectorTag = DashboardTags.PROFILE_SELECTOR,
                        manageClientTag = DashboardTags.MANAGE_CLIENTS,
                        manageProfileTag = DashboardTags.MANAGE_PROFILES,
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

    DashboardSheet(
        show = showClientSheet,
        sheetState = sheetState,
        onDismissRequest = { showClientSheet = false },
        titleResId = R.string.dashboard_select_client
    ) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredClients = remember(clients, searchQuery) {
            if (searchQuery.isBlank()) clients
            else clients.filter {
                it.companyName.contains(searchQuery, ignoreCase = true) ||
                    (it.location?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
        OutlinedTextField(
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
                    modifier = Modifier.testTag("${DashboardTags.CLIENT_ITEM_PREFIX}_${client.clientId}"),
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

    DashboardSheet(
        show = showProfileSheet,
        sheetState = sheetState,
        onDismissRequest = { showProfileSheet = false },
        titleResId = R.string.dashboard_select_profile
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(profiles) { profile ->
                MinimalListItem(
                    title = profile.profileName,
                    subtitle = profile.profileDescription ?: "",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.testTag("${DashboardTags.PROFILE_ITEM_PREFIX}_${profile.profileId}"),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardSheet(
    show: Boolean,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    titleResId: Int,
    content: @Composable () -> Unit
) {
    if (!show) return
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = titleResId),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileTestsRow(profile: TestProfile, modifier: Modifier = Modifier) {
    val neutralBadgeContent = MaterialTheme.colorScheme.onSurfaceVariant
    val neutralBadgeBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (profile.runTdr) {
            TestBadge(
                label = "TDR",
                color = neutralBadgeContent,
                backgroundColor = neutralBadgeBackground
            )
        }
        if (profile.runLinkStatus) {
            TestBadge(label = "LINK", color = MaterialTheme.colorScheme.primary)
        }
        if (profile.runLldp) {
            TestBadge(
                label = "LLDP",
                color = neutralBadgeContent,
                backgroundColor = neutralBadgeBackground
            )
        }
        if (profile.runPing) {
            TestBadge(label = "PING", color = MaterialTheme.colorScheme.primary)
        }
        if (profile.runSpeedTest) {
            TestBadge(label = "SPEED", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

private data class TestCtaState(
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit,
    val enabled: Boolean,
    val subtitle: String?
)
