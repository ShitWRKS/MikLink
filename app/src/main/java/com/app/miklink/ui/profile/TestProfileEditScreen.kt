package com.app.miklink.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestProfileEditScreen(
    navController: NavController,
    viewModel: TestProfileViewModel = hiltViewModel()
) {
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    if (isSaved) {
        LaunchedEffect(Unit) { navController.popBackStack() }
    }

    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val profileDescription by viewModel.profileDescription.collectAsStateWithLifecycle()
    val runTdr by viewModel.runTdr.collectAsStateWithLifecycle()
    val runLinkStatus by viewModel.runLinkStatus.collectAsStateWithLifecycle()
    val runLldp by viewModel.runLldp.collectAsStateWithLifecycle()
    val runPing by viewModel.runPing.collectAsStateWithLifecycle()
    val pingTarget1 by viewModel.pingTarget1.collectAsStateWithLifecycle()
    val pingTarget2 by viewModel.pingTarget2.collectAsStateWithLifecycle()
    val pingTarget3 by viewModel.pingTarget3.collectAsStateWithLifecycle()
    val pingCount by viewModel.pingCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit Profile" else "Add Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::saveProfile,
                modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                enabled = profileName.isNotBlank()
            ) {
                Text("Save Profile")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(value = profileName, onValueChange = { viewModel.profileName.value = it }, label = { Text("Profile Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = profileDescription, onValueChange = { viewModel.profileDescription.value = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }

            item { HorizontalDivider() }

            item {
                SwitchListItem(checked = runTdr, onCheckedChange = { viewModel.runTdr.value = it }, headlineText = "Run TDR (Cable-Test)", supportingText = "Requires compatible probe")
                SwitchListItem(checked = runLinkStatus, onCheckedChange = { viewModel.runLinkStatus.value = it }, headlineText = "Run Link Status Test")
                SwitchListItem(checked = runLldp, onCheckedChange = { viewModel.runLldp.value = it }, headlineText = "Run LLDP/CDP Neighbor Test")
                SwitchListItem(checked = runPing, onCheckedChange = { viewModel.runPing.value = it }, headlineText = "Run Ping Test")
            }

            if (runPing) {
                item {
                    Column {
                        Text("Ping Targets", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = pingTarget1, onValueChange = { viewModel.pingTarget1.value = it }, label = { Text("Ping Target 1") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = pingTarget2, onValueChange = { viewModel.pingTarget2.value = it }, label = { Text("Ping Target 2") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = pingTarget3, onValueChange = { viewModel.pingTarget3.value = it }, label = { Text("Ping Target 3") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pingCount,
                            onValueChange = { viewModel.pingCount.value = it },
                            label = { Text("Ping Count (1-20)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text("Numero di ping per ogni target (default: 4)") },
                            isError = pingCount.toIntOrNull()?.let { it < 1 || it > 20 } ?: (pingCount.isNotBlank())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchListItem(checked: Boolean, onCheckedChange: (Boolean) -> Unit, headlineText: String, supportingText: String? = null) {
    ListItem(
        headlineContent = { Text(headlineText) },
        supportingContent = { supportingText?.let { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth()
    )
}