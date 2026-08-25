package com.vismitmv.echominutes.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.vismitmv.echominutes.data.prefs.SecurePrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val apiKey: String = "",
    val saved: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = SecurePrefs(application)
    private val _uiState = MutableStateFlow(SettingsUiState(apiKey = prefs.getApiKey()))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onApiKeyChanged(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key, saved = false)
    }

    fun saveApiKey() {
        prefs.saveApiKey(_uiState.value.apiKey.trim())
        _uiState.value = _uiState.value.copy(saved = true)
    }

    fun clearSaved() {
        _uiState.value = _uiState.value.copy(saved = false)
    }
}
