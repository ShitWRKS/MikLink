package com.app.miklink

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.domain.model.preferences.CustomPalette
import com.app.miklink.core.domain.model.preferences.ThemeConfig
import com.app.miklink.core.domain.usecase.preferences.ObserveThemeConfigUseCase
import com.app.miklink.ui.NavGraph
import com.app.miklink.ui.theme.MikLinkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var observeThemeConfigUseCase: ObserveThemeConfigUseCase

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            val themeConfig by observeThemeConfigUseCase().collectAsStateWithLifecycle(
                initialValue = ThemeConfig.FOLLOW_SYSTEM
            )
            val customPalette by userPreferencesRepository.customPalette.collectAsStateWithLifecycle(
                initialValue = CustomPalette()
            )

            val isDarkTheme = when (themeConfig) {
                ThemeConfig.LIGHT -> false
                ThemeConfig.DARK -> true
                ThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            }

            MikLinkTheme(
                darkTheme = isDarkTheme,
                customPrimaryInfo = customPalette.primary,
                customSecondaryInfo = customPalette.secondary,
                customBackgroundInfo = customPalette.background,
                customContentInfo = customPalette.content
            ) {
                NavGraph()
            }
        }
    }
}