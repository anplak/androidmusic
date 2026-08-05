package com.anplak.androidmusic.player

import com.anplak.androidmusic.data.PlayHistoryRepository
import com.anplak.androidmusic.data.TrackStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Orchestrates playback session classification and persistence side effects.
 */
class PlaybackSessionRecorder(
    private val playbackStatsTracker: PlaybackStatsTracker,
    private val trackStatsRepository: TrackStatsRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val scope: CoroutineScope,
) {
    private var lastTrackedTrackId: Long? = null
    private var lastTrackWasPlaying: Boolean = false
    private var lastPosition: Long = 0L
    private var lastDuration: Long = 0L
    private var currentSessionId: String = UUID.randomUUID().toString()

    fun onUiStateTick(
        trackId: Long?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long,
    ) {
        val nowMs = System.currentTimeMillis()

        maybeRecordCompletion(trackId)

        if (trackId != lastTrackedTrackId) {
            val finalizedSession = playbackStatsTracker.onTrackChanged(trackId, nowMs)
            finalizedSession?.let { session ->
                scope.launch {
                    finalizeSession(session, nowMs)
                }
            }

            playbackStatsTracker.currentSession()?.let { session ->
                if (session.startReason == PlayStartReason.EXPLICIT) {
                    scope.launch {
                        recordQualifiedPlay(session)
                    }
                }
            }
            lastTrackedTrackId = trackId
        } else if (trackId != null) {
            playbackStatsTracker.updatePlaybackProgress(
                positionMs = positionMs,
                durationMs = durationMs,
            )
        }

        lastTrackWasPlaying = isPlaying
        lastPosition = positionMs
        lastDuration = durationMs
    }

    fun onManualAdvance() {
        val session = playbackStatsTracker.currentSession() ?: return
        if (session.startReason != PlayStartReason.AUTO_ADVANCE ||
            session.qualifiedPlayRecorded ||
            session.skipRecorded
        ) {
            return
        }
        session.skipRecorded = true
        scope.launch {
            trackStatsRepository.recordSkip(session.trackId)
        }
    }

    fun onClear() {
        val finalized = playbackStatsTracker.onTrackChanged(null)
        finalized?.let { session ->
            scope.launch {
                finalizeSession(session, System.currentTimeMillis())
            }
        }
        lastTrackedTrackId = null
    }

    fun onDestroy() {
        playbackStatsTracker.currentSession()?.let { session ->
            scope.launch {
                finalizeSession(session, System.currentTimeMillis())
            }
        }
    }

    private fun maybeRecordCompletion(currentTrackId: Long?) {
        val previousTrackId = lastTrackedTrackId ?: return
        val shouldCheckCompletion = lastTrackWasPlaying && lastDuration > 0 && currentTrackId != previousTrackId
        if (!shouldCheckCompletion) return

        val completionThreshold = lastDuration * 0.9
        if (lastPosition >= completionThreshold) {
            scope.launch {
                trackStatsRepository.recordCompletion(previousTrackId)
            }
        }
    }

    suspend fun finalizeSession(
        session: PlaybackSession,
        nowMs: Long,
    ) {
        val listenedMs =
            PlaybackSessionClassifier.listenedMs(
                session,
                lastPositionMs = session.maxPositionMs,
                nowMs = nowMs,
            )
        when (PlaybackSessionClassifier.classify(session, listenedMs, session.durationMs)) {
            SessionOutcome.AlreadyRecorded -> Unit
            SessionOutcome.QualifiedPlay -> recordQualifiedPlay(session)
            SessionOutcome.FastSkip -> {
                if (!session.skipRecorded) {
                    trackStatsRepository.recordSkip(session.trackId)
                }
            }
            SessionOutcome.NoOp -> Unit
        }
    }

    private suspend fun recordQualifiedPlay(session: PlaybackSession) {
        if (session.qualifiedPlayRecorded) return
        trackStatsRepository.recordQualifiedPlay(session.trackId, session.startedAtMs)
        session.qualifiedPlayRecorded = true
        recordQualifiedHistory(session)
    }

    private suspend fun recordQualifiedHistory(session: PlaybackSession) {
        if (session.historyRecorded) return
        playHistoryRepository.recordPlay(session.trackId, currentSessionId)
        session.historyRecorded = true
    }
}
