/*
 * Purpose: Run the link status step using MikroTikTestRepository and return domain link status data.
 * Inputs: Test execution context containing probe configuration.
 * Outputs: StepResult wrapping LinkStatusData describing link status and rate.
 * Notes: Repository already maps DTOs to domain; this step only orchestrates flow control.
 */
package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.step.LinkStatusStep
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Implementazione di LinkStatusStep.
 * Usa MikroTikTestRepository per verificare lo stato del link.
 */
class LinkStatusStepImpl @Inject constructor(
    private val mikrotikTestRepository: MikroTikTestRepository
) : LinkStatusStep {
    override suspend fun run(context: TestExecutionContext): StepResult<LinkStatusData> {
        return try {
            val linkStatus = mikrotikTestRepository.monitorEthernet(
                probe = context.probeConfig,
                interfaceName = context.probeConfig.testInterface,
                once = true
            )
            StepResult.Success(linkStatus)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            StepResult.Failed(TestError.NetworkError(e.message ?: "Link status check failed"))
        }
    }
}
