package com.tanik.acr.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tanik.acr.MainActivity
import com.tanik.acr.R
import com.tanik.acr.audio.RecorderEngine
import com.tanik.acr.data.RecordingRepository
import com.tanik.acr.state.RecorderState
import com.tanik.acr.state.RecorderStateStore

class RecorderService : Service() {
    private lateinit var repository: RecordingRepository
    private val recorderEngine = RecorderEngine()

    override fun onCreate() {
        super.onCreate()
        repository = RecordingRepository(this)
        createNotificationChannel()
        syncState("Recorder idle")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_PAUSE_RECORDING -> pauseRecording()
            ACTION_RESUME_RECORDING -> resumeRecording()
            else -> {
                if (!recorderEngine.isRecording) {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (recorderEngine.isRecording) {
            recorderEngine.cancel()
        }
        RecorderStateStore.set(RecorderState())
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecording() {
        if (recorderEngine.isRecording) {
            updateNotification()
            return
        }

        startInForeground(buildNotification("Preparing recorder"))
        val outputFile = repository.createTempOutputFile()
        val started = try {
            recorderEngine.start(outputFile, android.media.MediaRecorder.AudioSource.MIC)
            true
        } catch (_: Exception) {
            false
        }

        if (!started) {
            outputFile.delete()
            syncState("Unable to start recorder")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        syncState("Recording in progress")
        updateNotification()
    }

    private fun stopRecording() {
        val stoppedFile = recorderEngine.stop()
        val status = when {
            stoppedFile == null -> "Recording stopped before it could be saved"
            repository.saveRecording(stoppedFile) -> "Recording saved"
            else -> "Unable to save recording"
        }
        syncState(status)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseRecording() {
        if (!recorderEngine.isRecording) {
            return
        }
        recorderEngine.pause()
        syncState("Recording paused")
        updateNotification()
    }

    private fun resumeRecording() {
        if (!recorderEngine.isRecording) {
            return
        }
        recorderEngine.resume()
        syncState("Recording in progress")
        updateNotification()
    }

    private fun syncState(status: String) {
        RecorderStateStore.set(
            RecorderState(
                recording = recorderEngine.isRecording,
                paused = recorderEngine.isPaused,
                activeFileName = recorderEngine.currentFile?.name,
                statusMessage = status
            )
        )
    }

    private fun updateNotification() {
        if (!recorderEngine.isRecording) {
            return
        }
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(currentStatus()))
        }
    }

    private fun currentStatus(): String {
        return if (recorderEngine.isPaused) "Recording paused" else "Recording in progress"
    }

    private fun buildNotification(statusText: String): android.app.Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(statusText)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, getString(R.string.notification_stop), servicePendingIntent(ACTION_STOP_RECORDING, 10))
            .addAction(
                0,
                getString(if (recorderEngine.isPaused) R.string.notification_resume else R.string.notification_pause),
                servicePendingIntent(if (recorderEngine.isPaused) ACTION_RESUME_RECORDING else ACTION_PAUSE_RECORDING, 11)
            )
            .build()
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, RecorderService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startInForeground(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "recorder"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_RECORDING = "com.tanik.acr.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.tanik.acr.action.STOP_RECORDING"
        const val ACTION_PAUSE_RECORDING = "com.tanik.acr.action.PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.tanik.acr.action.RESUME_RECORDING"
    }
}
