package com.app.miklink.ui.client

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.ui.components.MinimalListItem
import com.app.miklink.ui.components.ModernSearchBar
import com.app.miklink.R
import kotlinx.coroutines.launch

// Removed legacy WebView-based printing imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    navController: NavController,
    viewModel: ClientListViewModel = hiltViewModel()
) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
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
                                "${filteredClients.size} ${if (filteredClients.size == 1) "cliente" else "clienti"}",
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
                placeholder = "Cerca cliente..."
            )

            if (clients.isEmpty()) {
                // Empty state (No clients at all)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.BusinessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "Nessun Cliente",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Aggiungi il tuo primo cliente per iniziare.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        text = "Nessun risultato trovato per \"$searchQuery\"",
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
                                subtitle = client.location ?: "Nessuna sede specificata",
                                icon = Icons.Default.Business,
                                onClick = { navController.navigate("client_edit/${client.clientId}") },
                                trailingContent = {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            try {
                                                snackbarHostState.showSnackbar("Generazione PDF...")
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
                                                        snackbarHostState.showSnackbar("PDF generato con successo")
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
                                                    snackbarHostState.showSnackbar("Errore generazione PDF")
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("ClientPDF", "Export error", e)
                                                snackbarHostState.showSnackbar("Errore export: ${e.message}")
                                            }
                                        }
                                    }) { Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.history_export_pdf)) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
