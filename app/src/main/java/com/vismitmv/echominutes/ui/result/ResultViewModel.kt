package com.vismitmv.echominutes.ui.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vismitmv.echominutes.data.db.AppDatabase
import com.vismitmv.echominutes.data.db.MeetingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ResultUiState(
    val meeting: MeetingEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ResultViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    private val dao = AppDatabase.getInstance(application).meetingDao()

    fun loadMeeting(meetingId: Long) {
        viewModelScope.launch {
            val meeting = withContext(Dispatchers.IO) { dao.getById(meetingId) }
            _uiState.value = if (meeting != null) {
                ResultUiState(meeting = meeting, isLoading = false)
            } else {
                ResultUiState(isLoading = false, error = "Meeting not found.")
            }
        }
    }
}
