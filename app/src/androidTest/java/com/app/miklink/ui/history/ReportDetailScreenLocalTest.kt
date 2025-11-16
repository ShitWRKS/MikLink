package com.app.miklink.ui.history

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.navigation.compose.rememberNavController
import com.app.miklink.data.db.model.Report
import com.app.miklink.ui.history.model.ParsedResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ReportDetailScreenLocalTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private class FakeProvider : ReportDetailScreenStateProvider {
        private val _report = MutableStateFlow(
            Report(
                reportId = 42L,
                clientId = null,
                timestamp = 0L,
                socketName = null,
                notes = null,
                probeName = "Probe",
                profileName = "Profile",
                overallStatus = "PASS",
                resultsJson = "{}"
            )
        )
        private val _results = MutableStateFlow(
            ParsedResults(
                ping = listOf(
                    com.app.miklink.data.network.PingResult(
                        avgRtt = "10ms", host = "8.8.8.8", maxRtt = "15ms", minRtt = "8ms", packetLoss = "0", received = "4", sent = "4", seq = "4", size = "64", time = "10ms", ttl = "64"
                    )
                )
            )
        )
        private val _pdfStatus = MutableStateFlow("")
        override val report: StateFlow<Report?> = _report
        override val parsedResults: StateFlow<ParsedResults?> = _results
        override val pdfStatus: StateFlow<String> = _pdfStatus
        override val socketName = MutableStateFlow("")
        override val notes = MutableStateFlow("")
        override fun updateReportDetails() { }
        override fun exportReportToPdf() { _pdfStatus.value = "EXPORT" }
    }

    @Test
    fun pingCard_expands_and_shows_details_locally() {
        val provider = FakeProvider()
        composeRule.setContent {
            val navController = rememberNavController()
            ReportDetailScreen(navController = navController, stateProvider = provider)
        }
        composeRule.onNodeWithTag("PingDetailsView").assertDoesNotExist()
        composeRule.onNodeWithTag("PingResultCard").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("PingDetailsView").assertIsDisplayed()
        composeRule.onNodeWithText("Avg: 10ms").assertIsDisplayed()
    }
}
