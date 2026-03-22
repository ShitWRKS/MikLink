/*
 * Purpose: Run LLDP/CDP neighbor discovery and surface domain neighbors.
 * Inputs: Test execution context (probe configuration with interface).
 * Outputs: StepResult carrying a list of NeighborData items or failure reason.
 * Notes: Repository returns domain neighbors; this step filters by user-selected protocols.
 */
package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.model.report.NeighborData
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Implementazione di NeighborDiscoveryStep.
 * Usa MikroTikTestRepository per eseguire discovery LLDP/CDP.
 * Filtra i neighbors in base ai protocolli selezionati dall'utente.
 */
class NeighborDiscoveryStepImpl @Inject constructor(
    private val mikrotikTestRepository: MikroTikTestRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : NeighborDiscoveryStep {
    override suspend fun run(context: TestExecutionContext): StepResult<List<NeighborData>> {
        return try {
            val enabledProtocols = userPreferencesRepository.neighborDiscoveryProtocols.first()
            val neighbors = mikrotikTestRepository.neighbors(
                probe = context.probeConfig,
                interfaceName = context.probeConfig.testInterface
            ).filter { neighbor ->
                neighbor.matchesAnyProtocol(enabledProtocols)
            }
            StepResult.Success(neighbors)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            StepResult.Failed(TestError.NetworkError(e.message ?: "Neighbor discovery failed"))
        }
    }

    private fun NeighborData.matchesAnyProtocol(enabledProtocols: Set<String>): Boolean {
        val protocols = discoveredBy?.split(",")?.map { it.trim().uppercase() } ?: return false
        val enabledUppercase = enabledProtocols.map { it.uppercase() }.toSet()
        return protocols.any { it in enabledUppercase }
    }
}
