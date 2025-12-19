/*
 * Purpose: Centralize semantics tags for Test Execution UI to keep instrumentation tests stable and localization-agnostic.
 * Inputs: None; constants are consumed by composables when applying testTag modifiers.
 * Outputs: Stable string tags used by UI tests to query toggles and log panels.
 */
package com.app.miklink.ui.test.components

object TestExecutionTags {
    const val IN_PROGRESS_TOGGLE = "test_execution_toggle_in_progress_logs"
    const val COMPLETED_TOGGLE = "test_execution_toggle_completed_logs"
    const val LOG_PANE = "test_execution_log_pane"
    const val LOG_LINE = "test_execution_log_line"
    const val HERO_RUNNING = "test_execution_hero_running"
    const val HERO_COMPLETED = "test_execution_hero_completed"
    const val LOGS_BOX = "test_execution_logs_box"
    const val SECTION_CARD_PREFIX = "test_execution_section_card"
    const val BOTTOM_CLOSE = "test_execution_bottom_close"
    const val BOTTOM_REPEAT = "test_execution_bottom_repeat"
    const val BOTTOM_SAVE = "test_execution_bottom_save"
    const val PING_SAMPLES_LIST = "test_execution_ping_samples_list"
}
