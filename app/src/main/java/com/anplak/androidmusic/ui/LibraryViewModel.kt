package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.FavoritesRepositoryImpl
import com.anplak.androidmusic.data.LibraryFilter
import com.anplak.androidmusic.data.LibraryFilterEngine
import com.anplak.androidmusic.data.LibraryScanResult
import com.anplak.androidmusic.data.LibrarySyncCoordinator
import com.anplak.androidmusic.data.LibrarySyncCoordinatorFactory
import com.anplak.androidmusic.data.LibrarySyncState
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.data.MusicLibraryRepositoryFactory
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
        val isRefreshing: Boolean = false,
        val syncFailed: Boolean = false,
        val favoriteIds: Set<Long> = emptySet(),
        val filter: LibraryFilter = LibraryFilter(),
        val localQuery: String = "",
        val showNoFilterResults: Boolean = false
    ) : LibraryUiState
    data object Empty : LibraryUiState
}

class LibraryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: MusicLibraryRepository = MusicLibraryRepositoryFactory.create(application),
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(
        AppDatabase.getInstance(application).favoriteDao()
    ),
    private val trackDao: TrackDao = AppDatabase.getInstance(application).trackDao(),
    private val syncCoordinator: LibrarySyncCoordinator = LibrarySyncCoordinatorFactory.get(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _scanSummary = MutableStateFlow<LibraryScanResult?>(null)
    val scanSummary: StateFlow<LibraryScanResult?> = _scanSummary.asStateFlow()

    private var currentTracks: List<TrackInfo> = emptyList()
    private var favoriteIds: Set<Long> = emptySet()
    private var recentlyAddedIds: Set<Long> = emptySet()
    private var filter: LibraryFilter = LibraryFilter()
    private var localQuery: String = ""
    private var isRefreshing: Boolean = false
    private var syncFailed: Boolean = false

    init {
        viewModelScope.launch {
            favoritesRepository.getAllFavoriteIds().collect { ids ->
                favoriteIds = ids
                if (shouldApplyFilters()) {
                    applyFilters()
                }
            }
        }

        viewModelScope.launch {
            repository.observeCachedTracks().collect { cached ->
                currentTracks = cached
                recentlyAddedIds = loadRecentlyAddedIds()
                if (shouldApplyFilters()) {
                    applyFilters()
                }
            }
        }

        viewModelScope.launch {
            syncCoordinator.syncState.collect { state ->
                when (state) {
                    LibrarySyncState.Idle -> Unit
                    LibrarySyncState.Running -> updateSyncFlags(isRefreshing = true, syncFailed = false)
                    is LibrarySyncState.Success -> {
                        updateSyncFlags(isRefreshing = false, syncFailed = false)
                        _scanSummary.value = state.result
                    }
                    is LibrarySyncState.Failed -> updateSyncFlags(isRefreshing = false, syncFailed = true)
                }
            }
        }

        viewModelScope.launch {
            if (repository.getCachedTracks().isEmpty()) {
                _uiState.value = LibraryUiState.Loading
                syncCoordinator.syncNow()
            } else {
                applyFilters()
                syncCoordinator.scheduleSync()
            }
        }
    }

    fun onLibraryVisible() {
        syncCoordinator.scheduleSync()
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
        viewModelScope.launch {
            syncFailed = false
            if (currentTracks.isEmpty()) {
                _uiState.value = LibraryUiState.Loading
            } else {
                updateSyncFlags(isRefreshing = true, syncFailed = false)
            }
            syncCoordinator.syncNow()
        }
    }

    fun clearScanSummary() {
        _scanSummary.value = null
    }

    private suspend fun loadRecentlyAddedIds(): Set<Long> {
        val sinceMs = System.currentTimeMillis() - RECENTLY_ADDED_WINDOW_MS
        return trackDao.getTracksAddedSince(sinceMs).map { it.id }.toSet()
    }

    private fun shouldApplyFilters(): Boolean {
        if (_uiState.value !is LibraryUiState.Loading) return true
        if (currentTracks.isNotEmpty()) return true
        return syncCoordinator.syncState.value !is LibrarySyncState.Running
    }

    private fun updateSyncFlags(isRefreshing: Boolean, syncFailed: Boolean) {
        this.isRefreshing = isRefreshing
        this.syncFailed = syncFailed
        val state = _uiState.value
        if (state is LibraryUiState.Content) {
            _uiState.value = state.copy(
                isRefreshing = isRefreshing,
                syncFailed = syncFailed
            )
        } else if (shouldApplyFilters()) {
            applyFilters()
        }
    }

    private fun applyFilters() {
        if (_uiState.value is LibraryUiState.Loading &&
            currentTracks.isEmpty() &&
            syncCoordinator.syncState.value is LibrarySyncState.Running
        ) {
            return
        }

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
            isRefreshing = isRefreshing,
            syncFailed = syncFailed,
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
