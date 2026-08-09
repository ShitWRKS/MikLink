package com.app.miklink.e2e.support

const val EVIDENCE_SCHEMA_VERSION = "1.0.0"

enum class TerminalStatus { PASS, FAIL, NOT_RUN, SKIP }
enum class StepKind { SETUP, ACTION, ASSERTION, OBSERVATION, CLEANUP }
enum class StepStatus { PASS, FAIL, SKIP }
enum class PrerequisiteStatus { AVAILABLE, UNAVAILABLE, NOT_APPLICABLE }
enum class CleanupStatus { NOT_REQUIRED, PASS, FAIL }
enum class RedactionStatus { NOT_REQUIRED, SANITIZED, VERIFIED_SCAN }
enum class ProbeMutationMode { APP_PATH_ONLY }

private fun isNormalizedRelativeEvidencePath(path: String): Boolean {
    if (path.isBlank() || path.startsWith('/') || path.contains('\\')) return false
    return path.split('/').none { it.isBlank() || it == "." || it == ".." }
}

data class BuildIdentity(
    val applicationId: String,
    val versionCode: Int,
    val versionName: String,
    val variant: String,
    val sourceRevision: String
) {
    init {
        require(applicationId.isNotBlank()) { "Application ID is required" }
        require(versionCode >= 1) { "Version code must be positive" }
        require(versionName.isNotBlank()) { "Version name is required" }
        require(variant == "debug" || variant == "release") { "Unsupported build variant" }
        require(sourceRevision.length >= 7) { "Source revision must identify the build" }
    }
}

data class DeviceIdentity(
    val serial: String,
    val model: String,
    val apiLevel: Int,
    val state: String = "device"
) {
    init {
        require(serial.isNotBlank()) { "Device serial is required" }
        require(model.isNotBlank()) { "Device model is required" }
        require(apiLevel >= 30) { "Device API level is unsupported" }
        require(state == "device") { "Device must be authorized and online" }
    }
}

data class SessionPolicy(
    val disposableLocalState: Boolean = false,
    val allowWifiDisruption: Boolean = false,
    val hostControlRetained: Boolean = false,
    val probeMutationMode: ProbeMutationMode = ProbeMutationMode.APP_PATH_ONLY
)

data class ArtifactReference(
    val path: String,
    val mediaType: String,
    val sizeBytes: Long,
    val sha256: String,
    val redactionStatus: RedactionStatus
) {
    init {
        require(isNormalizedRelativeEvidencePath(path)) {
            "Artifact path must be normalized, relative, and contained"
        }
        require(mediaType.isNotBlank()) { "Artifact media type is required" }
        require(sizeBytes >= 0) { "Artifact size cannot be negative" }
        require(sha256.matches(Regex("^[a-f0-9]{64}$"))) { "Artifact digest must be SHA-256" }
    }
}

data class CleanupResult(
    val status: CleanupStatus,
    val reasonCode: String? = null
)

data class PrerequisiteResult(
    val id: String,
    val required: Boolean,
    val status: PrerequisiteStatus,
    val reasonCode: String
) {
    init {
        require(id.isNotBlank()) { "Prerequisite ID is required" }
        require(reasonCode.isNotBlank()) { "Prerequisite reason code is required" }
    }
}

data class ScenarioStepResult(
    val stepId: String,
    val kind: StepKind,
    val status: StepStatus,
    val startedAt: String,
    val endedAt: String,
    val operationId: String? = null,
    val detail: String? = null
) {
    init {
        require(stepId.isNotBlank()) { "Step ID is required" }
        require(startedAt.isNotBlank() && endedAt.isNotBlank()) { "Step timestamps are required" }
        require(operationId == null || operationId.isNotBlank()) { "Operation ID cannot be blank" }
    }
}

