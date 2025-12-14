package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.model.report.LinkStatusData
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.step.LinkStatusStep
import javax.inject.Inject

/**
 * Implementazione di LinkStatusStep.
 * Usa MikroTikTestRepository per verificare lo stato del link.
 */
class LinkStatusStepImpl @Inject constructor(
    private val mikrotikTestRepository: MikroTikTestRepository
) : LinkStatusStep {
    override suspend fun run(context: TestExecutionContext): StepResult<LinkStatusData> {
        return try {
            val monitorResponse = mikrotikTestRepository.monitorEthernet(
                probe = context.probeConfig,
                interfaceName = context.probeConfig.testInterface,
                once = true
            )
            StepResult.Success(
                LinkStatusData(
                    status = monitorResponse.status,
                    rate = monitorResponse.rate
                )
            )
        } catch (e: Exception) {
            StepResult.Failed(TestError.NetworkError(e.message ?: "Link status check failed"))
        }
    }
}

