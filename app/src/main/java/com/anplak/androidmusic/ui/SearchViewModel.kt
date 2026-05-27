package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.GroupedSearchResults
import com.anplak.androidmusic.data.PlayHistoryEntry
import com.anplak.androidmusic.data.PlayHistoryRepositoryImpl
import com.anplak.androidmusic.data.RecentSearchStore
import com.anplak.androidmusic.data.SearchEngine
import com.anplak.androidmusic.data.SearchRepository
import com.anplak.androidmusic.data.SearchRepositoryImpl
import com.anplak.androidmusic.data.SearchResultItem
import com.anplak.androidmusic.data.SearchResultKind
import com.anplak.androidmusic.data.SharedPreferencesRecentSearchStore
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data class Idle(
        val recentQueries: List<String>,
        val suggestions: List<String>
    ) : SearchUiState

    data object Searching : SearchUiState
    data class Results(val grouped: GroupedSearchResults) : SearchUiState
    data object NoResults : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(FlowPreview::class)
class SearchViewModel @JvmOverloads constructor(
    application: Application,
    private val searchRepository: SearchRepository? = null,
    private val searchEngine: SearchEngine? = null,
    private val recentSearchStore: RecentSearchStore? = null
) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository: SearchRepository = searchRepository ?: SearchRepositoryImpl(
        trackDao = db.trackDao(),
        playlistDao = db.playlistDao(),
        playHistoryDao = db.playHistoryDao(),
        playHistoryRepository = PlayHistoryRepositoryImpl(db.playHistoryDao())
    )
    private val engine: SearchEngine = searchEngine ?: SearchEngine()
    private val recentStore: RecentSearchStore =
        recentSearchStore ?: SharedPreferencesRecentSearchStore(application)

    private val query = MutableStateFlow("")
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle(emptyList(), emptyList()))
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val recentQueries: StateFlow<List<String>> = recentStore.recentQueries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var searchJob: Job? = null
    private var trackById: Map<Long, TrackInfo> = emptyMap()
    private var historyById: Map<Long, PlayHistoryEntry> = emptyMap()
    private var suggestions: List<String> = emptyList()

    init {
        loadSuggestions()
        viewModelScope.launch {
            query
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { q -> performSearch(q) }
        }
        viewModelScope.launch {
            recentQueries.collect { recent ->
                val current = _uiState.value
                if (current is SearchUiState.Idle || query.value.isBlank()) {
                    _uiState.value = SearchUiState.Idle(recent, suggestions)
                }
            }
        }
    }

    fun onQueryChange(text: String) {
        query.value = text
        if (text.isNotBlank()) {
            _uiState.value = SearchUiState.Searching
        }
    }

    fun onSubmit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            recentStore.addRecentQuery(trimmed)
        }
        query.value = trimmed
    }

    fun onSuggestionClicked(suggestion: String) {
        onSubmit(suggestion)
    }

    fun resolveTrack(item: SearchResultItem): TrackInfo? {
        return when (item.kind) {
            SearchResultKind.TRACK -> item.trackId?.let { trackById[it] }
            SearchResultKind.HISTORY -> item.historyId?.let { historyById[it]?.track }
            else -> null
        }
    }

    fun libraryQueryForItem(item: SearchResultItem): String? {
        return when (item.kind) {
            SearchResultKind.ARTIST -> item.title
            SearchResultKind.ALBUM -> item.title
            else -> null
        }
    }

    private fun loadSuggestions() {
        viewModelScope.launch {
            runCatching {
                suggestions = repository.getSuggestions()
                if (query.value.isBlank()) {
                    _uiState.value = SearchUiState.Idle(recentQueries.value, suggestions)
                }
            }
        }
    }

    private fun performSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = SearchUiState.Idle(recentQueries.value, suggestions)
            return
        }
        if (trimmed.length < MIN_QUERY_LENGTH) {
            _uiState.value = SearchUiState.Idle(recentQueries.value, suggestions)
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Searching
            runCatching {
                val raw = repository.searchAll(trimmed)
                trackById = raw.tracks.associateBy { it.id } +
                    raw.history.associate { it.trackId to it.track }
                historyById = raw.history.associateBy { it.id }
                val grouped = engine.buildGrouped(trimmed, raw)
                _uiState.value = when {
                    grouped.totalCount == 0 -> SearchUiState.NoResults
                    else -> SearchUiState.Results(grouped)
                }
            }.onFailure { error ->
                _uiState.value = SearchUiState.Error(
                    error.message ?: "Search failed"
                )
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 300L
        private const val MIN_QUERY_LENGTH = 1
    }
}
