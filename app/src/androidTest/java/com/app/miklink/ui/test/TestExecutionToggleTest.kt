/*
 * Purpose: Validate TestExecution views render sections (including pending) and omit deprecated raw logs.
 * Inputs: Compose rules with synthetic TestSection data and a sample TestReport for completed view.
 * Outputs: Assertions on visible section titles and absence of removed raw log elements during instrumented tests.
 * Notes: Aligns Ping detail label with Packet Loss key used by domain/use case aggregation.
 */
package com.app.miklink.ui.test

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.R
import com.app.miklink.core.domain.model.TestReport
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.rememberLazyListState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestExecutionToggleTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleReport = TestReport(
        reportId = 1L,
        clientId = 1L,
        timestamp = 0L,
        socketName = "Socket-1",
        notes = "",
        probeName = "Probe",
        profileName = "Profile",
        overallStatus = "PASS",
        resultFormatVersion = 1,
        resultsJson = "{}"
    )

    @Test
    fun completed_view_toggle_shows_raw_logs() {
        composeRule.setContent {
            TestCompletedView(
                report = sampleReport,
                sections = listOf(
                    TestSection(
                        TestSectionCategory.TEST,
                        TestSectionType.PING,
                        "Ping",
                        "PASS",
                        listOf(TestDetail("Packet Loss", "0%"))
                    )
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
        // The toggles and raw logs have been removed: sections should be present and raw log should not be displayed
        composeRule.onNodeWithText("Ping").assertIsDisplayed()
        composeRule.onNodeWithText("RAW LOG: SAMPLE LINE").assertDoesNotExist()
    }

    @Test
    fun in_progress_view_toggle_shows_raw_logs() {
        composeRule.setContent {
            TestInProgressView(
                sections = listOf(
                    TestSection(
                        TestSectionCategory.TEST,
                        TestSectionType.PING,
                        "Ping",
                        "PASS",
                        listOf(TestDetail("Packet Loss", "0%"))
                    )
                ),
                listState = rememberLazyListState(),
                modifier = Modifier.fillMaxSize()
            )
        }
        // Toggles/raw logs removed: section should be present and raw log should not exist
        composeRule.onNodeWithText("Ping").assertIsDisplayed()
        composeRule.onNodeWithText("RAW LOG: SAMPLE IN PROGRESS").assertDoesNotExist()
    }

    @Test
    fun in_progress_view_shows_all_pending_sections() {
        composeRule.setContent {
            TestInProgressView(
                sections = listOf(
                    TestSection(TestSectionCategory.INFO, TestSectionType.NETWORK, "Network", "PENDING"),
                    TestSection(TestSectionCategory.TEST, TestSectionType.PING, "Ping", "PENDING"),
                    TestSection(TestSectionCategory.TEST, TestSectionType.SPEED, "Speed Test", "PENDING")
                ),
                listState = rememberLazyListState(),
                modifier = Modifier.fillMaxSize()
            )
        }

        composeRule.onNodeWithText("Network").assertIsDisplayed()
        composeRule.onNodeWithText("Ping").assertIsDisplayed()
        composeRule.onNodeWithText("Speed Test").assertIsDisplayed()
    }
}
