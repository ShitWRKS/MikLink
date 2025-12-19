/*
 * Purpose: Guardrail to ensure MikroTik repositories do not build Retrofit services directly.
 * Inputs: Source files under app/src/main/java/com/app/miklink/data/repository/mikrotik.
 * Outputs: Test failure when forbidden tokens (service factory/retrofit create) are found.
 * Notes: Keeps single transport path via MikroTikCallExecutor per ADR-0007/ADR-0002.
 */
package com.app.miklink.data.repository.mikrotik

import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class MikroTikRepositoryTransportGuardTest {

    @Test
    fun `mikrotik repos do not instantiate api services directly`() {
        val candidates = listOf(
            Paths.get("src/main/java/com/app/miklink/data/repository/mikrotik"),
            Paths.get("app/src/main/java/com/app/miklink/data/repository/mikrotik")
        )
        val repoDir: Path = candidates.firstOrNull { Files.exists(it) }
            ?: error("Repository directory not found in candidates: $candidates")

        val forbiddenTokens = listOf(
            "MikroTikServiceFactory",
            "create(MikroTikApiService",
            "Retrofit.Builder"
        )

        val offenders = mutableListOf<Pair<Path, String>>()
        Files.walk(repoDir).use { paths ->
            paths.filter { it.isRegularFile() && it.toString().endsWith(".kt") }
                .forEach { path ->
                    val content = path.readText()
                    forbiddenTokens
                        .filter { token -> content.contains(token) }
                        .forEach { token -> offenders.add(path to token) }
                }
        }

        val message = buildString {
            append("MikroTik repositories must use MikroTikCallExecutor. Forbidden tokens found: ")
            append(offenders.joinToString { (path, token) -> "${path.toAbsolutePath()} contains $token" })
        }
        assertTrue(message, offenders.isEmpty())
    }
}
