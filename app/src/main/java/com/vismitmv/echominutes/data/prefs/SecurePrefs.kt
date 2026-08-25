package com.vismitmv.echominutes.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val FILE_NAME = "secure_prefs"
private const val KEY_API_KEY = "gemini_api_key"

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

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, "") ?: ""

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun hasApiKey(): Boolean = getApiKey().isNotBlank()
}
