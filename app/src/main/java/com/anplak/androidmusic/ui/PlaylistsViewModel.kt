package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.Playlist
import com.anplak.androidmusic.data.PlaylistRepository
import com.anplak.androidmusic.data.PlaylistRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    fun loadPlaylistDetail(playlistId: Long) {
        viewModelScope.launch {
            _detailState.value = PlaylistDetailUiState.Loading
            
            playlistRepository.getPlaylistById(playlistId).collect { playlist ->
                if (playlist == null) {
                    _detailState.value = PlaylistDetailUiState.NotFound
                } else {
                    playlistRepository.getPlaylistTracks(playlistId).collect { tracks ->
                        _detailState.value = PlaylistDetailUiState.Content(
                            playlist = playlist,
                            tracks = tracks
                        )
                    }
                }
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

