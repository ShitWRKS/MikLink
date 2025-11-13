package com.app.miklink.utils

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.app.miklink.ui.history.model.ParsedResults
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {

    private fun populateTemplate(report: Report, client: Client?): String {
        val templateHtml = context.assets.open("report_template.html").bufferedReader().use { it.readText() }
        val jsonAdapter = moshi.adapter(ParsedResults::class.java)
        val results = try {
            jsonAdapter.fromJson(report.resultsJson)
        } catch (e: Exception) {
            null
        }

        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(report.timestamp))

        return templateHtml
            .replace("{{CLIENT_NAME}}", client?.companyName ?: "N/A")
            .replace("{{SOCKET_ID}}", report.socketName ?: "N/A")
            .replace("{{TEST_DATE_TIME}}", formattedDate)
            // ... add all other placeholders
    }

    suspend fun createPdf(report: Report, client: Client?, fileName: String): Boolean {
        val htmlContent = populateTemplate(report, client)

        return withContext(Dispatchers.Main) {
            try {
                val webView = WebView(context)
                var pdfGenerated = false

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        generatePdfFromWebView(view, fileName)
                        pdfGenerated = true
                    }
                }

                webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)

                var waitTime = 0
                while (!pdfGenerated && waitTime < 5000) {
                    delay(100)
                    waitTime += 100
                }
                pdfGenerated
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun generatePdfFromWebView(webView: WebView, fileName: String) {
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "PDF", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val pdfDocument = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(
            printAttributes.mediaSize!!.widthMils * 72 / 1000,
            printAttributes.mediaSize!!.heightMils * 72 / 1000,
            1
        ).create()

        val page = pdfDocument.startPage(pageInfo)
        webView.draw(page.canvas)
        pdfDocument.finishPage(page)

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        try {
            FileOutputStream(file).use {
                pdfDocument.writeTo(it)
            }
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }

        pdfDocument.close()
        webView.destroy()
    }
}
