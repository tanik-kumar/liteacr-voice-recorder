package com.tanik.acr.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordingRepository(context: Context) {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val nameFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun createTempOutputFile(): File {
        val directory = File(appContext.cacheDir, "pending_recordings")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, "recording_${nameFormatter.format(Date())}.m4a")
    }

    fun saveRecording(tempFile: File): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, tempFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, recordingsRelativePath())
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(audioCollection(), values) ?: return false
        return try {
            contentResolver.openOutputStream(uri, "w")?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return false

            contentResolver.update(
                uri,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                },
                null,
                null
            )
            true
        } catch (_: Exception) {
            contentResolver.delete(uri, null, null)
            false
        } finally {
            tempFile.delete()
        }
    }

    fun listRecordings(): List<RecordingFile> {
        val columns = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.Audio.AudioColumns.DURATION
        )

        val recordings = mutableListOf<RecordingFile>()
        contentResolver.query(
            audioCollection(),
            columns,
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(recordingsRelativePath()),
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val fileName = cursor.getString(nameIndex)
                val relativePath = cursor.getString(pathIndex) ?: recordingsRelativePath()
                recordings += RecordingFile(
                    uri = ContentUris.withAppendedId(audioCollection(), id),
                    fileName = fileName,
                    displayName = fileName.substringBeforeLast('.').replace('_', ' '),
                    modifiedAt = cursor.getLong(modifiedIndex) * 1000L,
                    sizeBytes = cursor.getLong(sizeIndex),
                    durationMs = cursor.getLong(durationIndex),
                    pathLabel = relativePath + fileName
                )
            }
        }
        return recordings
    }

    fun recordingsFolderLabel(): String = recordingsRelativePath().removeSuffix("/")

    fun delete(recording: RecordingFile): Boolean {
        return contentResolver.delete(recording.uri, null, null) > 0
    }

    private fun audioCollection(): Uri {
        return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    }

    private fun recordingsRelativePath(): String {
        return "${Environment.DIRECTORY_MUSIC}/LiteACR/"
    }

    @Suppress("unused")
    private fun readDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }
}
