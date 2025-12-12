package com.app.miklink.core.domain.test.step

import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestExecutionContext

/**
 * Step per eseguire il test TDR (Cable-Test).
 */
interface CableTestStep {
    suspend fun run(context: TestExecutionContext): StepResult
}

