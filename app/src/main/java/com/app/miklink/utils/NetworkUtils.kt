package com.app.miklink.utils

import com.app.miklink.data.remote.mikrotik.dto.NeighborDetail

/**
 * Finds the most likely candidate for a directly connected switch from a list of LLDP neighbors.
 * It prioritizes neighbors discovered via LLDP that have the 'bridge' capability.
 */
fun findDirectlyConnectedSwitch(neighbors: List<NeighborDetail>): NeighborDetail? {
    return neighbors.filter { it.systemCaps?.contains("bridge", ignoreCase = true) == true }
        .maxByOrNull { 
            when (it.discoveredBy?.lowercase()) {
                "lldp" -> 2 // Prioritize LLDP
                "cdp" -> 1  // Then CDP
                else -> 0
            }
        }
}