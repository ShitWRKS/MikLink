/*
 * Purpose: Guard pingDetails aggregation to ensure UI contracts (Packet Loss chip, RTT metrics, per-target lines) remain stable.
 * Inputs: Synthetic PingTargetOutcome lists with packet loss, RTT values, and errors to exercise formatting/aggregation logic.
 * Outputs: Assertions on pingDetailsSummary map content for loss key presence, non-zero handling, and per-target entries.
 * Notes: Tests internal helper directly to keep coverage deterministic without altering RunTestUseCaseImpl visibility.
 */
package com.app.miklink.core.domain.usecase.test

import com.app.miklink.core.domain.test.model.PingMeasurement
import com.app.miklink.core.domain.test.model.PingTargetOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PingDetailsContractTest {

    @Test
    fun `details include Packet Loss chip data when loss is zero`() {
        val outcomes = listOf(
            outcome(
                target = "8.8.8.8",
                packetLoss = "0",
                min = "10ms",
                avg = "12ms",
                max = "15ms"
            )
        )

        val details = pingDetailsSummary(outcomes)

        assertEquals("0%", details["Packet Loss"])
    }

    @Test
    fun `details surface non-zero packet loss`() {
        val outcomes = listOf(
            outcome(
                target = "1.1.1.1",
                packetLoss = "25",
                min = "20ms",
                avg = "22ms",
                max = "25ms"
            )
        )

        val details = pingDetailsSummary(outcomes)

        assertEquals("25%", details["Packet Loss"])
    }

    @Test
    fun `details include per-target breakdown line`() {
        val outcomes = listOf(
            outcome(
                target = "9.9.9.9",
                packetLoss = "0",
                min = "5ms",
                avg = "6ms",
                max = "7ms"
            )
        )

        val details = pingDetailsSummary(outcomes)

        assertTrue(details.keys.any { it == "Target 9.9.9.9" })
    }

    private fun outcome(
        target: String,
        packetLoss: String,
        min: String,
        avg: String,
        max: String
    ): PingTargetOutcome {
        val measurement = PingMeasurement(
            host = target,
            minRtt = min,
            avgRtt = avg,
            maxRtt = max,
            packetLoss = packetLoss,
            sent = "4",
            received = "4",
            seq = null,
            time = null,
            ttl = null,
            size = null
        )
        return PingTargetOutcome(
            target = target,
            resolved = target,
            packetLoss = packetLoss,
            results = listOf(measurement),
            error = null
        )
    }
}
