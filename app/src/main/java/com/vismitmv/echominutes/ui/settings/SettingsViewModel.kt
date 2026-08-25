package com.vismitmv.echominutes.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vismitmv.echominutes.data.prefs.SecurePrefs
import com.vismitmv.echominutes.sync.SyncWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val serverUrl: String = "",
    val syncApiKey: String = "",
    val autoSyncEnabled: Boolean = true,
    val saved: Boolean = false,
    val isSyncingNow: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = SecurePrefs(application)
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            apiKey = prefs.getApiKey(),
            serverUrl = prefs.getServerUrl(),
            syncApiKey = prefs.getSyncApiKey(),
            autoSyncEnabled = prefs.isAutoSyncEnabled()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onApiKeyChanged(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key, saved = false)
    }

    fun onServerUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, saved = false)
    }

    fun onSyncApiKeyChanged(key: String) {
        _uiState.value = _uiState.value.copy(syncApiKey = key, saved = false)
    }

    fun onAutoSyncToggled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled, saved = false)
        prefs.saveAutoSyncEnabled(enabled)
    }

    fun saveAllSettings() {
        prefs.saveApiKey(_uiState.value.apiKey.trim())
        prefs.saveServerUrl(_uiState.value.serverUrl.trim())
        prefs.saveSyncApiKey(_uiState.value.syncApiKey.trim())
        prefs.saveAutoSyncEnabled(_uiState.value.autoSyncEnabled)
        _uiState.value = _uiState.value.copy(saved = true)
    }

    fun syncNow() {
        saveAllSettings()
        _uiState.value = _uiState.value.copy(isSyncingNow = true)
        SyncWorker.enqueue(getApplication(), force = true)
        viewModelScope.launch {
            delay(1500)
            _uiState.value = _uiState.value.copy(isSyncingNow = false)
        }
    }

    fun clearSaved() {
        _uiState.value = _uiState.value.copy(saved = false)
    }
}
