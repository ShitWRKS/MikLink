package com.app.miklink.ui.testing

/** Stable, localization-independent identifiers exposed as resource IDs only in debug builds. */
object AgentUiTags {
    object Probe {
        const val SCREEN = "probe_edit_screen"
        const val ADDRESS = "probe_address_input"
        const val USERNAME = "probe_username_input"
        const val PASSWORD = "probe_password_input"
        const val VERIFY = "probe_verify_button"
        const val SAVE = "probe_save_button"
    }

    object Client {
        const val LIST = "client_list_screen"
        const val SEARCH = "client_search_input"
        const val ADD = "client_add_button"
        const val EDIT = "client_edit_screen"
        const val NAME = "client_name_input"
        const val LOCATION = "client_location_input"
        const val NOTES = "client_notes_input"
        const val NETWORK_DHCP = "client_network_dhcp"
        const val NETWORK_STATIC = "client_network_static"
        const val STATIC_CIDR = "client_static_cidr_input"
        const val STATIC_GATEWAY = "client_static_gateway_input"
        const val SAVE = "client_save_button"
        const val ITEM_PREFIX = "client_item"
    }

    object Profile {
        const val LIST = "profile_list_screen"
        const val ADD = "profile_add_button"
        const val EDIT = "profile_edit_screen"
        const val NAME = "profile_name_input"
        const val DESCRIPTION = "profile_description_input"
        const val RUN_TDR = "profile_run_tdr"
        const val RUN_LINK = "profile_run_link"
        const val RUN_NEIGHBORS = "profile_run_neighbors"
        const val RUN_PING = "profile_run_ping"
        const val RUN_SPEED = "profile_run_speed"
        const val PING_CONFIG = "profile_ping_config"
        const val PING_TARGET_1 = "profile_ping_target_1"
        const val PING_COUNT = "profile_ping_count"
        const val SAVE = "profile_save_button"
        const val ITEM_PREFIX = "profile_item"
    }

    object History {
        const val SCREEN = "history_screen"
        const val SEARCH = "history_search_input"
        const val REPORT_ITEM_PREFIX = "history_report_item"
        const val CLIENT_GROUP_PREFIX = "history_client_group"
        const val CLIENT_EXPAND_PREFIX = "history_client_expand"
    }

    object Report {
        const val SCREEN = "report_detail_screen"
        const val EXPORT_PDF = "report_export_pdf_button"
        const val PDF_DIALOG = "pdf_export_dialog"
        const val PDF_OPTIONS = "pdf_export_options"
        const val PDF_CONFIRM = "pdf_export_confirm_button"
        const val DELETE = "report_delete_button"
        const val REPEAT = "report_repeat_button"
        const val DELETE_CONFIRM = "report_delete_confirm"
        const val PDF_TITLE = "pdf_export_title_input"
        const val PDF_ORIENTATION_PORTRAIT = "pdf_export_orientation_portrait"
        const val PDF_ORIENTATION_LANDSCAPE = "pdf_export_orientation_landscape"
        const val PDF_SIGNATURES = "pdf_export_signatures"
        const val PDF_INCLUDE_EMPTY = "pdf_export_include_empty"
        const val PDF_HIDE_EMPTY_COLUMNS = "pdf_export_hide_empty_columns"
    }

    object Settings {
        const val SCREEN = "settings_screen"
        const val PROBE = "settings_probe_entry"
        const val PDF = "settings_pdf_entry"
        const val BACKUP = "settings_backup_entry"
        const val PDF_SCREEN = "pdf_settings_screen"
        const val POLLING = "settings_polling_slider"
        const val GLOW = "settings_glow_slider"
        const val ID_STRATEGY = "settings_id_strategy"
        const val DISCOVERY_PROTOCOLS = "settings_discovery_protocols"
        const val PDF_TITLE = "pdf_settings_title_input"
        const val PDF_INCLUDE_EMPTY = "pdf_settings_include_empty"
        const val PDF_HIDE_EMPTY_COLUMNS = "pdf_settings_hide_empty_columns"
        const val PDF_COLUMN_PREFIX = "pdf_settings_column"
        const val BACKUP_SCREEN = "backup_settings_screen"
        const val BACKUP_EXPORT = "backup_export_button"
        const val BACKUP_IMPORT = "backup_import_button"
    }

    val stableTags: Set<String> = setOf(
        Probe.SCREEN, Probe.ADDRESS, Probe.USERNAME, Probe.PASSWORD, Probe.VERIFY, Probe.SAVE,
        Client.LIST, Client.SEARCH, Client.ADD, Client.EDIT, Client.NAME, Client.LOCATION,
        Client.NOTES, Client.NETWORK_DHCP, Client.NETWORK_STATIC, Client.STATIC_CIDR,
        Client.STATIC_GATEWAY, Client.SAVE,
        Profile.LIST, Profile.ADD, Profile.EDIT, Profile.NAME, Profile.DESCRIPTION,
        Profile.RUN_TDR, Profile.RUN_LINK, Profile.RUN_NEIGHBORS, Profile.RUN_PING,
        Profile.RUN_SPEED, Profile.PING_CONFIG, Profile.PING_TARGET_1, Profile.PING_COUNT, Profile.SAVE,
        History.SCREEN, History.SEARCH, Report.SCREEN, Report.EXPORT_PDF, Report.PDF_DIALOG,
        Report.PDF_OPTIONS,
        Report.PDF_CONFIRM, Report.DELETE, Report.REPEAT, Report.DELETE_CONFIRM,
        Report.PDF_TITLE, Report.PDF_ORIENTATION_PORTRAIT, Report.PDF_ORIENTATION_LANDSCAPE,
        Report.PDF_SIGNATURES, Report.PDF_INCLUDE_EMPTY, Report.PDF_HIDE_EMPTY_COLUMNS,
        Settings.SCREEN, Settings.PROBE, Settings.POLLING, Settings.GLOW, Settings.ID_STRATEGY,
        Settings.DISCOVERY_PROTOCOLS, Settings.PDF, Settings.BACKUP, Settings.PDF_SCREEN,
        Settings.PDF_TITLE, Settings.PDF_INCLUDE_EMPTY, Settings.PDF_HIDE_EMPTY_COLUMNS,
        Settings.BACKUP_SCREEN, Settings.BACKUP_EXPORT, Settings.BACKUP_IMPORT
    )
}
