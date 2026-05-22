package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.AutoMixGenerator
import com.anplak.androidmusic.player.TrackInfo
import kotlin.random.Random

class RecommendationEngine(
    private val autoMixGenerator: AutoMixGenerator,
    private val clock: RecommendationClock = SystemRecommendationClock
) {
    suspend fun buildRows(inputs: RecommendationInputs): List<RecommendationRow> {
        if (inputs.library.isEmpty()) return emptyList()

        val libraryById = inputs.library.associateBy { it.id }
        val rows = mutableListOf<RecommendationRow>()

        buildContinueListening(inputs, libraryById)?.let { rows += it }
        rows += buildDailyMix(inputs)
        rows += buildBecauseRows(inputs, libraryById)
        rows += buildQuickMixes(inputs)

        return rows.distinctBy { it.id }.filter { it.tracks.isNotEmpty() }
    }

    private fun buildContinueListening(
        inputs: RecommendationInputs,
        libraryById: Map<Long, TrackInfo>
    ): RecommendationRow? {
        val tracks = inputs.lastSessionTrackIds
            .mapNotNull { libraryById[it] }
            .distinctBy { it.id }
        if (tracks.size < 2) return null

        return RecommendationRow(
            id = "continue_listening",
            type = RecommendationRowType.CONTINUE_LISTENING,
            title = "Continue Listening",
            subtitle = "${tracks.size} tracks from your last session",
            tracks = tracks.take(PREVIEW_LIMIT)
        )
    }

    private suspend fun buildDailyMix(inputs: RecommendationInputs): RecommendationRow {
        val daySeed = clock.epochDay()
        val artist = inputs.topArtists30d.firstOrNull { it.isNotBlank() }
            ?: inputs.library
                .filter { it.artist.isNotBlank() }
                .randomOrNull(Random(daySeed))
                ?.artist
            ?: "Unknown Artist"

        val tracks = autoMixGenerator.fromFavoriteArtist(
            artist = artist,
            libraryTracks = inputs.library,
            limit = DAILY_MIX_LIMIT
        )

        return RecommendationRow(
            id = "daily_mix_$daySeed",
            type = RecommendationRowType.DAILY_MIX,
            title = "Daily Mix",
            subtitle = artist,
            tracks = tracks
        )
    }

    private suspend fun buildBecauseRows(
        inputs: RecommendationInputs,
        libraryById: Map<Long, TrackInfo>
    ): List<RecommendationRow> {
        val favoriteTracks = inputs.favorites
            .mapNotNull { libraryById[it] }
            .take(2)
        val topTracks = inputs.topTracks30d
            .mapNotNull { libraryById[it] }
            .take(3)

        val seeds = (favoriteTracks + topTracks).distinctBy { it.id }.take(MAX_BECAUSE_ROWS)
        return seeds.mapNotNull { seed ->
            val coPlayed = inputs.coOccurrenceBySeed[seed.id].orEmpty()
            val similar = similarTracks(seed, inputs.library, coPlayed, DETAIL_LIMIT)
            if (similar.isEmpty()) return@mapNotNull null

            val label = seed.artist.ifBlank { seed.title }
            RecommendationRow(
                id = "because_${seed.id}",
                type = RecommendationRowType.BECAUSE_YOU_LISTEN,
                title = "Because you listen to $label",
                subtitle = seed.title,
                seedTrack = seed,
                tracks = similar
            )
        }
    }

    private suspend fun buildQuickMixes(inputs: RecommendationInputs): List<RecommendationRow> {
        val libraryById = inputs.library.associateBy { it.id }
        val seeds = inputs.favorites
            .mapNotNull { libraryById[it] }
            .ifEmpty {
                inputs.topTracks30d.mapNotNull { libraryById[it] }
            }
            .distinctBy { it.id }
            .take(MAX_QUICK_MIX_ROWS)

        return seeds.mapIndexed { index, seed ->
            val tracks = autoMixGenerator.fromFavoriteTrack(
                seed = seed,
                libraryTracks = inputs.library,
                limit = QUICK_MIX_LIMIT
            )
            RecommendationRow(
                id = "quick_mix_${seed.id}_$index",
                type = RecommendationRowType.QUICK_MIX,
                title = "Quick Mix",
                subtitle = seed.title,
                seedTrack = seed,
                tracks = tracks
            )
        }
    }

    private fun similarTracks(
        seed: TrackInfo,
        library: List<TrackInfo>,
        coPlayed: List<Long>,
        limit: Int
    ): List<TrackInfo> {
        val libraryById = library.associateBy { it.id }
        val byCo = coPlayed.mapNotNull { libraryById[it] }
        val sameAlbum = library.filter {
            it.album.isNotBlank() && it.album == seed.album && it.id != seed.id
        }
        val sameArtist = library.filter {
            it.artist.equals(seed.artist, ignoreCase = true) && it.id != seed.id
        }
        return (listOf(seed) + byCo + sameAlbum + sameArtist)
            .distinctBy { it.id }
            .take(limit)
    }

    companion object {
        private const val PREVIEW_LIMIT = 15
        private const val DETAIL_LIMIT = 25
        private const val DAILY_MIX_LIMIT = 15
        private const val QUICK_MIX_LIMIT = 10
        private const val MAX_BECAUSE_ROWS = 3
        private const val MAX_QUICK_MIX_ROWS = 2
        const val CO_OCCURRENCE_SEED_LIMIT = 5
        const val CO_OCCURRENCE_TRACK_LIMIT = 10
    }
}
