package com.app.miklink.core.domain.test.step

import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestExecutionContext

/**
 * Step per eseguire discovery LLDP/CDP/MNDP.
 */
interface NeighborDiscoveryStep {
    suspend fun run(context: TestExecutionContext): StepResult
}

