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
        playlistIdsByTrack: Map<Long, Set<Long>> = emptyMap(),
        candidateTracks: List<TrackInfo>? = null,
    ): List<TrackInfo> {
        if (libraryTracks.isEmpty()) return emptyList()
        val candidates = candidateTracks ?: libraryTracks
        val sameArtist =
            libraryTracks.filter { track ->
                track.id != seed.id && track.artist.equals(seed.artist, ignoreCase = true)
            }
        val preferred = listOf(seed) + shuffle(sameArtist, random)
        val preferredIds = preferred.mapTo(HashSet()) { it.id }
        val randomTierOrder = shuffle(candidates.filter { it.id !in preferredIds }, random)
        return MixArtistLimiter.limit(
            preferred = preferred,
            candidates = candidates,
            limit = limit,
            playlistIdsByTrack = playlistIdsByTrack,
            randomTierOrder = randomTierOrder,
            leadArtist = seed.artist,
        )
    }

    suspend fun fromFavoriteArtist(
        artist: String,
        libraryTracks: List<TrackInfo>,
        limit: Int,
        random: Random? = null,
        playlistIdsByTrack: Map<Long, Set<Long>> = emptyMap(),
        candidateTracks: List<TrackInfo>? = null,
    ): List<TrackInfo> {
        if (libraryTracks.isEmpty()) return emptyList()
        val candidates = candidateTracks ?: libraryTracks
        val artistTracks = libraryTracks.filter { it.artist.equals(artist, ignoreCase = true) }
        val preferred = shuffle(artistTracks, random)
        val preferredIds = preferred.mapTo(HashSet()) { it.id }
        val randomTierOrder = shuffle(candidates.filter { it.id !in preferredIds }, random)
        return MixArtistLimiter.limit(
            preferred = preferred,
            candidates = candidates,
            limit = limit,
            playlistIdsByTrack = playlistIdsByTrack,
            randomTierOrder = randomTierOrder,
            leadArtist = artist,
        )
    }

    suspend fun fromSmartPlaylist(
        tracks: List<TrackInfo>,
        limit: Int,
        random: Random? = null,
        playlistIdsByTrack: Map<Long, Set<Long>> = emptyMap(),
        candidateTracks: List<TrackInfo>? = null,
    ): List<TrackInfo> {
        if (tracks.isEmpty()) return emptyList()
        val candidates = candidateTracks ?: tracks
        return MixArtistLimiter.limit(
            preferred = shuffle(tracks, random),
            candidates = candidates,
            limit = limit,
            playlistIdsByTrack = playlistIdsByTrack,
            randomTierOrder = shuffle(candidates, random),
            leadArtist = null,
        )
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
