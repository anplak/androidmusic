package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.SmartPlaylistRepository
import com.anplak.androidmusic.data.SmartPlaylistRepositoryImpl
import com.anplak.androidmusic.data.SmartPlaylistType
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SmartPlaylistDetailUiState {
    data object Loading : SmartPlaylistDetailUiState
    data class Content(
        val type: SmartPlaylistType,
        val tracks: List<TrackInfo>
    ) : SmartPlaylistDetailUiState
    data object Empty : SmartPlaylistDetailUiState
}

class SmartPlaylistsViewModel @JvmOverloads constructor(
    application: Application,
    private val smartPlaylistRepository: SmartPlaylistRepository = SmartPlaylistRepositoryImpl(
        AppDatabase.getInstance(application).trackDao(),
        AppDatabase.getInstance(application).trackStatsDao()
    )
) : AndroidViewModel(application) {

    private val _detailState = MutableStateFlow<SmartPlaylistDetailUiState>(SmartPlaylistDetailUiState.Loading)
    val detailState: StateFlow<SmartPlaylistDetailUiState> = _detailState.asStateFlow()

    /**
     * Loads the detail view for a specific smart playlist type.
     */
    fun loadSmartPlaylist(type: SmartPlaylistType) {
        viewModelScope.launch {
            _detailState.value = SmartPlaylistDetailUiState.Loading
            
            smartPlaylistRepository.getTracksForType(type).collect { tracks ->
                _detailState.value = if (tracks.isEmpty()) {
                    SmartPlaylistDetailUiState.Empty
                } else {
                    SmartPlaylistDetailUiState.Content(
                        type = type,
                        tracks = tracks
                    )
                }
            }
        }
    }
}

