package com.app.miklink.e2e.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.app.miklink.ui.dashboard.DashboardTags
import com.app.miklink.ui.testing.AgentSemanticsConfig
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgentSemanticsIsolationTest {
    @Test
    fun debugPolicyExposesComposeTagsAsResourceIds() {
        assertTrue("androidTest must be compiled against the debug policy", AgentSemanticsConfig.enabled)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val launch = device.executeShellCommand(
            "am start -W -n com.app.miklink/.MainActivity"
        )
        assertTrue("MainActivity launch failed: $launch", launch.contains("Status: ok"))
        assertTrue(
            "Expected dashboard Compose tag as a UI Automator resource id",
            device.wait(Until.hasObject(By.res(DashboardTags.SCREEN)), 20_000L)
        )
    }
}
