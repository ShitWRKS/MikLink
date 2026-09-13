package com.app.miklink.e2e

import com.app.miklink.core.data.pdf.PdfGenerator
import com.app.miklink.core.data.repository.BackupRepository
import com.app.miklink.core.data.repository.client.ClientRepository
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.data.repository.probe.ProbeRepository
import com.app.miklink.core.data.repository.report.ReportRepository
import com.app.miklink.core.data.repository.test.TestProfileRepository
import com.app.miklink.core.domain.test.logging.DebugTraceRunContext
import com.app.miklink.core.domain.test.step.CableTestStep
import com.app.miklink.core.domain.test.step.LinkStatusStep
import com.app.miklink.core.domain.test.step.NeighborDiscoveryStep
import com.app.miklink.core.domain.test.step.NetworkConfigStep
import com.app.miklink.core.domain.test.step.PingStep
import com.app.miklink.core.domain.test.step.SpeedTestStep
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Debug-only dependency bridge for native instrumentation. It is absent from release artifacts. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugE2EEntryPoint {
    fun clientRepository(): ClientRepository
    fun testProfileRepository(): TestProfileRepository
    fun reportRepository(): ReportRepository
    fun probeRepository(): ProbeRepository
    fun userPreferencesRepository(): UserPreferencesRepository
    fun backupRepository(): BackupRepository
    fun pdfGenerator(): PdfGenerator
    fun networkConfigStep(): NetworkConfigStep
    fun linkStatusStep(): LinkStatusStep
    fun cableTestStep(): CableTestStep
    fun neighborDiscoveryStep(): NeighborDiscoveryStep
    fun pingStep(): PingStep
    fun speedTestStep(): SpeedTestStep
    fun debugTraceRunContext(): DebugTraceRunContext
}
