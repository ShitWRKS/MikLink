/*
 * Purpose: Validate TestExecution views expose log toggles and panes with stable semantics tags.
 * Inputs: Compose rules with synthetic sections/logs and sample TestReport for completed view.
 * Outputs: Assertions on toggle visibility, log pane visibility, and log content presence in both in-progress and completed screens.
 * Notes: Uses semantics tags instead of localized strings to keep tests stable across languages.
 */
package com.app.miklink.ui.test

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.ui.test.components.TestExecutionTags
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
    fun completed_view_toggle_shows_and_hides_logs() {
        composeRule.setContent {
            var showLogs by remember { mutableStateOf(false) }
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
                logs = listOf("Completed log line"),
                showLogs = showLogs,
                onToggleLogs = { showLogs = !showLogs },
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        composeRule.onNodeWithTag(TestExecutionTags.COMPLETED_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestExecutionTags.LOG_PANE).assertDoesNotExist()

        composeRule.onNodeWithTag(TestExecutionTags.COMPLETED_TOGGLE).performClick()
        composeRule.onNodeWithTag(TestExecutionTags.LOG_PANE).assertIsDisplayed()
        composeRule.onNodeWithText("Completed log line").assertIsDisplayed()

        composeRule.onNodeWithTag(TestExecutionTags.COMPLETED_TOGGLE).performClick()
        composeRule.onNodeWithTag(TestExecutionTags.LOG_PANE).assertDoesNotExist()
    }

    @Test
    fun in_progress_view_toggle_shows_and_hides_logs() {
        composeRule.setContent {
            var showLogs by remember { mutableStateOf(false) }
            TestInProgressView(
                sections = listOf(
                    TestSection(
                        TestSectionCategory.TEST,
                        TestSectionType.PING,
                        "Ping",
                        "PENDING"
                    )
                ),
                listState = rememberLazyListState(),
                logs = listOf("In-progress log"),
                showRawLogs = showLogs,
                onToggleRawLogs = { showLogs = !showLogs },
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        composeRule.onNodeWithTag(TestExecutionTags.IN_PROGRESS_TOGGLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TestExecutionTags.LOG_PANE).assertDoesNotExist()

        composeRule.onNodeWithTag(TestExecutionTags.IN_PROGRESS_TOGGLE).performClick()
        composeRule.onNodeWithTag(TestExecutionTags.LOG_PANE).assertIsDisplayed()
        composeRule.onNodeWithText("In-progress log").assertIsDisplayed()

        composeRule.onNodeWithTag(TestExecutionTags.IN_PROGRESS_TOGGLE).performClick()
        composeRule.onNodeWithTag(TestExecutionTags.LOG_PANE).assertDoesNotExist()
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
                logs = emptyList(),
                showRawLogs = false,
                onToggleRawLogs = {},
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        composeRule.onNodeWithText("Network").assertIsDisplayed()
        composeRule.onNodeWithText("Ping").assertIsDisplayed()
        composeRule.onNodeWithText("Speed Test").assertIsDisplayed()
    }

    @Test
    fun ping_targets_render_as_structured_list() {
        composeRule.setContent {
            TestCompletedView(
                report = sampleReport,
                sections = listOf(
                    TestSection(
                        TestSectionCategory.TEST,
                        TestSectionType.PING,
                        "Ping",
                        "FAIL",
                        listOf(
                            TestDetail("Packet Loss", "10%"),
                            TestDetail("Min RTT", "5ms"),
                            TestDetail("Avg RTT", "6ms"),
                            TestDetail("Max RTT", "8ms"),
                            TestDetail("Target 8.8.8.8", "loss=10% min=5ms avg=6ms max=8ms"),
                            TestDetail("Target 1.1.1.1", "ERR: timeout")
                        )
                    )
                ),
                logs = emptyList(),
                showLogs = false,
                onToggleLogs = {},
                modifier = Modifier.fillMaxSize()
            )
        }

        composeRule.onNodeWithText("Packet Loss").assertIsDisplayed()
        composeRule.onNodeWithText("Target 8.8.8.8").assertIsDisplayed()
        composeRule.onNodeWithText("5ms").assertIsDisplayed()
        composeRule.onNodeWithText("ERR: timeout").assertIsDisplayed()
    }
}
