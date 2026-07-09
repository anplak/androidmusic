package com.anplak.androidmusic.data

object RankingConfig {
    const val SKIP_WINDOW_MS = 15_000L
    const val QUALIFIED_PLAY_MS = 30_000L
    const val QUALIFIED_PLAY_FRACTION = 0.50f
    const val FAVORITE_BASE_MULTIPLIER = 3.0
    const val FAVORITE_HALF_LIFE_MS = 90L * 24 * 60 * 60 * 1000
    const val SKIP_DECAY_FACTOR = 0.65
    const val PLAY_COUNT_LOG_SCALE = 1.0
    const val MIN_WEIGHT = 0.05
    const val MAX_SHUFFLE_SHARE = 0.30
}
