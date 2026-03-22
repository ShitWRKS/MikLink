/*
 * Purpose: Execute cable-test (TDR) using MikroTikTestRepository and surface a domain summary.
 * Inputs: Test execution context carrying probe configuration and interface name.
 * Outputs: StepResult with CableTestSummary including parsed pairs/status.
 * Notes: Repository already provides domain mapping; this step only handles orchestration and error classification.
 */
package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.step.CableTestStep
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Implementazione di CableTestStep.
 * Usa MikroTikTestRepository per eseguire il test TDR.
 */
class CableTestStepImpl @Inject constructor(
    private val mikrotikTestRepository: MikroTikTestRepository
) : CableTestStep {
    override suspend fun run(context: TestExecutionContext): StepResult<CableTestSummary> {
        return try {
            val cableTestSummary = mikrotikTestRepository.cableTest(
                probe = context.probeConfig,
                interfaceName = context.probeConfig.testInterface,
                once = true
            )
            StepResult.Success(cableTestSummary)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Distinguere tra errori hardware vs errori temporanei
            val isUnsupported = e.message?.contains("non supportato", ignoreCase = true) == true
                    || e.message?.contains("unsupported", ignoreCase = true) == true
            if (isUnsupported) {
                StepResult.Failed(TestError.Unsupported(e.message ?: "TDR not supported"))
            } else {
                StepResult.Failed(TestError.NetworkError(e.message ?: "Cable test failed"))
            }
        }
    }
}
