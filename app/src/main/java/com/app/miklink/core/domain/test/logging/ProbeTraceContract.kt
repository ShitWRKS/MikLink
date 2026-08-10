package com.app.miklink.core.domain.test.logging

data class ProbeTracePoint(
    val schemaVersion: String,
    val sessionId: String,
    val scenarioId: String,
    val operationId: String?,
    val exchangeId: String?,
    val eventType: String,
    val payload: Map<String, Any?> = emptyMap()
)

object ProbeTraceContract {
    private const val VERSION = "1.0.0"

    fun violations(events: List<ProbeTracePoint>): List<String> {
        if (events.isEmpty()) return listOf("EMPTY_TRACE")
        val violations = mutableListOf<String>()
        if (events.any { it.schemaVersion != VERSION }) violations += "SCHEMA_VERSION_MISMATCH"
        if (events.any { it.sessionId.isBlank() || it.scenarioId.isBlank() }) violations += "MISSING_SESSION_CORRELATION"

        events.groupBy { it.exchangeId }.forEach { (exchangeId, exchange) ->
            if (exchangeId.isNullOrBlank()) {
                violations += "MISSING_EXCHANGE_ID"
                return@forEach
            }
            if (exchange.map { it.sessionId }.distinct().size != 1 ||
                exchange.map { it.scenarioId }.distinct().size != 1 ||
                exchange.map { it.operationId }.distinct().size != 1
            ) {
                violations += "CORRELATION_CHANGED:$exchangeId"
            }
            val requestIndex = exchange.indexOfFirst { it.eventType == "probe_request" }
            val terminalIndex = exchange.indexOfFirst {
                it.eventType == "probe_response" || it.eventType == "probe_error"
            }
            if (requestIndex < 0) violations += "MISSING_REQUEST:$exchangeId"
            if (terminalIndex < 0) violations += "MISSING_RESPONSE_OR_ERROR:$exchangeId"
            if (requestIndex >= 0 && terminalIndex >= 0 && terminalIndex <= requestIndex) {
                violations += "INVALID_EVENT_ORDER:$exchangeId"
            }
        }
        return violations.distinct()
    }

    fun requireValid(events: List<ProbeTracePoint>) {
        val errors = violations(events)
        require(errors.isEmpty()) { errors.joinToString() }
    }

    fun fullChainViolations(events: List<ProbeTracePoint>): List<String> {
        val violations = violations(events).toMutableList()
        val requiredOrder = listOf(
            "probe_request",
            "probe_response",
            "parsed_response",
            "normalized_response",
            "threshold_evaluation",
            "test_decision",
            "ui_snapshot"
        )
        events.groupBy { it.exchangeId }.forEach { (exchangeId, exchange) ->
            if (exchangeId.isNullOrBlank() || exchange.any { it.eventType == "probe_error" }) return@forEach
            var previous = -1
            requiredOrder.forEach { type ->
                val index = exchange.indexOfFirst { it.eventType == type }
                if (index < 0) {
                    violations += "MISSING_${type.uppercase()}:$exchangeId"
                } else if (index <= previous) {
                    violations += "INVALID_FULL_CHAIN_ORDER:$exchangeId"
                }
                if (index >= 0) previous = index
            }
        }
        return violations.distinct()
    }
}
