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

                val packetLoss = measurements.lastOrNull()?.packetLoss
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

        return if (outcomes.isNotEmpty()) {
            StepResult.Success(outcomes)
        } else {
            StepResult.Skipped(TestSkipReason.PING_NO_VALID_TARGETS)
        }
    }
}
