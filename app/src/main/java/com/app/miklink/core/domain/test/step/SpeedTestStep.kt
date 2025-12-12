package com.app.miklink.core.domain.test.step

import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestExecutionContext

/**
 * Step per eseguire speed test.
 */
interface SpeedTestStep {
    suspend fun run(context: TestExecutionContext): StepResult
}

