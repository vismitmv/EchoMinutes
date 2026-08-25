package com.vismitmv.echominutes.ui.record

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vismitmv.echominutes.audio.AudioRecorder
import com.vismitmv.echominutes.data.db.AppDatabase
import com.vismitmv.echominutes.data.db.MeetingEntity
import com.vismitmv.echominutes.data.prefs.SecurePrefs
import com.vismitmv.echominutes.gemini.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordUiState(
    val isRecording: Boolean = false,
    val isProcessing: Boolean = false,
    val elapsedSeconds: Int = 0,
    val error: String? = null,
    val navigateToResult: Long? = null,
    val hasApiKey: Boolean = false
)

class RecordViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()

    private val recorder = AudioRecorder()
    private val dao = AppDatabase.getInstance(application).meetingDao()
    private val prefs = SecurePrefs(application)
    private var currentFile: File? = null
    private var timerJob: Job? = null
    private var recordingStartTime: Long = 0L

    init {
        _uiState.value = _uiState.value.copy(hasApiKey = prefs.hasApiKey())
    }

    fun refreshApiKeyStatus() {
        _uiState.value = _uiState.value.copy(hasApiKey = prefs.hasApiKey())
    }

    fun startRecording(context: Context) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.filesDir, "meeting_$timestamp.m4a")
        currentFile = file
        recordingStartTime = System.currentTimeMillis()

        recorder.start(file)
        _uiState.value = _uiState.value.copy(isRecording = true, elapsedSeconds = 0, error = null)

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    elapsedSeconds = _uiState.value.elapsedSeconds + 1
                )
            }
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        recorder.stop()
        val durationSeconds = _uiState.value.elapsedSeconds
        _uiState.value = _uiState.value.copy(isRecording = false, isProcessing = true)

        val file = currentFile ?: run {
            _uiState.value = _uiState.value.copy(isProcessing = false, error = "No recording file found.")
            return
        }

        val apiKey = prefs.getApiKey()
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                error = "No API key configured. Please go to Settings."
            )
            return
        }

        viewModelScope.launch {
            val title = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date())

            // Save initial record to DB
            val meetingId = withContext(Dispatchers.IO) {
                dao.insert(
                    MeetingEntity(
                        title = title,
                        audioFilePath = file.absolutePath,
                        durationSeconds = durationSeconds
                    )
                )
            }

            // Call Gemini
            val result = withContext(Dispatchers.IO) {
                GeminiClient.transcribeAndSummarize(file, apiKey)
            }

            result.fold(
                onSuccess = { geminiResult ->
                    withContext(Dispatchers.IO) {
                        dao.update(
                            MeetingEntity(
                                id = meetingId,
                                title = title,
                                audioFilePath = file.absolutePath,
                                transcript = geminiResult.transcript,
                                summary = geminiResult.summary,
                                durationSeconds = durationSeconds,
                                syncStatus = "PENDING"
                            )
                        )
                    }
                    // Trigger background upload via WorkManager
                    com.vismitmv.echominutes.sync.SyncWorker.enqueue(getApplication())

                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        navigateToResult = meetingId
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = error.message ?: "Transcription failed."
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearNavigation() {
        _uiState.value = _uiState.value.copy(navigateToResult = null)
    }

    override fun onCleared() {
        super.onCleared()
        recorder.release()
        timerJob?.cancel()
    }
}
