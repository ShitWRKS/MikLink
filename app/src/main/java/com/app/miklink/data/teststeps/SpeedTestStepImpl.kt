/*
 * Purpose: Execute speed test step using MikroTikTestRepository and surface domain speed test data.
 * Inputs: Test execution context (probe config and client speed test settings).
 * Outputs: StepResult carrying SpeedTestData or skip/failure reasons.
 * Notes: Repository maps DTOs to domain; this step only performs validation and error handling.
 */
package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.model.TestSkipReason
import com.app.miklink.core.domain.test.step.SpeedTestStep
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Implementazione di SpeedTestStep.
 * Usa MikroTikTestRepository per eseguire speed test.
 */
class SpeedTestStepImpl @Inject constructor(
    private val mikrotikTestRepository: MikroTikTestRepository
) : SpeedTestStep {
    override suspend fun run(context: TestExecutionContext): StepResult<SpeedTestData> {
        val serverAddress = context.client.speedTestServerAddress
        if (serverAddress.isNullOrBlank()) {
            return StepResult.Skipped(TestSkipReason.SPEED_NO_SERVER)
        }

        return try {
            val speedTestResult = mikrotikTestRepository.speedTest(
                probe = context.probeConfig,
                serverAddress = serverAddress,
                username = context.client.speedTestServerUser,
                password = context.client.speedTestServerPassword,
                duration = "5"
            )
            StepResult.Success(speedTestResult)
        } catch (e: SecurityException) {
            StepResult.Failed(TestError.AuthError(e.message ?: "Authentication failed"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            StepResult.Failed(TestError.NetworkError(e.message ?: "Speed test failed"))
        }
    }
}
