package com.anplak.androidmusic.data

import android.content.Context

interface LibraryIndexPreferences {
    fun getMaxDurationMs(): Long
    fun setMaxDurationMs(durationMs: Long)
}

class SharedPreferencesLibraryIndexPreferences(context: Context) : LibraryIndexPreferences {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getMaxDurationMs(): Long {
        return prefs.getLong(
            KEY_MAX_DURATION_MS,
            LibraryIndexPolicy.DEFAULT_MAX_INDEX_DURATION_MS
        )
    }

    override fun setMaxDurationMs(durationMs: Long) {
        prefs.edit().putLong(KEY_MAX_DURATION_MS, durationMs).apply()
    }

    companion object {
        private const val PREFS_NAME = "library_index"
        private const val KEY_MAX_DURATION_MS = "max_duration_ms"
    }
}
