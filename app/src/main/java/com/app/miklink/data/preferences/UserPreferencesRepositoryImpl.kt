package com.app.miklink.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.domain.model.preferences.CustomPalette
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import com.app.miklink.core.domain.model.preferences.ThemeConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private val THEME_KEY = stringPreferencesKey("theme_config")
    private val ID_NUMBERING_STRATEGY_KEY = stringPreferencesKey("id_numbering_strategy")

    override val themeConfig: Flow<ThemeConfig> = dataStore.data
        .map { preferences ->
            val themeName = preferences[THEME_KEY] ?: ThemeConfig.FOLLOW_SYSTEM.name
            runCatching { ThemeConfig.valueOf(themeName) }.getOrDefault(ThemeConfig.FOLLOW_SYSTEM)
        }

    override val idNumberingStrategy: Flow<IdNumberingStrategy> = dataStore.data
        .map { preferences ->
            val strategyName = preferences[ID_NUMBERING_STRATEGY_KEY] ?: IdNumberingStrategy.CONTINUOUS_INCREMENT.name
            runCatching { IdNumberingStrategy.valueOf(strategyName) }.getOrDefault(IdNumberingStrategy.CONTINUOUS_INCREMENT)
        }

    private val CUSTOM_PRIMARY_KEY = intPreferencesKey("custom_primary_color")
    private val CUSTOM_SECONDARY_KEY = intPreferencesKey("custom_secondary_color")
    private val CUSTOM_BACKGROUND_KEY = intPreferencesKey("custom_background_color")
    private val CUSTOM_CONTENT_KEY = intPreferencesKey("custom_content_color")

    override val customPalette: Flow<CustomPalette> = dataStore.data
        .map { preferences ->
            CustomPalette(
                primary = preferences[CUSTOM_PRIMARY_KEY],
                secondary = preferences[CUSTOM_SECONDARY_KEY],
                background = preferences[CUSTOM_BACKGROUND_KEY],
                content = preferences[CUSTOM_CONTENT_KEY]
            )
        }

    override suspend fun setCustomPalette(primary: Int?, secondary: Int?, background: Int?, content: Int?) {
        dataStore.edit { preferences ->
            if (primary != null) preferences[CUSTOM_PRIMARY_KEY] = primary else preferences.remove(CUSTOM_PRIMARY_KEY)
            if (secondary != null) preferences[CUSTOM_SECONDARY_KEY] = secondary else preferences.remove(CUSTOM_SECONDARY_KEY)
            if (background != null) preferences[CUSTOM_BACKGROUND_KEY] = background else preferences.remove(CUSTOM_BACKGROUND_KEY)
            if (content != null) preferences[CUSTOM_CONTENT_KEY] = content else preferences.remove(CUSTOM_CONTENT_KEY)
        }
    }

    override suspend fun setTheme(themeConfig: ThemeConfig) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeConfig.name
        }
    }

    private val PDF_INCLUDE_EMPTY_TESTS_KEY = booleanPreferencesKey("pdf_include_empty_tests")
    private val PDF_SELECTED_COLUMNS_KEY = stringSetPreferencesKey("pdf_selected_columns")

    override val pdfIncludeEmptyTests: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PDF_INCLUDE_EMPTY_TESTS_KEY] ?: true }

    override val pdfSelectedColumns: Flow<Set<String>> = dataStore.data
        .map { preferences -> preferences[PDF_SELECTED_COLUMNS_KEY] ?: emptySet() }

    override suspend fun setPdfIncludeEmptyTests(include: Boolean) {
        dataStore.edit { preferences ->
            preferences[PDF_INCLUDE_EMPTY_TESTS_KEY] = include
        }
    }

    override suspend fun setPdfSelectedColumns(columns: Set<String>) {
        dataStore.edit { preferences ->
            preferences[PDF_SELECTED_COLUMNS_KEY] = columns
        }
    }

    override suspend fun setIdNumberingStrategy(strategy: IdNumberingStrategy) {
        dataStore.edit { preferences ->
            preferences[ID_NUMBERING_STRATEGY_KEY] = strategy.name
        }
    }

    private val PDF_REPORT_TITLE_KEY = stringPreferencesKey("pdf_report_title")
    private val PDF_HIDE_EMPTY_COLUMNS_KEY = booleanPreferencesKey("pdf_hide_empty_columns")

    override val pdfReportTitle: Flow<String> = dataStore.data
        .map { preferences -> preferences[PDF_REPORT_TITLE_KEY] ?: "Collaudo Cablaggio di Rete" }

    override val pdfHideEmptyColumns: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PDF_HIDE_EMPTY_COLUMNS_KEY] ?: false }

    override suspend fun setPdfReportTitle(title: String) {
        dataStore.edit { preferences ->
            preferences[PDF_REPORT_TITLE_KEY] = title
        }
    }

    override suspend fun setPdfHideEmptyColumns(hide: Boolean) {
        dataStore.edit { preferences ->
            preferences[PDF_HIDE_EMPTY_COLUMNS_KEY] = hide
        }
    }

    private val DASHBOARD_GLOW_INTENSITY_KEY = floatPreferencesKey("dashboard_glow_intensity")

    override val dashboardGlowIntensity: Flow<Float> = dataStore.data
        .map { preferences -> preferences[DASHBOARD_GLOW_INTENSITY_KEY] ?: 0.5f }

    override suspend fun setDashboardGlowIntensity(intensity: Float) {
        dataStore.edit { preferences ->
            preferences[DASHBOARD_GLOW_INTENSITY_KEY] = intensity
        }
    }

    private val PROBE_POLLING_INTERVAL_KEY = longPreferencesKey("probe_polling_interval")

    override val probePollingInterval: Flow<Long> = dataStore.data
        .map { preferences -> preferences[PROBE_POLLING_INTERVAL_KEY] ?: 5000L }

    override suspend fun setProbePollingInterval(interval: Long) {
        dataStore.edit { preferences ->
            preferences[PROBE_POLLING_INTERVAL_KEY] = interval
        }
    }
}
