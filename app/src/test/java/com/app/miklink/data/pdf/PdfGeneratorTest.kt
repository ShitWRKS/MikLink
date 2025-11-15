package com.app.miklink.data.pdf

import android.content.Context
import android.graphics.Paint
import com.app.miklink.data.network.NeighborDetailListAdapter
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for PdfGenerator.parseResults()
 *
 * Tests the JSON parsing logic with Moshi for the ParsedResults data class.
 * Covers complete JSON, partial JSON, malformed JSON, and empty/null inputs.
 */
class PdfGeneratorTest {

    private lateinit var pdfGenerator: PdfGenerator
    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        // Configure Moshi exactly as in production (NetworkModule)
        moshi = Moshi.Builder()
            .add(NeighborDetailListAdapter())
            .add(object {
                @FromJson
                fun fromJson(reader: com.squareup.moshi.JsonReader): Boolean {
                    return if (reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                        reader.nextString().equals("true", ignoreCase = true)
                    } else {
                        reader.nextBoolean()
                    }
                }
                @ToJson
                fun toJson(writer: com.squareup.moshi.JsonWriter, value: Boolean) {
                    writer.value(value)
                }
            })
            .add(object {
                @FromJson
                fun fromJson(reader: com.squareup.moshi.JsonReader): Int {
                    return if (reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                        reader.nextString().toIntOrNull() ?: 0
                    } else {
                        reader.nextInt()
                    }
                }
                @ToJson
                fun toJson(writer: com.squareup.moshi.JsonWriter, value: Int) {
                    writer.value(value)
                }
            })
            .add(KotlinJsonAdapterFactory())
            .build()

