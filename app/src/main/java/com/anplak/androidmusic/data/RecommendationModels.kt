package com.anplak.androidmusic.data

import com.anplak.androidmusic.player.TrackInfo

enum class RecommendationRowType {
    BECAUSE_YOU_LISTEN,
    QUICK_MIX,
    DAILY_MIX,
    CONTINUE_LISTENING
}

data class RecommendationRow(
    val id: String,
    val type: RecommendationRowType,
    val title: String,
    val subtitle: String? = null,
    val seedTrack: TrackInfo? = null,
    val tracks: List<TrackInfo>
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val trackCount: Int
)

data class RecommendationInputs(
    val library: List<TrackInfo>,
    val favorites: Set<Long>,
    val topArtists30d: List<String>,
    val topTracks30d: List<Long>,
    val recentHistory: List<PlayHistoryEntry>,
    val coOccurrenceBySeed: Map<Long, List<Long>>,
    val lastSessionTrackIds: List<Long>,
    val userPlaylists: List<PlaylistSummary>
)

fun interface RecommendationClock {
    fun epochDay(): Long
}

object SystemRecommendationClock : RecommendationClock {
    override fun epochDay(): Long = java.time.LocalDate.now().toEpochDay()
}
