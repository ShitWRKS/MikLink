package com.app.miklink.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Impostazioni") }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Management Section
            Text("GESTIONE DATI", modifier = Modifier.padding(all = 16.dp), style = MaterialTheme.typography.titleMedium)
            SettingsListItem(
                headline = "Clienti",
                leadingIcon = Icons.Default.Business,
                onClick = { navController.navigate("client_list") }
            )
            SettingsListItem(
                headline = "Sonde",
                leadingIcon = Icons.Default.Router,
                onClick = { navController.navigate("probe_list") }
            )
            SettingsListItem(
                headline = "Profili di Test",
                leadingIcon = Icons.Default.Speed,
                onClick = { navController.navigate("profile_list") }
            )

            HorizontalDivider()

            // Appearance Section
            Text("ASPETTO", modifier = Modifier.padding(all = 16.dp), style = MaterialTheme.typography.titleMedium)
            SettingsListItem(
                headline = "Tema",
                leadingIcon = Icons.Default.ColorLens,
                onClick = { /* TODO: Implement Theme Selector */ }
            )
        }
    }
}

@Composable
private fun SettingsListItem(headline: String, leadingIcon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(headline) },
        leadingContent = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}