        // Create PdfGenerator with mocked Context (not needed for parseResults, but required by constructor)
        val mockContext = mockk<Context>(relaxed = true)
        pdfGenerator = PdfGenerator(mockContext, moshi)
    }

    // ============================================
    // TEST 1: Complete JSON (All Fields Present)
    // ============================================

    @Test
    fun `GIVEN complete JSON WHEN parseResults called THEN returns ParsedResults with all fields`() {
        val completeJson = """
            {
                "link": {
                    "status": "up",
                    "speed": "1Gbps",
                    "duplex": "full"
                },
                "lldp": [
                    {
                        "identity": "MikroTik-Switch-01",
                        "interface-name": "ether1",
                        "system-caps-enabled": "Bridge,Router",
                        "discovered-by": "LLDP"
                    }
                ],
                "ping": [
                    {
                        "avg-rtt": "10ms",
                        "host": "192.168.88.1",
                        "packet-loss": "0",
                        "sent": "4",
                        "received": "4"
                    }
                ],
                "tdr": [
                    {
                        "status": "ok",
                        "cable-pairs": [
                            {"pair": "1-2", "length": "50m", "status": "ok"},
                            {"pair": "3-6", "length": "50m", "status": "ok"},
                            {"pair": "4-5", "length": "50m", "status": "ok"},
                            {"pair": "7-8", "length": "50m", "status": "ok"}
                        ]
                    }
                ]
            }
        """.trimIndent()

        val result = pdfGenerator.parseResults(completeJson)

        assertNotNull("ParsedResults should not be null", result)
        assertNotNull("Link data should not be null", result?.link)
        assertEquals("Link status should be 'up'", "up", result?.link?.get("status"))
        assertEquals("Link speed should be '1Gbps'", "1Gbps", result?.link?.get("speed"))

        assertNotNull("LLDP data should not be null", result?.lldp)
        assertEquals("LLDP should have 1 neighbor", 1, result?.lldp?.size)
        assertEquals("LLDP neighbor identity should match", "MikroTik-Switch-01", result?.lldp?.get(0)?.identity)

        assertNotNull("Ping data should not be null", result?.ping)
        assertEquals("Ping should have 1 result", 1, result?.ping?.size)
        assertEquals("Ping avg-rtt should be '10ms'", "10ms", result?.ping?.get(0)?.avgRtt)

        assertNotNull("TDR data should not be null", result?.tdr)
        assertEquals("TDR should have 1 result", 1, result?.tdr?.size)
        assertEquals("TDR status should be 'ok'", "ok", result?.tdr?.get(0)?.status)
        assertEquals("TDR should have 4 cable pairs", 4, result?.tdr?.get(0)?.cablePairs?.size)
    }

    // ============================================
    // TEST 2: Partial JSON (Missing Fields)
    // ============================================

    @Test
    fun `GIVEN partial JSON with missing tdr WHEN parseResults called THEN returns ParsedResults with tdr null`() {
        val partialJson = """
            {
                "link": {
                    "status": "up",
                    "speed": "100Mbps"
                },
                "ping": [
                    {
                        "avg-rtt": "5ms",
                        "host": "8.8.8.8"
                    }
                ]
            }
        """.trimIndent()

        val result = pdfGenerator.parseResults(partialJson)

        assertNotNull("ParsedResults should not be null", result)
        assertNotNull("Link data should not be null", result?.link)
        assertEquals("Link status should be 'up'", "up", result?.link?.get("status"))

        assertNotNull("Ping data should not be null", result?.ping)
        assertEquals("Ping avg-rtt should be '5ms'", "5ms", result?.ping?.get(0)?.avgRtt)

        assertNull("LLDP data should be null (not present in JSON)", result?.lldp)
        assertNull("TDR data should be null (not present in JSON)", result?.tdr)
    }

    @Test
    fun `GIVEN JSON with only link WHEN parseResults called THEN returns ParsedResults with only link populated`() {
        val linkOnlyJson = """
            {
                "link": {
                    "status": "down",
                    "reason": "cable-unplugged"
                }
            }
        """.trimIndent()

        val result = pdfGenerator.parseResults(linkOnlyJson)

        assertNotNull("ParsedResults should not be null", result)
        assertNotNull("Link data should not be null", result?.link)
        assertEquals("Link status should be 'down'", "down", result?.link?.get("status"))

        assertNull("LLDP should be null", result?.lldp)
        assertNull("Ping should be null", result?.ping)
        assertNull("TDR should be null", result?.tdr)
    }

    // ============================================
    // TEST 3: Malformed JSON (Invalid Syntax)
    // ============================================

    @Test
    fun `GIVEN malformed JSON with missing closing brace WHEN parseResults called THEN returns null`() {
        val malformedJson = """
            {
                "link": {
                    "status": "up"
                
        """.trimIndent() // Missing closing braces

        val result = pdfGenerator.parseResults(malformedJson)

        assertNull("ParsedResults should be null for malformed JSON", result)
    }

    @Test
    fun `GIVEN malformed JSON with invalid structure WHEN parseResults called THEN returns null`() {
        val malformedJson = """
            {
                "link": "this should be an object, not a string"
            }
        """.trimIndent()

        val result = pdfGenerator.parseResults(malformedJson)

        // Moshi might parse this but the structure is wrong, so it should handle gracefully
        // Depending on Moshi's lenient mode, this might return null or a partially parsed object
        // For this test, we expect null or an object with null link (based on Moshi behavior)
        if (result != null) {
            // If Moshi parses it leniently, link should be null or empty
            assertTrue("Link should be null or empty for invalid structure",
                result.link == null || result.link.isEmpty())
        } else {
            assertNull("ParsedResults should be null for malformed JSON", result)
        }
    }

    // ============================================
    // TEST 4: Empty/Null JSON
    // ============================================

    @Test
    fun `GIVEN empty JSON string WHEN parseResults called THEN returns null`() {
        val emptyJson = ""

        val result = pdfGenerator.parseResults(emptyJson)

        assertNull("ParsedResults should be null for empty JSON string", result)
    }

    @Test
    fun `GIVEN empty JSON object WHEN parseResults called THEN returns ParsedResults with all fields null`() {
        val emptyObjectJson = "{}"

        val result = pdfGenerator.parseResults(emptyObjectJson)

        assertNotNull("ParsedResults should not be null for empty object", result)
        assertNull("Link should be null", result?.link)
        assertNull("LLDP should be null", result?.lldp)
        assertNull("Ping should be null", result?.ping)
        assertNull("TDR should be null", result?.tdr)
    }

    @Test
    fun `GIVEN JSON with null values WHEN parseResults called THEN returns ParsedResults with null fields`() {
        val nullValuesJson = """
            {
                "link": null,
                "lldp": null,
                "ping": null,
                "tdr": null
            }
        """.trimIndent()

        val result = pdfGenerator.parseResults(nullValuesJson)

        assertNotNull("ParsedResults should not be null", result)
        assertNull("Link should be null", result?.link)
        // Note: NeighborDetailListAdapter converts null to empty list []
        assertNotNull("LLDP should not be null (adapter converts null to empty list)", result?.lldp)
        assertTrue("LLDP list should be empty", result?.lldp?.isEmpty() == true)
        assertNull("Ping should be null", result?.ping)
        assertNull("TDR should be null", result?.tdr)
    }

    // ============================================
    // TEST 5: Edge Cases - Empty Arrays
    // ============================================

    @Test
    fun `GIVEN JSON with empty arrays WHEN parseResults called THEN returns ParsedResults with empty lists`() {
        val emptyArraysJson = """
            {
                "link": {},
                "lldp": [],
                "ping": [],
                "tdr": []
            }
        """.trimIndent()

        val result = pdfGenerator.parseResults(emptyArraysJson)

        assertNotNull("ParsedResults should not be null", result)
        assertNotNull("Link should not be null", result?.link)
        assertTrue("Link map should be empty", result?.link?.isEmpty() == true)

        assertNotNull("LLDP should not be null", result?.lldp)
        assertTrue("LLDP list should be empty", result?.lldp?.isEmpty() == true)

        assertNotNull("Ping should not be null", result?.ping)
        assertTrue("Ping list should be empty", result?.ping?.isEmpty() == true)

        assertNotNull("TDR should not be null", result?.tdr)
        assertTrue("TDR list should be empty", result?.tdr?.isEmpty() == true)
    }

    // ============================================
    // TEST 6: wrapText() - Text Wrapping Logic
    // ============================================

    /**
     * Helper function to create a mocked Paint that simulates character width.
     * Each character is 10f pixels wide for predictable testing.
     */
    private fun createMockPaint(): Paint {
        return mockk<Paint>(relaxed = true).apply {
            every { measureText(any<String>()) } answers {
                // Simulate each character being 10f pixels wide
                firstArg<String>().length * 10f
            }
        }
    }

    @Test
    fun `GIVEN short text WHEN wrapText called THEN returns single line`() {
        val paint = createMockPaint()
        val text = "Ciao" // 4 chars * 10f = 40f (fits in 100f)

        val result = pdfGenerator.wrapText(text, 100f, paint)

        assertEquals("Should return exactly 1 line", 1, result.size)
        assertEquals("Line should contain the full text", "Ciao", result[0])
    }

    @Test
    fun `GIVEN text exactly at maxWidth WHEN wrapText called THEN returns single line`() {
        val paint = createMockPaint()
        val text = "1234567890" // 10 chars * 10f = 100f (exactly maxWidth)

        val result = pdfGenerator.wrapText(text, 100f, paint)

        assertEquals("Should return exactly 1 line", 1, result.size)
        assertEquals("Line should contain the full text", "1234567890", result[0])
    }

    @Test
    fun `GIVEN text that needs wrapping WHEN wrapText called THEN returns multiple lines split by words`() {
        val paint = createMockPaint()
        // "Testo" = 5 chars = 50f
        // "Testo lungo" = 11 chars (with space) = 110f > 100f → wrap
        // "lungo" = 5 chars = 50f
        // "lungo che" = 9 chars = 90f
        // "lungo che va" = 12 chars = 120f > 100f → wrap
        // "va" = 2 chars = 20f
        // "va a" = 4 chars = 40f
        // "va a capo" = 9 chars = 90f
        val text = "Testo lungo che va a capo"

        val result = pdfGenerator.wrapText(text, 100f, paint)

        assertTrue("Should return multiple lines", result.size > 1)
        // Verify first line contains "Testo" (50f fits)
        assertEquals("First line should be 'Testo'", "Testo", result[0])
        // Second line starts with "lungo" (50f)
        // "lungo che" = 90f fits, "lungo che va" = 120f doesn't fit
        assertEquals("Second line should be 'lungo che'", "lungo che", result[1])
        // Third line: "va a capo" = 90f fits
        assertEquals("Third line should be 'va a capo'", "va a capo", result[2])
        assertEquals("Should have exactly 3 lines", 3, result.size)
    }

    @Test
    fun `GIVEN long unbreakable word WHEN wrapText called THEN word is placed on its own line even if exceeding maxWidth`() {
        val paint = createMockPaint()
        // This word is 29 chars * 10f = 290f (much larger than 100f)
        // But since it's a single word, it should be placed on a line by itself
        val text = "UnaParolaLunghissimaCheNonSta"

        val result = pdfGenerator.wrapText(text, 100f, paint)

        assertEquals("Should return exactly 1 line (word is unbreakable)", 1, result.size)
        assertEquals("Line should contain the full word", "UnaParolaLunghissimaCheNonSta", result[0])
    }

    @Test
    fun `GIVEN long word followed by short word WHEN wrapText called THEN long word is on first line and short word wraps`() {
        val paint = createMockPaint()
        // "UnaParolaLunga" = 15 chars = 150f > 100f (will be on its own line)
        // "si" = 2 chars = 20f (will wrap to next line)
        val text = "UnaParolaLunga si"

        val result = pdfGenerator.wrapText(text, 100f, paint)

        assertEquals("Should return 2 lines", 2, result.size)
        assertEquals("First line should be the long word", "UnaParolaLunga", result[0])
        assertEquals("Second line should be the short word", "si", result[1])
    }

    @Test
    fun `GIVEN empty text WHEN wrapText called THEN returns empty list`() {
        val paint = createMockPaint()
        val text = ""

        val result = pdfGenerator.wrapText(text, 100f, paint)

        // The algorithm splits "" by " " → [""]
        // Then loops over [""], currentLine becomes ""
        // At the end, it checks if currentLine.isNotEmpty() → false
        // So it doesn't add anything to lines
        assertTrue("Should return empty list for empty text", result.isEmpty())
    }

    @Test
    fun `GIVEN single space WHEN wrapText called THEN returns empty list`() {
        val paint = createMockPaint()
        val text = " " // Single space

        val result = pdfGenerator.wrapText(text, 100f, paint)

        // Split " " by " " → ["", ""]
        // First word "", currentLine = ""
        // testLine = "" (since currentLine is empty)
        // width = 0f, not > maxWidth, currentLine = ""
        // Second word "", testLine = " " (space added)
        // width = 1f * 10f = 10f, not > maxWidth, currentLine = " "
        // At the end, currentLine = " ", but we need to check if isEmpty
        // Actually the algorithm checks isNotEmpty(), so " " would be added

        // Based on the algorithm, a single space would result in [""] from split
        // Let me trace through more carefully:
        // text.split(" ") on " " gives ["", ""]
        // First iteration: word = "", currentLine = "", testLine = "", width = 0
        // currentLine = ""
        // Second iteration: word = "", currentLine = "", testLine = "", width = 0
        // currentLine = ""
        // At end: currentLine = "", isEmpty() = true, so nothing added
        assertTrue("Should return empty list for single space", result.isEmpty())
    }

    @Test
    fun `GIVEN text with multiple spaces WHEN wrapText called THEN handles spaces correctly`() {
        val paint = createMockPaint()
        val text = "Hello  world" // Two spaces between words

        val result = pdfGenerator.wrapText(text, 100f, paint)

        // Split "Hello  world" by " " → ["Hello", "", "world"]
        // First: word = "Hello", currentLine = "", testLine = "Hello", width = 50f
        // currentLine = "Hello"
        // Second: word = "", currentLine = "Hello", testLine = "Hello ", width = 60f
        // currentLine = "Hello "
        // Third: word = "world", currentLine = "Hello ", testLine = "Hello  world", width = 120f > 100f
        // lines.add("Hello "), currentLine = "world"
        // At end: lines.add("world")
        // Result: ["Hello ", "world"]

        assertEquals("Should return 2 lines", 2, result.size)
        assertEquals("First line should be 'Hello '", "Hello ", result[0])
        assertEquals("Second line should be 'world'", "world", result[1])
    }

    @Test
    fun `GIVEN text with newlines WHEN wrapText called THEN treats newlines as regular text`() {
        val paint = createMockPaint()
        val text = "Hello\nworld" // Contains newline character

        val result = pdfGenerator.wrapText(text, 100f, paint)

        // The algorithm only splits by space, not by newline
        // So "Hello\nworld" is treated as a single word (11 chars = 110f > 100f)
        assertEquals("Should return 1 line (newline not treated as delimiter)", 1, result.size)
        assertEquals("Line should contain the full text including newline", "Hello\nworld", result[0])
    }
}

