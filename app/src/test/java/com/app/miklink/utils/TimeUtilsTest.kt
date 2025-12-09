package com.app.miklink.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test unitari per TimeUtils
 */
class TimeUtilsTest {

    @Test
    fun `normalizeTime con formato millisecondo-microsecondo`() {
        assertEquals("21ms", normalizeTime("21ms30us"))
        assertEquals("17ms", normalizeTime("17ms341us"))
        assertEquals("18ms", normalizeTime("18ms957us"))
        assertEquals("19ms", normalizeTime("19ms185us"))
    }

    @Test
    fun `normalizeTime con solo millisecondi`() {
        assertEquals("25ms", normalizeTime("25ms"))
        assertEquals("100ms", normalizeTime("100ms"))
        assertEquals("5ms", normalizeTime("5ms"))
    }

    @Test
    fun `normalizeTime con secondi`() {
        assertEquals("1000ms", normalizeTime("1s"))
        assertEquals("2000ms", normalizeTime("2s"))
        assertEquals("1500ms", normalizeTime("1s500ms"))
        assertEquals("3200ms", normalizeTime("3s200ms"))
    }

    @Test
    fun `normalizeTime con secondi e microsecondi (ignora microsecondi)`() {
        // I microsecondi vengono ignorati, manteniamo solo la parte significativa
        assertEquals("10000ms", normalizeTime("10s284us"))
        assertEquals("1000ms", normalizeTime("1s500us"))
    }

    @Test
    fun `normalizeTime con solo microsecondi restituisce 0ms`() {
        // Valori sub-millisecondo (solo microsecondi) → "0ms"
        assertEquals("0ms", normalizeTime("12us"))
        assertEquals("0ms", normalizeTime("500us"))
        assertEquals("0ms", normalizeTime("1us"))
        assertEquals("0ms", normalizeTime("999us"))
    }

    @Test
    fun `normalizeTime con 0ms e microsecondi restituisce 0ms`() {
        // Gateway locale: 0ms + microsecondi → "0ms"
        assertEquals("0ms", normalizeTime("0ms12us"))
        assertEquals("0ms", normalizeTime("0ms500us"))
        assertEquals("0ms", normalizeTime("0ms1us"))
    }

    @Test
    fun `normalizeTime con minuti`() {
        assertEquals("60000ms", normalizeTime("1m"))
        assertEquals("120000ms", normalizeTime("2m"))
        assertEquals("90000ms", normalizeTime("1m30s"))
    }

    @Test
    fun `normalizeTime con valore null`() {
        assertEquals("N/A", normalizeTime(null))
    }

    @Test
    fun `normalizeTime con stringa vuota`() {
        assertEquals("N/A", normalizeTime(""))
        assertEquals("N/A", normalizeTime("   "))
    }

    @Test
    fun `normalizeTime con formato non riconosciuto`() {
        // Se non riconosce il formato, restituisce il valore originale
        assertEquals("invalid", normalizeTime("invalid"))
        assertEquals("xyz", normalizeTime("xyz"))
    }

    @Test
    fun `normalizeTime casi reali da MikroTik API`() {
        // Basato sui dati reali dal curl test
        assertEquals("21ms", normalizeTime("21ms30us"))
        assertEquals("19ms", normalizeTime("19ms185us"))
        assertEquals("18ms", normalizeTime("18ms957us"))
        assertEquals("17ms", normalizeTime("17ms341us"))
        assertEquals("18ms", normalizeTime("18ms501us"))
        assertEquals("18ms", normalizeTime("18ms527us"))
    }
}
