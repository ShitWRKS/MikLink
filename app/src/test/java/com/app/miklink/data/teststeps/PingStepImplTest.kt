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
import kotlinx.coroutines.CancellationException
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

    @Test
    fun `run continues with sibling targets when one ping target fails`() = runTest {
        val profile = mockk<TestProfile>(relaxed = true) {
            every { pingTarget1 } returns "8.8.8.8"
            every { pingTarget2 } returns "1.1.1.1"
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

        coEvery { resolver.resolve(any(), any(), any(), "8.8.8.8") } returns "8.8.8.8"
        coEvery { resolver.resolve(any(), any(), any(), "1.1.1.1") } returns "1.1.1.1"
        coEvery { repository.ping(any(), "8.8.8.8", any(), any()) } throws IllegalStateException("timeout")
        coEvery { repository.ping(any(), "1.1.1.1", any(), any()) } returns emptyList()

        val result = step.run(context)

        assertTrue(result is StepResult.Success)
        val outcomes = (result as StepResult.Success).data
        assertEquals(2, outcomes.size)
        assertEquals("timeout", outcomes[0].error)
        assertEquals("8.8.8.8", outcomes[0].resolved)
        assertEquals(null, outcomes[1].error)
        assertEquals("1.1.1.1", outcomes[1].resolved)
    }

    @Test
    fun `run maps ping exception deterministically when message is missing`() = runTest {
        val profile = mockk<TestProfile>(relaxed = true) {
            every { pingTarget1 } returns "8.8.8.8"
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

        coEvery { resolver.resolve(any(), any(), any(), "8.8.8.8") } returns "8.8.8.8"
        coEvery { repository.ping(any(), "8.8.8.8", any(), any()) } throws IllegalStateException()

        val result = step.run(context)

        assertTrue(result is StepResult.Success)
        val outcome = (result as StepResult.Success).data.single()
        assertEquals("IllegalStateException", outcome.error)
    }

    @Test
    fun `run propagates cancellation`() = runTest {
        val profile = mockk<TestProfile>(relaxed = true) {
            every { pingTarget1 } returns "8.8.8.8"
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

        coEvery { resolver.resolve(any(), any(), any(), "8.8.8.8") } returns "8.8.8.8"
        coEvery { repository.ping(any(), "8.8.8.8", any(), any()) } throws CancellationException("cancelled")

        val result = runCatching { step.run(context) }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }
}
