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
    private val invariantResourceKeys = setOf(
        "app_name", "ok", "no", "dashboard_title", "dashboard_btn_report", "dashboard_section_socket",
        "pdf_col_ping", "pdf_col_tdr", "pdf_col_speed_test", "settings_build", "settings_developer",
        "test_execution_section_report", "status_info", "section_link", "section_lldp", "section_ping",
        "section_speed", "log_tdr_fail", "log_lldp_info", "test_progress_tdr_label",
        "test_progress_neighbors_label", "test_progress_ping_label", "quality_download_label", "quality_upload_label",
        "detail_label_gateway", "detail_label_dns", "link_status_na", "detail_label_target_number",
        "detail_label_target_generic", "test_details_ping_gateway_target", "test_details_ping_sample_label",
        "test_details_ping_sample_value", "detail_label_server", "detail_value_dhcp", "detail_value_link_ok",
        "detail_value_bound", "test_details_speed_ping", "test_details_speed_jitter", "client_edit_prefix_placeholder",
        "client_edit_separator_placeholder", "client_edit_suffix_placeholder", "client_edit_server_address_placeholder",
        "client_edit_username_placeholder", "client_edit_password_label", "history_tip_icon", "history_tip_bullet",
        "profile_edit_quick_fill_gateway", "profile_edit_quick_fill_google", "profile_edit_quick_fill_cloudflare",
        "profile_edit_target_label", "profile_edit_link_section_title", "profile_edit_threshold_ping",
        "profile_edit_threshold_jitter", "settings_language_en", "neighbor_protocol_cdp", "neighbor_protocol_lldp",
        "neighbor_protocol_mndp", "splash_shitworks_name"
    )

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

    @Test
    fun shitWorksTaglineIsCanonicalAndCannotBeLocalized() {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val defaultDocument = builder.parse(defaultStrings)
        val taglines = defaultDocument.getElementsByTagName("string")
        val tagline = (0 until taglines.length)
            .map { taglines.item(it) as Element }
            .single { it.getAttribute("name") == "splash_shitworks_tagline" }

        Assert.assertEquals("'cause shit always work", tagline.textContent.removeSurrounding("\""))
        Assert.assertEquals("false", tagline.getAttribute("translatable"))

        val localizedOverrides = defaultStrings.parentFile.parentFile
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") }
            .flatMap { directory ->
                directory.listFiles().orEmpty().filter { it.extension == "xml" }
            }
            .filter { file -> file.readText().contains("name=\"splash_shitworks_tagline\"") }
        Assert.assertTrue("Localized directories must not override the ShitWorks tagline", localizedOverrides.isEmpty())
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
                if (name !in invariantResourceKeys) {
                    Assert.assertNotEquals(
                        "Italian $kind $name/$quantity is accidentally identical to English",
                        english.trim(),
                        italianValues.getValue(quantity).trim()
                    )
                }
            }
        }
    }

    private fun placeholders(value: String): List<String> = Regex("%(?:\\d+\\$)?[a-zA-Z]").findAll(value).map { it.value }.toList()
}
