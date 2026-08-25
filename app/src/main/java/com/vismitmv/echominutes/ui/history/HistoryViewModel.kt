package com.vismitmv.echominutes.ui.history

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

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).meetingDao()

    private val _meetings = MutableStateFlow<List<MeetingEntity>>(emptyList())
    val meetings: StateFlow<List<MeetingEntity>> = _meetings.asStateFlow()

    init {
        loadMeetings()
    }

    fun loadMeetings() {
        viewModelScope.launch {
            _meetings.value = withContext(Dispatchers.IO) { dao.getAllMeetings() }
        }
    }
}
