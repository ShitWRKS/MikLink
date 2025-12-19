/*
 * Purpose: Domain events emitted during test execution flows.
 * Inputs: Progress updates, log lines, typed snapshots, completion or failure payloads.
 * Outputs: Stream items consumed by UI and persistence handlers.
 * Notes: Typed contract only per ADR-0011; legacy string section events removed.
 */
package com.app.miklink.core.domain.test.model

/**
 * Eventi emessi durante l'esecuzione di un test.
 * Serve per stream in Flow dal UseCase alla UI.
 */
sealed class TestEvent {
    data class Progress(val progress: TestProgress) : TestEvent()
    data class LogLine(val message: String) : TestEvent()
    data class SnapshotUpdated(val snapshot: TestRunSnapshot) : TestEvent()
    data class Completed(val outcome: TestOutcome) : TestEvent()
    data class Failed(val error: TestError) : TestEvent()
}
