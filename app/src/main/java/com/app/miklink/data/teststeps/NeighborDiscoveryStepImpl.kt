package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import javax.inject.Inject

/**
 * Implementazione di NeighborDiscoveryStep.
 * Usa MikroTikTestRepository per eseguire discovery LLDP/CDP.
 */
class NeighborDiscoveryStepImpl @Inject constructor(
    private val mikrotikTestRepository: MikroTikTestRepository
) : NeighborDiscoveryStep {
    override suspend fun run(context: TestExecutionContext): StepResult {
        return try {
            val neighbors = mikrotikTestRepository.neighbors(
                probe = context.probeConfig,
                interfaceName = context.probeConfig.testInterface
            )
            // Restituisce il primo neighbor o lista vuota (non è un errore se non ci sono neighbor)
            StepResult.Success(neighbors)
        } catch (e: Exception) {
            StepResult.Failed(TestError.NetworkError(e.message ?: "Neighbor discovery failed"))
        }
    }
}

