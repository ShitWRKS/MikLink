package com.app.miklink.core.domain.test.model

import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.ProbeConfig
import com.app.miklink.core.data.local.room.v1.model.TestProfile

/**
 * Contesto di esecuzione di un test.
 * Contiene tutte le entità caricate necessarie per eseguire i vari step.
 */
data class TestExecutionContext(
    val client: Client,
    val probeConfig: ProbeConfig,
    val profile: TestProfile,
    val socketId: String? = null,
    val notes: String? = null
)

