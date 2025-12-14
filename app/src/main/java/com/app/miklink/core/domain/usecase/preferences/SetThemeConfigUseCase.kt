package com.app.miklink.core.domain.usecase.preferences

import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.domain.model.preferences.ThemeConfig
import javax.inject.Inject

interface SetThemeConfigUseCase {
    suspend operator fun invoke(config: ThemeConfig)
}

class SetThemeConfigUseCaseImpl @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : SetThemeConfigUseCase {
    override suspend fun invoke(config: ThemeConfig) {
        userPreferencesRepository.setTheme(config)
    }
}
