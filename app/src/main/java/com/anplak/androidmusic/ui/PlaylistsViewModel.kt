package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.Playlist
import com.anplak.androidmusic.data.PlaylistRepository
import com.anplak.androidmusic.data.PlaylistRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface PlaylistsUiState {
    data object Loading : PlaylistsUiState
    data class Content(val playlists: List<Playlist>) : PlaylistsUiState
    data object Empty : PlaylistsUiState
}

sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data class Content(
        val playlist: Playlist,
        val tracks: List<TrackInfo>
    ) : PlaylistDetailUiState
    data object NotFound : PlaylistDetailUiState
}

class PlaylistsViewModel @JvmOverloads constructor(
    application: Application,
    private val playlistRepository: PlaylistRepository = PlaylistRepositoryImpl(
        AppDatabase.getInstance(application).playlistDao()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loading)
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    val detailState: StateFlow<PlaylistDetailUiState> = _detailState.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()
    
    // Track the current detail loading job to cancel it when loading a new playlist
    private var detailLoadingJob: Job? = null

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            playlistRepository.getPlaylists().collect { playlists ->
                _playlists.value = playlists
                _uiState.value = if (playlists.isEmpty()) {
                    PlaylistsUiState.Empty
                } else {
                    PlaylistsUiState.Content(playlists)
                }
            }
        }
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistRepository.createPlaylist(name.trim())
        }
    }
    
    /**
     * Creates a new playlist and immediately adds a track to it.
     */
    fun createPlaylistAndAddTrack(name: String, trackId: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name.trim())
            playlistRepository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun loadPlaylistDetail(playlistId: Long) {
        // Cancel any previous detail loading job to prevent subscription leaks
        detailLoadingJob?.cancel()
        
        detailLoadingJob = viewModelScope.launch {
            _detailState.value = PlaylistDetailUiState.Loading
            
            // Use combine instead of nested collects to avoid subscription leaks
            combine(
                playlistRepository.getPlaylistById(playlistId),
                playlistRepository.getPlaylistTracks(playlistId)
            ) { playlist, tracks ->
                if (playlist == null) {
                    PlaylistDetailUiState.NotFound
                } else {
                    PlaylistDetailUiState.Content(
                        playlist = playlist,
                        tracks = tracks
                    )
                }
            }.collect { state ->
                _detailState.value = state
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            playlistRepository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }
}
