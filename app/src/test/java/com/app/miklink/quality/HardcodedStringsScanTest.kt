package com.app.miklink.quality

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Test

internal object HardcodedUiTextScanner {
    private val patterns = listOf(
        Regex("""Text\s*\(\s*(?:text\s*=\s*)?\"([^\"]+)\"""", setOf(RegexOption.DOT_MATCHES_ALL)),
        Regex("""contentDescription\s*=\s*\"([^\"]+)\"""", setOf(RegexOption.DOT_MATCHES_ALL)),
        Regex("""showSnackbar\s*\(.*?message\s*=\s*\"([^\"]+)\"""", setOf(RegexOption.DOT_MATCHES_ALL)),
        Regex("""Toast\.makeText\s*\(.*?\"([^\"]+)\"""", setOf(RegexOption.DOT_MATCHES_ALL)),
        Regex("""UiState\.Error\s*\(\s*\"([^\"]+)\"""", setOf(RegexOption.DOT_MATCHES_ALL)),
        Regex("""(?:title|subtitle)\s*=\s*\"([^\"]+)\"""", setOf(RegexOption.DOT_MATCHES_ALL))
    )

    fun violations(source: String): List<String> = patterns.flatMap { pattern ->
        pattern.findAll(source).mapNotNull { match ->
            val literal = match.groupValues[1]
            val line = source.substring(0, match.range.first).count { it == '\n' } + 1
            val precedingLines = source.substring(0, match.range.first).lineSequence().toList().takeLast(2)
            val hasLocalIgnoreWithReason = precedingLines.any {
                Regex("""i18n-ignore:\s*\S.+""").containsMatchIn(it)
            }
            if (literal.isBlank() || literal.all { !it.isLetterOrDigit() && !it.isWhitespace() } || hasLocalIgnoreWithReason) null
            else "$line: $literal"
        }.toList()
    }.distinct()
}

class HardcodedStringsScanTest {
    @Test
    fun scansAllMainKotlinSources() {
        val sourceRoot: Path = sequenceOf(Paths.get("app", "src", "main"), Paths.get("src", "main")).first { Files.exists(it) }
        val violations = Files.walk(sourceRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .flatMap { path ->
                    if (path.toString().replace('\\', '/').contains("/data/")) java.util.stream.Stream.empty()
                    else HardcodedUiTextScanner.violations(Files.readString(path)).stream()
                        .map { "${sourceRoot.relativize(path)}:$it" }
                }
                .toList()
        }
        assertEquals("Hard-coded user-facing strings found:\n${violations.joinToString("\n")}", emptyList<String>(), violations)
    }

    @Test
    fun detectsInlineMultilineNamedAndAccessibleText() {
        val source = """
            Text("Inline")
            Text( text = "Named" )
            contentDescription =
                "Accessible"
            snackbarHostState.showSnackbar(
                message = "Snackbar",
            )
        """.trimIndent()
        assertEquals(4, HardcodedUiTextScanner.violations(source).size)
    }

    @Test
    fun detectsInterpolatedTextAndNamedComponentCopy() {
        val source = """
            Text("${'$'}count clients")
            MinimalListItem(subtitle = "No location specified")
            SectionHeader(title = "Client Info")
        """.trimIndent()

        assertEquals(3, HardcodedUiTextScanner.violations(source).size)
    }

    @Test
    fun honoursLocalTechnicalIgnore() {
        assertEquals(emptyList<String>(), HardcodedUiTextScanner.violations("// i18n-ignore: technical protocol token shown verbatim\nText(\"DHCP\")"))
    }
}
