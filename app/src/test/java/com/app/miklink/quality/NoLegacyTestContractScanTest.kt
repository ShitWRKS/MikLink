/*
 * Purpose: Guard against reintroducing the legacy stringly test contract.
 * Inputs: File system scan of domain/UI test packages.
 * Outputs: Fails build if TestSectionResult/SectionsUpdated or Map<String,String> details reappear.
 * Notes: Keeps engine→UI pipeline typed per ADR-0011.
 */
package com.app.miklink.quality

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert
import org.junit.Test

class NoLegacyTestContractScanTest {

    @Test
    fun noLegacyContractArtifacts() {
        val root = Paths.get("app", "src", "main", "java")
        val domains = listOf(
            root.resolve(Paths.get("com", "app", "miklink", "core", "domain", "test")),
            root.resolve(Paths.get("com", "app", "miklink", "ui", "test"))
        )

        val violations = mutableListOf<String>()
        domains.forEach { dir ->
            if (!Files.exists(dir)) return@forEach
            Files.walk(dir).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .forEach { file ->
                        val content = file.toFile().readText()
                        if ("TestSectionResult" in content || "SectionsUpdated" in content) {
                            violations += "Legacy type reference in ${relative(root, file)}"
                        }
                        if (content.contains("Map<String, String>")) {
                            violations += "Map<String,String> contract found in ${relative(root, file)}"
                        }
                    }
            }
        }

        if (violations.isNotEmpty()) {
            Assert.fail(violations.joinToString(separator = "\n"))
        }
    }

    private fun relative(root: Path, file: Path): String = root.relativize(file).toString()
}
