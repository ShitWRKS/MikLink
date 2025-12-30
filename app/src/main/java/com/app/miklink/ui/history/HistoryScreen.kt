/*
 * Purpose: Render test history list with filters, grouping by client, export/repeat actions, and inline PDF operations.
 * Inputs: HistoryViewModel state (reports, filters, pdf status), NavController, and user interactions for repeat/delete/export.
 * Outputs: Composable history UI with semantic status chips/badges and navigation/actions callbacks.
 */
package com.app.miklink.ui.history

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.ui.history.model.ReportsByClient
import com.app.miklink.ui.theme.MikLinkThemeTokens
import com.app.miklink.ui.components.ResultStatusLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable

import androidx.compose.ui.res.stringResource
import com.app.miklink.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val reportsByClient by viewModel.reportsByClient.collectAsStateWithLifecycle()
    val pdfStatus by viewModel.pdfStatus.collectAsStateWithLifecycle()
    val semantic = MikLinkThemeTokens.semantic

    var expandedClientId by remember { mutableStateOf<Long?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showExportDialog by remember { mutableStateOf<ReportsByClient?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Search and filter state
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    
    // Repeat test confirmation
    var showRepeatDialog by remember { mutableStateOf(false) }
    var pendingRepeatReport by remember { mutableStateOf<TestReport?>(null) }
    
    // Single Report Export Dialog Context
    var singleExportContext by remember { mutableStateOf<Pair<TestReport, String>?>(null) } // Report, ClientName

    val pdfReportTitle by viewModel.pdfReportTitle.collectAsStateWithLifecycle()
    val pdfHideEmptyColumns by viewModel.pdfHideEmptyColumns.collectAsStateWithLifecycle()

    val exportingClientMessage = stringResource(R.string.history_exporting_client)
    val exportSuccessMessage = stringResource(R.string.history_export_success)
    val noPdfAppMessage = stringResource(R.string.history_no_pdf_app)
    val noFileGeneratedMessage = stringResource(R.string.history_no_file_generated)
    val exportingSingleMessage = stringResource(R.string.history_exporting_single)
    val pdfGeneratedMessage = stringResource(R.string.history_pdf_generated)
    val noPdfViewerMessage = stringResource(R.string.history_no_pdf_viewer)
    val pdfGenerationErrorMessage = stringResource(R.string.history_pdf_generation_error)
    val repeatErrorMessage = stringResource(R.string.history_repeat_error)
    val errorPrefixTemplate = stringResource(R.string.history_error_prefix, "%s")

    LaunchedEffect(pdfStatus) {
        if (pdfStatus.isNotBlank()) {
            snackbarHostState.showSnackbar(pdfStatus)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.history_title)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                
                // Search bar
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { 
                        searchText = it
                        viewModel.updateSearchQuery(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.common_search))
                    },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchText = ""
                                viewModel.updateSearchQuery("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_clear))
                            }
                        }
                    },
                    singleLine = true
                )
                
                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterStatus == FilterStatus.ALL,
                        onClick = { viewModel.updateFilterStatus(FilterStatus.ALL) },
                        label = { Text(stringResource(R.string.history_filter_all)) },
                        leadingIcon = if (filterStatus == FilterStatus.ALL) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = filterStatus == FilterStatus.PASS,
                        onClick = { viewModel.updateFilterStatus(FilterStatus.PASS) },
                        label = { Text(stringResource(R.string.history_filter_pass)) },
                        leadingIcon = if (filterStatus == FilterStatus.PASS) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    FilterChip(
                        selected = filterStatus == FilterStatus.FAIL,
                        onClick = { viewModel.updateFilterStatus(FilterStatus.FAIL) },
                        label = { Text(stringResource(R.string.history_filter_fail)) },
                        leadingIcon = if (filterStatus == FilterStatus.FAIL) {
                            { Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        if (reportsByClient.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.history_no_reports),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.history_start_test_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.history_tip_icon), style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.history_tips_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(16.dp))
                            Row { Text(stringResource(R.string.history_tip_bullet), style = MaterialTheme.typography.bodyMedium); Text(stringResource(R.string.history_tip_1), style = MaterialTheme.typography.bodyMedium) }
                            Spacer(Modifier.height(8.dp))
                            Row { Text(stringResource(R.string.history_tip_bullet), style = MaterialTheme.typography.bodyMedium); Text(stringResource(R.string.history_tip_2), style = MaterialTheme.typography.bodyMedium) }
                            Spacer(Modifier.height(8.dp))
                            Row { Text(stringResource(R.string.history_tip_bullet), style = MaterialTheme.typography.bodyMedium); Text(stringResource(R.string.history_tip_3), style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate("dashboard") },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.history_btn_dashboard))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reportsByClient, key = { it.client?.clientId ?: -1 }) { clientData ->
                    ClientReportsCard(
                        clientData = clientData,
                        isExpanded = expandedClientId == clientData.client?.clientId,
                        onToggleExpand = {
                            expandedClientId = if (expandedClientId == clientData.client?.clientId) null
                            else clientData.client?.clientId
                        },
                        onReportEdit = { reportId ->
                            navController.navigate("report_detail/$reportId")
                        },
                        onReportDelete = { reportId ->
                            showDeleteDialog = reportId
                        },
                        onReportRepeat = { report ->
                            pendingRepeatReport = report
                            showRepeatDialog = true
                        },
                        onExportAll = {
                            // Show the configuration dialog instead of immediate export
                            showExportDialog = clientData
                        },
                        onExportSingleReport = { report ->
                            val clientName = clientData.client?.companyName ?: "Report"
                            singleExportContext = Pair(report, clientName)
                        },
                        viewModel = viewModel
                    )
                }
            }
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { reportId ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                title = { Text(stringResource(R.string.history_delete_title)) },
                text = { Text(stringResource(R.string.history_delete_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteReport(reportId)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

    // Export Configuration Dialog (Client Level)
    showExportDialog?.let { clientData ->
            PdfExportDialog(
                clientName = clientData.client?.companyName ?: "Report",
                globalReportTitle = pdfReportTitle,
                globalHideEmptyColumns = pdfHideEmptyColumns,
                onDismiss = { showExportDialog = null },
                onConfirm = { config ->
                    showExportDialog = null
                    coroutineScope.launch {
                        try {
                            snackbarHostState.showSnackbar(exportingClientMessage)
                            
                            val pdfFile = viewModel.generatePdfWithITextForClient(clientData, config)
                            
                            if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
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
                                    snackbarHostState.showSnackbar(exportSuccessMessage)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    snackbarHostState.showSnackbar(noPdfAppMessage)
                                }
                            } else {
                                snackbarHostState.showSnackbar(noFileGeneratedMessage)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("HistoryPDF", "Export Error", e)
                            snackbarHostState.showSnackbar(
                                String.format(Locale.getDefault(), errorPrefixTemplate, e.message ?: "")
                            )
                        }
                    }
                }
            )
        }

    // Export Configuration Dialog (Single Report)
    singleExportContext?.let { (report, clientName) ->
            PdfExportDialog(
                clientName = clientName,
                onDismiss = { singleExportContext = null },
                onConfirm = { config ->
                    singleExportContext = null
                    coroutineScope.launch {
                        try {
                            snackbarHostState.showSnackbar(exportingSingleMessage)
                            val pdfFile = viewModel.generatePdfForSingleReport(report, config)
                            
                            if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
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
                                    snackbarHostState.showSnackbar(pdfGeneratedMessage)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    snackbarHostState.showSnackbar(noPdfViewerMessage)
                                }
                            } else {
                                snackbarHostState.showSnackbar(pdfGenerationErrorMessage)
                            }
                        } catch (e: Exception) {
                             android.util.Log.e("HistoryPDF", "Error generating single PDF", e)
                             snackbarHostState.showSnackbar(
                                 String.format(Locale.getDefault(), errorPrefixTemplate, e.message ?: "")
                             )
                        }
                    }
                }
            )
        }
    }
    
    // Repeat test confirmation dialog
    if (showRepeatDialog && pendingRepeatReport != null) {
        AlertDialog(
            onDismissRequest = { 
                showRepeatDialog = false
                pendingRepeatReport = null
            },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    stringResource(R.string.history_repeat_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.history_repeat_message),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.history_repeat_socket, pendingRepeatReport?.socketName ?: "N/A"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        stringResource(R.string.history_repeat_action_q),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.history_repeat_replace_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(R.string.history_repeat_new_desc),
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        stringResource(R.string.history_repeat_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Nuovo Test button (secondary choice, outlined) - LEFT
                    OutlinedButton(
                        onClick = {
                            val report = pendingRepeatReport
                            showRepeatDialog = false
                            pendingRepeatReport = null
                            
                            if (report != null) {
                                coroutineScope.launch {
                                    val route = viewModel.getRepeatTestRoute(report)
                                    if (route != null) {
                                        navController.navigate(route)
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            "Impossibile ripetere il test: profilo o sonda non trovati"
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.history_btn_new_test))
                    }
                    
                    // Sostituisci button (preferred choice, primary color) - RIGHT
                    Button(
                        onClick = {
                            val report = pendingRepeatReport
                            showRepeatDialog = false
                            pendingRepeatReport = null
                            
                            if (report != null) {
                                coroutineScope.launch {
                                    // Delete old report first
                                    viewModel.deleteReport(report.reportId)
                                    
                                    // Then navigate to new test
                                    val route = viewModel.getRepeatTestRoute(report)
                                    if (route != null) {
                                        navController.navigate(route)
                                    } else {
                                        snackbarHostState.showSnackbar(repeatErrorMessage)
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.history_btn_replace))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRepeatDialog = false
                    pendingRepeatReport = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ClientReportsCard(
    clientData: ReportsByClient,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onReportEdit: (Long) -> Unit,
    onReportDelete: (Long) -> Unit,
    onReportRepeat: (TestReport) -> Unit,
    onExportAll: () -> Unit,
    onExportSingleReport: (TestReport) -> Unit = {},
    viewModel: HistoryViewModel
) {
    val semantic = MikLinkThemeTokens.semantic
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clientData.client?.companyName ?: stringResource(R.string.unknown_client),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(stringResource(R.string.history_total_tests, clientData.totalTests))
                        }
                        if (clientData.passedTests > 0) {
                            Badge(
                                containerColor = semantic.successContainer
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = semantic.onSuccessContainer
                                    )
                                    Text(
                                        "${clientData.passedTests}",
                                        color = semantic.onSuccessContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (clientData.failedTests > 0) {
                            Badge(
                                containerColor = semantic.failureContainer
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = semantic.onFailureContainer
                                    )
                                    Text(
                                        "${clientData.failedTests}",
                                        color = semantic.onFailureContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Styled export icon: use same visual treatment as Settings (primary tint, no bg)
                    IconButton(onClick = { onExportAll() }) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = stringResource(R.string.history_export_all),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Expand/collapse button: explicit tint for consistency
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider()
                    clientData.reports.forEach { report ->
                        ReportListItem(
                            report = report,
                            onEdit = { onReportEdit(report.reportId) },
                            onDelete = { onReportDelete(report.reportId) },
                            onRepeat = { onReportRepeat(report) },
                            onExportPdf = { onExportSingleReport(report) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun ReportListItem(
    report: TestReport,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRepeat: () -> Unit,
    onExportPdf: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val semantic = MikLinkThemeTokens.semantic
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.socketName ?: stringResource(R.string.unnamed_socket),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("d MMM, HH:mm", Locale.ITALIAN)
                    .format(Date(report.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(8.dp))

        ResultStatusLabel(status = report.overallStatus)

        // Action buttons: PDF, Repeat, Overflow menu
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExportPdf) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    stringResource(R.string.history_export_pdf),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onRepeat) {
                Icon(
                    Icons.Default.Refresh,
                    stringResource(R.string.history_repeat_test),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            
            // Overflow menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        stringResource(R.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}
