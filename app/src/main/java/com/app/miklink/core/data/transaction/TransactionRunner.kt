/*
 * Purpose: Port for running a block inside a persistence transaction (report + counter atomicity).
 * Inputs: A suspending block to execute atomically.
 * Outputs: The block result; on failure the transaction rolls back.
 * Notes: Port lives under core/data (ADR-0013). The Room implementation stays in the data layer.
 */
package com.app.miklink.core.data.transaction

interface TransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}
