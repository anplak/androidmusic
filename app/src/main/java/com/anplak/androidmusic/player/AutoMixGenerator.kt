package com.anplak.androidmusic.player

import kotlin.random.Random

/**
 * Generates auto-mix playlists based on a chosen seed.
 */
class AutoMixGenerator(
    private val smartShuffleGenerator: SmartShuffleGenerator,
) {
    suspend fun fromFavoriteTrack(
        seed: TrackInfo,
        libraryTracks: List<TrackInfo>,
        limit: Int,
        random: Random? = null,
    ): List<TrackInfo> {
        if (libraryTracks.isEmpty()) return emptyList()
        val sameArtist = libraryTracks.filter { it.artist.equals(seed.artist, ignoreCase = true) }
        val rest = libraryTracks.filter { it.artist.equals(seed.artist, ignoreCase = true).not() }
        val sameArtistShuffled = shuffle(sameArtist, random)
        val restShuffled = shuffle(rest, random)
        return (listOf(seed) + sameArtistShuffled + restShuffled)
            .distinctBy { it.id }
            .take(limit)
    }

    suspend fun fromFavoriteArtist(
        artist: String,
        libraryTracks: List<TrackInfo>,
        limit: Int,
        random: Random? = null,
    ): List<TrackInfo> {
        if (libraryTracks.isEmpty()) return emptyList()
        val artistTracks = libraryTracks.filter { it.artist.equals(artist, ignoreCase = true) }
        val fillerBase = libraryTracks.filter { it.artist.equals(artist, ignoreCase = true).not() }
        val filler = shuffle(fillerBase, random)
        return (artistTracks + filler)
            .distinctBy { it.id }
            .take(limit)
    }

    suspend fun fromSmartPlaylist(
        tracks: List<TrackInfo>,
        limit: Int,
        random: Random? = null,
    ): List<TrackInfo> {
        if (tracks.isEmpty()) return emptyList()
        return shuffle(tracks, random).take(limit)
    }

    private suspend fun shuffle(
        tracks: List<TrackInfo>,
        random: Random?,
    ): List<TrackInfo> =
        if (random != null) {
            smartShuffleGenerator.generateShuffledQueue(tracks, random = random)
        } else {
            smartShuffleGenerator.generateShuffledQueue(tracks)
        }
}
