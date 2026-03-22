/*
 * Purpose: Apply client network configuration through the MikroTik API before running tests.
 * Inputs: Test execution context (probe + client settings).
 * Outputs: NetworkConfigFeedback describing the applied configuration or failure reason.
 * Notes: Client override is intentionally not supported in the current single-probe flow.
 */
package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.NetworkConfigFeedback
import com.app.miklink.core.data.repository.test.NetworkConfigRepository
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.step.NetworkConfigStep
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Implementazione di NetworkConfigStep.
 * Usa NetworkConfigRepository per applicare la configurazione di rete.
 */
class NetworkConfigStepImpl @Inject constructor(
    private val networkConfigRepository: NetworkConfigRepository
) : NetworkConfigStep {
    override suspend fun run(context: TestExecutionContext): StepResult<NetworkConfigFeedback> {
        return try {
            val feedback = networkConfigRepository.applyClientNetworkConfig(
                probe = context.probeConfig,
                client = context.client,
                override = null // Override disabilitato nel flusso corrente single-probe
            )
            StepResult.Success(feedback)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            StepResult.Failed(TestError.NetworkError(e.message ?: "Network configuration failed"))
        }
    }
}
