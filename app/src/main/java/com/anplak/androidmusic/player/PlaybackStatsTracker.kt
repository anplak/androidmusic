package com.anplak.androidmusic.player

import com.anplak.androidmusic.data.RankingConfig
import com.anplak.androidmusic.data.TrackRankingWeights

enum class PlayStartReason {
    EXPLICIT,
    AUTO_ADVANCE,
}

data class PlaybackSession(
    val trackId: Long,
    val startedAtMs: Long,
    val startReason: PlayStartReason,
    var qualifiedPlayRecorded: Boolean = false,
    var historyRecorded: Boolean = false,
    var maxPositionMs: Long = 0L,
    var durationMs: Long = 0L,
    var skipRecorded: Boolean = false,
)

sealed interface SessionOutcome {
    data object AlreadyRecorded : SessionOutcome

    data object QualifiedPlay : SessionOutcome

    data object FastSkip : SessionOutcome

    data object NoOp : SessionOutcome
}

object PlaybackSessionClassifier {
    fun classify(
        session: PlaybackSession,
        listenedMs: Long,
        trackDurationMs: Long,
    ): SessionOutcome {
        if (session.qualifiedPlayRecorded) return SessionOutcome.AlreadyRecorded
        return when {
            TrackRankingWeights.isQualifiedListen(listenedMs, trackDurationMs) ->
                SessionOutcome.QualifiedPlay
            listenedMs < RankingConfig.SKIP_WINDOW_MS ->
                SessionOutcome.FastSkip
            else -> SessionOutcome.NoOp
        }
    }

    fun listenedMs(
        session: PlaybackSession,
        lastPositionMs: Long,
        nowMs: Long,
    ): Long {
        val wallClockMs = (nowMs - session.startedAtMs).coerceAtLeast(0)
        if (lastPositionMs <= 0) return wallClockMs
        // Player position can lag or carry over from the previous track; never exceed elapsed time.
        return if (lastPositionMs < wallClockMs) lastPositionMs else wallClockMs
    }
}

class PlaybackStatsTracker {
    private var session: PlaybackSession? = null
    private var pendingStartReason = PlayStartReason.AUTO_ADVANCE

    fun markExplicitStart() {
        pendingStartReason = PlayStartReason.EXPLICIT
    }

    fun markAutoAdvance() {
        pendingStartReason = PlayStartReason.AUTO_ADVANCE
    }

    fun currentSession(): PlaybackSession? = session

    /**
     * Finalizes the previous session (returned) and starts tracking [newTrackId] if non-null.
     */
    fun onTrackChanged(
        newTrackId: Long?,
        nowMs: Long = System.currentTimeMillis(),
    ): PlaybackSession? {
        val finalized = session
        session =
            newTrackId?.let { trackId ->
                PlaybackSession(
                    trackId = trackId,
                    startedAtMs = nowMs,
                    startReason = pendingStartReason,
                ).also {
                    pendingStartReason = PlayStartReason.AUTO_ADVANCE
                }
            }
        return finalized
    }

    fun updatePlaybackProgress(
        positionMs: Long,
        durationMs: Long,
    ) {
        session?.let {
            it.maxPositionMs = maxOf(it.maxPositionMs, positionMs)
            if (durationMs > 0) {
                it.durationMs = durationMs
            }
        }
    }

    fun clear() {
        session = null
    }
}
