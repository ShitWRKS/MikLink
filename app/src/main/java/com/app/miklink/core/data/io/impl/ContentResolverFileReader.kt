package com.app.miklink.core.data.io.impl

import android.content.Context
import android.net.Uri
import com.app.miklink.core.data.io.FileReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContentResolverFileReader @Inject constructor(
    private val context: Context
) : FileReader {
    override suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
        } catch (e: Exception) {
            null
        }
    }
}

