package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.data.MusicLibraryRepositoryImpl
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Content(val tracks: List<TrackInfo>) : LibraryUiState
    data object Empty : LibraryUiState
}

class LibraryViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: MusicLibraryRepository = MusicLibraryRepositoryImpl(
        application.contentResolver,
        application.applicationContext
    )
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    private var hasLoaded = false
    private var hasScanned = false
    
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
            
            _uiState.value = if (tracks.isEmpty()) {
                LibraryUiState.Empty
            } else {
                LibraryUiState.Content(tracks)
            }
            
            hasLoaded = true
        }
    }
    
    fun refresh() {
        hasLoaded = false
        hasScanned = false  // Re-scan on manual refresh
        loadLibrary()
    }
}
