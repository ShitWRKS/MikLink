package com.app.miklink.e2e.support

import android.os.Build
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.app.miklink.BuildConfig
import com.app.miklink.core.domain.test.logging.LogSanitizer
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.rules.Timeout
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ScenarioRule(
    val sessionId: String,
    private val scenarioIdResolver: (Description) -> String,
    private val evidenceRoot: File,
    private val timeoutMs: Long,
    private val cleanup: suspend () -> CleanupResult = { CleanupResult(CleanupStatus.NOT_REQUIRED) }
) : TestRule {
    private val writer = EvidenceWriter(evidenceRoot)
    private val sanitizer = LogSanitizer()
    private val artifactReferences = mutableListOf<ArtifactReference>()
    private lateinit var session: ScenarioSession
    private lateinit var activeScenarioId: String
    private var running = false
    private var reportedCleanup = CleanupResult(CleanupStatus.NOT_REQUIRED)

    var result: ScenarioResult? = null
        private set

    override fun apply(base: Statement, description: Description): Statement {
        val timed = Timeout(timeoutMs, TimeUnit.MILLISECONDS).apply(base, description)
        return object : Statement() {
            override fun evaluate() {
                activeScenarioId = scenarioIdResolver(description)
                val startedAt = Instant.now().toString()
                session = ScenarioSession(sessionId, activeScenarioId, startedAt)
                var failure: Throwable? = null
                var desiredStatus = TerminalStatus.PASS
                var reasonCode = "ASSERTIONS_PASSED"
                try {
                    timed.evaluate()
                } catch (abort: ScenarioAbort) {
                    desiredStatus = abort.status
                    reasonCode = abort.reasonCode
                } catch (assumption: AssumptionViolatedException) {
                    desiredStatus = TerminalStatus.SKIP
                    reasonCode = "JUNIT_ASSUMPTION"
                    failure = assumption
                } catch (throwable: Throwable) {
                    desiredStatus = TerminalStatus.FAIL
                    reasonCode = if (throwable is org.junit.runners.model.TestTimedOutException) {
                        "SCENARIO_TIMEOUT"
                    } else {
                        "SCENARIO_FAILURE"
                    }
                    failure = throwable
                } finally {
                    val ruleCleanup = runCatching { runBlocking { cleanup() } }
                        .getOrElse { CleanupResult(CleanupStatus.FAIL, "CLEANUP_EXCEPTION") }
                    val cleanupResult = mergeCleanup(reportedCleanup, ruleCleanup)
                    val endedAt = Instant.now().toString()
                    result = when (desiredStatus) {
                        TerminalStatus.NOT_RUN -> session.finishNotRun(
                            reasonCode = reasonCode,
                            endedAt = endedAt,
                            cleanup = cleanupResult
                        )
                        TerminalStatus.SKIP -> session.finishSkip(reasonCode, endedAt, cleanupResult)
                        TerminalStatus.PASS, TerminalStatus.FAIL -> {
                            ensureRunning()
                            if (desiredStatus == TerminalStatus.PASS && session.lastSuccessfulStepId() == null) {
                                session.recordStep(
                                    ScenarioStepResult(
                                        stepId = "junit-body-completed",
                                        kind = StepKind.ASSERTION,
                                        status = StepStatus.PASS,
                                        startedAt = endedAt,
                                        endedAt = endedAt,
                                        detail = "Instrumented scenario body completed"
                                    )
                                )
                            }
                            session.finish(
                                status = desiredStatus,
                                reasonCode = reasonCode,
                                endedAt = endedAt,
                                cleanup = cleanupResult,
                                detail = failure?.message?.let(sanitizer::sanitize),
                                artifactPaths = artifactReferences.map(ArtifactReference::path)
                            )
                        }
                    }
                    persistResult(requireNotNull(result), startedAt, endedAt, cleanupResult)
                }

                when (result?.status) {
                    TerminalStatus.NOT_RUN, TerminalStatus.SKIP ->
                        throw AssumptionViolatedException(result?.reasonCode ?: reasonCode)
                    TerminalStatus.FAIL -> throw failure ?: AssertionError(result?.reasonCode)
                    else -> Unit
                }
            }
        }
    }

    fun recordStep(step: ScenarioStepResult) {
        ensureRunning()
        session.recordStep(step.copy(detail = step.detail?.let(sanitizer::sanitize)))
        writer.writeProgress(sessionId, activeScenarioId, session.lastSuccessfulStepId())
    }

    fun recordCleanup(result: CleanupResult) {
        reportedCleanup = mergeCleanup(reportedCleanup, result)
    }

    fun copyArtifact(
        source: File,
        filename: String,
        mediaType: String,
        redactionStatus: RedactionStatus
    ): ArtifactReference {
        require(source.isFile) { "Artifact source is missing: ${source.name}" }
        val safeFilename = safeSegment(filename)
        val relativePath = "scenarios/${safeSegment(activeScenarioId)}/$safeFilename"
        val destination = File(evidenceRoot, relativePath)
        destination.parentFile?.mkdirs()
        if (source.canonicalFile != destination.canonicalFile) {
            source.copyTo(destination, overwrite = true)
        }
        val reference = ArtifactReference(
            path = relativePath,
            mediaType = mediaType,
            sizeBytes = destination.length(),
            sha256 = destination.sha256(),
            redactionStatus = redactionStatus
        )
        artifactReferences.removeAll { it.path == relativePath }
        artifactReferences += reference
        return reference
    }

    fun notRun(reasonCode: String, prerequisiteId: String = "scenario-prerequisite"): Nothing {
        session.recordPrerequisite(
            PrerequisiteResult(
                id = prerequisiteId,
                required = true,
                status = PrerequisiteStatus.UNAVAILABLE,
                reasonCode = reasonCode
            )
        )
        throw ScenarioAbort(TerminalStatus.NOT_RUN, reasonCode)
    }

    fun skip(reasonCode: String, prerequisiteId: String = "optional-capability"): Nothing {
        session.recordPrerequisite(
            PrerequisiteResult(
                id = prerequisiteId,
                required = false,
                status = PrerequisiteStatus.NOT_APPLICABLE,
                reasonCode = reasonCode
            )
        )
        throw ScenarioAbort(TerminalStatus.SKIP, reasonCode)
    }

    private fun ensureRunning() {
        if (running) return
        session.markReady()
        session.startRunning()
        running = true
    }

    private fun mergeCleanup(first: CleanupResult, second: CleanupResult): CleanupResult = when {
        first.status == CleanupStatus.FAIL -> first
        second.status == CleanupStatus.FAIL -> second
        first.status == CleanupStatus.PASS || second.status == CleanupStatus.PASS ->
            CleanupResult(CleanupStatus.PASS)
        else -> CleanupResult(CleanupStatus.NOT_REQUIRED)
    }

    private fun persistResult(
        scenarioResult: ScenarioResult,
        startedAt: String,
        endedAt: String,
        cleanupResult: CleanupResult
    ) {
        val resultFile = writer.writeScenarioResult(scenarioResult)
        val relativeResultPath = "scenarios/${safeSegment(activeScenarioId)}/scenario-result.json"
        val reference = ArtifactReference(
            path = relativeResultPath,
            mediaType = "application/json",
            sizeBytes = resultFile.length(),
            sha256 = resultFile.sha256(),
            redactionStatus = RedactionStatus.SANITIZED
        )
        val currentReferences = artifactReferences + reference
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val arguments = InstrumentationRegistry.getArguments()
        val device = UiDevice.getInstance(instrumentation)
        val serial = runCatching { device.executeShellCommand("getprop ro.serialno").trim() }
            .getOrDefault("unknown-device")
            .ifBlank { "unknown-device" }
        synchronized(manifestLock) {
            val existing = writer.readManifest()
            val effectiveCleanup = when {
                existing?.cleanup?.status == CleanupStatus.FAIL -> existing.cleanup
                cleanupResult.status == CleanupStatus.FAIL -> cleanupResult
                existing?.cleanup?.status == CleanupStatus.PASS || cleanupResult.status == CleanupStatus.PASS ->
                    CleanupResult(CleanupStatus.PASS)
                else -> CleanupResult(CleanupStatus.NOT_REQUIRED)
            }
            writer.writeManifest(
                SessionManifest(
                sessionId = sessionId,
                startedAt = existing?.startedAt ?: startedAt,
                endedAt = endedAt,
                build = BuildIdentity(
                    applicationId = BuildConfig.APPLICATION_ID,
                    versionCode = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                    variant = BuildConfig.BUILD_TYPE,
                    sourceRevision = BuildConfig.SOURCE_REVISION
                ),
                device = DeviceIdentity(serial, Build.MODEL, Build.VERSION.SDK_INT),
                policy = SessionPolicy(
                    disposableLocalState = arguments.getString("disposableLocalState").toBoolean(),
                    allowWifiDisruption = arguments.getString("allowWifiDisruption").toBoolean(),
                    hostControlRetained = arguments.getString("hostControlRetained").toBoolean()
                ),
                scenarioIds = (existing?.scenarioIds.orEmpty() + activeScenarioId).distinct(),
                artifacts = existing?.artifacts.orEmpty()
                    .filterNot { existingReference ->
                        currentReferences.any { it.path == existingReference.path }
                    } + currentReferences,
                cleanup = effectiveCleanup
                )
            )
        }
        instrumentation.sendStatus(
            0,
            Bundle().apply {
                putString("miklink.sessionId", sessionId)
                putString("miklink.scenarioId", activeScenarioId)
                putString("miklink.status", scenarioResult.status.name)
                putString("miklink.reasonCode", scenarioResult.reasonCode)
                putString("miklink.evidencePath", evidenceRoot.absolutePath)
            }
        )
    }

    private fun safeSegment(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "unknown" }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 120_000L
        private val manifestLock = Any()

        fun catalog(
            scenarioId: String,
            timeoutMs: Long = DEFAULT_TIMEOUT_MS,
            cleanup: suspend () -> CleanupResult = { CleanupResult(CleanupStatus.NOT_REQUIRED) }
        ): ScenarioRule = catalog(timeoutMs, cleanup) { scenarioId }

        fun catalog(
            timeoutMs: Long = DEFAULT_TIMEOUT_MS,
            cleanup: suspend () -> CleanupResult = { CleanupResult(CleanupStatus.NOT_REQUIRED) },
            scenarioIdResolver: (Description) -> String
        ): ScenarioRule {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val requestedSession = InstrumentationRegistry.getArguments().getString("sessionId")
            val sessionId = requestedSession?.takeIf { it.isNotBlank() }
                ?: "native-${System.currentTimeMillis()}-${System.nanoTime()}"
            val externalRoot = requireNotNull(instrumentation.targetContext.getExternalFilesDir(null))
            val root = File(externalRoot, "agent-tests/$sessionId")
            return ScenarioRule(sessionId, scenarioIdResolver, root, timeoutMs, cleanup)
        }
    }
}

private class ScenarioAbort(
    val status: TerminalStatus,
    val reasonCode: String
) : RuntimeException(reasonCode)
