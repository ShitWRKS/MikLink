package com.app.miklink.quality

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element

class StringTitleCapitalizationTest {
    @Test
    fun titleResourcesUseTitleCaseInEveryLocale() {
        val resourceRoot = sequenceOf(
            File("app/src/main/res"),
            File("src/main/res")
        ).first { it.exists() }
        val stringFiles = resourceRoot.listFiles().orEmpty()
            .filter { it.isDirectory && (it.name == "values" || it.name.startsWith("values-")) }
            .map { File(it, "strings.xml") }
            .filter(File::exists)

        val violations = stringFiles.flatMap(::titleCaseViolations)

        assertEquals(
            "Title resources must capitalize significant words:\n${violations.joinToString("\n")}",
            emptyList<String>(),
            violations
        )
    }

    private fun titleCaseViolations(file: File): List<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = document.getElementsByTagName("string")
        return (0 until strings.length).mapNotNull { index ->
            val element = strings.item(index) as Element
            val name = element.getAttribute("name")
            val value = element.textContent.trim()
            if (!isTitleResource(name) || value.endsWithSentencePunctuation()) return@mapNotNull null
            val invalidWords = WORD.findAll(value.withoutPlaceholders())
                .map { it.value }
                .filterNot { it.lowercase() in LOWERCASE_CONNECTORS }
                .filter { word -> word.firstOrNull()?.isLowerCase() == true }
                .toList()
            invalidWords.takeIf(List<String>::isNotEmpty)?.let {
                "${file.parentFile?.name.orEmpty()}/$name: '$value' (${it.joinToString()})"
            }
        }
    }

    private fun isTitleResource(name: String): Boolean =
        name.startsWith("title_") ||
            name.endsWith("_title") ||
            name.endsWith("_header") ||
            name.endsWith("_section") ||
            name.contains("_section_title") ||
            name.startsWith("settings_category_")

    private fun String.endsWithSentencePunctuation(): Boolean =
        lastOrNull() in setOf('.', '!', '?', ':', '…')

    private fun String.withoutPlaceholders(): String =
        replace(Regex("%(?:\\d+\\$)?[a-zA-Z]|\\\\n"), "")

    private companion object {
        val WORD = Regex("[\\p{L}][\\p{L}\\p{M}'’/-]*")
        val LOWERCASE_CONNECTORS = setOf(
            "a", "al", "alla", "alle", "allo", "ai", "agli", "and", "as", "at", "by",
            "con", "da", "dal", "dalla", "dalle", "dallo", "dai", "dagli", "di", "del",
            "della", "delle", "dello", "dei", "degli", "e", "for", "from", "in", "nel",
            "nella", "nelle", "nello", "nei", "negli", "o", "of", "on", "or", "per", "su",
            "sul", "sulla", "sulle", "sullo", "sui", "sugli", "the", "to", "with"
        )
    }
}
