package com.anplak.androidmusic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackRankingWeightsTest {

    private val nowMs = 1_700_000_000_000L
    private val dayMs = 24L * 60 * 60 * 1000

    @Test
    fun `effectiveWeight returns base weight for track with no signals`() {
        val weight = TrackRankingWeights.effectiveWeight(
            RankingInputs(
                playCount = 0,
                skipCount = 0,
                isFavorite = false,
                favoritedAt = null,
                nowMs = nowMs
            )
        )

        assertEquals(1.0, weight, 0.001)
    }

    @Test
    fun `effectiveWeight increases with qualified play count`() {
        val low = TrackRankingWeights.effectiveWeight(baseInputs(playCount = 1))
        val high = TrackRankingWeights.effectiveWeight(baseInputs(playCount = 20))

        assertTrue(high > low)
    }

    @Test
    fun `effectiveWeight decreases with skip count`() {
        val noSkips = TrackRankingWeights.effectiveWeight(baseInputs(skipCount = 0))
        val skipped = TrackRankingWeights.effectiveWeight(baseInputs(skipCount = 2))

        assertTrue(skipped < noSkips)
    }

    @Test
    fun `recent favorite outranks stale favorite at equal play count`() {
        val recent = TrackRankingWeights.effectiveWeight(
            baseInputs(
                isFavorite = true,
                favoritedAt = nowMs - dayMs
            )
        )
        val stale = TrackRankingWeights.effectiveWeight(
            baseInputs(
                isFavorite = true,
                favoritedAt = nowMs - (730L * dayMs)
            )
        )

        assertTrue(recent > stale)
    }

    @Test
    fun `isQualifiedListen returns false below 30 second threshold`() {
        assertFalse(TrackRankingWeights.isQualifiedListen(29_000L, 180_000L))
    }

    @Test
    fun `isQualifiedListen returns true at 30 second threshold`() {
        assertTrue(TrackRankingWeights.isQualifiedListen(30_000L, 180_000L))
    }

    @Test
    fun `isQualifiedListen returns true at 50 percent fraction`() {
        assertTrue(TrackRankingWeights.isQualifiedListen(20_000L, 40_000L))
    }

    @Test
    fun `effectiveWeight respects minimum floor`() {
        val weight = TrackRankingWeights.effectiveWeight(
            baseInputs(playCount = 0, skipCount = 50)
        )

        assertEquals(RankingConfig.MIN_WEIGHT, weight, 0.001)
    }

    private fun baseInputs(
        playCount: Int = 5,
        skipCount: Int = 0,
        isFavorite: Boolean = false,
        favoritedAt: Long? = null
    ) = RankingInputs(
        playCount = playCount,
        skipCount = skipCount,
        isFavorite = isFavorite,
        favoritedAt = favoritedAt,
        nowMs = nowMs
    )
}
