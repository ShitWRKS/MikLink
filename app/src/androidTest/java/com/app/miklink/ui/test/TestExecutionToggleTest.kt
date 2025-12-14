package com.app.miklink.ui.test

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.miklink.R
import com.app.miklink.core.domain.model.TestReport
import com.app.miklink.ui.test.TestSection
import com.app.miklink.ui.test.TestSectionCategory
import com.app.miklink.ui.test.TestSectionType
import com.app.miklink.ui.test.TestDetail
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestExecutionToggleTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleReport = Report(reportId = 1L, clientId = 1L, timestamp = 0L, socketName = "Socket-1", notes = "", probeName = "Probe", profileName = "Profile", overallStatus = "PASS", resultsJson = "{}")

    @Test
    fun completed_view_toggle_shows_raw_logs() {
        composeRule.setContent {
            val showRawLogState = remember { mutableStateOf(false) }
            TestCompletedView(
                report = sampleReport,
                sections = listOf(TestSection(TestSectionCategory.TEST, TestSectionType.PING, "Ping", "PASS", listOf(TestDetail("Loss", "0%")))),
                log = listOf("RAW LOG: SAMPLE LINE"),
                showRawLog = showRawLogState.value,
                onToggleRawLog = { showRawLogState.value = !showRawLogState.value },
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        val showLabel = composeRule.activity.getString(R.string.test_toggle_show_logs)
        val hideLabel = composeRule.activity.getString(R.string.test_toggle_hide_logs)

        // Initially the sections should be present (Ping card title)
        composeRule.onNodeWithText("Ping").assertIsDisplayed()

        // Click the toggle button
        composeRule.onNodeWithText(showLabel).performClick()
        composeRule.waitForIdle()

        // After toggle, raw log text should be visible
        composeRule.onNodeWithText("RAW LOG: SAMPLE LINE").assertIsDisplayed()

        // Toggle back
        composeRule.onNodeWithText(hideLabel).performClick()
        composeRule.waitForIdle()

        // Sections visible again
        composeRule.onNodeWithText("Ping").assertIsDisplayed()
    }

    @Test
    fun in_progress_view_toggle_shows_raw_logs() {
        composeRule.setContent {
            val showRawLogState = remember { mutableStateOf(false) }
            TestInProgressView(
                log = listOf("RAW LOG: SAMPLE IN PROGRESS"),
                sections = listOf(TestSection(TestSectionCategory.TEST, TestSectionType.PING, "Ping", "PASS", listOf(TestDetail("Loss", "0%")))),
                listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                showRawLog = showRawLogState.value,
                onToggleRawLog = { showRawLogState.value = !showRawLogState.value },
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        val showRawLabel = composeRule.activity.getString(R.string.test_toggle_show_raw_logs)
        val hideRawLabel = composeRule.activity.getString(R.string.test_toggle_hide_raw_logs)

        // Initially the section card should be present
        composeRule.onNodeWithText("Ping").assertIsDisplayed()

        // Click the toggle to show raw logs
        composeRule.onNodeWithText(showRawLabel).performClick()
        composeRule.waitForIdle()

        // Raw log should be visible
        composeRule.onNodeWithText("RAW LOG: SAMPLE IN PROGRESS").assertIsDisplayed()

        // Toggle back to sections
        composeRule.onNodeWithText(hideRawLabel).performClick()
        composeRule.waitForIdle()

        // Section visible again
        composeRule.onNodeWithText("Ping").assertIsDisplayed()
    }
}
