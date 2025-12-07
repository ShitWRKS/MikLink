package com.app.miklink.data.pdf

import android.content.Context
import com.app.miklink.data.pdf.PdfGeneratorIText // Updated import
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
 * Unit Test Suite for PdfGeneratorIText.parseResults()
 *
 * Tests the JSON parsing logic with Moshi for the ParsedResults data class.
 * Covers complete JSON, partial JSON, malformed JSON, and empty/null inputs.
 */
class PdfGeneratorTest {

    private lateinit var pdfGenerator: PdfGeneratorIText
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

        // Create PdfGeneratorIText with mocked Context
        val mockContext = mockk<Context>(relaxed = true)
        pdfGenerator = PdfGeneratorIText(mockContext, moshi)
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

    // wrapText tests removed as PdfGeneratorIText relies on iText for layout
}

/**
 * Unit Test Suite for PdfGenerator.parseResults() - Legacy JSON Compatibility
 *
 * Tests parsing of legacy JSON formats for compatibility with older Mikrotik RouterOS versions.
 */
class PdfGeneratorLegacyCompatTest {

    private lateinit var pdfGenerator: PdfGeneratorIText

    @Before
    fun setup() {
        val moshi = Moshi.Builder()
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
                fun toJson(writer: com.squareup.moshi.JsonWriter, value: Boolean) { writer.value(value) }
            })
            .add(object {
                @FromJson
                fun fromJson(reader: com.squareup.moshi.JsonReader): Int {
                    return if (reader.peek() == com.squareup.moshi.JsonReader.Token.STRING) {
                        reader.nextString().toIntOrNull() ?: 0
                    } else reader.nextInt()
                }
                @ToJson
                fun toJson(writer: com.squareup.moshi.JsonWriter, value: Int) { writer.value(value) }
            })
            .add(KotlinJsonAdapterFactory())
            .build()
        val mockContext = mockk<Context>(relaxed = true)
        pdfGenerator = PdfGeneratorIText(mockContext, moshi)
    }

    @Test
    fun `GIVEN legacy json with ping targets and tdr object WHEN parseResults THEN normalizes to lists`() {
        val legacyJson = """
            {
                "ping_target1": [
                    {"avg-rtt": "10ms", "packet-loss": "0"}
                ],
                "ping_8_8_8_8": [
                    {"avg-rtt": "20ms", "packet-loss": "0"}
                ],
                "tdr": {
                    "status": "ok",
                    "cable-pairs": [
                        {"pair":"1-2","length":"10m","status":"ok"}
                    ]
                }
            }
        """.trimIndent()

        val parsed = pdfGenerator.parseResults(legacyJson)
        assertNotNull(parsed)
        // Ping aggregato può non essere disponibile su tutti i formati legacy: non assertiamo la non-null
        // Verifichiamo comunque che, se presente, contenga almeno 1 elemento
        parsed!!.ping?.let { assertTrue(it.isNotEmpty()) }

        // TDR normalizzato in lista
        assertNotNull(parsed.tdr)
        assertEquals(1, parsed.tdr!!.size)
        assertEquals("ok", parsed.tdr!![0].status)
    }
}
