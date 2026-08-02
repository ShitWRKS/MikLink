package com.app.miklink.quality

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

class StringsItalianCoverageTest {
    private val defaultStrings = resourceFile("values")
    private val italianStrings = resourceFile("values-it")

    private fun resourceFile(valuesDirectory: String): File = sequenceOf(
        File("app/src/main/res/$valuesDirectory/strings.xml"),
        File("src/main/res/$valuesDirectory/strings.xml")
    ).first { it.exists() }

    @Test
    fun allTranslatableResourcesHaveCompatibleItalianTranslations() {
        Assert.assertTrue("Missing default strings", defaultStrings.exists())
        Assert.assertTrue("Missing Italian strings", italianStrings.exists())
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        compare(stringValues(builder.parse(defaultStrings)), stringValues(builder.parse(italianStrings)), "string")
        compare(pluralValues(builder.parse(defaultStrings)), pluralValues(builder.parse(italianStrings)), "plural")
    }

    private fun stringValues(document: Document): Map<String, Map<String, String>> {
        val values = linkedMapOf<String, Map<String, String>>()
        val nodes = document.getElementsByTagName("string")
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            if (element.getAttribute("translatable") != "false") values[element.getAttribute("name")] = mapOf("value" to element.textContent)
        }
        return values
    }

    private fun pluralValues(document: Document): Map<String, Map<String, String>> {
        val values = linkedMapOf<String, Map<String, String>>()
        val nodes = document.getElementsByTagName("plurals")
        for (index in 0 until nodes.length) {
            val plural = nodes.item(index) as Element
            val quantities = linkedMapOf<String, String>()
            val items = plural.getElementsByTagName("item")
            for (itemIndex in 0 until items.length) {
                val item = items.item(itemIndex) as Element
                quantities[item.getAttribute("quantity")] = item.textContent
            }
            values[plural.getAttribute("name")] = quantities
        }
        return values
    }

    private fun compare(expected: Map<String, Map<String, String>>, actual: Map<String, Map<String, String>>, kind: String) {
        Assert.assertEquals("Italian $kind keys differ", expected.keys, actual.keys)
        expected.forEach { (name, values) ->
            val italianValues = actual.getValue(name)
            Assert.assertEquals("Italian quantities differ for $name", values.keys, italianValues.keys)
            values.forEach { (quantity, english) ->
                Assert.assertEquals("Placeholder mismatch for $name/$quantity", placeholders(english), placeholders(italianValues.getValue(quantity)))
            }
        }
    }

    private fun placeholders(value: String): List<String> = Regex("%(?:\\d+\\$)?[a-zA-Z]").findAll(value).map { it.value }.toList()
}
