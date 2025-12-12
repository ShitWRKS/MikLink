package com.app.miklink.core.domain.test.step

import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestExecutionContext

/**
 * Step per applicare la configurazione di rete (DHCP/static) alla probe
 * in base al Client.
 */
interface NetworkConfigStep {
    suspend fun run(context: TestExecutionContext): StepResult
}