data class ScenarioResult(
    val schemaVersion: String = EVIDENCE_SCHEMA_VERSION,
    val sessionId: String,
    val scenarioId: String,
    val startedAt: String,
    val endedAt: String,
    val status: TerminalStatus,
    val reasonCode: String,
    val detail: String? = null,
    val lastSuccessfulStepId: String? = null,
    val prerequisites: List<PrerequisiteResult>,
    val steps: List<ScenarioStepResult>,
    val crashExitReason: String? = null,
    val artifactPaths: List<String> = emptyList(),
    val cleanup: CleanupResult
) {
    init {
        require(sessionId.isNotBlank()) { "Session ID is required" }
        require(scenarioId.isNotBlank()) { "Scenario ID is required" }
        require(startedAt.isNotBlank() && endedAt.isNotBlank()) { "Scenario timestamps are required" }
        require(reasonCode.isNotBlank()) { "Scenario reason code is required" }
        require(artifactPaths.distinct().size == artifactPaths.size) { "Artifact paths must be unique" }
        require(artifactPaths.all(::isNormalizedRelativeEvidencePath)) {
            "Artifact paths must be normalized, relative, and contained"
        }
        require(lastSuccessfulStepId == null || steps.any {
            it.stepId == lastSuccessfulStepId && it.status == StepStatus.PASS
        }) {
            "Last successful step must reference a passed step"
        }
        require(cleanup.status != CleanupStatus.FAIL || status == TerminalStatus.FAIL) {
            "Cleanup failure must make the scenario fail"
        }
        if (status == TerminalStatus.PASS) {
            require(steps.none { it.status == StepStatus.FAIL }) {
                "A passing scenario cannot contain a failed step"
            }
            require(prerequisites.none {
                it.required && it.status == PrerequisiteStatus.UNAVAILABLE
            }) {
                "A passing scenario cannot have an unavailable required prerequisite"
            }
        }
        if (status == TerminalStatus.NOT_RUN || status == TerminalStatus.SKIP) {
            require(steps.none { it.kind == StepKind.ACTION || it.kind == StepKind.ASSERTION }) {
                "A non-running scenario cannot contain evaluated actions or assertions"
            }
        }
    }
}

data class SessionManifest(
    val schemaVersion: String = EVIDENCE_SCHEMA_VERSION,
    val sessionId: String,
    val startedAt: String,
    val endedAt: String? = null,
    val build: BuildIdentity,
    val device: DeviceIdentity,
    val policy: SessionPolicy,
    val scenarioIds: List<String>,
    val artifacts: List<ArtifactReference>,
    val cleanup: CleanupResult? = null
) {
    init {
        require(sessionId.isNotBlank()) { "Session ID is required" }
        require(scenarioIds.isNotEmpty() && scenarioIds.distinct().size == scenarioIds.size) {
            "Scenario IDs must be non-empty and unique"
        }
        require(endedAt != null && cleanup != null) {
            "Session manifest must be finalized with end time and cleanup"
        }
        if (build.variant == "release") {
            require(scenarioIds == listOf("release-smoke")) {
                "Release manifests may describe only external black-box smoke"
            }
            require(
                !policy.disposableLocalState &&
                    !policy.allowWifiDisruption &&
                    !policy.hostControlRetained
            ) {
                "Release smoke cannot authorize destructive test policy"
            }
            require(
                artifacts.none {
                    it.path.endsWith(".ndjson", ignoreCase = true) ||
                        it.mediaType.equals("application/x-ndjson", ignoreCase = true)
                }
            ) {
                "Release smoke cannot contain enhanced trace artifacts"
            }
        }
    }
}

data class TraceEvent(
    val schemaVersion: String = EVIDENCE_SCHEMA_VERSION,
    val timestamp: String,
    val sessionId: String,
    val scenarioId: String,
    val operationId: String? = null,
    val exchangeId: String? = null,
    val eventType: String,
    val payload: Map<String, Any?>
)

