/*
 * Purpose: Verify MikroTikCallExecutor outcomes for HTTPS success and fallback scenarios.
 * Inputs: ProbeConfig instances, mocked MikroTikServiceProvider/ApiService, and simulated call lambdas.
 * Outputs: Assertions on CallOutcome meta fields and preserved failures when fallback exhausts.
 * Notes: Ensures HTTPS->HTTP fallback keeps both attempt errors per ADR-0002 transport policy.
 */
package com.app.miklink.data.remote.mikrotik.service

import com.app.miklink.core.domain.model.ProbeConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.net.ssl.SSLHandshakeException

class MikroTikCallExecutorTest {

    private val serviceProvider = mockk<MikroTikServiceProvider>()
    private val httpsApi = mockk<MikroTikApiService>()
    private val httpApi = mockk<MikroTikApiService>()
    private val executor = MikroTikCallExecutor(serviceProvider)

    private val httpsProbe = ProbeConfig(
        ipAddress = "10.0.0.1",
        username = "admin",
        password = "pass",
        testInterface = "ether1",
        isHttps = true,
        isOnline = false,
        modelName = null,
        tdrSupported = false
    )

    @Test
    fun `executeWithOutcome returns HTTPS success without fallback`() = runTest {
        mockLogs()
        every { serviceProvider.build(httpsProbe) } returns httpsApi

        val outcome = executor.executeWithOutcome(httpsProbe) { api ->
            assertEquals(httpsApi, api)
            "ok"
        }

        assertTrue(outcome is CallOutcome.Success)
        val success = outcome as CallOutcome.Success
        assertEquals("ok", success.value)
        assertTrue(success.meta.attemptedHttps)
        assertTrue(success.meta.effectiveIsHttps)
        assertFalse(success.meta.didFallbackToHttp)
    }

    @Test
    fun `executeWithOutcome falls back to HTTP after TLS handshake error`() = runTest {
        mockLogs()
        every { serviceProvider.build(match { it.isHttps }) } returns httpsApi
        every { serviceProvider.build(match { !it.isHttps }) } returns httpApi

        val outcome = executor.executeWithOutcome(httpsProbe) { api ->
            if (api == httpsApi) {
                throw SSLHandshakeException("protocol_version")
            }
            assertEquals(httpApi, api)
            "ok-over-http"
        }

        assertTrue(outcome is CallOutcome.Success)
        val success = outcome as CallOutcome.Success
        assertEquals("ok-over-http", success.value)
        assertTrue(success.meta.attemptedHttps)
        assertFalse(success.meta.effectiveIsHttps)
        assertTrue(success.meta.didFallbackToHttp)
    }

    @Test
    fun `executeWithOutcome preserves both HTTPS and HTTP failures when fallback fails`() = runTest {
        mockLogs()
        every { serviceProvider.build(match { it.isHttps }) } returns httpsApi
        every { serviceProvider.build(match { !it.isHttps }) } returns httpApi

        val outcome = executor.executeWithOutcome(httpsProbe) { api ->
            if (api == httpsApi) {
                throw SSLHandshakeException("protocol_version")
            }
            assertEquals(httpApi, api)
            throw IllegalStateException("http down")
        }

        assertTrue(outcome is CallOutcome.Failure)
        val failure = outcome as CallOutcome.Failure
        assertTrue(failure.meta.attemptedHttps)
        assertFalse(failure.meta.effectiveIsHttps)
        assertTrue(failure.meta.didFallbackToHttp)
        assertEquals(2, failure.failures.size)
        assertEquals("https", failure.failures[0].scheme)
        assertTrue(failure.failures[0].throwable is SSLHandshakeException)
        assertEquals("http", failure.failures[1].scheme)
        assertEquals("http down", failure.failures[1].throwable.message)
    }

    private fun mockLogs() {
        mockkStatic("android.util.Log")
        every { android.util.Log.isLoggable(any(), any()) } returns false
    }
}
