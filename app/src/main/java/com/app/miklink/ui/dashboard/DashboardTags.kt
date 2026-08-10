package com.app.miklink.ui.dashboard

object DashboardTags {
    const val SCREEN = "dashboard_screen"
    const val CLIENT_SELECTOR = "dashboard_client_selector"
    const val PROFILE_SELECTOR = "dashboard_profile_selector"
    const val START_TEST_BUTTON = "dashboard_start_test_button"
    const val CLIENT_ITEM_PREFIX = "dashboard_client_item"
    const val PROFILE_ITEM_PREFIX = "dashboard_profile_item"

    val stableTags: Set<String> = setOf(
        SCREEN,
        CLIENT_SELECTOR,
        PROFILE_SELECTOR,
        START_TEST_BUTTON
    )
}
