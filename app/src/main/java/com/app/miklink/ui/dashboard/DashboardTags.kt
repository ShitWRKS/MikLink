package com.app.miklink.ui.dashboard

object DashboardTags {
    const val SCREEN = "dashboard_screen"
    const val CLIENT_SELECTOR = "dashboard_client_selector"
    const val PROFILE_SELECTOR = "dashboard_profile_selector"
    const val START_TEST_BUTTON = "dashboard_start_test_button"
    const val HISTORY_BUTTON = "dashboard_history_button"
    const val SETTINGS_BUTTON = "dashboard_settings_button"
    const val MANAGE_CLIENTS = "dashboard_manage_clients"
    const val MANAGE_PROFILES = "dashboard_manage_profiles"
    const val CLIENT_ITEM_PREFIX = "dashboard_client_item"
    const val PROFILE_ITEM_PREFIX = "dashboard_profile_item"

    val stableTags: Set<String> = setOf(
        SCREEN,
        CLIENT_SELECTOR,
        PROFILE_SELECTOR,
        START_TEST_BUTTON,
        HISTORY_BUTTON,
        SETTINGS_BUTTON,
        MANAGE_CLIENTS,
        MANAGE_PROFILES
    )
}
