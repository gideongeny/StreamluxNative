package com.streamlux.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("streamlux_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_BACKGROUND_AUDIO = "background_audio_enabled"
        const val KEY_AUTOPLAY = "autoplay_enabled"
        const val KEY_NIGHT_MODE = "night_mode_enabled"
    }

    var isBackgroundAudioEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_AUDIO, true) // Default to ON per user request
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_AUDIO, value).apply()

    var isAutoplayEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOPLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()

    var isNightModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_MODE, value).apply()

    val nightModeFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_NIGHT_MODE) trySend(p.getBoolean(key, true))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(isNightModeEnabled) }
}
