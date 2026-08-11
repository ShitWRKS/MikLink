package com.app.miklink.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestProfileThresholdPreviewTest {
    private val presets = listOf(10.0, 100.0, 1_000.0, 2_500.0, 5_000.0, 10_000.0, 25_000.0, 40_000.0, 50_000.0, 100_000.0)

    @Test
    fun `link scale keeps custom values while marking out of preset range`() {
        val within = requireNotNull(linkScaleMarker(2_000.0, presets))
        val above = requireNotNull(linkScaleMarker(250_000.0, presets))

        assertFalse(within.outsidePresetRange)
        assertTrue(within.fraction in 0f..1f)
        assertTrue(above.outsidePresetRange)
        assertEquals(1f, above.fraction)
    }

    @Test
    fun `link scale is logarithmic across rate magnitudes`() {
        val oneHundred = requireNotNull(linkScaleMarker(100.0, presets))
        val oneThousand = requireNotNull(linkScaleMarker(1_000.0, presets))

        assertTrue(oneHundred.fraction > 0f)
        assertTrue(oneThousand.fraction > oneHundred.fraction)
    }

    @Test
    fun `RTT series is deterministic and omits absent references`() {
        assertEquals(emptyList<Double>(), illustrativeRttSeries(null, null))
        assertEquals(illustrativeRttSeries(30.0, 50.0), illustrativeRttSeries(30.0, 50.0))
        assertEquals(7, illustrativeRttSeries(null, 50.0).size)
    }

    @Test
    fun `throughput display maximum derives from effective values without limiting them`() {
        val displayMaximum = throughputDisplayMaximum(250.0, 100.0)

        assertTrue(displayMaximum > 250.0)
        assertEquals(1.0, throughputDisplayMaximum(null, null), 0.0)
    }

    @Test
    fun `adaptive slider range grows beyond current values without becoming a domain limit`() {
        assertEquals(100.0, adaptiveSliderMaximum(listOf(30.0, 50.0)), 0.0)
        assertEquals(500.0, adaptiveSliderMaximum(listOf(120.0, 200.0)), 0.0)
        assertEquals(1.0, adaptiveSliderMaximum(listOf(null, 0.0)), 0.0)
    }

    @Test
    fun `slider values round trip as validator friendly input`() {
        assertEquals("35", sliderInputValue(35f))
        assertEquals("35.25", sliderInputValue(35.25f))
    }

    @Test
    fun `throughput slider has stable logarithmic endpoints and round trips values`() {
        assertEquals(0f, throughputToSliderFraction(0.0))
        assertEquals(1f, throughputToSliderFraction(100_000.0))

        listOf(10.0, 100.0, 1_000.0, 10_000.0, 100_000.0).forEach { value ->
            assertEquals(
                value,
                sliderFractionToThroughput(throughputToSliderFraction(value)),
                value * 0.000_01 + 0.01
            )
        }
    }
}
