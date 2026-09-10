package com.example.data.preference

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_companion_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_GROQ = "groq"

        private const val KEY_PROVIDER = "key_provider"
        private const val KEY_GEMINI_KEY = "key_gemini_api_key"
        private const val KEY_GROQ_KEY = "key_groq_api_key"
        private const val KEY_GEMINI_MODEL = "key_gemini_model"
        private const val KEY_GROQ_MODEL = "key_groq_model"
        private const val KEY_SPEECH_RATE = "key_speech_rate"
        private const val KEY_SPEECH_PITCH = "key_speech_pitch"
        private const val KEY_AUTO_SPEAK = "key_auto_speak"
        private const val KEY_SCREEN_OFF_MODE = "key_screen_off_mode"
        private const val KEY_REQUESTED_INITIAL_PERMISSIONS = "key_requested_initial_permissions"

        // Latest Supported Gemini Models
        const val MODEL_GEMINI_3_8_FLASH = "gemini-3.8-flash"
        const val MODEL_GEMINI_3_5_FLASH = "gemini-3.5-flash"
        const val MODEL_GEMINI_2_5_FLASH = "gemini-2.5-flash"
        const val MODEL_GEMINI_3_1_PRO = "gemini-3.1-pro-preview"
        const val MODEL_GEMINI_3_1_FLASH_LITE = "gemini-3.1-flash-lite-preview"
        const val MODEL_GEMINI_2_5_NATIVE_AUDIO = "gemini-2.5-flash-native-audio-preview-12-2025"
        const val MODEL_GEMINI_FLASH_LATEST = "gemini-flash-latest"

        const val DEFAULT_GEMINI_MODEL = MODEL_GEMINI_2_5_FLASH
        const val DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"
    }

    var activeProvider: String
        get() = prefs.getString(KEY_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    var geminiApiKey: String
        get() {
            val userKey = prefs.getString(KEY_GEMINI_KEY, "") ?: ""
            if (userKey.isNotBlank()) return userKey
            // Fallback to BuildConfig if provided via secrets
            val buildConfigKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault("")
            return if (buildConfigKey != "MY_GEMINI_API_KEY" && buildConfigKey.isNotBlank()) buildConfigKey else ""
        }
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value).apply()

    var groqApiKey: String
        get() = prefs.getString(KEY_GROQ_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GROQ_KEY, value).apply()

    var geminiModel: String
        get() {
            val saved = prefs.getString(KEY_GEMINI_MODEL, DEFAULT_GEMINI_MODEL) ?: DEFAULT_GEMINI_MODEL
            return if (saved.contains("3.8") || saved.isBlank()) MODEL_GEMINI_2_5_FLASH else saved
        }
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value).apply()

    var groqModel: String
        get() = prefs.getString(KEY_GROQ_MODEL, DEFAULT_GROQ_MODEL) ?: DEFAULT_GROQ_MODEL
        set(value) = prefs.edit().putString(KEY_GROQ_MODEL, value).apply()

    var speechRate: Float
        get() = prefs.getFloat(KEY_SPEECH_RATE, 1.05f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()

    var speechPitch: Float
        get() = prefs.getFloat(KEY_SPEECH_PITCH, 1.15f)
        set(value) = prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()

    var isAutoSpeakEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply()

    var isScreenOffModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_OFF_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_OFF_MODE, value).apply()

    var hasRequestedInitialPermissions: Boolean
        get() = prefs.getBoolean(KEY_REQUESTED_INITIAL_PERMISSIONS, false)
        set(value) = prefs.edit().putBoolean(KEY_REQUESTED_INITIAL_PERMISSIONS, value).apply()

    fun hasActiveApiKey(): Boolean {
        return if (activeProvider == PROVIDER_GEMINI) {
            geminiApiKey.isNotBlank()
        } else {
            groqApiKey.isNotBlank()
        }
    }
}
