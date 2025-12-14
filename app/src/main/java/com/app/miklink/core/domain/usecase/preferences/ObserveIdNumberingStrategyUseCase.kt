package com.app.miklink.core.domain.usecase.preferences

import com.app.miklink.core.data.repository.preferences.UserPreferencesRepository
import com.app.miklink.core.domain.model.preferences.IdNumberingStrategy
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface ObserveIdNumberingStrategyUseCase {
    operator fun invoke(): Flow<IdNumberingStrategy>
}

class ObserveIdNumberingStrategyUseCaseImpl @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ObserveIdNumberingStrategyUseCase {
    override fun invoke(): Flow<IdNumberingStrategy> = userPreferencesRepository.idNumberingStrategy
}
