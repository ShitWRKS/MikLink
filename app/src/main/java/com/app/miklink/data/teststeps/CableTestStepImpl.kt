package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.model.report.TdrEntry
import com.app.miklink.core.domain.test.model.CableTestSummary
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.step.CableTestStep
import javax.inject.Inject

/**
 * Implementazione di CableTestStep.
 * Usa MikroTikTestRepository per eseguire il test TDR.
 */
class CableTestStepImpl @Inject constructor(
    private val mikrotikTestRepository: MikroTikTestRepository
) : CableTestStep {
    override suspend fun run(context: TestExecutionContext): StepResult<CableTestSummary> {
        return try {
            val cableTestResult = mikrotikTestRepository.cableTest(
                probe = context.probeConfig,
                interfaceName = context.probeConfig.testInterface,
                once = true
            )
            StepResult.Success(cableTestResult.toSummary())
        } catch (e: Exception) {
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

private fun com.app.miklink.core.data.remote.mikrotik.dto.CableTestResult.toSummary(): CableTestSummary {
    val entries = cablePairs.orEmpty().mapNotNull { pair ->
        val distance = pair["distance"] ?: pair["len"] ?: pair["length"]
        val status = pair["status"] ?: pair["state"] ?: pair["result"]
        val description = pair["pair"] ?: pair["description"]
        if (distance == null && status == null && description == null) {
            null
        } else {
            TdrEntry(
                distance = distance,
                status = status,
                description = description
            )
        }
    }.ifEmpty {
        listOf(TdrEntry(status = status))
    }
    return CableTestSummary(status = status, entries = entries)
}

