package com.anplak.androidmusic.data

import kotlin.math.ln
import kotlin.math.pow

data class RankingInputs(
    val playCount: Int,
    val skipCount: Int,
    val isFavorite: Boolean,
    val favoritedAt: Long?,
    val nowMs: Long
)

object TrackRankingWeights {
    fun effectiveWeight(inputs: RankingInputs): Double {
        var weight = 1.0 + RankingConfig.PLAY_COUNT_LOG_SCALE * ln(1.0 + inputs.playCount)
        weight *= RankingConfig.SKIP_DECAY_FACTOR.pow(inputs.skipCount.toDouble())
        if (inputs.isFavorite) {
            val age = (inputs.nowMs - (inputs.favoritedAt ?: inputs.nowMs)).coerceAtLeast(0)
            val decay = 0.5.pow(age.toDouble() / RankingConfig.FAVORITE_HALF_LIFE_MS)
            weight *= RankingConfig.FAVORITE_BASE_MULTIPLIER * decay.coerceAtLeast(0.2)
        }
        return weight.coerceAtLeast(RankingConfig.MIN_WEIGHT)
    }

    fun isQualifiedListen(listenedMs: Long, trackDurationMs: Long): Boolean =
        listenedMs >= RankingConfig.QUALIFIED_PLAY_MS ||
            (trackDurationMs > 0 &&
                listenedMs >= (trackDurationMs * RankingConfig.QUALIFIED_PLAY_FRACTION).toLong())
}
