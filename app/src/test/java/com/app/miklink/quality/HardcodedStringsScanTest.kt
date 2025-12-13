package com.app.miklink.quality

import org.junit.Assert
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

class HardcodedStringsScanTest {

    private val projectRoot: Path = Paths.get("app", "src", "main", "java")

    // Patterns to detect (simple, line-based patterns as specified)
    private val textPattern = Pattern.compile("Text\\s*\\(\\s*\".*?\".*\\)")
    private val textNamedPattern = Pattern.compile("Text\\s*\\(.*text\\s*=\\s*\".*?\".*\\)")
    private val contentDescPattern = Pattern.compile("contentDescription\\s*=\\s*\".*?\"")

    // Allowlist: empty string or strings made only of punctuation/symbols
    private val allowSymbolOnly = Pattern.compile("^\\p{Punct}+$")

    @Test
    fun scanUiForHardcodedStrings() {
        val uiDirs = listOf(
            Paths.get("com", "app", "miklink", "ui"),
            Paths.get("com", "app", "miklink", "core", "presentation")
        )

        val violations = mutableListOf<String>()

        if (!Files.exists(projectRoot)) {
            // Nothing to scan
            return
        }

        Files.walk(projectRoot).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .forEach { filePath ->
                    val relative = projectRoot.relativize(filePath).toString().replace('\\', '/')
                    val inUi = uiDirs.any { relative.startsWith(it.toString().replace('\\', '/')) }
                    if (!inUi) return@forEach

                    val lines = filePath.toFile().readLines()
                    for ((idx, raw) in lines.withIndex()) {
                        val line = raw.trim()
                        // ignore explicit i18n-ignore marker
                        if (line.contains("// i18n-ignore")) continue

                        // check patterns
                        val matched = (textPattern.matcher(line).find()
                                || textNamedPattern.matcher(line).find()
                                || contentDescPattern.matcher(line).find())

                        if (matched) {
                            // extract the string literal if possible
                            val literalRegex = Pattern.compile("\"(.*)\"")
                            val m = literalRegex.matcher(line)
                            var skip = false
                            if (m.find()) {
                                val content = m.group(1) ?: ""
                                if (content.isEmpty()) skip = true
                                if (allowSymbolOnly.matcher(content).matches()) skip = true
                            }

                            if (!skip) {
                                val snippet = line.trim()
                                violations.add("HARD_CODED_UI_TEXT: $relative:${idx + 1} -> $snippet")
                            }
                        }
                    }
                }
        }

        if (violations.isNotEmpty()) {
            val header = violations.joinToString("\n")
            val fix = "\n\nFIX (standard)\n" +
                    "crea una key in res/values/strings.xml\n" +
                    "crea la traduzione in res/values-it/strings.xml\n" +
                    "sostituisci con stringResource(R.string.<key>) (o context.getString(...) se non sei in composable)\n"

            Assert.fail(header + fix)
        }
    }
}
