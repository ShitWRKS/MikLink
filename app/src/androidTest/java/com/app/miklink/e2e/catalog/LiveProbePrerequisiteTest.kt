package com.app.miklink.e2e.catalog

import com.app.miklink.core.domain.model.TdrCapability
import com.app.miklink.e2e.support.PrerequisiteEvaluator
import com.app.miklink.e2e.support.PrerequisiteStatus
import com.app.miklink.e2e.support.TestFixtureManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LiveProbePrerequisiteTest {
    @Test
    fun missingConfiguredProbeIsNotRunAndNeverReplacedByFallback() {
        val result = PrerequisiteEvaluator.configuredProbe(null)
        assertEquals(PrerequisiteStatus.UNAVAILABLE, result.status)
        assertEquals("PROBE_NOT_CONFIGURED", result.reasonCode)
    }

    @Test
    fun reachabilityAuthenticationCapabilityAndSpeedAreIndependent() {
        val probe = com.app.miklink.core.domain.model.ProbeConfig(
            ipAddress = "192.0.2.1",
            username = "operator",
            password = "secret",
            testInterface = "ether1",
            isHttps = true,
            isOnline = false,
            modelName = null,
            tdrCapability = TdrCapability.UNSUPPORTED
        )
        val client = TestFixtureManager.defaultClient("prereq")
        assertEquals(PrerequisiteStatus.UNAVAILABLE, PrerequisiteEvaluator.reachableProbe(probe).status)
        assertEquals("PROBE_AUTHENTICATION_FAILED", PrerequisiteEvaluator.authenticatedProbe(false).reasonCode)
        assertEquals(PrerequisiteStatus.NOT_APPLICABLE, PrerequisiteEvaluator.tdrCapability(probe).status)
        assertEquals(PrerequisiteStatus.UNAVAILABLE, PrerequisiteEvaluator.speedServer(client).status)
        assertNotNull(PrerequisiteEvaluator.requireAvailable(listOf(PrerequisiteEvaluator.reachableProbe(probe))))
    }
}
