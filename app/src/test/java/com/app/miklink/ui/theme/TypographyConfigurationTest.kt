package com.app.miklink.ui.theme

import androidx.compose.ui.text.TextStyle
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class TypographyConfigurationTest {
    @Test
    fun uiTypeScaleUsesTheProportionalFamily() {
        val uiStyles = listOf(
            Typography.displayLarge,
            Typography.displayMedium,
            Typography.displaySmall,
            Typography.headlineLarge,
            Typography.headlineMedium,
            Typography.headlineSmall,
            Typography.titleLarge,
            Typography.titleMedium,
            Typography.titleSmall,
            Typography.bodyLarge,
            Typography.bodyMedium,
            Typography.bodySmall,
            Typography.labelLarge,
            Typography.labelMedium,
            Typography.labelSmall
        )

        assertNotSame(UiFontFamily, TechnicalFontFamily)
        uiStyles.forEach { style: TextStyle ->
            assertSame(UiFontFamily, style.fontFamily)
        }
    }

    @Test
    fun technicalStylesUseJetBrainsMono() {
        assertSame(TechnicalFontFamily, MonoBody.fontFamily)
        assertSame(TechnicalFontFamily, MonoLabel.fontFamily)
    }
}
