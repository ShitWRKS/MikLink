package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.app.miklink.e2e.support.ScenarioRule
import com.app.miklink.e2e.support.dismissKeyguardIfPossible
import com.app.miklink.ui.dashboard.DashboardTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LifecycleScenarioTest {
    @get:Rule val scenarioRule = ScenarioRule.catalog("lifecycle-resume")

    @Test
    fun foregroundBackgroundResumeKeepsTheSameUsableSurface() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (!device.dismissKeyguardIfPossible(context)) {
            scenarioRule.notRun("DEVICE_LOCKED", "device-unlocked")
        }
        device.executeShellCommand("am start -W -n com.app.miklink/.MainActivity")
        assertTrue(
            "Dashboard was not visible before lifecycle transition",
            device.wait(Until.hasObject(By.res(DashboardTags.SCREEN)), 20_000)
        )
        device.pressHome()
        assertTrue("App did not enter background", device.wait(Until.gone(By.res(DashboardTags.SCREEN)), 5_000))
        device.executeShellCommand("am start -W -n com.app.miklink/.MainActivity")
        assertTrue(
            "Dashboard was not restored after resume",
            device.wait(Until.hasObject(By.res(DashboardTags.SCREEN)), 10_000)
        )
        assertEquals("com.app.miklink", device.currentPackageName)
    }
}
