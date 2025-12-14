package com.app.miklink.core.domain.test.model

import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile

/**
 * Contesto di esecuzione di un test.
 * Contiene tutte le entità caricate necessarie per eseguire i vari step.
 */
data class TestExecutionContext(
    val client: Client,
    val probeConfig: ProbeConfig,
    val testProfile: TestProfile,
    val socketId: String?,
    val notes: String?
)

