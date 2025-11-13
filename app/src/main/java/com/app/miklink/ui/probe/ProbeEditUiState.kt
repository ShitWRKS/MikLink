package com.app.miklink.ui.probe

import com.app.miklink.data.repository.ProbeCheckResult

data class ProbeEditUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val name: String = "",
    val ipAddress: String = "",
    val username: String = "admin",
    val password: String = "",
    val probeCheckResult: ProbeCheckResult? = null
)
