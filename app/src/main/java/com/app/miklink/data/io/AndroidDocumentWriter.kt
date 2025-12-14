package com.app.miklink.data.io

import android.content.Context
import android.net.Uri
import com.app.miklink.core.data.io.DocumentDestination
import com.app.miklink.core.data.io.DocumentWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AndroidDocumentWriter @Inject constructor(
    @ApplicationContext private val context: Context
) : DocumentWriter {
    override suspend fun writeBytes(dest: DocumentDestination, bytes: ByteArray, mimeType: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(dest.uriString)
            context.contentResolver.openOutputStream(uri, "w").use { outputStream ->
                requireNotNull(outputStream) { "Cannot open output stream for $uri" }
                outputStream.write(bytes)
                outputStream.flush()
            }
        }
    }
}
