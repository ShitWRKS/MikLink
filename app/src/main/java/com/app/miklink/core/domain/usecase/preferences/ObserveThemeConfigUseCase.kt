package com.app.miklink.core.domain.usecase.preferences

import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.domain.model.preferences.ThemeConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface ObserveThemeConfigUseCase {
    operator fun invoke(): Flow<ThemeConfig>
}

class ObserveThemeConfigUseCaseImpl @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ObserveThemeConfigUseCase {
    override fun invoke(): Flow<ThemeConfig> = userPreferencesRepository.themeConfig
}
