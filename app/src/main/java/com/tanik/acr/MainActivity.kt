package com.tanik.acr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.tanik.acr.data.RecordingRepository
import com.tanik.acr.service.RecorderService
import com.tanik.acr.state.RecorderState
import com.tanik.acr.state.RecorderStateStore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var repository: RecordingRepository

    private lateinit var permissionStatusText: TextView
    private lateinit var serviceStatusText: TextView
    private lateinit var pathStatusText: TextView
    private lateinit var recordingsSummaryText: TextView
    private lateinit var grantPermissionsButton: MaterialButton
    private lateinit var recordButton: MaterialButton
    private lateinit var pauseResumeButton: MaterialButton
    private lateinit var openRecordingsButton: MaterialButton

    private var pendingStart = false
    private var lastKnownRecording = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissionSummary()
        if (pendingStart && hasAudioPermission()) {
            startRecorderService(RecorderService.ACTION_START_RECORDING)
        }
        pendingStart = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = getString(R.string.app_name)

        repository = RecordingRepository(this)

        bindViews()
        setupActions()
        refreshPermissionSummary()
        refreshRecordingsSummary()
        renderState(RecorderStateStore.state.value)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                RecorderStateStore.state.collect { state ->
                    renderState(state)
                    if (lastKnownRecording && !state.recording) {
                        refreshRecordingsSummary()
                    }
                    lastKnownRecording = state.recording
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionSummary()
        refreshRecordingsSummary()
    }

    private fun bindViews() {
        permissionStatusText = findViewById(R.id.permissionStatusText)
        serviceStatusText = findViewById(R.id.serviceStatusText)
        pathStatusText = findViewById(R.id.pathStatusText)
        recordingsSummaryText = findViewById(R.id.recordingsSummaryText)
        grantPermissionsButton = findViewById(R.id.grantPermissionsButton)
        recordButton = findViewById(R.id.recordButton)
        pauseResumeButton = findViewById(R.id.pauseResumeButton)
        openRecordingsButton = findViewById(R.id.openRecordingsButton)
    }

    private fun setupActions() {
        grantPermissionsButton.setOnClickListener {
            pendingStart = false
            requestPermissions()
        }

        recordButton.setOnClickListener {
            val state = RecorderStateStore.state.value
            if (state.recording) {
                startRecorderService(RecorderService.ACTION_STOP_RECORDING)
            } else if (!hasAudioPermission()) {
                pendingStart = true
                requestPermissions()
            } else {
                startRecorderService(RecorderService.ACTION_START_RECORDING)
            }
        }

        pauseResumeButton.setOnClickListener {
            val state = RecorderStateStore.state.value
            val action = if (state.paused) {
                RecorderService.ACTION_RESUME_RECORDING
            } else {
                RecorderService.ACTION_PAUSE_RECORDING
            }
            startRecorderService(action)
        }

        openRecordingsButton.setOnClickListener {
            startActivity(Intent(this, RecordingsActivity::class.java))
        }
    }

    private fun renderState(state: RecorderState) {
        serviceStatusText.text = buildString {
            append(state.statusMessage)
            state.activeFileName?.let { name ->
                append("\n")
                append(name)
            }
        }
        pathStatusText.text = getString(R.string.recordings_folder_value, repository.recordingsFolderLabel())
        recordButton.text = if (state.recording) {
            getString(R.string.stop_recording)
        } else {
            getString(R.string.start_recording)
        }
        pauseResumeButton.isEnabled = state.recording
        pauseResumeButton.text = if (state.paused) getString(R.string.resume) else getString(R.string.pause)
    }

    private fun refreshPermissionSummary() {
        val messages = mutableListOf<String>()
        if (!hasAudioPermission()) {
            messages += getString(R.string.permission_missing_audio)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            messages += getString(R.string.permission_missing_notifications)
        }
        permissionStatusText.text = if (messages.isEmpty()) getString(R.string.permission_ok) else messages.joinToString("\n")
    }

    private fun refreshRecordingsSummary() {
        recordingsSummaryText.text = getString(R.string.recordings_count, repository.listRecordings().size)
        pathStatusText.text = getString(R.string.recordings_folder_value, repository.recordingsFolderLabel())
    }

    private fun requestPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startRecorderService(action: String) {
        val intent = Intent(this, RecorderService::class.java).setAction(action)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}
