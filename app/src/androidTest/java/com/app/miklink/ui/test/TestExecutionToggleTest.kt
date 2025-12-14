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
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }
        // The toggles and raw logs have been removed: sections should be present and raw log should not be displayed
        composeRule.onNodeWithText("Ping").assertIsDisplayed()
        composeRule.onNodeWithText("RAW LOG: SAMPLE LINE").assertDoesNotExist()
    }

    @Test
    fun in_progress_view_toggle_shows_raw_logs() {
        composeRule.setContent {
            val showRawLogState = remember { mutableStateOf(false) }
            TestInProgressView(
                sections = listOf(TestSection(TestSectionCategory.TEST, TestSectionType.PING, "Ping", "PASS", listOf(TestDetail("Loss", "0%")))),
                listState = androidx.compose.foundation.lazy.rememberLazyListState(),
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }
        // Toggles/raw logs removed: section should be present and raw log should not exist
        composeRule.onNodeWithText("Ping").assertIsDisplayed()
        composeRule.onNodeWithText("RAW LOG: SAMPLE IN PROGRESS").assertDoesNotExist()
    }
}
