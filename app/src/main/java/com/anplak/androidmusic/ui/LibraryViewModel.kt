package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.FavoritesRepositoryImpl
import com.anplak.androidmusic.data.LibraryFilter
import com.anplak.androidmusic.data.LibraryFilterEngine
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.data.MusicLibraryRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.data.db.TrackDao
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Content(
        val tracks: List<TrackInfo>,
        val favoriteIds: Set<Long> = emptySet(),
        val filter: LibraryFilter = LibraryFilter(),
        val localQuery: String = "",
        val showNoFilterResults: Boolean = false
    ) : LibraryUiState
    data object Empty : LibraryUiState
}

class LibraryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: MusicLibraryRepository = MusicLibraryRepositoryImpl(
        application.contentResolver,
        application.applicationContext,
        AppDatabase.getInstance(application).trackDao()
    ),
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(
        AppDatabase.getInstance(application).favoriteDao()
    ),
    private val trackDao: TrackDao = AppDatabase.getInstance(application).trackDao()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var hasScanned = false
    private var currentTracks: List<TrackInfo> = emptyList()
    private var favoriteIds: Set<Long> = emptySet()
    private var recentlyAddedIds: Set<Long> = emptySet()
    private var filter: LibraryFilter = LibraryFilter()
    private var localQuery: String = ""

    init {
        viewModelScope.launch {
            favoritesRepository.getAllFavoriteIds().collect { ids ->
                favoriteIds = ids
                if (hasLoaded) {
                    applyFilters()
                }
            }
        }
    }

    fun loadLibrary() {
        if (hasLoaded) return

        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading

            if (!hasScanned) {
                repository.scanMusicDirectories()
                hasScanned = true
            }

            val tracks = repository.getAllTracks()
            currentTracks = tracks
            recentlyAddedIds = loadRecentlyAddedIds()

            hasLoaded = true
            applyFilters()
        }
    }

    fun setFilter(newFilter: LibraryFilter) {
        filter = newFilter
        applyFilters()
    }

    fun setLocalQuery(query: String) {
        localQuery = query
        applyFilters()
    }

    fun applyLibraryHint(query: String) {
        localQuery = query
        applyFilters()
    }

    fun toggleFavorite(trackId: Long) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(trackId)
        }
    }

    fun refresh() {
        hasLoaded = false
        hasScanned = false
        loadLibrary()
    }

    private suspend fun loadRecentlyAddedIds(): Set<Long> {
        val sinceMs = System.currentTimeMillis() - RECENTLY_ADDED_WINDOW_MS
        return trackDao.getTracksAddedSince(sinceMs).map { it.id }.toSet()
    }

    private fun applyFilters() {
        if (!hasLoaded) return

        if (currentTracks.isEmpty()) {
            _uiState.value = LibraryUiState.Empty
            return
        }

        val filtered = LibraryFilterEngine.apply(
            tracks = currentTracks,
            filter = filter,
            favoriteIds = favoriteIds,
            recentlyAddedIds = recentlyAddedIds
        ).filter { LibraryFilterEngine.matchesLocalQuery(it, localQuery) }

        _uiState.value = LibraryUiState.Content(
            tracks = filtered,
            favoriteIds = favoriteIds,
            filter = filter,
            localQuery = localQuery,
            showNoFilterResults = filtered.isEmpty()
        )
    }

    companion object {
        private const val RECENTLY_ADDED_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    }
}
