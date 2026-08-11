package com.app.miklink.core.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestThresholdsValidatorTest {
    @Test
    fun `speed throughput is bounded from zero through 100G`() {
        assertTrue(TestThresholdsValidator.isValidSpeedThroughput(0.0))
        assertTrue(TestThresholdsValidator.isValidSpeedThroughput(100_000.0))
        assertFalse(TestThresholdsValidator.isValidSpeedThroughput(-0.01))
        assertFalse(TestThresholdsValidator.isValidSpeedThroughput(100_000.01))
        assertFalse(TestThresholdsValidator.isValidSpeedThroughput(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `blank throughput input remains valid for default resolution`() {
        assertTrue(TestThresholdsValidator.isValidSpeedThroughputInput(""))
        assertTrue(TestThresholdsValidator.isValidSpeedThroughputInput("100000"))
        assertFalse(TestThresholdsValidator.isValidSpeedThroughputInput("100001"))
    }
}
