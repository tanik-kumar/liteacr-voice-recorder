package com.tanik.acr.data

import android.net.Uri

data class RecordingFile(
    val uri: Uri,
    val fileName: String,
    val displayName: String,
    val modifiedAt: Long,
    val sizeBytes: Long,
    val durationMs: Long,
    val pathLabel: String
)
