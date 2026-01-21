package com.anplak.androidmusic.player

/**
 * Generates auto-mix playlists based on a chosen seed.
 */
class AutoMixGenerator(
    private val smartShuffleGenerator: SmartShuffleGenerator
) {

    suspend fun fromFavoriteTrack(
        seed: TrackInfo,
        libraryTracks: List<TrackInfo>,
        limit: Int
    ): List<TrackInfo> {
        if (libraryTracks.isEmpty()) return emptyList()
        val sameArtist = libraryTracks.filter { it.artist.equals(seed.artist, ignoreCase = true) }
        val rest = libraryTracks.filter { it.artist.equals(seed.artist, ignoreCase = true).not() }
        val sameArtistShuffled = smartShuffleGenerator.generateShuffledQueue(sameArtist)
        val restShuffled = smartShuffleGenerator.generateShuffledQueue(rest)
        return (listOf(seed) + sameArtistShuffled + restShuffled)
            .distinctBy { it.id }
            .take(limit)
    }

    suspend fun fromFavoriteArtist(
        artist: String,
        libraryTracks: List<TrackInfo>,
        limit: Int
    ): List<TrackInfo> {
        if (libraryTracks.isEmpty()) return emptyList()
        val artistTracks = libraryTracks.filter { it.artist.equals(artist, ignoreCase = true) }
        val fillerBase = libraryTracks.filter { it.artist.equals(artist, ignoreCase = true).not() }
        val filler = smartShuffleGenerator.generateShuffledQueue(fillerBase)
        return (artistTracks + filler)
            .distinctBy { it.id }
            .take(limit)
    }

    suspend fun fromSmartPlaylist(
        tracks: List<TrackInfo>,
        limit: Int
    ): List<TrackInfo> {
        if (tracks.isEmpty()) return emptyList()
        return smartShuffleGenerator.generateShuffledQueue(tracks).take(limit)
    }
}
