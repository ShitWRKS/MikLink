package com.app.miklink.ui.history

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.navigation.compose.rememberNavController
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.core.domain.model.report.PingSample
import com.app.miklink.core.domain.model.report.ReportData
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
            TestReport(
                reportId = 42L,
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
