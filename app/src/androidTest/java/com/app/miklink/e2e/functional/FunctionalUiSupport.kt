package com.app.miklink.e2e.functional

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.app.miklink.e2e.catalog.appOnlyDependencies
import com.app.miklink.e2e.support.CleanupResult
import com.app.miklink.e2e.support.CleanupStatus
import com.app.miklink.e2e.support.RedactionStatus
import com.app.miklink.e2e.support.ScenarioStepResult
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.e2e.support.StepKind
import com.app.miklink.e2e.support.StepStatus
import com.app.miklink.e2e.support.dismissKeyguardIfPossible
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.testing.AgentUiTags
import java.io.File
import java.time.Instant
import java.util.regex.Pattern
import kotlinx.coroutines.flow.first

class FunctionalUiSupport(private val scenarioRule: ScenarioRule) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    val device: UiDevice = UiDevice.getInstance(instrumentation)
    private var expectedAbort = false

    fun runScenario(block: FunctionalUiSupport.() -> Unit) {
        if (!device.dismissKeyguardIfPossible(context)) {
            scenarioRule.notRun("DEVICE_LOCKED", "device-unlocked")
        }
        device.executeShellCommand(
            "am start -W -n ${context.packageName}/.MainActivity -f 0x10008000"
        )
        requireResource(DashboardTags.SCREEN, 20_000L, record = false)
        capture("before.png", "UI before functional journey")
        try {
            block()
            capture("after.png", "UI after functional journey")
            captureHierarchy("ui-hierarchy.xml", "Final semantic UI hierarchy")
        } catch (failure: Throwable) {
            if (!expectedAbort) {
                runCatching { capture("failure.png", "UI at functional failure") }
                runCatching { captureHierarchy("ui-hierarchy.xml", "Failure semantic UI hierarchy") }
            }
            throw failure
        }
    }

    fun notRun(reasonCode: String, prerequisiteId: String): Nothing {
        expectedAbort = true
        scenarioRule.notRun(reasonCode, prerequisiteId)
    }

    fun requireResource(
        tag: String,
        timeoutMs: Long = DEFAULT_WAIT_MS,
        scroll: Boolean = false,
        record: Boolean = true
    ): UiObject2 {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var direction = Direction.DOWN
        var scrollAttempts = 0
        do {
            device.findObject(By.res(tag))?.let {
                if (record) recordStep("assert:$tag", StepKind.ASSERTION)
                return it
            }
            if (scroll) {
                val scrollable = device.findObject(By.scrollable(true))
                if (scrollable != null) {
                    if (scrollAttempts > 0 && scrollAttempts % SCROLL_DIRECTION_ATTEMPTS == 0) {
                        direction = if (direction == Direction.DOWN) Direction.UP else Direction.DOWN
                    }
                    runCatching { scrollable.scroll(direction, 0.75f) }
                    scrollAttempts++
                }
            }
            SystemClock.sleep(POLL_MS)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("Required semantic resource not found: $tag")
    }

    fun clickResource(tag: String, scroll: Boolean = false, record: Boolean = true): UiObject2 =
        requireResource(tag, scroll = scroll, record = false).let {
            device.waitForIdle()
            SystemClock.sleep(UI_SETTLE_MS)
            requireResource(tag, scroll = scroll, record = false)
        }.also {
            check(it.isEnabled && it.isClickable) { "Semantic resource is not actionable: $tag" }
            it.click()
            device.waitForIdle()
            if (record) recordStep("click:$tag", StepKind.ACTION)
        }

    fun replaceText(tag: String, value: String, scroll: Boolean = false, record: Boolean = true) {
        val deadline = SystemClock.uptimeMillis() + TEXT_UPDATE_WAIT_MS
        do {
            val field = requireResource(tag, scroll = scroll, record = false)
            field.click()
            field.clear()
            field.text = value
            device.waitForIdle()
            SystemClock.sleep(POLL_MS * 2)
            val applied = device.findObject(By.res(tag))?.text.orEmpty() == value
            if (applied) {
                device.pressBack()
                device.waitForIdle()
                if (record) recordStep("input:$tag", StepKind.ACTION)
                return
            }
            SystemClock.sleep(POLL_MS)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("Text did not update for semantic resource: $tag")
    }

    fun requireText(text: String, timeoutMs: Long = DEFAULT_WAIT_MS): UiObject2 =
        device.wait(Until.findObject(By.text(text)), timeoutMs)
            ?.also { recordStep("assert:visible_text", StepKind.ASSERTION) }
            ?: throw AssertionError("Required visible text not found: $text")

    fun requireText(pattern: Pattern, timeoutMs: Long = DEFAULT_WAIT_MS): UiObject2 =
        device.wait(Until.findObject(By.text(pattern)), timeoutMs)
            ?.also { recordStep("assert:visible_text", StepKind.ASSERTION) }
            ?: throw AssertionError("Required visible text not found: ${pattern.pattern()}")

    fun clickText(text: String) {
        device.wait(Until.findObject(By.text(text)), DEFAULT_WAIT_MS)
            ?.click()
            ?: throw AssertionError("Required visible text not found")
        device.waitForIdle()
        recordStep("click:visible_text", StepKind.ACTION)
    }

    fun assertResourceAbsent(tag: String, timeoutMs: Long = 2_000L) {
        check(!device.wait(Until.hasObject(By.res(tag)), timeoutMs)) {
            "Semantic resource remained visible: $tag"
        }
        recordStep("assert:${tag}_absent", StepKind.ASSERTION)
    }

    fun waitForAnyResource(
        vararg tags: String,
        timeoutMs: Long = DEFAULT_WAIT_MS,
        record: Boolean = true
    ): Pair<String, UiObject2> {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            tags.forEach { tag ->
                device.findObject(By.res(tag))?.let { result ->
                    if (record) recordStep("assert:$tag", StepKind.ASSERTION)
                    return tag to result
                }
            }
            SystemClock.sleep(POLL_MS)
        }
        throw AssertionError("None of the required semantic resources appeared: ${tags.joinToString()}")
    }

    fun waitForResourceEnabled(
        tag: String,
        enabled: Boolean,
        timeoutMs: Long = DEFAULT_WAIT_MS,
        record: Boolean = true
    ): UiObject2 {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            device.findObject(By.res(tag))?.takeIf { it.isEnabled == enabled }?.let { result ->
                if (record) recordStep("assert:$tag:${if (enabled) "enabled" else "disabled"}", StepKind.ASSERTION)
                return result
            }
            SystemClock.sleep(POLL_MS)
        }
        throw AssertionError("Semantic resource $tag did not become ${if (enabled) "enabled" else "disabled"}")
    }

    fun pressBack() {
        device.pressBack()
        device.waitForIdle()
        recordStep("click:back", StepKind.ACTION)
    }

    fun scrollToTop() {
        val scrollable = device.wait(Until.findObject(By.scrollable(true)), DEFAULT_WAIT_MS)
            ?: throw AssertionError("Scrollable content did not become available")
        repeat(MAX_SCROLL_TO_EDGE_ATTEMPTS) {
            val moved = runCatching { scrollable.scroll(Direction.UP, 0.9f) }.getOrDefault(false)
            device.waitForIdle()
            if (!moved) return
        }
    }

    fun pressBackToDashboard() {
        repeat(4) {
            if (waitForStableDashboard(UI_SETTLE_MS)) {
                recordStep("open:dashboard", StepKind.ASSERTION)
                return
            }
            device.pressBack()
            if (waitForStableDashboard(NAVIGATION_WAIT_MS)) {
                recordStep("open:dashboard", StepKind.ASSERTION)
                return
            }
        }
        requireResource(DashboardTags.SCREEN)
    }

    private fun waitForStableDashboard(timeoutMs: Long): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        do {
            val dashboardReady = device.hasObject(By.res(DashboardTags.SCREEN)) &&
                device.hasObject(By.res(DashboardTags.SETTINGS_BUTTON))
            val anotherPrimaryScreenVisible = listOf(
                AgentUiTags.Client.LIST,
                AgentUiTags.Profile.LIST,
                AgentUiTags.History.SCREEN,
                AgentUiTags.Settings.SCREEN,
                AgentUiTags.Report.SCREEN,
                AgentUiTags.Probe.SCREEN
            ).any { device.hasObject(By.res(it)) }
            if (dashboardReady && !anotherPrimaryScreenVisible) return true
            SystemClock.sleep(POLL_MS)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    fun pressBackToResource(tag: String, maxPresses: Int = 4) {
        repeat(maxPresses) {
            if (device.hasObject(By.res(tag))) {
                recordStep("open:$tag", StepKind.ASSERTION)
                return
            }
            device.pressBack()
            if (device.wait(Until.hasObject(By.res(tag)), NAVIGATION_WAIT_MS)) {
                recordStep("open:$tag", StepKind.ASSERTION)
                return
            }
        }
        requireResource(tag)
    }

    fun recordStep(stepId: String, kind: StepKind = StepKind.ACTION, detail: String? = null) {
        val timestamp = nowStepTime()
        scenarioRule.recordStep(
            ScenarioStepResult(
                stepId = stepId,
                kind = kind,
                status = StepStatus.PASS,
                startedAt = timestamp,
                endedAt = timestamp,
                detail = detail
            )
        )
    }

    fun clickAtFraction(tag: String, horizontalFraction: Float) {
        require(horizontalFraction in 0f..1f)
        val target = requireResource(tag, scroll = true)
        val bounds = target.visibleBounds
        val x = bounds.left + (bounds.width() * horizontalFraction).toInt()
        check(device.click(x, bounds.centerY())) { "Dynamic tap failed for $tag" }
        device.waitForIdle()
    }

    fun newestPdfNotIn(existingPaths: Set<String>, timeoutMs: Long = 15_000L): File {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            context.cacheDir.listFiles { file -> file.isFile && file.extension.equals("pdf", true) }
                ?.filterNot { it.canonicalPath in existingPaths }
                ?.filter { it.length() > 100L }
                ?.maxByOrNull(File::lastModified)
                ?.let { return it }
            SystemClock.sleep(POLL_MS)
        }
        throw AssertionError("UI export did not produce a new PDF")
    }

    fun cachePdfPaths(): Set<String> = context.cacheDir
        .listFiles { file -> file.isFile && file.extension.equals("pdf", true) }
        ?.map(File::getCanonicalPath)
        ?.toSet()
        .orEmpty()

    fun registerPdf(source: File) {
        check(source.length() > 100L) { "Generated PDF is trivially small" }
        check(source.inputStream().use { it.readNBytes(5) }.toString(Charsets.US_ASCII) == "%PDF-") {
            "Generated file does not have a PDF header"
        }
        val trailer = source.inputStream().use { it.readBytes().takeLast(64).toByteArray() }
            .toString(Charsets.ISO_8859_1)
        check("%%EOF" in trailer) { "Generated PDF has no EOF marker" }
        scenarioRule.copyArtifact(
            source = source,
            filename = "report.pdf",
            mediaType = "application/pdf",
            redactionStatus = RedactionStatus.NOT_REQUIRED
        )
    }

    private fun capture(filename: String, title: String) {
        val file = File(context.cacheDir, "${scenarioRule.sessionId}-${System.nanoTime()}-$filename")
        check(device.takeScreenshot(file)) { "Unable to capture $filename" }
        scenarioRule.copyArtifact(file, filename, "image/png", RedactionStatus.NOT_REQUIRED)
        file.delete()
    }

    private fun captureHierarchy(filename: String, title: String) {
        val file = File(context.cacheDir, "${scenarioRule.sessionId}-${System.nanoTime()}-$filename")
        device.dumpWindowHierarchy(file)
        scenarioRule.copyArtifact(file, filename, "application/xml", RedactionStatus.SANITIZED)
        file.delete()
    }

    companion object {
        private const val DEFAULT_WAIT_MS = 10_000L
        private const val TEXT_UPDATE_WAIT_MS = 3_000L
        private const val POLL_MS = 150L
        private const val UI_SETTLE_MS = 300L
        private const val NAVIGATION_WAIT_MS = 3_000L
        private const val SCROLL_DIRECTION_ATTEMPTS = 6
        private const val MAX_SCROLL_TO_EDGE_ATTEMPTS = 12
    }
}

