package com.tanik.acr

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tanik.acr.data.RecordingFile
import com.tanik.acr.data.RecordingRepository
import com.tanik.acr.ui.RecordingsAdapter

class RecordingsActivity : AppCompatActivity() {
    private lateinit var repository: RecordingRepository
    private lateinit var recordingsAdapter: RecordingsAdapter
    private lateinit var recordingsPathText: TextView
    private lateinit var playbackStatusText: TextView
    private lateinit var emptyStateText: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var playingUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recordings)
        title = getString(R.string.recordings)

        repository = RecordingRepository(this)
        recordingsPathText = findViewById(R.id.recordingsPathText)
        playbackStatusText = findViewById(R.id.playbackStatusText)
        emptyStateText = findViewById(R.id.emptyStateText)

        setupRecordingList()
        findViewById<MaterialButton>(R.id.refreshButton).setOnClickListener {
            refreshRecordings()
        }

        recordingsPathText.text = getString(R.string.recordings_folder_value, repository.recordingsFolderLabel())
        refreshRecordings()
        updatePlaybackStatus(null)
    }

    override fun onStop() {
        super.onStop()
        stopPlayback(updateStatus = false)
    }

    private fun setupRecordingList() {
        recordingsAdapter = RecordingsAdapter(
            onPlayToggle = ::togglePlayback,
            onShare = ::shareRecording,
            onDelete = ::deleteRecording
        )
        findViewById<RecyclerView>(R.id.recordingsRecyclerView).adapter = recordingsAdapter
    }

    private fun refreshRecordings() {
        val recordings = repository.listRecordings()
        recordingsAdapter.submitList(recordings)
        recordingsAdapter.setPlayingUri(playingUri)
        emptyStateText.text = if (recordings.isEmpty()) {
            getString(R.string.no_recordings)
        } else {
            getString(R.string.recordings_count, recordings.size)
        }
    }

    private fun togglePlayback(recording: RecordingFile) {
        if (playingUri == recording.uri.toString()) {
            stopPlayback(updateStatus = true)
            updatePlaybackStatus(null)
            return
        }

        stopPlayback(updateStatus = false)

        val player = MediaPlayer()
        try {
            player.setDataSource(this, recording.uri)
            player.setOnCompletionListener {
                stopPlayback(updateStatus = false)
                updatePlaybackStatus(null)
            }
            player.prepare()
            player.start()
            mediaPlayer = player
            playingUri = recording.uri.toString()
            recordingsAdapter.setPlayingUri(playingUri)
            updatePlaybackStatus(recording)
        } catch (_: Exception) {
            player.release()
            playingUri = null
            recordingsAdapter.setPlayingUri(null)
            Toast.makeText(this, R.string.play_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlayback(updateStatus: Boolean) {
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.stop()
                }
            }
            player.release()
        }
        mediaPlayer = null
        playingUri = null
        recordingsAdapter.setPlayingUri(null)
        if (updateStatus) {
            playbackStatusText.text = getString(R.string.playback_idle)
        }
    }

    private fun updatePlaybackStatus(recording: RecordingFile?) {
        playbackStatusText.text = if (recording == null) {
            getString(R.string.playback_idle)
        } else {
            getString(R.string.playing_file_value, recording.pathLabel)
        }
    }

    private fun shareRecording(recording: RecordingFile) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, recording.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_recording_title)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteRecording(recording: RecordingFile) {
        if (playingUri == recording.uri.toString()) {
            stopPlayback(updateStatus = false)
            updatePlaybackStatus(null)
        }
        if (repository.delete(recording)) {
            refreshRecordings()
        } else {
            Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
