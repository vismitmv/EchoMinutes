package com.vismitmv.echominutes.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val FILE_NAME = "secure_prefs"
private const val KEY_API_KEY = "gemini_api_key"
private const val KEY_SERVER_URL = "sync_server_url"
private const val KEY_SYNC_API_KEY = "sync_api_key"
private const val KEY_AUTO_SYNC = "auto_sync_enabled"

private const val DEFAULT_SERVER_URL = "https://echominutes.vismitmv.com"

class SecurePrefs(context: Context) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Gemini API Key
    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""
    fun saveApiKey(key: String) = prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    // Cloud Sync Server URL
    fun getServerUrl(): String = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    fun saveServerUrl(url: String) = prefs.edit().putString(KEY_SERVER_URL, url.trim().trimEnd('/')).apply()

    // Cloud Sync API Key
    fun getSyncApiKey(): String = prefs.getString(KEY_SYNC_API_KEY, "") ?: ""
    fun saveSyncApiKey(key: String) = prefs.edit().putString(KEY_SYNC_API_KEY, key.trim()).apply()

    // Auto-sync toggle
    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_SYNC, true)
    fun saveAutoSyncEnabled(enabled: Boolean) = prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
}
