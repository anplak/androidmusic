package com.anplak.androidmusic.data

import kotlinx.coroutines.flow.first

interface RecommendationRepository {
    suspend fun loadInputs(): RecommendationInputs
}

class RecommendationRepositoryImpl(
    private val musicLibraryRepository: MusicLibraryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val playlistRepository: PlaylistRepository
) : RecommendationRepository {

    override suspend fun loadInputs(): RecommendationInputs {
        val library = musicLibraryRepository.getAllTracks()
        val favorites = favoritesRepository.getAllFavoriteIds().first()
        val since30d = System.currentTimeMillis() - THIRTY_DAYS_MS

        val topArtists = playHistoryRepository.getTopArtistsSince(since30d, TOP_ARTIST_LIMIT)
            .first()
            .map { it.artist }

        val topTracks = playHistoryRepository.getTopTracksSince(since30d, TOP_TRACK_LIMIT)
            .first()
            .map { it.trackId }

        val recentHistory = playHistoryRepository.getHistory(HISTORY_LIMIT, 0).first()
        val lastSessionTrackIds = playHistoryRepository.getLastSessionTrackIds(LAST_SESSION_LIMIT)

        val seedIds = (favorites.take(RecommendationEngine.CO_OCCURRENCE_SEED_LIMIT) +
            topTracks.take(RecommendationEngine.CO_OCCURRENCE_SEED_LIMIT))
            .distinct()
            .take(RecommendationEngine.CO_OCCURRENCE_SEED_LIMIT)

        val coOccurrenceBySeed = seedIds.associateWith { seedId ->
            playHistoryRepository.getCoPlayedTrackIds(
                seedId,
                RecommendationEngine.CO_OCCURRENCE_TRACK_LIMIT
            )
        }

        val userPlaylists = playlistRepository.getPlaylists().first().map { playlist ->
            PlaylistSummary(
                id = playlist.id,
                name = playlist.name,
                trackCount = playlist.trackCount
            )
        }

        return RecommendationInputs(
            library = library,
            favorites = favorites,
            topArtists30d = topArtists,
            topTracks30d = topTracks,
            recentHistory = recentHistory,
            coOccurrenceBySeed = coOccurrenceBySeed,
            lastSessionTrackIds = lastSessionTrackIds,
            userPlaylists = userPlaylists
        )
    }

    companion object {
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
        private const val TOP_ARTIST_LIMIT = 5
        private const val TOP_TRACK_LIMIT = 10
        private const val HISTORY_LIMIT = 50
        private const val LAST_SESSION_LIMIT = 15
    }
}
