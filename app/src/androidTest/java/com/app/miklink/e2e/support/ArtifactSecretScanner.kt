package com.app.miklink.e2e.support

import java.io.File

data class SecretFinding(val file: String, val canary: String)

class ArtifactSecretScanner(private val canaries: Set<String>) {
    init {
        require(canaries.isNotEmpty())
        require(canaries.none { it.isBlank() })
    }

    fun scan(files: Iterable<File>): List<SecretFinding> = buildList {
        files.filter { it.isFile }.forEach { file ->
            val bytes = file.readBytes()
            canaries.forEach { canary ->
                if (bytes.containsSubsequence(canary.toByteArray(Charsets.UTF_8))) {
                    add(SecretFinding(file.name, canary))
                }
            }
        }
    }

    private fun ByteArray.containsSubsequence(needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > size) return false
        return (0..size - needle.size).any { start ->
            needle.indices.all { offset -> this[start + offset] == needle[offset] }
        }
    }
}
