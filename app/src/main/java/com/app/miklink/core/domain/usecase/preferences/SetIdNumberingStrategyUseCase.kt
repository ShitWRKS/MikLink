package com.app.miklink.core.domain.usecase.preferences

import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import javax.inject.Inject

interface SetIdNumberingStrategyUseCase {
    suspend operator fun invoke(strategy: IdNumberingStrategy)
}

class SetIdNumberingStrategyUseCaseImpl @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : SetIdNumberingStrategyUseCase {
    override suspend fun invoke(strategy: IdNumberingStrategy) {
        userPreferencesRepository.setIdNumberingStrategy(strategy)
    }
}
