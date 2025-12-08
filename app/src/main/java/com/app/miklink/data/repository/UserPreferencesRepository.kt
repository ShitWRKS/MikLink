package com.app.miklink.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

enum class ThemeConfig {
    LIGHT,
    DARK,
    FOLLOW_SYSTEM
}

enum class IdNumberingStrategy {
    CONTINUOUS_INCREMENT,  // Default: sempre avanti
    FILL_GAPS             // Riempie i buchi
}

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val THEME_KEY = stringPreferencesKey("theme_config")
    private val ID_NUMBERING_STRATEGY_KEY = stringPreferencesKey("id_numbering_strategy")

    val themeConfig: Flow<ThemeConfig> = dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ThemeConfig.FOLLOW_SYSTEM.name
            try {
                ThemeConfig.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemeConfig.FOLLOW_SYSTEM
            }
        }

    val idNumberingStrategy: Flow<IdNumberingStrategy> = dataStore.data
        .map { preferences ->
            val strategyName = preferences[ID_NUMBERING_STRATEGY_KEY] ?: IdNumberingStrategy.CONTINUOUS_INCREMENT.name
            try {
                IdNumberingStrategy.valueOf(strategyName)
            } catch (e: IllegalArgumentException) {
                IdNumberingStrategy.CONTINUOUS_INCREMENT
            }
        }

    private val CUSTOM_PRIMARY_KEY = androidx.datastore.preferences.core.intPreferencesKey("custom_primary_color")
    private val CUSTOM_SECONDARY_KEY = androidx.datastore.preferences.core.intPreferencesKey("custom_secondary_color")
    private val CUSTOM_BACKGROUND_KEY = androidx.datastore.preferences.core.intPreferencesKey("custom_background_color")
    private val CUSTOM_CONTENT_KEY = androidx.datastore.preferences.core.intPreferencesKey("custom_content_color")

    data class CustomPalette(
        val primary: Int? = null,
        val secondary: Int? = null,
        val background: Int? = null,
        val content: Int? = null
    )

    val customPalette: Flow<CustomPalette> = dataStore.data
        .map { preferences ->
            CustomPalette(
                primary = preferences[CUSTOM_PRIMARY_KEY],
                secondary = preferences[CUSTOM_SECONDARY_KEY],
                background = preferences[CUSTOM_BACKGROUND_KEY],
                content = preferences[CUSTOM_CONTENT_KEY]
            )
        }

    suspend fun setCustomPalette(primary: Int?, secondary: Int?, background: Int?, content: Int? = null) {
        dataStore.edit { preferences ->
            if (primary != null) preferences[CUSTOM_PRIMARY_KEY] = primary else preferences.remove(CUSTOM_PRIMARY_KEY)
            if (secondary != null) preferences[CUSTOM_SECONDARY_KEY] = secondary else preferences.remove(CUSTOM_SECONDARY_KEY)
            if (background != null) preferences[CUSTOM_BACKGROUND_KEY] = background else preferences.remove(CUSTOM_BACKGROUND_KEY)
            if (content != null) preferences[CUSTOM_CONTENT_KEY] = content else preferences.remove(CUSTOM_CONTENT_KEY)
        }
    }

    suspend fun setTheme(themeConfig: ThemeConfig) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeConfig.name
        }
    }

    private val PDF_INCLUDE_EMPTY_TESTS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("pdf_include_empty_tests")
    private val PDF_SELECTED_COLUMNS_KEY = androidx.datastore.preferences.core.stringSetPreferencesKey("pdf_selected_columns")

    val pdfIncludeEmptyTests: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PDF_INCLUDE_EMPTY_TESTS_KEY] ?: true
        }

    val pdfSelectedColumns: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[PDF_SELECTED_COLUMNS_KEY] ?: emptySet()
        }

    suspend fun setPdfIncludeEmptyTests(include: Boolean) {
        dataStore.edit { preferences ->
            preferences[PDF_INCLUDE_EMPTY_TESTS_KEY] = include
        }
    }

    suspend fun setPdfSelectedColumns(columns: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PDF_SELECTED_COLUMNS_KEY] = columns
        }
    }

    suspend fun setIdNumberingStrategy(strategy: IdNumberingStrategy) {
        dataStore.edit { preferences ->
            preferences[ID_NUMBERING_STRATEGY_KEY] = strategy.name
        }
    }
    private val PDF_REPORT_TITLE_KEY = stringPreferencesKey("pdf_report_title")
    private val PDF_HIDE_EMPTY_COLUMNS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("pdf_hide_empty_columns")

    val pdfReportTitle: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PDF_REPORT_TITLE_KEY] ?: "Collaudo Cablaggio di Rete"
        }

    val pdfHideEmptyColumns: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PDF_HIDE_EMPTY_COLUMNS_KEY] ?: false
        }

    suspend fun setPdfReportTitle(title: String) {
        dataStore.edit { preferences ->
            preferences[PDF_REPORT_TITLE_KEY] = title
        }
    }

    suspend fun setPdfHideEmptyColumns(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[PDF_HIDE_EMPTY_COLUMNS_KEY] = hide
        }
    }
    private val DASHBOARD_GLOW_INTENSITY_KEY = androidx.datastore.preferences.core.floatPreferencesKey("dashboard_glow_intensity")

    val dashboardGlowIntensity: Flow<Float> = dataStore.data
        .map { preferences ->
            preferences[DASHBOARD_GLOW_INTENSITY_KEY] ?: 0.5f // Default Intensity 50%
        }

    suspend fun setDashboardGlowIntensity(intensity: Float) {
        dataStore.edit { preferences ->
            preferences[DASHBOARD_GLOW_INTENSITY_KEY] = intensity
        }
    }
    private val PROBE_POLLING_INTERVAL_KEY = androidx.datastore.preferences.core.longPreferencesKey("probe_polling_interval")

    val probePollingInterval: Flow<Long> = dataStore.data
        .map { preferences ->
            preferences[PROBE_POLLING_INTERVAL_KEY] ?: 5000L // Default 5s
        }

    suspend fun setProbePollingInterval(interval: Long) {
        dataStore.edit { preferences ->
            preferences[PROBE_POLLING_INTERVAL_KEY] = interval
        }
    }
}
