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
            return StepResult.Failed(
                TestError.ConfigurationError("Speed test server is not configured")
            )
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
            StepResult.Failed(TestError.Authentication(e.message ?: "Authentication failed", e))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            StepResult.Failed(TestError.Unexpected(e.message ?: "Speed test failed", e))
        }
    }
}
