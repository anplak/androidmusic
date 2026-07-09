package com.anplak.androidmusic.data

interface RankingClock {
    fun nowMs(): Long
}

object SystemRankingClock : RankingClock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
