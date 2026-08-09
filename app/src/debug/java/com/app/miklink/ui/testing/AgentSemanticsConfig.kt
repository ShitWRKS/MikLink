package com.app.miklink.ui.testing

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/** Compile-time debug policy for adb/UI Automator discovery of Compose test tags. */
object AgentSemanticsConfig {
    const val enabled: Boolean = true

    fun rootModifier(): Modifier = Modifier.semantics {
        testTagsAsResourceId = true
    }
}
