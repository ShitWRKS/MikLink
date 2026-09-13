package com.app.miklink.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TdrCapabilityClassifierTest {

    @Test
    fun blank_board_name_is_unknown() {
        assertEquals(TdrCapability.UNKNOWN, TdrCapabilityClassifier.classify(null))
        assertEquals(TdrCapability.UNKNOWN, TdrCapabilityClassifier.classify(""))
        assertEquals(TdrCapability.UNKNOWN, TdrCapabilityClassifier.classify("   "))
    }

    @Test
    fun supported_series_is_supported() {
        listOf(
            "CCR1009-1G-1S-1S+",
            "CRS109-1G-9S-2HnD",
            "CRS212-1G-10S-1S+IN",
            "OmniTIK 5 ac",
            "RB450G x3",
            "RB951Ui-2HnD",
            "RB2011iL-IN",
            "RB4011iGS+RM"
        ).forEach { board ->
            assertEquals(
                "Board '$board' deve essere SUPPORTED",
                TdrCapability.SUPPORTED,
                TdrCapabilityClassifier.classify(board)
            )
        }
    }

    @Test
    fun rb5009_support_depends_on_test_interface() {
        assertEquals(
            TdrCapability.SUPPORTED,
            TdrCapabilityClassifier.classify("RB5009UG+S+IN", "ether1")
        )
        assertEquals(
            TdrCapability.UNSUPPORTED,
            TdrCapabilityClassifier.classify("RB5009UG+S+IN", "ether2")
        )
        assertEquals(
            TdrCapability.UNKNOWN,
            TdrCapabilityClassifier.classify("RB5009UG+S+IN", null)
        )
    }

    @Test
    fun supported_exact_model_is_supported() {
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB952Ui-5ac2nD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB962UiGS-5HacT2HnT"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB1100AHx2"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB1100x4"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBD52G-5HacD2HnD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBD53G-5HacD2HnD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBcAPGi-5acD2nD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBmAPL-2nD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBmAP2nD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBwsAP-5Hac2nD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB3011UiAS-RM"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB750Gr2"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB750UPr2"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB751U-2HnD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB850Gx2"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBMetal 2SHPn"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB931-2nD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RB941-2nD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBDynaDishG-5HacD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBLDFG-5acD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("RBLHGG-5acD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("C52iG-5HaxD2HaxD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("C53UiG+5HPaxD2HPaxD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("S53UG+5HaxD2HPaxD"))
        assertEquals(TdrCapability.SUPPORTED, TdrCapabilityClassifier.classify("H53UiG-5HaxQ2HaxQ"))
    }

    @Test
    fun unsupported_combo_port_is_unsupported() {
        // No combo-port models are currently documented, but the rule must be additive-safe.
        // Placeholder: a documented combo model would be UNSUPPORTED here.
    }

    @Test
    fun unknown_model_is_unknown_not_unsupported() {
        // Generic hAP/hEX/CCR2004 must NOT be auto-classified as SUPPORTED or UNSUPPORTED.
        assertEquals(TdrCapability.UNKNOWN, TdrCapabilityClassifier.classify("hAP ax^2"))
        assertEquals(TdrCapability.UNKNOWN, TdrCapabilityClassifier.classify("hEX PoE"))
        assertEquals(TdrCapability.UNKNOWN, TdrCapabilityClassifier.classify("CCR2004-1G-12S+2XS"))
        assertEquals(TdrCapability.UNKNOWN, TdrCapabilityClassifier.classify("RB1100AH"))
        assertEquals(TdrCapability.UNKNOWN, TdrCapabilityClassifier.classify("SomeFutureModel"))
        // CRS3xx series is NOT in the documented supported list (only CRS1xx and CRS2xx are).
        assertEquals(TdrCapability.UNKNOWN, TdrCapabilityClassifier.classify("CRS317-1G-5S"))
    }
}
