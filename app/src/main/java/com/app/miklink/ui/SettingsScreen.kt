package com.app.miklink.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            ListItem(
                headlineContent = { Text("Manage Clients") },
                modifier = Modifier.clickable { navController.navigate("client_list") }
            )
            ListItem(
                headlineContent = { Text("Manage Probes") },
                modifier = Modifier.clickable { navController.navigate("probe_list") }
            )
            ListItem(
                headlineContent = { Text("Manage Test Profiles") },
                modifier = Modifier.clickable { navController.navigate("profile_list") }
            )
        }
    }
}
