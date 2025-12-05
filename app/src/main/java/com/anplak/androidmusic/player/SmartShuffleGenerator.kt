package com.anplak.androidmusic.player

import com.anplak.androidmusic.data.FavoritesRepository
import com.anplak.androidmusic.data.TrackStatsRepository
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * Generates smart shuffled queues with:
 * - Higher probability for favorites (3x weight)
 * - Higher probability for high play-count tracks (2x weight for top 20%)
 * - Avoids immediate repeats by excluding recently played tracks
 */
class SmartShuffleGenerator(
    private val favoritesRepository: FavoritesRepository,
    private val statsRepository: TrackStatsRepository,
    private val random: Random = Random.Default
) {
    
    companion object {
        private const val FAVORITE_WEIGHT = 3.0
        private const val HIGH_PLAY_COUNT_WEIGHT = 2.0
        private const val DEFAULT_WEIGHT = 1.0
        private const val HIGH_PLAY_COUNT_PERCENTILE = 0.2 // Top 20%
    }
    
    /**
     * Generates a shuffled queue from the given tracks.
     * 
     * @param tracks The tracks to shuffle
     * @param recentlyPlayedIds Track IDs to exclude from early selection (avoid repeats)
     * @return A shuffled list with smart weighting applied
     */
    suspend fun generateShuffledQueue(
        tracks: List<TrackInfo>,
        recentlyPlayedIds: Set<Long> = emptySet()
    ): List<TrackInfo> {
        if (tracks.isEmpty()) return emptyList()
        if (tracks.size == 1) return tracks
        
        // Get favorite IDs
        val favoriteIds = favoritesRepository.getAllFavoriteIds().first()
        
        // Get high play-count threshold
        val allStats = statsRepository.getAllStatsOrderedByPlayCount()
        val highPlayCountThreshold = calculateHighPlayCountThreshold(allStats.map { it.playCount })
        val highPlayCountIds = allStats
            .filter { it.playCount >= highPlayCountThreshold }
            .map { it.trackId }
            .toSet()
        
        // Build weighted list
        val weightedTracks = tracks.map { track ->
            val weight = calculateWeight(track.id, favoriteIds, highPlayCountIds)
            WeightedTrack(track, weight)
        }
        
        // Perform weighted shuffle with repeat avoidance
        return weightedShuffle(weightedTracks, recentlyPlayedIds)
    }
    
    /**
     * Calculates the weight for a track based on favorite status and play count.
     */
    internal fun calculateWeight(
        trackId: Long,
        favoriteIds: Set<Long>,
        highPlayCountIds: Set<Long>
    ): Double {
        var weight = DEFAULT_WEIGHT
        
        if (trackId in favoriteIds) {
            weight *= FAVORITE_WEIGHT
        }
        
        if (trackId in highPlayCountIds) {
            weight *= HIGH_PLAY_COUNT_WEIGHT
        }
        
        return weight
    }
    
    /**
     * Calculates the threshold for "high play count" (top 20% percentile).
     */
    internal fun calculateHighPlayCountThreshold(playCounts: List<Int>): Int {
        if (playCounts.isEmpty()) return Int.MAX_VALUE
        if (playCounts.size == 1) return playCounts.first()
        
        val sorted = playCounts.sortedDescending()
        val thresholdIndex = (sorted.size * HIGH_PLAY_COUNT_PERCENTILE).toInt().coerceAtLeast(1) - 1
        return sorted.getOrElse(thresholdIndex) { sorted.last() }
    }
    
    /**
     * Performs weighted random shuffle with repeat avoidance.
     * Recently played tracks are pushed to the end of the queue.
     */
    private fun weightedShuffle(
        weightedTracks: List<WeightedTrack>,
        recentlyPlayedIds: Set<Long>
    ): List<TrackInfo> {
        val result = mutableListOf<TrackInfo>()
        val remaining = weightedTracks.toMutableList()
        
        // Separate recently played tracks to add at the end
        val recentlyPlayed = remaining.filter { it.track.id in recentlyPlayedIds }
        remaining.removeAll(recentlyPlayed)
        
        // Weighted selection for non-recently-played tracks
        while (remaining.isNotEmpty()) {
            val selected = selectWeightedRandom(remaining)
            result.add(selected.track)
            remaining.remove(selected)
        }
        
        // Add recently played tracks at the end (shuffled among themselves)
        val shuffledRecent = recentlyPlayed.shuffled(random).map { it.track }
        result.addAll(shuffledRecent)
        
        return result
    }
    
    /**
     * Selects a random element from the list based on weights.
     */
    private fun selectWeightedRandom(weighted: List<WeightedTrack>): WeightedTrack {
        val totalWeight = weighted.sumOf { it.weight }
        var randomValue = random.nextDouble() * totalWeight
        
        for (item in weighted) {
            randomValue -= item.weight
            if (randomValue <= 0) {
                return item
            }
        }
        
        // Fallback to last item (should rarely happen due to floating point)
        return weighted.last()
    }
    
    private data class WeightedTrack(
        val track: TrackInfo,
        val weight: Double
    )
}

