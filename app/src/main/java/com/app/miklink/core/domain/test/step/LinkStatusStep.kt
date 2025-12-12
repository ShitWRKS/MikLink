package com.app.miklink.core.domain.test.step

import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestExecutionContext

/**
 * Step per verificare lo stato del link (Layer 1).
 */
interface LinkStatusStep {
    suspend fun run(context: TestExecutionContext): StepResult
}

