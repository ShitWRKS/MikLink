package com.app.miklink.ui.client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.ui.components.MinimalListItem
import com.app.miklink.ui.components.ModernSearchBar
import com.app.miklink.ui.components.ListEmptyState
import androidx.compose.ui.res.pluralStringResource
import com.app.miklink.R
import com.app.miklink.core.domain.model.Client
import com.app.miklink.ui.common.asString
import com.app.miklink.ui.testing.AgentUiTags
import com.app.miklink.ui.testing.AgentSemanticsConfig
import kotlinx.coroutines.launch

// Removed legacy WebView-based printing imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    navController: NavController,
    viewModel: ClientListViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val resolvedErrorMessage = errorMessage?.asString()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var clientPendingDeletion by remember { mutableStateOf<Client?>(null) }
    LaunchedEffect(resolvedErrorMessage) {
        resolvedErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }
    
    // Search State
    var searchQuery by remember { mutableStateOf("") }
    val filteredClients = remember(clients, searchQuery) {
        if (searchQuery.isBlank()) clients
        else clients.filter { 
            it.companyName.contains(searchQuery, ignoreCase = true) || 
            (it.location?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    // PDF generation is now handled by PdfGenerator (iText). The old HTML/WebView pipeline was removed

    Scaffold(
        modifier = Modifier.testTag(AgentUiTags.Client.LIST),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(id = com.app.miklink.R.string.client_list_title), fontWeight = FontWeight.Bold)
                            Text(
                                pluralStringResource(
                                    R.plurals.client_list_count,
                                    filteredClients.size,
                                    filteredClients.size
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = com.app.miklink.R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("client_add") },
                modifier = Modifier.testTag(AgentUiTags.Client.ADD),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.client_list_new_client)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            ModernSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                inputModifier = Modifier.testTag(AgentUiTags.Client.SEARCH),
                placeholder = stringResource(R.string.dashboard_search_client)
            )

            if (clients.isEmpty()) {
                // Empty state (No clients at all)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ListEmptyState(
                        icon = Icons.Default.BusinessCenter,
                        title = stringResource(R.string.client_list_empty_title),
                        body = stringResource(R.string.client_list_empty_body)
                    )
                }
            } else if (filteredClients.isEmpty()) {
                // Empty search results
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.client_list_empty_search, searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Client List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredClients, key = { it.clientId }) { client ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            MinimalListItem(
                                title = client.companyName,
                                subtitle = client.location ?: stringResource(R.string.client_list_no_location),
                                icon = Icons.Default.Business,
                                clickableModifier = Modifier.testTag("${AgentUiTags.Client.ITEM_PREFIX}_${client.clientId}"),
                                onClick = { navController.navigate("client_edit/${client.clientId}") },
                                trailingContent = {
                                    Row {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            try {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.history_exporting_client)
                                                )
                                                val pdfFile = viewModel.generatePdfWithIText(client.clientId)
                                                if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        pdfFile
                                                    )
                                                    // Open viewer or share
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "application/pdf")
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    try {
                                                        context.startActivity(intent)
                                                        snackbarHostState.showSnackbar(
                                                            context.getString(R.string.history_export_success)
                                                        )
                                                    } catch (e: android.content.ActivityNotFoundException) {
                                                        // No viewer, fallback to share
                                                        val share = android.content.Intent.createChooser(
                                                            android.content.Intent().apply {
                                                                action = android.content.Intent.ACTION_SEND
                                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                                type = "application/pdf"
                                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }, null
                                                        )
                                                        context.startActivity(share)
                                                    }
                                                } else {
                                                    snackbarHostState.showSnackbar(
                                                        context.getString(R.string.history_pdf_generation_error)
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                if (com.app.miklink.BuildConfig.DEBUG) android.util.Log.e("ClientPDF", "Export error", e)
                                                snackbarHostState.showSnackbar(
                                                    context.getString(
                                                        R.string.history_error_prefix,
                                                        e.message ?: context.getString(R.string.error_unknown)
                                                    )
                                                )
                                            }
                                        }
                                    }) { Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.history_export_pdf)) }
                                    IconButton(
                                        onClick = { clientPendingDeletion = client },
                                        modifier = Modifier.testTag("${AgentUiTags.Client.DELETE_PREFIX}_${client.clientId}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                                    }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    clientPendingDeletion?.let { client ->
        AlertDialog(
            modifier = AgentSemanticsConfig.rootModifier(),
            onDismissRequest = { clientPendingDeletion = null },
            title = { Text(stringResource(R.string.client_delete_title)) },
            text = { Text(stringResource(R.string.client_delete_message, client.companyName)) },
            confirmButton = {
                Button(
                    onClick = {
                        clientPendingDeletion = null
                        viewModel.deleteClient(client)
                    },
                    modifier = Modifier.testTag(AgentUiTags.Client.DELETE_CONFIRM)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { clientPendingDeletion = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
