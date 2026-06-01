package com.anplak.androidmusic.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface RecentSearchStore {
    fun recentQueries(): Flow<List<String>>
    suspend fun addRecentQuery(query: String)
}

class SharedPreferencesRecentSearchStore(context: Context) : RecentSearchStore {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _queries = MutableStateFlow(readQueries())
    private val queries: Flow<List<String>> = _queries.asStateFlow()

    override fun recentQueries(): Flow<List<String>> = queries

    override suspend fun addRecentQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val updated = (listOf(trimmed) + readQueries().filter { !it.equals(trimmed, ignoreCase = true) })
            .take(MAX_RECENT)
        prefs.edit()
            .putString(KEY_QUERIES, updated.joinToString(DELIMITER))
            .apply()
        _queries.value = updated
    }

    private fun readQueries(): List<String> {
        val raw = prefs.getString(KEY_QUERIES, null) ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split(DELIMITER).filter { it.isNotBlank() }
    }

    companion object {
        private const val PREFS_NAME = "recent_searches"
        private const val KEY_QUERIES = "queries"
        private const val DELIMITER = "\u0001"
        private const val MAX_RECENT = 10
    }
}
