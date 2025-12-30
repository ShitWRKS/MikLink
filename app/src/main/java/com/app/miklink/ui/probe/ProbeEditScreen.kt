/*
 * Purpose: Compose screen to edit probe configuration (IP/auth, HTTPS, test interface) with verification flow.
 * Inputs: ProbeEditViewModel state (address, credentials, https toggle, verification results), NavController for navigation.
 * Outputs: Saves probe via view model, renders interface dropdown after verification, provides user actions for verify/save.
 * Notes: Uses Material3 exposed dropdown; deprecated MenuAnchorType replaced with ExposedDropdownMenuAnchorType to avoid warnings.
 */
package com.app.miklink.ui.probe

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.app.miklink.R
import androidx.compose.material3.ExposedDropdownMenuAnchorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProbeEditScreen(
    navController: NavController,
    viewModel: ProbeEditViewModel = hiltViewModel()
) {
    // probe name removed from model — not tracked in the UI
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

    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) stringResource(id = R.string.title_edit_probe) else stringResource(id = R.string.title_add_probe)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = com.app.miklink.R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::onSaveClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                enabled = ipAddress.isNotBlank() && verificationState is VerificationState.Success
            ) {
                Text(stringResource(R.string.save))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // NAME input removed by design: probe should be generically named "Sonda"
            OutlinedTextField(value = ipAddress, onValueChange = { viewModel.ipAddress.value = it }, label = { Text(stringResource(R.string.probe_edit_ip_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = username, onValueChange = { viewModel.username.value = it }, label = { Text(stringResource(R.string.client_edit_username_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.password.value = it },
                label = { Text(stringResource(R.string.client_edit_password_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff
                    val descriptionRes = if (passwordVisible) R.string.hide_password else R.string.show_password

                    IconButton(onClick = {passwordVisible = !passwordVisible}){
                        Icon(imageVector  = image, stringResource(descriptionRes))
                    }
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.probe_edit_https_title)) },
                supportingContent = { Text(stringResource(R.string.probe_edit_https_description)) },
                trailingContent = { Switch(checked = isHttps, onCheckedChange = { viewModel.isHttps.value = it }) }
            )

            Spacer(Modifier.height(8.dp))

            when (val state = verificationState) {
                is VerificationState.Idle -> {
                    VerifyProbeButton(onClick = viewModel::onVerifyClicked)
                }
                is VerificationState.Loading -> {
                    CircularProgressIndicator()
                }
                is VerificationState.Success -> {
                    val boardName = state.boardName ?: stringResource(R.string.probe_edit_board_unknown)
                    Text(stringResource(R.string.probe_edit_board_label, boardName), style = MaterialTheme.typography.bodyLarge)
                    state.warning?.let { warning ->
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = testInterface,
                            onValueChange = { },
                            label = { Text(stringResource(R.string.probe_edit_test_interface_label)) },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            state.interfaces.forEach { iface ->
                                DropdownMenuItem(
                                    text = { Text(iface) },
                                    onClick = {
                                        viewModel.testInterface.value = iface
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                is VerificationState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    VerifyProbeButton(onClick = viewModel::onVerifyClicked)
                    OutlinedTextField(value = testInterface, onValueChange = { viewModel.testInterface.value = it }, label = { Text(stringResource(R.string.probe_edit_test_interface_manual)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
        }
    }
}

@Composable
private fun VerifyProbeButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(stringResource(R.string.probe_edit_verify_button))
    }
}
