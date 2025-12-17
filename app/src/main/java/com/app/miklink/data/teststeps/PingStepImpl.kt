/*
 * Purpose: Execute ping tests for configured targets, resolving DHCP gateway placeholders and aggregating outcomes.
 * Inputs: Test execution context (client, profile, probe) and resolved ping targets.
 * Outputs: StepResult with per-target outcomes including packet loss and measurements.
 * Notes: Repository returns domain PingMeasurement items; this step focuses on orchestration and result evaluation.
 */
package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.data.repository.test.PingTargetResolver
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.model.TestSkipReason
import com.app.miklink.core.domain.test.step.PingStep
import javax.inject.Inject

/**
 * Implementazione di PingStep.
 * Usa MikroTikTestRepository per eseguire ping e PingTargetResolver per risolvere target.
 */
class PingStepImpl @Inject constructor(
    private val mikrotikTestRepository: MikroTikTestRepository,
    private val pingTargetResolver: PingTargetResolver
) : PingStep {
    override suspend fun run(context: TestExecutionContext): StepResult<List<PingTargetOutcome>> {
        val pingTargets = listOfNotNull(
            context.testProfile.pingTarget1,
            context.testProfile.pingTarget2,
            context.testProfile.pingTarget3
        ).filter { it.isNotBlank() }

        if (pingTargets.isEmpty()) {
            return StepResult.Skipped(TestSkipReason.PING_NO_TARGETS)
        }

        val outcomes = mutableListOf<PingTargetOutcome>()
        var allPingsPassed = true

        for (target in pingTargets) {
            try {
                // Risolvi target (gestisce DHCP_GATEWAY)
                val resolvedTarget = pingTargetResolver.resolve(
                    probe = context.probeConfig,
                    client = context.client,
                    profile = context.testProfile,
                    input = target
                )

                if (resolvedTarget.equals("DHCP_GATEWAY", ignoreCase = true)) {
                    // Gateway non risolto: skip questo target
                    outcomes.add(
                        PingTargetOutcome(
                            target = target,
                            resolved = null,
                            packetLoss = null,
                            results = emptyList(),
                            error = "Gateway DHCP non disponibile"
                        )
                    )
                    continue
                }

                val measurements = mikrotikTestRepository.ping(
                    probe = context.probeConfig,
                    target = resolvedTarget,
                    interfaceName = context.probeConfig.testInterface,
                    count = context.testProfile.pingCount
                )

                // Verifica packet loss per determinare se questo target ha avuto successo
                val lastResult = measurements.lastOrNull()
                val packetLoss = lastResult?.packetLoss ?: "100"
                val numericLoss = packetLoss.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 100.0
                if (numericLoss > 0.0) {
                    allPingsPassed = false
                }
                outcomes.add(
                    PingTargetOutcome(
                        target = target,
                        resolved = resolvedTarget,
                        packetLoss = packetLoss,
                        results = measurements,
                        error = null
                    )
                )
            } catch (e: Exception) {
                allPingsPassed = false
                outcomes.add(
                    PingTargetOutcome(
                        target = target,
                        resolved = null,
                        packetLoss = null,
                        results = emptyList(),
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }

        val hasValidTargets = outcomes.any { it.results.isNotEmpty() }
        return when {
            allPingsPassed && hasValidTargets -> StepResult.Success(outcomes)
            !hasValidTargets -> StepResult.Skipped(TestSkipReason.PING_NO_VALID_TARGETS)
            else -> StepResult.Failed(TestError.NetworkError("Alcuni ping sono falliti"), outcomes)
        }
    }
}
