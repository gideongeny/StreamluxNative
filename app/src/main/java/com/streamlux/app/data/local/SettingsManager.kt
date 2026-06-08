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
        const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        const val KEY_ANALYTICS = "analytics_enabled"
        const val KEY_DEFAULT_SERVER = "default_server_index"
    }

    var defaultServerIndex: Int
        get() = prefs.getInt(KEY_DEFAULT_SERVER, 0) // Default to index 0 (VidEasy)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_SERVER, value).apply()

    var isBackgroundAudioEnabled: Boolean
        get() = prefs.getBoolean(KEY_BACKGROUND_AUDIO, true) // Default to ON per user request
        set(value) = prefs.edit().putBoolean(KEY_BACKGROUND_AUDIO, value).apply()

    var isAutoplayEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOPLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()

    var isNightModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_MODE, value).apply()

    var hasSeenOnboarding: Boolean
        get() = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, value).apply()

    var isAnalyticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANALYTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_ANALYTICS, value).apply()

    val nightModeFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == KEY_NIGHT_MODE) trySend(p.getBoolean(key, true))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.onStart { emit(isNightModeEnabled) }
}
