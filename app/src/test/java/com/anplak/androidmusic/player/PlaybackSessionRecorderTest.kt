package com.anplak.androidmusic.player

import com.anplak.androidmusic.data.ArtistPlayCount
import com.anplak.androidmusic.data.PlayHistoryEntry
import com.anplak.androidmusic.data.PlayHistoryRepository
import com.anplak.androidmusic.data.RankingConfig
import com.anplak.androidmusic.data.TrackPlayCount
import com.anplak.androidmusic.data.TrackStats
import com.anplak.androidmusic.data.TrackStatsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackSessionRecorderTest {
    private class FakeTrackStatsRepository : TrackStatsRepository {
        val qualifiedPlays = mutableListOf<Pair<Long, Long>>()
        val skips = mutableListOf<Long>()
        val completions = mutableListOf<Long>()

        override suspend fun recordQualifiedPlay(
            trackId: Long,
            timestamp: Long,
        ) {
            qualifiedPlays += trackId to timestamp
        }

        override suspend fun recordSkip(trackId: Long) {
            skips += trackId
        }

        override suspend fun recordCompletion(trackId: Long) {
            completions += trackId
        }

        override suspend fun getStats(trackId: Long): TrackStats? = null

        override fun observeStats(trackId: Long): Flow<TrackStats?> = emptyFlow()

        override suspend fun getAllStatsOrderedByPlayCount(): List<TrackStats> = emptyList()
    }

    private class FakePlayHistoryRepository : PlayHistoryRepository {
        val plays = mutableListOf<Pair<Long, String?>>()

        override suspend fun recordPlay(
            trackId: Long,
            sessionId: String?,
        ): Long {
            plays += trackId to sessionId
            return plays.size.toLong()
        }

        override suspend fun updateDuration(
            historyId: Long,
            duration: Long,
        ) = Unit

        override fun getHistory(
            limit: Int,
            offset: Int,
        ): Flow<List<PlayHistoryEntry>> = flowOf(emptyList())

        override fun getHistoryForTrack(
            trackId: Long,
            limit: Int,
        ): Flow<List<PlayHistoryEntry>> = flowOf(emptyList())

        override fun getHistorySince(timestamp: Long): Flow<List<PlayHistoryEntry>> = flowOf(emptyList())

        override fun getTotalPlayTimeSince(timestamp: Long): Flow<Long> = flowOf(0L)

        override fun getTopTracksSince(
            timestamp: Long,
            limit: Int,
        ): Flow<List<TrackPlayCount>> = flowOf(emptyList())

        override fun getTopArtistsSince(
            timestamp: Long,
            limit: Int,
        ): Flow<List<ArtistPlayCount>> = flowOf(emptyList())

        override suspend fun getHistoryCount(): Int = 0

        override suspend fun cleanupOldHistory(retentionDays: Int): Int = 0

        override suspend fun getCoPlayedTrackIds(
            seedTrackId: Long,
            limit: Int,
        ): List<Long> = emptyList()

        override suspend fun getLastSessionTrackIds(limit: Int): List<Long> = emptyList()
    }

    private fun createRecorder(
        scope: TestScope,
        tracker: PlaybackStatsTracker = PlaybackStatsTracker(),
        stats: FakeTrackStatsRepository = FakeTrackStatsRepository(),
        history: FakePlayHistoryRepository = FakePlayHistoryRepository(),
    ): Triple<PlaybackSessionRecorder, FakeTrackStatsRepository, FakePlayHistoryRepository> {
        val recorder =
            PlaybackSessionRecorder(
                playbackStatsTracker = tracker,
                trackStatsRepository = stats,
                playHistoryRepository = history,
                scope = scope,
            )
        return Triple(recorder, stats, history)
    }

    @Test
    fun `explicit track change records qualified play immediately`() =
        runTest(UnconfinedTestDispatcher()) {
            val tracker = PlaybackStatsTracker()
            tracker.markExplicitStart()
            val (recorder, stats, history) = createRecorder(this, tracker)

            recorder.onUiStateTick(trackId = 10L, isPlaying = true, positionMs = 0L, durationMs = 180_000L)
            advanceUntilIdle()

            assertEquals(1, stats.qualifiedPlays.size)
            assertEquals(10L, stats.qualifiedPlays.first().first)
            assertEquals(1, history.plays.size)
        }

    @Test
    fun `finalizeSession records skip for fast auto-advance session`() =
        runTest(UnconfinedTestDispatcher()) {
            val tracker = PlaybackStatsTracker()
            val (recorder, stats, _) = createRecorder(this, tracker)
            val session =
                PlaybackSession(
                    trackId = 5L,
                    startedAtMs = 1_000L,
                    startReason = PlayStartReason.AUTO_ADVANCE,
                    maxPositionMs = 5_000L,
                    durationMs = 180_000L,
                )

            recorder.finalizeSession(session, nowMs = 1_000L + RankingConfig.SKIP_WINDOW_MS - 1)
            advanceUntilIdle()

            assertEquals(listOf(5L), stats.skips)
        }

    @Test
    fun `manual advance records skip for unqualified auto-advance session`() =
        runTest(UnconfinedTestDispatcher()) {
            val tracker = PlaybackStatsTracker()
            tracker.markAutoAdvance()
            tracker.onTrackChanged(7L, nowMs = 0L)
            val (recorder, stats, _) = createRecorder(this, tracker)

            recorder.onManualAdvance()
            advanceUntilIdle()

            assertEquals(listOf(7L), stats.skips)
            assertTrue(tracker.currentSession()!!.skipRecorded)
        }

    @Test
    fun `completion is recorded when leaving a mostly finished track`() =
        runTest(UnconfinedTestDispatcher()) {
            val tracker = PlaybackStatsTracker()
            tracker.markExplicitStart()
            val (recorder, stats, _) = createRecorder(this, tracker)

            recorder.onUiStateTick(trackId = 1L, isPlaying = true, positionMs = 0L, durationMs = 100_000L)
            advanceUntilIdle()
            recorder.onUiStateTick(trackId = 1L, isPlaying = true, positionMs = 95_000L, durationMs = 100_000L)
            tracker.markExplicitStart()
            recorder.onUiStateTick(trackId = 2L, isPlaying = true, positionMs = 0L, durationMs = 100_000L)
            advanceUntilIdle()

            assertTrue(stats.completions.contains(1L))
        }
}
