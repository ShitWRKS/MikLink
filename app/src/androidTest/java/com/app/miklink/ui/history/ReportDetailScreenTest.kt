package com.app.miklink.ui.history

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.model.report.PingSample
import com.app.miklink.core.domain.model.report.ReportData

@RunWith(AndroidJUnit4::class)
class ReportDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeProvider : ReportDetailScreenStateProvider {
        private val _report = MutableStateFlow(
            TestReport(
                reportId = 1L,
                clientId = null,
                timestamp = 0L,
                socketName = null,
                notes = null,
                probeName = "Sonda",
                profileName = "Profile",
                overallStatus = "PASS",
                resultFormatVersion = 1,
                resultsJson = "{}"
            )
        )
        private val _results = MutableStateFlow(
            ReportData(
                pingSamples = listOf(
                    PingSample(
                        target = "8.8.8.8",
                        avgRtt = "10ms",
                        minRtt = "8ms",
                        maxRtt = "15ms",
                        packetLoss = "0",
                        seq = "4",
                        time = "10ms",
                        ttl = "64"
                    )
                )
            )
        )
        private val _pdfStatus = MutableStateFlow("")
        override val report: StateFlow<TestReport?> = _report
        override val parsedResults: StateFlow<ReportData?> = _results
        override val pdfStatus: StateFlow<String> = _pdfStatus
        override val socketName = MutableStateFlow("")
        override val notes = MutableStateFlow("")
        override fun updateReportDetails() {}
        override fun exportReportToPdf() { _pdfStatus.value = "EXPORT" }
    }

    @Test
    fun ping_details_hidden_then_visible_after_click() {
        val provider = FakeProvider()
        composeRule.setContent {
            val navController = rememberNavController()
            ReportDetailScreen(navController = navController, stateProvider = provider)
        }
        // Step 1: Dettagli nascosti di default
        composeRule.onNodeWithTag("PingDetailsView").assertDoesNotExist()
        // Step 2: Click sulla card Ping
        composeRule.onNodeWithTag("PingResultCard").performClick()
        // Attendere fine animazioni/ricomposizioni per evitare flakiness
        composeRule.waitForIdle()
        // Step 3: Dettagli visibili
        composeRule.onNodeWithTag("PingDetailsView").assertIsDisplayed()
        // Step 4: Verifica dati fittizi
        composeRule.onNodeWithText("Avg: 10ms").assertIsDisplayed()
    }
}
