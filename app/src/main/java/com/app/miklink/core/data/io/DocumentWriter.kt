package com.app.miklink.core.data.io

interface DocumentWriter {
    suspend fun writeBytes(dest: DocumentDestination, bytes: ByteArray, mimeType: String): Result<Unit>
}
