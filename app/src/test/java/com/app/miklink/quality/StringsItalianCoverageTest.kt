package com.app.miklink.quality

import org.junit.Assert
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringsItalianCoverageTest {

    private val defaultStrings = File("app/src/main/res/values/strings.xml")
    private val italianStrings = File("app/src/main/res/values-it/strings.xml")

    // Exceptions: keys that are allowed to be missing in Italian (prefixes)
    private val allowedPrefixes = listOf("app_name")

    @Test
    fun allTranslatableStringsHaveItalianTranslation() {
        if (!defaultStrings.exists()) return
        if (!italianStrings.exists()) {
            Assert.fail("Missing file: app/src/main/res/values-it/strings.xml")
        }

        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()

        val defaultDoc = dBuilder.parse(defaultStrings)
        defaultDoc.documentElement.normalize()

        val italianDoc = dBuilder.parse(italianStrings)
        italianDoc.documentElement.normalize()

        val defaultNodes = defaultDoc.getElementsByTagName("string")
        val italianNodes = italianDoc.getElementsByTagName("string")

        val italianKeys = mutableSetOf<String>()
        for (i in 0 until italianNodes.length) {
            val node = italianNodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            italianKeys.add(name)
        }

        val missing = mutableListOf<String>()
        for (i in 0 until defaultNodes.length) {
            val node = defaultNodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
            val translatable = node.attributes?.getNamedItem("translatable")?.nodeValue
            if (translatable == "false") continue
            if (allowedPrefixes.any { name.startsWith(it) }) continue
            if (!italianKeys.contains(name)) missing.add(name)
        }

        if (missing.isNotEmpty()) {
            Assert.fail("Missing Italian translations for keys:\n" + missing.joinToString("\n"))
        }
    }
}
