/*
 * Purpose: Verify MikroTikCallExecutor outcomes for HTTPS success and fallback scenarios.
 * Inputs: ProbeConfig instances, mocked MikroTikServiceProvider/ApiService, and simulated call lambdas.
 * Outputs: Assertions on CallOutcome meta fields and preserved failures when fallback exhausts.
 * Notes: Ensures HTTPS->HTTP fallback keeps both attempt errors per ADR-0002 transport policy.
 */
package com.app.miklink.data.remote.mikrotik.service

import com.app.miklink.core.domain.model.ProbeConfig
import com.app.miklink.core.domain.model.TdrCapability
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CancellationException
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
        tdrCapability = TdrCapability.UNKNOWN
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

    @Test
    fun `executeWithOutcome rethrows cancellation instead of mapping it as call failure`() = runTest {
        mockLogs()
        every { serviceProvider.build(match { !it.isHttps }) } returns httpApi
        val probe = httpsProbe.copy(isHttps = false)

        val result = runCatching {
            executor.executeWithOutcome(probe) {
                throw CancellationException("job cancelled")
            }
        }

        assertTrue(result.exceptionOrNull() is CancellationException)
    }

    // === classify tests for TestExecutionException ===

    @Test
    fun `classify preserves TestError from TestExecutionException`() {
        val testErrors = listOf(
            com.app.miklink.core.domain.test.model.TestError.ProbeUnavailable("probe gone"),
            com.app.miklink.core.domain.test.model.TestError.Authentication("auth failed"),
            com.app.miklink.core.domain.test.model.TestError.Timeout("timed out"),
            com.app.miklink.core.domain.test.model.TestError.InvalidResponse("bad response"),
            com.app.miklink.core.domain.test.model.TestError.Unexpected("unexpected"),
            com.app.miklink.core.domain.test.model.TestError.RouterOsError("router err", code = 500)
        )

        for (expected in testErrors) {
            val exception = com.app.miklink.core.domain.test.model.TestExecutionException(expected)
            val classified = executor.classify(exception)
            assertEquals(
                "Expected ${expected::class.simpleName} for ${expected.message}",
                expected::class, classified::class
            )
            assertEquals(expected.message, classified.message)
        }
    }

    @Test
    fun `classify maps ConnectException to ProbeUnavailable`() {
        val classified = executor.classify(java.net.ConnectException("refused"))
        assertTrue("Expected ProbeUnavailable but got ${classified::class.simpleName}",
            classified is com.app.miklink.core.domain.test.model.TestError.ProbeUnavailable)
    }

    @Test
    fun `classify maps SocketTimeoutException to Timeout`() {
        val classified = executor.classify(java.net.SocketTimeoutException("timeout"))
        assertTrue(classified is com.app.miklink.core.domain.test.model.TestError.Timeout)
    }

    @Test(expected = CancellationException::class)
    fun `classify rethrows CancellationException`() {
        executor.classify(CancellationException("cancelled"))
    }

    private fun mockLogs() {
        mockkStatic("android.util.Log")
        every { android.util.Log.isLoggable(any(), any()) } returns false
    }
}
