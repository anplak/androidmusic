package com.anplak.androidmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.FavoritesRepositoryImpl
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.data.MusicLibraryRepositoryImpl
import com.anplak.androidmusic.data.Playlist
import com.anplak.androidmusic.data.PlaylistRepository
import com.anplak.androidmusic.data.PlaylistRepositoryImpl
import com.anplak.androidmusic.data.SmartPlaylistRepository
import com.anplak.androidmusic.data.SmartPlaylistRepositoryImpl
import com.anplak.androidmusic.data.SmartPlaylistType
import com.anplak.androidmusic.data.TrackStatsRepository
import com.anplak.androidmusic.data.TrackStatsRepositoryImpl
import com.anplak.androidmusic.data.db.AppDatabase
import com.anplak.androidmusic.player.AutoMixGenerator
import com.anplak.androidmusic.player.SmartShuffleGenerator
import com.anplak.androidmusic.player.TrackInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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

sealed interface AutoMixSeed {
    data class FavoriteTrack(val track: TrackInfo) : AutoMixSeed
    data class FavoriteArtist(val artist: String) : AutoMixSeed
    data class SmartPlaylist(val type: SmartPlaylistType) : AutoMixSeed
}

sealed interface AutoMixState {
    data object Idle : AutoMixState
    data object Loading : AutoMixState
    data class Preview(val seed: AutoMixSeed, val tracks: List<TrackInfo>) : AutoMixState
    data class Error(val message: String) : AutoMixState
}

data class PlaylistDetailEditState(
    val isSelectionMode: Boolean = false,
    val selectedTrackIds: Set<Long> = emptySet(),
    val isReorderMode: Boolean = false,
    val reorderedTracks: List<TrackInfo> = emptyList(),
    val showRemoveConfirmation: Boolean = false,
    val autoMixState: AutoMixState = AutoMixState.Idle,
    val favoriteTracks: List<TrackInfo> = emptyList(),
    val favoriteArtists: List<String> = emptyList()
)

