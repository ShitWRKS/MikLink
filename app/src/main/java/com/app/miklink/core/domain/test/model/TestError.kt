package com.app.miklink.core.domain.test.model

/**
 * Errori possibili durante l'esecuzione di un test.
 * Sealed class per type-safe error handling.
 * 
 * TODO: Espandere con altri tipi di errore se necessario durante l'implementazione.
 */
sealed class TestError {
    abstract val message: String
    
    data class NetworkError(override val message: String) : TestError()
    data class AuthError(override val message: String) : TestError()
    data class Timeout(override val message: String) : TestError()
    data class Unsupported(override val message: String) : TestError()
    data class Unexpected(override val message: String, val cause: Throwable? = null) : TestError()
}

