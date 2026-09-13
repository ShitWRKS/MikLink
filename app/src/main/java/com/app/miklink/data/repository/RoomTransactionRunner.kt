package com.app.miklink.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.app.miklink.core.data.transaction.TransactionRunner

/**
 * Room-backed implementation of the [TransactionRunner] port.
 * The rollback is delegated to the Room transaction; no manual restore is performed.
 */
class RoomTransactionRunner(private val db: RoomDatabase) : TransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return db.withTransaction { block() }
    }
}
