package com.app.miklink.core.domain.usecase.test

import com.app.miklink.core.domain.test.model.TestEvent
import com.app.miklink.core.domain.test.model.TestPlan
import kotlinx.coroutines.flow.Flow

/**
 * UseCase per eseguire un test completo.
 * Orchestra tutti gli step necessari e emette eventi via Flow.
 */
interface RunTestUseCase {
    fun execute(plan: TestPlan): Flow<TestEvent>
}

