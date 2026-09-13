package com.app.miklink.ui.testing

import androidx.compose.ui.Modifier

/** Production policy: agent-oriented semantic resource IDs are never exposed. */
object AgentSemanticsConfig {
    const val enabled: Boolean = false

    fun rootModifier(): Modifier = Modifier
}
