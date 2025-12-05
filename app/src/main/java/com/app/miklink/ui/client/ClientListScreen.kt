package com.app.miklink.ui.client

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.print.PrintDocumentAdapter
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.launch

import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.alpha

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

    // State for PDF printing
    var pdfHtmlToPrint by remember { mutableStateOf<String?>(null) }
    var pdfJobName by remember { mutableStateOf("") }
    
    // Keep reference to WebView for explicit control
    val webViewRef = remember { mutableStateOf<android.webkit.WebView?>(null) }

    // Load content when pdfHtmlToPrint changes
    LaunchedEffect(pdfHtmlToPrint) {
        webViewRef.value?.let { webView ->
            webView.tag = pdfJobName
            if (pdfHtmlToPrint != null) {
                android.util.Log.d("ClientPDF", "LaunchedEffect loading HTML, length=${pdfHtmlToPrint!!.length}")
                webView.loadDataWithBaseURL("http://localhost/", pdfHtmlToPrint!!, "text/html", "UTF-8", null)
            }
        }
    }

    // Invisible WebView for printing
    // Invisible WebView for printing - Always attached to prevent renderer crash
    AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = true
                // Disable hardware acceleration to prevent crashes on some devices
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        android.util.Log.d("ClientPDF", "onPageFinished url=$url, hasContent=${pdfHtmlToPrint != null}")
                        // Only print if we have content and URL indicates loaded content (not blank)
                        if (pdfHtmlToPrint != null && url?.startsWith("http") == true) {
                            val printManager = ctx.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                            if (printManager != null) {
                                val jobName = view?.tag as? String ?: "Client_Report"
                                val adapter = createPrintDocumentAdapter(jobName)
                                
                                val wrappedAdapter = object : PrintDocumentAdapter() {
                                    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback?, extras: Bundle?) {
                                        adapter.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)
                                    }
                                    override fun onWrite(pages: Array<out PageRange>?, destination: ParcelFileDescriptor?, cancellationSignal: CancellationSignal?, callback: WriteResultCallback?) {
                                        adapter.onWrite(pages, destination, cancellationSignal, callback)
                                    }
                                    override fun onFinish() {
                                        adapter.onFinish()
                                        // Cleanup: Clear content and reset state
                                        pdfHtmlToPrint = null
                                        view?.loadUrl("about:blank")
                                    }
                                }
                                
                                printManager.print(jobName, wrappedAdapter, PrintAttributes.Builder().build())
                            }
                        }
                    }
                }
                
                // Store reference
                webViewRef.value = this
            }
        },
        modifier = Modifier.size(1.dp).alpha(0f)
    )

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
                            Text("Gestione Clienti", fontWeight = FontWeight.Bold)
                            Text(
                                "${filteredClients.size} ${if (filteredClients.size == 1) "cliente" else "clienti"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                text = { Text("NUOVO CLIENTE") },
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
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                try {
                                                    snackbarHostState.showSnackbar("Generazione PDF in corso...")
                                                    
                                                    // Use iText to generate PDF directly
                                                    val pdfFile = viewModel.generatePdfWithIText(client.clientId)
                                                    
                                                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                                                        // Open PDF with default viewer
                                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.fileprovider",
                                                            pdfFile
                                                        )
                                                        
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                            setDataAndType(uri, "application/pdf")
                                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        
                                                        try {
                                                            context.startActivity(intent)
                                                            snackbarHostState.showSnackbar("PDF generato con successo!")
                                                        } catch (e: android.content.ActivityNotFoundException) {
                                                            snackbarHostState.showSnackbar("Nessun visualizzatore PDF trovato")
                                                        }
                                                    } else {
                                                        snackbarHostState.showSnackbar("Nessun dato da esportare")
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("ClientPDF", "Error generating PDF with iText", e)
                                                    snackbarHostState.showSnackbar("Errore generazione PDF: ${e.message}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = "Export PDF",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
