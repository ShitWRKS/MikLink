package com.app.miklink.core.domain.test.model

/**
 * Eventi emessi durante l'esecuzione di un test.
 * Serve per stream in Flow dal UseCase alla UI.
 */
sealed class TestEvent {
    data class Progress(val progress: TestProgress) : TestEvent()
    data class SectionsUpdated(val sections: List<TestSectionResult>) : TestEvent()
    data class Completed(val outcome: TestOutcome) : TestEvent()
    data class Failed(val error: TestError) : TestEvent()
}

