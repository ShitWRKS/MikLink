package com.app.miklink.core.domain.test.step

import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestExecutionContext

/**
 * Step per eseguire test ping verso uno o più target.
 */
interface PingStep {
    suspend fun run(context: TestExecutionContext): StepResult<List<PingTargetOutcome>>
}

