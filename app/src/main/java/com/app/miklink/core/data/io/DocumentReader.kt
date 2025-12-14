package com.app.miklink.core.data.io

interface DocumentReader {
    suspend fun readText(dest: DocumentDestination): Result<String>
}
