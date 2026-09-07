package com.anplak.androidmusic.ui

import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.MusicLibraryRepository
import com.anplak.androidmusic.data.SmartPlaylistRepository
import com.anplak.androidmusic.player.AutoMixGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns auto-mix seed loading, generation, preview, and save orchestration.
 */
class AutoMixCoordinator(
    private val scope: CoroutineScope,
    private val favoritesRepository: FavoritesRepository,
    private val musicLibraryRepository: MusicLibraryRepository,
    private val smartPlaylistRepository: SmartPlaylistRepository,
    private val autoMixGenerator: AutoMixGenerator,
    private val editState: MutableStateFlow<PlaylistDetailEditState>,
    private val createPlaylistWithTracks: suspend (name: String, trackIds: List<Long>) -> Unit,
) {
    private var autoMixSeedJob: Job? = null

    fun loadSeeds() {
        autoMixSeedJob?.cancel()
        autoMixSeedJob =
            scope.launch {
                favoritesRepository.getAllFavorites().collect { favorites ->
                    val artists =
                        favorites.mapNotNull { it.artist.takeIf { artist -> artist.isNotBlank() } }
                            .distinct()
                            .sorted()
                    editState.update { state ->
                        state.copy(
                            favoriteTracks = favorites,
                            favoriteArtists = artists,
                        )
                    }
                }
            }
    }

    fun generate(
        seed: AutoMixSeed,
        limit: Int = DEFAULT_MIX_SIZE,
    ) {
        scope.launch {
            editState.update { it.copy(autoMixState = AutoMixState.Loading) }
            val mixTracks =
                when (seed) {
                    is AutoMixSeed.FavoriteTrack -> {
                        autoMixGenerator.fromFavoriteTrack(
                            seed = seed.track,
                            libraryTracks = musicLibraryRepository.getAllTracks(),
                            limit = limit,
                        )
                    }
                    is AutoMixSeed.FavoriteArtist -> {
                        autoMixGenerator.fromFavoriteArtist(
                            artist = seed.artist,
                            libraryTracks = musicLibraryRepository.getAllTracks(),
                            limit = limit,
                        )
                    }
                    is AutoMixSeed.SmartPlaylist -> {
                        val tracks = smartPlaylistRepository.getTracksForType(seed.type).first()
                        autoMixGenerator.fromSmartPlaylist(
                            tracks = tracks,
                            limit = limit,
                        )
                    }
                }
            editState.update {
                if (mixTracks.isEmpty()) {
                    it.copy(autoMixState = AutoMixState.Error("No tracks found for this mix."))
                } else {
                    it.copy(autoMixState = AutoMixState.Preview(seed, mixTracks))
                }
            }
        }
    }

    fun clearPreview() {
        editState.update { it.copy(autoMixState = AutoMixState.Idle) }
    }

    fun saveAsPlaylist(name: String) {
        val current = editState.value.autoMixState
        if (name.isBlank() || current !is AutoMixState.Preview) return
        scope.launch {
            createPlaylistWithTracks(name.trim(), current.tracks.map { it.id })
            editState.update { it.copy(autoMixState = AutoMixState.Idle) }
        }
    }

    companion object {
        const val DEFAULT_MIX_SIZE = 30
    }
}
