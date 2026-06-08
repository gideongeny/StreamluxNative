package com.streamlux.app.data.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streamlux.app.data.model.SportsFixture
import com.streamlux.app.data.model.SportsHighlight
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SportsCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("streamlux_sports_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveLiveMatches(matches: List<SportsFixture>) {
        try {
            val json = gson.toJson(matches)
            prefs.edit().putString("live_matches", json).apply()
        } catch (e: Exception) {
            Log.e("SportsCache", "Failed to save live matches", e)
        }
    }

    fun getLiveMatches(): List<SportsFixture> {
        return try {
            val json = prefs.getString("live_matches", null)
            if (json != null) {
                val type = object : TypeToken<List<SportsFixture>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SportsCache", "Failed to load live matches", e)
            emptyList()
        }
    }

    fun saveUpcomingMatches(matches: List<SportsFixture>) {
        try {
            val json = gson.toJson(matches)
            prefs.edit().putString("upcoming_matches", json).apply()
        } catch (e: Exception) {
            Log.e("SportsCache", "Failed to save upcoming matches", e)
        }
    }

    fun getUpcomingMatches(): List<SportsFixture> {
        return try {
            val json = prefs.getString("upcoming_matches", null)
            if (json != null) {
                val type = object : TypeToken<List<SportsFixture>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SportsCache", "Failed to load upcoming matches", e)
            emptyList()
        }
    }

    fun saveHighlights(highlights: List<SportsHighlight>) {
        try {
            val json = gson.toJson(highlights)
            prefs.edit().putString("highlights", json).apply()
        } catch (e: Exception) {
            Log.e("SportsCache", "Failed to save highlights", e)
        }
    }

    fun getHighlights(): List<SportsHighlight> {
        return try {
            val json = prefs.getString("highlights", null)
            if (json != null) {
                val type = object : TypeToken<List<SportsHighlight>>() {}.type
                gson.fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("SportsCache", "Failed to load highlights", e)
            emptyList()
        }
    }
}
