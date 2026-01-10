package com.app.miklink.data.teststeps

import com.app.miklink.core.data.repository.test.MikroTikTestRepository
import com.app.miklink.core.data.repository.test.PingTargetResolver
import com.app.miklink.core.domain.model.Client
import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TestProfile
import com.app.miklink.core.domain.test.model.StepResult
import com.app.miklink.core.domain.test.model.TestExecutionContext
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PingStepImplTest {
    private val repository: MikroTikTestRepository = mockk()
    private val resolver: PingTargetResolver = mockk()
    private val step = PingStepImpl(repository, resolver)

    @Test
    fun `run returns Success with error when DHCP_GATEWAY is unresolved`() = runTest {
        // Arrange
        val profile = mockk<TestProfile>(relaxed = true) {
            every { pingTarget1 } returns "DHCP_GATEWAY"
            every { pingTarget2 } returns null
            every { pingTarget3 } returns null
            every { pingCount } returns 1
        }
        val config = mockk<ProbeConfig>(relaxed = true) {
            every { testInterface } returns "ether1"
        }
        val client = mockk<Client>(relaxed = true)
        
        val context = TestExecutionContext(
            client = client,
            probeConfig = config,
            testProfile = profile,
            socketId = "S1",
            notes = null
        )

        // Resolver returns "DHCP_GATEWAY" literally, indicating failure to resolve to IP
        coEvery { resolver.resolve(any(), any(), any(), "DHCP_GATEWAY") } returns "DHCP_GATEWAY"

        // Act
        val result = step.run(context)

        // Assert
        assertTrue("Expected Success but got $result", result is StepResult.Success)
        val outcomes = (result as StepResult.Success).data
        assertEquals(1, outcomes.size)
        val outcome = outcomes[0]
        assertEquals("DHCP_GATEWAY", outcome.target)
        assertEquals("Gateway DHCP non disponibile", outcome.error)
    }
}
