package com.tanik.acr.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecorderState(
    val recording: Boolean = false,
    val paused: Boolean = false,
    val activeFileName: String? = null,
    val statusMessage: String = "Recorder idle"
)

object RecorderStateStore {
    private val mutableState = MutableStateFlow(RecorderState())
    val state: StateFlow<RecorderState> = mutableState.asStateFlow()

    fun set(value: RecorderState) {
        mutableState.value = value
    }
}
