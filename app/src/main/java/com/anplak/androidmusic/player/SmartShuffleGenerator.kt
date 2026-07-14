package com.anplak.androidmusic.player

import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.RankingClock
import com.anplak.androidmusic.data.RankingInputs
import com.anplak.androidmusic.data.SystemRankingClock
import com.anplak.androidmusic.data.TrackRankingWeights
import com.anplak.androidmusic.data.TrackStatsRepository
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * Generates smart shuffled queues using intent-aware track weights:
 * qualified play count, skip penalty, and favorite recency decay.
 */
class SmartShuffleGenerator(
    private val favoritesRepository: FavoritesRepository,
    private val statsRepository: TrackStatsRepository,
    private val clock: RankingClock = SystemRankingClock,
    private val random: Random = Random.Default
) {
    /**
     * Generates a shuffled queue from the given tracks.
     *
     * @param tracks The tracks to shuffle
     * @param recentlyPlayedIds Track IDs to exclude from early selection (avoid repeats)
     * @return A shuffled list with smart weighting applied
     */
    suspend fun generateShuffledQueue(
        tracks: List<TrackInfo>,
        recentlyPlayedIds: Set<Long> = emptySet(),
        random: Random = this.random
    ): List<TrackInfo> {
        if (tracks.isEmpty()) return emptyList()
        if (tracks.size == 1) return tracks

        val statsById = statsRepository.getAllStatsOrderedByPlayCount()
            .associateBy { it.trackId }
        val favoriteTimes = favoritesRepository.getFavoriteTimestamps().first()
        val nowMs = clock.nowMs()

        val weightedTracks = tracks.map { track ->
            val stats = statsById[track.id]
            val weight = TrackRankingWeights.effectiveWeight(
                RankingInputs(
                    playCount = stats?.playCount ?: 0,
                    skipCount = stats?.skipCount ?: 0,
                    isFavorite = track.id in favoriteTimes,
                    favoritedAt = favoriteTimes[track.id],
                    nowMs = nowMs
                )
            )
            WeightedTrack(track, weight)
        }

        return weightedShuffle(weightedTracks, recentlyPlayedIds, random)
    }

    /**
     * Performs weighted random shuffle with repeat avoidance.
     * Recently played tracks are pushed to the end of the queue.
     */
    private fun weightedShuffle(
        weightedTracks: List<WeightedTrack>,
        recentlyPlayedIds: Set<Long>,
        random: Random
    ): List<TrackInfo> {
        val result = mutableListOf<TrackInfo>()
        val remaining = weightedTracks.toMutableList()

        val recentlyPlayed = remaining.filter { it.track.id in recentlyPlayedIds }
        remaining.removeAll(recentlyPlayed)

        while (remaining.isNotEmpty()) {
            val selected = selectWeightedRandom(remaining, random)
            result.add(selected.track)
            remaining.remove(selected)
        }

        val shuffledRecent = recentlyPlayed.shuffled(random).map { it.track }
        result.addAll(shuffledRecent)

        return result
    }

    private fun selectWeightedRandom(weighted: List<WeightedTrack>, random: Random): WeightedTrack {
        val totalWeight = weighted.sumOf { it.weight }
        var randomValue = random.nextDouble() * totalWeight

        for (item in weighted) {
            randomValue -= item.weight
            if (randomValue <= 0) {
                return item
            }
        }

        return weighted.last()
    }

    private data class WeightedTrack(
        val track: TrackInfo,
        val weight: Double
    )
}
