package com.app.miklink.data.pdf

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.print.PrintAttributes
import android.webkit.WebView
import android.webkit.WebViewClient
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.Report
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    private companion object {
        // A4 dimensions in 1/72 of an inch (points)
        const val A4_WIDTH_PT = 595
        const val A4_HEIGHT_PT = 842
    }

    suspend fun populateSingleReportTemplate(report: Report, client: Client?): String {
        val template = withContext(Dispatchers.IO) { context.assets.open("report_template.html").bufferedReader().use { it.readText() } }
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(report.timestamp))

        return template
            .replace("{{CLIENT_NAME}}", client?.companyName ?: "N/A")
            .replace("{{SOCKET_ID}}", report.socketName ?: "N/A")
            .replace("{{TEST_DATE_TIME}}", formattedDate)
            .replace("{{RESULTS_JSON}}", report.resultsJson) 
    }

    suspend fun populateProjectReportTemplate(reports: List<Report>, client: Client?): String {
        val template = withContext(Dispatchers.IO) { context.assets.open("project_report_template.html").bufferedReader().use { it.readText() } }
        val rows = reports.joinToString("") { report ->
            "<tr>"
                .plus("<td>${report.socketName ?: "N/A"}</td>")
                .plus("<td>${report.floor ?: ""}/${report.room ?: ""}</td>")
                .plus("<td>${report.overallStatus}</td>")
                .plus("<td>N/A</td>") 
                .plus("<td>N/A</td>") 
                .plus("</tr>")
        }
        return template
            .replace("{{CLIENT_NAME}}", client?.companyName ?: "Unknown")
            .replace("{{EXPORT_DATE}}", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            .replace("{{TABLE_ROWS}}", rows)
    }

    suspend fun createPdf(htmlContent: String, outputUri: Uri) {
        withContext(Dispatchers.Main) {
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    createPdfFromWebView(view, outputUri)
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
        }
    }

    private fun createPdfFromWebView(webView: WebView, outputUri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return

        val density = context.resources.displayMetrics.density
        val widthPx = (A4_WIDTH_PT * density).toInt()
        val heightPx = (A4_HEIGHT_PT * density).toInt()

        // Force the webview to layout itself
        webView.layout(0, 0, widthPx, heightPx)

        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, 1).create()
        val document = PdfDocument()
        val page = document.startPage(pageInfo)
        
        // Draw the webview to the PDF canvas
        webView.draw(page.canvas)
        document.finishPage(page)

        try {
            context.contentResolver.openFileDescriptor(outputUri, "w")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use {
                    document.writeTo(it)
                }
            }
        } catch (e: Exception) {
            // Log error
        } finally {
            document.close()
            webView.destroy()
        }
    }
}