class PlaylistsViewModel @JvmOverloads constructor(
    application: Application,
    private val playlistRepository: PlaylistRepository = PlaylistRepositoryImpl(
        AppDatabase.getInstance(application).playlistDao()
    ),
    private val favoritesRepository: FavoritesRepository = FavoritesRepositoryImpl(
        AppDatabase.getInstance(application).favoriteDao()
    ),
    private val trackStatsRepository: TrackStatsRepository = TrackStatsRepositoryImpl(
        AppDatabase.getInstance(application).trackStatsDao()
    ),
    private val smartPlaylistRepository: SmartPlaylistRepository = SmartPlaylistRepositoryImpl(
        AppDatabase.getInstance(application).trackDao(),
        AppDatabase.getInstance(application).trackStatsDao()
    ),
    private val musicLibraryRepository: MusicLibraryRepository = MusicLibraryRepositoryImpl(
        application.contentResolver,
        application,
        AppDatabase.getInstance(application).trackDao()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PlaylistsUiState>(PlaylistsUiState.Loading)
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    val detailState: StateFlow<PlaylistDetailUiState> = _detailState.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _editState = MutableStateFlow(PlaylistDetailEditState())
    val editState: StateFlow<PlaylistDetailEditState> = _editState.asStateFlow()

    private val smartShuffleGenerator = SmartShuffleGenerator(
        favoritesRepository = favoritesRepository,
        statsRepository = trackStatsRepository
    )
    private val autoMixGenerator = AutoMixGenerator(smartShuffleGenerator)
    
    // Track the current detail loading job to cancel it when loading a new playlist
    private var detailLoadingJob: Job? = null
    private var autoMixSeedJob: Job? = null

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

    fun startSelectionMode() {
        _editState.update {
            it.copy(
                isSelectionMode = true,
                selectedTrackIds = emptySet(),
                isReorderMode = false,
                reorderedTracks = emptyList()
            )
        }
    }

    fun exitSelectionMode() {
        _editState.update {
            it.copy(isSelectionMode = false, selectedTrackIds = emptySet())
        }
    }

    fun toggleTrackSelection(trackId: Long) {
        _editState.update { state ->
            val updated = state.selectedTrackIds.toMutableSet()
            if (trackId in updated) {
                updated.remove(trackId)
            } else {
                updated.add(trackId)
            }
            state.copy(selectedTrackIds = updated)
        }
    }

    fun requestRemoveSelected() {
        _editState.update { state ->
            if (state.selectedTrackIds.isEmpty()) state else state.copy(showRemoveConfirmation = true)
        }
    }

    fun dismissRemoveConfirmation() {
        _editState.update { it.copy(showRemoveConfirmation = false) }
    }

    fun confirmRemoveSelected(playlistId: Long) {
        val selected = _editState.value.selectedTrackIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            playlistRepository.removeTracksFromPlaylist(playlistId, selected.toList())
            _editState.update {
                it.copy(
                    isSelectionMode = false,
                    selectedTrackIds = emptySet(),
                    showRemoveConfirmation = false
                )
            }
        }
    }

    fun startReorderMode(tracks: List<TrackInfo>) {
        _editState.update {
            it.copy(
                isReorderMode = true,
                reorderedTracks = tracks,
                isSelectionMode = false,
                selectedTrackIds = emptySet()
            )
        }
    }

    fun updateReorder(tracks: List<TrackInfo>) {
        _editState.update { it.copy(reorderedTracks = tracks) }
    }

    fun cancelReorderMode() {
        _editState.update { it.copy(isReorderMode = false, reorderedTracks = emptyList()) }
    }

    fun commitReorder(playlistId: Long) {
        val orderedTracks = _editState.value.reorderedTracks
        if (orderedTracks.isEmpty()) {
            cancelReorderMode()
            return
        }
        viewModelScope.launch {
            playlistRepository.reorderPlaylistTracks(
                playlistId = playlistId,
                orderedTrackIds = orderedTracks.map { it.id }
            )
            _editState.update { it.copy(isReorderMode = false, reorderedTracks = emptyList()) }
        }
    }

    fun duplicatePlaylist(sourcePlaylistId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistRepository.duplicatePlaylist(sourcePlaylistId, name.trim())
        }
    }

    fun mergePlaylists(primaryPlaylistId: Long, secondaryPlaylistId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistRepository.mergePlaylists(primaryPlaylistId, secondaryPlaylistId, name.trim())
        }
    }

    fun loadAutoMixSeeds() {
        autoMixSeedJob?.cancel()
        autoMixSeedJob = viewModelScope.launch {
            favoritesRepository.getAllFavorites().collect { favorites ->
                val artists = favorites.mapNotNull { it.artist.takeIf { artist -> artist.isNotBlank() } }
                    .distinct()
                    .sorted()
                _editState.update { state ->
                    state.copy(
                        favoriteTracks = favorites,
                        favoriteArtists = artists
                    )
                }
            }
        }
    }

    fun generateAutoMix(seed: AutoMixSeed, limit: Int = DEFAULT_MIX_SIZE) {
        viewModelScope.launch {
            _editState.update { it.copy(autoMixState = AutoMixState.Loading) }
            val mixTracks = when (seed) {
                is AutoMixSeed.FavoriteTrack -> {
                    autoMixGenerator.fromFavoriteTrack(
                        seed = seed.track,
                        libraryTracks = musicLibraryRepository.getAllTracks(),
                        limit = limit
                    )
                }
                is AutoMixSeed.FavoriteArtist -> {
                    autoMixGenerator.fromFavoriteArtist(
                        artist = seed.artist,
                        libraryTracks = musicLibraryRepository.getAllTracks(),
                        limit = limit
                    )
                }
                is AutoMixSeed.SmartPlaylist -> {
                    val tracks = smartPlaylistRepository.getTracksForType(seed.type).first()
                    autoMixGenerator.fromSmartPlaylist(
                        tracks = tracks,
                        limit = limit
                    )
                }
            }
            _editState.update {
                if (mixTracks.isEmpty()) {
                    it.copy(autoMixState = AutoMixState.Error("No tracks found for this mix."))
                } else {
                    it.copy(autoMixState = AutoMixState.Preview(seed, mixTracks))
                }
            }
        }
    }

    fun clearAutoMixPreview() {
        _editState.update { it.copy(autoMixState = AutoMixState.Idle) }
    }

    fun saveAutoMixAsPlaylist(name: String) {
        val current = _editState.value.autoMixState
        if (name.isBlank() || current !is AutoMixState.Preview) return
        viewModelScope.launch {
            playlistRepository.createPlaylistWithTracks(
                name = name.trim(),
                trackIds = current.tracks.map { it.id }
            )
            _editState.update { it.copy(autoMixState = AutoMixState.Idle) }
        }
    }

    companion object {
        private const val DEFAULT_MIX_SIZE = 30
    }
}
