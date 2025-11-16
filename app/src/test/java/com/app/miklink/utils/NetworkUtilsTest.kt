package com.app.miklink.utils

import com.app.miklink.data.network.NeighborDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkUtilsTest {

    // Test 1: Match Perfetto - Un singolo vicino Bridge/Router
    @Test
    fun `findDirectlyConnectedSwitch returns bridge neighbor when present`() {
        val neighbors = listOf(
            NeighborDetail(
                identity = "TestSwitch",
                interfaceName = "ether1",
                systemCaps = "Bridge, Router",
                discoveredBy = "lldp"
            ),
            NeighborDetail(
                identity = "Phone",
                interfaceName = "ether2",
                systemCaps = "Telephone",
                discoveredBy = "lldp"
            )
        )

        val result = findDirectlyConnectedSwitch(neighbors)

        assertEquals("TestSwitch", result?.identity)
        assertEquals("Bridge, Router", result?.systemCaps)
    }

    // Test 2: Match Multiplo - Due switch, verifica priorità LLDP
    @Test
    fun `findDirectlyConnectedSwitch returns first LLDP bridge when multiple exist`() {
        val neighbors = listOf(
            NeighborDetail(
                identity = "CDPSwitch",
                interfaceName = "ether1",
                systemCaps = "Bridge",
                discoveredBy = "cdp"
            ),
            NeighborDetail(
                identity = "LLDPSwitch",
                interfaceName = "ether2",
                systemCaps = "Bridge",
                discoveredBy = "lldp"
            )
        )

        val result = findDirectlyConnectedSwitch(neighbors)

        // LLDP deve avere priorità
        assertEquals("LLDPSwitch", result?.identity)
        assertEquals("lldp", result?.discoveredBy)
    }

    // Test 3: Nessun Match - Solo dispositivi non-switch
    @Test
    fun `findDirectlyConnectedSwitch returns null when no bridge capability found`() {
        val neighbors = listOf(
            NeighborDetail(
                identity = "Phone",
                interfaceName = "ether1",
                systemCaps = "Telephone",
                discoveredBy = "lldp"
            ),
            NeighborDetail(
                identity = "Station",
                interfaceName = "ether2",
                systemCaps = "Station",
                discoveredBy = "lldp"
            )
        )

        val result = findDirectlyConnectedSwitch(neighbors)

        assertNull(result)
    }

    // Test 4: Lista Vuota
    @Test
    fun `findDirectlyConnectedSwitch returns null for empty list`() {
        val result = findDirectlyConnectedSwitch(emptyList())
        assertNull(result)
    }

    // Test 5: Capabilities Miste - Verifica case-insensitive e capabilities multiple
    @Test
    fun `findDirectlyConnectedSwitch handles mixed capabilities with different cases`() {
        val neighbors = listOf(
            NeighborDetail(
                identity = "MixedSwitch",
                interfaceName = "ether1",
                systemCaps = "Telephone, BRIDGE, Router", // Uppercase BRIDGE
                discoveredBy = "lldp"
            )
        )

        val result = findDirectlyConnectedSwitch(neighbors)

        assertEquals("MixedSwitch", result?.identity)
    }

    // Test Extra: Priorità CDP vs Unknown
    @Test
    fun `findDirectlyConnectedSwitch prioritizes CDP over unknown protocol`() {
        val neighbors = listOf(
            NeighborDetail(
                identity = "UnknownSwitch",
                interfaceName = "ether1",
                systemCaps = "Bridge",
                discoveredBy = "unknown"
            ),
            NeighborDetail(
                identity = "CDPSwitch",
                interfaceName = "ether2",
                systemCaps = "Bridge",
                discoveredBy = "cdp"
            )
        )

        val result = findDirectlyConnectedSwitch(neighbors)

        assertEquals("CDPSwitch", result?.identity)
        assertEquals("cdp", result?.discoveredBy)
    }

    // Test Extra: systemCaps null o vuoto
    @Test
    fun `findDirectlyConnectedSwitch returns null when systemCaps is null`() {
        val neighbors = listOf(
            NeighborDetail(
                identity = "NoCapSwitch",
                interfaceName = "ether1",
                systemCaps = null,
                discoveredBy = "lldp"
            )
        )

        val result = findDirectlyConnectedSwitch(neighbors)

        assertNull(result)
    }

    // Test Extra: Verifica lowercase "bridge"
    @Test
    fun `findDirectlyConnectedSwitch matches lowercase bridge capability`() {
        val neighbors = listOf(
            NeighborDetail(
                identity = "LowercaseSwitch",
                interfaceName = "ether1",
                systemCaps = "bridge",
                discoveredBy = "lldp"
            )
        )

        val result = findDirectlyConnectedSwitch(neighbors)

        assertEquals("LowercaseSwitch", result?.identity)
    }
}

