package com.app.miklink.core.data.repository

import com.app.miklink.core.domain.model.ProbeConfig

/**
 * Data classes e sealed classes utilizzate dai repository per probe management.
 * 
 * Originariamente definite in AppRepository.kt, spostate qui durante EPIC S8
 * per permettere la rimozione completa di AppRepository.
 */

data class ProbeStatusInfo(val probe: ProbeConfig, val isOnline: Boolean)

sealed class ProbeCheckResult {
    data class Success(
        val boardName: String,
        val interfaces: List<String>,
        val effectiveIsHttps: Boolean,
        val didFallbackToHttp: Boolean,
        val warning: String? = null
    ) : ProbeCheckResult()
    data class Error(val message: String) : ProbeCheckResult()
}

data class NetworkConfigFeedback(
    val mode: String,
    val interfaceName: String,
    val address: String?,
    val gateway: String?,
    val dns: String?,
    val message: String
)
