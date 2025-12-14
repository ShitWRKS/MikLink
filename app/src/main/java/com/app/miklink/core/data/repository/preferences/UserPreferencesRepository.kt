package com.app.miklink.core.data.repository.preferences

import com.app.miklink.core.domain.model.preferences.CustomPalette
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import com.app.miklink.core.domain.model.preferences.ThemeConfig
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val themeConfig: Flow<ThemeConfig>
    val idNumberingStrategy: Flow<IdNumberingStrategy>
    val customPalette: Flow<CustomPalette>

    val pdfIncludeEmptyTests: Flow<Boolean>
    val pdfSelectedColumns: Flow<Set<String>>
    val pdfReportTitle: Flow<String>
    val pdfHideEmptyColumns: Flow<Boolean>

    val dashboardGlowIntensity: Flow<Float>
    val probePollingInterval: Flow<Long>

    suspend fun setCustomPalette(primary: Int?, secondary: Int?, background: Int?, content: Int? = null)
    suspend fun setTheme(themeConfig: ThemeConfig)
    suspend fun setIdNumberingStrategy(strategy: IdNumberingStrategy)
    suspend fun setPdfIncludeEmptyTests(include: Boolean)
    suspend fun setPdfSelectedColumns(columns: Set<String>)
    suspend fun setPdfReportTitle(title: String)
    suspend fun setPdfHideEmptyColumns(hide: Boolean)
    suspend fun setDashboardGlowIntensity(intensity: Float)
    suspend fun setProbePollingInterval(interval: Long)
}
