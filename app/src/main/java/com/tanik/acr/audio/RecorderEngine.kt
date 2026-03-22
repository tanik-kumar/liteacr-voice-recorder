package com.tanik.acr.audio

import android.media.MediaRecorder
import java.io.File

class RecorderEngine {
    private var recorder: MediaRecorder? = null
    private var activeFile: File? = null
    private var paused: Boolean = false

    val isRecording: Boolean
        get() = recorder != null

    val isPaused: Boolean
        get() = paused

    val currentFile: File?
        get() = activeFile

    @Throws(Exception::class)
    fun start(outputFile: File, audioSource: Int) {
        check(recorder == null) { "Recorder is already active" }

        val mediaRecorder = MediaRecorder()
        try {
            mediaRecorder.setAudioSource(audioSource)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(44_100)
            mediaRecorder.setAudioEncodingBitRate(128_000)
            mediaRecorder.setOutputFile(outputFile.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
        } catch (error: Exception) {
            runCatching { mediaRecorder.reset() }
            mediaRecorder.release()
            outputFile.delete()
            throw error
        }

        recorder = mediaRecorder
        activeFile = outputFile
        paused = false
    }

    fun pause() {
        recorder?.let {
            if (!paused) {
                it.pause()
                paused = true
            }
        }
    }

    fun resume() {
        recorder?.let {
            if (paused) {
                it.resume()
                paused = false
            }
        }
    }

    fun stop(): File? {
        val file = activeFile
        val currentRecorder = recorder ?: return null
        return try {
            if (paused) {
                currentRecorder.resume()
            }
            currentRecorder.stop()
            file
        } catch (_: RuntimeException) {
            file?.delete()
            null
        } finally {
            currentRecorder.reset()
            currentRecorder.release()
            recorder = null
            activeFile = null
            paused = false
        }
    }

    fun cancel() {
        val file = activeFile
        val currentRecorder = recorder ?: return
        try {
            currentRecorder.reset()
        } finally {
            currentRecorder.release()
            recorder = null
            activeFile = null
            paused = false
            file?.delete()
        }
    }
}
