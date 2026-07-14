package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.TrackInfo

object MixDimensionConfig {
    const val MIN_TRACKS_PER_BUCKET = 8
    const val MIN_PLAYLIST_POOL = 3
    const val MAX_DIMENSION_ROWS = 2
    const val GENRE_MIX_LIMIT = 15
    const val PLAYLIST_MIX_LIMIT = 15
    const val LANGUAGE_MIX_LIMIT = 15
    const val QUICK_MIX_DIMENSION_LIMIT = 10
}

data class DimensionMix(
    val id: String,
    val type: RecommendationRowType,
    val title: String,
    val subtitle: String?,
    val seedTrack: TrackInfo? = null,
    val pool: List<TrackInfo>
)

object MixDimensionEngine {

    fun pickCandidates(inputs: RecommendationInputs, epochDay: Long): List<DimensionMix> {
        val picks = mutableListOf<DimensionMix>()
        pickGenreMix(inputs, epochDay)?.let { picks += it }
        if (picks.size < MixDimensionConfig.MAX_DIMENSION_ROWS) {
            pickPlaylistAffinityMix(inputs, epochDay)?.let { picks += it }
        }
        if (picks.size < MixDimensionConfig.MAX_DIMENSION_ROWS) {
            pickLanguageMix(inputs, epochDay)?.let { picks += it }
        }
        return picks.take(MixDimensionConfig.MAX_DIMENSION_ROWS)
    }

    fun largestGenrePool(inputs: RecommendationInputs): List<TrackInfo>? {
        return genreBuckets(inputs.library)
            .filter { (_, tracks) -> tracks.size >= MixDimensionConfig.MIN_TRACKS_PER_BUCKET }
            .maxByOrNull { (_, tracks) -> tracks.size }
            ?.value
    }

    private fun pickGenreMix(inputs: RecommendationInputs, epochDay: Long): DimensionMix? {
        val (bucket, pool) = genreBuckets(inputs.library)
            .filter { (_, tracks) -> tracks.size >= MixDimensionConfig.MIN_TRACKS_PER_BUCKET }
            .maxByOrNull { (_, tracks) -> tracks.size }
            ?: return null

        val label = bucket.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
        return DimensionMix(
            id = "genre_mix_${bucket}_$epochDay",
            type = RecommendationRowType.GENRE_MIX,
            title = "Your $label mix",
            subtitle = null,
            pool = pool
        )
    }

    private fun pickPlaylistAffinityMix(inputs: RecommendationInputs, epochDay: Long): DimensionMix? {
        val libraryById = inputs.library.associateBy { it.id }
        val seeds = (inputs.favorites.toList().take(3) + inputs.topTracks30d.take(3)).distinct()

        for (seedId in seeds) {
            val seed = libraryById[seedId] ?: continue
            val pool = inputs.coPlaylistBySeed[seedId]
                .orEmpty()
                .mapNotNull { libraryById[it] }
            if (pool.size < MixDimensionConfig.MIN_PLAYLIST_POOL) continue

            return DimensionMix(
                id = "playlist_affinity_${seedId}_$epochDay",
                type = RecommendationRowType.PLAYLIST_AFFINITY,
                title = "From your playlists",
                subtitle = seed.title,
                seedTrack = seed,
                pool = pool
            )
        }
        return null
    }

    private fun pickLanguageMix(inputs: RecommendationInputs, epochDay: Long): DimensionMix? {
        val buckets = inputs.library
            .mapNotNull { track -> track.language?.let { it to track } }
            .groupBy({ it.first.lowercase() }, { it.second })

        val (bucket, pool) = buckets
            .filter { (_, tracks) -> tracks.size >= MixDimensionConfig.MIN_TRACKS_PER_BUCKET }
            .maxByOrNull { (_, tracks) -> tracks.size }
            ?: return null

        val label = languageDisplayName(bucket)
        return DimensionMix(
            id = "language_mix_${bucket}_$epochDay",
            type = RecommendationRowType.LANGUAGE_MIX,
            title = "Your $label mix",
            subtitle = null,
            pool = pool
        )
    }

    private fun genreBuckets(library: List<TrackInfo>): Map<String, List<TrackInfo>> =
        library
            .mapNotNull { track -> track.effectiveGenre()?.let { it to track } }
            .groupBy({ it.first.lowercase() }, { it.second })

    private fun languageDisplayName(bucket: String): String = when (bucket) {
        "en" -> "English"
        "de" -> "German"
        "fr" -> "French"
        "es" -> "Spanish"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        else -> bucket.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }
}
