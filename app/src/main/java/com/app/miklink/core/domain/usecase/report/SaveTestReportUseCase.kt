/*
 * Purpose: Persist a test report and apply the Socket-ID increment policy for run-test flows.
 * Inputs: TestReport to save and a flag indicating whether the client counter should be updated.
 * Outputs: Database identifier of the saved report.
 * Notes: Report insert and counter increment are atomic (same transaction, ADR-0010/ADR-0013).
 *   Duplication/import flows pass incrementClientCounter = false to avoid touching the counter.
 */
package com.app.miklink.core.domain.usecase.report

import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.transaction.TransactionRunner
import com.app.miklink.core.domain.model.TestReport
import javax.inject.Inject

interface SaveTestReportUseCase {
    suspend operator fun invoke(report: TestReport, incrementClientCounter: Boolean = true): Long
}

/**
 * Thrown when a counter increment is requested but the target client does not exist.
 * Triggers a rollback of the surrounding transaction.
 */
class ClientNotFoundException(clientId: Long) :
    IllegalStateException("Cannot increment Socket-ID counter: client $clientId does not exist")

class SaveTestReportUseCaseImpl @Inject constructor(
    private val reportRepository: ReportRepository,
    private val clientRepository: ClientRepository,
    private val transactionRunner: TransactionRunner
) : SaveTestReportUseCase {
    override suspend fun invoke(report: TestReport, incrementClientCounter: Boolean): Long {
        return transactionRunner.runInTransaction {
            val id = reportRepository.saveReport(report)

            if (incrementClientCounter) {
                val clientId = report.clientId
                if (clientId != null) {
                    // Atomic increment; 0 rows updated means the client does not exist.
                    val updated = clientRepository.incrementNextIdNumber(clientId)
                    if (updated == 0) {
                        throw ClientNotFoundException(clientId)
                    }
                }
                // clientId == null: orphan report; nothing to increment (preserved behavior).
            }

            id
        }
    }
}
