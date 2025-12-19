/*
 * Purpose: Centralize MikLink palette v1.1 (gray accent) and semantic status colors.
 * Inputs: None (constants used across the UI theme layer).
 * Outputs: Color constants consumed by Theme.kt and downstream UI components.
 */
package com.app.miklink.ui.theme

import androidx.compose.ui.graphics.Color

// Gray accent (Pantone 424 C inspired) - dark scheme
val MikLinkPrimaryDark = Color(0xFF707372)
val MikLinkPrimaryContainerDark = Color(0xFF4B4F56)
val MikLinkOnPrimaryDark = Color(0xFFF4F4F4) // RAL 9003 inspired
val MikLinkOnPrimaryContainerDark = Color(0xFFE9EDF2)

// Gray accent (Pantone Cool Gray 2 C inspired) - light scheme
val MikLinkPrimaryLight = Color(0xFF4B4F56)
val MikLinkPrimaryContainerLight = Color(0xFFD1D0CE)
val MikLinkOnPrimaryLight = Color(0xFFF4F4F4)
val MikLinkOnPrimaryContainerLight = Color(0xFF0E0E10)

// Legacy aliases (default to dark) for compatibility where a single primary is expected
val MikLinkPrimary = MikLinkPrimaryDark
val MikLinkPrimaryContainer = MikLinkPrimaryContainerDark
val MikLinkOnPrimary = MikLinkOnPrimaryDark
val MikLinkOnPrimaryContainer = MikLinkOnPrimaryContainerDark
val MikLinkSecondary = MikLinkPrimary
val MikLinkTertiary = MikLinkPrimary

// Neutral / Backgrounds (dark-first)
val TechDarkBackground = Color(0xFF0E0E10)
val TechDarkSurface = Color(0xFF15181D)
val TechDarkSurfaceVariant = Color(0xFF1C2128)
val TechDarkOutline = Color(0xFF2A313A)
val TechTextHigh = Color(0xFFE9EDF2)
val TechTextMedium = Color(0xFFB7C0CB)
val TechTextDisabled = Color(0xFF7B8794)

// Neutral / Backgrounds (light)
val TechLightBackground = Color(0xFFF4F6F8)
val TechLightSurface = Color(0xFFFFFFFF)
val TechLightSurfaceVariant = Color(0xFFE4E9F0)
val TechLightOutline = Color(0xFFCBD3DE)
val TechLightTextHigh = Color(0xFF0E0E10)
val TechLightTextMedium = Color(0xFF1C2128)
val TechLightTextDisabled = Color(0xFF7B8794)

// Status / Feedback (greens/reds unchanged)
val StatusSuccess = Color(0xFF2F6F4E)
val StatusSuccessContainer = Color(0xFF173A2A)
val StatusOnSuccess = Color(0xFFE6F5EC)
val StatusOnSuccessContainer = Color(0xFF9EDDBE)

val StatusFailure = Color(0xFFB14A4A)
val StatusFailureContainer = Color(0xFF3A1B1B)
val StatusOnFailure = Color(0xFFF9E6E6)
val StatusOnFailureContainer = Color(0xFFF5C7C7)

// Running is tied to primary per scheme
val StatusRunningDark = MikLinkPrimaryDark
val StatusRunningDarkContainer = MikLinkPrimaryContainerDark
val StatusOnRunningDark = MikLinkOnPrimaryDark
val StatusOnRunningDarkContainer = MikLinkOnPrimaryContainerDark

val StatusRunningLight = MikLinkPrimaryLight
val StatusRunningLightContainer = MikLinkPrimaryContainerLight
val StatusOnRunningLight = MikLinkOnPrimaryLight
val StatusOnRunningLightContainer = MikLinkOnPrimaryContainerLight

val GlowSuccess = Color(0xFF44DE95)
val GlowFailure = Color(0xFFFF6B6B)
val GlowRunning = Color(0xFFD1D0CE)

// Legacy aliases preserved for backward compatibility
val TechGreen = StatusSuccess
val TechRed = StatusFailure
