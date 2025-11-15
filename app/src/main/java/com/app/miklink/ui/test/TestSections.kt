package com.app.miklink.ui.test

enum class TestSectionType { NETWORK, LINK, LLDP, PING, TDR }

enum class TestSectionCategory { INFO, TEST }

data class TestDetail(val label: String, val value: String)

data class TestSection(
    val category: TestSectionCategory,
    val type: TestSectionType,
    val title: String,
    val status: String, // PASS/FAIL/PARTIAL/INFO/SKIPPED
    val details: List<TestDetail> = emptyList()
)
