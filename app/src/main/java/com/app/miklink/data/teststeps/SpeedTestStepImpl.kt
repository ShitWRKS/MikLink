package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.domain.model.report.SpeedTestData
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestError
import com.app.miklink.core.domain.test.model.TestExecutionContext
import com.app.miklink.core.domain.test.model.TestSkipReason
import com.app.miklink.core.domain.test.step.SpeedTestStep
import javax.inject.Inject

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
            return StepResult.Skipped(TestSkipReason.SPEED_NO_SERVER)
        }

        return try {
            val speedTestResult = mikrotikTestRepository.speedTest(
                probe = context.probeConfig,
                serverAddress = serverAddress,
                username = context.client.speedTestServerUser,
                password = context.client.speedTestServerPassword,
                duration = "5"
            )
            StepResult.Success(speedTestResult.toDomain(serverAddress))
        } catch (e: SecurityException) {
            StepResult.Failed(TestError.AuthError(e.message ?: "Authentication failed"))
        } catch (e: Exception) {
            StepResult.Failed(TestError.NetworkError(e.message ?: "Speed test failed"))
        }
    }
}

private fun com.app.miklink.data.remote.mikrotik.dto.SpeedTestResult.toDomain(serverAddress: String?): SpeedTestData {
    return SpeedTestData(
        status = status,
        ping = ping,
        jitter = jitter,
        loss = loss,
        tcpDownload = tcpDownload,
        tcpUpload = tcpUpload,
        udpDownload = udpDownload,
        udpUpload = udpUpload,
        warning = warning,
        serverAddress = serverAddress
    )
}

