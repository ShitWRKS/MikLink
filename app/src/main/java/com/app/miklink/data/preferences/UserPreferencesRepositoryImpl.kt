package com.app.miklink.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import com.app.miklink.core.data.pdf.ExportColumn
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private val ID_NUMBERING_STRATEGY_KEY = stringPreferencesKey("id_numbering_strategy")

    override val idNumberingStrategy: Flow<IdNumberingStrategy> = dataStore.data
        .map { preferences ->
            val strategyName = preferences[ID_NUMBERING_STRATEGY_KEY] ?: IdNumberingStrategy.CONTINUOUS_INCREMENT.name
            runCatching { IdNumberingStrategy.valueOf(strategyName) }.getOrDefault(IdNumberingStrategy.CONTINUOUS_INCREMENT)
        }

    private val PDF_INCLUDE_EMPTY_TESTS_KEY = booleanPreferencesKey("pdf_include_empty_tests")
    private val PDF_SELECTED_COLUMNS_KEY = stringSetPreferencesKey("pdf_selected_columns")

    override val pdfIncludeEmptyTests: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[PDF_INCLUDE_EMPTY_TESTS_KEY] ?: true }

    override val pdfSelectedColumns: Flow<Set<String>> = dataStore.data
        .map { preferences -> preferences[PDF_SELECTED_COLUMNS_KEY] ?: ExportColumn.values().map { it.name }.toSet() }

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
        .map { preferences -> preferences[PDF_REPORT_TITLE_KEY] ?: context.getString(com.app.miklink.R.string.pdf_default_report_title) }

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

    private val NEIGHBOR_DISCOVERY_PROTOCOLS_KEY = stringSetPreferencesKey("neighbor_discovery_protocols")

    override val neighborDiscoveryProtocols: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[NEIGHBOR_DISCOVERY_PROTOCOLS_KEY] ?: setOf("CDP", "LLDP", "MNDP")
        }

    override suspend fun setNeighborDiscoveryProtocols(protocols: Set<String>) {
        dataStore.edit { preferences ->
            preferences[NEIGHBOR_DISCOVERY_PROTOCOLS_KEY] = protocols
        }
    }
    
    override suspend fun resetPdfPreferencesToDefaults() {
        dataStore.edit { preferences ->
            preferences.remove(PDF_INCLUDE_EMPTY_TESTS_KEY)
            preferences.remove(PDF_SELECTED_COLUMNS_KEY)
            preferences.remove(PDF_REPORT_TITLE_KEY)
            preferences.remove(PDF_HIDE_EMPTY_COLUMNS_KEY)
        }
    }
}