class SessionRecordCleanup(private val token: String) {
    suspend fun run(): CleanupResult {
        val dependencies = appOnlyDependencies()
        val failures = mutableListOf<String>()
        val clients = dependencies.clientRepository().observeAllClients().first()
            .filter { token in it.companyName }
        val profiles = dependencies.testProfileRepository().observeAllProfiles().first()
            .filter { token in it.profileName }
        val clientIds = clients.map { it.clientId }.toSet()
        dependencies.reportRepository().observeAllReports().first()
            .filter { it.clientId in clientIds || token in (it.socketName ?: "") }
            .forEach { report ->
                runCatching { dependencies.reportRepository().deleteReport(report) }
                    .onFailure { failures += "report:${report.reportId}" }
            }
        profiles.forEach { profile ->
            runCatching { dependencies.testProfileRepository().deleteProfile(profile) }
                .onFailure { failures += "profile:${profile.profileId}" }
        }
        clients.forEach { client ->
            runCatching { dependencies.clientRepository().deleteClient(client) }
                .onFailure { failures += "client:${client.clientId}" }
        }
        return if (failures.isEmpty()) CleanupResult(CleanupStatus.PASS)
        else CleanupResult(CleanupStatus.FAIL, "FUNCTIONAL_CLEANUP_FAILED:${failures.joinToString()}")
    }
}

internal fun nowStepTime(): String = Instant.now().toString()
