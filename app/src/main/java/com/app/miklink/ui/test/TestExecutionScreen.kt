package com.app.miklink.ui.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.app.miklink.data.db.model.Report
import com.app.miklink.utils.UiState

@Composable
fun TestExecutionScreen(
    navController: NavController,
    viewModel: TestViewModel = hiltViewModel()
) {
    val log by viewModel.log.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(log.size) {
        if (log.isNotEmpty()) {
            listState.animateScrollToItem(log.size - 1)
        }
    }

    Scaffold(
        bottomBar = {
            if (uiState is UiState.Success) {
                val report = (uiState as UiState.Success<Report>).data
                val isFailed = report.overallStatus != "PASS"
                BottomAppBar(modifier = Modifier.navigationBarsPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) { Text("DISMISS") }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = viewModel::startTest, modifier = Modifier.weight(1f)) { Text("TEST AGAIN") }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                viewModel.saveReportToDb(report)
                                navController.popBackStack()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isFailed) "FIX LATER" else "SAVE")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Test in corso...", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                        items(log) { message ->
                            Text(message, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            is UiState.Success -> {
                val report = state.data
                val resultColor = if (report.overallStatus == "PASS") Color(0xFF4CAF50) else Color(0xFFF44336)

                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Card(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = resultColor.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (report.overallStatus == "PASS") "TEST PASSATO" else "TEST FALLITO",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = resultColor
                            )
                            Spacer(Modifier.height(16.dp))
                            Divider()
                            LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                                items(log) { message ->
                                    Text(message)
                                }
                            }
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
