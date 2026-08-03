package com.anplak.androidmusic.player

import com.anplak.androidmusic.data.RankingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackSessionClassifierTest {
    @Test
    fun `classify returns AlreadyRecorded when qualified play already recorded`() {
        val session =
            PlaybackSession(
                trackId = 1L,
                startedAtMs = 0L,
                startReason = PlayStartReason.EXPLICIT,
                qualifiedPlayRecorded = true,
            )

        val outcome = PlaybackSessionClassifier.classify(session, listenedMs = 0L, trackDurationMs = 180_000L)

        assertEquals(SessionOutcome.AlreadyRecorded, outcome)
    }

    @Test
    fun `classify returns QualifiedPlay at listen threshold`() {
        val session = PlaybackSession(1L, 0L, PlayStartReason.AUTO_ADVANCE)

        val outcome =
            PlaybackSessionClassifier.classify(
                session,
                listenedMs = RankingConfig.QUALIFIED_PLAY_MS,
                trackDurationMs = 180_000L,
            )

        assertEquals(SessionOutcome.QualifiedPlay, outcome)
    }

    @Test
    fun `classify returns FastSkip within skip window`() {
        val session = PlaybackSession(1L, 0L, PlayStartReason.AUTO_ADVANCE)

        val outcome =
            PlaybackSessionClassifier.classify(
                session,
                listenedMs = 10_000L,
                trackDurationMs = 180_000L,
            )

        assertEquals(SessionOutcome.FastSkip, outcome)
    }

    @Test
    fun `classify returns NoOp for abandoned auto-advance before threshold`() {
        val session = PlaybackSession(1L, 0L, PlayStartReason.AUTO_ADVANCE)

        val outcome =
            PlaybackSessionClassifier.classify(
                session,
                listenedMs = 20_000L,
                trackDurationMs = 180_000L,
            )

        assertEquals(SessionOutcome.NoOp, outcome)
    }

    @Test
    fun `listenedMs uses playback position when available`() {
        val session = PlaybackSession(1L, startedAtMs = 1_000L, startReason = PlayStartReason.EXPLICIT)

        val listened = PlaybackSessionClassifier.listenedMs(session, lastPositionMs = 42_000L, nowMs = 43_000L)

        assertEquals(42_000L, listened)
    }

    @Test
    fun `playback stats tracker marks explicit and auto advance`() {
        val tracker = PlaybackStatsTracker()

        tracker.markExplicitStart()
        val explicit = tracker.onTrackChanged(1L, nowMs = 100L)
        assertEquals(null, explicit)
        assertEquals(PlayStartReason.EXPLICIT, tracker.currentSession()?.startReason)

        tracker.markAutoAdvance()
        val finalized = tracker.onTrackChanged(2L, nowMs = 200L)
        assertEquals(1L, finalized?.trackId)
        assertEquals(PlayStartReason.AUTO_ADVANCE, tracker.currentSession()?.startReason)
    }

    @Test
    fun `listenedMs caps stale player position by wall clock`() {
        val session = PlaybackSession(1L, startedAtMs = 1_000L, startReason = PlayStartReason.AUTO_ADVANCE)

        val listened =
            PlaybackSessionClassifier.listenedMs(
                session,
                lastPositionMs = 60_000L,
                nowMs = 3_000L,
            )

        assertEquals(2_000L, listened)
    }

    @Test
    fun `playback stats tracker records max position for session`() {
        val tracker = PlaybackStatsTracker()
        tracker.markExplicitStart()
        tracker.onTrackChanged(1L, nowMs = 100L)

        tracker.updatePlaybackProgress(positionMs = 5_000L, durationMs = 180_000L)
        tracker.updatePlaybackProgress(positionMs = 2_000L, durationMs = 180_000L)

        val session = tracker.currentSession()
        assertEquals(5_000L, session?.maxPositionMs)
        assertEquals(180_000L, session?.durationMs)
    }

    @Test
    fun `auto-advance session is eligible for manual skip before qualification`() {
        val session =
            PlaybackSession(
                trackId = 3L,
                startedAtMs = 300L,
                startReason = PlayStartReason.AUTO_ADVANCE,
            )
        assertEquals(PlayStartReason.AUTO_ADVANCE, session.startReason)
        assertFalse(session.qualifiedPlayRecorded)
        assertFalse(session.skipRecorded)
    }
}
