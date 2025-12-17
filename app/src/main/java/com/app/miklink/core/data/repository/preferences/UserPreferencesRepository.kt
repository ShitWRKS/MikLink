package com.app.miklink.core.data.repository.preferences

import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val idNumberingStrategy: Flow<IdNumberingStrategy>

    val pdfIncludeEmptyTests: Flow<Boolean>
    val pdfSelectedColumns: Flow<Set<String>>
    val pdfReportTitle: Flow<String>
    val pdfHideEmptyColumns: Flow<Boolean>

    val dashboardGlowIntensity: Flow<Float>
    val probePollingInterval: Flow<Long>

    suspend fun setIdNumberingStrategy(strategy: IdNumberingStrategy)
    suspend fun setPdfIncludeEmptyTests(include: Boolean)
    suspend fun setPdfSelectedColumns(columns: Set<String>)
    suspend fun setPdfReportTitle(title: String)
    suspend fun setPdfHideEmptyColumns(hide: Boolean)
    suspend fun setDashboardGlowIntensity(intensity: Float)
    suspend fun setProbePollingInterval(interval: Long)
}
