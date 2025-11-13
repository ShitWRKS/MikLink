package com.app.miklink.ui.probe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProbeListScreen(
    navController: NavController,
    viewModel: ProbeListViewModel = hiltViewModel()
) {
    val probes by viewModel.probes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Manage Probes") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("probe_edit/-1") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Probe")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(probes) { probe ->
                ListItem(
                    headlineContent = { Text(probe.name) },
                    supportingContent = {
                        Text(
                            "${probe.ipAddress} | ${probe.modelName ?: "Unknown Model"} | ${probe.testInterface}"
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = if (probe.isOnline) "Online" else "Offline",
                            tint = if (probe.isOnline) Color.Green else Color.Red
                        )
                    },
                    modifier = Modifier.clickable {
                        navController.navigate("probe_edit/${probe.probeId}")
                    }
                )
            }
        }
    }
}
