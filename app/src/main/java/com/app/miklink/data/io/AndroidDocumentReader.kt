package com.app.miklink.data.io

import android.content.Context
import android.net.Uri
import com.app.miklink.core.data.io.DocumentDestination
import com.app.miklink.core.data.io.DocumentReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AndroidDocumentReader @Inject constructor(
    @ApplicationContext private val context: Context
) : DocumentReader {
    override suspend fun readText(dest: DocumentDestination): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(dest.uriString)
            context.contentResolver.openInputStream(uri).use { inputStream ->
                requireNotNull(inputStream) { "Cannot open input stream for $uri" }
                inputStream.bufferedReader().use { reader -> reader.readText() }
            }
        }
    }
}
