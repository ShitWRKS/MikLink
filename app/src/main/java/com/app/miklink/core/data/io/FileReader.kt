package com.app.miklink.core.data.io

import android.net.Uri

interface FileReader {
    suspend fun read(uri: Uri): String?
}

