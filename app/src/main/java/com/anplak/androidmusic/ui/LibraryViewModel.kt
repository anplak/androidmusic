package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.FavoritesRepositoryImpl
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.data.MusicLibraryRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Content(
        val tracks: List<TrackInfo>,
        val favoriteIds: Set<Long> = emptySet()
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
    )
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    private var hasLoaded = false
    private var hasScanned = false
    private var currentTracks: List<TrackInfo> = emptyList()
    
    init {
        // Observe favorite changes and update UI state
        viewModelScope.launch {
            favoritesRepository.getAllFavoriteIds().collect { favoriteIds ->
                val currentState = _uiState.value
                if (currentState is LibraryUiState.Content) {
                    _uiState.value = currentState.copy(favoriteIds = favoriteIds)
                }
            }
        }
    }
    
    fun loadLibrary() {
        // Session cache: don't reload if already loaded
        if (hasLoaded) return
        
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            
            // Scan music directories on first load to ensure MediaStore is up-to-date
            // This handles files added via adb, file manager, or downloads
            if (!hasScanned) {
                repository.scanMusicDirectories()
                hasScanned = true
            }
            
            val tracks = repository.getAllTracks()
            currentTracks = tracks
            
            _uiState.value = if (tracks.isEmpty()) {
                LibraryUiState.Empty
            } else {
                LibraryUiState.Content(tracks = tracks)
            }
            
            hasLoaded = true
        }
    }
    
    fun toggleFavorite(trackId: Long) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(trackId)
        }
    }
    
    fun refresh() {
        hasLoaded = false
        hasScanned = false  // Re-scan on manual refresh
        loadLibrary()
    }
}
