package com.app.miklink.ui.probe

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProbeEditScreen(
    navController: NavController,
    viewModel: ProbeEditViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val ipAddress by viewModel.ipAddress.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val isHttps by viewModel.isHttps.collectAsStateWithLifecycle()
    val testInterface by viewModel.testInterface.collectAsStateWithLifecycle()
    val verificationState by viewModel.verificationState.collectAsStateWithLifecycle()
    
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    if (isSaved) {
        LaunchedEffect(Unit) { navController.popBackStack() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (viewModel.isEditing) "Edit Probe" else "Add Probe") }) },
        bottomBar = {
            Button(
                onClick = viewModel::onSaveClicked,
                modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                enabled = name.isNotBlank() && ipAddress.isNotBlank()
            ) {
                Text("Save")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { viewModel.name.value = it }, label = { Text("Probe Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = ipAddress, onValueChange = { viewModel.ipAddress.value = it }, label = { Text("IP Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = username, onValueChange = { viewModel.username.value = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = password, onValueChange = { viewModel.password.value = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            ListItem(
                headlineContent = { Text("Use HTTPS (SSL)") },
                supportingContent = { Text("Ignores certificate errors") },
                trailingContent = { Switch(checked = isHttps, onCheckedChange = { viewModel.isHttps.value = it }) }
            )

            Spacer(Modifier.height(8.dp))

            when (val state = verificationState) {
                is VerificationState.Idle -> {
                    Button(onClick = viewModel::onVerifyClicked) {
                        Text("Verify Probe")
                    }
                }
                is VerificationState.Loading -> {
                    CircularProgressIndicator()
                }
                is VerificationState.Success -> {
                    Text("Board: ${state.boardName ?: "Unknown"}", style = MaterialTheme.typography.bodyLarge)
                    
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = testInterface,
                            onValueChange = { },
                            label = { Text("Test Interface") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            state.interfaces.forEach { iface ->
                                DropdownMenuItem(
                                    text = { Text(iface) },
                                    onClick = { viewModel.testInterface.value = iface; expanded = false }
                                )
                            }
                        }
                    }
                }
                is VerificationState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    OutlinedTextField(value = testInterface, onValueChange = { viewModel.testInterface.value = it }, label = { Text("Test Interface (Manual)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
        }
    }
}