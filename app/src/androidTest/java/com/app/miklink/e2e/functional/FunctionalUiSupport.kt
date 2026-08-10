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
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.e2e.support.dismissKeyguardIfPossible
import com.app.miklink.ui.dashboard.DashboardTags
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
        device.executeShellCommand("am force-stop ${context.packageName}")
        device.executeShellCommand("am start -W -n ${context.packageName}/.MainActivity")
        requireResource(DashboardTags.SCREEN, 20_000L)
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

    fun requireResource(tag: String, timeoutMs: Long = DEFAULT_WAIT_MS, scroll: Boolean = false): UiObject2 {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var direction = Direction.DOWN
        var scrollAttempts = 0
        do {
            device.findObject(By.res(tag))?.let { return it }
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

    fun clickResource(tag: String, scroll: Boolean = false): UiObject2 =
        requireResource(tag, scroll = scroll).also {
            it.click()
            device.waitForIdle()
        }

    fun replaceText(tag: String, value: String, scroll: Boolean = false) {
        val field = requireResource(tag, scroll = scroll)
        field.text = value
        device.waitForIdle()
    }

    fun requireText(text: String, timeoutMs: Long = DEFAULT_WAIT_MS): UiObject2 =
        device.wait(Until.findObject(By.text(text)), timeoutMs)
            ?: throw AssertionError("Required visible text not found: $text")

    fun requireText(pattern: Pattern, timeoutMs: Long = DEFAULT_WAIT_MS): UiObject2 =
        device.wait(Until.findObject(By.text(pattern)), timeoutMs)
            ?: throw AssertionError("Required visible text not found: ${pattern.pattern()}")

    fun clickText(text: String) {
        requireText(text).click()
        device.waitForIdle()
    }

    fun assertResourceAbsent(tag: String, timeoutMs: Long = 2_000L) {
        check(!device.wait(Until.hasObject(By.res(tag)), timeoutMs)) {
            "Semantic resource remained visible: $tag"
        }
    }

    fun pressBackToDashboard() {
        repeat(4) {
            if (device.hasObject(By.res(DashboardTags.SCREEN))) return
            device.pressBack()
            device.waitForIdle()
        }
        requireResource(DashboardTags.SCREEN)
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
        private const val POLL_MS = 150L
        private const val SCROLL_DIRECTION_ATTEMPTS = 6
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