class ScenarioSession(
    private val sessionId: String,
    private val scenarioId: String,
    private val startedAt: String
) {
    private enum class State { DISCOVERING, READY, RUNNING, FINISHED }

    private var state = State.DISCOVERING
    private val prerequisites = mutableListOf<PrerequisiteResult>()
    private val steps = mutableListOf<ScenarioStepResult>()
    private var terminal: ScenarioResult? = null

    fun recordPrerequisite(result: PrerequisiteResult) {
        check(state == State.DISCOVERING) { "Prerequisites must be recorded before execution" }
        prerequisites += result
    }

    fun markReady() {
        check(state == State.DISCOVERING) { "Scenario is not discovering" }
        check(prerequisites.none { it.required && it.status == PrerequisiteStatus.UNAVAILABLE }) {
            "Required prerequisite is unavailable"
        }
        state = State.READY
    }

    fun startRunning() {
        check(state == State.READY) { "Scenario is not ready" }
        state = State.RUNNING
    }

    fun recordStep(result: ScenarioStepResult) {
        check(state == State.RUNNING) { "Scenario is not running" }
        steps += result
    }

    fun finishNotRun(
        reasonCode: String,
        endedAt: String,
        cleanup: CleanupResult = CleanupResult(CleanupStatus.NOT_REQUIRED)
    ): ScenarioResult {
        check(state == State.DISCOVERING || state == State.READY) { "Scenario already ran or finished" }
        val status = if (cleanup.status == CleanupStatus.FAIL) TerminalStatus.FAIL else TerminalStatus.NOT_RUN
        val reason = if (cleanup.status == CleanupStatus.FAIL) cleanup.reasonCode ?: "CLEANUP_FAILED" else reasonCode
        return finishInternal(status, reason, endedAt, cleanup)
    }

    fun finishSkip(
        reasonCode: String,
        endedAt: String,
        cleanup: CleanupResult = CleanupResult(CleanupStatus.NOT_REQUIRED)
    ): ScenarioResult {
        check(state == State.DISCOVERING || state == State.READY) { "Scenario already ran or finished" }
        val status = if (cleanup.status == CleanupStatus.FAIL) TerminalStatus.FAIL else TerminalStatus.SKIP
        val reason = if (cleanup.status == CleanupStatus.FAIL) cleanup.reasonCode ?: "CLEANUP_FAILED" else reasonCode
        return finishInternal(status, reason, endedAt, cleanup)
    }

    fun finishAborted(
        status: TerminalStatus,
        reasonCode: String,
        endedAt: String,
        cleanup: CleanupResult
    ): ScenarioResult {
        check(status == TerminalStatus.NOT_RUN || status == TerminalStatus.SKIP)
        check(state == State.RUNNING && steps.none { it.kind == StepKind.ACTION || it.kind == StepKind.ASSERTION }) {
            "A scenario cannot be classified unavailable after evaluation starts"
        }
        val effectiveStatus = if (cleanup.status == CleanupStatus.FAIL) TerminalStatus.FAIL else status
        val effectiveReason = if (cleanup.status == CleanupStatus.FAIL) {
            cleanup.reasonCode ?: "CLEANUP_FAILED"
        } else {
            reasonCode
        }
        return finishInternal(effectiveStatus, effectiveReason, endedAt, cleanup)
    }

    fun finish(
        status: TerminalStatus,
        reasonCode: String,
        endedAt: String,
        cleanup: CleanupResult,
        detail: String? = null,
        crashExitReason: String? = null,
        artifactPaths: List<String> = emptyList()
    ): ScenarioResult {
        check(state == State.RUNNING) { "Scenario is not running" }
        check(status == TerminalStatus.PASS || status == TerminalStatus.FAIL) {
            "A running scenario ends PASS or FAIL"
        }
        val effectiveStatus = if (cleanup.status == CleanupStatus.FAIL) TerminalStatus.FAIL else status
        val effectiveReason = if (cleanup.status == CleanupStatus.FAIL) {
            cleanup.reasonCode ?: "CLEANUP_FAILED"
        } else {
            reasonCode
        }
        return finishInternal(
            effectiveStatus,
            effectiveReason,
            endedAt,
            cleanup,
            detail,
            crashExitReason,
            artifactPaths
        )
    }

    fun resultOrNull(): ScenarioResult? = terminal

    fun lastSuccessfulStepId(): String? = steps.lastOrNull { it.status == StepStatus.PASS }?.stepId

    private fun finishInternal(
        status: TerminalStatus,
        reasonCode: String,
        endedAt: String,
        cleanup: CleanupResult,
        detail: String? = null,
        crashExitReason: String? = null,
        artifactPaths: List<String> = emptyList()
    ): ScenarioResult {
        check(state != State.FINISHED) { "Scenario already finished" }
        val result = ScenarioResult(
            sessionId = sessionId,
            scenarioId = scenarioId,
            startedAt = startedAt,
            endedAt = endedAt,
            status = status,
            reasonCode = reasonCode,
            detail = detail,
            lastSuccessfulStepId = lastSuccessfulStepId(),
            prerequisites = prerequisites.toList(),
            steps = steps.toList(),
            crashExitReason = crashExitReason,
            artifactPaths = artifactPaths,
            cleanup = cleanup
        )
        terminal = result
        state = State.FINISHED
        return result
    }
}
