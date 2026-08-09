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
        const val SAVE = "client_save_button"
        const val ITEM_PREFIX = "client_item"
    }

    object Profile {
        const val LIST = "profile_list_screen"
        const val ADD = "profile_add_button"
        const val EDIT = "profile_edit_screen"
        const val NAME = "profile_name_input"
        const val SAVE = "profile_save_button"
        const val ITEM_PREFIX = "profile_item"
    }

    object History {
        const val SCREEN = "history_screen"
        const val SEARCH = "history_search_input"
        const val REPORT_ITEM_PREFIX = "history_report_item"
    }

    object Report {
        const val SCREEN = "report_detail_screen"
        const val EXPORT_PDF = "report_export_pdf_button"
        const val PDF_DIALOG = "pdf_export_dialog"
        const val PDF_CONFIRM = "pdf_export_confirm_button"
    }

    object Settings {
        const val SCREEN = "settings_screen"
        const val PDF = "settings_pdf_entry"
        const val BACKUP = "settings_backup_entry"
        const val PDF_SCREEN = "pdf_settings_screen"
        const val BACKUP_SCREEN = "backup_settings_screen"
        const val BACKUP_EXPORT = "backup_export_button"
        const val BACKUP_IMPORT = "backup_import_button"
    }

    val stableTags: Set<String> = setOf(
        Probe.SCREEN, Probe.ADDRESS, Probe.USERNAME, Probe.PASSWORD, Probe.VERIFY, Probe.SAVE,
        Client.LIST, Client.SEARCH, Client.ADD, Client.EDIT, Client.NAME, Client.SAVE,
        Profile.LIST, Profile.ADD, Profile.EDIT, Profile.NAME, Profile.SAVE,
        History.SCREEN, History.SEARCH, Report.SCREEN, Report.EXPORT_PDF, Report.PDF_DIALOG,
        Report.PDF_CONFIRM, Settings.SCREEN, Settings.PDF, Settings.BACKUP, Settings.PDF_SCREEN,
        Settings.BACKUP_SCREEN, Settings.BACKUP_EXPORT, Settings.BACKUP_IMPORT
    )
}
