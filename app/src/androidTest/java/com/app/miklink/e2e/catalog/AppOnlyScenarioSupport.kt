package com.app.miklink.e2e.catalog

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.app.miklink.e2e.DebugE2EEntryPoint
import com.app.miklink.e2e.support.CleanupStatus
import com.app.miklink.e2e.support.CoreFixtures
import com.app.miklink.e2e.support.TestFixtureManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

fun appOnlyDependencies(): DebugE2EEntryPoint {
    val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    return EntryPointAccessors.fromApplication(context, DebugE2EEntryPoint::class.java)
}

fun assertCatalogMembership(scenarioId: String, featureGroup: FeatureGroup) {
    val scenario = E2ETestCatalog.find(scenarioId)
    assertNotNull("Missing catalog scenario $scenarioId", scenario)
    requireNotNull(scenario)
    assertTrue("$scenarioId must be probe-independent", !scenario.requiresLiveProbe)
    assertTrue("$scenarioId must cover ${featureGroup.id}", featureGroup in scenario.featureGroups)
}

fun <T> withCoreFixtures(
    sessionName: String,
    onCleanup: (com.app.miklink.e2e.support.CleanupResult) -> Unit = {},
    block: suspend (DebugE2EEntryPoint, CoreFixtures) -> T
): T = runBlocking {
    val dependencies = appOnlyDependencies()
    val manager = TestFixtureManager(
        sessionId = "$sessionName-${System.nanoTime()}",
        clients = dependencies.clientRepository(),
        profiles = dependencies.testProfileRepository(),
        reports = dependencies.reportRepository()
    )
    val fixtures = manager.createCoreFixtures()
    try {
        block(dependencies, fixtures)
    } finally {
        val cleanup = manager.cleanup()
        onCleanup(cleanup)
        assertEquals("Fixture cleanup must succeed", CleanupStatus.PASS, cleanup.status)
    }
}

fun launchAndAwaitResource(tag: String, timeoutMs: Long = 20_000L): UiDevice {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.executeShellCommand("am start -W -n com.app.miklink/.MainActivity")
    assertTrue("Resource id $tag was not visible", device.wait(Until.hasObject(By.res(tag)), timeoutMs))
    return device
}

fun disposableStateAllowed(): Boolean =
    InstrumentationRegistry.getArguments().getString("disposableLocalState").toBoolean()
